pipeline {
    agent any

    environment {
        ANDROID_HOME = '/opt/android-sdk'
        PATH = "${ANDROID_HOME}/platform-tools:${ANDROID_HOME}/cmdline-tools/latest/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"
        GRADLE_OPTS = '-Xmx4g -Dfile.encoding=UTF-8'
        CI = 'true'
    }

    stages {
        stage('Checkout') {
            steps { checkout scm }
        }

        stage('Setup Signing & Build') {
            steps {
                withCredentials([
                    file(credentialsId: 'KEYSTORE_FILE', variable: 'KEYSTORE_FILE'),
                    string(credentialsId: 'KEYSTORE_PWD', variable: 'KEYSTORE_PWD'),
                    string(credentialsId: 'KEY_PWD', variable: 'KEY_PWD')
                ]) {
                    sh './gradlew clean --no-configuration-cache'
                    sh './gradlew spotlessCheck'
                    sh './gradlew lint'
                    sh './gradlew detekt'
                    sh './gradlew dependencyCheckAnalyze'
                    sh './gradlew assembleDebug'
                    sh './gradlew createDebugCombinedCoverageReport'
                    sh '''
                        TARGET_BRANCH="origin/main"
                        diff-cover app/build/reports/jacoco/createDebugCombinedCoverageReport/createDebugCombinedCoverageReport.xml \
                            --compare-branch $TARGET_BRANCH \
                            --fail-under=80 \
                            --src-roots "src/main/java"
                    '''
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
                    sh './gradlew assembleRelease'
                }
            }
        }
    }
    post {
        always { cleanWs() }
    }
}
