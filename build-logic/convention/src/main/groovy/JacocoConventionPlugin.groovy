import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.testing.jacoco.tasks.JacocoReport

class JacocoConventionPlugin implements Plugin<Project> {
  void apply(Project project) {
    project.pluginManager.apply('jacoco')

    // 为非 Android 模块提供后备配置
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

          def exclusions = [
            '**/R.class', '**/R$*.class', '**/BuildConfig.*', '**/Manifest*.*',
            '**/*_Factory.class', '**/*_MembersInjector.class',
            '**/Lambda$*.class', '**/Lambda.class', '**/*Lambda.class',
            '**/*Companion*', '**/*Module.*', '**/*Dagger*'
          ]

          def classesDir = project.layout.buildDirectory
            .dir("tmp/kotlin-classes/${variant.name}")
            .get().asFile
          if (classesDir.exists()) {
            task.classDirectories.from = project.files(
              project.fileTree(dir: classesDir, excludes: exclusions)
            )
          }

          // ===== 唯一改动：修正执行数据文件路径 =====
          // AGP 8.x 默认将单元测试的 .exec 生成到 outputs/unit_test_code_coverage 目录
          def execFile = project.layout.buildDirectory
            .file("outputs/unit_test_code_coverage/${variant.name}UnitTest/${testTaskName}.exec")
            .get().asFile
          if (execFile.exists()) {
            task.executionData.from = project.files(execFile)
          }
        }
      }
    }
  }
}
