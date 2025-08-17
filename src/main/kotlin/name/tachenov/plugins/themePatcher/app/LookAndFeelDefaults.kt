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
