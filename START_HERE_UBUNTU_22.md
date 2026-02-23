# 🎯 Guida Completa Deployment Luna2 - Ubuntu 22 LTS

Hai scelto **Ubuntu 22 LTS**! Ecco come procedere **passo-passo** da zero fino a avere tutto running in produzione.

---

## 📚 Quale Documento Leggere?

### 🏃 **FRETTA? (5 min)**
Leggi: **[QUICK_DEPLOY_UBUNTU.md](QUICK_DEPLOY_UBUNTU.md)**
- 3 step essenziali
- ~30 minuti totali
- Perfetto per chi sa cosa fa

### 📖 **PRIMO DEPLOYMENT? (seguire step-by-step)**
Leggi: **[DEPLOYMENT_UBUNTU_22_LTS.md](DEPLOYMENT_UBUNTU_22_LTS.md)**
- 20 step dettagliati
- Spiegazione per ogni comando
- Cosa aspettarsi ad ogni step
- Troubleshooting integrato
- **SUGGERITO per la PRIMA VOLTA**

### 📊 **VISUAL LEARNER? (capire architecture)**
Leggi: **[DEPLOYMENT_FLOW_DIAGRAM.md](DEPLOYMENT_FLOW_DIAGRAM.md)**
- Diagrammi ASCII
- Timeline visuale
- Decision tree
- Requisiti per fase
- Checklist success criteria

---

## ⚡ RIASSUNTO VELOCISSIMO (3 step)

Se sei esperto e vuoi solo ricordare l'ordine:

```bash
# STEP 1: Prepara server (Docker, Firewall)
ssh root@your-server-ip
wget https://raw.githubusercontent.com/zensoftwareit-ops/Luna2/main/prepare-server.sh
chmod +x prepare-server.sh && ./prepare-server.sh

# STEP 2: Installa Luna2 (15 min wait per build)
cd ~ && git clone https://github.com/zensoftwareit-ops/Luna2.git
cd Luna2
cp .env.example .env && nano .env  # Change passwords!
docker-compose -f docker-compose-preprod.yml build
docker-compose -f docker-compose-preprod.yml up -d

# STEP 3: Aggiungi dominio (DNS A record required)
./manage-domains.sh add yourdomain.com        # Rispondi: y per SSL
./manage-domains.sh test yourdomain.com       # Deve avere 3 ✓
# Open browser: https://yourdomain.com
# Login: admin/Admin@123456  CAMBIA SUBITO!
```

**Fatto in ~45 minuti. NEXT!** 🚀

---

## 🎬 STEP-BY-STEP COMPLETO (CONSIGLIATO)

Se è il tuo **primo deployment**, segui questo ordine:

### FASE 1: Preparazione Server (12 min)

| # | Step | Comando | Output Atteso |
|---|------|---------|---------------|
| 1 | SSH Access | `ssh root@ip` | `root@ubuntu:~$` |
| 2 | Verify OS | `lsb_release -a` | Ubuntu 22.04 |
| 3 | Update System | `apt update && apt upgrade -y` | No errors |
| 4 | Firewall | `ufw enable; ufw allow 22,80,443/tcp` | Status: active |
| 5 | Docker Install | Run script o manuale | `Docker version 25.x` |

**Checkpoint:** `docker ps` → OK ✓

### FASE 2: Installa Luna2 (25 min)

| # | Step | Comando |
|---|------|---------|
| 6 | Clone | `git clone ...Luna2.git && cd Luna2` |
| 7 | Initialize | `./init-management.sh` |
| 8 | Configure | `cp .env.example .env && nano .env` |
| 9 | Build | `docker-compose build` ☕ 15 min wait |
| 10 | Start | `docker-compose up -d` |

**Checkpoint:** `docker-compose ps` → All "healthy" ✓

### FASE 3: Produzione (10 min)

| # | Step | Comando |
|---|------|---------|
| 11 | Domain | `./manage-domains.sh add yourdomain.com` |
| 12 | Test | `./manage-domains.sh test yourdomain.com` |
| 13 | Login | Browser → `https://yourdomain.com` → Change password |
| 14 | Backup | `docker-compose exec mysql mysqldump ...` |
| 15 | Monitor | `docker-compose ps` + `docker stats` |

**Checkpoint:** HTTPS works, login succeeds ✓

---

## 📋 PRE-DEPLOYMENT CHECKLIST

