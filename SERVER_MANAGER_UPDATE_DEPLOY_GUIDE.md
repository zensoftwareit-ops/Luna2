# 🔄 Sistema Update & Deploy - Guida Completa

## 📋 Panoramica

Il Server Manager v2.0 include un sistema completo per:
1. **Sincronizzare** il codice dal repository GitHub
2. **Compilare** il nuovo WAR
3. **Distribuire** l'update a tutte le istanze
4. **Applicare migrazioni DB** senza perdita dati

---

## 🎯 Workflow Completo

```
┌─────────────────────────────────────────────────────────────┐
│                    DEVELOPER WORKFLOW                       │
└─────────────────────────────────────────────────────────────┘

1. Modifica codice localmente
2. git commit -m "feat: nuova funzionalità"
3. git push origin main

┌─────────────────────────────────────────────────────────────┐
│              SERVER MANAGER - UPDATE FASE                   │
└─────────────────────────────────────────────────────────────┘

4. Login al pannello: http://localhost:8888
5. Click "⬆️ Update da GitHub"
6. Click "⬆️ Esegui Update"

   Azioni automatiche:
   ├── cd /workspaces/Luna2
   ├── git pull origin main          ← Allinea con GitHub
   ├── mvn clean package -DskipTests ← Compila nuovo WAR
   └── ✅ target/luna2.war pronto

┌─────────────────────────────────────────────────────────────┐
│             SERVER MANAGER - DEPLOY FASE                    │
└─────────────────────────────────────────────────────────────┘

7. Click "🚀 Deploy a Tutte le Istanze"

   Per OGNI istanza (example1.com, client2.com, ...):
   
   ├── Applica migrazioni DB
   │   ├── Legge database/migrations/*.sql
   │   ├── Ordina alfabeticamente (001_.sql, 002_.sql, ...)
   │   ├── Esegue ogni SQL nel container MySQL
   │   └── ✅ Solo nuove colonne/tabelle (idempotente)
   │
   ├── Copia nuovo WAR
   │   └── docker cp target/luna2.war luna2-{domain}:/webapps/
   │
   └── Riavvia container applicativo
       └── docker restart luna2-{domain}

8. ✅ COMPLETATO
   - Tutte le istanze aggiornate
   - Database preservati
   - Downtime: ~10-30 secondi per istanza
```

---

## 🔧 API Endpoints

### 1. Update Sistema

```bash
POST /api/system/update
Authorization: Bearer {token}
```

**Risposta:**
```json
{
  "success": true,
  "message": "Update completato. Ora puoi fare deploy alle istanze.",
  "steps": [
    {
      "step": "git-pull",
      "status": "success",
      "output": "Already up to date."
    },
    {
      "step": "maven-build",
      "status": "success",
      "output": "WAR generato con successo"
    }
  ],
  "warPath": "/workspaces/Luna2/target/luna2.war"
}
```

### 2. Deploy Singola Istanza

```bash
POST /api/instances/example.com/deploy
Authorization: Bearer {token}
```

**Risposta:**
```json
{
  "success": true,
  "message": "Deploy completato per example.com",
  "steps": [
    {
      "step": "db-migrations",
      "status": "success",
      "output": "\n001_add_status.sql: OK\n002_audit_table.sql: OK"
    },
    {
      "step": "copy-war",
      "status": "success"
    },
    {
      "step": "restart-app",
      "status": "success"
    }
  ]
}
```

### 3. Deploy Globale (Tutte le Istanze)

```bash
POST /api/system/deploy-all
Authorization: Bearer {token}
```

**Risposta:**
```json
{
  "success": true,
  "message": "Deploy completato su tutte le istanze",
  "total": 3,
  "succeeded": 3,
  "failed": 0,
  "results": [
    {
      "domain": "example1.com",
      "success": true,
      "steps": [
        { "step": "copy-war", "success": true },
        { "step": "restart", "success": true }
      ]
    },
    {
      "domain": "client2.com",
      "success": true,
      "steps": [
        { "step": "copy-war", "success": true },
        { "step": "restart", "success": true }
      ]
    },
    {
      "domain": "demo.com",
      "success": true,
      "steps": [
        { "step": "copy-war", "success": true },
        { "step": "restart", "success": true }
      ]
    }
  ]
}
```

---

## 🗃️ Migrazioni Database

### Struttura Directory

```
/workspaces/Luna2/database/migrations/
├── 001_initial_schema.sql
├── 002_add_status_column.sql
├── 003_create_audit_table.sql
└── 004_add_indexes.sql
```

### Regole Migrazioni

1. **Nomenclatura sequenziale**: `001_`, `002_`, `003_`, ...
2. **Idempotenza obbligatoria**: Usa `IF NOT EXISTS` / `IF EXISTS`
3. **Ordine alfabetico**: I file vengono eseguiti in ordine
4. **Non cancellare mai**: Non rimuovere migrazioni già applicate

### Esempio Migrazione Idempotente

```sql
-- ✅ CORRETTO - Idempotente
ALTER TABLE noleggio_lead 
ADD COLUMN IF NOT EXISTS priority INT DEFAULT 0;

CREATE TABLE IF NOT EXISTS noleggio_notifications (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    lead_id BIGINT,
    message TEXT,
    sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_lead_priority 
ON noleggio_lead(priority);

-- ❌ SBAGLIATO - Non idempotente (fallisce alla seconda esecuzione)
ALTER TABLE noleggio_lead ADD COLUMN priority INT;
CREATE TABLE noleggio_notifications (...);
```

### Esempio Migrazione Complessa

