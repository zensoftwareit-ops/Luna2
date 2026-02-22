<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="s" uri="/struts-tags" %>
<!DOCTYPE html>
<html lang="it">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Commesse - Luna2</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.10.0/font/bootstrap-icons.css">
    <style>
        .stato-badge {
            font-size: 0.85rem;
            padding: 0.25rem 0.75rem;
        }
        .stato-APERTA { background-color: #e3f2fd; color: #1565c0; }
        .stato-IN_LAVORAZIONE { background-color: #fff3e0; color: #e65100; }
        .stato-SOSPESA { background-color: #ffebee; color: #c62828; }
        .stato-COMPLETATA { background-color: #e8f5e9; color: #2e7d32; }
        .stato-CHIUSA { background-color: #f3e5f5; color: #6a1b9a; }
        .stato-ANNULLATA { background-color: #f5f5f5; color: #616161; }
        
        .progress-bar-wrapper {
            position: relative;
            height: 30px;
        }
        .progress-bar-label {
            position: absolute;
            width: 100%;
            text-align: center;
            line-height: 30px;
            color: #000;
            font-weight: 600;
        }
        .commessa-scaduta {
            border-left: 4px solid #dc3545;
        }
    </style>
</head>
<body>
    <%@ include file="../includes/sidebar.jsp" %>

    <div class="col-md-10 content-wrapper p-4">
        <div class="container-fluid">
            <!-- Header -->
            <div class="d-flex justify-content-between align-items-center mb-4">
                <h1 class="h3">
                    <i class="bi bi-gear me-2"></i>Commesse
                </h1>
            </div>

            <!-- Messages -->
            <s:if test="message != null">
                <div class="alert alert-success alert-dismissible fade show" role="alert">
                    <i class="bi bi-check-circle me-2"></i><s:property value="message"/>
                    <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
                </div>
            </s:if>
            <s:if test="errorMessage != null">
                <div class="alert alert-danger alert-dismissible fade show" role="alert">
                    <i class="bi bi-exclamation-triangle me-2"></i><s:property value="errorMessage"/>
                    <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
                </div>
            </s:if>

            <!-- Stats Cards -->
            <div class="row mb-4">
                <div class="col-md-3 col-sm-6 mb-3">
                    <div class="card border-primary">
                        <div class="card-body">
                            <h6 class="text-muted mb-1">Aperte</h6>
                            <h3 class="mb-0"><s:property value="countAperte" default="0"/></h3>
                            <small class="text-muted">
                                <i class="bi bi-currency-euro"></i>
                                <s:property value="getText('{0,number,#,##0.00}', {valoreAperte})" default="0.00"/>
                            </small>
                        </div>
                    </div>
                </div>
                <div class="col-md-3 col-sm-6 mb-3">
                    <div class="card border-warning">
                        <div class="card-body">
                            <h6 class="text-muted mb-1">In Lavorazione</h6>
                            <h3 class="mb-0"><s:property value="countInLavorazione" default="0"/></h3>
                            <small class="text-muted">
                                <i class="bi bi-currency-euro"></i>
                                <s:property value="getText('{0,number,#,##0.00}', {valoreInLavorazione})" default="0.00"/>
                            </small>
                        </div>
                    </div>
                </div>
                <div class="col-md-3 col-sm-6 mb-3">
                    <div class="card border-danger">
                        <div class="card-body">
                            <h6 class="text-muted mb-1">Sospese</h6>
                            <h3 class="mb-0"><s:property value="countSospese" default="0"/></h3>
                        </div>
                    </div>
                </div>
                <div class="col-md-3 col-sm-6 mb-3">
                    <div class="card border-success">
                        <div class="card-body">
                            <h6 class="text-muted mb-1">Completate</h6>
                            <h3 class="mb-0"><s:property value="countCompletate" default="0"/></h3>
                        </div>
                    </div>
                </div>
            </div>

            <!-- Commesse Scadute Alert -->
            <s:if test="commesseScadute != null && !commesseScadute.isEmpty()">
                <div class="alert alert-danger" role="alert">
                    <h6 class="alert-heading"><i class="bi bi-exclamation-triangle me-2"></i>Commesse Scadute: <s:property value="commesseScadute.size()"/></h6>
                    <ul class="mb-0">
                        <s:iterator value="commesseScadute" var="scaduta">
                            <li>
                                <strong><s:property value="#scaduta.numero"/></strong> - 
                                <s:property value="#scaduta.descrizione"/> 
                                (Scadenza: <s:date name="#scaduta.dataPrevistFine" format="dd/MM/yyyy"/>)
                            </li>
                        </s:iterator>
                    </ul>
                </div>
            </s:if>

            <!-- Search and Filter -->
            <div class="card mb-4">
                <div class="card-body">
                    <form action="produzione-commesse" method="get" class="row g-3">
                        <div class="col-md-5">
                            <input type="text" name="searchTerm" class="form-control" 
                                   placeholder="Cerca per numero, descrizione, cliente..." 
                                   value="<s:property value='searchTerm'/>">
                        </div>
                        <div class="col-md-4">
                            <select name="stato" class="form-select">
                                <option value="">Tutti gli stati</option>
                                <option value="APERTA" <s:if test="stato == 'APERTA'">selected</s:if>>Aperta</option>
                                <option value="IN_LAVORAZIONE" <s:if test="stato == 'IN_LAVORAZIONE'">selected</s:if>>In Lavorazione</option>
                                <option value="SOSPESA" <s:if test="stato == 'SOSPESA'">selected</s:if>>Sospesa</option>
                                <option value="COMPLETATA" <s:if test="stato == 'COMPLETATA'">selected</s:if>>Completata</option>
                                <option value="CHIUSA" <s:if test="stato == 'CHIUSA'">selected</s:if>>Chiusa</option>
                                <option value="ANNULLATA" <s:if test="stato == 'ANNULLATA'">selected</s:if>>Annullata</option>
                            </select>
                        </div>
                        <div class="col-md-3">
                            <button type="submit" class="btn btn-primary w-100">
                                <i class="bi bi-search me-2"></i>Cerca
                            </button>
                        </div>
                    </form>
                </div>
            </div>

            <!-- Commesse Table -->
            <div class="card">
                <div class="card-header">
                    <h5 class="mb-0">Elenco Commesse</h5>
                </div>
                <div class="card-body">
                    <s:if test="commesse != null && !commesse.isEmpty()">
                        <div class="table-responsive">
                            <table class="table table-hover">
                                <thead>
                                    <tr>
                                        <th>Numero</th>
                                        <th>Cliente</th>
                                        <th>Descrizione</th>
                                        <th>Data Apertura</th>
                                        <th>Scadenza</th>
                                        <th>Stato</th>
                                        <th>Progresso</th>
                                        <th>Valore</th>
                                        <th>Azioni</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <s:iterator value="commesse" var="commessa">
                                        <tr class="<s:if test='#commessa.stato.name() == "IN_LAVORAZIONE" && #commessa.dataPrevistFine != null && #commessa.dataPrevistFine.before(new java.util.Date())'>commessa-scaduta</s:if>">
                                            <td>
                                                <strong><s:property value="#commessa.numero"/></strong>
                                            </td>
                                            <td>
                                                <s:if test="#commessa.preventivo != null && #commessa.preventivo.cliente != null">
                                                    <s:property value="#commessa.preventivo.cliente.ragioneSociale"/>
                                                </s:if>
                                                <s:else>N/A</s:else>
                                            </td>
                                            <td>
                                                <s:property value="#commessa.descrizione"/>
                                            </td>
                                            <td>
                                                <s:date name="#commessa.dataApertura" format="dd/MM/yyyy"/>
                                            </td>
                                            <td>
                                                <s:if test="#commessa.dataPrevistFine != null">
                                                    <s:date name="#commessa.dataPrevistFine" format="dd/MM/yyyy"/>
                                                </s:if>
                                                <s:else>-</s:else>
                                            </td>
                                            <td>
                                                <span class="badge stato-badge stato-<s:property value='#commessa.stato.name()'/>">
                                                    <s:property value="#commessa.stato.name().replace('_', ' ')"/>
                                                </span>
                                            </td>
                                            <td style="min-width: 120px;">
                                                <div class="progress-bar-wrapper">
                                                    <div class="progress" style="height: 30px;">
                                                        <div class="progress-bar bg-success" role="progressbar" 
                                                             style="width: <s:property value='#commessa.percentualeCompletamento != null ? #commessa.percentualeCompletamento : 0'/>%">
                                                        </div>
                                                    </div>
                                                    <div class="progress-bar-label">
                                                        <s:property value="#commessa.percentualeCompletamento != null ? #commessa.percentualeCompletamento : 0"/>%
                                                    </div>
                                                </div>
                                            </td>
                                            <td>
                                                <strong>&euro; <s:property value="getText('{0,number,#,##0.00}', {#commessa.totale})"/></strong>
                                            </td>
                                            <td>
                                                <div class="btn-group btn-group-sm">
                                                    <a href="produzione-view?id=<s:property value='#commessa.id'/>" 
                                                       class="btn btn-outline-primary" title="Visualizza">
                                                        <i class="bi bi-eye"></i>
                                                    </a>
                                                    
                                                    <!-- Pulsanti azione in base allo stato -->
                                                    <s:if test="#commessa.stato.name() == 'APERTA'">
                                                        <a href="produzione-avvia?id=<s:property value='#commessa.id'/>" 
                                                           class="btn btn-outline-success" title="Avvia Lavorazione">
                                                            <i class="bi bi-play-fill"></i>
                                                        </a>
                                                    </s:if>
                                                    
                                                    <s:if test="#commessa.stato.name() == 'IN_LAVORAZIONE'">
                                                        <a href="produzione-sospendi?id=<s:property value='#commessa.id'/>" 
                                                           class="btn btn-outline-warning" title="Sospendi">
                                                            <i class="bi bi-pause-fill"></i>
                                                        </a>
                                                        <a href="produzione-completa?id=<s:property value='#commessa.id'/>" 
                                                           class="btn btn-outline-success" title="Completa">
                                                            <i class="bi bi-check-circle"></i>
                                                        </a>
                                                    </s:if>
                                                    
                                                    <s:if test="#commessa.stato.name() == 'SOSPESA'">
                                                        <a href="produzione-riprendi?id=<s:property value='#commessa.id'/>" 
                                                           class="btn btn-outline-success" title="Riprendi">
                                                            <i class="bi bi-arrow-clockwise"></i>
                                                        </a>
                                                    </s:if>
                                                    
                                                    <s:if test="#commessa.stato.name() == 'COMPLETATA'">
                                                        <a href="produzione-fattura?id=<s:property value='#commessa.id'/>" 
                                                           class="btn btn-outline-primary" title="Crea Fattura">
                                                            <i class="bi bi-file-earmark-text"></i>
                                                        </a>
                                                    </s:if>
                                                </div>
                                            </td>
                                        </tr>
                                    </s:iterator>
                                </tbody>
                            </table>
                        </div>
                    </s:if>
                    <s:else>
                        <div class="alert alert-info mb-0">
                            <i class="bi bi-info-circle me-2"></i>
                            Nessuna commessa trovata. Le commesse vengono create dai preventivi accettati.
                        </div>
                    </s:else>
                </div>
            </div>
        </div>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
