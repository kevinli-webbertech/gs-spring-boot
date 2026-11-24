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

        // Nexus Docker (hosted) repository connector. This must match the HTTP port
        // configured for the Docker hosted repository in Nexus (Repository Settings >
        // HTTP), not the 8081 port used for the Maven repositories above.
        NEXUS_DOCKER_REGISTRY = 'nexus:8083'
        IMAGE_NAME            = 'spring-boot-complete'

        // Name/namespace of the Kubernetes Deployment that runs the webapp Pod; must
        // match the webapp-deployment.yaml applied to the Minikube cluster.
        K8S_DEPLOYMENT  = 'webapp'
        K8S_NAMESPACE   = 'default'
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

        stage('Build Container Image') {
            steps {
                // Reuses the repo's Dockerfile (multi-stage Maven build + JRE runtime)
                // and tags the image for the Nexus Docker hosted repository.
                sh "docker build -t ${NEXUS_DOCKER_REGISTRY}/${IMAGE_NAME}:${BUILD_NUMBER} ."
            }
        }

        stage('Push Image to Nexus') {
            steps {
                withCredentials([usernamePassword(
                    credentialsId: 'nexus-credentials',
                    usernameVariable: 'NEXUS_USER',
                    passwordVariable: 'NEXUS_PASSWORD'
                )]) {
                    sh """
                        echo "\$NEXUS_PASSWORD" | docker login ${NEXUS_DOCKER_REGISTRY} -u "\$NEXUS_USER" --password-stdin
                        docker push ${NEXUS_DOCKER_REGISTRY}/${IMAGE_NAME}:${BUILD_NUMBER}
                    """
                }
            }
        }

        stage('Deploy to Kubernetes') {
            steps {
                // Jenkins > Manage Jenkins > Credentials: a "Secret file" credential
                // holding a kubeconfig with access to the Minikube cluster must exist
                // with this ID. set image triggers a rolling update of the webapp
                // Deployment to the freshly pushed image, then we wait for it to finish.
                withCredentials([file(credentialsId: 'kubeconfig-credentials', variable: 'KUBECONFIG')]) {
                    sh """
                        kubectl --kubeconfig="\$KUBECONFIG" set image deployment/${K8S_DEPLOYMENT} ${K8S_DEPLOYMENT}=${NEXUS_DOCKER_REGISTRY}/${IMAGE_NAME}:${BUILD_NUMBER} -n ${K8S_NAMESPACE}
                        kubectl --kubeconfig="\$KUBECONFIG" rollout status deployment/${K8S_DEPLOYMENT} -n ${K8S_NAMESPACE} --timeout=180s
                    """
                }
            }
        }
    }

    post {
        always {
            echo 'Pipeline finished.'
            sh 'rm -f nexus-settings.xml'
            sh "docker logout ${NEXUS_DOCKER_REGISTRY} || true"
        }
        success {
            echo 'Build successful!'
        }
        failure {
            echo 'Build failed!'
        }
    }
}