Prima di iniziare, assicurati di avere:

- [ ] **Server Ubuntu 22.04 LTS** (non test su Ubuntu 20/21)
- [ ] **SSH access** con password o chiave
- [ ] **Dominio registrato** (opzionale ma consigliato)
- [ ] **DNS A record** (se usi dominio) puntato al server IP
- [ ] **Porte aperte**: 22 (SSH), 80 (HTTP), 443 (HTTPS)
- [ ] **Minimo 20 GB disco** libero (per docker, images, DB)
- [ ] **Minimo 4 GB RAM** (2 cores CPU minimo)
- [ ] **Tempo disponibile**: 45 minuti senza interruzioni
- [ ] **Connessione internet stabil** durante build (15 min)

**Not ready?** Prima di iniziare:
- [ ] Ordina server (Hetzner €2.99, Contabo €4.99)
- [ ] Attendi ~2-3 min per accensione server
- [ ] Registra dominio (Namecheap, GoDaddy)
- [ ] Configura DNS A record (attendi 10 min propagazione)

---

## 🎯 ORDINE ESATTO (come eseguire)

```
Start: ora sei nel tuo PC locale
    ↓
Step 1: SSH nel server → terminal sessione 1 aperta
    ↓
Step 2-5: Prepara server (12 min)
    ↓
Step 6: Clone Luna2
    ↓
Step 7: Inizializza estructura
    ↓
Step 8: Edita .env (CHANGE PASSWORDS!) ← IMPORTANT!
    ↓
Step 9: Build Docker Images (15 min WAIT)
    │   (leggi intanto: DEPLOYMENT_FLOW_DIAGRAM.md)
    │   (oppure fai un caffè ☕)
    ↓
Step 10: Avvia containers
    ↓
Step 11: Aggiungi dominio (DNS deve essere pronto!)
    ↓
Step 12: Testa dominio (deve avere 3 ✓)
    ↓
Step 13: Apri browser HTTPS → Login
    ↓
Step 14: CAMBIA PASSWORD ADMIN (VITALE!)
    ↓
Step 15: Configura backup/monitoring
    ↓
STATUS: PRODUCTION READY ✅
```

---

## 📱 COSA FARE DOPO DEPLOYMENT

### Subito (24 ore)

```bash
# 1. Change admin password
# → Open app → Settings → Users → Change password

# 2. Setup first backup
docker-compose exec mysql mysqldump -u luna2_user -p luna2 > backup.sql

# 3. Verify domains are accessible
https://yourdomain.com
http://yourdomain.com  → should redirect to HTTPS

# 4. Test API (for automation)
curl -X POST https://yourdomain.com/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"NewPassword"}'
```

### Questa Settimana

```bash
# Setup automatic SSL renewal
crontab -e
# Add: 0 3 * * 0 /root/Luna2/manage-domains.sh renew

# Setup log rotation
# (prevent disk from filling up)

# Add firewall rules (optional if local network)
# sudo ufw allow from 10.0.0.0/8
```

### Questo Mese

```bash
# Add other domains
./manage-domains.sh add domain2.com
./manage-domains.sh add domain3.com

# Configure email (optional)
# Edit .env: MAIL_SMTP_*

# Setup off-site backups (AWS S3, etc)

# Enable Portainer UI (optional)
# uncomment in docker-compose.yml
```

---

## 🔗 QUICK LINKS

| Documento | Per Chi | Tempo |
|-----------|---------|-------|
| **[QUICK_DEPLOY_UBUNTU.md](QUICK_DEPLOY_UBUNTU.md)** | Esperti | 5 min lettura |
| **[DEPLOYMENT_UBUNTU_22_LTS.md](DEPLOYMENT_UBUNTU_22_LTS.md)** | Principianti | 20 min lettura |
| **[DEPLOYMENT_FLOW_DIAGRAM.md](DEPLOYMENT_FLOW_DIAGRAM.md)** | Visual learners | 10 min lettura |
| [SERVER_REQUIREMENTS.md](SERVER_REQUIREMENTS.md) | Scelta server | 10 min |
| [SERVER_MANAGEMENT_PANEL.md](SERVER_MANAGEMENT_PANEL.md) | Gestione domini | 15 min |
| [SERVER_MANAGER_UI.md](SERVER_MANAGER_UI.md) | Pannello web | 10 min |
| [PORTAINER_SETUP.md](PORTAINER_SETUP.md) | Docker UI | 5 min |

