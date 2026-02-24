<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html lang="it">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Preventivazione - Broker Noleggio</title>
    <link rel="stylesheet" href="<s:url value='/css/bootstrap.min.css'/>">
    <link rel="stylesheet" href="<s:url value='/css/datatables.min.css'/>">
    <link rel="stylesheet" href="<s:url value='/css/luna2.css'/>">
</head>
<body>
<nav class="navbar navbar-expand-lg navbar-dark bg-dark">
    <div class="container-fluid">
        <span class="navbar-brand">Broker Noleggio - Preventivazione</span>
    </div>
</nav>

<div class="container-fluid mt-4">
    <div class="row">
        <!-- Sidebar Filters -->
        <div class="col-md-3">
            <div class="card">
                <div class="card-header bg-primary text-white">Filtri</div>
                <div class="card-body">
                    <form id="filterForm">
                        <div class="mb-3">
                            <label class="form-label">Fase</label>
                            <select id="fasceFiltro" class="form-select" name="fasceFiltro">
                                <option value="">Tutte</option>
                                <option value="PREVENTIVAZIONE">Preventivazione</option>
                                <option value="ISTRUTTORIA">Istruttoria</option>
                                <option value="ORDINE">Ordine</option>
                                <option value="POST_VENDITA">Post-Vendita</option>
                            </select>
                        </div>
                        
                        <div class="mb-3">
                            <label class="form-label">Cerca Cliente</label>
                            <input type="text" id="searchTerm" class="form-control" placeholder="Nome/Email">
                        </div>
                        
                        <button type="button" class="btn btn-primary btn-sm" onclick="filterTable()">Filtra</button>
                        <button type="button" class="btn btn-secondary btn-sm" onclick="resetFilter()">Reset</button>
                    </form>
                    
                    <hr>
                    <div id="dashboardStats" class="small">
                        <div class="mb-2">
                            <strong>Preventivazione:</strong> <span id="countPreventivazione">0</span>
                        </div>
                        <div class="mb-2">
                            <strong>Istruttoria:</strong> <span id="countIstruttoria">0</span>
                        </div>
                        <div class="mb-2">
                            <strong>Ordine:</strong> <span id="countOrdine">0</span>
                        </div>
                        <div class="mb-2">
                            <strong>Post-Vendita:</strong> <span id="countPostVendita">0</span>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <!-- Main Content -->
        <div class="col-md-9">
            <div class="card">
                <div class="card-header d-flex justify-content-between align-items-center">
                    <h5 class="mb-0">Lead Noleggio</h5>
                    <button class="btn btn-success btn-sm" data-bs-toggle="modal" data-bs-target="#createLeadModal">
                        + Nuovo Lead
                    </button>
                </div>

                <!-- DataTable -->
                <div class="card-body">
                    <table id="leadsTable" class="table table-striped table-hover">
                        <thead class="table-dark">
                            <tr>
                                <th>Pratica</th>
                                <th>Cliente</th>
                                <th>Email</th>
                                <th>Fase</th>
                                <th>Budget</th>
                                <th>Creazione</th>
                                <th>Azioni</th>
                            </tr>
                        </thead>
                        <tbody>
                            <s:iterator value="entities" status="stat">
                                <tr>
                                    <td><s:property value="numeroPratica"/></td>
                                    <td><s:property value="ragioneSociale"/></td>
                                    <td><s:property value="email"/></td>
                                    <td>
                                        <span class="badge" id="badge-<s:property value='id'/>">
                                            <s:property value="fase"/>
                                        </span>
                                    </td>
                                    <td><fmt:formatNumber value="${budgetMassimo}" pattern="€ #,##0.00"/></td>
                                    <td><fmt:formatDate value="${dataCreazione}" pattern="dd/MM/yyyy"/></td>
                                    <td>
                                        <button class="btn btn-sm btn-info" onclick="viewLead(<s:property value='id'/>)">Visualizza</button>
                                        <button class="btn btn-sm btn-warning" onclick="editLead(<s:property value='id'/>)">Modifica</button>
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

