package com.whisper3zzz.plugin.colorbrackets.util

import com.whisper3zzz.plugin.colorbrackets.settings.ColorBracketsSettings
import java.awt.Color

enum class BracketKind(
    val opening: String,
    val closing: String
) {
    ROUND("(", ")"),
    SQUARE("[", "]"),
    CURLY("{", "}"),
    ANGLE("<", ">")
}

object BracketSupport {
    private val bySymbol = BracketKind.entries
        .flatMap { kind -> listOf(kind.opening to kind, kind.closing to kind) }
        .toMap()

    private val openingSymbols = BracketKind.entries.map { it.opening }.toSet()
    private val closingSymbols = BracketKind.entries.map { it.closing }.toSet()

    private val excludedLanguages = setOf(
        "TEXT", "PLAIN_TEXT", "Markdown", "Properties", "Shell Script",
        "Batch", "Git file", "Log", "AsciiDoc", "reStructuredText"
    )

    fun kindOf(symbol: String): BracketKind? = bySymbol[symbol]

    fun isOpeningBracket(symbol: String): Boolean = symbol in openingSymbols

    fun isClosingBracket(symbol: String): Boolean = symbol in closingSymbols

    fun isExcludedLanguage(language: String): Boolean = language in excludedLanguages

    fun shouldProcessFile(language: String, textLength: Int, settings: ColorBracketsSettings): Boolean {
        if (isExcludedLanguage(language)) return false
        if (!settings.enableLargeFileLimit) return true

        val maxBytes = settings.maxFileSizeKb * 1024
        return textLength <= maxBytes
    }

    fun isEnabled(kind: BracketKind, settings: ColorBracketsSettings): Boolean {
        return when (kind) {
            BracketKind.ROUND -> settings.enableRoundBrackets
            BracketKind.SQUARE -> settings.enableSquareBrackets
            BracketKind.CURLY -> settings.enableCurlyBrackets
            BracketKind.ANGLE -> settings.enableAngleBrackets
        }
    }

    fun colorFor(kind: BracketKind, level: Int): Color {
        val palette = when (kind) {
            BracketKind.ROUND -> RainbowColors.ROUND_BRACKETS
            BracketKind.SQUARE -> RainbowColors.SQUARE_BRACKETS
            BracketKind.CURLY -> RainbowColors.CURLY_BRACKETS
            BracketKind.ANGLE -> RainbowColors.ANGLE_BRACKETS
        }
        return RainbowColors.getColor(level, palette)
    }
}
