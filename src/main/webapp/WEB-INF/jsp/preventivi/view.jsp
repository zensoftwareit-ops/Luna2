<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="s" uri="/struts-tags" %>
<!DOCTYPE html>
<html lang="it">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Preventivo - Luna2</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.10.0/font/bootstrap-icons.css">
</head>
<body>
    <%@ include file="../includes/sidebar.jsp" %>

    <div class="col-md-10 content-wrapper p-4">
        <div class="container-fluid">
            <div class="d-flex justify-content-between align-items-center mb-4">
                <h1 class="h3"><i class="bi bi-file-earmark-text me-2"></i>Preventivo</h1>
                <a href="<s:url action='preventivi-edit' namespace='/app/documenti'><s:param name='id' value='preventivo.id'/></s:url>" class="btn btn-outline-primary">
                    <i class="bi bi-pencil me-2"></i>Modifica
                </a>
            </div>

            <div class="card">
                <div class="card-body">
                    <dl class="row">
                        <dt class="col-sm-3">Numero</dt>
                        <dd class="col-sm-9"><s:property value="preventivo.numero"/></dd>

                        <dt class="col-sm-3">Cliente</dt>
                        <dd class="col-sm-9">
                            <s:if test="preventivo.cliente != null">
                                <s:property value="preventivo.cliente.ragioneSociale"/>
                            </s:if>
                            <s:else>-</s:else>
                        </dd>

                        <dt class="col-sm-3">Oggetto</dt>
                        <dd class="col-sm-9"><s:property value="preventivo.oggetto"/></dd>

                        <dt class="col-sm-3">Stato</dt>
                        <dd class="col-sm-9"><s:property value="preventivo.stato"/></dd>

                        <dt class="col-sm-3">Totale</dt>
                        <dd class="col-sm-9">€ <s:property value="preventivo.totale"/></dd>
                    </dl>
                </div>
            </div>
        </div>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
