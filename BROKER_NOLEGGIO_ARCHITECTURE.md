# 🚗 Modulo Broker Noleggio Auto - Architettura Completa

## 📋 Overview

**Modulo:** Broker Noleggio Auto (sottomodulo CRM)  
**Prerequisito:** Modulo CRM attivo  
**Urgenza:** Alta (vendita in pochi giorni)  
**Scope:** Gestione completa ciclo di vita noleggio auto da lead a post-vendita

---

## 🎯 Requisiti Funzionali (dal PDF)

### Fase 1: Acquisizione, Contatto e Preventivazione
- Registrazione lead (diretti o tramite segnalatori)
- Anagrafica completa cliente
- Esigenze specifiche (Modello auto, Budget, Mesi, Km)
- Storico preventivi inviati
- **Status:** Nuovo → Preventivo in elaborazione → Preventivo inviato → In attesa feedback → Accettato/Rifiutato
- **Automazioni:**
  - Follow-up commerciale: Alert 48h dopo invio preventivo
  - Gestione Rifiuto: Task automatico "Proporre nuova soluzione"

### Fase 2: Istruttoria, Raccolta Documenti e Valutazione
- Checklist documenti reddituali (upload file)
- Compagnia/e partner coinvolte
- Data invio pratica
- **Status:** In raccolta documenti → Inviata in valutazione → In istruttoria → Approvata/Rifiutata
- **Automazioni:**
  - Reminder documenti: Alert se mancanti dopo X giorni
  - Sollecito Compagnia: Alert se pratica in valutazione > 4-5 giorni
  - Alert Alternativa: Task "Inoltrare a compagnia alternativa" se rifiutato

### Fase 3: Ordine, Avanzamento e Logistica di Consegna
- Numero ordine assegnato
- Data presunta arrivo (ETA)
- Dettagli ritiro usato/permuta
- Centro di consegna assegnato
- **Status:** Ordine inserito → In produzione → In transito → Pronta consegna → Consegnata
- **Automazioni:**
  - Care-call periodica: Task ogni 20/30 giorni per aggiornare cliente
  - Coordinamento consegna: Alert quando auto passa in "Pronta consegna"

### Fase 4: Gestione Post-Vendita, Assistenza e Trouble Ticketing
- Sistema ticketing integrato
- Categoria problema (Sinistro, Guasto, Manutenzione, Amministrativo, etc.)
- Storico comunicazioni con compagnia
- **Status:** Aperto → In lavorazione interna → In attesa risposta Compagnia → Risolto/Chiuso
- **Automazioni:**
  - Allarme Anti-Rimbalzo: Alert rosso se status "In attesa Compagnia" > 48-72h
  - Follow-up chiusura: Task per richiamare cliente 24h dopo risoluzione

### Fase 5: Scadenziario Globale, Manutenzioni e Rinnovi
- Data inizio/fine contratto
- Km previsti da contratto
- Scadenza patente guida
- Scadenza revisione/tagliando
- **Status:** Contratto Regolare → Scadenza in avvicinamento → In fase rinegoziazione → Rinnovato/Terminato
- **Automazioni:**
  - Trigger rinnovo: Alert 4-6 mesi prima scadenza contrattuale
  - Verifica chilometrica: Email automatica cliente ogni 6 mesi per check Km
  - Promemoria tecnici: Notifiche scadenze patenti, revisioni, tagliandi

### Modulo Parallelo: Noleggio a Breve Termine (NBT)
- Calendario date consegna/ritiro
- Modello vettura assegnata
- Dati pagamento/importo cauzione
- Checklist stato vettura (ritiro/riconsegna)
- **Status:** Preventivato → Prenotazione Confermata → Vettura in uso → Vettura Rientrata → Pratica Chiusa
- **Automazioni:**
  - Promemoria consegna/ritiro: Alert giornaliero
  - Sblocco cauzione: Reminder per svincolo importo

---

## 🗄️ Architettura Database

### Entità Principali (7 nuove entità)

#### 1. **NoleggioLead** (Estende Lead CRM)
```sql
CREATE TABLE noleggio_leads (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    lead_id BIGINT NOT NULL,  -- FK to crm_leads
    
    -- Origine Lead
    origine_tipo ENUM('DIRETTO', 'SEGNALATORE') NOT NULL,
    segnalatore_id BIGINT,  -- FK to segnalatori (anagrafica)
    commissione_segnalatore DECIMAL(5,2),  -- % commissione
    
    -- Esigenze Cliente
    modello_richiesto VARCHAR(200),
    budget_mensile DECIMAL(10,2),
    durata_mesi INT,
    km_annui INT,
    
    -- Tipo Noleggio
    tipo_noleggio ENUM('LUNGO_TERMINE', 'BREVE_TERMINE') NOT NULL,
    
    -- Status Specifico
    fase ENUM('PREVENTIVAZIONE', 'ISTRUTTORIA', 'ORDINE', 'POST_VENDITA', 'SCADENZARIO') NOT NULL,
    
    -- Timestamps
    data_creazione TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    data_ultima_modifica TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (lead_id) REFERENCES crm_leads(id) ON DELETE CASCADE,
    FOREIGN KEY (segnalatore_id) REFERENCES clienti(id) ON DELETE SET NULL
);
```

