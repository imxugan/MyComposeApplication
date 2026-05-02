import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.DependencyHandler

/**
 * 统一单元测试依赖插件
 * 所有模块自动应用：JUnit、MockK、Coroutines Test、Compose Test
 * 基于 libs.versions.toml 版本 catalog
 */
class UnitTestConventionPlugin implements Plugin<Project> {

  @Override
  void apply(Project project) {
    // 只给【有 compose 的 Android 应用模块】添加测试依赖
    // 纯库模块 / 纯 test 模块不加，避免版本缺失报错
    def isAppModule = project.plugins.findPlugin("com.android.application") != null

    if (!isAppModule) {
      return
    }

    project.afterEvaluate {
      addTestDependencies(project.dependencies)
    }
  }

  private void addTestDependencies(DependencyHandler dependencies) {
    dependencies.add("testImplementation", project.libs.junit)
    dependencies.add("testImplementation", project.libs.mockk)
    dependencies.add("testImplementation", project.libs.kotlinx.coroutines.test)
    dependencies.add("testImplementation", project.libs.androidx.ui.test.junit4)
    dependencies.add("debugImplementation", project.libs.androidx.ui.test.manifest)
  }
}
