# 🎛️ Server Manager - Guida Utente

**Versione:** 2.0  
**Data:** Febbraio 2026

Guida completa all'utilizzo del **Luna2 Server Manager** per la gestione delle istanze multi-tenant.

---

## 📋 Indice

1. [Introduzione](#introduzione)
2. [Accesso al Pannello](#accesso-al-pannello)
3. [Dashboard Overview](#dashboard-overview)
4. [Creare una Nuova Istanza](#creare-una-nuova-istanza)
5. [Gestione Istanze Esistenti](#gestione-istanze-esistenti)
6. [Gestione SSL](#gestione-ssl)
7. [Update e Deploy](#update-e-deploy)
8. [Monitoring e Logs](#monitoring-e-logs)
9. [Manutenzione](#manutenzione)
10. [Troubleshooting](#troubleshooting)

---

## 🎯 Introduzione

Il **Luna2 Server Manager** è un pannello di controllo web-based per:

- ✅ **Creare** nuove istanze Luna2 per clienti
- ✅ **Gestire** istanze esistenti (enable/disable/remove)
- ✅ **Configurare SSL** (Let's Encrypt o certificati custom)
- ✅ **Deploy aggiornamenti** da GitHub a tutte le istanze
- ✅ **Monitorare** stato containers, logs, risorse
- ✅ **Amministrare** tutto da un'unica interfaccia web

**Porta:** 8888  
**URL:** `http://YOUR_SERVER_IP:8888`

---

## 🔐 Accesso al Pannello

### Step 1: Apri Browser

Naviga a:
```
http://YOUR_SERVER_IP:8888
```

Oppure, se hai configurato un dominio (consigliato):
```
https://manager.yourdomain.com
```

### Step 2: Pagina di Login

Vedrai una schermata di login pulita con:
- Campo **Username**
- Campo **Password**
- Pulsante **Login**

### Step 3: Inserisci Credenziali

**Username:** `admin`  
**Password:** quella configurata durante l'installazione (default: `Luna2Admin!`)

⚠️ **IMPORTANTE:** Se stai usando la password di default, cambiala subito!

### Step 4: Login

Click su **Login**

✅ Se le credenziali sono corrette, verrai reindirizzato alla **Dashboard**

---

## 📊 Dashboard Overview

Dopo il login, vedrai la **Dashboard** principale.

### Sezione 1: Statistics Cards (in alto)

Quattro card con statistiche sistema:

| Card | Descrizione |
|------|-------------|
| **📦 Istanze Totali** | Numero totale di istanze create |
| **🟢 Container Attivi** | Numero di container Docker running |
| **💾 Spazio Disco** | Utilizzo disco in GB |
| **🧠 Memoria Usata** | RAM utilizzata (MB/GB) |

### Sezione 2: Azioni Globali (barra superiore)

Pulsanti per azioni globali:

- **⬆️ Update da GitHub** - Sincronizza codice da repository
- **🚀 Deploy a Tutte le Istanze** - Distribuisci update a tutte le istanze
- **➕ Nuova Istanza** - Crea nuova istanza cliente
- **🔄 Aggiorna** - Refresh dati dashboard
- **🚪 Logout** - Disconnetti

### Sezione 3: Tabella Istanze

Tabella con tutte le istanze configurate:

| Colonna | Descrizione |
|---------|-------------|
| **Dominio** | Nome dominio istanza (es: `cliente.example.com`) |
| **Cliente** | Nome cliente/azienda |
| **MySQL** | Stato container database (🟢 running / 🔴 stopped) |
| **App** | Stato container applicazione (🟢 running / 🔴 stopped) |
| **SSL** | Stato certificato (🔒 valido fino a... / ⚠️ scaduto / ❌ nessuno) |
| **Azioni** | Pulsanti per gestione istanza |

### Pulsanti Azioni per Istanza

- **🚀 Deploy** - Deploy update su singola istanza
- **🔐 SSL** - Gestione certificati SSL
- **▶️ / ⏸️** - Enable/Disable istanza
- **🗑️** - Elimina istanza
- **📋 Log** - Visualizza container logs

**Aggiornamento Automatico:**  
La dashboard si aggiorna automaticamente ogni 30 secondi.

---

## ➕ Creare una Nuova Istanza

Questa è la funzione principale per aggiungere nuovi clienti.

### Prerequisiti Prima di Creare Istanza

Prima di procedere, assicurati di avere:

1. ✅ **Dominio registrato** per il cliente
2. ✅ **Record DNS A** configurato:
   ```
   cliente.example.com  A  YOUR_SERVER_IP
   ```
3. ✅ **TTL DNS basso** (300-600 secondi) per propagazione veloce
4. ✅ **Informazioni cliente** (nome, email contatto)

**⚠️ IMPORTANTE:** Il record DNS DEVE essere configurato PRIMA di creare l'istanza!

### Step 1: Click "Nuova Istanza"

Nel pannello dashboard, click sul pulsante **➕ Nuova Istanza** (in alto a destra).

Si aprirà un **form modale** con due campi.

### Step 2: Compila Form

#### Campo 1: Dominio

```
cliente.example.com
```

**Regole:**
- ✅ Solo lowercase (lettere minuscole)
- ✅ Formato: `subdomain.domain.tld` oppure `domain.tld`
- ✅ No spazi, no caratteri speciali (tranne `-` e `.`)
- ❌ No `http://` o `https://`
- ❌ No `/` alla fine

**Esempi validi:**
- `maria.example.com`
- `acmecorp.it`
- `client-123.mycompany.eu`

**Esempi NON validi:**
- `https://cliente.com` ❌ (no protocollo)
- `Cliente.COM` ❌ (no uppercase)
- `cliente .com` ❌ (no spazi)

#### Campo 2: Nome Cliente

```
Acme Corporation
```

**Regole:**
- ✅ Qualsiasi testo (lettere, numeri, spazi)
- ✅ Serve per identificazione interna
- ✅ Non viene usato tecnicamente (solo visualizzazione dashboard)

**Esempi:**
- `Mario Rossi`
- `Azienda Trasporti SRL`
- `Cliente Test #123`

### Step 3: Click "Crea Istanza"

Dopo aver compilato entrambi i campi, click sul pulsante **Crea Istanza**.

### Step 4: Attendi Creazione (2-5 minuti)

Vedrai una **progress bar** o messaggio di caricamento.

**Durante la creazione, il sistema:**

1. ✅ Valida dominio e nome cliente
2. ✅ Crea directory istanza
3. ✅ Genera `docker-compose-<dominio>.yml`
4. ✅ Crea database MySQL container
5. ✅ Inizializza schema database vuoto
6. ✅ Crea Tomcat container
7. ✅ Copia file WAR applicazione
8. ✅ Configura Nginx reverse proxy
9. ✅ Avvia containers
10. ✅ Verifica health containers

**Tempo stimato:** 2-5 minuti (dipende da risorse server)

### Step 5: Verifica Creazione

Al termine, vedrai:

✅ **Messaggio successo:** "Istanza creata con successo!"  
✅ **Nuova riga** nella tabella istanze con:
- Dominio: `cliente.example.com`
- Cliente: `Acme Corporation`
- MySQL: 🟢 running
- App: 🟢 running
- SSL: ❌ No SSL (ancora da configurare)

### Step 6: Test Accesso (Prima Configurazione SSL)

**Con HTTP (temporaneo):**
```
http://cliente.example.com
```

Dovresti vedere la **pagina di login Luna2**.

⚠️ **ATTENZIONE:** Questa è solo HTTP (non sicuro). Configura subito SSL!

---

## 🔐 Gestione SSL

Dopo aver creato l'istanza, il prossimo step è configurare **HTTPS** con certificato SSL.

### Opzione A: Let's Encrypt (Consigliato)

**Vantaggi:**
- ✅ Gratuito
- ✅ Automatico
- ✅ Rinnovo automatico ogni 90 giorni
- ✅ Affidabile e universalmente riconosciuto

#### Step 1: Click "🔐 SSL"

Nella tabella istanze, trova la riga dell'istanza e click sul pulsante **🔐 SSL**.

Si aprirà il **modale SSL Management**.

#### Step 2: Sezione "Let's Encrypt"

Vedrai:
- Info certificato attuale (se presente)
- Pulsante **Rinnova Let's Encrypt**

#### Step 3: Click "Rinnova Let's Encrypt"

Il sistema:
1. Verifica che il dominio punti al server (DNS check)
2. Chiama `certbot` per richiedere certificato
3. Configura Nginx con SSL
4. Riavvia Nginx

**Tempo:** 30-60 secondi

#### Step 4: Verifica Certificato

Dopo il successo:
- **SSL Status** diventa: 🔒 Valido fino a [data]
- **Giorni rimanenti:** 89 giorni (tipico per LE)

#### Step 5: Test HTTPS

Apri browser:
```
https://cliente.example.com
```

✅ Se vedi **lucchetto verde** nel browser → SSL funzionante!

### Opzione B: Certificato Custom (Wildcard o Acquistato)

Se hai un certificato wildcard (`*.example.com`) o un certificato acquistato da CA commerciale.

#### Prerequisiti

Devi avere due file:
1. **Certificato fullchain** (`.pem` o `.crt`) - contiene certificato + chain
2. **Chiave privata** (`.key` o `.pem`) - chiave privata

**Formato file:**
```
-----BEGIN CERTIFICATE-----
[certificato base64]
-----END CERTIFICATE-----
-----BEGIN CERTIFICATE-----
[intermediate certificate base64]
-----END CERTIFICATE-----
```

#### Step 1: Click "🔐 SSL"

Nel modale SSL Management, vai alla sezione **Upload Certificato Custom**.

#### Step 2: Upload Files

Vedrai due campi di upload:

1. **File Certificato (fullchain.pem):**
   - Click **Scegli file**
   - Seleziona il file certificato (es: `fullchain.pem`)

2. **File Chiave Privata (privkey.pem):**
   - Click **Scegli file**
   - Seleziona la chiave privata (es: `privkey.pem`)

#### Step 3: Click "Upload SSL"

Il sistema:
1. Valida formato PEM dei file
2. Copia certificati in `/etc/letsencrypt/live/<domain>/`
3. Configura Nginx con SSL custom
4. Riavvia Nginx

**Tempo:** 10-20 secondi

#### Step 4: Verifica

Stato SSL aggiornato con data scadenza certificato.

---

## 🚀 Update e Deploy

Il Server Manager include un **sistema di update e deploy** integrato per distribuire aggiornamenti software.

### Workflow Completo

```
GitHub (nuovo codice) 
    ↓
[Update da GitHub] → Pull + Build su server
    ↓
[Deploy a Tutte le Istanze] → Deploy WAR + Migrations DB
    ↓
Istanze aggiornate ✅
```

### Fase 1: Update da GitHub

#### Step 1: Click "⬆️ Update da GitHub"

Pulsante in alto nella dashboard.

Si aprirà il **modale Update System**.

#### Step 2: Verifica Info Pre-Update

Il modale mostra:
- **Branch corrente:** `main`
- **Ultimo commit:** hash e messaggio
- **WAR attuale:** data build

#### Step 3: Click "🔄 Pull da GitHub e Rebuild"

Il sistema esegue:
```bash
git pull origin main
mvn clean package -DskipTests
```

**Tempo:** 3-8 minuti (dipende da size update)

#### Step 4: Verifica Update

Al termine vedrai:
- ✅ **Git pull completato:** X file aggiornati
- ✅ **Build Maven completato:** `luna2.war` generato
- **Nuovo WAR:** data e dimensione

**⚠️ IMPORTANTE:** L'update NON modifica ancora le istanze attive! È solo preparazione.

### Fase 2: Deploy a Tutte le Istanze

Dopo l'update da GitHub, puoi distribuire a tutte le istanze.

#### Step 1: Click "🚀 Deploy a Tutte le Istanze"

Pulsante in alto nella dashboard.

Vedrai una **conferma**:
```
Vuoi davvero fare deploy su TUTTE le istanze?
Questo può richiedere diversi minuti.
```

#### Step 2: Conferma Deploy

Click **Conferma**.

#### Step 3: Deploy in Corso

Il sistema per ogni istanza:
1. ✅ Applica migrations DB (se presenti)
2. ✅ Copia nuovo WAR in Tomcat
3. ✅ Riavvia container Tomcat
4. ✅ Verifica health check

**Progress bar** mostra avanzamento (es: "3/10 istanze completate")

**Tempo stimato:** ~1-2 minuti per istanza

#### Step 4: Verifica Deploy

Al termine:
- ✅ **Tutte le istanze aggiornate**
- **Report:** X/Y istanze OK, Z errori (se presenti)

### Fase 3: Deploy Singola Istanza (Opzionale)

Se vuoi fare deploy solo su UNA istanza specifica (per testing):

#### Step 1: Click "🚀 Deploy" (sulla riga istanza)

Nella tabella istanze, click sul pulsante **🚀 Deploy** per l'istanza desiderata.

#### Step 2: Conferma

Modale conferma deploy singolo.

Click **Conferma Deploy**.

#### Step 3: Attendi (1-2 minuti)

Deploy procede come per deploy globale, ma solo su quell'istanza.

✅ Al termine: messaggio successo

---

## 📋 Monitoring e Logs

### Visualizzare Logs Container

#### Step 1: Click "📋 Log"

Nella tabella istanze, click sul pulsante **📋 Log** per l'istanza da monitorare.

#### Step 2: Modale Logs

Si apre modale con dropdown:
- **Seleziona Container:**
  - `mysql` - Logs database MySQL
  - `tomcat` - Logs applicazione Tomcat

#### Step 3: Seleziona Container e Visualizza

Seleziona container desiderato.

Vedrai ultimi **100 righe di log** in tempo reale.

**Esempio log Tomcat:**
```
[2026-02-27 10:45:23] INFO: Tomcat started on port 8080
[2026-02-27 10:45:25] INFO: Application context initialized
[2026-02-27 10:46:10] INFO: User login: admin@example.com
```

**Esempio log MySQL:**
```
[2026-02-27 10:45:20] [Note] mysqld: ready for connections
[2026-02-27 10:46:05] [Note] Access denied for user 'root'@'172.18.0.1'
```

#### Refresh Logs

Click **Aggiorna Logs** per caricare ultimi log.

### Monitoring Stato Containers

**Dashboard auto-refresh ogni 30 secondi.**

Controlla colonne **MySQL** e **App** per stato real-time:
- 🟢 **running** - Container attivo
- 🔴 **stopped** - Container spento
- ⚠️ **unhealthy** - Container in errore

---

## 🛠️ Manutenzione

### Enable/Disable Istanza

**Use case:** Disabilitare temporaneamente un'istanza (es: cliente sospeso pagamenti, manutenzione).

#### Disable Istanza

1. Click pulsante **⏸️** (pausa) sulla riga istanza
2. Conferma disabilitazione
3. Container verranno **stoppati** (non eliminati)
4. Sito offline

**Cosa succede:**
- Database e dati **rimangono intatti**
- Containers stoppati (non eliminati)
- Risorse server liberate
- Dominio ritorna 502 Bad Gateway

#### Enable Istanza

1. Click pulsante **▶️** (play) sulla riga istanza
2. Conferma riabilitazione
3. Container verranno **riavviati**
4. Sito online

**Tempo:** 30-60 secondi

### Rimuovere Istanza

**⚠️ ATTENZIONE: Azione IRREVERSIBILE! Tutti i dati verranno eliminati.**

#### Step 1: Backup Manuale (OBBLIGATORIO)

Prima di rimuovere, esegui backup manuale database:

```bash
# SSH su server
ssh luna2@YOUR_SERVER

# Export database
docker exec luna2-<dominio>-mysql mysqldump -u root -p luna2 > backup.sql
```

#### Step 2: Click "🗑️ Elimina"

Nella tabella istanze, click sul pulsante **🗑️** rosso.

#### Step 3: Conferma Eliminazione

Vedrai warning:
```
⚠️ ATTENZIONE!
Eliminare l'istanza rimuoverà:
- Containers Docker (MySQL + Tomcat)
- Database e tutti i dati
- Configurazione Nginx
- Certificati SSL

Questa operazione NON è reversibile!
```

Digita il nome dominio per confermare:
```
[_____________________]
```

Click **Elimina Definitivamente**.

#### Step 4: Rimozione

Il sistema:
1. Stoppa containers
2. Rimuove containers
3. Elimina volumes Docker (database)
4. Rimuove configurazione Nginx
5. Rimuove certificati SSL
6. Pulisce file sistema

**Tempo:** 1-2 minuti

✅ Istanza rimossa dalla tabella

### Restart Container

Se un container è in stato **unhealthy**:

#### Metodo 1: Via SSH

```bash
# SSH su server
ssh luna2@YOUR_SERVER

# Lista containers
docker ps -a | grep <dominio>

# Restart container specifico
docker restart luna2-<dominio>-tomcat
docker restart luna2-<dominio>-mysql
```

#### Metodo 2: Via Disable/Enable

1. Disable istanza (stoppa containers)
2. Attendi 10 secondi
3. Enable istanza (riavvia containers)

---

## 🔍 Troubleshooting

### Problema: "Istanza non si crea"

**Sintomi:** Errore durante creazione istanza

**Possibili cause:**

1. **DNS non configurato**
   ```bash
   # Verifica DNS
   nslookup cliente.example.com
   ```
   **Fix:** Configura record A nel pannello DNS prima di creare istanza

2. **Porta già in uso**
   ```bash
   # Verifica porte occupate
   sudo netstat -tulpn | grep :80
   sudo netstat -tulpn | grep :443
   ```
   **Fix:** Stoppa altri servizi su porta 80/443

3. **Disco pieno**
   ```bash
   # Verifica spazio disco
   df -h
   ```
   **Fix:** Libera spazio o upgrade server

### Problema: "SSL Let's Encrypt fallisce"

**Errore tipico:**
```
Failed to obtain certificate: DNS verification failed
```

**Soluzioni:**

1. **Verifica DNS propagato**
   ```bash
   dig cliente.example.com
   ```
   Deve rispondere con IP server. Se no, attendi propagazione (5-60 minuti).

2. **Verifica firewall**
   ```bash
   sudo ufw status
   ```
   Porta 80 e 443 devono essere aperte.

3. **Rate limit Let's Encrypt**
   - Massimo 5 certificati/settimana per dominio
   - Attendi 7 giorni se superato limite

### Problema: "Deploy fallisce"

**Sintomi:** Errore durante deploy

**Verifica logs:**
```bash
# Log Server Manager
sudo journalctl -u luna2-manager -n 50

# Log build Maven
cat ~/Luna2/target/maven-build.log
```

**Possibili cause:**

1. **Build Maven fallito**
   - Errori compilazione Java
   - **Fix:** Controlla codice sorgente, risolvi errori

2. **Migrations DB fallite**
   - Sintassi SQL errata
   - **Fix:** Verifica file `database/migrations/*.sql`

3. **Tomcat non risponde**
   - Container unhealthy
   - **Fix:** Restart container manualmente

### Problema: "Container sempre in restart"

**Sintomi:** Container in loop restart continuo

**Verifica logs:**
```bash
docker logs luna2-<dominio>-tomcat -f
docker logs luna2-<dominio>-mysql -f
```

**Possibili cause:**

1. **MySQL**
   - Corruzione database
   - Out of memory
   - **Fix:** Restore backup DB, aumenta RAM

2. **Tomcat**
   - WAR corrotto
   - Out of memory (heap)
   - Porta già in uso
   - **Fix:** Re-deploy WAR, aumenta heap Tomcat

### Problema: "Non riesco a fare login su pannello"

**Verifica credenziali:**
```bash
# Controlla env var servizio
sudo systemctl status luna2-manager | grep ADMIN
```

**Reset password:**
```bash
# Modifica service file
sudo nano /etc/systemd/system/luna2-manager.service

# Cambia ADMIN_PASS
Environment="ADMIN_PASS=NuovaPasswordSegreta123!"

# Ricarica e restart
sudo systemctl daemon-reload
sudo systemctl restart luna2-manager
```

### Problema: "Dashboard non si aggiorna"

**Sintomi:** Dati vecchi, statistiche non cambiano

**Soluzioni:**

1. **Hard refresh browser:** `Ctrl+Shift+R` (Chrome/Firefox)
2. **Svuota cache browser**
3. **Verifica server manager attivo:**
   ```bash
   sudo systemctl status luna2-manager
   ```

### Logs Utili

```bash
# Log Server Manager (real-time)
sudo journalctl -u luna2-manager -f

# Log Nginx access
sudo tail -f /var/log/nginx/access.log

# Log Nginx error
sudo tail -f /var/log/nginx/error.log

# Log Docker container
docker logs <container-name> -f --tail 100

# Verifica containers attivi
docker ps -a
```

---

## 📚 Best Practices

### Sicurezza

1. ✅ **Cambia password admin default** subito dopo installazione
2. ✅ **Usa SSL sempre** (Let's Encrypt o custom)
3. ✅ **Firewall attivo** (UFW) con solo porte necessarie aperte
4. ✅ **Backup regolari** database (automatico via cron)
5. ✅ **Monitoring** stato istanze quotidiano

### Performance

1. ✅ **Monitoring risorse** (CPU, RAM, Disco)
2. ✅ **Limita numero istanze** in base a risorse server
3. ✅ **Deploy in orari a basso traffico** (es: notte)
4. ✅ **Test su istanza singola** prima di deploy globale

### Manutenzione

1. ✅ **Update sistema operativo** mensile
   ```bash
   sudo apt update && sudo apt upgrade -y
   ```
2. ✅ **Pulizia Docker** mensile
   ```bash
   docker system prune -a -f
   ```
3. ✅ **Verifica certificati SSL** (scadenza)
4. ✅ **Review logs** per errori ricorrenti

---

## 🆘 Supporto

**Documentazione aggiuntiva:**
- [UBUNTU_SERVER_COMPLETE_SETUP.md](UBUNTU_SERVER_COMPLETE_SETUP.md) - Setup server da zero
- [SERVER_MANAGER_UPDATE_DEPLOY_GUIDE.md](SERVER_MANAGER_UPDATE_DEPLOY_GUIDE.md) - Deploy workflow dettagliato
- [DEPLOYMENT_UBUNTU_22_LTS.md](DEPLOYMENT_UBUNTU_22_LTS.md) - Deployment avanzato

**Repository:** https://github.com/zensoftwareit-ops/Luna2

---

**Ultima modifica:** 27 Febbraio 2026  
**Autore:** Luna2 Team  
**Versione:** 2.0
