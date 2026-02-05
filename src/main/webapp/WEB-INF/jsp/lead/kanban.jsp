<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="s" uri="/struts-tags" %>
<!DOCTYPE html>
<html lang="it">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>CRM - Lead - Luna2</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.10.0/font/bootstrap-icons.css">
    <style>
        .lead-card {
            transition: transform 0.2s;
            cursor: pointer;
        }
        .lead-card:hover {
            transform: translateY(-5px);
            box-shadow: 0 4px 12px rgba(0,0,0,0.15);
        }
        .kanban-column {
            background-color: #f8f9fa;
            border-radius: 8px;
            padding: 15px;
            min-height: 500px;
        }
        .badge-nuovo { background-color: #6c757d; }
        .badge-contattato { background-color: #17a2b8; }
        .badge-qualificato { background-color: #ffc107; color: #000; }
        .badge-proposta { background-color: #fd7e14; }
        .badge-negoziazione { background-color: #007bff; }
        .badge-vinto { background-color: #28a745; }
        .badge-perso { background-color: #dc3545; }
    </style>
</head>
<body>
    <%@ include file="../includes/sidebar.jsp" %>
    
    <div class="col-md-10 content-wrapper p-4">
        <div class="container-fluid">
            <div class="d-flex justify-content-between align-items-center mb-4">
                <h1 class="h3"><i class="bi bi-graph-up-arrow me-2"></i>CRM - Lead Management</h1>
                <div>
                    <button class="btn btn-outline-secondary me-2" onclick="toggleView()">
                        <i class="bi bi-list-ul me-2"></i>Vista Lista
                    </button>
                    <a href="<s:url action='create' namespace='/app/lead'/>" class="btn btn-primary">
                        <i class="bi bi-plus-circle me-2"></i>Nuovo Lead
                    </a>
                </div>
            </div>

            <s:if test="hasActionMessages()">
                <div class="alert alert-success alert-dismissible fade show">
                    <s:actionmessage/>
                    <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
                </div>
            </s:if>

            <!-- Kanban View -->
            <div id="kanban-view">
                <div class="row g-3">
                    <!-- Nuovo -->
                    <div class="col-md-2">
                        <div class="kanban-column">
                            <h6 class="text-center mb-3">
                                <span class="badge badge-nuovo w-100">NUOVO</span>
                            </h6>
                            <s:iterator value="leads">
                                <s:if test="stato.name() == 'NUOVO'">
                                    <div class="card lead-card mb-2" onclick="viewLead(<s:property value='id'/>)">
                                        <div class="card-body p-2">
                                            <h6 class="card-title mb-1 small"><s:property value="azienda"/></h6>
                                            <p class="card-text small text-muted mb-1">
                                                <i class="bi bi-person"></i> <s:property value="nome"/> <s:property value="cognome"/>
                                            </p>
                                            <p class="card-text small mb-0">
                                                <strong>€ <s:property value="valoreStimato"/></strong>
                                            </p>
                                        </div>
                                    </div>
                                </s:if>
                            </s:iterator>
                        </div>
                    </div>

                    <!-- Contattato -->
                    <div class="col-md-2">
                        <div class="kanban-column">
                            <h6 class="text-center mb-3">
                                <span class="badge badge-contattato w-100">CONTATTATO</span>
                            </h6>
                            <s:iterator value="leads">
                                <s:if test="stato.name() == 'CONTATTATO'">
                                    <div class="card lead-card mb-2" onclick="viewLead(<s:property value='id'/>)">
                                        <div class="card-body p-2">
                                            <h6 class="card-title mb-1 small"><s:property value="azienda"/></h6>
                                            <p class="card-text small text-muted mb-1">
                                                <i class="bi bi-person"></i> <s:property value="nome"/> <s:property value="cognome"/>
                                            </p>
                                            <p class="card-text small mb-0">
                                                <strong>€ <s:property value="valoreStimato"/></strong>
                                            </p>
                                        </div>
                                    </div>
                                </s:if>
                            </s:iterator>
                        </div>
                    </div>

                    <!-- Qualificato -->
                    <div class="col-md-2">
                        <div class="kanban-column">
                            <h6 class="text-center mb-3">
                                <span class="badge badge-qualificato w-100">QUALIFICATO</span>
                            </h6>
                            <s:iterator value="leads">
                                <s:if test="stato.name() == 'QUALIFICATO'">
                                    <div class="card lead-card mb-2" onclick="viewLead(<s:property value='id'/>)">
                                        <div class="card-body p-2">
                                            <h6 class="card-title mb-1 small"><s:property value="azienda"/></h6>
                                            <p class="card-text small text-muted mb-1">
                                                <i class="bi bi-person"></i> <s:property value="nome"/> <s:property value="cognome"/>
                                            </p>
                                            <p class="card-text small mb-0">
                                                <strong>€ <s:property value="valoreStimato"/></strong>
                                            </p>
                                        </div>
                                    </div>
                                </s:if>
                            </s:iterator>
                        </div>
                    </div>

                    <!-- Proposta -->
                    <div class="col-md-2">
                        <div class="kanban-column">
                            <h6 class="text-center mb-3">
                                <span class="badge badge-proposta w-100">PROPOSTA</span>
                            </h6>
                            <s:iterator value="leads">
                                <s:if test="stato.name() == 'PROPOSTA'">
                                    <div class="card lead-card mb-2" onclick="viewLead(<s:property value='id'/>)">
                                        <div class="card-body p-2">
                                            <h6 class="card-title mb-1 small"><s:property value="azienda"/></h6>
                                            <p class="card-text small text-muted mb-1">
                                                <i class="bi bi-person"></i> <s:property value="nome"/> <s:property value="cognome"/>
                                            </p>
                                            <p class="card-text small mb-0">
                                                <strong>€ <s:property value="valoreStimato"/></strong>
                                            </p>
                                        </div>
                                    </div>
                                </s:if>
                            </s:iterator>
                        </div>
                    </div>

                    <!-- Negoziazione -->
                    <div class="col-md-2">
                        <div class="kanban-column">
                            <h6 class="text-center mb-3">
                                <span class="badge badge-negoziazione w-100">NEGOZIAZIONE</span>
                            </h6>
                            <s:iterator value="leads">
                                <s:if test="stato.name() == 'NEGOZIAZIONE'">
                                    <div class="card lead-card mb-2" onclick="viewLead(<s:property value='id'/>)">
                                        <div class="card-body p-2">
                                            <h6 class="card-title mb-1 small"><s:property value="azienda"/></h6>
                                            <p class="card-text small text-muted mb-1">
                                                <i class="bi bi-person"></i> <s:property value="nome"/> <s:property value="cognome"/>
                                            </p>
                                            <p class="card-text small mb-0">
                                                <strong>€ <s:property value="valoreStimato"/></strong>
                                            </p>
                                        </div>
                                    </div>
                                </s:if>
                            </s:iterator>
                        </div>
                    </div>

                    <!-- Chiusi (Vinto/Perso) -->
                    <div class="col-md-2">
                        <div class="kanban-column">
                            <h6 class="text-center mb-3">
                                <span class="badge bg-secondary w-100">CHIUSI</span>
                            </h6>
                            <s:iterator value="leads">
                                <s:if test="stato.name() == 'VINTO' || stato.name() == 'PERSO'">
                                    <div class="card lead-card mb-2" onclick="viewLead(<s:property value='id'/>)">
                                        <div class="card-body p-2">
                                            <div class="d-flex justify-content-between align-items-start">
                                                <h6 class="card-title mb-1 small"><s:property value="azienda"/></h6>
                                                <span class="badge badge-<s:property value='stato.name().toLowerCase()'/>">
                                                    <s:if test="stato.name() == 'VINTO'">
                                                        <i class="bi bi-check-circle"></i>
                                                    </s:if>
                                                    <s:else>
                                                        <i class="bi bi-x-circle"></i>
                                                    </s:else>
                                                </span>
                                            </div>
                                            <p class="card-text small text-muted mb-1">
                                                <i class="bi bi-person"></i> <s:property value="nome"/> <s:property value="cognome"/>
                                            </p>
                                            <p class="card-text small mb-0">
                                                <strong>€ <s:property value="valoreStimato"/></strong>
                                            </p>
                                        </div>
                                    </div>
                                </s:if>
                            </s:iterator>
                        </div>
                    </div>
                </div>
            </div>

            <!-- Statistics -->
            <div class="row mt-4">
                <div class="col-md-12">
                    <div class="card">
                        <div class="card-body">
                            <h5 class="card-title">Pipeline Overview</h5>
                            <div class="row text-center">
                                <div class="col-md-2">
                                    <h3 class="text-primary"><s:property value="leadAperti"/></h3>
                                    <p class="text-muted">Lead Aperti</p>
                                </div>
                                <div class="col-md-2">
                                    <h3 class="text-success">€ <s:property value="valoreStimato"/></h3>
                                    <p class="text-muted">Valore Pipeline</p>
                                </div>
                                <div class="col-md-2">
                                    <h3 class="text-success"><s:property value="leadVinti"/></h3>
                                    <p class="text-muted">Lead Vinti (mese)</p>
                                </div>
                                <div class="col-md-2">
                                    <h3 class="text-info"><s:property value="tassoConversione"/>%</h3>
                                    <p class="text-muted">Tasso Conversione</p>
                                </div>
                                <div class="col-md-2">
                                    <h3 class="text-warning"><s:property value="tempoMedioChiusura"/></h3>
                                    <p class="text-muted">Giorni Medi Chiusura</p>
                                </div>
                                <div class="col-md-2">
                                    <h3 class="text-success">€ <s:property value="valoreMedioVinto"/></h3>
                                    <p class="text-muted">Valore Medio Vinto</p>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>

    </div>
</div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
    <script>
        function viewLead(id) {
            window.location.href = '<s:url action="view" namespace="/app/lead"/>' + '?id=' + id;
        }

        function toggleView() {
            window.location.href = '<s:url action="list" namespace="/app/lead"/>';
        }
    </script>
</body>
</html>
