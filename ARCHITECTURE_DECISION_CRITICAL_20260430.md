# 🔍 ARCHITECTURE DISCOVERY - DUAL STACK DETECTED
**Date**: 30 Apr 2026, 11:00 CET  
**Status**: ⚠️ MAJOR FINDING - Impacts entire project strategy  

---

## 🎯 KEY DISCOVERY

Luna2 è un progetto **IBRIDO** in **MIGRAZIONE PARZIALE**:

```
/workspaces/Luna2/
├── luna2-api/                          ← NEW: Spring Boot 2.7 REST API
│   ├── pom.xml                         ← Spring Boot parent
│   ├── src/main/java/it/zensoftware/luna2/api/
│   │   ├── controller/ (6 controllers) ← REST endpoints
│   │   ├── service/                    ← Business logic
│   │   ├── entity/                     ← JPA entities
│   │   ├── security/                   ← JWT auth
│   │   └── websocket/                  ← Real-time
│   └── Dockerfile                      ← Containerized
│
├── src/                                ← OLD: Struts2/Hibernate Legacy
│   └── main/java/it/zensoftware/luna2/action/
│       ├── PreventiviAction.java       ✅ Found!
│       ├── ProdottiAction.java         ✅ Found!
│       ├── FattureAction.java          ✅ Found!
│       ├── FatturePassiveAction.java   ✅ Found!
│       ├── DdtAction.java              ✅ Found!
│       ├── LeadAction.java             ✅ CRM legacy
│       ├── PayrollAction.java          ✅ HR prep
│       ├── SdiNotificheAction.java     ✅ SDL integration!
│       └── ~30 more Action classes...
│
├── pom.xml                             ← ROOT pom with both projects
└── docker-compose-*.yml                ← Multi-container setup
```

---

## 🔄 ARCHITECTURE IMPLICATIONS

### Dual-Stack Situation

| Layer | New (Spring Boot) | Old (Struts2) | Status |
|-------|---|---|--|
| **Frontend Controller** | Spring @RestController | Struts2 Action | BOTH EXIST |
| **Business Logic** | Service classes | Action classes | DUPLICATED |
| **Data Access** | JPA/Hibernate | DAO/Hibernate | MIXED |
| **Database** | Same MySQL 8.0 | Same MySQL 8.0 | SHARED |
| **Authentication** | JWT | Session-based | CONFLICT |
| **API Format** | REST JSON | SOAP/JSP | INCOMPATIBLE |
| **Deployment** | Docker JAR | WAR on Tomcat | SEPARATE |

### What This Means

✅ **Advantages**:
- Can deploy new API independently
- Old UI still works during migration
- Incremental migration possible

⚠️ **Problems**:
- Two deployment artifacts
- Duplicate business logic
- Database schema conflicts possible
- Authentication inconsistency
- Operational complexity

❌ **Risks**:
- Data sync issues
- Deployment coordination needed
- Testing matrix doubled

---

## 📊 MODULE INVENTORY - Complete Picture

### NEW REST API (Spring Boot `/luna2-api/`)

| Module | Controller | Status | LOC | Endpoints |
|--------|-------|--|--|--|
| **Clienti** | ✅ ClientiController | READY | 187 | 6 |
| **Fatture** | ✅ FattureController | 🟡 Partial | 265 | 7 (1 broken) |
| **Ordini** | ✅ OrdiniController | 🟡 Partial | 251 | 7 |
| **Dashboard** | ✅ DashboardController | 🟡 Partial | 180 | 2 |
| **Auth** | ✅ AuthController | READY | ? | JWT |
| **Calendar** | ✅ CalendarioController | 🟡 Partial | ? | ? |
| **Preventivi** | ❌ NOT IN NEW API | N/A | N/A | Legacy only |
| **Prodotti** | ❌ NOT IN NEW API | N/A | N/A | Legacy only |
| **DDT** | ❌ NOT IN NEW API | N/A | N/A | Legacy only |

