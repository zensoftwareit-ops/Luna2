# UAT Approval Workflow (Sprint 1)

## Prerequisiti

1. Import schema:
   - chmod +x database/import_approval_workflow.sh
   - DB_HOST=127.0.0.1 DB_PORT=3306 DB_NAME=luna2 DB_USER=root DB_PASS=*** database/import_approval_workflow.sh
2. Seed UAT:
   - mysql -h 127.0.0.1 -u root -p luna2 < database/approval_workflow_uat_seed.sql
3. Deploy applicazione:
   - mvn clean package -DskipTests

## URL UAT

1. Form nuova richiesta: /luna2/app/approval/ferie-form
2. Lista dipendente: /luna2/app/approval/ferie-list
3. Pending manager: /luna2/app/approval/pending-approvals

## Scenari UAT

1. Creazione richiesta ferie
   - Aprire ferie-form
   - Inserire data inizio/fine e descrizione
   - Salvare
   - Atteso: redirect su ferie-list con stato SUBMITTED

2. Approvazione manager
   - Aprire pending-approvals con utente manager
   - Premere Approva su una richiesta
   - Atteso: richiesta sparisce da pending e stato diventa APPROVED

3. Rifiuto manager
   - Aprire pending-approvals
   - Inserire motivo rifiuto
   - Premere Conferma rifiuto
   - Atteso: stato REJECTED e motivazione salvata

4. Validazioni
   - Data inizio > data fine
   - Atteso: errore bloccante
   - Rifiuto senza motivo
   - Atteso: errore bloccante

## Criteri di accettazione

1. Nessun errore 500 nei flussi principali
2. Stato workflow coerente: SUBMITTED -> APPROVED/REJECTED
3. Persistenza DB verificabile su approval_requests
4. Test unitari verdi: ApprovalWorkflowActionTest
