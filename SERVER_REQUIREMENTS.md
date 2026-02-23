# Luna2 Docker - Server Requirements & Setup Guide

## 🖥️ Hardware Requirements

### Minimum Specifications
| Component | Requirement |
|-----------|-------------|
| **CPU** | 2 cores (Intel/AMD, 2+ GHz) |
| **RAM** | 4 GB |
| **Disk** | 20 GB SSD (or fast HDD) |
| **Network** | 10 Mbps stable connection |

### Recommended Specifications (Production)
| Component | Requirement |
|-----------|-------------|
| **CPU** | 4+ cores |
| **RAM** | 8-16 GB |
| **Disk** | 100+ GB SSD |
| **Network** | 100+ Mbps |

### Example Server Configurations

#### For Development/Testing
```
DigitalOcean Droplet: $12/month
- 2 vCPU
- 4 GB RAM
- 80 GB SSD
- Ubuntu 22.04 LTS

AWS EC2 t3.medium
- 2 vCPU
- 4 GB RAM
- gp2 30 GB SSD
- Ubuntu 22.04 LTS

Linode Nanode 1GB (minimal):
- 1 vCPU
- 1 GB RAM
- 25 GB SSD
- Debian 12
⚠️ Works but tight on resources
```

#### For Production
```
DigitalOcean Droplet: $36/month
- 4 vCPU
- 8 GB RAM
- 160 GB SSD
- Ubuntu 22.04 LTS

AWS EC2 t3.large
- 2 vCPU
- 8 GB RAM
- gp3 100 GB SSD
- Ubuntu 22.04 LTS

Linode 8GB:
- 4 vCPU
- 8 GB RAM
- 160 GB SSD
- Debian 12 / Ubuntu 22.04
```

---

## 🐧 Linux Distribution Selection

### **Option 1: Ubuntu 22.04 LTS** ⭐ RECOMMENDED
```
✓ Latest LTS (Long Term Support until 2032)
✓ Best Docker support
✓ Largest community
✓ Pre-tested with this guide
✓ Official Docker packages available
✓ Security updates until 2032
```

**Download**: https://ubuntu.com/download/server

**Why**: Ubuntu 22.04 has the best Docker ecosystem, most tutorials, and longest support window.

---

### **Option 2: Ubuntu 20.04 LTS**
```
✓ Stable, thoroughly tested
✓ Excellent Docker support
✓ LTS until April 2030
✓ Lower resource usage than 22.04
✓ Good for minimal servers
```

**Use if**: Your server is older or you prefer proven stability.

---

### **Option 3: Debian 12 (Bookworm)**
```
✓ Very stable and minimal
✓ Perfect for production
✓ LTS support
✓ Official Docker packages available
✓ Larger community than Debian
```

**Use if**: You prefer lightweight, ultra-stable systems.

---

### **Option 4: Debian 11 (Bullseye)**
```
✓ Rock-solid stability
✓ Minimal dependencies
✓ Perfect for VPS with limited resources
✓ Support until 2026
```

**Use if**: Running on minimal VPS (1-2 GB RAM).

---

## ❌ NOT Recommended
- Alpine Linux - Limited Docker Compose support, smaller package repository
- CentOS/RHEL - Different package manager (yum), larger learning curve
- Arch Linux - Rolling release, less stable for production

---

## 📋 Complete Server Setup Checklist

### Step 1: Choose & Launch Server
- [ ] Select OS: **Ubuntu 22.04 LTS** (recommended)
- [ ] Choose instance size (at least 4GB RAM)
- [ ] Configure networking (open ports 80, 443, 22)
- [ ] Configure firewall
- [ ] Set hostname (e.g., `luna2-server`)

### Step 2: Initial Server Access
```bash
# SSH into server
ssh -i your-key.pem ubuntu@your-server-ip

# Update system
sudo apt update && sudo apt upgrade -y
```

### Step 3: Run Preparation Script
```bash
# Download the preparation script
wget https://your-repo/prepare-server.sh
# Or if already on server:
curl -O https://your-repo/prepare-server.sh

# Make executable
chmod +x prepare-server.sh

# Run it
./prepare-server.sh
```

### Step 4: Post-Setup
```bash
# Apply docker group changes
newgrp docker

# Verify Docker works
docker ps
docker-compose --version
```

---

## 🚀 What Gets Installed

### Required (Automatically Installed)
```
✓ Docker Engine (latest)
✓ Docker Compose (latest)
✓ curl - HTTP testing & downloads
✓ wget - File downloads
✓ git - Version control
✓ jq - JSON processing
✓ openssl - Certificate generation
✓ ca-certificates - SSL certificate store
```