### LEGACY ACTIONS (Struts2 `/src/`)

| Module | Action Class | Status | Key Feature |
|--------|-------|--|--|
| **Preventivi** | ✅ PreventiviAction | UNKNOWN | Quotes workflow |
| **Prodotti** | ✅ ProdottiAction | UNKNOWN | Catalog |
| **Fatture** | ✅ FattureAction | UNKNOWN | Active invoices |
| **Fatture Passive** | ✅ FatturePassiveAction | UNKNOWN | Supplier invoices |
| **DDT** | ✅ DdtAction | UNKNOWN | Delivery notes |
| **Lead/CRM** | ✅ LeadAction | UNKNOWN | Sales pipeline |
| **Payroll** | ✅ PayrollAction | UNKNOWN | HR/Buste Paga |
| **Presenze** | ✅ PresenzeAction | UNKNOWN | Badge/attendance |
| **SDI** | ✅ SdiNotificheAction | UNKNOWN | Tax file system! |
| **+20 more** | ✅ Various | UNKNOWN | Other modules |

---

## ⚡ STRATEGIC IMPLICATIONS

###Question 1: Which Stack Goes to Production?

**OPTIONS**:
```
A) Spring Boot REST only (new luna2-api/)
   ├─ Pro: Modern, RESTful, stateless, scalable
   ├─ Pro: Can containerize easily
   ├─ Con: Missing modules (Preventivi,Prodotti, DDT, CRM...)
   ├─ Con: Need to port all legacy logic
   └─ Timeline: 8-12 weeks

B) Struts2 + JSP only (legacy /src/)
   ├─ Pro: All features exist
   ├─ Pro: Less work upfront
   ├─ Con: Old stack, hard to deploy
   ├─ Con: Not suitable for modern APIs
   └─ Timeline: 2-3 weeks (quick fix)

C) Hybrid (both coexist)
   ├─ Pro: Short-term solution
   ├─ Con: Double deployment burden
   ├─ Con: Data sync complexity
   ├─ Con: User confusion (two UIs)
   └─ Timeline: 4 weeks + ops overhead

D) Dual-API (Spring endpoints + Struts2 backend)
   ├─ Pro: REST API + legacy features
   ├─ Con: Architectural hack
   ├─ Con: Hard to maintain long-term
   └─ Timeline: 6-8 weeks
```

**RECOMMENDATION**: 
**Option A** (Spring Boot only) + port critical modules in parallel
- Focus Weeks 1-2: Make new API production-ready
- Weeks 3-4: Port legacy modules to REST controllers
- Weeks 5-8: Complete missing modules

---

### Question 2: What About SdiNotificheAction?

**CRITICAL DISCOVERY**: The legacy code already has `SdiNotificheAction.java`!

```
This means:
✅ SDI integration COULD exist in legacy Struts2 code
❌ BUT it's NOT in the new Spring Boot REST API
⚠️ Need to inspect SdiNotificheAction immediately

ACTION: Read SdiNotificheAction.java to understand 
what SDI logic already exists (if any)
```

---

## 🎯 DECISION FRAMEWORK

For **production deployment May 21**, we must choose:

```
TIMELINE CONSTRAINT: 4 weeks
MODULES NEEDED: Clienti, Fatture, Ordini, Preventivi, Prodotti, Pagamenti

PLAN A (Recommended):
- Use Spring Boot REST API (luna2-api/) as foundation
- Week 1-2: Fix critical gaps (PDF, SDI, validation)
- Week 3: Port Preventivi + Prodotti from legacy
- Week 4: UAT + deployment
- ASSUMES: Legacy Struts2 code is reference, not production

PLAN B (High Risk):
- Use legacy Struts2 (faster initial deployment)
- Week 1: Stabilize and test
- Week 2: Deploy to preprod
- Week 3-4: UAT
- PROBLEMS: Old stack, no API, operational overhead

WE RECOMMEND: Plan A + async port of remaining modules to Q3
```

