# Luna2 OAuth Central Callback Service

Questo è il servizio di callback centralizzato per il flusso OAuth multi-tenant di Luna2.

## Struttura

```
auth-service/
├── src/
│   ├── main/
│   │   ├── java/it/zensoftware/luna2/authservice/
│   │   │   ├── AuthServiceApplication.java
│   │   │   ├── controller/
│   │   │   │   └── OAuthCallbackController.java
│   │   │   └── oauth/
│   │   │       └── OAuthStateValidator.java
│   │   └── resources/
│   │       └── application.properties
│   └── test/
├── pom.xml
├── DEPLOYMENT.md
├── .env.example
└── README.md
```

## Avvio rapido

### Prerequisites
- Java 11+
- Maven 3.8+

### Build e run

```bash
cd auth-service
mvn clean package

# Con variabili d'ambiente
export OAUTH_GOOGLE_STATE_SECRET="your-secret-key"
java -jar target/luna2-auth-service-1.0.0.jar

# Oppure con passaggio di parametri
java -jar target/luna2-auth-service-1.0.0.jar \
  --oauth.google.state.secret="your-secret-key" \
  --oauth.google.state.ttl.seconds=900
```

Il servizio ascolterà su `http://localhost:8080`.

## Endpoint principale

**GET** `/oauth/google/callback`

Riceve la callback da Google OAuth e redirige l'utente verso l'istanza cliente corretta.

### Parametri query

- `code` (obbligatorio): Authorization code da Google
- `state` (obbligatorio): JWT firmato contenente tenant + returnUrl + uid + timestamp
- `error` (opzionale): Errore da Google (es. `access_denied`)

### Response

- **Success**: Redirect 302/303 a `https://{tenant}/app/calendar/calendar-google-callback?code=...&state=...`
- **Error**: Redirect a error page con codice errore

## Configurazione

Vedi [DEPLOYMENT.md](DEPLOYMENT.md) per dettagli su variabili di ambiente e deployment.

### Per gli sviluppatori Luna2

Ricorda che il servizio centrale e tutte le istanze cliente devono condividere la **stessa** `oauth.google.state.secret`.

## Security Notes

- Lo state è firmato con HMAC-SHA256
- TTL del state protegge da replay attacks (default 15 minuti)
- Nonce aggiunto per ogni richiesta
- Timestamp validato per rilevare stati scaduti

Vedi [DEPLOYMENT.md](DEPLOYMENT.md) per più dettagli.
