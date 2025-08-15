package name.tachenov.plugins.themePatcher.app

import com.intellij.ide.ui.LafManager
import com.intellij.ide.ui.LafManagerListener
import javax.swing.UIManager

internal class LafPatchingListener : LafManagerListener {
    override fun lookAndFeelChanged(manager: LafManager) {
        println("Changed to ${manager.currentUIThemeLookAndFeel.id}")
        val defaults = UIManager.getLookAndFeelDefaults()
        for (ruleset in ThemePatcherConfigService.getInstance().rulesets) {
            for (rule in ruleset.rules) {
                defaults[rule.key] = rule.value.toUiDefaultsValue()
            }
        }
    }
}
