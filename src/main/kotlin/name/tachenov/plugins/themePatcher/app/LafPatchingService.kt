package name.tachenov.plugins.themePatcher.app

import com.intellij.ide.ui.LafManager
import com.intellij.ide.ui.laf.UIThemeLookAndFeelInfo
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.util.ui.JBUI
import java.awt.Color
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.SwingUtilities
import javax.swing.plaf.ColorUIResource

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

    private var lastPatchedThemeId: String? = null
    private val lastPatchedThemeOriginalValues = hashMapOf<String, Any?>()

    fun patchThemeOnLafChange() {
        val theme = LafManager.getInstance().currentUIThemeLookAndFeel ?: return
        LOG.debug("Patching the theme id=${theme.id}, name=${theme.name}")
        if (lastPatchedThemeId == theme.id) {
            // Some rules might have been removed,
            // so the values they patched might be stuck in the patched state.
            // Restore everything we've ever patched before patching again.
            restoreOriginalValues(lookAndFeelDefaults)
        }
        else {
            // The theme has been changed, and therefore the original values are no longer relevant.
            clearOriginalValues()
        }
        patchThemeValues(theme, lookAndFeelDefaults)
        lastPatchedThemeId = theme.id
    }

    private fun restoreOriginalValues(defaults: LookAndFeelDefaults) {
        defaults.putAll(lastPatchedThemeOriginalValues)
    }

    private fun clearOriginalValues() {
        lastPatchedThemeOriginalValues.clear()
    }

    private fun patchThemeValues(theme: UIThemeLookAndFeelInfo, defaults: LookAndFeelDefaults) {
        for (ruleset in ThemePatcherConfigService.getInstance().rulesets) {
            if (theme.id in ruleset.themes.map { it.themeId }) {
                LOG.debug("Applying the ruleset ${ruleset.rulesetName}")
                for (rule in ruleset.rules) {
                    if (lastPatchedThemeOriginalValues[rule.key] == null) { // Is it the first time we patch this value?
                        lastPatchedThemeOriginalValues[rule.key] = defaults[rule.key]
                    }
                    defaults[rule.key] = rule.value.toUiDefaultsValue()
                }
            }
        }
    }


    /**
     * Checks whether the give value from the LaF defaults is supported by the plugin.
     *
     * Supported types:
     *
     * - scaled `Int` values (e.g., `Tree.rowHeight`)
     *
     * Planned support, in the order of importance:
     *
     * - colors
     * - sizes (`Dimension`, `Insets`)
     * - font sizes (only for standard fonts)
     * - `Boolean` (surprisingly many different values)
     * - `Double` (mostly stuff like transparency and saturation)
     * - `Long` (only used for time factors, like `ComboBox.timeFactor`,
     * determining how fast the user must type for the Swing speed search to work)
     * - borders (reasonably limited subset)
     * - `Char` (only used for `PasswordField.echoChar`)
     *
     * Not planned:
     * - unscaled `Int` values (almost not used at all,
     * not easy to distinguish between scaled and unscaled values,
     * currently all values are scaled,
     * so setting a value for an unscaled value could lead to very strange effects,
     * e.g., `EditorPane.caretBlinkRate` will be scaled)
     * - fonts, except customizing sizes (not easy to implement and can mess up the UI)
     * - icons (doesn't seem important, and is somewhat hard to implement)
     * - `Float` (only used internally by the IDE to store scaling factors, messing with them would be dangerous)
     */
    fun supportsValueType(value: Any?): Boolean = convertToConfigValue(value) != null

    fun convertToConfigValue(value: Any?): LafValueConfig? =
        when (value) {
            null -> null
            is Int -> IntLafValueConfig(JBUI.unscale(value)) // unscale() is not very reliable, but there's no other easy way
            is Color -> ColorLafValueConfig(value)
            else -> null
        }
}

private fun LafValueConfig.toUiDefaultsValue(): Any = when (this) {
    is IntLafValueConfig -> JBUI.scale(intValue)
    is ColorLafValueConfig -> ColorUIResource(red, green, blue)
}

private val LOG = logger<LafPatchingService>()
