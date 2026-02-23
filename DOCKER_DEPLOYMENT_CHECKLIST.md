# Luna2 Docker Deployment - Step-by-Step Checklist

## 📋 Pre-Deployment (Do This ONCE)

### 1. Choose Your Linux Distribution
```
✓ RECOMMENDED: Ubuntu 22.04 LTS
  ├─ Best Docker support
  ├─ LTS until April 2032
  ├─ Most tutorials available
  └─ Pre-tested with this guide

Alternative options:
  ├─ Ubuntu 20.04 LTS (proven, stable)
  ├─ Debian 12 (ultra-stable, minimal)
  └─ Debian 11 (for minimal resources)

❌ NOT recommended:
  ├─ Alpine Linux (limited Docker Compose)
  ├─ CentOS/RHEL (different package manager)
  └─ Arch (rolling release, unstable for servers)
```

### 2. Select Server & Hardware
```
Minimum for Testing:
  CPU:  2 cores
  RAM:  4 GB
  Disk: 20 GB SSD

Production Recommended:
  CPU:  4 cores
  RAM:  8 GB
  Disk: 100 GB SSD

Popular Providers:
  ├─ DigitalOcean: $12/month (2CPU, 4GB, 80GB)
  ├─ Linode: $20/month (4CPU, 8GB, 160GB)
  ├─ AWS EC2 t3.medium: ~$20/month
  ├─ Hetzner: €2.99/month (very cheap!)
  └─ Contabo: €4.99/month (best value)
```

### 3. Launch Server & Get Root Access
```bash
# Via SSH
ssh -i your-key.pem ubuntu@your-server-ip

# Or if using username/password
ssh ubuntu@your-server-ip
```

### 4. Download Preparation Script
**Option A: Using wget**
```bash
wget https://raw.githubusercontent.com/zensoftwareit-ops/Luna2/main/prepare-server.sh
chmod +x prepare-server.sh
./prepare-server.sh
```

**Option B: Using curl**
```bash
curl -O https://raw.githubusercontent.com/zensoftwareit-ops/Luna2/main/prepare-server.sh
chmod +x prepare-server.sh
./prepare-server.sh
```

**Option C: Paste script directly**
```bash
cat > prepare-server.sh << 'EOF'
[paste entire script content here]
EOF
chmod +x prepare-server.sh
./prepare-server.sh
```

### 5. Run Script & Answer Prompts
```bash
./prepare-server.sh

# Script will ask:
# 1. Install Portainer docker UI? → Say 'y' or 'n'
# 2. Generate self-signed SSL? → Say 'y' for testing, 'n' if using Let's Encrypt
```

**Expected Output:**
```
========================================
SYSTEM REQUIREMENTS CHECK
========================================
✓ OS: Ubuntu 22.04 LTS
✓ CPU cores adequate: 4
✓ RAM adequate: 8GB
✓ Disk space adequate: 100GB

========================================
INSTALLING REQUIRED PACKAGES
========================================
✓ All required packages installed

========================================
INSTALLING DOCKER
========================================
✓ Docker already installed: Docker version 25.0.3
✓ Docker Compose already installed: Docker Compose version v2.24.1

========================================
SETUP COMPLETE
========================================
✓ Server is ready for Luna2 deployment!
```

### 6. Apply Docker Group Changes
```bash
# IMPORTANT: After running prepare-server.sh
newgrp docker

# Verify Docker works without sudo
docker ps
docker-compose --version
```

**If you want permanent changes (recommended):**
```bash
# Log out
exit

# Log back in
ssh ubuntu@your-server-ip

# Docker group changes are now permanent
```

---

## 🚀 Deployment (Do This ONCE)

### 1. Get Luna2 Repository
**Option A: Clone from Git**
```bash
git clone https://github.com/zensoftwareit-ops/Luna2.git
cd Luna2
```

**Option B: Download ZIP**
```bash
wget https://github.com/zensoftwareit-ops/Luna2/archive/refs/heads/main.zip
unzip main.zip
cd Luna2-main
```

### 2. Create Environment File
```bash
# Copy the template
cp .env.example .env

# Edit with your values
nano .env    # Or: vi .env
```

