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
    <style>
        .stat-card {
            border: none;
            border-radius: 12px;
            box-shadow: 0 2px 12px rgba(0,0,0,0.08);
            transition: transform 0.3s;
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

                    <!-- Stats Cards -->
                    <div class="row mb-4">
                        <div class="col-md-3 mb-3">
                            <div class="card stat-card">
                                <div class="card-body">
                                    <div class="d-flex justify-content-between align-items-center">
                                        <div>
                                            <h6 class="text-muted mb-2">Fatturato Mese</h6>
                                            <h3 class="mb-0">€ <s:property value="fatturatoMese"/></h3>
                                            <small class="text-success"><i class="bi bi-arrow-up"></i> +18%</small>
                                        </div>
                                        <div class="stat-icon bg-success bg-opacity-10 text-success">
                                            <i class="bi bi-currency-euro"></i>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>

                        <div class="col-md-3 mb-3">
                            <div class="card stat-card">
                                <div class="card-body">
                                    <div class="d-flex justify-content-between align-items-center">
                                        <div>
                                            <h6 class="text-muted mb-2">Preventivi Aperti</h6>
                                            <h3 class="mb-0"><s:property value="preventiviAperti"/></h3>
                                            <small class="text-info"><i class="bi bi-dash"></i> In attesa</small>
                                        </div>
                                        <div class="stat-icon bg-primary bg-opacity-10 text-primary">
                                            <i class="bi bi-file-earmark-text"></i>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>

                        <div class="col-md-3 mb-3">
                            <div class="card stat-card">
                                <div class="card-body">
                                    <div class="d-flex justify-content-between align-items-center">
                                        <div>
                                            <h6 class="text-muted mb-2">Clienti Attivi</h6>
                                            <h3 class="mb-0"><s:property value="clientiAttivi"/></h3>
                                            <small class="text-muted">Totali</small>
                                        </div>
                                        <div class="stat-icon bg-info bg-opacity-10 text-info">
                                            <i class="bi bi-people"></i>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>

                        <div class="col-md-3 mb-3">
                            <div class="card stat-card">
                                <div class="card-body">
                                    <div class="d-flex justify-content-between align-items-center">
                                        <div>
                                            <h6 class="text-muted mb-2">Scadenze Oggi</h6>
                                            <h3 class="mb-0">5</h3>
                                            <small class="text-warning"><i class="bi bi-exclamation-triangle"></i> Attenzione</small>
                                        </div>
                                        <div class="stat-icon bg-warning bg-opacity-10 text-warning">
                                            <i class="bi bi-calendar-event"></i>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>

                    <!-- Recent Documents -->
                    <div class="row">
                        <div class="col-md-12">
                            <div class="card">
                                <div class="card-header bg-white">
                                    <h5 class="mb-0">Ultimi Preventivi</h5>
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
                                                        <td>€ <s:property value="totale"/></td>
                                                        <td>
                                                            <span class="badge bg-<s:if test='stato.name() == "BOZZA"'>secondary</s:if><s:elseif test='stato.name() == "INVIATO"'>primary</s:elseif><s:elseif test='stato.name() == "ACCETTATO"'>success</s:elseif><s:else>danger</s:else>">
                                                                <s:property value="stato"/>
                                                            </span>
                                                        </td>
                                                        <td>
                                                            <a href="#" class="btn btn-sm btn-outline-primary">
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
</body>
</html>
