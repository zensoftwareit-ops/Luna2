#!/bin/bash

# ============================================================================
# Luna2 Server Preparation Script - Docker Edition
# Pre-installed everything needed to run Docker Compose
# Compatible: Ubuntu 20.04 LTS, 22.04 LTS, Debian 11, 12
# ============================================================================

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;36m'
NC='\033[0m'

print_header() {
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}========================================${NC}"
}

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠ $1${NC}"
}

print_step() {
    echo -e "${BLUE}▶ $1${NC}"
}

# ============================================================================
# SYSTEM CHECKS
# ============================================================================

print_header "SYSTEM REQUIREMENTS CHECK"

print_step "Checking OS..."
if [ -f /etc/os-release ]; then
    . /etc/os-release
    OS=$ID
    VERSION=$VERSION_ID
    print_success "OS: $PRETTY_NAME"
else
    print_error "Cannot determine OS"
    exit 1
fi

# Check if supported OS
case "$OS" in
    ubuntu|debian)
        print_success "Supported distribution detected: $OS $VERSION"
        ;;
    *)
        print_warning "This script is tested on Ubuntu and Debian"
        print_warning "It may work on other Debian-based distros"
        ;;
esac

print_step "Checking CPU cores..."
CORES=$(nproc)
echo "CPU Cores: $CORES"
if [ "$CORES" -lt 2 ]; then
    print_warning "Minimum 2 cores recommended (found: $CORES)"
else
    print_success "CPU cores adequate: $CORES"
fi

print_step "Checking available RAM..."
RAM_GB=$(($(grep MemTotal /proc/meminfo | awk '{print $2}') / 1024 / 1024))
echo "Available RAM: ${RAM_GB}GB"
if [ "$RAM_GB" -lt 4 ]; then
    print_warning "Minimum 4GB RAM recommended (found: ${RAM_GB}GB)"
else
    print_success "RAM adequate: ${RAM_GB}GB"
fi

print_step "Checking disk space..."
DISK_GB=$(df / | awk 'NR==2 {print $4/1024/1024}' | cut -d. -f1)
echo "Available disk: ${DISK_GB}GB"
if [ "$DISK_GB" -lt 20 ]; then
    print_warning "Minimum 20GB free space recommended (found: ${DISK_GB}GB)"
else
    print_success "Disk space adequate: ${DISK_GB}GB"
fi

# ============================================================================
# UPDATE SYSTEM
# ============================================================================

print_header "UPDATING SYSTEM PACKAGES"

print_step "Updating package list..."
sudo apt-get update -qq
print_success "Package list updated"

print_step "Upgrading installed packages..."
sudo DEBIAN_FRONTEND=noninteractive apt-get upgrade -y -qq
print_success "Packages upgraded"

# ============================================================================
# INSTALL REQUIRED PACKAGES
# ============================================================================

print_header "INSTALLING REQUIRED PACKAGES"

PACKAGES=(
    "curl"              # HTTP client for testing APIs
    "wget"              # Download utility
    "git"               # Version control (if pulling from repo)
    "jq"                # JSON processor for API testing
    "htop"              # System monitoring
    "vim"               # Text editor
    "nano"              # Text editor
    "net-tools"         # Network utilities (netstat, ifconfig)
    "dnsutils"          # DNS utilities (nslookup, dig)
    "openssh-client"    # SSH client
    "openssl"           # SSL/TLS utilities (certificate generation)
    "ca-certificates"   # SSL certificates
    "apt-transport-https" # HTTPS support for apt
    "software-properties-common" # Software management
    "build-essential"   # Compilation tools (optional)
)

print_step "Installing packages..."
for package in "${PACKAGES[@]}"; do
    if dpkg -l | grep "^ii" | grep -q "^ $package "; then
        echo "  ✓ $package is already installed"
    else
        echo "  Installing $package..."
        sudo apt-get install -y -qq "$package" || true
    fi
done
print_success "All required packages installed"

# ============================================================================
# INSTALL DOCKER
# ============================================================================

print_header "INSTALLING DOCKER"

if command -v docker &> /dev/null; then
    DOCKER_VERSION=$(docker --version)
    print_success "Docker already installed: $DOCKER_VERSION"
else
    print_step "Installing Docker from official repository..."
    
    # Add Docker GPG key
    curl -fsSL https://download.docker.com/linux/$OS/gpg | sudo gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg 2>/dev/null
    
    # Add Docker repository
    echo "deb [arch=amd64 signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/$OS $(lsb_release -cs) stable" | \
        sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
    
    # Install Docker
    sudo apt-get update -qq
    sudo apt-get install -y -qq docker-ce docker-ce-cli containerd.io docker-compose-plugin
    
    print_success "Docker installed: $(docker --version)"
fi

# ============================================================================
# INSTALL DOCKER COMPOSE
# ============================================================================

print_header "INSTALLING DOCKER COMPOSE"

if command -v docker-compose &> /dev/null; then
    COMPOSE_VERSION=$(docker-compose --version)
    print_success "Docker Compose already installed: $COMPOSE_VERSION"
