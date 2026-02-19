<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="s" uri="/struts-tags" %>
<!DOCTYPE html>
<html lang="it">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Dettagli Fattura Passiva - Luna2</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.10.0/font/bootstrap-icons.css">
    <style>
        .label-field { font-weight: 600; color: #495057; }
        .data-field { font-size: 0.95rem; }
        .xml-preview { 
            background-color: #f8f9fa; 
            border: 1px solid #dee2e6; 
            border-radius: 0.375rem; 
            max-height: 400px; 
            overflow-y: auto;
            font-family: 'Courier New', monospace;
            font-size: 0.85rem;
            padding: 1rem;
        }
    </style>
</head>
<body>
    <div class="d-flex">
        <%@ include file="../../includes/sidebar.jsp" %>

        <div class="col-md-10 content-wrapper p-4">
            <div class="container-fluid">
                <div class="mb-4">
                    <a href="<s:url action='fatture-passive' namespace='/app/documenti'/>" class="btn btn-outline-secondary mb-2">
                        <i class="bi bi-arrow-left me-2"></i>Torna alla lista
                    </a>
                </div>

                <s:if test="fatturaPassiva != null">
                    <!-- Intestazione -->
                    <div class="card mb-4">
                        <div class="card-header bg-primary text-white">
                            <div class="row align-items-center">
                                <div class="col">
                                    <h2 class="mb-0">Fattura n. <code><s:property value="fatturaPassiva.numero"/></code></h2>
                                </div>
                                <div class="col-auto">
                                    <s:if test="fatturaPassiva.statoPagamento.equals(@it.zensoftware.luna2.model.FatturaPassiva$StatoPagamento@DA_PAGARE)">
                                        <span class="badge bg-warning text-dark" style="font-size: 0.9rem;">💰 Da Pagare</span>
                                    </s:if>
                                    <s:elseif test="fatturaPassiva.statoPagamento.equals(@it.zensoftware.luna2.model.FatturaPassiva$StatoPagamento@PARZIALMENTE_PAGATA)">
                                        <span class="badge bg-info" style="font-size: 0.9rem;">⚠️ Parz. Pagata</span>
                                    </s:elseif>
                                    <s:elseif test="fatturaPassiva.statoPagamento.equals(@it.zensoftware.luna2.model.FatturaPassiva$StatoPagamento@PAGATA)">
                                        <span class="badge bg-success" style="font-size: 0.9rem;">✓ Pagata</span>
                                    </s:elseif>
                                    <s:elseif test="fatturaPassiva.statoPagamento.equals(@it.zensoftware.luna2.model.FatturaPassiva$StatoPagamento@SCADUTA)">
                                        <span class="badge bg-danger" style="font-size: 0.9rem;">✗ Scaduta</span>
                                    </s:elseif>
                                </div>
                            </div>
                        </div>
                    </div>

                    <!-- Informazioni Fornitore -->
                    <div class="card mb-4">
                        <div class="card-header">
                            <h5 class="mb-0"><i class="bi bi-building me-2"></i>Fornitore</h5>
                        </div>
                        <div class="card-body">
                            <div class="row">
                                <div class="col-md-6">
                                    <div class="mb-3">
                                        <p class="label-field mb-1">Ragione Sociale</p>
                                        <p class="data-field">
                                            <s:if test="fatturaPassiva.fornitore != null">
                                                <s:property value="fatturaPassiva.fornitore.ragioneSociale"/>
                                            </s:if>
                                            <s:else>
                                                <s:property value="fatturaPassiva.fornitoreNome"/>
                                            </s:else>
                                        </p>
                                    </div>
                                </div>
                                <div class="col-md-6">
                                    <div class="mb-3">
                                        <p class="label-field mb-1">Partita IVA</p>
                                        <p class="data-field">
                                            <code>
                                                <s:if test="fatturaPassiva.fornitore != null">
                                                    <s:property value="fatturaPassiva.fornitore.partitaIva"/>
                                                </s:if>
                                                <s:else>
                                                    <s:property value="fatturaPassiva.fornitorePiva"/>
                                                </s:else>
                                            </code>
                                        </p>
                                    </div>
                                </div>
                            </div>
                            <s:if test="fatturaPassiva.fornitore != null">
                                <div class="row">
                                    <div class="col-md-6">
                                        <div class="mb-0">
                                            <p class="label-field mb-1">Indirizzo</p>
                                            <p class="data-field">
                                                <s:if test="fatturaPassiva.fornitore.indirizzo != null && !fatturaPassiva.fornitore.indirizzo.isEmpty()">
                                                    <s:property value="fatturaPassiva.fornitore.indirizzo"/>, 
                                                    <s:property value="fatturaPassiva.fornitore.cap"/> 
                                                    <s:property value="fatturaPassiva.fornitore.citta"/>
                                                </s:if>
                                                <s:else>
                                                    <span class="text-muted">Non disponibile</span>
                                                </s:else>
                                            </p>
                                        </div>
                                    </div>
                                </div>
                            </s:if>
                        </div>
                    </div>

                    <!-- Informazioni Documento -->
                    <div class="card mb-4">
                        <div class="card-header">
                            <h5 class="mb-0"><i class="bi bi-receipt me-2"></i>Dati Documento</h5>
                        </div>
                        <div class="card-body">
                            <div class="row">
                                <div class="col-md-4">
                                    <div class="mb-3">
                                        <p class="label-field mb-1">Data Fattura</p>
                                        <p class="data-field">
                                            <s:date name="fatturaPassiva.dataFattura" format="dd/MM/yyyy"/>
                                        </p>
                                    </div>
                                </div>
                                <div class="col-md-4">
                                    <div class="mb-3">
                                        <p class="label-field mb-1">Data Scadenza</p>
                                        <p class="data-field">
                                            <s:if test="fatturaPassiva.dataScadenza != null">
                                                <s:date name="fatturaPassiva.dataScadenza" format="dd/MM/yyyy"/>
                                                <s:if test="fatturaPassiva.dataScadenza.time < @java.lang.System@currentTimeMillis() && !fatturaPassiva.statoPagamento.equals(@it.zensoftware.luna2.model.FatturaPassiva$StatoPagamento@PAGATA)">
                                                    <br/><small class="badge bg-danger">SCADUTA</small>
                                                </s:if>
                                            </s:if>
                                            <s:else>
                                                <span class="text-muted">Non disponibile</span>
                                            </s:else>
                                        </p>
                                    </div>
                                </div>
                                <div class="col-md-4">
                                    <div class="mb-3">
                                        <p class="label-field mb-1">Data Pagamento</p>
                                        <p class="data-field">
                                            <s:if test="fatturaPassiva.dataPagamento != null">
                                                <s:date name="fatturaPassiva.dataPagamento" format="dd/MM/yyyy"/>
                                            </s:if>
                                            <s:else>
                                                <span class="text-muted">Non pagata</span>
                                            </s:else>
                                        </p>
                                    </div>
                                </div>
                            </div>
                            <div class="mb-0">
                                <p class="label-field mb-1">Oggetto</p>
                                <p class="data-field">
                                    <s:property value="fatturaPassiva.numero"/>
                                </p>
                            </div>
                        </div>
                    </div>

                    <!-- Importi -->
                    <div class="card mb-4">
                        <div class="card-header">
                            <h5 class="mb-0"><i class="bi bi-cash-coin me-2"></i>Importi</h5>
                        </div>
                        <div class="card-body">
                            <div class="row">
                                <div class="col-md-4">
                                    <div class="mb-2">
                                        <p class="label-field mb-1">Imponibile</p>
                                        <p class="data-field">
                                            <s:if test="fatturaPassiva.imponibile != null">
                                                € <s:text name="format.number"><s:param value="fatturaPassiva.imponibile"/></s:text>
                                            </s:if>
                                            <s:else>€ 0,00</s:else>
                                        </p>
                                    </div>
                                </div>
                                <div class="col-md-4">
                                    <div class="mb-2">
                                        <p class="label-field mb-1">IVA</p>
                                        <p class="data-field">
                                            <s:if test="fatturaPassiva.iva != null">
                                                € <s:text name="format.number"><s:param value="fatturaPassiva.iva"/></s:text>
                                            </s:if>
                                            <s:else>€ 0,00</s:else>
                                        </p>
                                    </div>
                                </div>
                                <div class="col-md-4">
                                    <div class="mb-2">
                                        <p class="label-field mb-1">Totale</p>
                                        <p data-field style="font-size: 1.2rem; color: #28a745; font-weight: bold;">
                                            <s:if test="fatturaPassiva.totale != null">
                                                € <s:text name="format.number"><s:param value="fatturaPassiva.totale"/></s:text>
                                            </s:if>
                                            <s:else>€ 0,00</s:else>
                                        </p>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>

                    <!-- Informazioni SDI -->
                    <div class="card mb-4">
                        <div class="card-header">
                            <h5 class="mb-0"><i class="bi bi-cloud-check me-2"></i>Tracciamento SDI</h5>
                        </div>
                        <div class="card-body">
                            <div class="row">
                                <div class="col-md-6">
                                    <div class="mb-3">
                                        <p class="label-field mb-1">ID Messaggio SDI</p>
                                        <p class="data-field">
                                            <code>
                                                <s:if test="fatturaPassiva.sdiIdMessaggio != null && !fatturaPassiva.sdiIdMessaggio.isEmpty()">
                                                    <s:property value="fatturaPassiva.sdiIdMessaggio"/>
                                                </s:if>
                                                <s:else>
                                                    <span class="text-muted">Non disponibile</span>
                                                </s:else>
                                            </code>
                                        </p>
                                    </div>
                                </div>
                                <div class="col-md-6">
                                    <div class="mb-3">
                                        <p class="label-field mb-1">Stato Ricezione</p>
                                        <p class="data-field">
                                            <s:property value="fatturaPassiva.statoRicezione"/>
                                        </p>
                                    </div>
                                </div>
                            </div>
                            <div class="mb-0">
                                <p class="label-field mb-1">Data Ricezione</p>
                                <p class="data-field">
                                    <s:date name="fatturaPassiva.dataRicezione" format="dd/MM/yyyy HH:mm:ss"/>
                                </p>
                            </div>
                        </div>
                    </div>

                    <!-- XML Risposta SDI (espandibile) -->
                    <div class="card mb-4">
                        <div class="card-header">
                            <button class="btn btn-link text-decoration-none w-100 text-start p-0" type="button" 
                                    data-bs-toggle="collapse" data-bs-target="#xmlResponse">
                                <i class="bi bi-chevron-down me-2"></i>
                                <i class="bi bi-code me-2"></i>XML Risposta SDI
                            </button>
                        </div>
                        <div id="xmlResponse" class="collapse">
                            <div class="card-body">
                                <div class="xml-preview">
                                    <pre class="mb-0"><s:property value="fatturaPassiva.xmlSdi" escapeHtml="true"/></pre>
                                </div>
                            </div>
                        </div>
                    </div>

                    <!-- Azioni -->
                    <div class="card">
                        <div class="card-header">
                            <h5 class="mb-0"><i class="bi bi-lightning-fill me-2"></i>Azioni</h5>
                        </div>
                        <div class="card-body">
                            <div class="btn-group" role="group">
                                <s:if test="!fatturaPassiva.statoPagamento.equals(@it.zensoftware.luna2.model.FatturaPassiva$StatoPagamento@PAGATA)">
                                    <a href="<s:url action='fatture-passive-registra-pagamento' namespace='/app/documenti'><s:param name='id' value='fatturaPassiva.id'/></s:url>" 
                                       class="btn btn-success"
                                       onclick="return confirm('Registrare il pagamento di questa fattura?')">
                                        <i class="bi bi-check-circle me-2"></i>Registra Pagamento
                                    </a>
                                </s:if>
                                <a href="<s:url action='fatture-passive-delete' namespace='/app/documenti'><s:param name='id' value='fatturaPassiva.id'/></s:url>" 
                                   class="btn btn-danger"
                                   onclick="return confirm('Eliminare questa fattura?')">
                                    <i class="bi bi-trash me-2"></i>Elimina
                                </a>
                                <a href="<s:url action='fatture-passive' namespace='/app/documenti'/>" class="btn btn-outline-secondary">
                                    <i class="bi bi-arrow-left me-2"></i>Torna alla lista
                                </a>
                            </div>
                        </div>
                    </div>

                </s:if>
                <s:else>
                    <div class="alert alert-warning" role="alert">
                        <i class="bi bi-exclamation-triangle me-2"></i>
                        Fattura passiva non trovata.
                    </div>
                </s:else>
            </div>
        </div>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
