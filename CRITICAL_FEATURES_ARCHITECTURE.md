# Luna2 - Architettura Moderna (3 Punti Critici)

## Status: 23-02-2026

### ✅ Completato

#### 1. REST API Strutturata
**Modulo**: `luna2-api` (Spring Boot 3.2)
**Porta**: 8081
**Stato**: Struttura creata, endpoints definiti, compilazione in progress

##### Endpoint Disponibili (TODO: connessione DB)
```
GET /api/v1/health               - Health check
GET /api/v1/info                 - Info server

Clienti:
GET    /api/v1/clienti           - Lista paginata
GET    /api/v1/clienti/{id}      - Dettaglio
POST   /api/v1/clienti           - Crea
PUT    /api/v1/clienti/{id}      - Aggiorna
DELETE /api/v1/clienti/{id}      - Cancella

Ordini:
GET    /api/v1/ordini            - Lista paginata
GET    /api/v1/ordini/{id}       - Dettaglio
POST   /api/v1/ordini            - Crea
PUT    /api/v1/ordini/{id}       - Aggiorna
PUT    /api/v1/ordini/{id}/stato - Cambia stato
DELETE /api/v1/ordini/{id}       - Cancella

Fatture:
GET    /api/v1/fatture           - Lista paginata
GET    /api/v1/fatture/{id}      - Dettaglio
POST   /api/v1/fatture           - Crea
PUT    /api/v1/fatture/{id}      - Aggiorna
PUT    /api/v1/fatture/{id}/stato - Cambia stato
GET    /api/v1/fatture/{id}/pdf  - Export PDF
DELETE /api/v1/fatture/{id}      - Cancella

Dashboard:
GET    /api/v1/dashboard/stats   - Statistiche
GET    /api/v1/dashboard/ricavi  - Ricavi mensili
GET    /api/v1/dashboard/clienti-top - Top clienti
GET    /api/v1/dashboard/ordini-pending - Ordini pending

Calendario:
GET    /api/v1/calendario/events - Lista eventi
POST   /api/v1/calendario/events - Crea evento
PUT    /api/v1/calendario/events/{id} - Aggiorna
DELETE /api/v1/calendario/events/{id} - Cancella
POST   /api/v1/calendario/sync   - Sync bidirezionale
GET    /api/v1/calendario/providers - Provider configurati
```

#### 2. Dashboard Real-time con WebSocket
**Tecnologia**: Spring WebSocket + STOMP
**Endpoint**: `ws://localhost:8081/ws/dashboard`

##### Architettura
```
┌─────────────────────────────────────────────────────────┐
│ Browser Client (Dashboard UI)                            │
│  - SockJS/STOMP connection                               │
│  - Subscribe to /topic/notifications                     │
│  - Real-time display of push/email notifications         │
└────────────┬────────────────────────────────────────────┘
             │
             │ WebSocket (bi-directional)
             │
┌────────────▼────────────────────────────────────────────┐
│ luna2-api (Spring Boot)                                  │
│  - WebSocketConfig (STOMP broker)                        │
│  - DashboardWebSocketController                          │
│  - DashboardNotificationService (bridge)                 │
└────────────┬────────────────────────────────────────────┘
             │
             │ Java events
             │
┌────────────▼────────────────────────────────────────────┐
│ Luna2 Main (Struts2)                                     │
│  - Event

Publisher (EventPublisher.java)                    │
│  - Notification Listeners (email, push, db)              │
│  - NotificationHistory storage                           │
└─────────────────────────────────────────────────────────┘
```

##### Topics STOMP
- `/topic/notifications` - Broadcast pubblico
- `/user/{userId}/queue/messages` - Messaggi personali
- `/user/{userId}/queue/notifications` - Notifiche personali

##### Integrazione con Notification System Luna2
```java
// Dal notification system, quando evento è pubblicato:
EventPublisher.getInstance().subscribe("FATTURA_ARRIVATA", 
    new PushNotificationListener());

// Il listener viene triggerato:
→ DashboardNotificationService.notifyUser(userId, title, message, entityType, entityId)
→ messagingTemplate.convertAndSendToUser(userId, "/queue/notifications", notification)
→ Browser riceve notifica via WebSocket
```

