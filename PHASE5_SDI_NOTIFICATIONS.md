# Phase 5: Gestione Notifiche SDI

**Obiettivo**: Implementare il sistema completo di gestione delle notifiche da SDI (Sistema di Interscambio), con recupero automatico dello stato delle fatture inviate.

## Modifiche Apportate

### 1. **Entity Model**

#### Fattura.java
- Aggiunto campo `sdiCodice` (String) per salvare l'ID univoco ricevuto da SDI
- Aggiunto campo `sdiStato` (String) per tracciare lo stato: "INVIATA", "ACCETTATA", "SCARTATA", "ERRORE"
- Aggiunto getter/setter per entrambi i campi

#### SdiNotifica.java (NEW)
```java
- id: Long (chiave primaria)
- fattura: Fattura (relazione ManyToOne)
- sdiCodice: String (codice univoco ricevuto da SDI)
- stato: String (ACCETTATA, SCARTATA, ERRORE, CONSEGNATA)
- xmlRisposta: String (testo della risposta XML dalla notifica)
- dataRicezione: Date (timestamp ricezione notifica)
- letta: Boolean (per tracciare se utente ha visualizzato)
- descrizioneErrore: String (dettagli errore se presente)
- dataCreazione: Date (timestamp creazione record)
```

#### HibernateUtil.java
- Registrato `it.zensoftware.luna2.model.SdiNotifica.class`

### 2. **DAO Layer**

#### FatturaDAO.java
- Aggiunto metodo `findFattureWithoutNotifica()` per trovare tutte le fatture REALE che hanno `sdiCodice` ma non ancora notifica ricevuta

#### SdiNotificaDAO.java (NEW)
```java
- save(SdiNotifica): salva una nuova notifica
- update(SdiNotifica): aggiorna una notifica esistente
- findBySdiCodice(String): trova notifica per codice SDI
- findByFatturaId(Long): trova l'ultima notifica per una fattura
- findByFatturaIdAllNotifiche(Long): trova tutte le notifiche di una fattura
- findByStato(String): trova notifiche per stato
- findNonLette(): trova notifiche non ancora visualizzate
```

### 3. **Service Layer**

#### FatturaXMLService.java (MODIFICATO)
- Modificato `SdiResponse`:
  - Aggiunto campo `sdiCodice` estrapolato dalla risposta JSON usando regex
  - Aggiunto metodo `extractCodiceFromJson()` per parsare JSON con pattern `"codice": "xxxxx"` o `"id": "xxxxx"`
  - Aggiunto getter `getSdiCodice()`

#### SdiNotificheService.java (NEW)
```java
Scopo: recuperare le notifiche dal Sistema di Interscambio

Metodi principali:
- sincronizzaTutteNotifiche(): polling di tutte le fatture in attesa di notifica
- recuperaNotifica(String sdiCodice): GET HTTP a endpoint SDI con the codice
- parseXmlResponse(String): parsa risposta XML per estrarre stato e descrizione
- NotificaResponse: inner class con trovata(boolean), stato, descrizioneErrore, xmlRisposta

Comportamento:
- Per ogni fattura REALE con sdiCodice ma senza sdiStato:
  1. Effettua GET a: https://api.luna.itsolutions-cloud.com/ricevi-notifiche/index.php?codice=XXXXX
  2. Parsa risposta XML per estrarre stato (ACCETTATA|SCARTATA|ERRORE)
  3. Salva SdiNotifica nel database
  4. Aggiorna sdiStato della fattura
```

### 4. **Action Layer**

#### FattureAction.java (MODIFICATO)
`sendXmlSdi()`:
- Dopo ricevere successo dall'endpoint SDI:
  - Se `response.getSdiCodice()` non è null: salva il codice nella fattura
  - Imposta `fattura.setSdiStato("INVIATA")`
  - Chiama `fatturaDAO.update(fattura)`
  - Mostra messaggio di successo con il codice ricevuto
- Se invio fallisce: registra errore e mostra messaggio d'errore

