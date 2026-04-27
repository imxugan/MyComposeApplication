pipeline {
    agent any   // Jenkins 宿主机已配好 Android SDK

    environment {
        GRADLE_OPTS = '-Xmx4g -Dfile.encoding=UTF-8'
        CI = 'true'
    }

    options {
        buildDiscarder(logRotator(numToKeepStr: '30'))
        disableConcurrentBuilds()
    }

    stages {
        stage('Checkout') {
            steps { checkout scm }
        }

        stage('Set gradlew executable') {
            steps { sh 'chmod +x ./gradlew' }
        }

        stage('Clean') {
            steps { sh './gradlew clean --no-configuration-cache' }
        }

        stage('Spotless Check') {
            steps { sh './gradlew spotlessCheck' }
        }

        stage('Lint') {
            steps { sh './gradlew lint' }
        }

        stage('Detekt') {
            steps { sh './gradlew detekt' }
        }

        stage('OWASP Dependency Check') {
            steps { sh './gradlew dependencyCheckAnalyze' }
        }

        stage('Debug Build') {
            steps { sh './gradlew assembleDebug' }
        }

        stage('Unit Test + Coverage Report') {
            steps {
                sh './gradlew createDebugCombinedCoverageReport'
            }
            post {
                always {
                    junit '**/build/test-results/testDebugUnitTest/**/*.xml'
                }
            }
        }

        stage('Diff Coverage (Incremental)') {
            steps {
                sh '''
                    TARGET_BRANCH="origin/main"
                    diff-cover app/build/reports/jacoco/createDebugCombinedCoverageReport/createDebugCombinedCoverageReport.xml \\
                        --compare-branch $TARGET_BRANCH \\
                        --fail-under=80 \\
                        --src-roots "src/main/java"
                '''
            }
        }

        stage('Full Coverage Verification') {
            steps {
                sh '''
                    REPORT="app/build/reports/jacoco/createDebugCombinedCoverageReport/createDebugCombinedCoverageReport.xml"
                    if [ ! -f "$REPORT" ]; then
                        echo "Coverage report not found: $REPORT"
                        exit 1
                    fi
                    # 提取 LINE 级别的覆盖率
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
                    THRESHOLD=80.0
                    if (( $(echo "$PERCENT < $THRESHOLD" | bc -l) )); then
                        echo "Coverage ${PERCENT}% is below the minimum threshold of ${THRESHOLD}%"
                        exit 1
                    fi
                '''
            }
        }

        stage('Release Build') {
            steps { sh './gradlew assembleRelease' }
        }
    }

    post {
        always {
            cleanWs()
        }
    }
}