#### 2. **NoleggioPreventivo**
```sql
CREATE TABLE noleggio_preventivi (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    noleggio_lead_id BIGINT NOT NULL,
    
    -- Dati Preventivo
    numero_preventivo VARCHAR(50) UNIQUE,
    versione INT DEFAULT 1,  -- Per tracking revisioni
    
    -- Dettagli Auto
    marca VARCHAR(100),
    modello VARCHAR(200),
    allestimento VARCHAR(200),
    alimentazione VARCHAR(50),
    
    -- Condizioni Economiche
    canone_mensile DECIMAL(10,2),
    anticipo DECIMAL(10,2),
    durata_mesi INT,
    km_annui INT,
    km_extra_costo DECIMAL(6,3),  -- €/km extra
    
    -- Servizi Inclusi (JSON or separate table)
    servizi_inclusi TEXT,  -- JSON: ["Manutenzione", "Bollo", "Assicurazione", ...]
    
    -- Status
    status ENUM('BOZZA', 'INVIATO', 'ACCETTATO', 'RIFIUTATO', 'SOSTITUITO') NOT NULL,
    motivo_rifiuto TEXT,
    
    -- File PDF
    file_path VARCHAR(500),
    
    -- Dates
    data_elaborazione TIMESTAMP,
    data_invio TIMESTAMP,
    data_risposta TIMESTAMP,
    scadenza_validita DATE,
    
    -- Tracking
    utente_creazione_id BIGINT,
    
    FOREIGN KEY (noleggio_lead_id) REFERENCES noleggio_leads(id) ON DELETE CASCADE,
    FOREIGN KEY (utente_creazione_id) REFERENCES utenti(id)
);
```

#### 3. **NoleggioDocumento**
```sql
CREATE TABLE noleggio_documenti (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    noleggio_lead_id BIGINT NOT NULL,
    
    -- Tipo Documento
    tipo_documento ENUM(
        'DOCUMENTO_IDENTITA',
        'PATENTE_GUIDA',
        'CODICE_FISCALE',
        'BUSTA_PAGA',
        'CU',
        'UNICO',
        'BILANCIO_AZIENDALE',
        'VISURA_CAMERALE',
        'ALTRO'
    ) NOT NULL,
    
    -- Dettagli
    descrizione VARCHAR(500),
    obbligatorio BOOLEAN DEFAULT TRUE,
    
    -- File
    nome_file VARCHAR(255),
    file_path VARCHAR(500),
    file_size BIGINT,
    mime_type VARCHAR(100),
    
    -- Status
    status ENUM('DA_RICHIEDERE', 'RICHIESTO', 'RICEVUTO', 'VALIDATO', 'RIFIUTATO') NOT NULL,
    note_validazione TEXT,
    
    -- Dates
    data_richiesta TIMESTAMP,
    data_ricezione TIMESTAMP,
    data_validazione TIMESTAMP,
    
    -- Scadenza (per patente, documento, etc.)
    data_scadenza_documento DATE,
    
    FOREIGN KEY (noleggio_lead_id) REFERENCES noleggio_leads(id) ON DELETE CASCADE
);
```

#### 4. **NoleggioValutazione**
```sql
CREATE TABLE noleggio_valutazioni (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    noleggio_lead_id BIGINT NOT NULL,
    
    -- Compagnia Partner
    compagnia_id BIGINT NOT NULL,  -- FK to fornitori
    compagnia_nome VARCHAR(200),
    compagnia_referente VARCHAR(200),
    compagnia_email VARCHAR(200),
    compagnia_telefono VARCHAR(50),
    
    -- Tracking Pratica
    numero_pratica_compagnia VARCHAR(100),
    data_invio TIMESTAMP,
    data_risposta_attesa DATE,
    
    -- Status
    status ENUM(
        'DA_INVIARE',
        'INVIATA',
        'IN_VALUTAZIONE',
        'RICHIESTA_INTEGRAZIONE',
        'APPROVATA',
        'RIFIUTATA'
    ) NOT NULL,
    
    -- Esito
    motivazione TEXT,
    importo_approvato DECIMAL(10,2),
    condizioni_speciali TEXT,
    
    -- Dates
    data_ultimo_sollecito TIMESTAMP,
    numero_solleciti INT DEFAULT 0,
    
    FOREIGN KEY (noleggio_lead_id) REFERENCES noleggio_leads(id) ON DELETE CASCADE,
    FOREIGN KEY (compagnia_id) REFERENCES fornitori(id)
);
```

