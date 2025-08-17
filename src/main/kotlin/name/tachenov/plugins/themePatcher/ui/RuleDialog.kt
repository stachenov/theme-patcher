package name.tachenov.plugins.themePatcher.ui

import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.components.JBTextField
import name.tachenov.plugins.themePatcher.app.IntLafValueConfig
import name.tachenov.plugins.themePatcher.app.LafPatchingService
import name.tachenov.plugins.themePatcher.app.LafValueConfig
import name.tachenov.plugins.themePatcher.app.RuleConfig
import name.tachenov.plugins.themePatcher.ui.ThemePatcherMessageBundle.message
import java.awt.Component
import javax.swing.*
import javax.swing.GroupLayout.Alignment.LEADING
import javax.swing.LayoutStyle.ComponentPlacement.RELATED

internal fun showRuleDialog(parent: Component, initialValue: RuleConfig?): RuleConfig? {
    val dialog = RuleDialog(parent, initialValue)
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
) : DialogWrapper(null, parent, false, IdeModalityType.IDE) {
    private val lookAndFeelDefaults = UIManager.getLookAndFeelDefaults()

    private val valueInput = ValueInput()

    private val keyModel = DefaultComboBoxModel(
        lookAndFeelDefaults.entries
            .asSequence()
            .filter { (key, value) -> LafPatchingService.getInstance().supportsValueType(value) }
            .map { it.key }
            .filterIsInstance<String>()
            .toList()
            .sorted()
            .toTypedArray()
    )

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
        init()
    }

    override fun createCenterPanel(): JComponent {
        if (initialValue != null) {
            keyComboBox.selectedItem = initialValue.key
            valueInput.value = initialValue.value
        }
        keyComboBox.addActionListener {
            val selectedKey = keyComboBox.selectedItem as? String? ?: return@addActionListener
            valueInput.value = LafPatchingService.getInstance().convertToConfigValue(lookAndFeelDefaults[selectedKey])
        }

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

private class ValueInput : JTabbedPane() {
    var value: LafValueConfig?
        get() = currentTab.value
        set(value) {
            if (value != null) {
                val tab = tabForType(value)
                tab.value = value
                currentTab = tab
            }
            else {
                intInputTab.value = null
                currentTab = intInputTab
            }
        }

    val acceptedValuesForm: String get() = currentTab.acceptedValuesForm

    private val intInputTab = IntInput()

    private var currentTab: TypedValueInput<*> = intInputTab
        set(value) {
            field = value
            selectedComponent = value.component
        }

    @Suppress("UNCHECKED_CAST")
    private fun tabForType(type: LafValueConfig): TypedValueInput<LafValueConfig> = when (type) {
        is IntLafValueConfig -> intInputTab
    } as TypedValueInput<LafValueConfig>

    init {
        addTab(message("configurable.type.int"), intInputTab.component)
    }
}

private sealed class TypedValueInput<T : LafValueConfig> {
    abstract val acceptedValuesForm: String
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
