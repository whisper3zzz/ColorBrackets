package com.whisper3zzz.plugin.colorbrackets.visitor

import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInsight.daemon.impl.HighlightInfoType
import com.intellij.codeInsight.daemon.impl.HighlightVisitor
import com.intellij.codeInsight.daemon.impl.analysis.HighlightInfoHolder
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.whisper3zzz.plugin.colorbrackets.settings.ColorBracketsSettings
import com.whisper3zzz.plugin.colorbrackets.util.BracketSupport
import java.awt.Color
import java.awt.Font
import java.util.concurrent.ConcurrentHashMap

class RainbowHighlightVisitor : HighlightVisitor, DumbAware {

    // Cache TextAttributes to reduce object allocation
    private val attributeCache = ConcurrentHashMap<Color, TextAttributes>()
    
    private var highlightInfoHolder: HighlightInfoHolder? = null
    
    private val rainbowInfoType = HighlightInfoType.HighlightInfoTypeImpl(
        HighlightSeverity.INFORMATION, 
        DefaultLanguageHighlighterColors.CONSTANT
    )

    override fun suitableForFile(file: PsiFile): Boolean {
        val settings = ColorBracketsSettings.instance
        if (!settings.isEnabled) return false
        // Only process code files, not plain text or documents
        val language = file.language.id
        return !BracketSupport.isExcludedLanguage(language)
    }

    override fun analyze(
        file: PsiFile,
        updateWholeFile: Boolean,
        holder: HighlightInfoHolder,
        action: Runnable
    ): Boolean {
        highlightInfoHolder = holder
        try {
            action.run()
        } finally {
            highlightInfoHolder = null
        }
        return true
    }

    override fun visit(element: PsiElement) {
        // Only process leaf elements with single character
        if (element !is LeafPsiElement || element.textLength != 1) return
        
        val text = element.text
        val settings = ColorBracketsSettings.instance

        val kind = BracketSupport.kindOf(text) ?: return

        // Check per-bracket-type settings
        if (!BracketSupport.isEnabled(kind, settings)) return

        val level = getLevel(element)
        val color = BracketSupport.colorFor(kind, level)
        val attributes = getAttributes(color)

        addHighlight(element, attributes)
    }

    private fun addHighlight(element: PsiElement, attributes: TextAttributes) {
        val holder = highlightInfoHolder ?: return
        
        val info = HighlightInfo.newHighlightInfo(rainbowInfoType)
            .range(element)
            .textAttributes(attributes)
            .create()
        
        holder.add(info)
    }

    override fun clone(): HighlightVisitor = RainbowHighlightVisitor()

    private fun getAttributes(color: Color): TextAttributes {
        return attributeCache.computeIfAbsent(color) {
            TextAttributes(color, null, null, null, Font.PLAIN)
        }
    }

    private fun getLevel(element: PsiElement): Int {
        var level = 0
        var current = element.parent
        
        val maxDepth = 20
        while (current != null && current !is PsiFile && level < maxDepth) {
            if (hasBrackets(current)) {
                level++
            }
            current = current.parent
        }
        return (level - 1).coerceAtLeast(0)
    }

    private fun hasBrackets(element: PsiElement): Boolean {
        val maxScan = 100
        
        var child = element.firstChild
        var count = 0
        while (child != null && count < maxScan) {
            if (BracketSupport.isOpeningBracket(child.text)) return true
            child = child.nextSibling
            count++
        }
        
        child = element.lastChild
        count = 0
        while (child != null && count < maxScan) {
            if (BracketSupport.isClosingBracket(child.text)) return true
            child = child.prevSibling
            count++
        }
        
        return false
    }
}
