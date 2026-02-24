<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="/struts-tags" prefix="s" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<!DOCTYPE html>
<html lang="it">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Ordini - Noleggio Auto</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.datatables.net/1.13.7/css/dataTables.bootstrap5.min.css" rel="stylesheet">
</head>
<body>
<div class="container-fluid mt-4">
    <div class="row mb-4">
        <div class="col-md-8">
            <h1>Gestione Ordini - Noleggio Auto</h1>
        </div>
        <div class="col-md-4 text-end">
            <button class="btn btn-primary" data-bs-toggle="modal" data-bs-target="#ordineModal" onclick="newOrdine()">
                <i class="bi bi-plus-circle"></i> Nuovo Ordine
            </button>
        </div>
    </div>

    <!-- Filtri -->
    <div class="card mb-3">
        <div class="card-body">
            <form class="row g-3">
                <div class="col-md-4">
                    <label class="form-label">Stato</label>
                    <select class="form-control" id="statusFiltro" onchange="loadOrdini()">
                        <option value="">-- Tutti gli stati --</option>
                        <option value="ORDINE_CREATO">Ordine Creato</option>
                        <option value="IN_LAVORAZIONE">In Lavorazione</option>
                        <option value="CONFERMATO">Confermato</option>
                        <option value="CONSEGNATO">Consegnato</option>
                    </select>
                </div>
                <div class="col-md-5">
                    <label class="form-label">Ricerca (Targa/Cliente)</label>
                    <input type="text" class="form-control" id="searchTerm" placeholder="Ricerca...">
                </div>
                <div class="col-md-3">
                    <label class="form-label">&nbsp;</label>
                    <button type="button" class="btn btn-success w-100" onclick="loadOrdini()">Cerca</button>
                </div>
            </form>
        </div>
    </div>

    <!-- Avvisi Importanti -->
    <div class="alert alert-danger" id="careCallAlert" style="display:none;">
        <strong>Care Call in Scadenza!</strong> <span id="careCallCount"></span> ordini richiedono unha care call nei prossimi giorni.
    </div>

    <!-- Tabella Ordini -->
    <div class="card">
        <div class="card-body">
            <table id="ordiniTable" class="table table-striped table-hover">
                <thead class="table-dark">
                    <tr>
                        <th>Numero Ordine</th>
                        <th>Targa</th>
                        <th>Cliente</th>
                        <th>ETA Consegna</th>
                        <th>Stato</th>
                        <th>Care Call</th>
                        <th>Azioni</th>
                    </tr>
                </thead>
                <tbody></tbody>
            </table>
        </div>
    </div>
</div>

<!-- Modal Ordine -->
<div class="modal fade" id="ordineModal" tabindex="-1">
    <div class="modal-dialog modal-lg">
        <div class="modal-content">
            <div class="modal-header">
                <h5 class="modal-title">Nuovo Ordine</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
            </div>
            <form id="ordineForm">
                <div class="modal-body">
                    <input type="hidden" id="ordineId" name="id">
                    <input type="hidden" id="leadId" name="leadId">
                    
                    <div class="row">
                        <div class="col-md-6">
                            <div class="mb-3">
                                <label class="form-label">Targa *</label>
                                <input type="text" class="form-control" id="targa" name="targa" required>
                            </div>
                        </div>
                        <div class="col-md-6">
                            <div class="mb-3">
                                <label class="form-label">Marca/Modello</label>
                                <input type="text" class="form-control" id="marcaModello" name="marcaModello">
                            </div>
                        </div>
                    </div>

                    <div class="row">
                        <div class="col-md-6">
                            <div class="mb-3">
                                <label class="form-label">VIN</label>
                                <input type="text" class="form-control" id="vin" name="vin">
                            </div>
                        </div>
                        <div class="col-md-6">
                            <div class="mb-3">
                                <label class="form-label">Data Immatricolazione</label>
                                <input type="date" class="form-control" id="dataImmatr" name="dataImmatr">
                            </div>
                        </div>
                    </div>

                    <div class="row">
                        <div class="col-md-6">
                            <div class="mb-3">
                                <label class="form-label">ETA Consegna</label>
                                <input type="date" class="form-control" id="etaConsegna" name="etaConsegna">
                            </div>
                        </div>
                        <div class="col-md-6">
                            <div class="mb-3">
                                <label class="form-label">KM Attuali</label>
                                <input type="number" class="form-control" id="kmAttuali" name="kmAttuali">
                            </div>
                        </div>
                    </div>

                    <div class="mb-3">
                        <label class="form-label">Note Consegna</label>
                        <textarea class="form-control" id="noteConsegna" name="noteConsegna" rows="3"></textarea>
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
    loadOrdini();
    checkCareCall();
});

