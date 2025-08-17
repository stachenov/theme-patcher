package name.tachenov.plugins.themePatcher.app

import com.intellij.openapi.components.SerializablePersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import kotlinx.serialization.Serializable

@State(name = "themePatcher", storages = [Storage("themePatcher.xml")])
internal class ThemePatcherConfigService : SerializablePersistentStateComponent<Config>(Config()) {
    companion object {
        fun getInstance(): ThemePatcherConfigService = service()
    }

    var rulesets: List<RulesetConfig>
        get() = state.rulesets
        set(value) {
            updateState {
                it.copy(rulesets = value)
            }
            LafPatchingService.getInstance().patchCurrentTheme()
        }
}

@Serializable
internal data class Config(
    val rulesets: List<RulesetConfig> = emptyList(),
)

@Serializable
internal data class RulesetConfig(
    val rulesetName: String,
    val themes: List<ThemeConfig>,
    val rules: List<RuleConfig>,
)

@Serializable
internal data class ThemeConfig(
    val themeName: String,
    val themeId: String,
)

@Serializable
internal data class RuleConfig(
    val key: String,
    val value: LafValueConfig,
)

@Serializable
internal sealed class LafValueConfig

@Serializable
internal data class IntLafValueConfig(val intValue: Int): LafValueConfig() {
    override fun toString(): String = intValue.toString()
}
