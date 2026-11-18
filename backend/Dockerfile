# Build stage
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests -Dmaven.test.skip=true clean package

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# wget is required for HEALTHCHECK. Alpine JRE images do not include it by default,
# so without this the probe always fails and the container never becomes healthy.
RUN apk add --no-cache wget \
    && addgroup -S joyway && adduser -S joyway -G joyway

COPY --from=build /app/target/*.jar app.jar

# Ephemeral local disk (STORAGE_PROVIDER=local only). Production on Render uses R2 (s3).
RUN mkdir -p /app/storage && chown -R joyway:joyway /app

USER joyway

ENV SPRING_PROFILES_ACTIVE=prod
ENV PORT=8080
# Render free instances are ~512MB. Leave headroom for the OS.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"
EXPOSE 8080

# Readiness (not liveness) so an instance that cannot reach the database is
# reported unhealthy instead of silently serving failing requests.
HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=5 \
  CMD wget -qO- http://127.0.0.1:${PORT}/api/health/ready || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
