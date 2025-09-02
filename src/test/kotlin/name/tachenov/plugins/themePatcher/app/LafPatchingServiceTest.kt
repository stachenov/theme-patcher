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

import com.intellij.ide.ui.UIDensity
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import javax.swing.UIDefaults

internal class LafPatchingServiceTest {
    private lateinit var currentTheme: ThemeConfig
    private lateinit var uiDensity: UIDensity
    private var scaling: Float = 1.0f
    private lateinit var rulesets: MutableList<RulesetConfig>
    private lateinit var lafDefaults: LookAndFeelDefaults
    private lateinit var sut: LafPatchingService

    private val lafSettings: LafSettings
        get() = LafSettings(currentTheme, uiDensity, scaling)

    @BeforeEach
    fun setUp() {
        currentTheme = THEME1
        uiDensity = UIDensity.DEFAULT
        scaling = 1.0f
        rulesets = mutableListOf()
        lafDefaults = LookAndFeelDefaults(UIDefaults())
        sut = LafPatchingService()
    }

    @Test
    fun `patch scalable int value`() {
        addRuleset("ruleset1", currentTheme)
        addRule("ruleset1", "Tree.rowHeight", 20)
        lafDefaults["Tree.rowHeight"] = 24
        patchLafDefaults()
        assertThat(lafDefaults["Tree.rowHeight"]).isEqualTo(20)
    }

    @Test
    fun `only themes mentioned in the ruleset are affected`() {
        addRuleset("ruleset1", THEME1)
        addRule("ruleset1", "Tree.rowHeight", 20)
        lafDefaults["Tree.rowHeight"] = 24
        currentTheme = THEME2
        patchLafDefaults()
        assertThat(lafDefaults["Tree.rowHeight"]).isEqualTo(24)
    }

    @Test
    fun `when editing rules, previously patched values are restored`() {
        addRuleset("ruleset1", THEME1)
        addRule("ruleset1", "Tree.rowHeight", 20)
        addRule("ruleset1", "List.rowHeight", 18)
        lafDefaults["Tree.rowHeight"] = 24
        lafDefaults["List.rowHeight"] = 22
        currentTheme = THEME1
        patchLafDefaults()
        assertThat(lafDefaults["Tree.rowHeight"]).isEqualTo(20)
        assertThat(lafDefaults["List.rowHeight"]).isEqualTo(18)
        removeRule("ruleset1", "List.rowHeight")
        patchLafDefaults()
        assertThat(lafDefaults["Tree.rowHeight"]).isEqualTo(20)
        assertThat(lafDefaults["List.rowHeight"]).isEqualTo(22)
    }

    @Test
    fun `when switching themes, previously saved original values are not restored`() {
        addRuleset("ruleset1", THEME1)
        addRule("ruleset1", "Tree.rowHeight", 20)
        addRule("ruleset1", "List.rowHeight", 18)
        addRuleset("ruleset2", THEME2)
        addRule("ruleset2", "Tree.rowHeight", 16)
        lafDefaults["Tree.rowHeight"] = 24
        lafDefaults["List.rowHeight"] = 22
        currentTheme = THEME1
        patchLafDefaults()
        assertThat(lafDefaults["Tree.rowHeight"]).isEqualTo(20)
        assertThat(lafDefaults["List.rowHeight"]).isEqualTo(18)
        currentTheme = THEME2
        lafDefaults["Tree.rowHeight"] = 28
        lafDefaults["List.rowHeight"] = 26
        patchLafDefaults()
        assertThat(lafDefaults["Tree.rowHeight"]).isEqualTo(16)
        assertThat(lafDefaults["List.rowHeight"]).isEqualTo(26)
        patchLafDefaults() // the second call attempts to restore values patched by the first call
        assertThat(lafDefaults["Tree.rowHeight"]).isEqualTo(16)
        assertThat(lafDefaults["List.rowHeight"]).isEqualTo(26)
    }

    @Test
    fun `patch scalable int value - compact mode, both values are specified`() {
        addRuleset("ruleset1", currentTheme)
        addRule("ruleset1", "Tree.rowHeight", 20)
        addRule("ruleset1", "Tree.rowHeight.compact", 16)
        lafDefaults["Tree.rowHeight"] = 24
        uiDensity = UIDensity.COMPACT
        patchLafDefaults()
        assertThat(lafDefaults["Tree.rowHeight"]).isEqualTo(16)
    }

    @Test
    fun `patch scalable int value - not compact mode, both values are specified`() {
        addRuleset("ruleset1", currentTheme)
        addRule("ruleset1", "Tree.rowHeight", 20)
        addRule("ruleset1", "Tree.rowHeight.compact", 16)
        lafDefaults["Tree.rowHeight"] = 24
        uiDensity = UIDensity.DEFAULT
        patchLafDefaults()
        assertThat(lafDefaults["Tree.rowHeight"]).isEqualTo(20)
    }

    @Test
    fun `patch scalable int value - compact mode, only compact mode value is specified`() {
        addRuleset("ruleset1", currentTheme)
        addRule("ruleset1", "Tree.rowHeight.compact", 16)
        lafDefaults["Tree.rowHeight"] = 24
        uiDensity = UIDensity.COMPACT
        patchLafDefaults()
        assertThat(lafDefaults["Tree.rowHeight"]).isEqualTo(16)
    }

    private fun patchLafDefaults() {
        sut.patchLafDefaults(lafSettings, lafDefaults, rulesets)
    }

    private fun addRuleset(rulesetName: String, vararg themes: ThemeConfig) {
        assertThat(rulesets.associateBy { it.rulesetName }).doesNotContainKey(rulesetName)
        rulesets += RulesetConfig(rulesetName, themes.toList(), emptyList())
    }

    private fun addRule(rulesetName: String, key: String, value: Int) {
        val rulesetIndex = rulesets.indexOfFirst { it.rulesetName == rulesetName }
        val ruleset = rulesets[rulesetIndex]
        rulesets[rulesetIndex] = ruleset.copy(rules = ruleset.rules + rule(key, value))
    }

    private fun rule(key: String, value: Int) = RuleConfig(key, IntLafValueConfig(value))

    private fun removeRule(rulesetName: String, key: String) {
        val rulesetIndex = rulesets.indexOfFirst { it.rulesetName == rulesetName }
        val ruleset = rulesets[rulesetIndex]
        val rule = ruleset.rules.single { it.key == key }
        rulesets[rulesetIndex] = ruleset.copy(rules = ruleset.rules - rule)
    }
}

private val THEME1 = ThemeConfig("Theme One", "theme1")
private val THEME2 = ThemeConfig("Theme Two", "theme2")
