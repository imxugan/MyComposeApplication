import com.diffplug.gradle.spotless.SpotlessExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * 自定义 Spotless 代码格式化规约插件
 * 统一管理项目中 Kotlin 代码(.kt) 和 Gradle Kotlin 脚本(.gradle.kts) 的代码格式化规则
 */
class SpotlessConventionPlugin : Plugin<Project> {

    /**
     * 插件应用入口方法，Gradle 会在应用插件时自动执行
     * @param project 应用当前插件的 Gradle 项目对象
     */
    override fun apply(project: Project) {
        // 自动引入官方 Spotless 插件，无需在 build.gradle.kts 中手动声明
        project.pluginManager.apply("com.diffplug.spotless")

        // 获取 Spotless 扩展配置，用于配置格式化规则
        val spotless = project.extensions.getByType(SpotlessExtension::class.java)

        // ======================== 配置 Kotlin 代码(.kt)格式化规则 ========================
        spotless.kotlin {
            // 匹配项目中所有 Kotlin 源文件
            target("**/*.kt")

            // 使用指定版本(1.0.1)的 ktlint 进行代码格式化与检查
            ktlint("1.0.1").editorConfigOverride(
                mapOf(
                    // 忽略带有 @Composable 注解的函数命名规范检查（Jetpack Compose 专用）
                    "ktlint_function_naming_ignore_when_annotated_with" to "Composable",
                    // 禁用 Kotlin 文件命名规范检查（允许非大驼峰命名）
                    "ktlint_standard_filename" to "disabled"
                )
            )

            trimTrailingWhitespace()   // 移除代码行尾多余空格
            indentWithSpaces()         // 使用空格缩进（而非 Tab）
            endWithNewline()           // 文件末尾自动保留空行
        }

        // ======================== 配置 Gradle Kotlin 脚本(.gradle.kts)格式化规则 ========================
        spotless.kotlinGradle {
            // 匹配项目中所有 Gradle Kotlin 脚本文件
            target("*.gradle.kts", "**/*.gradle.kts")

            // 使用 ktlint 1.0.1 格式化脚本文件
            ktlint("1.0.1")

            trimTrailingWhitespace()   // 移除脚本行尾多余空格
            endWithNewline()           // 脚本文件末尾自动保留空行
        }
    }
}