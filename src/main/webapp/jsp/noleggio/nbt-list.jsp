<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html lang="it">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>NBT - Noleggio Breve Termine</title>
    <link rel="stylesheet" href="<s:url value='/css/bootstrap.min.css'/>">
    <link rel="stylesheet" href="<s:url value='/css/datatables.min.css'/>">
    <link rel="stylesheet" href="<s:url value='/css/luna2.css'/>">
</head>
<body>
<nav class="navbar navbar-expand-lg navbar-dark bg-dark">
    <div class="container-fluid">
        <span class="navbar-brand">Broker Noleggio - NBT (Noleggio Breve Termine 1-30 gg)</span>
    </div>
</nav>

<div class="container-fluid mt-4">
    <!-- Revenue Stats -->
    <div class="alert alert-info">
        <div class="row">
            <div class="col-md-3">
                <strong>Richieste Mese:</strong> <span id="requestsMonth">-</span>
            </div>
            <div class="col-md-3">
                <strong>Completati:</strong> <span id="completedMonth">-</span>
            </div>
            <div class="col-md-3">
                <strong>Fatturato NBT:</strong> <span id="revenueMonth">€ -</span>
            </div>
            <div class="col-md-3">
                <strong>Tasso Conversione:</strong> <span id="conversionRate">-</span>
            </div>
        </div>
    </div>

    <div class="card">
        <div class="card-header d-flex justify-content-between">
            <h5 class="mb-0">Noleggi Breve Termine</h5>
            <button class="btn btn-success btn-sm" data-bs-toggle="modal" data-bs-target="#createNBTModal">
                + Nuova Richiesta
            </button>
        </div>

        <ul class="nav nav-tabs" role="tablist">
            <li class="nav-item">
                <a class="nav-link active" data-bs-toggle="tab" href="#richieste">Richieste</a>
            </li>
            <li class="nav-item">
                <a class="nav-link" data-bs-toggle="tab" href="#confermate">Confermate</a>
            </li>
            <li class="nav-item">
                <a class="nav-link" data-bs-toggle="tab" href="#affidate">Affidate</a>
            </li>
            <li class="nav-item">
                <a class="nav-link" data-bs-toggle="tab" href="#completate">Completate</a>
            </li>
            <li class="nav-item">
                <a class="nav-link" data-bs-toggle="tab" href="#alert">Avvisi</a>
            </li>
        </ul>

        <div class="card-body">
            <div class="tab-content">
                <div id="richieste" class="tab-pane fade show active">
                    <table class="table table-striped">
                        <thead class="table-dark">
                            <tr>
                                <th>Pratica</th>
                                <th>Cliente</th>
                                <th>Periodo</th>
                                <th>Giorni</th>
                                <th>Preventivo Valido Fino</th>
                                <th>Azioni</th>
                            </tr>
                        </thead>
                        <tbody id="richiesteTable"></tbody>
                    </table>
                </div>

                <div id="confermate" class="tab-pane fade">
                    <table class="table table-striped">
                        <thead class="table-dark">
                            <tr>
                                <th>Pratica</th>
                                <th>Cliente</th>
                                <th>Veicolo Assegnato</th>
                                <th>Data Consegna</th>
                                <th>Status</th>
                                <th>Azioni</th>
                            </tr>
                        </thead>
                        <tbody id="confermate Table"></tbody>
                    </table>
                </div>

                <div id="affidate" class="tab-pane fade">
                    <table class="table table-striped">
                        <thead class="table-dark">
                            <tr>
                                <th>Pratica</th>
                                <th>Cliente</th>
                                <th>Veicolo</th>
                                <th>Km Iniziali</th>
                                <th>Restituzione Prevista</th>
                                <th>Azioni</th>
                            </tr>
                        </thead>
                        <tbody id="affidateTable"></tbody>
                    </table>
                </div>

                <div id="completate" class="tab-pane fade">
                    <table class="table table-striped">
                        <thead class="table-dark">
                            <tr>
                                <th>Pratica</th>
                                <th>Cliente</th>
                                <th>Veicolo</th>
                                <th>KM Percorsi</th>
                                <th>Totale Pagato</th>
                                <th>Data Completamento</th>
                            </tr>
                        </thead>
                        <tbody id="completateTable"></tbody>
                    </table>
                </div>

                <div id="alert" class="tab-pane fade">
                    <div class="alert alert-warning">
                        <h6>Follow-up 24h</h6>
                        <p>Preventivi scaduti in 24 ore (reminder inviato):</p>
                        <div id="followupList"></div>
                    </div>
                    
                    <div class="alert alert-danger">
                        <h6>Restituzione Imminente</h6>
                        <p>Veicoli da restituire entro 24 ore:</p>
                        <div id="returnsList"></div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>

