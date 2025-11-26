package com.whisper3zzz.plugin.colorbrackets.util

import com.intellij.ui.JBColor
import java.awt.Color

object RainbowColors {
    // Colors cycle: Yellow -> Purple -> Blue -> Green -> Orange -> Cyan -> Red
    private val COLORS = listOf(
        Color(0xFF, 0xD7, 0x02), // Yellow
        Color(0xDA, 0x70, 0xD6), // Purple
        Color(0x17, 0x9F, 0xFF), // Blue
        Color(0x17, 0xE5, 0x61), // Green
        Color(0xFF, 0x93, 0x00), // Orange
        Color(0x00, 0xBF, 0xFF), // Cyan
        Color(0xFF, 0x45, 0x00)  // Red
    )

    val ROUND_BRACKETS = COLORS
    val SQUARE_BRACKETS = COLORS
    val CURLY_BRACKETS = COLORS
    val ANGLE_BRACKETS = COLORS

    fun getColor(level: Int, brackets: List<Color>): Color {
        return brackets[level % brackets.size]
    }
}
