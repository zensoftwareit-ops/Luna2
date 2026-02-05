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
                <a href="<s:url action='create' namespace='/app/documenti'/>" class="btn btn-primary">
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
                                    <th>Azioni</th>
                                </tr>
                            </thead>
                            <tbody>
                                <s:iterator value="preventivi">
                                    <tr data-stato="<s:property value='stato'/>">
                                        <td><strong><s:property value="numero"/></strong></td>
                                        <td><s:date name="dataPreventivo" format="dd/MM/yyyy"/></td>
                                        <td><s:property value="cliente.ragioneSociale"/></td>
                                        <td><s:property value="oggetto"/></td>
                                        <td><strong>€ <s:property value="totale"/></strong></td>
                                        <td>
                                            <span class="badge bg-<s:if test='stato.name() == "BOZZA"'>secondary</s:if><s:elseif test='stato.name() == "INVIATO"'>primary</s:elseif><s:elseif test='stato.name() == "ACCETTATO"'>success</s:elseif><s:else>danger</s:else>">
                                                <s:property value="stato"/>
                                            </span>
                                        </td>
                                        <td>
                                            <div class="btn-group btn-group-sm">
                                                <a href="<s:url action='view'><s:param name='id' value='id'/></s:url>" 
                                                   class="btn btn-outline-info" title="Visualizza">
                                                    <i class="bi bi-eye"></i>
                                                </a>
                                                <a href="<s:url action='edit'><s:param name='id' value='id'/></s:url>" 
                                                   class="btn btn-outline-primary" title="Modifica">
                                                    <i class="bi bi-pencil"></i>
                                                </a>
                                                <a href="<s:url action='pdf'><s:param name='id' value='id'/></s:url>" 
                                                   class="btn btn-outline-danger" title="PDF" target="_blank">
                                                    <i class="bi bi-file-pdf"></i>
                                                </a>
                                                <s:if test="stato.name() == 'BOZZA'">
                                                    <button type="button" class="btn btn-outline-success" 
                                                            onclick="sendEmail(<s:property value='id'/>)" title="Invia">
                                                        <i class="bi bi-envelope"></i>
                                                    </button>
                                                </s:if>
                                            </div>
                                        </td>
                                    </tr>
                                </s:iterator>
                            </tbody>
                        </table>
                    </div>
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
            var table = $('#preventiviTable').DataTable({
                language: {
                    url: 'https://cdn.datatables.net/plug-ins/1.13.4/i18n/it-IT.json'
                },
                pageLength: 25,
                order: [[0, 'desc']]
            });

            // Tab filtering
            $('a[data-bs-toggle="tab"]').on('shown.bs.tab', function (e) {
                var target = $(e.target).attr("href");
                if (target === '#tutti') {
                    table.column(5).search('').draw();
                } else {
                    var stato = target.substring(1).toUpperCase();
                    table.column(5).search(stato).draw();
                }
            });
        });

        function sendEmail(id) {
            if (confirm('Inviare il preventivo al cliente via email?')) {
                window.location.href = '<s:url action="send"/>' + '?id=' + id;
            }
        }
    </script>
</body>
</html>
