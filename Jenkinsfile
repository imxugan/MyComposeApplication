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

                    // 2. 代码格式检查
                    sh './gradlew spotlessCheck'

                    // 3. Lint 检查
                    sh './gradlew lint'

                    // 4. OWASP 依赖漏洞扫描（暂时注释，需要时恢复）
                    // sh './gradlew dependencyCheckAnalyze'

                    // 5. 编译 Debug 包（测试覆盖率需要 debug build）
                    sh './gradlew assembleDebug'

                    // 6. 【临时诊断】运行单元测试并检查覆盖率相关文件和路径
                    sh '''
                        echo "==================== 诊断开始 ===================="
                        echo "1. 运行单元测试 (强制重新执行)"
                        ./gradlew :app:testDebugUnitTest --rerun-tasks
                        echo ""
                        echo "2. 查找所有 .exec 文件"
                        find . -name "*.exec" -type f 2>/dev/null
                        echo ""
                        echo "3. 检查 kotlin-classes/debug 目录是否存在 .class 文件"
                        if [ -d app/build/tmp/kotlin-classes/debug ]; then
                            echo "目录存在，列出前20个 .class 文件:"
                            find app/build/tmp/kotlin-classes/debug -name "*.class" | head -20
                        else
                            echo "目录 app/build/tmp/kotlin-classes/debug 不存在"
                            echo "尝试查找所有 kotlin-classes 目录:"
                            find app/build -type d -name "kotlin-classes" 2>/dev/null
                        fi
                        echo ""
                        echo "4. 执行报告生成任务 (带 --info 输出)"
                        ./gradlew :app:createDebugCombinedCoverageReport --rerun-tasks --info 2>&1 | tee /tmp/jacoco-report-info.log
                        echo ""
                        echo "5. 检查报告目录"
                        ls -R app/build/reports/jacoco/ 2>/dev/null || echo "reports/jacoco 目录不存在"
                        echo ""
                        echo "6. 打印关键日志 (过滤 jacoco 相关信息)"
                        grep -iE "jacoco|classfiles|sourcedir|report|\.exec|\.class|skipping|up-to-date" /tmp/jacoco-report-info.log | head -50
                        echo "==================== 诊断结束 ===================="
                    '''

                    // 以下步骤在诊断期间会失败是正常现象，我们只需要上面的日志
                    // 7. 增量覆盖率检查（暂时保留，会因缺少报告而失败）
                    sh '''
                        TARGET_BRANCH="origin/main"
                        diff-cover app/build/reports/jacoco/createDebugCombinedCoverageReport/createDebugCombinedCoverageReport.xml \\
                            --compare-branch $TARGET_BRANCH \\
                            --fail-under=80 \\
                            --src-roots "src/main/java"
                    '''

                    // 8. 全量覆盖率验证（暂时保留，会因缺少报告而失败）
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

                    // 9. 构建 Release 包（签名信息由环境变量提供）
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
