# 🔧 LUNA2 - SPECIFICHE TECNICHE DETTAGLIATE

## 📋 SOMMARIO ARCHITETTONICO

```
┌─────────────────────────────────────────────────────────────┐
│                      CLIENT BROWSER                          │
│              (Bootstrap 5 / jQuery / DataTables)             │
└──────────────────────────┬──────────────────────────────────┘
                           │ HTTPS
┌──────────────────────────▼──────────────────────────────────┐
│                     NGINX REVERSE PROXY                      │
│           (SSL/TLS, compression, rate limiting)             │
└──────────────────────────┬──────────────────────────────────┘
                           │ HTTP localhost:8080
┌──────────────────────────▼──────────────────────────────────┐
│          APACHE TOMCAT 9.0 (Java Application Server)        │
│                                                              │
│  ┌──────────────────────────────────────────────────────┐   │
│  │         LUNA2 Web Application (WAR)                  │   │
│  │  ┌────────────────────────────────────────────────┐  │   │
│  │  │   Struts2 MVC Framework + Actions              │  │   │
│  │  │  ├─ ClientiAction (CRUD, search, contatti)     │  │   │
│  │  │  ├─ ProdottiAction (catalog, import/export)    │  │   │
│  │  │  ├─ PreventiviAction (full workflow)           │  │   │
│  │  │  ├─ FattureAction (active invoices, SDI)       │  │   │
│  │  │  ├─ FatturePassiveAction (incoming invoices)   │  │   │
│  │  │  ├─ ComMesseAction (production orders)         │  │   │
│  │  │  └─ ... (20 total actions)                     │  │   │
│  │  └────────────────────────────────────────────────┘  │   │
│  │  ┌────────────────────────────────────────────────┐  │   │
│  │  │   Service Layer (Business Logic)               │  │   │
│  │  │  ├─ PreventivoService                          │  │   │
│  │  │  ├─ FattureService                             │  │   │
│  │  │  ├─ FattureExportService (Assosoftware)        │  │   │
│  │  │  ├─ EmailService (pixel tracking)              │  │   │
│  │  │  ├─ SDIService (XML generation)                │  │   │
│  │  │  └─ ... (15 total services)                    │  │   │
│  │  └────────────────────────────────────────────────┘  │   │
│  │  ┌────────────────────────────────────────────────┐  │   │
│  │  │   DAO Layer (Data Access)                      │  │   │
│  │  │  ├─ Hibernate ORM + HQL                        │  │   │
│  │  │  ├─ JNDI DataSource (connection pooling)       │  │   │
│  │  │  └─ 30+ DAO classes                            │  │   │
│  │  └────────────────────────────────────────────────┘  │   │
│  │  ┌────────────────────────────────────────────────┐  │   │
│  │  │   Background Tasks                             │  │   │
│  │  │  ├─ CronJobListener (SDI polling every 5 min)  │  │   │
│  │  │  │  ├─ Notifiche polling (SDI status)          │  │   │
│  │  │  │  └─ Fatture Passive polling (incoming)      │  │   │
│  │  │  └─ EmailService (async send)                  │  │   │
│  │  └────────────────────────────────────────────────┘  │   │
│  │  ┌────────────────────────────────────────────────┐  │   │
│  │  │   PDF Generation                               │  │   │
│  │  │  └─ iText5 (Preventivi, Fatture, Ordini)       │  │   │
│  │  └────────────────────────────────────────────────┘  │   │
│  └──────────────────────────────────────────────────────┘   │
│                                                              │
│  ┌──────────────────────────────────────────────────────┐   │
│  │   Utilities & Helpers                               │   │
│  │  ├─ EmailTrackingUtil (pixel injection)             │   │
│  │  ├─ PDFUtil (generation)                            │   │
│  │  ├─ SDIXmlUtil (XML parsing/generation)             │   │
│  │  ├─ DateFormatUtil (Italian formatting)             │   │
│  │  └─ ValidationUtil (business rules)                 │   │
│  └──────────────────────────────────────────────────────┘   │
└─────────────────────────┬──────────────────────────────────┘
                           │
        ┌──────────────────┴──────────────────┐
        │                                     │
┌───────▼────────┐               ┌───────────▼──────┐
│   MYSQL 8.0    │               │  FILE SYSTEM     │
│   DATABASE     │               │  /uploads/       │
│                │               │  (PDFs, etc)     │
│ ┌────────────┐ │               └──────────────────┘
│ │ 40+ Tables │ │                    │
│ │ Hibernate  │ │                    │ SFTP/SCP
│ │ JPA        │ │                    │
│ └────────────┘ │
│                │
│ ┌────────────┐ │
│ │ Backups    │ │
│ │ (daily)    │ │
│ └────────────┘ │
└────────────────┘

        ┌────────────────────────────────────────────┐
        │      EXTERNAL INTEGRATIONS                 │
        │                                             │
        │  ┌──────────────────────────────────────┐  │
        │  │  SDI (Sistema Interscambio)          │  │
        │  │  POST /invia-fattura (XML)           │  │
        │  │  GET /ricevi-notifiche/ (polling)    │  │
        │  │  GET /ricevi-fatture/ (polling)      │  │
        │  └──────────────────────────────────────┘  │
        │                                             │
        │  ┌──────────────────────────────────────┐  │
        │  │  Email Service                       │  │
        │  │  SMTP (Gmail, Office365, custom)     │  │
        │  └──────────────────────────────────────┘  │
        │                                             │
        │  ┌──────────────────────────────────────┐  │
        │  │  S3 / Cloud Storage (optional)       │  │
        │  │  For backup + file archival          │  │
        │  └──────────────────────────────────────┘  │
        └────────────────────────────────────────────┘
```