#### 3. Calendar Sync Bidirezionale
**Provider**: Google Calendar, iCloud (CalDAV)
**Classe**: `CalendarSyncService`

##### Flusso di Sincronizzazione (Bidirezionale)
```
POST /api/v1/calendario/sync?provider=icloud&accountId=1
│
├─ PULL: Scarica da provider esterno
│  ├─ CalDavCalendarProvider.listEvents()
│  ├─ CalDAV GET icalendar.ics
│  ├─ Parse VEvents con ical4j
│  ├─ Per ogni evento:
│  │  ├─ Se NEW (externalEventId not in DB) → INSERT
│  │  └─ Se MODIFIED → UPDATE
│
├─ MERGE: Sincronizza con events locali
│  ├─ Gestisci conflitti temporali
│  └─ Aggiorna lastSyncedAt
│
├─ PUSH: Carica modifiche locali al provider
│  ├─ Trova events con status PENDING_UPLOAD
│  ├─ Per ogni evento:
│  │  ├─ Se NEW → CalDAV POST (MKCALENDAR)
│  │  ├─ Se MODIFIED → CalDAV PUT
│  │  └─ Se CANCELLED → CalDAV DELETE
│
└─ RETURN: SyncResult
   ├─ downloadedEvents: 5
   ├─ uploadedEvents: 2
   ├─ success: true
   └─ timestamp: 2026-02-23T08:00:00
```

## 📋 Architettura Globale

### Luna2 - Stack Completo

```
┌─────────────────────────────────────────────┐
│         Browser / Mobile Client              │
│  - Dashboard (WebSocket)                     │
│  - REST API calls                            │
│  - Legacy Struts2 web                        │
└────────┬────────────────────────────────────┘
         │
    ┌────┴────────────────────┬─────────────┐
    │                         │             │
    ▼                         ▼             ▼
┌────────────────┐  ┌──────────────────┐  ┌──────────────┐
│  luna2-api     │  │  auth-service    │  │  Luna2       │
│  (Spring Boot) │  │  (Spring Boot)   │  │  (Struts2)   │
│  Port: 8081    │  │  Port: 8080      │  │  Port: 8888  │
└────────────────┘  └──────────────────┘  └──────────────┘
│ Controllers:   │  │ OAuth2 endpoint  │  │ Actions      │
├─ Clienti      │  │ Google callback  │  ├─ CRM         │
├─ Ordini       │  │ HMAC-SHA256 state│  ├─ Warehouse   │
├─ Fatture      │  │ JWT tokens       │  ├─ Fatture     │
├─ Dashboard    │  │ State validation │  ├─ Calendar    │
├─ Calendario   │  └──────────────────┘  └──────────────┘
├─ WebSocket    │
└────────────────┘
│ Services:      │
├─ CalendarSync │
├─ Dashboard    │
│  Notification │
└────────────────┘

         │
         ▼
    ┌────────────────────┐
    │   Luna2 Database   │
    │   (MySQL)          │
    │                    │
    │ Tables:            │
    ├─ Clienti           │
    ├─ Ordini            │
    ├─ Fatture           │
    ├─ CalendarAccounts  │
    ├─ CalendarEvents    │
    ├─ Reminders         │
    ├─ Notifiche         │
    ├─ NotificationPref  │
    └─ NotificationHist  │
    └────────────────────┘

         │
         ├─ File System (email templates, PDFs)
         ├─ External APIs (Google Calendar, iCloud CalDAV)
         ├─ SMTP Server (email invio)
         └─ WebSocket Broker (STOMP)
```

## 🔄 Flussi Critici

###  Scenario 1: nuovo ordine → notifiche real-time

```
1. Utente crea ordine dal Struts2 action
   └─ OrderiAction.create()

2. EventPublisher.publish("ORDINE_CREATO", event)
   └─ Event contiene: userId, ordineId, amount...

3. Subscribers ricevono evento:
   ├─ PushNotificationListener → WebSocket
   ├─ EmailNotificationListener → SMTP
   └─ DatabaseNotificationListener → DB

4. PushNotificationListener:
   └─ DashboardNotificationService.notifyUser(
       userId: "user123",
       title: "Nuovo Ordine",
       message: "ORD-001 da ACME Corp",
       entityType: "ordine",
       entityId: 1L
     )

5. DashboardNotificationService:
   └─ messagingTemplate.convertAndSendToUser(
       "user123", "/queue/notifications", notification
     )

6. Browser riceve via WebSocket:
   ```javascript
   {
     id: "uuid...",
     title: "Nuovo Ordine",
     message: "ORD-001 da ACME Corp",
     type: "success",
     entityType: "ordine",
     entityId: 1,
     timestamp: "2026-02-23T08:30:00"
   }
   ```

7. Dashboard mostra notifica con link a /app/ordini/1
```

