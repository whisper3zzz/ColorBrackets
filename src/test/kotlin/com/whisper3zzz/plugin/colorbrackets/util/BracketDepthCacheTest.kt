package com.whisper3zzz.plugin.colorbrackets.util

import com.whisper3zzz.plugin.colorbrackets.settings.ColorBracketsSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class BracketDepthCacheTest {
    @Test
    fun `calculates nesting levels from a bracket stack`() {
        val entries = BracketDepthCache.buildEntries(nestedTokens())

        assertEntry(entries, 0, BracketKind.CURLY, 0)
        assertEntry(entries, 1, BracketKind.ROUND, 1)
        assertEntry(entries, 2, BracketKind.SQUARE, 2)
        assertEntry(entries, 3, BracketKind.SQUARE, 2)
        assertEntry(entries, 4, BracketKind.ROUND, 1)
        assertEntry(entries, 5, BracketKind.CURLY, 0)
    }

    @Test
    fun `builds bracket pairs with matching offsets and levels`() {
        val pairs = BracketDepthCache.buildPairs(nestedTokens())

        assertPair(pairs[0], BracketKind.SQUARE, 2, 3, 2)
        assertPair(pairs[1], BracketKind.ROUND, 1, 4, 1)
        assertPair(pairs[2], BracketKind.CURLY, 0, 5, 0)
    }

    @Test
    fun `finds the innermost containing pair by kind`() {
        val pairs = BracketDepthCache.buildPairs(
            sequenceOf(
                BracketDepthCache.ScanToken("{", 0),
                BracketDepthCache.ScanToken("{", 2),
                BracketDepthCache.ScanToken("}", 4),
                BracketDepthCache.ScanToken("}", 6)
            )
        )

        val inner = BracketDepthCache.findContainingPair(pairs, 3, BracketKind.CURLY)
        assertNotNull(inner)
        assertPair(inner, BracketKind.CURLY, 2, 4, 1)

        val outer = BracketDepthCache.findContainingPair(pairs, 5, BracketKind.CURLY)
        assertNotNull(outer)
        assertPair(outer, BracketKind.CURLY, 0, 6, 0)
    }

    @Test
    fun `containing pair lookup filters by bracket kind`() {
        val pairs = BracketDepthCache.buildPairs(nestedTokens())

        val curly = BracketDepthCache.findContainingPair(pairs, 2, BracketKind.CURLY)
        assertNotNull(curly)
        assertPair(curly, BracketKind.CURLY, 0, 5, 0)

        val square = BracketDepthCache.findContainingPair(pairs, 2, BracketKind.SQUARE)
        assertNotNull(square)
        assertPair(square, BracketKind.SQUARE, 2, 3, 2)
    }

    @Test
    fun `angle brackets contribute to nesting only after context filtering accepts them`() {
        val entries = BracketDepthCache.buildEntries(
            sequenceOf(
                BracketDepthCache.ScanToken("<", 0),
                BracketDepthCache.ScanToken("(", 1),
                BracketDepthCache.ScanToken(")", 2),
                BracketDepthCache.ScanToken(">", 3)
            )
        )

        assertEntry(entries, 0, BracketKind.ANGLE, 0)
        assertEntry(entries, 1, BracketKind.ROUND, 1)
        assertEntry(entries, 2, BracketKind.ROUND, 1)
        assertEntry(entries, 3, BracketKind.ANGLE, 0)
    }

    @Test
    fun `accepts angle brackets in generic type contexts`() {
        assertTrue(
            BracketDepthCache.acceptsAngleBracketContext(
                sequenceOf("LT", "TYPE_ARGUMENT_LIST", "REFERENCE_EXPRESSION")
            )
        )
        assertTrue(
            BracketDepthCache.acceptsAngleBracketContext(
                sequenceOf("GT", "TYPE_PARAMETER_LIST", "CLASS_DECLARATION")
            )
        )
        assertTrue(
            BracketDepthCache.acceptsAngleBracketContext(
                sequenceOf("LT", "TEMPLATE_ARGUMENT_LIST", "TEMPLATE_DECLARATION")
            )
        )
        assertTrue(
            BracketDepthCache.acceptsAngleBracketContext(
                sequenceOf("LT", "CPP_TEMPLATE_ID", "CPP_TEMPLATE_DECLARATION")
            )
        )
        assertTrue(
            BracketDepthCache.acceptsAngleBracketContext(
                sequenceOf("GT", "TEMPLATE_PARAMETER_LIST", "CPP_CLASS")
            )
        )
    }

    @Test
    fun `rejects angle brackets in comparison contexts`() {
        assertFalse(
            BracketDepthCache.acceptsAngleBracketContext(
                sequenceOf("LT", "BINARY_EXPRESSION", "EXPRESSION")
            )
        )
        assertFalse(
            BracketDepthCache.acceptsAngleBracketContext(
                sequenceOf("GT", "RELATIONAL_EXPRESSION", "IF_STATEMENT")
            )
        )
    }

    @Test
    fun `auto angle mode accepts only generic contexts`() {
        val settings = ColorBracketsSettings()
        settings.angleBracketMode = ColorBracketsSettings.ANGLE_BRACKET_AUTO

        assertTrue(
            BracketDepthCache.shouldIncludeAngleBracket(
                sequenceOf("LT", "CPP_TEMPLATE_ID"),
                settings
            )
        )
        assertFalse(
            BracketDepthCache.shouldIncludeAngleBracket(
                sequenceOf("LT", "BINARY_EXPRESSION"),
                settings
            )
        )
    }

    @Test
    fun `always angle mode accepts comparison contexts`() {
        val settings = ColorBracketsSettings()
        settings.angleBracketMode = ColorBracketsSettings.ANGLE_BRACKET_ALWAYS

        assertTrue(
            BracketDepthCache.shouldIncludeAngleBracket(
                sequenceOf("LT", "BINARY_EXPRESSION"),
                settings
            )
        )
    }

    @Test
    fun `never angle mode excludes generic contexts`() {
        val settings = ColorBracketsSettings()
        settings.angleBracketMode = ColorBracketsSettings.ANGLE_BRACKET_NEVER

        assertFalse(
            BracketDepthCache.shouldIncludeAngleBracket(
                sequenceOf("LT", "CPP_TEMPLATE_ID"),
                settings
            )
        )
    }

    @Test
    fun `disabled angle brackets are excluded from cache`() {
        val settings = ColorBracketsSettings()
        settings.enableAngleBrackets = false
        settings.angleBracketMode = ColorBracketsSettings.ANGLE_BRACKET_ALWAYS

        assertFalse(
            BracketDepthCache.shouldIncludeAngleBracket(
                sequenceOf("LT", "CPP_TEMPLATE_ID"),
                settings
            )
        )
    }

    private fun assertEntry(
        entries: Map<Int, BracketDepthCache.Entry>,
        offset: Int,
        kind: BracketKind,
        level: Int
    ) {
        val entry = entries[offset]
        assertNotNull(entry, "Expected bracket entry at offset $offset")
        assertEquals(kind, entry.kind)
        assertEquals(level, entry.level)
    }

    private fun assertPair(
        pair: BracketDepthCache.BracketPair,
        kind: BracketKind,
        openingOffset: Int,
        closingOffset: Int,
        level: Int
    ) {
        assertEquals(kind, pair.kind)
        assertEquals(openingOffset, pair.openingOffset)
        assertEquals(closingOffset, pair.closingOffset)
        assertEquals(level, pair.level)
    }

    private fun nestedTokens(): Sequence<BracketDepthCache.ScanToken> {
        return sequenceOf(
            BracketDepthCache.ScanToken("{", 0),
            BracketDepthCache.ScanToken("(", 1),
            BracketDepthCache.ScanToken("[", 2),
            BracketDepthCache.ScanToken("]", 3),
            BracketDepthCache.ScanToken(")", 4),
            BracketDepthCache.ScanToken("}", 5)
        )
    }
}
