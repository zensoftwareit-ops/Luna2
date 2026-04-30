# ✅ CHECKLIST PREPRODUZIONE → PRODUZIONE
**Luna2 Project - Development & Deployment Roadmap**  
**Data**: 30 Aprile 2026  
**Status**: READY TO EXECUTE  

---

## 🎯 FASE 1: SECURITY & TESTING (Settimana 1-2)

### Security Hardening
- [ ] **CVE Dependency Scan**
  ```bash
  mvn dependency:check
  # Expected: All critical/high CVEs resolved
  # Owner: Tech Lead
  # Timeline: 1 day
  ```

- [ ] **Input Validation Extension**
  - [ ] Extend ValidationService.java
  - [ ] Add validators for: DDT, Ordini, CRM modules
  - [ ] Test with 50+ edge cases (negative values, special chars, SQL keywords)
  - Owner: Backend Dev | Timeline: 2-3 days

- [ ] **CSRF Security Verification**
  - [ ] Verify CSRF tokens on all POST/PUT/DELETE
  - [ ] Test SameSite cookie attribute
  - [ ] Validate cookie httpOnly + Secure flags
  - Owner: Security Audit | Timeline: 1 day

- [ ] **SQL Injection Prevention**
  - [ ] Verify ALL queries use Hibernate parameterized queries
  - [ ] Run SQLMap automated testing
  - [ ] Document any raw SQL (should be zero or justified)
  - Owner: Backend Dev | Timeline: 1-2 days

- [ ] **XSS & Content Security Policy**
  - [ ] Set CSP headers in Nginx
  - [ ] Test with OWASP ZAP
  - [ ] Validate output encoding on all JSP templates
  - Owner: Frontend Dev | Timeline: 1 day

---

### Unit & Integration Tests (TARGET: 70% coverage)

- [ ] **Action Class Tests** (16 total)
  ```java
  // Example structure:
  // src/test/java/com/zensoftware/luna2/action/
  // ├── ClienteActionTest.java
  // ├── PreventivoActionTest.java
  // ├── FatturaActionTest.java
  // └── SdiNotificheActionTest.java
  
  // Expected: Each action has 3-5 test scenarios
  // Timeline: 3-4 days for all 16
  Owner: QA/Backend | Timeline: 4 days
  ```

- [ ] **Service Layer Tests** (Email, SDI, Validation)
  - [ ] EmailService: test SMTP connection + pixel injection
  - [ ] SdiService: mock REST calls + assert state transitions
  - [ ] ValidationService: 20+ edge cases per validator
  - Owner: Backend Dev | Timeline: 3 days

- [ ] **Database Integration Tests**
  - [ ] Use H2 in-memory for test DB
  - [ ] Test CRUD operations all entities
  - [ ] Test transaction rollback on error
  - [ ] Test concurrent access (2+ threads)
  - Owner: Backend Dev | Timeline: 2 days

- [ ] **End-to-End Workflow Tests** (Selenium or TestNG)
  ```
  Test Scenarios:
  1. Preventivo → Email → Accettazione → Fattura
  2. Fattura → SDI invio → Notifiche polling
  3. Fattura Passiva → Import SDI → Registrazione
  4. Pagamento registrazione
  5. Cliente CRUD + search
  6. Prodotto import CSV
  
  Owner: QA | Timeline: 3-4 days
  ```

- [ ] **Test Configuration**
  - [ ] src/test/resources/application-test.properties
  - [ ] src/test/resources/test-data.sql (sample data)
  - [ ] Maven surefire plugin configured (mvn test)
  - Owner: Tech Lead | Timeline: 1 day

---

### Performance & Load Testing

- [ ] **Baseline Performance Metrics**
  - [ ] Home page load time < 500ms
  - [ ] Fattura list query < 1s (100 records)
  - [ ] PDF generation < 3s
  - [ ] Email send < 2s
  - Owner: Backend Dev | Timeline: 1 day

