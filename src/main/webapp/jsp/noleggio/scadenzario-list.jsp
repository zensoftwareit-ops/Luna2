<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html lang="it">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Scadenzario - Broker Noleggio</title>
    <link rel="stylesheet" href="<s:url value='/css/bootstrap.min.css'/>">
    <link rel="stylesheet" href="<s:url value='/css/datatables.min.css'/>">
    <link rel="stylesheet" href="<s:url value='/css/luna2.css'/>">
</head>
<body>
<nav class="navbar navbar-expand-lg navbar-dark bg-dark">
    <div class="container-fluid">
        <span class="navbar-brand">Broker Noleggio - Scadenzario (Fase 5)</span>
    </div>
</nav>

<div class="container-fluid mt-4">
    <!-- Summary Alerts -->
    <div class="row">
        <div class="col-md-2">
            <div class="card text-center bg-warning text-dark">
                <div class="card-body">
                    <h6>Rinnovi Prossimi</h6>
                    <h4 id="countRinnovi">-</h4>
                </div>
            </div>
        </div>
        <div class="col-md-2">
            <div class="card text-center bg-info text-white">
                <div class="card-body">
                    <h6>Verifiche KM</h6>
                    <h4 id="countKm">-</h4>
                </div>
            </div>
        </div>
        <div class="col-md-2">
            <div class="card text-center bg-danger text-white">
                <div class="card-body">
                    <h6>Revisioni</h6>
                    <h4 id="countRevisioni">-</h4>
                </div>
            </div>
        </div>
        <div class="col-md-2">
            <div class="card text-center bg-primary text-white">
                <div class="card-body">
                    <h6>Tagliandi</h6>
                    <h4 id="countTagliandi">-</h4>
                </div>
            </div>
        </div>
        <div class="col-md-2">
            <div class="card text-center bg-secondary text-white">
                <div class="card-body">
                    <h6>Patenti Scad.</h6>
                    <h4 id="countPatenti">-</h4>
                </div>
            </div>
        </div>
        <div class="col-md-2">
            <div class="card text-center bg-dark text-white">
                <div class="card-body">
                    <h6>Assicurazioni</h6>
                    <h4 id="countAssicurazioni">-</h4>
                </div>
            </div>
        </div>
    </div>

    <!-- Tabs for different scadenzario types -->
    <div class="card mt-4">
        <div class="card-header">
            <h5 class="mb-0">Timeline Scadenze</h5>
        </div>

        <ul class="nav nav-tabs" role="tablist">
            <li class="nav-item">
                <a class="nav-link active" data-bs-toggle="tab" href="#rinnovi">Rinnovi</a>
            </li>
            <li class="nav-item">
                <a class="nav-link" data-bs-toggle="tab" href="#verifiche">Verifiche KM</a>
            </li>
            <li class="nav-item">
                <a class="nav-link" data-bs-toggle="tab" href="#revisioni">Revisioni</a>
            </li>
            <li class="nav-item">
                <a class="nav-link" data-bs-toggle="tab" href="#tagliandi">Tagliandi</a>
            </li>
            <li class="nav-item">
                <a class="nav-link" data-bs-toggle="tab" href="#calendarioEventi">Calendario Eventi</a>
            </li>
        </ul>

        <div class="card-body">
            <div class="tab-content">
                <!-- Renewals -->
                <div id="rinnovi" class="tab-pane fade show active">
                    <table class="table table-striped">
                        <thead class="table-dark">
                            <tr>
                                <th>Contratto</th>
                                <th>Targa</th>
                                <th>Scadenza</th>
                                <th>Giorni Rimanenti</th>
                                <th>Azioni</th>
                            </tr>
                        </thead>
                        <tbody id="rinnowiTable"></tbody>
                    </table>
                </div>

                <!-- KM Verifications -->
                <div id="verifiche" class="tab-pane fade">
                    <table class="table table-striped">
                        <thead class="table-dark">
                            <tr>
                                <th>Contratto</th>
                                <th>Targa</th>
                                <th>KM Attuali</th>
                                <th>KM Limite</th>
                                <th>Ecceduti</th>
                                <th>Azioni</th>
                            </tr>
                        </thead>
                        <tbody id="verificheTable"></tbody>
                    </table>
                </div>

                <!-- Revisions -->
                <div id="revisioni" class="tab-pane fade">
                    <table class="table table-striped">
                        <thead class="table-dark">
                            <tr>
                                <th>Contratto</th>
                                <th>Targa</th>
                                <th>Prossima Revisione</th>
                                <th>Giorni Rimanenti</th>
                                <th>Azioni</th>
                            </tr>
                        </thead>
                        <tbody id="revisioniTable"></tbody>
                    </table>
                </div>

                <!-- Maintenance -->
                <div id="tagliandi" class="tab-pane fade">
                    <table class="table table-striped">
                        <thead class="table-dark">
                            <tr>
                                <th>Contratto</th>
                                <th>Targa</th>
                                <th>Prossimo Tagliando</th>
                                <th>Giorni Rimanenti</th>
                                <th>Azioni</th>
                            </tr>
                        </thead>
                        <tbody id="tagliandiTable"></tbody>
                    </table>
                </div>

                <!-- Calendar Events -->
                <div id="calendarioEventi" class="tab-pane fade">
                    <button class="btn btn-primary btn-sm mb-3" onclick="runAutomation()">Sincronizza Calendario</button>
                    <table class="table table-striped">
                        <thead class="table-dark">
                            <tr>
                                <th>Evento</th>
                                <th>Data</th>
                                <th>Tipo</th>
                                <th>Contratto</th>
                                <th>Sincronizzato</th>
                            </tr>
                        </thead>
                        <tbody id="calendarioTable"></tbody>
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
    loadSummary();
    loadScadenzario();
});

