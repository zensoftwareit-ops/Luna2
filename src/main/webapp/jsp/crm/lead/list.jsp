<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib uri="http://struts.apache.org/tags-struts2" prefix="s" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Lead - CRM Luna2</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.datatables.net/1.13.6/css/dataTables.bootstrap5.min.css" rel="stylesheet">
    <style>
        .lead-list-container { padding: 20px; }
        .stage-badge { font-size: 0.9rem; padding: 0.4rem 0.8rem; }
        .high-value { color: #198754; font-weight: bold; }
        .medium-value { color: #0d6efd; }
        .low-value { color: #6c757d; }
        .search-bar { margin-bottom: 20px; }
        .filter-section { background: #f8f9fa; padding: 15px; border-radius: 5px; margin-bottom: 20px; }
        .action-buttons { white-space: nowrap; }
    </style>
</head>
<body>
<nav class="navbar navbar-expand-lg navbar-dark bg-dark">
    <div class="container-fluid">
        <a class="navbar-brand" href="#">Luna2 CRM</a>
        <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#navbarNav">
            <span class="navbar-toggler-icon"></span>
        </button>
        <div class="collapse navbar-collapse" id="navbarNav">
            <ul class="navbar-nav ms-auto">
                <li class="nav-item"><a class="nav-link active" href="#"><i class="bi bi-people"></i> Lead</a></li>
                <li class="nav-item"><a class="nav-link" href="/crm/lead/pipeline"><i class="bi bi-kanban"></i> Pipeline</a></li>
                <li class="nav-item"><a class="nav-link" href="#"><i class="bi bi-bar-chart"></i> Analytics</a></li>
            </ul>
        </div>
    </div>
</nav>

<div class="lead-list-container">
    <div class="row mb-4">
        <div class="col-md-8">
            <h2><i class="bi bi-people"></i> Lead Management</h2>
        </div>
        <div class="col-md-4 text-end">
            <a href="/crm/lead/create" class="btn btn-primary">
                <i class="bi bi-plus-circle"></i> Nuovo Lead
            </a>
            <button class="btn btn-success" onclick="exportToCSV()">
                <i class="bi bi-download"></i> Esporta CSV
            </button>
        </div>
    </div>

    <!-- FILTER SECTION -->
    <div class="filter-section">
        <form id="filterForm" class="row g-3">
            <div class="col-md-3">
                <label for="filterStage" class="form-label">Stato/Stage</label>
                <select id="filterStage" name="filterStage" class="form-select form-select-sm">
                    <option value="">-- Tutti --</option>
                    <option value="NUOVO">🔵 BOZZA</option>
                    <option value="QUALIFICATO">🟢 QUALIFICATO</option>
                    <option value="CONTATTATO">🟡 PROPOSTA</option>
                    <option value="PREVENTIVO">🟠 NEGOZIAZIONE</option>
                    <option value="VINTO">✅ VINTO</option>
                    <option value="PERSO">❌ PERSO</option>
                </select>
            </div>
            <div class="col-md-3">
                <label for="filterProbability" class="form-label">Probabilità Chiusura</label>
                <input type="range" id="filterProbability" name="filterProbability" class="form-range" 
                       min="0" max="100" value="0">
                <small id="probabilityValue">0%</small>
            </div>
            <div class="col-md-3">
                <label for="searchTerm" class="form-label">Cerca Azienda/Contatto</label>
                <input type="text" id="searchTerm" name="searchTerm" class="form-control form-control-sm" 
                       placeholder="Filtra per nome...">
            </div>
            <div class="col-md-3 d-flex align-items-end gap-2">
                <button type="button" class="btn btn-sm btn-outline-primary flex-grow-1" onclick="applyFilters()">
                    <i class="bi bi-funnel"></i> Filtra
                </button>
                <button type="button" class="btn btn-sm btn-outline-secondary" onclick="resetFilters()">
                    <i class="bi bi-arrow-clockwise"></i> Reset
                </button>
            </div>
        </form>
    </div>

    <!-- DATATABLE -->
    <div class="table-responsive">
        <table id="leadsTable" class="table table-hover table-sm">
            <thead class="table-dark">
                <tr>
                    <th>ID</th>
                    <th>Nome Contatto</th>
                    <th>Azienda</th>
                    <th>Email</th>
                    <th>Telefono</th>
                    <th>Stato/Stage</th>
                    <th>Budget Stimato</th>
                    <th>Probabilità %</th>
                    <th>Valore Stimato</th>
                    <th>Data Contatto</th>
                    <th style="width: 120px;">Azioni</th>
                </tr>
            </thead>
            <tbody>
                <!-- DataTables popola questa sezione -->
            </tbody>
        </table>
    </div>
</div>

<!-- MODALS -->
<!-- Modal per dettagli rapidi -->
<div class="modal fade" id="detailsModal" tabindex="-1">
    <div class="modal-dialog modal-lg">
        <div class="modal-content">
            <div class="modal-header">
                <h5 class="modal-title">Dettagli Lead</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
            </div>
            <div class="modal-body" id="detailsContent">
                <!-- Contenuto dinamico -->
            </div>
        </div>
    </div>
</div>

<script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
<script src="https://cdn.datatables.net/1.13.6/js/jquery.dataTables.min.js"></script>
<script src="https://cdn.datatables.net/1.13.6/js/dataTables.bootstrap5.min.js"></script>

<script>
// Initialize DataTable
let table = $('#leadsTable').DataTable({
    processing: true,
    serverSide: true,
    ajax: {
        url: '/crm/lead!list.action',
        data: function(d) {
            d.filterStage = $('#filterStage').val();
            d.filterProbability = $('#filterProbability').val();
            d.searchTerm = $('#searchTerm').val();
        }
    },
    columns: [
        { data: 'id' },
        { data: 'nomeContatto' },
        { data: 'azienda' },
        { data: 'email' },
        { data: 'telefono' },
        { 
            data: 'stato',
            render: function(data) {
                let badge = 'secondary';
                if (data === 'NUOVO') badge = 'primary';
                else if (data === 'QUALIFICATO') badge = 'success';
                else if (data === 'CONTATTATO') badge = 'warning';
                else if (data === 'VINTO') badge = 'success';
                else if (data === 'PERSO') badge = 'danger';
                return '<span class="badge bg-' + badge + '">' + data + '</span>';
            }
        },
        { 
            data: 'budgetStimato',
            render: function(data) {
                return data ? '€ ' + new Intl.NumberFormat('it-IT').format(data) : '-';
            }
        },
        { 
            data: 'probabilitaChiusura',
            render: function(data) {
                let color = 'low-value';
                if (data >= 70) color = 'high-value';
                else if (data >= 40) color = 'medium-value';
                return '<span class="' + color + '">' + (data || 0) + '%</span>';
            }
        },
        { 
            data: 'valoreStimato',
            render: function(data) {
                return data ? '€ ' + new Intl.NumberFormat('it-IT').format(data.toFixed(2)) : '€ 0';
            }
        },
        { 
            data: 'dataContatto',
            render: function(data) {
                return data ? new Date(data).toLocaleDateString('it-IT') : '-';
            }
        },
        { 
            data: 'id',
            render: function(data) {
                return '<div class="action-buttons">' +
                    '<a href="/crm/lead!view?id=' + data + '" class="btn btn-sm btn-info" title="Visualizza">' +
                    '<i class="bi bi-eye"></i></a> ' +
                    '<a href="/crm/lead!edit?id=' + data + '" class="btn btn-sm btn-warning" title="Modifica">' +
                    '<i class="bi bi-pencil"></i></a> ' +
                    '<button class="btn btn-sm btn-danger" onclick="deleteLead(' + data + ')" title="Elimina">' +
                    '<i class="bi bi-trash"></i></button>' +
                    '</div>';
            }
        }
    ],
    pageLength: 20,
    lengthMenu: [[10, 20, 50, 100], [10, 20, 50, 100]],
    order: [[9, 'desc']], // Ordina per data contatto (discendente)
    language: {
        url: 'https://cdn.datatables.net/plug-ins/1.13.6/i18n/it-IT.json'
    }
});

// Filter handlers
$('#filterProbability').on('input', function() {
    $('#probabilityValue').text($(this).val() + '%');
});

function applyFilters() {
    table.ajax.reload();
}

function resetFilters() {
    $('#filterStage').val('');
    $('#filterProbability').val('0');
    $('#searchTerm').val('');
    table.ajax.reload();
}

function deleteLead(id) {
    if (confirm('Sei sicuro di voler eliminare questo lead?')) {
        $.ajax({
            url: '/crm/lead!delete.action?id=' + id,
            type: 'POST',
            success: function() {
                table.ajax.reload();
                alert('Lead eliminato con successo');
            },
            error: function() {
                alert('Errore nell\'eliminazione del lead');
            }
        });
    }
}

function exportToCSV() {
    window.location.href = '/crm/lead!export.action';
}

// Refresh table every 5 minutes for real-time updates (optional)
setInterval(() => {
    table.ajax.reload(null, false);
}, 300000);
</script>
</body>
</html>
