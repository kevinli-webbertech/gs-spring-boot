pipeline {
    agent any // Specifies that the pipeline can run on any available agent

    options {
        skipStagesAfterUnstable()
    }

    tools {
        maven '3.9.11'
    }

    environment {
        // Nexus repository ids must match pom.xml's <distributionManagement> server ids,
        // and the <server> entries in the settings.xml generated below.
        NEXUS_RELEASE_REPO_ID  = 'nexus-releases'
        NEXUS_SNAPSHOT_REPO_ID = 'nexus-snapshots'
    }

    stages {
        stage('Checkout Source Code') {
            steps {
                checkout scm
            }
        }

        stage('Test') {
            steps {
                sh 'mvn clean test'
            }
        }

        stage('Build and Package') {
            steps {
                sh 'mvn clean package -DskipTests'
            }
        }

        stage('Archive Artifacts') {
            steps {
                archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
            }
        }

        stage('Deploy to Nexus') {
            steps {
                // Jenkins > Manage Jenkins > Credentials: a "Username with password"
                // credential with this ID must exist for the Nexus deploy user.
                withCredentials([usernamePassword(
                    credentialsId: 'nexus-credentials',
                    usernameVariable: 'NEXUS_USER',
                    passwordVariable: 'NEXUS_PASSWORD'
                )]) {
                    // `-Dusername/-Dpassword` are not honored by the Maven deploy plugin;
                    // credentials must come from a settings.xml <server> entry matching
                    // the repository id, so we generate one for this build only.
                    writeFile file: 'nexus-settings.xml', text: """<settings>
  <servers>
    <server>
      <id>${NEXUS_RELEASE_REPO_ID}</id>
      <username>${NEXUS_USER}</username>
      <password>${NEXUS_PASSWORD}</password>
    </server>
    <server>
      <id>${NEXUS_SNAPSHOT_REPO_ID}</id>
      <username>${NEXUS_USER}</username>
      <password>${NEXUS_PASSWORD}</password>
    </server>
  </servers>
</settings>"""
                    sh 'mvn deploy -DskipTests -s nexus-settings.xml'
                }
            }
        }
    }

    post {
        always {
            echo 'Pipeline finished.'
            sh 'rm -f nexus-settings.xml'
        }
        success {
            echo 'Build successful!'
        }
        failure {
            echo 'Build failed!'
        }
    }
}
