<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
<!DOCTYPE html>
<html lang="it">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Ticket - Broker Noleggio</title>
    <link rel="stylesheet" href="<s:url value='/css/bootstrap.min.css'/>">
    <link rel="stylesheet" href="<s:url value='/css/datatables.min.css'/>">
    <link rel="stylesheet" href="<s:url value='/css/luna2.css'/>">
</head>
<body>
<nav class="navbar navbar-expand-lg navbar-dark bg-dark">
    <div class="container-fluid">
        <span class="navbar-brand">Broker Noleggio - Ticketing (Fase 4)</span>
    </div>
</nav>

<div class="container-fluid mt-4">
    <div class="row">
        <div class="col-md-3">
            <div class="card">
                <div class="card-header bg-danger text-white">⚠ Avvisi</div>
                <div class="card-body small">
                    <div class="mb-2">
                        <strong>Anti-Rimbalzo:</strong> <span id="antiBounceCount" class="badge bg-warning">0</span>
                    </div>
                    <div class="mb-2">
                        <strong>SLA Risposta:</strong> <span id="slaRispostaCount" class="badge bg-danger">0</span>
                    </div>
                    <div class="mb-2">
                        <strong>SLA Risoluzione:</strong> <span id="slaRisoluzioneCount" class="badge bg-danger">0</span>
                    </div>
                </div>
            </div>
            
            <div class="card mt-3">
                <div class="card-header">Filtri</div>
                <div class="card-body">
                    <div class="mb-2">
                        <label class="form-label">Status</label>
                        <select id="filterStatus" class="form-select form-select-sm" onchange="filterTickets()">
                            <option value="">Tutti</option>
                            <option value="APERTO">Aperto</option>
                            <option value="IN_LAVORAZIONE">In Lavorazione</option>
                            <option value="IN_ATTESA_CLIENTE">In Attesa Cliente</option>
                            <option value="RISOLTO">Risolto</option>
                            <option value="CHIUSO">Chiuso</option>
                        </select>
                    </div>
                    <div class="mb-2">
                        <label class="form-label">Priorità</label>
                        <select id="filterPriorità" class="form-select form-select-sm" onchange="filterTickets()">
                            <option value="">Tutte</option>
                            <option value="BASSA">Bassa</option>
                            <option value="MEDIA">Media</option>
                            <option value="ALTA">Alta</option>
                            <option value="CRITICA">Critica</option>
                        </select>
                    </div>
                </div>
            </div>
        </div>

        <div class="col-md-9">
            <div class="card">
                <div class="card-header d-flex justify-content-between">
                    <h5 class="mb-0">Ticket Support</h5>
                    <button class="btn btn-success btn-sm">+ Nuovo Ticket</button>
                </div>

                <div class="card-body">
                    <table id="ticketsTable" class="table table-striped table-hover">
                        <thead class="table-dark">
                            <tr>
                                <th>Ticket</th>
                                <th>Oggetto</th>
                                <th>Priorità</th>
                                <th>Status</th>
                                <th>Assegnato a</th>
                                <th>Apertura</th>
                                <th>Azioni</th>
                            </tr>
                        </thead>
                        <tbody>
                            <s:iterator value="tickets" status="stat">
                                <tr>
                                    <td><strong><s:property value="numeroTicket"/></strong></td>
                                    <td><s:property value="oggetto"/></td>
                                    <td>
                                        <s:if test="priorita == 'CRITICA'">
                                            <span class="badge bg-danger">CRITICA</span>
                                        </s:if>
                                        <s:elseif test="priorita == 'ALTA'">
                                            <span class="badge bg-warning">ALTA</span>
                                        </s:elseif>
                                        <s:else>
                                            <span class="badge bg-info"><s:property value="priorita"/></span>
                                        </s:else>
                                    </td>
                                    <td><span class="badge bg-secondary"><s:property value="status"/></span></td>
                                    <td><s:property value="operatoreNome"/></td>
                                    <td><s:property value="dataCreazione"/></td>
                                    <td>
                                        <button class="btn btn-sm btn-info" onclick="viewTicket(<s:property value='id'/>)">Dettagli</button>
                                        <button class="btn btn-sm btn-warning" onclick="updateStatus(<s:property value='id'/>)">Modifica</button>
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

<script src="<s:url value='/js/jquery.min.js'/>"></script>
<script src="<s:url value='/js/bootstrap.bundle.min.js'/>"></script>
<script src="<s:url value='/js/datatables.min.js'/>"></script>

<script>
$(document).ready(function() {
    loadAlerts();
    
    $('#ticketsTable').DataTable({
        language: {
            url: '<s:url value="/js/datatable-it.json"/>'
        },
        pageLength: 25
    });
});

function loadAlerts() {
    // Load anti-bounce alerts
    $.ajax({
        url: '<s:url value="/action/noleggio/ticket!antiBounceAlerts.action"/>',
        dataType: 'json',
        success: function(data) {
            $('#antiBounceCount').text(data.count || 0);
        }
    });
    
    // Load SLA exceeded
    $.ajax({
        url: '<s:url value="/action/noleggio/ticket!slaExceeded.action"/>',
        dataType: 'json',
        success: function(data) {
            $('#slaRispostaCount').text(data.slaRispostaCount || 0);
            $('#slaRisoluzioneCount').text(data.slaRisoluzioneCount || 0);
        }
    });
}

function filterTickets() {
    let status = $('#filterStatus').val();
    let priorita = $('#filterPriorità').val();
    
    let table = $('#ticketsTable').DataTable();
    table.search('').draw();
    
    if(status) {
        table.column(3).search(status).draw();
    }
    if(priorita) {
        table.column(2).search(priorita).draw();
    }
}

function viewTicket(id) {
    window.location.href = '<s:url value="/action/noleggio/ticket!view.action"/>?id=' + id;
}

function updateStatus(id) {
    window.location.href = '<s:url value="/action/noleggio/ticket!create.action"/>?id=' + id;
}
</script>

</body>
</html>
