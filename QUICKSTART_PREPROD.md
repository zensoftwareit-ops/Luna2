# Luna2 Pre-Production Setup - Quick Reference

## 📦 Deliverables for Pre-Production Installation

This folder contains everything needed to install Luna2 on a pre-production server without Google OAuth centralized authentication.

### Files Created/Modified

#### 1. **Database Schema** ✅
   - 📄 `database/luna2-complete-schema.sql` - Complete, production-ready SQL schema
     - All tables: Users, Clients, Suppliers, Products, Orders, Invoices, Quotes, DDTs
     - Inventory management, CRM, Calendar, Notifications
     - Default admin user: `admin` / `Admin@123456`
     - Default test user: `testuser` / `User@123456`

#### 2. **Setup Scripts** ✅
   - 🔧 `setup-preprod.sh` - Fully automated setup script (Linux)
     - Creates database and user
     - Configures application properties
     - Builds project with Maven
     - Creates startup scripts
     - Generates Systemd service template
   
   - 🚀 `start-api.sh` - Quick API startup script
   - ⏹️ `stop.sh` - Stop running services

#### 3. **Docker Deployment** ✅
   - 🐳 `docker-compose-preprod.yml` - Complete Docker Compose configuration
     - MySQL 8.0 with auto-initialized database
     - Luna2 API container with health checks
     - Nginx reverse proxy with SSL/TLS support
     - Volume persistence for data and logs
   
   - 📦 `luna2-api/Dockerfile` - Production-ready API container
   - ⚙️ `.env.example` - Environment configuration template

#### 4. **Nginx Configuration** ✅
   - 🌐 `docker/nginx/nginx.conf` - Production nginx reverse proxy setup
     - SSL/TLS support
     - Rate limiting
     - Security headers
     - WebSocket support
     - Gzip compression

#### 5. **Documentation** ✅
   - 📖 `PREPROD_DEPLOYMENT_GUIDE.md` - Complete deployment guide
     - Prerequisites and installation
     - 3 deployment methods (Script, Docker, Manual)
     - Configuration details
     - API testing examples
     - Troubleshooting guide
     - Security best practices
   
   - 📋 `test-api.sh` - Automated API testing script
     - Tests all major endpoints
     - Validates JWT authentication
     - Health checks

---

## 🚀 Quick Start (Choose One Method)

### Method 1: Automated Script (Linux - Recommended)
```bash
chmod +x setup-preprod.sh
# Edit script to configure: DB credentials, JWT secret, app port
./setup-preprod.sh
./start-api.sh
tail -f logs/luna2-api.log
```

### Method 2: Docker Compose (Easiest)
```bash
cp .env.example .env
# Edit .env with your configuration
docker-compose -f docker-compose-preprod.yml up -d
docker-compose -f docker-compose-preprod.yml logs -f luna2-api
```

### Method 3: Manual (Full Control)
See `PREPROD_DEPLOYMENT_GUIDE.md` - Option C

---

## 📋 Pre-Deployment Checklist

- [ ] Java 11+ installed (`java -version`)
- [ ] Maven 3.8.1+ installed (`mvn --version`)
- [ ] MySQL 8.0+ installed and running
- [ ] Network access to MySQL port (3306)
- [ ] Available port 8080 (API server)
- [ ] Available port 80/443 (if using Nginx)
- [ ] ~20GB free disk space
- [ ] 4GB+ RAM available

---

## 🔑 Default Credentials

After setup, login with:

```
Username: admin
Password: Admin@123456
```

Or test account:
```
Username: testuser
Password: User@123456
```

⚠️ **MANDATORY**: Change these in production!

---

## 🧪 Testing the Installation

After server is running:

```bash
# Make the test script executable
chmod +x test-api.sh

# Run all API tests
./test-api.sh

# Or test manually
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"Admin@123456"}'
```

---

## 📊 Database Overview

The schema includes these main entities:

### Core Business
- **Clienti** (Customers) - Contact details, payment terms, credit limits
- **Fornitori** (Suppliers) - Vendor information
- **Prodotti** (Products) - Inventory items, pricing

