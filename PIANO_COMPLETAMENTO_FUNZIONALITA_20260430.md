# 🎯 PIANO COMPLETAMENTO FUNZIONALITÀ LUNA2
**Data**: 30 Aprile 2026, 11:00 CET  
**Stato**: IMPLEMENTATION PLAN  
**Durata Stimata**: 40-50 ore di sviluppo

---

## 📊 STATO REALE MAPPATO

### ✅ IMPLEMENTATI (8 Controller)
```
✅ AuthController          - JWT auth working
✅ ClientiController       - 187 LOC, CRUD completo
✅ FattureController       - 266 LOC, CRUD partial (PDF = 501)
✅ OrdiniController        - 252 LOC, CRUD completo
✅ DashboardController     - 181 LOC, stats + ricavi reali
✅ CalendarioController    - Sync tax deadlines
✅ BaseController          - Base class
✅ DashboardWebSocketController - Real-time setup
```

### ❌ MANCANTI (5 Controller - BLOCKER)
```
❌ PreventiviController      - NOT FOUND (high priority)
❌ ProdottiController        - NOT FOUND (high priority)
❌ PagamentiController       - NOT FOUND (high priority)
❌ DDTController             - NOT FOUND (medium priority)
❌ CRMController             - NOT FOUND (low priority)
```

### ⚠️ INCOMPLETI (Funzionalità mancanti)
```
⚠️ FattureController
   └─ GET /fatture/{id}/pdf returns 501
   └─ NO SDI integration (XML generation, posting, polling)
   └─ NO email tracking

⚠️ OrdiniController
   └─ NO report generation
   └─ NO stock management integration

⚠️ DashboardController
   └─ NO KPI visualization
   └─ NO anomaly detection

⚠️ Pagamenti (se esiste)
   └─ NO payment method handling
   └─ NO reconciliation
```

---

## 🚀 IMPLEMENTATION ROADMAP

### PHASE 1: CREATE MISSING CONTROLLERS (2-3 giorni, ~24 ore)

#### TASK 1.1: PreventiviController (HIGH PRIORITY)
```java
// Endpoints needed:
POST   /api/v1/preventivi              - Create quotation
GET    /api/v1/preventivi              - List with pagination
GET    /api/v1/preventivi/{id}         - Get detail
PUT    /api/v1/preventivi/{id}         - Update quotation
DELETE /api/v1/preventivi/{id}         - Delete
GET    /api/v1/preventivi/{id}/pdf     - Export PDF
PUT    /api/v1/preventivi/{id}/stato   - Change state (BOZZA→INVIATO→ACCETTATO)
POST   /api/v1/preventivi/{id}/email   - Send by email with tracking
GET    /api/v1/preventivi/search       - Advanced search

// Model: Preventivo.java
├─ id, numero, cliente_id, data_creazione
├─ stato (BOZZA, INVIATO, ACCETTATO, RIFIUTATO, CONVERTITO)
├─ importo_lordo, importo_netto, importo_iva
├─ note, allegati_path
└─ data_ultimo_aggiornamento

// Related: RigaPreventivo.java
├─ preventivo_id, prodotto_id, quantita
├─ prezzo_unitario, iva_percentuale, importo_totale
└─ ordine_visualizzazione

Effort: 6-7 hours
Owner: Backend Dev 1
Template: Copy structure from FattureController
Status: 🟥 NOT STARTED
```

#### TASK 1.2: ProdottiController (HIGH PRIORITY)
```java
// Endpoints needed:
POST   /api/v1/prodotti              - Create product
GET    /api/v1/prodotti              - List (with filters)
GET    /api/v1/prodotti/{id}         - Get detail
PUT    /api/v1/prodotti/{id}         - Update product
DELETE /api/v1/prodotti/{id}         - Delete
POST   /api/v1/prodotti/import-csv   - Bulk import from CSV
GET    /api/v1/prodotti/search       - Search by nome/sku

// Model: Prodotto.java
├─ id, nome, descrizione, prezzo_unitario
├─ giacenza, sku, categoria
└─ data_creazione, attivo

Effort: 4-5 hours
Owner: Backend Dev 1
Template: Similar to ClientiController (simpler)
Status: 🟥 NOT STARTED
```

