<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="s" uri="/struts-tags" %>
<!DOCTYPE html>
<html lang="it">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><s:if test="lead.id == null">Nuovo Lead</s:if><s:else>Modifica Lead</s:else> - Luna2</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.10.0/font/bootstrap-icons.css">
</head>
<body>
    <%@ include file="../includes/sidebar.jsp" %>
    
    <div class="col-md-10 content-wrapper p-4">
        <div class="container-fluid">
            <div class="d-flex justify-content-between align-items-center mb-4">
                <h1 class="h3">
                    <i class="bi bi-person-lines-fill me-2"></i>
                    <s:if test="lead.id == null">Nuovo Lead</s:if>
                    <s:else>Modifica Lead</s:else>
                </h1>
                <a href="<s:url action='list' namespace='/app/leads'/>" class="btn btn-secondary">
                    <i class="bi bi-arrow-left me-2"></i>Torna all'elenco
                </a>
            </div>

            <s:if test="hasActionErrors()">
                <div class="alert alert-danger alert-dismissible fade show">
                    <s:actionerror/>
                    <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
                </div>
            </s:if>

            <div class="card">
                <div class="card-body">
                    <s:form action="save" method="post" theme="simple" cssClass="needs-validation" novalidate="true">
                        <!-- CSRF Protection Token -->
                        <s:token/>
                        
                        <s:hidden name="lead.id"/>
                        
                        <h5 class="card-title mb-3">Informazioni Contatto</h5>
                        <div class="row mb-3">
                            <div class="col-md-12">
                                <label class="form-label">Azienda *</label>
                                <s:textfield name="lead.azienda" cssClass="form-control" required="true"/>
                                <s:fielderror fieldName="lead.azienda" cssClass="text-danger small"/>
                            </div>
                        </div>

                        <div class="row mb-3">
                            <div class="col-md-6">
                                <label class="form-label">Nome Contatto *</label>
                                <s:textfield name="lead.nomeContatto" cssClass="form-control" required="true"/>
                                <s:fielderror fieldName="lead.nomeContatto" cssClass="text-danger small"/>
                            </div>
                            <div class="col-md-6">
                                <label class="form-label">Cognome Contatto</label>
                                <s:textfield name="lead.cognomeContatto" cssClass="form-control"/>
                            </div>
                        </div>

                        <div class="row mb-3">
                            <div class="col-md-6">
                                <label class="form-label">Email *</label>
                                <s:textfield name="lead.email" cssClass="form-control" type="email" required="true"/>
                                <s:fielderror fieldName="lead.email" cssClass="text-danger small"/>
                            </div>
                            <div class="col-md-6">
                                <label class="form-label">Telefono</label>
                                <s:textfield name="lead.telefono" cssClass="form-control"/>
                            </div>
                        </div>

                        <h5 class="card-title mb-3 mt-4">Indirizzo</h5>
                        <div class="row mb-3">
                            <div class="col-md-8">
                                <label class="form-label">Indirizzo</label>
                                <s:textfield name="lead.indirizzo" cssClass="form-control"/>
                            </div>
                            <div class="col-md-4">
                                <label class="form-label">CAP</label>
                                <s:textfield name="lead.cap" cssClass="form-control"/>
                            </div>
                        </div>

                        <div class="row mb-3">
                            <div class="col-md-6">
                                <label class="form-label">Città</label>
                                <s:textfield name="lead.citta" cssClass="form-control"/>
                            </div>
                            <div class="col-md-3">
                                <label class="form-label">Provincia</label>
                                <s:textfield name="lead.provincia" cssClass="form-control" maxlength="2"/>
                            </div>
                            <div class="col-md-3">
                                <label class="form-label">Nazione</label>
                                <s:textfield name="lead.paese" cssClass="form-control" value="Italia"/>
                            </div>
                        </div>

                        <h5 class="card-title mb-3 mt-4">Dati Lead</h5>
                        <div class="row mb-3">
                            <div class="col-md-6">
                                <label class="form-label">Origine Contatto</label>
                                <s:select name="lead.origine" 
                                         list="originiLead" 
                                         cssClass="form-select"
                                         listKey="name()" listValue="name()"/>
                            </div>
                            <div class="col-md-6">
                                <label class="form-label">Stato Lead *</label>
                                <s:select name="lead.stato" 
                                         list="statiLead" 
                                         cssClass="form-select"
                                         listKey="name()" listValue="name()" required="true"/>
                            </div>
                        </div>

                        <div class="row mb-3">
                            <div class="col-md-6">
                                <label class="form-label">Budget Stimato (€)</label>
                                <s:textfield name="lead.budgetStimato" cssClass="form-control" type="number" step="0.01"/>
                            </div>
                            <div class="col-md-6">
                                <label class="form-label">Probabilità Chiusura (%)</label>
                                <s:textfield name="lead.probabilitaChiusura" cssClass="form-control" type="number" min="0" max="100"/>
                            </div>
                        </div>

                        <div class="row mb-3">
                            <div class="col-md-6">
                                <label class="form-label">Data Contatto</label>
                                <s:textfield name="lead.dataContatto" cssClass="form-control" type="date"/>
                            </div>
                            <div class="col-md-6">
                                <label class="form-label">Data Prossimo Follow-up</label>
                                <s:textfield name="lead.dataProssimoFollowup" cssClass="form-control" type="date"/>
                            </div>
                        </div>

                        <div class="row mb-3">
                            <div class="col-md-12">
                                <label class="form-label">Esigenza/Descrizione</label>
                                <s:textarea name="lead.esigenza" cssClass="form-control" rows="3" placeholder="Descrivi l'esigenza del cliente..."/>
                            </div>
                        </div>

                        <div class="mb-3">
                            <label class="form-label">Note</label>
                            <s:textarea name="lead.note" cssClass="form-control" rows="3"/>
                        </div>

                        <div class="mt-4">
                            <button type="submit" class="btn btn-primary">
                                <i class="bi bi-save me-2"></i>Salva Lead
                            </button>
                            <a href="<s:url action='list' namespace='/app/leads'/>" class="btn btn-secondary">
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
