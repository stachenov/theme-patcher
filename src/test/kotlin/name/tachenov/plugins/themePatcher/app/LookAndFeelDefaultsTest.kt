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

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.awt.Color
import java.util.*
import javax.swing.UIDefaults

internal class LookAndFeelDefaultsTest {
    private lateinit var defaults: UIDefaults
    private lateinit var sut: LookAndFeelDefaults

    @BeforeEach
    fun setUp() {
        defaults = UIDefaults()
        sut = LookAndFeelDefaults(defaults)
    }

    @Test
    fun `wildcard defaults`() {
        val wildcards = Hashtable<Any, Any?>()
        defaults["*"] = wildcards
        sut["*.background"] = Color.RED
        sut["Panel.background"] = Color.BLACK
        assertThat(wildcards.keys).containsExactly("background")
        assertThat(wildcards.values).containsExactly(Color.RED)
        assertThat(sut.entries).containsExactlyInAnyOrder(
            LookAndFeelEntry("*.background", Color.RED),
            LookAndFeelEntry("Panel.background", Color.BLACK),
            LookAndFeelEntry("*", wildcards),
        )
    }
}
