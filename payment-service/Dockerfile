FROM gradle:8.5-jdk17-alpine AS builder
WORKDIR /app

# Copy root Gradle files
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle

# Copy service source
COPY payment-service ./payment-service

RUN chmod +x gradlew
RUN ./gradlew :payment-service:build -x test --no-daemon

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/payment-service/build/libs/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
