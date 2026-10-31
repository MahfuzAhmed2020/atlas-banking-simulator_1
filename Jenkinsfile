pipeline {
    agent any
    tools { jdk 'JDK21'; maven 'Maven3' }
    stages {
        stage('Backend test') {
            steps { dir('backend') { bat 'mvn -B clean test' } }
        }
        stage('Single-origin frontend build') {
            steps {
                powershell '''
                    & powershell -NoProfile -ExecutionPolicy Bypass -File scripts/build-single-origin.ps1
                    if ($LASTEXITCODE -ne 0) { throw 'Single-origin build failed' }
                '''
            }
        }
        stage('Package application') {
            steps {
                dir('backend') { bat 'mvn -B -DskipTests package' }
                powershell '''
                    New-Item -ItemType Directory -Force release | Out-Null
                    Copy-Item backend/target/atlas-banking-0.1.0.jar release/atlas-banking.jar -Force
                    & powershell -NoProfile -ExecutionPolicy Bypass -File scripts/package.ps1
                    if ($LASTEXITCODE -ne 0) { throw 'Source ZIP packaging failed' }
                '''
            }
        }
    }
    post {
        always { junit allowEmptyResults: true, testResults: 'backend/target/surefire-reports/*.xml' }
        success { archiveArtifacts artifacts: 'release/*.jar,release/*.zip', fingerprint: true }
    }
}
