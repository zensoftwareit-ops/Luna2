<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="s" uri="/struts-tags" %>
<!DOCTYPE html>
<html lang="it">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Configuratore Conti - Luna2</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.10.0/font/bootstrap-icons.css">
</head>
<body>
<%@ include file="../includes/sidebar.jsp" %>

<div class="col-md-10 content-wrapper p-4">
    <div class="container-fluid">
        <div class="d-flex justify-content-between align-items-center mb-4">
            <h1 class="h3"><i class="bi bi-sliders me-2"></i>Configuratore Conti Automatici</h1>
            <a href="<s:url action='dashboard' namespace='/app/contabilita'/>" class="btn btn-outline-secondary">Dashboard</a>
        </div>

        <s:if test="hasActionMessages()"><div class="alert alert-success"><s:actionmessage/></div></s:if>
        <s:if test="hasActionErrors()"><div class="alert alert-danger"><s:actionerror/></div></s:if>

        <div class="card border-0 shadow-sm">
            <div class="card-body">
                <form action="<s:url action='configuratore-save' namespace='/app/contabilita'/>" method="post">
                    <div class="table-responsive">
                        <table class="table table-striped align-middle">
                            <thead class="table-light">
                            <tr>
                                <th>Chiave logica</th>
                                <th>Regime</th>
                                <th>Forma</th>
                                <th>Conto</th>
                            </tr>
                            </thead>
                            <tbody>
                            <s:iterator value="postingConfigs" var="cfg">
                                <tr>
                                    <td>
                                        <input type="text" class="form-control" name="configKeys" value="<s:property value='#cfg.logicalKey'/>" readonly>
                                    </td>
                                    <td>
                                        <input type="text" class="form-control" name="configRegimes" value="<s:property value='#cfg.taxRegime'/>" placeholder="ANY">
                                    </td>
                                    <td>
                                        <input type="text" class="form-control" name="configForms" value="<s:property value='#cfg.businessForm'/>" placeholder="ANY">
                                    </td>
                                    <td>
                                        <select name="configAccountIds" class="form-select">
                                            <s:iterator value="conti" var="a">
                                                <option value="<s:property value='#a.id'/>" <s:if test="#a.id == #cfg.account.id">selected</s:if>><s:property value="#a.code"/> - <s:property value="#a.name"/></option>
                                            </s:iterator>
                                        </select>
                                    </td>
                                </tr>
                            </s:iterator>
                            </tbody>
                        </table>
                    </div>

                    <button type="submit" class="btn btn-primary">Salva configurazioni</button>
                </form>
            </div>
        </div>
    </div>
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>