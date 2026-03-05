package com.whisper3zzz.plugin.colorbrackets.visitor

import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInsight.daemon.impl.HighlightVisitor
import com.intellij.codeInsight.daemon.impl.analysis.HighlightInfoHolder
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.whisper3zzz.plugin.colorbrackets.util.RainbowColors
import java.awt.Color
import java.awt.Font
import java.util.concurrent.ConcurrentHashMap

class RainbowHighlightVisitor : HighlightVisitor, DumbAware {
    
    private val openingBrackets = setOf("(", "[", "{", "<")
    private val closingBrackets = setOf(")", "]", "}", ">")
    
    // Cache TextAttributes to reduce object allocation
    private val attributeCache = ConcurrentHashMap<Color, TextAttributes>()
    
    private var highlightInfoHolder: HighlightInfoHolder? = null

    override fun suitableForFile(file: PsiFile): Boolean {
        // Only process code files, not plain text or documents
        val language = file.language.id
        return !isExcludedLanguage(language)
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
        if (!openingBrackets.contains(text) && !closingBrackets.contains(text)) return

        val level = getLevel(element)
        val color = getColorForBracket(text, level)
        val attributes = getAttributes(color)

        addHighlight(element, attributes)
    }

    private fun addHighlight(element: PsiElement, attributes: TextAttributes) {
        val holder = highlightInfoHolder ?: return
        
        val builder = HighlightInfo.newHighlightInfo(HighlightSeverity.INFORMATION)
            .range(element)
            .textAttributes(attributes)
        
        holder.add(builder.create())
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
            if (openingBrackets.contains(child.text)) return true
            child = child.nextSibling
            count++
        }
        
        child = element.lastChild
        count = 0
        while (child != null && count < maxScan) {
            if (closingBrackets.contains(child.text)) return true
            child = child.prevSibling
            count++
        }
        
        return false
    }

    private fun getColorForBracket(text: String, level: Int): Color {
        return when (text) {
            "(", ")" -> RainbowColors.getColor(level, RainbowColors.ROUND_BRACKETS)
            "[", "]" -> RainbowColors.getColor(level, RainbowColors.SQUARE_BRACKETS)
            "{", "}" -> RainbowColors.getColor(level, RainbowColors.CURLY_BRACKETS)
            "<", ">" -> RainbowColors.getColor(level, RainbowColors.ANGLE_BRACKETS)
            else -> RainbowColors.getColor(level, RainbowColors.ROUND_BRACKETS)
        }
    }

    private fun isExcludedLanguage(language: String): Boolean {
        // Exclude plain text and document file types (not code)
        return excludedLanguages.contains(language)
    }

    companion object {
        private val excludedLanguages = setOf(
            "TEXT", "PLAIN_TEXT", "Markdown", "Properties", "Shell Script",
            "Batch", "Git file", "Log", "AsciiDoc", "reStructuredText"
        )
    }
}
