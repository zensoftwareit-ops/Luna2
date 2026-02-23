# Luna2 Multi-Tenant Architecture

## Panoramica

Sistema avanzato che permette di gestire **multiple istanze completamente separate** di Luna2, una per ogni cliente/dominio. Ogni cliente ha:

- **Istanza Luna2 dedicata** con Docker container
- **Database MySQL separato** (dati isolati)
- **PhpMyAdmin** per gestire il database del cliente
- **Porta dedicata** per MySQL, App e PhpMyAdmin
- **Certificato SSL individuale** via Let's Encrypt
- **Nginx reverse proxy** che instrada il dominio alla giusta istanza

## Architettura

```
┌─────────────────────────────────────────────────────────────────┐
│                          INTERNET                                │
└──────────────┬──────────────────────────────────────┬────────────┘
               │ HTTPS                                │ HTTPS
         domain1.com                           domain2.com
               │                                      │
┌──────────────┴──────────────────────────────────────┴────────────┐
│                      NGINX GATEWAY (docker-compose-main.yml)      │
│  Port 80: HTTP redirect                                           │
│  Port 443: HTTPS termination                                      │
│  Reverse proxy per clienti                                        │
└──────────────┬──────────────────────────────────────┬────────────┘
               │ localhost:8081                       │ localhost:8082
               │ (App Port Customer 1)                │ (App Port Customer 2)
┌──────────────┴──────────────┐         ┌─────────────┴─────────────┐
│  CUSTOMER 1 (domain1.com)    │         │ CUSTOMER 2 (domain2.com)  │
├──────────────────────────────┤         ├───────────────────────────┤
│  Luna2 App (Port 8081)       │         │ Luna2 App (Port 8082)     │
│  MySQL (Port 3307)           │         │ MySQL (Port 3308)         │
│  PhpMyAdmin (Port 8000)      │         │ PhpMyAdmin (Port 8001)    │
│  Network: network-domain1.com│         │ Network: network-domain2.com
└──────────────────────────────┘         └───────────────────────────┘
```

## File Principali

### 1. docker-compose-main.yml
Contiene **solo Nginx** come entrypoint principale.

```yaml
services:
  nginx:
    # Reverse proxy per tutti i clienti
    # Carica configs da: docker/nginx/multi-tenant/*.conf
    
  portainer:
    # UI opzionale per gestire Docker
```

**Usare:**
```bash
docker-compose -f docker-compose-main.yml up -d
```

### 2. docker-compose-customer-template.yml
Template per generare docker-compose per ogni cliente.

Viene usato da `manage-domains-multitenant.sh` per generare:
- `docker-compose-customer-domain1.com.yml`
- `docker-compose-customer-domain2.com.yml`
- etc.

Ogni file contiene:
```yaml
services:
  mysql-DOMINIO
  luna2-DOMINIO
  phpmyadmin-DOMINIO
networks:
  network-DOMINIO
```

### 3. manage-domains-multitenant.sh
Script CLI per gestire clienti e domini. **Comandi principali:**

```bash
# Aggiungere un cliente
./manage-domains-multitenant.sh add acme.com "ACME Inc"
  ↓ Crea:
    - docker-compose-customer-acme.com.yml
    - docker/nginx/multi-tenant/acme.com.conf
    - data/mysql-acme.com/ (dati)
    - data/phpmyadmin-acme.com/ (UI)
    - logs/luna2-acme.com/ (logs app)

# Listare clienti
./manage-domains-multitenant.sh list

# Mostrare mapping porte
./manage-domains-multitenant.sh ports

# Testare cliente
./manage-domains-multitenant.sh test acme.com

# Richiedere SSL
./manage-domains-multitenant.sh ssl acme.com

# Rinnovare SSL
./manage-domains-multitenant.sh renew acme.com  # Un cliente
./manage-domains-multitenant.sh renew           # Tutti

# Rimuovere cliente
./manage-domains-multitenant.sh remove acme.com

# Mostrare logs
./manage-domains-multitenant.sh logs acme.com   # Luna2 logs
./manage-domains-multitenant.sh logs acme.com mysql   # MySQL logs
./manage-domains-multitenant.sh logs acme.com phpmyadmin

# Status cliente
./manage-domains-multitenant.sh status acme.com
```

### 4. docker/nginx/nginx-main.conf
Configurazione Nginx principale. Carica le configs dei clienti da:
```
/etc/nginx/conf.d/sites/*.conf
```

Ogni cliente ha file: `docker/nginx/multi-tenant/DOMINIO.conf`

### 5. docker/nginx/multi-tenant/
Directory con config Nginx per ogni cliente.

