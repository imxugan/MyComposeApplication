import org.gradle.api.Plugin
import org.gradle.api.Project
import org.owasp.dependencycheck.gradle.extension.DependencyCheckExtension

/**
 * OWASP 依赖安全检查规约插件
 * 统一为所有子模块配置 OWASP Dependency-Check 第三方依赖漏洞检测
 * 作用：自动扫描项目依赖包的已知安全漏洞（CVE），保障依赖安全
 */
class OwaspConventionPlugin : Plugin<Project> {

  override fun apply(project: Project) {
    project.pluginManager.apply("org.owasp.dependencycheck")

    val nvdApiKey = System.getenv("NVD_API_KEY")
      ?: project.findProperty("owasp.nvdApiKey") as? String

    project.extensions.configure(DependencyCheckExtension::class.java) {
      // 所有可能被 Kotlin 识别为 val 的属性，统统使用 Java setter 方法
      setFailBuildOnCVSS(7.0f)

      val suppressionXml = project.rootProject.file("owasp-suppression.xml")
      if (suppressionXml.exists()) {
        setSuppressionFile(suppressionXml.path)
      } else {
        setSuppressionFile("")
      }

      analyzers {
        setAssemblyEnabled(false)          // 显式调用 setter，替代 assemblyEnabled = false

        retirejs {
          setEnabled(false)                // 显式调用 setter，替代 enabled = false
        }
      }

      nvd {
        setApiKey(nvdApiKey)
      }

      data {
        setDirectory("/var/lib/jenkins/owasp-cache")      // 这个通常不是 val，可保留
      }

      hostedSuppressions {
        setUrl("https://jeremylong.github.io/DependencyCheck/suppressions/publishedSuppressions.xml")
      }
    }
  }
}
