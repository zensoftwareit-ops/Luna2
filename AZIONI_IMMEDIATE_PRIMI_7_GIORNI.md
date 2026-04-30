# 🚀 AZIONI IMMEDIATE - PRIMI 7 GIORNI
**Luna2 Project - Quick Start Guide**  
**Creato**: 30 Aprile 2026  
**Deadline**: 7 Maggio 2026  

---

## 📍 TU SEI QUI

```
CURRENT STATE (Today - Apr 30)
├─ ✅ Code completato (85% core)
├─ ✅ Documentazione completa
├─ ⚠️ Zero test automatici
├─ ⚠️ SDI testato solo staging
├─ ⚠️ Deployment manuale
└─ ❌ Production-ready? NO

TARGET STATE (7 days)
├─ ✅ CVE fixes applicati
├─ ✅ Test suite basic 40%+
├─ ✅ SDI staging validato
├─ ✅ Docker compose testato
└─ ✅ Team aligned on roadmap
```

---

## 📋 GIORNO 1 (30 Aprile - TODAY)

### MORNING (9:00-12:00)

**TASK 1: Team Kick-off Meeting** (30 min)
```
├─ Partecipanti: Tech Lead, Backend Dev, QA, DevOps, PM
├─ Agenda:
│  ├─ Show ANALISI_PROGETTO_COMPLETA_20260430.md
│  ├─ Review CHECKLIST_ESECUTIVA_PREPROD_TO_PROD.md
│  ├─ Answer Q&A (risks, concerns, assumptions)
│  └─ Assign owners to each task
├─ Duration: 30-45 min
└─ Output: Assigned ownership + start dates
```

**TASK 2: Setup Development Environment** (1.5 hours)
```bash
# Each dev executes:

1. Clone repo (if not done)
   git clone <repo-url>
   cd Luna2

2. Install dependencies
   mvn clean install

3. Run existing tests (if any)
   mvn test

4. Start locally
   # Option A: Tomcat + MySQL locally
   # Option B: Docker Compose locally
   docker-compose -f docker-compose-preprod.yml up -d

5. Verify app running
   curl http://localhost:8080/luna2-api/
   # Expected: 200 OK response

6. Slack status: "✅ Dev env ready"
```

**TASK 3: Create Issue Tracking Board** (30 min)
```
Platform: GitHub Issues / Linear / Jira (per choice)

Create 5 Epics:
├─ EPIC-1: Security & CVE Fixes (5 tasks)
├─ EPIC-2: Test Suite Implementation (12 tasks)
├─ EPIC-3: SDI Staging Validation (4 tasks)
├─ EPIC-4: Docker Deployment (3 tasks)
└─ EPIC-5: Customer UAT (5 tasks)

Each Epic has:
├─ Description (link to checklist)
├─ Assigned owner
├─ Estimated effort (days)
├─ Start date
└─ Target completion

Output: Board shared with team
```

### AFTERNOON (13:00-17:00)

**TASK 4: Security Assessment Kickoff** (1 hour)
```
Action: Run CVE scan on current dependencies

mvn dependency:check -DfailOnWarning=true

Expected output: List of CVEs
├─ CRITICAL (Red): 0 (or fix immediately)
├─ HIGH (Red): X (prioritize for fix)
├─ MEDIUM (Yellow): Y (schedule this week)
└─ LOW (Green): OK to defer

Owner: Tech Lead
Output: CVE spreadsheet with fix plan
```

**TASK 5: Database Schema Audit** (1.5 hours)
```
Action: Document current schema + propose missing pieces

Run:
mysql -h localhost -u root -p luna2_dev -e "SHOW TABLES;"

Document:
├─ Current tables (16-20 tables)
├─ Missing indexes (if any)
├─ Missing constraints (FK, unique keys)
├─ Backup strategy
└─ Disaster recovery plan

Output: Database_Schema_Audit.md
Owner: DevOps/Backend
```

