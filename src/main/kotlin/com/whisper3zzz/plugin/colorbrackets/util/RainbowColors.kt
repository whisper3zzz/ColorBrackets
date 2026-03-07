package com.whisper3zzz.plugin.colorbrackets.util

import com.intellij.ide.ui.LafManagerListener
import com.intellij.openapi.application.ApplicationManager
import com.intellij.util.ui.UIUtil
import java.awt.Color

object RainbowColors {

    // Dark theme colors (high contrast on dark backgrounds)
    private val COLORS_DARK = listOf(
        Color(0xFF, 0xD7, 0x02), // Yellow
        Color(0x17, 0x9F, 0xFF), // Blue
        Color(0xFF, 0x93, 0x00), // Orange
        Color(0xDA, 0x70, 0xD6), // Purple
        Color(0x17, 0xE5, 0x61), // Green
        Color(0x00, 0xBF, 0xFF), // Cyan
        Color(0xFF, 0x45, 0x00)  // Red
    )

    // Light theme colors (softer but still distinct on light backgrounds)
    private val COLORS_LIGHT = listOf(
        Color(0xC6, 0x88, 0x00), // Dark Yellow
        Color(0x00, 0x6F, 0xC6), // Dark Blue
        Color(0xC8, 0x63, 0x00), // Dark Orange
        Color(0x8B, 0x38, 0x8B), // Dark Purple
        Color(0x00, 0x8B, 0x37), // Dark Green
        Color(0x00, 0x82, 0xB0), // Dark Cyan
        Color(0xC8, 0x28, 0x00)  // Dark Red
    )

    private val COLORS: List<Color>
        get() = if (UIUtil.isUnderDarcula()) COLORS_DARK else COLORS_LIGHT

    val ROUND_BRACKETS: List<Color> get() = COLORS
    val SQUARE_BRACKETS: List<Color> get() = COLORS
    val CURLY_BRACKETS: List<Color> get() = COLORS
    val ANGLE_BRACKETS: List<Color> get() = COLORS

    fun getColor(level: Int, brackets: List<Color>): Color {
        return brackets[level % brackets.size]
    }
}