#### TASK 1.3: PagamentiController (HIGH PRIORITY)
```java
// Endpoints needed:
POST   /api/v1/pagamenti           - Register payment
GET    /api/v1/pagamenti           - List with filters
GET    /api/v1/pagamenti/{id}      - Get detail
PUT    /api/v1/pagamenti/{id}      - Update payment
DELETE /api/v1/pagamenti/{id}      - Void payment
GET    /api/v1/pagamenti/search    - Search by fattura/data/cliente

// Model: Pagamento.java
├─ id, fattura_id, importo, data_pagamento
├─ metodo (BONIFICO, CARTA, CONTANTI, ASSEGNO, ALTRO)
├─ riferimento (bank code, transaction id)
└─ note

Effort: 4-5 hours
Owner: Backend Dev 2
Status: 🟥 NOT STARTED
```

#### TASK 1.4: DDTController (MEDIUM PRIORITY)
```java
// Endpoints needed:
POST   /api/v1/ddt              - Create DDT
GET    /api/v1/ddt              - List
GET    /api/v1/ddt/{id}         - Detail
PUT    /api/v1/ddt/{id}         - Update
DELETE /api/v1/ddt/{id}         - Delete
GET    /api/v1/ddt/{id}/pdf     - Export PDF
PUT    /api/v1/ddt/{id}/stato   - Change state

// Model: DDT.java
├─ id, numero_ddt, cliente_id, data_creazione
├─ data_consegna, stato
├─ note, righe (RigaDDT)
└─ metadata

Effort: 5-6 hours
Owner: Backend Dev 2
Status: 🟥 NOT STARTED
```

#### TASK 1.5: CRMController (LOW PRIORITY - defer to June)
```
// Features needed (SPIKE): Lead management, pipeline, tasks
// Can defer to Phase 2 (June)
// Minimal MVP: Lead CRUD + pipeline states
```

---

### PHASE 2: COMPLETE MISSING FEATURES (3-4 giorni, ~24 ore)

#### TASK 2.1: PDF Generation (FattureController)
```java
// Currently: GET /fatture/{id}/pdf returns 501
// Need: Generate PDF using iText or Flying Saucer

Dependencies to add (pom.xml):
├─ com.itextpdf:itext7-core (or Flying Saucer)
├─ org.freemarker:freemarker (for templates)
└─ commons-io (for byte array handling)

Implementation:
1. Create PdfService.java
2. Load template (HTML → PDF)
3. Inject data (fattura, righe, cliente)
4. Return byte[] with correct headers

Template: HTML template in resources/templates/fattura-pdf.html
├─ Fattura header (numero, data, cliente)
├─ Righe (prodotto, qty, prezzo, totale)
├─ Totali (netto, iva, lordo)
└─ Note + footer

Effort: 6-8 hours
Owner: Backend Dev 1
Status: 🟥 NOT STARTED
```

#### TASK 2.2: SDI Integration (FattureController + SdiService)
```java
// Missing entirely! Need:

1. SdiService.java
   ├─ generateXml(fattura) → byte[]
   ├─ validateXml(byte[]) → boolean
   ├─ submitToSdi(byte[]) → SdiResponse
   └─ pollNotifications(sdiCode) → SdiNotifica[]

2. SdiController.java (webhook/polling endpoints)
   ├─ POST /api/v1/sdi/submit/{id}         - Manual submit
   ├─ GET  /api/v1/sdi/status/{id}         - Check status
   └─ POST /api/v1/sdi/webhook             - Webhook from SDI

3. Background Job (SdiPollingJob.java)
   ├─ Runs every 5 minutes
   ├─ Queries fatture with stato=EMESSA_SDI_INVIATA
   ├─ Polls SDI for notifiche
   ├─ Updates fattura stato + triggers email

4. Models:
   ├─ SdiNotifica.java (fattura_id, sdi_code, tipo, stato)
   └─ FatturaExtension (sdi_codice, sdi_stato, data_sdi_ricezione)

Effort: 12-15 hours (complex business logic)
Owner: Backend Dev 1 + Tech Lead
Status: 🟥 NOT STARTED
```

