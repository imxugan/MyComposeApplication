import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.testing.jacoco.tasks.JacocoReport

class JacocoConventionPlugin implements Plugin<Project> {
  void apply(Project project) {
    project.pluginManager.apply('jacoco')

    // 非 Android 模块的后备配置
    project.tasks.withType(Test).configureEach { Test test ->
      test.jacoco {
        enabled = true
        includes = ['**/*.class']
        excludes = ['**/R.class', '**/BuildConfig.*']
        destinationFile = project.layout.buildDirectory
          .file("jacoco/${test.name}.exec").get().asFile
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

        if (project.tasks.findByName(testTaskName) == null) {
          return
        }

        project.tasks.register(taskName, JacocoReport) { JacocoReport task ->
          task.group = 'verification'
          task.description = "Generates combined unit test coverage report for ${variant.name} variant."
          task.dependsOn testTaskName

          task.reports {
            html.required = true
            xml.required = true
          }

          task.sourceDirectories.from = project.files(
            project.file("src/main/java"),
            project.file("src/${variant.name}/java")
          )

          // ---------- 修改点：排除规则微调 ----------
          // 移除 `**/*Companion*`，避免误伤 Kotlin Companion Object 的覆盖统计
          def exclusions = [
            '**/R.class', '**/R$*.class', '**/BuildConfig.*', '**/Manifest*.*',
            '**/*_Factory.class', '**/*_MembersInjector.class',
            '**/Lambda$*.class', '**/Lambda.class', '**/*Lambda.class',
            '**/*Module.*', '**/*Dagger*'
          ]

          // ---------- ★ 唯一修改点：使用 AGP 离线插桩后的类目录 ----------
          // 原因：启用 testCoverageEnabled 后，单元测试实际执行的是插桩类
          // 该类位于 intermediates/classes/<variant>/jacocoDebug，必须使用此目录
          // 才能与 executionData 正确匹配
          def jacocoClassesDir = project.layout.buildDirectory
            .dir("intermediates/classes/${variant.name}/jacocoDebug")
            .get().asFile

          task.classDirectories.from = project.files(
            project.fileTree(dir: jacocoClassesDir, excludes: exclusions)
          )

          // executionData 路径不变
          def execFile = project.layout.buildDirectory
            .file("outputs/unit_test_code_coverage/${variant.name}UnitTest/${testTaskName}.exec")
            .get().asFile
          task.executionData.from = project.files(execFile)
        }
      }
    }
  }
}
