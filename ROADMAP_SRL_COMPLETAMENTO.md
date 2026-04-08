# 🎯 SRL 10-15 DIPENDENTI - ROADMAP COMPLETAMENTO LUNA2

**Data Analisi**: Aprile 2026  
**Stato Attuale**: Contabilità 100% + Scadenzario Fiscale 100% ✅  
**Prossimo**: Human Resources & Payroll

---

## 📊 MATRICE COMPLETAMENTO MODULI

| # | Modulo | Priorità | Status | Implementazione | Impatto Immediato |
|---|--------|----------|--------|---|---|
| 1 | Contabilità (Prima Nota + Bilancio + Cespiti) | 🔴 CRITICA | ✅ 100% | ✅ DONE | ✅ Operativo |
| 2 | **Scadenzario Fiscale** | 🔴 CRITICA | ✅ 100% | ✅ DONE (Apr 2026) | ✅ Operativo |
| 3 | Presenze & Timesheet | 🔴 CRITICA | ❌ 0% | 4-6 settimane | Badge + Ferie |
| 4 | Buste Paga/Stipendi | 🔴 CRITICA | ❌ 0% | 6-8 settimane | Dipendenti pagati |
| 5 | Dichiarazioni Fiscali (F24/CUD/UNICO) | 🔴 CRITICA | ❌ 0% | 4-6 settimane | Adempimenti legali |
| 6 | Workflow Approvazione | 🟡 ALTA | ❌ 0% | 2-3 settimane | Ferie/Permessi/Spese |
| 7 | Tesoreria & Riconciliazione Bancaria | 🟡 ALTA | 🟠 30% | 3-4 settimane | Cash flow visibility |
| 8 | Bilancio Sistematizzato (SP + CE) | 🟡 MEDIA | 🟠 40% | 2-3 settimane | Gestione equilibrio |
| 9 | HR Analytics & Dashboard | 🟢 BASSA | ❌ 0% | 2-3 settimane | Metriche dipendenti |
| 10 | Audit Trail Completo | 🟢 BASSA | 🟠 50% | 1-2 settimane | Tracciabilità |

---

## 🚀 ROADMAP IMPLEMENTAZIONE CONSIGLIATA

### **FASE 1 - Q2 2026 (Aprile-Giugno) - Foundation**

#### Sprint 1: Workflow Approvazione (Settimane 1-2)
**Why First?** Facile, nessuna complessità di calcolo, impatto immediato per team operations  
**What**: 
- Richieste ferie/permessi → Approvazione manager → Tracking automatico
- Richieste spese → Submission → Validazione → Rimborso
- Stati: DRAFT → SUBMITTED → APPROVED/REJECTED → COMPLETED

**Dipendenze**: Auth già presente (user.role)  
**Team Impact**: Subito operativo per 10-15 dipendenti  
**Effort**: 3-4 gg dev

#### Sprint 2: Presenze & Badge (Settimane 3-4)
**Why**: Foundational per timesheet e buste paga (servono ore lavorate)  
**What**:
- Badge check-in/check-out (manuale + QR scan future)
- Ferie (giorni disponibili auto-calcolati da contratto)
- Permessi (gestiti via Workflow v1)
- Malattia/assenza (certificati digitali)
- Straordinari (tracciamento ore extra)

**Struktura DB**: 
```sql
CREATE TABLE presenze (
  id, user_id, data_checkin, data_checkout, 
  tipo_assenza (FERIE/PERMESSO/MALATTIA/NORMALE),
  ore_lavorate, straordinari, note
);
```

**Dipendenze**: Nessuna (standalone)  
**Team Impact**: Visibilità orari, base per calcolo stipendi  
**Effort**: 4-5 gg dev + 1 gg QA

#### Sprint 3: Tesoreria Baseline (Settimane 5-6)
**Why**: Preparazione per dichiarazioni F24, visione cash flow  
**What**:
- Import estratti conto bancari (CSV/OFX)
- Riconciliazione fatture con movimenti
- Saldo cassa in tempo reale
- Report flussi di cassa (mensili)
- Alert deficit cassa < X€

**Dipendenze**: Fatture (già presenti)  
**Info Entry**: Accesso bancario o file manuale  
**Effort**: 3-4 gg dev

---

### **FASE 2 - Q3 2026 (Luglio-Settembre) - HR Core**