### Scenario 2: Sync calendario iCloud

```
1. User: POST /api/v1/calendario/sync?provider=icloud&accountId=1

2. CalendarSyncService.syncCalendars("icloud", 1):
   
   A. PULL (Scarica da iCloud):
      ├─ Recupera CalendarAccount(id=1) da DB
      ├─ PasswordEncryptionService.decrypt(caldavPasswordEncrypted)
      ├─ CalDavCalendarProvider.listEvents()
      │  ├─ HTTP GET caldavUrl/calendar.ics
      │  ├─ Basic Auth: username:decryptedPassword
      │  ├─ Parse iCalendar (ical4j)
      │  └─ Return List<CalendarEvent>
      │
      ├─ Per ogni evento da provider:
      │  ├─ Cerca in DB: eventDAO.findByExternalId(externalEventId)
      │  ├─ Se not found → eventDAO.save(evento)  [NEW]
      │  └─ Se found & modified → eventDAO.update(evento)  [UPDATE]
      │
      └─ result.downloadedEvents = 5

   B. PUSH (Carica a iCloud):
      ├─ eventDAO.findByStatus(PENDING_UPLOAD)
      ├─ Per ogni evento pending:
      │  ├─ Se NEW → CalDavCalendarProvider.createOrUpdateEvent()
      │  │           HTTP POST/MKCALENDAR + PUT
      │  │           evento.setExternalEventId(remoteId)
      │  │           evento.setStatus(SYNCED)
      │  │
      │  └─ Se MODIFIED → CalDavCalendarProvider.createOrUpdateEvent()
      │              HTTP PUT
      │
      ├─ Soft-delete: eventi marked as CANCELLED
      │  └─ CalDavCalendarProvider.deleteEvent()
      │     HTTP DELETE
      │
      └─ result.uploadedEvents = 2

3. RETURN (HTTP 200):
   ```json
   {
     "provider": "icloud",
     "timestamp": "2026-02-23T08:45:00",
     "success": true,
     "downloadedEvents": 5,
     "uploadedEvents": 2,
     "errorMessage": null,
     "warnings": []
   }
   ```

4. Browser aggiorna calendario UI
```

## 📊 Database Schema Extensions

```sql
-- Calendar
CREATE TABLE calendar_events (
  id BIGINT PRIMARY KEY,
  calendar_account_id BIGINT,
  external_event_id VARCHAR(500),  -- UID dal provider
  title VARCHAR(255),
  description TEXT,
  location VARCHAR(255),
  start_time DATETIME,
  end_time DATETIME,
  source_type ENUM('EVENT', 'REMINDER', 'TASK'),
  source_id BIGINT,
  provider VARCHAR(50),  -- GOOGLE, ICLOUD
  status ENUM('ACTIVE', 'SYNCED', 'PENDING_UPLOAD', 'SYNC_ERROR', 'CANCELLED'),
  created_at DATETIME,
  updated_at DATETIME,
  last_synced_at DATETIME,
  FOREIGN KEY (calendar_account_id) REFERENCES calendar_accounts(id)
);

-- Notification History (giàesiste)
CREATE TABLE notification_history (
  id BIGINT PRIMARY KEY,
  user_id BIGINT,
  event_type VARCHAR(100),
  channel ENUM('EMAIL', 'PUSH', 'SMS', 'SLACK', 'DATABASE'),
  subject VARCHAR(255),
  message TEXT,
  status ENUM('SENT', 'FAILED', 'DELIVERED'),
  sent_at DATETIME,
  created_at DATETIME
);

-- Notification Preferences (già esiste)
CREATE TABLE notification_preferences (
  id BIGINT PRIMARY KEY,
  user_id BIGINT,
  email_enabled BOOLEAN DEFAULT 1,
  push_enabled BOOLEAN DEFAULT 1,
  sms_enabled BOOLEAN DEFAULT 0,  -- Placeholder
  slack_enabled BOOLEAN DEFAULT 0,
  created_at DATETIME,
  updated_at DATETIME
);
```

