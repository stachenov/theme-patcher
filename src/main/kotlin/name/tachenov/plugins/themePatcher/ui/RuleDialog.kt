/**
Copyright 2025 Sergei Tachenov

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */
package name.tachenov.plugins.themePatcher.ui

import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.ColorPanel
import com.intellij.ui.components.JBTextField
import name.tachenov.plugins.themePatcher.app.*
import name.tachenov.plugins.themePatcher.ui.ThemePatcherMessageBundle.message
import java.awt.CardLayout
import java.awt.Component
import javax.swing.DefaultComboBoxModel
import javax.swing.GroupLayout
import javax.swing.GroupLayout.Alignment.LEADING
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.LayoutStyle.ComponentPlacement.RELATED

internal fun showRuleDialog(parent: Component, initialValue: RuleConfig?, availableKeys: List<String>): RuleConfig? {
    if (availableKeys.isEmpty()) return null // very unlikely, unless the user has already customized every key in the world
    val dialog = RuleDialog(parent, initialValue, availableKeys)
    return if (dialog.showAndGet()) {
        dialog.value
    }
    else {
        null
    }
}

private class RuleDialog(
    parent: Component,
    private val initialValue: RuleConfig?,
    availableKeys: List<String>,
) : DialogWrapper(null, parent, false, IdeModalityType.IDE) {
    private val valueInput = ValueInput()

    private val keyModel = DefaultComboBoxModel(availableKeys.toTypedArray())

    private val keyComboBox = ComboBox(keyModel).apply {
        isSwingPopup = false // to enable speed search, as there is a lot of keys
    }

    val value: RuleConfig?
        get() {
            val key = keyComboBox.selectedItem as? String?
            val value = valueInput.value
            return if (key != null && value != null) {
                RuleConfig(key, value)
            }
            else {
                null
            }
        }

    init {
        title = message("configurable.rule.title")
        init()
    }

    override fun getPreferredFocusedComponent(): JComponent = keyComboBox

    override fun createCenterPanel(): JComponent {
        if (initialValue != null) {
            keyComboBox.selectedItem = initialValue.key
            valueInput.value = initialValue.value
        }
        keyComboBox.addActionListener {
            showSelectedKeyInput()
        }
        showSelectedKeyInput()

        val result = JPanel()
        val layout = GroupLayout(result)
        val hg = layout.createParallelGroup(LEADING)
        val vg = layout.createSequentialGroup()

        hg.apply {
            addComponent(keyComboBox)
            addComponent(valueInput)
        }
        vg.apply {
            addComponent(keyComboBox)
            addPreferredGap(RELATED)
            addComponent(valueInput)
        }

        layout.setHorizontalGroup(hg)
        layout.setVerticalGroup(vg)
        result.layout = layout
        return result
    }

    private fun showSelectedKeyInput() {
        val selectedKey = keyComboBox.selectedItem as? String? ?: return
        valueInput.value = LafPatchingService.getInstance().convertToConfigValue(selectedKey, lookAndFeelDefaults[selectedKey])
    }

    override fun doValidate(): ValidationInfo? = when {
        keyComboBox.selectedItem == null -> { // not normally possible, but just in case
            ValidationInfo(message("configurable.rule.validation.no.key"), keyComboBox)
        }
        valueInput.value == null -> {
            ValidationInfo(message("configurable.rule.validation.invalid.value", valueInput.acceptedValuesForm), valueInput)
        }
        else -> null
    }
}

private class ValueInput : JPanel() {
    var value: LafValueConfig?
        get() = currentInput.value
        set(value) {
            if (value != null) {
                val tab = tabForType(value)
                tab.value = value
                currentInput = tab
            }
            else {
                intInput.value = null
                currentInput = intInput
            }
        }

    val acceptedValuesForm: String get() = currentInput.acceptedValuesForm ?: "<ERROR>" // some editors simply don't produce invalid values

