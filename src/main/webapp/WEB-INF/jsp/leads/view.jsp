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
</head>
<body>
    <%@ include file="../includes/sidebar.jsp" %>
    
    <div class="col-md-10 content-wrapper p-4">
        <div class="container-fluid">
            <div class="d-flex justify-content-between align-items-center mb-4">
                <h1 class="h3">
                    <i class="bi bi-person-badge me-2"></i>
                    Dettaglio Lead
                </h1>
                <div>
                    <a href="<s:url action='edit' namespace='/app/leads'><s:param name='id' value='lead.id'/></s:url>" class="btn btn-primary">
                        <i class="bi bi-pencil me-2"></i>Modifica
                    </a>
                    <a href="<s:url action='list' namespace='/app/leads'/>" class="btn btn-secondary">
                        <i class="bi bi-arrow-left me-2"></i>Torna all'elenco
                    </a>
                </div>
            </div>

            <div class="row">
                <div class="col-md-8">
                    <div class="card mb-4">
                        <div class="card-header bg-primary text-white">
                            <h5 class="mb-0"><i class="bi bi-info-circle me-2"></i>Informazioni Lead</h5>
                        </div>
                        <div class="card-body">
                            <div class="row mb-3">
                                <div class="col-md-12">
                                    <label class="text-muted small">Azienda</label>
                                    <p class="h4"><s:property value="lead.azienda"/></p>
                                </div>
                            </div>

                            <div class="row mb-3">
                                <div class="col-md-6">
                                    <label class="text-muted small">Contatto</label>
                                    <p><s:property value="lead.nomeContattoCompleto"/></p>
                                </div>
                                <div class="col-md-6">
                                    <label class="text-muted small">Stato</label>
                                    <p>
                                        <span class="badge bg-<s:if test='lead.stato.name() == "NUOVO"'>secondary</s:if><s:elseif test='lead.stato.name() == "CONTATTATO"'>info</s:elseif><s:elseif test='lead.stato.name() == "QUALIFICATO"'>primary</s:elseif><s:elseif test='lead.stato.name() == "PREVENTIVO"'>warning</s:elseif><s:elseif test='lead.stato.name() == "NEGOZIAZIONE"'>danger</s:elseif><s:elseif test='lead.stato.name() == "VINTO"'>success</s:elseif><s:else>dark</s:else> fs-6">
                                            <s:property value="lead.stato"/>
                                        </span>
                                    </p>
                                </div>
                            </div>

                            <div class="row mb-3">
                                <div class="col-md-6">
                                    <label class="text-muted small">Email</label>
                                    <p><a href="mailto:<s:property value='lead.email'/>"><s:property value="lead.email"/></a></p>
                                </div>
                                <div class="col-md-6">
                                    <label class="text-muted small">Telefono</label>
                                    <p><s:property value="lead.telefono" default="-"/></p>
                                </div>
                            </div>
                        </div>
                    </div>

                    <div class="card mb-4">
                        <div class="card-header bg-success text-white">
                            <h5 class="mb-0"><i class="bi bi-cash-coin me-2"></i>Dati Commerciali</h5>
                        </div>
                        <div class="card-body">
                            <div class="row mb-3">
                                <div class="col-md-6">
                                    <label class="text-muted small">Budget Stimato</label>
                                    <p>
                                        <s:if test="lead.budgetStimato != null">
                                            € <s:property value="lead.budgetStimato"/>
                                        </s:if>
                                        <s:else>-</s:else>
                                    </p>
                                </div>
                                <div class="col-md-6">
                                    <label class="text-muted small">Probabilità Chiusura</label>
                                    <p>
                                        <div class="progress" style="height: 25px;">
                                            <div class="progress-bar bg-success" role="progressbar" 
                                                 style="width: <s:property value='lead.probabilitaChiusura'/>%">
                                                <s:property value="lead.probabilitaChiusura"/>%
                                            </div>
                                        </div>
                                    </p>
                                </div>
                            </div>

                            <div class="row mb-3">
                                <div class="col-md-6">
                                    <label class="text-muted small">Origine Contatto</label>
                                    <p><s:property value="lead.origine" default="-"/></p>
                                </div>
                                <div class="col-md-6">
                                    <label class="text-muted small">Data Contatto</label>
                                    <p>
                                        <s:if test="lead.dataContatto != null">
                                            <s:date name="lead.dataContatto" format="dd/MM/yyyy" />
                                        </s:if>
                                        <s:else>-</s:else>
                                    </p>
                                </div>
                            </div>

                            <div class="row mb-3">
                                <div class="col-md-12">
                                    <label class="text-muted small">Esigenza</label>
                                    <p><s:property value="lead.esigenza" default="-"/></p>
                                </div>
                            </div>
                        </div>
                    </div>

                    <div class="card mb-4">
                        <div class="card-header bg-info text-white">
                            <h5 class="mb-0"><i class="bi bi-geo-alt me-2"></i>Indirizzo</h5>
                        </div>
                        <div class="card-body">
                            <p>
                                <s:property value="lead.indirizzo" default="-"/><br/>
                                <s:property value="lead.citta" default="-"/> (<s:property value="lead.provincia" default="-"/>), <s:property value="lead.cap" default="-"/><br/>
                                <s:property value="lead.paese" default="Italia"/>
                            </p>
                        </div>
                    </div>
                </div>

                <div class="col-md-4">
                    <div class="card mb-4">
                        <div class="card-header bg-secondary text-white">
                            <h5 class="mb-0"><i class="bi bi-sticky me-2"></i>Note</h5>
                        </div>
                        <div class="card-body">
                            <p class="text-muted">
                                <s:if test="lead.note != null && !lead.note.trim().isEmpty()">
                                    <s:property value="lead.note" escapeHtml="false"/>
                                </s:if>
                                <s:else>
                                    <em>Nessuna nota</em>
                                </s:else>
                            </p>
                        </div>
                    </div>

                    <div class="card">
                        <div class="card-header bg-light">
                            <h5 class="mb-0"><i class="bi bi-clock-history me-2"></i>Informazioni Sistema</h5>
                        </div>
                        <div class="card-body small">
                            <div class="mb-2">
                                <label class="text-muted">Creato da:</label>
                                <p><s:property value="lead.createdBy.username" default="-"/></p>
                            </div>
                            <div class="mb-2">
                                <label class="text-muted">Data creazione:</label>
                                <p>
                                    <s:if test="lead.dataCreazione != null">
                                        <s:date name="lead.dataCreazione" format="dd/MM/yyyy HH:mm" />
                                    </s:if>
                                    <s:else>-</s:else>
                                </p>
                            </div>
                            <div class="mb-2">
                                <label class="text-muted">Ultima modifica:</label>
                                <p>
                                    <s:if test="lead.dataModifica != null">
                                        <s:date name="lead.dataModifica" format="dd/MM/yyyy HH:mm" />
                                    </s:if>
                                    <s:else>-</s:else>
                                </p>
                            </div>
                            <div>
                                <label class="text-muted">Prossimo Follow-up:</label>
                                <p>
                                    <s:if test="lead.dataProssimoFollowup != null">
                                        <s:date name="lead.dataProssimoFollowup" format="dd/MM/yyyy" />
                                    </s:if>
                                    <s:else>-</s:else>
                                </p>
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
</body>
</html>
