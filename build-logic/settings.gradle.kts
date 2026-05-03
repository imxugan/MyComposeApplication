// build-logic/settings.gradle.kts
// 1. 插件管理配置：定义【Gradle 插件】从哪里下载
// 注意：这里只管理插件的仓库，不管理项目依赖的仓库
pluginManagement {
    repositories {
        gradlePluginPortal()  // Gradle 官方插件中心（最核心）
        google()              // Google 插件仓库
        mavenCentral()        // 中央仓库
    }
}

// 2. 依赖解析管理：整个项目所有依赖的总配置（核心区域）
dependencyResolutionManagement {
    // 严格模式：禁止任何子模块自己声明 repositories，否则直接编译失败
    // 强制所有仓库统一在这里管理，这是大型项目标准规范
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    // 项目依赖的仓库（所有第三方库都从这里下载）
    repositories {
        // Google 仓库：只允许 Android 相关依赖，过滤无关依赖加速构建
        google {
            content {
                includeGroupByRegex("com\\.android.*")  // 匹配 com.android 开头
                includeGroupByRegex("com\\.google.*")    // 匹配 com.google 开头
                includeGroupByRegex("androidx.*")       // 匹配 androidx 开头
            }
        }
        mavenCentral()  // 通用开源库中央仓库
        gradlePluginPortal()
    }

    // 3. 版本目录配置：让 build-logic 模块能使用根项目的 libs.versions.toml
    // 必须写在 dependencyResolutionManagement 内部（你之前的关键问题）
    versionCatalogs {
        // 创建名为 libs 的版本目录（和根项目保持一致）
        create("libs") {
            // 引用根项目下的 gradle/libs.versions.toml
            // ../ 表示向上退一级目录（从 build-logic 回到项目根目录）
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

// 4. build-logic 本身的项目配置
rootProject.name = "build-logic"  // 当前包含构建的名称
include(":convention")             // 包含 convention 子模块（存放自定义插件）