<!-- Create NBT Modal -->
<div class="modal fade" id="createNBTModal" tabindex="-1">
    <div class="modal-dialog modal-lg">
        <div class="modal-content">
            <div class="modal-header">
                <h5 class="modal-title">Nuova Richiesta NBT</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
            </div>
            <form id="createNBTForm">
                <div class="modal-body">
                    <div class="row">
                        <div class="col-md-6 mb-3">
                            <label class="form-label">Cliente</label>
                            <input type="text" class="form-control" name="ragioneSociale" required>
                        </div>
                        <div class="col-md-6 mb-3">
                            <label class="form-label">Email</label>
                            <input type="email" class="form-control" name="email" required>
                        </div>
                    </div>
                    
                    <div class="row">
                        <div class="col-md-6 mb-3">
                            <label class="form-label">Data Inizio</label>
                            <input type="date" class="form-control" name="dataInizio" required>
                        </div>
                        <div class="col-md-6 mb-3">
                            <label class="form-label">Data Fine</label>
                            <input type="date" class="form-control" name="dataFine" required>
                        </div>
                    </div>
                    
                    <div class="row">
                        <div class="col-md-6 mb-3">
                            <label class="form-label">Marca Preferita</label>
                            <input type="text" class="form-control" name="marcaPreferita">
                        </div>
                        <div class="col-md-6 mb-3">
                            <label class="form-label">Modello Preferito</label>
                            <input type="text" class="form-control" name="modelloPreferito">
                        </div>
                    </div>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Annulla</button>
                    <button type="submit" class="btn btn-primary">Crea Richiesta</button>
                </div>
            </form>
        </div>
    </div>
</div>

<script src="<s:url value='/js/jquery.min.js'/>"></script>
<script src="<s:url value='/js/bootstrap.bundle.min.js'/>"></script>
<script src="<s:url value='/js/datatables.min.js'/>"></script>

<script>
$(document).ready(function() {
    loadDashboard();
    loadNBT();
    
    $('#createNBTForm').on('submit', function(e) {
        e.preventDefault();
        $.ajax({
            type: 'POST',
            url: '<s:url value="/action/noleggio/nbt!save.action"/>',
            data: $(this).serialize(),
            success: function() {
                location.reload();
            }
        });
    });
});

function loadDashboard() {
    $.ajax({
        url: '<s:url value="/action/noleggio/nbt!dashboard.action"/>',
        dataType: 'json',
        success: function(data) {
            if(data.statusData) {
                $('#requestsMonth').text(data.statusData.RICHIESTA || 0);
                $('#completedMonth').text(data.completedCount || 0);
                $('#revenueMonth').text('€ ' + (data.totalRevenue || 0).toLocaleString('it-IT', {minimumFractionDigits: 2}));
                let conversion = data.completedCount && data.statusData.RICHIESTA 
                    ? ((data.completedCount / (data.completedCount + (data.statusData.RICHIESTA || 0))) * 100).toFixed(1)
                    : '0';
                $('#conversionRate').text(conversion + '%');
            }
        }
    });
}

function loadNBT() {
    $.ajax({
        url: '<s:url value="/action/noleggio/nbt!list.action"/>',
        dataType: 'json',
        success: function(data) {
            if(data.nbts) {
                // Populate tables based on status
                data.nbts.forEach(function(nbt) {
                    let row = `
                        <tr>
                            <td>${nbt.numeroPratica}</td>
                            <td>${nbt.ragioneSociale}</td>
                            <td>${nbt.dataInizio} - ${nbt.dataFine}</td>
                            <td>${nbt.numeroGiorni}</td>
                            <td>${nbt.preventivoValidoFino}</td>
                            <td>
                                <button class="btn btn-sm btn-info" onclick="viewNBT(${nbt.id})">Dettagli</button>
                                <button class="btn btn-sm btn-warning" onclick="editNBT(${nbt.id})">Modifica</button>
                            </td>
                        </tr>
                    `;
                    
                    if(nbt.status === 'RICHIESTA') {
                        $('#richiesteTable').append(row);
                    }
                });
            }
        }
    });
    
    // Load follow-up alerts
    $.ajax({
        url: '<s:url value="/action/noleggio/nbt!requireingFollowup.action"/>',
        dataType: 'json',
        success: function(data) {
            if(data.nbts) {
                data.nbts.forEach(function(nbt) {
                    $('#followupList').append(`<div><strong>${nbt.numeroPratica}</strong> - ${nbt.ragioneSociale}</div>`);
                });
            }
        }
    });
    
    // Load return alerts
    $.ajax({
        url: '<s:url value="/action/noleggio/nbt!requireingReturn.action"/>',
        dataType: 'json',
        success: function(data) {
            if(data.nbts) {
                data.nbts.forEach(function(nbt) {
                    $('#returnsList').append(`<div><strong>${nbt.numeroPratica}</strong> - ${nbt.targetaVeicoloAssegnato}</div>`);
                });
            }
        }
    });
}

function viewNBT(id) {
    window.location.href = '<s:url value="/action/noleggio/nbt!view.action"/>?id=' + id;
}

function editNBT(id) {
    window.location.href = '<s:url value="/action/noleggio/nbt!create.action"/>?id=' + id;
}
</script>

</body>
</html>