---

## 📊 DATABASE SCHEMA OVERVIEW

### Table Count: 40+ tables

#### CORE ANAGRAFICHE (4 tables)
```
┌─────────────┐
│  CLIENTI    │
├─────────────┼─────────────────────────────────────┐
│ id          │ Primary Key                         │
│ nome        │ Company name (UNIQUE)               │
│ piva        │ VAT ID (UNIQUE, 11 chars)           │
│ cf          │ Codice Fiscale (16 chars, opt)      │
│ indirizzo   │ Address                             │
│ città       │ City                                │
│ cap         │ Postal code                         │
│ email       │ Primary email                       │
│ telefono    │ Phone number                        │
│ iban        │ Bank account (opt)                  │
│ stato       │ ENUM (ATTIVO, INATTIVO, CANCELLATO)│
│ dataCreazione │ Timestamp (auto)                  │
│ dataModifica  │ Timestamp (auto)                  │
├─────────────┴─────────────────────────────────────┤
│ Relationships:                                     │
│ ├─ 1:N Preventivi                             │
│ ├─ 1:N Ordini                                 │
│ ├─ 1:N Fatture                                │
│ ├─ 1:N Contatti (contact persons)             │
│ └─ 1:N SdiNotifica (notifications)            │
└────────────────────────────────────────────────────┘

┌──────────────┐
│ FORNITORI    │  [Similar structure to CLIENTI]
├──────────────┤
│ Similar to CLIENTI + specificità fornitore
│ Relationships:
│ ├─ 1:N FatturePassive (incoming invoices)
│ └─ 1:N Ordini Fornitori (purchase orders)
└──────────────┘

┌──────────────┐
│ PRODOTTI     │
├──────────────┼───────────────────────────────────┐
│ id           │ Primary Key                       │
│ codice       │ SKU (UNIQUE)                      │
│ nome         │ Product name                      │
│ descrizione  │ Long description                  │
│ prezzo       │ Decimal (10,2) - EUR              │
│ iva_id       │ FK → ALIQUOTE_IVA (rates)         │
│ categoria_id │ FK → CATEGORIE (taxonomy)         │
│ giacenza     │ Inventory qty                     │
│ giacenza_min │ Min threshold (warning)           │
│ attivo       │ Boolean (soft delete)             │
│ dataCreazione│ Timestamp                         │
│ dataModifica │ Timestamp                         │
├──────────────┴────────────────────────────────────┤
│ Relationships:                                    │
│ ├─ N:1 CATEGORIE                             │
│ ├─ N:1 ALIQUOTE_IVA                          │
│ ├─ 1:N PREVENTIVI_RIGA (line items)          │
│ ├─ 1:N FATTURE_RIGA                          │
│ └─ 1:N MOVIMENTI_MAGAZZINO                   │
└────────────────────────────────────────────────────┘

┌──────────────┐
│ CONTATTI     │  [Contact persons for CLIENTI/FORNITORI]
├──────────────┼───────────────────────────────────┐
│ id           │ Primary Key                       │
│ cliente_id   │ FK → CLIENTI (or fornitore_id)    │
│ nome         │ Contact person name               │
│ cognome      │ Last name                         │
│ email        │ Email address (UNIQUE per cliente)│
│ telefono     │ Phone number                      │
│ ruolo        │ Role (Responsabile, ...)          │
│ predefinito  │ Boolean (primary contact)         │
│ dataCreazione│ Timestamp                         │
│ dataModifica │ Timestamp                         │
└────────────────────────────────────────────────────┘
```

