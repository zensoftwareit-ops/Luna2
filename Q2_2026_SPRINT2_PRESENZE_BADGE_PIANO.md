# 📅 PIANO OPERATIVO Q2 2026 - PRESENZE E BADGE

**Progetto**: Luna2 - SRL Administration  
**Fase**: Q2 2026 (Aprile-Giugno)  
**Sprint 2**: Presenze & Badge (Settimane 3-5)  
**Priority**: CRITICA  
**Owner**: Dev Team  
**Dipende da**: Sprint 1 ✅ (Workflow OK)  
**Status**: 🟡 PIANIFICAZIONE

---

## 📋 OBIETTIVO SPRINT 2

Implementare sistema di **gestione presenze** per:
- ✅ Check-in/check-out (badge QR)
- ✅ Ferie automatiche (da contratto)
- ✅ Permessi (da Workflow Sprint 1)
- ✅ Malattia/assenza certificata
- ✅ Straordinari tracking
- ✅ Report presenze per HR

**Output**: Sistema presenze operativo, base per Buste Paga Sprint 3

---

## 🏗️ ARCHITETTURA PRESENZE

```
DIPENDENTE
    ↓
[08:00] Check-in (QR badge / manuale)
    ↓
[17:00] Check-out
    ↓
SISTEMA CALCOLA
    ↓
- Ore lavorate: 9h
- Normale: 8h
- Straordinario: 1h
    ↓
HR vede timesheet
Buste Paga legge ore
    ↓
[Fine mese] Report presenze
```

---

## 🗄️ SCHEMA DATABASE

### Tabella: time_records (check-in/check-out)
```sql
CREATE TABLE time_records (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  company_id BIGINT NOT NULL,
  employee_id BIGINT NOT NULL,
  record_date DATE NOT NULL,
  
  -- Check-in/check-out
  check_in_time TIME,
  check_out_time TIME,
  check_in_location VARCHAR(100), -- Office/Remote/Site
  check_in_method VARCHAR(50), -- BADGE_QR, MANUAL, API
  
  -- Flags
  is_holiday BOOLEAN DEFAULT FALSE,
  is_vacation BOOLEAN DEFAULT FALSE,
  is_sick_leave BOOLEAN DEFAULT FALSE,
  is_permitted_absence BOOLEAN DEFAULT FALSE,
  
  -- Calculations (auto)
  hours_worked DECIMAL(5,2),
  hours_overtime DECIMAL(5,2),
  
  -- Notes
  notes TEXT,
  
  -- Audit
  created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  
  FOREIGN KEY (company_id) REFERENCES companies(id),
  FOREIGN KEY (employee_id) REFERENCES users(id),
  UNIQUE KEY uk_employee_date (employee_id, record_date),
  INDEX idx_company_date (company_id, record_date)
);
```

### Tabella: vacation_balance
```sql
CREATE TABLE vacation_balance (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  company_id BIGINT NOT NULL,
  employee_id BIGINT NOT NULL,
  year INT NOT NULL,
  
  -- Ferie da contratto (es. CCNL Commercio = 26 gg)
  total_days_entitled INT,
  
  -- Usate (calcolate da approvazioni)
  days_used INT DEFAULT 0,
  
  -- Rimanenti (calcolate)
  days_remaining INT GENERATED ALWAYS AS (total_days_entitled - days_used) STORED,
  
  -- Ferie riportate da anno precedente
  carried_over_days INT DEFAULT 0,
  
  FOREIGN KEY (company_id) REFERENCES companies(id),
  FOREIGN KEY (employee_id) REFERENCES users(id),
  UNIQUE KEY uk_employee_year (employee_id, year)
);
```

### Tabella: holidays (giorni festivi)
```sql
CREATE TABLE holidays (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  company_id BIGINT NOT NULL,
  holiday_date DATE NOT NULL,
  holiday_name VARCHAR(100), -- "Capodanno", "Pasqua", etc.
  
  FOREIGN KEY (company_id) REFERENCES companies(id),
  UNIQUE KEY uk_company_date (company_id, holiday_date)
);
```

---

## 🎯 USER STORIES

### US-PR-001: Dipendente check-in al mattino

**As a** dipendente  
**I want to** segnare il mio arrivo al lavoro  
**So that** il sistema traccia le mie ore

**Acceptance Criteria:**
- [ ] App mobile o porta web con QR scanner
- [ ] Scansiona badge QR
- [ ] Sistema registra `check_in_time` automaticamente
- [ ] Conferma visiva: "✓ Check-in 08:15"
- [ ] Se dimentica: manuale entry con nota

