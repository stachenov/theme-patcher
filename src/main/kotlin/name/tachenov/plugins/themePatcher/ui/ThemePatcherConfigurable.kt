@file:Suppress("UnstableApiUsage")

package name.tachenov.plugins.themePatcher.ui

import com.intellij.ide.ui.LafManager
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataKey
import com.intellij.openapi.actionSystem.DataSink
import com.intellij.openapi.actionSystem.UiDataProvider
import com.intellij.openapi.options.BoundSearchableConfigurable
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.InputValidator
import com.intellij.openapi.ui.Messages
import com.intellij.ui.LayeredIcon
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBList
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.toMutableProperty
import com.intellij.ui.dsl.listCellRenderer.listCellRenderer
import com.intellij.ui.table.JBTable
import name.tachenov.plugins.themePatcher.app.LafValue
import name.tachenov.plugins.themePatcher.app.RuleConfig
import name.tachenov.plugins.themePatcher.app.RulesetConfig
import name.tachenov.plugins.themePatcher.app.ThemeConfig
import name.tachenov.plugins.themePatcher.app.ThemePatcherConfigService
import name.tachenov.plugins.themePatcher.ui.ThemePatcherMessageBundle.message
import javax.swing.DefaultListModel
import javax.swing.GroupLayout
import javax.swing.GroupLayout.Alignment.LEADING
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
                    cell(RulesetEditor()).bind(
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
        }

    init {
        rulesetListToolbarDecorator.setAddAction {
            addRuleset()
        }
        rulesetListPanel = rulesetListToolbarDecorator.createPanel()
        rulesetList.addListSelectionListener {
            themeList.model = rulesetList.selectedValue?.let { themeListModels[it] }
            ruleTable.model = rulesetList.selectedValue?.let { ruleTableModels[it] }
        }

        themeToolbarDecorator.addExtraAction(AddThemeActionGroup())
        themePanel = themeToolbarDecorator.createPanel()
        themeList.cellRenderer = listCellRenderer {
            text(value.themeName)
        }

        ruleToolbarDecorator.setAddAction {
            addRule()
        }
        rulePanel = ruleToolbarDecorator.createPanel()

        val layout = GroupLayout(this)
        val vg = layout.createParallelGroup(LEADING)
        val hg = layout.createSequentialGroup()
        vg.apply {
            addComponent(rulesetListPanel)
            addComponent(themePanel)
            addComponent(rulePanel)
        }
        hg.apply {
            addComponent(rulesetListPanel)
            addPreferredGap(RELATED)
            addComponent(themePanel)
            addPreferredGap(RELATED)
            addComponent(rulePanel)
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
        val rule = showRuleDialog(this, null)
        if (rule != null) {
            ruleTableModel.add(rule)
        }
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
            val key = getValueAt(row, KEY_COLUMN) as String
            val value = getValueAt(row, VALUE_COLUMN) as LafValue
            RuleConfig(key, value)
        }

    override fun getColumnClass(columnIndex: Int): Class<*> = when (columnIndex) {
        KEY_COLUMN -> String::class.java
        VALUE_COLUMN -> LafValue::class.java
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
