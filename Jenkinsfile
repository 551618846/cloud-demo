pipeline {
    agent any

    environment {
        PROJECT_DIR = 'd:/git_rep/cloud-demo/cloud-demo'
        COMPOSE_FILE = 'docker-compose-app.yml'
    }

    stages {
        stage('Checkout') {
            steps {
                echo 'Checkout code...'
                checkout scm
            }
        }

        stage('Build') {
            steps {
                echo 'Building project...'
                sh 'mvn clean package -DskipTests'
            }
        }

        stage('Build Docker Images') {
            steps {
                echo 'Building Docker images...'
                sh "docker compose -f ${PROJECT_DIR}/${COMPOSE_FILE} build"
            }
        }

        stage('Deploy') {
            steps {
                echo 'Deploying services...'
                sh "docker compose -f ${PROJECT_DIR}/${COMPOSE_FILE} down"
                sh "docker compose -f ${PROJECT_DIR}/${COMPOSE_FILE} up -d"
            }
        }

        stage('Health Check') {
            steps {
                echo 'Checking services health...'
                sh 'sleep 30'
                sh 'docker ps'
            }
        }
    }

    post {
        success {
            echo 'Deployment successful!'
        }
        failure {
            echo 'Deployment failed!'
        }
        always {
            cleanWs()
        }
    }
}
