<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="/struts-tags" prefix="s" %>

<!DOCTYPE html>
<html lang="it">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Scadenzario - Manutenzione e Rinnovi - Luna2</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.10.0/font/bootstrap-icons.css">
    <link href="https://cdn.datatables.net/1.13.7/css/dataTables.bootstrap5.min.css" rel="stylesheet">
</head>
<body>
    <%@ include file="../WEB-INF/jsp/includes/sidebar.jsp" %>

    <div class="col-md-10 content-wrapper p-4">
        <div class="container-fluid">
            <!-- Header -->
            <div class="d-flex justify-content-between align-items-center mb-4">
                <h1 class="h3"><i class="bi bi-calendar-check me-2"></i>Scadenzario - Manutenzione e Rinnovi (Phase 5)</h1>
                <button class="btn btn-primary" onclick="runScheduleAutomation()">
                    <i class="bi bi-gear me-1"></i>Esegui Automazione
                </button>
            </div>

            <!-- Summary Cards -->
            <div class="row mb-4" id="summaryCards">
                <div class="col-md-3">
                    <div class="card border-danger">
                        <div class="card-body text-center">
                            <h6 class="card-title text-muted">Rinnovi (4-6 mesi)</h6>
                            <h3 id="renewalsCount" class="text-danger">-</h3>
                        </div>
                    </div>
                </div>
                <div class="col-md-3">
                    <div class="card border-warning">
                        <div class="card-body text-center">
                            <h6 class="card-title text-muted">Verifiche KM</h6>
                            <h3 id="kmVerifCount" class="text-warning">-</h3>
                        </div>
                    </div>
                </div>
                <div class="col-md-3">
                    <div class="card border-info">
                        <div class="card-body text-center">
                            <h6 class="card-title text-muted">Revisioni (30gg)</h6>
                            <h3 id="revisioniCount" class="text-info">-</h3>
                        </div>
                    </div>
                </div>
                <div class="col-md-3">
            <div class="card text-center">
                <div class="card-body">
                    <h6>Tagliandi 15gg</h6>
                    <h4 id="tagliandiCount" class="text-info">-</h4>
                </div>
            </div>
        </div>
        <div class="col-md-2">
            <div class="card text-center">
                <div class="card-body">
                    <h6>Patenti Scad</h6>
                    <h4 id="patentiCount" class="text-orange">-</h4>
                </div>
            </div>
        </div>
        <div class="col-md-2">
            <div class="card text-center">
                <div class="card-body">
                    <h6>Assicurazioni</h6>
                    <h4 id="assicurazioniCount" class="text-orange">-</h4>
                </div>
            </div>
        </div>
    </div>

    <!-- Tabs -->
    <ul class="nav nav-tabs mb-3" id="scheduleTab" role="tablist">
        <li class="nav-item"><a class="nav-link active" data-bs-toggle="tab" href="#renewalsTab">Rinnovi</a></li>
        <li class="nav-item"><a class="nav-link" data-bs-toggle="tab" href="#kmTab">Verifiche KM</a></li>
        <li class="nav-item"><a class="nav-link" data-bs-toggle="tab" href="#revisioniTab">Revisioni</a></li>
        <li class="nav-item"><a class="nav-link" data-bs-toggle="tab" href="#tagliandiTab">Tagliandi</a></li>
        <li class="nav-item"><a class="nav-link" data-bs-toggle="tab" href="#calendarTab">Calendario</a></li>
    </ul>

    <div class="tab-content">
        <!-- Rinnovi Tab -->
        <div id="renewalsTab" class="tab-pane fade show active">
            <table class="table table-striped">
                <thead class="table-dark">
                    <tr><th>Numero Contratto</th><th>Targa</th><th>Data Fine</th><th>Giorni Rimasti</th><th>Azioni</th></tr>
                </thead>
                <tbody id="renewalsBody"></tbody>
            </table>
        </div>

        <!-- Verifiche KM Tab -->
        <div id="kmTab" class="tab-pane fade">
            <table class="table table-striped">
                <thead class="table-dark">
                    <tr><th>Numero Contratto</th><th>Targa</th><th>KM Attuali</th><th>KM Limite</th><th>KM Ecceduti</th><th>Azioni</th></tr>
                </thead>
                <tbody id="kmBody"></tbody>
            </table>
        </div>

        <!-- Revisioni Tab -->
        <div id="revisioniTab" class="tab-pane fade">
            <table class="table table-striped">
                <thead class="table-dark">
                    <tr><th>Numero Contratto</th><th>Targa</th><th>Prossima Revisione</th><th>KM Prossima</th><th>Azioni</th></tr>
                </thead>
                <tbody id="revisioniBody"></tbody>
            </table>
        </div>

        <!-- Tagliandi Tab -->
        <div id="tagliandiTab" class="tab-pane fade">
            <table class="table table-striped">
                <thead class="table-dark">
                    <tr><th>Numero Contratto</th><th>Targa</th><th>Prossimo Tagliando</th><th>KM Prossimo</th><th>Azioni</th></tr>
                </thead>
                <tbody id="tagliandiBody"></tbody>
            </table>
        </div>

        <!-- Calendario Tab -->
        <div id="calendarTab" class="tab-pane fade">
            <button class="btn btn-sm btn-primary mb-2" onclick="showCalendarModal()">Crea Evento Manuale</button>
            <table class="table table-striped">
                <thead class="table-dark">
                    <tr><th>Titolo</th><th>Data</th><th>Tipo</th><th>Utente Assegnato</th><th>Sincronizzato</th></tr>
                </thead>
                <tbody id="calendarBody"></tbody>
            </table>
        </div>
    </div>