- [ ] **Load Test: 100 Concurrent Users**
  ```bash
  # Using JMeter
  # Scenario: 50% read (list), 30% create, 20% update
  # Expected: 95th percentile < 2s
  
  # Timeline: 1 day setup + execution
  ```

---

## 🔗 FASE 2: SDI INTEGRATION (Stagind & Prod)

### SDI Staging Environment Test

- [ ] **Environment Configuration**
  ```properties
  # application.properties
  sdi.endpoint.staging=https://sdi.agenziaentrate.gov.it/soap/SdiRiceviFile/v1.0/WS  
  sdi.endpoint.prod=https://sdi.agenziaentrate.gov.it/soap/SdiRiceviFile/v1.0/WS_PROD
  sdi.username=${SDI_USERNAME}
  sdi.password=${SDI_PASSWORD}
  sdi.mode=staging   # Change to 'prod' before go-live
  sdi.timeout.sec=15
  ```
  - Owner: Tech Lead | Timeline: 1 day

- [ ] **SDI Test Cases**
  ```
  Test 1: Generate valid XML → Check XSD compliance
  Test 2: Submit to SDI → Receive codice SDI (TEST_*)
  Test 3: Polling notifiche (5 min cycle) → Assert ACCETTATA state
  Test 4: Handle SDI ERRORE responses → Proper error logging
  Test 5: Duplicate submission prevention → Assert error
  Test 6: Very large invoice (100 rows) → Performance check
  
  Owner: Backend Dev | Timeline: 3-4 days
  ```

- [ ] **Email Notification on SDI State**
  - [ ] Email triggers on SDI_ACCETTATA
  - [ ] Email content includes SDI codice + date
  - [ ] Fallback if email fails (queue retry)
  - Owner: Backend Dev | Timeline: 1 day

- [ ] **Logging & Monitoring SDI**
  - [ ] Log all SDI requests/responses
  - [ ] Alert on polling failures (cron job issues)
  - [ ] Dashboard chart: fatture submitted vs accepted
  - Owner: DevOps | Timeline: 1-2 days

---

## 🐳 FASE 3: DOCKER DEPLOYMENT (Preprod)

### Docker Build & Test Locally

- [ ] **Build Docker Images**
  ```bash
  docker build -t luna2-api:1.0 ./luna2-api
  docker build -t server-manager:1.0 ./server-manager
  docker build -t nginx-gateway:1.0 ./docker/nginx  # if custom Nginx
  
  # Verify images exist
  docker images | grep luna2
  
  Owner: DevOps | Timeline: 1 day
  ```

- [ ] **Docker Compose Test (Local)**
  ```bash
  # Start stack locally
  docker-compose -f docker-compose-preprod.yml up -d
  
  # Verify all services
  docker ps  # Should show 4-6 containers running
  
  # Health checks
  docker-compose ps  # All should be 'healthy'
  
  # Test API
  curl http://localhost:8080/luna2-api/
  
  # Cleanup
  docker-compose down
  
  Owner: DevOps | Timeline: 1 day
  ```

### Preprod Server Deployment

- [ ] **Server Provisioning**
  - [ ] Spin up Ubuntu 22.04 LTS instance (4GB RAM, 100GB disk minimum)
  - [ ] SSH key access configured
  - [ ] DNS A record pointing to server IP
  - Owner: DevOps | Timeline: 0.5 days

- [ ] **Server Setup (Terraform or manual)**
  ```bash
  # Execute deployment guide: DEPLOYMENT_PREPROD_DOCKER_COMPLETE.md
  # Steps include:
  # ├─ System updates (apt upgrade)
  # ├─ Docker installation
  # ├─ Firewall configuration (ufw)
  # ├─ Git repo clone
  # ├─ .env file creation (secrets management)
  # ├─ Database initialization
  # ├─ Docker Compose deploy
  # ├─ Nginx SSL (Let's Encrypt + certbot)
  # └─ Health checks
  
  Owner: DevOps | Timeline: 1.5 days
  ```

