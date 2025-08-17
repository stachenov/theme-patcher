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
        valueInput.value = LafPatchingService.getInstance().convertToConfigValue(lookAndFeelDefaults[selectedKey])
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

    private var currentInput: TypedValueInput<*> = intInput
        set(value) {
            field = value
            inputLayout.show(this, value.javaClass.name)
        }

    @Suppress("UNCHECKED_CAST")
    private fun tabForType(type: LafValueConfig): TypedValueInput<LafValueConfig> = when (type) {
        is IntLafValueConfig -> intInput
        is ColorLafValueConfig -> colorInput
    } as TypedValueInput<LafValueConfig>

    init {
        this.layout = inputLayout
        addInput(intInput)
        addInput(colorInput)
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
