# 🚀 Luna2 Preprod Deployment - Ubuntu 22 LTS (Docker Multi-Tenant)

**Guida completa per deployare Luna2 su server preprod con architettura multi-tenant via Docker.**

- Ogni cliente = dominio proprio + istanza Luna2 + DB MySQL isolato + PhpMyAdmin dedicato
- Proxy Nginx centrale che instrada i domini alle istanze corrette
- Mini pannello web per gestire dominio/istanze/database
- HTTPS automatico con Let's Encrypt

**⏱️ Tempo totale: ~90 minuti (prima volta) + 3 minuti per nuovo cliente**

---

## 📋 Indice

1. [Prerequisiti](#prerequisiti)
2. [Step 1-5: Setup Server Base](#step-1-5-setup-server-base)
3. [Step 6-10: Setup Docker & Compose](#step-6-10-setup-docker--compose)
4. [Step 11-15: Deploy Gateway Nginx](#step-11-15-deploy-gateway-nginx)
5. [Step 16-20: Deploy Mini Management Panel](#step-16-20-deploy-mini-management-panel)
6. [Step 21-25: Deploy Prima Istanza Cliente](#step-21-25-deploy-prima-istanza-cliente)
7. [Step 26-30: Aggiungere Nuovi Clienti](#step-26-30-aggiungere-nuovi-clienti)
8. [Step 31-35: Monitoring & Maintenance](#step-31-35-monitoring--maintenance)

---

## 📍 Prerequisiti

**Requisiti Server:**
- Ubuntu 22.04 LTS (fresh install)
- 4GB RAM minimo (8GB consigliato per 5+ istanze)
- 100GB disco (scalabile)
- SSH access
- Porte 22, 80, 443 aperte

**Requisiti Dominio:**
- Domain registrato
- DNS pointing a server IP
- Opzionale: wildcard domain (*.tuodominio.com)

**Costi Stimati:**
- Hetzner CX31: €9.90/mese (2 vCPU, 8GB, 160GB SSD) ✅ CONSIGLIATO
- DigitalOcean Standard: $24/mese (2 vCPU, 4GB, 80GB)

---

## ⏱️ STEP 1-5: SETUP SERVER BASE

### STEP 1: Accedi al Server

```bash
# SSH al server
ssh root@your-server-ip

# Verifica Ubuntu version
lsb_release -a
# Output atteso: Ubuntu 22.04 LTS
```

---

### STEP 2: Update Sistema

```bash
# Update packages
sudo apt update && sudo apt upgrade -y

# Install essentials
sudo apt install -y \
  curl wget git jq htop vim nano \
  ca-certificates apt-transport-https \
  openssl certbot python3-certbot-nginx \
  build-essential
```

---

### STEP 3: Configura Firewall

```bash
# Abilita firewall
sudo ufw enable

# Apri porte critiche
sudo ufw allow 22/tcp    # SSH
sudo ufw allow 80/tcp    # HTTP
sudo ufw allow 443/tcp   # HTTPS
sudo ufw allow 3306/tcp  # MySQL (opzionale, se accesso remoto)
sudo ufw allow 9000/tcp  # Portainer (opzionale)

# Verifica
sudo ufw status verbose
```

---

### STEP 4: Crea Directory di Lavoro

```bash
# Crea struttura directory
mkdir -p /opt/luna2/{gateway,management,instances,scripts,backups}
mkdir -p /opt/luna2/gateway/nginx/conf.d
mkdir -p /opt/luna2/gateway/nginx/ssl

cd /opt/luna2

# Dai permessi
chmod -R 755 /opt/luna2
```

---

### STEP 5: Installa Docker & Docker Compose

```bash
# Aggiungi Docker repo
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | \
  sudo gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg

echo "deb [arch=amd64 signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] \
  https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | \
  sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

# Installa Docker
sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin

# Aggiungi user al gruppo docker
sudo usermod -aG docker $USER
newgrp docker

# Verifica
docker --version
docker compose version
```

---

## ⏱️ STEP 6-10: SETUP DOCKER & COMPOSE

### STEP 6: Crea Docker Network

```bash
# Crea network multi-tenant
docker network create luna2-network

# Verifica
docker network ls | grep luna2
```

---

### STEP 7: Prepara WAR di Luna2

```bash
# Clone repository (se non fatto già)
cd ~/
git clone https://github.com/zensoftwareit-ops/Luna2.git

# Build WAR
cd Luna2
mvn clean package -DskipTests -q

# Copia WAR in location accessibile
cp target/luna2.war /opt/luna2/gateway/

echo "✓ WAR copato: $(ls -lh /opt/luna2/gateway/luna2.war)"
```

---

### STEP 8: Crea Gateway Nginx (docker-compose.yml)

**File:** `/opt/luna2/gateway/docker-compose.yml`

```yaml
version: '3.8'

services:
  # NGINX Gateway - Router centrale per tutti i domini
  nginx-gateway:
    image: nginx:latest
    container_name: luna2-nginx-gateway
    restart: unless-stopped
    ports:
      - "80:80"
      - "443:443"
    volumes:
      # Nginx configs
      - ./nginx/nginx-main.conf:/etc/nginx/nginx.conf:ro
      - ./nginx/conf.d:/etc/nginx/conf.d:ro
      
      # SSL certificates
      - ./nginx/ssl:/etc/nginx/ssl:ro
      - /etc/letsencrypt:/etc/letsencrypt:ro
      
      # Logs
      - ./logs/nginx:/var/log/nginx
    networks:
      - luna2-network
    environment:
      TZ: Europe/Rome
    healthcheck:
      test: ["CMD", "wget", "--quiet", "--tries=1", "--spider", "http://localhost/health"]
      interval: 30s
      timeout: 10s
      retries: 3

  # Portainer - Docker UI (opzionale)
  portainer:
    image: portainer/portainer-ce:latest
    container_name: luna2-portainer
    restart: unless-stopped
    ports:
      - "9000:9000"
      - "8000:8000"
    volumes:
      - /var/run/docker.sock:/var/run/docker.sock
      - ./portainer-data:/data
    environment:
      TZ: Europe/Rome

networks:
  luna2-network:
    name: luna2-network
    external: true

volumes:
  portainer-data:
```

---

### STEP 9: Crea Nginx Main Config

**File:** `/opt/luna2/gateway/nginx/nginx-main.conf`

```nginx
user nginx;
worker_processes auto;
error_log /var/log/nginx/error.log warn;
pid /var/run/nginx.pid;

events {
    worker_connections 1024;
}

http {
    include /etc/nginx/mime.types;
    default_type application/octet-stream;

    log_format main '$remote_addr - $remote_user [$time_local] "$request" '
                    '$status $body_bytes_sent "$http_referer" '
                    '"$http_user_agent" "$http_x_forwarded_for"';

    access_log /var/log/nginx/access.log main;

    sendfile on;
    tcp_nopush on;
    tcp_nodelay on;
    keepalive_timeout 65;
    types_hash_max_size 2048;
    client_max_body_size 100M;

    gzip on;
    gzip_vary on;
    gzip_proxied any;
    gzip_comp_level 6;
    gzip_types text/plain text/css text/xml text/javascript 
               application/json application/javascript application/xml+rss;

    # Health check endpoint
    server {
        listen 80;
        server_name _;
        location /health {
            return 200 "OK\n";
            add_header Content-Type text/plain;
        }
    }

    # Include single-tenant instance configs
    include /etc/nginx/conf.d/*.conf;
}
```

---

### STEP 10: Script Helper per Gestire Istanze

**File:** `/opt/luna2/scripts/manage-instance.sh`

```bash
#!/bin/bash

# Script per creare/modificare/eliminare istanze Luna2

set -e

INSTANCES_DIR="/opt/luna2/instances"
GATEWAY_DIR="/opt/luna2/gateway"

# Colori output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Funzione help
show_help() {
    cat << EOF
Uso: $0 [COMANDO] [OPZIONI]

Comandi:
  create CLIENT_NAME DOMAIN DB_PASS    Crea nuova istanza
  list                                 Elenca istanze attive
  start INSTANCE_NAME                  Avvia istanza
  stop INSTANCE_NAME                   Stoppa istanza
  status                               Mostra status di tutte istanze
  delete INSTANCE_NAME                 Elimina istanza (PERICOLO!)
  logs INSTANCE_NAME [LINES]           Visualizza logs
  backup INSTANCE_NAME                 Backup database istanza

Esempi:
  $0 create acmecorp acmecorp.com s3cur3P@ssw0rd!
  $0 list
  $0 status
  $0 logs acmecorp-luna2 50
EOF
    exit 0
}

# Crea nuova istanza
create_instance() {
    local CLIENT_NAME="$1"
    local DOMAIN="$2"
    local DB_PASS="$3"
    local INSTANCE_NAME="${CLIENT_NAME}-luna2"
    local INSTANCE_DIR="$INSTANCES_DIR/$INSTANCE_NAME"
    local PORT=$((9100 + $(ls -1 $INSTANCES_DIR | wc -l)))

    if [ -z "$CLIENT_NAME" ] || [ -z "$DOMAIN" ] || [ -z "$DB_PASS" ]; then
        echo -e "${RED}[ERR] Uso: $0 create CLIENT_NAME DOMAIN DB_PASS${NC}"
        exit 1
    fi

    echo -e "${GREEN}[*] Creazione istanza per $CLIENT_NAME ($DOMAIN)...${NC}"

    # Crea directory
    mkdir -p "$INSTANCE_DIR/data/mysql"
    mkdir -p "$INSTANCE_DIR/data/luna2-files"

    # Genera docker-compose per istanza
    cat > "$INSTANCE_DIR/docker-compose.yml" << DOCKERCOMPOSE
version: '3.8'

services:
  mysql-${INSTANCE_NAME}:
    image: mysql:8.0
    container_name: mysql-${INSTANCE_NAME}
    restart: unless-stopped
    environment:
      MYSQL_ROOT_PASSWORD: ${DB_PASS}
      MYSQL_DATABASE: luna2
      MYSQL_USER: luna2_user
      MYSQL_PASSWORD: ${DB_PASS}
      TZ: Europe/Rome
    ports:
      - "33060$(($(echo $INSTANCE_NAME | sum | cut -d' ' -f1) % 10)):3306"
    volumes:
      - ./data/mysql:/var/lib/mysql
    networks:
      - luna2-network
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      timeout: 5s
      retries: 5

  luna2-${INSTANCE_NAME}:
    image: tomcat:9.0
    container_name: luna2-${INSTANCE_NAME}
    restart: unless-stopped
    depends_on:
      - mysql-${INSTANCE_NAME}
    environment:
      DB_HOST: mysql-${INSTANCE_NAME}
      DB_NAME: luna2
      DB_USER: luna2_user
      DB_PASSWORD: ${DB_PASS}
      TZ: Europe/Rome
    volumes:
      - $GATEWAY_DIR/luna2.war:/usr/local/tomcat/webapps/ROOT.war
      - ./data/luna2-files:/opt/luna2/files
    networks:
      - luna2-network
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/"]
      interval: 30s
      timeout: 10s
      retries: 3

  phpmyadmin-${INSTANCE_NAME}:
    image: phpmyadmin:latest
    container_name: phpmyadmin-${INSTANCE_NAME}
    restart: unless-stopped
    depends_on:
      - mysql-${INSTANCE_NAME}
    environment:
      PMA_HOST: mysql-${INSTANCE_NAME}
      PMA_USER: luna2_user
      PMA_PASSWORD: ${DB_PASS}
      PMA_ABSOLUTE_URI: https://${DOMAIN}/phpmyadmin/
      TZ: Europe/Rome
    networks:
      - luna2-network

networks:
  luna2-network:
    name: luna2-network
    external: true
DOCKERCOMPOSE

    # Genera Nginx config per domain
    cat > "$GATEWAY_DIR/nginx/conf.d/${INSTANCE_NAME}.conf" << NGINXCONF
upstream backend_${INSTANCE_NAME} {
    server luna2-${INSTANCE_NAME}:8080;
}

upstream phpmyadmin_${INSTANCE_NAME} {
    server phpmyadmin-${INSTANCE_NAME}:80;
}

server {
    listen 80;
    server_name ${DOMAIN};
    
    location / {
        return 301 https://${DOMAIN}\$request_uri;
    }
    
    location /.well-known/acme-challenge/ {
        root /var/www/certbot;
    }
}

server {
    listen 443 ssl http2;
    server_name ${DOMAIN};

    ssl_certificate /etc/letsencrypt/live/${DOMAIN}/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/${DOMAIN}/privkey.pem;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;

    client_max_body_size 100M;

    # Luna2 App
    location / {
        proxy_pass http://backend_${INSTANCE_NAME};
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }

    # PhpMyAdmin
    location /phpmyadmin/ {
        proxy_pass http://phpmyadmin_${INSTANCE_NAME}/;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
    }
}
NGINXCONF

    # Avvia istanza
    cd "$INSTANCE_DIR"
    docker compose up -d

    echo -e "${GREEN}[✓] Istanza creata!${NC}"
    echo -e "${YELLOW}Accesso:${NC}"
    echo "  App: https://${DOMAIN}"
    echo "  PhpMyAdmin: https://${DOMAIN}/phpmyadmin/"
    echo "  DB User: luna2_user"
    echo "  DB Password: ${DB_PASS}"
}

# List istanze
list_instances() {
    echo -e "${YELLOW}Istanze attive:${NC}"
    docker ps --filter "name=luna2-" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
}

# Status
show_status() {
    echo -e "${YELLOW}Docker containers:${NC}"
    docker ps -a --filter "name=luna2-" --format "table {{.Names}}\t{{.Status}}"
}

# Logs
show_logs() {
    local INSTANCE="$1"
    local LINES="${2:-50}"
    docker logs --tail $LINES -f luna2-$INSTANCE
}

# Main
case "${1:-help}" in
    create)  create_instance "$2" "$3" "$4" ;;
    list)    list_instances ;;
    status)  show_status ;;
    logs)    show_logs "$2" "$3" ;;
    help|*)  show_help ;;
esac
```

Rendi eseguibile:

```bash
chmod +x /opt/luna2/scripts/manage-instance.sh
```

---

## ⏱️ STEP 11-15: DEPLOY GATEWAY NGINX

### STEP 11: Avvia Gateway

```bash
cd /opt/luna2/gateway

# Crea directory logs
mkdir -p logs

# Avvia containers
docker compose up -d

# Verifica
docker ps | grep luna2
```

---

### STEP 12: Configura SSL (Let's Encrypt)

```bash
# Per un dominio di test (example.com)
sudo certbot certonly --standalone \
  -d example.com \
  -d *.example.com \
  --agree-tos \
  --non-interactive \
  -m admin@example.com

# Copia certificati in Nginx
sudo cp /etc/letsencrypt/live/example.com/* /opt/luna2/gateway/nginx/ssl/

# Dai permessi
sudo chmod 644 /opt/luna2/gateway/nginx/ssl/*
```

---

### STEP 13: Ricarica Nginx

```bash
docker exec luna2-nginx-gateway nginx -s reload

# Verifica
docker logs luna2-nginx-gateway | tail -10
```

---

### STEP 14: Health Check Gateway

```bash
# Verifica porta 80
curl -I http://localhost/

# Output atteso: HTTP/1.1 404 Not Found (normale, nessun domain configurato)
```

---

### STEP 15: Backup Config Gateway

```bash
tar -czf /opt/luna2/backups/gateway-config-$(date +%Y%m%d).tar.gz \
  /opt/luna2/gateway/nginx/

echo "✓ Gateway config backed up"
```

---

## ⏱️ STEP 16-20: DEPLOY MINI MANAGEMENT PANEL

**Mini web panel per aggiungere clienti facilmente senza CLI**

### STEP 16: Crea Management Panel (Node.js)

**File:** `/opt/luna2/management/docker-compose.yml`

```yaml
version: '3.8'

services:
  management-panel:
    image: node:18-slim
    container_name: luna2-management-panel
    restart: unless-stopped
    working_dir: /app
    volumes:
      - ./app:/app
      - /opt/luna2/instances:/opt/luna2/instances
      - /opt/luna2/gateway/nginx/conf.d:/opt/luna2/nginx/conf.d
      - /var/run/docker.sock:/var/run/docker.sock
    ports:
      - "5000:5000"
    environment:
      NODE_ENV: production
      PORT: 5000
    networks:
      - luna2-network

networks:
  luna2-network:
    name: luna2-network
    external: true
```

---

### STEP 17: Crea Node App per Management

**File:** `/opt/luna2/management/app/server.js`

```javascript
const express = require('express');
const path = require('path');
const fs = require('fs');
const { execSync } = require('child_process');
const docker = require('dockerode');

const app = express();
const PORT = process.env.PORT || 5000;
const d = new docker({ socketPath: '/var/run/docker.sock' });

app.use(express.json());
app.use(express.static('public'));
app.set('view engine', 'ejs');

// Dashboard
app.get('/', async (req, res) => {
    try {
        const containers = await d.listContainers({ all: true });
        const luna2Containers = containers.filter(c =>
            c.Names[0].includes('luna2') || c.Names[0].includes('mysql')
        );

        res.render('dashboard', { containers: luna2Containers });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// API: Crea istanza
app.post('/api/instances/create', (req, res) => {
    const { clientName, domain, dbPassword } = req.body;

    if (!clientName || !domain || !dbPassword) {
        return res.status(400).json({ error: 'Missing required fields' });
    }

    try {
        const script = '/opt/luna2/scripts/manage-instance.sh';
        execSync(`${script} create ${clientName} ${domain} ${dbPassword}`);

        res.json({ 
            success: true,
            message: `Instance created: ${domain}`,
            accessPoints: {
                app: `https://${domain}`,
                phpmyadmin: `https://${domain}/phpmyadmin/`,
                dbUser: 'luna2_user',
                dbPassword: dbPassword
            }
        });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// API: List istanze
app.get('/api/instances', async (req, res) => {
    try {
        const containers = await d.listContainers({ all: true });
        const instances = containers
            .filter(c => c.Names[0].includes('luna2-'))
            .map(c => ({
                name: c.Names[0],
                status: c.State,
                image: c.Image,
                ports: c.Ports
            }));

        res.json(instances);
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// API: Logs istanza
app.get('/api/instances/:name/logs', async (req, res) => {
    try {
        const container = d.getContainer(req.params.name);
        const logs = await container.logs({ stdout: true, stderr: true });
        res.set('Content-Type', 'text/plain');
        res.send(logs.toString());
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// API: Health check
app.get('/health', (req, res) => {
    res.json({ status: 'OK' });
});

app.listen(PORT, () => {
    console.log(`✓ Management Panel online: http://localhost:${PORT}`);
});
```

---

### STEP 18: Crea Package.json

**File:** `/opt/luna2/management/app/package.json`

```json
{
  "name": "luna2-management-panel",
  "version": "1.0.0",
  "description": "Mini management panel per Luna2 multi-tenant",
  "main": "server.js",
  "scripts": {
    "start": "node server.js",
    "dev": "nodemon server.js"
  },
  "dependencies": {
    "express": "^4.18.2",
    "ejs": "^3.1.8",
    "dockerode": "^4.0.2"
  }
}
```

---

### STEP 19: Crea Dashboard EJS

**File:** `/opt/luna2/management/app/views/dashboard.ejs`

```html
<!DOCTYPE html>
<html>
<head>
    <title>Luna2 Management Panel</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <style>
        body { background: #f8f9fa; }
        .navbar { background: linear-gradient(90deg, #667eea 0%, #764ba2 100%); }
        .card { border-left: 4px solid #667eea; }
    </style>
</head>
<body>
    <nav class="navbar navbar-dark">
        <span class="navbar-brand mb-0 h1"><i class="bi bi-gear"></i> Luna2 Management</span>
    </nav>

    <div class="container mt-5">
        <!-- Create Instance Form -->
        <div class="card mb-4">
            <div class="card-header bg-primary text-white">
                <h5 class="mb-0">➕ Crea Nuova Istanza Client</h5>
            </div>
            <div class="card-body">
                <form id="createForm">
                    <div class="row">
                        <div class="col-md-4">
                            <label class="form-label">Nome Client</label>
                            <input type="text" class="form-control" id="clientName" 
                                   placeholder="es. acmecorp" required>
                        </div>
                        <div class="col-md-4">
                            <label class="form-label">Dominio</label>
                            <input type="text" class="form-control" id="domain"
                                   placeholder="es. acmecorp.com" required>
                        </div>
                        <div class="col-md-4">
                            <label class="form-label">DB Password</label>
                            <input type="password" class="form-control" id="dbPassword"
                                   placeholder="Strong password!" required>
                        </div>
                    </div>
                    <button type="submit" class="btn btn-success mt-3">Crea Istanza</button>
                </form>
            </div>
        </div>

        <!-- Instances List -->
        <div class="card">
            <div class="card-header bg-info text-white">
                <h5 class="mb-0">📊 Istanze Attive</h5>
            </div>
            <div class="card-body">
                <table class="table table-striped" id="instancesTable">
                    <thead>
                        <tr>
                            <th>Nome</th>
                            <th>Status</th>
                            <th>Porte</th>
                            <th>Azioni</th>
                        </tr>
                    </thead>
                    <tbody></tbody>
                </table>
            </div>
        </div>
    </div>

    <script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
    <script>
        // Load instances
        function loadInstances() {
            $.get('/api/instances', function(instances) {
                const tbody = $('#instancesTable tbody');
                tbody.empty();
                instances.forEach(inst => {
                    tbody.append(`
                        <tr>
                            <td><strong>${inst.name}</strong></td>
                            <td><span class="badge ${inst.status === 'running' ? 'bg-success' : 'bg-danger'}">${inst.status}</span></td>
                            <td>${JSON.stringify(inst.ports)}</td>
                            <td>
                                <button class="btn btn-sm btn-info" onclick="viewLogs('${inst.name}')">Logs</button>
                            </td>
                        </tr>
                    `);
                });
            });
        }

        // Create instance
        $('#createForm').submit(function(e) {
            e.preventDefault();
            $.ajax({
                type: 'POST',
                url: '/api/instances/create',
                contentType: 'application/json',
                data: JSON.stringify({
                    clientName: $('#clientName').val(),
                    domain: $('#domain').val(),
                    dbPassword: $('#dbPassword').val()
                }),
                success: function(data) {
                    alert('✓ ' + data.message + '\n\nAccesso:\n' +
                          'App: ' + data.accessPoints.app + '\n' +
                          'PhpMyAdmin: ' + data.accessPoints.phpmyadmin);
                    $('#createForm')[0].reset();
                    loadInstances();
                },
                error: function(err) {
                    alert('✗ Errore: ' + err.responseJSON.error);
                }
            });
        });

        // View logs
        function viewLogs(name) {
            window.open(`/api/instances/${name}/logs`, '_blank');
        }

        // Load at startup
        loadInstances();
        setInterval(loadInstances, 30000); // Refresh ogni 30s
    </script>
</body>
</html>
```

---

### STEP 20: Avvia Management Panel

```bash
cd /opt/luna2/management/app

# Installa dipendenze
npm install

# Avvia
npm start

# Output atteso:
# ✓ Management Panel online: http://localhost:5000
```

Accedi da: `http://server-ip:5000`

---

## ⏱️ STEP 21-25: DEPLOY PRIMA ISTANZA CLIENTE

### STEP 21: Crea Prima Istanza (via CLI superveloce)

```bash
/opt/luna2/scripts/manage-instance.sh create \
  "acmecorp" \
  "demo.acmecorp.com" \
  "MyS3curePa55w0rd!"

# Output:
# [*] Creazione istanza per acmecorp...
# [✓] Istanza creata!
# Accesso:
#   App: https://demo.acmecorp.com
#   PhpMyAdmin: https://demo.acmecorp.com/phpmyadmin/
#   DB User: luna2_user
#   DB Password: MyS3curePa55w0rd!
```

---

### STEP 22: Setup SSL per Primo Dominio

```bash
# Ottieni certificato Let's Encrypt
sudo certbot certonly --standalone \
  -d demo.acmecorp.com \
  --agree-tos \
  --non-interactive \
  -m admin@acmecorp.com

# Copia certificati
sudo cp /etc/letsencrypt/live/demo.acmecorp.com/* \
  /opt/luna2/gateway/nginx/ssl/

# Ricarica Nginx
docker exec luna2-nginx-gateway nginx -s reload
```

---

### STEP 23: Test Accesso App

```bash
# Aspetta 30 secondi per startup Tomcat
sleep 30

# Test da CLI
curl -ILk https://demo.acmecorp.com

# Output atteso:
# HTTP/2 302 (redirect a login)
```

---

### STEP 24: Accedi all'App da Browser

```
URL: https://demo.acmecorp.com/
Nome Utente: admin
Password: admin123 (default - CAMBIA!)
```

---

### STEP 25: Verifica PhpMyAdmin

```
URL: https://demo.acmecorp.com/phpmyadmin/
Utente: luna2_user
Password: MyS3curePa55w0rd!
```

Dovresti vedere il database `luna2` con tutte le tabelle.

---

## ⏱️ STEP 26-30: AGGIUNGERE NUOVI CLIENTI

### STEP 26: Aggiungi Client Via Web Panel

**URL:** `http://server-ip:5000`

1. Compila form:
   - Nome Client: `newclient`
   - Dominio: `app.newclient.com`
   - DB Password: `Secur3P@ss2024!`

2. Clicca "Crea Istanza"

---

### STEP 27: SSL per Nuovo Dominio

```bash
sudo certbot certonly --standalone \
  -d app.newclient.com \
  --agree-tos \
  --non-interactive \
  -m admin@newclient.com

# Copia in Nginx
sudo cp /etc/letsencrypt/live/app.newclient.com/* \
  /opt/luna2/gateway/nginx/ssl/

# Reload
docker exec luna2-nginx-gateway nginx -s reload
```

---

### STEP 28: Verifica Nuovo Client

```bash
# Health check
curl -ILk https://app.newclient.com

# Lista containers attivi
docker ps | grep newclient-luna2
```

---

### STEP 29: Istruzioni per Cliente

Invia al cliente:

```
🎉 La tua istanza Luna2 è ONLINE!

📱 ACCESSO APP:
   URL: https://app.newclient.com
   Utente: admin
   Password: admin123

📊 DATABASE MANAGER (PhpMyAdmin):
   URL: https://app.newclient.com/phpmyadmin/
   Utente DB: luna2_user
   Password DB: Secur3P@ss2024!

⚠️ IMPORTANTE:
   1. Cambia password admin SUBITO da app
   2. Configura moduli secondo esigenze
   3. Backup settimanali raccomandati

📞 Support: support@tuoazienda.com
```

---

### STEP 30: Automazione Rinnovi SSL (Cron)

```bash
# Crea script renewal
cat > /opt/luna2/scripts/renew-ssl.sh << 'RENEWSCRIPT'
#!/bin/bash

# Renewal Let's Encrypt certificates automaticamente
sudo certbot renew --quiet --no-eff-email

# Reload Nginx
docker exec luna2-nginx-gateway nginx -s reload

# Log
echo "[$(date)] SSL renewal completed" >> /opt/luna2/logs/ssl-renewal.log
RENEWSCRIPT

chmod +x /opt/luna2/scripts/renew-ssl.sh

# Aggiungi cron job (renewal ogni giorno alle 3 AM)
sudo tee -a /etc/crontab << CRON
0 3 * * * /opt/luna2/scripts/renew-ssl.sh
CRON
```

---

## ⏱️ STEP 31-35: MONITORING & MAINTENANCE

### STEP 31: Crea Sistema Monitoring

**File:** `/opt/luna2/scripts/monitor.sh`

```bash
#!/bin/bash

# Monitora salute istanze e invia alerts

INSTANCES_DIR="/opt/luna2/instances"
CONTAINERS_DOWN=0

echo -e "\n=== Luna2 Health Check - $(date) ==="

# Check ogni container
for container in $(docker ps -a --filter "name=luna2-" --format "{{.Names}}"); do
    STATUS=$(docker inspect --format='{{.State.Running}}' $container)
    
    if [ "$STATUS" == "false" ]; then
        echo "❌ $container - DOWN"
        CONTAINERS_DOWN=$((CONTAINERS_DOWN + 1))
    else
        UPTIME=$(docker inspect --format='{{.State.StartedAt}}' $container)
        echo "✓ $container - UP (since $UPTIME)"
    fi
done

echo -e "\n📊 Summary:"
echo "Total Instances: $(ls -1d $INSTANCES_DIR/*/ 2>/dev/null | wc -l)"
echo "Containers Down: $CONTAINERS_DOWN"

# Alert se qualcosa è down
if [ $CONTAINERS_DOWN -gt 0 ]; then
    echo "⚠️  ALERT: $CONTAINERS_DOWN container(s) down!"
    # Puoi aggiungere qui invio email/Telegram notification
fi
```

---

### STEP 32: Backup Automatico Database

```bash
# Crea backup script
cat > /opt/luna2/scripts/backup-all.sh << 'BACKUPSCRIPT'
#!/bin/bash

BACKUP_DIR="/opt/luna2/backups"
RETENTION_DAYS=30

mkdir -p $BACKUP_DIR

echo "Starting backup of all databases..."

# Backup ogni istanza
for instance in $(ls -1d /opt/luna2/instances/*/); do
    INSTANCE_NAME=$(basename $instance)
    CONTAINER="mysql-${INSTANCE_NAME}"
    
    docker exec $CONTAINER mysqldump -u luna2_user -pSecur3P@ss2024! luna2 | \
        gzip > "$BACKUP_DIR/${INSTANCE_NAME}-$(date +%Y%m%d_%H%M%S).sql.gz"
    
    echo "✓ Backup: $INSTANCE_NAME"
done

# Elimina backup vecchi (>30 giorni)
find $BACKUP_DIR -name "*.sql.gz" -mtime +$RETENTION_DAYS -delete

echo "✓ Backup completed"
BACKUPSCRIPT

chmod +x /opt/luna2/scripts/backup-all.sh

# Aggiungi cron job (backup ogni giorno alle 2 AM)
sudo tee -a /etc/crontab << CRON
0 2 * * * /opt/luna2/scripts/backup-all.sh
CRON
```

---

### STEP 33: Logs Centrali

```bash
# Crea aggregatore logs
docker logs luna2-nginx-gateway > /opt/luna2/logs/nginx.log
docker logs luna2-portainer > /opt/luna2/logs/portainer.log

# Per monitare real-time:
docker logs -f luna2-nginx-gateway
```

---

### STEP 34: Portainer Dashboard

**Accedi a:** `http://server-ip:9000`

1. Setup admin account
2. Monitora tutti containers
3. Gestisci immagini, networks, volumes

---

### STEP 35: Escalabilità Futura

Se serve scalare a 100+ clienti:

```bash
# 1. Aggiungi Kubernetes (k3s)
curl -sfL https://get.k3s.io | sh -

# 2. Usa Helm charts per deploy automatico

# 3. Setup load balancer (Nginx upstream multiple servers)

# 4. Database cluster (MySQL replication/Galera)

# 5. Centralized logging (ELK stack)
```

---

## 📋 Checklista Finale

- ✅ Server Ubuntu 22.04 preparato
- ✅ Docker & Docker Compose installati
- ✅ Nginx gateway online
- ✅ Management panel accessible
- ✅ Prima istanza cliente running
- ✅ SSL certificates configurati
- ✅ PhpMyAdmin accessibile
- ✅ Backup cron attivo
- ✅ SSL renewal cron attivo
- ✅ Portainer dashboard online

---

## 🆘 Troubleshooting

**App non raggiungibile?**
```bash
# Check Nginx logs
docker logs luna2-nginx-gateway

# Reload Nginx config
docker exec luna2-nginx-gateway nginx -s reload

# Test DNS
nslookup app.newclient.com
```

**Database connection error?**
```bash
# Check MySQL container
docker logs mysql-instancename-luna2

# Test connessione
docker exec myql-instancename-luna2 mysql -u luna2_user -p luna2
```

**SSL certificate issues?**
```bash
# Valida certificato
openssl x509 -in /etc/letsencrypt/live/domain.com/fullchain.pem -text

# Rinnova manualmente
sudo certbot renew --force-renewal -d domain.com
```

---

## 📞 Support & Documentation

- **Nginx docs:** https://nginx.org/en/docs/
- **Docker docs:** https://docs.docker.com/
- **Let's Encrypt:** https://letsencrypt.org/docs/
- **Luna2 docs:** https://github.com/zensoftwareit-ops/Luna2

---

**Fine guida deployment! Buon lavoro! 🚀**
