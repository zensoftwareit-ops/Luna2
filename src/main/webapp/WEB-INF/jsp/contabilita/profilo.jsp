<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="s" uri="/struts-tags" %>
<!DOCTYPE html>
<html lang="it">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Profilo Contabile - Luna2</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.10.0/font/bootstrap-icons.css">
</head>
<body>
    <%@ include file="../includes/sidebar.jsp" %>

    <div class="col-md-10 content-wrapper p-4">
        <div class="container-fluid">
            <div class="d-flex justify-content-between align-items-center mb-4">
                <h1 class="h3"><i class="bi bi-building-gear me-2"></i>Profilo Fiscale e Societario</h1>
                <a href="<s:url action='dashboard' namespace='/app/contabilita'/>" class="btn btn-outline-secondary">Dashboard</a>
            </div>

            <s:if test="hasActionErrors()">
                <div class="alert alert-danger"><s:actionerror/></div>
            </s:if>

            <div class="card border-0 shadow-sm">
                <div class="card-body">
                    <s:form action="profilo-save" namespace="/app/contabilita" method="post" theme="simple" enctype="multipart/form-data">
                        <s:hidden name="profilo.id"/>
                        <div class="row g-3">
                            <div class="col-md-6">
                                <label class="form-label">Ragione sociale</label>
                                <s:textfield name="profilo.companyName" cssClass="form-control"/>
                            </div>
                            <div class="col-md-3">
                                <label class="form-label">Partita IVA</label>
                                <s:textfield name="profilo.vatNumber" cssClass="form-control"/>
                            </div>
                            <div class="col-md-3">
                                <label class="form-label">Codice fiscale</label>
                                <s:textfield name="profilo.taxCode" cssClass="form-control"/>
                            </div>
                            <div class="col-md-4">
                                <label class="form-label">Forma giuridica</label>
                                <s:select name="profilo.businessForm" list="businessForms" cssClass="form-select"/>
                            </div>
                            <div class="col-md-4">
                                <label class="form-label">Regime fiscale</label>
                                <s:select name="profilo.taxRegime" list="taxRegimes" cssClass="form-select"/>
                            </div>
                            <div class="col-md-4">
                                <label class="form-label">Frequenza liquidazione IVA</label>
                                <s:select name="profilo.vatFrequency" list="vatFrequencies" cssClass="form-select"/>
                            </div>
                            <div class="col-md-3">
                                <label class="form-label">Mese inizio esercizio</label>
                                <s:textfield name="profilo.fiscalYearStartMonth" cssClass="form-control"/>
                            </div>
                            <div class="col-md-3">
                                <label class="form-label">Aliquota contributiva %</label>
                                <s:textfield name="profilo.contributionRate" cssClass="form-control"/>
                            </div>
                            <div class="col-md-3">
                                <label class="form-label">Aliquota imposta sostitutiva %</label>
                                <s:textfield name="profilo.substituteTaxRate" cssClass="form-control"/>
                            </div>
                            <div class="col-md-3">
                                <label class="form-label">Email commercialista</label>
                                <s:textfield name="profilo.accountantEmail" cssClass="form-control"/>
                            </div>
                            <div class="col-md-6">
                                <label class="form-label">PEC</label>
                                <s:textfield name="profilo.pecEmail" cssClass="form-control"/>
                            </div>
                            <div class="col-md-12">
                                <label class="form-label">Note operative</label>
                                <s:textarea name="profilo.notes" cssClass="form-control" rows="4"/>
                            </div>
                            <div class="col-md-6">
                                <label class="form-label">Logo aziendale</label>
                                <div class="input-group">
                                    <input type="file" name="logoFile" class="form-control" accept="image/*" />
                                </div>
                                <s:if test="profilo.logoPath != null">
                                    <div class="mt-2">
                                        <small class="text-muted d-block mb-2">Logo attuale:</small>
                                        <img src="${pageContext.request.contextPath}<s:property value='profilo.logoPath'/>" 
                                             alt="Logo aziendale" class="img-thumbnail" style="max-width: 150px; max-height: 100px;">
                                    </div>
                                </s:if>
                            </div>
                        </div>

                        <div class="mt-4 d-flex gap-2">
                            <button type="submit" class="btn btn-primary"><i class="bi bi-save me-2"></i>Salva profilo</button>
                            <a href="<s:url action='dashboard' namespace='/app/contabilita'/>" class="btn btn-outline-secondary">Annulla</a>
                        </div>
                    </s:form>
                </div>
            </div>
        </div>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>