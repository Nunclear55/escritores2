pipeline {
    agent any

    environment {
        IMAGE_NAME = 'escritores-backend'
        CONTAINER_NAME = 'escritores-backend'
        DOCKER_NETWORK = 'escritores-net'
        APP_PORT = '8080'
        MYSQL_HOST = 'escritores-mysql'
        MYSQL_DATABASE = 'historias_db'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Test') {
            steps {
                sh 'chmod +x mvnw'
                sh './mvnw test -Dmaven.test.failure.ignore=true'
            }
        }

        stage('Build JAR') {
            steps {
                sh './mvnw clean package -DskipTests'
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
                      -e SPRING_DATASOURCE_URL="jdbc:mysql://$MYSQL_HOST:3306/$MYSQL_DATABASE?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true" \
                      -e SPRING_DATASOURCE_USERNAME="root" \
                      -e SPRING_DATASOURCE_PASSWORD="RootPasswordSeguro123" \
                      -e JWT_SECRET="CambiaEstaClaveJWTMuySeguraDeMasDe32Caracteres123456" \
                      $IMAGE_NAME:latest
                '''
            }
        }
    }

    post {
        success {
            echo 'Backend desplegado correctamente en http://100.54.216.197:8080'
        }
        failure {
            echo 'Falló el pipeline del backend.'
        }
    }
}