<!-- Create Lead Modal -->
<div class="modal fade" id="createLeadModal" tabindex="-1">
    <div class="modal-dialog modal-lg">
        <div class="modal-content">
            <div class="modal-header">
                <h5 class="modal-title">Nuovo Lead</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
            </div>
            <form id="createLeadForm" method="post" action="<s:url value='/action/noleggio/lead.action'/>">
                <div class="modal-body">
                    <div class="row">
                        <div class="col-md-6 mb-3">
                            <label class="form-label">Ragione Sociale</label>
                            <input type="text" class="form-control" name="entity.ragioneSociale" required>
                        </div>
                        <div class="col-md-6 mb-3">
                            <label class="form-label">Email</label>
                            <input type="email" class="form-control" name="entity.email" required>
                        </div>
                    </div>
                    
                    <div class="row">
                        <div class="col-md-6 mb-3">
                            <label class="form-label">Telefono</label>
                            <input type="tel" class="form-control" name="entity.telefono">
                        </div>
                        <div class="col-md-6 mb-3">
                            <label class="form-label">Contatto</label>
                            <input type="text" class="form-control" name="entity.nomeContatto">
                        </div>
                    </div>
                    
                    <div class="row">
                        <div class="col-md-6 mb-3">
                            <label class="form-label">KM Annuali Previsti</label>
                            <input type="number" class="form-control" name="entity.kmAnnualiPrevisti" placeholder="30000">
                        </div>
                        <div class="col-md-6 mb-3">
                            <label class="form-label">Budget Massimo</label>
                            <input type="number" class="form-control" name="entity.budgetMassimo" step="0.01" placeholder="50000.00">
                        </div>
                    </div>
                    
                    <div class="row">
                        <div class="col-md-6 mb-3">
                            <label class="form-label">Data Inizio Noleggio</label>
                            <input type="date" class="form-control" name="entity.dataInizioNoleggio">
                        </div>
                        <div class="col-md-6 mb-3">
                            <label class="form-label">Durata (Mesi)</label>
                            <input type="number" class="form-control" name="entity.durataMesi" placeholder="36">
                        </div>
                    </div>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Annulla</button>
                    <button type="submit" class="btn btn-primary" name="save" value="save">Crea Lead</button>
                </div>
            </form>
        </div>
    </div>
</div>

<script src="<s:url value='/js/jquery.min.js'/>"></script>
<script src="<s:url value='/js/bootstrap.bundle.min.js'/>"></script>
<script src="<s:url value='/js/datatables.min.js'/>"></script>

<script>
let table;

$(document).ready(function() {
    // Initialize DataTable
    table = $('#leadsTable').DataTable({
        language: {
            url: '<s:url value="/js/datatable-it.json"/>'
        },
        pageLength: 25,
        columnDefs: [{
            orderable: false,
            targets: 6
        }]
    });
    
    // Load dashboard stats
    loadDashboard();
    
    // Submit form
    $('#createLeadForm').on('submit', function(e) {
        e.preventDefault();
        $.ajax({
            type: 'POST',
            url: '<s:url value="/action/noleggio/lead!save.action"/>',
            data: $(this).serialize(),
            success: function() {
                location.reload();
            }
        });
    });
});

function filterTable() {
    let fase = $('#fasceFiltro').val();
    let search = $('#searchTerm').val();
    
    $.ajax({
        url: '<s:url value="/action/noleggio/lead!list.action"/>',
        data: {fasceFiltro: fase, searchTerm: search},
        success: function(data) {
            // Re-filter table
            if (fase) {
                table.column(3).search(fase).draw();
            }
            if (search) {
                table.search(search).draw();
            }
        }
    });
}

function resetFilter() {
    $('#fasceFiltro').val('');
    $('#searchTerm').val('');
    table.search('').column(3).search('').draw();
}

function loadDashboard() {
    $.ajax({
        url: '<s:url value="/action/noleggio/lead!dashboardData.action"/>',
        dataType: 'json',
        success: function(data) {
            if(data.countByFase) {
                $('#countPreventivazione').text(data.countByFase['PREVENTIVAZIONE'] || '0');
                $('#countIstruttoria').text(data.countByFase['ISTRUTTORIA'] || '0');
                $('#countOrdine').text(data.countByFase['ORDINE'] || '0');
                $('#countPostVendita').text(data.countByFase['POST_VENDITA'] || '0');
            }
        }
    });
}

function viewLead(id) {
    window.location.href = '<s:url value="/action/noleggio/lead!view.action"/>?id=' + id;
}

function editLead(id) {
    window.location.href = '<s:url value="/action/noleggio/lead!create.action"/>?id=' + id;
}
</script>

</body>
</html>
