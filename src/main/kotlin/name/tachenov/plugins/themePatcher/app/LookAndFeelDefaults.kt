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

import javax.swing.UIDefaults
import javax.swing.UIManager

internal val lookAndFeelDefaults = LookAndFeelDefaults

internal object LookAndFeelDefaults {
    private val lafDefaults: UIDefaults
        get() = UIManager.getLookAndFeelDefaults()

    @Suppress("UNCHECKED_CAST")
    private val starMap: MutableMap<Any, Any?>?
        get() = lafDefaults["*"] as? MutableMap<Any, Any?>?

    private val starMapEntries: Set<LookAndFeelEntry>
        get() = starMap?.entries?.mapNotNull {
            if (it.key is String) {
                LookAndFeelEntry("*." + it.key, it.value)
            }
            else {
                null
            }
        }?.toSet() ?: emptySet()

    private val regularEntries: Set<LookAndFeelEntry>
        get() = lafDefaults.entries.mapNotNull {
            val key = it.key
            if (key is String) {
                LookAndFeelEntry(key, it.value)
            }
            else {
                null
            }
        }.toSet()

    val entries: Set<LookAndFeelEntry>
        get() = starMapEntries + regularEntries

    operator fun get(key: String): Any? =
        if (key.startsWith("*.")) {
            starMap?.get(key.substring(2))
        }
        else {
            lafDefaults[key]
        }

    operator fun set(key: String, value: Any?): Any? =
        if (key.startsWith("*.")) {
            starMap?.put(key.substring(2), value)
        }
        else {
            lafDefaults[key] = value
        }

    fun putAll(values: Map<String, Any?>) {
        for ((key, value) in values.entries) {
            this[key] = value
        }
    }

}

internal data class LookAndFeelEntry(
    val key: String,
    val value: Any?,
)
