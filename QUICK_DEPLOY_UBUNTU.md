# ⚡ Luna2 Quick Deployment - Ubuntu 22 LTS

**3 comandi per avviare tutto in 30 minuti**

## Step 1️⃣: Prepara Server (10 min)

```bash
# SSH nel server
ssh root@your-server-ip

# Una sola volta - prepara tutto (Docker, firewall, etc)
# Scarica lo script:
wget https://raw.githubusercontent.com/zensoftwareit-ops/Luna2/main/prepare-server.sh
chmod +x prepare-server.sh

# Esegui (risponde a prompt se necessario)
./prepare-server.sh

# Output atteso:
# ✓ Docker installed
# ✓ Firewall configured
# ✓ Ports 80, 443 open
```

---

## Step 2️⃣: Installa Luna2 (15 min)

```bash
# Clone repo
cd ~
git clone https://github.com/zensoftwareit-ops/Luna2.git
cd Luna2

# Configura variabili (cambiar password!)
cp .env.example .env
nano .env
# Modifica: MYSQL_ROOT_PASSWORD, DB_PASSWORD, JWT_SECRET
# Salva: CTRL+X → Y → ENTER

# Genera database e immagini Docker
docker-compose -f docker-compose-preprod.yml build

# ASPETTA 10-15 MINUTI... (scarica immagini, compila codice)

# Avvia
docker-compose -f docker-compose-preprod.yml up -d

# Verifica tutto è "healthy"
docker-compose -f docker-compose-preprod.yml ps
sleep 30
docker-compose -f docker-compose-preprod.yml ps
```

✅ **Se vedi tutti "Up (healthy)" → FUNZIONA!**

---

## Step 3️⃣: Configura Dominio (5 min)

```bash
# Prerequisito: A record DNS deve puntare al tuo server IP
# es. yourdomain.com A your-server-ip

# Checki DNS propagazione
nslookup yourdomain.com
# Deve mostrare il tuo IP

# Aggiungi dominio con SSL automatico
cd ~/Luna2
./manage-domains.sh add yourdomain.com
# Quando chiede: "Generate SSL? (y/n)" → Rispondi: y

# Test
./manage-domains.sh test yourdomain.com
# Deve mostrare 3 ✓
```

✅ **Se tutto ha ✓ → È LIVE!**

---

## 📌 Accedi all'Applicazione

**URL:** https://yourdomain.com  
**User:** admin  
**Pass:** Admin@123456  

⚠️ **CAMBIA SUBITO LA PASSWORD!**

---

## 🎯 Quick Commands Reference

```bash
# Status
docker-compose -f ~/Luna2/docker-compose-preprod.yml ps

# Logs
docker-compose -f ~/Luna2/docker-compose-preprod.yml logs -f luna2-api

# Aggiungi altro dominio
~/Luna2/manage-domains.sh add another-domain.com

# Rinova SSL (mensile)
~/Luna2/manage-domains.sh renew

# Backup database
docker-compose -f ~/Luna2/docker-compose-preprod.yml exec mysql \
  mysqldump -u luna2_user -p luna2 > ~/backup-$(date +%Y%m%d).sql

# Stop tutto
docker-compose -f ~/Luna2/docker-compose-preprod.yml down

# Restart API
docker-compose -f ~/Luna2/docker-compose-preprod.yml restart luna2-api
```

---

## ⚠️ Problemi?

| Problema | Soluzione |
|----------|-----------|
| Container non parte | `docker-compose logs` (leggi errore) |
| Dominio non raggiungibile | `nslookup yourdomain.com` (controlla DNS) |
| "502 Bad Gateway" | `docker-compose restart luna2-api` |
| Certificato non generato | DNS non propagato, aspetta 10 min e riprova |
| Accesso negato | Assicurati di essere il proprietario di `~/Luna2` |

---

## 💾 Backup Settimanale

```bash
# Aggiungi a crontab
crontab -e

# Aggiungi questa linea:
0 0 * * 0 docker-compose -f ~/Luna2/docker-compose-preprod.yml exec mysql \
  mysqldump -u luna2_user -p luna2 > ~/backups/backup-$(date +\%Y\%m\%d).sql
```

---

**Documentazione completa:** [DEPLOYMENT_UBUNTU_22_LTS.md](DEPLOYMENT_UBUNTU_22_LTS.md)

**Fatto in 30 minuti! 🚀**
