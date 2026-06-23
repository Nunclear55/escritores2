pipeline {
    agent any

    environment {
        IMAGE_NAME = 'escritores-backend'
        CONTAINER_NAME = 'escritores-backend'
        DOCKER_NETWORK = 'escritores-net'
        APP_PORT = '8080'
        MYSQL_HOST = 'escritores-mysql'
        MYSQL_DATABASE = 'historias_db'
        DB_USERNAME = 'root'
        DB_PASSWORD = credentials('mysql-root-password')
        JWT_SECRET = credentials('jwt-secret')
        SONAR_TOKEN = credentials('sonar-token')
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Test and Coverage') {
            steps {
                sh 'chmod +x mvnw'
                sh './mvnw -B clean verify'
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: 'target/surefire-reports/*.xml'
                }
            }
        }

        stage('SonarCloud') {
            steps {
                sh './mvnw -B sonar:sonar -Dsonar.token=$SONAR_TOKEN'
            }
        }

        stage('Build Docker Image') {
            steps {
                sh 'docker build -t $IMAGE_NAME:latest .'
            }
        }

        stage('Deploy Backend') {
            steps {
                sh '''
                    docker network create $DOCKER_NETWORK || true

                    docker stop $CONTAINER_NAME || true
                    docker rm $CONTAINER_NAME || true

                    docker run -d \
                      --name $CONTAINER_NAME \
                      --restart always \
                      --network $DOCKER_NETWORK \
                      -p $APP_PORT:8080 \
                      -e DB_URL="jdbc:mysql://$MYSQL_HOST:3306/$MYSQL_DATABASE?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true" \
                      -e DB_USERNAME="$DB_USERNAME" \
                      -e DB_PASSWORD="$DB_PASSWORD" \
                      -e JWT_SECRET="$JWT_SECRET" \
                      $IMAGE_NAME:latest
                '''
            }
        }
    }

    post {
        success {
            echo 'Backend desplegado correctamente.'
        }
        failure {
            echo 'Falló el pipeline del backend.'
        }
    }
}
