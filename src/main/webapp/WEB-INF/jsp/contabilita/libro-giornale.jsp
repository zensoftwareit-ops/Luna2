<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="s" uri="/struts-tags" %>
<!DOCTYPE html>
<html lang="it">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Libro Giornale - Luna2</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.10.0/font/bootstrap-icons.css">
</head>
<body>
<%@ include file="../includes/sidebar.jsp" %>

<div class="col-md-10 content-wrapper p-4">
    <div class="container-fluid">
        <div class="d-flex justify-content-between align-items-center mb-4">
            <h1 class="h3"><i class="bi bi-journal-richtext me-2"></i>Libro Giornale</h1>
            <div class="d-flex gap-2">
                <a href="<s:url action='libro-giornale-stampa' namespace='/app/contabilita'/>" class="btn btn-outline-primary">
                    <i class="bi bi-printer me-1"></i>Stampa PDF
                </a>
                <a href="<s:url action='dashboard' namespace='/app/contabilita'/>" class="btn btn-outline-secondary">Dashboard</a>
            </div>
        </div>

        <div class="card border-0 shadow-sm">
            <div class="table-responsive">
                <table class="table table-striped mb-0">
                    <thead class="table-light">
                    <tr>
                        <th>Protocollo</th>
                        <th>Data</th>
                        <th>Descrizione</th>
                        <th>Documento</th>
                        <th>Fonte</th>
                        <th class="text-end">Dare</th>
                        <th class="text-end">Avere</th>
                        <th>Stato</th>
                    </tr>
                    </thead>
                    <tbody>
                    <s:iterator value="registrazioni">
                        <tr>
                            <td><s:property value="protocolNumber"/></td>
                            <td><s:date name="entryDate" format="dd/MM/yyyy"/></td>
                            <td><s:property value="description"/></td>
                            <td><s:property value="documentNumber"/></td>
                            <td><s:property value="sourceType"/> #<s:property value="sourceId"/></td>
                            <td class="text-end">€ <s:property value="totalDebit"/></td>
                            <td class="text-end">€ <s:property value="totalCredit"/></td>
                            <td><span class="badge bg-secondary"><s:property value="status"/></span></td>
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