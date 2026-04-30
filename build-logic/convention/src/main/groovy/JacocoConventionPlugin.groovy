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

          // ---------- 修改点：精确指定两类字节码目录 ----------
          // 原因：AGP 8.x 将 Kotlin 和 Java 的字节码分别输出到以下两个目录
          // - tmp/kotlin-classes/ : 存放 Kotlin 代码编译后的 .class 文件
          // - intermediates/javac/ : 存放 Java 代码编译后的 .class 文件
          // JaCoCo 必须同时包含这两个路径，才能统计所有代码的覆盖率

          def kotlinClassesDir = project.layout.buildDirectory
            .dir("tmp/kotlin-classes/${variant.name}")
            .get().asFile
          def javaClassesDir = project.layout.buildDirectory
            .dir("intermediates/javac/${variant.name}/classes")
            .get().asFile

          task.classDirectories.from = project.files(
            project.fileTree(dir: kotlinClassesDir, excludes: exclusions),
            project.fileTree(dir: javaClassesDir, excludes: exclusions)
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