#### PREVENTIVI & ORDINI (6 tables)
```
┌──────────────┐
│ PREVENTIVI   │
├──────────────┼───────────────────────────────────┐
│ id           │ Primary Key (auto-increment)      │
│ numero       │ Sequential number (per year)      │
│ anno         │ Year (for multi-year unique)      │
│ data         │ Date issued                       │
│ cliente_id   │ FK → CLIENTI                      │
│ importo_netto│ Decimal (sum of lines - discounts)│
│ importo_iva  │ Decimal (calculated)              │
│ importo_totale│ Decimal (netto + iva)            │
│ sconto       │ Decimal (% or €)                  │
│ stato        │ ENUM (BOZZA, INVIATO, ACCETTATO, │
│              │        RIFIUTATO, SCADUTO)        │
│ validita_giorni│ Int (used for scadenza calc)    │
│ motivazione  │ Text (if rifiutato)               │
│ note         │ Internal notes                    │
│ dataCreazione│ Timestamp (auto)                  │
│ dataModifica │ Timestamp (auto)                  │
│ dataInvio    │ Timestamp (when INVIATO)          │
├──────────────┴────────────────────────────────────┤
│ Relationships:                                    │
│ ├─ N:1 CLIENTI (required)                     │
│ ├─ 1:N PREVENTIVI_RIGA (line items)           │
│ ├─ 1:N COMMESSE (if modulo enabled)           │
│ ├─ 1:N EMAIL_TRACKING (open/click tracking)   │
│ └─ 1:N PDF_CACHE (generated PDFs)             │
└────────────────────────────────────────────────────┘

┌──────────────┐
│ PREVENTIVI_RIGA │
├──────────────┼───────────────────────────────────┐
│ id           │ Primary Key                       │
│ preventivo_id│ FK → PREVENTIVI (required)        │
│ prodotto_id  │ FK → PRODOTTI (optional - free text)│
│ descrizione  │ Line description (if no prodotto) │
│ quantita     │ Decimal (2 decimals)              │
│ prezzo_unitario│ Decimal (unit price EUR)        │
│ sconto_perc  │ Decimal (% discount)              │
│ iva_perc     │ Decimal (tax %, from ALIQUOTE)    │
│ importo_netto│ Calculated (quantita*prezzo*(1-sconto))│
│ importo_iva  │ Calculated (netto*iva%)           │
│ importo_totale│ Calculated (netto+iva)           │
│ ordine_riga  │ Int (display order)               │
├──────────────┴────────────────────────────────────┤
│ Relationships:                                    │
│ ├─ N:1 PREVENTIVI (required, cascade delete) │
│ └─ N:1 PRODOTTI (optional)                    │
└────────────────────────────────────────────────────┘

┌──────────────┐
│ ORDINI       │  [Similar to PREVENTIVI but for sales orders]
├──────────────┤
│ Structure: Similar to PREVENTIVI
│ Key differences:
│  - stato: (BOZZA, CONFERMATO, EVASO, ANNULLATO)
│  - No scadenza (validità date)
│  - Link to FATTURE (conversion)
│ Relationships:
│  ├─ 1:N ORDINI_RIGA
│  ├─ N:1 CLIENTI
│  └─ 1:1 FATTURE (optional, if fatturato)
└────────────────────────────────────────────────────┘

┌──────────────┐
│ ORDINI_RIGA  │  [Similar to PREVENTIVI_RIGA]
└──────────────┘
```

