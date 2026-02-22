# 🚀 LUNA2 - ROADMAP IMPLEMENTAZIONE COMPLETA

## 📋 PIANO DI IMPLEMENTAZIONE

### TIMELINE TOTALE: 3 mesi (1 dev, 20h/week, sequenziale)

```
FASE 1: CRM/LEAD (Settimane 1-4)      [80 ore]
├─ Lead management (CRUD)
├─ Pipeline tracking (5 stadi)
├─ Activity log (call, email, note)
├─ Follow-up reminders
└─ UI dashboard + list

FASE 2: MAGAZZINO/WMS (Settimane 5-10) [120 ore]
├─ Multi-warehouse setup
├─ Giacenze management
├─ Movimenti in/out
├─ Barcode scanning
├─ Picking list / picking interface
├─ PO generation (from orders)
└─ Inventory reports + alerts

FASE 3: REPORTING BI (Settimane 11-12)  [40 ore]
├─ Dashboard personalizzato
├─ Report builder (drag-drop)
├─ Charts & analytics
├─ Export PDF/Excel
└─ Scheduled reports (email)

FASE 4: AI Module (OPTIONAL, Settimane 13-14) [40 ore]
├─ Lead scoring (machine learning)
├─ Sales forecast (time series)
├─ Product recommendation
└─ Churn prediction

TOTAL: ~280 ore = 14 settimane @ 20h/week
```

---

## 🎯 FASE 1: CRM/LEAD PIPELINE (Settimane 1-4)

### Entità da Creare

```
┌─────────────┐        ┌──────────────┐
│   LEAD      │╲       │ PIPELINE_STAGE │
│             │ ╲──────│              │
│ id (PK)     │        │ id (PK)      │
│ nome        │        │ nome         │
│ email       │        │ sequenza     │
│ telefono    │        │ colore       │
│ azienda     │        │ descrizione  │
│ progetto    │        │ is_final_stage│
│ importo_stim│        └──────────────┘
│ probabilita │
│ data_creazione
│ data_chiusura│        ┌──────────────┐
│ stage_id (FK)├────────│ STORIA_LEAD  │
│ cliente_id  │        │              │
│             │        │ id (PK)      │
└─────────────┘        │ lead_id (FK) │
                       │ stage_da     │
                       │ stage_a      │
                       │ data_cambio  │
                       │ motivo       │
                       │ user_id      │
                       └──────────────┘

       ┌──────────────┐
       │  ACTIVITY    │
       │              │
       │ id (PK)      │
       │ lead_id (FK) │
       │ tipo         │ (CALL, EMAIL, NOTE, MEETING)
       │ titolo       │
       │ descrizione  │
       │ data         │
       │ utente_id    │
       │ prossima_att │ (date for follow-up)
       │ completata   │ (boolean)
       └──────────────┘

       ┌──────────────┐        ┌──────────────┐
       │  TASK        │        │  REMINDER    │
       │              │        │              │
       │ id (PK)      │╲───────│ id (PK)      │
       │ lead_id (FK) │        │ task_id (FK) │
       │ descrizione  │        │ datetime     │
       │ data_scad    │        │ tipo_notifica│
       │ priorita     │        │ stato        │
       │ assegnato_a  │        │ letto        │
       │ completata   │        └──────────────┘
       │ completata_il│
       └──────────────┘
```

### Files da Creare

**Entità**:
- `src/main/java/it/zensoftware/luna2/model/Lead.java`
- `src/main/java/it/zensoftware/luna2/model/PipelineStage.java`
- `src/main/java/it/zensoftware/luna2/model/StoriaLead.java`
- `src/main/java/it/zensoftware/luna2/model/Activity.java` (CALL, EMAIL, NOTE, MEETING)
- `src/main/java/it/zensoftware/luna2/model/Task.java`
- `src/main/java/it/zensoftware/luna2/model/Reminder.java`

**DAO**:
- `src/main/java/it/zensoftware/luna2/dao/LeadDAO.java`
- `src/main/java/it/zensoftware/luna2/dao/PipelineStageDAO.java`
- `src/main/java/it/zensoftware/luna2/dao/ActivityDAO.java`
- `src/main/java/it/zensoftware/luna2/dao/TaskDAO.java`

**Services**:
- `src/main/java/it/zensoftware/luna2/service/LeadService.java`
- `src/main/java/it/zensoftware/luna2/service/ActivityService.java`

