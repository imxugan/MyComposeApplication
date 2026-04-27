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

    // 2. 统一配置 OWASP 检查规则（所有模块共用一套安全标准）
    project.dependencyCheck {
      // CVSS 漏洞评分 ≥ 7.0 时，直接构建失败（阻断高危漏洞上线）
      // CVSS 7.0 属于高危漏洞，必须修复
      failBuildOnCVSS = 7.0f

      // 指定漏洞白名单/忽略规则文件（根目录下的 owasp-suppression.xml）
      // 用于忽略确认无害/已修复/误报的漏洞
      suppressionFiles = project.rootProject.file('owasp-suppression.xml').path

      // 关闭 .NET assembly 分析器（Java 项目无需启用，提升扫描速度）
      analyzers.assemblyEnabled = false
    }
  }
}