#### FATTURE (9 tables)
```
┌──────────────┐
│ FATTURE      │  [ Active Invoices - Issued by Company ]
├──────────────┼───────────────────────────────────┐
│ id           │ Primary Key                       │
│ numero       │ Progressive per anno (UNIQUE)     │
│ anno         │ Year (for multi-year unique)      │
│ tipo_fattura │ ENUM (PROFORMA, REALE)            │
│ data_emissione│ Date issued                      │
│ data_scadenza│ Due date (auto-calc from terms)   │
│ cliente_id   │ FK → CLIENTI (required)           │
│ preventivo_id│ FK → PREVENTIVI (optional, link)  │
│ commessa_id  │ FK → COMMESSE (optional, if prod) │
│ importo_netto│ Decimal (sum lines)               │
│ importo_iva  │ Decimal (sum IVA)                 │
│ importo_totale│ Decimal (netto + iva)            │
│ stato        │ ENUM (BOZZA, EMESSA, PAGATA,    │
│              │        PARZIALMENTE_PAGATA, etc) │
│ sdi_codice   │ String (returned by SDI, unique)  │
│ sdi_stato    │ ENUM (INVIATA, ACCETTATA,        │
│              │        SCARTATA, ERRORE)         │
│ xml_fattura  │ LongText (stored for audit)       │
│ note         │ Internal notes                    │
│ dataCreazione│ Timestamp (auto)                  │
│ dataModifica │ Timestamp (auto)                  │
│ dataInvioSdi │ Timestamp (when sent to SDI)      │
├──────────────┴────────────────────────────────────┤
│ Relationships:                                    │
│ ├─ N:1 CLIENTI (required)                     │
│ ├─ N:1 PREVENTIVI (optional)                  │
│ ├─ N:1 COMMESSE (optional)                    │
│ ├─ 1:N FATTURE_RIGA (line items)              │
│ ├─ 1:N PAGAMENTI (payment tracking)           │
│ ├─ 1:N EMAIL_TRACKING (send log)              │
│ ├─ 1:N SDI_NOTIFICA (SDI responses)           │
│ └─ 1:N PDF_CACHE (generated PDFs)             │
└────────────────────────────────────────────────────┘

┌──────────────┐
│ FATTURE_RIGA │  [ Invoice line items ]
├──────────────┼───────────────────────────────────┐
│ id           │ Primary Key                       │
│ fattura_id   │ FK → FATTURE (required)           │
│ prodotto_id  │ FK → PRODOTTI (optional)          │
│ descrizione  │ Line description                  │
│ quantita     │ Decimal (qty)                     │
│ prezzo_unitario│ Decimal (unit price)            │
│ sconto_perc  │ Decimal (% discount)              │
│ iva_perc     │ Decimal (tax %)                   │
│ importo_netto│ Calculated                        │
│ importo_iva  │ Calculated                        │
│ importo_totale│ Calculated                       │
│ ordine_riga  │ Display order                     │
├──────────────┴────────────────────────────────────┤
│ Relationships:                                    │
│ ├─ N:1 FATTURE (required, cascade delete)    │
│ └─ N:1 PRODOTTI (optional)                    │
└────────────────────────────────────────────────────┘

┌──────────────┐
│ PAGAMENTI    │  [ Payment tracking ]
├──────────────┼───────────────────────────────────┐
│ id           │ Primary Key                       │
│ fattura_id   │ FK → FATTURE (required)           │
│ importo      │ Decimal (payment amount)          │
│ data_pagamento│ Date of payment                  │
│ tipo         │ ENUM (BONIFICO, CARTA, CONTANTI, │
│              │        ASSEGNO, ...)              │
│ riferimento  │ String (reference num)            │
│ note         │ Text notes                        │
│ dataCreazione│ Timestamp                         │
├──────────────┴────────────────────────────────────┤
│ Relationships:                                    │
│ └─ N:1 FATTURE (required, cascade delete)    │
└────────────────────────────────────────────────────┘

┌──────────────┐
│ FATTURE_PASSIVE │  [ Incoming invoices from suppliers ]
├──────────────┼───────────────────────────────────┐
│ id           │ Primary Key                       │
│ numero       │ Supplier's invoice number         │
│ anno         │ Year                              │
│ fornitore_id │ FK → FORNITORI (optional, fallback name)|
│ fornitore_nome│ String (if fornitore_id null)    │
│ fornitore_piva│ String (if fornitore_id null)    │
│ data_fattura │ Invoice date (from supplier)      │
│ data_ricezione│ Receipt date (polling time)       │
│ data_scadenza│ Due date                          │
│ importo_netto│ Decimal (from XML parsing)        │
│ importo_iva  │ Decimal (from XML)                │
│ importo_totale│ Decimal (from XML)               │
│ stato_pagamento│ ENUM (DA_PAGARE, PARZIALMENTE, │
│              │       PAGATA, SCADUTA)            │
│ sdi_messaggio_id│ String (SDI message ID)        │
│ xml_invoice  │ LongText (original XML stored)    │
│ dataCreazione│ Timestamp                         │
│ dataModifica │ Timestamp                         │
├──────────────┴────────────────────────────────────┤
│ Relationships:                                    │
│ ├─ N:1 FORNITORI (optional, lookup)          │
│ ├─ 1:N PAGAMENTI_PASSIVE (optional)           │
│ └─ 1:N EMAIL_TRACKING (optional, notify)      │
└────────────────────────────────────────────────────┘

┌──────────────┐
│ SDI_NOTIFICA │  [ SDI response tracking ]
├──────────────┼───────────────────────────────────┐
│ id           │ Primary Key                       │
│ fattura_id   │ FK → FATTURE (optional, if match) │
│ sdi_codice   │ String (fattura's SDI code)       │
│ tipo         │ String (notification type)        │
│ stato        │ ENUM (ACCETTATA, SCARTATA, etc)   │
│ descrizione  │ Text (error message if exists)    │
│ xml_response │ LongText (stored SDI XML)         │
│ data_notifica│ Timestamp (from SDI)              │
│ dataRicezione│ Timestamp (when polled)           │
│ letto        │ Boolean (user acknowledged)       │
├──────────────┴────────────────────────────────────┤
│ Relationships:                                    │
│ └─ N:1 FATTURE (optional)                    │
└────────────────────────────────────────────────────┘

┌──────────────┐
│ EMAIL_TRACKING │ [ Open & Click tracking ]
├──────────────┼───────────────────────────────────┐
│ id           │ Primary Key                       │
│ tipo_doc     │ ENUM (PREVENTIVO, FATTURA)        │
│ documento_id │ ID (preventivo_id or fattura_id)  │
│ email        │ Recipient email                   │
│ data_invio   │ Send timestamp                    │
│ data_primo_open│ First open timestamp (nullable) │
│ num_aperture │ Open count                        │
│ data_primo_click│ First click timestamp          │
│ num_click    │ Click count                       │
│ tracking_id  │ Unique ID (for pixel/link)        │
│ ip_address   │ (from open) optional geoip        │
│ user_agent   │ Browser info (optional)           │
│ note         │ Status/remarks                    │
├──────────────┴────────────────────────────────────┤
│ Relationships:                                    │
│ ├─ Reference PREVENTIVI (via documento_id)   │
│ └─ Reference FATTURE (via documento_id)      │
└────────────────────────────────────────────────────┘

┌──────────────┐
│ PDF_CACHE    │  [ Generated PDF files ]
├──────────────┼───────────────────────────────────┐
│ id           │ Primary Key                       │
│ tipo_doc     │ ENUM (PREVENTIVO, FATTURA, ORDINE)│
│ documento_id │ References documento_id           │
│ layout       │ ENUM (TECNICO, NARRATIVO, etc)    │
│ file_path    │ String (server path or S3 URL)    │
│ file_size    │ Long (bytes)                      │
│ data_generazione│ Timestamp                      │
│ validita_fino│ Timestamp (cache expiry)          │
│ hash_md5     │ (for duplicate detection)         │
├──────────────┴────────────────────────────────────┤
│ Purpose: Cache PDFs to avoid re-generation      │
│ Typical TTL: 30 days or sync update             │
└────────────────────────────────────────────────────┘
```