### Optional (Offered During Install)
```
○ Portainer - Docker management UI (port 9000)
○ Self-signed SSL certificate
```

### NOT Required (Because Using Docker)
```
❌ Java - Inside Docker container
❌ Maven - Inside Docker container
❌ MySQL - Inside Docker container
❌ PHP/Node/etc - Inside Docker container
```

---

## 💾 Disk Space Breakdown

```
Ubuntu Base:                      ~3 GB
Docker & Components:             ~500 MB
Docker Images (pulled on first): ~2-3 GB
  - mysql:8.0:                    ~600 MB
  - openjdk:11-jre-slim:          ~200 MB
  - nginx:latest:                 ~150 MB
Luna2 Running State:              ~500 MB
Database (grows over time):       ~1-10 GB+

Total Available Should Be:        ~20 GB minimum
```

---

## 🔌 Network Ports

### Required to Open
```
Port 22   - SSH (administration)
Port 80   - HTTP (redirect to HTTPS)
Port 443  - HTTPS (secure API)
Port 3306 - MySQL (optional, for admin only)
```

### Internal (Docker Network Only)
```
Port 8080 - API (internal to container)
Port 9000 - Portainer (if installed)
```

### Firewall Configuration
```bash
# Ubuntu/Debian with UFW
sudo ufw allow 22/tcp    # SSH
sudo ufw allow 80/tcp    # HTTP
sudo ufw allow 443/tcp   # HTTPS
sudo ufw enable
```

---

## 📊 Docker Architecture

```
┌─────────────────────────────────────────┐
│   Your Server (Ubuntu/Debian)           │
├─────────────────────────────────────────┤
│ ┌─────────────────────────────────────┐ │
│ │      Docker Daemon                  │ │
│ ├─────────────────────────────────────┤ │
│ │ ┌──────────┐ ┌──────────┐ ┌──────┐ │ │
│ │ │ MySQL    │ │  Luna2   │ │ Nginx│ │ │
│ │ │ Container│ │  API     │ │Proxy │ │ │
│ │ │ :3306    │ │ :8080    │ │:80   │ │ │
│ │ │          │ │          │ │:443  │ │ │
│ │ └──────────┘ └──────────┘ └──────┘ │ │
│ │                                     │ │
│ │ Shared Network: luna2-network      │ │
│ │ Volumes: mysql_data, logs          │ │
│ └─────────────────────────────────────┘ │
└─────────────────────────────────────────┘
         ↓
    Internet (port 80/443)
```

---

## 🔐 Security Best Practices

### 1. Minimal System
- ✓ Only Docker & essentials installed
- ✓ No Java/MySQL/PHP on host
- ✓ Everything containerized & isolated

### 2. Firewall
```bash
sudo ufw enable
sudo ufw default deny incoming
sudo ufw default allow outgoing
sudo ufw allow 22/tcp    # SSH
sudo ufw allow 80/tcp    # HTTP
sudo ufw allow 443/tcp   # HTTPS
```

### 3. SSH Hardening
```bash
# Disable root login
sudo sed -i 's/^#PermitRootLogin.*/PermitRootLogin no/' /etc/ssh/sshd_config

# Disable password auth (use keys only)
sudo sed -i 's/^#PasswordAuthentication.*/PasswordAuthentication no/' /etc/ssh/sshd_config

# Restart SSH
sudo systemctl restart sshd
```

### 4. Automatic Updates
```bash
# Install unattended-upgrades
sudo apt install unattended-upgrades
sudo dpkg-reconfigure -plow unattended-upgrades
```

### 5. SSL Certificates
```bash
# Use Let's Encrypt (free)
sudo apt install certbot python3-certbot-nginx
sudo certbot certonly --standalone -d your-domain.com
```

---

## 📦 Cloud Provider Quick Links

### DigitalOcean 
```
Droplet: Ubuntu 22.04 LTS, 4GB/80GB SSD ($12/month)
https://m.do.co/c/your-referral-link
```

### AWS EC2
```
t3.medium (2vCPU, 4GB RAM)
ami-0c55b159cbfafe1f0 (Ubuntu 22.04 LTS)
$15-20/month
```

### Linode
```
Linode 8GB (4vCPU, 8GB RAM)
Ubuntu 22.04 LTS
$40/month
```

### Hetzner Cloud
```
CX21 (2vCPU, 4GB RAM, 40GB SSD)
Ubuntu 22.04 LTS
€2.99/month (~€36/year)
```

