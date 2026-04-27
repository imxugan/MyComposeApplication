pipeline {
    agent any

    environment {
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

                    // 2. 清理
                    sh './gradlew clean --no-configuration-cache'

                    // 3. 代码格式检查
                    sh './gradlew spotlessCheck'

                    // 4. Lint 检查
                    sh './gradlew lint'

                    // 5. OWASP 依赖漏洞扫描
                    sh './gradlew dependencyCheckAnalyze'

                    // 6. 编译 Debug 包（测试覆盖率需要 debug build）
                    sh './gradlew assembleDebug'

                    // 7. 运行单元测试并生成覆盖率报告
                    sh './gradlew createDebugCombinedCoverageReport'

                    // 8. 增量覆盖率检查（只检查本次变更代码）
                    sh '''
                        TARGET_BRANCH="origin/main"
                        diff-cover app/build/reports/jacoco/createDebugCombinedCoverageReport/createDebugCombinedCoverageReport.xml \\
                            --compare-branch $TARGET_BRANCH \\
                            --fail-under=80 \\
                            --src-roots "src/main/java"
                    '''

                    // 9. 全量覆盖率验证（行覆盖率，阈值 80%）
                    sh '''
                        REPORT="app/build/reports/jacoco/createDebugCombinedCoverageReport/createDebugCombinedCoverageReport.xml"
                        if [ ! -f "$REPORT" ]; then
                            echo "Coverage report not found: $REPORT"
                            exit 1
                        fi
                        COVERED=$(grep -o '<counter type="LINE"[^>]*covered="[0-9]*"' "$REPORT" | grep -o 'covered="[0-9]*"' | grep -o '[0-9]*')
                        MISSED=$(grep -o '<counter type="LINE"[^>]*missed="[0-9]*"' "$REPORT" | grep -o 'missed="[0-9]*"' | grep -o '[0-9]*')
                        TOTAL=$((COVERED + MISSED))
                        if [ $TOTAL -eq 0 ]; then
                            echo "No lines to check."
                            exit 0
                        fi
                        RATIO=$(echo "scale=4; $COVERED / $TOTAL" | bc)
                        PERCENT=$(echo "scale=1; $RATIO * 100" | bc)
                        echo "Line coverage: ${PERCENT}%"
                        if (( $(echo "$PERCENT < 80.0" | bc -l) )); then
                            echo "Coverage ${PERCENT}% is below 80%"
                            exit 1
                        fi
                    '''

                    // 10. 构建 Release 包（签名信息由环境变量提供）
                    sh './gradlew assembleRelease'
                }
            }
        }
    }

    post {
        always {
            cleanWs()
        }
    }
}