#### COMMESSE (3 tables)
```
┌──────────────┐
│ COMMESSE     │  [ Production Orders / Projects ]
├──────────────┼───────────────────────────────────┐
│ id           │ Primary Key (auto-increment)      │
│ numero       │ Sequential per year               │
│ anno         │ Year                              │
│ preventivo_id│ FK → PREVENTIVI (required link)   │
│ cliente_id   │ FK → CLIENTI (FK from preventivo) │
│ descrizione  │ Project description               │
│ data_inizio  │ Start date                        │
│ data_fine_prevista│ Estimated finish             │
│ data_fine_effettiva│ Actual finish (when COMPLETATA)│
│ percentuale_completamento│ Int (0-100%)          │
│ stato        │ ENUM (APERTA, IN_LAVORAZIONE,   │
│              │        SOSPESA, COMPLETATA,      │
│              │        CHIUSA, ANNULLATA)        │
│ fattura_id   │ FK → FATTURE (if fatturata)       │
│ note         │ Internal notes                    │
│ dataCreazione│ Timestamp                         │
│ dataModifica │ Timestamp                         │
├──────────────┴────────────────────────────────────┤
│ Relationships:                                    │
│ ├─ N:1 PREVENTIVI (required)                  │
│ ├─ N:1 CLIENTI (from preventivo)              │
│ ├─ 1:N COMMESSE_RIGA (line items)             │
│ ├─ 1:N COMMESSE_STORICO (state audit trail)   │
│ └─ N:1 FATTURE (optional, after completion)   │
└────────────────────────────────────────────────────┘

┌──────────────┐
│ COMMESSE_RIGA │  [ Line items from preventivo ]
├──────────────┼───────────────────────────────────┐
│ id           │ Primary Key                       │
│ commessa_id  │ FK → COMMESSE (required)          │
│ preventivo_riga_id│ FK → PREVENTIVI_RIGA (link)  │
│ prodotto_id  │ FK → PRODOTTI (optional)          │
│ descrizione  │ Item description                  │
│ quantita     │ Decimal (qty)                     │
│ quantita_completata│ Decimal (completed qty)     │
│ prezzo_unitario│ Decimal (price)                 │
│ importo_totale│ Calculated                       │
│ ordine_riga  │ Display order                     │
├──────────────┴────────────────────────────────────┤
│ Relationships:                                    │
│ ├─ N:1 COMMESSE (required, cascade)           │
│ ├─ N:1 PREVENTIVI_RIGA (optional)             │
│ └─ N:1 PRODOTTI (optional)                    │
└────────────────────────────────────────────────────┘

┌──────────────┐
│ COMMESSE_STORICO │  [ State change audit trail ]
├──────────────┼───────────────────────────────────┐
│ id           │ Primary Key                       │
│ commessa_id  │ FK → COMMESSE (required)          │
│ stato_da     │ Previous state                    │
│ stato_a      │ New state                         │
│ motivo       │ Reason for change                 │
│ utente_id    │ User who made change              │
│ data_cambio  │ Timestamp of change               │
│ note         │ Optional notes                    │
├──────────────┴────────────────────────────────────┤
│ Relationships:                                    │
│ └─ N:1 COMMESSE (required, cascade delete)    │
└────────────────────────────────────────────────────┘
```

