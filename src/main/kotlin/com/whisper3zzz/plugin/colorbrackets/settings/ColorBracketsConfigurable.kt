package com.whisper3zzz.plugin.colorbrackets.settings

import com.intellij.openapi.options.Configurable
import com.intellij.ui.components.JBCheckBox
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JSpinner
import javax.swing.JTextField
import javax.swing.SpinnerNumberModel

class ColorBracketsConfigurable : Configurable {

    private var panel: JPanel? = null

    private val cbEnabled = JBCheckBox("启用 ColorBrackets")
    private val cbRoundBrackets = JBCheckBox("彩色圆括号 ( )")
    private val cbSquareBrackets = JBCheckBox("彩色方括号 [ ]")
    private val cbCurlyBrackets = JBCheckBox("彩色大括号 { }")
    private val cbAngleBrackets = JBCheckBox("彩色尖括号 < >")
    private val cbAngleBracketMode = JComboBox(
        arrayOf(
            ColorBracketsSettings.ANGLE_BRACKET_AUTO,
            ColorBracketsSettings.ANGLE_BRACKET_ALWAYS,
            ColorBracketsSettings.ANGLE_BRACKET_NEVER
        )
    )
    private val cbScopeHighlight = JBCheckBox("启用代码块竖线高亮")
    private val cbBoldBrackets = JBCheckBox("括号加粗")
    private val cbColorPalette = JComboBox(
        arrayOf(
            ColorBracketsSettings.COLOR_PALETTE_DEFAULT,
            ColorBracketsSettings.COLOR_PALETTE_VIVID,
            ColorBracketsSettings.COLOR_PALETTE_SOFT
        )
    )
    private val spScopeLineWidth = JSpinner(
        SpinnerNumberModel(
            ColorBracketsSettings.DEFAULT_SCOPE_LINE_WIDTH,
            ColorBracketsSettings.MIN_SCOPE_LINE_WIDTH,
            ColorBracketsSettings.MAX_SCOPE_LINE_WIDTH,
            1
        )
    )
    private val spScopeLineOpacity = JSpinner(
        SpinnerNumberModel(
            ColorBracketsSettings.DEFAULT_SCOPE_LINE_OPACITY,
            ColorBracketsSettings.MIN_SCOPE_LINE_OPACITY,
            ColorBracketsSettings.MAX_SCOPE_LINE_OPACITY,
            5
        )
    )
    private val cbLargeFileLimit = JBCheckBox("启用大文件保护")
    private val spMaxFileSizeKb = JSpinner(
        SpinnerNumberModel(
            ColorBracketsSettings.DEFAULT_MAX_FILE_SIZE_KB,
            ColorBracketsSettings.MIN_FILE_SIZE_KB,
            ColorBracketsSettings.MAX_FILE_SIZE_KB,
            64
        )
    )
    private val cbLanguageFilterMode = JComboBox(
        arrayOf(
            ColorBracketsSettings.LANGUAGE_FILTER_ALL,
            ColorBracketsSettings.LANGUAGE_FILTER_ONLY,
            ColorBracketsSettings.LANGUAGE_FILTER_EXCEPT
        )
    )
    private val tfLanguageFilterList = JTextField()

    override fun getDisplayName(): String = "ColorBrackets"

