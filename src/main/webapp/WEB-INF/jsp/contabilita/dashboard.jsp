<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="s" uri="/struts-tags" %>
<!DOCTYPE html>
<html lang="it">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Contabilita - Luna2</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.10.0/font/bootstrap-icons.css">
</head>
<body>
    <%@ include file="../includes/sidebar.jsp" %>

    <div class="col-md-10 content-wrapper p-4">
        <div class="container-fluid">
            <div class="d-flex justify-content-between align-items-center mb-4">
                <div>
                    <h1 class="h3 mb-1"><i class="bi bi-calculator me-2"></i>Contabilita Aziendale</h1>
                    <p class="text-muted mb-0">Dashboard operativa per regimi forfettario, ordinario, ditte individuali, societa semplici e SRL</p>
                </div>
                <div class="btn-group">
                    <a href="<s:url action='profilo' namespace='/app/contabilita'/>" class="btn btn-outline-primary">Profilo fiscale</a>
                    <a href="<s:url action='registrazione-nuova' namespace='/app/contabilita'/>" class="btn btn-primary">Nuova registrazione</a>
                </div>
            </div>

            <s:if test="hasActionMessages()">
                <div class="alert alert-success alert-dismissible fade show">
                    <s:actionmessage/>
                    <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
                </div>
            </s:if>

            <div class="row g-3 mb-4">
                <div class="col-md-3">
                    <div class="card border-0 shadow-sm h-100">
                        <div class="card-body">
                            <div class="text-muted small">Totale Dare</div>
                            <div class="display-6">€ <s:property value="totalDare"/></div>
                        </div>
                    </div>
                </div>
                <div class="col-md-3">
                    <div class="card border-0 shadow-sm h-100">
                        <div class="card-body">
                            <div class="text-muted small">Totale Avere</div>
                            <div class="display-6">€ <s:property value="totalAvere"/></div>
                        </div>
                    </div>
                </div>
                <div class="col-md-3">
                    <div class="card border-0 shadow-sm h-100">
                        <div class="card-body">
                            <div class="text-muted small">Registrazioni contabilizzate</div>
                            <div class="display-6"><s:property value="postedEntries"/></div>
                        </div>
                    </div>
                </div>
                <div class="col-md-3">
                    <div class="card border-0 shadow-sm h-100">
                        <div class="card-body">
                            <div class="text-muted small">Scadenze aperte / scadute</div>
                            <div class="display-6"><s:property value="openDeadlines"/> / <span class="text-danger"><s:property value="overdueDeadlines"/></span></div>
                        </div>
                    </div>
                </div>
            </div>

            <div class="row g-4">
                <div class="col-lg-5">
                    <div class="card border-0 shadow-sm h-100">
                        <div class="card-header bg-white">
                            <h5 class="mb-0">Profilo contabile</h5>
                        </div>
                        <div class="card-body">
                            <s:if test="profilo != null">
                                <dl class="row mb-0">
                                    <dt class="col-sm-5">Ragione sociale</dt>
                                    <dd class="col-sm-7"><s:property value="profilo.companyName"/></dd>
                                    <dt class="col-sm-5">Forma giuridica</dt>
                                    <dd class="col-sm-7"><s:property value="profilo.businessForm"/></dd>
                                    <dt class="col-sm-5">Regime fiscale</dt>
                                    <dd class="col-sm-7"><s:property value="profilo.taxRegime"/></dd>
                                    <dt class="col-sm-5">Frequenza IVA</dt>
                                    <dd class="col-sm-7"><s:property value="profilo.vatFrequency"/></dd>
                                    <dt class="col-sm-5">P.IVA</dt>
                                    <dd class="col-sm-7"><s:property value="profilo.vatNumber"/></dd>
                                </dl>
                            </s:if>
                            <s:else>
                                <p class="text-muted mb-0">Profilo non ancora configurato.</p>
                            </s:else>
                        </div>
                    </div>
                </div>
                <div class="col-lg-7">
                    <div class="card border-0 shadow-sm h-100">
                        <div class="card-header bg-white d-flex justify-content-between align-items-center">
                            <h5 class="mb-0">Scadenze imminenti</h5>
                            <a href="<s:url action='scadenze' namespace='/app/contabilita'/>" class="btn btn-sm btn-outline-secondary">Gestisci</a>
                        </div>
                        <div class="card-body p-0">
                            <div class="table-responsive">
                                <table class="table table-hover mb-0">
                                    <thead class="table-light">
                                        <tr>
                                            <th>Titolo</th>
                                            <th>Tipo</th>
                                            <th>Data</th>
                                            <th>Stato</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        <s:iterator value="scadenze">
                                            <tr>
                                                <td><s:property value="title"/></td>
                                                <td><s:property value="type"/></td>
                                                <td><s:date name="deadlineDate" format="dd/MM/yyyy"/></td>
                                                <td><span class="badge bg-secondary"><s:property value="status"/></span></td>
                                            </tr>
                                        </s:iterator>
                                    </tbody>
                                </table>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <div class="card border-0 shadow-sm mt-4">
                <div class="card-header bg-white d-flex justify-content-between align-items-center">
                    <h5 class="mb-0">Ultime registrazioni</h5>
                    <a href="<s:url action='registrazioni' namespace='/app/contabilita'/>" class="btn btn-sm btn-outline-secondary">Vai alla prima nota</a>
                </div>
                <div class="card-body p-0">
                    <div class="table-responsive">
                        <table class="table table-striped mb-0">
                            <thead class="table-light">
                                <tr>
                                    <th>Protocollo</th>
                                    <th>Data</th>
                                    <th>Descrizione</th>
                                    <th>Tipo</th>
                                    <th>Stato</th>
                                    <th class="text-end">Importo</th>
                                </tr>
                            </thead>
                            <tbody>
                                <s:iterator value="registrazioni">
                                    <tr>
                                        <td><s:property value="protocolNumber"/></td>
                                        <td><s:date name="entryDate" format="dd/MM/yyyy"/></td>
                                        <td><s:property value="description"/></td>
                                        <td><s:property value="type"/></td>
                                        <td><span class="badge bg-info text-dark"><s:property value="status"/></span></td>
                                        <td class="text-end">€ <s:property value="totalDebit"/></td>
                                    </tr>
                                </s:iterator>
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>