---

## 📋 IMMEDIATE NEXT STEPS (Today)

```
PRIORITY 1: Inspections (2 hours)

□ Read SdiNotificheAction.java 
  Location: /Luna2/src/main/java/it/zensoftware/luna2/action/SdiNotificheAction.java
  Question: What SDI logic exists in legacy code?

□ Read PreventiviAction.java
  Question: What Preventivi workflow is implemented?

□ Determine: Which modules in /src/ are PRODUCTION-READY?

PRIORITY 2: Decision (1 hour)

□ Tech Lead + PM: Choose Stack
  ├─ Option A: Spring Boot REST (recommended)
  ├─ Option B: Legacy Struts2
  └─ Option C: Hybrid temporary

□ Update project timeline based on choice

PRIORITY 3: Planning (1 hour)

□ If Option A: Create REST controller checklist
  ├─ Which legacy modules to port first?
  ├─ Which to implement new?
  └─ Resource allocation

□ If Option B: Start Struts2 stabilization
  ├─ List critical bugs
  ├─ Prep deployment config
  └─ Find ops resources
```

---

## 🚨 RISK ASSESSMENT

| Risk | Likelihood | Impact | Mitigation |
|------|--------|----|---|
| **Dual stack unmaintainable** | HIGH | CRITICAL | Choose one stack NOW |
| **DDT/CRM missing from new API** | HIGH | HIGH | Accept deferral to Q3 |
| **SDI partially implemented** | MEDIUM | CRITICAL | Audit legacy SdiAction |
| **Data inconsistency** | MEDIUM | HIGH | Ensure single DB source |
| **Deployment complexity** | HIGH | MEDIUM | Clear ops runbooks |
| **Missed timeline** | MEDIUM | HIGH | Cut scope not quality |

---

## 📞 DECISION NEEDED FROM LEADERSHIP

```
QUESTIONS FOR TEAM:

1. Can we defer DDT + CRM modules to Q3?
   → If NO, timeline extends to 8-12 weeks
   → If YES, can deliver streamlined API in 4 weeks

2. Should we use new Spring Boot REST API or legacy Struts2?
   → A) REST (modern, 50% effort, needs porting)
   → B) Legacy (quick, 20% effort, ops burden)
   → C) Both temporarily (complex, risky)

3. Do we have SLA/production requirements for this?
   → Low: Legacy is fine, cheaper ops
   → Medium: REST preferable, better scaling
   → High: REST mandatory, enterprise-ready

MAKE DECISION BY: End of May 1 kickoff meeting
```

---

## 📊 GO-LIVE SCENARIOS

###Scenario A: Spring Boot REST API (Recommended)

```
Modules for launch:
✅ Clienti (READY)
✅ Fatture (need PDF + SDI)
✅ Ordini (need reports)
⚠️ Dashboard (need charts)
✅ Auth (READY)

Modules DEFERRED to Q3:
❌ Preventivi (port from legacy, 1-2 weeks)
❌ Prodotti (port from legacy, 1-2 weeks)
❌ Pagamenti (implement new, 1 week)
❌ DDT (not critical, defer fully)
❌ CRM/Leads (not critical, defer fully)

Timeline: 4 weeks feasible
Deployment: Docker containers
Operations: Simpler, no Tomcat needed
Future: Phased porting of remaining modules
```

### Scenario B: Legacy Struts2 WAR

```
Modules included:
✅ All 30 modules (assumption)

Timeline: 2-3 weeks to stabilize
Deployment: Tomcat + WAR
Operations: Complex, legacy Java stack
Scaling: Limited (stateful sessions)
Future: Need full rewrite eventually

Risk: May break at scale
```

---

**Document Status**: ⚠️ AWAITING DECISION  
**Next Update**: Post May 1 kickoff meeting  
**Decision Impact**: Timeline + architecture + personnel allocation

