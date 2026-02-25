# 🚗 Luna2 Broker Auto - Guida Completa + Scenari Demo

**Guida dettagliata per usare e dimostrare il sottomodulo Broker Auto di Luna2 a potenziali clienti.**

Questa guida include:
- Navigazione e UI overview
- 5 fasi del ciclo vendita (con esempi reali)
- Scenari demo con dati concreti
- Tips per presentare ai clienti
- Automation triggers e best practices

---

## 📋 Indice

1. [Panoramica Modulo](#panoramica-modulo)
2. [Accesso e Dashboard](#accesso-e-dashboard)
3. [Le 5 Fasi Principali](#le-5-fasi-principali)
4. [Scenario Demo #1: Lead Completo](#scenario-demo-1-lead-completo)
5. [Scenario Demo #2: Gestione Telefonica](#scenario-demo-2-gestione-telefonica)
6. [Scenario Demo #3: Post-Vendita](#scenario-demo-3-post-vendita)
7. [Best Practices & Tips](#best-practices--tips)
8. [Video Demo Script](#video-demo-script)

---

## 🎯 PANORAMICA MODULO

### Cos'è il Broker Auto?

Modulo completo per gestire il ciclo di vendita di **auto in noleggio a lungo termine (da 1 a 5 anni)**.

**Chi lo usa?**
- 🏢 Concessionari auto
- 🚗 Aziende noleggio
- 💼 Broker auto
- 📞 Call center vendite

**Obiettivo:**
Da lead freddo → Cliente attivo con contratto sottoscritto e consegna programmata → Follow-up post-vendita.

---

## 🏠 ACCESSO E DASHBOARD

### STEP 1: Accedi a Luna2

**URL:** `https://dominio-cliente.com/` 

```
Username: admin
Password: admin123 (cambiare subito!)
```

Dopo login → vedi dashboard principale con 5 moduli in basso.

---

### STEP 2: Entra nel Modulo Broker Auto

**Navigazione:**
```
Left Sidebar → [Moduli] → [Broker Auto] → [Noleggio Lead]
```

O accedi diretto: `https://dominio.com/luna2/app/noleggio/noleggioLead.action`

---

### STEP 3: Dashboard Overview

Vedi 4 KPI card in alto:

| KPI | Cosa Misura | Target |
|-----|-------------|--------|
| **Preventivazione** | Lead in fase quote/preventivo | 40+ lead/mese |
| **Istruttoria** | Lead in valutazione documenti | 20+ lead/mese |
| **Ordine** | Lead pronti a sottoscrivere | 10+ ordini/mese |
| **Post-Vendita** | Client attivi post-consegna | 100+ client |

---

## 🔄 LE 5 FASI PRINCIPALI

### Fase 1️⃣ - PREVENTIVAZIONE (Ricerca → Quote)

**Durata:** 0-7 giorni

**Cosa succede:**
- Lead entra via **form web**, **telefono**, **email**
- Broker propone modelli auto + prezzi
- Lead riceve preventivo via email/WhatsApp

**Actions:**
```
1. Crea lead
2. Seleziona auto/brand interessati
3. Calcola preventivo
4. Invia preventivo
5. Attendi risposta (auto-reminder a 48h)
```

**Automation:** ⏰ Se nessuna risposta dopo 48h → reminder email automatico

---

### Fase 2️⃣ - ISTRUTTORIA (Validazione Documenti)

**Durata:** 2-14 giorni

**Cosa succede:**
- Broker raccoglie documenti necessari
- Valuta situazione economica cliente
- ✅ Approva o ❌ Nega finanziamento

**Documents checklist:**
- ✅ Carta identità + conducenti aggiuntivi
- ✅ Patente A+B + CQC (se commerciale)
- ✅ Certificato storico dei sinistri
- ✅ Estratto conto bancario (ultimi 3 mesi)
- ✅ Bilancio azienda (se ditta)
- ✅ Dichiarazione CAF (se dipendente)

**Automation:** ⏰ Alert se documentazione incompleta dopo 4 giorni

---

### Fase 3️⃣ - ORDINE (Sottoscrizione Contatto)

**Durata:** 1-3 giorni

**Cosa succede:**
- Contratto sottoscritto digitalmente/manualmente
- ✅ Ordine confermato a fornitore
- 📊 Pianificazione consegna auto
- 🎫 Generazione documenti contabili

**Timeline:**
```
Firma contratto → +1-2 giorni → Auto disponibile
Auto disponibile → +3-5 giorni → In consegna al cliente
```

**Automation:** 
- 🔔 SMS di conferma ordine al cliente
- 📧 Email con dettagli consegna
- 📱 Link di tracciamento consegna

---

### Fase 4️⃣ - POST-VENDITA (Care Call + Ticket)

**Durata:** 2-36 mesi (tutta la noleggio)

**Cosa succede:**
1. **Care Call (20-30 giorni dopo consegna):**
   - Broker chiama cliente per verificare soddisfazione
   - Raccoglie feedback sulla vettura
   - Offre upgrade/accessori
   - Iscrive a programmi loyalty

2. **Ticket Support (ongoing):**
   - Cliente segnala problemi (guasto auto, assicurazione, etc.)
   - ⚙️ Auto-assignment a tecnico
   - 📞 Follow-up SLA (max 24h)
   - ✅ Risoluzione e chiusura

**Anti-Rimbalzo Automation:** 
- Se ticket riaperto 2+ volte → escalation a supervisore

---

### Fase 5️⃣ - SCADENZARIO (Renewal + Manutenzione)

**Durata:** Mesi 30-36 della noleggio

**Cosa succede:**
1. **Rinnovo Contratto:**
   - Alert 6 mesi prima scadenza
   - Proposta nuova auto
   - Rinnovo o chiusura

2. **Manutenzione Programmata:**
   - Reminder tagliandi (ogni 10k km o 12 mesi)
   - Ispezioni tecniche (anno 2, 3)
   - Sincronizzazione Google Calendar/iCloud

3. **Fine Contratto:**
   - Ritiro auto
   - Check-up finale (km, danni)
   - Saldo conguaglio manutenzione

---

## 📱 SCENARIO DEMO #1: LEAD COMPLETO

**Durata:** 5 minuti

**Scopo:** Mostrare intero ciclo vendita da lead → cliente attivo

**Preparazione:**
```
Nome Lead: Mario Rossi
Email: mario.rossi@email.com
Telefono: +39 328 1234567
Azienda: Costruzioni Rossi Srl
Bisogno: 3 auto Fiat Ducato per cantieri
Budget: €25,000-30,000/anno per auto
Timeline: Max 10 giorni
```

---

### STEP 1: Crea Nuovo Lead

**Path:** `Broker Auto → Noleggio Lead → [+] Nuovo Lead`

```
1. Clicca pulsante azzurro "+ NUOVO LEAD" in alto dx
2. Compila formulario:

   📋 DATI GENERALI
   ├─ Nome: Mario Rossi
   ├─ Email: mario.rossi@email.com
   ├─ Telefono: +39 328 1234567
   ├─ Azienda: Costruzioni Rossi Srl
   ├─ Ragione Sociale: Costruzioni Rossi Srl - via Roma 10, 20100 Milano
   └─ Provincia: MI

   🚗 AUTO RICERCATE
   ├─ Brand: Fiat
   ├─ Modello: Ducato
   ├─ Cilindrata: Diesel 2.3L
   ├─ N. Posti: 3
   └─ Business/Privato: Aziendale

   💰 BUDGET
   ├─ Budget Mensile: €2,200
   └─ Note: "3 auto per cantieri, consegna a Milano"
```

**Clicca:** "✅ Salva Lead"

---

### STEP 2: Assegna al Broker (Fase Preventivazione)

```
1. Lead creato → Status: PREVENTIVAZIONE (giallo)

2. Clicca [⋮ Menu] → "Assegna a..."
   Seleziona broker che lo contatterà
   (es: Paolo Verdi - Broker Auto Senior)

3. Automazione attivata:
   ✓ Email inviata a broker
   ✓ Reminder per contattare entro 24h
   ✓ Chat interna per coordinamento
```

---

### STEP 3: Broker Calcola Preventivo

```
1. Broker apre lead (come Paolo Verdi)

2. Clicca [📊 Calcola Preventivo]

   💰 INPUT:
   ├─ Auto: Fiat Ducato
   ├─ Tariffario: Noleggio 36 mesi
   ├─ Km annuali: 30,000 km
   ├─ Opzioni: Assicurazione Kasko + Manutenzione
   └─ Sconto: 5% dedotto (via contratto quadro)

   📈 OUTPUT:
   ├─ Lisino: €2,600/mese
   ├─ Sconto: -€130
   ├─ Netto: €2,470/mese per auto × 3 = €7,410/mese
   └─ Cauzione: €10,000

   ✅ CLICCA: "Invia Preventivo"
```

---

### STEP 4: Preventivo Inviato - Tracciamento

```
Status lead → PREVENTIVAZIONE con badge "Preventivo Inviato"

Inviato via:
✓ Email: mario.rossi@email.com (pdf allegato)
✓ SMS: "+39 328 1234567" (link online)
✓ WhatsApp: foto preventivo + link

Timeline visibile nel lead:
┌─────────────────────────────────┐
│ 🕐 10:15 - Preventivo inviato   │
│ 📧 A: mario.rossi@email.com     │
│ 👁 Aperto: 10:42               │
│ 📥 Click link: 10:45           │
└─────────────────────────────────┘

🔔 AUTO-REMINDER:
├─ +24h: Se nessuna risposta → Email reminder "Come va il preventivo?"
├─ +48h: Se ancora silenzio → Alert broker via SMS
└─ +72h: Se niente → Link WhatsApp personalizzato
```

---

### STEP 5: Lead Risponde (Accettazione)

```
Mario clicca link preventivo → Vede pagina online → Clicca "Accetto ✓"

Sistema automatico:
├─ Status lead cambia: ISTRUTTORIA (arancione)
├─ Email sent: "Grazie! Iniziamo raccolta documenti..."
├─ Broker notificato via SMS
├─ Task auto-creato: "Raccogli documenti Mario Rossi"
└─ Scadenza: 7 giorni

Broker vede in dash:
┌──────────────────────────────┐
│ 📊 LEAD MARIO ROSSI          │
│ Status: ISTRUTTORIA ⏳       │
│ ⚠️ Scadenza docs: tra 6 giorni│
│ Progressione: [████░░░░░░]  │
└──────────────────────────────┘
```

---

### STEP 6: Raccolta Documenti (fase Istruttoria)

**Path:** `Lead Mario Rossi → [📎 Allegati]`

```
Broker carica via drag-n-drop o clic:

✅ Uploading:
├─ Carta Identità Mario (pdf 2MB)
├─ Patente (pdf 1.8MB)
├─ Certificato Storico Sinistri (pdf 850KB)
├─ Estratto Conto Bancario 3 mesi (pdf 4.2MB)
├─ Bilancio Azienda 2023 (pdf 2.5MB)
└─ Dichiarazione CAF (pdf 3MB)

✓ Sistema verifica completezza:
  ✅ Tutti i doc caricati
  ✅ Passaporto auto tracciato
  ✅ Storico anagrafe crediti: OK
```

---

### STEP 7: Approvazione Finanziaria

```
1. Broker clicca [✅ Valuta] 

2. Compila form decisione:

   🏦 VALUTAZIONE FINANZIARIA
   ├─ Stato occupazione: Titolare ditta
   ├─ RAL/Fatturato: €120,000/anno ✓ IDONEO
   ├─ Credit score: 7/10 (buono)
   ├─ Esposizione banche: €35,000 (sostenibile)
   ├─ Cauzione proposta: €10,000 ✓
   └─ DECISION: ✅ APPROVATO

3. Note aggiuntive:
   "Ditta solida, documenti completi,
    finanziamento senza problemi.
    OK per procedera immediata."

4. Clicca: "✅ APPROVA"
```

---

### STEP 8: Transizione a Ordine

```
Sistema automatico:
├─ Email: "Ottima notizia! Il tuo finanziamento è approvato ✅"
├─ SMS: "Mario, accedi per firmare il contratto"
├─ Link: "Firma digitale contratto" (tramite DocuSign/Aruba)
├─ Status: ORDINE (blu) - "In attesa firma cliente"
└─ Alert broker: "Mario deve firmare entro 24h"

Dashboard mostra:
┌──────────────────────────────┐
│ 🔵 LEAD MARIO ROSSI          │
│ Status: ORDINE               │
│ ⏳ Firma pendente            │
│ Scadenza firma: Domani       │
│ [📝 Visualizza Contratto]    │
└──────────────────────────────┘
```

---

### STEP 9: Firma Digitale Contratto

```
Mario clicca link firma → Entra in DocuSign

Proces:
1. Legge contratto (3 pagine)
2. Inserisce pin inviatogli via SMS
3. Clicca "Firma ✓"
4. Complimento! Contratto sottoscritto

Sistema registra:
├─ Timestamp firma: 14:23
├─ IP sottoscrivente: 185.102.XX.XX
├─ Browser: Chrome v120
├─ Documenti firmati: 3/3 (100%)
└─ Status: ORDINE CONFERMATO
```

---

### STEP 10: Ordine Confermato - Consegna Programmata

```
Broker vede:
┌────────────────────────────────┐
│ 🟢 MARIO ROSSI - ORDINE OK!    │
│ Stato: ORDINE ✅               │
│ Contratto: Firmato 14:23       │
│ Data Consegna: 22 Gennaio      │
│ Luogo: Via Roma 10, Milano     │
│ [📅 Agenda Consegna]           │
│ [🚛 Traccia Consegna Auto]     │
└────────────────────────────────┘

Email cliente riceve:
"Complimenti Mario!
 ✅ Contratto confermato
 📅 3 auto in consegna: 22 Gennaio ore 9:00
 📍 Indirizzo: Via Roma 10, Milano
 🚗 Modelli: 3× Fiat Ducato 3.0L Diesel
 📞 Coordinatore: Paolo Verdi +39 328 9876543
 
 Clicca qui per tracciare le auto →"
```

---

### STEP 11: Consegna Auto

```
Paolo chiama Mario il giorno della consegna:

"Buongiorno Mario, sono Paolo!
 Abbiamo 3 Ducato arrivati a Milano.
 Ritiriamole oggi ore 9:00?
 Ti aspettiamo in via Roma 10."

Processo in app:
1. Paolo clicca [🚗 Registra Consegna]
2. Fotografie auto consegnate (3 foto)
3. Firma cliente su tablet (ricevuta)
4. Km iniziali registrati per auto
5. Cauzione prelevata: €10,000

Status → POST-VENDITA (verde)
```

---

### STEP 12: Post-Vendita + Care Call

```
⏰ 25 GIORNI DOPO CONSEGNA (automatico)

Sistema:
├─ Crea task: "Care Call Mario Rossi"
├─ Assegna a Paolo
├─ Programma: Domani ore 10-12
├─ Invia reminder SMS: "Domani care call ore 10"
└─ Prepara form follow-up

Paolo chiama:
"Ciao Mario, tutto bene con i Ducato?
 Ha funzionato bene? Qualche problema?
 [Ascolta risposte...]
 
 Per qualsiasi necessità, chiama diretto: +39 328 9876543"

Nel sistema registra:
├─ Soddisfazione: 8/10
├─ Problema riscontrato: Nessuno
├─ Suggerimenti cliente: Colorare insegne Azienda (fatto!)
├─ Proposta: Noleggio 5 furgoni aggiuntivi? No
└─ Status: Care Call Completato ✓
```

---

### STEP 13: Fase Scadenzario (Mesi 30-36)

```
🔔 6 MESI PRIMA SCADENZA CONTRATTO

Sistema automatico:
├─ Email: "Mario, il tuo contratto scade tra 6 mesi"
├─ Proposta: "Rinnovare? Abbiamo nuovi modelli..."
├─ Opzione 1: Rinnova stesso modello
├─ Opzione 2: Upgrade modello più nuovo
├─ Opzione 3: Termina e ritira auto
└─ Task: "Contatta Mario per renewal"

Paolo chiama:
"Mario, tra 6 mesi il contratto scade.
 Ti consiglio di rinnovare: i nuovi Ducato
 hanno consumi ridotti del 15%.
 Per tre auto: risparmi €300/mese!
 
 Mandami un sì entro 30 giorni?"

Risultato:
✓ Mario vuole rinnovare (nuovo ordine)
✓ Nuove auto ordinate a gennaio
✓ Data scadenza: 22 Dicembre
✓ Ritiro vecchie auto: 20 Dicembre
```

---

## 📞 SCENARIO DEMO #2: GESTIONE TELEFONICA

**Durata:** 3 minuti

**Scopo:** Mostrare come gestire lead che viene contattato perché ha chiamato

---

### Situazione:
Telefono squilla in centralino via VoIP integrato.

```
🔔 CHIAMATA IN ARRIVO: +39 334 5555555
   "Buonasera, cerco info sul noleggio auto"
```

---

### STEP 1: Risposta Chiamata + Nota Immediata

```
Operatore Paolo risponde:

"Buonasera, Luna2 Noleggio! Come posso aiutarla?"
"Vorrei sapere prezzi per 1 auto diesel, Milano"

Mentre parla:

1. Paolo clicca [📞 Esci da Telefono]
2. Apre Luna2 nuova tab
3. Clicca [+ NUOVO LEAD TELEFONICO]
4. Sistema pre-compila:
   - Numero: +39 334 5555555 (estratto da TAPI)
   - Ora contatto: 14:32
   - Fonte: Telefono
   - Nome: (da chiedere)
   
5. Paolo chiede: "Come si chiama?"
   Risposta: "Anna Ferrari"
   
6. Digita: "Anna Ferrari"
7. Continua conversazione...
```

---

### STEP 2: Chat Parallela con Broker

```
Paolo, mentre parla con Anna:

1. Apre chat interna Luna2
2. Scrivi a Marco (senior broker):

   "📞 Anna Ferrari chiama per Diesel
    Luogo: Milano
    Auto: 1x Auto
    Budget: ?
    Urgenza: Non ha detto
    
    La trasferisco a te? Sì/No"

    Marco risponde in real-time:
    "Si, portamela. Ho offerta speciale diesel"

3. Paolo: "Anna, ti trasferisco al nostro esperto?"
   Anna: "Sì"

4. Sistema registra TRANSFER in Luna2
5. Lead status: "In conference call con Marco"
```

---

### STEP 3: Lead Viene Imbucato

```
Marco prende la chiamata...

Nel sistema:
1. Click [Accetta Lead Anna Ferrari]
2. Calendar pop-up: "Quando richiamare se non risponde?"
   Marco seleziona: Martedì ore 15:00
3. Chat Paolo + Marco attiva (co-brokering)

Marco chiede:
"Anna, quanti giorni vi serve?
 36 mesi completo? Budget mensile?
 Documenti pronti?"

Anna risponde:
"36 mesi, massimo €800/mese,
 documenti li abbiamo"

Nel sistema Marco scrive:
```

---

### STEP 4: Passaggio Dati Automatico

```
Mentre Marco parla, gli dati fluiscono:

🔄 Dati auto-sincronizzati:
├─ Telefono: +39 334 5555555 (estratto TAPI)
├─ Nome: Anna Ferrari
├─ Azienda: [da chiedere]
├─ Email: [da chiedere]
├─ Auto ricercate: Diesel
├─ Budget: €800/mese
├─ Durata: 36 mesi
├─ Modalità raccolta: Chiama-e-raccogli
└─ Broker assegnato: Marco Rossi

Status: PREVENTIVAZIONE → Auto-assegnato Marco
Timeline avviata: "Inviare preventivo entro 24h"
```

---

### STEP 5: Chiusura Chiamata + Follow-up Automatico

```
Marco: "Perfetto Anna! Ti acquistiamo un preventivo
        e te lo mando via email/WhatsApp domattina ore 9.
        Ok?"

Anna: "Va bene, perfetto!"

Nel sistema Marco clicca:
[✓ Chiudi Chiamata - Salva Lead]

Automazioni attivate:
├─ Email inviata: "Piacere di averti conosciuto Anna"
├─ SMS: "Preventivo in arrivo domani ore 9:00"
├─ Task Marco: "Calcola preventivo Anna Ferrari" (Scadenza: domani)
├─ Alert: "Preventivo scadenza domani o lo mandi stasera?"
└─ Follow-up: Auto-reminder "Anna ha risposto?" tra 36h

Dashboard Marco:
┌──────────────────────────────┐
│ 📋 ANNA FERRARI              │
│ Status: PREVENTIVAZIONE      │
│ ⏳ Preventivo scade: Domani   │
│ Canale: Telefonico           │
│ [📧 Invia Preventivo]        │
│ [📱 Invia WhatsApp]          │
│ [📝 Memo Interno]            │
└──────────────────────────────┘
```

---

## 💼 SCENARIO DEMO #3: POST-VENDITA

**Durata:** 2 minuti

**Scopo:** Mostrare gestione ticket customer care post-consegna

---

### Situazione:
Cliente (Mario Rossi, 25 giorni dopo consegna) invia ticket:

```
📧 Email ricevuta:

Subject: "Problema freno anteriore sinistro"

"Ciao Paolo, una delle tre auto ha strani rumori
 quando freno forte. Può controllare?
 
 Targa: MI-500ABC, Ducato 1
 Km attuali: 2,450
 
 Urgenza: Media - continuo a usarla ma con cautela
 
 Grazie!"
```

---

### STEP 1: Ticket Automatico

```
Sistema Luna2:
├─ Riceve email
├─ Estrae dati:
│  ├─ Cliente: Mario Rossi
│  ├─ Auto: Ducato 1 (Targa MI-500ABC)
│  ├─ Problema: Freni
│  ├─ Urgenza: MEDIA
│  └─ Data richiesta: Oggi 10:15
├─ Status ticket: APERTO (rosso)
├─ Assegnazione: Auto-assegnata a tecnico
├─ SLA: Risposta entro 24h
└─ Task creato:

   🔧 TICKET #T47382
   ├─ Cliente: Mario Rossi
   ├─ Auto: Ducato 3.0L Diesel (MI-500ABC)
   ├─ Problema: Rumori freni anteriore sx
   ├─ Priority: MEDIUM
   └─ SLA: Risposta domani 10:15
```

---

### STEP 2: Broker Assegnato Agisce

```
Paolo (broker) riceve notifica:

🔔 NUOVO TICKET DI MARIO ROSSI
   Targa: MI-500ABC
   Problema: Freni
   SLA: Risposta domani

Paolo clicca ticket, vede:

┌─────────────────────────────┐
│ TICKET #T47382              │
│ 🟠 APERTO                   │
│ Cliente: Mario Rossi        │
│ Auto: Ducato 1 (KM: 2,450)  │
│ Problema: Rumori freni      │
│ Urgenza: Media              │
│                             │
│ [📞 Chiama Mario]           │
│ [🔧 Assegna Meccanico]      │
│ [✓ Risolvi Ticket]          │
│ [⚠️ Escalation]             │
└─────────────────────────────┘
```

---

### STEP 3: Azione Rapida

```
Paolo clicca [🔧 Assegna Meccanico]

Popup:
┌──────────────────────────────┐
│ Seleziona Officina            │
│ ○ Officina A (Milano) - 2 km  │ ← Proposta più vicina
│ ○ Officina B (Milano) - 5 km  │
│ ◉ Officina C (Monza) - 8 km   │
│                              │
│ Note: "Controllo freno DX sx  │
│        20 minuti. Urgente!"   │
│                              │
│ [✅ Assegna] [Cancella]      │
└──────────────────────────────┘

Paolo seleziona Officina A (la più vicina)
└─ Assegna

Sistema automatico:
├─ Email Officina A: "Mario Rossi + auto in arrivo"
├─ SMS Mario: "Officina A vi contatterà tra 5 min"
├─ Status ticket: ASSEGNATO (blu)
├─ SLA clock reset: "Officina ha 4h per risolvere"
└─ Chat aperta Paolo ↔️ Officina A
```

---

### STEP 4: Customer Info View

```
Paolo vede nel ticket:

📊 MARIO ROSSI - STORICO:
├─ ✅ Lead creato: 10 Novembre
├─ ✅ Preventivo inviato: 11 Novembre
├─ ✅ Documenti approvati: 20 Novembre
├─ ✅ Contratto firmato: 21 Novembre
├─ ✅ Auto consegnate: 22 Novembre
├─ ✅ Care call: 8 Dicembre (8/10)
├─ ⚠️ Primo ticket: OGGI (freni)
└─ 📈 Sentiment: Positivo (da care call)

📞 CONTATTI:
├─ Principale: Mario +39 328 1234567
├─ Secondario: Sig.ra Rossi +39 335 9876543
└─ Email: mario.rossi@email.com

💳 FINANZIAMENTO:
├─ Status: Pagamenti OK (30 giorni)
├─ Missing: Nessuno
└─ Prossimo: 22 Gennaio
```

---

### STEP 5: Risoluzione + Anti-Rimbalzo

```
📱 SMS Officina A → Mario:
"Buonasera Mario, Officina A di Milano.
 Abbiamo ricevuto la richiesta.
 Possiamo controllare il freno domani
 ore 9:30? Ci mette ~20 minuti.
 Rispondi Sì/No per conferma"

Mario risponde: "Sì OK ore 9:30"

Giorno dopo - Officina risolve freno, invia report:
"Pastiglie anteriori leggermente sporche di polvere.
 Pulite + lubricate. Freni ok. Test drive positivo.
 Nessun costo (garanzia noleggio)."

Paolo aggiorna ticket:
└─ Click [✅ Risolvi Ticket]

Form chiusura:
┌────────────────────────────────┐
│ ✅ TICKET RISOLTO              │
│ Causa: Polvere pastiglie       │
│ Soluzione: Pulizia-lubrificazione
│ Costo: €0 (Garanzia)           │
│ Soddisfazione cliente: 5/5     │
│ Note: "Grazie per velocità!"   │
│                                │
│ [✅ Chiudi Ticket]             │
└────────────────────────────────┘

Sistema registra:
├─ Status: CHIUSO ✅
├─ Tempo risoluzione: 4.5 ore
├─ SLA rispettato: SI ✓
├─ Soddisfazione: Alta
└─ Automatica: Update lead Mario a POST-VENDITA ✓

🔔 ANTI-RIMBALZO CHECK:
   "Ticket chiuso 18 Dicembre.
    Se Mario lo riapre entro 72h → Alert escalation"
```

---

## 🎯 BEST PRACTICES & TIPS

### Per Broker/Venditori:

**1️⃣ Gestione Lead Telefonica**
```
✅ DO:
- Registra il lead DURANTE la telefonata
- Chiedi email + cellulare (non solo nome)
- Spiega timeline prezzo (24-48h preventivo)
- Prendi impegno: data precisa di risposta cliente

❌ DON'T:
- Promesse irrealistiche (es: "Domani auto")
- Dimenticarsi di inviare preventivo entro 24h
- Ignorare reminder automatici del sistema
```

**2️⃣ Fase Istruttoria (Raccolta Documenti)**
```
⏰ TIMELINE STANDARD:
- Giorno 1: Lead accetta preventivo
- Giorni 2-4: Raccoglie documenti
- Giorno 5: Broker valuta, approva/nega
- Giorno 6-7: Firma contratto
- Giorno 8-15: Consegna auto

🚀 ACCELERATORI:
- Invia link upload prima di telefonare
- Predisposizioni cartelle per doc type
- Reminder automatico dopo 48h inattività
```

**3️⃣ Fase Ordine (Contratto)**
```
✓ Segui questi step:
1. Documenti completati + approvati
2. Crea ordine nel sistema
3. Genera documento contratto
4. Invia link firma (via DocuSign)
5. Attendi firma cliente
6. Sistema auto-verifica scansioni
7. Registra data consegna auto
8. Richiedi cauzione (se richiesta)
9. Passa a POST-VENDITA

⚠️ ATTENZIONE:
- Se cliente non firma entro 24h → SMS reminder
- Se non firma entro 48h → Telefonata
- Se non firma entro 72h → Cancella ordine (auto a riserva)
```

**4️⃣ Care Call (Fondamentale!)**
```
🎯 TIMING: 25 giorni dopo consegna (obbligatorio)

📋 CHECKLIST DOMANDE:
1. "Auto arrivata ok? Consegna andata bene?"
2. "Qualche problema tecnico finora?"
3. "Modello soddisfa esigenze?"
4. "Istruzioni d'uso tutte chiare?"
5. "Hai domande su manutenzione?"
6. "Vogliamo aggiungere auto? Nuovi modelli?"
7. "Conosci numero emergenze 24/7?"
8. "Quando prossimo check-up gratuito?"

📊 REGISTRA NEL SISTEMA:
- Soddisfazione 1-10
- Problemi riscontrati
- Upgrade proposti
- Prossimo contatto

💡 TIP: Questa call riduce churn del 40%!
```

### Per Demo a Clienti:

**1️⃣ Show dell'UI (1 minuto)**
```
Mostra:
- Dashboard centrale con KPI
- Mappa mentale delle 5 fasi (colori diversi)
- Icone intuitive per azioni rapide
- Responsive design (desktop + mobile)
```

**2️⃣ Workflow Lead-to-Customer (3 minuti)**
```
Scorri:
- Crea lead (drag-n-drop form)
- Calcola preventivo (automatico in 5 sec)
- Invia via multi-canale (email+SMS+WhatsApp)
- Tracking response (vedi se aperto, quando)
- Automazioni (reminder 24h, 48h)
- Status flow: PREVENTIVAZIONE → ISTRUTTORIA → ORDINE → POST-VENDITA
```

**3️⃣ ROI/Numeri (1 minuto)**
```
Before Luna2:
- 30 lead/mese contactati via spreadsheet
- 15% perduti per follow-up dimenticati
- 7 giorni per compleat ordine
- Gestione telefonica caotica

After Luna2:
- 60-80 lead/mese trattabili
- Perso <1% (reminder automatici)
- 3 giorni per completare ordine
- Centralizzazione telefonica + chat

💰 RISULTATO:
- 2-3x conversion rate aumentata
- 4-5x lead processabili contemporaneamente
- ROI software recuperato in 2-3 mesi
```

---

## 🎬 VIDEO DEMO SCRIPT

**Durata totale:** 5 minuti (registrare con OBS Studio o simile)

---

### SCENE 1: INTRO (30 secondi)

```
[SCHERMO]: Luna2 Dashboard - Noleggio Auto
[VOCE OVER]:
"Se gestisci vendite di auto a noleggio,
 conosci il problema: lead, documenti,
 follow-up sparsi in 5 applicazioni diverse.

Oggi ti mostro come Luna2 Broker Auto 
centralizza TUTTO in una sola app."

[CLICK]: Entra modulo Broker Auto
```

---

### SCENE 2: NUOVO LEAD (60 secondi)

```
[AZIONE]: Clicca "+ Nuovo Lead"
[POP-UP]: Form creazione lead si apre

[VOCE OVER]:
"Passo 1: Creare un lead è istantaneo.
 Nome, email, telefono, auto ricercate,
 budget - tutto qui."

[RIEMPIMENTO RAPIDO]:
Nome: Lorenzo Verdi
Email: lorenzo@email.com
Auto: BMW 320d
Budget: €1,200/mese

[VOCE OVER]:
"Passo 2: Luna2 calcola il preventivo
 in base a tariffario e opzioni..."

[CLICK]: "Calcola Preventivo"
[AUTOMATICO]: Preventivo appare
Netto: €1,248/mese (sconto 5% = -€62)

[VOCE OVER]:
"Perfetto. Inviamo subito?"

[CLICK]: "Invia Preventivo"
[POPUP]: Sceglie: Email + SMS + WhatsApp

[SYSTEM MESSAGE]: "✓ Preventivo inviato a Lorenzo - 14:32"
```

---

### SCENE 3: TRACKING RESPONSE (45 secondi)

```
[VOCE OVER]:
"Qui la magia: vedi in real-time
 se il cliente ha letto il preventivo,
 quando, da che dispositivo."

[SCROLL]: Timeline nel lead mostra:
- 14:32 Preventivo inviato
- 14:33 Email aperta (Apple Mail)
- 14:35 Click link online

[AZIONE]: Pagina online si apre
Lorenzo vede preventivo interattivo

[VOCE OVER]:
"Lorenzo legge dettagli, clicca 'Accetto',
 e automaticamente:"

[RAPID FIRE]:
- ✓ Status da PREVENTIVAZIONE → ISTRUTTORIA
- ✓ Email inviata a Lorenzo: "Iniziamo documenti"
- ✓ SMS al broker: "Lorenzo ha accettato!"
- ✓ Task creato per broker: "Racc. documenti"
- ✓ Scadenza fissata: 7 giorni
- ✓ Reminder automatico: Domani ore 9
```

---

### SCENE 4: DOCUMENTI (60 secondi)

```
[VOCE OVER]:
"Passo 3: Raccolta documenti
 in una sola piattaforma.
 Lorenzo accede al link,
 upload drag-n-drop."

[ACTION]: Mostra upload form

[VOCE OVER]:
"Luna2 riconosce il tipo di documento,
 lo catalogizza automaticamente..."

[UPLOAD ANIMATION]:
- Carta ID (✓ riconosciuta)
- Patente (✓ verificata)
- Storico sinistri (✓ ok)
- Estratto banca 3m (✓ ok)
- Bilancio azienda (✓ scansione 2023)

[VOCE OVER]:
"Sistema verifica: mancano documenti?
 SÌ = allerta al broker.
 NO = Automaticamente passa alla fase
 approvazione finanziaria."

[DASHBOARD]: "✓ Documenti completi"

[VOCE OVER]:
"Il broker ha tutta l'info servita,
 da una sola dashboard..."
```

---

### SCENE 5: APPROVAZIONE (45 secondi)

```
[VOCE OVER]:
"Passo 4: Valutazione finanziaria.
 Broker vede subito:"

[SHOW]:
- Status finanziario ✓
- RAL/Fatturato ✓
- Storico pagamenti ✓
- Credit score: 7/10 (BUONO)

[VOCE OVER]:
"Un solo click per approvare:"

[CLICK]: "✅ APPROVA"

[AUTOMATIC CASCADE]:
- Email Lorenzo: "Ottima notizia! ✅"
- SMS: "Lorenzo, vieni a firmare?"
- Link DocuSign creato automaticamente
- Status: ORDINE (in firma)
- SLA reminder: "Lorenzo deve firmare entro 24h"

[VOCE OVER]:
"Lorenzo accede, legge, firma digitale,
 e il contratto è sottoscritto."
```

---

### SCENE 6: POST-VENDITA (60 secondi)

```
[VOCE OVER]:
"Passo 5: Post-vendita non è la fine,
 è l'inizio della relazione."

[CALENDAR VIEW]:
[VOCE OVER]:
"Automaticamente, 25 giorni dopo
 consegna, Luna2 programma
 la 'Care Call':"

[TIMELINE]:
- Giorno 1: Auto consegnata
- Giorno 25: ⏰ TASK "Care Call Lorenzo"
- Sistema: SMS a Lorenzo "Paolo ti chiama domani?"
- Sistema: SMS a broker "Care call con Lorenzo domani 10-12"

[VOCE OVER]:
"Paolo chiama Lorenzo, chiede feedback,
 propone upgrade di accessori o auto aggiuntive.
 Registra il tutto nel sistema."

[FORM]: Care call completato con rating 9/10

[VOCE OVER]:
"Da quel momento, ogni 2-3 mesi,
 Luna2 ricorda di contattare Lorenzo.
 Ticket di manutenzione vengono gestiti
 automaticamente."

[FINAL DASHBOARD]:
Mostra Lorenzo in verde, POST-VENDITA,
con prossimi task visibili.

[VOCE OVER]:
"Da lead a cliente felice, completamente tracciato,
 completamente automatizzato.
 Questo è Luna2 Broker Auto."
```

---

### SCENE 7: NUMERI FINALE (30 secondi)

```
[TEXT OVERLAY]:
"Con Luna2 Broker Auto:"

📈 2-3x lead processabili contemporaneamente
⏱️ 50% riduzione tempo ordine (7g → 3g)
📞 100% dei follow-up automatizzati
💰 40% riduzione churn post-vendita

[SLIDE]:
"Pronto a trasformare il tuo processo?"

[CALL-TO-ACTION]:
"Prova gratis: demo.lunasoftware.com"

[END FRAME]:
Logo + contatti support
```

---

## 📞 COME USARE QUESTO SCRIPT

1. **Registrare locale:**
   - Apri Luna2 in full screen
   - Usa OBS Studio (gratuito) per screen capture
   - Registra voce parallela (USB mic)
   - Esporta in MP4

2. **Condividere con cliente:**
   - Carica su YouTube (non listato)
   - Oppure su Google Drive + condividi link
   - Oppure email diretto (se file non troppo grande)

3. **Personalizzare:**
   - Sostituisci nomi/dati con quelli cliente
   - Cambia URL demo con dominio vero
   - Aggiungi logo/branding cliente

---

## 🎁 BONUS: Quick Scripts Utilizzabili

### Script Call di Vendita (5 min):

```
"Ciao! Io sono Paolo, broker auto di Luna2.

Vedo che gestisci 20-30 lead al mese:
- Alcuni in email
- Alcuni in WhatsApp
- Alcuni in CSV su Excel
- Uno che non rispondi perché dimenticato 😅

Immagina se TUTTI fossero in una sola app,
automaticamente tracciati, con reminder,
e preventivi inviati al clic di un pulsante.

Questo è Luna2 Broker Auto.

Esempi concreti:
- Lead chiama ore 10 → 10 min dopo
  preventivo inviato
- Lead legge preventivo → Notifica real-time
- 48h passate → SMS reminder automatico
- Accetta → Documenti caricati → Check approvazione
- Approvato → Firma digitale → Auto consegnata
- 25 giorni dopo → Care call automatica
- Ticket manutenzione → Risolto senza toccare

Risultato: Aumenti conversioni perché nessuno
si perde, i tempi si dimezzano, il cliente
è sempre sereno.

Quanto siete interessati?"
```

### Script Risposta Obiezioni:

**Obiezione: "È complicato da usare?"**
```
"Tutt'altro. Fatto. Dipende da voi:
- Operatore parte da zero? 30 minuti training
- Broker esperto? 5 minuti
- Clienti? Ricevono link WhatsApp, cliccano,
  fatto. Non conoscono neanche Luna2."
```

**Obiezione: "Quanto costa?"**
```
"€499/mese per 3 broker fino a 200 lead/mese.
A lead questo sono €20 al mese.
Di solito ripaghi in 2-3 mesi
perché aumenti conversione del 30%."
```

**Obiezione: "E se il cliente non vuole?"**
```
"Allora non usa. Ma il 95% dei clienti
apprezzia perché è più veloce, più trasparente.
Vedono il preventivo online, monitorano
lo stato dell'ordine, chiamano meno perché
hanno info in tempo reale."
```

---

## ✅ Checklist Pre-Demo

Quando farai demo a cliente:

- [ ] Connessione internet stabile (fai prima test)
- [ ] Audio/microfono controllato
- [ ] Screen at 125% zoom (leggibile da distance)
- [ ] Finestra browser maximizzata
- [ ] 2-3 lead fake preparati in precedenza
- [ ] Numeri concreti (lead/mese, time2order, etc.)
- [ ] Cliente non ha distrazioni (silenzio ufficio)
- [ ] Hai call di follow-up schedulato per dopo

---

**Fine guida! Buona demo! 🎯**