    override fun createComponent(): JComponent {
        // Toggle bracket options when master switch changes
        cbEnabled.addChangeListener {
            syncControlEnabledState()
        }
        cbLargeFileLimit.addChangeListener {
            syncControlEnabledState()
        }
        cbScopeHighlight.addChangeListener {
            syncControlEnabledState()
        }
        cbAngleBrackets.addChangeListener {
            syncControlEnabledState()
        }
        cbLanguageFilterMode.addActionListener {
            syncControlEnabledState()
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
            .addLabeledComponent("尖括号策略", cbAngleBracketMode)
            .addLabeledComponent("颜色方案", cbColorPalette)
            .addComponent(cbBoldBrackets.also { it.border = JBUI.Borders.emptyLeft(40) })
            .addVerticalGap(8)
            .addComponent(JLabel("作用域高亮").also {
                it.border = JBUI.Borders.emptyLeft(20)
            })
            .addComponent(cbScopeHighlight.also { it.border = JBUI.Borders.emptyLeft(40) })
            .addLabeledComponent("竖线宽度", spScopeLineWidth)
            .addLabeledComponent("竖线透明度 (%)", spScopeLineOpacity)
            .addVerticalGap(8)
            .addComponent(JLabel("性能").also {
                it.border = JBUI.Borders.emptyLeft(20)
            })
            .addComponent(cbLargeFileLimit.also { it.border = JBUI.Borders.emptyLeft(40) })
            .addLabeledComponent("最大文件大小 (KB)", spMaxFileSizeKb)
            .addVerticalGap(8)
            .addComponent(JLabel("语言过滤").also {
                it.border = JBUI.Borders.emptyLeft(20)
            })
            .addLabeledComponent("过滤模式", cbLanguageFilterMode)
            .addLabeledComponent("语言 ID 列表", tfLanguageFilterList)
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
                selectedAngleBracketMode() != settings.angleBracketMode ||
                cbScopeHighlight.isSelected != settings.enableScopeHighlight ||
                cbLargeFileLimit.isSelected != settings.enableLargeFileLimit ||
                maxFileSizeKbValue() != settings.maxFileSizeKb ||
                selectedColorPalette() != settings.colorPalette ||
                cbBoldBrackets.isSelected != settings.boldBrackets ||
                scopeLineWidthValue() != settings.scopeLineWidth ||
                scopeLineOpacityValue() != settings.scopeLineOpacity ||
                selectedLanguageFilterMode() != settings.languageFilterMode ||
                tfLanguageFilterList.text != settings.languageFilterList
    }

    override fun apply() {
        val changed = isModified()
        val settings = ColorBracketsSettings.instance
        settings.isEnabled = cbEnabled.isSelected
        settings.enableRoundBrackets = cbRoundBrackets.isSelected
        settings.enableSquareBrackets = cbSquareBrackets.isSelected
        settings.enableCurlyBrackets = cbCurlyBrackets.isSelected
        settings.enableAngleBrackets = cbAngleBrackets.isSelected
        settings.angleBracketMode = selectedAngleBracketMode()
        settings.enableScopeHighlight = cbScopeHighlight.isSelected
        settings.enableLargeFileLimit = cbLargeFileLimit.isSelected
        settings.maxFileSizeKb = maxFileSizeKbValue()
        settings.colorPalette = selectedColorPalette()
        settings.boldBrackets = cbBoldBrackets.isSelected
        settings.scopeLineWidth = scopeLineWidthValue()
        settings.scopeLineOpacity = scopeLineOpacityValue()
        settings.languageFilterMode = selectedLanguageFilterMode()
        settings.languageFilterList = tfLanguageFilterList.text
        if (changed) {
            ColorBracketsSettingsTracker.incModificationCount()
        }
    }

    override fun reset() {
        val settings = ColorBracketsSettings.instance
        cbEnabled.isSelected = settings.isEnabled
        cbRoundBrackets.isSelected = settings.enableRoundBrackets
        cbSquareBrackets.isSelected = settings.enableSquareBrackets
        cbCurlyBrackets.isSelected = settings.enableCurlyBrackets
        cbAngleBrackets.isSelected = settings.enableAngleBrackets
        cbAngleBracketMode.selectedItem = settings.angleBracketMode
        cbScopeHighlight.isSelected = settings.enableScopeHighlight
        cbLargeFileLimit.isSelected = settings.enableLargeFileLimit
        spMaxFileSizeKb.value = settings.maxFileSizeKb
        cbColorPalette.selectedItem = settings.colorPalette
        cbBoldBrackets.isSelected = settings.boldBrackets
        spScopeLineWidth.value = settings.scopeLineWidth
        spScopeLineOpacity.value = settings.scopeLineOpacity
        cbLanguageFilterMode.selectedItem = settings.languageFilterMode
        tfLanguageFilterList.text = settings.languageFilterList

        syncControlEnabledState()
    }

    private fun syncControlEnabledState() {
        val enabled = cbEnabled.isSelected
        cbRoundBrackets.isEnabled = enabled
        cbSquareBrackets.isEnabled = enabled
        cbCurlyBrackets.isEnabled = enabled
        cbAngleBrackets.isEnabled = enabled
        cbAngleBracketMode.isEnabled = enabled && cbAngleBrackets.isSelected
        cbScopeHighlight.isEnabled = enabled
        cbColorPalette.isEnabled = enabled
        cbBoldBrackets.isEnabled = enabled
        spScopeLineWidth.isEnabled = enabled && cbScopeHighlight.isSelected
        spScopeLineOpacity.isEnabled = enabled && cbScopeHighlight.isSelected
        cbLargeFileLimit.isEnabled = enabled
        spMaxFileSizeKb.isEnabled = enabled && cbLargeFileLimit.isSelected
        cbLanguageFilterMode.isEnabled = enabled
        tfLanguageFilterList.isEnabled = enabled && selectedLanguageFilterMode() != ColorBracketsSettings.LANGUAGE_FILTER_ALL
    }

    private fun maxFileSizeKbValue(): Int {
        return (spMaxFileSizeKb.value as Number).toInt()
    }

    private fun scopeLineWidthValue(): Int {
        return (spScopeLineWidth.value as Number).toInt()
    }

    private fun scopeLineOpacityValue(): Int {
        return (spScopeLineOpacity.value as Number).toInt()
    }

    private fun selectedColorPalette(): String {
        return cbColorPalette.selectedItem as? String ?: ColorBracketsSettings.COLOR_PALETTE_DEFAULT
    }

    private fun selectedAngleBracketMode(): String {
        return cbAngleBracketMode.selectedItem as? String ?: ColorBracketsSettings.ANGLE_BRACKET_AUTO
    }

    private fun selectedLanguageFilterMode(): String {
        return cbLanguageFilterMode.selectedItem as? String ?: ColorBracketsSettings.LANGUAGE_FILTER_ALL
    }
}