</div>

<!-- Modal for Manual Calendar Event -->
<div class="modal fade" id="calendarModal" tabindex="-1">
    <div class="modal-dialog">
        <div class="modal-content">
            <div class="modal-header">
                <h5 class="modal-title">Crea Evento Calendario</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
            </div>
            <form id="eventForm">
                <div class="modal-body">
                    <div class="mb-3">
                        <label class="form-label">Contratto *</label>
                        <select class="form-control" id="contrattoId" name="contrattoId" required>
                            <option value="">-- Seleziona contratto --</option>
                        </select>
                    </div>
                    <div class="mb-3">
                        <label class="form-label">Titolo *</label>
                        <input type="text" class="form-control" id="eventTitle" name="eventTitle" required>
                    </div>
                    <div class="mb-3">
                        <label class="form-label">Data *</label>
                        <input type="date" class="form-control" id="eventDate" name="eventDate" required>
                    </div>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Chiudi</button>
                    <button type="submit" class="btn btn-primary">Crea Evento</button>
                </div>
            </form>
        </div>
    </div>
</div>

<script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
<script src="https://cdn.datatables.net/1.13.7/js/jquery.dataTables.min.js"></script>

<script>
$(document).ready(function() {
    loadSummary();
    loadRenewals();
    loadKmVerif();
    loadRevisioni();
    loadTagliandi();
    loadCalendarEvents();
});

function loadSummary() {
    $.ajax({
        url: '<s:url action="noleggioScadenzarioAction_summary" />',
        type: 'GET',
        dataType: 'json',
        success: function(data) {
            $('#renewalsCount').text(data.summary.expiringRenewals || 0);
            $('#kmVerifCount').text(data.summary.kmVerifications || 0);
            $('#revisioniCount').text(data.summary.upcomingRevisioni || 0);
            $('#tagliandiCount').text(data.summary.upcomingTagliandi || 0);
            $('#patentiCount').text(data.summary.expiringLicenses || 0);
            $('#assicurazioniCount').text(data.summary.expiringInsurances || 0);
        }
    });
}

function loadRenewals() {
    $.ajax({
        url: '<s:url action="noleggioScadenzarioAction_expiringRenewals" />',
        type: 'GET',
        dataType: 'json',
        success: function(data) {
            let html = '';
            $.each(data.contratti || [], function() {
                html += '<tr><td>' + this.numeroContratto + '</td><td>' + this.targa + '</td><td>' + this.dataFine + '</td><td> - </td><td><button class="btn btn-sm btn-primary" onclick="createCalendarEvent(' + this.id + ')">Crea Evento</button></td></tr>';
            });
            $('#renewalsBody').html(html || '<tr><td colspan="5">Nessun rinnovo in scadenza</td></tr>');
        }
    });
}