    private val inputLayout = CardLayout()
    private val intInput = IntInput()
    private val colorInput = ColorInput()
    private val dimensionInput = DimensionInput()
    private val insetsInput = InsetsInput()
    private val fontSizeInput = FontSizeInput()

    private var currentInput: TypedValueInput<*> = intInput
        set(value) {
            field = value
            inputLayout.show(this, value.javaClass.name)
        }

    @Suppress("UNCHECKED_CAST")
    private fun tabForType(type: LafValueConfig): TypedValueInput<LafValueConfig> = when (type) {
        is IntLafValueConfig -> intInput
        is ColorLafValueConfig -> colorInput
        is DimensionLafValueConfig -> dimensionInput
        is InsetsLafValueConfig -> insetsInput
        is FontSizeLafValueConfig -> fontSizeInput
    } as TypedValueInput<LafValueConfig>

    init {
        this.layout = inputLayout
        addInput(intInput)
        addInput(colorInput)
        addInput(dimensionInput)
        addInput(insetsInput)
        addInput(fontSizeInput)
    }

    private fun addInput(input: TypedValueInput<*>) {
        add(input.component, input.javaClass.name)
    }
}

private sealed class TypedValueInput<T : LafValueConfig> {
    abstract val acceptedValuesForm: String?
    abstract var value: T?
    abstract val component: JComponent
}

private class IntInput : TypedValueInput<IntLafValueConfig>() {
    override val acceptedValuesForm: String
        get() = message("configurable.type.int.accepted")

    override var value: IntLafValueConfig?
        get() = component.text.toIntOrNull()?.let { IntLafValueConfig(it) }
        set(value) {
            component.text = value?.toString() ?: ""
        }

    override val component: JBTextField = JBTextField()
}

private class ColorInput : TypedValueInput<ColorLafValueConfig>() {
    override val acceptedValuesForm: String? = null

    override var value: ColorLafValueConfig?
        get() = component.selectedColor?.let { ColorLafValueConfig(it) }
        set(value) {
            component.selectedColor = value?.toColor()
        }

    override val component: ColorPanel = ColorPanel()
}

private class DimensionInput : TypedValueInput<DimensionLafValueConfig>() {
    override val acceptedValuesForm: String
        get() = message("configurable.type.dimension.accepted")

    override var value: DimensionLafValueConfig?
        get() = parseDimension(component.text)
        set(value) {
            component.text = if (value == null) "0,0" else "${value.width},${value.height}"
        }

    override val component: JBTextField = JBTextField()
}

private fun parseDimension(string: String): DimensionLafValueConfig? {
    val values = string.split(',').mapNotNull { it.toIntOrNull() }
    if (values.size != 2) return null
    return DimensionLafValueConfig(values[0], values[1])
}

private class InsetsInput : TypedValueInput<InsetsLafValueConfig>() {
    override val acceptedValuesForm: String
        get() = message("configurable.type.insets.accepted")

    override var value: InsetsLafValueConfig?
        get() = parseInsets(component.text)
        set(value) {
            component.text = if (value == null) "0,0" else "${value.top},${value.left},${value.bottom},${value.right}"
        }

    override val component: JBTextField = JBTextField()
}

private fun parseInsets(string: String): InsetsLafValueConfig? {
    val values = string.split(',').mapNotNull { it.toIntOrNull() }
    if (values.size != 4) return null
    return InsetsLafValueConfig(values[0], values[1], values[2], values[3])
}

private class FontSizeInput : TypedValueInput<FontSizeLafValueConfig>() {
    override val acceptedValuesForm: String
        get() = message("configurable.type.font.size.accepted")

    override var value: FontSizeLafValueConfig?
        get() = component.text.toIntOrNull()?.takeIf { it in 6..36 }?.let { FontSizeLafValueConfig(it) }
        set(value) {
            component.text = value?.toString() ?: ""
        }

    override val component: JBTextField = JBTextField()
}
