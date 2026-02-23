# Quick Start - Testare OAuth Centralizzato Localmente

## Prerequisiti

- Java 11+
- Maven 3.8+
- Docker + Docker Compose (opzionale, per deploy con proxy)
- Git

## Step 1: Build dei servizi

```bash
# Build auth-service
cd auth-service
mvn clean package -DskipTests
cd ..

# Build Luna2
mvn clean package -DskipTests
```

## Step 2: Configurazione locale

### Option A: Test in RAM con H2 (Rapido)

Modifica `/etc/hosts` (su Mac/Linux) o `C:\Windows\System32\drivers\etc\hosts` (su Windows):

```
127.0.0.1 localhost
127.0.0.1 client1.local
127.0.0.1 client2.local
127.0.0.1 auth.local
```

### Option B: Test con Docker Compose (Raccomandato)

```bash
# Se hai certificati SSL auto-firmati (opzionale per dev)
mkdir -p docker/nginx/certs
cd docker/nginx/certs

# Genera certificato auto-firmato
openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
  -keyout gestionaleluna.it.key \
  -out gestionaleluna.it.crt \
  -subj "/CN=auth.gestionaleluna.it"

cd /workspaces/Luna2
```

Modifica `/etc/hosts`:

```
127.0.0.1 auth.gestionaleluna.it
127.0.0.1 client1.gestionaleluna.it
127.0.0.1 client2.gestionaleluna.it
```

## Step 3: Avvia i servizi

### Opzione A: Standalone (senza proxy)

```bash
# Terminal 1: Auth Service
cd auth-service
java -jar target/luna2-auth-service-1.0.0.jar \
  --oauth.google.state.secret="Wmc3ik2d7J1nvCqH4ToQjTzKBphr5LaQ+PYwPpXLicU=" \
  --server.port=8080

# Terminal 2: Luna2 client 1
cd ..
export MAVEN_OPTS="-Dcustomer.domain=client1.gestionaleluna.it"
mvn tomcat7:run -Dmaven.tomcat.port=8081

# Terminal 3: Luna2 client 2 (opzionale)
export MAVEN_OPTS="-Dcustomer.domain=client2.gestionaleluna.it"
mvn tomcat7:run -Dmaven.tomcat.port=8082
```

### Opzione B: Con Docker Compose

```bash
# Copia il .env
cp auth-service/.env .env

# Avvia tutto
docker-compose -f docker-compose.auth-service.yml up
```

## Step 4: Test del flusso OAuth

### 1. Accedi all'istanza client

Browser → `http://client1.gestionaleluna.it:8081` (oppure con Docker)

Login come utente di test (credenziali default second schema).

### 2. Naviga al calendario

Menu → Calendario → "Connetti Google Calendar"

### 3. Analizza il flusso

```bash
# Terminal: Monitora i log del auth-service
docker logs -f luna2-auth-service
# o se standalone: guarda il terminal dove l'hai lanciato
```

Osserva:
- ✅ URL Google con `state` parameter
- ✅ Redirect a auth.gestionaleluna.it/oauth/google/callback
- ✅ Auth service valida lo state
- ✅ Redirect di ritorno al client1
- ✅ Token exchange completato

### 4. Debug state (opzionale)

Se vuoi analizzare manualmente lo state generato:

```bash
# Durante il flusso OAuth, copia lo state dal URL
# Esempio: ...&state=eyJhbGc...&...

# Decode (da shell con node/python/etc):
node -e "console.log(JSON.stringify(JSON.parse(Buffer.from('PASTE_HERE', 'base64').toString('utf8')), null, 2))"
```

Dovresti vedere:

```json
{
  "tenant": "client1.gestionaleluna.it",
  "returnUrl": "http://client1.gestionaleluna.it:8081/app/calendar/calendar",
  "uid": 123,
  "ts": 1708608345000,
  "nonce": "z8K2q7M9..."
}
```

## Troubleshooting

### "Connection refused on oauth callback"

- Assicurati che le porte siano corrette
- Verifica `/etc/hosts` entries
- Con Docker, controlla che auth-service sia up: `docker ps`

### "State validation failed"

- ✅ Controlla che `calendar.google.stateSecret` su Luna2 == `OAUTH_GOOGLE_STATE_SECRET` su auth-service
- ✅ Di solito nel file `.env` e in `application.properties`
- ✅ Se lo hai cambiato, rebuild entrambi

### "Google OAuth error: redirect_uri_mismatch"

- Non stai usando le credenziali Google reali in locale
- Questo è OK: la callback HTTP locale non è registrata in Google
- Per testare token exchange vero, usa env di staging con URL reali

### Port già in uso

```bash
# Trova processo sulla porta
lsof -i :8080  # o 8081, 8082, etc.

# Uccidi processo
kill -9 <PID>
```

## Test Multi-tenant

Ripeti il processo su client2 (porta 8082) e vedrai che:

1. Ogni client genera il suo state con il suo tenant
2. Auth service valida correttamente per ciascuno
3. Redirect torna all'istanza giusta

## Cleanup

```bash
# Stop Docker
docker-compose -f docker-compose.auth-service.yml down

# Oppure CTRL+C nei terminal standalone

# Pulisci build
mvn clean
```

## Note Finali

- **State secret** è già pre-caricato: `Wmc3ik2d7J1nvCqH4ToQjTzKBphr5LaQ+PYwPpXLicU=`
- **Google OAuth** non funzionerà con hostname locali; solo flusso state/redirect
- **Token exchange** funzionerà solo se registri dominio reale in Google Cloud Console
- Per produzione, vedi [OAUTH_CENTRALIZED_SETUP.md](OAUTH_CENTRALIZED_SETUP.md)

---

Happy testing! 🚀
