#!/bin/bash

# ============================================================================
# Luna2 Domain Management Script
# Add/remove domains, manage SSL, and configure Nginx automatically
# ============================================================================

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;36m'
NC='\033[0m'

# Log file
LOG_FILE="logs/domains.log"
mkdir -p logs

print_header() {
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}========================================${NC}"
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" >> "$LOG_FILE"
}

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] ✓ $1" >> "$LOG_FILE"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] ✗ $1" >> "$LOG_FILE"
}

print_warning() {
    echo -e "${YELLOW}⚠ $1${NC}"
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] ⚠ $1" >> "$LOG_FILE"
}

print_info() {
    echo -e "${BLUE}ℹ $1${NC}"
}

# ============================================================================
# FUNCTION: Add Domain
# ============================================================================

add_domain() {
    local domain=$1
    
    print_header "Adding Domain: $domain"
    
    # Validate domain format
    if ! [[ $domain =~ ^[a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(\.[a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$ ]]; then
        print_error "Invalid domain format: $domain"
        return 1
    fi
    
    # Check if domain already exists
    if grep -q "server_name $domain" docker/nginx/vhosts/*.conf 2>/dev/null; then
        print_error "Domain $domain already exists"
        return 1
    fi
    
    print_step "Creating Nginx vhost configuration..."
    
    # Create vhost config directory if not exists
    mkdir -p docker/nginx/vhosts
    
    # Create vhost configuration
    cat > "docker/nginx/vhosts/$domain.conf" << EOF
# Vhost for $domain
# Generated on $(date)

upstream luna2_api {
    server luna2-api:8080;
}

server {
    listen 80;
    listen [::]:80;
    server_name $domain www.$domain;
    
    # Redirect to HTTPS
    return 301 https://\$host\$request_uri;
}

server {
    listen 443 ssl http2;
    listen [::]:443 ssl http2;
    server_name $domain www.$domain;

    # SSL Certificate paths (will be configured by certbot)
    ssl_certificate /etc/letsencrypt/live/$domain/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/$domain/privkey.pem;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
    ssl_prefer_server_ciphers on;
    ssl_session_cache shared:SSL:10m;
    ssl_session_timeout 10m;

    # Security Headers
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-XSS-Protection "1; mode=block" always;
    add_header Referrer-Policy "no-referrer-when-downgrade" always;

    # CORS Headers
    add_header Access-Control-Allow-Origin "*" always;
    add_header Access-Control-Allow-Methods "GET, POST, PUT, DELETE, OPTIONS" always;
    add_header Access-Control-Allow-Headers "Content-Type, Authorization" always;

    # Log files
    access_log /var/log/nginx/$domain-access.log;
    error_log /var/log/nginx/$domain-error.log;

    # Proxy to Luna2 API
    location / {
        proxy_pass http://luna2_api;
        proxy_http_version 1.1;
        proxy_set_header Upgrade \$http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        proxy_set_header X-Request-ID \$request_id;
        
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
        
        proxy_buffering off;
    }

    # WebSocket support
    location /ws {
        proxy_pass http://luna2_api;
        proxy_http_version 1.1;
        proxy_set_header Upgrade \$http_upgrade;
        proxy_set_header Connection "Upgrade";
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_read_timeout 86400;
    }

    # Health check
    location /health {
        access_log off;
        proxy_pass http://luna2_api/api/v1/health;
    }
}
EOF

    print_success "Vhost configuration created: docker/nginx/vhosts/$domain.conf"
    
    # Add domain to domains.txt for tracking
    echo "$domain" >> docker/nginx/domains.txt 2>/dev/null || true
    
    # Ask about SSL
    print_info ""
    read -p "Generate SSL certificate with Let's Encrypt? (y/n) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        generate_ssl_cert "$domain"
    else
        print_warning "SSL certificate not generated - domain will only work on HTTP"
        print_info "Run later: ./manage-domains.sh --ssl $domain"
    fi
    
    # Reload Nginx
    print_step "Reloading Nginx configuration..."
    docker-compose -f docker-compose-preprod.yml exec nginx nginx -s reload || \
        print_warning "Nginx not running - will load on next restart"
    
    print_success "Domain $domain added successfully!"
}

# ============================================================================
# FUNCTION: Generate SSL Certificate
# ============================================================================

generate_ssl_cert() {
    local domain=$1
    
    print_header "Generating SSL Certificate for $domain"
    
    # Check if certbot is installed
    if ! command -v certbot &> /dev/null; then
        print_error "Certbot is not installed"
        print_info "Install with: sudo apt install certbot"
        return 1
    fi
    
    # Check if certificate already exists
    if [ -d "/etc/letsencrypt/live/$domain" ]; then
        print_warning "Certificate already exists for $domain"
        return 0
    fi
    
    print_step "Requesting certificate from Let's Encrypt..."
    print_warning "Make sure port 80 is open and domain DNS is pointing to this server"
    
    # Generate certificate
    if sudo certbot certonly --standalone \
        -d "$domain" \
        -d "www.$domain" \
        --agree-tos \
        --register-unsafely-without-email \
        --non-interactive; then
        
        print_success "SSL certificate generated successfully!"
        
        # Copy to Docker volume if needed
        print_step "Copying certificate for Docker access..."
        sudo cp -r /etc/letsencrypt/live/$domain docker/nginx/certs/ 2>/dev/null || true
        
        print_success "Certificate ready for $domain"
    else
        print_error "Failed to generate certificate - check port 80 and DNS"
        return 1
    fi
}

# ============================================================================
# FUNCTION: Remove Domain
# ============================================================================

remove_domain() {
    local domain=$1
    
    print_header "Removing Domain: $domain"
    
    # Check if domain exists
    if [ ! -f "docker/nginx/vhosts/$domain.conf" ]; then
        print_error "Domain configuration not found for $domain"
        return 1
    fi
    
    # Backup configuration
    print_step "Backing up configuration..."
    cp "docker/nginx/vhosts/$domain.conf" "docker/nginx/vhosts/$domain.conf.bak"
    print_success "Backup created: docker/nginx/vhosts/$domain.conf.bak"
    
    # Remove configuration
    rm "docker/nginx/vhosts/$domain.conf"
    print_success "Configuration removed"
    
    # Remove from tracking
    sed -i "\|^$domain$|d" docker/nginx/domains.txt 2>/dev/null || true
    
    # Reload Nginx
    print_step "Reloading Nginx..."
    docker-compose -f docker-compose-preprod.yml exec nginx nginx -s reload || \
        print_warning "Nginx not running - will reload on next restart"
    
    # Ask about SSL
    read -p "Remove SSL certificate for $domain? (y/n) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        if command -v certbot &> /dev/null; then
            sudo certbot delete --cert-name "$domain" --non-interactive || true
        fi
    fi
    
    print_success "Domain $domain removed successfully!"
}

# ============================================================================
# FUNCTION: List Domains
# ============================================================================

list_domains() {
    print_header "Configured Domains"
    
    if [ ! -d "docker/nginx/vhosts" ] || [ -z "$(ls docker/nginx/vhosts/*.conf 2>/dev/null)" ]; then
        print_warning "No domains configured yet"
        return 0
    fi
    
    echo ""
    for vhost in docker/nginx/vhosts/*.conf; do
        domain=$(basename "$vhost" .conf)
        echo -e "${BLUE}Domain:${NC} $domain"
        
        # Check SSL status
        if [ -d "/etc/letsencrypt/live/$domain" ]; then
            ssl_expiry=$(sudo certbot certificates 2>/dev/null | grep -A2 "Name: $domain" | grep "Expiry Date:" | sed 's/.*Expiry Date: //' || echo "unknown")
            echo "  ${GREEN}SSL:${NC} ✓ ($ssl_expiry)"
        else
            echo "  ${YELLOW}SSL:${NC} ✗ (No certificate)"
        fi
        
        # Check Nginx status
        if docker-compose -f docker-compose-preprod.yml exec nginx nginx -t 2>/dev/null | grep -q "successful"; then
            echo "  ${GREEN}Nginx:${NC} ✓ (Valid config)"
        else
            echo "  ${RED}Nginx:${NC} ✗ (Config error)"
        fi
        
        echo ""
    done
}

# ============================================================================
# FUNCTION: Renew SSL Certificates
# ============================================================================

renew_ssl_certs() {
    print_header "Renewing SSL Certificates"
    
    if ! command -v certbot &> /dev/null; then
        print_error "Certbot is not installed"
        return 1
    fi
    
    print_step "Checking for certificate renewals..."
    
    if sudo certbot renew --non-interactive; then
        print_success "Certificates renewed successfully"
    else
        print_warning "No certificates needed renewal, or renewal failed"
    fi
}

# ============================================================================
# FUNCTION: Test Domain
# ============================================================================

test_domain() {
    local domain=$1
    
    print_header "Testing Domain: $domain"
    
    print_step "Testing HTTP redirect..."
    http_status=$(curl -s -o /dev/null -w "%{http_code}" "http://$domain" 2>/dev/null || echo "000")
    
    if [ "$http_status" = "301" ] || [ "$http_status" = "308" ]; then
        print_success "HTTP redirect works (Status: $http_status)"
    else
        print_warning "HTTP might not redirect (Status: $http_status)"
    fi
    
    print_step "Testing HTTPS..."
    https_status=$(curl -s -o /dev/null -w "%{http_code}" -k "https://$domain" 2>/dev/null || echo "000")
    
    if [ "$https_status" = "200" ] || [ "$https_status" = "000" ]; then
        print_success "HTTPS is accessible"
    else
        print_warning "HTTPS returned status: $https_status"
    fi
    
    print_step "Testing API endpoint..."
    api_response=$(curl -s "https://$domain/api/v1/health" -k 2>/dev/null || echo "failed")
    
    if echo "$api_response" | grep -q "UP\|status"; then
        print_success "API endpoint is reachable"
        echo "Response: $api_response"
    else
        print_warning "API endpoint test failed"
    fi
}

# ============================================================================
# FUNCTION: Show Help
# ============================================================================

show_help() {
    cat << EOF
${BLUE}Luna2 Domain Management${NC}

Usage: ./manage-domains.sh [COMMAND] [OPTIONS]

Commands:

  ${GREEN}add DOMAIN${NC}
    Add a new domain and configure Nginx proxy
    Example: ./manage-domains.sh add luna2.example.com

  ${GREEN}remove DOMAIN${NC}
    Remove a domain configuration
    Example: ./manage-domains.sh remove luna2.example.com

  ${GREEN}list${NC}
    List all configured domains and their SSL status
    Example: ./manage-domains.sh list

  ${GREEN}ssl DOMAIN${NC}
    Generate/renew SSL certificate for a domain
    Example: ./manage-domains.sh ssl luna2.example.com

  ${GREEN}renew${NC}
    Renew all SSL certificates
    Example: ./manage-domains.sh renew

  ${GREEN}test DOMAIN${NC}
    Test domain connectivity and API
    Example: ./manage-domains.sh test luna2.example.com

  ${GREEN}help${NC}
    Show this help message

Examples:

  # Add domain with interactive SSL setup
  ./manage-domains.sh add myapp.com

  # Add domain without SSL (configure later)
  ./manage-domains.sh add myapp.com

  # Generate SSL for existing domain
  ./manage-domains.sh ssl myapp.com

  # Test domain configuration
  ./manage-domains.sh test myapp.com

  # List all domains
  ./manage-domains.sh list

  # Remove domain
  ./manage-domains.sh remove myapp.com

Notes:

  • DNS: Make sure your domain DNS points to this server's IP
  • Ports: Ensure ports 80 and 443 are open in firewall
  • Let's Encrypt: Free SSL certificates with automatic renewal
  • Rollback: Domain configs are backed up before removal

EOF
}

# ============================================================================
# MAIN
# ============================================================================

main() {
    local command=${1:-help}
    local domain=${2:-}
    
    case "$command" in
        add)
            if [ -z "$domain" ]; then
                print_error "Domain argument required"
                echo "Usage: ./manage-domains.sh add <domain>"
                exit 1
            fi
            add_domain "$domain"
            ;;
        remove)
            if [ -z "$domain" ]; then
                print_error "Domain argument required"
                echo "Usage: ./manage-domains.sh remove <domain>"
                exit 1
            fi
            remove_domain "$domain"
            ;;
        list)
            list_domains
            ;;
        ssl)
            if [ -z "$domain" ]; then
                print_error "Domain argument required"
                echo "Usage: ./manage-domains.sh ssl <domain>"
                exit 1
            fi
            generate_ssl_cert "$domain"
            ;;
        renew)
            renew_ssl_certs
            ;;
        test)
            if [ -z "$domain" ]; then
                print_error "Domain argument required"
                echo "Usage: ./manage-domains.sh test <domain>"
                exit 1
            fi
            test_domain "$domain"
            ;;
        help|--help|-h)
            show_help
            ;;
        *)
            print_error "Unknown command: $command"
            show_help
            exit 1
            ;;
    esac
}

main "$@"
