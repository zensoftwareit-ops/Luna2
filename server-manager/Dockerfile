FROM node:18-slim

# Set working directory
WORKDIR /app

# Install system dependencies
RUN apt-get update && apt-get install -y \
    curl \
    docker.io \
    certbot \
    && rm -rf /var/lib/apt/lists/*

# Copy application files
COPY package.json package-lock.json* ./
RUN npm install --production

COPY server.js ./
COPY public ./public

# Expose port
EXPOSE 8888

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
    CMD curl -f http://localhost:8888/api/health || exit 1

# Start server
CMD ["node", "server.js"]
