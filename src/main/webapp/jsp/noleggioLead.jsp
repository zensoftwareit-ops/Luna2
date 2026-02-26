<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="/struts-tags" prefix="s" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<!DOCTYPE html>
<html lang="it">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>CRM Broker Auto - Lead Noleggio - Luna2</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.10.0/font/bootstrap-icons.css">
    <link href="https://cdn.jsdelivr.net/npm/datatables.net-bs5@1.13.6/css/dataTables.bootstrap5.min.css" rel="stylesheet">
    <style>
        .fase-badge { font-weight: bold; padding: 6px 12px; border-radius: 4px; font-size: 0.85rem; }
        .fase-preventivazione { background: #e3f2fd; color: #1976d2; }
        .fase-istruttoria { background: #fff3e0; color: #f57c00; }
        .fase-ordine { background: #e8f5e9; color: #388e3c; }
        .fase-post-vendita { background: #f3e5f5; color: #7b1fa2; }
    </style>
</head>
<body>
    <%@ include file="../WEB-INF/jsp/includes/sidebar.jsp" %>

    <div class="col-md-10 content-wrapper p-4">
        <div class="container-fluid">
            <!-- Header -->
            <div class="d-flex justify-content-between align-items-center mb-4">
                <h1 class="h3"><i class="bi bi-car-front me-2"></i>CRM Broker Auto - Gestione Lead</h1>
                <button class="btn btn-primary" data-bs-toggle="modal" data-bs-target="#leadModal" onclick="newLead()">
                    <i class="bi bi-plus-circle me-1"></i>Nuovo Lead
                </button>
            </div>

            <!-- Filtri -->
            <div class="card mb-3 shadow-sm">
                <div class="card-body">
                    <form id="filterForm" class="row g-3">
                        <div class="col-md-3">
                            <label class="form-label">Fase</label>
                            <select class="form-select" id="fasceFiltro" name="fasceFiltro" onchange="loadLeads()">
                                <option value="">— Tutte le fasi —</option>
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
                        <div class="col-md-4">
                            <label class="form-label">&nbsp;</label>
                            <button type="button" class="btn btn-success w-100" onclick="loadLeads()">
                                <i class="bi bi-search me-1"></i>Cerca
                            </button>
                        </div>
                    </form>
                </div>
            </div>

            <!-- Dashboard Stats -->
            <div class="row mb-4" id="dashboardStats">
                <div class="col-md-3">
                    <div class="card border-primary">
                        <div class="card-body text-center">
                            <h6 class="card-title text-muted">Preventivazione</h6>
                            <h3 id="stat-preventivazione" class="text-primary">-</h3>
                        </div>
                    </div>
                </div>
                <div class="col-md-3">
                    <div class="card border-warning">
                        <div class="card-body text-center">
                            <h6 class="card-title text-muted">Istruttoria</h6>
                            <h3 id="stat-istruttoria" class="text-warning">-</h3>
                        </div>
                    </div>
                </div>
                <div class="col-md-3">
                    <div class="card border-success">
                        <div class="card-body text-center">
                            <h6 class="card-title text-muted">Ordine</h6>
                            <h3 id="stat-ordine" class="text-success">-</h3>
                        </div>
                    </div>
                </div>
                <div class="col-md-3">
                    <div class="card border-info">
                        <div class="card-body text-center">
                            <h6 class="card-title text-muted">Post-Vendita</h6>
                            <h3 id="stat-post-vendita" class="text-info">-</h3>
                        </div>
                    </div>
                </div>
            </div>

            <!-- Tabella Lead -->
            <div class="card shadow-sm">
                <div class="card-header bg-light">
                    <h5 class="mb-0"><i class="bi bi-list-ul me-2"></i>Elenco Lead Noleggio</h5>
                </div>
                <div class="card-body">
                    <table id="leadsTable" class="table table-striped table-hover">
                        <thead class="table-light">
                            <tr>
                                <th>Pratica</th>
                                <th>Cliente</th>
                                <th>Email</th>
                                <th>Telefono</th>
                                <th>Fase</th>
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

    <!-- Modal Lead -->
    <div class="modal fade" id="leadModal" tabindex="-1">
        <div class="modal-dialog modal-lg">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title" id="modalTitle"><i class="bi bi-file-earmark-plus me-2"></i>Nuovo Lead</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                </div>
                <form id="leadForm">
                    <div class="modal-body">
                        <input type="hidden" id="leadId" name="id">
                        
                        <div class="row">
                            <div class="col-md-6">
                                <div class="mb-3">
                                    <label class="form-label">Ragione Sociale <span class="text-danger">*</span></label>
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
                                    <input type="number" class="form-control" id="durataMesi" name="durataMesi">
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
                            <textarea class="form-control" id="note" name="note" rows="3" placeholder="Note aggiuntive..."></textarea>
                        </div>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Annulla</button>
                        <button type="submit" class="btn btn-primary"><i class="bi bi-check-circle me-1"></i>Salva</button>
                    </div>
                </form>
            </div>
        </div>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/jquery@3.7.0/dist/jquery.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/datatables.net@1.13.6/js/jquery.dataTables.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/datatables.net-bs5@1.13.6/js/dataTables.bootstrap5.min.js"></script>

    <script>
    $(document).ready(function() {
        loadLeads();
        loadDashboard();
    });

    function loadLeads() {
        let fase = $('#fasceFiltro').val();
        let search = $('#searchTerm').val();
        
        $.ajax({
            url: '<s:url action="lead!list" namespace="/app/noleggio"/>',
            type: 'GET',
            data: { fasceFiltro: fase, searchTerm: search },
            success: function(data) {
                let html = '';
                if(data && data.length > 0) {
                    data.forEach(function(lead) {
                        html += '<tr>';
                        html += '<td><strong>' + (lead.numeroPratica || '-') + '</strong></td>';
                        html += '<td>' + (lead.ragioneSociale || '-') + '</td>';
                        html += '<td>' + (lead.email || '-') + '</td>';
                        html += '<td>' + (lead.telefono || '-') + '</td>';
                        html += '<td><span class="fase-badge fase-' + (lead.fase || '').toLowerCase() + '">' + (lead.fase || '-') + '</span></td>';
                        html += '<td>' + (lead.utenteAssegnatoId || '-') + '</td>';
                        html += '<td>' + (lead.dataCreazione || '') + '</td>';
                        html += '<td class="text-end">';
                        html += '<button class="btn btn-sm btn-info" onclick="editLead(' + lead.id + ')" title="Modifica"><i class="bi bi-pencil"></i></button> ';
                        html += '<button class="btn btn-sm btn-danger" onclick="deleteLead(' + lead.id + ')" title="Elimina"><i class="bi bi-trash"></i></button>';
                        html += '</td>';
                        html += '</tr>';
                    });
                } else {
                    html = '<tr><td colspan="8" class="text-center text-muted">Nessun lead trovato</td></tr>';
                }
                $('#leadsTable tbody').html(html);
            },
            error: function() {
                $('#leadsTable tbody').html('<tr><td colspan="8" class="text-center text-danger">Errore nel caricamento</td></tr>');
            }
        });
    }

    function loadDashboard() {
        $.ajax({
            url: '<s:url action="lead!dashboardData" namespace="/app/noleggio"/>',
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
        $('#modalTitle').html('<i class="bi bi-file-earmark-plus me-2"></i>Nuovo Lead');
    }

    function editLead(id) {
        $.ajax({
            url: '<s:url action="lead!view" namespace="/app/noleggio"/>',
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
                $('#modalTitle').html('<i class="bi bi-pencil me-2"></i>Modifica Lead');
                new bootstrap.Modal(document.getElementById('leadModal')).show();
            }
        });
    }

    function deleteLead(id) {
        if (confirm('Sei sicuro di voler eliminare questo lead?')) {
            $.ajax({
                url: '<s:url action="lead!delete" namespace="/app/noleggio"/>',
                type: 'POST',
                data: { id: id },
                success: function() {
                    alert('Lead eliminato');
                    loadLeads();
                    loadDashboard();
                }
            });
        }
    }

    $('#leadForm').submit(function(e) {
        e.preventDefault();
        $.ajax({
            url: '<s:url action="lead!save" namespace="/app/noleggio"/>',
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
