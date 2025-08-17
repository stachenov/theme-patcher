@file:Suppress("UnstableApiUsage")

package name.tachenov.plugins.themePatcher.ui

import com.intellij.ide.ui.LafManager
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.options.BoundSearchableConfigurable
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.InputValidator
import com.intellij.openapi.ui.Messages
import com.intellij.ui.LayeredIcon
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBList
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.toMutableProperty
import com.intellij.ui.dsl.listCellRenderer.listCellRenderer
import com.intellij.ui.table.JBTable
import name.tachenov.plugins.themePatcher.app.*
import name.tachenov.plugins.themePatcher.ui.ThemePatcherMessageBundle.message
import javax.swing.DefaultListModel
import javax.swing.GroupLayout
import javax.swing.GroupLayout.Alignment.LEADING
import javax.swing.GroupLayout.DEFAULT_SIZE
import javax.swing.GroupLayout.PREFERRED_SIZE
import javax.swing.JPanel
import javax.swing.LayoutStyle.ComponentPlacement.RELATED
import javax.swing.ListModel
import javax.swing.table.DefaultTableModel

internal class ThemePatcherConfigurable : BoundSearchableConfigurable("Theme Patcher", "name.tachenov.plugins.themePatcher") {
    override fun createPanel(): DialogPanel =
        panel {
            group(message("configurable.rules")) {
                row {
                    val configService = ThemePatcherConfigService.getInstance()
                    cell(RulesetEditor()).align(AlignX.FILL).bind(
                        componentGet = { component -> component.rulesets },
                        componentSet = { component, value -> component.rulesets = value },
                        prop = configService::rulesets.toMutableProperty(),
                    )
                }
            }
        }
}

private class RulesetEditor : JPanel(), UiDataProvider {

    private val rulesetListModel = DefaultListModel<String>()
    private val rulesetList = JBList(rulesetListModel)
    private val rulesetListToolbarDecorator = ToolbarDecorator.createDecorator(rulesetList)
    private val rulesetListPanel: JPanel

    private val themeListModels = hashMapOf<String, DefaultListModel<ThemeConfig>>()
    private val themeList = JBList<ThemeConfig>()
    private val themeToolbarDecorator = ToolbarDecorator.createDecorator(themeList)
    private val themePanel: JPanel

    private val ruleTableModels = hashMapOf<String, RuleTableModel>()
    private val ruleTable = JBTable()
    private val ruleToolbarDecorator = ToolbarDecorator.createDecorator(ruleTable)
    private val rulePanel: JPanel

    var rulesets: List<RulesetConfig>
        get() = (0 until rulesetListModel.size).map {
            val name = rulesetListModel.get(it)
            RulesetConfig(
                rulesetName = name,
                themes = themeListModels.getValue(name).values,
                rules = ruleTableModels.getValue(name).values,
            )
        }
        set(value) {
            val selectedRuleset = rulesetList.selectedValue
            rulesetListModel.clear()
            themeListModels.clear()
            rulesetListModel.addAll(value.map { it.rulesetName })
            value.forEach { ruleset ->
                themeListModels[ruleset.rulesetName] = DefaultListModel<ThemeConfig>().also { themeListModel ->
                    themeListModel.addAll(ruleset.themes)
                }
                ruleTableModels[ruleset.rulesetName] = RuleTableModel().also { ruleTableModel ->
                    ruleTableModel.addAll(ruleset.rules)
                }
            }
            if (value.isNotEmpty()) {
                rulesetList.selectedIndex = if (selectedRuleset == null) {
                    0
                }
                else {
                    val rulesetIndex = value.indexOfFirst { it.rulesetName == selectedRuleset }
                    if (rulesetIndex >= 0) {
                        rulesetIndex
                    }
                    else {
                        0
                    }
                }
            }
        }