---

## ⚠️ ERRORI COMUNI

### Build fallisce
```bash
# Probabile: non abbastanza spazio o memoria
df -h /              # Serve 5 GB libero
free -h              # Serve 2 GB RAM libero

# Soluzione:
docker system prune -a
# oppure:
docker volume prune
```

### "502 Bad Gateway"
```bash
# Luna2 API non risponde
docker-compose logs luna2-api

# Probabile: Database non connesso
docker-compose logs luna2-mysql

# Fix:
docker-compose restart luna2-api
# oppure restart tutto:
docker-compose restart
```

### Dominio non c'è il certificato SSL
```bash
# DNS non propagato ancora
nslookup yourdomain.com  # deve mostrare il tuo IP

# Aspetta 10 minuti e riprova:
./manage-domains.sh ssl yourdomain.com
```

### Accesso negato a docker
```bash
# Non sei nel gruppo docker
sudo usermod -aG docker $USER

# Logout e login di nuovo (O)
newgrp docker
```

---

## ✅ VERIFICA FINALE

Dopo tutti i 15 step, verifica:

```bash
# 1. Container status
docker-compose -f ~/Luna2/docker-compose-preprod.yml ps
# Atteso: 3 righe con "Up (healthy)"

# 2. Test dominio
curl https://yourdomain.com/api/v1/health
# Atteso: {"status":"UP"}

# 3. Check logs (no errors)
docker-compose -f ~/Luna2/docker-compose-preprod.yml logs
# Atteso: No ERROR messages

# 4. Disk space
df -h /
# Atteso: >50% disponibile

# 5. Backup exists
ls -lh ~/backup-*.sql
# Atteso: file recente
```

✅ **Tutto verde? FATTO!** 🎉

---

## 📞 Help & Support

| Problema | Azione |
|----------|--------|
| Comando non trovato | Controlla path (es. `~/Luna2/manage-domains.sh`) |
| Permission denied | Usa `sudo` o aggiungi user a docker group |
| Dominio non raggiungibile | Verifica DNS con `nslookup yourdomain.com` |
| Container non parte | Leggi logs: `docker-compose logs` |
| Database non connesso | Riavvia: `docker-compose restart luna2-mysql` |
| Certificato scaduto | Rinnova: `./manage-domains.sh renew` |

**Per debug completo:** `docker-compose logs -f --all`

---

## 🚀 PROSSIMI STEP DOPO DEPLOYMENT

Una volta che Luna2 è up, considera:

1. **Sicurezza** (1-2 ore)
   - Disabilita root SSH login
   - Setup SSH key authentication
   - Install fail2ban (brute force protection)
   - Regular OS updates

2. **Monitoring** (1 ora)
   - Setup uptime checks (Uptime Robot, etc)
   - Alert su SSL expiry
   - Monitor CPU/RAM/Disk
   - Daily backup checks

3. **Scalabilità** (as needed)
   - Aggiungi più domini
   - Configura email (Gmail SMTP per notifiche)
   - Setup calendar sync
   - Add users/teams

4. **Backup** (1 ora)
   - Daily backup to cloud (S3, etc)
   - Test restore procedure
   - Off-site copy

---

## 📊 Timeline Riassuntiva

```
Today:
  ✓ Server live e raggiungibile
  ✓ Luna2 installato
  ✓ Primo dominio configurato
  ✓ Admin password cambiata

This Week:
  ✓ SSL auto-renewal configured
  ✓ Backup working
  ✓ Log rotation setup

This Month:
  ✓ Security hardening done
  ✓ More domains added
  ✓ Monitoring configured
  ✓ Off-site backups active
```

---

**Pronto a iniziare?**

**Scegli il tuo percorso:**

- 🏃 **Fretta?** → [QUICK_DEPLOY_UBUNTU.md](QUICK_DEPLOY_UBUNTU.md) (3 step)
- 📖 **Primo deployment?** → [DEPLOYMENT_UBUNTU_22_LTS.md](DEPLOYMENT_UBUNTU_22_LTS.md) (20 step)
- 📊 **Visual?** → [DEPLOYMENT_FLOW_DIAGRAM.md](DEPLOYMENT_FLOW_DIAGRAM.md) (diagrammi)

**Via, buon deployment! 🚀**
