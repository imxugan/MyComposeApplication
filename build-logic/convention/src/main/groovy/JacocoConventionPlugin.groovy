import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.testing.jacoco.tasks.JacocoReport

class JacocoConventionPlugin implements Plugin<Project> {
  void apply(Project project) {
    project.pluginManager.apply('jacoco')

    // ✅ 从 libs.versions.toml 读取版本（官方规范）
    project.jacoco {
      toolVersion = project.libs.versions.jacoco.get()
    }

    project.tasks.withType(Test).configureEach { Test test ->
      test.jacoco {
        enabled = true
      }
    }

    project.pluginManager.withPlugin('com.android.application') {
      configureAndroidJacoco(project)
    }
    project.pluginManager.withPlugin('com.android.library') {
      configureAndroidJacoco(project)
    }
  }

  private void configureAndroidJacoco(Project project) {
    project.afterEvaluate {
      def android = project.extensions.findByName('android')
      if (android == null) return

      def variants = []
      if (android.hasProperty('applicationVariants')) {
        variants.addAll(android.applicationVariants)
      }
      if (android.hasProperty('libraryVariants')) {
        variants.addAll(android.libraryVariants)
      }

      variants.each { variant ->
        def variantName = variant.name.capitalize()
        def taskName = "create${variantName}CombinedCoverageReport"
        def testTaskName = "test${variantName}UnitTest"

        if (!project.tasks.findByName(testTaskName)) {
          return
        }

        project.tasks.register(taskName, JacocoReport) { JacocoReport task ->
          task.group = 'verification'
          task.description = "Generates combined unit test coverage report for ${variant.name} variant."
          task.dependsOn testTaskName

          task.reports {
            html.required.set(true)
            xml.required.set(true)
          }

          task.sourceDirectories.from(
            project.files("src/main/java", "src/${variant.name}/java")
          )

          // ✅ 官方正确路径：原始未插桩的 Kotlin 类
          def originalClassesDir = project.layout.buildDirectory
            .dir("tmp/kotlin-classes/${variant.name}")
            .get().asFile

          // ✅ 强制排除 Compose 生成类（解决你堆栈里的报错）
          def exclusions = [
            '**/R.class', '**/R$*.class', '**/BuildConfig.*', '**/Manifest*.*',
            '**/ComposableSingletons$*.class', // 🔥 解决你报错的核心行
            '**/*$Lambda$*.class',
            '**/*_Factory.class', '**/*Dagger*', '**/*Module.*'
          ]

          task.classDirectories.from(
            project.fileTree(dir: originalClassesDir, excludes: exclusions)
          )

          def execFile = project.layout.buildDirectory
            .file("outputs/unit_test_code_coverage/${variant.name}UnitTest/${testTaskName}.exec")
            .get().asFile
          task.executionData.from(project.files(execFile))
        }
      }
    }
  }
}
