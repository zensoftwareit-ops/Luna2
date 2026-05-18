<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="s" uri="/struts-tags" %>
<!DOCTYPE html>
<html lang="it">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Liquidazioni IVA - Luna2</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.10.0/font/bootstrap-icons.css">
    <link rel="stylesheet" href="https://cdn.datatables.net/1.13.4/css/dataTables.bootstrap5.min.css">
</head>
<body>
    <%@ include file="../includes/sidebar.jsp" %>

    <div class="col-md-10 content-wrapper p-4">
        <div class="container-fluid">
            <div class="d-flex justify-content-between align-items-center mb-4">
                <h1 class="h3"><i class="bi bi-calculator me-2"></i>Liquidazioni IVA</h1>
                <div class="btn-group">
                    <button type="button" class="btn btn-primary" data-bs-toggle="modal" data-bs-target="#calcolaModal">
                        <i class="bi bi-plus-circle me-2"></i>Calcola Liquidazione
                    </button>
                    <a href="<s:url action='liquida-auto' namespace='/app/contabilita'/>" class="btn btn-success">
                        <i class="bi bi-gear me-2"></i>Genera Automaticamente
                    </a>
                </div>
            </div>

            <s:if test="hasActionMessages()">
                <div class="alert alert-success alert-dismissible fade show">
                    <s:actionmessage/>
                    <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
                </div>
            </s:if>

            <s:if test="hasActionErrors()">
                <div class="alert alert-danger alert-dismissible fade show">
                    <s:actionerror/>
                    <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
                </div>
            </s:if>

            <!-- Filtri -->
            <div class="card mb-3">
                <div class="card-body">
                    <form method="get" action="<s:url action='liquida-list' namespace='/app/contabilita'/>" class="row g-3 align-items-end">
                        <div class="col-md-3">
                            <label class="form-label">Anno</label>
                            <input type="number" name="annoFiltro" class="form-control" value="<s:property value='annoFiltro'/>" min="2020" max="2099">
                        </div>
                        <div class="col-md-3">
                            <label class="form-label">Stato</label>
                            <select name="statoFiltro" class="form-select">
                                <option value="">Tutti</option>
                                <option value="DRAFT" <s:if test="statoFiltro == 'DRAFT'">selected</s:if>>Bozza</option>
                                <option value="CALCULATED" <s:if test="statoFiltro == 'CALCULATED'">selected</s:if>>Calcolata</option>
                                <option value="SUBMITTED" <s:if test="statoFiltro == 'SUBMITTED'">selected</s:if>>Presentata</option>
                                <option value="PAID" <s:if test="statoFiltro == 'PAID'">selected</s:if>>Pagata</option>
                                <option value="OVERDUE" <s:if test="statoFiltro == 'OVERDUE'">selected</s:if>>Scaduta</option>
                            </select>
                        </div>
                        <div class="col-md-3">
                            <button type="submit" class="btn btn-outline-primary">
                                <i class="bi bi-search me-2"></i>Filtra
                            </button>
                        </div>
                    </form>
                </div>
            </div>

            <!-- Tabella Liquidazioni -->
            <div class="card">
                <div class="card-body">
                    <s:if test="liquidazioni != null && !liquidazioni.isEmpty()">
                        <div class="table-responsive">
                            <table id="liquidazioniTable" class="table table-striped table-hover">
                                <thead>
                                    <tr>
                                        <th>Periodo</th>
                                        <th>Data Inizio</th>
                                        <th>Data Fine</th>
                                        <th>IVA Fatture Attive</th>
                                        <th>IVA Deducibile</th>
                                        <th>IVA Netta</th>
                                        <th>Scadenza</th>
                                        <th>Stato</th>
                                        <th>Azioni</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <s:iterator value="liquidazioni" var="liq">
                                        <tr>
                                            <td><strong><s:property value="#liq.liquidationPeriod"/></strong></td>
                                            <td><s:date name="#liq.liquidationDate" format="dd/MM/yyyy"/></td>
                                            <td><s:date name="#liq.endDate" format="dd/MM/yyyy"/></td>
                                            <td class="text-end">€ <s:text name="format.number"><s:param value="#liq.ivaInvoicesAmount"/></s:text></td>
                                            <td class="text-end">€ <s:text name="format.number"><s:param value="#liq.ivaCostsAmount"/></s:text></td>
                                            <td class="text-end"><strong>€ <s:text name="format.number"><s:param value="#liq.amountDue"/></s:text></strong></td>
                                            <td><s:date name="#liq.dueDate" format="dd/MM/yyyy"/></td>
                                            <td>
                                                <s:if test="#liq.status == @it.zensoftware.luna2.model.IvaLiquidation$LiquidationStatus@DRAFT">
                                                    <span class="badge bg-secondary">Bozza</span>
                                                </s:if>
                                                <s:elseif test="#liq.status == @it.zensoftware.luna2.model.IvaLiquidation$LiquidationStatus@CALCULATED">
                                                    <span class="badge bg-warning text-dark">Calcolata</span>
                                                </s:elseif>
                                                <s:elseif test="#liq.status == @it.zensoftware.luna2.model.IvaLiquidation$LiquidationStatus@SUBMITTED">
                                                    <span class="badge bg-info">Presentata</span>
                                                </s:elseif>
                                                <s:elseif test="#liq.status == @it.zensoftware.luna2.model.IvaLiquidation$LiquidationStatus@PAID">
                                                    <span class="badge bg-success">Pagata</span>
                                                </s:elseif>
                                                <s:elseif test="#liq.status == @it.zensoftware.luna2.model.IvaLiquidation$LiquidationStatus@OVERDUE">
                                                    <span class="badge bg-danger">Scaduta</span>
                                                </s:elseif>
                                            </td>
                                            <td>
                                                <div class="btn-group btn-group-sm">
                                                    <a href="<s:url action='liquida-export' namespace='/app/contabilita'><s:param name='id' value='#liq.id'/></s:url>"
                                                       class="btn btn-outline-success" title="Esporta PDF" target="_blank">
                                                        <i class="bi bi-file-pdf"></i>
                                                    </a>
                                                    <s:if test="#liq.status != @it.zensoftware.luna2.model.IvaLiquidation$LiquidationStatus@PAID">
                                                        <a href="<s:url action='liquida-mark-paid' namespace='/app/contabilita'><s:param name='id' value='#liq.id'/></s:url>"
                                                           class="btn btn-outline-primary" title="Marca come pagata"
                                                           onclick="return confirm('Marcarea questa liquidazione come pagata?')">
                                                            <i class="bi bi-check-circle"></i>
                                                        </a>
                                                    </s:if>
                                                </div>
                                            </td>
                                        </tr>
                                    </s:iterator>
                                </tbody>
                            </table>
                        </div>
                    </s:if>
                    <s:else>
                        <div class="text-center text-muted py-5">
                            <i class="bi bi-inbox" style="font-size: 3em; display: block; margin-bottom: 10px;"></i>
                            <p>Nessuna liquidazione trovata</p>
                        </div>
                    </s:else>
                </div>
            </div>
        </div>
    </div>

    <!-- Modal Calcola -->
    <div class="modal fade" id="calcolaModal" tabindex="-1">
        <div class="modal-dialog">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title">Calcola Liquidazione IVA</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                </div>
                <form method="post" action="<s:url action='liquida-calcola' namespace='/app/contabilita'/>">
                    <div class="modal-body">
                        <div class="mb-3">
                            <label class="form-label">Data Inizio</label>
                            <input type="date" name="liquidazioneDateFrom" class="form-control" required>
                        </div>
                        <div class="mb-3">
                            <label class="form-label">Data Fine</label>
                            <input type="date" name="liquidazioneDateTo" class="form-control">
                        </div>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Chiudi</button>
                        <button type="submit" class="btn btn-primary">Calcola</button>
                    </div>
                </form>
            </div>
        </div>
    </div>

    <script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
    <script src="https://cdn.datatables.net/1.13.4/js/jquery.dataTables.min.js"></script>
    <script src="https://cdn.datatables.net/1.13.4/js/dataTables.bootstrap5.min.js"></script>
    <script>
        $(document).ready(function() {
            if ($('#liquidazioniTable').length) {
                $('#liquidazioniTable').DataTable({
                    language: {
                        url: 'https://cdn.datatables.net/plug-ins/1.13.4/i18n/it-IT.json'
                    },
                    pageLength: 25,
                    order: [[0, 'desc']],
                    columnDefs: [
                        { targets: 8, orderable: false }
                    ],
                    autoWidth: false
                });
            }
        });
    </script>
</body>
</html>
