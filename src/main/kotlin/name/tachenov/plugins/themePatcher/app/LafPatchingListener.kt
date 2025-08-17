package name.tachenov.plugins.themePatcher.app

import com.intellij.ide.ui.LafManager
import com.intellij.ide.ui.LafManagerListener

internal class LafPatchingListener : LafManagerListener {
    override fun lookAndFeelChanged(manager: LafManager) {
        LafPatchingService.getInstance().patchThemeOnLafChange()
    }
}