### Transactions
- **Preventivi** (Quotes) - Sales proposals
- **Ordini** (Orders) - Sales orders with tracking
- **DDT** (Shipping Documents) - Delivery notes
- **Fatture** (Invoices) - Billing with payment status

### CRM
- **Lead** - Sales opportunities
- **Activity** - Customer interactions
- **Task** - Action items
- **Reminder** - Notifications

### Support
- **Calendar_Events** - Event scheduling
- **Notification_History** - System notifications
- **Notes** - Document annotations
- **Scadenze** - Deadlines and follow-ups

---

## 🔐 Security Recommendations

1. **Change default JWT secret** in `application.properties`
   ```
   jwt.secret=YOUR_GENERATED_SECRET
   ```
   Generate with: `openssl rand -base64 32`

2. **Change database password**
   ```sql
   ALTER USER 'luna2_user'@'%' IDENTIFIED BY 'new_secure_password';
   ```

3. **Enable SSL/TLS** - Use Let's Encrypt with Nginx
   ```bash
   sudo certbot certonly --standalone -d your-domain.com
   ```

4. **Restrict database access** - Only allow local connections
5. **Enable firewall** - Allow only necessary ports
6. **Regular backups** - Daily automated MySQL exports

---

## 📊 Monitoring

### Health Check
```bash
curl http://localhost:8080/api/v1/health
```

### View Logs
```bash
# Docker
docker-compose -f docker-compose-preprod.yml logs luna2-api

# Direct
tail -f logs/luna2-api.log
```

### Database Status
```bash
mysql -u luna2_user -p luna2
> SHOW PROCESSLIST;
> SHOW TABLE STATUS;
```

---

## 🛠️ Common Operations

### Restart API
```bash
./stop.sh
./start-api.sh
```

### Backup Database
```bash
mysqldump -u luna2_user -p luna2 > luna2_backup_$(date +%Y%m%d).sql
```

### View API Configuration
```bash
cat luna2-api/src/main/resources/application.properties
```

### Access Database
```bash
mysql -h localhost -u luna2_user -p luna2
```

---

## 📞 Support

If you encounter issues:

1. **Check logs** - `logs/luna2-api.log`
2. **Verify database** - Connect to MySQL and run queries
3. **Check network** - Ensure ports 8080, 3306 are accessible
4. **Review config** - Check `application.properties` values
5. **See documentation** - Read `PREPROD_DEPLOYMENT_GUIDE.md`

---

## 📝 Production Checklist (Before Going Live)

- [ ] All default passwords changed
- [ ] SSL/TLS certificates installed
- [ ] Database backups configured (daily)
- [ ] Monitoring/alerting set up
- [ ] Firewall rules configured
- [ ] Nginx reverse proxy tested
- [ ] Database indexes verified
- [ ] Connection pool settings tuned
- [ ] Logging configured
- [ ] Google Calendar OAuth configured (if needed)

---

## 🔄 Next Steps

1. **Complete setup** using one of the three methods
2. **Test API** using `./test-api.sh`
3. **Configure optional features**:
   - Google Calendar integration
   - Email notifications
   - SSL certificates
4. **Deploy to production** following security best practices
5. **Monitor and maintain** with regular backups and updates

---

## ✅ Files at a Glance

| File | Purpose | Type |
|------|---------|------|
| `database/luna2-complete-schema.sql` | Complete DB schema | SQL |
| `setup-preprod.sh` | Automated setup | Bash Script |
| `docker-compose-preprod.yml` | Container orchestration | YAML |
| `luna2-api/Dockerfile` | API container image | Docker |
| `docker/nginx/nginx.conf` | Reverse proxy config | Config |
| `.env.example` | Environment template | Config |
| `PREPROD_DEPLOYMENT_GUIDE.md` | Full documentation | Markdown |
| `test-api.sh` | API testing suite | Bash Script |

---

**Version**: 1.0.0  
**Status**: Production Ready  
**Last Updated**: February 23, 2026
