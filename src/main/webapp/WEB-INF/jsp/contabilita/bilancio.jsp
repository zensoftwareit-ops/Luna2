<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="s" uri="/struts-tags" %>
<!DOCTYPE html>
<html lang="it">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Bilancio - Luna2</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.10.0/font/bootstrap-icons.css">
</head>
<body>
<%@ include file="../includes/sidebar.jsp" %>

<div class="col-md-10 content-wrapper p-4">
    <div class="container-fluid">
        <div class="d-flex justify-content-between align-items-center mb-4">
            <h1 class="h3"><i class="bi bi-bar-chart-line me-2"></i>Bilancio di Verifica</h1>
            <div class="d-flex gap-2">
                <form action="<s:url action='ratei-risconti-auto' namespace='/app/contabilita'/>" method="post" class="d-flex gap-2">
                    <input type="number" class="form-control" name="annoEsercizio" style="width: 120px" placeholder="Anno">
                    <button type="submit" class="btn btn-outline-primary">Ratei/Risconti auto</button>
                </form>
                <form action="<s:url action='chiusura-auto' namespace='/app/contabilita'/>" method="post" class="d-flex gap-2">
                    <input type="number" class="form-control" name="annoEsercizio" style="width: 120px" placeholder="Anno">
                    <button type="submit" class="btn btn-primary">Chiusura automatica</button>
                </form>
                <form action="<s:url action='apertura-auto' namespace='/app/contabilita'/>" method="post" class="d-flex gap-2">
                    <input type="number" class="form-control" name="annoEsercizio" style="width: 120px" placeholder="Anno chiusura">
                    <button type="submit" class="btn btn-outline-success">Apertura anno successivo</button>
                </form>
                <a href="<s:url action='dashboard' namespace='/app/contabilita'/>" class="btn btn-outline-secondary">Dashboard</a>
            </div>
        </div>

        <s:if test="hasActionMessages()"><div class="alert alert-success"><s:actionmessage/></div></s:if>
        <s:if test="hasActionErrors()"><div class="alert alert-danger"><s:actionerror/></div></s:if>

        <div class="row g-3 mb-4">
            <div class="col-md-4"><div class="card"><div class="card-body"><small class="text-muted">Totale Dare</small><div class="h4">€ <s:property value="bilancioTotaleDare"/></div></div></div></div>
            <div class="col-md-4"><div class="card"><div class="card-body"><small class="text-muted">Totale Avere</small><div class="h4">€ <s:property value="bilancioTotaleAvere"/></div></div></div></div>
            <div class="col-md-4"><div class="card"><div class="card-body"><small class="text-muted">Delta</small><div class="h4">€ <s:property value="bilancioDelta"/></div></div></div></div>
        </div>

        <div class="card border-0 shadow-sm">
            <div class="table-responsive">
                <table class="table table-striped mb-0">
                    <thead class="table-light">
                    <tr>
                        <th>Codice</th>
                        <th>Conto</th>
                        <th class="text-end">Totale Dare</th>
                        <th class="text-end">Totale Avere</th>
                        <th class="text-end">Saldo</th>
                    </tr>
                    </thead>
                    <tbody>
                    <s:iterator value="bilancioRows">
                        <tr>
                            <td><s:property value="accountCode"/></td>
                            <td><s:property value="accountName"/></td>
                            <td class="text-end">€ <s:property value="totalDebit"/></td>
                            <td class="text-end">€ <s:property value="totalCredit"/></td>
                            <td class="text-end">€ <s:property value="balance"/></td>
                        </tr>
                    </s:iterator>
                    </tbody>
                </table>
            </div>
        </div>
    </div>
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>