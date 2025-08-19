/**
Copyright 2025 Sergei Tachenov

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */
package name.tachenov.plugins.themePatcher.app

import com.intellij.openapi.components.SerializablePersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import kotlinx.serialization.Serializable
import java.awt.Color
import java.awt.Dimension
import java.awt.Insets
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

@Serializable
internal data class InsetsLafValueConfig(
    val top: Int,
    val left: Int,
    val bottom: Int,
    val right: Int,
) : LafValueConfig() {
    constructor(insets: Insets) : this(insets.top,  insets.left, insets.bottom, insets.right)
    override fun toString(): String = "%d,%d,%d,%d".format(top,  left, bottom, right)
}

@Serializable
internal data class FontSizeLafValueConfig(
    val size: Int,
) : LafValueConfig() {
    override fun toString(): String = size.toString()
}
