# 🚀 Quick Start: Server Management & Multi-Domain Setup

Hai un sistema completo di gestione server con supporto per più domini. Ecco come iniziare.

## 📦 Cosa è Incluso

✅ **manage-domains.sh** (3.7 KB)
- Aggiungere/rimuovere domini
- Generare certificati SSL Let's Encrypt
- Testare domini
- Rinnovare certificati

✅ **init-management.sh** (3.7 KB)
- Configurare le directory per il multi-dominio
- Generare certificato self-signed test
- Inizializzare logging

✅ **docker-compose-preprod.yml** (aggiornato)
- Portainer opzionale (pannello web)
- Nginx multi-dominio configurato
- Health checks migliorati

✅ **docker/nginx/nginx-multihost.conf** (Nginx principale)
- Support per multiple vhost
- Rate limiting
- Security headers
- gzip compression

✅ **Documentazione Completa**
- SERVER_MANAGEMENT_PANEL.md (guida completa)
- PORTAINER_SETUP.md (pannello di controllo)


## ⚡ 5 Minuti per Iniziare

### Step 1: Inizializzare Directory Structure

```bash
cd /workspaces/Luna2
./init-management.sh
```

Output atteso:
```
✓ Created domain tracking file
✓ Self-signed certificate created
✓ manage-domains.sh made executable
✅ Server management setup complete!
```

### Step 2: Avviare il Deployment Docker

```bash
docker-compose -f docker-compose-preprod.yml up -d
```

Verifica:
```bash
docker-compose -f docker-compose-preprod.yml ps
```

Tutti i container dovrebbero essere **Up (healthy)**.

## 🌐 Gestire Domini

### Aggiungere un Dominio

```bash
./manage-domains.sh add example.com
```

**Prerequisiti:**
- Dominio registrato
- DNS A record → IP del tuo server
- Porte 80, 443 aperte

**Output:**
```
✓ Vhost configuration created
✓ Certificate generated successfully!
✓ Nginx configuration reloaded
✓ Domain example.com added successfully!
```

### Testare il Dominio

```bash
./manage-domains.sh test example.com
```

**Output atteso:**
```
✓ HTTP redirect works (Status: 301)
✓ HTTPS is accessible
✓ API endpoint is reachable
Response: {"status":"UP"}
```

### Elencare Domini

```bash
./manage-domains.sh list
```

**Output:**
```
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

Crea backup automatico della configurazione.


## 🎛️ Pannello di Controllo (Portainer)

### Abilitare Portainer

1. Edit `docker-compose-preprod.yml`
2. Cerca sezione `# portainer:` (attorno a linea 140)
3. Decommentare tutte le righe

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

4. Avviare:

```bash
docker-compose -f docker-compose-preprod.yml up -d portainer
```

5. Accedere: **http://your-server-ip:9000**

### Primo Accesso Portainer

1. Crea account admin (username + password)
2. Seleziona "Local" come environment
3. Clicca "Connect"

**⚠️ Cambiar password subito!**


## 📊 Usare Portainer

### Monitorare Container

1. Home → Visualizza stato tutto
2. Containers → Dettagli singoli container
3. Logs → Leggi errori

### Troubleshooting da Portainer

**Luna2 si è fermato?**
1. Containers → find "luna2-api"
2. Click → Logs tab (vedi errori)
3. Click → Restart button

**Memoria piena?**
1. Home → vedi "Disk Usage"
2. Se > 90%: da terminale `docker system prune -a`

### Backup Database

```bash
# Via terminal (non Portainer)
docker-compose -f docker-compose-preprod.yml exec mysql \
  mysqldump -u luna2_user -p luna2 > backup.sql
```


## 🛠️ Comandi Frequenti

```bash
# Aggiungere dominio
./manage-domains.sh add myapp.com

# Generare SSL
./manage-domains.sh ssl myapp.com

# Testare dominio
./manage-domains.sh test myapp.com

# Elencare domains
./manage-domains.sh list

# Rinnovare SSL (schedulare su cron)
./manage-domains.sh renew

# Vedere status container
docker-compose -f docker-compose-preprod.yml ps

# Seguire logs
docker-compose -f docker-compose-preprod.yml logs -f

# Riavviare servizio
docker-compose -f docker-compose-preprod.yml restart luna2-api

# Monitoraggio risorse
docker stats
```


