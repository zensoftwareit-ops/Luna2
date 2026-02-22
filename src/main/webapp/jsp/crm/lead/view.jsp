<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib uri="http://struts.apache.org/tags-struts2" prefix="s" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Dettagli Lead - Luna2 CRM</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css" rel="stylesheet">
    <style>
        .lead-header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 30px; }
        .info-card { background: white; border-radius: 8px; padding: 20px; margin-bottom: 20px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
        .label-key { font-weight: 600; color: #495057; }
        .timeline { position: relative; padding: 20px 0; }
        .timeline-item { display: flex; margin-bottom: 30px; }
        .timeline-marker { width: 40px; height: 40px; background: #667eea; border-radius: 50%; display: flex; align-items: center; justify-content: center; color: white; font-weight: bold; margin-right: 20px; flex-shrink: 0; }
        .timeline-content { flex: 1; }
        .timeline-date { font-size: 0.85rem; color: #6c757d; margin-bottom: 5px; }
        .activity-badge { display: inline-block; padding: 3px 8px; border-radius: 3px; font-size: 0.75rem; font-weight: 600; margin-bottom: 8px; }
        .badge-call { background: #d4edff; color: #0c5aa0; }
        .badge-email { background: #fff3cd; color: #856404; }
        .badge-note { background: #e2e3e5; color: #383d41; }
        .badge-meeting { background: #d1ecf1; color: #0c5aa0; }
        .task-item { background: #f8f9fa; padding: 12px; border-left: 4px solid #667eea; margin-bottom: 10px; border-radius: 4px; }
        .task-overdue { border-left-color: #dc3545; }
        .task-completed { opacity: 0.6; border-left-color: #28a745; }
        .stat-box { text-align: center; padding: 15px; }
        .stat-number { font-size: 1.8rem; font-weight: 700; color: #667eea; }
        .stat-label { font-size: 0.85rem; color: #6c757d; text-transform: uppercase; letter-spacing: 0.5px; }
    </style>
</head>
<body>
<!-- HEADER -->
<div class="lead-header">
    <div class="container-fluid">
        <div class="row align-items-center">
            <div class="col-md-8">
                <h2><i class="fas fa-user-circle"></i> <s:property value="lead.nomeContatto" /> <s:property value="lead.cognomeContatto" /></h2>
                <p class="mb-0"><strong><s:property value="lead.azienda" /></strong></p>
            </div>
            <div class="col-md-4 text-end">
                <span class="badge bg-light text-dark" style="font-size: 1rem; padding: 10px 15px;">
                    <i class="fas fa-bullseye"></i> Valore Stimato: <strong>€ <s:property value="valoreStimato" /></strong>
                </span>
            </div>
        </div>
    </div>
</div>

<!-- MAIN CONTENT -->
<div class="container-fluid" style="padding: 30px;">
    <div class="row">
        <!-- LEFT COLUMN: Lead Info + Stats -->
        <div class="col-lg-4">
            <!-- CONTACT INFO -->
            <div class="info-card">
                <h5 class="mb-3"><i class="fas fa-address-card"></i> Informazioni Contatto</h5>
                <div class="row mb-3">
                    <div class="col-sm-4"><span class="label-key">Email:</span></div>
                    <div class="col-sm-8"><a href="mailto:<s:property value='lead.email' />"><s:property value="lead.email" /></a></div>
                </div>
                <div class="row mb-3">
                    <div class="col-sm-4"><span class="label-key">Telefono:</span></div>
                    <div class="col-sm-8"><a href="tel:<s:property value='lead.telefono' />"><s:property value="lead.telefono" /></a></div>
                </div>
                <div class="row mb-3">
                    <div class="col-sm-4"><span class="label-key">Indirizzo:</span></div>
                    <div class="col-sm-8"><s:property value="lead.indirizzo" /> <s:property value="lead.citta" /> (<s:property value="lead.provincia" />)</div>
                </div>
            </div>

            <!-- PIPELINE INFO -->
            <div class="info-card">
                <h5 class="mb-3"><i class="fas fa-pipe"></i> Pipeline</h5>
                <div class="row mb-3">
                    <div class="col-sm-5"><span class="label-key">Stato Attuale:</span></div>
                    <div class="col-sm-7">
                        <span class="badge bg-primary" style="font-size: 0.9rem;">
                            <s:property value="lead.stato" />
                        </span>
                    </div>
                </div>
                <div class="row mb-3">
                    <div class="col-sm-5"><span class="label-key">Budget Stimato:</span></div>
                    <div class="col-sm-7"><strong>€ <s:property value="lead.budgetStimato" /></strong></div>
                </div>
                <div class="row mb-3">
                    <div class="col-sm-5"><span class="label-key">Probabilità:</span></div>
                    <div class="col-sm-7">
                        <div class="progress">
                            <div class="progress-bar" role="progressbar" style="width: <s:property value='lead.probabilitaChiusura' />%;">
                                <s:property value="lead.probabilitaChiusura" />%
                            </div>
                        </div>
                    </div>
                </div>
                <button class="btn btn-sm btn-primary w-100" data-bs-toggle="modal" data-bs-target="#stageChangeModal">
                    <i class="fas fa-exchange-alt"></i> Cambia Stage
                </button>
            </div>

            <!-- QUICK STATS -->
            <div class="info-card">
                <h5 class="mb-3"><i class="fas fa-chart-bar"></i> Statistiche</h5>
                <div class="row">
                    <div class="col-6">
                        <div class="stat-box">
                            <div class="stat-number"><s:property value="activities.size()" /></div>
                            <div class="stat-label">Attività</div>
                        </div>
                    </div>
                    <div class="col-6">
                        <div class="stat-box">
                            <div class="stat-number"><s:property value="tasks.size()" /></div>
                            <div class="stat-label">Task</div>
                        </div>
                    </div>
                </div>
            </div>

            <!-- ACTION BUTTONS -->
            <div class="info-card">
                <a href="/crm/lead!edit?id=<s:property value='lead.id'/>" class="btn btn-warning w-100 mb-2">
                    <i class="fas fa-edit"></i> Modifica Lead
                </a>
                <button class="btn btn-info w-100 mb-2" data-bs-toggle="modal" data-bs-target="#activityModal">
                    <i class="fas fa-plus"></i> Aggiungi Attività
                </button>
                <button class="btn btn-success w-100" data-bs-toggle="modal" data-bs-target="#taskModal">
                    <i class="fas fa-tasks"></i> Aggiungi Task
                </button>
            </div>
        </div>

        <!-- RIGHT COLUMN: Timeline + History -->
        <div class="col-lg-8">
            <!-- ACTIVITY TIMELINE -->
            <div class="info-card">
                <h5 class="mb-4"><i class="fas fa-history"></i> Timeline Attività</h5>
                <div class="timeline">
                    <s:iterator value="activities" var="activity">
                        <div class="timeline-item">
                            <div class="timeline-marker">
                                <i class="fas fa-circle" style="font-size: 0.6rem;"></i>
                            </div>
                            <div class="timeline-content">
                                <div class="timeline-date">
                                    <s:date name="#activity.dataAttivita" format="dd/MM/yyyy HH:mm" />
                                </div>
                                <div class="activity-badge" style="background: #d4edff; color: #0c5aa0;" id="badge-<s:property value='#activity.tipo'/>">
                                    <i class="fas fa-phone"></i> <s:property value="#activity.tipo" />
                                </div>
                                <strong><s:property value="#activity.titolo" /></strong>
                                <p class="mb-0" style="margin-top: 8px; font-size: 0.9rem; color: #495057;">
                                    <s:property value="#activity.descrizione" />
                                </p>
                                <small class="text-muted">
                                    di <s:property value="#activity.utente.nome" /> <s:property value="#activity.utente.cognome" />
                                </small>
                            </div>
                        </div>
                    </s:iterator>
                </div>
            </div>

            <!-- TASK LIST -->
            <div class="info-card">
                <h5 class="mb-3"><i class="fas fa-tasks"></i> Task Pendenti</h5>
                <s:iterator value="tasks" var="task">
                    <div class="task-item <s:if test='#task.stato.toString() == \"COMPLETED\"'>task-completed</s:if><s:if test='#task.stato.toString() == \"CANCELLED\"'>task-cancelled</s:if>">
                        <div class="d-flex justify-content-between align-items-start">
                            <div>
                                <strong><s:property value="#task.descrizione" /></strong>
                                <div style="margin-top: 5px;">
                                    <span class="badge bg-warning text-dark"><s:property value="#task.priorita" /></span>
                                    <span class="badge bg-secondary">Scadenza: <s:date name="#task.dataScadenza" format="dd/MM/yyyy" /></span>
                                </div>
                            </div>
                            <div>
                                <s:if test='#task.stato.toString() == "COMPLETED"'>
                                    <i class="fas fa-check-circle" style="color: #28a745; font-size: 1.2rem;"></i>
                                </s:if>
                                <s:else>
                                    <button class="btn btn-sm btn-success" onclick="completeTask(<s:property value='#task.id'/>)">
                                        ✓
                                    </button>
                                </s:else>
                            </div>
                        </div>
                    </div>
                </s:iterator>
            </div>
        </div>
    </div>
</div>

<!-- MODALS -->
<!-- Modal Cambio Stage -->
<div class="modal fade" id="stageChangeModal" tabindex="-1">
    <div class="modal-dialog">
        <div class="modal-content">
            <div class="modal-header">
                <h5 class="modal-title"><i class="fas fa-exchange-alt"></i> Cambia Stage</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
            </div>
            <form id="stageChangeForm">
                <div class="modal-body">
                    <div class="mb-3">
                        <label for="newStage" class="form-label">Nuovo Stage</label>
                        <select id="newStage" name="newStage" class="form-select" required>
                            <option value="">-- Seleziona Stage --</option>
                            <option value="BOZZA">🔵 BOZZA</option>
                            <option value="QUALIFICATO">🟢 QUALIFICATO</option>
                            <option value="PROPOSTA">🟡 PROPOSTA</option>
                            <option value="NEGOZIAZIONE">🟠 NEGOZIAZIONE</option>
                            <option value="VINTO">✅ VINTO</option>
                            <option value="PERSO">❌ PERSO</option>
                        </select>
                    </div>
                    <div class="mb-3">
                        <label for="stageReason" class="form-label">Motivo del Cambio</label>
                        <textarea id="stageReason" name="stageChangeReason" class="form-control" rows="3" placeholder="Spiega il motivo..."></textarea>
                    </div>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Annulla</button>
                    <button type="button" class="btn btn-primary" onclick="changeStage()">Cambia Stage</button>
                </div>
            </form>
        </div>
    </div>
</div>

<!-- Modal Aggiungi Attività -->
<div class="modal fade" id="activityModal" tabindex="-1">
    <div class="modal-dialog">
        <div class="modal-content">
            <div class="modal-header">
                <h5 class="modal-title"><i class="fas fa-plus"></i> Aggiungi Attività</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
            </div>
            <form id="activityForm">
                <div class="modal-body">
                    <div class="mb-3">
                        <label for="actType" class="form-label">Tipo Attività</label>
                        <select id="actType" name="activityType" class="form-select" required>
                            <option value="">-- Seleziona Tipo --</option>
                            <option value="CALL">☎️ Telefonata</option>
                            <option value="EMAIL">✉️ Email</option>
                            <option value="MEETING">👥 Riunione</option>
                            <option value="NOTE">📝 Nota</option>
                        </select>
                    </div>
                    <div class="mb-3">
                        <label for="actTitle" class="form-label">Titolo</label>
                        <input type="text" id="actTitle" name="activityTitle" class="form-control" required>
                    </div>
                    <div class="mb-3">
                        <label for="actDesc" class="form-label">Descrizione</label>
                        <textarea id="actDesc" name="activityDescription" class="form-control" rows="3"></textarea>
                    </div>
                    <div class="mb-3">
                        <label for="nextDate" class="form-label">Prossima Data (opzionale)</label>
                        <input type="datetime-local" id="nextDate" name="nextActivityDate" class="form-control">
                    </div>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Annulla</button>
                    <button type="button" class="btn btn-primary" onclick="addActivity()">Aggiungi Attività</button>
                </div>
            </form>
        </div>
    </div>
</div>

<!-- Modal Aggiungi Task -->
<div class="modal fade" id="taskModal" tabindex="-1">
    <div class="modal-dialog">
        <div class="modal-content">
            <div class="modal-header">
                <h5 class="modal-title"><i class="fas fa-tasks"></i> Aggiungi Task</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
            </div>
            <form id="taskForm">
                <div class="modal-body">
                    <div class="mb-3">
                        <label for="taskDesc" class="form-label">Descrizione Task</label>
                        <input type="text" id="taskDesc" name="taskDescription" class="form-control" required>
                    </div>
                    <div class="mb-3">
                        <label for="taskDue" class="form-label">Data Scadenza</label>
                        <input type="date" id="taskDue" name="taskDueDate" class="form-control" required>
                    </div>
                    <div class="mb-3">
                        <label for="taskPriority" class="form-label">Priorità</label>
                        <select id="taskPriority" name="taskPriority" class="form-select">
                            <option value="BASSA">Bassa</option>
                            <option value="MEDIA" selected>Media</option>
                            <option value="ALTA">Alta</option>
                            <option value="URGENTE">Urgente</option>
                        </select>
                    </div>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Annulla</button>
                    <button type="button" class="btn btn-primary" onclick="addTask()">Crea Task</button>
                </div>
            </form>
        </div>
    </div>
</div>

<script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>

<script>
const leadId = <s:property value="lead.id" />;

function changeStage() {
    const newStage = document.getElementById('newStage').value;
    const reason = document.getElementById('stageReason').value;
    
    $.ajax({
        url: '/crm/lead!changeStage.action',
        type: 'POST',
        data: {
            id: leadId,
            newStage: newStage,
            stageChangeReason: reason
        },
        success: function(response) {
            if (response.success) {
                alert(response.message);
                location.reload();
            }
        }
    });
}

function addActivity() {
    const type = document.getElementById('actType').value;
    const title = document.getElementById('actTitle').value;
    const desc = document.getElementById('actDesc').value;
    const nextDate = document.getElementById('nextDate').value;
    
    $.ajax({
        url: '/crm/lead!addActivity.action',
        type: 'POST',
        data: {
            id: leadId,
            activityType: type,
            activityTitle: title,
            activityDescription: desc,
            nextActivityDate: nextDate
        },
        success: function(response) {
            if (response.success) {
                alert('Attività aggiunta con successo');
                location.reload();
            }
        }
    });
}

function addTask() {
    const desc = document.getElementById('taskDesc').value;
    const dueDate = document.getElementById('taskDue').value;
    const priority = document.getElementById('taskPriority').value;
    
    $.ajax({
        url: '/crm/lead!addTask.action',
        type: 'POST',
        data: {
            id: leadId,
            taskDescription: desc,
            taskDueDate: dueDate,
            taskPriority: priority
        },
        success: function(response) {
            if (response.success) {
                alert('Task creato con successo');
                location.reload();
            }
        }
    });
}

function completeTask(taskId) {
    $.ajax({
        url: '/crm/activity!completeActivity.action',
        type: 'POST',
        data: { id: taskId },
        success: function() {
            location.reload();
        }
    });
}
</script>
</body>
</html>