else
    print_step "Installing Docker Compose standalone..."
    
    DOCKER_COMPOSE_VERSION=$(curl -s https://api.github.com/repos/docker/compose/releases/latest | grep 'tag_name' | cut -d'"' -f4)
    DOCKER_COMPOSE_URL="https://github.com/docker/compose/releases/download/$DOCKER_COMPOSE_VERSION/docker-compose-$(uname -s)-$(uname -m)"
    
    sudo curl -L "$DOCKER_COMPOSE_URL" -o /usr/local/bin/docker-compose
    sudo chmod +x /usr/local/bin/docker-compose
    
    print_success "Docker Compose installed: $(docker-compose --version)"
fi

# ============================================================================
# DOCKER DAEMON CONFIGURATION
# ============================================================================

print_header "CONFIGURING DOCKER"

print_step "Enabling Docker daemon..."
sudo systemctl enable docker
sudo systemctl start docker
print_success "Docker daemon enabled and started"

print_step "Creating docker group and adding user..."
if ! getent group docker > /dev/null; then
    sudo groupadd docker
fi

# Add current user to docker group
sudo usermod -aG docker "$USER" || true
print_warning "You must log out and log back in for group changes to take effect"
print_warning "Or run: newgrp docker"

# ============================================================================
# INSTALL ADDITIONAL UTILITIES
# ============================================================================

print_header "INSTALLING OPTIONAL UTILITIES"

# Install Docker Compose V1 via apt (alternative to standalone)
print_step "Installing docker-compose via apt..."
sudo apt-get install -y -qq docker-compose || print_warning "docker-compose apt package unavailable (using standalone is fine)"

# Install Portainer for Docker UI (optional)
print_step "Offering optional Docker management UI..."
read -p "Install Portainer Community Edition for Docker management? (y/n) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    print_step "Installing Portainer..."
    docker volume create portainer_data
    docker run -d -p 8000:8000 -p 9000:9000 --name=portainer --restart=always \
        -v /var/run/docker.sock:/var/run/docker.sock \
        -v portainer_data:/data \
        portainer/portainer-ce:latest
    print_success "Portainer installed - Access at http://localhost:9000"
fi

# ============================================================================
# SSL/TLS CERTIFICATE SETUP (Optional)
# ============================================================================

print_header "SSL/TLS CERTIFICATE SETUP"

print_step "Creating certificate directory..."
mkdir -p "$HOME/luna2/docker/nginx/certs"
print_success "Certificate directory created: $HOME/luna2/docker/nginx/certs"

print_step "Offering self-signed certificate generation..."
read -p "Generate self-signed SSL certificate for testing? (y/n) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    print_step "Generating self-signed certificate..."
    openssl req -x509 -newkey rsa:4096 -keyout "$HOME/luna2/docker/nginx/certs/luna2.key" \
        -out "$HOME/luna2/docker/nginx/certs/luna2.crt" -days 365 -nodes \
        -subj "/C=IT/ST=Italy/L=Milan/O=Luna2/CN=luna2.local" 2>/dev/null
    print_success "Self-signed certificate generated"
    print_warning "For production, use Let's Encrypt: certbot certonly --standalone -d your-domain.com"
fi

# ============================================================================
# VERIFY INSTALLATION
# ============================================================================

print_header "VERIFYING INSTALLATION"

print_step "Checking Docker..."
if docker --version &>/dev/null; then
    print_success "Docker: $(docker --version)"
else
    print_error "Docker verification failed"
    exit 1
fi

print_step "Checking Docker Compose..."
if docker-compose --version &>/dev/null; then
    print_success "Docker Compose: $(docker-compose --version)"
else
    print_error "Docker Compose verification failed"
    exit 1
fi

print_step "Checking Docker daemon..."
if docker ps &>/dev/null; then
    print_success "Docker daemon is running and accessible"
else
    print_warning "Docker daemon not accessible - you may need to run: newgrp docker"
fi

print_step "Checking required utilities..."
for cmd in curl wget jq git openssl; do
    if command -v $cmd &>/dev/null; then
        print_success "$cmd: $(which $cmd)"
    else
        print_warning "$cmd: not found"
    fi
done

# ============================================================================
# FINAL SUMMARY
# ============================================================================

print_header "SETUP COMPLETE"

echo ""
echo -e "${BLUE}System Information:${NC}"
echo "  OS: $PRETTY_NAME"
echo "  CPU Cores: $CORES"
echo "  RAM: ${RAM_GB}GB"
echo "  Free Disk: ${DISK_GB}GB"

echo ""
echo -e "${BLUE}Installed Components:${NC}"
echo "  ✓ Docker: $(docker --version)"
echo "  ✓ Docker Compose: $(docker-compose --version)"
echo "  ✓ Essential utilities (curl, wget, jq, git, openssl)"
echo "  ✓ Security tools (openssh-client, ca-certificates)"

echo ""
echo -e "${BLUE}Next Steps:${NC}"
echo "  1. Log out and log back in (for docker group changes)"
echo "  2. Clone Luna2 repository"
echo "  3. Copy .env.example to .env"
echo "  4. Edit .env with your configuration"
echo "  5. Run: docker-compose -f docker-compose-preprod.yml up -d"

echo ""
echo -e "${YELLOW}⚠️  IMPORTANT - User Group Changes${NC}"
echo "  Run this to apply docker group changes immediately:"
echo "    newgrp docker"
echo ""
echo "  Or log out and back in for permanent effect"

echo ""
echo -e "${GREEN}✓ Server is ready for Luna2 deployment!${NC}"
echo ""
