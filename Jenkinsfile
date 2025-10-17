pipeline {
    agent any // Specifies that the pipeline can run on any available agent

    options {
      skipStagesAfterUnstable()
    }

    tools {
         maven 'Maven 3.9.6'
    }

    stages {
        stage('Checkout Source Code') {
            steps {
                git branch: 'main', url: 'https://github.com/kevinli-webbertech/gs-spring-boot.git' 
            }
        }

        stage('Test') {
            steps {
                sh './mvnw clean test' // Example for a Maven project
            }
        }

        stage('Build and Package') {
            steps {
                sh './mvnw clean package -DskipTests'
            }
        }

        stage('Archive Artifacts') {
            steps {
                archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
            }
        }
    }

    post {
        always {
            echo 'Pipeline finished.'
        }
        success {
            echo 'Build successful!'
        }
        failure {
            echo 'Build failed!'
        }
    }
}
