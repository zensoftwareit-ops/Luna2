<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="s" uri="/struts-tags" %>
<!DOCTYPE html>
<html lang="it">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Dettaglio Lead - Luna2</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.10.0/font/bootstrap-icons.css">
    <style>
        .timeline {
            position: relative;
            padding: 20px 0;
        }
        .timeline-item {
            position: relative;
            padding-left: 60px;
            padding-bottom: 30px;
        }
        .timeline-item::before {
            content: '';
            position: absolute;
            left: 20px;
            top: 0;
            bottom: -30px;
            width: 2px;
            background: #dee2e6;
        }
        .timeline-item:last-child::before {
            display: none;
        }
        .timeline-icon {
            position: absolute;
            left: 0;
            width: 40px;
            height: 40px;
            border-radius: 50%;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 1.2rem;
            color: white;
            z-index: 1;
        }
        .activity-card {
            border-left: 4px solid;
            transition: all 0.3s;
        }
        .activity-card:hover {
            box-shadow: 0 4px 8px rgba(0,0,0,0.1);
            transform: translateY(-2px);
        }
    </style>
</head>
<body>
    <%@ include file="../includes/sidebar.jsp" %>

    <div class="col-md-10 content-wrapper p-4">
        <div class="container-fluid">
            <!-- Header -->
            <div class="d-flex justify-content-between align-items-center mb-4">
                <div>
                    <nav aria-label="breadcrumb">
                        <ol class="breadcrumb mb-2">
                            <li class="breadcrumb-item"><a href="<s:url action='list' namespace='/app/lead'/>">Lead</a></li>
                            <li class="breadcrumb-item active"><s:property value="lead.azienda"/></li>
                        </ol>
                    </nav>
                    <h1 class="h3 mb-0"><s:property value="lead.azienda"/></h1>
                </div>
                <div>
                    <a href="<s:url action='edit' namespace='/app/lead'><s:param name='id' value='lead.id'/></s:url>" 
                       class="btn btn-secondary me-2">
                        <i class="bi bi-pencil me-1"></i>Modifica
                    </a>
                    <a href="<s:url action='list' namespace='/app/lead'/>" class="btn btn-outline-secondary">
                        <i class="bi bi-arrow-left me-1"></i>Torna alla lista
                    </a>
                </div>
            </div>

            <div class="row">
                <!-- Colonna Sinistra: Info Lead -->
                <div class="col-md-8">
                    <!-- Card Info Principali -->
                    <div class="card mb-3">
                        <div class="card-header bg-primary text-white">
                            <h5 class="mb-0"><i class="bi bi-info-circle me-2"></i>Informazioni Lead</h5>
                        </div>
                        <div class="card-body">
                            <div class="row">
                                <div class="col-md-6 mb-3">
                                    <label class="text-muted small">Stato</label>
                                    <div>
                                        <s:if test="lead.stato.name() == 'NUOVO'">
                                            <span class="badge bg-secondary fs-6">Nuovo</span>
                                        </s:if>
                                        <s:elseif test="lead.stato.name() == 'CONTATTATO'">
                                            <span class="badge bg-info fs-6">Contattato</span>
                                        </s:elseif>
                                        <s:elseif test="lead.stato.name() == 'QUALIFICATO'">
                                            <span class="badge bg-primary fs-6">Qualificato</span>
                                        </s:elseif>
                                        <s:elseif test="lead.stato.name() == 'PREVENTIVO'">
                                            <span class="badge bg-warning fs-6">Preventivo</span>
                                        </s:elseif>
                                        <s:elseif test="lead.stato.name() == 'NEGOZIAZIONE'">
                                            <span class="badge bg-warning fs-6">Negoziazione</span>
                                        </s:elseif>
                                        <s:elseif test="lead.stato.name() == 'VINTO'">
                                            <span class="badge bg-success fs-6">Vinto</span>
                                        </s:elseif>
                                        <s:elseif test="lead.stato.name() == 'PERSO'">
                                            <span class="badge bg-danger fs-6">Perso</span>
                                        </s:elseif>
                                    </div>
                                </div>
                                <div class="col-md-6 mb-3">
                                    <label class="text-muted small">Origine</label>
                                    <div><strong><s:property value="lead.origine"/></strong></div>
                                </div>
                                <div class="col-md-6 mb-3">
                                    <label class="text-muted small">Contatto</label>
                                    <div><strong><s:property value="lead.nomeContatto"/> <s:property value="lead.cognomeContatto"/></strong></div>
                                </div>
                                <div class="col-md-6 mb-3">
                                    <label class="text-muted small">Telefono</label>
                                    <div>
                                        <s:if test="lead.telefono != null">
                                            <a href="tel:<s:property value='lead.telefono'/>">
                                                <i class="bi bi-telephone me-1"></i><s:property value="lead.telefono"/>
                                            </a>
                                        </s:if>
                                        <s:else>-</s:else>
                                    </div>
                                </div>
                                <div class="col-md-6 mb-3">
                                    <label class="text-muted small">Email</label>
                                    <div>
                                        <s:if test="lead.email != null">
                                            <a href="mailto:<s:property value='lead.email'/>">
                                                <i class="bi bi-envelope me-1"></i><s:property value="lead.email"/>
                                            </a>
                                        </s:if>
                                        <s:else>-</s:else>
                                    </div>
                                </div>
                                <div class="col-md-6 mb-3">
                                    <label class="text-muted small">Indirizzo</label>
                                    <div>
                                        <s:if test="lead.indirizzo != null">
                                            <s:property value="lead.indirizzo"/><br>
                                            <s:property value="lead.cap"/> <s:property value="lead.citta"/> (<s:property value="lead.provincia"/>)
                                        </s:if>
                                        <s:else>-</s:else>
                                    </div>
                                </div>
                                <div class="col-md-6 mb-3">
                                    <label class="text-muted small">Budget Stimato</label>
                                    <div class="fs-5 text-primary">
                                        <s:if test="lead.budgetStimato != null">
                                            € <s:text name="format.decimal"><s:param value="lead.budgetStimato"/></s:text>
                                        </s:if>
                                        <s:else>Non specificato</s:else>
                                    </div>
                                </div>
                                <div class="col-md-6 mb-3">
                                    <label class="text-muted small">Probabilità Chiusura</label>
                                    <div>
                                        <div class="progress" style="height: 25px;">
                                            <div class="progress-bar fw-bold" role="progressbar" 
                                                 style="width: <s:property value='lead.probabilitaChiusura'/>%; 
                                                        background-color: <s:if test='lead.probabilitaChiusura >= 70'>#28a745</s:if><s:elseif test='lead.probabilitaChiusura >= 40'>#ffc107</s:elseif><s:else>#dc3545</s:else>">
                                                <s:property value="lead.probabilitaChiusura"/>%
                                            </div>
                                        </div>
                                    </div>
                                </div>
                                <div class="col-md-6 mb-3">
                                    <label class="text-muted small">Data Contatto</label>
                                    <div><s:date name="lead.dataContatto" format="dd/MM/yyyy"/></div>
                                </div>
                                <div class="col-md-6 mb-3">
                                    <label class="text-muted small">Prossimo Follow-up</label>
                                    <div>
                                        <s:if test="lead.dataProssimoFollowup != null">
                                            <s:date name="lead.dataProssimoFollowup" format="dd/MM/yyyy HH:mm"/>
                                            <s:if test="lead.dataProssimoFollowup.time < nowMillis">
                                                <span class="badge bg-danger ms-2">Scaduto</span>
                                            </s:if>
                                        </s:if>
                                        <s:else>-</s:else>
                                    </div>
                                </div>
                            </div>
                            
                            <s:if test="lead.esigenza != null && lead.esigenza.length() > 0">
                                <hr>
                                <label class="text-muted small">Esigenza</label>
                                <p class="mb-0"><s:property value="lead.esigenza" escapeHtml="false"/></p>
                            </s:if>
                            
                            <s:if test="lead.note != null && lead.note.length() > 0">
                                <hr>
                                <label class="text-muted small">Note Interne</label>
                                <p class="mb-0"><s:property value="lead.note" escapeHtml="false"/></p>
                            </s:if>
                            
                            <s:if test="lead.tags != null && lead.tags.size() > 0">
                                <hr>
                                <label class="text-muted small">Tag</label>
                                <div>
                                    <s:iterator value="lead.tags" var="tag">
                                        <span class="badge bg-secondary me-1"><s:property value="#tag.nome"/></span>
                                    </s:iterator>
                                </div>
                            </s:if>
                        </div>
                    </div>

                    <!-- Timeline Attività -->
                    <div class="card">
                        <div class="card-header bg-info text-white d-flex justify-content-between align-items-center">
                            <h5 class="mb-0"><i class="bi bi-clock-history me-2"></i>Timeline Attività</h5>
                            <button type="button" class="btn btn-light btn-sm" data-bs-toggle="modal" data-bs-target="#addActivityModal">
                                <i class="bi bi-plus-circle me-1"></i>Nuova Attività
                            </button>
                        </div>
                        <div class="card-body">
                            <s:if test="activities != null && activities.size() > 0">
                                <div class="timeline">
                                    <s:iterator value="activities" var="activity" status="stat">
                                        <div class="timeline-item">
                                            <s:if test="#activity.tipo.name() == 'CALL'">
                                                <div class="timeline-icon bg-primary">
                                                    <i class="bi bi-telephone"></i>
                                                </div>
                                            </s:if>
                                            <s:elseif test="#activity.tipo.name() == 'EMAIL'">
                                                <div class="timeline-icon bg-info">
                                                    <i class="bi bi-envelope"></i>
                                                </div>
                                            </s:elseif>
                                            <s:elseif test="#activity.tipo.name() == 'MEETING'">
                                                <div class="timeline-icon bg-success">
                                                    <i class="bi bi-people"></i>
                                                </div>
                                            </s:elseif>
                                            <s:else>
                                                <div class="timeline-icon bg-secondary">
                                                    <i class="bi bi-sticky"></i>
                                                </div>
                                            </s:else>
                                            
                                            <div class="activity-card card" style="border-left-color: 
                                                <s:if test='#activity.tipo.name() == \"CALL\"'>#0d6efd</s:if>
                                                <s:elseif test='#activity.tipo.name() == \"EMAIL\"'>#0dcaf0</s:elseif>
                                                <s:elseif test='#activity.tipo.name() == \"MEETING\"'>#198754</s:elseif>
                                                <s:else>#6c757d</s:else>">
                                                <div class="card-body">
                                                    <div class="d-flex justify-content-between">
                                                        <h6 class="mb-1"><s:property value="#activity.titolo"/></h6>
                                                        <small class="text-muted">
                                                            <s:date name="#activity.dataAttivita" format="dd/MM/yyyy HH:mm"/>
                                                        </small>
                                                    </div>
                                                    <s:if test="#activity.descrizione != null">
                                                        <p class="mb-2 text-muted"><s:property value="#activity.descrizione"/></p>
                                                    </s:if>
                                                    <div class="d-flex justify-content-between align-items-center">
                                                        <div>
                                                            <s:if test="#activity.stato.name() == 'COMPLETED'">
                                                                <span class="badge bg-success">Completata</span>
                                                            </s:if>
                                                            <s:elseif test="#activity.stato.name() == 'PENDING'">
                                                                <span class="badge bg-warning">Da fare</span>
                                                            </s:elseif>
                                                            <s:else>
                                                                <span class="badge bg-secondary">Annullata</span>
                                                            </s:else>
                                                        </div>
                                                        <s:if test="#activity.dataProssimaAttivita != null">
                                                            <small class="text-info">
                                                                <i class="bi bi-calendar-event"></i> Prossima: 
                                                                <s:date name="#activity.dataProssimaAttivita" format="dd/MM/yyyy"/>
                                                            </small>
                                                        </s:if>
                                                    </div>
                                                </div>
                                            </div>
                                        </div>
                                    </s:iterator>
                                </div>
                            </s:if>
                            <s:else>
                                <p class="text-muted text-center py-4">
                                    <i class="bi bi-inbox fs-1 d-block mb-2"></i>
                                    Nessuna attività registrata per questo lead
                                </p>
                            </s:else>
                        </div>
                    </div>
                </div>

                <!-- Colonna Destra: Actions & Tasks -->
                <div class="col-md-4">
                    <!-- Card Azioni Rapide -->
                    <div class="card mb-3">
                        <div class="card-header bg-warning">
                            <h6 class="mb-0"><i class="bi bi-lightning-charge me-2"></i>Azioni Rapide</h6>
                        </div>
                        <div class="card-body">
                            <button type="button" class="btn btn-outline-primary w-100 mb-2" data-bs-toggle="modal" data-bs-target="#changeStageModal">
                                <i class="bi bi-arrow-right-circle me-1"></i>Cambia Stato
                            </button>
                            <button type="button" class="btn btn-outline-info w-100 mb-2" data-bs-toggle="modal" data-bs-target="#addActivityModal">
                                <i class="bi bi-plus-circle me-1"></i>Aggiungi Attività
                            </button>
                            <button type="button" class="btn btn-outline-secondary w-100 mb-2" data-bs-toggle="modal" data-bs-target="#addTaskModal">
                                <i class="bi bi-calendar-check me-1"></i>Aggiungi Task
                            </button>
                            <s:if test="lead.stato.name() == 'VINTO'">
                                <a href="<s:url action='create' namespace='/app/clienti'><s:param name='leadId' value='lead.id'/></s:url>" 
                                   class="btn btn-success w-100">
                                    <i class="bi bi-person-plus me-1"></i>Converti in Cliente
                                </a>
                            </s:if>
                        </div>
                    </div>

                    <!-- Card Task Aperti -->
                    <div class="card">
                        <div class="card-header bg-success text-white">
                            <h6 class="mb-0"><i class="bi bi-check2-square me-2"></i>Task Aperti</h6>
                        </div>
                        <div class="card-body">
                            <s:if test="tasks != null && tasks.size() > 0">
                                <s:iterator value="tasks" var="task">
                                    <div class="card mb-2 <s:if test='#task.isScaduto()'>border-danger</s:if>">
                                        <div class="card-body p-2">
                                            <small class="text-muted">
                                                <i class="bi bi-calendar"></i> <s:date name="#task.dataScadenza" format="dd/MM/yyyy"/>
                                            </small>
                                            <p class="mb-1"><s:property value="#task.descrizione"/></p>
                                            <s:if test="#task.priorita.name() == 'URGENTE'">
                                                <span class="badge bg-danger">Urgente</span>
                                            </s:if>
                                            <s:elseif test="#task.priorita.name() == 'ALTA'">
                                                <span class="badge bg-warning">Alta</span>
                                            </s:elseif>
                                            <s:elseif test="#task.priorita.name() == 'MEDIA'">
                                                <span class="badge bg-info">Media</span>
                                            </s:elseif>
                                            <s:else>
                                                <span class="badge bg-secondary">Bassa</span>
                                            </s:else>
                                        </div>
                                    </div>
                                </s:iterator>
                            </s:if>
                            <s:else>
                                <p class="text-muted text-center mb-0">Nessun task aperto</p>
                            </s:else>
                        </div>
                    </div>

                    <!-- Card Storia Cambi Stato -->
                    <s:if test="stageHistory != null && stageHistory.size() > 0">
                        <div class="card mt-3">
                            <div class="card-header bg-dark text-white">
                                <h6 class="mb-0"><i class="bi bi-diagram-3 me-2"></i>Storia Stati</h6>
                            </div>
                            <div class="card-body p-0">
                                <div class="list-group list-group-flush">
                                    <s:iterator value="stageHistory" var="storia">
                                        <div class="list-group-item">
                                            <small class="text-muted d-block mb-1">
                                                <s:date name="#storia.dataCambio" format="dd/MM/yyyy HH:mm"/>
                                            </small>
                                            <div>
                                                <span class="badge bg-secondary"><s:property value="#storia.stageDa"/></span>
                                                <i class="bi bi-arrow-right mx-1"></i>
                                                <span class="badge bg-primary"><s:property value="#storia.stageA"/></span>
                                            </div>
                                            <s:if test="#storia.motivo != null">
                                                <small class="text-muted d-block mt-1"><s:property value="#storia.motivo"/></small>
                                            </s:if>
                                        </div>
                                    </s:iterator>
                                </div>
                            </div>
                        </div>
                    </s:if>
                </div>
            </div>
        </div>
    </div>

    <!-- Modal Cambia Stato -->
    <div class="modal fade" id="changeStageModal" tabindex="-1">
        <div class="modal-dialog">
            <div class="modal-content">
                <form action="<s:url action='changeStage' namespace='/app/lead'/>" method="post">
                    <input type="hidden" name="id" value="<s:property value='lead.id'/>">
                    <div class="modal-header">
                        <h5 class="modal-title">Cambia Stato Lead</h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                    </div>
                    <div class="modal-body">
                        <div class="mb-3">
                            <label class="form-label">Nuovo Stato</label>
                            <select name="newStage" class="form-select" required>
                                <option value="">Seleziona...</option>
                                <option value="NUOVO">Nuovo</option>
                                <option value="CONTATTATO">Contattato</option>
                                <option value="QUALIFICATO">Qualificato</option>
                                <option value="PREVENTIVO">Preventivo</option>
                                <option value="NEGOZIAZIONE">Negoziazione</option>
                                <option value="VINTO">Vinto</option>
                                <option value="PERSO">Perso</option>
                            </select>
                        </div>
                        <div class="mb-3">
                            <label class="form-label">Motivo (opzionale)</label>
                            <textarea name="stageChangeReason" class="form-control" rows="3"></textarea>
                        </div>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Annulla</button>
                        <button type="submit" class="btn btn-primary">Salva Cambio</button>
                    </div>
                </form>
            </div>
        </div>
    </div>

    <!-- Modal Aggiungi Attività -->
    <div class="modal fade" id="addActivityModal" tabindex="-1">
        <div class="modal-dialog">
            <div class="modal-content">
                <form action="<s:url action='addActivity' namespace='/app/lead'/>" method="post">
                    <input type="hidden" name="id" value="<s:property value='lead.id'/>">
                    <div class="modal-header">
                        <h5 class="modal-title">Nuova Attività</h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                    </div>
                    <div class="modal-body">
                        <div class="mb-3">
                            <label class="form-label">Tipo</label>
                            <select name="activityType" class="form-select" required>
                                <option value="CALL">Telefonata</option>
                                <option value="EMAIL">Email</option>
                                <option value="MEETING">Riunione</option>
                                <option value="NOTE">Nota</option>
                            </select>
                        </div>
                        <div class="mb-3">
                            <label class="form-label">Titolo</label>
                            <input type="text" name="activityTitle" class="form-control" required>
                        </div>
                        <div class="mb-3">
                            <label class="form-label">Descrizione</label>
                            <textarea name="activityDescription" class="form-control" rows="3"></textarea>
                        </div>
                        <div class="mb-3">
                            <label class="form-label">Prossima Attività (opzionale)</label>
                            <input type="datetime-local" name="nextActivityDate" class="form-control">
                        </div>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Annulla</button>
                        <button type="submit" class="btn btn-primary">Salva Attività</button>
                    </div>
                </form>
            </div>
        </div>
    </div>

    <!-- Modal Aggiungi Task -->
    <div class="modal fade" id="addTaskModal" tabindex="-1">
        <div class="modal-dialog">
            <div class="modal-content">
                <form action="<s:url action='addTask' namespace='/app/lead'/>" method="post">
                    <input type="hidden" name="id" value="<s:property value='lead.id'/>">
                    <div class="modal-header">
                        <h5 class="modal-title">Nuovo Task</h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                    </div>
                    <div class="modal-body">
                        <div class="mb-3">
                            <label class="form-label">Descrizione</label>
                            <textarea name="taskDescription" class="form-control" rows="3" required></textarea>
                        </div>
                        <div class="mb-3">
                            <label class="form-label">Data Scadenza</label>
                            <input type="date" name="taskDueDate" class="form-control" required>
                        </div>
                        <div class="mb-3">
                            <label class="form-label">Priorità</label>
                            <select name="taskPriority" class="form-select" required>
                                <option value="BASSA">Bassa</option>
                                <option value="MEDIA" selected>Media</option>
                                <option value="ALTA">Alta</option>
                                <option value="URGENTE">Urgente</option>
                            </select>
                        </div>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Annulla</button>
                        <button type="submit" class="btn btn-success">Crea Task</button>
                    </div>
                </form>
            </div>
        </div>
    </div>

    </div>
</div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