function loadSummary() {
    $.ajax({
        url: '<s:url value="/action/noleggio/scadenzario!summary.action"/>',
        dataType: 'json',
        success: function(data) {
            if(data.summary) {
                $('#countRinnovi').text(data.summary.expiringRenewals);
                $('#countKm').text(data.summary.kmVerifications);
                $('#countRevisioni').text(data.summary.upcomingRevisioni);
                $('#countTagliandi').text(data.summary.upcomingTagliandi);
                $('#countPatenti').text(data.summary.expiringLicenses);
                $('#countAssicurazioni').text(data.summary.expiringInsurances);
            }
        }
    });
}

function loadScadenzario() {
    // Load renewals
    $.ajax({
        url: '<s:url value="/action/noleggio/scadenzario!expiringRenewals.action"/>',
        dataType: 'json',
        success: function(data) {
            if(data.contratti) {
                data.contratti.forEach(function(c) {
                    let row = `
                        <tr>
                            <td>${c.numeroContratto}</td>
                            <td>${c.targa}</td>
                            <td>${c.dataFine}</td>
                            <td><span class="badge bg-warning">${giorni(c.dataFine)}</span></td>
                            <td><button class="btn btn-sm btn-primary" onclick="viewContratto(${c.id})">Dettagli</button></td>
                        </tr>
                    `;
                    $('#rinnowiTable').append(row);
                });
            }
        }
    });
    
    // Load revisions
    $.ajax({
        url: '<s:url value="/action/noleggio/scadenzario!upcomingRevisioni.action"/>',
        dataType: 'json',
        success: function(data) {
            if(data.contratti) {
                data.contratti.forEach(function(c) {
                    let row = `
                        <tr>
                            <td>${c.numeroContratto}</td>
                            <td>${c.targa}</td>
                            <td>${c.dataProssimaRevisione}</td>
                            <td><span class="badge bg-danger">${giorni(c.dataProssimaRevisione)}</span></td>
                            <td><button class="btn btn-sm btn-primary" onclick="viewContratto(${c.id})">Dettagli</button></td>
                        </tr>
                    `;
                    $('#revisioniTable').append(row);
                });
            }
        }
    });
    
    // Load maintenance
    $.ajax({
        url: '<s:url value="/action/noleggio/scadenzario!upcomingTagliandi.action"/>',
        dataType: 'json',
        success: function(data) {
            if(data.contratti) {
                data.contratti.forEach(function(c) {
                    let row = `
                        <tr>
                            <td>${c.numeroContratto}</td>
                            <td>${c.targa}</td>
                            <td>${c.dataProssimoTagliando}</td>
                            <td><span class="badge bg-info">${giorni(c.dataProssimoTagliando)}</span></td>
                            <td><button class="btn btn-sm btn-primary" onclick="viewContratto(${c.id})">Dettagli</button></td>
                        </tr>
                    `;
                    $('#tagliandiTable').append(row);
                });
            }
        }
    });
}

function runAutomation() {
    $.ajax({
        url: '<s:url value="/action/noleggio/scadenzario!runAutomation.action"/>',
        dataType: 'json',
        success: function(data) {
            if(data.success) {
                alert('Sincronizzazione completata');
                location.reload();
            }
        }
    });
}

function viewContratto(id) {
    window.location.href = '<s:url value="/action/noleggio/contratto!view.action"/>?id=' + id;
}

function giorni(dataString) {
    let data = new Date(dataString);
    let oggi = new Date();
    let diff = data - oggi;
    return Math.ceil(diff / (1000 * 60 * 60 * 24));
}
</script>

</body>
</html>
