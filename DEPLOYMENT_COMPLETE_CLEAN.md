# Luna2 Multi-Tenant Deployment Guide

## PARTE 1: SETUP INIZIALE DEL SERVER

### Fase 1.1: Prerequisiti Server
**Sistema:** Ubuntu 22.04 LTS  
**CPU:** 4+ cores  
**RAM:** 8GB+  
**Disco:** 100GB SSD

```bash
# Update sistema
sudo apt update && sudo apt upgrade -y

# Installa dipendenze
sudo apt install -y \
    curl wget git htop net-tools \
    ca-certificates gnupg lsb-release

# Apri firewall per servizi
sudo ufw allow 22/tcp
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw allow 9000/tcp
sudo ufw enable
```

---

### Fase 1.2: Installa Docker
```bash
# Aggiungi repo Docker ufficiale
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg

echo "deb [arch=amd64 signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | \
  sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

sudo apt update && sudo apt install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin

# Verifica installazione
docker --version
docker compose version

# Aggiungi user attuale a gruppo docker
sudo usermod -aG docker $USER
newgrp docker
```

---

### Fase 1.3: Crea struttura directory
```bash
sudo mkdir -p /opt/luna2/{gateway,management,instances,scripts,logs}
sudo chown -R $USER:$USER /opt/luna2
```

---

### Fase 1.4: Crea network Docker
```bash
docker network create luna2-network
```

---

### Fase 1.5: Setup DNS e Certificati SSL
```bash
# Sostituisci con il TUO dominio base
YOUR_DOMAIN="demo.gestionaleluna.it"
YOUR_EMAIL="admin@gestionaleluna.it"

# Punta il dominio al server (tramite A record)
# Esempio: A record: demo.gestionaleluna.it -> IP_SERVER

# Genera certificato Let's Encrypt per dominio base
sudo certbot certonly --standalone \
  -d $YOUR_DOMAIN \
  --agree-tos -n -m $YOUR_EMAIL
```

---

### Fase 1.6: Deploy Nginx Gateway

**File:** `/opt/luna2/gateway/docker-compose.yml`

