FROM gradle:8.5-jdk17-alpine AS builder
WORKDIR /app

# Copy root Gradle files
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle

# Copy service source
COPY order-service ./order-service

RUN chmod +x gradlew
RUN ./gradlew :order-service:build -x test --no-daemon

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/order-service/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
