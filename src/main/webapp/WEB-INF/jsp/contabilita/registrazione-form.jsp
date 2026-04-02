<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="s" uri="/struts-tags" %>
<!DOCTYPE html>
<html lang="it">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Nuova Registrazione - Luna2</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.10.0/font/bootstrap-icons.css">
</head>
<body>
    <%@ include file="../includes/sidebar.jsp" %>

    <div class="col-md-10 content-wrapper p-4">
        <div class="container-fluid">
            <div class="d-flex justify-content-between align-items-center mb-4">
                <h1 class="h3"><i class="bi bi-pencil-square me-2"></i>Nuova Registrazione</h1>
                <a href="<s:url action='registrazioni' namespace='/app/contabilita'/>" class="btn btn-outline-secondary">Torna alla prima nota</a>
            </div>

            <s:if test="hasActionErrors()">
                <div class="alert alert-danger"><s:actionerror/></div>
            </s:if>

            <div class="card border-0 shadow-sm">
                <div class="card-body">
                    <s:form action="registrazione-save" namespace="/app/contabilita" method="post" theme="simple">
                        <div class="row g-3 mb-4">
                            <div class="col-md-3">
                                <label class="form-label">Protocollo</label>
                                <s:textfield name="registrazione.protocolNumber" cssClass="form-control"/>
                            </div>
                            <div class="col-md-3">
                                <label class="form-label">Data registrazione</label>
                                <s:textfield name="registrazioneEntryDate" cssClass="form-control" type="date"/>
                            </div>
                            <div class="col-md-3">
                                <label class="form-label">Data competenza</label>
                                <s:textfield name="registrazioneCompetenceDate" cssClass="form-control" type="date"/>
                            </div>
                            <div class="col-md-3">
                                <label class="form-label">Tipo</label>
                                <s:select name="registrazione.type" list="entryTypes" cssClass="form-select"/>
                            </div>
                            <div class="col-md-6">
                                <label class="form-label">Descrizione</label>
                                <s:textfield name="registrazione.description" cssClass="form-control"/>
                            </div>
                            <div class="col-md-3">
                                <label class="form-label">Numero documento</label>
                                <s:textfield name="registrazione.documentNumber" cssClass="form-control"/>
                            </div>
                            <div class="col-md-3">
                                <label class="form-label">Controparte</label>
                                <s:textfield name="registrazione.counterparty" cssClass="form-control"/>
                            </div>
                            <div class="col-md-12">
                                <label class="form-label">Note</label>
                                <s:textarea name="registrazione.notes" cssClass="form-control" rows="3"/>
                            </div>
                        </div>

                        <div class="alert alert-info">Inserisci righe contabili in partita doppia. Dare e Avere devono coincidere.</div>

                        <div class="table-responsive">
                            <table class="table table-bordered align-middle">
                                <thead class="table-light">
                                    <tr>
                                        <th>Conto</th>
                                        <th>Tipo</th>
                                        <th>Descrizione riga</th>
                                        <th>Importo</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <s:iterator begin="0" end="5" status="rowIndex">
                                        <tr>
                                            <td>
                                                <select name="lineAccountIds" class="form-select">
                                                    <option value="">Seleziona conto</option>
                                                    <s:iterator value="conti">
                                                        <option value="<s:property value='id'/>"><s:property value="code"/> - <s:property value="name"/></option>
                                                    </s:iterator>
                                                </select>
                                            </td>
                                            <td>
                                                <select name="lineTypes" class="form-select">
                                                    <option value="DEBIT">Dare</option>
                                                    <option value="CREDIT">Avere</option>
                                                </select>
                                            </td>
                                            <td><input type="text" name="lineDescriptions" class="form-control"></td>
                                            <td><input type="number" name="lineAmounts" class="form-control" min="0" step="0.01"></td>
                                        </tr>
                                    </s:iterator>
                                </tbody>
                            </table>
                        </div>

                        <div class="mt-4 d-flex gap-2">
                            <button type="submit" class="btn btn-primary">Salva registrazione</button>
                            <a href="<s:url action='registrazioni' namespace='/app/contabilita'/>" class="btn btn-outline-secondary">Annulla</a>
                        </div>
                    </s:form>
                </div>
            </div>
        </div>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>