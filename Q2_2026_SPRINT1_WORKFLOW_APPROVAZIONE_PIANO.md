# 📅 PIANO OPERATIVO Q2 2026 - WORKFLOW APPROVAZIONE

**Progetto**: Luna2 - SRL Administration  
**Fase**: Q2 2026 (Aprile-Giugno)  
**Sprint 1**: Workflow Approvazione (Settimane 1-2)  
**Priority**: CRITICA  
**Owner**: Dev Team  
**Status**: 🟡 PIANIFICAZIONE

---

## 📋 OBIETTIVO SPRINT 1

Implementare sistema di **approvazione workflow** per:
- ✅ Richieste ferie
- ✅ Richieste permessi
- ✅ Richieste spese
- ✅ Approvazione manager
- ✅ Tracking automatico

**Output**: Sistema operativo per "ferie on-demand" senza email

---

## 🏗️ ARCHITETTURA WORKFLOW

```
DIPENDENTE
    ↓
[DRAFT] Richiesta ferie
    ↓
[SUBMITTED] Invia a manager
    ↓
MANAGER
    ↓
[APPROVED/REJECTED] Revisione
    ↓
[COMPLETED] Tracking in ferie totali
    ↓
HR Dashboard vede tutte le richieste
```

---

## 🗄️ SCHEMA DATABASE

### Tabella: approval_requests
```sql
CREATE TABLE approval_requests (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  company_id BIGINT NOT NULL,
  requester_id BIGINT NOT NULL,
  approver_id BIGINT,
  request_type VARCHAR(50) NOT NULL, -- 'FERIE', 'PERMESSO', 'SPESA'
  
  -- Dettagli richiesta
  description TEXT,
  
  -- Date specifiche per tipo
  start_date DATE,
  end_date DATE,
  days_requested INT,
  amount_requested DECIMAL(10,2), -- per spese
  
  -- Status workflow
  status VARCHAR(20) DEFAULT 'DRAFT', -- DRAFT, SUBMITTED, APPROVED, REJECTED, COMPLETED
  approval_date DATETIME,
  rejection_reason TEXT,
  
  -- Tracking
  created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  submitted_date DATETIME,
  updated_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  
  FOREIGN KEY (company_id) REFERENCES companies(id),
  FOREIGN KEY (requester_id) REFERENCES users(id),
  FOREIGN KEY (approver_id) REFERENCES users(id),
  INDEX idx_status (status),
  INDEX idx_requester (requester_id),
  INDEX idx_approver (approver_id)
);
```

### Tabella: approval_history (audit trail)
```sql
CREATE TABLE approval_history (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  request_id BIGINT NOT NULL,
  action VARCHAR(50), -- CREATED, SUBMITTED, APPROVED, REJECTED
  actor_id BIGINT NOT NULL,
  action_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  notes TEXT,
  
  FOREIGN KEY (request_id) REFERENCES approval_requests(id),
  FOREIGN KEY (actor_id) REFERENCES users(id),
  INDEX idx_request (request_id)
);
```

---

## 🎯 USER STORIES

### US-WA-001: Dipendente sottopone richiesta ferie

**As a** dipendente  
**I want to** richiedere ferie via sistema  
**So that** il manager le approva automaticamente senza email

**Acceptance Criteria:**
- [ ] Dipendente apre form "Nuova richiesta ferie"
- [ ] Compila: data inizio, data fine, motivo (opz)
- [ ] Sistema calcola "giorni richiesti" automaticamente
- [ ] Clicca "Invia a approvazione"
- [ ] Status diventa "SUBMITTED"
- [ ] Manager riceve notifica

**Tasks**:
1. Create form JSP `/app/approval/ferie-form`
2. Create action class `ApprovalRequestAction.java`
3. Add table `approval_requests`
4. Email notification to approver

---

### US-WA-002: Manager approva/rifiuta

**As a** manager  
**I want to** approvare richieste ferie dal team  
**So that** le ferie sono gestite centralmente

**Acceptance Criteria:**
- [ ] Manager vai a "Richieste da approvare"
- [ ] Vede lista richieste in "SUBMITTED"
- [ ] Clicca su una
- [ ] Vede: nome dipendente, date, giorni, motivo
- [ ] Clicca "APPROVA" o "RIFIUTA"
- [ ] Se RIFIUTA, inserisce motivo
- [ ] Status aggiornato
- [ ] Dipendente notificato

**Tasks**:
1. Create dashboard "Richieste in approvazione"
2. Create approval form
3. Update status logic
4. Notification system

---

### US-WA-003: Tracking ferie automatico

**As a** HR  
**I want to** vedere saldo ferie per dipendente  
**So that** so quante ferie rimangono

**Acceptance Criteria:**
- [ ] HR vede tab "Ferie per dipendente"
- [ ] Per ogni dipendente: ferie totali, usate, rimanenti
- [ ] Calcolo automatico da approvazioni
- [ ] Export CSV possibile

**Tasks**:
1. Query: ferie approvate per dipendente
2. Dashboard HR
3. CSV export

---

## 🛠️ IMPLEMENTATION CHECKLIST

### Week 1
- [ ] Database schema creation
- [ ] Entity classes (ApprovalRequest.java)
- [ ] DAO layer (ApprovalRequestDAO.java)
- [ ] Basic CRUD endpoints

### Week 2
- [ ] JSP form for ferie request
- [ ] Manager approval dashboard
- [ ] Email notifications
- [ ] Status transitions logic
- [ ] Testing & UAT prep

---

## 📊 EFFORT ESTIMATE

| Task | Days | Developer |
|------|------|-----------|
| DB Schema + Entity | 2 | Backend |
| DAO + Service layer | 2 | Backend |
| Create endpoints | 2 | Backend |
| JSP forms | 1.5 | Frontend |
| Manager dashboard | 1.5 | Frontend |
| Email notifications | 2 | Backend |
| Testing | 1.5 | QA |
| Documentation | 1 | Tech Writer |
| **TOTAL** | **14 days** | - |

---

## 🔗 DEPENDENCIES

**Blockers**: Nessuno (può partire subito)

**After this completes:**
- Sprint 2 (Presenze) dipende da questo (OK)
- Sprint 3 (Buste paga) dipende da Presenze

---

## 🎬 KICK-OFF PLAN

**Monday, April 1, 2026**
- [ ] Team meeting (1h)
- [ ] DB schema review
- [ ] Assign tasks
- [ ] Setup development environment

**Goal**: Avere prototype di US-WA-001 entro Wednesday

---

## ✅ SUCCESS CRITERIA

Sprint è considerato "DONE" quando:
- [ ] Dipendente può richiedere ferie
- [ ] Manager può approvarle
- [ ] Email notifications funzionano
- [ ] Ferie approvate appaiono in HR dashboard
- [ ] Zero critical bugs
- [ ] UAT passed

---

## 📝 NOTES

- **Scadenzario integration**: Le ferie approvate non devono conflittare con scadenzario (es. ferie non durante chiusura aziendale) - add future validation
- **Multi-language**: Implementare in italiano + English flags
- **Mobile**: Form deve essere mobile-friendly (dipendenti accedono da phone)

---

**Piano creato**: 2024-12-19  
**Sprint inizio**: 2026-04-01  
**Sprint fine**: 2026-04-14  
**Status**: READY FOR KICKOFF