#### SUPPORT TABLES (4+ tables)
```
┌──────────────┐
│ USERS        │  [ Application users ]
├──────────────┼───────────────────────────────────┐
│ id           │ Primary Key                       │
│ username     │ Unique login ID                   │
│ email        │ Contact email (UNIQUE)            │
│ password_hash│ BCrypt hashed password            │
│ full_name    │ Display name                      │
│ ruolo        │ ENUM (ADMIN, USER, SALES, etc)    │
│ attivo       │ Boolean (enabled/disabled)        │
│ ultima_login │ Timestamp (last successful login) │
│ dataCreazione│ Timestamp                         │
│ dataModifica │ Timestamp                         │
├──────────────┴────────────────────────────────────┤
│ Purpose: Authentication & authorization         │
│ Security: Passwords NOT stored in plain text    │
└────────────────────────────────────────────────────┘

┌──────────────┐
│ ALIQUOTE_IVA │  [ Tax rates master data ]
├──────────────┼───────────────────────────────────┐
│ id           │ Primary Key                       │
│ descrizione  │ Tax description (eg "IVA 22%")    │
│ percentuale  │ Decimal (22.00)                   │
│ codice       │ Tax code                          │
│ attiva       │ Boolean (currently used)          │
├──────────────┴───────────────────────────────────┤
│ Standard IVA rates in Italy:                    │
│ - 22% (Standard)                                │
│ - 10% (Reduced - food, services)                │
│ - 5% (Super-reduced - medicines, books)         │
│ - 4% (Very reduced - medicine, aids)            │
│ - 0% / Esente (Exempt - some services)          │
└────────────────────────────────────────────────────┘

┌──────────────┐
│ CATEGORIE    │  [ Product categories ]
├──────────────┤
│ id, nome, descrizione, attiva, dataCreazione
└──────────────┘

┌──────────────┐
│ MODULE_SETTINGS │  [ Feature toggles ]
├──────────────┼───────────────────────────────────┐
│ id           │ Primary Key                       │
│ nome_modulo  │ ENUM (CORE, MAGAZZINO, PRODUZIONE,│
│              │        CRM, REPORT, AI, ...)      │
│ abilitato    │ Boolean (enabled or disabled)     │
│ descrizione  │ Text (human readable)             │
├──────────────┴───────────────────────────────────┤
│ Current state:                                   │
│ - CORE: ENABLED (always)                       │
│ - MAGAZZINO: DISABLED (not implemented)        │
│ - PRODUZIONE: DISABLED (working but optional)  │
│ - CRM: DISABLED (partial implementation)       │
│ - AI: DISABLED (not implemented)                │
│ - REPORT: ENABLED (basic reports)              │
└────────────────────────────────────────────────────┘
```

---

## 🔌 API ENDPOINTS (Struts2 Actions)

