<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="it">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Dashboard - Luna2</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.10.0/font/bootstrap-icons.css">
    <script src="https://cdn.jsdelivr.net/npm/chart.js@4.4.0/dist/chart.umd.min.js"></script>
    <style>
        .stat-card {
            border: none;
            border-radius: 12px;
            box-shadow: 0 2px 12px rgba(0,0,0,0.08);
            transition: transform 0.3s;
            height: 100%;
        }
        .stat-card:hover {
            transform: translateY(-5px);
            box-shadow: 0 4px 20px rgba(0,0,0,0.12);
        }
        .stat-icon {
            width: 60px;
            height: 60px;
            border-radius: 12px;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 24px;
        }
        .chart-container {
            position: relative;
            height: 300px;
        }
        .alert-badge {
            position: absolute;
            top: 10px;
            right: 10px;
        }
        .activity-item {
            padding: 12px;
            border-left: 3px solid #e9ecef;
            margin-bottom: 10px;
            transition: all 0.2s;
        }
        .activity-item:hover {
            background: #f8f9fa;
            border-left-color: #0d6efd;
        }
    </style>
</head>
<body>
    <%@ include file="includes/sidebar.jsp" %>

            <div class="content-wrapper p-4">
                <div class="container-fluid">
                    <div class="d-flex justify-content-between align-items-center mb-4">
                        <h1 class="h3">Dashboard</h1>
                        <div class="text-muted">
                            <i class="bi bi-person-circle me-2"></i>
                            <strong>${sessionScope.currentUser.nomeCompleto}</strong>
                        </div>
                    </div>

                    <s:if test="hasActionMessages()">
                        <div class="alert alert-success alert-dismissible fade show">
                            <s:actionmessage/>
                            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
                        </div>
                    </s:if>

                    <!-- Stats Cards Row 1 -->
                    <div class="row mb-4">
                        <div class="col-xl-3 col-md-6 mb-3">
                            <div class="card stat-card">
                                <div class="card-body">
                                    <div class="d-flex justify-content-between align-items-center">
                                        <div>
                                            <h6 class="text-muted mb-2">Fatturato Mese</h6>
                                            <h3 class="mb-0">€ <s:property value="getText('{0,number,#,##0.00}', {fatturatoMese})"/></h3>
                                            <small class="text-success"><i class="bi bi-arrow-up"></i> Anno: € <s:property value="getText('{0,number,#,##0}', {fatturatoAnno})"/></small>
                                        </div>
                                        <div class="stat-icon bg-success bg-opacity-10 text-success">
                                            <i class="bi bi-currency-euro"></i>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>

                        <div class="col-xl-3 col-md-6 mb-3">
                            <div class="card stat-card">
                                <div class="card-body">
                                    <div class="d-flex justify-content-between align-items-center">
                                        <div>
                                            <h6 class="text-muted mb-2">Ordini in Lavorazione</h6>
                                            <h3 class="mb-0"><s:property value="ordiniInLavorazione"/></h3>
                                            <small class="text-primary"><i class="bi bi-box-seam"></i> Attivi</small>
                                        </div>
                                        <div class="stat-icon bg-primary bg-opacity-10 text-primary">
                                            <i class="bi bi-cart-check"></i>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>

                        <div class="col-xl-3 col-md-6 mb-3">
                            <div class="card stat-card">
                                <div class="card-body">
                                    <div class="d-flex justify-content-between align-items-center">
                                        <div>
                                            <h6 class="text-muted mb-2">Lead Attivi</h6>
                                            <h3 class="mb-0"><s:property value="leadAttivi"/></h3>
                                            <small class="text-info"><i class="bi bi-graph-up"></i> Conv. <s:property value="leadConversioneRate"/>%</small>
                                        </div>
                                        <div class="stat-icon bg-info bg-opacity-10 text-info">
                                            <i class="bi bi-funnel"></i>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>

                        <div class="col-xl-3 col-md-6 mb-3">
                            <div class="card stat-card">
                                <div class="card-body position-relative">
                                    <s:if test="giacenzeSottoScorta > 0">
                                        <span class="badge bg-danger alert-badge"><s:property value="giacenzeSottoScorta"/> Alert</span>
                                    </s:if>
                                    <div class="d-flex justify-content-between align-items-center">
                                        <div>
                                            <h6 class="text-muted mb-2">Magazzino</h6>
                                            <h3 class="mb-0">€ <s:property value="getText('{0,number,#,##0}', {valoreMagazzinoTotale})"/></h3>
                                            <small class="<s:if test='giacenzeSottoScorta > 0'>text-danger</s:if><s:else>text-muted</s:else>">
                                                <i class="bi bi-<s:if test='giacenzeSottoScorta > 0'>exclamation-triangle</s:if><s:else>check-circle</s:else>"></i> 
                                                <s:if test="giacenzeSottoScorta > 0"><s:property value="giacenzeSottoScorta"/> Sotto scorta</s:if>
                                                <s:else>OK</s:else>
                                            </small>
                                        </div>
                                        <div class="stat-icon bg-<s:if test='giacenzeSottoScorta > 0'>warning</s:if><s:else>secondary</s:else> bg-opacity-10 text-<s:if test='giacenzeSottoScorta > 0'>warning</s:if><s:else>secondary</s:else>">
                                            <i class="bi bi-box"></i>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>

                    <!-- Stats Cards Row 2 -->
                    <div class="row mb-4">
                        <div class="col-xl-3 col-md-6 mb-3">
                            <div class="card stat-card">
                                <div class="card-body">
                                    <div class="d-flex justify-content-between align-items-center">
                                        <div>
                                            <h6 class="text-muted mb-2">Preventivi Aperti</h6>
                                            <h3 class="mb-0"><s:property value="preventiviAperti"/></h3>
                                            <small class="text-muted"><i class="bi bi-clock"></i> In attesa</small>
                                        </div>
                                        <div class="stat-icon bg-purple bg-opacity-10 text-purple">
                                            <i class="bi bi-file-earmark-text"></i>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>

                        <div class="col-xl-3 col-md-6 mb-3">
                            <div class="card stat-card">
                                <div class="card-body">
                                    <div class="d-flex justify-content-between align-items-center">
                                        <div>
                                            <h6 class="text-muted mb-2">Fatture Anno</h6>
                                            <h3 class="mb-0"><s:property value="fattureEmesseAnno"/></h3>
                                            <small class="text-success"><i class="bi bi-receipt"></i> Emesse</small>
                                        </div>
                                        <div class="stat-icon bg-success bg-opacity-10 text-success">
                                            <i class="bi bi-file-text"></i>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>

                        <div class="col-xl-3 col-md-6 mb-3">
                            <div class="card stat-card">
                                <div class="card-body">
                                    <div class="d-flex justify-content-between align-items-center">
                                        <div>
                                            <h6 class="text-muted mb-2">Clienti Attivi</h6>
                                            <h3 class="mb-0"><s:property value="clientiAttivi"/></h3>
                                            <small class="text-muted"><i class="bi bi-people"></i> Totali</small>
                                        </div>
                                        <div class="stat-icon bg-info bg-opacity-10 text-info">
                                            <i class="bi bi-building"></i>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>

                        <div class="col-xl-3 col-md-6 mb-3">
                            <div class="card stat-card">
                                <div class="card-body">
                                    <div class="d-flex justify-content-between align-items-center">
                                        <div>
                                            <h6 class="text-muted mb-2">Commesse Attive</h6>
                                            <h3 class="mb-0"><s:property value="commesseAttive"/></h3>
                                            <small class="text-primary"><i class="bi bi-gear"></i> In corso</small>
                                        </div>
                                        <div class="stat-icon bg-primary bg-opacity-10 text-primary">
                                            <i class="bi bi-briefcase"></i>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>

                    <!-- Charts Row -->
                    <div class="row mb-4">
                        <div class="col-lg-8 mb-3">
                            <div class="card">
                                <div class="card-header bg-white">
                                    <h5 class="mb-0"><i class="bi bi-graph-up me-2"></i>Fatturato Ultimi 12 Mesi</h5>
                                </div>
                                <div class="card-body">
                                    <div class="chart-container">
                                        <canvas id="revenueChart"></canvas>
                                    </div>
                                </div>
                            </div>
                        </div>

                        <div class="col-lg-4 mb-3">
                            <div class="card">
                                <div class="card-header bg-white">
                                    <h5 class="mb-0"><i class="bi bi-pie-chart me-2"></i>Lead Pipeline</h5>
                                </div>
                                <div class="card-body">
                                    <div class="chart-container">
                                        <canvas id="leadChart"></canvas>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>

                    <!-- Tables Row -->
                    <div class="row">
                        <div class="col-lg-6 mb-3">
                            <div class="card">
                                <div class="card-header bg-white d-flex justify-content-between align-items-center">
                                    <h5 class="mb-0">Ultime Fatture</h5>
                                    <a href="<s:url action='fatture-list'/>" class="btn btn-sm btn-outline-primary">Vedi tutte</a>
                                </div>
                                <div class="card-body p-0">
                                    <div class="list-group list-group-flush">
                                        <s:iterator value="ultimeFatture">
                                            <div class="list-group-item">
                                                <div class="d-flex justify-content-between align-items-center">
                                                    <div>
                                                        <h6 class="mb-0"><s:property value="numero"/></h6>
                                                        <small class="text-muted"><s:property value="cliente.ragioneSociale"/></small>
                                                    </div>
                                                    <div class="text-end">
                                                        <div class="fw-bold text-success">€ <s:property value="getText('{0,number,#,##0.00}', {totale})"/></div>
                                                        <small class="text-muted"><s:date name="dataFattura" format="dd/MM/yyyy"/></small>
                                                    </div>
                                                </div>
                                            </div>
                                        </s:iterator>
                                        <s:if test="ultimeFatture == null || ultimeFatture.size() == 0">
                                            <div class="list-group-item text-center text-muted py-4">
                                                <i class="bi bi-inbox fs-1 d-block mb-2"></i>
                                                Nessuna fattura recente
                                            </div>
                                        </s:if>
                                    </div>
                                </div>
                            </div>
                        </div>

                        <div class="col-lg-6 mb-3">
                            <div class="card">
                                <div class="card-header bg-white d-flex justify-content-between align-items-center">
                                    <h5 class="mb-0">Ultimi Lead</h5>
                                    <a href="<s:url action='lead-list'/>" class="btn btn-sm btn-outline-primary">Vedi tutti</a>
                                </div>
                                <div class="card-body p-0">
                                    <div class="list-group list-group-flush">
                                        <s:iterator value="ultimiLead">
                                            <div class="list-group-item">
                                                <div class="d-flex justify-content-between align-items-center">
                                                    <div>
                                                        <h6 class="mb-0"><s:property value="nomeCompleto"/></h6>
                                                        <small class="text-muted"><s:property value="azienda"/> - <s:property value="email"/></small>
                                                    </div>
                                                    <span class="badge bg-<s:if test='stato.name() == "NUOVO"'>info</s:if><s:elseif test='stato.name() == "CONTATTATO"'>primary</s:elseif><s:elseif test='stato.name() == "QUALIFICATO"'>warning</s:elseif><s:elseif test='stato.name() == "VINTO"'>success</s:elseif><s:else>danger</s:else>">
                                                        <s:property value="stato"/>
                                                    </span>
                                                </div>
                                            </div>
                                        </s:iterator>
                                        <s:if test="ultimiLead == null || ultimiLead.size() == 0">
                                            <div class="list-group-item text-center text-muted py-4">
                                                <i class="bi bi-person-plus fs-1 d-block mb-2"></i>
                                                Nessun lead recente
                                            </div>
                                        </s:if>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>

                    <!-- Preventivi Table -->
                    <div class="row">
                        <div class="col-12">
                            <div class="card">
                                <div class="card-header bg-white d-flex justify-content-between align-items-center">
                                    <h5 class="mb-0">Ultimi Preventivi</h5>
                                    <a href="<s:url action='preventivi-list'/>" class="btn btn-sm btn-outline-primary">Vedi tutti</a>
                                </div>
                                <div class="card-body">
                                    <div class="table-responsive">
                                        <table class="table table-hover">
                                            <thead>
                                                <tr>
                                                    <th>Numero</th>
                                                    <th>Cliente</th>
                                                    <th>Data</th>
                                                    <th>Totale</th>
                                                    <th>Stato</th>
                                                    <th>Azioni</th>
                                                </tr>
                                            </thead>
                                            <tbody>
                                                <s:iterator value="ultimiPreventivi">
                                                    <tr>
                                                        <td><strong><s:property value="numero"/></strong></td>
                                                        <td><s:property value="cliente.ragioneSociale"/></td>
                                                        <td><s:date name="dataPreventivo" format="dd/MM/yyyy"/></td>
                                                        <td>€ <s:property value="getText('{0,number,#,##0.00}', {totale})"/></td>
                                                        <td>
                                                            <span class="badge bg-<s:if test='stato.name() == "BOZZA"'>secondary</s:if><s:elseif test='stato.name() == "INVIATO"'>primary</s:elseif><s:elseif test='stato.name() == "ACCETTATO"'>success</s:elseif><s:else>danger</s:else>">
                                                                <s:property value="stato"/>
                                                            </span>
                                                        </td>
                                                        <td>
                                                            <a href="<s:url action='preventivi-view'><s:param name='id' value='id'/></s:url>" class="btn btn-sm btn-outline-primary">
                                                                <i class="bi bi-eye"></i>
                                                            </a>
                                                        </td>
                                                    </tr>
                                                </s:iterator>
                                                <s:if test="ultimiPreventivi == null || ultimiPreventivi.size() == 0">
                                                    <tr>
                                                        <td colspan="6" class="text-center text-muted">
                                                            Nessun preventivo presente
                                                        </td>
                                                    </tr>
                                                </s:if>
                                            </tbody>
                                        </table>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
    <script>
        // Revenue Chart
        const revenueCtx = document.getElementById('revenueChart').getContext('2d');
        new Chart(revenueCtx, {
            type: 'line',
            data: {
                labels: [<s:iterator value="mesiChart" status="stat">'<s:property/>'<s:if test="!#stat.last">,</s:if></s:iterator>],
                datasets: [{
                    label: 'Fatturato (€)',
                    data: [<s:iterator value="fatturatoMensile" status="stat"><s:property/><s:if test="!#stat.last">,</s:if></s:iterator>],
                    borderColor: 'rgb(75, 192, 192)',
                    backgroundColor: 'rgba(75, 192, 192, 0.1)',
                    tension: 0.4,
                    fill: true
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        display: true,
                        position: 'top'
                    }
                },
                scales: {
                    y: {
                        beginAtZero: true,
                        ticks: {
                            callback: function(value) {
                                return '€ ' + value.toLocaleString();
                            }
                        }
                    }
                }
            }
        });

        // Lead Pipeline Chart
        const leadCtx = document.getElementById('leadChart').getContext('2d');
        new Chart(leadCtx, {
            type: 'doughnut',
            data: {
                labels: ['Nuovo', 'Contattato', 'Qualificato', 'Proposta', 'Vinto'],
                datasets: [{
                    data: [
                        <s:property value="leadPerStato['NUOVO']" default="0"/>,
                        <s:property value="leadPerStato['CONTATTATO']" default="0"/>,
                        <s:property value="leadPerStato['QUALIFICATO']" default="0"/>,
                        <s:property value="leadPerStato['PROPOSTA']" default="0"/>,
                        <s:property value="leadPerStato['VINTO']" default="0"/>
                    ],
                    backgroundColor: [
                        'rgba(13, 202, 240, 0.8)',
                        'rgba(13, 110, 253, 0.8)',
                        'rgba(255, 193, 7, 0.8)',
                        'rgba(25, 135, 84, 0.8)',
                        'rgba(32, 201, 151, 0.8)'
                    ]
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        position: 'bottom'
                    }
                }
            }
        });
    </script>
</body>
</html>