**Struts2 Actions**:
- `src/main/java/it/zensoftware/luna2/struts/action/LeadAction.java` (10 metodi)
- `src/main/java/it/zensoftware/luna2/struts/action/ActivityAction.java` (5 metodi)

**JSP Views**:
- `src/main/webapp/jsp/crm/lead/list.jsp`
- `src/main/webapp/jsp/crm/lead/view.jsp`
- `src/main/webapp/jsp/crm/lead/edit.jsp`
- `src/main/webapp/jsp/crm/lead/pipeline.jsp` (Kanban board)
- `src/main/webapp/jsp/crm/activity/log.jsp`

### Funzionalità CRM Fase 1

**LeadAction** metodi:
1. `list()` - Elenco lead con filtri (stage, probabilita, data creazione)
2. `view()` - Dettagli lead + activity log
3. `create()` - Form nuovo lead
4. `save()` - Salva lead (insert/update)
5. `changeStage()` - Move lead tra stage (drag-drop pipeline)
6. `addActivity()` - Aggiungi call/email/note
7. `addTask()` - Crea task follow-up
8. `pipeline()` - Kanban board view
9. `delete()` - Elimina lead
10. `export()` - Export lead list (CSV)

**ActivityAction** metodi:
1. `listByLead()` - Attività per lead
2. `createActivity()` - Nueva activity
3. `completeActivity()` - Mark as done
4. `updateDueDate()` - Cambia data follow-up
5. `deleteActivity()` - Remove activity

**Workflow Stati CRM**:
```
LEAD STAGES (pipeline):
1. BOZZA (nuovo lead, non qualificato)
2. QUALIFICATO (contatto effettuato, interesse confermato)
3. PROPOSTA (preventivo/offerta inviato)
4. NEGOZIAZIONE (in trattativa)
5. VINTO (diventato cliente, fattura emessa)
6. PERSO (deal lost, motivo registrato)

ACTIVITY TYPES:
- CALL (telefonata)
- EMAIL (corrispondenza)
- MEETING (riunione)
- NOTE (nota interna)

REMINDER STATUS:
- PENDING (non letto)
- VISUALIZZATO (letto, non fatto)
- COMPLETATO (azione eseguita)
```

**UI Highlights**:
- Kanban board drag-drop stage
- Activity log (timeline format)
- Follow-up calendar (week/month view)
- Lead conversion rate metrics
- Deal value by stage (weighted by probability)

---

## 🏪 FASE 2: MAGAZZINO/WMS (Settimane 5-10)

### Entità da Creare

```
┌─────────────────┐
│  WAREHOUSE      │
│                 │
│ id (PK)         │
│ nome            │ (es: "Deposito Milano", "Warehouse Roma")
│ indirizzo       │
│ manager_id (FK) │ → Users
│ attivo          │
│ dataCreazione   │
└─────────────────┘

┌─────────────────┐        ┌──────────────┐
│  GIACENZE       │        │  POSIZIONI   │
│                 │        │              │
│ id (PK)         │        │ id (PK)      │
│ prodotto_id──┐  │        │ warehouse_id │
│ warehouse_id─┼──┼────────│ codice_posiz │
│ quantita     │  │        │ scaffale     │
│ quantita_min │  │        │ ripiano      │
│ quantita_max │  │        │ attivo       │
│ data_ultimo_ │  │        └──────────────┘
│   movimento  │  │
│ dataCreazione│  │
└───────────────┤─┘
                │
         ┌──────┴────────────┐
         │                   │
    ┌────▼──────────┐   ┌────▼──────────┐
    │ MOVIMENTO     │   │ BARCODE       │
    │ MAGAZZINO     │   │               │
    │               │   │ id (PK)       │
    │ id (PK)       │   │ prodotto_id   │
    │ giacenza_id   │   │ ean13         │
    │ tipo          │   │ tipo_barcode  │
    │ quantita      │   │ attivo        │
    │ motivazione   │   └───────────────┘
    │ da_warehouse  │
    │ a_warehouse   │
    │ data_movimento│
    │ utente_id     │
    │ data_creazione│
    └───────────────┘

┌──────────────────┐
│  PICKING_LIST    │
│                  │
│ id (PK)          │
│ ordine_id (FK)   │ → Ordini
│ warehouse_id     │
│ stato            │ (DRAFT, IN_PROGRESS, COMPLETATO)
│ data_creazione   │
│ data_completamento│
│ utente_id        │
└──────────────────┘

┌──────────────────┐
│  PICKING_ITEM    │
│                  │
│ id (PK)          │
│ picking_list_id  │
│ prodotto_id      │
│ quantita_richiesta│
│ quantita_prelevata│
│ stato            │
│ posizione_ean    │
└──────────────────┘
```