### 3. Configure .env File
```ini
# Database Configuration
DB_HOST=mysql
DB_PORT=3306
DB_NAME=luna2
DB_USER=luna2_user
DB_PASSWORD=YOUR_SECURE_PASSWORD_HERE    ← CHANGE THIS!
MYSQL_ROOT_PASSWORD=YOUR_ROOT_PASSWORD_HERE  ← CHANGE THIS!

# Application Configuration
APP_PORT=8080
JWT_SECRET=YOUR_JWT_SECRET_HERE           ← CHANGE THIS!
JWT_EXP_MINUTES=60

# Email (optional, fill in only if you have SMTP)
MAIL_SMTP_HOST=smtp.gmail.com
MAIL_SMTP_PORT=587
MAIL_SMTP_USER=your-email@gmail.com
MAIL_SMTP_PASSWORD=your-app-password

# Timezone
TZ=Europe/Rome
```

### 4. Generate Secure Passwords
```bash
# Generate random password for database
openssl rand -base64 32

# Generate random JWT secret
openssl rand -base64 32

# Example output:
# a7F3kL9mP2jX8vQ5bN6wE1sT4yU3cZ0xR9dM7hJ2pL6vK8nF4tG0bC3yW5qS9aE
```

### 5. Configure SSL Certificates (Choose One)

**Option A: Self-Signed (for testing)**
```bash
mkdir -p docker/nginx/certs
openssl req -x509 -newkey rsa:4096 \
  -keyout docker/nginx/certs/luna2.key \
  -out docker/nginx/certs/luna2.crt \
  -days 365 -nodes \
  -subj "/C=IT/ST=Italy/L=Milan/O=Luna2/CN=luna2.local"
```

**Option B: Let's Encrypt (for production)**
```bash
# Install certbot (on your local machine or server)
sudo apt install certbot

# Get certificate (port 80 must be open)
sudo certbot certonly --standalone -d your-domain.com

# Certificates will be in: /etc/letsencrypt/live/your-domain.com/

# Copy to server
scp -r /etc/letsencrypt/live/your-domain.com/* \
  ubuntu@your-server:~/Luna2/docker/nginx/certs/
```

**Option C: Skip for Now (only use HTTP)**
```bash
# Don't start nginx
# Modify docker-compose-preprod.yml to comment out nginx service
```

### 6. (Optional) Create Required Directories
```bash
mkdir -p logs
mkdir -p docker/nginx/certs
mkdir -p config

# Verify structure
ls -la
```

---

## 🐳 Start Docker Containers

### 1. Pull Latest Images (First Time Only)
```bash
docker-compose -f docker-compose-preprod.yml pull
```

**This downloads:**
- mysql:8.0 (~600 MB)
- openjdk:11-jre-slim (~200 MB)
- nginx:latest (~150 MB)
- **Total**: ~1 GB

### 2. Start All Containers
```bash
docker-compose -f docker-compose-preprod.yml up -d
```

**Expected output:**
```
Creating luna2-mysql ... done
Creating luna2-api ... done
Creating luna2-nginx ... done
```

### 3. Check Container Status
```bash
docker-compose -f docker-compose-preprod.yml ps

# Expected:
# NAME            STATUS                PORTS
# luna2-mysql     Up 10 seconds (healthy) 3306/tcp
# luna2-api       Up 5 seconds (health: starting) 0.0.0.0:8080->8080/tcp
# luna2-nginx     Up 3 seconds (healthy) 0.0.0.0:80->80, 0.0.0.0:443->443
```

### 4. Wait for Full Startup
```bash
# Wait 30 seconds for database initialization
sleep 30

# Check API health
curl http://localhost:8080/api/v1/health

# Expected response:
# {"status":"UP"}
```

### 5. View Live Logs
```bash
# Follow API logs
docker-compose -f docker-compose-preprod.yml logs -f luna2-api

# Or all services
docker-compose -f docker-compose-preprod.yml logs -f

# Exit with Ctrl+C
```

---

## ✅ Verification & Testing

### 1. API Health Check
```bash
curl http://localhost:8080/api/v1/health
```

**Expected:**
```json
{"status":"UP"}
```

### 2. Test Login
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"Admin@123456"}'
```

**Expected:**
```json
{
  "success": true,
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "userId": 1,
  "username": "admin",
  "role": "ADMIN"
}
```

### 3. Test API Endpoints
```bash
# Get JWT token first
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"Admin@123456"}' | jq -r '.token')

