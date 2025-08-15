package name.tachenov.plugins.themePatcher.ui

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurableProvider

class ThemePatcherConfigurableProvider : ConfigurableProvider() {
    override fun createConfigurable(): Configurable = ThemePatcherConfigurable()
}
