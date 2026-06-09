package com.whisper3zzz.plugin.colorbrackets.util

import com.whisper3zzz.plugin.colorbrackets.settings.ColorBracketsSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BracketSupportTest {
    @Test
    fun `maps bracket symbols to their kind`() {
        assertEquals(BracketKind.ROUND, BracketSupport.kindOf("("))
        assertEquals(BracketKind.ROUND, BracketSupport.kindOf(")"))
        assertEquals(BracketKind.SQUARE, BracketSupport.kindOf("["))
        assertEquals(BracketKind.SQUARE, BracketSupport.kindOf("]"))
        assertEquals(BracketKind.CURLY, BracketSupport.kindOf("{"))
        assertEquals(BracketKind.CURLY, BracketSupport.kindOf("}"))
        assertEquals(BracketKind.ANGLE, BracketSupport.kindOf("<"))
        assertEquals(BracketKind.ANGLE, BracketSupport.kindOf(">"))
        assertNull(BracketSupport.kindOf("a"))
    }

    @Test
    fun `detects opening and closing brackets`() {
        assertTrue(BracketSupport.isOpeningBracket("("))
        assertTrue(BracketSupport.isOpeningBracket("["))
        assertTrue(BracketSupport.isOpeningBracket("{"))
        assertTrue(BracketSupport.isOpeningBracket("<"))
        assertFalse(BracketSupport.isOpeningBracket(")"))
        assertFalse(BracketSupport.isOpeningBracket("x"))

        assertTrue(BracketSupport.isClosingBracket(")"))
        assertTrue(BracketSupport.isClosingBracket("]"))
        assertTrue(BracketSupport.isClosingBracket("}"))
        assertTrue(BracketSupport.isClosingBracket(">"))
        assertFalse(BracketSupport.isClosingBracket("("))
        assertFalse(BracketSupport.isClosingBracket("x"))
    }

    @Test
    fun `uses per bracket settings`() {
        val settings = ColorBracketsSettings()

        settings.enableRoundBrackets = false
        settings.enableSquareBrackets = true
        settings.enableCurlyBrackets = false
        settings.enableAngleBrackets = true

        assertFalse(BracketSupport.isEnabled(BracketKind.ROUND, settings))
        assertTrue(BracketSupport.isEnabled(BracketKind.SQUARE, settings))
        assertFalse(BracketSupport.isEnabled(BracketKind.CURLY, settings))
        assertTrue(BracketSupport.isEnabled(BracketKind.ANGLE, settings))
    }

    @Test
    fun `excludes non code languages`() {
        assertTrue(BracketSupport.isExcludedLanguage("TEXT"))
        assertTrue(BracketSupport.isExcludedLanguage("Markdown"))
        assertTrue(BracketSupport.isExcludedLanguage("Log"))
        assertFalse(BracketSupport.isExcludedLanguage("JAVA"))
        assertFalse(BracketSupport.isExcludedLanguage("kotlin"))
    }
}
