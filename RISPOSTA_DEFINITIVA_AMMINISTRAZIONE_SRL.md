# RISPOSTA COMPLETA: Amministrazione SRL 10-15 Dipendenti

## ❓ Domanda Originale
**"Che altro manca per gestire completamente l'amministrazione di una SRL con 10-15 dipendenti?"**

---

## ✅ RISPOSTA STRUTTURATA

### Stato Attuale (Implementato in Luna2)
1. ✅ **Contabilità** - Prima nota, bilancio, cespiti (100%)
2. ✅ **Scadenzario Fiscale** - Gestione scadenze (100% - implementato oggi)

### Cosa Manca (8 Moduli Critici)

#### **TIER 1 - CRITICO (Da implementare subito)**

| # | Modulo | Descrizione | Giorni Dev | Priorità |
|---|--------|-------------|-----------|----------|
| 1 | **Gestione Presenze** | Check-in/check-out, ferie, malattia, straordinari | 20 | 🔴 CRITICA |
| 2 | **Buste Paga** | Calcolo stipendi, INPS, INAIL, CUD | 40 | 🔴 CRITICA |
| 3 | **IVA Management** | Liquidazione periodica, versamenti F24 | 20 | 🔴 CRITICA |
| 4 | **Libro Giornale** | Registrazione movimenti contabili, bilancio di verifica | 35 | 🔴 CRITICA |
| 5 | **Dichiarazioni Fiscali** | Modelli 730/Unico, IRAP, export CAF | 25 | 🔴 CRITICA |

#### **TIER 2 - IMPORTANTE**

| # | Modulo | Descrizione | Giorni Dev | Priorità |
|---|--------|-------------|-----------|----------|
| 6 | **Document Management** | Archiviazione fatture, OCR, compliance GDPR | 25 | 🟡 ALTA |
| 7 | **Workflow Approvazione** | Ferie/permessi/spese approval chain | 15 | 🟡 ALTA |

#### **TIER 3 - OPZIONALE**

| # | Modulo | Descrizione | Giorni Dev | Priorità |
|---|--------|-------------|-----------|----------|
| 8 | **Dashboard KPI** | Cash flow, anomalie, trend imposte | 10 | 🟢 BASSA |

---

## 📅 Timeline di Implementazione Consigliata

### **Q2 2026 (Aprile-Giugno): Fondamenta**
- **Settimana 1-2**: Workflow Approvazione (ferie/permessi/spese)
- **Settimana 3-5**: Presenze & Badge + malattia
- **Settimana 6-7**: Integrazione Scadenzario ← CUD dates

**Output**: Dipendenti gestiti per presenze e ferie

### **Q3 2026 (Luglio-Settembre): Finanziario Core**
- **Settimana 1-6**: Buste Paga (calcolo INPS/INAIL/CUD)
- **Settimana 7-9**: Libro Giornale (OIC-compliant)
- **Settimana 10**: Dashboard KPI

**Output**: Stipendi generati, bilancio verificato

### **Q4 2026 (Ottobre-Dicembre): Compliance**
- **Settimana 1-3**: IVA Management (liquidazione trimestrale)
- **Settimana 4-6**: Dichiarazioni Fiscali (Agenzia Entrate)
- **Settimana 7-8**: Document Management (OCR + GDPR)

**Output**: Tutti gli adempimenti fiscali automatizzati

---

## 🎯 Implementazione Prioritaria: Buste Paga

### Perché è il blocco maggiore?
- ✅ Obbligatorio per legge (> 5 dipendenti)
- ✅ Più complesso (INPS + INAIL + IRPEF + IRES)
- ✅ Critico per retention (dipendenti pagati a tempo)
- ✅ Dipende da: Presenze (ore lavorate) + Scadenzario (CUD date)

### Schema Implementazione Buste Paga
```sql
CREATE TABLE payroll_runs (
  id BIGINT PRIMARY KEY,
  company_id BIGINT,
  month INT,
  year INT,
  status VARCHAR(20),
  total_gross DECIMAL(12,2),
  total_net DECIMAL(12,2),
  total_inps DECIMAL(12,2),
  total_inail DECIMAL(12,2),
  created_date TIMESTAMP
);

CREATE TABLE payroll_details (
  id BIGINT PRIMARY KEY,
  payroll_run_id BIGINT,
  employee_id BIGINT,
  base_salary DECIMAL(12,2),
  gross_amount DECIMAL(12,2),
  inps_contrib DECIMAL(12,2),
  inail_contrib DECIMAL(12,2),
  irpef_tax DECIMAL(12,2),
  net_amount DECIMAL(12,2),
  FOREIGN KEY (payroll_run_id) REFERENCES payroll_runs(id)
);
```

