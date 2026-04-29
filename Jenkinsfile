pipeline {
    agent any

    environment {
        NVD_API_KEY = credentials('NVD_API_KEY')
        ANDROID_HOME = '/opt/android-sdk'
        // 完全重写 PATH，确保 Android SDK 工具和系统命令可用
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
                    // 1. 修复权限（必须第一条）
                    sh 'chmod +x ./gradlew'

                    // 2. 代码格式检查
                    sh './gradlew spotlessCheck'

                    // 3. Lint 检查
                    sh './gradlew lint'

                    // 4. OWASP 依赖漏洞扫描（不阻塞构建 + 校验报告）
                    sh """
                    ./gradlew dependencyCheckAnalyze \
                      -Dnvd.api.key=$NVD_API_KEY \
                      --failOnError=false
                    """

                    // 检查报告是否生成（未生成则标记为 unstable）
                    script {
                        def reportPath = "app/build/reports/dependency-check-report.html"
                        if (!fileExists(reportPath)) {
                            echo "⚠️ OWASP 报告未生成，可能扫描失败"
                            currentBuild.result = 'UNSTABLE'
                        } else {
                            echo "✅ OWASP 报告已生成: ${reportPath}"
                        }
                    }

                    // 5. 编译 Debug 包（测试覆盖率需要 debug build）
                    sh './gradlew assembleDebug'

                    // 6. 运行单元测试并生成覆盖率报告
                    sh './gradlew :app:createDebugCombinedCoverageReport --rerun-tasks'

                    // 7. 增量覆盖率检查（只检查本次变更代码）
                    sh '''
                        TARGET_BRANCH="origin/main"
                        diff-cover app/build/reports/jacoco/createDebugCombinedCoverageReport/createDebugCombinedCoverageReport.xml \\
                            --compare-branch $TARGET_BRANCH \\
                            --fail-under=80 \\
                            --src-roots "src/main/java"
                    '''

                    // 8. 全量覆盖率验证（行覆盖率，阈值 80%）
                    sh '''
                        REPORT="app/build/reports/jacoco/createDebugCombinedCoverageReport/createDebugCombinedCoverageReport.xml"
                        if [ ! -f "$REPORT" ]; then
                            echo "============================================"
                            echo "  ❌ 构建失败：未找到覆盖率报告 ($REPORT)"
                            echo "============================================"
                            exit 1
                        fi
                        # 提取所有 LINE 级别的 covered 和 missed 数值，用 awk 求和
                        COVERED=$(grep -o '<counter type="LINE"[^>]*covered="[0-9]*"' "$REPORT" | grep -o 'covered="[0-9]*"' | grep -o '[0-9]*' | awk '{s+=$1} END{print s}')
                        MISSED=$(grep -o '<counter type="LINE"[^>]*missed="[0-9]*"' "$REPORT" | grep -o 'missed="[0-9]*"' | grep -o '[0-9]*' | awk '{s+=$1} END{print s}')
                        TOTAL=$((COVERED + MISSED))
                        if [ $TOTAL -eq 0 ]; then
                            echo ""
                            echo "============================================"
                            echo "  ⚠️  没有任何代码可统计，跳过覆盖率验证"
                            echo "============================================"
                            echo ""
                            exit 0
                        fi
                        RATIO=$(echo "scale=4; $COVERED / $TOTAL" | bc)
                        PERCENT=$(echo "scale=1; $RATIO * 100" | bc)
                        echo ""
                        echo "============================================"
                        echo "  当前行覆盖率: ${PERCENT}% (已覆盖: ${COVERED} 行, 未覆盖: ${MISSED} 行)"
                        echo "============================================"
                        echo ""
                        if (( $(echo "$PERCENT < 80.0" | bc -l) )); then
                            echo "============================================"
                            echo "  ❌ 构建失败：覆盖率 ${PERCENT}% 不满足最低阈值 80%"
                            echo "  原因：项目缺少单元测试或测试未覆盖业务代码"
                            echo "  解决：为业务类补充JUnit测试用例"
                            echo "============================================"
                            exit 1
                        else
                            echo "============================================"
                            echo "  ✅ 覆盖率验证通过 (${PERCENT}% ≥ 80%)"
                            echo "============================================"
                        fi
                    '''

                    // 9. 构建 Release 包（签名信息由环境变量提供）
                    sh './gradlew assembleRelease'
                }
            }
        }
    }

    post {
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

                // ✅ 固定人员
                to: 'imxugan@163.com',

                // ✅ 动态人员（关键）
                recipientProviders: [
                    [$class: 'DevelopersRecipientProvider'],  // 提交人
                    [$class: 'CulpritsRecipientProvider']     // 最近导致失败的人
                ]
            )
        }

        unstable {
            emailext(
                subject: "⚠️ CI 警告: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                body: """
                    <h3>⚠️ 构建不稳定</h3>
                    <p>可能原因：覆盖率不足 / diff 未达标</p>
                    <p><a href="${env.BUILD_URL}">查看详情</a></p>
                """,
                mimeType: 'text/html',

                to: 'imxugan@163.com',

                recipientProviders: [
                    [$class: 'DevelopersRecipientProvider']
                ]
            )
        }

        always {
            cleanWs()
        }
    }
}
