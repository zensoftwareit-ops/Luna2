FROM tomcat:9-jre17

# Set labels
LABEL maintainer="Luna2 Development Team"
LABEL version="1.0.0"
LABEL description="Luna2 WAR Application - Production-Ready"

# Install curl for healthcheck
RUN apt-get update && apt-get install -y --no-install-recommends \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Create luna2 log directory
RUN mkdir -p /var/log/luna2 && \
    chmod 755 /var/log/luna2

# Remove default webapps to reduce image size
RUN rm -rf /usr/local/tomcat/webapps/*

# Copy the compiled WAR from parent directory
COPY ../target/luna2.war /usr/local/tomcat/webapps/ROOT.war

# Copy Tomcat configuration (if exists)
COPY server.xml /usr/local/tomcat/conf/server.xml 2>/dev/null || true

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/ || exit 1
