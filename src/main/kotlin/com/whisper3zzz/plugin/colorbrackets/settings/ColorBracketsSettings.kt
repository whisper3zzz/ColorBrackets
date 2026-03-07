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
        var enableScopeHighlight: Boolean = true
    )

    private var state = State()

    override fun getState(): State = state

    override fun loadState(state: State) {
        this.state = state
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

    var enableScopeHighlight: Boolean
        get() = state.enableScopeHighlight
        set(value) { state.enableScopeHighlight = value }

    companion object {
        val instance: ColorBracketsSettings
            get() = ApplicationManager.getApplication().getService(ColorBracketsSettings::class.java)
    }
}