function loadOrdini() {
    let status = $('#statusFiltro').val();
    let search = $('#searchTerm').val();
    
    $.ajax({
        url: '<s:url action="noleggioOrdineAction_list" />',
        type: 'GET',
        data: { statusFiltro: status, searchTerm: search },
        dataType: 'json',
        success: function(data) {
            let html = '';
            $.each(data, function() {
                html += '<tr>';
                html += '<td>' + this.numeroOrdine + '</td>';
                html += '<td><strong>' + this.targa + '</strong></td>';
                html += '<td>' + this.marcaModello + '</td>';
                html += '<td>' + this.etaConsegna + '</td>';
                html += '<td><span class="badge bg-info">' + this.status + '</span></td>';
                html += '<td>' + (this.ultimaCareCall ? this.ultimaCareCall : 'Non eseguita') + '</td>';
                html += '<td>';
                html += '<button class="btn btn-sm btn-info" onclick="editOrdine(' + this.id + ')">Edit</button> ';
                html += '<button class="btn btn-sm btn-warning" onclick="registerCareCall(' + this.id + ')">Care Call</button>';
                html += '</td>';
                html += '</tr>';
            });
            $('#ordiniTable tbody').html(html);
        }
    });
}

function checkCareCall() {
    $.ajax({
        url: '<s:url action="noleggioOrdineAction_requireingCareCall" />',
        type: 'GET',
        dataType: 'json',
        success: function(data) {
            if (data.count > 0) {
                $('#careCallCount').text(data.count + ' ordini');
                $('#careCallAlert').show();
            }
        }
    });
}

function registerCareCall(id) {
    let nota = prompt('Inserisci nota care call:');
    if (nota != null) {
        $.ajax({
            url: '<s:url action="noleggioOrdineAction_registerCareCall" />',
            type: 'POST',
            data: { id: id, nota: nota },
            dataType: 'json',
            success: function(data) {
                alert('Care call registrata');
                loadOrdini();
                checkCareCall();
            }
        });
    }
}

function newOrdine() {
    $('#ordineId').val('');
    $('#ordineForm')[0].reset();
}

function editOrdine(id) {
    $.ajax({
        url: '<s:url action="noleggioOrdineAction_view" />',
        type: 'GET',
        data: { id: id },
        dataType: 'json',
        success: function(data) {
            $('#ordineId').val(data.id);
            $('#targa').val(data.targa);
            $('#marcaModello').val(data.marcaModello);
            $('#vin').val(data.vin);
            $('#dataImmatr').val(data.dataImmatricolazione);
            $('#etaConsegna').val(data.etaConsegna);
            $('#kmAttuali').val(data.kmAttuali);
            $('#noteConsegna').val(data.noteConsegna);
            new bootstrap.Modal(document.getElementById('ordineModal')).show();
        }
    });
}

$('#ordineForm').submit(function(e) {
    e.preventDefault();
    $.ajax({
        url: '<s:url action="noleggioOrdineAction_save" />',
        type: 'POST',
        data: $(this).serialize(),
        success: function(data) {
            alert('Ordine salvato');
            bootstrap.Modal.getInstance(document.getElementById('ordineModal')).hide();
            loadOrdini();
        }
    });
});
</script>
</body>
</html>