#### 5. **NoleggioOrdine**
```sql
CREATE TABLE noleggio_ordini (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    noleggio_lead_id BIGINT NOT NULL,
    noleggio_preventivo_id BIGINT NOT NULL,
    
    -- Dati Ordine
    numero_ordine VARCHAR(100) UNIQUE NOT NULL,
    numero_ordine_compagnia VARCHAR(100),
    
    -- Status
    status ENUM(
        'INSERITO',
        'CONFERMATO',
        'IN_PRODUZIONE',
        'IN_TRANSITO',
        'PRONTA_CONSEGNA',
        'CONSEGNATA',
        'ANNULLATO'
    ) NOT NULL,
    
    -- Tracking Auto
    marca VARCHAR(100),
    modello VARCHAR(200),
    targa VARCHAR(20),
    telaio VARCHAR(50),
    colore VARCHAR(50),
    
    -- Date Tracking
    data_ordine TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    data_conferma_produzione DATE,
    data_eta_arrivo DATE,
    data_arrivo_effettivo DATE,
    data_consegna_cliente DATE,
    
    -- Logistica Consegna
    centro_consegna_id BIGINT,  -- FK to sedi/centri
    centro_consegna_nome VARCHAR(200),
    centro_consegna_indirizzo TEXT,
    
    -- Ritiro Usato/Permuta
    ha_permuta BOOLEAN DEFAULT FALSE,
    dettagli_permuta TEXT,
    targa_usato VARCHAR(20),
    valore_permuta DECIMAL(10,2),
    
    -- Care-Call Tracking
    data_ultimo_care_call DATE,
    prossimo_care_call DATE,
    
    FOREIGN KEY (noleggio_lead_id) REFERENCES noleggio_leads(id) ON DELETE CASCADE,
    FOREIGN KEY (noleggio_preventivo_id) REFERENCES noleggio_preventivi(id)
);
```

#### 6. **NoleggioTicket**
```sql
CREATE TABLE noleggio_tickets (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    noleggio_lead_id BIGINT NOT NULL,
    noleggio_ordine_id BIGINT,
    
    -- Dati Ticket
    numero_ticket VARCHAR(50) UNIQUE NOT NULL,
    
    -- Categoria Problema
    categoria ENUM(
        'SINISTRO',
        'GUASTO_MECCANICO',
        'GUASTO_ELETTRONICO',
        'MANUTENZIONE',
        'AMMINISTRATIVO',
        'CONTRATTUALE',
        'ALTRO'
    ) NOT NULL,
    
    priorita ENUM('BASSA', 'MEDIA', 'ALTA', 'URGENTE') NOT NULL DEFAULT 'MEDIA',
    
    -- Descrizione
    titolo VARCHAR(500) NOT NULL,
    descrizione TEXT NOT NULL,
    
    -- Status
    status ENUM(
        'APERTO',
        'IN_LAVORAZIONE',
        'IN_ATTESA_CLIENTE',
        'IN_ATTESA_COMPAGNIA',
        'RISOLTO',
        'CHIUSO'
    ) NOT NULL DEFAULT 'APERTO',
    
    -- Comunicazione Compagnia
    inoltrato_a_compagnia BOOLEAN DEFAULT FALSE,
    data_inoltro_compagnia TIMESTAMP,
    data_ultima_risposta_compagnia TIMESTAMP,
    ore_attesa_compagnia INT,  -- per anti-rimbalzo
    
    -- Risoluzione
    soluzione TEXT,
    data_risoluzione TIMESTAMP,
    
    -- Assignment
    assegnato_a_utente_id BIGINT,
    
    -- Dates
    data_apertura TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    data_chiusura TIMESTAMP,
    data_ultimo_aggiornamento TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (noleggio_lead_id) REFERENCES noleggio_leads(id) ON DELETE CASCADE,
    FOREIGN KEY (noleggio_ordine_id) REFERENCES noleggio_ordini(id),
    FOREIGN KEY (assegnato_a_utente_id) REFERENCES utenti(id)
);
```

#### 7. **NoleggioContratto** (Scadenzario)
```sql
CREATE TABLE noleggio_contratti (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    noleggio_lead_id BIGINT NOT NULL,
    noleggio_ordine_id BIGINT NOT NULL,
    
    -- Dati Contratto
    numero_contratto VARCHAR(100) UNIQUE NOT NULL,
    
    -- Date Contrattuali
    data_inizio DATE NOT NULL,
    data_fine DATE NOT NULL,
    durata_mesi INT NOT NULL,
    
    -- Km
    km_previsti_anno INT NOT NULL,
    km_totali_previsti INT,  -- durata_mesi / 12 * km_anno
    km_attuali INT DEFAULT 0,
    data_ultima_rilevazione_km DATE,
    
    -- Canone
    canone_mensile DECIMAL(10,2) NOT NULL,
    
    -- Scadenze Tecniche
    data_prossima_revisione DATE,
    data_prossimo_tagliando DATE,
    km_prossimo_tagliando INT,
    
    -- Scadenze Documenti
    data_scadenza_patente_conducente DATE,
    data_scadenza_assicurazione DATE,
    
    -- Rinnovo
    status_rinnovo ENUM(
        'ATTIVO',
        'SCADENZA_VICINA',
        'IN_FASE_RINNOVO',
        'RINNOVATO',
        'TERMINATO'
    ) NOT NULL DEFAULT 'ATTIVO',
    
    alert_rinnovo_inviato BOOLEAN DEFAULT FALSE,
    data_primo_alert_rinnovo DATE,
    
    -- Verifiche Periodiche
    data_ultima_verifica_km DATE,
    prossima_verifica_km DATE,
    
    FOREIGN KEY (noleggio_lead_id) REFERENCES noleggio_leads(id) ON DELETE CASCADE,
    FOREIGN KEY (noleggio_ordine_id) REFERENCES noleggio_ordini(id)
);
```

