pipeline {
    agent any

    parameters {
        choice(name: 'DEPLOY_ENVIRONMENT', choices: ['dev', 'uat', 'prod'], description: 'Deployment environment')
        string(name: 'REGISTRY', defaultValue: 'ghcr.io', description: 'Container registry host')
        string(name: 'IMAGE_REPOSITORY', defaultValue: 'ghcr.io/your-org/tax-platform', description: 'Docker image repository')
        string(name: 'DEPLOY_HOST', defaultValue: '', description: 'Deployment server hostname or IP')
        string(name: 'DEPLOY_USER', defaultValue: 'deploy', description: 'SSH user on deployment server')
        string(name: 'DEPLOY_PATH', defaultValue: '/opt/tax-platform', description: 'Application directory on deployment server')
    }

    environment {
        IMAGE_TAG = "${BUILD_NUMBER}"
        REGISTRY_CREDENTIALS = credentials('docker-registry')
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Test') {
            steps {
                sh 'mvn -B clean test'
            }
        }

        stage('Build and Push Images') {
            steps {
                sh '''
                    set -eu
                    echo "$REGISTRY_CREDENTIALS_PSW" | docker login "$REGISTRY" \\
                      --username "$REGISTRY_CREDENTIALS_USR" --password-stdin
                    for service in eureka-server api-gateway province-service organization-service taxpayer-service fine-service payment-service; do
                      docker build -f "$service/Dockerfile" -t "$IMAGE_REPOSITORY/$service:$IMAGE_TAG" .
                      docker push "$IMAGE_REPOSITORY/$service:$IMAGE_TAG"
                    done
                    docker logout "$REGISTRY"
                '''
            }
        }

        stage('Approve Production') {
            when {
                expression { params.DEPLOY_ENVIRONMENT == 'prod' }
            }
            steps {
                input message: 'Deploy this build to production?', ok: 'Deploy'
            }
        }

        stage('Deploy') {
            steps {
                sshagent(credentials: ['deployment-ssh']) {
                    sh '''
                        set -eu
                        test -n "$DEPLOY_HOST"
                        scp -o StrictHostKeyChecking=accept-new docker-compose.prod.yml \\
                          deploy/deploy-compose.sh "$DEPLOY_USER@$DEPLOY_HOST:$DEPLOY_PATH/"
                        ssh -o StrictHostKeyChecking=accept-new "$DEPLOY_USER@$DEPLOY_HOST" \\
                          "chmod +x '$DEPLOY_PATH/deploy-compose.sh' && '$DEPLOY_PATH/deploy-compose.sh' '$DEPLOY_PATH' '$IMAGE_REPOSITORY' '$IMAGE_TAG'"
                    '''
                }
            }
        }
    }

    post {
        always {
            junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'
        }
        cleanup {
            sh 'docker image prune -f || true'
        }
    }
}
