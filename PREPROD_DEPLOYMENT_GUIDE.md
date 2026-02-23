# Luna2 ERP - Pre-Production Deployment Guide

## Overview

Luna2 è un gestionale ERP completo con moduli di:
- **CRM**: Gestione lead, attività, opportunity
- **Contabilità**: Preventivi, ordini, DDT, fatture con tracciamento pagamenti
- **Magazzino**: Gestione prodotti, giacenze, movimenti
- **Calendario**: Sincronizzazione con Google Calendar e iCloud CalDAV
- **Dashboard**: Real-time statistics e notifiche WebSocket

Questo documento fornisce le istruzioni per deployare Luna2 su un server di pre-produzione.

---

## Prerequisites

### Hardware Requirements
- **CPU**: 2+ cores
- **RAM**: 4GB minimum (8GB recommended)
- **Disk**: 20GB minimum SSD
- **Network**: Stable internet connection

### Software Requirements
- **OS**: Linux (Ubuntu 20.04+ recommended) or Windows with WSL2
- **Java**: JDK 11+ (OpenJDK 11 LTS recommended)
- **Maven**: 3.8.1+
- **MySQL**: 8.0+
- **Docker & Docker Compose**: (Optional, for containerized deployment)

### Installation

#### 1. Install Java
```bash
# Ubuntu/Debian
sudo apt update
sudo apt install openjdk-11-jdk maven mysql-server mysql-client

# Verify installation
java -version
mvn --version
mysql --version
```

#### 2. Install Docker (Optional)
```bash
# Ubuntu/Debian
sudo apt install docker.io docker-compose
sudo usermod -aG docker $USER
```

---

## Deployment Methods

### Option A: Script-Based Automated Setup (Recommended for Linux)

#### Quick Start
```bash
# 1. Clone/Extract the project
cd /opt/luna2  # or your preferred location
chmod +x setup-preprod.sh

# 2. Configure environment (edit the script to set):
# - DB_HOST (default: localhost)
# - DB_PORT (default: 3306)
# - DB_USER (default: luna2_user)
# - DB_PASSWORD (generate secure password)
# - APP_PORT (default: 8080)
# - JWT_SECRET (generate: openssl rand -base64 32)

# 3. Run setup script
./setup-preprod.sh

# 4. Start the API server
./start-api.sh

# 5. Tail the logs
tail -f logs/luna2-api.log
```

#### What the Script Does
1. ✓ Validates prerequisites (Java, Maven, MySQL)
2. ✓ Creates MySQL database and user
3. ✓ Imports complete schema
4. ✓ Configures application.properties
5. ✓ Builds project (Maven clean package)
6. ✓ Creates startup scripts
7. ✓ Generates Systemd service template (optional)

---

### Option B: Docker Compose (Recommended for Production)

#### Quick Start
```bash
# 1. Create configuration
cp .env.example .env

# 2. Edit .env with your values
nano .env

# 3. Generate SSL certificates (optional but recommended)
mkdir -p docker/nginx/certs
openssl req -x509 -newkey rsa:4096 -keyout docker/nginx/certs/luna2.key \
  -out docker/nginx/certs/luna2.crt -days 365 -nodes

# 4. Start services
docker-compose -f docker-compose-preprod.yml up -d

# 5. Verify services
docker-compose -f docker-compose-preprod.yml ps
docker-compose -f docker-compose-preprod.yml logs luna2-api
```

#### What Docker Compose Does
1. ✓ Starts MySQL 8.0 container with auto-initialized database
2. ✓ Builds and starts Luna2 API container
3. ✓ Starts Nginx reverse proxy with SSL/TLS
4. ✓ Health checks all services
5. ✓ Volume persistence for database and logs

---

### Option C: Manual Installation

#### Step 1: Database Setup
```bash
# 1. Connect to MySQL (as root)
mysql -h localhost -u root -p

# 2. In MySQL prompt
CREATE DATABASE luna2 CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'luna2_user'@'%' IDENTIFIED BY 'your_secure_password';
GRANT ALL PRIVILEGES ON luna2.* TO 'luna2_user'@'%';
FLUSH PRIVILEGES;
EXIT;

# 3. Import schema
mysql -h localhost -u luna2_user -p luna2 < database/luna2-complete-schema.sql
```

#### Step 2: Configure Application
```bash
# Edit Luna2 API configuration
nano luna2-api/src/main/resources/application.properties

# Update these values:
# spring.datasource.url=jdbc:mysql://localhost:3306/luna2
# spring.datasource.username=luna2_user
# spring.datasource.password=your_password
# jwt.secret=your_jwt_secret
# server.port=8080
```

#### Step 3: Build Project
```bash
# Build Luna2 Core
cd /path/to/Luna2
mvn clean package -DskipTests

# Build Luna2 API
cd luna2-api
mvn clean package -DskipTests
```