#### 8. **NoleggioNBT** (Noleggio Breve Termine)
```sql
CREATE TABLE noleggio_nbt (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    
    -- Cliente (può essere anche occasionale, non lead)
    cliente_id BIGINT,  -- FK to clienti (optional)
    nome_cliente VARCHAR(200) NOT NULL,
    cognome_cliente VARCHAR(200) NOT NULL,
    email VARCHAR(200),
    telefono VARCHAR(50) NOT NULL,
    documento_tipo VARCHAR(50),
    documento_numero VARCHAR(50),
    
    -- Date Noleggio
    data_ritiro DATETIME NOT NULL,
    data_riconsegna DATETIME NOT NULL,
    durata_giorni INT,
    
    -- Vettura
    vettura_id BIGINT,  -- FK to parco_auto (se gestito)
    marca VARCHAR(100),
    modello VARCHAR(200),
    targa VARCHAR(20),
    
    -- Economico
    tariffa_giornaliera DECIMAL(10,2),
    importo_totale DECIMAL(10,2),
    cauzione DECIMAL(10,2),
    cauzione_sbloccata BOOLEAN DEFAULT FALSE,
    data_sblocco_cauzione TIMESTAMP,
    
    -- Pagamento
    metodo_pagamento VARCHAR(50),
    pagamento_ricevuto BOOLEAN DEFAULT FALSE,
    
    -- Checklist Stato Vettura
    km_ritiro INT,
    km_riconsegna INT,
    carburante_ritiro VARCHAR(20),  -- es: "Pieno", "3/4", "1/2"
    carburante_riconsegna VARCHAR(20),
    
    -- Danni/Note
    note_ritiro TEXT,
    danni_ritiro TEXT,
    note_riconsegna TEXT,
    danni_riconsegna TEXT,
    
    -- Foto (paths JSON)
    foto_ritiro TEXT,  -- JSON array
    foto_riconsegna TEXT,  -- JSON array
    
    -- Status
    status ENUM(
        'PREVENTIVATO',
        'PRENOTAZIONE_CONFERMATA',
        'VETTURA_IN_USO',
        'VETTURA_RIENTRATA',
        'PRATICA_CHIUSA'
    ) NOT NULL DEFAULT 'PREVENTIVATO',
    
    -- Dates
    data_prenotazione TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    data_ultima_modifica TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (cliente_id) REFERENCES clienti(id) ON DELETE SET NULL
);
```

---

## 🔧 Layer Architettura

### DAO Layer (7 DAO classes)

```
NoleggioLeadDAO
NoleggioP

reventivoDAO
NoleggioDocumentoDAO
NoleggioValutazioneDAO
NoleggioOrdineDAO
NoleggioTicketDAO
NoleggioContrattoDAO
NoleggioNBTDAO
```

### Service Layer (3 Service classes)

#### 1. **NoleggioLeadService**
```java
// Gestione fasi 1-3
- createNoleggioLead()
- generaPreventivo()
- inviaPreventivo()
- accettaPreventivo()
- rifiutaPreventivo()
- richiedoDocumenti()
- validaDocumenti()
- inviaInValutazione()
- creaOrdine()
- aggiornaStatusOrdine()
- scheduleCareCall()
```

#### 2. **NoleggioTicketService**
```java
// Gestione fase 4 (post-vendita)
- apriTicket()
- assegnaTicket()
- inoltraACompagnia()
- checkAntiRimbalzo()  // verifica se alert da scattare
- aggiornaTicket()
- risolviTicket()
- chiudiTicket()
- getTicketAperti()
- getTicketInAttesaCompagnia()
```

#### 3. **NoleggioScadenzarioService**
```java
// Gestione fase 5 (scadenzario)
- checkScadenzeContratti()  // da eseguire giornalmente
- aggiornaKm()
- checkScadenzeDocumenti()
- checkScadenzeRevisioni()
- inviaAlertRinnovo()
- verificaChilometrica()  // ogni 6 mesi
- rinnovaContratto()
```

#### 4. **NoleggioNBTService**
```java
// Gestione NBT
- creaPrenotazioneNBT()
- confermaPrenotazione()
- checkInVettura()
- checkOutVettura()
- sbloccaCauzione()
- chiudiPraticaNBT()
```

### Automation Engine

#### **NoleggioAutomationService**

**INTEGRAZIONE CON SISTEMA NOTIFICHE CORE ESISTENTE:**

Il modulo **USA il sistema di notifiche già implementato** nel core:
- `PushNotificationService.getInstance()` per notifiche push real-time
- `EventPublisher` per pubblicare eventi
- `NotificationEvent` per creare notifiche con priorità
- Sistema email reminder esistente

