pipeline {
    agent any
    
    environment {
        // --- CONFIGURATION ---
        DOCKER_USER = 'izkybin'
        DOCKER_CRED_ID = 'dockerhub-id'
        
        IMAGE_BACK = "${DOCKER_USER}/toolrent-backend"
        IMAGE_FRONT = "${DOCKER_USER}/toolrent-frontend"
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Test Backend (JUnit)') {
            steps {
                dir('BackCore') {
                    sh 'chmod +x mvnw'
                    sh './mvnw clean package'
                }
            }
        }

        stage('Build & Push Backend') {
            steps {
                dir('BackCore') {
                    script {
                        docker.withRegistry('', DOCKER_CRED_ID) {
                            // Versioned tag (traceable, allows rollback) + latest (used by compose.yml)
                            def app = docker.build("${IMAGE_BACK}:${env.BUILD_NUMBER}")
                            app.push()
                            app.push('latest')
                        }
                    }
                }
            }
        }

        stage('Build & Push Frontend') {
            steps {
                dir('CoreBack-frontend') {
                    script {
                        docker.withRegistry('', DOCKER_CRED_ID) {
                            def app = docker.build("${IMAGE_FRONT}:${env.BUILD_NUMBER}")
                            app.push()
                            app.push('latest')
                        }
                    }
                }
            }
        }
    }
}