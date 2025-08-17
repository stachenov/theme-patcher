package name.tachenov.plugins.themePatcher.app

import com.intellij.ide.ui.LafManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.util.ui.JBUI
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.SwingUtilities
import javax.swing.UIManager

@Service(Service.Level.APP)
internal class LafPatchingService {
    companion object {
        fun getInstance(): LafPatchingService = service()
    }

    private val firstRun = AtomicBoolean(true)

    /**
     * Patches the current theme once any project is opened.
     *
     * The IJ platform doesn't provide a good "run on startup" extension,
     * and for a good reason: such uncontrolled extensions can slow down startup unnecessarily.
     *
     * However, there's a good alternative: "run on project open" extensions.
     * We can sort of emulate "run on startup" by running it only once.
     * The difference is that when the IDE is loaded without opening any project (just the Welcome Screen),
     * then the theme won't be patched until the project is opened.
     * But that's a downside we're willing to tolerate, as users don't generally
     * stare at the Welcome Screen long enough for an unpatched theme can become an issue.
     */
    fun patchThemeOnFirstProjectOpen() {
        if (firstRun.compareAndSet(true, false)) {
            patchCurrentTheme()
        }
    }

    fun patchCurrentTheme() {
        // We can't just patch the theme here, as it needs to be actually applied.
        // So we trigger a full theme refresh, which will act like the user changed the theme,
        // and eventually call LafPatchingListener and update the UI.
        SwingUtilities.invokeLater { // must do it on the EDT
            LafManager.getInstance().updateUI()
        }
    }

    fun patchThemeOnLafChange() {
        val theme = LafManager.getInstance().currentUIThemeLookAndFeel ?: return
        LOG.debug("Patching the theme id=${theme.id}, name=${theme.name}")
        val defaults = UIManager.getLookAndFeelDefaults()
        for (ruleset in ThemePatcherConfigService.getInstance().rulesets) {
            if (theme.id in ruleset.themes.map { it.themeId }) {
                LOG.debug("Applying the ruleset ${ruleset.rulesetName}")
                for (rule in ruleset.rules) {
                    defaults[rule.key] = rule.value.toUiDefaultsValue()
                }
            }
        }
    }

    fun supportsValueType(value: Any?): Boolean = convertToConfigValue(value) != null

    fun convertToConfigValue(value: Any?): LafValueConfig? =
        when (value) {
            null -> null
            is Int -> IntLafValueConfig(JBUI.unscale(value)) // unscale() is not very reliable, but there's no other easy way
            else -> null
        }
}

private fun LafValueConfig.toUiDefaultsValue(): Any = when (this) {
    is IntLafValueConfig -> JBUI.scale(intValue)
}

private val LOG = logger<LafPatchingService>()