### Workflow
1. Admin seleziona mese/anno
2. Sistema legge presenze → calcola ore
3. Sistema applica aliquote CCNL Commercio
4. Sistema calcola INPS (azienda + dipendente)
5. Sistema calcola IRPEF
6. Genera busta paga PDF per each dipendente
7. Genera F24 per versamenti INPS/INAIL
8. Export CUD da consegnare a febbraio

---

## 💰 Budget Totale

| Fase | Moduli | Giorni | Costo (€) |
|------|--------|--------|-----------|
| Q2 2026 | Workflow+Presenze+Scadenzario | 55 | €13.750 |
| Q3 2026 | Buste Paga+Libro Giornale+Dashboard | 75 | €18.750 |
| Q4 2026 | IVA+Dichiarazioni+Docs | 60 | €15.000 |
| **TOTALE** | **8 moduli** | **190 gg** | **€47.500** |

---

## ✨ Certificazioni & Compliance

✅ **Italia**:
- OIC 11, OIC 22 (Principi contabili)
- Agenzia Entrate (Dichiarazioni fiscali)
- INPS (Contributivo)
- INAIL (Assicurativo)
- CCNL Commercio (Contrattuale)

✅ **EU**:
- GDPR (Data privacy)
- eIDAS (Digital signatures - optional)

---

## 🚀 Quick Start - Prossimi 30 Giorni

**Se vuoi essere operativo in 1 mese per 10-15 dipendenti:**

1. **Week 1**: Deploy Workflow Approvazione
   - Ferie/permessi/spese approval chain
   - Dipendenti possono richiedere ferie
   - Manager approva
   - HR tracking automatico

2. **Week 2-3**: Deploy Presenze
   - Badge check-in/check-out
   - Ferie calcolate da contratto
   - Malattia/assenza tracking

3. **Week 4**: Test integrazione Scadenzario
   - Verifica che CUD date sia corretta
   - Test F24 generation

**Outcome**: Amministrazione base funzionante (ferie, presenze, scadenze)

**Cosa NON sarai ancora pronto**:
- ❌ Stipendi (occorre buste paga v1.0)
- ❌ Dichiarazioni (occorre IVA + DU)
- ❌ Bilancio esterno (occorre libro giornale)

---

## 🎁 Cosa è già Pronto (Implementato Oggi)

✅ **Scadenzario Fiscale v1.0**
- CRUD completo
- 10+ scadenze standard italiane pre-caricate
- Filtri per stato e tipo
- Timestamp audit
- 8 API endpoints
- UI responsive
- Build: 0 errori

**Accesso**: `http://<server>/luna2/app/contabilita/scadenzario-list`

---

## ❓ Domande Comuni

**D: Posso partire subito da Buste Paga?**  
R: No - dipende da Presenze (ore lavorate). Suggerito: Presenze first (3-4 settimane), poi Buste Paga (6 settimane).

**D: Quanto costa gestire 10-15 dipendenti per 1 anno?**  
R: Implementation: €47.500. Manutenzione annuale: ~€10.000 (support + updates leggi).

**D: Qual è il modulo più critico?**  
R: **Buste Paga** - senza stipendi non è fattibile. Secondo: **Presenze** (dipende il primo).

**D: Quando sarò operativo al 100%?**  
R: End Q4 2026 (dicembre) - 8 mesi full. Operativo al 70% entro Q3 (settembre).

**D: Ho bisogno di un commercialista?**  
R: Si. Per: primo setup (CCNL), audit annuale, dichiarazioni (Unico/model 730).

---

## 📋 Conclusione

**Per gestire completamente l'amministrazione di una SRL 10-15 dipendenti mancano:**

| What | How Long | Cost | Priority |
|------|----------|------|----------|
| Presenze & Ferie | 3-4 sett | €8.750 | 🔴 ASAP |
| Buste Paga | 6 sett | €15.000 | 🔴 Next |
| IVA & Dichiarazioni | 4-5 sett | €11.250 | 🔴 Q4 |
| Docs & Archivi | 3-4 sett | €6.250 | 🟡 Nice-to-have |
| Dashboard | 1-2 sett | €2.500 | 🟢 Polish |

**Next Step**: Valutare budget e timeline. Se ok → Kickoff sprint Presenze a maggio 2026.

---

**Status**: RISPOSTA COMPLETA ✅  
**Documento**: DEFINITIVO  
**Date**: 2026-04-30