### Clienti Management
```
GET  /clienti.action              → List all clienti with search/filter
GET  /clienti!view.action?id=X    → View single cliente details
GET  /clienti!create.action       → Show create form
POST /clienti!save.action         → Save new cliente
GET  /clienti!edit.action?id=X    → Show edit form
POST /clienti!update.action       → Update existing cliente
POST /clienti!delete.action       → Delete cliente (with confirmation)
GET  /clienti!search.action       → API search (JSON response)
```

### Preventivi Workflow
```
GET  /preventivi.action                      → List
GET  /preventivi!view.action?id=X            → View
GET  /preventivi!create.action               → Create form
POST /preventivi!save.action                 → Save
POST /preventivi!sendEmail.action            → Send via email + tracking
GET  /preventivi!pdf.action?id=X&layout=...  → Generate PDF
POST /preventivi!trasformInFattura.action    → Convert to invoice
POST /preventivi!accetta.action              → Mark as accepted
```

### Fatture Workflow
```
GET  /fatture.action                         → List active invoices
GET  /fatture!view.action?id=X               → View details
GET  /fatture!create.action                  → Create form
POST /fatture!save.action                    → Save
POST /fatture!sendToSdi.action               → Submit to SDI (XML generation)
GET  /fatture!checkSdiStatus.action          → Poll SDI for status
GET  /fatture!pdf.action?id=X                → Generate PDF
POST /fatture!registraPagamento.action       → Register payment
GET  /fatture!export.action?anno=2024        → Export Assosoftware format
```

### Email Tracking
```
GET  /pixel.gif?tracking_id=XXXXX → Invisible pixel (serves 1x1 GIF)
                                     Updates num_aperture in EMAIL_TRACKING
POST /email/webhook.action        → Email service callbacks (bounce handling)
GET  /email!dashboard.action      → View email metrics
```

### SDI Operations (Background - CronJobListener)
```
Timer-based (every 5 min):
1. Query all FATTURE with stato=EMESSA_SDI_INVIATA
2. For each: GET /ricevi-notifiche/?codice=XXXXX
3. Parse XML response, update FATTURE.sdi_stato and create SdiNotifica
4. If ACCETTATA: trigger email send to customer

Timer-based (every 5 min):
1. Query FORNITORI, extract PIVA
2. For each: GET /ricevi-fatture/?piva=XXXXX
3. Parse XML, create FatturaPassiva entries
4. Notify user of new invoices
```

---

## ⚙️ CONFIGURATION PROPERTIES

### application.properties
```properties
# Server
server.port=8080
server.servlet.context-path=/
server.servlet.session.timeout=3600

# Database
spring.datasource.url=jdbc:mysql://localhost:3306/luna2?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Europe/Rome
spring.datasource.username=luna2app
spring.datasource.password=PASSWORD_HERE
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.jpa.hibernate.ddl-auto=validate

# Email
mail.smtp.host=smtp.gmail.com
mail.smtp.port=587
mail.smtp.user=noreply@yourdomain.com
mail.smtp.password=APP_PASSWORD
mail.smtp.auth=true
mail.smtp.starttls=true
mail.smtp.starttls.required=true
mail.from.address=noreply@yourdomain.com
mail.from.name=Luna2 ERP

# SDI Integration
sdi.submit.url=https://api.luna.itsolutions-cloud.com/invia-fattura
sdi.submit.timeout.connect=15000
sdi.submit.timeout.read=30000

sdi.notify.url=https://api.luna.itsolutions-cloud.com/ricevi-notifiche/
sdi.notify.polling.interval=300000  # 5 minutes

sdi.passive.url=https://api.luna.itsolutions-cloud.com/ricevi-fatture/
sdi.passive.polling.interval=300000  # 5 minutes

# PDF Generation
pdf.temp.dir=/tmp/luna2-pdf/
pdf.font.encoding=UTF-8

# File Upload
upload.dir=/var/uploads/luna2/
upload.max.size=10485760  # 10MB

# Logging
logging.level.root=INFO
logging.level.it.solutions=DEBUG
logging.level.org.hibernate=WARN
logging.file.name=/var/logs/luna2/application.log
logging.file.max-size=10MB
logging.file.max-history=30

# Locale & Formatting (Italian)
spring.messages.basename=messages
spring.messages.encoding=UTF-8
java.util.Locale=it_IT
```

---

## 🔒 SECURITY CONFIGURATION

