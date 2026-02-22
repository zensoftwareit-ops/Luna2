<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="it">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>AI Intelligence - Luna2</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.10.0/font/bootstrap-icons.css">
    <style>
        .ai-card {
            border: none;
            border-radius: 12px;
            box-shadow: 0 2px 12px rgba(0,0,0,0.08);
            transition: transform 0.3s;
        }
        .ai-card:hover {
            transform: translateY(-3px);
            box-shadow: 0 4px 20px rgba(0,0,0,0.12);
        }
        .ai-header {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            padding: 2rem;
            border-radius: 12px;
            margin-bottom: 2rem;
        }
        .insight-badge {
            padding: 8px 16px;
            border-radius: 20px;
            font-weight: 600;
            display: inline-block;
        }
        .priority-critical { background: #dc3545; color: white; }
        .priority-high { background: #fd7e14; color: white; }
        .priority-medium { background: #ffc107; color: #000; }
        .priority-low { background: #6c757d; color: white; }
        .suggestion-item {
            padding: 15px;
            border-left: 4px solid #667eea;
            background: #f8f9fa;
            margin-bottom: 10px;
            border-radius: 6px;
        }
        .metric-card {
            text-align: center;
            padding: 20px;
        }
        .metric-value {
            font-size: 2.5rem;
            font-weight: 700;
            color: #667eea;
        }
        .trend-up { color: #28a745; }
        .trend-down { color: #dc3545; }
        .trend-stable { color: #6c757d; }
    </style>
</head>
<body>
    <%@ include file="../includes/sidebar.jsp" %>

            <div class="content-wrapper p-4">
                <div class="container-fluid">
                    <!-- AI Header -->
                    <div class="ai-header">
                        <div class="d-flex justify-content-between align-items-center">
                            <div>
                                <h1 class="h2 mb-2"><i class="bi bi-cpu me-2"></i>AI Intelligence Center</h1>
                                <p class="mb-0 opacity-75">Analisi predittive e suggerimenti intelligenti per il tuo business</p>
                            </div>
                            <div class="text-end">
                                <div class="badge bg-light text-dark fs-6">
                                    <i class="bi bi-lightning-charge-fill text-warning"></i> Sistema Attivo
                                </div>
                            </div>
                        </div>
                    </div>

                    <s:if test="hasActionErrors()">
                        <div class="alert alert-danger alert-dismissible fade show">
                            <s:actionerror/>
                            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
                        </div>
                    </s:if>

                    <!-- Key Metrics -->
                    <div class="row mb-4">
                        <div class="col-md-3 mb-3">
                            <div class="card ai-card metric-card">
                                <div class="metric-value">€ <s:property value="getText('{0,number,#,##0}', {previsioneFatturatoMeseSuccessivo})"/></div>
                                <div class="text-muted">Previsione Fatturato</div>
                                <small class="text-info"><i class="bi bi-graph-up-arrow"></i> Prossimo mese</small>
                            </div>
                        </div>

                        <div class="col-md-3 mb-3">
                            <div class="card ai-card metric-card">
                                <div class="metric-value <s:if test='tendenzaVendite == \"CRESCITA\"'>trend-up</s:if><s:elseif test='tendenzaVendite == \"DECRESCITA\"'>trend-down</s:elseif><s:else>trend-stable</s:else>">
                                    <s:if test='tendenzaVendite == "CRESCITA"'><i class="bi bi-arrow-up-circle-fill"></i></s:if>
                                    <s:elseif test='tendenzaVendite == "DECRESCITA"'><i class="bi bi-arrow-down-circle-fill"></i></s:elseif>
                                    <s:else><i class="bi bi-dash-circle-fill"></i></s:else>
                                </div>
                                <div class="text-muted">Tendenza Vendite</div>
                                <small><s:property value="tendenzaVendite"/></small>
                            </div>
                        </div>

                        <div class="col-md-3 mb-3">
                            <div class="card ai-card metric-card">
                                <div class="metric-value"><s:property value="leadDaContattare"/></div>
                                <div class="text-muted">Lead da Contattare</div>
                                <small class="text-warning"><i class="bi bi-telephone"></i> Azione richiesta</small>
                            </div>
                        </div>

                        <div class="col-md-3 mb-3">
                            <div class="card ai-card metric-card">
                                <div class="metric-value"><s:property value="probabilitaConversioneMedia"/>%</div>
                                <div class="text-muted">Conversione Media</div>
                                <small class="text-success"><i class="bi bi-trophy"></i> Lead qualificati</small>
                            </div>
                        </div>
                    </div>

                    <!-- AI Suggestions -->
                    <div class="row mb-4">
                        <div class="col-12">
                            <div class="card ai-card">
                                <div class="card-header bg-white border-0 pt-4">
                                    <h5 class="mb-0"><i class="bi bi-lightbulb text-warning me-2"></i>Suggerimenti Intelligenti</h5>
                                </div>
                                <div class="card-body">
                                    <s:if test="suggerimentiAi != null && suggerimentiAi.size() > 0">
                                        <s:iterator value="suggerimentiAi">
                                            <div class="suggestion-item">
                                                <s:property escapeHtml="false"/>
                                            </div>
                                        </s:iterator>
                                    </s:if>
                                    <s:else>
                                        <div class="text-center text-muted py-4">
                                            <i class="bi bi-robot fs-1 d-block mb-3"></i>
                                            <p>Nessun suggerimento al momento. Continua a raccogliere dati per insight migliori.</p>
                                        </div>
                                    </s:else>
                                </div>
                            </div>
                        </div>
                    </div>

                    <!-- Detailed Insights -->
                    <div class="row">
                        <!-- Prodotti da Riordinare -->
                        <div class="col-lg-4 mb-4">
                            <div class="card ai-card h-100">
                                <div class="card-header bg-white border-0 pt-4">
                                    <h5 class="mb-0"><i class="bi bi-box-seam text-danger me-2"></i>Priorità Magazzino</h5>
                                </div>
                                <div class="card-body p-0">
                                    <div class="list-group list-group-flush">
                                        <s:if test="prodottiDaRiordinare != null && prodottiDaRiordinare.size() > 0">
                                            <s:iterator value="prodottiDaRiordinare">
                                                <div class="list-group-item">
                                                    <div class="d-flex justify-content-between align-items-start mb-2">
                                                        <div class="flex-grow-1">
                                                            <h6 class="mb-1"><s:property value="prodotto"/></h6>
                                                            <small class="text-muted">
                                                                Disponibili: <s:property value="quantitaAttuale"/> | 
                                                                Minimo: <s:property value="scortaMinima"/>
                                                            </small>
                                                        </div>
                                                        <span class="insight-badge priority-<s:property value='priorita.toLowerCase()'/>">
                                                            <s:property value="priorita"/>
                                                        </span>
                                                    </div>
                                                    <div class="alert alert-light mb-0 py-2">
                                                        <i class="bi bi-cart-plus"></i> Ordina: <strong><s:property value="quantitaSuggerita"/> unità</strong>
                                                    </div>
                                                </div>
                                            </s:iterator>
                                        </s:if>
                                        <s:else>
                                            <div class="list-group-item text-center text-muted py-4">
                                                <i class="bi bi-check-circle fs-2 d-block mb-2 text-success"></i>
                                                <p class="mb-0">Tutti i prodotti hanno giacenza adeguata</p>
                                            </div>
                                        </s:else>
                                    </div>
                                </div>
                            </div>
                        </div>

                        <!-- Clienti a Rischio -->
                        <div class="col-lg-4 mb-4">
                            <div class="card ai-card h-100">
                                <div class="card-header bg-white border-0 pt-4">
                                    <h5 class="mb-0"><i class="bi bi-exclamation-triangle text-warning me-2"></i>Clienti da Riattivare</h5>
                                </div>
                                <div class="card-body p-0">
                                    <div class="list-group list-group-flush">
                                        <s:if test="clientiRischio != null && clientiRischio.size() > 0">
                                            <s:iterator value="clientiRischio">
                                                <div class="list-group-item">
                                                    <h6 class="mb-1"><s:property value="cliente"/></h6>
                                                    <small class="text-muted d-block mb-2">
                                                        <i class="bi bi-envelope"></i> <s:property value="email"/><br/>
                                                        <i class="bi bi-clock-history"></i> Ultimo ordine: 
                                                        <s:if test='ultimoOrdine instanceof java.util.Date'>
                                                            <s:date name="ultimoOrdine" format="dd/MM/yyyy"/>
                                                        </s:if>
                                                        <s:else>
                                                            <s:property value="ultimoOrdine"/>
                                                        </s:else>
                                                    </small>
                                                    <div class="d-grid">
                                                        <button class="btn btn-sm btn-outline-primary">
                                                            <i class="bi bi-telephone"></i> Contatta
                                                        </button>
                                                    </div>
                                                </div>
                                            </s:iterator>
                                        </s:if>
                                        <s:else>
                                            <div class="list-group-item text-center text-muted py-4">
                                                <i class="bi bi-people fs-2 d-block mb-2 text-success"></i>
                                                <p class="mb-0">Tutti i clienti sono attivi</p>
                                            </div>
                                        </s:else>
                                    </div>
                                </div>
                            </div>
                        </div>

                        <!-- Opportunità Vendita -->
                        <div class="col-lg-4 mb-4">
                            <div class="card ai-card h-100">
                                <div class="card-header bg-white border-0 pt-4">
                                    <h5 class="mb-0"><i class="bi bi-bullseye text-success me-2"></i>Opportunità</h5>
                                </div>
                                <div class="card-body p-0">
                                    <div class="list-group list-group-flush">
                                        <s:if test="opportunitaVendita != null && opportunitaVendita.size() > 0">
                                            <s:iterator value="opportunitaVendita">
                                                <div class="list-group-item">
                                                    <div class="d-flex justify-content-between align-items-start mb-2">
                                                        <div>
                                                            <h6 class="mb-1"><s:property value="cliente"/></h6>
                                                            <small class="text-success">
                                                                <i class="bi bi-currency-euro"></i> € <s:property value="getText('{0,number,#,##0}', {fatturatoRecente})"/>
                                                            </small>
                                                        </div>
                                                        <span class="badge bg-success"><s:property value="probabilita"/></span>
                                                    </div>
                                                    <p class="mb-2 small text-muted">
                                                        <i class="bi bi-info-circle"></i> <s:property value="opportunita"/>
                                                    </p>
                                                    <div class="d-grid">
                                                        <button class="btn btn-sm btn-success">
                                                            <i class="bi bi-send"></i> Proponi Offerta
                                                        </button>
                                                    </div>
                                                </div>
                                            </s:iterator>
                                        </s:if>
                                        <s:else>
                                            <div class="list-group-item text-center text-muted py-4">
                                                <i class="bi bi-search fs-2 d-block mb-2"></i>
                                                <p class="mb-0">Analizzando opportunità...</p>
                                            </div>
                                        </s:else>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>

                    <!-- Info Footer -->
                    <div class="row">
                        <div class="col-12">
                            <div class="alert alert-info">
                                <div class="d-flex align-items-center">
                                    <i class="bi bi-info-circle fs-4 me-3"></i>
                                    <div>
                                        <strong>Come funziona l'AI?</strong><br/>
                                        <small>Il sistema analizza i dati storici di vendite, magazzino, CRM e clienti per generare 
                                        previsioni e suggerimenti. Gli algoritmi utilizzano medie mobili, analisi delle tendenze e 
                                        pattern recognition per identificare opportunità e rischi.</small>
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
