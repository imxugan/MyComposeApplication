import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension

/**
 * 统一单元测试依赖插件
 * 所有模块自动应用：JUnit、MockK、Coroutines Test、Compose Test
 * 基于 libs.versions.toml 版本 catalog
 */
class UnitTestConventionPlugin : Plugin<Project> {

  override fun apply(project: Project) {
    val isAppModule = project.plugins.findPlugin("com.android.application") != null
    if (!isAppModule) return

    project.afterEvaluate {
      val libs = project.extensions.getByType(VersionCatalogsExtension::class.java).named("libs")
      val dependencies = project.dependencies

      fun addLib(configuration: String, alias: String) {
        dependencies.addProvider(configuration, libs.findLibrary(alias).get())
      }

      addLib("testImplementation", "junit")
      addLib("testImplementation", "mockk")
      addLib("testImplementation", "kotlinx-coroutines-test")
      addLib("testImplementation", "androidx-compose-ui-test-junit4")
      addLib("debugImplementation", "androidx-compose-ui-test-manifest")
    }
  }
}