#### Sprint 4: Buste Paga v1 (Settimane 1-3)
**Why**: Dipendenti devono essere pagati - CRITICO  
**What**:
- Template contratti (scadenza esercizio italiano)
- Calcolo automatico lordo → netto con ritenute IRPEF
- Integrazione co.ni.p.i. per aliquote previdenziali
- Gestione CUD automatico (da presenze + stipendi)
- Export per bonifici + tracciamento pagamenti

**Formule Italiane**: 
- Lordo = (Giorni lavoro × Stipendio giornaliero) + Bonus + TFR accantonato
- Netto = Lordo - Ritenuta Irpef (scaglioni 2026) - Contributi Previdenziali - Sindacale

**Critical Fields**:
- IBAN dipendenti, CUD data, giorni ferie residui, tredicesima accantonata

**Dipendenze**: Presenze (ore), Scadenzario (CUD date), Contabilità (posting automatico)  
**Effort**: 6-7 gg dev (complesso calcoli fiscali) + legale review

#### Sprint 5: Dichiarazioni Fiscali F24/CUD (Settimane 4-5)
**Why**: Obbligatorie per legge entro scadenze (vedi Scadenzario)  
**What**:
- F24 automatico: INPS (contributi) + IVA (versamenti mensili) + IRPEF (ritenute dipendenti)
- CUD generation (Certificazione Unica Dipendenti) da buste paga
- Export XML per trasmissione telematica AdE
- Tracking stato trasmissione (AccettatoAdE / Rigettato / Pendente)

**Integration**: Agenzia Entrate Web Services (SSL certificate required)  
**Dipendenze**: Buste Paga v1, Contabilità  
**Effort**: 5-6 gg dev + AdE compliance review

---

### **FASE 3 - Q4 2026 (Ottobre-Dicembre) - Analytics & Polish**

#### Sprint 6: Bilancio Sistematizzato (Settimane 1-2)
**What**:
- Stato Patrimoniale automatico (da conti + cespiti ammortizzati)
- Conto Economico (ricavi - costi per struttura gestionale)
- Ratei e risconti auto (già in Contabilità, serve UI sistematizzato)
- Deposito in Camera di Commercio (integrazione portale)
- Export PDF/XML bilancio

**Dipendenze**: Contabilità + Cespiti  
**Effort**: 3-4 gg dev

#### Sprint 7: HR Analytics Dashboard (Settimane 3-4)
**What**:
- KPI dipendenti: turnover, costi massimi, assenze medie, produttività
- Trend ferie/permessi per anno
- Costo lordo total vs budget
- ROI dipendente (ricavi generati / costo)

**Charts**: Chart.js (già integrato in Luna2)  
**Effort**: 2-3 gg dev

---

## 📋 DETAILED REQUIREMENTS PER FASE

### PRESENZE & TIMESHEET - Tech Spec

**Entity: Presenza**
```java
@Entity
public class Presenza {
    Long id;
    User dipendente;
    @Temporal(DATE)
    Date data;
    LocalTime ora_checkin;     // Es: 09:00
    LocalTime ora_checkout;    // Es: 18:00
    BigDecimal ore_lavorate;   // Calcolato auto, default 8h
    
    @Enumerated
    TipoAssenza tipo;  // NORMALE, FERIE, PERMESSO, MALATTIA, STRAORDINARIO_FUORI_ORARIO
    
    String certificato_medico_url;  // Per malattia
    String note;
    User approved_by;  // Manager che approva
    
    @Temporal(TIMESTAMP)
    Date created_at, updated_at;
}

enum TipoAssenza {
    NORMALE, FERIE, PERMESSO, MALATTIA, MATERNITA, INFORTUNIO
}
```

**DAO Methods**:
- `List<Presenza> findByUser(User u, Date da, Date a)`
- `List<Presenza> findFeriePending(User u)` 
- `BigDecimal sumOreStraordinarieMese(User u, int mese)`

**UI**: Calendario mensile tipo Google Calendar  
**Workflow**: Dipendente richiede → Manager approva/rifiuta

---

### BUSTE PAGA (STIPENDI) - Tech Spec