    init {
        themeList.cellRenderer = listCellRenderer {
            text(value.themeName)
        }

        rulesetListToolbarDecorator.setAddAction {
            addRuleset()
        }
        themeToolbarDecorator.addExtraAction(AddThemeActionGroup())
        ruleToolbarDecorator.setAddAction {
            addRule()
        }
        ruleToolbarDecorator.setEditAction {
            editRule()
        }
        ruleToolbarDecorator.setRemoveAction {
            removeRule()
        }

        rulesetListPanel = rulesetListToolbarDecorator.createPanel()
        themePanel = themeToolbarDecorator.createPanel()
        rulePanel = ruleToolbarDecorator.createPanel()

        rulesetList.addListSelectionListener {
            themeList.model = rulesetList.selectedValue?.let { themeListModels[it] } ?: DefaultListModel()
            ruleTable.model = rulesetList.selectedValue?.let { ruleTableModels[it] } ?: DefaultTableModel()
        }

        val layout = GroupLayout(this)
        val vg = layout.createParallelGroup(LEADING)
        val hg = layout.createSequentialGroup()
        vg.apply {
            addComponent(rulesetListPanel)
            addComponent(themePanel)
            addComponent(rulePanel)
        }
        hg.apply {
            addComponent(rulesetListPanel, DEFAULT_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
            addPreferredGap(RELATED)
            addComponent(themePanel, DEFAULT_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
            addPreferredGap(RELATED)
            addComponent(rulePanel, DEFAULT_SIZE, DEFAULT_SIZE, INFINITE_SIZE)
        }
        layout.setHorizontalGroup(hg)
        layout.setVerticalGroup(vg)
        this.layout = layout
    }

    private fun addRuleset() {
        val name = Messages.showInputDialog(
            this,
            message("configurable.add.ruleset.message"),
            message("configurable.add.ruleset.title"),
            null,
            "New Ruleset",
            RulesetValidator(rulesets.map { it.rulesetName }),
        )
        if (name != null) {
            val rulesetName = name.trim()
            rulesetListModel.add(rulesetListModel.size(), rulesetName)
            themeListModels[rulesetName] = DefaultListModel<ThemeConfig>()
            ruleTableModels[rulesetName] = RuleTableModel()
        }
    }

    private fun addRule() {
        val ruleTableModel = rulesetList.selectedValue?.let { ruleset -> ruleTableModels[ruleset] } ?: return
        val rule = showRuleDialog(this, null, availableKeys(ruleTableModel))
        if (rule != null) {
            ruleTableModel.add(rule)
        }
    }

    private fun editRule() {
        val selectedRow = ruleTable.selectedRow.takeIf { it >= 0 }?.let { ruleTable.convertRowIndexToModel(it) } ?: return
        val ruleTableModel = rulesetList.selectedValue?.let { ruleset -> ruleTableModels[ruleset] } ?: return
        val initialValue = ruleTableModel.getValue(selectedRow)
        val rule = showRuleDialog(this, initialValue, availableKeys(ruleTableModel, includeKey = initialValue.key))
        if (rule != null) {
            ruleTableModel.setValue(selectedRow, rule)
        }
    }

    private fun removeRule() {
        val selectedRow = ruleTable.selectedRow.takeIf { it >= 0 }?.let { ruleTable.convertRowIndexToModel(it) } ?: return
        val ruleTableModel = rulesetList.selectedValue?.let { ruleset -> ruleTableModels[ruleset] } ?: return
        ruleTableModel.removeRow(selectedRow)
    }

    private fun availableKeys(ruleTableModel: RuleTableModel, includeKey: String? = null): List<String> {
        val allAvailable = lookAndFeelDefaults.entries
            .asSequence()
            .filter { LafPatchingService.getInstance().supportsValueType(it.value) }
            .map { it.key }
            .toSet()
        val alreadyUsed = ruleTableModel.values.map { it.key }.toSet()
        val notYetUsed = allAvailable - alreadyUsed
        val result = if (includeKey == null) notYetUsed else notYetUsed + includeKey
        return result.sorted()
    }

    override fun uiDataSnapshot(sink: DataSink) {
        sink[THEME_LIST_MODEL_KEY] = rulesetList.selectedValue?.let { ruleset -> themeListModels[ruleset] }
    }
}

private class RuleTableModel : DefaultTableModel() {
    companion object {
        const val KEY_COLUMN = 0
        const val VALUE_COLUMN = 1
    }

    init {
        addColumn("key")
        addColumn("value")
    }

    val values: List<RuleConfig>
        get() = (0 until rowCount).map { row ->
            getValue(row)
        }

    fun getValue(row: Int): RuleConfig {
        val key = getValueAt(row, KEY_COLUMN) as String
        val value = getValueAt(row, VALUE_COLUMN) as LafValueConfig
        return RuleConfig(key, value)
    }

    fun setValue(row: Int, rule: RuleConfig) {
        setValueAt(rule.key, row, KEY_COLUMN)
        setValueAt(rule.value, row, VALUE_COLUMN)
    }

    override fun getColumnClass(columnIndex: Int): Class<*> = when (columnIndex) {
        KEY_COLUMN -> String::class.java
        VALUE_COLUMN -> LafValueConfig::class.java
        else -> throw IllegalArgumentException("No colum with the index $columnIndex")
    }

    fun addAll(values: List<RuleConfig>) {
        for (value in values) {
            add(value)
        }
    }

    fun add(value: RuleConfig) {
        addRow(arrayOf(value.key, value.value))
    }
}

private class AddThemeActionGroup : ActionGroup() {
    init {
        templatePresentation.icon = LayeredIcon.ADD_WITH_DROPDOWN
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        e.presentation.isPopupGroup = true
        e.presentation.isEnabled = themesToAdd(e).isNotEmpty()
    }

    override fun getChildren(e: AnActionEvent?): Array<AnAction> {
        if (e == null) return emptyArray()
        return themesToAdd(e).map { AddThemeAction(it) }.toTypedArray()
    }

    private fun themesToAdd(e: AnActionEvent): List<ThemeConfig> {
        val themeListModel = e.getData(THEME_LIST_MODEL_KEY) ?: return emptyList()
        val alreadyAddedThemes = (0 until themeListModel.size()).map { themeListModel.get(it) }.toSet()
        val canAddThemes = LafManager.getInstance().installedThemes.mapNotNull { themeLaf ->
            val theme = ThemeConfig(themeLaf.name, themeLaf.id)
            if (theme in alreadyAddedThemes) null else theme
        }.toList()
        return canAddThemes
    }
}

private class AddThemeAction(
    private val theme: ThemeConfig,
): DumbAwareAction() {
    init {
        templatePresentation.text = theme.themeName
    }

    override fun actionPerformed(e: AnActionEvent) {
        val themeListModel = e.getData(THEME_LIST_MODEL_KEY) ?: return
        themeListModel.add(themeListModel.size(), theme)
    }
}

private class RulesetValidator(private val existingRulesetNames: List<String>) : InputValidator {
    override fun checkInput(inputString: String?): Boolean = inputString?.trim()?.let { trimmed ->
        trimmed.isNotEmpty() && trimmed !in existingRulesetNames
    } == true

    override fun canClose(inputString: String?): Boolean = checkInput(inputString)
}

private val <T> ListModel<T>.values: List<T>
    get() = (0 until size).map { getElementAt(it) }

private val THEME_LIST_MODEL_KEY = DataKey.create<DefaultListModel<ThemeConfig>>("name.tachenov.plugins.themePatcher.THEME_LIST_MODEL")
private const val INFINITE_SIZE = Short.MAX_VALUE.toInt()
