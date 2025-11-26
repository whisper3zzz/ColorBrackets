package com.whisper3zzz.plugin.colorbrackets.util

import com.intellij.ui.JBColor
import java.awt.Color

object RainbowColors {
    // Colors cycle: Yellow -> Blue -> Orange -> Purple -> Green -> Cyan -> Red
    // Adjusted for better contrast between adjacent levels
    private val COLORS = listOf(
        Color(0xFF, 0xD7, 0x02), // Yellow (Level 0)
        Color(0x17, 0x9F, 0xFF), // Blue   (Level 1)
        Color(0xFF, 0x93, 0x00), // Orange (Level 2)
        Color(0xDA, 0x70, 0xD6), // Purple (Level 3)
        Color(0x17, 0xE5, 0x61), // Green  (Level 4)
        Color(0x00, 0xBF, 0xFF), // Cyan   (Level 5)
        Color(0xFF, 0x45, 0x00)  // Red    (Level 6)
    )

    val ROUND_BRACKETS = COLORS
    val SQUARE_BRACKETS = COLORS
    val CURLY_BRACKETS = COLORS
    val ANGLE_BRACKETS = COLORS

    fun getColor(level: Int, brackets: List<Color>): Color {
        return brackets[level % brackets.size]
    }
}
