<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="s" uri="/struts-tags" %>
<!DOCTYPE html>
<html lang="it">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>CRM / Lead - Luna2</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.10.0/font/bootstrap-icons.css">
    <link rel="stylesheet" href="https://cdn.datatables.net/1.13.4/css/dataTables.bootstrap5.min.css">
    <style>
        .badge-stato {
            min-width: 90px;
            padding: 0.4rem 0.8rem;
        }
        .lead-row:hover {
            background-color: #f8f9fa;
            cursor: pointer;
        }
        .probabilita-bar {
            height: 20px;
            border-radius: 4px;
            background: linear-gradient(90deg, #28a745 0%, #ffc107 50%, #dc3545 100%);
        }
    </style>
</head>
<body>
    <%@ include file="../includes/sidebar.jsp" %>

    <div class="col-md-10 content-wrapper p-4">
        <div class="container-fluid">
            <!-- Header -->
            <div class="d-flex justify-content-between align-items-center mb-4">
                <h1 class="h3"><i class="bi bi-graph-up-arrow me-2"></i>CRM / Lead</h1>
                <div>
                    <a href="<s:url action='pipeline' namespace='/app/lead'/>" class="btn btn-outline-secondary me-2">
                        <i class="bi bi-kanban me-1"></i>Vista Pipeline
                    </a>
                    <a href="<s:url action='create' namespace='/app/lead'/>" class="btn btn-primary">
                        <i class="bi bi-plus-circle me-1"></i>Nuovo Lead
                    </a>
                </div>
            </div>

            <!-- KPI Cards -->
            <div class="row mb-4">
                <div class="col-md-3">
                    <div class="card border-primary">
                        <div class="card-body">
                            <div class="d-flex justify-content-between align-items-center">
                                <div>
                                    <h6 class="text-muted mb-1">Lead Attivi</h6>
                                    <h3 class="mb-0"><s:property value="leads.size()"/></h3>
                                </div>
                                <i class="bi bi-people fs-1 text-primary"></i>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="col-md-3">
                    <div class="card border-warning">
                        <div class="card-body">
                            <div class="d-flex justify-content-between align-items-center">
                                <div>
                                    <h6 class="text-muted mb-1">In Negoziazione</h6>
                                    <h3 class="mb-0"><s:property value="leads.{? #this.stato == @it.zensoftware.luna2.model.Lead$Stato@NEGOZIAZIONE}.size()"/></h3>
                                </div>
                                <i class="bi bi-hourglass-split fs-1 text-warning"></i>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="col-md-3">
                    <div class="card border-success">
                        <div class="card-body">
                            <div class="d-flex justify-content-between align-items-center">
                                <div>
                                    <h6 class="text-muted mb-1">Vinti (Mese)</h6>
                                    <h3 class="mb-0"><s:property value="leads.{? #this.stato == @it.zensoftware.luna2.model.Lead$Stato@VINTO}.size()"/></h3>
                                </div>
                                <i class="bi bi-trophy fs-1 text-success"></i>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="col-md-3">
                    <div class="card border-info">
                        <div class="card-body">
                            <div class="d-flex justify-content-between align-items-center">
                                <div>
                                    <h6 class="text-muted mb-1">Valore Pipeline</h6>
                                    <h3 class="mb-0">€ <s:property value="getText('{0,number,#,##0.00}', {leads.{budgetStimato}.sum()})"/></h3>
                                </div>
                                <i class="bi bi-cash-stack fs-1 text-info"></i>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <!-- Filters -->
            <div class="card mb-3">
                <div class="card-body">
                    <form method="get" class="row g-3">
                        <div class="col-md-4">
                            <input type="text" class="form-control" name="searchTerm" 
                                   placeholder="Cerca azienda o contatto..." value="<s:property value='searchTerm'/>">
                        </div>
                        <div class="col-md-3">
                            <select class="form-select" name="statoFiltro">
                                <option value="">Tutti gli stati</option>
                                <option value="NUOVO" <s:if test="statoFiltro == 'NUOVO'">selected</s:if>>Nuovo</option>
                                <option value="CONTATTATO" <s:if test="statoFiltro == 'CONTATTATO'">selected</s:if>>Contattato</option>
                                <option value="QUALIFICATO" <s:if test="statoFiltro == 'QUALIFICATO'">selected</s:if>>Qualificato</option>
                                <option value="PREVENTIVO" <s:if test="statoFiltro == 'PREVENTIVO'">selected</s:if>>Preventivo</option>
                                <option value="NEGOZIAZIONE" <s:if test="statoFiltro == 'NEGOZIAZIONE'">selected</s:if>>Negoziazione</option>
                                <option value="VINTO" <s:if test="statoFiltro == 'VINTO'">selected</s:if>>Vinto</option>
                                <option value="PERSO" <s:if test="statoFiltro == 'PERSO'">selected</s:if>>Perso</option>
                            </select>
                        </div>
                        <div class="col-md-2">
                            <button type="submit" class="btn btn-primary w-100">
                                <i class="bi bi-search me-1"></i>Filtra
                            </button>
                        </div>
                        <div class="col-md-3 text-end">
                            <a href="<s:url action='export' namespace='/app/lead'/>" class="btn btn-outline-success">
                                <i class="bi bi-file-excel me-1"></i>Esporta Excel
                            </a>
                        </div>
                    </form>
                </div>
            </div>

            <!-- Lead Table -->
            <div class="card">
                <div class="card-body">
                    <table id="leadsTable" class="table table-hover">
                        <thead>
                            <tr>
                                <th>Azienda</th>
                                <th>Contatto</th>
                                <th>Telefono/Email</th>
                                <th>Stato</th>
                                <th>Budget Stimato</th>
                                <th>Probabilità</th>
                                <th>Data Contatto</th>
                                <th>Prossimo Follow-up</th>
                                <th>Azioni</th>
                            </tr>
                        </thead>
                        <tbody>
                            <s:iterator value="leads" var="lead">
                                <tr class="lead-row" onclick="window.location='<s:url action='view' namespace='/app/lead'><s:param name='id' value='#lead.id'/></s:url>'">
                                    <td>
                                        <strong><s:property value="#lead.azienda"/></strong>
                                        <s:if test="#lead.tags != null && #lead.tags.size() > 0">
                                            <br>
                                            <s:iterator value="#lead.tags" var="tag">
                                                <span class="badge bg-secondary me-1"><s:property value="#tag.nome"/></span>
                                            </s:iterator>
                                        </s:if>
                                    </td>
                                    <td>
                                        <s:property value="#lead.nomeContatto"/> <s:property value="#lead.cognomeContatto"/>
                                    </td>
                                    <td>
                                        <s:if test="#lead.telefono != null">
                                            <i class="bi bi-telephone"></i> <s:property value="#lead.telefono"/><br>
                                        </s:if>
                                        <s:if test="#lead.email != null">
                                            <i class="bi bi-envelope"></i> <s:property value="#lead.email"/>
                                        </s:if>
                                    </td>
                                    <td>
                                        <s:if test="#lead.stato.name() == 'NUOVO'">
                                            <span class="badge bg-secondary badge-stato">Nuovo</span>
                                        </s:if>
                                        <s:elseif test="#lead.stato.name() == 'CONTATTATO'">
                                            <span class="badge bg-info badge-stato">Contattato</span>
                                        </s:elseif>
                                        <s:elseif test="#lead.stato.name() == 'QUALIFICATO'">
                                            <span class="badge bg-primary badge-stato">Qualificato</span>
                                        </s:elseif>
                                        <s:elseif test="#lead.stato.name() == 'PREVENTIVO'">
                                            <span class="badge bg-warning badge-stato">Preventivo</span>
                                        </s:elseif>
                                        <s:elseif test="#lead.stato.name() == 'NEGOZIAZIONE'">
                                            <span class="badge bg-warning badge-stato">Negoziazione</span>
                                        </s:elseif>
                                        <s:elseif test="#lead.stato.name() == 'VINTO'">
                                            <span class="badge bg-success badge-stato">Vinto</span>
                                        </s:elseif>
                                        <s:elseif test="#lead.stato.name() == 'PERSO'">
                                            <span class="badge bg-danger badge-stato">Perso</span>
                                        </s:elseif>
                                    </td>
                                    <td>
                                        <s:if test="#lead.budgetStimato != null">
                                            € <s:text name="format.decimal"><s:param value="#lead.budgetStimato"/></s:text>
                                        </s:if>
                                        <s:else>-</s:else>
                                    </td>
                                    <td>
                                        <div class="d-flex align-items-center">
                                            <div class="progress flex-grow-1 me-2" style="height: 20px;">
                                                <div class="progress-bar" role="progressbar" 
                                                     style="width: <s:property value='#lead.probabilitaChiusura'/>%; 
                                                            background-color: <s:if test='#lead.probabilitaChiusura >= 70'>#28a745</s:if><s:elseif test='#lead.probabilitaChiusura >= 40'>#ffc107</s:elseif><s:else>#dc3545</s:else>">
                                                    <s:property value="#lead.probabilitaChiusura"/>%
                                                </div>
                                            </div>
                                        </div>
                                    </td>
                                    <td>
                                        <s:date name="#lead.dataContatto" format="dd/MM/yyyy"/>
                                    </td>
                                    <td>
                                        <s:if test="#lead.dataProssimoFollowup != null">
                                            <s:date name="#lead.dataProssimoFollowup" format="dd/MM/yyyy"/>
                                            <s:if test="#lead.dataProssimoFollowup.time < nowMillis">
                                                <span class="badge bg-danger ms-1">Scaduto</span>
                                            </s:if>
                                        </s:if>
                                        <s:else>-</s:else>
                                    </td>
                                    <td onclick="event.stopPropagation()">
                                        <div class="btn-group btn-group-sm" role="group">
                                            <a href="<s:url action='view' namespace='/app/lead'><s:param name='id' value='#lead.id'/></s:url>" 
                                               class="btn btn-outline-primary" title="Visualizza">
                                                <i class="bi bi-eye"></i>
                                            </a>
                                            <a href="<s:url action='edit' namespace='/app/lead'><s:param name='id' value='#lead.id'/></s:url>" 
                                               class="btn btn-outline-secondary" title="Modifica">
                                                <i class="bi bi-pencil"></i>
                                            </a>
                                            <a href="<s:url action='delete' namespace='/app/lead'><s:param name='id' value='#lead.id'/></s:url>" 
                                               class="btn btn-outline-danger" title="Elimina"
                                               onclick="return confirm('Eliminare questo lead?')">
                                                <i class="bi bi-trash"></i>
                                            </a>
                                        </div>
                                    </td>
                                </tr>
                            </s:iterator>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    </div>

    </div>
</div>

    <script src="https://code.jquery.com/jquery-3.7.0.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
    <script src="https://cdn.datatables.net/1.13.4/js/jquery.dataTables.min.js"></script>
    <script src="https://cdn.datatables.net/1.13.4/js/dataTables.bootstrap5.min.js"></script>
    <script>
        $(document).ready(function() {
            $('#leadsTable').DataTable({
                language: {
                    url: '//cdn.datatables.net/plug-ins/1.13.4/i18n/it-IT.json'
                },
                order: [[6, 'desc']],
                pageLength: 25,
                responsive: true
            });
        });
    </script>
</body>
</html>
