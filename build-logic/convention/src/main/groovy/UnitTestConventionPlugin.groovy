import org.gradle.api.Plugin
import org.gradle.api.Project
/**
 * 统一单元测试依赖插件
 * 所有模块自动应用：JUnit、MockK、Coroutines Test、Compose Test
 * 基于 libs.versions.toml 版本 catalog
 */
class UnitTestConventionPlugin implements Plugin<Project> {

  void apply(Project project) {
    // 强制等 Android 插件加载完毕
    project.pluginManager.withPlugin("com.android.application") {
      applyTestDependencies(project)
    }
    project.pluginManager.withPlugin("com.android.library") {
      applyTestDependencies(project)
    }
  }

  private void applyTestDependencies(Project project) {
    project.dependencies {
      testImplementation(project.libs.junit)
      testImplementation(project.libs.mockk)
      testImplementation(project.libs.kotlinx.coroutines.test)
      testImplementation(project.libs.androidx.ui.test.junit4)
      debugImplementation(project.libs.androidx.ui.test.manifest)
    }
  }
}
