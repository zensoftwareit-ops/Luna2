# Server Management Panel & Multi-Domain Setup

Guida completa per gestire il server Luna2, configurare più domini e accedere al pannello di controllo.

## 📋 Sommario

1. [Strumenti Disponibili](#strumenti-disponibili)
2. [Pannello di Controllo Portainer](#pannello-di-controllo-portainer)
3. [Gestione Domini](#gestione-domini)
4. [Setup SSL/TLS](#setupssltls)
5. [Configurazione Nginx](#configurazione-nginx)
6. [Monitoraggio e Troubleshooting](#monitoraggio-e-troubleshooting)

---

## 🛠️ Strumenti Disponibili

### 1. **Portainer** – UI di Gestione Docker

Portainer è un pannello web minimale per gestire container, immagini, volumi e reti Docker.

**Abilitazione:**

```bash
# Nel file docker-compose-preprod.yml, decommentare la sezione portainer:
docker-compose -f docker-compose-preprod.yml up -d portainer
```

**Accesso:**
- URL: `http://your-server-ip:9000`
- Credenziali iniziali: `admin` / `12345678`
- **⚠️ IMPORTANTE:** Cambiarle immediatamente dopo il primo login

**Funzionalità:**
- Visualizzare stato container, immagini, volumi
- Restart/stop container
- View logs
- Modificare variabili d'ambiente (se necessario)
- Resource monitoring (CPU, memoria, disco)
- Upload di config files

### 2. **manage-domains.sh** – Script Gestione Domini

Script bash per aggiungere, rimuovere, testare e rinnovare domini SSL.

**Utilizzo:**

```bash
# Mostrare aiuto
./manage-domains.sh help

# Aggiungere un nuovo dominio
./manage-domains.sh add example.com

# Rimuovere un dominio
./manage-domains.sh remove example.com

# Generare SSL per un dominio
./manage-domains.sh ssl example.com

# Rinnovare tutti i certificati SSL
./manage-domains.sh renew

# Testare un dominio
./manage-domains.sh test example.com

# Elencare tutti i domini
./manage-domains.sh list
```

---

## 🎛️ Pannello di Controllo Portainer

### Installazione Iniziale

**Step 1: Abilitare Portainer**

Apri `docker-compose-preprod.yml` e cerca la sezione commentata:

```yaml
# portainer:
#   image: portainer/portainer-ce:latest
#   ...
```

Decommentala (rimuovi i `#`):

```yaml
portainer:
  image: portainer/portainer-ce:latest
  container_name: luna2-portainer
  
  ports:
    - "9000:9000"
    - "8000:8000"
  
  volumes:
    - /var/run/docker.sock:/var/run/docker.sock
    - portainer_data:/data
  
  networks:
    - luna2-network
  
  restart: unless-stopped
```

**Step 2: Avviare Portainer**

```bash
docker-compose -f docker-compose-preprod.yml up -d portainer
```

Aspetta 10-15 secondi che si avvii:

```bash
docker-compose -f docker-compose-preprod.yml logs portainer
```

**Step 3: Accedere**

Apri il browser: `http://your-server-ip:9000`

- Crea un account admin (username, password)
- Seleziona "Local" come environment
- Esplora il dashboard

### Funzioni Principali in Portainer

#### Monitoraggio Container

1. **Home** → Visualizza stato di tutti i container
2. **Containers** → Dettagli di ogni container, restart, logs
3. **Images** → Immagini Docker disponibili, pull di nuove

#### Troubleshooting

**Container Luna2 si è fermato?**
- Vai in Containers
- Cerca `luna2-api`
- Clicca sulla riga
- Scorri a "Logs" per leggere gli errori
- Clicca "Start" per riavviarlo

**Memoria piena?**
- Vai in Dashboard
- Guarda "Disk Usage"
- Se > 90%, run: `docker system prune -a`

#### Backup Database

```bash
# Da terminale (non via Portainer)
docker-compose -f docker-compose-preprod.yml exec mysql \
  mysqldump -u luna2_user -p luna2 > backup.sql
```

---

## 🌐 Gestione Domini

### Architettura Multi-Dominio

```
┌─────────────────────────────────────────┐
│         DNS (A record)                  │
│  example.com → 192.168.1.100            │
│  app.com → 192.168.1.100                │
└──────────────────┬──────────────────────┘
                   │
                   ▼
        ┌─────────────────────┐
        │   Nginx (Port 80)   │────┬─────── example.com.conf
        │   (Port 443)        │    ├─────── app.com.conf
        │   Reverse Proxy     │    └─────── default.conf
        └─────────────────────┘
                   │
                   ▼
        ┌─────────────────────┐
        │   Luna2 API:8080    │
        │  (Container interno)│
        └─────────────────────┘
```

Ogni dominio ha:
- File di configurazione Nginx (es. `docker/nginx/vhosts/example.com.conf`)
- Certificato SSL (Let's Encrypt)
- Rewrite rules, rate limiting, headers custom

### Aggiungere un Nuovo Dominio

**Prerequisiti:**
- Dominio registrato (GoDaddy, Namecheap, ecc.)
- A record che punta al tuo server IP: `example.com A 192.168.x.x`
- Porte 80 e 443 aperte nel firewall

**Step 1: Preparare il Server**

```bash
# Rendere lo script eseguibile
chmod +x manage-domains.sh

# Creare la directory vhosts se non esiste
mkdir -p docker/nginx/vhosts docker/nginx/ssl docker/nginx/certs
touch docker/nginx/domains.txt
```

**Step 2: Aggiungere il Dominio**

```bash
./manage-domains.sh add example.com
```

Lo script ti chiederà:
- ✓ Genera DNS? (sì)
- ✓ Genera SSL con Let's Encrypt? (sì per dominio vero, no per test)

Esempio output:

```
========================================
Adding Domain: example.com
========================================
✓ Vhost configuration created: docker/nginx/vhosts/example.com.conf
ℹ Generate SSL certificate with Let's Encrypt? (y/n) y
✓ Certificate generated successfully!
✓ Nginx configuration reloaded
✓ Domain example.com added successfully!
```

**Step 3: Testare il Dominio**

```bash
./manage-domains.sh test example.com
```

Output atteso:

```
========================================
Testing Domain: example.com
========================================
✓ HTTP redirect works (Status: 301)
✓ HTTPS is accessible
✓ API endpoint is reachable
Response: {"status":"UP"}
```

### Aggiungere Più Domini

Ripeti il processo per ogni dominio:

```bash
./manage-domains.sh add app.com
./manage-domains.sh add crm.example.com
./manage-domains.sh add api.example.com
```

### Listare Tutti i Domini

```bash
./manage-domains.sh list
```

Output:

```
========================================
Configured Domains
========================================

Domain: example.com
  SSL: ✓ (Expiry Date: 2025-05-23)
  Nginx: ✓ (Valid config)

Domain: app.com
  SSL: ✗ (No certificate)
  Nginx: ✓ (Valid config)
```

### Rimuovere un Dominio

```bash
./manage-domains.sh remove example.com
```

Ti chiederà di confermare:
- Backup della config
- Rimozione del certificato SSL

---

## 🔒 Setup SSL/TLS

### Certificati Supportati

1. **Let's Encrypt** (gratuito, automatico) ⭐ CONSIGLIATO
2. **Self-signed** (test/staging)
3. **Certificato commerciale** (GoDaddy, GlobalSign, etc.)

### Let's Encrypt (Automatico)

```bash
# Generare certificato per un dominio
./manage-domains.sh ssl example.com

# Rinnovare TUTTI i certificati (importante!)
./manage-domains.sh renew
```

**Rinnovo automatico:**

Configura un cronjob per rinnovare ogni 60 giorni:

```bash
# Aggiungere al crontab
crontab -e

# Aggiungere questa riga:
0 3 * * 0 /workspaces/Luna2/manage-domains.sh renew >> /var/log/certbot-renew.log 2>&1
```

### Self-Signed (Test)

```bash
# Generare cert self-signed (non per produzione!)
openssl req -x509 -newkey rsa:4096 -keyout docker/nginx/ssl/self-signed.key \
  -out docker/nginx/ssl/self-signed.crt -days 365 -nodes

# Usare questo cert per TUTTI i domini
# (browser mostrerà avvertimento di sicurezza)
```

---

## ⚙️ Configurazione Nginx

### File Principali

```
docker/nginx/
├── nginx-multihost.conf      # Configurazione principale
├── vhosts/                   # File per ogni dominio
│   ├── example.com.conf
│   ├── app.com.conf
│   └── domains.txt           # Lista domini
├── certs/                    # Certificati (backup)
│   └── example.com/
│       ├── fullchain.pem
│       └── privkey.pem
└── ssl/                      # Self-signed cert
    ├── self-signed.crt
    └── self-signed.key
```

### Struttura Vhost

Ogni file `docker/nginx/vhosts/{dominio}.conf`:

```nginx
upstream luna2_api {
    server luna2-api:8080;
}

server {
    # HTTP → HTTPS redirect
    listen 80;
    server_name example.com www.example.com;
    return 301 https://$host$request_uri;
}

server {
    # HTTPS con HTTP/2
    listen 443 ssl http2;
    server_name example.com www.example.com;
    
    # SSL
    ssl_certificate /etc/letsencrypt/live/example.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/example.com/privkey.pem;
    
    # Security headers
    add_header Strict-Transport-Security "max-age=31536000" always;
    add_header X-Content-Type-Options "nosniff" always;
    
    # Proxy to Luna2
    location / {
        proxy_pass http://luna2_api;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

### Customizzare un Dominio

**Esempio: Rate Limiting Specifico**

Edit `docker/nginx/vhosts/example.com.conf`:

```nginx
# Aggiungere in alto:
limit_req_zone $binary_remote_addr zone=custom_limit:10m rate=2r/s;

server {
    # ...aggiungere in location /:
    limit_req zone=custom_limit burst=10 nodelay;
}
```

**Reload config:**

```bash
docker-compose -f docker-compose-preprod.yml exec nginx nginx -s reload
```

### Vedere Logs Nginx

```bash
# Tail logs real-time
docker-compose -f docker-compose-preprod.yml logs -f nginx

# Logs specifici per dominio
docker-compose -f docker-compose-preprod.yml exec nginx tail -f /var/log/nginx/example.com-access.log

# Errori
docker-compose -f docker-compose-preprod.yml exec nginx tail -f /var/log/nginx/example.com-error.log
```

---

## 📊 Monitoraggio e Troubleshooting

### Health Check dei Container

```bash
# Verificare che tutti i container siano UP
docker-compose -f docker-compose-preprod.yml ps

# Atteso output:
# NAME                 STATUS
# luna2-mysql          Up (healthy)
# luna2-api            Up (healthy)
# luna2-nginx          Up (healthy)
# luna2-portainer      Up
```

### Log Aggregati

```bash
# Tutti i log
docker-compose -f docker-compose-preprod.yml logs

# Solo ultima ora
docker-compose -f docker-compose-preprod.yml logs --since 1h

# Seguire log in tempo reale
docker-compose -f docker-compose-preprod.yml logs -f

# Log di un singolo servizio
docker-compose -f docker-compose-preprod.yml logs -f luna2-api
```

### Problemi Comuni

#### Dominio non raggiungibile

```bash
# 1. Verificare che Nginx sia avviato
docker-compose -f docker-compose-preprod.yml ps nginx

# 2. Verificare che la config sia valida
docker-compose -f docker-compose-preprod.yml exec nginx nginx -t
# Atteso: "successful"

# 3. Controllare logs
docker-compose -f docker-compose-preprod.yml logs nginx

# 4. Verificare DNS (da terminale locale)
nslookup example.com
# Deve ritornare il tuo server IP

# 5. Testare connessione
curl -v http://example.com
curl -v -k https://example.com
```

#### Certificato SSL scaduto

```bash
# Rinnovare
./manage-domains.sh renew

# Verificare data scadenza
echo | openssl s_client -servername example.com -connect example.com:443 2>/dev/null | \
  openssl x509 -noout -dates
```

#### Nginx error: "address already in use"

```bash
# Trovare cosa usa porta 80/443
lsof -i :80
lsof -i :443

# Se è altro servizio, fermarlo:
sudo systemctl stop apache2
# oppure
sudo systemctl stop other-service

# Oppure riavviare Nginx in docker
docker-compose -f docker-compose-preprod.yml restart nginx
```

#### Luna2 API non risponde

```bash
# Verificare container
docker-compose -f docker-compose-preprod.yml ps luna2-api

# Vedere logs
docker-compose -f docker-compose-preprod.yml logs luna2-api

# Verificare connessione database
docker-compose -f docker-compose-preprod.yml exec luna2-api \
  curl -v http://mysql:3306

# Riavviare API
docker-compose -f docker-compose-preprod.yml restart luna2-api
```

#### "502 Bad Gateway" nel browser

Significa Nginx non riesce a raggiungere Luna2 API:

```bash
# Verificare che API sia healthy
docker-compose -f docker-compose-preprod.yml logs luna2-api

# Vedere ultima riga di error.log
docker-compose -f docker-compose-preprod.yml exec nginx \
  tail -20 /var/log/nginx/error.log

# Riavviare tutto in ordine
docker-compose -f docker-compose-preprod.yml restart mysql luna2-api nginx
```

### Monitoraggio Risorse

```bash
# Uso CPU/Memoria
docker stats

# Esempio output:
# CONTAINER            CPU %   MEM USAGE
# luna2-mysql          0.5%    256MB / 1GB
# luna2-api            1.2%    512MB / 1GB
# luna2-nginx          0.1%    32MB / 512MB

# Spazio disco
docker system df

# Pulizia vecchie immagini/container
docker system prune -a
```

### Backup & Restore

**Backup Database:**

```bash
docker-compose -f docker-compose-preprod.yml exec mysql \
  mysqldump -u luna2_user -p luna2 > backup-$(date +%Y%m%d).sql
```

**Restore:**

```bash
docker-compose -f docker-compose-preprod.yml exec -T mysql \
  mysql -u luna2_user -p luna2 < backup-20250223.sql
```

---

## 📝 Cheat Sheet Comandi

| Comando | Descrizione |
|---------|------------|
| `./manage-domains.sh add example.com` | Aggiungere dominio |
| `./manage-domains.sh list` | Elencare domini |
| `./manage-domains.sh test example.com` | Testare dominio |
| `./manage-domains.sh ssl example.com` | Generare SSL |
| `./manage-domains.sh remove example.com` | Rimuovere dominio |
| `docker-compose -f docker-compose-preprod.yml up -d` | Avviare tutto |
| `docker-compose -f docker-compose-preprod.yml logs -f` | Seguire logs |
| `docker-compose -f docker-compose-preprod.yml ps` | Status container |
| `docker stats` | Monitoraggio risorse |
| `docker system prune -a` | Pulizia Docker |

---

## 🚀 Prossimi Step

1. ✅ Abilitare Portainer (opzionale ma consigliato)
2. ✅ Aggiungere primo dominio: `./manage-domains.sh add yourdomain.com`
3. ✅ Verificare DNS punta al tuo server
4. ✅ Testare: `./manage-domains.sh test yourdomain.com`
5. ✅ Accedere via browser: `https://yourdomain.com`

---

**Ultimo aggiornamento:** 23 Febbraio 2025