**TASK 6: SDI Integration Verification** (1 hour)
```
Review SdiService.java + SdiNotificheAction.java

Checklist:
☐ Endpoint URL configurable (properties file)
☐ XML generation XSD-compliant (testable)
☐ REST client timeout configured (15 sec)
☐ Polling mechanism in CronJobListener (5 min cycle)
☐ Error handling for SDI failures
☐ Logging comprehensive (all requests/responses)

Owner: Backend Dev
Output: SDI_Status_20260430.md
```

---

## 📋 GIORNI 2-3 (1-2 Maggio)

### GOAL: CVE Fixes + Test Foundation

**MORNING: CVE Fixes & Documentation** (4 hours)

```
TASK 7: Apply CVE Patches
└─ For each HIGH/CRITICAL CVE found:
   ├─ Research fix (update pom.xml version)
   ├─ Test locally (mvn clean package)
   ├─ Document reason for version bump
   └─ Create git commit with CVE reference

Example:
OLD: <commons-io.version>2.5</commons-io.version>
NEW: <commons-io.version>2.18.0</commons-io.version>
REASON: CVE-2024-50379 - Path traversal

Repeat for:
├─ commons-fileupload
├─ commons-beanutils
├─ protobuf
└─ Any other flagged

Timeline: 2-3 hours
Owner: Tech Lead + Backend Dev
Verification: mvn test (build must pass)
```

**AFTERNOON: Test Framework Setup** (4 hours)

```
TASK 8: Create Test Suite Structure

mkdir -p src/test/java/com/zensoftware/luna2/action
mkdir -p src/test/java/com/zensoftware/luna2/service
mkdir -p src/test/resources

Create pom.xml test dependencies:
├─ JUnit 4 or 5 (latest)
├─ Mockito (for mocks)
├─ TestNG (for integration tests)
└─ H2 (for in-memory test DB)

Create test configuration:
├─ src/test/resources/application-test.properties
├─ src/test/resources/test-data.sql
└─ src/test/java/TestBase.java (base class)

Verify:
mvn test  # Should show test discovery works (0 tests OK for now)

Timeline: 2-3 hours
Owner: QA + Backend
Output: Maven test execution ready
```

---

### GIORNI 4-7 (3-7 Maggio)

### GOAL: Core Test Suite (40% coverage) + SDI Staging

**MONDAY: Unit Tests - Action Layer** (8 hours)

```
TASK 9: Implement 5 Core Action Tests

Files to create:
├─ src/test/java/.../ClienteActionTest.java
├─ src/test/java/.../ProdottoActionTest.java
├─ src/test/java/.../PreventivoActionTest.java
├─ src/test/java/.../FatturaActionTest.java
└─ src/test/java/.../SdiNotificheActionTest.java

Each test file contains:
├─ Setup (mock DB, initialize action)
├─ Test: Create new entity (assert success)
├─ Test: Read entity (assert found)
├─ Test: Update entity (assert changes saved)
├─ Test: Delete entity (assert removed)
└─ Test: Invalid inputs (assert error handling)

Expected result:
└─ 5 test files × 5 tests each = 25 tests
   ├─ Pass rate: 100%
   ├─ Coverage: ~40%
   └─ Execution time: < 30 sec

Timeline: 3-4 hours per action class
Owner: Backend Dev + QA
Command: mvn test -Dtest=*ActionTest
```

**TUESDAY: Service Layer Integration Tests** (6 hours)

```
TASK 10: Test EmailService + SdiService

EmailService Tests:
├─ Test SMTP connection (mock)
├─ Test pixel injection (assert HTML contains UUID)
├─ Test link watermarking (assert URL has tracking param)
├─ Test error handling (SMTP timeout, auth failure)
└─ Test async send fallback

SdiService Tests:
├─ Test XML generation (assert XSD valid)
├─ Test REST call (mock endpoint)
├─ Test response parsing (extract codice SDI)
├─ Test timeout handling (15 sec default)
└─ Test retry logic

Timeline: 2-3 hours per service
Owner: Backend Dev
Files: src/test/java/.../service/*ServiceTest.java
```

