<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="s" uri="/struts-tags" %>
<!DOCTYPE html>
<html lang="it">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Dettaglio DDT - Luna2</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.10.0/font/bootstrap-icons.css">
</head>
<body>
    <div class="d-flex">
        <%@ include file="../../includes/sidebar.jsp" %>

        <div class="col-md-10 content-wrapper p-4">
            <div class="container-fluid">
                <div class="d-flex justify-content-between align-items-center mb-4">
                    <h1 class="h3"><i class="bi bi-truck-flatbed me-2"></i>Dettaglio DDT</h1>
                    <div class="btn-group">
                        <a href="<s:url action='ddt' namespace='/app/documenti'/>" class="btn btn-outline-secondary">
                            <i class="bi bi-arrow-left me-1"></i>Lista DDT
                        </a>
                        <a href="<s:url action='ddt-generatePdf' namespace='/app/documenti'><s:param name='id' value='id'/></s:url>" target="_blank" class="btn btn-success">
                            <i class="bi bi-file-pdf me-1"></i>PDF
                        </a>
                        <form method="post" action="<s:url action='ddt-converti-fattura' namespace='/app/documenti'/>" class="d-inline">
                            <input type="hidden" name="id" value="<s:property value='id'/>">
                            <button type="submit" class="btn btn-warning" onclick="return confirm('Convertire questo DDT in fattura?')">
                                <i class="bi bi-arrow-left-right me-1"></i>Converti in fattura
                            </button>
                        </form>
                    </div>
                </div>

                <s:if test="hasActionMessages()">
                    <div class="alert alert-success"><s:actionmessage/></div>
                </s:if>
                <s:if test="hasActionErrors()">
                    <div class="alert alert-danger"><s:actionerror/></div>
                </s:if>

                <div class="card mb-4">
                    <div class="card-header"><h6 class="mb-0">Testata DDT</h6></div>
                    <div class="card-body">
                        <div class="row g-3">
                            <div class="col-md-3"><strong>Numero:</strong> <code><s:property value="ddt.numero"/></code></div>
                            <div class="col-md-3"><strong>Data:</strong> <s:date name="ddt.dataDdt" format="dd/MM/yyyy"/></div>
                            <div class="col-md-6"><strong>Cliente:</strong> <s:property value="ddt.clienteRagioneSociale"/></div>
                            <div class="col-md-4"><strong>Causale:</strong> <s:property value="ddt.causaleTrasporto"/></div>
                            <div class="col-md-4"><strong>Trasportatore:</strong> <s:property value="ddt.trasportatore"/></div>
                            <div class="col-md-4"><strong>Colli:</strong> <s:property value="ddt.numeroColli"/></div>
                            <div class="col-md-12"><strong>Indirizzo destinazione:</strong> <s:property value="ddt.indirizzoDestinazione"/></div>
                            <div class="col-md-4"><strong>Imponibile:</strong> € <s:property value="ddt.imponibile"/></div>
                            <div class="col-md-4"><strong>IVA:</strong> € <s:property value="ddt.iva"/></div>
                            <div class="col-md-4"><strong>Totale:</strong> <strong>€ <s:property value="ddt.totale"/></strong></div>
                        </div>
                    </div>
                </div>

                <div class="card mb-4">
                    <div class="card-header"><h6 class="mb-0">Aggiungi riga</h6></div>
                    <div class="card-body">
                        <form method="post" action="<s:url action='ddt-add-riga' namespace='/app/documenti'/>">
                            <input type="hidden" name="id" value="<s:property value='id'/>">
                            <div class="row g-3">
                                <div class="col-md-4">
                                    <label class="form-label">Prodotto *</label>
                                    <select name="prodottoId" class="form-select" required>
                                        <option value="">Seleziona prodotto</option>
                                        <s:iterator value="prodotti" var="p">
                                            <option value="<s:property value='#p.id'/>"><s:property value="#p.codice"/> - <s:property value="#p.nome"/></option>
                                        </s:iterator>
                                    </select>
                                </div>
                                <div class="col-md-4">
                                    <label class="form-label">Descrizione</label>
                                    <input type="text" name="descrizioneRiga" class="form-control" placeholder="Se vuoto prende nome prodotto">
                                </div>
                                <div class="col-md-2">
                                    <label class="form-label">Quantita *</label>
                                    <input type="number" step="0.001" min="0.001" name="quantita" class="form-control" required>
                                </div>
                                <div class="col-md-2">
                                    <label class="form-label">Prezzo unit. *</label>
                                    <input type="number" step="0.01" min="0" name="prezzoUnitario" class="form-control" required>
                                </div>
                                <div class="col-md-2">
                                    <label class="form-label">IVA %</label>
                                    <input type="number" step="0.01" min="0" name="ivaPercentuale" class="form-control" value="22.00">
                                </div>
                                <div class="col-md-10"></div>
                                <div class="col-md-2 text-end">
                                    <button type="submit" class="btn btn-primary w-100"><i class="bi bi-plus-circle me-1"></i>Aggiungi</button>
                                </div>
                            </div>
                        </form>
                    </div>
                </div>

                <div class="card">
                    <div class="card-header"><h6 class="mb-0">Righe DDT</h6></div>
                    <div class="card-body">
                        <s:if test="righe != null && !righe.isEmpty()">
                            <div class="table-responsive">
                                <table class="table table-striped table-hover align-middle">
                                    <thead>
                                        <tr>
                                            <th>#</th>
                                            <th>Prodotto</th>
                                            <th>Descrizione</th>
                                            <th>Quantita</th>
                                            <th>Prezzo</th>
                                            <th>Imponibile</th>
                                            <th>IVA %</th>
                                            <th>Azioni</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        <s:iterator value="righe" var="r">
                                            <tr>
                                                <td><s:property value="#r.rigaNumero"/></td>
                                                <td><s:property value="#r.prodottoNome"/></td>
                                                <td><s:property value="#r.descrizione"/></td>
                                                <td><s:property value="#r.quantita"/></td>
                                                <td>€ <s:property value="#r.prezzoUnitario"/></td>
                                                <td>€ <s:property value="#r.imponibileRiga"/></td>
                                                <td><s:property value="#r.ivaPercentuale"/></td>
                                                <td>
                                                    <form method="post" action="<s:url action='ddt-delete-riga' namespace='/app/documenti'/>" class="d-inline">
                                                        <input type="hidden" name="id" value="<s:property value='id'/>">
                                                        <input type="hidden" name="rigaId" value="<s:property value='#r.id'/>">
                                                        <button type="submit" class="btn btn-sm btn-outline-danger" onclick="return confirm('Eliminare questa riga?')">
                                                            <i class="bi bi-trash"></i>
                                                        </button>
                                                    </form>
                                                </td>
                                            </tr>
                                        </s:iterator>
                                    </tbody>
                                </table>
                            </div>
                        </s:if>
                        <s:else>
                            <div class="alert alert-info mb-0">Nessuna riga presente. Aggiungi almeno una riga per convertire in fattura.</div>
                        </s:else>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
