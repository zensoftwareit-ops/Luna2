FROM amazoncorretto:17

# Set labels
LABEL maintainer="Luna2 Development Team"
LABEL version="1.0.0"
LABEL description="Luna2 REST API - Production-Ready"

# Install curl for healthcheck
RUN yum update -y && yum install -y \
    curl \
    && yum clean all

# Create app user
RUN useradd -m -u 1000 luna2 && \
    mkdir -p /var/log/luna2 && \
    chown -R luna2:luna2 /var/log/luna2

# Set working directory
WORKDIR /app

# Copy built application
COPY target/luna2-api-1.0.0.jar luna2-api.jar
COPY src/main/resources/application.properties /config/application.properties

# Create symbolic link for logs
RUN ln -s /var/log/luna2 /app/logs

# Set ownership
RUN chown -R luna2:luna2 /app

# Switch to non-root user
USER luna2

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD curl -f http://localhost:8080/api/v1/health || exit 1

# Run application
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "luna2-api.jar"]
