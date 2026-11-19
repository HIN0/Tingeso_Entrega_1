# Jenkins for the ToolRent CI/CD pipeline (local use).
# - JDK 17: runs the backend tests (mvnw) inside the pipeline
# - Docker CLI + buildx: builds and pushes the images using the host Docker engine (shared socket)
# - Plugins required by the Jenkinsfile
FROM jenkins/jenkins:lts-jdk17

USER root
# Static Docker CLI and its plugins (buildx, compose) taken from the official docker:cli image
COPY --from=docker:cli /usr/local/bin/docker /usr/local/bin/docker
COPY --from=docker:cli /usr/local/libexec/docker/cli-plugins /usr/local/libexec/docker/cli-plugins

USER jenkins
# git: checkout from GitHub | workflow-aggregator: Pipeline jobs (Jenkinsfile)
# docker-workflow: docker.build / docker.withRegistry | credentials-binding: Docker Hub credentials
RUN jenkins-plugin-cli --plugins git workflow-aggregator docker-workflow credentials-binding
