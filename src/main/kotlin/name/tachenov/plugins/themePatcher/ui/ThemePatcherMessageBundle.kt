package name.tachenov.plugins.themePatcher.ui

import com.intellij.DynamicBundle
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey
import java.util.function.Supplier

@NonNls
private const val BUNDLE = "messages.ThemePatcherMessageBundle"

internal object ThemePatcherMessageBundle {
    private val instance = DynamicBundle(ThemePatcherMessageBundle::class.java, BUNDLE)

    @JvmStatic
    fun message(key: @PropertyKey(resourceBundle = BUNDLE) String, vararg params: Any): @Nls String {
        return instance.getMessage(key, *params)
    }

    @JvmStatic
    fun lazyMessage(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any): Supplier<@Nls String> {
        return instance.getLazyMessage(key, *params)
    }
}
