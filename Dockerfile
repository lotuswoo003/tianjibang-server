# syntax=docker/dockerfile:1

# ===== Builder stage =====
FROM maven:3.9.8-eclipse-temurin-17 AS builder
WORKDIR /app

# Leverage caching for dependencies
COPY pom.xml .
RUN mvn -B -q -e -DskipTests dependency:go-offline

# Copy sources and build
COPY src ./src
RUN mvn -B -DskipTests package

# ===== Runtime stage =====
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Add non-root user
RUN addgroup -S app && adduser -S app -G app
USER app

ENV JAVA_OPTS=""
ENV SERVER_PORT=8080
EXPOSE 8080

# Copy fat jar
COPY --from=builder /app/target/tinajibang-server-*.jar /app/app.jar

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]

