import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * OWASP 依赖安全检查规约插件
 * 统一为所有子模块配置 OWASP Dependency-Check 第三方依赖漏洞检测
 * 作用：自动扫描项目依赖包的已知安全漏洞（CVE），保障依赖安全
 */
class OwaspConventionPlugin implements Plugin<Project> {

  /**
   * 插件应用逻辑：给目标 Project 自动配置 dependency-check 插件与默认规则
   * @param project 应用该插件的项目/模块
   */
  @Override
  void apply(Project project) {
    // 1. 自动应用 OWASP 依赖检查核心插件
    project.pluginManager.apply('org.owasp.dependencycheck')

    // 获取 NVD API Key
    // 1. 尝试从系统环境变量中获取
    // 2. 若不存在，则从 gradle.properties 中读取
    // 3. 若都不存在，则设为 null (此时插件会使用无key模式，速度极慢)
    def nvdApiKey = System.getenv("NVD_API_KEY") ?: project.findProperty("owasp.nvdApiKey")

    // 2. 统一配置 OWASP 检查规则（所有模块共用一套安全标准）
    project.dependencyCheck {
      // CVSS 漏洞评分 ≥ 7.0 时，直接构建失败（阻断高危漏洞上线）
      // CVSS 7.0 属于高危漏洞，必须修复
      failBuildOnCVSS = 7.0f

      // 指定漏洞白名单/忽略规则文件（根目录下的 owasp-suppression.xml）
      // 用于忽略确认无害/已修复/误报的漏洞
      // 仅当 suppression 文件存在时才配置，避免因文件缺失导致失败
      def suppressionFile = project.rootProject.file('owasp-suppression.xml')
      if (suppressionFile.exists()) {
        suppressionFiles = [suppressionFile.path]
      }

      analyzers {

        // 关闭 .NET Assembly 分析器
        // 👉 作用：
        //    - 扫描 .dll / .exe 等 .NET 依赖
        // 👉 为什么关闭：
        //    - Android / Java 项目不会用到 .NET
        // 👉 收益：
        //    - 减少无意义扫描，略微提升构建速度
        assemblyEnabled = false


        // 关闭 Node.js 依赖分析
        // 👉 作用：
        //    - 扫描 package.json / node_modules
        // 👉 为什么关闭：
        //    - 当前是 Android 项目，没有 Node 依赖
        // 👉 收益：
        //    - 明显减少扫描时间（中等优化）
        nodeEnabled = false


        // 关闭 npm audit 分析（Node 漏洞审计）
        // 👉 作用：
        //    - 调用 npm audit 检查 JS 依赖漏洞
        // 👉 为什么关闭：
        //    - 同样依赖 Node 环境 + 网络
        //    - CI 环境可能没有 npm
        // 👉 收益：
        //    - 避免环境依赖问题 + 提升稳定性
        nodeAuditEnabled = false
      }

      // 配置 NVD API Key
      // 参考: https://jeremylong.github.io/DependencyCheck/dependency-check-gradle/configuration.html
      nvd {
        apiKey = nvdApiKey // 设置从环境变量或 gradle.properties 获取的 Key
      }

      data {
        // 🚀 CI 缓存核心
        directory = "/var/lib/jenkins/owasp-cache"
      }
    }
  }
}
