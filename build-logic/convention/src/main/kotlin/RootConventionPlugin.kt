// RootConventionPlugin.kt
import org.gradle.api.Plugin
import org.gradle.api.Project

class RootConventionPlugin : Plugin<Project> {

    override fun apply(project: Project) {

        if (project != project.rootProject) return

        // ⭐ 只做一件事：应用子插件
        project.subprojects {
            pluginManager.apply("com.xg.spotless.convention")

            pluginManager.apply("com.xg.mycomposeapplication.jacoco.convention")

            pluginManager.apply("com.xg.mycomposeapplication.owasp.convention")

            pluginManager.apply("com.xg.mycomposeapplication.unit.test.convention")
        }
    }
}