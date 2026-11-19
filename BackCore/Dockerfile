# 1. Java 17 runtime base image
FROM eclipse-temurin:17-jdk-alpine

# 2. JAR built by "mvnw package" (in target/)
ARG JAR_FILE=target/*.jar

# 3. Copy the JAR into the image as app.jar
COPY ${JAR_FILE} app.jar

# 4. Port of the Spring Boot application
EXPOSE 8080

# 5. Start the application
ENTRYPOINT ["java", "-jar", "/app.jar"]
