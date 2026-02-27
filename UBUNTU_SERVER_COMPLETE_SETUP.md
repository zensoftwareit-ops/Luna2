# 🖥️ Guida Installazione Server Ubuntu - Luna2 Complete Setup

**Versione:** 2.0  
**Data:** Febbraio 2026  
**Sistema Operativo:** Ubuntu 22.04 LTS / 24.04 LTS

Questa guida ti porta da un server Ubuntu nuovo di zecca a un sistema Luna2 multi-tenant completo con pannello di gestione.

---

## 📋 Indice

1. [Requisiti Hardware](#requisiti-hardware)
2. [Prerequisiti Software](#prerequisiti-software)
3. [Fase 1: Preparazione Server](#fase-1-preparazione-server)
4. [Fase 2: Installazione Docker](#fase-2-installazione-docker)
5. [Fase 3: Installazione Node.js e Tools](#fase-3-installazione-nodejs-e-tools)
6. [Fase 4: Preparazione Template Luna2](#fase-4-preparazione-template-luna2)
7. [Fase 5: Installazione Server Manager](#fase-5-installazione-server-manager)
8. [Fase 6: Configurazione Firewall](#fase-6-configurazione-firewall)
9. [Fase 7: First Run e Verifica](#fase-7-first-run-e-verifica)
10. [Configurazione Produzione](#configurazione-produzione)
11. [Troubleshooting](#troubleshooting)

---

## 🎯 Architettura Multi-Tenant

**Concetto fondamentale:** In questo setup NON installiamo Luna2 "globalmente". Invece:

1. **Prepariamo il template** (Fase 4) → Codice sorgente + WAR compilato
2. **Installiamo il pannello di gestione** (Fase 5-7) → Server Manager
3. **Creiamo istanze clienti** (tramite pannello) → Ogni cliente ha la sua copia isolata

**Ogni istanza cliente ottiene:**
- ✅ Container MySQL dedicato (credenziali uniche)
- ✅ Container Tomcat dedicato (con copia del WAR)
- ✅ Configurazione Docker isolata
- ✅ Dominio proprio con SSL
- ✅ **Zero condivisione** con altre istanze

---

## 📊 Requisiti Hardware

### Configurazione Minima (Test/Development)

| Componente | Specifica |
|------------|-----------|
| **CPU** | 2 core (2+ GHz) |
| **RAM** | 4 GB |
| **Disco** | 40 GB SSD |
| **Rete** | 10 Mbps |
| **SO** | Ubuntu 22.04+ LTS |

**Costo esempio:** ~$12/mese (DigitalOcean, Hetzner, Vultr)

### Configurazione Produzione (Consigliata)

| Componente | Specifica |
|------------|-----------|
| **CPU** | 4+ core |
| **RAM** | 8-16 GB |
| **Disco** | 100+ GB SSD |
| **Rete** | 100+ Mbps |
| **SO** | Ubuntu 22.04+ LTS |

**Costo esempio:** ~$30-50/mese

### Configurazione Multi-Tenant (5+ istanze)

| Componente | Specifica |
|------------|-----------|
| **CPU** | 8+ core |
| **RAM** | 32+ GB |
| **Disco** | 500+ GB SSD NVMe |
| **Rete** | 1 Gbps |
| **SO** | Ubuntu 22.04+ LTS |

**Costo esempio:** ~$100-200/mese

---

## 🔧 Prerequisiti Software

Il sistema Luna2 richiede:

- ✅ **Docker** 20.10+
- ✅ **Docker Compose** 2.0+
- ✅ **Node.js** 18+
- ✅ **Git** 2.0+
- ✅ **Maven** 3.6+ (per compilazione)
- ✅ **Java JDK** 17+ (per compilazione)
- ✅ **Nginx** (per reverse proxy)
- ✅ **Certbot** (per SSL Let's Encrypt)

**Tutte le dipendenze verranno installate seguendo questa guida.**

---

## 🚀 Fase 1: Preparazione Server

### 1.1 Accesso via SSH

```bash
# Da tuo computer locale
ssh root@YOUR_SERVER_IP

# Oppure con chiave SSH
ssh -i ~/.ssh/id_rsa root@YOUR_SERVER_IP
```

### 1.2 Update Sistema

```bash
# Update repository e pacchetti esistenti
apt update && apt upgrade -y

# Installa pacchetti base
apt install -y curl wget git nano vim htop net-tools
```

**Tempo stimato:** 5-10 minuti

### 1.3 Scegli Modalità Operativa

Puoi lavorare in due modi:

#### Opzione A: User Non-Root (Consigliato per Produzione)

**Vantaggi:** Maggiore sicurezza, isolamento processi  
**Svantaggi:** Devi usare `sudo` per operazioni privilegiate

```bash
# Crea user 'luna2'
adduser luna2

# Aggiungi a gruppo sudo
usermod -aG sudo luna2

# Switch a nuovo user
su - luna2
```

**Da ora in poi, tutti i comandi vanno eseguiti come user `luna2`.**  
Se serve sudo, usa: `sudo comando`

**⚠️ NOTA:** L'utente verrà aggiunto al gruppo `docker` dopo l'installazione nella Fase 2.

#### Opzione B: Resta come Root (Setup Veloce)

**Vantaggi:** Nessun bisogno di `sudo`, comandi più diretti  
**Svantaggi:** Meno sicuro, non raccomandato per produzione

```bash
# Resta logged come root
# Non creare user separato
```

**Da ora in poi, esegui tutti i comandi come `root`.**  
**Rimuovi `sudo` da tutti i comandi nelle fasi successive.**

---

**💡 Scelta consigliata:** Opzione A per server di produzione, Opzione B solo per test rapidi.

---

## 🐳 Fase 2: Installazione Docker

### 2.1 Rimuovi Vecchie Versioni (se presenti)

```bash
sudo apt remove docker docker-engine docker.io containerd runc -y
```

### 2.2 Installa Docker

```bash
# Installa dipendenze
sudo apt install -y \
    ca-certificates \
    curl \
    gnupg \
    lsb-release

# Aggiungi GPG key Docker
sudo mkdir -p /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | \
    sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg

# Aggiungi repository Docker
echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] \
  https://download.docker.com/linux/ubuntu \
  $(lsb_release -cs) stable" | \
  sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

# Installa Docker Engine
sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin

# Verifica installazione
docker --version
docker compose version
```

**Output atteso:**
```
Docker version 24.0.7, build afdd53b
Docker Compose version v2.23.0
```

### 2.3 Configura Docker (Non-Root Access)

**⚠️ Questo step è necessario SOLO se hai scelto Opzione A (user non-root) nella Fase 1.3**  
**Se stai usando root, salta direttamente al test `docker run hello-world`**

```bash
# Aggiungi user corrente al gruppo docker (creato durante installazione)
sudo usermod -aG docker $USER

# Applica cambio gruppo (re-login)
newgrp docker

# Test docker senza sudo
docker run hello-world
```

**Se vedi "Hello from Docker!" → ✅ Docker funziona**

**Se stai usando root:**
```bash
# Test docker come root (non serve configurazione gruppo)
docker run hello-world
```

### 2.4 Configura Docker per Produzione

```bash
# Crea file configurazione
sudo mkdir -p /etc/docker
sudo tee /etc/docker/daemon.json > /dev/null <<EOF
{
  "log-driver": "json-file",
  "log-opts": {
    "max-size": "10m",
    "max-file": "3"
  },
  "storage-driver": "overlay2"
}
EOF

# Riavvia Docker
sudo systemctl restart docker
sudo systemctl enable docker
```

**Tempo stimato Fase 2:** 10-15 minuti

---

## 📦 Fase 3: Installazione Node.js e Tools

### 3.1 Installa Node.js 18 LTS

```bash
# Rimuovi vecchie versioni
sudo apt remove nodejs npm -y

# Installa NodeSource repository
curl -fsSL https://deb.nodesource.com/setup_18.x | sudo -E bash -

# Installa Node.js
sudo apt install -y nodejs

# Verifica versione
node --version
npm --version
```

**Output atteso:**
```
v18.19.0
10.2.3
```

### 3.2 Installa Java JDK 17

```bash
# Installa OpenJDK 17
sudo apt install -y openjdk-17-jdk

# Verifica versione
java -version
```

**Output atteso:**
```
openjdk version "17.0.9" 2023-10-17
```

### 3.3 Installa Maven 3.9+

```bash
# Installa Maven
sudo apt install -y maven

# Verifica versione
mvn -version
```

**Output atteso:**
```
Apache Maven 3.9.x
```

### 3.4 Installa MySQL Client

```bash
# Installa client MySQL
sudo apt install -y mysql-client

# Verifica
mysql --version
```

### 3.5 Installa Nginx

```bash
# Installa Nginx
sudo apt install -y nginx

# Avvia e abilita all'avvio
sudo systemctl start nginx
sudo systemctl enable nginx

# Verifica
sudo systemctl status nginx
```

### 3.6 Installa Certbot (SSL Let's Encrypt)

```bash
# Installa Certbot
sudo apt install -y certbot python3-certbot-nginx

# Verifica
certbot --version
```

**Tempo stimato Fase 3:** 10-15 minuti

---

## 🚀 Fase 4: Preparazione Template Luna2

**💡 CONCETTO CHIAVE:** In un setup multi-tenant, NON installiamo Luna2 "globalmente". Prepariamo solo il **template** (codice + WAR compilato) che il Server Manager userà per creare le istanze.

### 4.1 Clone Repository

```bash
# Vai alla home directory
cd ~

# Clone repository
git clone https://github.com/zensoftwareit-ops/Luna2.git

# Entra nella directory
cd Luna2

# Verifica branch
git branch
```

**📝 NOTA:** 
- Se usi **user luna2**: repo sarà in `/home/luna2/Luna2`
- Se usi **root**: repo sarà in `/root/Luna2`

### 4.2 Build Template WAR

Ora compiliamo il file WAR che sarà il **template** per tutte le istanze:

```bash
# Compila progetto (prima volta: ~5-10 minuti)
mvn clean package -DskipTests

# Verifica WAR generato
ls -lh target/luna2.war
```

**Output atteso:**
```
-rw-r--r-- 1 luna2 luna2 78M Feb 27 10:30 target/luna2.war
```

✅ Se vedi il file WAR → Template pronto!

**Cosa succede quando crei un'istanza:**
```
target/luna2.war (TEMPLATE)
       ↓ copia
Container Tomcat istanza1 → /usr/local/tomcat/webapps/luna2.war
       ↓ copia
Container Tomcat istanza2 → /usr/local/tomcat/webapps/luna2.war
       ↓ copia  
Container Tomcat istanza3 → /usr/local/tomcat/webapps/luna2.war
```

**💡 Ogni istanza ha la sua COPIA del WAR** con:
- ✅ Database MySQL dedicato (credenziali uniche auto-generate)
- ✅ Container Tomcat isolato
- ✅ Configurazione Docker specifica (`docker-compose-<dominio>.yml`)
- ✅ **Nessuna condivisione di risorse tra istanze**

### 4.3 Verifica Struttura (NO global containers!)

```bash
# Verifica che NON ci siano container "globali" running
docker ps
```

**Output atteso:**
```
CONTAINER ID   IMAGE     COMMAND   CREATED   STATUS    PORTS     NAMES
(vuoto - nessun container attivo, ed è corretto!)
```

**❌ NON avviare docker-compose a livello globale!**  
I file `docker-compose-*.yml` nella root sono:
- `docker-compose-demo.yml` → Solo per testing/demo
- `docker-compose-preprod.yml` → Solo per ambiente pre-produzione singolo
- `docker-compose-main.yml` → Solo per istanza singola (non multi-tenant)

**✅ Per multi-tenant, il Server Manager creerà `docker-compose-<dominio>.yml` per ogni istanza!**

**Tempo stimato Fase 4:** 15-30 minuti (build inclusa)

---

## 🎛️ Fase 5: Installazione Server Manager

Il **Server Manager** è il pannello di gestione web per amministrare tutte le istanze Luna2.

### 5.1 Installa Dipendenze Node.js

```bash
# Vai alla directory server-manager
cd ~/Luna2/server-manager

# Installa dipendenze
npm install

# Verifica installazione
ls -lh node_modules/ | head
```

### 5.2 Configura Credenziali Admin

**⚠️ QUESTO FILE .ENV È DIVERSO:** È per il Server Manager (pannello di gestione), non per le istanze cliente!

```bash
# Crea file .env per server-manager
nano .env
```

**File `.env` per Server Manager:**
```env
PORT=8888
ADMIN_USER=admin
ADMIN_PASS=Luna2Admin!ChangeMe
WORKSPACE_DIR=/home/luna2/Luna2
LETSENCRYPT_DIR=/etc/letsencrypt/live
```

**⚠️ CAMBIA `ADMIN_PASS` con password forte!**

**📝 Cosa controlla questo file:**
- `PORT`: Porta del pannello web (default 8888)
- `ADMIN_USER`/`ADMIN_PASS`: Credenziali di login al pannello
- `WORKSPACE_DIR`: Path dove si trova Luna2
- `LETSENCRYPT_DIR`: Directory certificati SSL (per gestione automatica)

### 5.3 Test Server Manager (Dev Mode)

```bash
# Avvia in modalità development
npm start
```

**Output atteso:**
```
[2026-02-27T10:35:22.456Z] [INFO] Luna2 Server Manager v2.0 – porta 8888
[2026-02-27T10:35:22.457Z] [INFO] UI: http://localhost:8888
[2026-02-27T10:35:22.457Z] [INFO] Credenziali: admin / [ADMIN_PASS env var]
```

**Test accesso:**
1. Da browser: `http://YOUR_SERVER_IP:8888`
2. Login: `admin` / password che hai impostato
3. Se vedi la dashboard → ✅ Funziona!

**Stoppa il server:** `Ctrl+C`

### 5.4 Setup come Servizio Systemd (Produzione)

Per avere il Server Manager sempre attivo, lo configuriamo come servizio:

```bash
# Crea file di servizio
sudo nano /etc/systemd/system/luna2-manager.service
```

**Contenuto file:**
```ini
[Unit]
Description=Luna2 Server Manager
After=network.target

[Service]
Type=simple
User=luna2
WorkingDirectory=/home/luna2/Luna2/server-manager
Environment="NODE_ENV=production"
Environment="PORT=8888"
Environment="ADMIN_USER=admin"
Environment="ADMIN_PASS=Luna2Admin!ChangeMe"
Environment="WORKSPACE_DIR=/home/luna2/Luna2"
ExecStart=/usr/bin/node server.js
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

**⚠️ Se stai usando root invece di user luna2:**
- Cambia `User=luna2` → `User=root`
- Cambia `WorkingDirectory=/home/luna2/Luna2/server-manager` → `WorkingDirectory=/root/Luna2/server-manager`
- Cambia `WORKSPACE_DIR=/home/luna2/Luna2` → `WORKSPACE_DIR=/root/Luna2`

**⚠️ Aggiorna `ADMIN_PASS` con la tua password!**

```bash
# Ricarica systemd
sudo systemctl daemon-reload

# Avvia servizio
sudo systemctl start luna2-manager

# Abilita all'avvio
sudo systemctl enable luna2-manager

# Verifica status
sudo systemctl status luna2-manager
```

**Output atteso:**
```
● luna2-manager.service - Luna2 Server Manager
     Loaded: loaded (/etc/systemd/system/luna2-manager.service; enabled)
     Active: active (running) since Wed 2026-02-27 10:40:15 UTC; 5s ago
```

✅ Se vedi `active (running)` → Server Manager attivo

**Log in tempo reale:**
```bash
sudo journalctl -u luna2-manager -f
```

**Tempo stimato Fase 5:** 10-15 minuti

---

## 🔥 Fase 6: Configurazione Firewall

### 6.1 Installa UFW (se non presente)

```bash
sudo apt install -y ufw
```

### 6.2 Configura Regole Base

```bash
# Permetti SSH (IMPORTANTE! Altrimenti ti blocchi fuori)
sudo ufw allow 22/tcp

# Permetti HTTP e HTTPS
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp

# Permetti Server Manager (porta 8888)
sudo ufw allow 8888/tcp

# Abilita firewall
sudo ufw enable

# Verifica regole
sudo ufw status
```

**Output atteso:**
```
Status: active

To                         Action      From
--                         ------      ----
22/tcp                     ALLOW       Anywhere
80/tcp                     ALLOW       Anywhere
443/tcp                     ALLOW       Anywhere
8888/tcp                   ALLOW       Anywhere
```

### 6.3 Configurazione Nginx Base

```bash
# Backup configurazione default
sudo cp /etc/nginx/nginx.conf /etc/nginx/nginx.conf.backup

# Test configurazione Nginx
sudo nginx -t

# Riavvia Nginx
sudo systemctl restart nginx
```

**Tempo stimato Fase 6:** 5 minuti

---

## ✅ Fase 7: First Run e Verifica

### 7.1 Verifica Tutti i Servizi

```bash
# Docker
docker ps
sudo systemctl status docker

# Server Manager
sudo systemctl status luna2-manager

# Nginx
sudo systemctl status nginx
```

**Tutto dovrebbe essere `active (running)`**

### 7.2 Verifica Accesso Web

**Test 1: Server Manager**
```bash
# Da server
curl http://localhost:8888/api/health
```

**Output atteso:**
```json
{"status":"OK","timestamp":"2026-02-27T10:45:00.000Z","version":"2.0"}
```

**Test 2: Da Browser**

Apri browser e vai a:
```
http://YOUR_SERVER_IP:8888
```

Dovresti vedere la **pagina di login** del Server Manager.

### 7.3 Login Dashboard

1. **Username:** `admin`
2. **Password:** quella che hai impostato in `.env`
3. Click **Login**

✅ Se vedi la dashboard → **Installazione completata con successo!**

### 7.4 Verifica Directory Structure

```bash
# Verifica struttura
tree -L 2 ~/Luna2
```

**Dovresti vedere:**
```
Luna2/
├── database/
│   ├── migrations/
│   └── *.sql
├── server-manager/
│   ├── node_modules/
│   ├── public/
│   ├── server.js
│   └── package.json
├── src/
├── target/
│   └── luna2.war          ← TEMPLATE per tutte le istanze
├── docker-compose*.yml     ← File esempio (non usati in multi-tenant)
└── pom.xml
```

**NO container globali attivi:**
```bash
docker ps
```

**Output atteso (PRIMA di creare istanze):**
```
CONTAINER ID   IMAGE     COMMAND   CREATED   STATUS    PORTS     NAMES
(vuoto - ed è corretto!)
```

**Solo il Server Manager deve essere running:**
```bash
sudo systemctl status luna2-manager
```

✅ `active (running)` → Pronto per creare la prima istanza!

**Tempo stimato Fase 7:** 5-10 minuti

---

## 🏭 Configurazione Produzione

### Ottimizzazioni Consigliate

#### 1. Configurazione Nginx (Proxy per Server Manager)

```bash
# Crea configurazione Nginx per Server Manager
sudo nano /etc/nginx/sites-available/luna2-manager
```

**Contenuto:**
```nginx
server {
    listen 80;
    server_name manager.yourdomain.com;

    location / {
        proxy_pass http://localhost:8888;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_cache_bypass $http_upgrade;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

```bash
# Abilita sito
sudo ln -s /etc/nginx/sites-available/luna2-manager /etc/nginx/sites-enabled/

# Test configurazione
sudo nginx -t

# Ricarica Nginx
sudo systemctl reload nginx
```

#### 2. Setup SSL Let's Encrypt

```bash
# Richiedi certificato SSL
sudo certbot --nginx -d manager.yourdomain.com

# Rinnovo automatico già configurato via cron
```

#### 3. Backup Automatico

```bash
# Crea script backup
nano ~/backup-luna2.sh
```

**Contenuto script:**
```bash
#!/bin/bash
BACKUP_DIR="/home/luna2/backups"
DATE=$(date +%Y%m%d_%H%M%S)

mkdir -p $BACKUP_DIR

# Backup configurazione Server Manager
tar -czf $BACKUP_DIR/server-manager-config-$DATE.tar.gz \
    /home/luna2/Luna2/server-manager/.env

# Backup lista istanze e configurazioni
if [ -f /home/luna2/Luna2/instances.json ]; then
    cp /home/luna2/Luna2/instances.json $BACKUP_DIR/instances-$DATE.json
fi

# Backup docker-compose files di tutte le istanze
tar -czf $BACKUP_DIR/docker-compose-files-$DATE.tar.gz \
    /home/luna2/Luna2/docker-compose-*.yml 2>/dev/null || true

# Backup databases (tutti i container MySQL)
for container in $(docker ps --format '{{.Names}}' | grep mysql); do
    docker exec $container mysqldump -u root -p\$MYSQL_ROOT_PASSWORD --all-databases \
        > $BACKUP_DIR/db-${container}-${DATE}.sql 2>/dev/null || true
done

# Rimozione backup più vecchi di 30 giorni
find $BACKUP_DIR -type f -mtime +30 -delete

echo "Backup completato: $DATE"
```

```bash
# Rendi eseguibile
chmod +x ~/backup-luna2.sh

# Aggiungi a crontab (backup giornaliero alle 2 AM)
crontab -e
```

Aggiungi:
```
# Per user luna2:
0 2 * * * /home/luna2/backup-luna2.sh >> /home/luna2/backups/backup.log 2>&1

# Per root:
# 0 2 * * * /root/backup-luna2.sh >> /root/backups/backup.log 2>&1
```

**💡 TIP:** Lo script fa backup di:
- ✅ Configurazione Server Manager (.env)
- ✅ Lista istanze (instances.json)
- ✅ Tutti i docker-compose-*.yml (uno per ogni istanza)
- ✅ **Tutti i database MySQL** di tutte le istanze

#### 4. Monitoring con Fail2ban

```bash
# Installa Fail2ban
sudo apt install -y fail2ban

# Crea jail per SSH
sudo nano /etc/fail2ban/jail.local
```

```ini
[sshd]
enabled = true
port = 22
filter = sshd
logpath = /var/log/auth.log
maxretry = 3
bantime = 3600
```

```bash
# Riavvia Fail2ban
sudo systemctl restart fail2ban
sudo systemctl enable fail2ban
```

---

## 🔍 Troubleshooting

### Problema: "Docker daemon not running"

**Soluzione:**
```bash
sudo systemctl start docker
sudo systemctl enable docker
```

### Problema: "Permission denied" su Docker

**Soluzione:**
```bash
sudo usermod -aG docker $USER
newgrp docker
```

### Problema: "Port 8888 already in use"

**Verifica cosa usa la porta:**
```bash
sudo lsof -i :8888
```

**Kill processo:**
```bash
sudo kill -9 PID
```

### Problema: "Maven build failed"

**Verifica Java version:**
```bash
java -version  # Deve essere 17+
mvn -version
```

**Pulisci e rebuilda:**
```bash
cd ~/Luna2
mvn clean
mvn package -DskipTests
```

**⚠️ IMPORTANTE:** Il build del WAR serve solo per creare il template. Se hai già un `target/luna2.war` funzionante, non è necessario rebuilddare per creare nuove istanze! Il Server Manager userà il WAR esistente.

**Rebuild è necessario solo quando:**
- ✅ Aggiorni il codice (git pull)
- ✅ Modifichi funzionalità
- ✅ Fai deploy di nuove versioni

### Problema: "Cannot connect to Server Manager"

**Verifica servizio:**
```bash
sudo systemctl status luna2-manager
sudo journalctl -u luna2-manager --no-pager -n 50
```

**Verifica firewall:**
```bash
sudo ufw status
sudo ufw allow 8888/tcp
```

### Problema: "Out of memory"

**Controlla RAM:**
```bash
free -h
```

**Aumenta swap (temporaneo):**
```bash
sudo fallocate -l 2G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
```

### Log Utili

```bash
# Log Server Manager
sudo journalctl -u luna2-manager -f

# Log Docker
docker logs CONTAINER_NAME -f

# Log Nginx
sudo tail -f /var/log/nginx/error.log
sudo tail -f /var/log/nginx/access.log

# Log sistema
dmesg | tail -50
```

---

## 📚 Prossimi Passi

✅ **Installazione completata!**

Ora puoi:
1. **Creare la prima istanza** → Vedi [SERVER_MANAGER_USER_GUIDE.md](SERVER_MANAGER_USER_GUIDE.md)
2. **Configurare un dominio** → Usa il pannello Server Manager
3. **Deploy applicazione** → Update da GitHub + Deploy

---

## 🆘 Supporto

**Documentazione:**
- [SERVER_MANAGER_USER_GUIDE.md](SERVER_MANAGER_USER_GUIDE.md) - Guida uso pannello
- [SERVER_MANAGER_UPDATE_DEPLOY_GUIDE.md](SERVER_MANAGER_UPDATE_DEPLOY_GUIDE.md) - Update e deploy
- [DEPLOYMENT_UBUNTU_22_LTS.md](DEPLOYMENT_UBUNTU_22_LTS.md) - Deployment dettagliato

**Repository:** https://github.com/zensoftwareit-ops/Luna2

---

**Tempo totale installazione:** 60-90 minuti  
**Ultima modifica:** 27 Febbraio 2026  
**Autore:** Luna2 Team
