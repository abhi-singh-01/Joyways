# Build stage
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests clean package

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# wget is required for HEALTHCHECK. Alpine JRE images do not include it by default,
# so without this the probe always fails and the container never becomes healthy.
RUN apk add --no-cache wget \
    && addgroup -S joyway && adduser -S joyway -G joyway

COPY --from=build /app/target/*.jar app.jar

# /app/storage supports local dev (STORAGE_PROVIDER=local) or one-off migration runs.
# Production uses Azure Blob (STORAGE_PROVIDER=azure); credentials are injected at runtime, never baked in.
RUN mkdir -p /app/storage && chown -R joyway:joyway /app

USER joyway

ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080

# Readiness (not liveness) so an instance that cannot reach the database is
# reported unhealthy instead of silently serving failing requests.
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget -qO- http://127.0.0.1:8080/api/health/ready || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
