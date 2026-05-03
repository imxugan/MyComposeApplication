import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension
import org.gradle.testing.jacoco.tasks.JacocoReport

class JacocoConventionPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    project.pluginManager.apply("jacoco")

    val jacocoExt = project.extensions.getByType(JacocoPluginExtension::class.java)
    jacocoExt.toolVersion = project.providers
      .gradleProperty("jacoco.version")
      .orElse("0.8.12")
      .get()

    project.pluginManager.withPlugin("com.android.application") {
      configureAndroidJacoco(project)
    }
    project.pluginManager.withPlugin("com.android.library") {
      configureAndroidJacoco(project)
    }
  }

  private fun configureAndroidJacoco(project: Project) {
    project.afterEvaluate {
      val android = project.extensions.findByName("android") ?: return@afterEvaluate

      val variants = mutableListOf<Any>()
      try {
        val method = android.javaClass.getMethod("getApplicationVariants")
        @Suppress("UNCHECKED_CAST")
        val appVariants = method.invoke(android) as? Iterable<Any> ?: emptyList()
        variants.addAll(appVariants)
      } catch (_: NoSuchMethodException) {
      }
      try {
        val method = android.javaClass.getMethod("getLibraryVariants")
        @Suppress("UNCHECKED_CAST")
        val libVariants = method.invoke(android) as? Iterable<Any> ?: emptyList()
        variants.addAll(libVariants)
      } catch (_: NoSuchMethodException) {
      }

      variants.forEach { variant ->
        val variantName = variant.javaClass.getMethod("getName").invoke(variant).toString()
        val capitalizedName = variantName.replaceFirstChar { it.uppercase() }
        val taskName = "create${capitalizedName}CombinedCoverageReport"
        val testTaskName = "test${capitalizedName}UnitTest"

        if (project.tasks.findByName(testTaskName) == null) {
          return@forEach
        }

        // 直接创建任务，避免 register 的泛型歧义
        val reportTask = project.tasks.create(taskName, JacocoReport::class.java)
        reportTask.group = "verification"
        reportTask.description = "Generates combined unit test coverage report for $variantName variant."
        reportTask.dependsOn(testTaskName)

        reportTask.reports.html.required.set(true)
        reportTask.reports.xml.required.set(true)

        reportTask.sourceDirectories.setFrom(
          project.files("src/main/java"),
          project.files("src/$variantName/java")
        )

        val originalClassesDir = project.layout.buildDirectory
          .dir("tmp/kotlin-classes/$variantName")
          .get().asFile

        val exclusions = listOf(
          "**/R.class", "**/R\$*.class", "**/BuildConfig.*", "**/Manifest*.*",
          "**/ComposableSingletons\$*.class",
          "**/*\$Lambda\$*.class",
          "**/*_Factory.class", "**/*Dagger*", "**/*Module.*"
        )

        reportTask.classDirectories.from(
          project.fileTree(originalClassesDir).exclude(exclusions)
        )

        val execFile = project.layout.buildDirectory
          .file("outputs/unit_test_code_coverage/${variantName}UnitTest/$testTaskName.exec")
          .get().asFile
        reportTask.executionData.from(project.files(execFile))
      }
    }
  }
}