### Struts2 Interceptor Stack
```xml
<interceptor-stack name="defaultStack">
    <interceptor-ref name="exception"/>
    <interceptor-ref name="alias"/>
    <interceptor-ref name="prepare"/>
    <interceptor-ref name="i18n"/>
    <interceptor-ref name="debugging"/>
    <interceptor-ref name="scoping"/>
    <interceptor-ref name="servletConfig"/>
    <interceptor-ref name="params"/>
    <interceptor-ref name="conversionError"/>
    <interceptor-ref name="validation">
        <param name="excludeMethods">input,back,cancel,browse</param>
    </interceptor-ref>
    <interceptor-ref name="workflow">
        <param name="excludeMethods">input,back,cancel,browse</param>
    </interceptor-ref>
    <!-- ADD: <interceptor-ref name="tokens"/> -->
</interceptor-stack>
```

### Authentication Filter
```java
// All actions inherit from BaseAction which checks for:
// 1. Session exists AND
// 2. User object in session AND
// 3. User NOT in INACTIVE state
// → Otherwise redirect to /login.action
```

---

## 📈 PERFORMANCE METRICS & TUNING

### Database Connection Pool
```properties
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=10000
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1800000
```

### Tomcat Threading
```xml
<!-- In catalina.properties: -->
maxConnections=1000
maxThreads=200
minSpareThreads=25
```

### Key Indexes (to add in production)
```sql
-- Preventivi
ALTER TABLE preventivi ADD INDEX idx_cliente_id (cliente_id);
ALTER TABLE preventivi ADD INDEX idx_stato (stato);
ALTER TABLE preventivi ADD INDEX idx_anno (anno);
ALTER TABLE preventivi ADD UNIQUE INDEX idx_numero_anno (numero, anno);

-- Fatture
ALTER TABLE fatture ADD INDEX idx_cliente_id (cliente_id);
ALTER TABLE fatture ADD INDEX idx_sdi_codice (sdi_codice);
ALTER TABLE fatture ADD INDEX idx_sdi_stato (sdi_stato);
ALTER TABLE fatture ADD UNIQUE INDEX idx_numero_anno (numero, anno);

-- Email Tracking
ALTER TABLE email_tracking ADD INDEX idx_tracking_id (tracking_id);
ALTER TABLE email_tracking ADD INDEX idx_documento_id (documento_id, tipo_doc);

-- Fatture Passive
ALTER TABLE fatture_passive ADD INDEX idx_fornitore_id (fornitore_id);
ALTER TABLE fatture_passive ADD INDEX idx_stato_pagamento (stato_pagamento);
```

---

## 🧪 TESTING MATRIX

```
Module              Unit    Integration  E2E  Performance
─────────────────────────────────────────────────────────
Clienti CRUD        ✅      ✅            ✅   ✅
Preventivi          ✅      ✅            ✅   ✅
Fatture Attive      ✅      ✅            ⚠️   ✅
Email Tracking      ✅      ⚠️            ❌   ✅
SDI Integration     ⚠️      ⚠️            ⚠️   ⚠️ (test endpoint)
Fatture Passive     ✅      ✅            ✅   ✅
Commesse            ✅      ✅            ✅   ✅
Reporting           ⚠️      ⚠️            ❌   ❌
```

---

## 📝 DEPLOYMENT ARTIFACT

### Build Output
```
luna2/
├── target/
│   ├── luna2.war (60-80 MB) ← Main deployable
│   ├── luna2-sources.jar
│   ├── luna2-javadoc.jar
│   └── surefire-reports/
├── pom.xml
├── Dockerfile (optional, for docker build)
└── deployment.properties
```

### WAR Contents
```
luna2.war
├── WEB-INF/
│   ├── classes/
│   │   ├── application.properties
│   │   ├── struts.xml
│   │   ├── log4j2.xml
│   │   └── [compiled .class files]
│   ├── lib/
│   │   ├── hibernate-core-5.x.x.jar
│   │   ├── struts2-core-2.x.x.jar
│   │   ├── mysql-connector-java-8.x.x.jar
│   │   ├── itext-core-5.x.x.jar
│   │   └── [100+ other dependencies]
│   ├── web.xml
│   └── views/ [JSP files compiled to Java]
├── jsp/ [JSP source files]
│   ├── clienti/
│   ├── preventivi/
│   ├── fatture/
│   └── ...
├── img/ [Static images/CSS/JS]
└── META-INF/
    ├── MANIFEST.MF
    └── maven/
```

---

**Document Version**: 2.0  
**Last Update**: Post-Commesse  
**Architecture**: Tomcat 9 + Struts2 + Hibernate 5  
**Database**: MySQL 8.0 / PostgreSQL 13+
