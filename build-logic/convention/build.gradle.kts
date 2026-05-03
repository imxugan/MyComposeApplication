// build-logic/convention/build.gradle.kts
plugins {
    `kotlin-dsl`   // 用于开发 Gradle 插件的 Kotlin 支持
}

dependencies {
    // 如果需要在插件中访问 Android Gradle Plugin 的类型，可以添加
     compileOnly(libs.android.gradlePlugin)

    compileOnly(libs.spotless.gradlePlugin)
    implementation(libs.owasp.dependencycheck.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("jacocoConvention") {
            id = "com.xg.mycomposeapplication.jacoco.convention"
            implementationClass = "JacocoConventionPlugin"
        }

        register("spotless") {
            id = "com.xg.spotless.convention"
            implementationClass = "SpotlessConventionPlugin"
        }

        register("root") {
            id = "com.xg.mycomposeapplication.root.convention"
            implementationClass = "RootConventionPlugin"
        }

        register("owaspConvention") {
            id = "com.xg.mycomposeapplication.owasp.convention"
            implementationClass = "OwaspConventionPlugin"
        }

        register("unitTest") {
            id = "com.xg.mycomposeapplication.unit.test.convention"
            implementationClass = "UnitTestConventionPlugin"
        }
    }
}