```java
// Esecuzione automazioni descritte nel PDF
// Da schedulare con cron job

public void eseguiAutomazioni() {
    PushNotificationService pushService = PushNotificationService.getInstance();
    
    // Fase 1 - Preventivazione
    followUpPreventiviInviati48h();  // → Genera Task CRM + Push Notification
    gestioneRifiutiConNuovaProposta();
    
    // Fase 2 - Istruttoria
    reminderDocumentiMancanti();  // → Email reminder + Push
    sollecitoCompagnie4GG();
    alertAlternativaSeRifiutato();
    
    // Fase 3 - Ordine
    scheduleCareCall20Giorni();  // → Task CRM + Push
    alertCoordinamentoConsegna();
    
    // Fase 4 - Post-Vendita
    checkAntiRimbalzo48h();  // → Push notification PRIORITA' ALTA
    followUpChiusuraTicket24h();
    
    // Fase 5 - Scadenzario
    alertRinnovo4Mesi();  // → Email + Push + Task CRM
    verificaChilometricaOgni6Mesi();
    promemoriaTecnici();
    
    // NBT
    promemoria ConsegnaRitiroGiornaliero();
    reminderSbloccoCauzione();
}

// Esempio: Invio notifica anti-rimbalzo
private void inviaNotificaAntiRimbalzo(NoleggioTicket ticket) {
    // Push notification real-time
    PushNotificationService.getInstance().sendNotificationToUser(
        String.valueOf(ticket.getAssegnatoAUtenteId()),
        "⚠️ ALERT Anti-Rimbalzo",
        "Ticket #" + ticket.getNumeroTicket() + " in attesa compagnia da " + 
        ticket.getOreAttesaCompagnia() + " ore!",
        "URGENTE"
    );
    
    // Task CRM automatico
    Task task = new Task();
    task.setTitolo("URGENTE: Sollecitare compagnia per ticket #" + ticket.getNumeroTicket());
    task.setPriorita(Task.Priorita.URGENTE);
    task.setDataScadenza(new Date());  // Immediato
    taskDAO.save(task);
}
```

---

## 📅 INTEGRAZIONE CALENDARIO (Google/iCloud)

### **Scadenzario Automatico con Sincronizzazione Google Calendar / iCloud**

Il modulo **USA il sistema calendario già implementato** nel core (integrazione Google Calendar e iCloud via CalDAV):

**Componenti Esistenti:**
- `CalendarEvent` - Entità per eventi sincronizzati
- `CalendarAccount` - Account Google/iCloud configurati
- `CalendarSyncService` - Sincronizzazione bidirezionale automatica
- `GoogleCalendarProvider` - Provider Google Calendar API
- `CalDavCalendarProvider` - Provider CalDAV (iCloud, etc.)

**Estensione CalendarEvent.SourceType:**

```java
// In model/CalendarEvent.java - ESTENDERE enum esistente
public enum SourceType {
    REMINDER,                    // Esistente
    TASK,                        // Esistente
    MEETING,                     // Esistente
    NOLEGGIO_RINNOVO,           // NEW: Alert rinnovo contratto 4-6 mesi prima
    NOLEGGIO_REVISIONE,         // NEW: Scadenza revisione auto
    NOLEGGIO_TAGLIANDO,         // NEW: Scadenza tagliando manutenzione
    NOLEGGIO_PATENTE,           // NEW: Scadenza patente conducente
    NOLEGGIO_ASSICURAZIONE,     // NEW: Scadenza assicurazione veicolo
    NOLEGGIO_VERIFICA_KM        // NEW: Verifica chilometrica semestrale
}
```

**Creazione Eventi Calendario Automatici (in NoleggioScadenzarioService):**

