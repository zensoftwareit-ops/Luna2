<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="/struts-tags" prefix="s" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<!DOCTYPE html>
<html lang="it">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Lead - Noleggio Auto</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.datatables.net/1.13.7/css/dataTables.bootstrap5.min.css" rel="stylesheet">
    <style>
        .fase-badge { font-weight: bold; padding: 5px 10px; border-radius: 3px; }
        .fase-preventivazione { background: #e3f2fd; color: #1976d2; }
        .fase-istruttoria { background: #fff3e0; color: #f57c00; }
        .fase-ordine { background: #e8f5e9; color: #388e3c; }
        .fase-post-vendita { background: #f3e5f5; color: #7b1fa2; }
    </style>
</head>
<body>
<div class="container-fluid mt-4">
    <div class="row mb-4">
        <div class="col-md-8">
            <h1>Gestione Lead - Noleggio Auto</h1>
        </div>
        <div class="col-md-4 text-end">
            <button class="btn btn-primary" data-bs-toggle="modal" data-bs-target="#leadModal" onclick="newLead()">
                <i class="bi bi-plus-circle"></i> Nuovo Lead
            </button>
        </div>
    </div>

    <!-- Filtri -->
    <div class="card mb-3">
        <div class="card-body">
            <form id="filterForm" class="row g-3">
                <div class="col-md-4">
                    <label class="form-label">Fase</label>
                    <select class="form-control" id="fasceFiltro" name="fasceFiltro" onchange="loadLeads()">
                        <option value="">-- Tutte le fasi --</option>
                        <option value="PREVENTIVAZIONE">Preventivazione</option>
                        <option value="ISTRUTTORIA">Istruttoria</option>
                        <option value="ORDINE">Ordine</option>
                        <option value="POST_VENDITA">Post-Vendita</option>
                    </select>
                </div>
                <div class="col-md-5">
                    <label class="form-label">Ricerca</label>
                    <input type="text" class="form-control" id="searchTerm" placeholder="Cliente, email, telefono...">
                </div>
                <div class="col-md-3">
                    <label class="form-label">&nbsp;</label>
                    <button type="button" class="btn btn-success w-100" onclick="loadLeads()">
                        <i class="bi bi-search"></i> Cerca
                    </button>
                </div>
            </form>
        </div>
    </div>

    <!-- Dashboard Stats -->
    <div class="row mb-3" id="dashboardStats">
        <div class="col-md-3">
            <div class="card text-center">
                <div class="card-body">
                    <h5 class="card-title">Preventivazione</h5>
                    <h3 id="stat-preventivazione" class="text-primary">-</h3>
                </div>
            </div>
        </div>
        <div class="col-md-3">
            <div class="card text-center">
                <div class="card-body">
                    <h5 class="card-title">Istruttoria</h5>
                    <h3 id="stat-istruttoria" class="text-warning">-</h3>
                </div>
            </div>
        </div>
        <div class="col-md-3">
            <div class="card text-center">
                <div class="card-body">
                    <h5 class="card-title">Ordine</h5>
                    <h3 id="stat-ordine" class="text-success">-</h3>
                </div>
            </div>
        </div>
        <div class="col-md-3">
            <div class="card text-center">
                <div class="card-body">
                    <h5 class="card-title">Post-Vendita</h5>
                    <h3 id="stat-post-vendita" class="text-info">-</h3>
                </div>
            </div>
        </div>
    </div>

    <!-- Tabella Lead -->
    <div class="card">
        <div class="card-body">
            <table id="leadsTable" class="table table-striped table-hover">
                <thead class="table-dark">
                    <tr>
                        <th>Numero Pratica</th>
                        <th>Cliente</th>
                        <th>Email</th>
                        <th>Telefono</th>
                        <th>Fase</th>
                        <th>Assegnato a</th>
                        <th>Data Creazione</th>
                        <th>Azioni</th>
                    </tr>
                </thead>
                <tbody>
                </tbody>
            </table>
        </div>
    </div>
</div>

<!-- Modal Lead -->
<div class="modal fade" id="leadModal" tabindex="-1">
    <div class="modal-dialog modal-lg">
        <div class="modal-content">
            <div class="modal-header">
                <h5 class="modal-title" id="modalTitle">Nuovo Lead</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
            </div>
            <form id="leadForm">
                <div class="modal-body">
                    <input type="hidden" id="leadId" name="id">
                    
                    <div class="row">
                        <div class="col-md-6">
                            <div class="mb-3">
                                <label class="form-label">Ragione Sociale *</label>
                                <input type="text" class="form-control" id="ragioneSociale" name="ragioneSociale" required>
                            </div>
                        </div>
                        <div class="col-md-6">
                            <div class="mb-3">
                                <label class="form-label">Contatto</label>
                                <input type="text" class="form-control" id="nomeContatto" name="nomeContatto">
                            </div>
                        </div>
                    </div>

                    <div class="row">
                        <div class="col-md-6">
                            <div class="mb-3">
                                <label class="form-label">Email</label>
                                <input type="email" class="form-control" id="email" name="email">
                            </div>
                        </div>
                        <div class="col-md-6">
                            <div class="mb-3">
                                <label class="form-label">Telefono</label>
                                <input type="text" class="form-control" id="telefono" name="telefono">
                            </div>
                        </div>
                    </div>

                    <div class="row">
                        <div class="col-md-6">
                            <div class="mb-3">
                                <label class="form-label">Utilizzo Previsto</label>
                                <input type="text" class="form-control" id="utilizzo" name="utilizzo">
                            </div>
                        </div>
                        <div class="col-md-6">
                            <div class="mb-3">
                                <label class="form-label">KM Annuali Previsti</label>
                                <input type="number" class="form-control" id="kmAnnuali" name="kmAnnuali">
                            </div>
                        </div>
                    </div>

                    <div class="row">
                        <div class="col-md-6">
                            <div class="mb-3">
                                <label class="form-label">Durata Noleggio (mesi)</label>
                                <input type="number" class="form-control" id="durataM esi" name="durataMesi">
                            </div>
                        </div>
                        <div class="col-md-6">
                            <div class="mb-3">
                                <label class="form-label">Budget Massimo</label>
                                <input type="number" step="0.01" class="form-control" id="budgetMax" name="budgetMax">
                            </div>
                        </div>
                    </div>

                    <div class="mb-3">
                        <label class="form-label">Note</label>
                        <textarea class="form-control" id="note" name="note" rows="3"></textarea>
                    </div>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Chiudi</button>
                    <button type="submit" class="btn btn-primary">Salva</button>
                </div>
            </form>
        </div>
    </div>
</div>

<script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
<script src="https://cdn.datatables.net/1.13.7/js/jquery.dataTables.min.js"></script>
<script src="https://cdn.datatables.net/1.13.7/js/dataTables.bootstrap5.min.js"></script>

<script>
$(document).ready(function() {
    loadLeads();
    loadDashboard();
});

function loadLeads() {
    let fase = $('#fasceFiltro').val();
    let search = $('#searchTerm').val();
    
    $.ajax({
        url: '<s:url action="noleggioLeadAction_list" />',
        type: 'GET',
        data: { fasceFiltro: fase, searchTerm: search },
        success: function(data) {
            let html = '';
            $(data).each(function() {
                html += '<tr>';
                html += '<td>' + this.numeroPratica + '</td>';
                html += '<td>' + this.ragioneSociale + '</td>';
                html += '<td>' + this.email + '</td>';
                html += '<td>' + this.telefono + '</td>';
                html += '<td><span class="fase-badge fase-' + this.fase.toLowerCase() + '">' + this.fase + '</span></td>';
                html += '<td>' + (this.utenteAssegnatoId || '-') + '</td>';
                html += '<td><fmt:formatDate value="' + this.dataCreazione + '" pattern="dd/MM/yyyy HH:mm" /></td>';
                html += '<td>';
                html += '<button class="btn btn-sm btn-info" onclick="editLead(' + this.id + ')">Edit</button> ';
                html += '<button class="btn btn-sm btn-danger" onclick="deleteLead(' + this.id + ')">Delete</button>';
                html += '</td>';
                html += '</tr>';
            });
            $('#leadsTable tbody').html(html);
        }
    });
}

function loadDashboard() {
    $.ajax({
        url: '<s:url action="noleggioLeadAction_dashboardData" />',
        type: 'GET',
        dataType: 'json',
        success: function(data) {
            $('#stat-preventivazione').text(data.countPreventivazione || 0);
            $('#stat-istruttoria').text(data.countIstruttoria || 0);
            $('#stat-ordine').text(data.countOrdine || 0);
            $('#stat-post-vendita').text(data.countPostVendita || 0);
        }
    });
}

function newLead() {
    $('#leadId').val('');
    $('#leadForm')[0].reset();
    $('#modalTitle').text('Nuovo Lead');
}

function editLead(id) {
    $.ajax({
        url: '<s:url action="noleggioLeadAction_view" />',
        type: 'GET',
        data: { id: id },
        success: function(data) {
            $('#leadId').val(data.id);
            $('#ragioneSociale').val(data.ragioneSociale);
            $('#nomeContatto').val(data.nomeContatto);
            $('#email').val(data.email);
            $('#telefono').val(data.telefono);
            $('#utilizzo').val(data.utilizzo);
            $('#kmAnnuali').val(data.kmAnnuali);
            $('#durataMesi').val(data.durataMesi);
            $('#budgetMax').val(data.budgetMax);
            $('#note').val(data.note);
            $('#modalTitle').text('Modifica Lead');
            new bootstrap.Modal(document.getElementById('leadModal')).show();
        }
    });
}

function deleteLead(id) {
    if (confirm('Sei sicuro di voler eliminare questo lead?')) {
        $.ajax({
            url: '<s:url action="noleggioLeadAction_delete" />',
            type: 'POST',
            data: { id: id },
            success: function() {
                alert('Lead eliminato');
                loadLeads();
            }
        });
    }
}

$('#leadForm').submit(function(e) {
    e.preventDefault();
    $.ajax({
        url: '<s:url action="noleggioLeadAction_save" />',
        type: 'POST',
        data: $(this).serialize(),
        success: function(data) {
            alert(data.message || 'Salvataggio completato');
            bootstrap.Modal.getInstance(document.getElementById('leadModal')).hide();
            loadLeads();
            loadDashboard();
        },
        error: function() {
            alert('Errore nel salvataggio');
        }
    });
});
</script>
</body>
</html>
