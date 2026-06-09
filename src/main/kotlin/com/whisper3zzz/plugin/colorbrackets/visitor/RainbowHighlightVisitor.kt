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
import com.whisper3zzz.plugin.colorbrackets.settings.ColorBracketsSettings
import com.whisper3zzz.plugin.colorbrackets.util.BracketDepthCache
import com.whisper3zzz.plugin.colorbrackets.util.BracketSupport
import java.awt.Color
import java.awt.Font
import java.util.concurrent.ConcurrentHashMap

class RainbowHighlightVisitor : HighlightVisitor, DumbAware {

    // Cache TextAttributes to reduce object allocation
    private val attributeCache = ConcurrentHashMap<AttributeKey, TextAttributes>()
    
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
        return BracketSupport.shouldProcessFile(language, file.textLength, settings)
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
        if (element.firstChild != null || element.textLength != 1) return

        val settings = ColorBracketsSettings.instance

        val bracket = BracketDepthCache.get(element) ?: return

        // Check per-bracket-type settings
        if (!BracketSupport.isEnabled(bracket.kind, settings)) return

        val color = BracketSupport.colorFor(bracket.kind, bracket.level, settings)
        val attributes = getAttributes(color, settings.boldBrackets)

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

    private fun getAttributes(color: Color, bold: Boolean): TextAttributes {
        return attributeCache.computeIfAbsent(AttributeKey(color, bold)) {
            val fontType = if (bold) Font.BOLD else Font.PLAIN
            TextAttributes(color, null, null, null, fontType)
        }
    }

    private data class AttributeKey(
        val color: Color,
        val bold: Boolean
    )
}
