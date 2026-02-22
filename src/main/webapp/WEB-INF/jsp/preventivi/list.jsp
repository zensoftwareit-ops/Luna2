<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="s" uri="/struts-tags" %>
<!DOCTYPE html>
<html lang="it">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Preventivi - Luna2</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.10.0/font/bootstrap-icons.css">
    <link rel="stylesheet" href="https://cdn.datatables.net/1.13.4/css/dataTables.bootstrap5.min.css">
</head>
<body>
    <%@ include file="../includes/sidebar.jsp" %>
    
    <div class="col-md-10 content-wrapper p-4">
        <div class="container-fluid">
            <div class="d-flex justify-content-between align-items-center mb-4">
                <h1 class="h3"><i class="bi bi-file-earmark-text me-2"></i>Preventivi</h1>
                <a href="<s:url action='preventivi-create' namespace='/app/documenti'/>" class="btn btn-primary">
                    <i class="bi bi-plus-circle me-2"></i>Nuovo Preventivo
                </a>
            </div>

            <s:if test="hasActionMessages()">
                <div class="alert alert-success alert-dismissible fade show">
                    <s:actionmessage/>
                    <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
                </div>
            </s:if>

            <!-- Filter Tabs -->
            <ul class="nav nav-tabs mb-3">
                <li class="nav-item">
                    <a class="nav-link active" href="#tutti" data-bs-toggle="tab">Tutti</a>
                </li>
                <li class="nav-item">
                    <a class="nav-link" href="#bozza" data-bs-toggle="tab">Bozze</a>
                </li>
                <li class="nav-item">
                    <a class="nav-link" href="#inviati" data-bs-toggle="tab">Inviati</a>
                </li>
                <li class="nav-item">
                    <a class="nav-link" href="#accettati" data-bs-toggle="tab">Accettati</a>
                </li>
                <li class="nav-item">
                    <a class="nav-link" href="#rifiutati" data-bs-toggle="tab">Rifiutati</a>
                </li>
            </ul>

            <div class="card">
                <div class="card-body">
                    <s:if test="preventiviConTracking != null && !preventiviConTracking.isEmpty()">
                        <div class="table-responsive">
                            <table id="preventiviTable" class="table table-striped table-hover">
                                <thead>
                                    <tr>
                                        <th>Numero</th>
                                        <th>Data</th>
                                        <th>Cliente</th>
                                        <th>Oggetto</th>
                                        <th>Totale</th>
                                        <th>Stato</th>
                                        <th>📧 Tracciamento</th>
                                        <th>Azioni</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <s:iterator value="preventiviConTracking" var="dto">
                                        <tr data-stato="<s:property value='#dto.preventivo.stato'/>">
                                            <td><code><s:property value="#dto.preventivo.numero"/></code></td>
                                            <td><s:date name="#dto.preventivo.dataPreventivo" format="dd/MM/yyyy"/></td>
                                            <td>
                                                <s:if test="#dto.preventivo.cliente != null">
                                                    <s:property value="#dto.preventivo.cliente.ragioneSociale"/>
                                                </s:if>
                                                <s:else>
                                                    <span class="text-muted">-</span>
                                                </s:else>
                                            </td>
                                            <td><s:property value="#dto.preventivo.oggetto"/></td>
                                            <td class="text-end">
                                                <strong>
                                                    <s:if test="#dto.preventivo.totale != null">
                                                        € <s:text name="format.number"><s:param value="#dto.preventivo.totale"/></s:text>
                                                    </s:if>
                                                    <s:else>€ 0,00</s:else>
                                                </strong>
                                            </td>
                                            <td>
                                                <s:if test="#dto.preventivo.stato == @it.zensoftware.luna2.model.Preventivo$Stato@BOZZA">
                                                    <span class="badge bg-secondary">Bozza</span>
                                                </s:if>
                                                <s:elseif test="#dto.preventivo.stato == @it.zensoftware.luna2.model.Preventivo$Stato@INVIATO">
                                                    <span class="badge bg-info">Inviato</span>
                                                </s:elseif>
                                                <s:elseif test="#dto.preventivo.stato == @it.zensoftware.luna2.model.Preventivo$Stato@ACCETTATO">
                                                    <span class="badge bg-success">Accettato</span>
                                                </s:elseif>
                                                <s:elseif test="#dto.preventivo.stato == @it.zensoftware.luna2.model.Preventivo$Stato@RIFIUTATO">
                                                    <span class="badge bg-danger">Rifiutato</span>
                                                </s:elseif>
                                                <s:elseif test="#dto.preventivo.stato == @it.zensoftware.luna2.model.Preventivo$Stato@SCADUTO">
                                                    <span class="badge bg-warning text-dark">Scaduto</span>
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
                                                    <a href="<s:url action='preventivi-view' namespace='/app/documenti'><s:param name='id' value='#dto.preventivo.id'/></s:url>" 
                                                       class="btn btn-outline-info" title="Visualizza">
                                                        <i class="bi bi-eye"></i>
                                                    </a>
                                                    <div class="btn-group" role="group">
                                                        <button type="button" class="btn btn-outline-success dropdown-toggle" data-bs-toggle="dropdown" title="Scarica PDF">
                                                            <i class="bi bi-file-pdf"></i>
                                                        </button>
                                                        <ul class="dropdown-menu">
                                                            <li><a class="dropdown-item" href="<s:url action='preventivi-pdf' namespace='/app/documenti'><s:param name='id' value='#dto.preventivo.id'/><s:param name='tipo' value='tecnico'/></s:url>" target="_blank">PDF Tecnico</a></li>
                                                            <li><a class="dropdown-item" href="<s:url action='preventivi-pdf' namespace='/app/documenti'><s:param name='id' value='#dto.preventivo.id'/><s:param name='tipo' value='descrittivo'/></s:url>" target="_blank">PDF Descrittivo</a></li>
                                                        </ul>
                                                    </div>
                                                    <a href="<s:url action='preventivi-edit' namespace='/app/documenti'><s:param name='id' value='#dto.preventivo.id'/></s:url>" 
                                                       class="btn btn-outline-primary" title="Modifica">
                                                        <i class="bi bi-pencil"></i>
                                                    </a>
                                                    <s:if test="#dto.preventivo.stato == @it.zensoftware.luna2.model.Preventivo$Stato@BOZZA">
                                                        <a href="<s:url action='preventivi-invia' namespace='/app/documenti'><s:param name='id' value='#dto.preventivo.id'/></s:url>" 
                                                           class="btn btn-outline-success" title="Invia"
                                                           onclick="return confirm('Inviare questo preventivo?')">
                                                            <i class="bi bi-send"></i>
                                                        </a>
                                                    </s:if>
                                                    <a href="<s:url action='preventivi-duplica' namespace='/app/documenti'><s:param name='id' value='#dto.preventivo.id'/></s:url>" 
                                                       class="btn btn-outline-secondary" title="Duplica">
                                                        <i class="bi bi-files"></i>
                                                    </a>
                                                    
                                                    <!-- Pulsante Trasforma in Commessa (solo se ACCETTATO e modulo produzione abilitato) -->
                                                    <s:if test="#dto.preventivo.stato == @it.zensoftware.luna2.model.Preventivo$Stato@ACCETTATO && produzioneEnabled">
                                                        <a href="<s:url action='preventivi-trasforma-commessa' namespace='/app/documenti'><s:param name='id' value='#dto.preventivo.id'/></s:url>" 
                                                           class="btn btn-success" title="Trasforma in Commessa"
                                                           onclick="return confirm('Trasformare questo preventivo in una commessa?')">
                                                            <i class="bi bi-gear"></i>
                                                        </a>
                                                    </s:if>
                                                    
                                                    <!-- Pulsante Crea Fattura diretta (solo se ACCETTATO e modulo produzione disabilitato) -->
                                                    <s:if test="#dto.preventivo.stato == @it.zensoftware.luna2.model.Preventivo$Stato@ACCETTATO && !produzioneEnabled">
                                                        <a href="<s:url action='preventivi-crea-fattura' namespace='/app/documenti'><s:param name='id' value='#dto.preventivo.id'/></s:url>" 
                                                           class="btn btn-primary" title="Crea Fattura"
                                                           onclick="return confirm('Creare una fattura da questo preventivo?')">
                                                            <i class="bi bi-file-earmark-text"></i>
                                                        </a>
                                                    </s:if>
                                                    
                                                    <a href="<s:url action='preventivi-delete' namespace='/app/documenti'><s:param name='id' value='#dto.preventivo.id'/></s:url>" 
                                                       class="btn btn-outline-danger" title="Elimina"
                                                       onclick="return confirm('Eliminare questo preventivo?')">
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
                        <div class="text-center text-muted py-5">
                            <i class="bi bi-inbox" style="font-size: 3em; display: block; margin-bottom: 10px;"></i>
                            <p>Nessun preventivo trovato</p>
                        </div>
                    </s:else>
                </div>
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
        $(document).ready(function() {
            // Only initialize DataTable if table exists and has data
            if ($('#preventiviTable').length) {
                var table = $('#preventiviTable').DataTable({
                    language: {
                        url: 'https://cdn.datatables.net/plug-ins/1.13.4/i18n/it-IT.json'
                    },
                    pageLength: 25,
                    order: [[0, 'desc']],
                    columnDefs: [
                        { targets: 6, orderable: false }
                    ],
                    autoWidth: false
                });

                // Tab filtering
                $('a[data-bs-toggle="tab"]').on('shown.bs.tab', function (e) {
                    var target = $(e.target).attr("href");
                    if (target === '#tutti') {
                        table.column(5).search('').draw();
                    } else if (target === '#bozza') {
                        table.column(5).search('Bozza', true, false).draw();
                    } else if (target === '#inviati') {
                        table.column(5).search('Inviato', true, false).draw();
                    } else if (target === '#accettati') {
                        table.column(5).search('Accettato', true, false).draw();
                    } else if (target === '#rifiutati') {
                        table.column(5).search('Rifiutato', true, false).draw();
                    }
                });
            }
        });
    </script>
</body>
</html>
