# 🚗 DEMO LIVE - Luna2 Broker Auto (Call 12:30)

> Questo documento contiene tutto quello che serve mostrare nella call

---

## 📊 SCENARIO DEMO IN 10 MINUTI

### Setup Rapido (Da eseguire PRIMA della call)

**Sul server di demo:**

```bash
# 1. Verifica database demo sia pronto
mysql -u root -p luna2_db < /workspaces/Luna2/database/luna2-noleggio-demo-data.sql

# 2. Accedi all'app
# URL: https://demo.gestionaleluna.it/
# User: admin / Password: admin123
```

---

## 🎬 FLOW DEMO (10 minuti)

### ⏱️ MINUTO 1-2: ACCESSO + OVERVIEW
```
1. Apri Luna2 in browser
2. Login: admin / admin123
3. Sidebar sinistra → "CRM Broker Auto"
4. Mostra DASHBOARD con 4 KPI in alto
```

**Screenshot aspettato:**
```
┌─────────────────────────────────────────┐
│ 🚗 CRM Broker Auto - Gestione Lead     │
├─────────────────────────────────────────┤
│ 📊 Preventivazione: 45 lead             │
│ 📋 Istruttoria: 23 lead                 │
│ ✅ Ordine: 12 ordini                    │
│ 💼 Post-Vendita: 156 clienti            │
└─────────────────────────────────────────┘
```

---

### ⏱️ MINUTO 3-4: SCENARIO #1 - NUOVO LEAD

**Azione:** Creare un nuovo lead clientealla ricerca di auto a noleggio

**Percorso:**
```
1. Clicca bottone "+ Nuovo Lead" (in alto a destra)
2. Compila form:
   - Nome: "Carlo Rossi"
   - Azienda: "Rossi SPA - Consulenza"
   - Telefono: "+39 333 456 7890"
   - Email: "carlo@rossispa.it"
   - Esigenza: "3-4 auto per flotta aziendale"
   - Budget: "€2.500 - €3.500/mese per auto"

3. Seleziona BRAND (multi-select):
   ☑ Volkswagen
   ☑ Peugeot
   ☑ Fiat

4. Click "Salva"
```

**Risultato:**
- Lead creato in stato **"PREVENTIVAZIONE"**
- Sistema genera email automatica di conferma
- Assegnato a broker (es: "Paolo Verdi")

---

### ⏱️ MINUTO 5-6: SCENARIO #2 - PROPOSTE AUTO

**Azione:** Aggiungere proposte auto al lead

**Percorso:**
```
1. Dal lead appena creato, sezione "+ Proposta Auto"
2. Aggiungi 3 auto:

   AUTO 1:
   - Model: VW Passat 2.0 TDI
   - Targa: "XX-222-AB" 
   - Prezzo noleggio: €2.800/mese
   - Durata: 36 mesi
   - Chilometri: 45.000 km/anno

   AUTO 2:
   - Model: Peugeot 3008 1.5 BlueHDi
   - Targa: "XX-333-CD"
   - Prezzo noleggio: €3.200/mese
   - Durata: 48 mesi
   - Chilometri: 40.000 km/anno

   AUTO 3:
   - Model: Fiat 500X 1.3 Multijet
   - Targa: "XX-444-EF"
   - Prezzo noleggio: €2.500/mese
   - Durata: 36 mesi
   - Chilometri: 45.000 km/anno

3. Click "Genera Preventivo"
```

**Sistema automaticamente:**
- Calcola costi totali per ogni opzione
- Applica sconti negoziali (se configurati)
- Genera PDF preventivo firmato
- Invia email al cliente con 3 proposte

---

### ⏱️ MINUTO 7-8: SCENARIO #3 - TRACKING & FOLLOW-UP

**Mostra funzionalità di follow-up automatico:**

```
1. Dal lead, sezione "Timeline Attività"
   - 📧 Email preventivo inviata: 14:32
   - 📞 Chiamata in follow-up: programmata domani 10:00
   - ⏰ Reminder: "Nessuna risposta in 48h → follow-up automatico"

2. Sezione "Note Broker": Aggiungi nota
   "Clienteinteressato al VW Passat. 
    Richiamerà lunedì con risposta definitiva."

3. Mostra automazione:
   - Se cliente apre email → status passa a "EMAIL_APERTA"
   - Se clicca link auto → status "LINK_CLICCATO"
   - Se non risponde in 48h → email reminder automatica
```

---

### ⏱️ MINUTO 9-10: SCENARIO #4 - CONVERSIONE IN ORDINE

**Azione:** Passare lead a "ORDINE" (simulare accettazione cliente)

**Percorso:**
```
1. Dal lead, click "Accetta Preventivo"
2. Sistema cambia stato: PREVENTIVAZIONE → ISTRUTTORIA
   (Il lead passa alla lista "Documenti da verificare")

3. Aggiungi documenti cliente:
   - ✅ Patente fronte/retro
   - ✅ Carta identità
   - ✅ Documento legalità IVA
   - ✅ Certificazione azienda (CCIAA)

4. Click "Documenti OK"
5. Stato cambia: ISTRUTTORIA → ORDINE
   - Email notifica cliente: "Ordine confermato!"
   - Genera contratto automatico
   - Mostra campo "Data Consegna": es 2026-05-10

6. Risultato finale:
   - Lead ora in lista "ORDINI ATTIVI"
   - Countdown verso consegna
   - Post-vendita tracking (manutenzione, rinnovi, ecc)
```

