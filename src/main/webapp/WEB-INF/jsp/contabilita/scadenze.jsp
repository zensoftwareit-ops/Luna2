<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="s" uri="/struts-tags" %>
<!DOCTYPE html>
<html lang="it">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Scadenze Fiscali - Luna2</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.10.0/font/bootstrap-icons.css">
</head>
<body>
    <%@ include file="../includes/sidebar.jsp" %>

    <div class="col-md-10 content-wrapper p-4">
        <div class="container-fluid">
            <div class="d-flex justify-content-between align-items-center mb-4">
                <h1 class="h3"><i class="bi bi-calendar-event me-2"></i>Scadenze Fiscali</h1>
                <a href="<s:url action='dashboard' namespace='/app/contabilita'/>" class="btn btn-outline-secondary">Dashboard</a>
            </div>

            <s:if test="hasActionMessages()">
                <div class="alert alert-success"><s:actionmessage/></div>
            </s:if>
            <s:if test="hasActionErrors()">
                <div class="alert alert-danger"><s:actionerror/></div>
            </s:if>

            <div class="row g-4">
                <div class="col-lg-4">
                    <div class="card border-0 shadow-sm">
                        <div class="card-header bg-white">
                            <h5 class="mb-0">Nuova scadenza</h5>
                        </div>
                        <div class="card-body">
                            <s:form action="scadenza-save" namespace="/app/contabilita" method="post" theme="simple">
                                <s:hidden name="scadenza.id"/>
                                <div class="mb-3">
                                    <label class="form-label">Titolo</label>
                                    <s:textfield name="scadenza.title" cssClass="form-control"/>
                                </div>
                                <div class="mb-3">
                                    <label class="form-label">Tipo</label>
                                    <s:select name="scadenza.type" list="deadlineTypes" cssClass="form-select"/>
                                </div>
                                <div class="mb-3">
                                    <label class="form-label">Periodicita</label>
                                    <s:select name="scadenza.frequency" list="deadlineFrequencies" cssClass="form-select"/>
                                </div>
                                <div class="mb-3">
                                    <label class="form-label">Data scadenza</label>
                                    <s:textfield name="scadenzaDate" cssClass="form-control" type="date"/>
                                </div>
                                <div class="mb-3">
                                    <label class="form-label">Importo previsto</label>
                                    <s:textfield name="scadenza.amountDue" cssClass="form-control"/>
                                </div>
                                <div class="mb-3">
                                    <label class="form-label">Note</label>
                                    <s:textarea name="scadenza.notes" cssClass="form-control" rows="3"/>
                                </div>
                                <button type="submit" class="btn btn-primary w-100">Salva scadenza</button>
                            </s:form>
                        </div>
                    </div>
                </div>
                <div class="col-lg-8">
                    <div class="card border-0 shadow-sm">
                        <div class="card-header bg-white">
                            <h5 class="mb-0">Calendario adempimenti</h5>
                        </div>
                        <div class="table-responsive">
                            <table class="table table-hover mb-0">
                                <thead class="table-light">
                                    <tr>
                                        <th>Titolo</th>
                                        <th>Tipo</th>
                                        <th>Data</th>
                                        <th>Importo</th>
                                        <th>Stato</th>
                                        <th></th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <s:iterator value="scadenze">
                                        <tr>
                                            <td><s:property value="title"/></td>
                                            <td><s:property value="type"/></td>
                                            <td><s:date name="deadlineDate" format="dd/MM/yyyy"/></td>
                                            <td>€ <s:property value="amountDue"/></td>
                                            <td><span class="badge bg-secondary"><s:property value="status"/></span></td>
                                            <td>
                                                <form action="<s:url action='scadenza-completa' namespace='/app/contabilita'/>" method="post" class="d-inline">
                                                    <input type="hidden" name="id" value="<s:property value='id'/>">
                                                    <button type="submit" class="btn btn-sm btn-outline-success">Completa</button>
                                                </form>
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

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>