**WEDNESDAY: SDI Staging Integration** (6 hours)

```
TASK 11: End-to-End SDI Staging Test

Prerequisite:
├─ Agenzia Entrate staging credentials (requested earlier)
├─ Test certificates (if required)
└─ Sample cliente + prodotto data

Scenario:
1. Create preventivo (2-3 rows, valid amounts)
2. Convert to fattura PROFORMA
3. Convert to fattura REALE
4. Generate XML + validate schema
5. Submit to SDI staging endpoint
6. Receive codice SDI (e.g., "TEST_ABCD1234")
7. Poll notifiche (every 30 sec for 5 min)
8. Assert ACCETTATA state
9. Send email notification
10. Log all steps

Timeline: 3-4 hours (includes waiting for responses)
Owner: Backend Dev + QA
Output: SDI_Staging_Test_Report_20260507.md

Checklist:
☐ XML generated correctly
☐ SDI endpoint accepts submission
☐ Codice SDI received and stored
☐ Polling retrieves notifiche
☐ Email sent on ACCETTATA
☐ No errors in logs
```

**THURSDAY: Docker Compose Test** (4 hours)

```
TASK 12: Build & Test Docker Stack Locally

Steps:
1. Clean previous images
   docker-compose down -v

2. Build images
   docker build -t luna2-api:1.0 ./luna2-api
   docker build -t server-manager:1.0 ./server-manager

3. Start stack
   docker-compose -f docker-compose-preprod.yml up -d

4. Verify services
   docker ps  # All containers running + healthy

5. Test connectivity
   curl http://localhost:8080/luna2-api/
   curl http://localhost:9000  # server-manager
   mysql -h 127.0.0.1 -u root -p (test DB connection)

6. Run sample workflow
   ├─ Login to UI
   ├─ Create cliente
   ├─ Create preventivo
   ├─ Email test
   └─ Verify tracking

7. Stop stack
   docker-compose down

Timeline: 2 hours (mostly waiting for builds)
Owner: DevOps
Output: Docker test log + fixes needed
```

**FRIDAY: Sprint Review + Planning** (2 hours)

```
TASK 13: Sprint Review Meeting

Attendees: Tech Lead, All Devs, PM, QA, Customer (optional)

Review completed:
☐ CVE fixes applied
☐ Test framework setup
☐ 25 unit tests passing
☐ SDI staging integration tested
☐ Docker compose working

Open issues:
├─ Any failing tests?
├─ Any blockers?
├─ Any customer questions?
└─ Anything learned?

Plan next sprint (May 8-14):
├─ Complete remaining unit tests (30% of codebase)
├─ Integration tests (database, transactions)
├─ E2E test scenarios (Selenium or TestNG)
├─ Performance baseline (load testing)
└─ Preprod server preparation

Output: Sprint report shared with team + customer
```

---

## 🎯 FIRST WEEK SUCCESS CRITERIA

### MUST HAVES ✅

- [ ] **0 Critical CVEs** (all fixed or accepted risk)
- [ ] **25+ unit tests** passing locally
- [ ] **SDI staging flow** tested end-to-end
- [ ] **Docker compose** working locally
- [ ] **Team alignment** on roadmap + ownership
- [ ] **Issue board** created + populated
- [ ] **Roadmap visible** to customer + stakeholders

### NICE TO HAVES 🎁

- [ ] 40+ unit tests completed
- [ ] Integration test framework ready
- [ ] Performance baseline measured
- [ ] Preprod server infrastructure booked/provisioned
- [ ] Customer demo of new test automation
- [ ] Blog post: "Luna2 engineering progress"

---

## 📞 DAILY STANDUP FORMAT

**Time**: 9:30 AM CET (15 min, daily Mon-Fri)

**Attendees**: Backend, Frontend, QA, DevOps, PM

