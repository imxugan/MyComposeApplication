.gitignore 中不要用中文，会导致.gitignore 中配置的规则无效

不要在gradle.properties 文件中配置敏感信息，将敏感信息配置到 local.properties 文件中

# 项目的关键版本信息
AGP: 8.5.0

Gradle: 8.7

compileSdk: 34

minSdk: 24

Gradle JDK: 21

Kotlin: 1.9.23

kotlin.jvmTarget: 21

Java sourceCompatibility: 21      // 可选，但推荐

Java targetCompatibility: 21      // 可选，但推荐

AndroidX Core: 1.13.0

AndroidX Lifecycle: 2.8.4

kotlinCompilerExtensionVersion: 1.5.13

Compose: 1.6.0 (如果使用)

KSP:  (如果未使用则留空 , 如果使用  填写示例   1.9.23-1.0.19 )

NDK:  ((如果未使用则留空 , 如果涉及原生，填写示例  26.1.10909125 )




# 以下表格列出了每一项版本信息在 Android 项目中的具体查看位置：
| 名称 | 查看位置 |
|------|----------|
| AGP (Android Gradle Plugin) | 根目录的 `build.gradle[.kts]` 或 `libs.versions.toml` 中的 `com.android.application` 插件版本 |
| Gradle | `gradle/wrapper/gradle-wrapper.properties` 文件中的 `distributionUrl` 字段 |
| compileSdk | 模块级 `build.gradle[.kts]` 中 `android` 块内的 `compileSdk` 或 `compileSdkVersion` |
| minSdk | 模块级 `build.gradle[.kts]` 中 `android` 块内的 `minSdk` 或 `minSdkVersion`（通常在 `defaultConfig` 中） |
| Gradle JDK | Android Studio 菜单：`File → Project Structure → SDK Location → Gradle Settings → Gradle JDK`；或命令行执行 `./gradlew -version` 查看 `JVM` 行 |
| Kotlin 版本 | 根目录 `build.gradle[.kts]` 中 `plugins` 块的 `org.jetbrains.kotlin.android` 版本；或 `libs.versions.toml` 中的 `kotlin` 版本；或执行 `./gradlew -version` 查看 `Kotlin:` 行 |
| kotlin.jvmTarget | 模块级 `build.gradle[.kts]` 中 `android` 块下的 `kotlinOptions { jvmTarget = "..." }` |
| Java sourceCompatibility | 模块级 `build.gradle[.kts]` 中 `android` 块下的 `compileOptions { sourceCompatibility = ... }` |
| Java targetCompatibility | 同上，`compileOptions { targetCompatibility = ... }` |
| AndroidX Core | `libs.versions.toml` 或模块级 `build.gradle[.kts]` 中 `dependencies` 下的 `androidx.core:core-ktx` 版本 |
| AndroidX Lifecycle | `libs.versions.toml` 或模块级 `build.gradle[.kts]` 中 `dependencies` 下的 `androidx.lifecycle:lifecycle-runtime-ktx` 版本 |
| Compose | `libs.versions.toml` 中的 `compose-bom` 版本（需根据 BOM 映射表查实际 Compose 版本）；或模块级 `build.gradle[.kts]` 中 `androidx.compose.ui:ui` 等依赖的版本（若未用 BOM） |
| KSP | 根目录 `build.gradle[.kts]` 中 `plugins` 块的 `com.google.devtools.ksp` 版本；或 `libs.versions.toml` 中的 `ksp` 版本 |
| NDK | 模块级 `build.gradle[.kts]` 中 `android` 块内的 `ndkVersion`；或 `android.defaultConfig.ndk` 中配置的版本 |



## 通过脚本获取
./gradlew --init-script version-info.init.gradle printProjectInfo


