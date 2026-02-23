# Luna2 Server Manager

Piccolo pannello web UI per gestire:
- **Nginx** - Vhost e configurazione proxy
- **SSL** - Certificati Let's Encrypt (generazione e rinnovo)
- **Domini** - Aggiungere, rimuovere, testare domini
- **Sistema** - Monitoraggio risorse

## 🚀 Quick Start

### 1. Installare Node.js (se non già installato)

```bash
curl -fsSL https://deb.nodesource.com/setup_18.x | sudo -E bash -
sudo apt install -y nodejs
```

### 2. Installare dipendenze e avviare

```bash
cd /workspaces/Luna2/server-manager
npm install
npm start

# Oppure usare lo script:
./start.sh
```

### 3. Accedere

Apri il browser: **http://localhost:8888**

## 📱 Features

### Stato Sistema
- ✅ Status Nginx (online/offline)
- ✅ Numero domini configurati
- ✅ Uso disco
- ✅ Memoria disponibile

### Gestione Domini
- ✅ Aggiungere nuovo dominio
- ✅ Opzione SSL Let's Encrypt automatico
- ✅ Lista domini con status SSL
- ✅ Rimuovere dominio

### Certificati SSL
- ✅ Rinnova singolo dominio
- ✅ Rinnova tutti i certificati
- ✅ Visualizza data scadenza

### Strumenti
- ✅ Testa dominio (HTTP, HTTPS, API)
- ✅ Testa configurazione Nginx
- ✅ Ricarica Nginx

## 🔌 API Endpoints

Tutti gli endpoint sono disponibili via REST API:

```bash
# List domains
curl http://localhost:8888/api/domains

# Add domain
curl -X POST http://localhost:8888/api/domains \
  -H "Content-Type: application/json" \
  -d '{"domain":"example.com","generateSSL":true}'

# Test domain
curl -X POST http://localhost:8888/api/domains/example.com/test

# Renew SSL
curl -X POST http://localhost:8888/api/ssl/renew

# Get status
curl http://localhost:8888/api/status

# Health check
curl http://localhost:8888/api/health
```

## 🐳 Docker Support

Puoi anche eseguire il manager in un container Docker:

```bash
# Build image
docker build -t luna2-manager .

# Run container
docker run -d \
  -p 8888:8888 \
  -v /workspaces/Luna2:/luna2 \
  -v /var/run/docker.sock:/var/run/docker.sock \
  --name luna2-manager \
  luna2-manager
```

## 🛡️ Security

- **Authentication**: Nessuna per ora (proteggere con firewall o reverse proxy)
- **CORS**: Abilitato per semplicità (limitare in produzione)
- **Autorizzazione**: Tutti i comandi disponibili a chi accede

**Per produzione:**
1. Aggiungere autenticazione di base (htaccess, JWT, etc.)
2. Limitare CORS a origin specifici
3. Mettere il manager dietro un reverse proxy con SSL
4. Usare subnet private per il manager

## 📝 Configurazione

Il manager accede ai seguenti percorsi:
- `docker-compose-preprod.yml` - Configurazione Docker Compose
- `docker/nginx/vhosts/` - Configurazioni vhost Nginx
- `/etc/letsencrypt/` - Certificati Let's Encrypt
- `manage-domains.sh` - Script shell per gestione domini

**Assicurati che:**
- I percorsi siano corretti nel server.js
- L'utente che esegue il manager abbia permessi sudo (per certbot, docker)
- Docker sock sia accessibile

## 🐛 Troubleshooting

### "Command not found: docker-compose"

Assicurati che docker-compose sia installato:

```bash
docker-compose --version
# oppure
docker compose version
```

### "Permission denied"

Il manager potrebbe aver bisogno di sudo per alcuni comandi:

```bash
# Eseguire con sudo
sudo npm start

# Oppure aggiungere l'utente al gruppo docker
sudo usermod -aG docker $USER
```

### "Cannot find module"

Reinstallare dipendenze:

```bash
rm -rf node_modules package-lock.json
npm install
```

### Porta 8888 già in uso

Cambiare porta:

```bash
PORT=9999 npm start
```

O trovare il processo:

```bash
lsof -i :8888
kill -9 <PID>
```

## 📚 Struttura Progetto

```
server-manager/
├── server.js                 # Server principale (Express.js)
├── package.json              # Dipendenze Node.js
├── start.sh                  # Script avvio
├── public/
│   ├── index.html           # UI principale (HTML + CSS + JS)
│   └── favicon.ico          # Icona (opzionale)
└── README.md                # Questa documentazione
```

## 🔄 Flusso Applicativo

```
User (Browser)
    ↓
HTML UI (index.html)
    ↓
JavaScript client
    ↓
Express API (server.js:8888)
    ↓
Helper functions
    ↓
manage-domains.sh (shell script)
    ↓
Docker / Nginx / certbot commands
```

## 🎯 Prossimi Miglioramenti

- [ ] Autenticazione utente
- [ ] Logging persistente
- [ ] Visualizzazione logs Nginx real-time
- [ ] Editor visuale vhost config
- [ ] Statistiche uso dominio
- [ ] Backup/restore domains
- [ ] Multi-server management
- [ ] Mobile responsive (miglioramenti)

## 📋 Licenza

MIT
