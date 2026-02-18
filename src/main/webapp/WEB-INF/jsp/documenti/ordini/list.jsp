<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="s" uri="/struts-tags" %>
<!DOCTYPE html>
<html lang="it">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Ordini - Luna2</title>
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
                    <h1 class="h3"><i class="bi bi-bag me-2"></i>Ordini</h1>
                    <a href="<s:url action='ordini-create' namespace='/app/documenti'/>" class="btn btn-primary">
                        <i class="bi bi-plus-circle me-2"></i>Nuovo Ordine
                    </a>
                </div>

                <!-- Filtri -->
                <div class="card mb-4">
                    <div class="card-body">
                        <form method="post" action="<s:url action='ordini' namespace='/app/documenti'/>">
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
                                    <label for="stato" class="form-label">Stato</label>
                                    <select id="stato" name="stato" class="form-select" onchange="this.form.submit()">
                                        <option value="">-- Tutti gli stati --</option>
                                        <option value="CONFERMATO">Confermato</option>
                                        <option value="IN_LAVORAZIONE">In Lavorazione</option>
                                        <option value="PARZIALMENTE_EVASO">Parzialmente Evaso</option>
                                        <option value="EVASO">Evaso</option>
                                        <option value="ANNULLATO">Annullato</option>
                                    </select>
                                </div>
                            </div>
                        </form>
                    </div>
                </div>

                <!-- Tabella Ordini -->
                <div class="card">
                    <div class="card-body">
                        <s:if test="ordiniConTracking != null && !ordiniConTracking.isEmpty()">
                            <div class="table-responsive">
                                <table id="ordiniTable" class="table table-striped table-hover">
                                    <thead>
                                        <tr>
                                            <th>Numero</th>
                                            <th>Data</th>
                                            <th>Cliente</th>
                                            <th>Totale</th>
                                            <th>Stato</th>
                                            <th>📧 Tracciamento</th>
                                            <th>Azioni</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        <s:iterator value="ordiniConTracking" var="dto">
                                            <tr data-stato="<s:property value='#dto.ordine.stato'/>">
                                                <td><code><s:property value="#dto.ordine.numero"/></code></td>
                                                <td><s:date name="#dto.ordine.dataOrdine" format="dd/MM/yyyy"/></td>
                                                <td>
                                                    <s:if test="#dto.ordine.cliente != null">
                                                        <s:property value="#dto.ordine.cliente.ragioneSociale"/>
                                                    </s:if>
                                                    <s:else>
                                                        <span class="text-muted">-</span>
                                                    </s:else>
                                                </td>
                                                <td class="text-end">
                                                    <strong>
                                                        <s:if test="#dto.ordine.totale != null">
                                                            € <s:text name="format.number"><s:param value="#dto.ordine.totale"/></s:text>
                                                        </s:if>
                                                        <s:else>€ 0,00</s:else>
                                                    </strong>
                                                </td>
                                                <td>
                                                    <s:if test="#dto.ordine.stato == @it.zensoftware.luna2.model.Ordine$Stato@CONFERMATO">
                                                        <span class="badge bg-info">Confermato</span>
                                                    </s:if>
                                                    <s:elseif test="#dto.ordine.stato == @it.zensoftware.luna2.model.Ordine$Stato@IN_LAVORAZIONE">
                                                        <span class="badge bg-primary">In Lavorazione</span>
                                                    </s:elseif>
                                                    <s:elseif test="#dto.ordine.stato == @it.zensoftware.luna2.model.Ordine$Stato@PARZIALMENTE_EVASO">
                                                        <span class="badge bg-warning text-dark">Parzialmente Evaso</span>
                                                    </s:elseif>
                                                    <s:elseif test="#dto.ordine.stato == @it.zensoftware.luna2.model.Ordine$Stato@EVASO">
                                                        <span class="badge bg-success">Evaso</span>
                                                    </s:elseif>
                                                    <s:elseif test="#dto.ordine.stato == @it.zensoftware.luna2.model.Ordine$Stato@ANNULLATO">
                                                        <span class="badge bg-danger">Annullato</span>
                                                    </s:elseif>
                                                </td>
                                                <td>
                                                    <span class="badge" data-bs-toggle="tooltip" title="Email inviate / aperte / scaricate">
                                                        <span class="badge bg-secondary" title="Email inviate"><s:property value="#dto.totalEmails"/></span>
                                                        <s:if test="#dto.totalEmails > 0">
                                                            <span class="badge bg-info" title="Email aperte">👁 <s:property value="#dto.openedEmails"/></span>
                                                            <span class="badge bg-success" title="Email scaricate">⬇ <s:property value="#dto.totalDownloads"/></span>
                                                        </s:if>
                                                    </span>
                                                </td>
                                                <td>
                                                    <div class="btn-group btn-group-sm">
                                                        <a href="<s:url action='ordini-view' namespace='/app/documenti'><s:param name='id' value='#dto.ordine.id'/></s:url>" 
                                                           class="btn btn-outline-info" title="Visualizza">
                                                            <i class="bi bi-eye"></i>
                                                        </a>
                                                        <a href="<s:url action='ordini-pdf' namespace='/app/documenti'><s:param name='id' value='#dto.ordine.id'/></s:url>" 
                                                           target="_blank" class="btn btn-outline-success" title="Scarica PDF">
                                                            <i class="bi bi-file-pdf"></i>
                                                        </a>
                                                        <a href="<s:url action='ordini-edit' namespace='/app/documenti'><s:param name='id' value='#dto.ordine.id'/></s:url>" 
                                                           class="btn btn-outline-primary" title="Modifica">
                                                            <i class="bi bi-pencil"></i>
                                                        </a>
                                                        <a href="<s:url action='ordini-invia' namespace='/app/documenti'><s:param name='id' value='#dto.ordine.id'/></s:url>" 
                                                           class="btn btn-outline-success" title="Invia Email"
                                                           onclick="return confirm('Inviare questo ordine per email?')">
                                                            <i class="bi bi-send"></i>
                                                        </a>
                                                        <s:if test="#dto.ordine.stato != @it.zensoftware.luna2.model.Ordine$Stato@ANNULLATO">
                                                            <a href="<s:url action='ordini-delete' namespace='/app/documenti'><s:param name='id' value='#dto.ordine.id'/></s:url>" 
                                                               class="btn btn-outline-danger" title="Elimina"
                                                               onclick="return confirm('Eliminare questo ordine?')">
                                                                <i class="bi bi-trash"></i>
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
                            <div class="alert alert-info" role="alert">
                                <i class="bi bi-info-circle me-2"></i>Nessun ordine trovato per i criteri selezionati.
                            </div>
                        </s:else>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
    <script src="https://cdn.datatables.net/1.13.4/js/jquery.dataTables.min.js"></script>
    <script src="https://cdn.datatables.net/1.13.4/js/dataTables.bootstrap5.min.js"></script>

    <script>
        document.addEventListener('DOMContentLoaded', function() {
            // Initialize Bootstrap tooltips
            var tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
            var tooltipList = tooltipTriggerList.map(function (tooltipTriggerEl) {
                return new bootstrap.Tooltip(tooltipTriggerEl);
            });

            // Only initialize DataTable if table exists and has data
            if ($('#ordiniTable').length) {
                $('#ordiniTable').DataTable({
                    language: {
                        url: 'https://cdn.datatables.net/plug-ins/1.13.4/i18n/it-IT.json'
                    },
                    pageLength: 25,
                    columnDefs: [
                        { orderable: false, targets: -1 }  // Disable sorting for Azioni column
                    ]
                });
            }
        });
    </script>
</body>
</html>
