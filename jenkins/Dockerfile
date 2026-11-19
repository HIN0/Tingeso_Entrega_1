# Jenkins for the ToolRent CI/CD pipeline (local use).
# - JDK 21: required by current Jenkins releases; it also runs the backend tests (mvnw),
#   which target Java 17 (a newer JDK compiles and runs older targets)
# - Docker CLI + buildx: builds and pushes the images using the host Docker engine (shared socket)
# - Plugins required by the Jenkinsfile
# Update Jenkins by rebuilding this image, not from the web UI (see README.md)
FROM jenkins/jenkins:lts-jdk21

USER root
# Static Docker CLI and its plugins (buildx, compose) taken from the official docker:cli image
COPY --from=docker:cli /usr/local/bin/docker /usr/local/bin/docker
COPY --from=docker:cli /usr/local/libexec/docker/cli-plugins /usr/local/libexec/docker/cli-plugins

USER jenkins
# git: checkout from GitHub | workflow-aggregator: Pipeline jobs (Jenkinsfile)
# docker-workflow: docker.build / docker.withRegistry | credentials-binding: Docker Hub credentials
RUN jenkins-plugin-cli --plugins git workflow-aggregator docker-workflow credentials-binding