# List customers
curl -X GET http://localhost:8080/api/v1/clienti \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json'

# Dashboard stats
curl -X GET http://localhost:8080/api/v1/dashboard/stats \
  -H "Authorization: Bearer $TOKEN"
```

### 4. Run Automated Test Suite
```bash
chmod +x test-api.sh
./test-api.sh
```

---

## 📊 Monitoring & Maintenance

### Regular Checks (Daily)
```bash
# Check container status
docker-compose -f docker-compose-preprod.yml ps

# Check system resources
docker stats

# Check logs for errors
docker-compose -f docker-compose-preprod.yml logs luna2-api | grep ERROR
```

### Database Backup (Before Making Changes)
```bash
docker-compose -f docker-compose-preprod.yml exec mysql \
  mysqldump -u root -p$MYSQL_ROOT_PASSWORD luna2 > luna2_backup_$(date +%Y%m%d).sql
```

### View Database
```bash
# Connect to MySQL
docker-compose -f docker-compose-preprod.yml exec mysql \
  mysql -u luna2_user -p$DB_PASSWORD luna2

# In MySQL prompt:
> SELECT COUNT(*) FROM users;
> SHOW TABLES;
> EXIT;
```

---

## 🛑 Stopping & Restarting

### Stop All Containers
```bash
docker-compose -f docker-compose-preprod.yml down
```

### Restart All Containers
```bash
docker-compose -f docker-compose-preprod.yml restart
```

### Restart Specific Container
```bash
docker-compose -f docker-compose-preprod.yml restart luna2-api
```

### Clean Up Everything (⚠️ WARNING: Deletes all data!)
```bash
docker-compose -f docker-compose-preprod.yml down -v
```

---

## 🔒 Security Checklist

After successful deployment:

```
✓ Change admin password
  - POST /api/v1/auth/change-password

✓ Change JWT secret
  - Update JWT_SECRET in .env
  - Regenerate with: openssl rand -base64 32
  - Restart API: docker-compose restart luna2-api

✓ Change database password
  - Update DB_PASSWORD in .env
  - Restart: docker-compose down && docker-compose up -d

✓ Enable SSL/TLS
  - Use Let's Encrypt (see above)
  - Or configure with your certificate authority

✓ Firewall rules
  sudo ufw allow 22/tcp   # SSH only
  sudo ufw allow 80/tcp   # HTTP
  sudo ufw allow 443/tcp  # HTTPS
  sudo ufw enable

✓ Backup database
  - Daily automated exports
  - Weekly offsite backups
```

---

## ⏱️ Timeline Summary

| Step | Duration | Task |
|------|----------|------|
| 1 | 5-10 min | Launch server, SSH in |
| 2 | 5-10 min | Run prepare-server.sh |
| 3 | 2-5 min | Clone Luna2 repo |
| 4 | 5-10 min | Configure .env file |
| 5 | 5 min | Generate SSL certs |
| 6 | 2-3 min | `docker-compose up -d` |
| 7 | 3-5 min | Verify & test API |
| **Total** | **25-45 min** | **Full deployment** |

---

## 🆘 Quick Troubleshooting

### Container won't start
```bash
docker-compose -f docker-compose-preprod.yml logs mysql
docker-compose -f docker-compose-preprod.yml logs luna2-api
```

### Port already in use
```bash
# Find what's using the port
sudo lsof -i :80
sudo lsof -i :443
sudo lsof -i :8080
```

### Disk full
```bash
docker system prune -a --volumes
df -h
```

### Can't connect to API
```bash
# Check if container is running
docker ps

# Check if API started correctly
docker-compose -f docker-compose-preprod.yml logs luna2-api -f

# Test with nc
nc -zv localhost 8080
```

---

## 🎯 You're Done! 🎉

After completing all steps above, Luna2 is fully deployed and ready to use:

```
✓ API Server: http://your-server-ip:8080
✓ HTTPS (if configured): https://your-domain.com
✓ Default login: admin / Admin@123456
✓ Database: Running in Docker
✓ All services: Automated startup on reboot
```

**Next Steps:**
1. Change default password immediately
2. Configure Google Calendar (optional)
3. Set up daily backups
4. Monitor system resources
5. Plan for scaling (if needed)

---

**Document Version**: 1.0.0  
**Last Updated**: February 23, 2026  
**Status**: Production Ready