#### SdiNotificheAction.java (NEW)
```java
Metodo 1: sincronizzaNotifiche() 
- Endpoint: POST /app/sdi-sincronizza-notifiche
- Scopo: controllare tutte le fatture in attesa e aggiornarne lo stato
- Utilizzato dal cron job per polling automatico
- Ritorna JSON response con actionMessages

Metodo 2: getStatoNotifica()
- Endpoint: GET /app/sdi-stato-notifica?fatturaId=123
- Scopo: polling asincrono da client per una specifica fattura
- Se notifica non trovata nel DB, tenta di recuperarla live da SDI
- Ritorna stato JSON con ultimo stato disponibile
```

### 5. **Background Task (Cron)**

#### CronJobListener.java (NEW)
```java
- Implementa ServletContextListener con @WebListener
- Avviato automaticamente all'startup dell'applicazione
- Crea Timer che esegue SdiNotifichePollerTask ogni 5 minuti
- SdiNotifichePollerTask:
  - Istanzia DAO e Service
  - Chiama service.sincronizzaTutteNotifiche()
  - Logga risultati e errori
  - Esecuzione in thread daemon (non blocca shutdown)
```

### 6. **Routing (struts.xml)**

```xml
<action name="sdi-sincronizza-notifiche" class="SdiNotificheAction" method="sincronizzaNotifiche">
    <result name="success" type="json">
    <result name="error" type="json">

<action name="sdi-stato-notifica" class="SdiNotificheAction" method="getStatoNotifica">
    <result name="success" type="json">
    <result name="error" type="json">
```

### 7. **UI (list.jsp)**

#### Modifiche alla tabella fatture:
1. Aggiunta colonna "SDI Stato" tra "📧 Tracciamento" e "Azioni"
   - Mostra codice SDI ricevuto (es. `2602180948`)
   - Badge dello stato: 
     - 🔄 INVIATA (badge info - blu)
     - ✓ ACCETTATA (badge success - verde)
     - ✗ SCARTATA (badge danger - rosso)
     - ❌ ERRORE (badge danger - rosso)

2. Bottone "Invia XML a SDI" ora:
   - Disabilitato se `sdiCodice` è già valorizzato
   - Tooltip: "Già inviato a SDI"
   - Previene reinvii accidentali

## Workflow Completo

### 1. Invio Fattura a SDI (Fase 4)
```
Utente clicca "Invia XML a SDI" su Fattura REALE
↓
FattureAction.sendXmlSdi() chiama FatturaXMLService.generateFatturaXML()
↓
FatturaXMLService.sendToSdi() fa POST HTTP all'endpoint SDI con XML
↓
SDI risponde con: {"codice": "2602180948"} (es. timestamp yyMMddHHmm)
↓
FatturaXMLService.SdiResponse estrae il codice via regex
↓
FattureAction salva:
  - fattura.setSdiCodice("2602180948")
  - fattura.setSdiStato("INVIATA")
  - fatturaDAO.update(fattura)
↓
Utente vede messaggio: "XML inviato a SDI con successo. Codice: 2602180948"
↓
Bottone "Invia XML" viene disabilitato (già inviato)
```

### 2. Recupero Automatico Notifiche (Fase 5 - Cron)
```
Ogni 5 minuti (CronJobListener):
↓
SdiNotifichePollerTask.run() esegue sincronizzaTutteNotifiche()
↓
Per ogni Fattura con sdiCodice == "2602180948" e sdiStato == NULL:
  POST GET a: https://api.luna.itsolutions-cloud.com/ricevi-notifiche/index.php?codice=2602180948
  ↓
  SDI risponde con: <Notifica><Stato>ACCETTATA</Stato>...</Notifica>
  ↓
  SdiNotificheService.parseXmlResponse() estrae stato
  ↓
  Salva SdiNotifica nel DB:
    - sdiCodice: "2602180948"
    - stato: "ACCETTATA"
    - xmlRisposta: (intero XML di risposta)
  ↓
  Aggiorna Fattura:
    - fattura.setSdiStato("ACCETTATA")
    - fatturaDAO.update(fattura)
  ↓
UI list.jsp mostra:
  - Codice: 2602180948
  - Badge: ✓ ACCETTATA (verde)
```

