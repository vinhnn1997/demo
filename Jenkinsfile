pipeline {
    agent any

    parameters {
        choice(name: 'DEPLOY_ENVIRONMENT', choices: ['dev', 'uat', 'prod'], description: 'Deployment environment')
        string(name: 'REGISTRY', defaultValue: 'ghcr.io', description: 'Container registry host')
        string(name: 'IMAGE_REPOSITORY', defaultValue: 'ghcr.io/your-org/tax-platform', description: 'Docker image repository')
        choice(name: 'BUILD_SERVICE', choices: ['all', 'eureka-server', 'api-gateway', 'province-service', 'organization-service', 'taxpayer-service', 'fine-service', 'payment-service', 'identity-service', 'dataplatform'], description: 'Build and deploy only this service, or all services')
        string(name: 'COMMON_MAVEN_REPOSITORY_URL', defaultValue: '', description: 'Maven repository URL for common-library')
        booleanParam(name: 'PUBLISH_COMMON_LIBRARY', defaultValue: false, description: 'Publish the current common-library version before building services')
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
                deleteDir()
                checkout scm
            }
        }

        stage('Publish Common Library') {
            when {
                expression { params.PUBLISH_COMMON_LIBRARY }
            }
            steps {
                script {
                    if (!params.COMMON_MAVEN_REPOSITORY_URL?.trim()) {
                        error 'COMMON_MAVEN_REPOSITORY_URL is required to publish common-library'
                    }
                }
                withCredentials([file(credentialsId: 'maven-settings', variable: 'MAVEN_SETTINGS')]) {
                    withEnv(["COMMON_MAVEN_REPOSITORY_URL=${params.COMMON_MAVEN_REPOSITORY_URL}"]) {
                        sh 'mvn -s "$MAVEN_SETTINGS" -B -f common-library/pom.xml clean deploy -DaltDeploymentRepository="common-library::${COMMON_MAVEN_REPOSITORY_URL}"'
                    }
                }
            }
        }

        stage('Test') {
            steps {
                script {
                    if (!params.COMMON_MAVEN_REPOSITORY_URL?.trim()) {
                        error 'COMMON_MAVEN_REPOSITORY_URL is required to resolve common-library'
                    }
                    def services = params.BUILD_SERVICE == 'all'
                        ? ['eureka-server', 'api-gateway', 'province-service', 'organization-service', 'taxpayer-service', 'fine-service', 'payment-service', 'identity-service', 'dataplatform']
                        : [params.BUILD_SERVICE]
                    withCredentials([file(credentialsId: 'maven-settings', variable: 'MAVEN_SETTINGS')]) {
                        withEnv(["COMMON_MAVEN_REPOSITORY_URL=${params.COMMON_MAVEN_REPOSITORY_URL}"]) {
                            services.each { service ->
                                sh "mvn -s \"$MAVEN_SETTINGS\" -B -f ${service}/pom.xml test"
                            }
                        }
                    }
                }
            }
        }

        stage('Build and Push Images') {
            steps {
                script {
                    if (!params.COMMON_MAVEN_REPOSITORY_URL?.trim()) {
                        error 'COMMON_MAVEN_REPOSITORY_URL is required to build service images'
                    }
                }
                withCredentials([file(credentialsId: 'maven-settings', variable: 'MAVEN_SETTINGS')]) {
                    withEnv(["COMMON_MAVEN_REPOSITORY_URL=${params.COMMON_MAVEN_REPOSITORY_URL}"]) {
                        sh '''
                            set -eu
                            export DOCKER_BUILDKIT=1
                            echo "$REGISTRY_CREDENTIALS_PSW" | docker login "$REGISTRY" \\
                              --username "$REGISTRY_CREDENTIALS_USR" --password-stdin
                            services="$BUILD_SERVICE"
                            if [ "$services" = all ]; then
                              services="eureka-server api-gateway province-service organization-service taxpayer-service fine-service payment-service identity-service dataplatform"
                            fi
                            for service in $services; do
                              docker build --secret id=maven_settings,src="$MAVEN_SETTINGS" \\
                                --build-arg COMMON_MAVEN_REPOSITORY_URL="$COMMON_MAVEN_REPOSITORY_URL" \\
                                -f "$service/Dockerfile" -t "$IMAGE_REPOSITORY/$service:$IMAGE_TAG" .
                              docker push "$IMAGE_REPOSITORY/$service:$IMAGE_TAG"
                            done
                            docker logout "$REGISTRY"
                        '''
                    }
                }
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
                                                    "chmod +x '$DEPLOY_PATH/deploy-compose.sh' && '$DEPLOY_PATH/deploy-compose.sh' '$DEPLOY_PATH' '$IMAGE_REPOSITORY' '$IMAGE_TAG' '$BUILD_SERVICE'"
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