- [ ] **Environment Variables**
  ```bash
  # .env file (NEVER commit to git!)
  DB_PASSWORD_1=<SECURE_PASSWORD_1>
  DB_PASSWORD_2=<SECURE_PASSWORD_2>
  MYSQL_ROOT_PASSWORD=<ROOT_PASS>
  SMTP_HOST=smtp.gmail.com
  SMTP_PORT=587
  SMTP_USER=noreply@mycompany.com
  SMTP_PASSWORD=<GMAIL_APP_PASSWORD>
  SDI_USERNAME=<AGENZIA_ENTRATE_USER>
  SDI_PASSWORD=<AGENZIA_ENTRATE_PASS>
  ENVIRONMENT=PREPROD
  LOG_LEVEL=INFO
  
  Owner: DevOps | Timeline: 0.5 days
  ```

- [ ] **Database Backup Strategy**
  ```bash
  # Schedule daily backup
  # 0 2 * * * /opt/backups/mysql-daily-backup.sh
  
  # Backup script should:
  # ├─ Dump all databases
  # ├─ Encrypt with GPG
  # ├─ Upload to S3/cloud storage
  # ├─ Retain 30 days
  # └─ Alert if backup fails
  
  Owner: DevOps | Timeline: 1 day
  ```

- [ ] **Monitoring & Alerts**
  - [ ] Configure container health checks in compose file
  - [ ] Setup Portainer for visual container management
  - [ ] Configure email alerts for health check failures
  - [ ] Basic uptime monitoring (ping from cron)
  - Owner: DevOps | Timeline: 1 day

---

## 🧪 FASE 4: CUSTOMER ACCEPTANCE TESTING (UAT)

### Test Data Preparation

- [ ] **Sample Dataset Creation**
  ```sql
  -- Insert via Luna2 UI or SQL directly
  
  10 Clienti con:
  ├─ Valid email (for tracking)
  ├─ P.IVA / CF
  ├─ Indirizzo completo (for SDI fatture)
  └─ Telefono
  
  20 Prodotti con:
  ├─ Category tags
  ├─ Prezzo unitario (vari range)
  ├─ Giacenza
  └─ Foto (opzionale per MVP)
  
  5 Preventivi con email tracking:
  ├─ Vari clienti
  ├─ Vari prodotti (1-10 righe)
  └─ Stati diversi (BOZZA, INVIATO, ACCETTATO)
  
  Owner: QA | Timeline: 2-3 days
  ```

- [ ] **UAT Scenarios**
  ```
  Scenario 1: PMI Small (1-5 clienti, 20 fatture/anno)
  ├─ Create cliente
  ├─ Create preventivo (3 righe)
  ├─ Email tracking (open + download)
  ├─ Convert to fattura
  ├─ Register pagamento
  └─ Export PDF ✅
  
  Scenario 2: PMI Medium (10-30 clienti, 100 fatture/anno)
  ├─ Bulk import prodotti (CSV)
  ├─ Create multiple preventivi
  ├─ Batch email invio
  ├─ Fatture attive → SDI staging
  ├─ Monitor notifiche polling
  └─ Register pagamenti (mix metodi)
  
  Scenario 3: Fornitori
  ├─ Receive fatture via SDI
  ├─ Auto-import fatture passive
  ├─ Match con OA (Optional)
  ├─ Crea reminder pagamenti
  └─ Export for accounting
  
  Owner: Customer + QA | Timeline: 3-4 days
  ```

- [ ] **UAT Sign-Off**
  ```
  Customer confirms:
  ☐ Workflow funziona as expected
  ☐ Email deliverability OK
  ☐ PDF output acceptable
  ☐ Performance acceptable (< 2s page load)
  ☐ No critical bugs found
  ☐ Ready for production
  
  Owner: Product Manager | Timeline: Ongoing
  ```

---

## 🚀 FASE 5: PRODUCTION DEPLOYMENT

