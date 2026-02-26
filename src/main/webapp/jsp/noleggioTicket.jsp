<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="/struts-tags" prefix="s" %>

<!DOCTYPE html>
<html lang="it">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Ticket Assistenza - Noleggio - Luna2</title>
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
                <h1 class="h3"><i class="bi bi-life-preserver me-2"></i>Gestione Ticket - Post-Vendita (Phase 4)</h1>
                <button class="btn btn-primary" data-bs-toggle="modal" data-bs-target="#ticketModal" onclick="newTicket()">
                    <i class="bi bi-plus-circle me-1"></i>Nuovo Ticket
                </button>
            </div>

            <!-- Avvisi Critici -->
            <div class="alert alert-danger alert-dismissible fade show" id="antiBounceAlert" style="display:none;" role="alert">
                <i class="bi bi-exclamation-octagon-fill me-2"></i>
                <strong>Anti-Rimbalzo Attivi!</strong> <span id="antiBounceCount"></span> ticket hanno finestra di 48-72h
                <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
            </div>
            <div class="alert alert-warning alert-dismissible fade show" id="slaAlert" style="display:none;" role="alert">
                <i class="bi bi-exclamation-triangle-fill me-2"></i>
                <strong>SLA Ecceduti!</strong> <span id="slaCount"></span> ticket hanno SLA scaduto
                <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
            </div>

            <!-- Filtri -->
            <div class="card mb-3 shadow-sm">
                <div class="card-body">
                    <form class="row g-3">
                        <div class="col-md-3">
                            <label class="form-label">Stato</label>
                            <select class="form-select" id="statusFiltro" onchange="loadTickets()">
                                <option value="">Tutti</option>
                                <option value="APERTO">Aperto</option>
                                <option value="IN_LAVORAZIONE">In Lavorazione</option>
                                <option value="RISOLTO">Risolto</option>
                                <option value="CHIUSO">Chiuso</option>
                            </select>
                        </div>
                        <div class="col-md-2">
                            <label class="form-label">Priorità</label>
                            <select class="form-select" id="prioritaFiltro" onchange="loadTickets()">
                                <option value="">Tutte</option>
                                <option value="BASSA">Bassa</option>
                                <option value="MEDIA">Media</option>
                                <option value="ALTA">Alta</option>
                                <option value="CRITICA">Critica</option>
                            </select>
                        </div>
                        <div class="col-md-4">
                            <label class="form-label">Ricerca</label>
                            <input type="text" class="form-control" id="searchTerm" placeholder="Numero ticket, oggetto...">
                        </div>
                        <div class="col-md-3">
                            <label class="form-label">&nbsp;</label>
                            <button type="button" class="btn btn-success w-100" onclick="loadTickets()">
                                <i class="bi bi-search me-1"></i>Cerca
                            </button>
                        </div>
                    </form>
                </div>
            </div>

            <!-- Tabella Ticket -->
            <div class="card shadow-sm">
                <div class="card-header bg-light">
                    <h5 class="mb-0"><i class="bi bi-list-ul me-2"></i>Elenco Ticket Assistenza</h5>
                </div>
                <div class="card-body">
                    <table id="ticketsTable" class="table table-striped table-hover">
                        <thead class="table-light">
                            <tr>
                                <th>Numero</th>
                                <th>Oggetto</th>
                                <th>Priorità</th>
                                <th>Stato</th>
                                <th>Assegnato</th>
                                <th>Creazione</th>
                                <th class="text-end">Azioni</th>
                            </tr>
                        </thead>
                        <tbody></tbody>
                    </table>
                </div>
            </div>
        </div>
    </div>

<!-- Modal Ticket -->
<div class="modal fade" id="ticketModal" tabindex="-1">
    <div class="modal-dialog modal-lg">
        <div class="modal-content">
            <div class="modal-header">
                <h5 class="modal-title">Dettagli Ticket</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
            </div>
            <div class="modal-body">
                <div id="ticketDetails"></div>
            </div>
            <div class="modal-footer">
                <button class="btn btn-warning" onclick="reopenTicket()">Riapri</button>
                <button class="btn btn-danger" onclick="escalateTicket()">Escalation</button>
                <button class="btn btn-success" onclick="closeTicket()">Chiudi</button>
                <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Chiudi</button>
            </div>
        </div>
    </div>
</div>

<script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
<script src="https://cdn.datatables.net/1.13.7/js/jquery.dataTables.min.js"></script>
<script src="https://cdn.datatables.net/1.13.7/js/dataTables.bootstrap5.min.js"></script>

<script>
$(document).ready(function() {
    loadTickets();
    checkAlerts();
});

