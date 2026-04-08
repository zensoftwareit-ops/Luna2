# 📋 IMPLEMENTATION GUIDE - Sprint 1 Workflow Approvazione

**Sprint**: Q2 2026 - Week 1-2  
**Module**: Approval Workflow (Ferie, Permessi, Spese)  
**Status**: SCAFFOLDING COMPLETE - Ready for Backend Integration  
**Date Created**: 2024-12-19

---

## ✅ What's Been Delivered

### 1. Database Schema (✅ READY)
**File**: `/workspaces/Luna2/database/approval_workflow_schema.sql`

**Tables created**:
- `approval_requests` - Richieste di approvazione
- `approval_history` - Audit trail
- `vacation_balance` - Saldo ferie per dipendente/anno
- `approval_templates` - Regole template per tipo richiesta

**Views created**:
- `v_pending_approvals` - Richieste pending per manager
- `v_vacation_status` - Saldo ferie status
- `v_approval_stats` - Statistiche approvazioni

**How to execute**:
```bash
mysql -u luna2_user -p luna2_db < /workspaces/Luna2/database/approval_workflow_schema.sql
```

---

### 2. Java Model (✅ READY)
**File**: `src/main/java/it/zensoftware/luna2/model/ApprovalRequest.java`

**Features**:
- JPA entity with full mappings
- LocalDate/LocalDateTime fields
- CRUD-ready
- Foreign key constraints

**Status**: 0 compilation errors ✅

---

### 3. Data Access Layer (✅ READY)
**File**: `src/main/java/it/zensoftware/luna2/dao/ApprovalRequestDAO.java`

**Methods**:
- `findByRequester()` - Richieste di un dipendente
- `findPendingApprovals()` - Richieste da approvare per manager
- `save()` - Crea nuova richiesta
- `approve()` - Approva richiesta
- `reject()` - Rifiuta richiesta

**Status**: Stub implementation (TODO: Hibernate integration) ✅

---

### 4. Controller/Action (✅ READY)
**File**: `src/main/java/it/zensoftware/luna2/action/ApprovalWorkflowAction.java`

**Methods**:
- `ferieList()` - Lista richieste ferie dipendente
- `ferieCreate()` - Crea nuova richiesta
- `approve()` - Approva da manager
- `reject()` - Rifiuta da manager

**Status**: Ready for routing + JSP integration ✅

---

### 5. UI Form (✅ READY)
**File**: `src/main/webapp/WEB-INF/jsp/approval/ferie-form.jsp`

**Features**:
- Bootstrap 5 responsive design
- Date picker fields
- Auto-calculation of days
- Error/success alerts
- Client-side validation

**Status**: Ready to deploy ✅

---

## 🚀 Next Steps for Dev Team

### Week 1 - Backend Setup

#### Task 1.1: Execute Database Schema
```sql
-- Connect to Luna2 database
mysql -h localhost -u root -p
USE luna2;
SOURCE /workspaces/Luna2/database/approval_workflow_schema.sql;
```

**Verify**:
```sql
SHOW TABLES LIKE 'approval%';
DESCRIBE approval_requests;
```

#### Task 1.2: Implement DAO Methods with Hibernate
**File to update**: `ApprovalRequestDAO.java`

Replace stub methods with real Hibernate queries:

```java
@Override
public List<ApprovalRequest> findByRequester(Long companyId, Long requesterId) {
    Session session = sessionFactory.getCurrentSession();
    Criteria criteria = session.createCriteria(ApprovalRequest.class);
    criteria.add(Restrictions.eq("companyId", companyId));
    criteria.add(Restrictions.eq("requesterId", requesterId));
    criteria.addOrder(Order.desc("createdDate"));
    return criteria.list();
}
```

**Similar for**:
- `findPendingApprovals()`
- `save()`
- `approve()`
- `reject()`

#### Task 1.3: Install Struts2 Routing
**File**: `struts.xml`

Add actions:
```xml
<package name="approval" namespace="/app/approval">
    <action name="ferie-form" class="approvalWorkflowAction" method="ferieForm">
        <result name="input">/WEB-INF/jsp/approval/ferie-form.jsp</result>
    </action>
    
    <action name="ferie-create" class="approvalWorkflowAction" method="ferieCreate">
        <result name="success" type="redirect">/app/approval/ferie-list</result>
        <result name="input">/WEB-INF/jsp/approval/ferie-form.jsp</result>
    </action>
    
    <action name="ferie-list" class="approvalWorkflowAction" method="ferieList">
        <result name="success">/WEB-INF/jsp/approval/ferie-list.jsp</result>
    </action>
    
    <action name="approve" class="approvalWorkflowAction" method="approve">
        <result name="success" type="redirect">/app/approval/pending-approvals</result>
    </action>
</package>
```

### Week 2 - UI & Integration

#### Task 2.1: Create Approval List JSP
**File needed**: `src/main/webapp/WEB-INF/jsp/approval/ferie-list.jsp`