## 📚 Documentazione

| File | Contenuto |
|------|-----------|
| [SERVER_MANAGEMENT_PANEL.md](SERVER_MANAGEMENT_PANEL.md) | Guida completa multi-dominio, SSL, troubleshooting |
| [PORTAINER_SETUP.md](PORTAINER_SETUP.md) | Setup e utilizzo Portainer in dettaglio |
| [DOCKER_DEPLOYMENT_CHECKLIST.md](DOCKER_DEPLOYMENT_CHECKLIST.md) | Step-by-step deployment |
| [SERVER_REQUIREMENTS.md](SERVER_REQUIREMENTS.md) | Requisiti hardware, distro Linux |


## 🔒 SSL/TLS Automatico

I certificati Let's Encrypt vengono aggiornati automaticamente.

Configura rinnovo automatico:

```bash
# Aggiungere a crontab
crontab -e

# Aggiungere riga:
0 3 * * 0 /workspaces/Luna2/manage-domains.sh renew >> /var/log/certbot-renew.log 2>&1
```

L'ultimo giorno del mese il certificato viene rinnovato automaticamente.


## ⚠️ Problemi Comuni

### "502 Bad Gateway"
- Nginx non riesce a raggiungere Luna2 API
- Soluzione: `docker-compose -f docker-compose-preprod.yml restart luna2-api`

### Dominio non raggiungibile
- Verifica DNS: `nslookup example.com` (deve mostrare tuo IP)
- Verifica firewall: `sudo ufw status`
- Testa: `curl -v http://example.com`

### Certificato non valido
- Rinova: `./manage-domains.sh ssl yourdomain.com`
- O: `./manage-domains.sh renew` (tutti i certificati)

### "Port already in use"
```bash
lsof -i :80    # Trova cosa usa porta 80
lsof -i :443   # Trova cosa usa porta 443
# Stoppa il servizio che ostacola
```

Vedi [SERVER_MANAGEMENT_PANEL.md](SERVER_MANAGEMENT_PANEL.md#-monitoraggio-e-troubleshooting) per soluzioni complete.


## 🎯 Workflow Tipico

### Nuovo Utente
1. ✅ `./init-management.sh` (setup directories)
2. ✅ `docker-compose up -d` (start containers)
3. ✅ `./manage-domains.sh add yourdomain.com` (add domain)
4. ✅ Test: `./manage-domains.sh test yourdomain.com`
5. ✅ Access: `https://yourdomain.com`

### Aggiungere Secondo Dominio
```bash
./manage-domains.sh add seconddomain.com
./manage-domains.sh test seconddomain.com
```

### Rinnovamento SSL Mensile
```bash
./manage-domains.sh renew
```

### Monitoraggio
- **GUI**: Apri Portainer → Home → vedi status
- **CLI**: `docker-compose -f docker-compose-preprod.yml ps`
- **Logs**: `docker-compose -f docker-compose-preprod.yml logs -f`


## 🔐 Security Checklist

- [ ] Cambiare password admin nel database
- [ ] Cambiare JWT_SECRET in .env
- [ ] Abilitare firewall: `sudo ufw enable`
- [ ] Aprire solo porte necessarie (22, 80, 443)
- [ ] Generare certificati SSL reali (non self-signed)
- [ ] Configurare backup automatico del database
- [ ] Disabilitare root SSH login
- [ ] Installare unattended-upgrades per patching automatico


## 📞 Supporto

Consulta la documentazione completa:

1. **Multi-dominio & SSL**: [SERVER_MANAGEMENT_PANEL.md](SERVER_MANAGEMENT_PANEL.md)
2. **Pannello Portainer**: [PORTAINER_SETUP.md](PORTAINER_SETUP.md)
3. **Deployment**: [DOCKER_DEPLOYMENT_CHECKLIST.md](DOCKER_DEPLOYMENT_CHECKLIST.md)
4. **Hardware/OS**: [SERVER_REQUIREMENTS.md](SERVER_REQUIREMENTS.md)


---

**Versione:** 1.0  
**Data:** 23 Febbraio 2025  
**Status:** ✅ Production Ready
