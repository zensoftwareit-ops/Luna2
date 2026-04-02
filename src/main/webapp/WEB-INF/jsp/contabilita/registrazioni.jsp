<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="s" uri="/struts-tags" %>
<!DOCTYPE html>
<html lang="it">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Prima Nota - Luna2</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.10.0/font/bootstrap-icons.css">
</head>
<body>
    <%@ include file="../includes/sidebar.jsp" %>

    <div class="col-md-10 content-wrapper p-4">
        <div class="container-fluid">
            <div class="d-flex justify-content-between align-items-center mb-4">
                <h1 class="h3"><i class="bi bi-journal-text me-2"></i>Prima Nota</h1>
                <div class="d-flex gap-2">
                    <a href="<s:url action='sincronizza-fatture' namespace='/app/contabilita'/>" class="btn btn-outline-primary">Sincronizza fatture</a>
                    <a href="<s:url action='registrazione-nuova' namespace='/app/contabilita'/>" class="btn btn-primary">Nuova registrazione</a>
                </div>
            </div>

            <s:if test="hasActionMessages()">
                <div class="alert alert-success"><s:actionmessage/></div>
            </s:if>
            <s:if test="hasActionErrors()">
                <div class="alert alert-danger"><s:actionerror/></div>
            </s:if>

            <div class="card border-0 shadow-sm">
                <div class="table-responsive">
                    <table class="table table-hover mb-0">
                        <thead class="table-light">
                            <tr>
                                <th>Protocollo</th>
                                <th>Data</th>
                                <th>Descrizione</th>
                                <th>Tipo</th>
                                <th>Controparte</th>
                                <th>Stato</th>
                                <th class="text-end">Dare</th>
                                <th>Azioni</th>
                            </tr>
                        </thead>
                        <tbody>
                            <s:iterator value="registrazioni">
                                <tr>
                                    <td><s:property value="protocolNumber"/></td>
                                    <td><s:date name="entryDate" format="dd/MM/yyyy"/></td>
                                    <td><s:property value="description"/></td>
                                    <td><s:property value="type"/></td>
                                    <td><s:property value="counterparty"/></td>
                                    <td><span class="badge bg-info text-dark"><s:property value="status"/></span></td>
                                    <td class="text-end">€ <s:property value="totalDebit"/></td>
                                    <td>
                                        <form action="<s:url action='registrazione-posta' namespace='/app/contabilita'/>" method="post" class="d-inline">
                                            <input type="hidden" name="id" value="<s:property value='id'/>">
                                            <button type="submit" class="btn btn-sm btn-outline-success">Contabilizza</button>
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

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>