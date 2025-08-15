package name.tachenov.plugins.themePatcher.app

import com.intellij.openapi.components.SerializablePersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.util.ui.JBUI
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
    val value: LafValue,
)

@Serializable
internal sealed class LafValue {
    abstract fun toUiDefaultsValue(): Any
    abstract fun toPresentableString(): String
}

@Serializable
internal data class IntLafValue(val intValue: Int): LafValue() {
    override fun toUiDefaultsValue(): Any = JBUI.scale(intValue)
    override fun toPresentableString(): String = intValue.toString()
}

internal fun parseValue(string: String): LafValue? {
    return IntLafValue(string.toInt())
}
