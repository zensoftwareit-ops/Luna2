<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="s" uri="/struts-tags" %>
<!DOCTYPE html>
<html lang="it">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Storico Movimenti - Luna2</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.0/font/bootstrap-icons.css">
    <link rel="stylesheet" href="https://cdn.datatables.net/1.13.4/css/dataTables.bootstrap5.min.css">
    <style>
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
        }
        .sidebar {
            position: fixed;
            top: 0;
            left: 0;
            height: 100vh;
            width: 250px;
            background: linear-gradient(180deg, #667eea 0%, #764ba2 100%);
            overflow-y: auto;
            z-index: 1000;
        }
        .sidebar .nav-link {
            color: rgba(255,255,255,0.8);
            padding: 12px 20px;
            border-radius: 8px;
            margin: 4px 0;
            transition: all 0.3s;
        }
        .sidebar .nav-link:hover,
        .sidebar .nav-link.active {
            background: rgba(255,255,255,0.2);
            color: white;
        }
        .logo-text {
            font-size: 1.8rem;
            font-weight: 700;
            color: white;
        }
        .content-wrapper {
            margin-left: 250px;
            width: calc(100% - 250px);
            background-color: #f8f9fa;
            min-height: 100vh;
        }
        @media (max-width: 991.98px) {
            .sidebar {
                position: static;
                width: 100%;
                height: auto;
            }
            .content-wrapper {
                margin-left: 0;
            }
        }
    </style>
</head>
<body>
<div class="sidebar p-4">
    <div class="text-center mb-4">
        <img src="${pageContext.request.contextPath}/assets/images/logo.png" alt="Luna2" class="img-fluid mb-2" style="max-height: 60px;">
        <p class="text-white-50 small">Gestionale Cloud</p>
    </div>

    <ul class="nav flex-column">
        <li class="nav-item">
            <a class="nav-link" href="<s:url action='dashboard' namespace='/app'/>"><i class="bi bi-speedometer2 me-2"></i>Dashboard</a>
        </li>
        <s:if test="#session.enabledModules['CORE']">
            <li class="nav-item">
                <a class="nav-link" href="<s:url action='list' namespace='/app/clienti'/>"><i class="bi bi-people me-2"></i>Clienti</a>
            </li>
            <li class="nav-item">
                <a class="nav-link" href="<s:url action='list' namespace='/app/fornitori'/>"><i class="bi bi-truck me-2"></i>Fornitori</a>
            </li>
            <li class="nav-item">
                <a class="nav-link" href="<s:url action='list' namespace='/app/prodotti'/>"><i class="bi bi-box-seam me-2"></i>Prodotti</a>
            </li>
                <li class="nav-item mt-3">
                    <h6 class="text-white-50 text-uppercase small px-3">Documenti</h6>
                </li>
                <li class="nav-item">
                    <a class="nav-link" href="<s:url action='preventivi' namespace='/app/documenti'/>"><i class="bi bi-file-earmark-text me-2"></i>Preventivi</a>
                </li>
                <li class="nav-item">
                    <a class="nav-link" href="<s:url action='ordini' namespace='/app/documenti'/>"><i class="bi bi-cart-check me-2"></i>Ordini</a>
                </li>
                <li class="nav-item">
                    <a class="nav-link" href="<s:url action='ddt' namespace='/app/documenti'/>"><i class="bi bi-truck-flatbed me-2"></i>DDT</a>
                </li>
                <li class="nav-item">
                    <a class="nav-link" href="<s:url action='fatture' namespace='/app/documenti'/>"><i class="bi bi-receipt me-2"></i>Fatture</a>
                </li>
            </s:if>
            <s:if test="#session.enabledModules['MAGAZZINO']">
                <li class="nav-item mt-3">
                    <a class="nav-link active" href="<s:url action='list' namespace='/app/magazzino'/>"><i class="bi bi-archive me-2"></i>Magazzino</a>
                </li>
            </s:if>
            <s:if test="#session.enabledModules['PRODUZIONE']">
                <li class="nav-item">
                    <a class="nav-link" href="<s:url action='commesse' namespace='/app/produzione'/>"><i class="bi bi-gear me-2"></i>Produzione</a>
                </li>
            </s:if>
            <s:if test="#session.enabledModules['AI']">
                <li class="nav-item">
                    <a class="nav-link" href="<s:url action='index' namespace='/app/ai'/>"><i class="bi bi-cpu me-2"></i>Modulo AI</a>
                </li>
            </s:if>
            <s:if test="#session.enabledModules['CORE']">
                <li class="nav-item">
                    <a class="nav-link" href="<s:url action='vendite' namespace='/app/report'/>"><i class="bi bi-bar-chart me-2"></i>Report</a>
                </li>
            </s:if>
            <s:if test="#session.currentUser != null && #session.currentUser.username != null && #session.currentUser.username.equalsIgnoreCase('admin')">
                <li class="nav-item mt-3">
                    <a class="nav-link" href="<s:url action='moduli' namespace='/app/admin'/>"><i class="bi bi-sliders me-2"></i>Moduli</a>
                </li>
            </s:if>
            <li class="nav-item mt-5">
                <a class="nav-link" href="<s:url action='logout' namespace='/'/>"><i class="bi bi-box-arrow-right me-2"></i>Logout</a>
            </li>
        </ul>

        <div class="mt-auto pt-4 text-center">
            <small class="text-white-50"><i class="bi bi-person-circle me-1"></i>${sessionScope.currentUser.nomeCompleto}</small>
        </div>
    </div>

    <div class="col-md-10 content-wrapper p-4">
        <div class="container-fluid">
        <div class="d-flex justify-content-between align-items-center mb-4">
            <h1 class="h3">
                <i class="bi bi-clock-history me-2"></i>Storico Movimenti Magazzino
            </h1>
            <a href="<s:url action='list' namespace='/app/magazzino'/>" class="btn btn-outline-secondary">
                <i class="bi bi-arrow-left me-2"></i>Torna a Giacenze
            </a>
        </div>

        <div class="card shadow-sm">
            <div class="card-body">
                <table id="tableMovimenti" class="table table-striped table-hover">
                    <thead>
                        <tr>
                            <th>Data</th>
                            <th>Tipo</th>
                            <th>Prodotto</th>
                                    <th>Causale</th>
                                    <th class="text-end">Quantità</th>
                                    <th class="text-end">Giacenza Prima</th>
                                    <th class="text-end">Giacenza Dopo</th>
                                    <th class="text-end">Costo Unit.</th>
                                    <th>Documento</th>
                                    <th>Note</th>
                                    <th>Utente</th>
                                </tr>
                            </thead>
                            <tbody>
                                <s:iterator value="movimenti" var="m">
                                    <tr>
                                        <td>
                                            <s:date name="#m.dataMovimento" format="dd/MM/yyyy"/>
                                            <br/>
                                            <small class="text-muted">
                                                <s:date name="#m.dataCreazione" format="HH:mm"/>
                                            </small>
                                        </td>
                                        <td>
                                            <s:if test="#m.tipoMovimento.name() == 'CARICO'">
                                                <span class="badge bg-success">
                                                    <i class="bi bi-plus-circle me-1"></i>
                                                    <s:property value="#m.tipoMovimento.descrizione"/>
                                                </span>
                                            </s:if>
                                            <s:elseif test="#m.tipoMovimento.name() == 'SCARICO'">
                                                <span class="badge bg-danger">
                                                    <i class="bi bi-dash-circle me-1"></i>
                                                    <s:property value="#m.tipoMovimento.descrizione"/>
                                                </span>
                                            </s:elseif>
                                            <s:elseif test="#m.tipoMovimento.name() == 'RETTIFICA'">
                                                <span class="badge bg-warning text-dark">
                                                    <i class="bi bi-pencil-square me-1"></i>
                                                    <s:property value="#m.tipoMovimento.descrizione"/>
                                                </span>
                                            </s:elseif>
                                            <s:else>
                                                <span class="badge bg-info">
                                                    <s:property value="#m.tipoMovimento.descrizione"/>
                                                </span>
                                            </s:else>
                                        </td>
                                        <td>
                                            <strong><s:property value="#m.prodotto.nome"/></strong>
                                            <br/>
                                            <small class="text-muted">
                                                <code><s:property value="#m.prodotto.codice"/></code>
                                            </small>
                                        </td>
                                        <td>
                                            <s:property value="#m.causale"/>
                                        </td>
                                        <td class="text-end">
                                            <s:if test="#m.quantita > 0">
                                                <span class="text-success fw-bold">
                                                    +<s:property value="#m.quantita"/>
                                                </span>
                                            </s:if>
                                            <s:else>
                                                <span class="text-danger fw-bold">
                                                    <s:property value="#m.quantita"/>
                                                </span>
                                            </s:else>
                                        </td>
                                        <td class="text-end">
                                            <s:if test="#m.giacenzaPrima != null">
                                                <s:property value="#m.giacenzaPrima"/>
                                            </s:if>
                                            <s:else>-</s:else>
                                        </td>
                                        <td class="text-end">
                                            <strong><s:property value="#m.giacenzaDopo"/></strong>
                                        </td>
                                        <td class="text-end">
                                            <s:if test="#m.costoUnitario != null">
                                                € <s:property value="getText('{0,number,#,##0.00}', {#m.costoUnitario})"/>
                                            </s:if>
                                            <s:else>-</s:else>
                                        </td>
                                        <td>
                                            <s:if test="#m.documentoTipo != null">
                                                <small>
                                                    <s:property value="#m.documentoTipo"/>
                                                    <s:if test="#m.documentoNumero != null">
                                                        <br/><code><s:property value="#m.documentoNumero"/></code>
                                                    </s:if>
                                                </small>
                                            </s:if>
                                            <s:else>-</s:else>
                                        </td>
                                        <td>
                                            <s:if test="#m.note != null && #m.note.length() > 0">
                                                <button type="button" class="btn btn-sm btn-outline-info" 
                                                        data-bs-toggle="tooltip" 
                                                        title="<s:property value='#m.note' escapeHtml='true'/>">
                                                    <i class="bi bi-sticky"></i>
                                                </button>
                                            </s:if>
                                            <s:else>-</s:else>
                                        </td>
                                        <td>
                                            <s:if test="#m.createdBy != null">
                                                <small><s:property value="#m.createdBy.username"/></small>
                                            </s:if>
                                            <s:else>-</s:else>
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

    
    <script src="https://cdn.datatables.net/1.13.4/js/jquery.dataTables.min.js"></script>
    <script src="https://cdn.datatables.net/1.13.4/js/dataTables.bootstrap5.min.js"></script>
    <script>
        $(document).ready(function() {
            $('#tableMovimenti').DataTable({
                language: {
                    url: 'https://cdn.datatables.net/plug-ins/1.13.4/i18n/it-IT.json'
                },
                order: [[0, 'desc']],
                pageLength: 50
            });

            // Attiva tooltips
            var tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
            var tooltipList = tooltipTriggerList.map(function (tooltipTriggerEl) {
                return new bootstrap.Tooltip(tooltipTriggerEl);
            });
        });
    </script>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
