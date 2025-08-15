package name.tachenov.plugins.themePatcher.ui

import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBTextField
import name.tachenov.plugins.themePatcher.app.RuleConfig
import name.tachenov.plugins.themePatcher.app.parseValue
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

    private val keyModel = DefaultComboBoxModel(UIManager.getLookAndFeelDefaults().keys.filterIsInstance<String>().sorted().toTypedArray())
    private val keyComboBox = ComboBox(keyModel)
    private val valueInput = JBTextField()

    val value: RuleConfig?
        get() {
            val key = keyComboBox.selectedItem as? String?
            val value = parseValue(valueInput.text)
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
        val result = JPanel()
        val layout = GroupLayout(result)
        val hg = layout.createParallelGroup(LEADING)
        val vg = layout.createSequentialGroup()

        if (initialValue != null) {
            keyComboBox.selectedItem = initialValue.key
        }

        if (initialValue != null) {
            valueInput.text = initialValue.value.toPresentableString()
        }

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
}