#### TASK 2.3: Email Tracking (EmailTrackingService)
```java
// Need to integrate with FattureController, PreventivoController

Features:
1. Generate unique pixel ID per email
2. Inject tracking pixel: <img src="/api/v1/track/{uuid}" />
3. Watermark links: /api/v1/track/{uuid}/download?url=...
4. Track opens/downloads in email_tracking table
5. Dashboard with metrics

Implementation:
1. EmailTrackingService.java
2. TrackingController.java endpoints
3. Background cleanup job
4. Dashboard query updates

Effort: 8-10 hours
Owner: Backend Dev 2
Status: 🟥 NOT STARTED
```

#### TASK 2.4: Input Validation Framework
```java
// Currently: Minimal validation
// Need: @Valid + custom validators

Implementation:
1. Create ValidationConfig + custom annotations
2. Add @Valid to all @RequestBody parameters
3. Custom validators:
   ├─ @ValidPartitaIva
   ├─ @ValidEmail
   ├─ @ValidItalianPhoneNumber
   ├─ @ValidBankAccount (for IBAN)
   └─ @ValidAmount (no negative/too large)

4. GlobalExceptionHandler
   ├─ Handle MethodArgumentNotValidException
   ├─ Return standardized error response
   └─ Log validation failures

Effort: 4-6 hours
Owner: QA/Backend Dev
Status: 🟥 NOT STARTED
```

---

### PHASE 3: TEST SUITE (2-3 giorni, ~20 ore)

#### TASK 3.1: Unit Tests for Controllers
```java
// Create: src/test/java/.../api/controller/*ControllerTest.java

Per controller (6 test files):
├─ testList()           - verify pagination
├─ testGetById()        - verify detail + 404
├─ testCreate()         - verify save + 201 status
├─ testUpdate()         - verify update + 200
├─ testDelete()         - verify delete + 204
├─ testSearch()         - verify search logic
└─ testInvalidInput()   - verify validation errors

Dependencies:
├─ JUnit 5
├─ Mockito
├─ MockMvc
└─ TestRestTemplate

Effort: 10-12 hours (6 controllers × 2 hours)
Owner: QA/Backend
Status: 🟥 NOT STARTED
```

#### TASK 3.2: Integration Tests (End-to-end)
```java
// Test complete workflows:

1. Preventivo → Fattura → Pagamento
2. Fattura → SDI → Notifiche
3. Ordine → Consegna → Fattura
4. Email tracking workflow

Effort: 6-8 hours
Owner: QA
Status: 🟥 NOT STARTED
```

---

### PHASE 4: DATABASE & SCHEMA (1 giorno, ~6 ore)

#### TASK 4.1: Verify/Create Entity Models
```java
// Entities needed (JPA):

✅ Cliente.java              (exists)
❌ Preventivo.java          (need create)
❌ RigaPreventivo.java      (need create)
✅ Fattura.java             (exists - verify fields)
❌ Prodotto.java            (need create)
❌ Pagamento.java           (need create)
❌ DDT.java                 (need create)
❌ RigaDDT.java             (need create)
❌ SdiNotifica.java         (need create)
❌ EmailTracking.java       (need create)
❌ Ordine.java              (exists - verify)

Effort: 4-6 hours
Owner: Backend Dev 1
Status: 🟥 NOT STARTED
```

