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

    @Test
    fun `skips files larger than configured limit`() {
        val settings = ColorBracketsSettings()
        settings.enableLargeFileLimit = true
        settings.maxFileSizeKb = ColorBracketsSettings.MIN_FILE_SIZE_KB

        assertTrue(BracketSupport.shouldProcessFile("JAVA", 64 * 1024, settings))
        assertFalse(BracketSupport.shouldProcessFile("JAVA", 64 * 1024 + 1, settings))
    }

    @Test
    fun `processes large files when limit is disabled`() {
        val settings = ColorBracketsSettings()
        settings.enableLargeFileLimit = false
        settings.maxFileSizeKb = ColorBracketsSettings.MIN_FILE_SIZE_KB

        assertTrue(BracketSupport.shouldProcessFile("JAVA", 64 * 1024 + 1, settings))
    }

    @Test
    fun `large file check still excludes non code languages`() {
        val settings = ColorBracketsSettings()
        settings.enableLargeFileLimit = false

        assertFalse(BracketSupport.shouldProcessFile("TEXT", 1, settings))
    }

    @Test
    fun `language filter can allow only configured languages`() {
        val settings = ColorBracketsSettings()
        settings.languageFilterMode = ColorBracketsSettings.LANGUAGE_FILTER_ONLY
        settings.languageFilterList = "JAVA, kotlin; C++"

        assertTrue(BracketSupport.shouldProcessFile("JAVA", 1, settings))
        assertTrue(BracketSupport.shouldProcessFile("KOTLIN", 1, settings))
        assertTrue(BracketSupport.shouldProcessFile("c++", 1, settings))
        assertFalse(BracketSupport.shouldProcessFile("Python", 1, settings))
    }

    @Test
    fun `language filter can exclude configured languages`() {
        val settings = ColorBracketsSettings()
        settings.languageFilterMode = ColorBracketsSettings.LANGUAGE_FILTER_EXCEPT
        settings.languageFilterList = "Markdown, Python"

        assertFalse(BracketSupport.shouldProcessFile("Python", 1, settings))
        assertTrue(BracketSupport.shouldProcessFile("JAVA", 1, settings))
    }

    @Test
    fun `parses comma semicolon and newline separated language ids`() {
        assertEquals(
            setOf("JAVA", "KOTLIN", "C++"),
            BracketSupport.parseLanguageFilter("java, kotlin\nC++")
        )
    }
}
