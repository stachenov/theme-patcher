package name.tachenov.plugins.themePatcher.app

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

internal class LafPatchingActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        LafPatchingService.getInstance().patchThemeOnFirstProjectOpen()
    }
}
