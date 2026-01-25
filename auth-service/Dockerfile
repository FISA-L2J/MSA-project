FROM eclipse-temurin:17-jdk-alpine
EXPOSE 8082
COPY build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]