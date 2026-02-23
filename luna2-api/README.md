# Luna2 REST API

REST API moderna per Luna2 CRM, costruita con Spring Boot 3.2.

## Features

- ✅ **REST API completa**: Clienti, Ordini, Fatture, Dashboard, Calendario
- ✅ **WebSocket real-time**: Notifiche live per dashboard
- ✅ **Calendar Sync**: Sincronizzazione bidirezionale Google Calendar & iCloud
- 📝 **OpenAPI/Swagger**: Documentazione interattiva

## Architettura

```
luna2-api (Spring Boot 3.2 - porta 8081)
├── controller/          # REST endpoints
├── websocket/           # STOMP/WebSocket
├── service/             # Business logic (TODO)
├── entity/              # JPA entities
└── resources/           # Config + SQL
```

## Endpoint API

### Base
- `GET /api/v1/health` - Health check
- `GET /api/v1/info` - API info

### Clienti
- `GET /api/v1/clienti` - Lista paginata
- `GET /api/v1/clienti/{id}` - Dettaglio
- `POST /api/v1/clienti` - Crea
- `PUT /api/v1/clienti/{id}` - Aggiorna
- `DELETE /api/v1/clienti/{id}` - Cancella
- `GET /api/v1/clienti/search` - Ricerca avanzata

### Ordini
- `GET /api/v1/ordini` - Lista paginata
- `GET /api/v1/ordini/{id}` - Dettaglio
- `POST /api/v1/ordini` - Crea
- `PUT /api/v1/ordini/{id}` - Aggiorna
- `PUT /api/v1/ordini/{id}/stato` - Cambia stato
- `DELETE /api/v1/ordini/{id}` - Cancella

### Fatture
- `GET /api/v1/fatture` - Lista paginata
- `GET /api/v1/fatture/{id}` - Dettaglio
- `POST /api/v1/fatture` - Crea
- `PUT /api/v1/fatture/{id}` - Aggiorna
- `PUT /api/v1/fatture/{id}/stato` - Cambia stato
- `GET /api/v1/fatture/{id}/pdf` - Esporta PDF
- `DELETE /api/v1/fatture/{id}` - Cancella

### Dashboard
- `GET /api/v1/dashboard/stats` - Statistiche globali
- `GET /api/v1/dashboard/ricavi` - Ricavi ultimi 12 mesi
- `GET /api/v1/dashboard/clienti-top` - Top 5 clienti
- `GET /api/v1/dashboard/ordini-pending` - Ordini in sospeso

### Calendario
- `GET /api/v1/calendario/events` - Lista eventi
- `GET /api/v1/calendario/events/{id}` - Dettaglio evento
- `POST /api/v1/calendario/events` - Crea evento
- `PUT /api/v1/calendario/events/{id}` - Aggiorna evento
- `DELETE /api/v1/calendario/events/{id}` - Cancella evento
- `POST /api/v1/calendario/sync` - Sincronizza provider esterno
- `GET /api/v1/calendario/providers` - Lista provider

## WebSocket Endpoints

### STOMP (Connessione)
```
ws://localhost:8081/ws/dashboard
```

### Topics
- `/topic/notifications` - Notifiche broadcast
- `/user/{userId}/queue/messages` - Messaggi personali

### Client Example (JavaScript)
```javascript
// Connettersi
var stompClient = new StompClient();
stompClient.connect({}, function(frame) {
    console.log('Connesso:', frame);
    
    // Sottoscrivere notifiche
    stompClient.subscribe('/topic/notifications', function(message) {
        console.log('Notifica:', JSON.parse(message.body));
    });
});

// Inviare messaggio (ping)
stompClient.send('/app/dashboard/ping', {}, 
    JSON.stringify({userId: 'user123', message: 'Ping', type: 'ping'}));
```

## Calendar Sync Bidirezionale

### Flusso di Sincronizzazione
```
POST /api/v1/calendario/sync?provider=icloud
1. Fetch eventi da CalDAV provider (GET)
2. Salva nuovi eventi nel DB
3. Upload modifiche locali (PUT/DELETE)
4. Ritorna SyncResult (eventi caricati/scaricati)
```

## Build & Run

### Build
```bash
cd luna2-api
mvn clean package -DskipTests
```

### Run
```bash
java -jar target/luna2-api-1.0.0.jar
```

### Development Mode
```bash
mvn spring-boot:run
```

## Configuration

### Database
Modifica `src/main/resources/application.properties`:
```
spring.datasource.url=jdbc:mysql://localhost:3306/luna2
spring.datasource.username=luna2_user
spring.datasource.password=luna2_password
```

### Encryption Secret
```
calendar.encryption.secret=YourSecretKeyHere
```

## TODO - Implementazione

- [ ] JpaRepository per entità (Clienti, Ordini, Fatture, Calendario)
- [ ] Service layer (business logic)
- [ ] Ricerca avanzata nel database
- [ ] Export PDF fatture
- [ ] Rate limiting API
- [ ] JWT Authentication
- [ ] Audit trail logging
- [ ] Calendar sync scheduler (@Scheduled)
- [ ] Test unitari & integrazione

## Dependencies

- Spring Boot 3.2.12
- Spring Data JPA
- Spring Security
- Spring WebSocket (STOMP)
- MySQL Connector 8.0.33
- ical4j 3.2.12 (calendario)
- jasypt 1.9.3 (encryption)
- springdoc-openapi 2.1.0 (Swagger)

## OpenAPI Documentation

Disponibile su: `http://localhost:8081/api-docs`

Swagger UI: `http://localhost:8081/swagger-ui.html`

## Integration con Luna2 Principale

Questa API può funzionare:
1. **Standalone** - accede direttamente al DB
2. **Complementare a Struts2** - affianca l'interfaccia web
3. **Sostitutiva** - per client mobile/SPA

## Supporto

Per domande o bug: 
- Issue tracker: GitHub
- Contatti: dev@zensoftware.it
