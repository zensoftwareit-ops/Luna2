#!/bin/bash

# ============================================================================
# Initialize Server Management Setup
# Creates necessary directories and files for multi-domain management
# ============================================================================

set -e

echo "🔧 Initializing server management components..."

# Create required directories
mkdir -p docker/nginx/vhosts
mkdir -p docker/nginx/certs
mkdir -p docker/nginx/ssl
mkdir -p logs/nginx
mkdir -p logs/api
mkdir -p logs/mysql

# Initialize domain tracking file
if [ ! -f docker/nginx/domains.txt ]; then
    touch docker/nginx/domains.txt
    echo "✓ Created domain tracking file"
fi

# Create default nginx config
if [ ! -f docker/nginx/certs/README.txt ]; then
    cat > docker/nginx/certs/README.txt << 'EOF'
# SSL Certificates for Luna2

This directory contains SSL certificates for Let's Encrypt domains.
Certificates are automatically copied here when generated.

Structure:
  domain.com/
    - fullchain.pem   (combined cert + chain)
    - privkey.pem     (private key)
    - cert.pem        (certificate only)
    - chain.pem       (intermediate certificates)

Self-signed certificates (test only):
  - Located in ../ssl/ directory
  - Use only for development/testing
EOF
fi

# Generate self-signed certificate for testing
if [ ! -f docker/nginx/ssl/self-signed.crt ]; then
    echo "🔐 Generating self-signed SSL certificate for testing..."
    openssl req -x509 -newkey rsa:2048 \
        -keyout docker/nginx/ssl/self-signed.key \
        -out docker/nginx/ssl/self-signed.crt \
        -days 365 -nodes \
        -subj "/C=IT/ST=Italy/L=Rome/O=Luna2/CN=localhost"
    echo "✓ Self-signed certificate created"
fi

# Make manage-domains.sh executable
if [ -f manage-domains.sh ]; then
    chmod +x manage-domains.sh
    echo "✓ manage-domains.sh made executable"
fi

# Create docker/nginx/vhosts directory with README
if [ ! -f docker/nginx/vhosts/README.txt ]; then
    cat > docker/nginx/vhosts/README.txt << 'EOF'
# Vhost Configurations

Each domain gets its own configuration file here.
Files are automatically loaded by Nginx via the main nginx.conf.

To add a new domain:
  ./manage-domains.sh add example.com

To remove a domain:
  ./manage-domains.sh remove example.com

To view all domains:
  ./manage-domains.sh list

Example vhost file structure:
  example.com.conf
    - upstream luna2_api (points to luna2-api:8080)
    - HTTP server block (port 80, redirect to HTTPS)
    - HTTPS server block (port 443 with SSL)
    - location / (proxy_pass to luna2-api)
EOF
fi

# Create basic configuration file
if [ ! -f docker/nginx/log-rotation.conf ]; then
    cat > docker/nginx/log-rotation.conf << 'EOF'
# Logrotate configuration for Luna2 Nginx logs
# 
# Install with:
#   sudo cp docker/nginx/log-rotation.conf /etc/logrotate.d/luna2-nginx
#
# Test with:
#   sudo logrotate -d /etc/logrotate.d/luna2-nginx

/workspaces/Luna2/logs/nginx/*.log {
    daily
    rotate 14
    compress
    delaycompress
    notifempty
    create 0640 nobody adm
    sharedscripts
    postrotate
        docker-compose -f /workspaces/Luna2/docker-compose-preprod.yml \
            exec nginx nginx -s reload > /dev/null 2>&1 || true
    endscript
}
EOF
fi

echo ""
echo "✅ Server management setup complete!"
echo ""
echo "📋 Next steps:"
echo "  1. Add your first domain:"
echo "     ./manage-domains.sh add yourdomain.com"
echo ""
echo "  2. Enable Portainer (optional):"
echo "     Uncomment portainer in docker-compose-preprod.yml"
echo "     docker-compose -f docker-compose-preprod.yml up -d portainer"
echo ""
echo "  3. View documentation:"
echo "     cat SERVER_MANAGEMENT_PANEL.md"
echo ""