```java
public void creaEventoScadenza(NoleggioContratto contratto, TipoScadenza tipo, Date dataScadenza) {
    CalendarEvent event = new CalendarEvent();
    event.setUserId(String.valueOf(contratto.getUtenteAssegnatoId()));
    
    // Usarà l'account Google/iCloud configurato per l'utente
    CalendarAccount account = calendarAccountDAO.findByUserId(event.getUserId());
    if (account != null) {
        event.setProvider(account.getProvider());  // GOOGLE o ICLOUD
    }
    
    switch (tipo) {
        case RINNOVO:
            event.setSourceType(CalendarEvent.SourceType.NOLEGGIO_RINNOVO);
            event.setTitle("🔄 Rinnovo Contratto Noleggio #" + contratto.getNumeroContratto());
            event.setDescription(
                "Cliente: " + contratto.getNomeCliente() + "\n" +
                "Veicolo: " + contratto.getMarca() + " " + contratto.getModello() + "\n" +
                "Targa: " + contratto.getTarga() + "\n" +
                "Canone: €" + contratto.getCanoneMensile() + "/mese\n\n" +
                "⚠️ Contattare cliente 4-6 mesi prima scadenza per proposta rinnovo"
            );
            break;
            
        case REVISIONE:
            event.setSourceType(CalendarEvent.SourceType.NOLEGGIO_REVISIONE);
            event.setTitle("🔧 Revisione Auto - " + contratto.getTarga());
            event.setDescription(
                "Revisione obbligatoria veicolo\n" +
                "Marca/Modello: " + contratto.getMarca() + " " + contratto.getModello() + "\n" +
                "⚠️ Prenotare centro revisioni"
            );
            break;
            
        case TAGLIANDO:
            event.setSourceType(CalendarEvent.SourceType.NOLEGGIO_TAGLIANDO);
            event.setTitle("🔩 Tagliando Manutenzione - " + contratto.getTarga());
            event.setDescription(
                "Tagliando manutenzione programmata\n" +
                "Km previsto: " + contratto.getKmProssimoTagliando() + " km"
            );
            break;
            
        case PATENTE:
            event.setSourceType(CalendarEvent.SourceType.NOLEGGIO_PATENTE);
            event.setTitle("🪪 Scadenza Patente Conducente");
            event.setDescription(
                "Contratto: " + contratto.getNumeroContratto() + "\n" +
                "Cliente: " + contratto.getNomeCliente() + "\n" +
                "⚠️ Richiedere copia patente aggiornata"
            );
            break;
            
        case VERIFICA_KM:
            event.setSourceType(CalendarEvent.SourceType.NOLEGGIO_VERIFICA_KM);
            event.setTitle("📊 Verifica Chilometrica - " + contratto.getTarga());
            event.setDescription(
                "Richiesta chilometraggio attuale al cliente\n" +
                "Contratto: " + contratto.getNumeroContratto() + "\n" +
                "Km previsti: " + contratto.getKmTotaliPrevisti() + " km totali"
            );
            break;
    }
    
    event.setSourceId(contratto.getId());
    event.setStartTime(convertToLocalDateTime(dataScadenza));
    event.setEndTime(convertToLocalDateTime(dataScadenza).plusHours(1));
    event.setStatus(CalendarEvent.Status.ACTIVE);
    
    // Salva evento - verrà sincronizzato AUTOMATICAMENTE con Google/iCloud
    calendarEventDAO.save(event);
    
    logger.info("✅ Evento calendario creato: " + tipo + " - Contratto: " + 
                contratto.getNumeroContratto() + " - Sync con " + 
                (account != null ? account.getProvider() : "nessun account"));
}

/**
 * Automazione giornaliera: verifica scadenze e crea eventi calendario
 * Schedulata da CronJobListener ogni giorno alle 02:00
 */
public void verificaScadenzeECreaEventiCalendario() {
    List<NoleggioContratto> contratti = noleggioContrattoDAO.findAttivi();
    
    for (NoleggioContratto contratto : contratti) {
        // 1. Alert rinnovo 4-6 mesi prima scadenza
        if (contratto.isRinnovoInScadenza(120)) {  // 4 mesi = 120 giorni
            if (!esisteEventoScadenza(contratto.getId(), TipoScadenza.RINNOVO)) {
                Date dataAlert = sottraiGiorni(contratto.getDataFine(), 120);
                creaEventoScadenza(contratto, TipoScadenza.RINNOVO, dataAlert);
            }
        }
        
        // 2. Verifica chilometrica ogni 6 mesi
        if (contratto.richiedeVerificaKm()) {
            Date prossimaVerifica = contratto.getProssimaVerificaKm();
            if (!esisteEventoScadenza(contratto.getId(), TipoScadenza.VERIFICA_KM)) {
                creaEventoScadenza(contratto, TipoScadenza.VERIFICA_KM, prossimaVerifica);
            }
        }
        
        // 3. Revisione auto
        if (contratto.getDataProssimaRevisione() != null) {
            if (!esisteEventoScadenza(contratto.getId(), TipoScadenza.REVISIONE)) {
                // Alert 30 giorni prima
                Date dataAlert = sottraiGiorni(contratto.getDataProssimaRevisione(), 30);
                creaEventoScadenza(contratto, TipoScadenza.REVISIONE, dataAlert);
            }
        }
        
        // 4. Tagliando manutenzione
        if (contratto.getDataProssimoTagliando() != null) {
            if (!esisteEventoScadenza(contratto.getId(), TipoScadenza.TAGLIANDO)) {
                Date dataAlert = sottraiGiorni(contratto.getDataProssimoTagliando(), 15);
                creaEventoScadenza(contratto, TipoScadenza.TAGLIANDO, dataAlert);
            }
        }
        
        // 5. Patente conducente
        if (contratto.getDataScadenzaPatenteConducente() != null) {
            if (!esisteEventoScadenza(contratto.getId(), TipoScadenza.PATENTE)) {
                // Alert 30 giorni prima scadenza
                Date dataAlert = sottraiGiorni(contratto.getDataScadenzaPatenteConducente(), 30);
                creaEventoScadenza(contratto, TipoScadenza.PATENTE, dataAlert);
            }
        }
        
        // 6. Assicurazione
        if (contratto.getDataScadenzaAssicurazione() != null) {
            if (!esisteEventoScadenza(contratto.getId(), TipoScadenza.ASSICURAZIONE)) {
                Date dataAlert = sottraiGiorni(contratto.getDataScadenzaAssicurazione(), 30);
                creaEventoScadenza(contratto, TipoScadenza.ASSICURAZIONE, dataAlert);
            }
        }
    }
    
    logger.info("✅ Verifica scadenze completata. Eventi calendario creati/aggiornati.");
}
```

**Benefici Integrazione Calendario:**

