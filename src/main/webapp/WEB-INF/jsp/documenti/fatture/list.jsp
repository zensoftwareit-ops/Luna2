<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="s" uri="/struts-tags" %>
<!DOCTYPE html>
<html lang="it">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Fatture - Luna2</title>
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
                    <h1 class="h3"><i class="bi bi-receipt me-2"></i>Fatture</h1>
                    <a href="<s:url action='fatture-create' namespace='/app/documenti'/>" class="btn btn-primary">
                        <i class="bi bi-plus-circle me-2"></i>Nuova Fattura
                    </a>
                </div>

                <!-- Filtri -->
                <div class="card mb-4">
                    <div class="card-body">
                        <form method="post" action="<s:url action='fatture' namespace='/app/documenti'/>">
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
                                    <label for="tipo" class="form-label">Tipo</label>
                                    <select id="tipo" name="tipo" class="form-select" onchange="this.form.submit()">
                                        <option value="">-- Tutti i tipi --</option>
                                        <option value="PROFORMA">Proforma</option>
                                        <option value="REALE">Fattura Reale</option>
                                    </select>
                                </div>
                            </div>
                        </form>
                    </div>
                </div>

                <!-- Tabella Fatture -->
                <div class="card">
                    <div class="card-body">
                        <s:if test="fattureConTracking != null && !fattureConTracking.isEmpty()">
                            <div class="table-responsive">
                                <table id="fattureTable" class="table table-striped table-hover">
                                    <thead>
                                        <tr>
                                            <th>Numero</th>
                                            <th>Data</th>
                                            <th>Cliente</th>
                                            <th>Totale</th>
                                            <th>Tipo</th>
                                            <th>📧 Tracciamento</th>
                                            <th>Azioni</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        <s:iterator value="fattureConTracking" var="dto">
                                            <tr data-tipo="<s:property value='#dto.fattura.tipoFattura'/>">
                                                <td><code><s:property value="#dto.fattura.numero"/></code></td>
                                                <td><s:date name="#dto.fattura.dataFattura" format="dd/MM/yyyy"/></td>
                                                <td>
                                                    <s:if test="#dto.fattura.cliente != null">
                                                        <s:property value="#dto.fattura.cliente.ragioneSociale"/>
                                                    </s:if>
                                                    <s:else>
                                                        <span class="text-muted">-</span>
                                                    </s:else>
                                                </td>
                                                <td class="text-end">
                                                    <strong>
                                                        <s:if test="#dto.fattura.totale != null">
                                                            € <s:text name="format.number"><s:param value="#dto.fattura.totale"/></s:text>
                                                        </s:if>
                                                        <s:else>€ 0,00</s:else>
                                                    </strong>
                                                </td>
                                                <td>
                                                    <s:if test="#dto.fattura.tipoFattura == @it.zensoftware.luna2.model.Fattura$TipoFattura@PROFORMA">
                                                        <span class="badge bg-info">Proforma</span>
                                                    </s:if>
                                                    <s:elseif test="#dto.fattura.tipoFattura == @it.zensoftware.luna2.model.Fattura$TipoFattura@REALE">
                                                        <span class="badge bg-success">Fattura Reale</span>
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
                                                        <a href="<s:url action='fatture-edit' namespace='/app/documenti'><s:param name='id' value='#dto.fattura.id'/></s:url>" 
                                                           class="btn btn-outline-primary" title="Modifica">
                                                            <i class="bi bi-pencil"></i>
                                                        </a>
                                                        <a href="<s:url action='fatture-generatePdf' namespace='/app/documenti'><s:param name='id' value='#dto.fattura.id'/></s:url>" 
                                                           target="_blank" class="btn btn-outline-success" title="Scarica PDF">
                                                            <i class="bi bi-file-pdf"></i>
                                                        </a>
                                                        <a href="<s:url action='fatture-sendEmail' namespace='/app/documenti'><s:param name='id' value='#dto.fattura.id'/></s:url>" 
                                                           class="btn btn-outline-success" title="Invia Email"
                                                           onclick="return confirm('Inviare questa fattura per email?')">
                                                            <i class="bi bi-send"></i>
                                                        </a>
                                                        <a href="<s:url action='fatture-delete' namespace='/app/documenti'><s:param name='id' value='#dto.fattura.id'/></s:url>" 
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
                                <i class="bi bi-info-circle me-2"></i>Nessuna fattura trovata per i criteri selezionati.
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
            if ($('#fattureTable').length) {
                $('#fattureTable').DataTable({
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