```sql
-- 005_refactor_status_field.sql

-- 1. Aggiungi nuova colonna
ALTER TABLE noleggio_lead 
ADD COLUMN IF NOT EXISTS status_new VARCHAR(50);

-- 2. Migra dati (solo se status_new è NULL)
UPDATE noleggio_lead 
SET status_new = CASE 
    WHEN old_status = 1 THEN 'ACTIVE'
    WHEN old_status = 2 THEN 'CLOSED'
    ELSE 'PENDING'
END
WHERE status_new IS NULL;

-- 3. Aggiungi indice
CREATE INDEX IF NOT EXISTS idx_status_new 
ON noleggio_lead(status_new);

-- NOTA: Rimozione vecchia colonna va fatta manualmente dopo verifica
-- ALTER TABLE noleggio_lead DROP COLUMN old_status;  -- Commentato per sicurezza
```

---

## 🔒 Sicurezza Database

### Preservazione Dati Garantita

Il sistema **NON** esegue mai:
- `DROP DATABASE`
- `TRUNCATE TABLE`
- `DELETE FROM` (tranne in migrazioni esplicite)
- Reset password o configurazioni

### Backup Consigliato

Prima di deploy maggiori, esegui backup manuale:

```bash
# Backup singola istanza
docker exec mysql-example.com mysqldump -uluna2 -pluna2pass luna2 > backup-example.sql

# Backup tutte le istanze
for domain in example1.com client2.com demo.com; do
    docker exec mysql-$domain mysqldump -uluna2 -pluna2pass luna2 > backup-$domain-$(date +%Y%m%d).sql
done
```

---

## 🚨 Gestione Errori

### Scenario: Maven Build Fallisce

**UI mostra:**
```
✗ maven-build: failed
Errore: Compilation error in MyClass.java
```

**Soluzione:**
1. Fix errori di compilazione localmente
2. git push
3. Riprova update

### Scenario: Deploy Fallisce su Un'Istanza

**UI mostra:**
```
Totale: 3 | OK: 2 | Falliti: 1
✓ example1.com
✓ client2.com
✗ demo.com: Container not found
```

**Soluzione:**
1. Verifica container esista: `docker ps -a | grep demo.com`
2. Se container spento: Click "▶️ Abilita" nel pannello
3. Riprova deploy singolo

### Scenario: Migrazione SQL Fallisce

**Log mostra:**
```
002_add_column.sql: WARN
Error: Duplicate column name 'status'
```

**Soluzione:**
- Se è errore benigno ("colonna già esiste"), il deploy **continua**
- Se errore critico, il deploy **fallisce** e serve intervento manuale

---

## 📊 Monitoring Deploy

### Log Real-Time

Durante il deploy, l'UI mostra:

```
⏳ Deploy in corso...

✓ git-pull: success (Already up to date)
✓ maven-build: success (BUILD SUCCESS - 2m 34s)
✓ example1.com: copy-war → restart → OK
✓ client2.com: copy-war → restart → OK
✓ demo.com: copy-war → restart → OK

✅ Deploy completato su tutte le istanze (3/3)
```

### Verifica Post-Deploy

1. **Check container status**: Tutti dovrebbero essere `running`
2. **Check log applicativi**: `docker logs luna2-{domain}`
3. **Test funzionalità**: Accedi alle istanze e verifica

```bash
# Log container
docker logs --tail 50 luna2-example.com

# Status container
docker ps --format "table {{.Names}}\t{{.Status}}" | grep luna2
```

---

## 🎓 Best Practices

### 1. Test Locale Prima di Deploy

```bash
# Testa migrazioni localmente
mysql -uluna2 -pluna2pass luna2 < database/migrations/003_new_feature.sql
```

### 2. Deploy Graduale

Per deploy critici:
1. Deploy su **istanza di test** (`test.example.com`)
2. Verifica funzionalità
3. Deploy su **altre istanze**

### 3. Comunicazione con Clienti

Prima di deploy con downtime:
- Avvisa clienti con 24-48h di anticipo
- Pianifica deploy in orari di basso traffico (notte)
- Comunica durata stimata (es. "10-30 secondi")

### 4. Rollback Plan

Se deploy fallisce gravemente:

```bash
# 1. Stop nuovo container
docker stop luna2-example.com

# 2. Ripristina vecchio WAR (se hai backup)
docker cp backup-luna2.war luna2-example.com:/usr/local/tomcat/webapps/luna2.war

# 3. Ripristina DB (se hai backup)
cat backup-example.sql | docker exec -i mysql-example.com mysql -uluna2 -pluna2pass luna2

# 4. Restart
docker start luna2-example.com
```

---

## 📞 Supporto

### Problemi Comuni

| Errore | Causa | Soluzione |
|--------|-------|-----------|
| "WAR non trovato" | Maven build non eseguito | Esegui prima "Update da GitHub" |
| "Container not found" | Istanza non esiste | Verifica con `docker ps -a` |
| "Permission denied" | Permessi filesystem | `chmod +x manage-domains-multitenant.sh` |
| "Git pull failed" | Conflitti merge | `cd /workspaces/Luna2 && git stash && git pull` |

### Log Debug

```bash
# Log server manager
docker logs -f server-manager

# Log specifico deploy
grep "DEPLOY" /var/log/luna2-manager.log
```

---

## 🎯 Riepilogo Vantaggi

✅ **Zero configurazione manuale**: Tutto automatico via UI  
✅ **Zero perdita dati**: Database preservati, solo migrazioni  
✅ **Zero downtime prolungato**: ~10-30 sec per istanza  
✅ **Rollback facile**: Backup automatici + ripristino rapido  
✅ **Audit completo**: Log di ogni deploy con timestamp  
✅ **Deploy selettivo**: Singola istanza o globale  
✅ **Migrazioni sicure**: Idempotenti e verificate  

---

**Versione:** 2.0  
**Ultima modifica:** 27 Feb 2026  
**Autore:** Luna2 Team
