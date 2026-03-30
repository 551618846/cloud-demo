pipeline {
    agent none

    environment {
        PROJECT_DIR = '/var/jenkins_home/workspace/cloud-demo'
        COMPOSE_FILE = 'docker-compose-app.yml'
    }

    stages {
        stage('Checkout') {
            agent any
            steps {
                echo 'Checkout code...'
                checkout scm
                stash includes: '**/*', name: 'source'
            }
        }

        stage('Build') {
            agent {
                docker {
                    image 'maven:3.9-eclipse-temurin-21'
                    args '-v $HOME/.m2:/root/.m2'
                }
            }
            steps {
                echo 'Building project...'
                unstash 'source'
                sh 'mvn clean package -DskipTests'
                stash includes: '**/target/*.jar', name: 'artifacts'
            }
        }

        stage('Build Docker Images') {
            agent any
            steps {
                echo 'Building Docker images...'
                unstash 'source'
                unstash 'artifacts'
                sh "docker compose -f ${WORKSPACE}/${COMPOSE_FILE} build"
            }
        }

        stage('Deploy') {
            agent any
            steps {
                echo 'Deploying services...'
                sh "docker compose -f ${WORKSPACE}/${COMPOSE_FILE} down"
                sh "docker compose -f ${WORKSPACE}/${COMPOSE_FILE} up -d"
            }
        }

        stage('Health Check') {
            agent any
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
    }
}
