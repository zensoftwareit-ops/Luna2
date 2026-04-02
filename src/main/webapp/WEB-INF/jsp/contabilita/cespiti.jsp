<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="s" uri="/struts-tags" %>
<!DOCTYPE html>
<html lang="it">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Cespiti - Luna2</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.10.0/font/bootstrap-icons.css">
</head>
<body>
<%@ include file="../includes/sidebar.jsp" %>

<div class="col-md-10 content-wrapper p-4">
    <div class="container-fluid">
        <div class="d-flex justify-content-between align-items-center mb-4">
            <h1 class="h3"><i class="bi bi-building me-2"></i>Modulo Cespiti</h1>
            <div class="d-flex gap-2">
                <form action="<s:url action='cespiti-ammortamenti-auto' namespace='/app/contabilita'/>" method="post" class="d-flex gap-2">
                    <input type="number" class="form-control" name="annoEsercizio" style="width: 130px" placeholder="Anno">
                    <button type="submit" class="btn btn-outline-primary">Ammortamento automatico</button>
                </form>
                <a href="<s:url action='dashboard' namespace='/app/contabilita'/>" class="btn btn-outline-secondary">Dashboard</a>
            </div>
        </div>

        <s:if test="hasActionMessages()"><div class="alert alert-success"><s:actionmessage/></div></s:if>
        <s:if test="hasActionErrors()"><div class="alert alert-danger"><s:actionerror/></div></s:if>

        <div class="card border-0 shadow-sm mb-4">
            <div class="card-header bg-white">
                <h5 class="mb-0"><s:if test="cespite != null && cespite.id != null">Modifica cespite</s:if><s:else>Nuovo cespite</s:else></h5>
            </div>
            <div class="card-body">
                <s:form action="cespite-save" namespace="/app/contabilita" method="post" theme="simple" cssClass="row g-3">
                    <s:hidden name="cespite.id"/>
                    <div class="col-md-2">
                        <label class="form-label">Codice</label>
                        <s:textfield name="cespite.code" cssClass="form-control"/>
                    </div>
                    <div class="col-md-4">
                        <label class="form-label">Descrizione</label>
                        <s:textfield name="cespite.description" cssClass="form-control"/>
                    </div>
                    <div class="col-md-2">
                        <label class="form-label">Tipo</label>
                        <s:select name="cespite.assetType" list="assetTypes" cssClass="form-select"/>
                    </div>
                    <div class="col-md-2">
                        <label class="form-label">Data acquisto</label>
                        <s:textfield name="cespitePurchaseDate" type="date" cssClass="form-control"/>
                    </div>
                    <div class="col-md-2">
                        <label class="form-label">Costo storico</label>
                        <s:textfield name="cespite.purchaseAmount" cssClass="form-control"/>
                    </div>
                    <div class="col-md-2">
                        <label class="form-label">Valore residuo</label>
                        <s:textfield name="cespite.residualValue" cssClass="form-control"/>
                    </div>
                    <div class="col-md-2">
                        <label class="form-label">Aliquota %</label>
                        <s:textfield name="cespite.depreciationRate" cssClass="form-control"/>
                    </div>
                    <div class="col-md-2">
                        <label class="form-label">Vita utile (anni)</label>
                        <s:textfield name="cespite.usefulLifeYears" cssClass="form-control"/>
                    </div>
                    <div class="col-md-2">
                        <label class="form-label">Stato</label>
                        <s:select name="cespite.status" list="assetStatuses" cssClass="form-select"/>
                    </div>
                    <div class="col-md-4">
                        <label class="form-label">Note</label>
                        <s:textfield name="cespite.notes" cssClass="form-control"/>
                    </div>
                    <div class="col-12 d-flex gap-2">
                        <button type="submit" class="btn btn-primary">Salva cespite</button>
                        <a href="<s:url action='cespiti' namespace='/app/contabilita'/>" class="btn btn-outline-secondary">Reset</a>
                    </div>
                </s:form>
            </div>
        </div>

        <div class="card border-0 shadow-sm">
            <div class="card-header bg-white d-flex justify-content-between align-items-center">
                <h5 class="mb-0">Registro cespiti</h5>
                <span class="badge bg-primary">Quote generate ultimo run: <s:property value="generatedAmmortamenti"/></span>
            </div>
            <div class="table-responsive">
                <table class="table table-striped mb-0 align-middle">
                    <thead class="table-light">
                    <tr>
                        <th>Codice</th>
                        <th>Descrizione</th>
                        <th>Tipo</th>
                        <th>Data acquisto</th>
                        <th class="text-end">Costo storico</th>
                        <th class="text-end">Fondo ammortamento</th>
                        <th class="text-end">Valore residuo</th>
                        <th>Stato</th>
                        <th>Azioni</th>
                    </tr>
                    </thead>
                    <tbody>
                    <s:iterator value="cespiti">
                        <tr>
                            <td><s:property value="code"/></td>
                            <td><s:property value="description"/></td>
                            <td><s:property value="assetType"/></td>
                            <td><s:date name="purchaseDate" format="dd/MM/yyyy"/></td>
                            <td class="text-end">€ <s:property value="purchaseAmount"/></td>
                            <td class="text-end">€ <s:property value="accumulatedDepreciation"/></td>
                            <td class="text-end">€ <s:property value="residualValue"/></td>
                            <td><span class="badge bg-secondary"><s:property value="status"/></span></td>
                            <td>
                                <a href="<s:url action='cespiti' namespace='/app/contabilita'><s:param name='id' value='id'/></s:url>" class="btn btn-sm btn-outline-primary">Modifica</a>
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
