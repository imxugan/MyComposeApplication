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

                    sh 'echo "Debug: Checking if NVD_API_KEY is set: ${NVD_API_KEY:0:10}..."'
                    // 4. OWASP 依赖漏洞扫描（失败标记 UNSTABLE，继续后续步骤，并发送邮件）
                    script {
                        try {
                            sh './gradlew dependencyCheckAnalyze'
                        } catch (err) {
                            echo "⚠️ OWASP 扫描失败，构建标记为 UNSTABLE"
                            currentBuild.result = 'UNSTABLE'
                            // 如果需要立即发邮件，可在此处加 emailext，否则依赖 post -> unstable 统一发送
                        }
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
                            --fail-under=80 \
                            --src-roots "src/main/java"
                    '''

                    // 8. 全量覆盖率验证
                    sh '''
                        REPORT="app/build/reports/jacoco/createDebugCombinedCoverageReport/createDebugCombinedCoverageReport.xml"
                        if [ ! -f "$REPORT" ]; then
                            echo "❌ 未找到覆盖率报告"
                            exit 1
                        fi
                        COVERED=$(grep -o '<counter type="LINE"[^>]*covered="[0-9]*"' "$REPORT" | grep -o 'covered="[0-9]*"' | grep -o '[0-9]*' | awk '{s+=$1} END{print s}')
                        MISSED=$(grep -o '<counter type="LINE"[^>]*missed="[0-9]*"' "$REPORT" | grep -o 'missed="[0-9]*"' | grep -o '[0-9]*' | awk '{s+=$1} END{print s}')
                        TOTAL=$((COVERED + MISSED))
                        if [ $TOTAL -eq 0 ]; then
                            echo "⚠️ 无代码可统计，跳过覆盖率检查"
                            exit 0
                        fi
                        RATIO=$(echo "scale=4; $COVERED / $TOTAL" | bc)
                        PERCENT=$(echo "scale=1; $RATIO * 100" | bc)
                        echo "当前行覆盖率: ${PERCENT}%"
                        if (( $(echo "$PERCENT < 80.0" | bc -l) )); then
                            echo "❌ 覆盖率不达标，构建失败"
                            exit 1
                        fi
                    '''

                    // 9. 构建 Release 包
                    sh './gradlew assembleRelease'
                }
            }
        }
    }

    post {
        // 构建失败（任何致命错误）发送邮件
        failure {
            emailext(
                subject: "❌ CI 失败: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                body: """
                    <h3>🚨 构建失败</h3>
                    <p><b>项目:</b> ${env.JOB_NAME}</p>
                    <p><b>构建号:</b> ${env.BUILD_NUMBER}</p>
                    <p><b>分支:</b> ${env.GIT_BRANCH}</p>
                    <p><b>详情:</b> <a href="${env.BUILD_URL}">${env.BUILD_URL}</a></p>
                """,
                mimeType: 'text/html',
                to: 'imxugan@163.com'
            )
        }

        // 构建不稳定（如 OWASP 失败）发送邮件
        unstable {
            emailext(
                subject: "⚠️ CI 警告: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                body: """
                    <h3>⚠️ 构建不稳定</h3>
                    <p>可能原因：OWASP 扫描未通过（网络问题或发现漏洞）、覆盖率不足等。</p>
                    <p><a href="${env.BUILD_URL}">查看详情</a></p>
                """,
                mimeType: 'text/html',
                to: 'imxugan@163.com'
            )
        }

        // 无论成功失败都清理工作区
        always {
            cleanWs()
        }
    }
}
