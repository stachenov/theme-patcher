package name.tachenov.plugins.themePatcher.app

import com.intellij.openapi.components.SerializablePersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import kotlinx.serialization.Serializable
import java.awt.Color
import java.awt.Dimension
import javax.swing.plaf.ColorUIResource

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

@Serializable
internal data class ColorLafValueConfig(
    val red: Int,
    val green: Int,
    val blue: Int,
) : LafValueConfig() {
    constructor(color: Color) : this(color.red, color.green, color.blue)
    fun toColor(): Color = ColorUIResource(red, green, blue)
    override fun toString(): String = "#%02X%02X%02X".format(red, green, blue)
}

@Serializable
internal data class DimensionLafValueConfig(
    val width: Int,
    val height: Int,
) : LafValueConfig() {
    constructor(dimension: Dimension) : this(dimension.width, dimension.height)
    override fun toString(): String = "%d,%d".format(width, height)
}
