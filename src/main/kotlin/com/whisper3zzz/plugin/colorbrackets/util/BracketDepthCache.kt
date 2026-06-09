package com.whisper3zzz.plugin.colorbrackets.util

import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker

object BracketDepthCache {
    data class Entry(
        val kind: BracketKind,
        val level: Int
    )

    private data class OpenBracket(
        val kind: BracketKind,
        val level: Int
    )

    internal data class ScanToken(
        val symbol: String,
        val offset: Int
    )

    private val skippedContextMarkers = listOf(
        "COMMENT",
        "STRING",
        "CHARACTER_LITERAL",
        "CHAR_LITERAL",
        "REGEXP"
    )

    private val angleContextMarkers = listOf(
        "TYPE_PARAMETER",
        "TYPE_ARGUMENT",
        "REFERENCE_PARAMETER",
        "TEMPLATE",
        "GENERIC"
    )

    fun get(file: PsiFile): Map<Int, Entry> {
        return CachedValuesManager.getCachedValue(file) {
            CachedValueProvider.Result.create(
                build(file),
                PsiModificationTracker.MODIFICATION_COUNT
            )
        }
    }

    fun get(element: PsiElement): Entry? {
        val file = element.containingFile ?: return null
        return get(file)[element.textRange.startOffset]
    }

    private fun build(file: PsiFile): Map<Int, Entry> {
        val tokens = ArrayList<ScanToken>()
        forEachLeaf(file) { leaf ->
            if (leaf.textLength != 1 || shouldSkipContext(leaf)) return@forEachLeaf

            val symbol = leaf.text
            val kind = BracketSupport.kindOf(symbol) ?: return@forEachLeaf
            if (kind == BracketKind.ANGLE && !isLikelyAngleBracket(leaf)) return@forEachLeaf

            tokens.add(ScanToken(symbol, leaf.textRange.startOffset))
        }

        return buildEntries(tokens.asSequence())
    }

    internal fun buildEntries(tokens: Sequence<ScanToken>): Map<Int, Entry> {
        val entries = HashMap<Int, Entry>()
        val stack = ArrayList<OpenBracket>()

        tokens.forEach { token ->
            val kind = BracketSupport.kindOf(token.symbol) ?: return@forEach
            if (BracketSupport.isOpeningBracket(token.symbol)) {
                val level = stack.size
                entries[token.offset] = Entry(kind, level)
                stack.add(OpenBracket(kind, level))
            } else {
                val level = popMatchingLevel(stack, kind)
                entries[token.offset] = Entry(kind, level)
            }
        }

        return entries
    }

    private fun popMatchingLevel(stack: MutableList<OpenBracket>, kind: BracketKind): Int {
        val matchIndex = stack.indexOfLast { it.kind == kind }
        if (matchIndex < 0) return stack.size

        val level = stack[matchIndex].level
        while (stack.size > matchIndex) {
            stack.removeAt(stack.lastIndex)
        }
        return level
    }

    private fun forEachLeaf(root: PsiElement, consumer: (PsiElement) -> Unit) {
        val pending = ArrayDeque<PsiElement>()
        pending.add(root)

        while (pending.isNotEmpty()) {
            val current = pending.removeLast()
            if (current.firstChild == null) {
                consumer(current)
                continue
            }

            var child = current.lastChild
            while (child != null) {
                pending.add(child)
                child = child.prevSibling
            }
        }
    }

    private fun shouldSkipContext(element: PsiElement): Boolean {
        if (element is PsiComment) return true

        var current: PsiElement? = element
        var depth = 0
        while (current != null && depth < 4) {
            val typeName = current.node?.elementType?.toString()?.uppercase().orEmpty()
            if (skippedContextMarkers.any { marker -> marker in typeName }) return true
            current = current.parent
            depth++
        }

        return false
    }

    private fun isLikelyAngleBracket(element: PsiElement): Boolean {
        return acceptsAngleBracketContext(contextTypeNames(element))
    }

    private fun contextTypeNames(element: PsiElement): Sequence<String> = sequence {
        var current: PsiElement? = element
        var depth = 0
        while (current != null && depth < 8) {
            yield(current.node?.elementType?.toString().orEmpty())
            current = current.parent
            depth++
        }
    }

    internal fun acceptsAngleBracketContext(contextTypeNames: Sequence<String>): Boolean {
        return contextTypeNames
            .map { it.uppercase() }
            .any { typeName -> angleContextMarkers.any { marker -> marker in typeName } }
    }
}