#### TASK 4.2: Create DB Migration Scripts
```sql
-- For each missing entity:
-- 1. Create table
-- 2. Add indexes
-- 3. Add foreign keys
-- 4. Create backup

Files needed:
├─ V001_init_schema.sql           (existing - verify)
├─ V002_add_preventivi.sql        (new)
├─ V003_add_prodotti.sql          (new)
├─ V004_add_pagamenti.sql         (new)
├─ V005_add_ddt.sql               (new)
├─ V006_add_sdi_tracking.sql      (new)
├─ V007_add_email_tracking.sql    (new)
└─ V008_indexes_performance.sql   (new)

Effort: 2-3 hours
Owner: DevOps
Status: 🟥 NOT STARTED
```

---

## 📈 EFFORT ESTIMATION BY ROLE

```
Role              Hours   Weeks   Cost (€80/h)
────────────────────────────────────────────
Backend Dev 1     35-40   1.0    €2,800 - €3,200
Backend Dev 2     15-20   0.5    €1,200 - €1,600
QA Engineer       12-15   0.5    €960 - €1,200
DevOps            4-6     0.2    €320 - €480
Tech Lead         8-10    0.3    €640 - €800
────────────────────────────────────────────
TOTAL             74-91   2.3    €6,920 - €8,280
```

---

## 🎯 SEQUENCING & DEPENDENCIES

```
Week 1 (May 1-7)
├─ Task 1.1: PreventiviController         (6-7 h) → Day 1-2
├─ Task 1.2: ProdottiController          (4-5 h) → Day 2
├─ Task 1.3: PagamentiController         (4-5 h) → Day 3
├─ Task 2.1: PDF Generation              (6-8 h) → Day 3-4
└─ Task 2.4: Input Validation            (4-6 h) → Day 5

Week 2 (May 8-14)
├─ Task 1.4: DDTController               (5-6 h) → Day 1-2
├─ Task 2.2: SDI Integration             (12-15 h) → Day 2-5
├─ Task 2.3: Email Tracking              (8-10 h) → Day 5-5
└─ Task 3.1: Unit Tests                  (10-12 h) → Day 6-7

Week 3 (May 15-21)
├─ Task 3.2: Integration Tests           (6-8 h) → Day 1-2
├─ Task 4.1: Entity Models               (4-6 h) → Day 2-3
├─ Task 4.2: DB Migrations               (2-3 h) → Day 3
└─ QA & Bug Fixes                        (8-10 h) → Day 4-7
```

---

## ✅ DEFINITION OF DONE - PER MODULO

### ✅ DONE quando:

**Controller-level**
- [ ] All REST endpoints implemented (GET, POST, PUT, DELETE)
- [ ] DTO created with proper fields
- [ ] Input validation with @Valid + custom validators
- [ ] Error handling (404, 400, 500)
- [ ] Unit tests (80%+ coverage)
- [ ] Integration test (happy path + edge cases)

**Database-level**
- [ ] JPA Entity created with annotations
- [ ] Foreign keys defined
- [ ] Indexes created for frequently queried fields
- [ ] Migration script created

**Feature-level (if applies)**
- [ ] PDF generation tested
- [ ] SDI integration tested with staging endpoint
- [ ] Email tracking pixel working
- [ ] Background jobs running

---

## 🚀 QUICK START - TODAY (30 Aprile)

**Task**: Start TASK 1.1 (PreventiviController) NOW

```bash
# 1. Create skeleton
touch /workspaces/Luna2/luna2-api/src/main/java/it/zensoftware/luna2/api/controller/PreventiviController.java

# 2. Create entity
touch /workspaces/Luna2/luna2-api/src/main/java/it/zensoftware/luna2/model/Preventivo.java

# 3. Copy structure from FattureController
# (Detailed implementation below)
```

---

**Status**: 🟥 READY TO START  
**Next**: Assign task owners + begin implementation  
**Timeline**: 4-5 weeks to completion  
