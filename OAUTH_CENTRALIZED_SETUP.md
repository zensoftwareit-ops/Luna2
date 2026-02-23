# OAuth Google Centralizzato - Guida di Configurazione

## Panoramica

Sistema OAuth centralizzato per Google Calendar, multi-tenant, con callback su `https://auth.gestionaleluna.it/oauth/google/callback`.

## Architettura

```
┌─────────────────────────────────────────────────────────────────┐
│  Istanza Cliente (es. cliente1.gestionaleluna.it)              │
│                                                                 │
│  1. User clicca "Connetti Google Calendar"                      │
│  2. CalendarAction.connectGoogle() genera state firmato         │
│     State contiene:                                             │
│     - tenant: cliente1.gestionaleluna.it                        │
│     - returnUrl: https://cliente1.gestionaleluna.it/app/calendar/...
│     - uid: ID utente                                            │
│     - ts: timestamp (protezione replay)                         │
│     - nonce: random (protezione replay)                         │
│     State firmato: [payload_b64].[hmac_sha256_signature]        │
│  3. Redirige a Google OAuth                                     │
└─────────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│  Google OAuth                                                   │
│                                                                 │
│  1. User autorizza l'accesso al calendario                      │
│  2. Google redirige con authorization_code                      │
└─────────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│  https://auth.gestionaleluna.it/oauth/google/callback           │
│  (Auth Service)                                                 │
│                                                                 │
│  1. Riceve code + state da Google                               │
│  2. Valida state (firma, scadenza, nonce)                       │
│  3. Estrae tenant + returnUrl dallo state                       │
│  4. Redirige a istanza cliente + code + state                   │
└─────────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│  Istanza Cliente: /app/calendar/calendar-google-callback        │
│                                                                 │
│  1. CalendarAction.googleCallback()                             │
│  2. Verifica state (stesso secret della centrale)               │
│  3. Fa token exchange con:                                      │
│     - code                                                      │
│     - clientId                                                  │
│     - clientSecret                                              │
│     - redirectUri: https://auth.gestionaleluna.it/oauth/...    │
│  4. Riceve access_token + refresh_token                         │
│  5. Salva nel DB (CalendarAccount)                              │
│  6. Redirige utente al dashboard calendario                     │
└─────────────────────────────────────────────────────────────────┘
```

## Setup

### 1. Genera la chiave segreta di firma

Sulla macchina di administrazione:

```bash
openssl rand -base64 32
```

Output es: `Gkt5gJkFZy+rLrSt5cVyM2q4s3e7b9Xy+Z1wH3p4Q5R=`

Salva questo valore per il passo successivo.

### 2. Deploy Auth Service su https://auth.gestionaleluna.it

#### Step 2a: Prepara il JAR

```bash
cd auth-service
mvn clean package -DskipTests
cp target/luna2-auth-service-1.0.0.jar /path/to/deployment/
```

#### Step 2b: Configurazione docker-compose (opzionale)

```yaml
version: '3.8'

services:
  auth-service:
    image: openjdk:11-jre-slim
    container_name: luna2-auth-service
    volumes:
      - ./luna2-auth-service-1.0.0.jar:/app/app.jar
    environment:
      OAUTH_GOOGLE_STATE_SECRET: "Gkt5gJkFZy+rLrSt5cVyM2q4s3e7b9Xy+Z1wH3p4Q5R="
      OAUTH_GOOGLE_STATE_TTL_SECONDS: "900"
    ports:
      - "8080:8080"
    command: java -jar /app/app.jar
    restart: unless-stopped

  nginx:
    image: nginx:alpine
    container_name: luna2-auth-proxy
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf:ro
      - ./certs/:/etc/nginx/certs/:ro
    ports:
      - "443:443"
    depends_on:
      - auth-service
    restart: unless-stopped
```

#### Step 2c: Configurazione Nginx (SSL/TLS)

```nginx
server {
    listen 443 ssl http2;
    server_name auth.gestionaleluna.it;

    ssl_certificate /etc/nginx/certs/gestionaleluna.it.crt;
    ssl_certificate_key /etc/nginx/certs/gestionaleluna.it.key;

    location /oauth/google/callback {
        proxy_pass http://auth-service:8080/oauth/google/callback;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location /error {
        proxy_pass http://auth-service:8080/error;
        proxy_set_header Host $host;
    }
}

server {
    listen 80;
    server_name auth.gestionaleluna.it;
    return 301 https://$server_name$request_uri;
}
```

