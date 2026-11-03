
# ==============================
# Stage 1: Build Application
# ==============================
FROM gradle:8.14-jdk21 AS builder

WORKDIR /app

# Copy Gradle configuration first
COPY gradlew .
COPY gradle ./gradle
COPY build.gradle.kts settings.gradle.kts ./

# Ensure Gradle wrapper is executable
RUN chmod +x gradlew

# Download dependencies
RUN ./gradlew dependencies --no-daemon || true

# Copy source code
COPY src ./src

# Build Spring Boot application
RUN ./gradlew clean bootJar --no-daemon

# ==============================
# Stage 2: Run Application
# ==============================
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copy generated JAR
COPY --from=builder /app/build/libs/*.jar app.jar

# Render provides PORT environment variable
ENV PORT=8080

EXPOSE 8080

# Start application
ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT:-8080} -jar app.jar"]