---

## 📌 DATI DEMO LIVE (Copy-Paste Pronti)

### Broker Lead #1 (Carlo Rossi)
```
Nome: Carlo Rossi
Azienda: Rossi SPA - Consulenza Fiscale
Tel: +39 333 456 7890
Email: carlo@rossispa.it
Esigenza: 3-4 auto flotta aziendale
Budget: €2.500 - €3.500/mese
Brand: Volkswagen, Peugeot, Fiat
Stato: PREVENTIVAZIONE
```

### Broker Lead #2 (Maria Bianchi) - GIÀ IN ISTRUTTORIA
```
Nome: Maria Bianchi
Azienda: Bianchi Srl - Consulenza
Tel: +39 334 567 8901
Email: maria@bianchisrl.it
Esigenza: 2 auto direzione + riunioni
Budget: €1.800 - €2.200/mese
Brand: Audi, BMW
Stato: ISTRUTTORIA (documenti in verifica)
Proposte: Audi A4 Avant (€2.100/mese), BMW 320i (€1.950/mese)
```

### Broker Lead #3 (Francesco Verdi) - GIÀ IN ORDINE
```
Nome: Francesco Verdi
Azienda: Verdi & Co Srl - Import/Export
Tel: +39 335 678 9012
Email: fverdi@verdieco.it
Esigenza: Flotta logistica (5 furgoni)
Auto selezionate: Fiat Ducato 2.3 diesel (x5)
Prezzo: €1.200/mese x 5 = €6.000/mese totale
Stato: ORDINE (contratti firmati, consegna 15 Maggio 2026)
Post-vendita: Tracking manutenzione programmata
```

---

## 🎯 TALKING POINTS DURANTE DEMO

### 1️⃣ **Gestione Completa del Ciclo Vendita**
"A differenza di foggli Excel sparsi, Luna2 Broker Auto centralizza:
- Lead → Preventivi → Istruttoria Documenti → Ordini → Post-vendita
- Tutto tracciato, niente si perde"

### 2️⃣ **Automazioni Intelligenti**
"Il sistema fa:
- Email automatiche di reminder (48h senza risposta)
- Tracking quando cliente apre email/clicca link
- Generazione automatica PDF preventivi
- Documenti digitali + firma e-commerce"

### 3️⃣ **Dashboard in Tempo Reale**
"Vedi subito: quanti lead in ogni fase, quanti ordini al mese,
revenue forecast basato su ordini confermati"

### 4️⃣ **Multi-Broker**
"Se la tua agenzia ha 5 broker, ciasc uno vede solo i propri lead
ma il manager vede statistiche aggregate di tutta l'agenzia"

### 5️⃣ **ROI: Time-Saving**
"Meno tempo su Excel/email, più tempo a fare vendite.
Gli studi mostrano +35% produttività con automazione"

---

## 🔗 LINK RAPIDI CALL

| Risorsa | Link |
|---------|------|
| **App Live** | `https://demo.gestionaleluna.it/` |
| **Guida Demo Completa** | [BROKER_AUTO_USER_GUIDE_DEMO.md](BROKER_AUTO_USER_GUIDE_DEMO.md) |
| **Status Implementazione** | [BROKER_AUTO_IMPLEMENTATION_STATUS.md](BROKER_AUTO_IMPLEMENTATION_STATUS.md) |
| **Credenziali** | admin / admin123 |

---

## ✅ CHECKLIST PRE-CALL (5 min prima)

- [ ] App Luna2 è UP e raggiungibile
- [ ] Database demo dati caricato (`luna2-noleggio-demo-data.sql`)
- [ ] Login funziona (admin/admin123)
- [ ] Sidebar mostra "CRM Broker Auto"
- [ ] Dashboard mostra KPI card
- [ ] Bottone "+ Nuovo Lead" è visibile
- [ ] Connessione internet stabile

---

## 🎥 SCREENSHOT MOCK (Per testare UI)

Se la demo ha lag/problemi, ti bastano questi mock per continuare:

```
DASHBOARD:
┌─────────────────────────────────────────┐
│ 🚗 CRM Broker Auto                      │
├──────┬──────┬──────┬──────────────────┤
│ 45   │ 23   │ 12   │ 156              │
│ PREV │ ISTR │ ORDI │ POST-VENDITA     │
└──────┴──────┴──────┴──────────────────┘

LEAD DETAIL:
┌─────────────────────────────────────────┐
│ Carlo Rossi - Rossi SPA                 │
│ Stato: PREVENTIVAZIONE                  │
│ Auto proposte: 3                        │
│ Follow-up: Lunedì 10:00                 │
│ Timeline:                               │
│  14:32 - Preventivo inviato ✓          │
│  Domani - Follow-up prog.               │
└─────────────────────────────────────────┘
```

---

**Pronto per la call! 🚀**