Esempio `docker/nginx/multi-tenant/acme.com.conf`:
```nginx
# HTTP -> HTTPS redirect
server {
    listen 80;
    server_name acme.com www.acme.com;
    # redirect to HTTPS
}

# HTTPS - Luna2 App
server {
    listen 443 ssl http2;
    server_name acme.com www.acme.com;
    location / {
        proxy_pass http://localhost:8081;  # Port dedicata
    }
}

# HTTPS - PhpMyAdmin
server {
    listen 443 ssl http2;
    server_name phpmyadmin.acme.com pma.acme.com;
    location / {
        proxy_pass http://localhost:8000;  # Port dedicata
    }
}
```

### 6. server-manager/server.js
API Node.js + Web UI per gestire i cliente da browser.

**Endpoint principali:**
```
GET /api/customers              # Lista clienti
POST /api/customers             # Aggiungere cliente
GET /api/customers/:domain      # Dettagli cliente
DELETE /api/customers/:domain   # Rimuovere cliente

GET /api/customers/:domain/containers
POST /api/customers/:domain/containers/:container/start
POST /api/customers/:domain/containers/:container/stop
POST /api/customers/:domain/containers/:container/restart

GET /api/customers/:domain/logs/:container
POST /api/customers/:domain/ssl
POST /api/customers/:domain/ssl/renew

GET /api/status
GET /api/health
```

## Allocazione Porte

Lo script alloca porte in sequenza:

| Servizio | Start | Esempio |
|----------|-------|---------|
| MySQL | 3307 | 3307 (client 1), 3308 (client 2), 3309 (client 3)... |
| Luna2 App | 8081 | 8081 (client 1), 8082 (client 2), 8083 (client 3)... |
| PhpMyAdmin | 8000 | 8000 (client 1), 8001 (client 2), 8002 (client 3)... |

Mapping salvato in `domains-config.txt`:
```
domain|customer|mysql_port|app_port|pma_port|created|status
acme.com|ACME Inc|3307|8081|8000|2024-02-23 15:30:00|active
example.com|Example Corp|3308|8082|8001|2024-02-23 15:35:00|active
```

## File di Configurazione

### domains-config.txt
Registro di tutti i clienti:
```
# Multi-Tenant Domain Configuration
# Format: DOMINIO|NOME_CLIENTE|PORTA_MYSQL|PORTA_APP|PORTA_PHPMYADMIN|CREATO|STATO
acme.com|ACME Inc|3307|8081|8000|2024-02-23 15:30:00|active
example.com|Example Corp|3308|8082|8001|2024-02-23 15:35:00|active
```

## Setup e Deployment

### Step 0: Prerequisiti
```bash
# Sul server Ubuntu 22.04 LTS con Docker installato
docker --version  # ≥ 20.10
docker-compose --version  # ≥ 1.29
```

### Step 1: Avviare Nginx Gateway
```bash
cd /workspaces/Luna2
docker-compose -f docker-compose-main.yml up -d
```

Verifica:
```bash
docker ps | grep nginx  # Dovrebbe mostrare nginx-gateway
curl http://localhost/health  # Dovrebbe ritornare "Gateway OK"
```

### Step 2: Aggiungere un Cliente
```bash
./manage-domains-multitenant.sh add acme.com "ACME Inc"
```

Output:
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

### Step 3: Configurare DNS
A casa provider DNS:
```
A record: acme.com       → 1.2.3.4 (IP server)
A record: www.acme.com   → 1.2.3.4 (IP server)
A record: phpmyadmin.acme.com → 1.2.3.4 (IP server)
```

Verifica (attendi 10 minuti per propagazione):
```bash
nslookup acme.com
# Dovrebbe ritornare: 1.2.3.4
```

### Step 4: Attivare SSL
```bash
./manage-domains-multitenant.sh ssl acme.com
```

Risultato:
```
[INFO] Richiedendo certificato SSL per acme.com...
Let's Encrypt will save debug logs to /var/log/letsencrypt/letsencrypt.log
...
[SUCCESS] Certificato ottenuto per acme.com
```

### Step 5: Testare Cliente
```bash
./manage-domains-multitenant.sh test acme.com
```

Output:
```
[SUCCESS] MySQL: OK (port 3307)
[SUCCESS] Luna2 App: OK (port 8081)
[SUCCESS] PhpMyAdmin: OK (port 8000)

[INFO] Container health:
NAME                          STATUS
mysql-acme.com                Up (healthy)
luna2-acme.com                Up (healthy)
phpmyadmin-acme.com           Up (healthy)
```

### Step 6: Accedere
```
Luna2: https://acme.com
  Credenziali: admin / Admin@123456

PhpMyAdmin: https://phpmyadmin.acme.com
  Host: mysql-acme.com (o localhost:3307)
  User: luna2_user
  Password: Luna2User@2024
```

## Management via Web UI

Avviare server-manager:
```bash
cd /workspaces/Luna2/server-manager
npm install  # First time only
npm start
# oppure
node server.js
```

Accedere a: **http://localhost:8888**