### 3. Configura ogni istanza cliente

Su **ogni** istanza Luna2 cliente, modifica `application.properties`:

```properties
# Google OAuth - Callback centralizzato
calendar.google.clientId=294689541039-9lik6th1m2q1q7utibji84tqbt6b1spl.apps.googleusercontent.com
calendar.google.clientSecret=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
calendar.google.redirectUri=https://auth.gestionaleluna.it/oauth/google/callback

# Firma del state - STESSA CHIAVE DEL SERVIZIO CENTRALE
calendar.google.stateSecret=Gkt5gJkFZy+rLrSt5cVyM2q4s3e7b9Xy+Z1wH3p4Q5R=

# TTL dello state (secondi)
calendar.google.stateTtlSeconds=900

# Identificativo univoco per il tenant (default: server hostname)
calendar.tenantKey=cliente1.gestionaleluna.it
```

## Test

### Test 1: Connessione locale (dev)

Se stai testando in locale con domini finti, puoi far transitare il traffic locale verso la callback centrale via edit del file /etc/hosts:

```bash
127.0.0.1 localhost
127.0.0.1 client1.local
127.0.0.1 auth.gestionaleluna.it
```

Avvia auth-service:

```bash
cd auth-service
java -jar target/luna2-auth-service-1.0.0.jar \
  --oauth.google.state.secret="your-secret-key" \
  --server.port=8080
```

Avvia Luna2 client su porta diversa (es. 8081):

```bash
# In luna2 root
mvn tomcat7:run -Dmaven.tomcat.port=8081
```

Accedi a client (es. http://client1.local:8081) e clicca "Connetti Google Calendar".

### Test 2: Analisi dello state

Per debugging, puoi decodificare lo state generato:

```bash
# Estrai il payload
payload_b64="<prime_parte_dello_state>"
echo "$payload_b64" | base64 -d | jq .
```

Output atteso:

```json
{
  "tenant": "client1.gestionaleluna.it",
  "returnUrl": "https://client1.gestionaleluna.it/app/calendar/calendar",
  "uid": 123,
  "ts": 1708608345000,
  "nonce": "z8K2q7M9x4R5vL3p"
}
```

## Security Considerations

- **State Signature**: HMAC-SHA256 con chiave segreta condivisa
- **TTL**: Default 900 secondi (15 minuti). Regola `calendar.google.stateTtlSeconds` se necessario
- **Nonce**: Generato casualmente per ogni richiesta
- **Replay Attack Protection**: Timestamp + TTL + nonce
- **HTTPS**: Sempre usare HTTPS in produzione per auth.gestionaleluna.it
- **Secret Key**: Non mettere mai la chiave in repo pubblici; usare variabili d'ambiente

## Troubleshooting

### "State validation failed"

- Verifica che `calendar.google.stateSecret` sia identico su auth-service e Luna2
- Verifica che l'orologio del server non sia scaduto di più di TTL
- Controlla i log di auth-service per errori di firma

### "Token exchange failed"

- Verifica che `calendar.google.clientSecret` sia corretto
- Verifica che `calendar.google.redirectUri=https://auth.gestionaleluna.it/oauth/google/callback` sia registrato in Google Cloud Console
- Assicurati che il `code` non sia scaduto (di solito 10 minuti)

### "Missing tenant or returnUrl"

- Verifica che il state sia ben formato (payload parte decódáble da Base64)
- Controlla che il nonce sia presente

### Auth Service non raggiungibile

- Verifica la configurazione Nginx reverse proxy
- Assicurati che il certificato SSL sia valido
- Controlla i log di docker/nginx per errori di connessione

## Riferimenti

- [Auth Service README](../auth-service/README.md)
- [Auth Service DEPLOYMENT](../auth-service/DEPLOYMENT.md)
- [CalendarAction Implementation](../src/main/java/it/zensoftware/luna2/action/CalendarAction.java)
- [OAuth State Validator](../auth-service/src/main/java/it/zensoftware/luna2/authservice/oauth/OAuthStateValidator.java)
- [Google OAuth Documentation](https://developers.google.com/identity/protocols/oauth2)