**Tasks**:
1. QR badge generation (print per dipendenti)
2. Mobile-friendly check-in form
3. API endpoint `POST /app/hr/check-in`
4. Notification dopo check-in

---

### US-PR-002: Calcolo ore lavorate

**As a** sistema  
**I want to** calcolare automaticamente le ore  
**So that** non c'è errore umano

**Acceptance Criteria:**
- [ ] Check-in: 08:15, Check-out: 17:00
- [ ] Sistema calcola: 17:00 - 08:15 = 8:45 (8.75 ore)
- [ ] Se > 8 ore: 1 ora = straordinario (OT)
- [ ] Se < 8 ore: day marked incomplete (alert HR)
- [ ] Report per settimana/mese

**Logic**:
```
hours_worked = check_out_time - check_in_time
hours_overtime = MAX(0, hours_worked - 8)
```

---

### US-PR-003: Ferie automatiche da contratto

**As a** HR  
**I want to** che il sistema sappia quante ferie ha ogni dipendente  
**So that** non ci sono conflitti o errori

**Acceptance Criteria:**
- [ ] Dipendente assunto con CCNL Commercio → 26 gg ferie anno
- [ ] Sistema auto-calcola `vacation_balance`
- [ ] Quando ferie approvate (da Sprint 1) → decrementa `days_used`
- [ ] Dashboard HR mostra: "Tot: 26, Usate: 5, Rimanenti: 21"

**Tasks**:
1. Carica CCNL standard (26 gg per Commercio)
2. Setup initial vacation_balance
3. Sync con approval_requests (Sprint 1)
4. Dashboard query

---

### US-PR-004: Malattia e permessi

**As a** dipendente  
**I want to** registrare malattia o permesso  
**So that** HR traccia assenze

**Acceptance Criteria:**
- [ ] App mobile con button "Malattia"
- [ ] Upload certificato digitale (PDF)
- [ ] Status: `is_sick_leave = TRUE`
- [ ] HR vede: "Malattia - Certificato caricato"
- [ ] Non decremata da ferie, tracciata separatamente

**Tasks**:
1. Malattia form
2. File upload handler
3. HR dashboard filter per assenze

---

## 📊 EFFORT ESTIMATE

| Task | Days | Role |
|------|------|------|
| DB Schema | 2 | DBA |
| Entity + DAO | 2.5 | Backend |
| Check-in/out endpoints | 2.5 | Backend |
| Mobile form (QR) | 2 | Frontend |
| Calculations logic | 1.5 | Backend |
| Vacation setup | 1.5 | Backend |
| HR dashboard | 2 | Frontend |
| Reports (CSV) | 1.5 | Backend |
| Testing | 2 | QA |
| Documentation | 1 | Tech |
| **TOTAL** | **18.5 days** | - |

---

## 🔗 DEPENDENCIES

**Dipende da**: Sprint 1 ✅ (Workflow Approvazione)

**Fornisce per**: Sprint 3 ✅ (Buste Paga)
- Ore lavorate necessarie per calcolo stipendi

---

## 🎬 KICK-OFF PLAN

**Monday, April 15, 2026** (post Sprint 1)
- [ ] Team meeting
- [ ] Review Schema
- [ ] Assign tasks
- [ ] Print QR badges

**Goal**: Prototype check-in entro Wednesday

---

## ✅ SUCCESS CRITERIA

Sprint è "DONE" quando:
- [ ] Dipendente può fare check-in/check-out
- [ ] Ore calcolate automaticamente
- [ ] Ferie da contratto setup
- [ ] HR vede report presenze
- [ ] Zero critical bugs
- [ ] UAT passed

---

## 📝 NOTE IMPLEMENTATIVE

### QR Badge Strategy
```
Format: UUID_{employee_id}_{company_id}
Example: UUID_125_5

Physical: Printed badges for each employee
Digital: App scans and POSTs to API
```

### Timezone Consideration
- Tutti gli orari in timezone companend (es. Europe/Rome)
- Database store in UTC + convert UI

### Overtime Rules (CCNL Commercio)
- Ora extra pagata a 15% in più
- Max 20 ore OT per mese
- Paga supplementare per SabatoDomenica

---

**Piano creato**: 2024-12-19  
**Sprint inizio**: 2026-04-15  
**Sprint fine**: 2026-04-29  
**Status**: READY FOR SPRINT 1 COMPLETION

