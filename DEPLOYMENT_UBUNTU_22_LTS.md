# 🚀 Luna2 Complete Deployment Guide - Ubuntu 22 LTS (Multi-Tenant Edition)

Guida completa **passo-passo** per deployare Luna2 da zero su server Ubuntu 22 LTS con **architettura multi-tenant**.

Ogni dominio = istanza Luna2 separata + database isolato + PhpMyAdmin dedicato.

**Tempo totale: 45-60 minuti (setup gateway) + 5 minuti per cliente**

---

## 📋 Indice Rapido

1. [Prerequisiti](#-prerequisiti)
2. [Step 1-5: Preparazione Server](#step-1-5-preparazione-server)
3. [Step 6-10: Setup Luna2 Base](#step-6-10-setup-luna2-base)
4. [Step 11-15: Configurazione Multi-Tenant](#-step-11-15-configurazione-multi-tenant)
5. [Step 16-20: Testing & Monitoring](#step-16-20-testing--monitoring)

---

## 📍 Prerequisiti

**Cosa ti serve:**
- ✅ Server Ubuntu 22.04 LTS (fresh install)
- ✅ SSH access (root o sudo)
- ✅ Dominio registrato (opzionale, ma consigliato per HTTPS)
- ✅ Porte 80, 443, 22 aperte nel firewall

**Costi stimati:**
- Hetzner: €2.99/mese (2 CPU, 4GB RAM, 40GB)
- Contabo: €4.99/mese (4 CPU, 8GB RAM, 200GB)
- DigitalOcean: $12/mese (2 CPU, 4GB RAM, 80GB)

---

## ⏱️ STEP 1-5: PREPARAZIONE SERVER

### ✅ STEP 1: Accedi al Server (2 min)

```bash
# Connettiti via SSH
ssh root@your-server-ip

# Oppure se hai user non-root:
ssh ubuntu@your-server-ip
```

**Output atteso:**
```
Welcome to Ubuntu 22.04.3 LTS (GNU/Linux ...)
ubuntu@luna2:~$
```

---

### ✅ STEP 2: Verifica OS e Requisiti (3 min)

```bash
# Verifica versione Ubuntu
lsb_release -a

# Verifica risorse
free -h          # Memoria
df -h /          # Disco
nproc            # CPU cores
uname -m         # Architettura (x86_64 va bene)
```

**Output atteso:**
```
Description:    Ubuntu 22.04.3 LTS
Release:        22.04

              total        used        free
Mem:           3.8Gi       200Mi       3.6Gi
Disk /dev/sda1 39G         2.0G        37G

4  ← numero CPU cores
x86_64  ← architettura corretta
```

✅ **Se tutto è OK, procedi**

---

### ✅ STEP 3: Aggiornamenti Sistema (5 min)

```bash
# Update package list
sudo apt update

# Upgrade packages
sudo apt upgrade -y

# Install essentials
sudo apt install -y \
  curl \
  wget \
  git \
  jq \
  htop \
  vim \
  nano \
  ca-certificates \
  apt-transport-https
```

**Output finale atteso:**
```
Processing triggers for ca-certificates...
Setting up wget (1.21.2-2ubuntu1) ...
```

---

### ✅ STEP 4: Configura Firewall (3 min)

```bash
# Abilita firewall
sudo ufw enable

# Apri porta SSH (IMPORTANTE!)
sudo ufw allow 22/tcp

# Apri porte HTTP/HTTPS
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp

# Verifica regole
sudo ufw status verbose
```

**Output atteso:**
```
Status: active

To                         Action      From
--                         ------      ----
22/tcp                     ALLOW       Anywhere
80/tcp                     ALLOW       Anywhere
443/tcp                     ALLOW       Anywhere
```

---

### ✅ STEP 5: Installa Docker (10 min)

```bash
# Aggiungi repository Docker ufficiale
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | \
  sudo gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg

echo \
  "deb [arch=amd64 signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] \
  https://download.docker.com/linux/ubuntu \
  $(lsb_release -cs) stable" | \
  sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

# Aggiorna package list
sudo apt update

# Installa Docker
sudo apt install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin

# Verifica installazione
docker --version
docker-compose version
```

**Output atteso:**
```
Docker version 25.0.1, build 29cf629
Docker Compose version v2.24.0
```

**Configura il tuo user (se non sei root):**

```bash
# Aggiungi user al gruppo docker
sudo usermod -aG docker $USER

# Applica cambio
newgrp docker

# Testa senza sudo
docker ps
```

---

## ⏱️ STEP 6-10: SETUP LUNA2 BASE

### ✅ STEP 6: Clone Repository (2 min)

```bash
# Vai alla home
cd ~

# Clone repository
git clone https://github.com/zensoftwareit-ops/Luna2.git

# Entra nella directory
cd Luna2

# Verifica struttura
ls -la
```

**Output atteso:**
```
total 800
drwxr-xr-x docker-compose-main.yml             (NUOVO: Gateway per multi-tenant)
drwxr-xr-x docker-compose-customer-template.yml (NUOVO: Template cliente)
drwxr-xr-x database/
drwxr-xr-x docker/
drwxr-xr-x luna2-api/
drwxr-xr-x server-manager/
-rwxr-xr-x manage-domains-multitenant.sh        (NUOVO: Multi-tenant)
-rwxr-xr-x init-management.sh
-rwxr-xr-x prepare-server.sh
```

---

### ✅ STEP 7: Inizializza Struttura di Gestione (2 min)

```bash
# Rendi eseguibili gli script
chmod +x init-management.sh \
         manage-domains.sh \
         manage-domains-multitenant.sh

# Inizializza directory structure
./init-management.sh
```

**Output atteso:**
```
🔧 Initializing server management components...
✓ Created domain tracking file
✓ Self-signed certificate created
✓ manage-domains.sh made executable
✓ manage-domains-multitenant.sh made executable
✅ Server management setup complete!
```

---

### ✅ STEP 8: Configura le Variabili d'Ambiente (3 min)

```bash
# Copia il file di esempio
cp .env.example .env

# Modifica con valori sicuri
nano .env
```

**File da modificare (`.env`):**

```bash
# Database (CAMBIA PASSWORD!)
MYSQL_ROOT_PASSWORD=ChangeMeRootPassword123!
DB_NAME=luna2
DB_USER=luna2_user
DB_PASSWORD=ChangeMeLuna2Password456!

# JWT (Genera string random)
JWT_SECRET=GeneraQualcosaRandom123456789012345

# Mail (opzionale per ora)
# MAIL_SMTP_HOST=smtp.gmail.com
# MAIL_SMTP_PORT=587
# MAIL_SMTP_USERNAME=your-email@gmail.com
# MAIL_SMTP_PASSWORD=app-password

# Timezone
TZ=Europe/Rome
```

**Salva file:**
- Premi `CTRL+X`
- Premi `Y` (yes)
- Premi `ENTER`

---

### ✅ STEP 9: Costruisci Immagini Docker (15 min) ⏳

```bash
# Costruisci l'immagine Luna2 API
docker-compose -f docker-compose-preprod.yml build

# Questo scarica immagini di base e compila il codice
# ASPETTA 10-15 MINUTI...
```

**Output during build:**
```
[+] Building 245.3s (20/20) FINISHED
...
Successfully built 1a2b3c4d5e6f
```

---

### ✅ STEP 10: Prontezza per Configurazione Multi-Tenant

Ora procediamo con la configurazione multi-tenant:

---

## 🔵 STEP 11-15: CONFIGURAZIONE MULTI-TENANT

Ideale per: SaaS, agenzia web hosting, gestisci N clienti

### ✅ STEP 11B: Avvia Gateway Nginx (2 min)

```bash
# Avvia il gateway principale (solo Nginx)
docker-compose -f docker-compose-main.yml up -d

# Verifica
docker-compose -f docker-compose-main.yml ps
```

**Output atteso:**
```
NAME              STATUS              PORTS
nginx-gateway     Up (healthy)        80/tcp, 443/tcp
portainer         Up (healthy)        9000/tcp
```

**Testa il gateway:**
```bash
curl http://localhost/health
# Output atteso: Gateway OK
```

---

### ✅ STEP 12B: Aggiungi Primo Cliente (5 min)

```bash
# Aggiungi cliente con dominio, DB, e PhpMyAdmin separati
./manage-domains-multitenant.sh add acme.com "ACME Inc"
```

**Questo comando crea automaticamente:**
- ✅ Container Luna2 dedicato
- ✅ Database MySQL separato
- ✅ PhpMyAdmin per cliente
- ✅ Nginx reverse proxy config
- ✅ Allocazione porte univoche (es: MySQL 3307, App 8081, PhpMyAdmin 8000)

**Output atteso:**
```
[INFO] Aggiungendo nuovo cliente: acme.com (ACME Inc)
[INFO] Porte allocate - MySQL: 3307, APP: 8081, PhpMyAdmin: 8000
[INFO] Generando Docker Compose per acme.com...
[INFO] Generando Nginx config per acme.com...
[SUCCESS] Cliente aggiunto completamente!

┌─────────────────────────────────────────────────┐
│ INFORMAZIONI CLIENTE                             │
├─────────────────────────────────────────────────┤
│ Dominio: acme.com
│ Cliente: ACME Inc
│ Port MySQL: 3307 (interno)
│ Port App: 8081
│ Port PhpMyAdmin: 8000
└─────────────────────────────────────────────────┘

PROSSIMI PASSI:
1. Aspetta 30s per health check
2. Configura DNS: A record per acme.com → IP server
3. Richiedi SSL: ./manage-domains-multitenant.sh ssl acme.com
4. Accedi a:
   - Luna2: https://acme.com
   - PhpMyAdmin: https://phpmyadmin.acme.com
```

---

### ✅ STEP 13B: Mostra Infoazioni Clienti (1 min)

```bash
# Lista tutti i clienti
./manage-domains-multitenant.sh list

# Mostra mapping porte
./manage-domains-multitenant.sh ports
```

**Output atteso:**
```
[✓] acme.com
    │ Client: ACME Inc
    │ Porte: MySQL=3307, App=8081, PhpMyAdmin=8000
    │ Creato: 2024-02-23 15:30:00
    → Containers: RUNNING ✓

┌────────────────────────────────────────────┐
│ PORT MAPPING                               │
├────────────────────────────────────────────┤
│ DOMINIO          MYSQL    APP      PHPMYADMIN
├────────────────────────────────────────────┤
│ acme.com         3307     8081     8000
└────────────────────────────────────────────┘
```

---

### ✅ STEP 14B: Configura DNS e SSL (7 min)

**Configurare DNS:**

Nel tuo provider DNS, aggiungi questi A record:
```
acme.com                 A → 1.2.3.4 (IP server)
www.acme.com              A → 1.2.3.4 (IP server)
phpmyadmin.acme.com       A → 1.2.3.4 (IP server)
```

⏳ **Aspetta 5-10 minuti per propagazione DNS**

**Verifica DNS:**
```bash
nslookup acme.com
# Output: acme.com has address 1.2.3.4
```

**Richiedi certificato SSL:**
```bash
./manage-domains-multitenant.sh ssl acme.com
```

**Output atteso:**
```
[INFO] Richiedendo certificato SSL per acme.com...
Let's Encrypt will save debug logs...
[SUCCESS] Certificato ottenuto per acme.com
```

---

### ✅ STEP 15B: Testa Cliente (2 min)

```bash
# Testa connessione cliente
./manage-domains-multitenant.sh test acme.com
```

**Output atteso:**
```
[SUCCESS] MySQL: OK (port 3307)
[SUCCESS] Luna2 App: OK (port 8081)
[SUCCESS] PhpMyAdmin: OK (port 8000)

[INFO] Container health:
NAME                      STATUS
mysql-acme.com            Up (healthy)
luna2-acme.com            Up (healthy)
phpmyadmin-acme.com       Up (healthy)
```

---

### ✅ ACCEDI AI SERVIZI CLIENTE

**Luna2 Application:**
```
https://acme.com
Credenziali: admin / Admin@123456
```

**PhpMyAdmin (Gestione Database):**
```
https://phpmyadmin.acme.com
Host: mysql-acme.com (o localhost:3307)
User: luna2_user
Password: Luna2User@2024
```

⚠️ **SICUREZZA: Cambia il password admin subito!**

Dentro Luna2:
1. Accedi con admin/Admin@123456
2. Vai a Settings → Users
3. Cambia password root
4. Cambia password applicazione

---

### ✅ AGGIUNGERE ULTERIORI CLIENTI (5 min per cliente)

```bash
# Aggiungi secondo cliente
./manage-domains-multitenant.sh add example.com "Example Corp"
# Porte allocate automaticamente: MySQL 3308, App 8082, PhpMyAdmin 8001

# Aggiungi terzo cliente
./manage-domains-multitenant.sh add test.com "Test Customer"
# Porte allocate automaticamente: MySQL 3309, App 8083, PhpMyAdmin 8002

# Continua...
```

Ogni cliente avrà:
- ✅ Istanza Luna2 completamente separata
- ✅ Database isolato (niente commistione dati)
- ✅ PhpMyAdmin dedicated
- ✅ SSL certificate individuale
- ✅ Network Docker isolata

---

### ✅ OPTIONAL: Server Manager Web UI (2 min)

Gestisci i clienti da browser UI (invece di CLI):

```bash
# Installa Node.js (se non presente)
curl -fsSL https://deb.nodesource.com/setup_18.x | sudo -E bash -
sudo apt install -y nodejs

# Installa dipendenze
cd ~/Luna2/server-manager
npm install

# Avvia in background
npm start &
```

**Accedi:**
```
http://localhost:8888
```

**Features disponibili:**
- ✅ Visualizzare lista clienti
- ✅ Aggiungere nuovo cliente
- ✅ Rimuovere cliente
- ✅ Controllare status container per cliente
- ✅ Riavviare/Fermare/Avviare servizi per cliente
- ✅ Visualizzare logs per cliente
- ✅ Richiedere/Rinnovare SSL
- ✅ Accesso diretto a PhpMyAdmin del cliente

---

## ⏱️ STEP 16-20: TESTING & MONITORING

### ✅ STEP 16: Testa API (2 min)

```bash
# Ottieni token JWT
TOKEN=$(curl -s -X POST https://yourdomain.com/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"Admin@123456"}' | jq -r '.token')

# Testa endpoint GET
curl -H "Authorization: Bearer $TOKEN" \
  https://yourdomain.com/api/v1/users

# Output atteso: lista utenti in JSON
```

---

### ✅ STEP 17: Backup Database (2 min)

```bash
# Backup cliente acme.com
docker-compose -f ~/Luna2/docker-compose-customer-acme.com.yml exec mysql \
  mysqldump -u luna2_user -p luna2 > ~/backup-acme-$(date +%Y%m%d).sql

# Backup tutti i clienti
for domain in $(./manage-domains-multitenant.sh list | awk '{print $1}'); do
  docker-compose -f ~/Luna2/docker-compose-customer-${domain}.yml exec mysql \
    mysqldump -u luna2_user -p luna2 > ~/backup-${domain}-$(date +%Y%m%d).sql
done
```

**Verifica:**
```bash
ls -lh ~/backup-*.sql
```

**Salva in luogo sicuro! (es. cloud, backup drive, etc.)**

---

### ✅ STEP 18: Setup Log Rotation (2 min)

```bash
# Evita che i logs riempiano il disco
sudo cat > /etc/logrotate.d/luna2 << 'EOF'
/root/Luna2/logs/**/*.log {
    daily
    rotate 14
    compress
    delaycompress
    notifempty
    missingok
}
EOF

# Testa
sudo logrotate -d /etc/logrotate.d/luna2
```

---

### ✅ STEP 19: Setup Auto-Renewal SSL (1 min)

```bash
# Cron per rinnovare certificati di TUTTI i clienti ogni settimana
(crontab -l 2>/dev/null; echo "0 3 * * 0 /root/Luna2/manage-domains-multitenant.sh renew >> /var/log/certbot.log 2>&1") | crontab -

# Verifica
crontab -l | grep manage-domains
```

---

### ✅ STEP 20: Monitoraggio Continuo (2 min)

```bash
# Status di tutti i clienti
./manage-domains-multitenant.sh list

# Status di un cliente specifico
./manage-domains-multitenant.sh status acme.com

# Vedi logs di un cliente
./manage-domains-multitenant.sh logs acme.com luna2

# Uso risorse (Docker)
docker stats

# Spazio disco
df -h /

# Mapping porte allocate
./manage-domains-multitenant.sh ports
```

---

## 📊 CHECKLIST FINALE

### Verifiche Comuni

```bash
# ✓ Server Ubuntu 22 LTS
uname -a | grep Ubuntu

# ✓ Docker installato e running
docker ps

# ✓ Gateway Nginx online
curl http://localhost/health
```

### Verifiche Multi-Tenant

```bash
# ✓ Clienti registrati
./manage-domains-multitenant.sh list

# ✓ Mapping porte corrette
./manage-domains-multitenant.sh ports

# ✓ Almeno un cliente aggiunto e healthy
./manage-domains-multitenant.sh status acme.com

# ✓ Dominio cliente raggiungibile su HTTPS
curl https://acme.com -I

# ✓ PhpMyAdmin raggiungibile
curl https://phpmyadmin.acme.com -I

# ✓ Backup clienti eseguiti
ls -lh ~/backup-customer-*.sql

# ✓ SSL certificati validi
echo | openssl s_client -servername acme.com \
  -connect acme.com:443 2>/dev/null | \
  openssl x509 -noout -dates
```

---

## 🎯 COMMAND CHEAT SHEET - MULTI-TENANT

**Gestione Clienti:**
```bash
# Aggiungere cliente
./manage-domains-multitenant.sh add newdomain.com "Customer Name"

# Listare clienti
./manage-domains-multitenant.sh list

# Mostrare porte
./manage-domains-multitenant.sh ports

# Testare cliente
./manage-domains-multitenant.sh test newdomain.com

# Status cliente
./manage-domains-multitenant.sh status newdomain.com

# Rinnovare SSL cliente
./manage-domains-multitenant.sh ssl newdomain.com

# Rinnovare SSL (tutti)
./manage-domains-multitenant.sh renew

# Logs cliente
./manage-domains-multitenant.sh logs newdomain.com luna2

# Rimuovere cliente
./manage-domains-multitenant.sh remove newdomain.com
```

**Container Management:**
```bash
# Docker compose per cliente
docker-compose -f docker-compose-customer-DOMINIO.yml up -d
docker-compose -f docker-compose-customer-DOMINIO.yml ps
docker-compose -f docker-compose-customer-DOMINIO.yml logs luna2-DOMINIO
docker-compose -f docker-compose-customer-DOMINIO.yml restart luna2-DOMINIO
```

**Database:**
```bash
# Backup cliente
docker-compose -f docker-compose-customer-DOMINIO.yml exec mysql \
  mysqldump -u luna2_user -p luna2 > backup-DOMINIO.sql

# Restore cliente
docker-compose -f docker-compose-customer-DOMINIO.yml exec -T mysql \
  mysql -u luna2_user -p luna2 < backup-DOMINIO.sql

# Accedi MySQL CLI cliente
docker-compose -f docker-compose-customer-DOMINIO.yml exec mysql \
  mysql -u luna2_user -p
```

**Monitoraggio:**
```bash
# Status gateway
docker-compose -f docker-compose-main.yml ps

# Status clienti
./manage-domains-multitenant.sh list

# Status cliente specifico
./manage-domains-multitenant.sh status DOMINIO

# Resource usage
docker stats

# Disk usage
df -h /

# Memory
free -h

# CPU load
uptime
```

---

## ⚠️ PROBLEMI COMUNI

### Container non parte
```bash
# Vedi gli errori
./manage-domains-multitenant.sh logs acme.com luna2

# Se errore di memoria/disco:
docker system prune -a    # Pulisci Docker
df -h /                   # Verifica spazio disco
free -h                   # Verifica memoria
```

### Dominio non raggiungibile
```bash
# Verify DNS
nslookup yourdomain.com   # Deve mostrare il tuo IP

# Verifica nginx
docker logs nginx-gateway

# Test locale
curl http://localhost -H "Host: yourdomain.com"
```

### "502 Bad Gateway"
```bash
# Cliente non risponde
./manage-domains-multitenant.sh logs acme.com luna2

# Check database connection
./manage-domains-multitenant.sh logs acme.com mysql

# Riavvia cliente
docker-compose -f docker-compose-customer-acme.com.yml restart
```

### Certificato SSL scaduto
```bash
# Rinnova un cliente
./manage-domains-multitenant.sh renew acme.com

# Rinnova tutti
./manage-domains-multitenant.sh renew

# Verifica data
echo | openssl s_client -servername yourdomain.com \
  -connect yourdomain.com:443 2>/dev/null | \
  openssl x509 -noout -dates
```

---

## 📚 DOCUMENTAZIONE CORRELATA

- [MULTI_TENANT_ARCHITECTURE.md](MULTI_TENANT_ARCHITECTURE.md) - Architettura multi-tenant dettagliata
- [SERVER_REQUIREMENTS.md](SERVER_REQUIREMENTS.md) - Dettagli server
- [DOCKER_DEPLOYMENT_CHECKLIST.md](DOCKER_DEPLOYMENT_CHECKLIST.md) - Checklist dettagliata
- [SERVER_MANAGEMENT_PANEL.md](SERVER_MANAGEMENT_PANEL.md) - Gestione domini avanzata
- [SERVER_MANAGER_UI.md](SERVER_MANAGER_UI.md) - Pannello web UI
- [PORTAINER_SETUP.md](PORTAINER_SETUP.md) - Setup Portainer

---

## 🎉 FATTO!

✅ Server Ubuntu 22 LTS configurato
✅ Docker installato e running
✅ Nginx Gateway online e ready per clienti
✅ Primo cliente aggiunto (clone per altri)
✅ Sistema di SSL automatico per clienti
✅ Gestione clienti tramite CLI + Web UI
✅ Isolamento totale dati tra clienti
✅ Scaling a N clienti pronto

**Tempo totale:** 45-60 minuti (setup) + 5 min per cliente

---

**Prossimi step:**

- Aggiungi più clienti (5 min l'uno): `./manage-domains-multitenant.sh add domain.com "Customer"`
- Configura server-manager UI web (optional per monitoraggio centralizzato)
- Sincronizza backup su cloud (S3, Rclone, etc.)
- Setup monitoring centralizzato (Prometheus, Grafana)
- Configura mail (SMTP) per notifiche
- Configura OAuth (Google, Microsoft) per login federato

---

**Domande? Vedi la documentazione o controlla i log:**
```bash
# Multi-tenant logs
./manage-domains-multitenant.sh logs acme.com luna2
docker logs nginx-gateway
```

**Buona fortuna!** 🚀

````
