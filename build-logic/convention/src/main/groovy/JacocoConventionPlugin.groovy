import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.testing.jacoco.tasks.JacocoReport

/**
 * JaCoCo 覆盖率公约插件
 * 职责：
 *   1. 为所有 Android 模块（app + library）自动创建覆盖率报告任务
 *   2. 自动跳过没有单元测试的模块，避免构建失败
 * 设计原则：
 *   不依赖 AGP 内部 API（如 internal.api.BaseVariant）
 *   通过 afterEvaluate 确保 Android 扩展就绪后，再动态注册变体任务
 */
class JacocoConventionPlugin implements Plugin<Project> {
  void apply(Project project) {
    // 1. 先应用 jacoco 插件
    project.pluginManager.apply('jacoco')

    // 2. 配置所有 Test 任务：收集覆盖率执行数据
    project.afterEvaluate {
      // 为所有 Test 任务启用 JaCoCo 数据采集
      project.tasks.withType(Test).configureEach { Test test ->
        test.jacoco {
          enabled = true
          includeNoLocationClasses = true
          excludes = ['jdk.internal.*']
          // 指定 .exec 数据文件的输出路径
          destinationFile = project.layout.buildDirectory
            .file("jacoco/${test.name}.exec")
            .get()
            .asFile
        }
      }
    }

    // 3. 延迟注册覆盖率报告任务（等待 Android 插件就绪）
    project.pluginManager.withPlugin('com.android.application') {
      // 当 Android Application 插件被应用后，才执行此回调
      registerCoverageReportTasks(project)
    }
    project.pluginManager.withPlugin('com.android.library') {
      // 当 Android Library 插件被应用后，才执行此回调
      registerCoverageReportTasks(project)
    }
  }

  /**
   * 核心方法：为 Android 模块动态注册覆盖率报告任务
   * 使用 afterEvaluate 确保 'android' 扩展和变体列表完全就绪
   */
  private void registerCoverageReportTasks(Project project) {
    project.afterEvaluate {
      def android = project.extensions.findByName('android')
      if (android == null) {
        project.logger.warn("JacocoPlugin: 'android' extension not found on ${project.name}")
        return
      }

      // 获取所有变体（app 或 library）
      def variants = []
      try {
        // 兼容老版本 AGP 的写法
        if (android.hasProperty('applicationVariants')) {
          variants.addAll(android.applicationVariants)
        }
        if (android.hasProperty('libraryVariants')) {
          variants.addAll(android.libraryVariants)
        }
      } catch (Exception e) {
        project.logger.warn("JacocoPlugin: Failed to resolve variants for ${project.name}: ${e.message}")
        return
      }

      // 为每个变体创建覆盖率报告任务
      variants.each { variant ->
        createCoverageReportTask(project, variant)
      }
    }
  }

  /**
   * 为单个变体创建覆盖率报告任务
   * @param variant Android 构建变体对象（application 或 library 变体）
   */
  private void createCoverageReportTask(Project project, Object variant) {
    // variant.name 在所有变体类型中都是相同的属性
    String variantName = variant.name.capitalize()
    String taskName = "create${variantName}CombinedCoverageReport"
    String testTaskName = "test${variantName}UnitTest"

    // 安全检查：如果该变体没有对应的单元测试任务，直接跳过
    if (project.tasks.findByName(testTaskName) == null) {
      project.logger.info("JacocoPlugin: Skipping ${taskName} — no ${testTaskName} found.")
      return
    }

    // 注册覆盖率报告任务
    project.tasks.register(taskName, JacocoReport) { JacocoReport task ->
      task.group = 'verification'
      task.description = "Generates combined unit test coverage report for ${variant.name} variant."
      task.dependsOn testTaskName

      // 报告格式：HTML（本地浏览） + XML（CI 解析）
      task.reports {
        html.required = true
        xml.required = true
      }

      // 源码目录
      task.sourceDirectories.from = project.files(
        project.file("src/main/java"),
        project.file("src/${variant.name}/java")
      )

      // 覆盖率统计需排除的类
      def exclusions = [
        '**/R.class', '**/R$*.class', '**/BuildConfig.*', '**/Manifest*.*',
        '**/*_Factory.class', '**/*_MembersInjector.class',
        '**/Lambda$*.class', '**/Lambda.class', '**/*Lambda.class',
        '**/*Companion*', '**/*Module.*', '**/*Dagger*'
      ]

      // 字节码目录
      def classesDir = project.layout.buildDirectory
        .dir("tmp/kotlin-classes/${variant.name}")
        .get().asFile
      if (classesDir.exists()) {
        task.classDirectories.from = project.files(
          project.fileTree(dir: classesDir, excludes: exclusions)
        )
      }

      // 执行数据文件路径（由 Test 任务生成）
      task.executionData.from = project.layout.buildDirectory
        .file("jacoco/${testTaskName}.exec")
    }
  }
}