### Features della Web UI
- ✅ Vedere lista di tutti i clienti
- ✅ Aggiungere nuovo cliente
- ✅ Rimuovere cliente
- ✅ Visualizzare status container
- ✅ Riavviare/Fermare/Avviare container
- ✅ Visualizzare logs
- ✅ Richiedere/Rinnovare certificati SSL
- ✅ Accesso diretto a PhpMyAdmin

## Isolamento e Sicurezza

### Isolamento dei Dati
- Ogni cliente ha **database separato** (MySQL separato per ogni container)
- Database di un cliente NON accessibile da altri
- Volume Docker isolato: `data/mysql-DOMINIO/`

### Isolamento della Rete
- Ogni cliente ha **propria network Docker**: `network-DOMINIO`
- Container di un cliente comunicano solo dentro la loro network
- Isolamento totale tra clienti

### Isolamento delle Applicazioni
- Ogni istanza Luna2 ha **port dedicata** (8081, 8082, 8083...)
- Container app separati: `luna2-domain1.com`, `luna2-domain2.com`
- Istanze completamente indipendenti

### Sicurezza Addizionale
- No-new-privileges su tutti i container
- SSL/TLS obbligatorio (HTTP redirect a HTTPS)
- PhpMyAdmin può avere Basic Auth aggiuntivo
- Rate limiting su Nginx (100 req/s generale, 10 req/m login)

## Manutenzione

### Backup Automatico (TODO)
Per ogni cliente:
```bash
docker-compose -f docker-compose-customer-DOMINIO.yml exec mysql \
  mysqldump -u luna2_user -p luna2 > backup-DOMINIO-$(date +%Y%m%d).sql
```

### Monitoraggio
```bash
# Status generale
./manage-domains-multitenant.sh list

# Status specifico cliente
./manage-domains-multitenant.sh status acme.com

# Logs in real-time
./manage-domains-multitenant.sh logs acme.com luna2   # App
./manage-domains-multitenant.sh logs acme.com mysql   # Database
```

### Rinnovamento SSL (Automatico)
```bash
# Rinnovare un cliente
./manage-domains-multitenant.sh renew acme.com

# Rinnovare tutti
./manage-domains-multitenant.sh renew

# Schedulare con cron (una volta al mese)
# 0 2 1 * * cd /workspaces/Luna2 && ./manage-domains-multitenant.sh renew
```

### Rimozione Cliente
```bash
./manage-domains-multitenant.sh remove acme.com
# Chiederà conferma
# Opzione per rimuovere anche i dati (DB, logs)
```

## Troubleshooting

### Cliente non raggiungibile
```bash
# 1. Verificare DNS propagation
nslookup acme.com

# 2. Verificare container running
./manage-domains-multitenant.sh test acme.com

# 3. Verificare porta esposta
curl http://localhost:8081/healthz

# 4. Verificare logs
./manage-domains-multitenant.sh logs acme.com luna2
./manage-domains-multitenant.sh logs acme.com mysql
```

### Certificato SSL fallito
```bash
# 1. Verificare DNS propagato (aspettare 10 min)
nslookup phpmyadmin.acme.com

# 2. Verificare porta 80 aperta
curl http://acme.com/.well-known/acme-challenge/test

# 3. Rivedere logs Nginx
docker exec nginx-gateway nginx -t

# 4. Rifare SSL
./manage-domains-multitenant.sh ssl acme.com
```

### Container down o unhealthy
```bash
# Riavviare container
./manage-domains-multitenant.sh logs acme.com luna2  # Leggere errore

# Se MySQL non healthy
docker logs mysql-acme.com | tail -50

# Riavviare intero cliente
docker-compose -f docker-compose-customer-acme.com.yml restart
```

## Scaling

Il sistema supporta scaling a N clienti:

| Aspetto | Limite | Note |
|---------|--------|------|
| Clienti | 100+ | Dipende da risorse server |
| Porte | Illimitate | Allocazione automatica |
| DNS | Illimitate | Configurare A record |
| SSL | Illimitate | Let's Encrypt permette 50 cert/dominio/week |
| RAM per cliente | ~300 MB | MySQL ~100 MB, App ~200 MB |
| Disk per cliente | ~1-5 GB | Dipende dalla mole dati |

**Server consigliato per 10 clienti:**
- 4-6 CPU cores
- 8-16 GB RAM
- 50-100 GB SSD

**Server consigliato per 50+ clienti:**
- 8-16 CPU cores
- 32-64 GB RAM
- 200+ GB SSD
- Load balancer (HAProxy, Keepalived)
- Backup centralizzato

## Prossime Feature

- [ ] Backup automatico giornaliero per cliente
- [ ] Replicazione DB tra server
- [ ] Load balancing per cliente (active-active)
- [ ] Monitoring & alerting (Prometheus, Grafana)
- [ ] CLI di backup/restore
- [ ] Auto-scaling basato su CPU/Memory
- [ ] Migrare cliente tra server
- [ ] Subdomain wildcard support
