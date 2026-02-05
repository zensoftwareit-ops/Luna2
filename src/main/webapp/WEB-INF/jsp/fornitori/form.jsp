<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="s" uri="/struts-tags" %>
<!DOCTYPE html>
<html lang="it">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title><s:if test="fornitore.id == null">Nuovo Fornitore</s:if><s:else>Modifica Fornitore</s:else> - Luna2</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.10.0/font/bootstrap-icons.css">
</head>
<body>
    <%@ include file="../includes/sidebar.jsp" %>

    <div class="col-md-10 content-wrapper p-4">
        <div class="container-fluid">
            <div class="d-flex justify-content-between align-items-center mb-4">
                <h1 class="h3">
                    <i class="bi bi-truck me-2"></i>
                    <s:if test="fornitore.id == null">Nuovo Fornitore</s:if>
                    <s:else>Modifica Fornitore</s:else>
                </h1>
                <a href="<s:url action='list' namespace='/app/fornitori'/>" class="btn btn-secondary">
                    <i class="bi bi-arrow-left me-2"></i>Torna all'elenco
                </a>
            </div>

            <div class="card">
                <div class="card-body">
                        <s:if test="hasActionErrors()">
                            <div class="alert alert-danger alert-dismissible fade show">
                                <s:actionerror/>
                                <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
                            </div>
                        </s:if>

                        <s:form action="save" method="post" theme="simple" cssClass="needs-validation" novalidate="true">
                            <s:hidden name="fornitore.id"/>

                            <h5 class="card-title mb-3">Informazioni Generali</h5>
                            <div class="row mb-3">
                                <div class="col-md-12">
                                    <label class="form-label">Codice Fornitore *</label>
                                    <s:textfield name="fornitore.codiceFornitore" cssClass="form-control" required="true"/>
                                    <s:fielderror fieldName="fornitore.codiceFornitore" cssClass="text-danger small"/>
                                </div>
                            </div>

                            <div class="row mb-3">
                                <div class="col-md-12">
                                    <label class="form-label">Ragione Sociale *</label>
                                    <s:textfield name="fornitore.ragioneSociale" cssClass="form-control" required="true"/>
                                    <s:fielderror fieldName="fornitore.ragioneSociale" cssClass="text-danger small"/>
                                </div>
                            </div>

                            <div class="row mb-3">
                                <div class="col-md-6">
                                    <label class="form-label">Partita IVA</label>
                                    <s:textfield name="fornitore.partitaIva" cssClass="form-control"/>
                                </div>
                                <div class="col-md-6">
                                    <label class="form-label">Codice Fiscale</label>
                                    <s:textfield name="fornitore.codiceFiscale" cssClass="form-control"/>
                                </div>
                            </div>

                            <h5 class="card-title mb-3 mt-4">Indirizzo</h5>
                            <div class="row mb-3">
                                <div class="col-md-8">
                                    <label class="form-label">Indirizzo</label>
                                    <s:textfield name="fornitore.indirizzo" cssClass="form-control"/>
                                </div>
                                <div class="col-md-4">
                                    <label class="form-label">CAP</label>
                                    <s:textfield name="fornitore.cap" cssClass="form-control"/>
                                </div>
                            </div>

                            <div class="row mb-3">
                                <div class="col-md-6">
                                    <label class="form-label">Città</label>
                                    <s:textfield name="fornitore.citta" cssClass="form-control"/>
                                </div>
                                <div class="col-md-3">
                                    <label class="form-label">Provincia</label>
                                    <s:textfield name="fornitore.provincia" cssClass="form-control" maxlength="2"/>
                                </div>
                                <div class="col-md-3">
                                    <label class="form-label">Nazione</label>
                                    <s:textfield name="fornitore.paese" cssClass="form-control" value="Italia"/>
                                </div>
                            </div>

                            <h5 class="card-title mb-3 mt-4">Contatti</h5>
                            <div class="row mb-3">
                                <div class="col-md-6">
                                    <label class="form-label">Email</label>
                                    <s:textfield name="fornitore.email" cssClass="form-control" type="email"/>
                                </div>
                                <div class="col-md-6">
                                    <label class="form-label">Telefono</label>
                                    <s:textfield name="fornitore.telefono" cssClass="form-control"/>
                                </div>
                            </div>

                            <div class="row mb-3">
                                <div class="col-md-12">
                                    <label class="form-label">Sito Web</label>
                                    <s:textfield name="fornitore.sitoWeb" cssClass="form-control"/>
                                </div>
                            </div>

                            <h5 class="card-title mb-3 mt-4">Dati Commerciali</h5>
                            <div class="row mb-3">
                                <div class="col-md-12">
                                    <label class="form-label">Condizioni Pagamento</label>
                                    <s:textfield name="fornitore.condizioniPagamento" cssClass="form-control" placeholder="es. 30 gg DFFM"/>
                                </div>
                            </div>

                            <div class="mb-3">
                                <label class="form-label">Note</label>
                                <s:textarea name="fornitore.note" cssClass="form-control" rows="3"/>
                            </div>

                            <div class="mt-4">
                                <button type="submit" class="btn btn-primary">
                                    <i class="bi bi-save me-2"></i>Salva Fornitore
                                </button>
                                <a href="<s:url action='list' namespace='/app/fornitori'/>" class="btn btn-secondary">
                                    Annulla
                                </a>
                            </div>
                        </s:form>
                </div>
            </div>
        </div>
    </div>

    </div>
</div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
