<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html lang="it">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>NBT - Noleggio Breve Termine - Luna2</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.10.0/font/bootstrap-icons.css">
    <link href="https://cdn.datatables.net/1.13.7/css/dataTables.bootstrap5.min.css" rel="stylesheet">
</head>
<body>
    <%@ include file="../../WEB-INF/jsp/includes/sidebar.jsp" %>

    <div class="col-md-10 content-wrapper p-4">
        <div class="container-fluid">
            <!-- Header -->
            <div class="d-flex justify-content-between align-items-center mb-4">
                <h1 class="h3"><i class="bi bi-clock-history me-2"></i>NBT - Noleggio Breve Termine (1-30 gg)</h1>
                <button class="btn btn-primary" data-bs-toggle="modal" data-bs-target="#createNBTModal">
                    <i class="bi bi-plus-circle me-1"></i>Nuova Richiesta
                </button>
            </div>

            <!-- KPI Cards -->
            <div class="row mb-4">
                <div class="col-md-3">
                    <div class="card border-info">
                        <div class="card-body">
                            <h6 class="card-title text-muted">Richieste Mese</h6>
                            <h3 class="text-info" id="requestsMonth">-</h3>
                        </div>
                    </div>
                </div>
                <div class="col-md-3">
                    <div class="card border-success">
                        <div class="card-body">
                            <h6 class="card-title text-muted">Completati</h6>
                            <h3 class="text-success" id="completedMonth">-</h3>
                        </div>
                    </div>
                </div>
                <div class="col-md-3">
                    <div class="card border-primary">
                        <div class="card-body">
                            <h6 class="card-title text-muted">Fatturato NBT</h6>
                            <h3 class="text-primary" id="revenueMonth">€ -</h3>
                        </div>
                    </div>
                </div>
                <div class="col-md-3">
                    <div class="card border-warning">
                        <div class="card-body">
                            <h6 class="card-title text-muted">Tasso Conversione</h6>
                            <h3 class="text-warning" id="conversionRate">-</h3>
                        </div>
                    </div>
                </div>
            </div>

            <!-- Main Card with Tabs -->
            <div class="card shadow-sm">
                <ul class="nav nav-tabs" role="tablist">
                    <li class="nav-item">
                        <a class="nav-link active" data-bs-toggle="tab" href="#richieste">
                            <i class="bi bi-file-earmark-text me-1"></i>Richieste
                        </a>
                    </li>
                    <li class="nav-item">
                        <a class="nav-link" data-bs-toggle="tab" href="#confermate">
                            <i class="bi bi-check-circle me-1"></i>Confermate
                        </a>
                    </li>
                    <li class="nav-item">
                        <a class="nav-link" data-bs-toggle="tab" href="#affidate">
                            <i class="bi bi-car-front me-1"></i>Affidate
                        </a>
                    </li>
                    <li class="nav-item">
                        <a class="nav-link" data-bs-toggle="tab" href="#completate">
                            <i class="bi bi-check2-all me-1"></i>Completate
                        </a>
                    </li>
                    <li class="nav-item">
                        <a class="nav-link" data-bs-toggle="tab" href="#alert">
                            <i class="bi bi-exclamation-triangle me-1"></i>Avvisi
                        </a>
                    </li>
                </ul>

                <div class="card-body">
                    <div class="tab-content">
                        <!-- Richieste Tab -->
                        <div id="richieste" class="tab-pane fade show active">
                            <table class="table table-striped table-hover">
                                <thead class="table-light">
                                    <tr>
                                        <th>Pratica</th>
                                        <th>Cliente</th>
                                        <th>Periodo</th>
                                        <th>Giorni</th>
                                        <th>Preventivo Valido Fino</th>
                                        <th class="text-end">Azioni</th>
                                    </tr>
                                </thead>
                                <tbody id="richiesteTable"></tbody>
                            </table>
                        </div>

                        <!-- Confermate Tab -->
                        <div id="confermate" class="tab-pane fade">
                            <table class="table table-striped table-hover">
                                <thead class="table-light">
                                    <tr>
                                        <th>Pratica</th>
                                        <th>Cliente</th>
                                        <th>Veicolo Assegnato</th>
                                        <th>Data Consegna</th>
                                        <th>Status</th>
                                        <th class="text-end">Azioni</th>
                                    </tr>
                                </thead>
                                <tbody id="confirmateTable"></tbody>
                            </table>
                        </div>

                        <!-- Affidate Tab -->
                        <div id="affidate" class="tab-pane fade">
                            <table class="table table-striped table-hover">
                                <thead class="table-light">
                                    <tr>
                                        <th>Pratica</th>
                                        <th>Cliente</th>
                                        <th>Veicolo</th>
                                        <th>Km Iniziali</th>
                                        <th>Restituzione Prevista</th>
                                        <th class="text-end">Azioni</th>
                                    </tr>
                                </thead>
                                <tbody id="affidateTable"></tbody>
                            </table>
                        </div>

                        <!-- Completate Tab -->
                        <div id="completate" class="tab-pane fade">
                            <table class="table table-striped table-hover">
                                <thead class="table-light">
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

                        <!-- Alerts Tab -->
                        <div id="alert" class="tab-pane fade">
                            <div class="row">
                                <div class="col-md-6">
                                    <div class="alert alert-warning border-2">
                                        <h6><i class="bi bi-exclamation-triangle-fill me-2"></i>Follow-up 24h</h6>
                                        <p class="small text-muted mb-2">Preventivi scaduti in 24 ore (reminder inviato):</p>
                                        <div id="followupList"></div>
                                    </div>
                                </div>
                                <div class="col-md-6">
                                    <div class="alert alert-danger border-2">
                                        <h6><i class="bi bi-clock-history me-2"></i>Restituzione Imminente</h6>
                                        <p class="small text-muted mb-2">Veicoli da restituire entro 24 ore:</p>
                                        <div id="returnsList"></div>
                                    </div>
                                </div>
                            </div>
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
                    <h5 class="modal-title"><i class="bi bi-plus-circle me-2"></i>Nuova Richiesta NBT</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                </div>
                <form id="createNBTForm">
                    <div class="modal-body">
                        <div class="row">
                            <div class="col-md-6 mb-3">
                                <label class="form-label">Cliente</label>
                                <input type="text" class="form-control" name="ragioneSociale" placeholder="Ragione sociale" required>
                            </div>
                            <div class="col-md-6 mb-3">
                                <label class="form-label">Email</label>
                                <input type="email" class="form-control" name="email" placeholder="email@azienda.it" required>
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
                                <input type="text" class="form-control" name="marcaPreferita" placeholder="Es. Fiat, BMW,...">
                            </div>
                            <div class="col-md-6 mb-3">
                                <label class="form-label">Modello Preferito</label>
                                <input type="text" class="form-control" name="modelloPreferito" placeholder="Es. 500, X3,...">
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

    <script src="https://code.jquery.com/jquery-3.6.4.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
    <script src="https://cdn.datatables.net/1.13.7/js/jquery.dataTables.min.js"></script>
    <script src="https://cdn.datatables.net/1.13.7/js/dataTables.bootstrap5.min.js"></script>

    <script>
    $(document).ready(function() {
        loadDashboard();
        loadNBT();
        
        $('#createNBTForm').on('submit', function(e) {
            e.preventDefault();
            $.ajax({
                type: 'POST',
                url: '<s:url action="nbt-save" namespace="/app/noleggio"/>',
                data: $(this).serialize(),
                success: function() {
                    location.reload();
                }
            });
        });
    });

    function loadDashboard() {
        $.ajax({
            url: '<s:url action="nbt-dashboard" namespace="/app/noleggio"/>',
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
            url: '<s:url action="nbt-list" namespace="/app/noleggio"/>',
            dataType: 'json',
            success: function(data) {
                if(data.nbts) {
                    data.nbts.forEach(function(nbt) {
                        let row = `
                            <tr>
                                <td><strong>${nbt.numeroPratica}</strong></td>
                                <td>${nbt.ragioneSociale}</td>
                                <td>${nbt.dataInizio} — ${nbt.dataFine}</td>
                                <td>${nbt.numeroGiorni}</td>
                                <td>${nbt.preventivoValidoFino}</td>
                                <td class="text-end">
                                    <button class="btn btn-sm btn-info" onclick="viewNBT(${nbt.id})" title="Visualizza">
                                        <i class="bi bi-eye"></i>
                                    </button>
                                    <button class="btn btn-sm btn-warning" onclick="editNBT(${nbt.id})" title="Modifica">
                                        <i class="bi bi-pencil"></i>
                                    </button>
                                </td>
                            </tr>
                        `;
                        
                        if(nbt.status === 'RICHIESTA_RICEVUTA') {
                            $('#richiesteTable').append(row);
                        }
                    });
                }
            }
        });
        
        $.ajax({
            url: '<s:url action="nbt-followup" namespace="/app/noleggio"/>',
            dataType: 'json',
            success: function(data) {
                if(data.nbts && data.nbts.length > 0) {
                    data.nbts.forEach(function(nbt) {
                        $('#followupList').append(`<div class="mb-2"><strong>${nbt.numeroPratica}</strong> — ${nbt.ragioneSociale}</div>`);
                    });
                } else {
                    $('#followupList').html('<span class="text-muted">Nessun follow-up</span>');
                }
            }
        });
        
        $.ajax({
            url: '<s:url action="nbt-return" namespace="/app/noleggio"/>',
            dataType: 'json',
            success: function(data) {
                if(data.nbts && data.nbts.length > 0) {
                    data.nbts.forEach(function(nbt) {
                        let veicolo = nbt.targetaVeicoloAssegnato || nbt.veicolo || 'Non assegnato';
                        $('#returnsList').append(`<div class="mb-2"><strong>${nbt.numeroPratica}</strong> — ${veicolo}</div>`);
                    });
                } else {
                    $('#returnsList').html('<span class="text-muted">Nessun veicolo imminente</span>');
                }
            }
        });
    }

    function viewNBT(id) {
        window.location.href = '<s:url action="nbt" namespace="/app/noleggio"/>?id=' + id;
    }

    function editNBT(id) {
        window.location.href = '<s:url action="nbt" namespace="/app/noleggio"/>?id=' + id + '&edit=true';
    }
    </script>

</body>
</html>
