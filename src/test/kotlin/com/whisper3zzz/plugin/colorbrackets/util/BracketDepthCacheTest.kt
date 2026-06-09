package com.whisper3zzz.plugin.colorbrackets.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class BracketDepthCacheTest {
    @Test
    fun `calculates nesting levels from a bracket stack`() {
        val entries = BracketDepthCache.buildEntries(
            sequenceOf(
                BracketDepthCache.ScanToken("{", 0),
                BracketDepthCache.ScanToken("(", 1),
                BracketDepthCache.ScanToken("[", 2),
                BracketDepthCache.ScanToken("]", 3),
                BracketDepthCache.ScanToken(")", 4),
                BracketDepthCache.ScanToken("}", 5)
            )
        )

        assertEntry(entries, 0, BracketKind.CURLY, 0)
        assertEntry(entries, 1, BracketKind.ROUND, 1)
        assertEntry(entries, 2, BracketKind.SQUARE, 2)
        assertEntry(entries, 3, BracketKind.SQUARE, 2)
        assertEntry(entries, 4, BracketKind.ROUND, 1)
        assertEntry(entries, 5, BracketKind.CURLY, 0)
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
}