## 🚀 Deploy & Run

### Option A: Docker Compose (All Services)
```yaml
version: '3.8'
services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_DATABASE: luna2
      MYSQL_ROOT_PASSWORD: root
    ports:
      - "3306:3306"

  auth-service:
    build: ./auth-service
    ports:
      - "8080:8080"
    depends_on:
      - mysql

  luna2-api:
    build: ./luna2-api
    ports:
      - "8081:8081"
    depends_on:
      - mysql
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/luna2

  luna2:
    build: .
    ports:
      - "8888:8888"
    depends_on:
      - mysql
```

### Option B: Local Development
```bash
# Terminal 1: Luna2 Struts2 (port 8888)
cd /workspaces/Luna2
mvn clean package -DskipTests
java -jar target/luna2.war

# Terminal 2: auth-service (port 8080)
cd auth-service
java -jar target/luna2-auth-service-1.0.0.jar

# Terminal 3: luna2-api (port 8081)
cd luna2-api
mvn spring-boot:run
```

## ⚠️ TODO - Next Steps

### luna2-api (Completamento Compilazione)
- [ ] Fix DTO constructors (aggiungere tutti i costruttori missing)
- [ ] Test endpoint health check
- [ ] Connessione JPA a database Luna2
- [ ] Repository layer per Clienti, Ordini, Fatture

### WebSocket Integration
- [ ] Connettere DashboardNotificationService a Luna2 EventPublisher
- [ ] Frontend HTML5 STOMP client per dashboard
- [ ] Test connectionione WebSocket da browser

### Calendar Sync
- [ ] Implementare CalendarSyncService.pullFromExternalProvider()
- [ ] Implementare CalendarSyncService.pushToExternalProvider()
- [ ] Test Google Calendar sync
- [ ] Test iCloud CalDAV sync
- [ ] Scheduled task @Scheduled per periodic sync

### Authentication & Security
- [ ] JWT token generation in AuthenticationService
- [ ] Spring SecurityConfigurer per REST API
- [ ] CORS configuration
- [ ] Rate limiting

### Missing Features
- [ ] Export PDF fatture
- [ ] Email service integration (SMTP)
- [ ] Search/Filter avanzati
- [ ] Batch operations
- [ ] Audit trail

## 📈 Performance & Scalability

### Caching Strategy
- Redis cache per clienti/ordini/fatture (TTL 5 min)
- In-memory WebSocket subscription map
- Calendar sync results cached (TTL 1 hour)

### Database Indexes
```sql
CREATE INDEX idx_calendar_events_account ON calendar_events(calendar_account_id);
CREATE INDEX idx_calendar_events_external_id ON calendar_events(external_event_id);
CREATE INDEX idx_notification_history_user ON notification_history(user_id);
CREATE INDEX idx_notification_history_created ON notification_history(created_at);
```

### Load Balancing
- Nginx reverse proxy per luna2-api (load balance 2-3 istances)
- WebSocket affinity (sticky sessions)
- Shared Redis per distributed sessions

## 🔐 Security Considerations

1. **REST API Authentication**: JWT bearer tokens (TODO)
2. **Calendar Credentials**: AES256 encryption (Jasypt) ✅
3. **OAuth2**: Centralized auth-service ✅
4. **HTTPS**: TLS 1.3 required
5. **CORS**: Whitelist domains only
6. **Rate Limiting**: 100 req/min per IP (TODO)
7. **SQL Injection Protection**: Prepared statements (JPA)
8. **XSS Protection**: Content-Security-Policy headers

## 📚 References

- Spring Boot 3.2 Docs: https://spring.io/projects/spring-boot
- STOMP Protocol: https://stomp.github.io/
- CalDAV RFC: https://tools.ietf.org/html/rfc4918
- iCalendar Format: https://tools.ietf.org/html/rfc5545
- OpenAPI 3.0: https://spec.openapis.org/oas/v3.0.0
