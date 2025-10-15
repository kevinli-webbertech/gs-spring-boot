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
                // Checkout the source code from your Git repository
                git branch: 'main', url: 'https://github.com/kevinli-webbertech/gs-spring-boot.git' // Replace with your repo details
            }
        }

        stage('Build and Package') {
            steps {
                // Build the Spring Boot application and package it into a JAR
                // -DskipTests skips running tests during the build, useful for quick builds.
                // For production, consider running tests in a separate 'Test' stage.
                sh 'mvn clean package -DskipTests'
            }
        }

        stage('Archive Artifacts') {
            steps {
                // Archive the generated JAR file for later use or deployment
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