### Contabo
```
VPS S SSD (4 vCPU, 8GB RAM, 200GB SSD)
Ubuntu 22.04 LTS
€4.99/month
```

---

## 🔧 Server Setup Script Usage

### Prerequisites
- Access to Ubuntu/Debian server
- sudo privileges (or run as root)
- Internet connection

### Quick Start
```bash
# 1. Download script
wget https://your-repo/prepare-server.sh
chmod +x prepare-server.sh

# 2. Run script
./prepare-server.sh

# 3. Follow prompts
# - Install Portainer? (optional)
# - Generate SSL cert? (for testing)

# 4. Log out and back in for docker group changes
exit
# SSH back in
ssh ubuntu@your-server

# 5. Verify
docker ps
docker-compose --version
```

### What the Script Does
1. ✓ Checks system resources (CPU, RAM, Disk)
2. ✓ Updates all system packages
3. ✓ Installs required utilities (curl, wget, jq, git, openssl)
4. ✓ Installs Docker Engine from official repository
5. ✓ Installs Docker Compose (latest version)
6. ✓ Configures Docker daemon to autostart
7. ✓ Adds current user to docker group
8. ✓ Optionally installs Portainer UI
9. ✓ Optionally generates self-signed SSL certificate
10. ✓ Verifies all installations

---

## 📥 Downloading Luna2 to Server

### Option 1: Clone from Git
```bash
# Clone repository
git clone https://github.com/zensoftwareit-ops/Luna2.git
cd Luna2

# Copy environment template
cp .env.example .env

# Edit configuration
nano .env
```

### Option 2: Download as ZIP
```bash
# Download
wget https://github.com/zensoftwareit-ops/Luna2/archive/refs/heads/main.zip
unzip main.zip
cd Luna2-main

# Copy environment template
cp .env.example .env
nano .env
```

### Option 3: Transfer from Local Machine
```bash
# From your local machine
scp -r Luna2 ubuntu@your-server:~/

# SSH in
ssh ubuntu@your-server
cd Luna2
cp .env.example .env
nano .env
```

---

## 🚀 Quick Deployment Timeline

```
Server Prep:        5-10 minutes
  └─ wget script
  └─ Run prepare-server.sh
  └─ Restart shell

Clone Luna2:        2-5 minutes
  └─ git clone
  └─ cp .env.example .env

Configure:          5-10 minutes
  └─ Edit .env file
  └─ Generate SSL cert (if needed)

Deploy:             2-5 minutes
  └─ docker-compose up -d
  └─ Wait for containers to start

Verify:             2-3 minutes
  └─ Check health endpoints
  └─ Test API
  
Total Time:         ~20-30 minutes
```

---

## ✅ Verification Commands

After deployment:

```bash
# Check Docker status
docker ps
docker stats

# Check Luna2 containers
docker-compose ps
docker-compose logs -f luna2-api

# Test API health
curl http://localhost:8080/api/v1/health
curl https://localhost:443/api/v1/health  # With HTTPS

# Test login
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"Admin@123456"}'

# Check disk usage
df -h
docker system df
```

---

## 🆘 Troubleshooting

### Docker won't start
```bash
sudo systemctl restart docker
sudo systemctl status docker
journalctl -u docker -n 50
```

### Permission denied
```bash
# Add user to docker group
sudo usermod -aG docker $USER
# Apply immediately
newgrp docker
# Or logout and back in
```

### Port already in use
```bash
# Find what's using port 80/443
sudo lsof -i :80
sudo lsof -i :443
```

### Low disk space
```bash
# Check usage
df -h

# Cleanup Docker
docker system prune -a --volumes

# Check container sizes
docker system df
```

---

## 📞 Support Resources

### Official Documentation
- Docker: https://docs.docker.com/
- Docker Compose: https://docs.docker.com/compose/
- Ubuntu: https://help.ubuntu.com/
- Debian: https://www.debian.org/doc/

### Monitoring Tools (Optional)
```bash
# Install htop for monitoring
sudo apt install htop
htop

# Monitor Docker
docker stats
```

---

## 🎯 Recommended Setup (Quick Summary)

```
1. Distribution: Ubuntu 22.04 LTS
2. RAM: 4 GB minimum (8 GB recommended)
3. CPU: 2 cores minimum (4 cores recommended)
4. Disk: 20+ GB SSD
5. Installation Method: Docker Compose
6. Script: prepare-server.sh

Timeline: 20-30 minutes to full deployment
Cost: $5-36/month depending on provider
Maintenance: Minimal (Docker handles everything)
```

---

**Last Updated**: February 23, 2026  
**Status**: Production Ready