### Pre-Go-Live Checklist (1 day before)

- [ ] **Final Security Review**
  - [ ] CVE scan results clean
  - [ ] Code review completed
  - [ ] OWASP Top 10 assessment passed
  - [ ] Penetration test results acceptable
  - Owner: Security | Timeline: 0.5 days

- [ ] **Database Backup & Migration Test**
  ```bash
  # 1. Full backup preprod DB (screenshot for rollback)
  mysqldump -u root -p --all-databases > /backup/predb_backup_final.sql
  
  # 2. Create prod database with schema
  # 3. Test restore from backup
  # 4. Verify data integrity
  
  Owner: DevOps | Timeline: 0.5 days
  ```

- [ ] **DNS & Certificate Readiness**
  - [ ] Production domain DNS A record ready to switch
  - [ ] Let's Encrypt certificate requested for prod domain
  - [ ] Certificate renewal automation tested (certbot)
  - [ ] Rollback procedure documented (revert DNS in 5 min)
  - Owner: DevOps | Timeline: 0.5 days

- [ ] **Support & Communication**
  - [ ] Status page prepared (https://status.luna2.com)
  - [ ] Support team on standby (24h post-launch)
  - [ ] Incident response plan documented
  - [ ] Customer notification email drafted
  - Owner: Product Manager | Timeline: 1 day before

### Go-Live Execution (Day H)

```
TIMELINE: 00:00 UTC (Flessibile con customer)

00:00 - Code Freeze
       └─ No commits to main branch
       └─ Tag release: v1.0.0

01:00 - Final System Check
       ├─ Preprod sanity test (all modules)
       ├─ DB backup complete
       ├─ Nginx health check
       └─ All containers healthy

02:00 - Production Environment Spin-up
       ├─ Deploy docker-compose on prod server
       ├─ Verify containers running
       ├─ Database schema migrate
       ├─ Load initial data (clienti, prodotti, settings)
       └─ Register production certs (Let's Encrypt)

03:00 - DNS Cutover
       ├─ Update domain A record → prod server IP
       ├─ Propagation time: 1-5 min
       ├─ Verify www.luna2.com → prod IP
       └─ Clear browser cache if needed

04:00 - Application Testing
       ├─ Test login flow
       ├─ Test home page load
       ├─ Create sample preventivo → fattura
       ├─ Test email sending (track pixel)
       └─ Verify SDI endpoint (staging or actual)

05:00 - Customer Notification
       ├─ Send email: "Luna2 launched successfully"
       ├─ Provide login credentials
       ├─ Share user guide & support contacts
       └─ Announce 24h support window

06:00-24:00 - Active Monitoring
       ├─ Monitor error logs every 15 min
       ├─ Watch for support tickets
       ├─ Be ready for rollback if critical issue
       └─ Log all issues for post-mortem
```

### Post-Go-Live (Week 1)

- [ ] **Bug Fix SLA: 4 hours for critical**
  - [ ] Triage all reported issues
  - [ ] Deploy hotfixes (separate branch)
  - [ ] Document lessons learned
  - Owner: Dev Team | Timeline: Ongoing

- [ ] **Performance Monitoring Dashboard**
  - [ ] Setup APM tool (Datadog, New Relic, or custom)
  - [ ] Chart API response times
  - [ ] Database query analysis
  - [ ] Container resource usage
  - Owner: DevOps | Timeline: 1-2 days

- [ ] **Usage Analytics**
  - [ ] Track active users
  - [ ] Monitor feature usage (which modules used most)
  - [ ] Identify bottlenecks or errors
  - Owner: Product Manager | Timeline: 1 week

---

## 📋 DEPENDENCY OWNERSHIP & TIMELINE

### Critical Path

```
0 days:   ┌─ Security Hardening (2 days)
          │  └─ CVE fixes, input validation, CSRF review
          │
2 days:   ├─ Test Suite (4 days)
          │  └─ Unit/integration/E2E tests
          │
6 days:   ├─ SDI Staging Test (3-4 days)
          │  └─ XML generation, endpoint testing, polling
          │
10 days:  ├─ Docker Preprod Deploy (3 days)
          │  └─ Build images, compose test, server setup
          │
13 days:  ├─ UAT with Customer (3-4 days)
          │  └─ Sample data, scenario testing, sign-off
          │
17 days:  └─ Production Go-Live (1 day)
             └─ DNS cutover, monitoring, support standby

TOTAL: ~4-5 weeks (17-21 days) from NOW to LIVE
```

### Team Size & Roles

```
Role               | Person      | Commitment | Test Phase | Deploy Phase
───────────────────┼─────────────┼────────────┼────────────┼──────────────
Tech Lead          | [Name]      | 40h/week   | Planning   | Oversight
Backend Dev        | [Name]      | 40h/week   | Code       | Debugging
QA/Test Eng        | [Name]      | 30h/week   | Test exec  | UAT oversight
Frontend Dev       | [Name]      | 20h/week   | UI testing | Bug fixes
DevOps/Infra       | [Name]      | 30h/week   | Prep       | Deployment
Product Manager    | [Name]      | 20h/week   | Planning   | Communication
Customer Rep       | [Name]      | 10h/week   | UAT        | Sign-off
Security Officer   | [Name]      | 10h/week   | Review     | Final audit
```

---

## 📈 DEFINITION OF DONE

**Criteria for moving to next phase:**

✅ **Phase 1 (Security & Testing) DONE when:**
- [ ] All CVE fixes applied & verified
- [ ] 70%+ test coverage (unit + integration)
- [ ] Zero critical security findings (OWASP)
- [ ] Performance baseline established
- [ ] All unit tests passing (mvn test)

✅ **Phase 2 (SDI) DONE when:**
- [ ] SDI XML generation validated (XSD compliant)
- [ ] End-to-end SDI flow tested (staging)
- [ ] Polling mechanism working (5 min cycle)
- [ ] Email notifiche on SDI state transitions
- [ ] 3+ complete workflows tested

✅ **Phase 3 (Docker) DONE when:**
- [ ] Docker images build successfully
- [ ] docker-compose up runs all services
- [ ] Health checks return healthy status
- [ ] Preprod server responsive (https://)
- [ ] Database connectivity verified

✅ **Phase 4 (UAT) DONE when:**
- [ ] Customer runs all test scenarios
- [ ] Zero P0 (critical) bugs found
- [ ] Customer signs acceptance document
- [ ] All performance SLAs met (< 2s)
- [ ] Production readiness confirmed

✅ **Phase 5 (Go-Live) DONE when:**
- [ ] DNS cutover successful
- [ ] All modules responsive on prod
- [ ] No critical errors in logs
- [ ] Customer can login + use features
- [ ] 24h support monitoring completed

---

## 🎬 HOW TO USE THIS CHECKLIST

1. **Print or share digitally** with team
2. **Assign owner to each task** (in brackets [Name])
3. **Update status daily** (progress tracking)
4. **Hold daily standup** (15 min, discuss blockers)
5. **Track dates** (actual vs planned)
6. **Document lessons learned** (post-phase review)

---

## 📞 ESCALATION CONTACTS

```
CRITICAL ISSUES (Immediate response):
├─ Tech Lead: [email/phone]
├─ DevOps: [email/phone]
└─ Product Manager: [email/phone]

SUPPORT HOURS: Mon-Fri 8:00-20:00 CET
AFTER-HOURS: Emergency only, on-call rotation

CUSTOMER CONTACTS:
├─ Technical POC: [email]
├─ Business POC: [email]
└─ Executive Sponsor: [email]
```

---

**Document Version**: 1.0  
**Last Updated**: 30 Apr 2026  
**Next Review**: After Phase 1 completion (7 days)  
**Status**: ✅ READY FOR TEAM DISTRIBUTION
