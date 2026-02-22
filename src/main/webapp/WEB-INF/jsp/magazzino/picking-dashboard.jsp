<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="/struts-tags" prefix="s" %>
<!DOCTYPE html>
<html>
<head>
    <title>Dashboard Picking - Magazzino</title>
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link rel="stylesheet" href="<s:url value='/assets/css/bootstrap.min.css'/>">
    <style>
        .card-in-attesa { border-left: 4px solid #17a2b8; }
        .card-in-corso { border-left: 4px solid #ffc107; }
        .card-completati { border-left: 4px solid #28a745; }
        .picking-card { cursor: pointer; transition: all 0.3s; }
        .picking-card:hover { transform: translateY(-5px); box-shadow: 0 4px 8px rgba(0,0,0,0.2); }
        .progress-picking { height: 25px; font-size: 14px; }
        .badge-priority { font-size: 0.9rem; }
    </style>
</head>
<body>
    <jsp:include page="/WEB-INF/jsp/includes/navbar.jsp"/>
    
    <div class="container-fluid mt-4">
        <div class="row mb-3">
            <div class="col-md-6">
                <h2><i class="fas fa-clipboard-list"></i> Dashboard Picking</h2>
            </div>
            <div class="col-md-6 text-right">
                <s:form action="picking-dashboard" method="get" cssClass="form-inline float-right">
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
            <div class="col-md-4">
                <div class="card card-in-attesa">
                    <div class="card-body">
                        <h6 class="card-subtitle mb-2 text-muted">In Attesa</h6>
                        <h3 class="card-title"><s:property value="countInAttesa"/></h3>
                    </div>
                </div>
            </div>
            <div class="col-md-4">
                <div class="card card-in-corso">
                    <div class="card-body">
                        <h6 class="card-subtitle mb-2 text-muted">In Corso</h6>
                        <h3 class="card-title"><s:property value="countInCorso"/></h3>
                    </div>
                </div>
            </div>
            <div class="col-md-4">
                <div class="card card-completati">
                    <div class="card-body">
                        <h6 class="card-subtitle mb-2 text-muted">Completati (oggi)</h6>
                        <h3 class="card-title"><s:property value="countCompletati"/></h3>
                    </div>
                </div>
            </div>
        </div>
        
        <!-- Azioni Rapide -->
        <div class="row mb-3">
            <div class="col-md-12">
                <div class="btn-group" role="group">
                    <s:a action="picking-formNew" cssClass="btn btn-primary">
                        <i class="fas fa-plus-circle"></i> Nuova Picking List
                    </s:a>
                    <s:a action="picking-list" cssClass="btn btn-outline-secondary">
                        <i class="fas fa-list"></i> Tutte le Picking
                    </s:a>
                    <s:a action="picking-myPicking" cssClass="btn btn-outline-info">
                        <i class="fas fa-user"></i> Le mie Picking
                    </s:a>
                </div>
            </div>
        </div>
        
        <!-- Picking Lists in Corso -->
        <h4 class="mb-3">Picking in Corso</h4>
        
        <s:if test="pickingLists == null || pickingLists.size() == 0">
            <div class="alert alert-info">
                <i class="fas fa-info-circle"></i> Nessuna picking list in corso
            </div>
        </s:if>
        <s:else>
            <div class="row">
                <s:iterator value="pickingLists" var="pl">
                    <div class="col-md-6 col-lg-4 mb-3">
                        <div class="card picking-card" onclick="location.href='<s:url action="picking-view"><s:param name="pickingListId" value="#pl.id"/></s:url>'">
                            <div class="card-header">
                                <div class="d-flex justify-content-between align-items-center">
                                    <strong><s:property value="#pl.numero"/></strong>
                                    <s:if test="#pl.priorita > 5">
                                        <span class="badge badge-danger badge-priority">Alta</span>
                                    </s:if>
                                    <s:elseif test="#pl.priorita > 3">
                                        <span class="badge badge-warning badge-priority">Media</span>
                                    </s:elseif>
                                    <s:else>
                                        <span class="badge badge-secondary badge-priority">Normale</span>
                                    </s:else>
                                </div>
                            </div>
                            <div class="card-body">
                                <p class="card-text">
                                    <strong>Ordine:</strong> <s:property value="#pl.ordine.numero"/><br/>
                                    <strong>Assegnato a:</strong> 
                                    <s:if test="#pl.assegnatoA != null">
                                        <s:property value="#pl.assegnatoA.username"/>
                                    </s:if>
                                    <s:else>
                                        <span class="text-muted">Non assegnato</span>
                                    </s:else>
                                    <br/>
                                    <strong>Stato:</strong> 
                                    <s:if test="#pl.stato.name() == 'IN_PROGRESS'">
                                        <span class="badge badge-warning">In Corso</span>
                                    </s:if>
                                    <s:elseif test="#pl.stato.name() == 'ASSEGNATO'">
                                        <span class="badge badge-info">Assegnato</span>
                                    </s:elseif>
                                    <s:elseif test="#pl.stato.name() == 'DRAFT'">
                                        <span class="badge badge-secondary">Bozza</span>
                                    </s:elseif>
                                </p>
                                
                                <div class="progress progress-picking">
                                    <div class="progress-bar" role="progressbar" 
                                         style="width: <s:property value='#pl.percentualeCompletamento'/>%"
                                         aria-valuenow="<s:property value='#pl.percentualeCompletamento'/>" 
                                         aria-valuemin="0" aria-valuemax="100">
                                        <s:property value="#pl.percentualeCompletamento"/>%
                                    </div>
                                </div>
                                
                                <div class="mt-2">
                                    <small class="text-muted">
                                        <i class="fas fa-clock"></i> 
                                        Creato: <s:date name="#pl.dataCreazione" format="dd/MM/yyyy HH:mm"/>
                                    </small>
                                </div>
                            </div>
                            <div class="card-footer">
                                <div class="btn-group btn-group-sm w-100">
                                    <s:a action="picking-view" cssClass="btn btn-primary">
                                        <s:param name="pickingListId" value="#pl.id"/>
                                        <i class="fas fa-eye"></i> Visualizza
                                    </s:a>
                                    <s:if test="#pl.stato.name() == 'ASSEGNATO'">
                                        <s:a action="picking-avvia" cssClass="btn btn-success">
                                            <s:param name="pickingListId" value="#pl.id"/>
                                            <i class="fas fa-play"></i> Avvia
                                        </s:a>
                                    </s:if>
                                    <s:if test="#pl.stato.name() == 'IN_PROGRESS'">
                                        <s:a action="picking-completa" cssClass="btn btn-success">
                                            <s:param name="pickingListId" value="#pl.id"/>
                                            <i class="fas fa-check"></i> Completa
                                        </s:a>
                                    </s:if>
                                </div>
                            </div>
                        </div>
                    </div>
                </s:iterator>
            </div>
        </s:else>
    </div>
    
    <script src="<s:url value='/assets/js/jquery.min.js'/>"></script>
    <script src="<s:url value='/assets/js/bootstrap.bundle.min.js'/>"></script>
    <script>
        // Auto refresh ogni 30 secondi
        setTimeout(function(){ location.reload(); }, 30000);
    </script>
</body>
</html>