function loadKmVerif() {
    $.ajax({
        url: '<s:url action="noleggioScadenzarioAction_kmVerifications" />',
        type: 'GET',
        dataType: 'json',
        success: function(data) {
            let html = '';
            $.each(data.contratti || [], function() {
                html += '<tr><td>' + this.numeroContratto + '</td><td>' + this.targa + '</td><td>' + (this.kmAttuali || 0) + '</td><td>' + (this.kmLimite || 0) + '</td><td>' + (this.kmEcceduti || 0) + '</td><td><button class="btn btn-sm btn-warning">Verificare</button></td></tr>';
            });
            $('#kmBody').html(html || '<tr><td colspan="6">Nessuna verifica KM</td></tr>');
        }
    });
}

function loadRevisioni() {
    $.ajax({
        url: '<s:url action="noleggioScadenzarioAction_upcomingRevisioni" />',
        type: 'GET',
        dataType: 'json',
        success: function(data) {
            let html = '';
            $.each(data.contratti || [], function() {
                html += '<tr><td>' + this.numeroContratto + '</td><td>' + this.targa + '</td><td>' + this.dataProssimaRevisione + '</td><td>' + (this.kmProssimaRevisione || 0) + '</td><td><button class="btn btn-sm btn-info">Pianifica</button></td></tr>';
            });
            $('#revisioniBody').html(html || '<tr><td colspan="5">Nessuna revisione in scadenza</td></tr>');
        }
    });
}

function loadTagliandi() {
    $.ajax({
        url: '<s:url action="noleggioScadenzarioAction_upcomingTagliandi" />',
        type: 'GET',
        dataType: 'json',
        success: function(data) {
            let html = '';
            $.each(data.contratti || [], function() {
                html += '<tr><td>' + this.numeroContratto + '</td><td>' + this.targa + '</td><td>' + this.dataProssimoTagliando + '</td><td>' + (this.kmProssimoTagliando || 0) + '</td><td><button class="btn btn-sm btn-info">Pianifica</button></td></tr>';
            });
            $('#tagliandiBody').html(html || '<tr><td colspan="5">Nessun tagliando in scadenza</td></tr>');
        }
    });
}

function loadCalendarEvents() {
    $.ajax({
        url: '<s:url action="noleggioScadenzarioAction_calendarEvents" />',
        type: 'GET',
        dataType: 'json',
        success: function(data) {
            let html = '';
            $.each(data.events || [], function() {
                html += '<tr><td>' + this.titolo + '</td><td>' + this.data + '</td><td>' + this.sourceType + '</td><td>' + (this.utenteAssegnatoId || '-') + '</td><td>' + (this.sincronizzatoGoogle ? 'Google' : '-') + '</td></tr>';
            });
            $('#calendarBody').html(html || '<tr><td colspan="5">Nessun evento</td></tr>');
        }
    });
}

function runScheduleAutomation() {
    $.ajax({
        url: '<s:url action="noleggioScadenzarioAction_runAutomation" />',
        type: 'GET',
        dataType: 'json',
        success: function(data) {
            alert(data.message || 'Automazione completata');
            loadSummary();
            loadRenewals();
            loadKmVerif();
            loadRevisioni();
            loadTagliandi();
            loadCalendarEvents();
        }
    });
}

function showCalendarModal() {
    new bootstrap.Modal(document.getElementById('calendarModal')).show();
}

function createCalendarEvent(id) {
    $('#contrattoId').val(id);
    showCalendarModal();
}

$('#eventForm').submit(function(e) {
    e.preventDefault();
    $.ajax({
        url: '<s:url action="noleggioScadenzarioAction_createEvent" />',
        type: 'POST',
        data: $(this).serialize(),
        dataType: 'json',
        success: function(data) {
            alert('Evento creato');
            bootstrap.Modal.getInstance(document.getElementById('calendarModal')).hide();
            loadCalendarEvents();
        }
    });
});
</script>
</body>
</html>
