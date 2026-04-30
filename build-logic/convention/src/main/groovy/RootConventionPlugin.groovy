// RootConventionPlugin.groovy
import org.gradle.api.Plugin
import org.gradle.api.Project

class RootConventionPlugin implements Plugin<Project> {

  @Override
  void apply(Project project) {

    if (project != project.rootProject) return

    // ⭐ 只做一件事：应用子插件
    project.subprojects { sub ->

      sub.pluginManager.apply("com.xg.spotless.convention")

      sub.pluginManager.apply("com.xg.mycomposeapplication.jacoco.convention")

      sub.pluginManager.apply("com.xg.mycomposeapplication.owasp.convention")

      sub.pluginManager.apply("com.xg.mycomposeapplication.unit.test.convention")
    }
  }
}
