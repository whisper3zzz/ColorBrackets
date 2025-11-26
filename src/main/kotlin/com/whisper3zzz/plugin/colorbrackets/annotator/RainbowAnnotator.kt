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
        // Optimization: Check text length first as it's the cheapest
        if (element.textLength != 1) return 
        
        // Relaxed leaf check: Instead of element.firstChild != null, we check if it has significant children.
        // Some PSI implementations might have empty children arrays but return null for firstChild, or vice versa.
        // Or they might have children that are just whitespace/empty.
        // For safety in C++ (CLion) and other complex PSIs, we trust the text check more.
        // But to avoid annotating parent nodes that happen to have text "(" (unlikely for 1-char nodes but possible),
        // we can check if it's a "token" type or has no children.
        if (element.children.isNotEmpty()) return

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
        var current = element.parent
        
        val maxDepth = 20
        while (current != null && current !is PsiFile && level < maxDepth) {
            if (hasBrackets(current)) {
                level++
            }
            current = current.parent
        }
        // The immediate parent (or the first container found) is the container OF the bracket.
        // We want that to be Level 0.
        // So we subtract 1 from the total count of containers.
        return (level - 1).coerceAtLeast(0)
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
        // Increased scan limit to handle complex nodes (e.g. C++ macros, long lists)
        val maxScan = 100
        
        var child = element.firstChild
        var count = 0
        while (child != null && count < maxScan) {
            // Relaxed check: just contains text, ignore length check to be safe against whitespace/tokens
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
}
