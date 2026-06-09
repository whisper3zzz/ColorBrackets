package com.whisper3zzz.plugin.colorbrackets.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@State(
    name = "ColorBracketsSettings",
    storages = [Storage("color-brackets.xml")]
)
class ColorBracketsSettings : PersistentStateComponent<ColorBracketsSettings.State> {

    data class State(
        var isEnabled: Boolean = true,
        var enableRoundBrackets: Boolean = true,
        var enableSquareBrackets: Boolean = true,
        var enableCurlyBrackets: Boolean = true,
        var enableAngleBrackets: Boolean = true,
        var angleBracketMode: String = ANGLE_BRACKET_AUTO,
        var enableScopeHighlight: Boolean = true,
        var enableLargeFileLimit: Boolean = true,
        var maxFileSizeKb: Int = DEFAULT_MAX_FILE_SIZE_KB,
        var colorPalette: String = COLOR_PALETTE_DEFAULT,
        var boldBrackets: Boolean = false,
        var scopeLineWidth: Int = DEFAULT_SCOPE_LINE_WIDTH,
        var scopeLineOpacity: Int = DEFAULT_SCOPE_LINE_OPACITY,
        var languageFilterMode: String = LANGUAGE_FILTER_ALL,
        var languageFilterList: String = ""
    )

    private var state = State()

    override fun getState(): State = state

    override fun loadState(state: State) {
        this.state = state
        ColorBracketsSettingsTracker.incModificationCount()
    }

    var isEnabled: Boolean
        get() = state.isEnabled
        set(value) { state.isEnabled = value }

    var enableRoundBrackets: Boolean
        get() = state.enableRoundBrackets
        set(value) { state.enableRoundBrackets = value }

    var enableSquareBrackets: Boolean
        get() = state.enableSquareBrackets
        set(value) { state.enableSquareBrackets = value }

    var enableCurlyBrackets: Boolean
        get() = state.enableCurlyBrackets
        set(value) { state.enableCurlyBrackets = value }

    var enableAngleBrackets: Boolean
        get() = state.enableAngleBrackets
        set(value) { state.enableAngleBrackets = value }

    var angleBracketMode: String
        get() = state.angleBracketMode
        set(value) {
            state.angleBracketMode = if (value in ANGLE_BRACKET_MODES) value else ANGLE_BRACKET_AUTO
        }

    var enableScopeHighlight: Boolean
        get() = state.enableScopeHighlight
        set(value) { state.enableScopeHighlight = value }

    var enableLargeFileLimit: Boolean
        get() = state.enableLargeFileLimit
        set(value) { state.enableLargeFileLimit = value }

    var maxFileSizeKb: Int
        get() = state.maxFileSizeKb
        set(value) { state.maxFileSizeKb = value.coerceIn(MIN_FILE_SIZE_KB, MAX_FILE_SIZE_KB) }

    var colorPalette: String
        get() = state.colorPalette
        set(value) {
            state.colorPalette = if (value in COLOR_PALETTES) value else COLOR_PALETTE_DEFAULT
        }

    var boldBrackets: Boolean
        get() = state.boldBrackets
        set(value) { state.boldBrackets = value }

    var scopeLineWidth: Int
        get() = state.scopeLineWidth
        set(value) { state.scopeLineWidth = value.coerceIn(MIN_SCOPE_LINE_WIDTH, MAX_SCOPE_LINE_WIDTH) }

    var scopeLineOpacity: Int
        get() = state.scopeLineOpacity
        set(value) { state.scopeLineOpacity = value.coerceIn(MIN_SCOPE_LINE_OPACITY, MAX_SCOPE_LINE_OPACITY) }

    var languageFilterMode: String
        get() = state.languageFilterMode
        set(value) {
            state.languageFilterMode = if (value in LANGUAGE_FILTER_MODES) value else LANGUAGE_FILTER_ALL
        }

    var languageFilterList: String
        get() = state.languageFilterList
        set(value) { state.languageFilterList = value }

    companion object {
        const val DEFAULT_MAX_FILE_SIZE_KB = 1024
        const val MIN_FILE_SIZE_KB = 64
        const val MAX_FILE_SIZE_KB = 102400
        const val DEFAULT_SCOPE_LINE_WIDTH = 1
        const val MIN_SCOPE_LINE_WIDTH = 1
        const val MAX_SCOPE_LINE_WIDTH = 8
        const val DEFAULT_SCOPE_LINE_OPACITY = 100
        const val MIN_SCOPE_LINE_OPACITY = 10
        const val MAX_SCOPE_LINE_OPACITY = 100

        const val COLOR_PALETTE_DEFAULT = "DEFAULT"
        const val COLOR_PALETTE_VIVID = "VIVID"
        const val COLOR_PALETTE_SOFT = "SOFT"
        val COLOR_PALETTES = setOf(COLOR_PALETTE_DEFAULT, COLOR_PALETTE_VIVID, COLOR_PALETTE_SOFT)

        const val ANGLE_BRACKET_AUTO = "AUTO"
        const val ANGLE_BRACKET_ALWAYS = "ALWAYS"
        const val ANGLE_BRACKET_NEVER = "NEVER"
        val ANGLE_BRACKET_MODES = setOf(ANGLE_BRACKET_AUTO, ANGLE_BRACKET_ALWAYS, ANGLE_BRACKET_NEVER)

        const val LANGUAGE_FILTER_ALL = "ALL"
        const val LANGUAGE_FILTER_ONLY = "ONLY"
        const val LANGUAGE_FILTER_EXCEPT = "EXCEPT"
        val LANGUAGE_FILTER_MODES = setOf(LANGUAGE_FILTER_ALL, LANGUAGE_FILTER_ONLY, LANGUAGE_FILTER_EXCEPT)

        val instance: ColorBracketsSettings
            get() = ApplicationManager.getApplication().getService(ColorBracketsSettings::class.java)
    }
}