**Format** (per person):
```
Yesterday: [Completed X tasks]
Today: [Working on Y tasks]
Blocker: [If any, escalate immediately]
Confidence: [70%, need help from someone]

Example:
Yesterday: Implemented ClienteActionTest, 3/5 tests passing
Today: Fixing test 4-5, then start ProdottoActionTest
Blocker: Need test DB schema export from DevOps
Confidence: 80% (expecting helper from QA)
```

---

## 🆘 CRITICAL CONTACTS

```
IF STUCK ON:                    CONTACT
──────────────────────────────────────────────
CVE fix compilation error       → Tech Lead
Test framework / Mockito        → QA Lead
Database schema issue           → DevOps
SDI endpoint access             → Customer/PM
Docker build failure            → DevOps
Struts2 routing issue           → Backend Lead
Email service setup             → Backend Lead
Production readiness questions  → PM + Tech Lead
```

---

## 📚 QUICK REFERENCE DOCUMENTS

```
📄 MAIN ANALYSIS:
└─ /Luna2/ANALISI_PROGETTO_COMPLETA_20260430.md
   ├─ Architecture overview
   ├─ Module status
   ├─ Security checklist
   ├─ Database schema
   ├─ Deployment guide
   └─ Risks & mitigation

📄 EXECUTION CHECKLIST:
└─ /Luna2/CHECKLIST_ESECUTIVA_PREPROD_TO_PROD.md
   ├─ Phase 1-5 tasks (detailed)
   ├─ Owner assignments
   ├─ Timeline (dependency chart)
   ├─ Definition of Done
   └─ Escalation contacts

📄 THIS DOCUMENT:
└─ /Luna2/AZIONI_IMMEDIATE_PRIMI_7_GIORNI.md
   ├─ Daily breakdown (7 days)
   ├─ Success criteria
   ├─ Standup format
   └─ Quick contacts

📄 EXISTING DOCS (Use as reference):
├─ DEPLOYMENT_PREPROD_DOCKER_COMPLETE.md
├─ CRM_PHASE1_COMPLETION.md
├─ ASSESSMENT_COMPLETO.md
├─ MASTER_EXECUTION_PLAN_Q2_Q4_2026.md
├─ CSRF_IMPLEMENTATION.md
├─ INPUT_VALIDATION_FRAMEWORK.md
└─ API.md
```

---

## ✅ BEFORE LEAVING TODAY (Apr 30)

**CHECKLIST (Everyone):**
- [ ] Read ANALISI_PROGETTO_COMPLETA_20260430.md (30 min)
- [ ] Read CHECKLIST_ESECUTIVA_PREPROD_TO_PROD.md (20 min)
- [ ] Understand your roles/tasks (10 min)
- [ ] Ask questions in team chat (anytime)
- [ ] Update Slack status: "Ready to start May 1"

**CHECKLIST (Tech Lead Only):**
- [ ] Create issue board with 5 epics
- [ ] Assign owners to each major task
- [ ] Schedule daily standups (9:30 AM)
- [ ] Schedule weekly sprint reviews (Friday 4 PM)
- [ ] Share documents with team

**CHECKLIST (DevOps):**
- [ ] Prepare dev environment setup guide
- [ ] Book preprod server (if not done)
- [ ] Prepare .env template file
- [ ] Document database initialization steps

---

## 🏁 NEXT MILESTONE: May 7

**GOAL: Complete Week 1 successfully**

Expected state on May 7:
```
✅ CVE fixes applied & tested
✅ 40%+ test coverage
✅ SDI staging validated
✅ Docker fully working
✅ Team confident on timeline
✅ Zero blockers identified
✅ Customer aware of progress
└─ READY FOR WEEK 2: More tests + Preprod
```

---

**Document Owner**: Project Lead  
**Last Updated**: 30 Apr 2026 @ 17:00 CET  
**Distribution**: All team members + Customer  
**Format**: Share as Markdown + Print + Pin in Slack  

🚀 **LET'S GO LIVE IN 4 WEEKS!** 🚀
