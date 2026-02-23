# Luna2 OAuth Central Callback Service

Servizio centralizzato di callback OAuth per Google Calendar multi-tenant.

## Architettura

```
1. Istanza cliente (es. cliente1.gestionaleluna.it)
   ↓
2. Utente autorizza → Google OAuth flow
   ↓
3. Google redirige → https://auth.gestionaleluna.it/oauth/google/callback
   ↓
4. Auth Service valida state, redirige → https://cliente1.gestionaleluna.it/app/calendar/calendar-google-callback
   ↓
5. Istanza cliente completa token exchange e salva i token
```

## Build

```bash
cd auth-service
mvn clean package -DskipTests
java -jar target/luna2-auth-service-1.0.0.jar
```

## Configurazione

### Variabili di ambiente (su https://auth.gestionaleluna.it)

- `OAUTH_GOOGLE_STATE_SECRET`: chiave segreta per firmare/verificare lo state OAuth
  - Generare con: `openssl rand -base64 32`
  - Deve essere la **stessa** usata da tutte le istanze Luna2 cliente

- `OAUTH_GOOGLE_STATE_TTL_SECONDS`: TTL dello state in secondi (default 900 = 15 minuti)

### application.properties

```properties
oauth.google.state.secret=${OAUTH_GOOGLE_STATE_SECRET}
oauth.google.state.ttl.seconds=${OAUTH_GOOGLE_STATE_TTL_SECONDS:900}
```

## Deployment su https://auth.gestionaleluna.it

### Option 1: Docker

```dockerfile
FROM openjdk:11-jre-slim
WORKDIR /app
COPY target/luna2-auth-service-1.0.0.jar app.jar
ENV OAUTH_GOOGLE_STATE_SECRET=${OAUTH_GOOGLE_STATE_SECRET}
ENV OAUTH_GOOGLE_STATE_TTL_SECONDS=900
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

Deploy con reverse proxy (nginx/Apache) che mappi `/oauth/google/callback` a `http://localhost:8080/oauth/google/callback`.

### Option 2: JAR standalone

```bash
java -jar luna2-auth-service-1.0.0.jar \
  --oauth.google.state.secret="XXXXX" \
  --oauth.google.state.ttl.seconds=900
```

## Endpoint

### GET /oauth/google/callback

Riceve il callback da Google con:
- `code`: authorization code
- `state`: JWT firmato con payload JSON Base64-URL encoded + firma HMAC-SHA256
- `error`: (opzionale) errore da Google

**Azioni:**
1. Valida lo state (firma, scadenza)
2. Estrae tenant e returnUrl dallo state
3. Redirige a: `https://{tenant}/app/calendar/calendar-google-callback?code={code}&state={state}`

## Configurazione Luna2 (Istanza Cliente)

In ogni istanza Luna2, impostare in `application.properties`:

```properties
calendar.google.clientId=294689541039-9lik6th1m2q1q7utibji84tqbt6b1spl.apps.googleusercontent.com
calendar.google.clientSecret=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
calendar.google.redirectUri=https://auth.gestionaleluna.it/oauth/google/callback
calendar.google.stateSecret=<STESSA CHIAVE DEL SERVIZIO CENTRALE>
calendar.tenantKey=<DOMINIO_CLIENTE>
```

## Flow dettagliato

1. Utente su cliente1.gestionaleluna.it clicca "Connetti Google Calendar"
2. CalendarAction.connectGoogle() genera lo state firmato con:
   ```json
   {
     "tenant": "cliente1.gestionaleluna.it",
     "returnUrl": "https://cliente1.gestionaleluna.it/app/calendar/calendar",
     "uid": 123,
     "ts": 1708608345000,
     "nonce": "base64_random"
   }
   ```
3. Stato firmato: `payload_b64.signature`
4. Redirige a Google: `https://accounts.google.com/o/oauth2/v2/auth?...&state=payload_b64.signature`
5. Google chiede autorizzazione all'utente
6. Google redirige a: `https://auth.gestionaleluna.it/oauth/google/callback?code=...&state=...`
7. Auth Service valida lo state, estrae il tenant
8. Auth Service redirige a: `https://cliente1.gestionaleluna.it/app/calendar/calendar-google-callback?code=...&state=...`
9. CalendarAction.googleCallback() sull'istanza cliente:
   - Valida lo state
   - Fa token exchange con `redirectUri=https://auth.gestionaleluna.it/oauth/google/callback`
   - Salva i token nel DB
   - Redirige l'utente al dashboard calendario

## Security

- Lo state è firmato con HMAC-SHA256 usando una chiave segreta condivisa
- Ogni state ha un timestamp e un nonce per prevenire replay attacks
- La verifica dell'age dello state impedisce replay ritardati
- Firma non costante confronta i bytes per evitare timing attacks
