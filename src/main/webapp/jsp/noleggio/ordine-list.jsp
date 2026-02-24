<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html lang="it">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Ordini - Broker Noleggio</title>
    <link rel="stylesheet" href="<s:url value='/css/bootstrap.min.css'/>">
    <link rel="stylesheet" href="<s:url value='/css/datatables.min.css'/>">
    <link rel="stylesheet" href="<s:url value='/css/luna2.css'/>">
</head>
<body>
<nav class="navbar navbar-expand-lg navbar-dark bg-dark">
    <div class="container-fluid">
        <span class="navbar-brand">Broker Noleggio - Ordini (Fase 3)</span>
    </div>
</nav>

<div class="container-fluid mt-4">
    <div class="card">
        <div class="card-header d-flex justify-content-between align-items-center bg-primary text-white">
            <h5 class="mb-0">Ordini Attivi</h5>
            <div>
                <span class="badge bg-warning" id="careCallCount">Care Call:</span>
                <span class="badge bg-danger" id="delayedCount">Ritardati:</span>
            </div>
        </div>

        <div class="card-body">
            <ul class="nav nav-tabs mb-3" role="tablist">
                <li class="nav-item">
                    <a class="nav-link active" data-bs-toggle="tab" href="#ordiniCreated">Creati</a>
                </li>
                <li class="nav-item">
                    <a class="nav-link" data-bs-toggle="tab" href="#ordiniWorking">In Lavorazione</a>
                </li>
                <li class="nav-item">
                    <a class="nav-link" data-bs-toggle="tab" href="#ordiniConfirmed">Confermati</a>
                </li>
                <li class="nav-item">
                    <a class="nav-link" data-bs-toggle="tab" href="#ordiniDelivered">Consegnati</a>
                </li>
            </ul>

            <div class="tab-content">
                <div id="ordiniCreated" class="tab-pane fade show active">
                    <table class="table table-striped table-hover">
                        <thead class="table-dark">
                            <tr>
                                <th>Ordine</th>
                                <th>Cliente</th>
                                <th>Targa</th>
                                <th>Status</th>
                                <th>ETA Consegna</th>
                                <th>Azioni</th>
                            </tr>
                        </thead>
                        <tbody id="ordiniCreatedBody">
                        </tbody>
                    </table>
                </div>

                <div id="ordiniWorking" class="tab-pane fade">
                    <table class="table table-striped table-hover">
                        <thead class="table-dark">
                            <tr>
                                <th>Ordine</th>
                                <th>Cliente</th>
                                <th>Targa</th>
                                <th>Progress</th>
                                <th>Azioni</th>
                            </tr>
                        </thead>
                        <tbody id="ordiniWorkingBody">
                        </tbody>
                    </table>
                </div>

                <div id="ordiniConfirmed" class="tab-pane fade">
                    <table class="table table-striped table-hover">
                        <thead class="table-dark">
                            <tr>
                                <th>Ordine</th>
                                <th>Cliente</th>
                                <th>Targa</th>
                                <th>Data Consegna</th>
                                <th>Giorni Rimanenti</th>
                                <th>Azioni</th>
                            </tr>
                        </thead>
                        <tbody id="ordiniConfirmedBody">
                        </tbody>
                    </table>
                </div>

                <div id="ordiniDelivered" class="tab-pane fade">
                    <table class="table table-striped table-hover">
                        <thead class="table-dark">
                            <tr>
                                <th>Ordine</th>
                                <th>Cliente</th>
                                <th>Targa</th>
                                <th>Data Consegna</th>
                                <th>Giorni Dall'Ultima Care Call</th>
                                <th>Azioni</th>
                            </tr>
                        </thead>
                        <tbody id="ordiniDeliveredBody">
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    </div>

    <!-- Care Call Modal -->
    <div class="modal fade" id="careCallModal" tabindex="-1">
        <div class="modal-dialog">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title">Registra Care Call</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                </div>
                <form id="careCallForm">
                    <div class="modal-body">
                        <div class="mb-3">
                            <label class="form-label">Note</label>
                            <textarea class="form-control" id="noteCarecall" name="note" rows="5" required></textarea>
                        </div>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Annulla</button>
                        <button type="submit" class="btn btn-primary">Registra</button>
                    </div>
                </form>
            </div>
        </div>
    </div>
</div>

<script src="<s:url value='/js/jquery.min.js'/>"></script>
<script src="<s:url value='/js/bootstrap.bundle.min.js'/>"></script>

<script>
let currentOrdineId;

$(document).ready(function() {
    loadOrders();
    checkCareCallStatus();
});

function loadOrders() {
    $.ajax({
        url: '<s:url value="/action/noleggio/ordine!list.action"/>',
        dataType: 'json',
        success: function(data) {
            if(data.ordini) {
                data.ordini.forEach(function(ordine) {
                    let row = `
                        <tr>
                            <td>${ordine.numeroOrdine}</td>
                            <td>${ordine.clienteName}</td>
                            <td>${ordine.targa}</td>
                            <td><span class="badge bg-info">${ordine.status}</span></td>
                            <td>${ordine.etaConsegna}</td>
                            <td>
                                <button class="btn btn-sm btn-info" onclick="viewOrdine(${ordine.id})">Dettagli</button>
                                <button class="btn btn-sm btn-warning" onclick="openCareCall(${ordine.id})">Care Call</button>
                            </td>
                        </tr>
                    `;
                    // Append to appropriate tab based on status
                    if(ordine.status === 'ORDINE_CREATO') {
                        $('#ordiniCreatedBody').append(row);
                    } else if(ordine.status === 'IN_LAVORAZIONE') {
                        $('#ordiniWorkingBody').append(row);
                    } else if(ordine.status === 'CONFERMATO') {
                        $('#ordiniConfirmedBody').append(row);
                    } else if(ordine.status === 'CONSEGNATO') {
                        $('#ordiniDeliveredBody').append(row);
                    }
                });
            }
        }
    });
}

function checkCareCallStatus() {
    $.ajax({
        url: '<s:url value="/action/noleggio/ordine!requireingCareCall.action"/>',
        dataType: 'json',
        success: function(data) {
            $('#careCallCount').text('Care Call: ' + (data.count || 0));
        }
    });
    
    $.ajax({
        url: '<s:url value="/action/noleggio/ordine!criticallyDelayed.action"/>',
        dataType: 'json',
        success: function(data) {
            $('#delayedCount').text('Ritardati: ' + (data.count || 0));
        }
    });
}

function openCareCall(ordineId) {
    currentOrdineId = ordineId;
    const careCallModal = new bootstrap.Modal(document.getElementById('careCallModal'));
    careCallModal.show();
}

function viewOrdine(id) {
    window.location.href = '<s:url value="/action/noleggio/ordine!view.action"/>?id=' + id;
}

$('#careCallForm').on('submit', function(e) {
    e.preventDefault();
    $.ajax({
        type: 'POST',
        url: '<s:url value="/action/noleggio/ordine!registerCareCall.action"/>',
        data: {
            id: currentOrdineId,
            note: $('#noteCarecall').val()
        },
        dataType: 'json',
        success: function(response) {
            if(response.success) {
                alert('Care call registrata');
                location.reload();
            }
        }
    });
});
</script>

</body>
</html>
