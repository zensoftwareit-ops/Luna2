# 🚀 Luna2 Docker - TL;DR (Too Long; Didn't Read)

## 📌 The 3 Commands You Need

If you just want to get started quickly and don't want to read 50 pages of documentation, here are the only 3 commands you need:

### Command 1: Prepare Server (Run ONCE)
```bash
wget https://raw.githubusercontent.com/zensoftwareit-ops/Luna2/main/prepare-server.sh && \
chmod +x prepare-server.sh && \
./prepare-server.sh

# Then:
newgrp docker
```

### Command 2: Deploy Luna2 (Run ONCE)
```bash
git clone https://github.com/zensoftwareit-ops/Luna2.git && \
cd Luna2 && \
cp .env.example .env && \
# Edit .env with your passwords:
nano .env && \
docker-compose -f docker-compose-preprod.yml up -d
```

### Command 3: Test API (Run ONCE)
```bash
./test-api.sh
```

---

## ✅ What You Get

After those 3 commands:

```
✓ Docker Engine installed & running
✓ Docker Compose installed & configured
✓ MySQL 8.0 database running
✓ Luna2 REST API running at port 8080
✓ Nginx reverse proxy at ports 80/443
✓ All health checks passing
✓ API ready for testing

Login with:
  Username: admin
  Password: Admin@123456
```

---

## 🖥️ Server Requirements (Minimum)

| Component | Amount |
|-----------|--------|
| OS | Ubuntu 22.04 LTS or Debian 12 |
| CPU | 2 cores |
| RAM | 4 GB |
| Disk | 20 GB SSD |
| Internet | Stable |

---

## 💰 Recommended Providers (Cheapest)

- **Hetzner Cloud**: €2.99/month (CX21: 2CPU, 4GB, 40GB)
- **Contabo**: €4.99/month (VPS S: 4CPU, 8GB, 200GB)
- **DigitalOcean**: $12/month (Droplet: 2CPU, 4GB, 80GB)
- **Linode**: $20/month (4CPU, 8GB, 160GB)

---

## 📋 Step-by-Step (3 Steps Total)

### Step 1: SSH into Your Server
```bash
ssh ubuntu@your-server-ip
```

### Step 2: Run These Commands
```bash
# Clone the repo and setup
git clone https://github.com/zensoftwareit-ops/Luna2.git && cd Luna2

# Download and run server prep script
wget https://raw.githubusercontent.com/zensoftwareit-ops/Luna2/main/prepare-server.sh
chmod +x prepare-server.sh
./prepare-server.sh

# Apply docker group changes
newgrp docker

# Configure Luna2
cp .env.example .env
nano .env  # Edit passwords

# Start Docker containers
docker-compose -f docker-compose-preprod.yml up -d

# Wait 30 seconds, then test
sleep 30
./test-api.sh
```

### Step 3: Done! 🎉
```
API Server: http://your-server-ip:8080
Default login: admin / Admin@123456
```

---

## 🔐 Important: Change These NOW

```bash
# 1. Edit .env and change:
DB_PASSWORD=something_secure_here
MYSQL_ROOT_PASSWORD=something_secure_here
JWT_SECRET=some_random_string

# 2. Generate secure passwords:
openssl rand -base64 32

# 3. Change admin password after login
# (via API or web interface)
```

---

## 📞 Need More Detail?

If something doesn't work, check these documents:

| Document | Purpose |
|----------|---------|
| [SERVER_REQUIREMENTS.md](SERVER_REQUIREMENTS.md) | Detailed hardware specs & Linux distro guide |
| [DOCKER_DEPLOYMENT_CHECKLIST.md](DOCKER_DEPLOYMENT_CHECKLIST.md) | Step-by-step deployment with all details |
| [PREPROD_DEPLOYMENT_GUIDE.md](PREPROD_DEPLOYMENT_GUIDE.md) | Complete guide with troubleshooting |
| [QUICKSTART_PREPROD.md](QUICKSTART_PREPROD.md) | Quick reference guide |