**Template**:
```jsp
<%-- Lista richieste ferie --%>
<table class="table">
    <thead>
        <tr>
            <th>Data Inizio</th>
            <th>Data Fine</th>
            <th>Giorni</th>
            <th>Status</th>
            <th>Azioni</th>
        </tr>
    </thead>
    <tbody>
        <s:iterator value="approvalRequests">
            <tr>
                <td><s:property value="startDate"/></td>
                <td><s:property value="endDate"/></td>
                <td><s:property value="daysRequested"/></td>
                <td><span class="badge bg-warning"><s:property value="status"/></span></td>
                <td>
                    <s:if test="status=='DRAFT'">
                        <button class="btn btn-sm btn-primary">Sottoponi</button>
                    </s:if>
                </td>
            </tr>
        </s:iterator>
    </tbody>
</table>
```

#### Task 2.2: Create Manager Approval Dashboard
**File needed**: `src/main/webapp/WEB-INF/jsp/approval/pending-approvals.jsp`

Shows pending approvals for manager with approve/reject buttons.

#### Task 2.3: Email Notifications
**File**: Create `ApprovalNotificationService.java`

Methods:
- `sendSubmissionNotification()` - Notifica a manager quando dipendente sottopone
- `sendApprovalNotification()` - Notifica a dipendente quando ferie approvate
- `sendRejectionNotification()` - Notifica a dipendente quando rifiutate

### Week 2 - Testing

#### Task 2.4: Unit Tests
**File**: `src/test/java/it/zensoftware/luna2/action/ApprovalWorkflowActionTest.java`

```java
@Test
public void testFerieCreate() {
    action.setApprovalRequest(new ApprovalRequest(...));
    String result = action.ferieCreate();
    assertEquals("success", result);
}
```

#### Task 2.5: Integration Tests
Test full workflow:
1. Dipendente crea richiesta ferie
2. Status = DRAFT
3. Sottopone richiesta
4. Status = SUBMITTED
5. Manager approva
6. Status = APPROVED
7. Vacation_balance aggiornato

---

## 🔗 Integration Checklist

- [ ] Database schema executed
- [ ] ApprovalRequest entity mapped to JPA
- [ ] ApprovalRequestDAO updated with Hibernate queries
- [ ] Struts2 routing configured
- [ ] ferie-form.jsp deployed
- [ ] ferie-list.jsp created
- [ ] pending-approvals.jsp created
- [ ] ApprovalNotificationService created + email configured
- [ ] Unit tests passing
- [ ] Integration tests passing
- [ ] UAT with business (manager + employee users)

---

## 📂 File Structure Summary

```
/workspaces/Luna2/
├── database/
│   └── approval_workflow_schema.sql [NEW]
├── src/main/java/it/zensoftware/luna2/
│   ├── model/
│   │   └── ApprovalRequest.java [NEW]
│   ├── dao/
│   │   └── ApprovalRequestDAO.java [NEW]
│   ├── action/
│   │   └── ApprovalWorkflowAction.java [NEW]
│   └── service/ (TODO)
│       └── ApprovalNotificationService.java
├── src/main/webapp/WEB-INF/jsp/approval/
│   ├── ferie-form.jsp [NEW]
│   ├── ferie-list.jsp [TODO]
│   └── pending-approvals.jsp [TODO]
└── src/main/resources/
    └── struts.xml [UPDATE NEEDED]
```

---

## 🧪 Testing URLs (After Deployment)

1. **Ferie Form**:
   ```
   http://localhost:8080/luna2/app/approval/ferie-form
   ```

2. **Create Ferie Request** (POST):
   ```
   POST http://localhost:8080/luna2/app/approval/ferie-create
   Params: startDate, endDate, description
   ```

3. **List My Requests**:
   ```
   http://localhost:8080/luna2/app/approval/ferie-list
   ```

4. **Manager Pending Approvals**:
   ```
   http://localhost:8080/luna2/app/approval/pending-approvals
   ```

---

## 🎯 Success Criteria

Sprint 1 is "DONE" when:
- ✅ All database tables created
- ✅ Employee can create ferie request
- ✅ Status transitions: DRAFT → SUBMITTED → APPROVED/REJECTED
- ✅ Manager can approve/reject with reason
- ✅ Email notifications sent
- ✅ Unit + Integration tests passing
- ✅ No critical bugs
- ✅ UAT signed off

---

## 📞 Questions / Blockers?

If you encounter issues:
1. Check database schema was executed
2. Verify Hibernate session factory configured
3. Check struts.xml routing
4. Review logs for error stack traces

**Dev Lead**: [Contact]  
**Timeline**: 14 dev days (Week 1-2 April 2026)

---

**Document**: IMPLEMENTATION_GUIDE_SPRINT1  
**Status**: READY FOR DEVELOPMENT  
**Version**: 1.0