```yaml
version: '3.8'

services:
  nginx-gateway:
    image: nginx:latest
    container_name: luna2-nginx-gateway
    restart: unless-stopped
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx/nginx-main.conf:/etc/nginx/nginx.conf:ro
      - ./nginx/conf.d:/etc/nginx/conf.d:ro
      - /etc/letsencrypt:/etc/letsencrypt:ro
      - ./logs:/var/log/nginx
    networks:
      - luna2-network
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost/health"]
      interval: 30s
      timeout: 10s
      retries: 3

  portainer:
    image: portainer/portainer-ce:latest
    container_name: luna2-portainer
    restart: unless-stopped
    volumes:
      - /var/run/docker.sock:/var/run/docker.sock
      - portainer_data:/data
    ports:
      - "9000:9000"
      - "8000:8000"
    networks:
      - luna2-network

volumes:
  portainer_data:

networks:
  luna2-network:
    name: luna2-network
    external: true
```

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
    server_tokens off;

    gzip on;
    gzip_types text/plain text/css text/xml text/javascript application/json;

    # Health check endpoint
    server {
        listen 80;
        server_name _;
        location /health {
            access_log off;
            return 200 "OK\n";
            add_header Content-Type text/plain;
        }
    }

    # Include config per singole istanze client
    include /etc/nginx/conf.d/*.conf;
}
```

**Avvia il gateway:**

```bash
cd /opt/luna2/gateway
mkdir -p nginx/conf.d logs
docker compose up -d

# Verifica
sleep 3
curl http://localhost/health
docker ps | grep nginx-gateway
```

---

### Fase 1.7: Deploy Management Panel (Node.js)

**File:** `/opt/luna2/management/app/package.json`

```json
{
  "name": "luna2-management-panel",
  "version": "1.0.0",
  "main": "server.js",
  "scripts": {
    "start": "node server.js"
  },
  "dependencies": {
    "express": "^4.18.2",
    "ejs": "^3.1.8"
  }
}
```

**File:** `/opt/luna2/management/app/server.js`

```javascript
const express = require('express');
const { execSync } = require('child_process');

const app = express();
const PORT = process.env.PORT || 5000;

app.use(express.json());
app.use(express.urlencoded({ extended: true }));
app.set('view engine', 'ejs');

// Dashboard
app.get('/', (req, res) => {
    try {
        const output = execSync('docker ps -a --filter "name=luna2-" --format "{{.Names}}\t{{.Status}}"', { encoding: 'utf8' });
        const containers = output.trim().split('\n').filter(l => l).map(line => {
            const [name, status] = line.split('\t');
            return { name, status };
        });
        res.render('dashboard', { containers });
    } catch (err) {
        res.render('dashboard', { containers: [] });
    }
});

// API: Crea istanza
app.post('/api/instances/create', (req, res) => {
    const { clientName, domain, dbPassword } = req.body;

    if (!clientName || !domain || !dbPassword) {
        return res.status(400).json({ error: 'Parametri mancanti' });
    }

    try {
        console.log(`[*] Generazione certificato SSL per ${domain}...`);
        
        // Genera certificato
        try {
            execSync(`sudo certbot certonly --standalone -d ${domain} --agree-tos -n -m admin@example.com 2>&1`, 
                { encoding: 'utf8' });
            console.log(`[✓] Certificato generato`);
        } catch (err) {
            if (!err.message.includes('Cert not yet due for renewal')) {
                console.warn(`[!] Avviso: ${err.message.substring(0, 100)}`);
            }
        }

        // Crea istanza
        console.log(`[*] Creazione istanza...`);
        execSync(`/opt/luna2/scripts/manage-instance.sh create ${clientName} ${domain} ${dbPassword}`, 
            { encoding: 'utf8' });

        res.json({ 
            success: true,
            message: `Istanza creata: ${domain}`,
            accessPoints: {
                app: `https://${domain}`,
                phpmyadmin: `https://${domain}/phpmyadmin/`,
                dbUser: 'luna2_user',
                dbPassword: dbPassword
            }
        });
    } catch (err) {
        console.error(`[✗] Errore: ${err.message}`);
        res.status(500).json({ error: err.message });
    }
});

// API: List istanze
app.get('/api/instances', (req, res) => {
    try {
        const output = execSync('docker ps -a --filter "name=luna2-" --format "{{.Names}}\t{{.Status}}"', { encoding: 'utf8' });
        const instances = output.trim().split('\n').filter(l => l).map(line => {
            const [name, status] = line.split('\t');
            return { name, status };
        });
        res.json(instances);
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// API: Logs
app.get('/api/instances/:name/logs', (req, res) => {
    try {
        const logs = execSync(`docker logs ${req.params.name}`, { encoding: 'utf8' });
        res.set('Content-Type', 'text/plain');
        res.send(logs);
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

app.listen(PORT, () => console.log(`✓ Panel: http://localhost:${PORT}`));
```

**File:** `/opt/luna2/management/app/views/dashboard.ejs`

```html
<!DOCTYPE html>
<html>
<head>
    <title>Luna2 Management</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <style>
        body { background: #f8f9fa; }
        .navbar { background: linear-gradient(90deg, #667eea 0%, #764ba2 100%); }
        .card { border-left: 4px solid #667eea; }
        .spinner-box { display: none; text-align: center; padding: 20px; }
        .success-box { display: none; background: #d4edda; border: 1px solid #c3e6cb; border-radius: 5px; padding: 15px; color: #155724; }
    </style>
</head>
<body>
    <nav class="navbar navbar-dark">
        <span class="navbar-brand h1"><i class="bi bi-gear"></i> Luna2 Management</span>
    </nav>

    <div class="container mt-5">
        <div class="card mb-4">
            <div class="card-header bg-primary text-white">
                <h5>➕ Crea Istanza</h5>
            </div>
            <div class="card-body">
                <div class="spinner-box" id="spinnerBox">
                    <div class="spinner-border text-primary mb-3"></div>
                    <p>Creazione in corso...</p>
                </div>

                <div class="success-box" id="successBox">
                    <h5>✓ Istanza creata!</h5>
                    <p id="successMessage"></p>
                </div>

                <form id="createForm">
                    <div class="row">
                        <div class="col-md-4">
                            <label class="form-label">Nome Client</label>
                            <input type="text" class="form-control" id="clientName" required>
                        </div>
                        <div class="col-md-4">
                            <label class="form-label">Dominio</label>
                            <input type="text" class="form-control" id="domain" required>
                        </div>
                        <div class="col-md-4">
                            <label class="form-label">DB Password</label>
                            <input type="password" class="form-control" id="dbPassword" minlength="12" required>
                        </div>
                    </div>
                    <button type="submit" class="btn btn-success mt-3">🚀 Crea</button>
                </form>
            </div>
        </div>

        <div class="card">
            <div class="card-header bg-info text-white">
                <h5>📊 Istanze Attive</h5>
            </div>
            <div class="card-body">
                <table class="table" id="instancesTable">
                    <thead>
                        <tr>
                            <th>Nome</th>
                            <th>Status</th>
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
        function loadInstances() {
            $.get('/api/instances', function(instances) {
                const tbody = $('#instancesTable tbody');
                tbody.empty();
                instances.forEach(inst => {
                    const status = inst.status.includes('Up') ? 'bg-success' : 'bg-danger';
                    tbody.append(`<tr><td><strong>${inst.name}</strong></td><td><span class="badge ${status}">${inst.status}</span></td><td><button class="btn btn-sm btn-info" onclick="viewLogs('${inst.name}')">Logs</button></td></tr>`);
                });
            });
        }

        $('#createForm').submit(function(e) {
            e.preventDefault();
            $('#createForm').hide();
            $('#spinnerBox').show();
            $.ajax({
                type: 'POST',
                url: '/api/instances/create',
                contentType: 'application/json',
                data: JSON.stringify({
                    clientName: $('#clientName').val(),
                    domain: $('#domain').val(),
                    dbPassword: $('#dbPassword').val()
                }),
                timeout: 300000,
                success: function(data) {
                    $('#spinnerBox').hide();
                    $('#successBox').show();
                    $('#successMessage').html(`<strong>${data.accessPoints.app}</strong><br>User: luna2_user`);
                    loadInstances();
                    setTimeout(() => {
                        $('#successBox').hide();
                        $('#createForm').show();
                        $('#createForm')[0].reset();
                    }, 5000);
                },
                error: function(xhr) {
                    $('#spinnerBox').hide();
                    $('#createForm').show();
                    alert('Errore: ' + (xhr.responseJSON?.error || 'Sconosciuto'));
                }
            });
        });

        function viewLogs(name) { window.open(`/api/instances/${name}/logs`, '_blank'); }
        loadInstances();
        setInterval(loadInstances, 30000);
    </script>
</body>
</html>
```

**Installa e avvia:**

```bash
mkdir -p /opt/luna2/management/app/views
cd /opt/luna2/management/app

# Installa dipendenze
npm install

# Avvia in background
nohup npm start > /opt/luna2/logs/management-panel.log 2>&1 &

# Verifica
sleep 2
curl http://localhost:5000 | head -10
```

**Accedi al pannello:** `http://server-ip:5000`

---

## PARTE 2: CREARE NUOVE ISTANZE CLIENTE

### Fase 2.1: Script CLI

**File:** `/opt/luna2/scripts/manage-instance.sh`

```bash
#!/bin/bash
set -e

INSTANCES_DIR="/opt/luna2/instances"
GATEWAY_DIR="/opt/luna2/gateway"

create_instance() {
    local CLIENT_NAME="$1"
    local DOMAIN="$2"
    local DB_PASS="$3"
    local INSTANCE_NAME="${CLIENT_NAME}-luna2"
    local INSTANCE_DIR="$INSTANCES_DIR/$INSTANCE_NAME"
    local DB_PORT=$((3306 + $(ls -1 $INSTANCES_DIR 2>/dev/null | wc -l)))

    if [ -z "$CLIENT_NAME" ] || [ -z "$DOMAIN" ] || [ -z "$DB_PASS" ]; then
        echo "Uso: $0 create CLIENT_NAME DOMAIN DB_PASS" >&2
        exit 1
    fi

    echo "[*] Creazione istanza: $INSTANCE_NAME..."
    mkdir -p "$INSTANCE_DIR/data/mysql" "$INSTANCE_DIR/data/luna2-files"

    # Docker-compose per istanza
    cat > "$INSTANCE_DIR/docker-compose.yml" << DOCKEREOF
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
      - "${DB_PORT}:3306"
    volumes:
      - ./data/mysql:/var/lib/mysql
    networks:
      - luna2-network

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
    volumes:
      - ${GATEWAY_DIR}/luna2.war:/usr/local/tomcat/webapps/ROOT.war
      - ./data/luna2-files:/opt/luna2/files
    networks:
      - luna2-network

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
    networks:
      - luna2-network

networks:
  luna2-network:
    name: luna2-network
    external: true
DOCKEREOF

    # Nginx config
    mkdir -p "$GATEWAY_DIR/nginx/conf.d"
    cat > "$GATEWAY_DIR/nginx/conf.d/${INSTANCE_NAME}.conf" << NGINXEOF
upstream backend_${INSTANCE_NAME} {
    server luna2-${INSTANCE_NAME}:8080;
}
upstream phpmyadmin_${INSTANCE_NAME} {
    server phpmyadmin-${INSTANCE_NAME}:80;
}

server {
    listen 80;
    server_name ${DOMAIN};
    location / { return 301 https://\$host\$request_uri; }
    location /.well-known/acme-challenge/ { root /var/www/certbot; }
}

server {
    listen 443 ssl;
    http2 on;
    server_name ${DOMAIN};
    ssl_certificate /etc/letsencrypt/live/${DOMAIN}/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/${DOMAIN}/privkey.pem;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
    client_max_body_size 100M;

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

    location /phpmyadmin/ {
        proxy_pass http://phpmyadmin_${INSTANCE_NAME}/;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
    }
}
NGINXEOF

    # Avvia
    cd "$INSTANCE_DIR"
    docker compose up -d
    docker exec luna2-nginx-gateway nginx -s reload

    echo "[✓] Istanza creata!"
    echo "  App: https://${DOMAIN}"
    echo "  PhpMyAdmin: https://${DOMAIN}/phpmyadmin/"
    echo "  DB User: luna2_user (Password: ${DB_PASS})"
}

case "${1:-help}" in
    create)  create_instance "$2" "$3" "$4" ;;
    *)       echo "Uso: $0 create CLIENT_NAME DOMAIN DB_PASS" ;;
esac
```

**Rendi eseguibile:**

```bash
chmod +x /opt/luna2/scripts/manage-instance.sh
```

---

### Fase 2.2: Crea Istanza via CLI

```bash
/opt/luna2/scripts/manage-instance.sh create \
  "acmecorp" \
  "demo.acmecorp.com" \
  "MyS3curePa55w0rd!"

# Output:
# [✓] Istanza creata!
#   App: https://demo.acmecorp.com
#   PhpMyAdmin: https://demo.acmecorp.com/phpmyadmin/
```

---

### Fase 2.3: Crea Istanza via UI (Pannello)

1. Accedi a `http://server-ip:5000`
2. Compila il form:
   - **Nome Client:** acmecorp
   - **Dominio:** demo.acmecorp.com
   - **DB Password:** MyS3curePa55w0rd!
3. Clicca **🚀 Crea**
4. Attendi 2-3 minuti per certificato SSL e deployment
5. Vedi il risultato nella sezione "Istanze Attive"

---

### Fase 2.4: Accedi all'Istanza

Appena creata:
- **App:** https://demo.acmecorp.com
- **PhpMyAdmin:** https://demo.acmecorp.com/phpmyadmin/
  - User: `luna2_user`
  - Password: quella impostata

---

## Troubleshooting

### Container non parte
```bash
docker logs luna2-INSTANCE_NAME-luna2 2>&1 | tail -50
```

### Nginx 404
```bash
# Verifica che il certificate esista
ls -la /etc/letsencrypt/live/DOMAIN/

# Ricaricare Nginx
docker exec luna2-nginx-gateway nginx -s reload
```

### MySQL non connette all'app
```bash
# Verifica che MySQL sia healthy
docker logs mysql-INSTANCE_NAME-luna2 2>&1 | head -30
```

---

## Fine Guida

Setup completo! Da ora in poi, per aggiungere nuovi client, usa il pannello o lo script CLI.
