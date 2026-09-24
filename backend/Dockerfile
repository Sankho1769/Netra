# NETRA Backend - Production Container Configuration
FROM eclipse-temurin:21-jre-alpine AS runtime

# Install curl for health check probing and tzdata for timezone management
RUN apk add --no-cache curl tzdata

# Create dedicated unprivileged service account
RUN addgroup -S netra && adduser -S netra -G netra

WORKDIR /app

# Application artifact
ARG JAR_FILE=target/netra-backend-*.jar
COPY ${JAR_FILE} app.jar

# Enforce least privilege filesystem permissions
RUN chown -R netra:netra /app

USER netra:netra

EXPOSE 8080

# Container JVM optimization:
# - G1GC for low-pause predictable garbage collection under load
# - Container-aware RAM percentages (avoids hardcoded max heap)
# - Exit on OOM so container orchestrator (Kubernetes/ECS/Docker) restarts instance
# - Secure random generation and standard UTC timezone
ENV JAVA_OPTS="-XX:+UseG1GC \
    -XX:InitialRAMPercentage=50.0 \
    -XX:MaxRAMPercentage=75.0 \
    -XX:+ExitOnOutOfMemoryError \
    -Djava.security.egd=file:/dev/./urandom \
    -Duser.timezone=UTC"

# Actuator liveness probe health check
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health/liveness || exit 1

# exec ensures java runs as PID 1 to trap SIGTERM for graceful shutdown
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
