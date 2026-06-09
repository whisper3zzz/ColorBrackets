package com.whisper3zzz.plugin.colorbrackets.util

import com.intellij.util.ui.UIUtil
import com.whisper3zzz.plugin.colorbrackets.settings.ColorBracketsSettings
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

    private val VIVID = listOf(
        Color(0xFF, 0xB3, 0x00),
        Color(0x00, 0xA6, 0xFF),
        Color(0xFF, 0x5C, 0x8A),
        Color(0x9B, 0x72, 0xFF),
        Color(0x00, 0xD0, 0x84),
        Color(0x00, 0xC2, 0xD1),
        Color(0xFF, 0x7A, 0x00)
    )

    private val SOFT_DARK = listOf(
        Color(0xE6, 0xC7, 0x5C),
        Color(0x7A, 0xB8, 0xE8),
        Color(0xE8, 0xA0, 0x6A),
        Color(0xC7, 0x9A, 0xD9),
        Color(0x72, 0xC9, 0x85),
        Color(0x6F, 0xC8, 0xD6),
        Color(0xE0, 0x7A, 0x75)
    )

    private val SOFT_LIGHT = listOf(
        Color(0x9A, 0x74, 0x1E),
        Color(0x3E, 0x7E, 0xA8),
        Color(0xA8, 0x61, 0x36),
        Color(0x81, 0x5A, 0x93),
        Color(0x3C, 0x85, 0x4E),
        Color(0x3B, 0x83, 0x8C),
        Color(0x9A, 0x4F, 0x4A)
    )

    private val COLORS: List<Color>
        get() = if (UIUtil.isUnderDarcula()) COLORS_DARK else COLORS_LIGHT

    val ROUND_BRACKETS: List<Color> get() = COLORS
    val SQUARE_BRACKETS: List<Color> get() = COLORS
    val CURLY_BRACKETS: List<Color> get() = COLORS
    val ANGLE_BRACKETS: List<Color> get() = COLORS

    fun getColor(level: Int, brackets: List<Color>, paletteName: String? = null): Color {
        val palette = when (paletteName) {
            ColorBracketsSettings.COLOR_PALETTE_VIVID -> VIVID
            ColorBracketsSettings.COLOR_PALETTE_SOFT -> if (UIUtil.isUnderDarcula()) SOFT_DARK else SOFT_LIGHT
            else -> brackets
        }
        return palette[level % palette.size]
    }
}
