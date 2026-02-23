# 📊 Luna2 Deployment Flow - Visual Guide

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                                                                 │
│  YOUR DOMAIN (https://yourdomain.com)                           │
│  ↓                                                              │
│  DNS A Record → Server IP (your.server.ip)                     │
│                                                                 │
└──────────────────────────┬──────────────────────────────────────┘
                           │
                           ▼
           ┌───────────────────────────────┐
           │     Nginx Reverse Proxy       │
           │  (Port 80 → 443)              │
           │  (SSL/TLS)                    │
           │  (/etc/letsencrypt/)          │
           └────────────┬──────────────────┘
                        │
           ┌────────────┴────────────┐
           │                         │
           ▼                         ▼
    ┌────────────────┐         ┌─────────────┐
    │   Luna2 API    │         │   Database  │
    │   (Port 8080)  │◄───────→│   MySQL     │
    │   OpenJDK 11   │         │   (Port 3306)
    │   Spring Boot  │         │   (Private) │
    └────────────────┘         └─────────────┘

  (All inside Docker containers → automatic management)
```

---

## Deployment Timeline

```
TIME    STEP                           DURATION    CUMULATIVE
────────────────────────────────────────────────────────────────

0:00    1. SSH Connect                 1 min       1 min
        └─ ssh root@server-ip

1:00    2. Verify OS & Resources       3 min       4 min
        └─ lsb_release, free -h, df -h

4:00    3. System Updates              5 min       9 min
        └─ apt update && apt upgrade

9:00    4. Firewall Config             3 min       12 min
        └─ ufw enable, open 22/80/443

12:00   5. Install Docker              10 min      22 min
        └─ docker, docker-compose

22:00   6. Clone Repository            2 min       24 min
        └─ git clone Luna2

24:00   7. Initialize Management       2 min       26 min
        └─ ./init-management.sh

26:00   8. Configure .env              3 min       29 min
        └─ nano .env (change passwords!)

29:00   9. Build Docker Images         15 min      44 min ☕
        └─ docker-compose build

44:00   10. Start Containers           2 min       46 min
         └─ docker-compose up -d

46:00   11. Add Domain                 5 min       51 min
         └─ ./manage-domains.sh add domain.com

51:00   12. Test Domain                2 min       53 min
         └─ ./manage-domains.sh test domain.com

53:00   13. Login & Change Password    2 min       55 min
         └─ Browser → https://domain.com

55:00   14. Setup Manager Panel        3 min       58 min (optional)
         └─ cd server-manager && npm start

58:00   15. Configure Portainer        2 min       60 min (optional)
         └─ Uncomment in docker-compose

        ... additional configuration steps ...

══════════════════════════════════════════════════════════════════
TOTAL TIME: 30-45 minutes (with coffee ☕)
```

---

## Step-by-Step Execution Order

```
PHASE 1: SYSTEM PREPARATION (12 minutes)
├─ STEP 1: SSH Access
├─ STEP 2: Verify OS & Resources
├─ STEP 3: Update & Upgrade System  ← Ubuntu packages
├─ STEP 4: Configure Firewall       ← Open ports 80,443
└─ STEP 5: Install Docker           ← Required for deployment

                            ⬇️ Server Ready

PHASE 2: LUNA2 INSTALLATION (25 minutes)
├─ STEP 6: Clone Repository
├─ STEP 7: Initialize Management    ← Create directories
├─ STEP 8: Configure Environment    ← .env (CHANGE PASSWORDS!)
├─ STEP 9: Build Docker Images      ← 15 min wait ☕
└─ STEP 10: Start Containers        ← up -d

                            ⬇️ Luna2 Running

PHASE 3: PRODUCTION SETUP (10 minutes)
├─ STEP 11: Add First Domain        ← DNS name → HTTPS
├─ STEP 12: Test Domain
├─ STEP 13: Login & Change Password ← IMPORTANT!
├─ STEP 14: Manager Panel (opt.)    ← Web UI
└─ STEP 15: Portainer (opt.)        ← Docker UI

                            ⬇️ Production Ready ✅

PHASE 4: OPERATIONS (daily)
├─ Monitor: docker-compose ps
├─ Logs: docker-compose logs -f
├─ Backup: mysqldump (weekly)
├─ Renew SSL: manage-domains.sh renew (monthly)
└─ Updates: apt update && apt upgrade (monthly)
```

---

## Quick Reference: Commands Order

```bash
# 📍 SESSION START
ssh root@your-server-ip

# 🔧 Phase 1: System Prep
lsb_release -a
free -h
df -h /
sudo ufw enable && sudo ufw allow 22/tcp && sudo ufw allow 80/tcp && sudo ufw allow 443/tcp
# (Docker install via prepare-server.sh or manual)

# 🚀 Phase 2: Luna2 Setup
cd ~
git clone https://github.com/zensoftwareit-ops/Luna2.git
cd Luna2
cp .env.example .env
nano .env          # ← EDIT PASSWORDS HERE
./init-management.sh
docker-compose -f docker-compose-preprod.yml build
docker-compose -f docker-compose-preprod.yml up -d

# ✅ Phase 3: Production
./manage-domains.sh add yourdomain.com
./manage-domains.sh test yourdomain.com
# Open browser: https://yourdomain.com
# Login: admin / Admin@123456
# Change password immediately!

# 📊 Phase 4: Monitoring
docker-compose -f docker-compose-preprod.yml ps
docker-compose -f docker-compose-preprod.yml logs -f

# 💾 Backup
docker-compose -f docker-compose-preprod.yml exec mysql \
  mysqladmin -u luna2_user -p luna2 > ~/backup.sql
```

---

## Decision Tree

```
START: Want to deploy Luna2?
  │
  ├─→ Have Ubuntu 22 LTS server? [YES/NO]
  │   └─ NO  → Get one (Hetzner €2.99/mo, DO $12/mo)
  │   └─ YES → Continue
  │
  ├─→ Know your server password/SSH key? [YES/NO]
  │   └─ NO  → Get access credentials
  │   └─ YES → Continue
  │
  ├─→ Have a domain? [YES/NO]
  │   └─ NO  → Register one (optional, but recommended)
  │   └─ YES → Set A record to server IP
  │
  ├─→ Ready 30-45 minutes? [YES/NO]
  │   └─ NO  → Schedule for later
  │   └─ YES → Start! ↓
  │
  ├─→ STEP 1-5: Prep Server (12 min)
  │   └─ System updates, Docker install, Firewall
  │
  ├─→ STEP 6-10: Install Luna2 (25 min)
  │   └─ Clone, .env, Build, Start (wait 15 min for build)
  │
  ├─→ STEP 11-13: Production Setup (10 min)
  │   └─ Add domain, Test, Login
  │
  ├─→ Success? [YES/NO]
  │   └─ YES → CONGRATULATIONS! 🎉
  │   └─ NO  → Check logs: docker-compose logs
  │
  └─→ Daily maintenance:
      ├─ Monitor: docker-compose ps
      ├─ Backup: weekly
      ├─ Renew SSL: monthly
      └─ Updates: monthly
```

---

## Resource Requirements by Phase

```
PHASE 1: System Prep
├─ CPU: 1-2 cores (light)
├─ RAM: 500 MB used
├─ Disk: 100 MB
└─ Network: 50 MB download

PHASE 2: Building Images
├─ CPU: 2-4 cores (heavy) ⚠️ WAIT 15 MIN
├─ RAM: 2-3 GB used
├─ Disk: 3-5 GB (Docker images)
└─ Network: 500 MB - 1 GB download

PHASE 3: Running Containers
├─ CPU: minimal (idle) → peaks with requests
├─ RAM: 1.5-2 GB baseline
├─ Disk: 500 MB + database growth
└─ Network: minimal (local traffic)

PHASE 4: Operations
├─ CPU: <5% average
├─ RAM: 1-2 GB baseline
├─ Disk: grows with data (1-10 GB/month typical)
└─ Network: minimal
```

---

## Success Criteria ✅

After each phase, verify:

**PHASE 1 Complete:**
```
✓ OS identified (Ubuntu 22.04)
✓ Firewall enabled
✓ Ports 80, 443 open
✓ Docker working (docker ps)
```

**PHASE 2 Complete:**
```
✓ All 3 containers "Up (healthy)"
  - luna2-mysql
  - luna2-api
  - luna2-nginx
✓ No error messages in logs
```

**PHASE 3 Complete:**
```
✓ Test shows 3 green checkmarks
✓ Domain loads in browser
✓ HTTPS without warnings
✓ Admin login works
```

**PHASE 4 Operations:**
```
✓ docker-compose ps shows healthy
✓ Database backed up
✓ SSL renewal scheduled
✓ Monitoring in place
```

---

## Common Mistakes to Avoid ❌

| Mistake | Impact | Fix |
|---------|--------|-----|
| Run without `sudo` (not in docker group) | "Permission denied" | `sudo usermod -aG docker $USER` + logout/in |
| Wrong password in .env | Build fails | Edit .env, restart: `docker-compose down && up -d` |
| DNS not pointing to server | "Domain not found" | Wait 10 min for propagation, check with `nslookup` |
| Port 80 blocked | "Connection timeout" | Open firewall: `sudo ufw allow 80` |
| Not changing default password | Security risk | Change admin password ASAP |
| Stopping without backup | Data loss | Backup before maintenance: `mysqldump` |
| Building without disk space | Build fails | Check: `df -h /` (need 5 GB free) |

---

## Estimated Costs (12 months)

| Provider | Specs | Monthly | Yearly |
|----------|-------|---------|--------|
| Hetzner Cloud | 2 CPU, 4 GB, 40 GB | €2.99 | €35.88 |
| Contabo | 4 CPU, 8 GB, 200 GB | €4.99 | €59.88 |
| DigitalOcean | 2 CPU, 4 GB, 80 GB | $12 | $144 |
| AWS t3.medium | 2 CPU, 4 GB, 30 GB | ~$20 | ~$240 |
| Linode | 4 CPU, 8 GB, 160 GB | $40 | $480 |

**Recommendation:** Start with **Hetzner** (€2.99) or **Contabo** (€4.99) for testing, scale up if needed.

---

## Next Steps After Deployment ✅

1. **Security Hardening (1 hour)**
   - [ ] SSH key authentication only
   - [ ] Disable root login
   - [ ] Setup fail2ban
   - [ ] Regular OS updates

2. **Monitoring & Alerts (2 hours)**
   - [ ] Setup uptime monitoring
   - [ ] SSL expiry alerts
   - [ ] Disk space alerts
   - [ ] Daily backups to cloud

3. **Scaling (optional)**
   - [ ] Add more domains
   - [ ] Configure email (Gmail SMTP)
   - [ ] Setup calendar sync
   - [ ] Add more users

4. **Backup Strategy (30 min)**
   - [ ] Daily DB backup to S3
   - [ ] Weekly off-site backup
   - [ ] Test restore procedure

---

**Ready? Start with Step 1! 🚀**