✅ **Sincronizzazione automatica** con Google Calendar / iCloud Calendar  
✅ **Notifiche push mobile** via app Google Calendar / Apple Calendar  
✅ **Visibilità calendario** anche fuori dal software Luna2  
✅ **Condivisione calendario** con team (se calendario condiviso)  
✅ **Reminder nativi** iOS/Android per scadenze noleggio  
✅ **Nessun sviluppo aggiuntivo** - sistema già esistente e testato  
✅ **Backup automatico** eventi su cloud Google/Apple  

**Configurazione Utente:**

Ogni utente broker può configurare il proprio account calendario in:
`/app/calendar/settings` → Collega Google Calendar o iCloud

**Schedulazione Automatica:**

```java
// In CronJobListener.java - AGGIUNGERE nuovo task
timerNoleggio = new Timer("NoleggioScadenzario", true);
timerNoleggio.scheduleAtFixedRate(new NoleggioScadenzarioTask(),
    0,  // Esegui subito
    24 * 60 * 60 * 1000);  // Ogni 24 ore

private static class NoleggioScadenzarioTask extends TimerTask {
    @Override
    public void run() {
        try {
            NoleggioScadenzarioService service = new NoleggioScadenzarioService();
            service.verificaScadenzeECreaEventiCalendario();
        } catch (Exception e) {
            logger.error("Errore in NoleggioScadenzarioTask", e);
        }
    }
}
```

---

## 🎨 UI Layer (JSP Views)

### Views Principali

1. **noleggio/lead/list.jsp** - Lista lead noleggio con filtri per fase
2. **noleggio/lead/view.jsp** - Vista dettagliata lead con timeline completa
3. **noleggio/lead/edit.jsp** - Form creazione/modifica lead
4. **noleggio/preventivo/list.jsp** - Storico preventivi per lead
5. **noleggio/preventivo/create.jsp** - Form nuovo preventivo
6. **noleggio/preventivo/view.jsp** - Dettaglio preventivo + PDF
7. **noleggio/documenti/checklist.jsp** - Checklist documenti richiesti
8. **noleggio/valutazione/view.jsp** - Status valutazione compagnie
9. **noleggio/ordine/tracking.jsp** - Tracking ordine con ETA
10. **noleggio/ticket/list.jsp** - Lista ticket post-vendita
11. **noleggio/ticket/view.jsp** - Dettaglio ticket + storico comunicazioni
12. **noleggio/scadenzario/dashboard.jsp** - Dashboard scadenze contracts
13. **noleggio/nbt/list.jsp** - Calendario NBT
14. **noleggio/nbt/checkin.jsp** - Form check-in vettura
15. **noleggio/nbt/checkout.jsp** - Form check-out vettura

---

## 🔀 Struts2 Actions

### Actions Principali

```
NoleggioLeadAction (15 metodi)
NoleggioP reventivoAction (10 metodi)
NoleggioDocumentoAction (8 metodi)
NoleggioValutazioneAction (8 metodi)
NoleggioOrdineAction (10 metodi)
NoleggioTicketAction (12 metodi)
NoleggioContrattoAction (10 metodi)
NoleggioNBTAction (12 metodi)
```

---

## 🔔 Sistema Automazioni & Task

### Integrazione con CRM Task System

Tutte le automazioni descritte nel PDF generano **Task CRM** automatici:

```java
// Esempio: Alert Follow-up 48h dopo invio preventivo
Task followUpTask = new Task();
followUpTask.setLeadId(noleggioLead.getLeadId());
followUpTask.setTitolo("Follow-up Preventivo Inviato");
followUpTask.setDescrizione("Richiamare cliente per feedback su preventivo #" + preventivo.getNumeroPreventivo());
followUpTask.setDataScadenza(Date dopo 48h);
followUpTask.setPriorita(Task.Priorita.ALTA);
followUpTask.setTipo("FOLLOW_UP_PREVENTIVO");
taskDAO.save(followUpTask);
```

### Tipi di Task Generati Automaticamente

| Tipo Task | Trigger | Scadenza |
|-----------|---------|----------|
| FOLLOW_UP_PREVENTIVO | Preventivo inviato | +48h |
| NUOVA_PROPOSTA_RIFIUTO | Preventivo rifiutato | Immediato |
| REMINDER_DOCUMENTI | Documenti mancanti | +3 giorni |
| SOLLECITO_COMPAGNIA | Valutazione pending | +4 giorni |
| COMPAGNIA_ALTERNATIVA | Valutazione rifiutata | Immediato |
| CARE_CALL_ORDINE | Ordine in produzione | Ogni 20-30 giorni |
| COORDINAMENTO_CONSEGNA | Auto pronta consegna | Immediato |
| ANTI_RIMBALZO_TICKET | Ticket in attesa compagnia | +48h |
| FOLLOW_UP_TICKET_CHIUSO | Ticket risolto | +24h |
| ALERT_RINNOVO | Contratto scadenza vicina | -4 mesi |
| VERIFICA_KM | Contratto attivo | Ogni 6 mesi |
| SCADENZA_REVISIONE | Revisione in scadenza | -30 giorni |
| NBT_PROMEMORIA_RITIRO | Prenotazione NBT | Giorno del ritiro |
| NBT_SBLOCCO_CAUZIONE | Vettura rientrata | +24h |

---

## 📊 Dashboard & KPI

### Widget Dashboard Modulo Noleggio

