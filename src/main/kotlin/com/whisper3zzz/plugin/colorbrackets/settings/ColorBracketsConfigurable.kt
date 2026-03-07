package com.whisper3zzz.plugin.colorbrackets.settings

import com.intellij.openapi.options.Configurable
import com.intellij.ui.components.JBCheckBox
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

class ColorBracketsConfigurable : Configurable {

    private var panel: JPanel? = null

    private val cbEnabled = JBCheckBox("启用 ColorBrackets")
    private val cbRoundBrackets = JBCheckBox("彩色圆括号 ( )")
    private val cbSquareBrackets = JBCheckBox("彩色方括号 [ ]")
    private val cbCurlyBrackets = JBCheckBox("彩色大括号 { }")
    private val cbAngleBrackets = JBCheckBox("彩色尖括号 < >")
    private val cbScopeHighlight = JBCheckBox("启用代码块竖线高亮")

    override fun getDisplayName(): String = "ColorBrackets"

    override fun createComponent(): JComponent {
        // Toggle bracket options when master switch changes
        cbEnabled.addChangeListener {
            val enabled = cbEnabled.isSelected
            cbRoundBrackets.isEnabled = enabled
            cbSquareBrackets.isEnabled = enabled
            cbCurlyBrackets.isEnabled = enabled
            cbAngleBrackets.isEnabled = enabled
            cbScopeHighlight.isEnabled = enabled
        }

        panel = FormBuilder.createFormBuilder()
            .addComponent(cbEnabled)
            .addVerticalGap(8)
            .addComponent(JLabel("彩虹括号").also {
                it.border = JBUI.Borders.emptyLeft(20)
            })
            .addComponent(cbRoundBrackets.also { it.border = JBUI.Borders.emptyLeft(40) })
            .addComponent(cbSquareBrackets.also { it.border = JBUI.Borders.emptyLeft(40) })
            .addComponent(cbCurlyBrackets.also { it.border = JBUI.Borders.emptyLeft(40) })
            .addComponent(cbAngleBrackets.also { it.border = JBUI.Borders.emptyLeft(40) })
            .addVerticalGap(8)
            .addComponent(JLabel("作用域高亮").also {
                it.border = JBUI.Borders.emptyLeft(20)
            })
            .addComponent(cbScopeHighlight.also { it.border = JBUI.Borders.emptyLeft(40) })
            .addComponentFillVertically(JPanel(), 0)
            .panel

        return panel!!
    }

    override fun isModified(): Boolean {
        val settings = ColorBracketsSettings.instance
        return cbEnabled.isSelected != settings.isEnabled ||
                cbRoundBrackets.isSelected != settings.enableRoundBrackets ||
                cbSquareBrackets.isSelected != settings.enableSquareBrackets ||
                cbCurlyBrackets.isSelected != settings.enableCurlyBrackets ||
                cbAngleBrackets.isSelected != settings.enableAngleBrackets ||
                cbScopeHighlight.isSelected != settings.enableScopeHighlight
    }

    override fun apply() {
        val settings = ColorBracketsSettings.instance
        settings.isEnabled = cbEnabled.isSelected
        settings.enableRoundBrackets = cbRoundBrackets.isSelected
        settings.enableSquareBrackets = cbSquareBrackets.isSelected
        settings.enableCurlyBrackets = cbCurlyBrackets.isSelected
        settings.enableAngleBrackets = cbAngleBrackets.isSelected
        settings.enableScopeHighlight = cbScopeHighlight.isSelected
    }

    override fun reset() {
        val settings = ColorBracketsSettings.instance
        cbEnabled.isSelected = settings.isEnabled
        cbRoundBrackets.isSelected = settings.enableRoundBrackets
        cbSquareBrackets.isSelected = settings.enableSquareBrackets
        cbCurlyBrackets.isSelected = settings.enableCurlyBrackets
        cbAngleBrackets.isSelected = settings.enableAngleBrackets
        cbScopeHighlight.isSelected = settings.enableScopeHighlight

        // Sync enabled state
        val enabled = settings.isEnabled
        cbRoundBrackets.isEnabled = enabled
        cbSquareBrackets.isEnabled = enabled
        cbCurlyBrackets.isEnabled = enabled
        cbAngleBrackets.isEnabled = enabled
        cbScopeHighlight.isEnabled = enabled
    }
}
