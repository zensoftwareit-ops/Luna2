<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="/struts-tags" prefix="s" %>
<!DOCTYPE html>
<html>
<head>
    <title>Dashboard Giacenze - Magazzino</title>
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link rel="stylesheet" href="<s:url value='/assets/css/bootstrap.min.css'/>">
    <style>
        .card-giacenze { border-left: 4px solid #007bff; }
        .card-sotto-scorta { border-left: 4px solid #dc3545; }
        .card-da-riordinare { border-left: 4px solid #ffc107; }
        .card-valore { border-left: 4px solid #28a745; }
        .table-giacenze { font-size: 0.9rem; }
        .badge-alert { animation: pulse 2s infinite; }
        @keyframes pulse {
            0%, 100% { opacity: 1; }
            50% { opacity: 0.5; }
        }
    </style>
</head>
<body>
    <jsp:include page="/WEB-INF/jsp/includes/navbar.jsp"/>
    
    <div class="container-fluid mt-4">
        <div class="row mb-3">
            <div class="col-md-6">
                <h2><i class="fas fa-warehouse"></i> Dashboard Giacenze</h2>
            </div>
            <div class="col-md-6 text-right">
                <s:form action="warehouse-dashboardGiacenze" method="get" cssClass="form-inline float-right">
                    <label for="warehouseId" class="mr-2">Magazzino:</label>
                    <s:select name="warehouseId" list="warehouses" listKey="id" listValue="nome" 
                              cssClass="form-control mr-2" onchange="this.form.submit()"/>
                </s:form>
            </div>
        </div>
        
        <s:if test="hasActionMessages()">
            <div class="alert alert-success"><s:actionmessage/></div>
        </s:if>
        <s:if test="hasActionErrors()">
            <div class="alert alert-danger"><s:actionerror/></div>
        </s:if>
        
        <!-- KPI Cards -->
        <div class="row mb-4">
            <div class="col-md-3">
                <div class="card card-giacenze">
                    <div class="card-body">
                        <h6 class="card-subtitle mb-2 text-muted">Totale Articoli</h6>
                        <h3 class="card-title"><s:property value="giacenze.size()"/></h3>
                    </div>
                </div>
            </div>
            <div class="col-md-3">
                <div class="card card-sotto-scorta">
                    <div class="card-body">
                        <h6 class="card-subtitle mb-2 text-muted">Sotto Scorta</h6>
                        <h3 class="card-title">
                            <s:property value="sottoScorta.size()"/>
                            <s:if test="sottoScorta.size() > 0">
                                <span class="badge badge-danger badge-alert ml-2">!</span>
                            </s:if>
                        </h3>
                    </div>
                </div>
            </div>
            <div class="col-md-3">
                <div class="card card-da-riordinare">
                    <div class="card-body">
                        <h6 class="card-subtitle mb-2 text-muted">Da Riordinare</h6>
                        <h3 class="card-title">
                            <s:property value="daRiordinare.size()"/>
                            <s:if test="daRiordinare.size() > 0">
                                <span class="badge badge-warning ml-2">!</span>
                            </s:if>
                        </h3>
                    </div>
                </div>
            </div>
            <div class="col-md-3">
                <div class="card card-valore">
                    <div class="card-body">
                        <h6 class="card-subtitle mb-2 text-muted">Valore Totale</h6>
                        <h3 class="card-title">€ <s:text name="format.decimal">
                            <s:param value="valoreTotale"/>
                        </s:text></h3>
                    </div>
                </div>
            </div>
        </div>
        
        <!-- Azioni Rapide -->
        <div class="row mb-3">
            <div class="col-md-12">
                <div class="btn-group" role="group">
                    <s:a action="warehouse-formCarico" cssClass="btn btn-success">
                        <i class="fas fa-plus-circle"></i> Carico Merce
                    </s:a>
                    <s:a action="warehouse-formScarico" cssClass="btn btn-warning">
                        <i class="fas fa-minus-circle"></i> Scarico Merce
                    </s:a>
                    <s:a action="warehouse-formTrasferimento" cssClass="btn btn-info">
                        <i class="fas fa-exchange-alt"></i> Trasferimento
                    </s:a>
                    <s:a action="warehouse-report" cssClass="btn btn-primary">
                        <i class="fas fa-file-pdf"></i> Report
                    </s:a>
                </div>
            </div>
        </div>
        
        <!-- Alert Sotto Scorta -->
        <s:if test="sottoScorta != null && sottoScorta.size() > 0">
            <div class="alert alert-danger">
                <h5><i class="fas fa-exclamation-triangle"></i> Prodotti Sotto Scorta</h5>
                <ul class="mb-0">
                    <s:iterator value="sottoScorta" var="g">
                        <li>
                            <strong><s:property value="#g.prodotto.codice"/> - <s:property value="#g.prodotto.descrizione"/></strong>: 
                            Giacenza: <s:property value="#g.quantitaAttuale"/> | 
                            Minima: <s:property value="#g.quantitaMinima"/>
                        </li>
                    </s:iterator>
                </ul>
            </div>
        </s:if>
        
        <!-- Tabella Giacenze -->
        <div class="card">
            <div class="card-header">
                <h5>Giacenze - <s:property value="warehouse.nome"/></h5>
            </div>
            <div class="card-body">
                <div class="table-responsive">
                    <table class="table table-hover table-giacenze">
                        <thead>
                            <tr>
                                <th>Codice</th>
                                <th>Descrizione</th>
                                <th>Posizione</th>
                                <th class="text-right">Disponibile</th>
                                <th class="text-right">Impegnata</th>
                                <th class="text-right">In Ordine</th>
                                <th class="text-right">Costo Medio</th>
                                <th class="text-right">Valore</th>
                                <th>Stato</th>
                                <th>Azioni</th>
                            </tr>
                        </thead>
                        <tbody>
                            <s:iterator value="giacenze" var="g">
                                <tr <s:if test="#g.isSottoScorta()">class="table-danger"</s:if>
                                    <s:elseif test="#g.isDaRiordinare()">class="table-warning"</s:elseif>>
                                    <td><s:property value="#g.prodotto.codice"/></td>
                                    <td><s:property value="#g.prodotto.descrizione"/></td>
                                    <td>
                                        <s:if test="#g.posizionePrincipale != null">
                                            <s:property value="#g.posizionePrincipale.posizioneCompleta"/>
                                        </s:if>
                                    </td>
                                    <td class="text-right"><s:property value="#g.quantitaDisponibile"/></td>
                                    <td class="text-right"><s:property value="#g.quantitaImpegnata"/></td>
                                    <td class="text-right"><s:property value="#g.quantitaInOrdine"/></td>
                                    <td class="text-right">€ <s:property value="#g.costoMedioPonderato"/></td>
                                    <td class="text-right">€ <s:property value="#g.valoreGiacenza"/></td>
                                    <td>
                                        <s:if test="#g.isSottoScorta()">
                                            <span class="badge badge-danger">Sotto Scorta</span>
                                        </s:if>
                                        <s:elseif test="#g.isDaRiordinare()">
                                            <span class="badge badge-warning">Da Riordinare</span>
                                        </s:elseif>
                                        <s:else>
                                            <span class="badge badge-success">OK</span>
                                        </s:else>
                                    </td>
                                    <td>
                                        <div class="btn-group btn-group-sm">
                                            <s:a action="warehouse-viewGiacenza" cssClass="btn btn-sm btn-info">
                                                <s:param name="giacenzaId" value="#g.id"/>
                                                <i class="fas fa-info-circle"></i>
                                            </s:a>
                                            <s:a action="warehouse-formCarico" cssClass="btn btn-sm btn-success">
                                                <s:param name="prodottoId" value="#g.prodotto.id"/>
                                                <s:param name="warehouseId" value="#g.warehouse.id"/>
                                                <i class="fas fa-plus"></i>
                                            </s:a>
                                            <s:a action="warehouse-formScarico" cssClass="btn btn-sm btn-warning">
                                                <s:param name="prodottoId" value="#g.prodotto.id"/>
                                                <s:param name="warehouseId" value="#g.warehouse.id"/>
                                                <i class="fas fa-minus"></i>
                                            </s:a>
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
    
    <script src="<s:url value='/assets/js/jquery.min.js'/>"></script>
    <script src="<s:url value='/assets/js/bootstrap.bundle.min.js'/>"></script>
</body>
</html>
