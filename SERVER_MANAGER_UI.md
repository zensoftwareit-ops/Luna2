# 🎛️ Luna2 Server Manager - Pannello Web UI

Una **piccola interfaccia web** per gestire Nginx, certificati SSL e domini - tutto da un unico pannello.

## 📦 Cosa è Incluso

| File | Descrizione |
|------|------------|
| **server.js** (11 KB) | Express.js API server |
| **public/index.html** | UI web (HTML + CSS + JS) |
| **package.json** | Dipendenze Node.js |
| **start.sh** | Script avvio |
| **Dockerfile** | Per eseguire come container |
| **README.md** | Documentazione completa |

## 🚀 Quick Start (5 Minuti)

### Opzione 1: Local Development

```bash
cd /workspaces/Luna2/server-manager
npm install
npm start
```

Apri browser: **http://localhost:8888**

### Opzione 2: Docker

```bash
cd /workspaces/Luna2/server-manager

# Build image
docker build -t luna2-manager .

# Run
docker run -d \
  -p 8888:8888 \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -v /workspaces/Luna2:/luna2 \
  --name luna2-manager \
  luna2-manager
```

## 📱 UI Features

### 1️⃣ **Stato Sistema**
```
┌─────────────────────────┐
│ Stato Nginx:     ✓ OK   │
│ Domini:          5      │
│ Uso Disco:       45%    │
│ Memoria:         3.2 GB │
└─────────────────────────┘
```

### 2️⃣ **Aggiungere Dominio**
```
┌─────────────────────────┐
│ Nome: example.com       │
│ ☑ SSL Let's Encrypt    │
│ [➕ Aggiungi]           │
└─────────────────────────┘
```

### 3️⃣ **Lista Domini**
```
example.com
├─ SSL: ✓ Scade: 2025-05-23
├─ [🧪 Test] [🔐 SSL] [🗑️ Rimuovi]

app.com
├─ SSL: ✗ No SSL
├─ [🧪 Test] [🔐 SSL] [🗑️ Rimuovi]
```

### 4️⃣ **Strumenti**
```
[🔄 Rinnova Tutti SSL]
[🧪 Testa Nginx Config]
[🔄 Ricarica Nginx]
```

## 🔌 API REST

Puoi usare il manager via API:

```bash
# Get all domains
curl http://localhost:8888/api/domains

# Add domain
curl -X POST http://localhost:8888/api/domains \
  -H "Content-Type: application/json" \
  -d '{"domain":"myapp.com","generateSSL":true}'

# Test domain
curl -X POST http://localhost:8888/api/domains/myapp.com/test

# Renew single SSL
curl -X POST http://localhost:8888/api/domains/myapp.com/ssl

# Renew all SSL certificates
curl -X POST http://localhost:8888/api/ssl/renew

# Reload Nginx
curl -X POST http://localhost:8888/api/nginx/reload

# Get system status
curl http://localhost:8888/api/status

# Health check
curl http://localhost:8888/api/health
```

## 🎯 Workflow Tipico

### Nuovo Dominio

1. **Vai a:** http://your-server:8888
2. **Inserisci:** `example.com`
3. **Check:** "SSL Let's Encrypt"
4. **Clicca:** "➕ Aggiungi Dominio"
5. **Aspetta:** 2-3 minuti per SSL
6. **Pulsa:** Vedi dominio in lista con ✓ SSL

### Testare Dominio

1. **In lista domain:** click [🧪 Test]
2. **Browser apre modal**
3. **Vedi risultati:**
   - ✓ HTTP redirect ✓
   - ✓ HTTPS accessible
   - ✓ API endpoint reachable

### Rinnovare SSL

1. **In globale:** click [🔄 Rinnova Tutti SSL]
2. **O singolo:** in domain popup → [🔐 SSL]
3. **Attendi:** processo completo

## 🛠️ Configurazione

Il manager accede a:

```
/workspaces/Luna2/
├── docker-compose-preprod.yml    ← accede
├── docker/nginx/vhosts/          ← legge/scrive
├── manage-domains.sh             ← esegue
└── server-manager/
    └── (accede al file system sopra)
```

**Permessi richiesti:**
- ✓ Leggere/scrivere `docker/nginx/vhosts/`
- ✓ Eseguire `manage-domains.sh`
- ✓ Accesso a `/var/run/docker.sock` (per comandi Docker)
- ✓ Accesso a `/etc/letsencrypt/` (per certificati)

## 🔐 Security Notes

**Produzione:**
- Mettere dietro NGINX con SSL
- Aggiungere HTTP Basic Auth
- Limitare a IP specifico
- Usare subnet privata

**Sviluppo:**
- OK così com'è (localhost)
- Non esporre pubblicamente
- Firewall deve bloccare porta 8888

## 🔧 Troubleshooting

### "Cannot find module"

```bash
cd /workspaces/Luna2/server-manager
npm install
```

### Porta 8888 occupata

```bash
# Cambia porta
PORT=9999 npm start

# O trova il processo
lsof -i :8888
kill -9 <PID>
```

### "Permission denied"

```bash
# Esegui con sudo (necessario per certbot, docker)
sudo npm start

# O aggiungi user a docker group
sudo usermod -aG docker $USER
# (poi riavvia shell)
```

### "docker-compose not found"

```bash
# Assicurati che sia installato
which docker-compose
# oppure
which docker
docker compose version
```

## 📊 Monitoring

Il manager si aggiorna ogni 30 secondi:
- Stato Nginx
- Status container Docker
- Utilizzo risorse sistema

Visualizza live:
- Numero domini configurati
- Domini con/senza SSL
- Data scadenza certificati
- Utilizzo disco e memoria

## 🚀 Estensioni Future

- [ ] Autenticazione JWT
- [ ] Real-time Nginx logs view
- [ ] Visual vhost config editor
- [ ] Backup/restore domains
- [ ] Multi-server management
- [ ] Monitoraggio uptime domini
- [ ] Alert scadenza certificati

## 📚 Documentazione

- [server-manager/README.md](server-manager/README.md) - Dettagli tecnici
- [SERVER_MANAGEMENT_PANEL.md](SERVER_MANAGEMENT_PANEL.md) - Guida completa
- [manage-domains.sh](manage-domains.sh) - Script underlying

## 🎬 Demo

```bash
# Terminal 1: Avvia manager
cd /workspaces/Luna2/server-manager
npm start

# Terminal 2: Aggiungi dominio via CLI
curl -X POST http://localhost:8888/api/domains \
  -H "Content-Type: application/json" \
  -d '{"domain":"test.local"}'

# Terminal 3: Vedi in UI (http://localhost:8888)
# Domain appare automaticamente nella lista!
```

---

**Pronto?** Vai a http://your-server:8888 e inizia a gestire i tuoi domini! 🚀