let currentTicketId = null;

function loadTickets() {
    $.ajax({
        url: '<s:url action="noleggioTicketAction_list" />',
        type: 'GET',
        data: { 
            statusFiltro: $('#statusFiltro').val(),
            prioritaFiltro: $('#prioritaFiltro').val(),
            searchTerm: $('#searchTerm').val()
        },
        dataType: 'json',
        success: function(data) {
            let html = '';
            $.each(data, function() {
                let priorityBadge = 'bg-info';
                if (this.priorita == 'ALTA') priorityBadge = 'bg-warning';
                if (this.priorita == 'CRITICA') priorityBadge = 'bg-danger';
                
                html += '<tr>';
                html += '<td><strong>' + this.numeroTicket + '</strong></td>';
                html += '<td>' + this.oggetto + '</td>';
                html += '<td><span class="badge ' + priorityBadge + '">' + this.priorita + '</span></td>';
                html += '<td>' + this.status + '</td>';
                html += '<td>' + (this.operatoreAssegnatoId || '-') + '</td>';
                html += '<td>' + this.dataCreazione + '</td>';
                html += '<td>';
                html += '<button class="btn btn-sm btn-info" onclick="viewTicket(' + this.id + ')">Visualizza</button>';
                html += '</td>';
                html += '</tr>';
            });
            $('#ticketsTable tbody').html(html);
        }
    });
}

function checkAlerts() {
    // Check anti-bounce
    $.ajax({
        url: '<s:url action="noleggioTicketAction_antiBounceAlerts" />',
        type: 'GET',
        dataType: 'json',
        success: function(data) {
            if (data.count > 0) {
                $('#antiBounceCount').text(data.count);
                $('#antiBounceAlert').show();
            }
        }
    });

    // Check SLA exceeded
    $.ajax({
        url: '<s:url action="noleggioTicketAction_slaExceeded" />',
        type: 'GET',
        dataType: 'json',
        success: function(data) {
            let slaTotal = (data.slaRispostaCount || 0) + (data.slaRisoluzioneCount || 0);
            if (slaTotal > 0) {
                $('#slaCount').text(slaTotal);
                $('#slaAlert').show();
            }
        }
    });
}

function viewTicket(id) {
    currentTicketId = id;
    $.ajax({
        url: '<s:url action="noleggioTicketAction_view" />',
        type: 'GET',
        data: { id: id },
        dataType: 'json',
        success: function(data) {
            let html = '<div class="row">';
            html += '<div class="col-md-6"><strong>Numero:</strong> ' + data.numeroTicket + '</div>';
            html += '<div class="col-md-6"><strong>Priorità:</strong> ' + data.priorita + '</div>';
            html += '</div>';
            html += '<p><strong>Oggetto:</strong> ' + data.oggetto + '</p>';
            html += '<p><strong>Descrizione:</strong><br>' + data.descrizione + '</p>';
            html += '<p><strong>Stato:</strong> ' + data.status + '</p>';
            html += '<p><strong>Assegnato a:</strong> ' + (data.operatoreAssegnatoId || 'Non assegnato') + '</p>';
            if (data.soluzioneAdottata) {
                html += '<p><strong>Soluzione:</strong><br>' + data.soluzioneAdottata + '</p>';
            }
            $('#ticketDetails').html(html);
            new bootstrap.Modal(document.getElementById('ticketModal')).show();
        }
    });
}

function reopenTicket() {
    let motivo = prompt('Motivo riapertura:');
    if (motivo && currentTicketId) {
        $.ajax({
            url: '<s:url action="noleggioTicketAction_reopen" />',
            type: 'POST',
            data: { id: currentTicketId, motivo: motivo },
            success: function() {
                alert('Ticket riaperto');
                loadTickets();
                checkAlerts();
            }
        });
    }
}

function closeTicket() {
    let soluzione = prompt('Soluzione adottata:');
    if (soluzione && currentTicketId) {
        $.ajax({
            url: '<s:url action="noleggioTicketAction_close" />',
            type: 'POST',
            data: { id: currentTicketId, soluzioneAdottata: soluzione },
            success: function() {
                alert('Ticket chiuso');
                loadTickets();
            }
        });
    }
}

function escalateTicket() {
    let motivo = prompt('Motivo escalation:');
    if (motivo && currentTicketId) {
        $.ajax({
            url: '<s:url action="noleggioTicketAction_escalate" />',
            type: 'POST',
            data: { id: currentTicketId, motivo: motivo },
            success: function() {
                alert('Ticket escalato');
                loadTickets();
            }
        });
    }
}
</script>
</body>
</html>