**Entity: BustaPaga**
```java
@Entity
public class BustaPaga {
    Long id;
    User dipendente;
    Integer mese, anno;  // 2026-04
    
    // Lordo
    BigDecimal stipendio_lordo;
    BigDecimal bonus_mensile;
    BigDecimal tfr_accantonato;  // Trattamento fine rapporto
    
    // Ritenute
    BigDecimal ritenuta_irpef;  // Calcolata con scaglioni
    BigDecimal contributi_previdenziali;  // %
    BigDecimal sindacale;
    
    // Netto
    BigDecimal netto_bancario;  // Quello che riceve
    
    // Stato
    @Enumerated
    StatoBusta stato = BOZZA;  // BOZZA, APPROVATA, PAGATA, RIFIUTATA
    
    Date data_pagamento_prevista;
    Date data_pagamento_effettiva;
    String iban_destinazione;
    String numero_mandato;  // Riferimento bonifico
    
    String note;
}

enum StatoBusta {
    BOZZA, APPROVATA, PAGATA, STORNATA, RIFIUTATA
}
```

**Logic**:
```
lordo_totale = stipendio_lordo * giorni_presenze_mese / 26 + bonus + tfr_accant
irpef = findAliquota(anno_fiscale).applica(lordo_totale)
contributi = lordo_totale * 0.0919  // 2026
netto = lordo_totale - irpef - contributi - sindacale
```

**Integration**: 
- Automatico da Presenze (giorni effettivi lavoro)
- Posting automatico a Contabilità (conto 4100 "Salari e stipendi", conto 2100 "Debiti verso dipendenti")

**UI**: Form template + tabella mese + PDF export + bonifico export

---

### DICHIARAZIONI FISCALI - Tech Spec

**F24 Automatico**:
- Legge Scadenzario → data F24 (es 20 aprile)
- Legge Buste Paga → calcola IRPEF ritenuto + Contributi INPS totali mese
- Legge Contabilità → calcola IVA dovuta (ricavi - bonus sconto - acq. UE)
- Genera XML F24 standard telematico
- Mantiene storico trasmissioni (timestamp, risposta AdE)

**CUD (Certificazione Unica Dipendenti)**:
- Auto-generato da BustaPaga per anno (es. CUD 2025 consegnato entro 31 gen 2026)
- Incluso: lordo totale, ritenute, contributi, assenze, ferie residue
- Export XML + PDF stampa con firma CFO

**Dipendenze verso Moduli**:
- Scadenzario: per date filing
- Presenze: per verifich giorni lavorati
- Buste Paga: per importi trattenuti
- Contabilità: per IVA dovuta

---

## 💰 STIMA BUDGET & TEMPO

| Fase | Settimane | Dev Hours | QA Hours | Totale | Costo (€/ora workshop) |
|------|-----------|-----------|----------|--------|--------|
| 1. Workflow + Presenze + Tesoreria | 6 | 100h | 20h | 120h | €12k |
| 2. Buste Paga + Dichiarazioni | 5 | 90h | 18h | 108h | €10.8k |
| 3. Bilancio + HR Analytics | 4 | 50h | 10h | 60h | €6k |
| **TOTALE** | **15 settimane** | **240h** | **48h** | **288h** | **€28.8k** |

**Timeline**: Q2-Q4 2026 (15 settimane = 3.75 mesi = realistica con 1 dev + 1 QA part-time)

---

## ✅ SUCCESS METRICS

**Dopo completamento (fine Q4 2026), Luna2 supporterà:**

- ✅ Gestione 10-15 dipendenti (presenze, ferie, stipendi)
- ✅ Fatturazione completa (preventivi → fatture → incassi)
- ✅ Contabilità double-entry (prima nota + bilancio + cespiti)
- ✅ Compliance fiscale (F24, CUD, IVA, dichiarazioni)
- ✅ Tesoreria (cash flow + riconciliamento)
- ✅ Scadenzario automatico (10+ reminder annuali)
- ✅ Report & Analytics (vendite, costi, ROI dipendenti)

**SRL è COMPLETAMENTE GESTITA da Luna2** ✅ Production Ready!

---

## 🔗 DIPENDENZE CRITICHE

```
    Presenze
      ↓
    Buste Paga ← Contabilità
      ↓            ↓
    Dichiarazioni ← Scadenzario ← Fatture
      ↓
    Bilancio ← Cespiti
```

**Sequenza garantita**: Nessun circolo vizioso, workflow top-down.

---

## 📞 NEXT STEPS

1. **Review**: Stakeholder approva roadmap
2. **Kick-off Sprint 1**: Workflow approvazione (inizio maggio)
3. **Setup**: Dbase schema, Git branch, staging server
4. **Dev**: 2-3 checkpoint meeting/settimana
5. **Lancio**: Production deploy Q4 2026

---

**Documento**: SRL 10-15 Dipendenti Roadmap v1.0  
**Data**: Aprile 2026  
**Status**: ✅ Ready for Review & Approval
