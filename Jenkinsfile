pipeline {
    agent {
        docker {
            image 'eclipse-temurin:17-jdk'
            args '-u root'
        }
    }

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
            steps {
                checkout scm
            }
        }

        stage('Grant Permissions') {
            steps {
                sh 'chmod +x ./gradlew'
            }
        }

        stage('Clean') {
            steps {
                sh './gradlew clean --no-configuration-cache'
            }
        }

        stage('Spotless Check') {
            steps {
                sh './gradlew spotlessCheck'
            }
        }

        stage('Detekt Static Check') {
            steps {
                sh './gradlew detekt'
            }
            post {
                always {
                    publishHTML([
                            allowMissing: true,
                            reportDir: 'build/reports/detekt',
                            reportFiles: 'index.html',
                            reportName: 'Detekt Report'
                    ])
                }
            }
        }

        stage('OWASP Dependency Check') {
            steps {
                sh './gradlew dependencyCheckAnalyze'
            }
            post {
                always {
                    publishHTML([
                            allowMissing: true,
                            reportDir: 'build/reports/dependency-check',
                            reportFiles: 'index.html',
                            reportName: 'OWASP Report'
                    ])
                }
            }
        }

        stage('Multi-Module Build') {
            steps {
                sh './gradlew assembleDevDebug assembleTestDebug assembleProdRelease --parallel'
            }
        }

        stage('Unit Test') {
            steps {
                sh './gradlew testDevDebug testTestDebug testProdRelease --parallel'
            }
            post {
                always {
                    junit '**/build/test-results/**/*.xml'
                }
            }
        }

        stage('JaCoCo Coverage') {
            steps {
                sh './gradlew jacocoTestReport'
            }
            post {
                always {
                    publishHTML([
                            allowMissing: true,
                            reportDir: 'build/reports/jacoco',
                            reportFiles: 'index.html',
                            reportName: 'JaCoCo Coverage Report'
                    ])
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