### 3. Polling Manuale da Client (Opzionale)
```
Utente vuole controllare lo stato al momento:
↓
JavaScript fa GET a: /app/sdi-stato-notifica?fatturaId=123
↓
SdiNotificheAction.getStatoNotifica() ricerca la notifica nel DB
Se non trovata: chiama service.recuperaNotifica() per live query
↓
Se notifica trova, aggiorna letta=true
↓
Ritorna JSON con stato corrente
↓
UI aggiorna dynamicamente senza reload
```

## Configurazione Richiesta

### application.properties
```properties
# SDI Endpoint per invio fatture (Phase 4)
sdi.endpoint=https://api.luna.itsolutions-cloud.com/invia-sdi/index.php

# SDI Endpoint per recupero notifiche (Phase 5)
sdi.notifiche.endpoint=https://api.luna.itsolutions-cloud.com/ricevi-notifiche/index.php
```

### Frequenza Polling
- **Default**: 5 minuti (configurabile in `CronJobListener.POLLING_INTERVAL`)
- **Formato**: millisecondi
- **Modifica**: dopo il deployment, ricompilare e rideploy

## Database Schema

### Nuove Colonne in `fatture`
```sql
ALTER TABLE fatture ADD COLUMN sdi_codice VARCHAR(50);
ALTER TABLE fatture ADD COLUMN sdi_stato VARCHAR(20);
```

### Nuova Tabella `sdi_notifiche`
```sql
CREATE TABLE sdi_notifiche (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    fattura_id BIGINT NOT NULL,
    sdi_codice VARCHAR(50) NOT NULL,
    stato VARCHAR(20) NOT NULL,
    xml_risposta TEXT,
    data_ricezione TIMESTAMP,
    letta BOOLEAN DEFAULT FALSE,
    descrizione_errore TEXT,
    data_creazione TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (fattura_id) REFERENCES fatture(id)
);
```

## Testing

### Test Manuale
1. Creare Fattura REALE
2. Cliccare "Invia XML a SDI"
3. Verificare che:
   - Codice SDI sia salvato nel DB
   - Bottone "Invia XML" sia disabilitato
   - UI mostri lo stato "INVIATA"
4. Attendere 5 minuti (o eseguire manualmente GET `/app/sdi-sincronizza-notifiche`)
5. Verificare che:
   - Notifica sia stata salvata in `sdi_notifiche`
   - Stato della fattura sia aggiornato a "ACCETTATA" (o altro)
   - UI mostri il badge con nuovo stato

### Logging
- `FatturaXMLService`: "XML SDI inviato per fattura XXX - Codice SDI: ..."
- `SdiNotificheService`: "Notifica recuperata per fattura: ACCETTATA"
- `CronJobListener`: "Poller SDI avviato con frequenza 5 minuti"
- `SdiNotifichePollerTask`: "Polling notifiche SDI completato"

## Errori Noti & Risoluzione

| Errore | Causa | Soluzione |
|--------|-------|-----------|
| "HTTP 404" al GET notifiche | SDI endpoint non raggiungibile | Verificare URL in application.properties |
| "Codice non ricevuto" nella risposta | API SDI non ritorna campo "codice" | Controllare formato risposta API, adattare regex |
| Notifiche non sincronizzate dopo ore | Cron job non partito | Verificare log CronJobListener, riavviare app |
| Stato rimane "INVIATA" per sempre | Nessuna notifica da SDI | Controllare che fattura sia stata effettivamente elaborata da SDI |

## Note Tecniche

- **Thread Safety**: DAO apre nuove sessioni Hibernate per ogni operazione
- **Error Handling**: Errori nel polling cron non bloccano l'app, solo loggati
- **Retry**: Nessun retry automatico, ma sincronizzazione avviene ogni 5 minuti
- **Performance**: Ogni ciclo cron cerca solo fatture senza notifica (indexed query)
- **JSON Parsing**: Usato regex semplice (non libreria JSON) perché response potrebbe contenere XML

