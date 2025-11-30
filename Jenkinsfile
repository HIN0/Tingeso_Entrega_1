pipeline {
    agent any
    
    environment {
        // --- CONFIGURACIÓN ---
        DOCKER_USER = 'izkybin' 
        
        // El ID de credencial (Paso 1)
        DOCKER_CRED_ID = '41f8f2cae91743e797fcf746c808f624'
        
        // Nombres de las imágenes
        IMAGE_BACK = "${DOCKER_USER}/toolrent-backend"
        IMAGE_FRONT = "${DOCKER_USER}/toolrent-frontend"
    }

    stages {
        stage('Descargar Código') {
            steps {
                // Descarga el código fresco desde GitHub
                checkout scm
            }
        }

        stage('Test Backend (JUnit)') {
            steps {
                dir('backend') {
                    // Damos permisos al ejecutable de Maven y testeamos
                    sh 'chmod +x mvnw'
                    sh './mvnw clean test'
                }
            }
        }

        stage('Build & Push Backend') {
            steps {
                dir('backend') {
                    script {
                        docker.withRegistry('', DOCKER_CRED_ID) {
                            // Crea la imagen y la sube
                            def app = docker.build("${IMAGE_BACK}:latest")
                            app.push()
                        }
                    }
                }
            }
        }

        stage('Build & Push Frontend') {
            steps {
                dir('frontend') {
                    script {
                        docker.withRegistry('', DOCKER_CRED_ID) {
                            // Crea la imagen y la sube
                            def app = docker.build("${IMAGE_FRONT}:latest")
                            app.push()
                        }
                    }
                }
            }
        }
    }
}