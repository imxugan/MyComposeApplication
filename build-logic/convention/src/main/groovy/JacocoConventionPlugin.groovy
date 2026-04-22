import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.testing.jacoco.tasks.JacocoReport

class JacocoConventionPlugin implements Plugin<Project> {
    void apply(Project project) {
        project.pluginManager.apply('jacoco')

        // 1. 关键：使用 afterEvaluate 确保在所有任务创建完成后再配置
        project.afterEvaluate {
            project.tasks.withType(Test).configureEach { Test test ->
                test.jacoco {
                    enabled = true
                    includeNoLocationClasses = true
                    excludes = ['jdk.internal.*']
                    // 使用 Gradle 8+ 推荐的 buildDirectory API
                    destinationFile = project.layout.buildDirectory
                            .file("jacoco/${test.name}.exec")
                            .get()
                            .asFile
                }
            }
        }

        // 2. 为 Android 应用模块创建覆盖率报告任务
        if (project.plugins.hasPlugin('com.android.application')) {
            def android = project.extensions.findByName('android')
            if (android == null) return

            android.applicationVariants.all { variant ->
                def variantName = variant.name.capitalize()
                def taskName = "create${variantName}CombinedCoverageReport"

                project.tasks.register(taskName, JacocoReport) { JacocoReport task ->
                    task.dependsOn "test${variantName}UnitTest"
                    task.reports {
                        html.required = true
                        xml.required = false
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
                            .get()
                            .asFile
                    task.classDirectories.from = project.files(
                            project.fileTree(dir: classesDir, excludes: exclusions)
                    )
                    task.executionData.from = project.layout.buildDirectory
                            .file("jacoco/test${variantName}UnitTest.exec")
                }
            }
        }
    }
}