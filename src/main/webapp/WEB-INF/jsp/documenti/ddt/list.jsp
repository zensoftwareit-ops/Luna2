<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="s" uri="/struts-tags" %>
<!DOCTYPE html>
<html lang="it">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>DDT - Luna2</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.10.0/font/bootstrap-icons.css">
</head>
<body>
    <div class="d-flex">
        <%@ include file="../../includes/sidebar.jsp" %>

        <div class="col-md-10 content-wrapper p-4">
            <div class="container-fluid">
                <div class="d-flex justify-content-between align-items-center mb-4">
                    <h1 class="h3"><i class="bi bi-truck-flatbed me-2"></i>DDT</h1>
                </div>

                <s:if test="hasActionMessages()">
                    <div class="alert alert-success">
                        <s:actionmessage/>
                    </div>
                </s:if>
                <s:if test="hasActionErrors()">
                    <div class="alert alert-danger">
                        <s:actionerror/>
                    </div>
                </s:if>

                <div class="card mb-4">
                    <div class="card-header">
                        <h6 class="mb-0"><i class="bi bi-plus-circle me-2"></i>Nuovo DDT</h6>
                    </div>
                    <div class="card-body">
                        <form method="post" action="<s:url action='ddt-save' namespace='/app/documenti'/>">
                            <div class="row g-3">
                                <div class="col-md-4">
                                    <label class="form-label">Cliente *</label>
                                    <select name="clienteId" class="form-select" required>
                                        <option value="">Seleziona cliente</option>
                                        <s:iterator value="clienti" var="c">
                                            <option value="<s:property value='#c.id'/>"><s:property value="#c.ragioneSociale"/></option>
                                        </s:iterator>
                                    </select>
                                </div>
                                <div class="col-md-3">
                                    <label class="form-label">Data DDT *</label>
                                    <input type="date" name="dataDdt" class="form-control" required>
                                </div>
                                <div class="col-md-5">
                                    <label class="form-label">Causale trasporto</label>
                                    <input type="text" name="causaleTrasporto" class="form-control" placeholder="Es. Vendita merce">
                                </div>
                                <div class="col-md-3">
                                    <label class="form-label">Aspetto beni</label>
                                    <input type="text" name="aspettoBeni" class="form-control" placeholder="Es. Scatole">
                                </div>
                                <div class="col-md-2">
                                    <label class="form-label">N. colli</label>
                                    <input type="number" min="1" name="numeroColli" class="form-control" placeholder="1">
                                </div>
                                <div class="col-md-3">
                                    <label class="form-label">Trasportatore</label>
                                    <input type="text" name="trasportatore" class="form-control">
                                </div>
                                <div class="col-md-4">
                                    <label class="form-label">Indirizzo destinazione</label>
                                    <input type="text" name="indirizzoDestinazione" class="form-control">
                                </div>
                                <div class="col-md-12">
                                    <label class="form-label">Note</label>
                                    <textarea name="note" class="form-control" rows="2"></textarea>
                                </div>
                                <div class="col-12 text-end">
                                    <button type="submit" class="btn btn-primary">
                                        <i class="bi bi-save me-2"></i>Crea DDT
                                    </button>
                                </div>
                            </div>
                        </form>
                    </div>
                </div>

                <div class="card">
                    <div class="card-header d-flex justify-content-between align-items-center">
                        <h6 class="mb-0"><i class="bi bi-list-ul me-2"></i>Elenco DDT</h6>
                        <form method="get" action="<s:url action='ddt' namespace='/app/documenti'/>" class="d-flex gap-2">
                            <input type="number" name="anno" class="form-control form-control-sm" style="width: 110px" placeholder="Anno" value="<s:property value='anno'/>">
                            <input type="text" name="searchTerm" class="form-control form-control-sm" placeholder="Numero/Cliente" value="<s:property value='searchTerm'/>">
                            <button class="btn btn-sm btn-outline-secondary" type="submit">Filtra</button>
                        </form>
                    </div>
                    <div class="card-body">
                        <s:if test="ddtList != null && !ddtList.isEmpty()">
                            <div class="table-responsive">
                                <table class="table table-striped table-hover align-middle">
                                    <thead>
                                        <tr>
                                            <th>Numero</th>
                                            <th>Data</th>
                                            <th>Cliente</th>
                                            <th>Causale</th>
                                            <th>Trasportatore</th>
                                            <th>Colli</th>
                                            <th class="text-end">Totale</th>
                                            <th>Azioni</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        <s:iterator value="ddtList" var="d">
                                            <tr>
                                                <td><code><s:property value="#d.numero"/></code></td>
                                                <td><s:date name="#d.dataDdt" format="dd/MM/yyyy"/></td>
                                                <td><s:property value="#d.clienteRagioneSociale"/></td>
                                                <td><s:property value="#d.causaleTrasporto"/></td>
                                                <td><s:property value="#d.trasportatore"/></td>
                                                <td><s:property value="#d.numeroColli"/></td>
                                                <td class="text-end">€ <s:property value="#d.totale"/></td>
                                                <td>
                                                    <div class="btn-group btn-group-sm">
                                                        <a href="<s:url action='ddt-view' namespace='/app/documenti'><s:param name='id' value='#d.id'/></s:url>"
                                                           class="btn btn-outline-primary" title="Dettaglio e righe">
                                                            <i class="bi bi-card-list"></i>
                                                        </a>
                                                        <a href="<s:url action='ddt-generatePdf' namespace='/app/documenti'><s:param name='id' value='#d.id'/></s:url>"
                                                           target="_blank"
                                                           class="btn btn-outline-success" title="Stampa PDF">
                                                            <i class="bi bi-file-pdf"></i>
                                                        </a>
                                                        <form method="post" action="<s:url action='ddt-converti-fattura' namespace='/app/documenti'/>" class="d-inline">
                                                            <input type="hidden" name="id" value="<s:property value='#d.id'/>">
                                                            <button type="submit" class="btn btn-outline-warning" onclick="return confirm('Convertire questo DDT in fattura?')" title="Converti in fattura">
                                                                <i class="bi bi-arrow-left-right"></i>
                                                            </button>
                                                        </form>
                                                        <form method="post" action="<s:url action='ddt-delete' namespace='/app/documenti'/>" class="d-inline">
                                                            <input type="hidden" name="id" value="<s:property value='#d.id'/>">
                                                            <button type="submit" class="btn btn-outline-danger" onclick="return confirm('Eliminare il DDT selezionato?')" title="Elimina DDT">
                                                                <i class="bi bi-trash"></i>
                                                            </button>
                                                        </form>
                                                    </div>
                                                </td>
                                            </tr>
                                        </s:iterator>
                                    </tbody>
                                </table>
                            </div>
                        </s:if>
                        <s:else>
                            <div class="alert alert-info mb-0">
                                Nessun DDT trovato. Crea il primo DDT dal form qui sopra.
                            </div>
                        </s:else>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