### Funzionalità Warehouse

**WarehouseAction** (5 metodi):
1. `listWarehouses()` - Elenco magazzini
2. `viewWarehouse()` - Dettagli warehouse
3. `createWarehouse()` - Nuovo magazzino
4. `updateGiacenze()` - Batch update quantità
5. `reportInventory()` - Inventory valuation report

**GiacenzeAction** (8 metodi):
1. `listGiacenze()` - Elenco giacenze per warehouse
2. `viewGiacenza()` - Dettagli prodotto in warehouse
3. `moveGiacenza()` - Trasferimento tra warehouse
4. `addMovimento()` - Registra movimento in/out
5. `listMovimenti()` - Storico movimenti
6. `alertStockMin()` - Prodotti sotto soglia
7. `exportGiacenze()` - Export inventario
8. `importGiacenze()` - Import bulk update

**PickingListAction** (6 metodi):
1. `createPickingList()` - Da ordine cliente
2. `listPickingLists()` - Elenco picking da fare
3. `viewPickingList()` - Dettagli picking (mobile-friendly)
4. `scanBarcode()` - Scan EAN13 (AJAX)
5. `completePickingList()` - Marca como pronto per spedizione
6. `reprintLabel()` - Ristampa etichetta barcode

**BarcodeAction** (4 metodi):
1. `generateBarcode()` - Crea EAN13 (o import da supplier)
2. `listBarcodes()` - Tutti barcode per prodotto
3. `printBarcodes()` - Stampa batch etichette (Zebra format)
4. `updateBarcode()` - Cambia/correggi

### Schema DB Warehouse

```sql
-- Warehouse (magazzini)
CREATE TABLE warehouse (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  nome VARCHAR(100) NOT NULL,
  indirizzo VARCHAR(255),
  manager_id BIGINT,
  attivo BOOLEAN DEFAULT TRUE,
  dataCreazione TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (manager_id) REFERENCES users(id)
);

-- Giacenze (inventory per warehouse/prodotto)
CREATE TABLE giacenze (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  prodotto_id BIGINT NOT NULL,
  warehouse_id BIGINT NOT NULL,
  quantita INT NOT NULL DEFAULT 0,
  quantita_min INT,
  quantita_max INT,
  data_ultimo_movimento DATETIME,
  dataCreazione TIMESTAMP,
  UNIQUE KEY (prodotto_id, warehouse_id),
  FOREIGN KEY (prodotto_id) REFERENCES prodotti(id),
  FOREIGN KEY (warehouse_id) REFERENCES warehouse(id)
);

-- Movimenti magazzino (history)
CREATE TABLE movimento_magazzino (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  giacenza_id BIGINT NOT NULL,
  tipo ENUM('ENTRATA', 'USCITA', 'TRASFERIMENTO', 'RETTIFICA', 'SCARTO'),
  quantita INT NOT NULL,
  motivazione VARCHAR(255),
  da_warehouse_id BIGINT,
  a_warehouse_id BIGINT,
  data_movimento DATETIME NOT NULL,
  utente_id BIGINT,
  riferimento_documento VARCHAR(50),  -- es: ordine_id, picking_list_id
  dataCreazione TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (giacenza_id) REFERENCES giacenze(id),
  FOREIGN KEY (da_warehouse_id) REFERENCES warehouse(id),
  FOREIGN KEY (a_warehouse_id) REFERENCES warehouse(id),
  FOREIGN KEY (utente_id) REFERENCES users(id),
  INDEX (data_movimento)
);

-- Barcode (EAN13 per tracking)
CREATE TABLE barcode (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  prodotto_id BIGINT NOT NULL,
  ean13 VARCHAR(13) UNIQUE,
  tipo_barcode ENUM('EAN13', 'CODE128', 'QR_CODE'),
  attivo BOOLEAN DEFAULT TRUE,
  dataCreazione TIMESTAMP,
  FOREIGN KEY (prodotto_id) REFERENCES prodotti(id)
);

-- Picking list (per spedizione ordini)
CREATE TABLE picking_list (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  ordine_id BIGINT NOT NULL,
  warehouse_id BIGINT NOT NULL,
  stato ENUM('DRAFT', 'IN_PROGRESS', 'COMPLETATO', 'ANNULLATO'),
  data_creazione DATETIME DEFAULT CURRENT_TIMESTAMP,
  data_completamento DATETIME,
  utente_id BIGINT,
  note TEXT,
  FOREIGN KEY (ordine_id) REFERENCES ordini(id),
  FOREIGN KEY (warehouse_id) REFERENCES warehouse(id),
  FOREIGN KEY (utente_id) REFERENCES users(id)
);

-- Picking item (righe picking list)
CREATE TABLE picking_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  picking_list_id BIGINT NOT NULL,
  prodotto_id BIGINT NOT NULL,
  quantita_richiesta INT NOT NULL,
  quantita_prelevata INT DEFAULT 0,
  stato ENUM('PENDING', 'PICKED', 'VERIFIED'),
  posizione_ean VARCHAR(50),  -- posizione warehouse (shelf/bin)
  riga_ordine_id BIGINT,
  FOREIGN KEY (picking_list_id) REFERENCES picking_list(id) ON DELETE CASCADE,
  FOREIGN KEY (prodotto_id) REFERENCES prodotti(id),
  FOREIGN KEY (riga_ordine_id) REFERENCES ordini_riga(id)
);
```

