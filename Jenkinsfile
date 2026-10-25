pipeline {
    agent any

    options {
        timestamps()
        timeout(time: 30, unit: 'MINUTES')
        disableConcurrentBuilds(abortPrevious: true)
        buildDiscarder(logRotator(numToKeepStr: '15', artifactNumToKeepStr: '5'))
    }

    parameters {
        booleanParam(name: 'SKIP_TESTS', defaultValue: false, description: 'Bỏ qua unit test (chỉ dùng khi khẩn cấp)')
        booleanParam(name: 'DEPLOY', defaultValue: true, description: 'Deploy sau khi build (chỉ áp dụng cho nhánh main)')
    }

    environment {
        DOCKER_BUILDKIT = '1'
        REGISTRY        = credentials('registry-host')        // Secret text: vd docker.io/<user>
        IMAGE           = "${REGISTRY}/da-backend"
        APP_PORT        = '8083'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
                script {
                    env.GIT_SHORT = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
                    env.IMAGE_TAG = "${env.GIT_SHORT}-${env.BUILD_NUMBER}"
                }
            }
        }

        stage('Build & Test') {
            // Chạy trong container Maven, cache ~/.m2 bằng volume để các lần build sau không tải lại dependency
            agent {
                docker {
                    image 'maven:3.9-eclipse-temurin-21-alpine'
                    args  '-v maven-repo:/root/.m2'
                    reuseNode true
                }
            }
            environment {
                MAVEN_OPTS = '-Xmx1g'
            }
            steps {
                script {
                    def goal = params.SKIP_TESTS ? 'package -DskipTests' : 'verify'
                    sh "mvn -B -ntp -T 1C ${goal}"
                }
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: 'target/surefire-reports/*.xml'
                }
                success {
                    archiveArtifacts artifacts: 'target/*.jar', fingerprint: true, onlyIfSuccessful: true
                }
            }
        }

        stage('Docker Build & Push') {
            when {
                anyOf { branch 'main'; branch 'admin'; branch 'develop' }
            }
            steps {
                withCredentials([usernamePassword(credentialsId: 'registry-creds',
                                                  usernameVariable: 'REG_USER',
                                                  passwordVariable: 'REG_PASS')]) {
                    sh '''
                        echo "$REG_PASS" | docker login "${REGISTRY%%/*}" -u "$REG_USER" --password-stdin
                        # Dùng lại layer từ image trước để tăng tốc
                        docker pull "$IMAGE:latest" || true
                        docker build \
                            --cache-from "$IMAGE:latest" \
                            --build-arg BUILDKIT_INLINE_CACHE=1 \
                            -t "$IMAGE:$IMAGE_TAG" -t "$IMAGE:latest" .
                        docker push "$IMAGE:$IMAGE_TAG"
                        docker push "$IMAGE:latest"
                    '''
                }
            }
            post {
                always {
                    sh 'docker logout "${REGISTRY%%/*}" || true'
                }
            }
        }

        stage('Deploy') {
            when {
                allOf { branch 'main'; expression { params.DEPLOY } }
            }
            steps {
                withCredentials([
                    sshUserPrivateKey(credentialsId: 'deploy-ssh', keyFileVariable: 'SSH_KEY', usernameVariable: 'SSH_USER'),
                    string(credentialsId: 'deploy-host', variable: 'DEPLOY_HOST'),
                    file(credentialsId: 'da-backend-env', variable: 'ENV_FILE'),         // nội dung giống .env
                    file(credentialsId: 'da-backend-gcloud', variable: 'GCLOUD_JSON')    // service account GCP
                ]) {
                    sh '''
                        SSH="ssh -i $SSH_KEY -o StrictHostKeyChecking=accept-new $SSH_USER@$DEPLOY_HOST"
                        SCP="scp -i $SSH_KEY -o StrictHostKeyChecking=accept-new"

                        $SSH "mkdir -p ~/da-backend"
                        $SCP "$ENV_FILE"    "$SSH_USER@$DEPLOY_HOST:~/da-backend/.env"
                        $SCP "$GCLOUD_JSON" "$SSH_USER@$DEPLOY_HOST:~/da-backend/gcloud.json"

                        $SSH "IMAGE=$IMAGE:$IMAGE_TAG APP_PORT=$APP_PORT bash -s" <<'EOF'
set -e
cd ~/da-backend
chmod 600 .env gcloud.json
docker pull "$IMAGE"
PREV=$(docker inspect -f '{{.Config.Image}}' da-backend 2>/dev/null || true)
docker rm -f da-backend 2>/dev/null || true
docker run -d --name da-backend --restart unless-stopped \
    --env-file .env \
    -e GCS_CREDENTIALS_FILE=/app/gcloud.json \
    -v "$PWD/gcloud.json:/app/gcloud.json:ro" \
    -v "$PWD/logs:/app/logs" \
    -p "$APP_PORT:$APP_PORT" "$IMAGE"

# Health check, rollback về image cũ nếu lỗi
for i in $(seq 1 30); do
    if curl -fs "http://localhost:$APP_PORT/actuator/health" >/dev/null 2>&1 || \
       [ "$(curl -s -o /dev/null -w '%{http_code}' http://localhost:$APP_PORT/)" != "000" ]; then
        echo "App is up"; docker image prune -f >/dev/null; exit 0
    fi
    sleep 5
done
echo "Health check failed" >&2
docker logs --tail 50 da-backend >&2 || true
if [ -n "$PREV" ]; then
    docker rm -f da-backend
    docker run -d --name da-backend --restart unless-stopped --env-file .env \
        -e GCS_CREDENTIALS_FILE=/app/gcloud.json -v "$PWD/gcloud.json:/app/gcloud.json:ro" \
        -v "$PWD/logs:/app/logs" -p "$APP_PORT:$APP_PORT" "$PREV"
    echo "Rolled back to $PREV" >&2
fi
exit 1
EOF
                    '''
                }
            }
        }
    }

    post {
        always {
            sh 'docker image prune -f --filter "until=72h" || true'
            cleanWs(deleteDirs: true, notFailBuild: true)
        }
        success { echo "OK: ${env.IMAGE}:${env.IMAGE_TAG}" }
        failure { echo "Build #${env.BUILD_NUMBER} thất bại" }
    }
}
