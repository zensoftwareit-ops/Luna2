<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="s" uri="/struts-tags" %>
<!DOCTYPE html>
<html lang="it">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Fatture Passive - Luna2</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.10.0/font/bootstrap-icons.css">
    <link rel="stylesheet" href="https://cdn.datatables.net/1.13.4/css/dataTables.bootstrap5.min.css">
</head>
<body>
    <div class="d-flex">
        <%@ include file="../../includes/sidebar.jsp" %>

        <div class="col-md-10 content-wrapper p-4">
            <div class="container-fluid">
                <div class="d-flex justify-content-between align-items-center mb-4">
                    <h1 class="h3"><i class="bi bi-inbox me-2"></i>Fatture Passive (Ricevute)</h1>
                    <a href="<s:url action='fatture-passive-sincronizza' namespace='/app/documenti'/>" 
                       class="btn btn-primary" 
                       onclick="return confirm('Sincronizzare le fatture passive ricevute da SDI?')">
                        <i class="bi bi-arrow-clockwise me-2"></i>Sincronizza da SDI
                    </a>
                </div>

                <!-- Filtri -->
                <div class="card mb-4">
                    <div class="card-body">
                        <form method="post" action="<s:url action='fatture-passive' namespace='/app/documenti'/>">
                            <div class="row g-3">
                                <div class="col-md-4">
                                    <label for="anno" class="form-label">Anno</label>
                                    <select id="anno" name="anno" class="form-select" onchange="this.form.submit()">
                                        <option value="">-- Tutti gli anni --</option>
                                        <option value="2024">2024</option>
                                        <option value="2025">2025</option>
                                        <option value="2026" selected>2026</option>
                                    </select>
                                </div>
                                <div class="col-md-4">
                                    <label for="stato" class="form-label">Stato Pagamento</label>
                                    <select id="stato" name="stato" class="form-select" onchange="this.form.submit()">
                                        <option value="">-- Tutti --</option>
                                        <option value="DA_PAGARE">Da Pagare</option>
                                        <option value="PARZIALMENTE_PAGATA">Parzialmente Pagata</option>
                                        <option value="PAGATA">Pagata</option>
                                        <option value="SCADUTA">Scaduta</option>
                                    </select>
                                </div>
                            </div>
                        </form>
                    </div>
                </div>

                <!-- Tabella Fatture Passive -->
                <div class="card">
                    <div class="card-body">
                        <s:if test="fatturePassive != null && !fatturePassive.isEmpty()">
                            <div class="table-responsive">
                                <table id="fatturePassiveTable" class="table table-striped table-hover">
                                    <thead>
                                        <tr>
                                            <th>Numero</th>
                                            <th>Data</th>
                                            <th>Fornitore</th>
                                            <th>Totale</th>
                                            <th>Scadenza</th>
                                            <th>Stato</th>
                                            <th>Ricezione</th>
                                            <th>Azioni</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        <s:iterator value="fatturePassive" var="fattura">
                                            <tr>
                                                <td><code><s:property value="#fattura.numero"/></code></td>
                                                <td><s:date name="#fattura.dataFattura" format="dd/MM/yyyy"/></td>
                                                <td>
                                                    <s:if test="#fattura.fornitore != null">
                                                        <s:property value="#fattura.fornitore.ragioneSociale"/>
                                                    </s:if>
                                                    <s:else>
                                                        <s:property value="#fattura.fornitoreNome"/> 
                                                        <small class="text-muted">(<s:property value="#fattura.fornitorePiva"/>)</small>
                                                    </s:else>
                                                </td>
                                                <td class="text-end">
                                                    <strong>
                                                        <s:if test="#fattura.totale != null">
                                                            € <s:text name="format.number"><s:param value="#fattura.totale"/></s:text>
                                                        </s:if>
                                                        <s:else>€ 0,00</s:else>
                                                    </strong>
                                                </td>
                                                <td>
                                                    <s:if test="#fattura.dataScadenza != null">
                                                        <s:date name="#fattura.dataScadenza" format="dd/MM/yyyy"/>
                                                        <s:if test="#fattura.dataScadenza.time < @java.lang.System@currentTimeMillis() && !#fattura.statoPagamento.equals(@it.zensoftware.luna2.model.FatturaPassiva$StatoPagamento@PAGATA)">
                                                            <br/><small class="badge bg-danger">SCADUTA</small>
                                                        </s:if>
                                                    </s:if>
                                                </td>
                                                <td>
                                                    <s:if test="#fattura.statoPagamento.equals(@it.zensoftware.luna2.model.FatturaPassiva$StatoPagamento@DA_PAGARE)">
                                                        <span class="badge bg-warning text-dark">💰 Da Pagare</span>
                                                    </s:if>
                                                    <s:elseif test="#fattura.statoPagamento.equals(@it.zensoftware.luna2.model.FatturaPassiva$StatoPagamento@PARZIALMENTE_PAGATA)">
                                                        <span class="badge bg-info">⚠️ Parz. Pagata</span>
                                                    </s:elseif>
                                                    <s:elseif test="#fattura.statoPagamento.equals(@it.zensoftware.luna2.model.FatturaPassiva$StatoPagamento@PAGATA)">
                                                        <span class="badge bg-success">✓ Pagata</span>
                                                    </s:elseif>
                                                    <s:elseif test="#fattura.statoPagamento.equals(@it.zensoftware.luna2.model.FatturaPassiva$StatoPagamento@SCADUTA)">
                                                        <span class="badge bg-danger">✗ Scaduta</span>
                                                    </s:elseif>
                                                </td>
                                                <td>
                                                    <small class="text-muted">
                                                        <s:date name="#fattura.dataRicezione" format="dd/MM/yyyy HH:mm"/>
                                                    </small>
                                                </td>
                                                <td>
                                                    <div class="btn-group btn-group-sm">
                                                        <a href="<s:url action='fatture-passive-view' namespace='/app/documenti'><s:param name='id' value='#fattura.id'/></s:url>" 
                                                           class="btn btn-outline-primary" title="Visualizza">
                                                            <i class="bi bi-eye"></i>
                                                        </a>
                                                        <s:if test="!#fattura.statoPagamento.equals(@it.zensoftware.luna2.model.FatturaPassiva$StatoPagamento@PAGATA)">
                                                            <a href="<s:url action='fatture-passive-registra-pagamento' namespace='/app/documenti'><s:param name='id' value='#fattura.id'/></s:url>" 
                                                               class="btn btn-outline-success" title="Registra Pagamento"
                                                               onclick="return confirm('Registrare il pagamento di questa fattura?')">
                                                                <i class="bi bi-check-circle"></i>
                                                            </a>
                                                        </s:if>
                                                        <a href="<s:url action='fatture-passive-delete' namespace='/app/documenti'><s:param name='id' value='#fattura.id'/></s:url>" 
                                                           class="btn btn-outline-danger" title="Elimina"
                                                           onclick="return confirm('Eliminare questa fattura?')">
                                                            <i class="bi bi-trash"></i>
                                                        </a>
                                                    </div>
                                                </td>
                                            </tr>
                                        </s:iterator>
                                    </tbody>
                                </table>
                            </div>
                        </s:if>
                        <s:else>
                            <div class="alert alert-info" role="alert">
                                <i class="bi bi-info-circle me-2"></i>Nessuna fattura passiva trovata. Prova a sincronizzare le fatture da SDI.
                            </div>
                        </s:else>
                    </div>
                </div>

                <!-- Esportazione Assosoftware -->
                <div class="card mt-4">
                    <div class="card-body">
                        <div class="row align-items-center">
                            <div class="col">
                                <h6 class="mb-0"><i class="bi bi-download me-2"></i>Esportazione Assosoftware</h6>
                                <small class="text-muted">Esporta le fatture passive in formato standard Assosoftware</small>
                            </div>
                            <div class="col-auto">
                                <a href="<s:url action='fatture-passive-esporta-assosoftware' namespace='/app/documenti'><s:param name='anno' value='anno'/></s:url>" 
                                   class="btn btn-outline-secondary btn-sm" 
                                   title="Scarica tutte le fatture passive in formato Assosoftware per l'anno selezionato">
                                    <i class="bi bi-file-earmark-text me-1"></i>Esporta Anno
                                </a>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
    <script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
    <script src="https://cdn.datatables.net/1.13.4/js/jquery.dataTables.min.js"></script>
    <script src="https://cdn.datatables.net/1.13.4/js/dataTables.bootstrap5.min.js"></script>
    <script>
        $(document).ready(function() {
            $('#fatturePassiveTable').DataTable({
                language: {
                    url: '//cdn.datatables.net/plug-ins/1.13.4/i18n/it-IT.json'
                },
                columnDefs: [
                    { orderable: false, targets: -1 }
                ]
            });
        });
    </script>
</body>
</html>