### Workflow Warehouse

```
ORDERING FLOW:
1. Ordine cliente creato (stato CONFERMATO)
   → Automatic trigger: Create PICKING_LIST
2. Warehouse team: Get picking_list (mobile interface)
3. Scan barcode prodotto → quantita prelevata
4. Quando tutto prelevato → Mark picking_list COMPLETATO
5. Trigger: Email spedizione a cliente con tracking
6. Automatic MOVIMENTO_MAGAZZINO registrato (USCITA)
7. GIACENZE aggiornate in real-time

INVENTORY RECEIPT:
1. Ricevi ordine da fornitore
2. Create MOVIMENTO_MAGAZZINO (ENTRATA)
3. Scan EAN o manual input quantita
4. GIACENZE auto-aggiornate
5. Alert se quantita_max exceeded

ALERT SYSTEM:
- Stock MIN: Email weekly report
- Stock MAX: Alert immediate se exceeds
- Slow-moving items: Report monthly (non venduti 90+ giorni)
```

---

## 📊 FASE 3: REPORTING BI (Settimane 11-12)

### Query Report Principali

**Sales Reports**:
- Revenue by month/quarter/year
- Top customers by revenue
- Top products by volume
- Sales forecast (trend analysis)

**CRM Reports**:
- Lead pipeline value (stage × probability × amount)
- Lead source analysis
- Conversion rate by stage
- Sales rep performance

**Warehouse Reports**:
- Inventory valuation (FIFO/LIFO)
- Slow-moving items
- Stock turn-over by category
- Warehouse space utilization

**Finance Reports**:
- AR aging (accounts receivable)
- AP aging (accounts payable)
- Cash flow forecast
- Margin analysis by product

---

## 🤖 FASE 4: AI MODULE (Optional, Settimane 13-14)

### Features AI

**Lead Scoring**:
- Probability model (logistic regression)
- Features: email engagement, visit frequency, deal size
- Output: Lead score 0-100

**Sales Forecast**:
- Time series (ARIMA) for revenue prediction
- Monthly/quarterly projections

**Churn Prediction**:
- Identify at-risk customers
- Recommend retention actions

---

## 📈 EFFORT ESTIMATE PER FASE

| Fase | Ore | Giorni | Settimane |
|------|------|--------|-----------|
| CRM | 80 | 16 | 4 |
| Magazzino | 120 | 24 | 6 |
| Reporting | 40 | 8 | 2 |
| AI (optional) | 40 | 8 | 2 |
| **TOTAL** | **280** | **56** | **14** |

@ 20h/week = **14 settimane** (3 mesi + 2 giorni)

---

## 🎯 INIZIO IMPLEMENTAZIONE

**Prossimo step**: Implemento Fase 1 (CRM) subito

Creerò:
1. ✅ Lead entity + DAO + Service
2. ✅ PipelineStage + StoriaLead (audit trail)
3. ✅ Activity + Task + Reminder
4. ✅ LeadAction con 10 metodi
5. ✅ JSP views (list, view, pipeline kanban)
6. ✅ struts.xml config

Tempo: ~4-6 ore per avere CRM base funzionante

Iniziamo?