#### Step 4: Start Application
```bash
# Start API server
cd luna2-api
java -Xmx1024m -Xms512m -Dspring.profiles.active=production \
  -jar target/luna2-api-1.0.0.jar

# Or use screen/tmux to keep it running
screen -S luna2-api
java -Xmx1024m -Xms512m -Dspring.profiles.active=production \
  -jar target/luna2-api-1.0.0.jar
# Press Ctrl-A then D to detach
```

---

## Configuration Details

### Database Configuration (Hibernate)
```properties
# Connection
spring.datasource.url=jdbc:mysql://HOST:PORT/luna2?useSSL=false&serverTimezone=UTC
spring.datasource.username=luna2_user
spring.datasource.password=SECURE_PASSWORD

# Connection Pool
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.idle-timeout=600000

# Hibernate
spring.jpa.database-platform=org.hibernate.dialect.MySQL8Dialect
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.properties.hibernate.jdbc.batch_size=20
```

### JWT Configuration
```properties
# Generate secure secret (minimum 32 characters)
# openssl rand -base64 32

jwt.secret=YOUR_GENERATED_SECRET_HERE
jwt.exp-minutes=60
jwt.issuer=luna2-api
```

### Email Configuration (Optional)
```properties
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your-email@gmail.com
spring.mail.password=your-app-password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
spring.mail.properties.mail.smtp.starttls.required=true
```

### Calendar Sync Configuration
```properties
# Google Calendar (optional, configure later)
google.calendar.client.id=YOUR_CLIENT_ID
google.calendar.client.secret=YOUR_CLIENT_SECRET
calendar.sync.interval-minutes=30

# Calendar Encryption
calendar.encryption.secret=16-CHAR-MINIMUM-KEY
```

---

## Default Credentials

After successful setup, use these credentials to login:

### Admin Account
- **Username**: `admin`
- **Password**: `Admin@123456`
- **Role**: ADMIN

### Test Account
- **Username**: `testuser`
- **Password**: `User@123456`
- **Role**: USER

⚠️ **IMPORTANT**: Change these passwords in production!

---

## API Test Examples

### 1. Login & Get JWT Token
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"Admin@123456"}'

# Response:
# {
#   "success": true,
#   "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
#   "userId": 1,
#   "username": "admin",
#   "role": "ADMIN"
# }
```

### 2. List Customers (Requires JWT)
```bash
TOKEN="your_jwt_token_here"

curl -X GET 'http://localhost:8080/api/v1/clienti?page=0&size=10' \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json'
```

### 3. Create a Customer
```bash
TOKEN="your_jwt_token_here"

curl -X POST http://localhost:8080/api/v1/clienti \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "ragioneSociale": "Beta Srl",
    "partitaIva": "99887766554",
    "email": "info@betasrl.it",
    "telefono": "+39 123 456 7890",
    "indirizzo": "Via Roma 123",
    "citta": "Milano",
    "provincia": "MI",
    "cap": "20100"
  }'
```

### 4. Get Dashboard Statistics
```bash
TOKEN="your_jwt_token_here"

curl -X GET http://localhost:8080/api/v1/dashboard/stats \
  -H "Authorization: Bearer $TOKEN"
```

### 5. List Orders
```bash
TOKEN="your_jwt_token_here"

curl -X GET 'http://localhost:8080/api/v1/ordini?anno=2026&page=0&size=10' \
  -H "Authorization: Bearer $TOKEN"
```

---

## Systemd Service Setup (Optional)

For automatic startup on system reboot:

```bash
# 1. Create service user
sudo useradd -m -s /bin/bash luna2

# 2. Copy service file
sudo cp /tmp/luna2-api.service /etc/systemd/system/

# 3. Update paths in service file
sudo nano /etc/systemd/system/luna2-api.service

# 4. Enable and start service
sudo systemctl daemon-reload
sudo systemctl enable luna2-api
sudo systemctl start luna2-api

# 5. Check status
sudo systemctl status luna2-api
```

---

## Nginx Reverse Proxy Setup

### Basic Nginx Configuration
```nginx
upstream luna2_api {
    server 127.0.0.1:8080;
}

