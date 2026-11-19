# 1. Java 21 runtime (JRE: running the JAR does not need a compiler, so the image is smaller)
FROM eclipse-temurin:21-jre-alpine

# 2. JAR built by "mvnw package" (in target/)
ARG JAR_FILE=target/*.jar

# 3. Copy the JAR into the image as app.jar
COPY ${JAR_FILE} app.jar

# 4. Port of the Spring Boot application
EXPOSE 8080

# 5. Start the application
ENTRYPOINT ["java", "-jar", "/app.jar"]