---

## 🆘 Quick Troubleshooting

### Docker command not found
```bash
# Check if Docker is installed
docker ps

# If not, run prepare-server.sh again
./prepare-server.sh

# Then apply group changes
newgrp docker
```

### Containers won't start
```bash
# Check logs
docker-compose -f docker-compose-preprod.yml logs

# Restart
docker-compose -f docker-compose-preprod.yml down
docker-compose -f docker-compose-preprod.yml up -d
```

### Can't connect to API
```bash
# Wait 30 seconds (containers are starting)
sleep 30

# Test health endpoint
curl http://localhost:8080/api/v1/health

# Check container status
docker-compose -f docker-compose-preprod.yml ps
```

### Out of disk space
```bash
# Clean Docker
docker system prune -a --volumes

# Check disk
df -h
```

---

## 🎯 Default Credentials

```
Admin Account:
  Login: admin
  Password: Admin@123456

Test Account:
  Login: testuser
  Password: User@123456
```

⚠️ **CHANGE THESE IN PRODUCTION!**

---

## 🌐 What's Installed?

```
Inside Containers (Docker handles all):
✓ MySQL 8.0 database
✓ Java 11 JRE
✓ Luna2 REST API
✓ Nginx web server

On Your Server (Need to install):
✓ Docker Engine
✓ Docker Compose
✓ curl, wget, git, jq, openssl

NOT Required (because of Docker):
✗ Java JDK
✗ Maven
✗ PHP/Node/Python
✗ MySQL client
```

---

## 📊 What Gets Downloaded

```
First time docker-compose up -d runs:
- MySQL 8.0 image:              ~600 MB
- OpenJDK 11-slim image:        ~200 MB
- Nginx image:                  ~150 MB
- Luna2 API JAR:                ~50 MB

Total Download:                 ~1 GB

Bandwidth needed: High-speed internet recommended
Time to download: 2-5 minutes (depends on connection)
```

---

## ⏱️ Total Time to Deploy

```
Server Prep:      5-10 minutes
Setup Script:     5-10 minutes
Clone & Config:   2-5 minutes  
Docker Download:  2-5 minutes
Startup:          2-3 minutes
Testing:          2-3 minutes
─────────────────────────────
TOTAL:            20-35 minutes
```

---

## 🔄 After Deployment

### Daily Tasks (Optional)
```bash
# Check status
docker-compose -f docker-compose-preprod.yml ps

# View logs
docker-compose -f docker-compose-preprod.yml logs -f luna2-api
```

### Weekly Tasks (Optional)
```bash
# Backup database
docker-compose -f docker-compose-preprod.yml exec mysql \
  mysqldump -u root -p$MYSQL_ROOT_PASSWORD luna2 > backup_$(date +%Y%m%d).sql

# Clean docker
docker system prune
```

---

## 🎓 Learning Path

If you want to understand what's happening:

1. **Basic**: Read this document (TL;DR)
2. **Intermediate**: Read [DOCKER_DEPLOYMENT_CHECKLIST.md](DOCKER_DEPLOYMENT_CHECKLIST.md)
3. **Advanced**: Read [PREPROD_DEPLOYMENT_GUIDE.md](PREPROD_DEPLOYMENT_GUIDE.md)
4. **Expert**: Read source code in `luna2-api/src/main/java/it/zensoftware/luna2/api/`

---

## 🚀 You're Ready!

```
GO FORTH AND DEPLOY! 🚀
```

**Questions?** Check the detailed guides above.  
**Something broke?** Check [PREPROD_DEPLOYMENT_GUIDE.md](PREPROD_DEPLOYMENT_GUIDE.md) - Troubleshooting section.

---

**Version**: 1.0.0  
**Last Updated**: February 23, 2026  
**Status**: Ready to Deploy
