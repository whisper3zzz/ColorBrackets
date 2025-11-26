package com.whisper3zzz.plugin.colorbrackets.annotator

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.psi.util.PsiTreeUtil
import com.whisper3zzz.plugin.colorbrackets.util.RainbowColors
import java.awt.Color
import java.awt.Font
import java.util.concurrent.ConcurrentHashMap

class RainbowAnnotator : Annotator {
    private val openingBrackets = setOf("(", "[", "{", "<")
    private val closingBrackets = setOf(")", "]", "}", ">")
    
    // Cache TextAttributes to reduce object allocation
    private val attributeCache = ConcurrentHashMap<Color, TextAttributes>()

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        // Optimization: Fast check for leaf nodes and text length
        if (element.firstChild != null) return // Not a leaf
        if (element.textLength != 1) return // Most brackets are single char

        val text = element.text
        if (!openingBrackets.contains(text) && !closingBrackets.contains(text)) return

        val level = getLevel(element)
        val color = getColorForBracket(text, level)
        val attributes = getAttributes(color)

        holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
            .range(element)
            .enforcedTextAttributes(attributes)
            .create()
    }

    private fun getAttributes(color: Color): TextAttributes {
        return attributeCache.computeIfAbsent(color) {
            TextAttributes(color, null, null, null, Font.PLAIN)
        }
    }

    private fun getLevel(element: PsiElement): Int {
        var level = 0
        // Start counting from the parent's parent to make the outermost bracket Level 0 (Yellow)
        // element is the bracket leaf node. element.parent is the container (e.g. Class).
        // We want the container itself to be Level 0.
        // So we count how many containers are *above* element.parent.
        var current = element.parent?.parent
        
        val maxDepth = 20
        while (current != null && current !is PsiFile && level < maxDepth) {
            if (hasBrackets(current)) {
                level++
            }
            current = current.parent
        }
        return level
    }

    private fun hasBrackets(element: PsiElement): Boolean {
        return CachedValuesManager.getCachedValue(element) {
            CachedValueProvider.Result.create(
                computeHasBrackets(element),
                PsiModificationTracker.MODIFICATION_COUNT
            )
        }
    }

    private fun computeHasBrackets(element: PsiElement): Boolean {
        // Check if this element has any children that are brackets
        // Optimization: Check first few and last few children
        val maxScan = 10
        
        var child = element.firstChild
        var count = 0
        while (child != null && count < maxScan) {
            if (child.textLength == 1 && openingBrackets.contains(child.text)) return true
            child = child.nextSibling
            count++
        }
        
        child = element.lastChild
        count = 0
        while (child != null && count < maxScan) {
            if (child.textLength == 1 && closingBrackets.contains(child.text)) return true
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
}