```
┌─────────────────────────────────────────────────┐
│  🚗 BROKER NOLEGGIO AUTO - Dashboard            │
├─────────────────────────────────────────────────┤
│                                                 │
│  📊 KPI Mensili:                                │
│    • Lead Attivi:               42              │
│    • Preventivi Inviati:        18              │
│    • Pratiche in Valutazione:    8              │
│    • Ordini in Corso:           12              │
│    • Contratti Attivi:         156              │
│                                                 │
│  ⚠️  Alert Urgenti (7):                         │
│    🔴 3 Ticket con anti-rimbalzo attivo         │
│    🟡 4 Compagnie da sollecitare                │
│    🟢 2 Auto pronte consegna                    │
│                                                 │
│  📅 Scadenze Prossime 30 giorni:                │
│    • 5 Contratti da rinnovare                   │
│    • 12 Revisioni da programmare                │
│    • 3 Patenti in scadenza                      │
│                                                 │
│  💰 Valore Pipeline:        € 1.240.000         │
│  📈 Tasso Conversione:           65%            │
│                                                 │
└─────────────────────────────────────────────────┘
```

---

## 🚀 Piano Implementazione

### Milestone 1: Foundation (Giorni 1-2)
- ✅ Creare entità model (8 classes)
- ✅ Generare tabelle SQL
- ✅ Implementare DAO layer (8 DAO)

### Milestone 2: Business Logic (Giorni 2-3)
- ✅ Implementare Service layer (4 services)
- ✅ Automation engine per task automatici
- ✅ Integrazione con CRM Task system esistente

### Milestone 3: Controllers (Giorno 4)
- ✅ Implementare Actions Struts2 (8 actions)
- ✅ JSON endpoints per AJAX
- ✅ File upload per documenti

### Milestone 4: UI (Giorni 4-5)
- ✅ Implementare JSP views (15 viste)
- ✅ Dashboard modulo noleggio
- ✅ Form validazioni client-side

### Milestone 5: Testing & Deploy (Giorno 6)
- ✅ Test integrazione
- ✅ Seed data per demo
- ✅ Documentazione utente

**Timeline Totale: 6 giorni lavorativi**

---

## 📝 Note Implementazione

### Dipendenze da CRM Esistente

Il modulo **DEVE avere CRM attivo** perché:
1. Usa `crm_leads` table come base
2. Usa `crm_tasks` per automazioni
3. Usa `crm_activities` per log azioni
4. Usa `crm_pipeline_stages` per workflow

### Configurazione Modulo

**INTEGRAZIONE CON SISTEMA MODULI ESISTENTE:**

```sql
-- Inserimento in module_settings
INSERT INTO module_settings (code, name, enabled, description) VALUES
('BROKER_NOLEGGIO', 'Broker Noleggio Auto', false, 
 'Gestione completa broker noleggio auto. Richiede modulo CRM attivo.');
```

**Sidebar UI (includes/sidebar.jsp):**

```jsp
<%-- Aggiunto dopo blocco CRM --%>
<s:if test="#session.enabledModules['BROKER_NOLEGGIO']">
    <li class="nav-item">
        <a class="nav-link" href="<s:url action='dashboard' namespace='/app/noleggio'/>">
            <i class="bi bi-car-front me-2"></i>Broker Noleggio
        </a>
    </li>
    <li class="nav-item">
        <a class="nav-link" href="<s:url action='list' namespace='/app/noleggio/lead'/>">
            <i class="bi bi-person-badge me-2"></i>Lead Noleggio
        </a>
    </li>
    <li class="nav-item">
        <a class="nav-link" href="<s:url action='list' namespace='/app/noleggio/ticket'/>">
            <i class="bi bi-ticket-detailed me-2"></i>Ticket Post-Vendita
        </a>
    </li>
    <li class="nav-item">
        <a class="nav-link" href="<s:url action='dashboard' namespace='/app/noleggio/scadenzario'/>">
            <i class="bi bi-calendar-check me-2"></i>Scadenzario
        </a>
    </li>
    <li class="nav-item">
        <a class="nav-link" href="<s:url action='list' namespace='/app/noleggio/nbt'/>">
            <i class="bi bi-calendar-event me-2"></i>Noleggio Breve Termine
        </a>
    </li>
</s:if>
```

**Validation in Interceptor:**

```java
// ModuleAccessInterceptor verifica dipendenze
if (enabledModules.get("BROKER_NOLEGGIO") && !enabledModules.get("CRM")) {
    addActionError("Modulo BROKER_NOLEGGIO richiede modulo CRM attivo");
    return Action.ERROR;
}
```

---

## 🎯 Obiettivo Finale

Sistema completo che:
- ✅ Gestisce tutte le 5 fasi descritte nel PDF
- ✅ Automazioni complete con task/alert automatici
- ✅ Sistema anti-rimbalzo per compagnie
- ✅ Scadenzario intelligente
- ✅ Modulo NBT parallelo
- ✅ Dashboard con KPI real-time
- ✅ Pronto per vendita in pochi giorni

---

**Status:** 📋 Architettura Completa - Ready for Implementation  
**Next Step:** Iniziare Milestone 1 - Database Schema & Model Entities
