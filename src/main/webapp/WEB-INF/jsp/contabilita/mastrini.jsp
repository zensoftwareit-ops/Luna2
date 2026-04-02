<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="s" uri="/struts-tags" %>
<!DOCTYPE html>
<html lang="it">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Mastrini - Luna2</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.10.0/font/bootstrap-icons.css">
</head>
<body>
<%@ include file="../includes/sidebar.jsp" %>

<div class="col-md-10 content-wrapper p-4">
    <div class="container-fluid">
        <div class="d-flex justify-content-between align-items-center mb-4">
            <h1 class="h3"><i class="bi bi-journal-bookmark me-2"></i>Mastrini</h1>
            <a href="<s:url action='dashboard' namespace='/app/contabilita'/>" class="btn btn-outline-secondary">Dashboard</a>
        </div>

        <div class="card border-0 shadow-sm mb-4">
            <div class="card-body">
                <form action="<s:url action='mastrini' namespace='/app/contabilita'/>" method="get" class="row g-3 align-items-end">
                    <div class="col-md-6">
                        <label class="form-label">Conto</label>
                        <select name="accountId" class="form-select">
                            <s:iterator value="conti">
                                <option value="<s:property value='id'/>" <s:if test="id == accountId">selected</s:if>><s:property value="code"/> - <s:property value="name"/></option>
                            </s:iterator>
                        </select>
                    </div>
                    <div class="col-md-3">
                        <button type="submit" class="btn btn-primary">Mostra mastrino</button>
                    </div>
                    <div class="col-md-3 text-end">
                        <span class="badge bg-dark">Saldo: € <s:property value="mastrinoSaldo"/></span>
                    </div>
                </form>
                <div class="mt-3 d-flex gap-2">
                    <a class="btn btn-outline-primary" href="<s:url action='mastrino-pdf' namespace='/app/contabilita'><s:param name='accountId' value='accountId'/></s:url>">Stampa PDF</a>
                    <a class="btn btn-outline-success" href="<s:url action='mastrino-excel' namespace='/app/contabilita'><s:param name='accountId' value='accountId'/></s:url>">Export Excel</a>
                </div>
            </div>
        </div>

        <div class="card border-0 shadow-sm">
            <div class="table-responsive">
                <table class="table table-hover mb-0">
                    <thead class="table-light">
                    <tr>
                        <th>Data</th>
                        <th>Protocollo</th>
                        <th>Descrizione</th>
                        <th>Tipo Riga</th>
                        <th class="text-end">Importo</th>
                    </tr>
                    </thead>
                    <tbody>
                    <s:iterator value="mastrinoLines">
                        <tr>
                            <td><s:date name="entry.entryDate" format="dd/MM/yyyy"/></td>
                            <td><s:property value="entry.protocolNumber"/></td>
                            <td><s:property value="description"/></td>
                            <td><s:property value="lineType"/></td>
                            <td class="text-end">€ <s:property value="amount"/></td>
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