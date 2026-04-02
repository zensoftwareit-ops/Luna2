<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="s" uri="/struts-tags" %>
<!DOCTYPE html>
<html lang="it">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Piano dei Conti - Luna2</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.10.0/font/bootstrap-icons.css">
</head>
<body>
    <%@ include file="../includes/sidebar.jsp" %>

    <div class="col-md-10 content-wrapper p-4">
        <div class="container-fluid">
            <div class="d-flex justify-content-between align-items-center mb-4">
                <h1 class="h3"><i class="bi bi-diagram-3 me-2"></i>Piano dei Conti</h1>
                <a href="<s:url action='dashboard' namespace='/app/contabilita'/>" class="btn btn-outline-secondary">Dashboard</a>
            </div>

            <s:if test="hasActionErrors()">
                <div class="alert alert-danger"><s:actionerror/></div>
            </s:if>

            <div class="row g-4">
                <div class="col-lg-4">
                    <div class="card border-0 shadow-sm">
                        <div class="card-header bg-white">
                            <h5 class="mb-0">Nuovo conto</h5>
                        </div>
                        <div class="card-body">
                            <s:form action="conto-save" namespace="/app/contabilita" method="post" theme="simple">
                                <s:hidden name="conto.id"/>
                                <div class="mb-3">
                                    <label class="form-label">Codice</label>
                                    <s:textfield name="conto.code" cssClass="form-control"/>
                                </div>
                                <div class="mb-3">
                                    <label class="form-label">Nome</label>
                                    <s:textfield name="conto.name" cssClass="form-control"/>
                                </div>
                                <div class="mb-3">
                                    <label class="form-label">Categoria</label>
                                    <s:select name="conto.category" list="accountCategories" cssClass="form-select"/>
                                </div>
                                <div class="mb-3">
                                    <label class="form-label">Descrizione</label>
                                    <s:textarea name="conto.description" cssClass="form-control" rows="3"/>
                                </div>
                                <div class="form-check mb-2">
                                    <s:checkbox name="conto.enabled" cssClass="form-check-input"/>
                                    <label class="form-check-label">Attivo</label>
                                </div>
                                <div class="form-check mb-3">
                                    <s:checkbox name="conto.systemAccount" cssClass="form-check-input"/>
                                    <label class="form-check-label">Conto di sistema</label>
                                </div>
                                <button type="submit" class="btn btn-primary w-100">Salva conto</button>
                            </s:form>
                        </div>
                    </div>
                </div>
                <div class="col-lg-8">
                    <div class="card border-0 shadow-sm">
                        <div class="card-header bg-white">
                            <h5 class="mb-0">Conti configurati</h5>
                        </div>
                        <div class="table-responsive">
                            <table class="table table-striped mb-0">
                                <thead class="table-light">
                                    <tr>
                                        <th>Codice</th>
                                        <th>Nome</th>
                                        <th>Categoria</th>
                                        <th>Sistema</th>
                                        <th>Stato</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <s:iterator value="conti">
                                        <tr>
                                            <td><s:property value="code"/></td>
                                            <td><s:property value="name"/></td>
                                            <td><s:property value="category"/></td>
                                            <td><s:if test="systemAccount">Si</s:if><s:else>No</s:else></td>
                                            <td><span class="badge bg-secondary"><s:if test="enabled">Attivo</s:if><s:else>Disattivo</s:else></span></td>
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

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>