server {
    listen 80;
    server_name your-domain.com;

    location / {
        proxy_pass http://luna2_api;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

### SSL/TLS with Let's Encrypt
```bash
# Install Certbot
sudo apt install certbot python3-certbot-nginx

# Generate certificate
sudo certbot certonly --standalone -d your-domain.com

# Configure Nginx for SSL (use docker/nginx/nginx.conf as template)
```

---

## Monitoring & Maintenance

### Check Application Health
```bash
# API health endpoint
curl http://localhost:8080/api/v1/health

# Docker services
docker-compose -f docker-compose-preprod.yml ps
docker-compose -f docker-compose-preprod.yml logs -f luna2-api

# System resources
docker stats
```

### Database Backup
```bash
# Manual backup
mysqldump -h localhost -u luna2_user -p luna2 > luna2_backup_$(date +%Y%m%d).sql

# Automated daily backup (add to crontab)
0 2 * * * mysqldump -h localhost -u luna2_user -pPASSWORD luna2 > /backups/luna2_$(date +\%Y\%m\%d).sql
```

### Log Rotation
```bash
# Configure logrotate
sudo nano /etc/logrotate.d/luna2

# Content:
# /workspaces/Luna2/logs/*.log {
#   daily
#   rotate 14
#   compress
#   delaycompress
#   notifempty
#   create 0640 luna2 luna2
# }
```

### Performance Monitoring
```bash
# Monitor JVM heap usage
jps -l  # List Java processes
jstat -gc <pid> 1000  # Monitor garbage collection

# Monitor database
mysql -u luna2_user -p luna2
> SHOW PROCESSLIST;
> SHOW STATUS;
```

---

## Troubleshooting

### Application won't start
```bash
# Check Java version
java -version  # Must be 11+

# Check ports
lsof -i :8080  # Check if 8080 is available

# Check logs
tail -100 logs/luna2-api.log

# Verify database connection
mysql -h localhost -u luna2_user -p luna2 -e "SELECT COUNT(*) FROM users;"
```

### Database connection errors
```bash
# Verify credentials
mysql -h localhost -u luna2_user -p luna2

# Check MySQL is running
sudo systemctl status mysql

# Check connection string in application.properties
# jdbc:mysql://HOST:PORT/DB?useSSL=false&serverTimezone=UTC
```

### Login fails
```bash
# Verify admin user exists
mysql -u luna2_user -p luna2 -e "SELECT * FROM users WHERE username='admin';"

# Reset admin password (hash: Admin@123456)
mysql -u luna2_user -p luna2 << EOF
UPDATE users 
SET password='$2a$10$7PJKF5IXtYt5LVLnBd/XB.b3lZ9.kUlp9p5oFJZHx9qSrKVJ3KjNi'
WHERE username='admin';
EOF
```

### High memory usage
```bash
# Increase heap size in startup script
-Xmx2048m -Xms1024m  # For 4GB RAM server
-Xmx4096m -Xms2048m  # For 8GB RAM server
```

---

## Security Recommendations

### 1. Change Default Credentials
```bash
# Change admin password immediately
# Use the change password API endpoint or direct database update
```

### 2. Configure Firewall
```bash
sudo ufw allow 22/tcp    # SSH
sudo ufw allow 80/tcp    # HTTP
sudo ufw allow 443/tcp   # HTTPS
sudo ufw allow 3306/tcp  # MySQL (internal only!)
sudo ufw enable
```

### 3. Enable SSL/TLS
```bash
# Use Let's Encrypt with Nginx
sudo certbot certonly --standalone -d your-domain.com
```

### 4. Rotate JWT Secret
```bash
# Generate new secret
openssl rand -base64 32

# Update in application.properties
jwt.secret=NEW_GENERATED_SECRET
```

### 5. Database Security
```bash
# Remove anonymous users
mysql -u root -p << EOF
DELETE FROM mysql.user WHERE user='';
FLUSH PRIVILEGES;
EOF

# Restrict MySQL to localhost
# In /etc/mysql/my.cnf: bind-address = 127.0.0.1
```

### 6. Regular Backups
```bash
# Daily automated backups
0 2 * * * /usr/bin/mysqldump -u luna2_user -pPASSWORD luna2 | gzip > /backups/luna2_$(date +\%Y\%m\%d).sql.gz
```

---

## Scaling Considerations

### For increased load:

1. **Database optimization**
   - Enable query caching
   - Add indexes (already included in schema)
   - Use read replicas for reporting

2. **Application instances**
   - Run multiple Luna2 API instances
   - Use Nginx load balancer
   - Share state in Redis (optional)

3. **Monitoring**
   - Enable APM (New Relic, DataDog, etc.)
   - Monitor database performance
   - Set up alerting for errors

4. **Caching**
   - Add Redis for session storage
   - Enable application-level caching
   - Use CDN for static assets

---

## Support & Issues

For issues, check:
1. Application logs: `logs/luna2-api.log`
2. Database logs: `/var/log/mysql/error.log`
3. Nginx logs: `/var/log/nginx/error.log`

---

## Next Steps

After successful deployment:

1. ✓ Configure Google Calendar OAuth (optional)
   - Get credentials from Google Cloud Console
   - Update `application.properties`

2. ✓ Configure email notifications (optional)
   - Set up SMTP credentials

3. ✓ Set up backups
   - Daily database exports
   - Weekly file system backups

4. ✓ Configure monitoring
   - Application health checks
   - Database monitoring
   - Error alerting

5. ✓ Change all default passwords
   - Admin user
   - Database user
   - JWT secret

6. ✓ Test all features
   - CRUD operations
   - Calendar sync
   - Notifications
   - Reports

---

## Document Information
- **Version**: 1.0.0
- **Last Updated**: February 23, 2026
- **Status**: Production Ready
- **Created by**: Luna2 Development Team
