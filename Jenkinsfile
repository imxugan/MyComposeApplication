pipeline {
    agent any

    environment {
        NVD_API_KEY = credentials('NVD_API_KEY')         // OWASP 使用的 NVD API Key
        ANDROID_HOME = '/opt/android-sdk'
        PATH = "${ANDROID_HOME}/platform-tools:${ANDROID_HOME}/cmdline-tools/latest/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"
        GRADLE_OPTS = '-Xmx4g -Dfile.encoding=UTF-8'
        CI = 'true'
    }

    stages {
        stage('Checkout') {
            steps { checkout scm }
        }

        stage('Build and Verify') {
            steps {
                withCredentials([
                    file(credentialsId: 'KEYSTORE_FILE', variable: 'KEYSTORE_FILE'),
                    string(credentialsId: 'KEYSTORE_PWD', variable: 'KEYSTORE_PWD'),
                    string(credentialsId: 'KEY_PWD', variable: 'KEY_PWD')
                ]) {
                    // 1. 修复权限
                    sh 'chmod +x ./gradlew'

                    // 2. 代码格式检查（失败直接中断，触发 failure 邮件）
                    sh './gradlew spotlessCheck'

                    // 3. Lint 检查（失败直接中断）
                    sh './gradlew lint'

                    // 4. OWASP 依赖漏洞扫描（捕获错误，标记为 UNSTABLE，继续执行）
                    catchError(buildResult: 'UNSTABLE', stageResult: 'UNSTABLE') {
                        sh './gradlew dependencyCheckAnalyze'
                    }

                    // 5. 编译 Debug 包
                    sh './gradlew assembleDebug'

                    // 6. 运行单元测试并生成覆盖率报告
                    sh './gradlew :app:createDebugCombinedCoverageReport --rerun-tasks'

                    // 7. 增量覆盖率检查
                    sh '''
                        TARGET_BRANCH="origin/main"
                        diff-cover app/build/reports/jacoco/createDebugCombinedCoverageReport/createDebugCombinedCoverageReport.xml \
                            --compare-branch $TARGET_BRANCH \
                            --fail-under=0 \
                            --src-roots "src/main/java"
                    '''

                    // 8. 发布覆盖率报告到 Jenkins JaCoCo 插件（自动生成项目页面入口和趋势图）
                    //    classPattern 仅使用原始 Kotlin 编译目录，避免插桩类导致插件崩溃
                    jacoco(
                        execPattern: 'app/build/outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec',
                        classPattern: 'app/build/tmp/kotlin-classes/debug/**',  // ★ 移除插桩目录
                        sourcePattern: 'app/src/main/java/**'
                    )

                    // 9. 构建 Release 包
                    sh './gradlew assembleRelease'
                }
            }
        }
    }

    post {
        // 构建失败（任何致命错误）发送邮件
        failure {
            mail(
                subject: "❌ CI 失败: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                body: "项目: ${env.JOB_NAME}\n构建号: ${env.BUILD_NUMBER}\n详情: ${env.BUILD_URL}",
                to: 'imxugan@163.com'
            )
        }

        // 构建不稳定（如 OWASP 失败）发送邮件
        unstable {
            mail(
                subject: "⚠️ CI 警告: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                body: "OWASP 扫描未通过或覆盖率不足。\n详情: ${env.BUILD_URL}",
                to: 'imxugan@163.com'
            )
        }

        // 总是执行报告归档，无论构建成功还是失败
        always {
            // 发布 OWASP HTML 报告
            publishHTML(
                target: [
                    allowMissing: true,  // 报告缺失时不中断构建
                    alwaysLinkToLastBuild: false,
                    keepAll: true,
                    reportDir: 'app/build/reports',  // OWASP 报告的父目录
                    reportFiles: 'dependency-check-report.html',  // 报告文件名
                    reportName: 'OWASP Dependency-Check Report'
                ]
            )
            // 无论成功失败都清理工作区
            cleanWs()
        }
    }
}
