<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="s" uri="/struts-tags" %>
<!DOCTYPE html>
<html lang="it">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Magazzino - Luna2</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.10.0/font/bootstrap-icons.css">
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
                <a class="nav-link" href="<s:url action='dashboard' namespace='/app'/>">
                    <i class="bi bi-speedometer2 me-2"></i>Dashboard
                </a>
            </li>
            <s:if test="#session.enabledModules['CORE']">
                <li class="nav-item">
                    <a class="nav-link" href="<s:url action='list' namespace='/app/clienti'/>">
                        <i class="bi bi-people me-2"></i>Clienti
                    </a>
                </li>
                <li class="nav-item">
                    <a class="nav-link" href="<s:url action='list' namespace='/app/fornitori'/>">
                        <i class="bi bi-truck me-2"></i>Fornitori
                    </a>
                </li>
            </s:if>
            <s:if test="#session.enabledModules['CRM']">
                <li class="nav-item">
                    <a class="nav-link" href="<s:url action='list' namespace='/app/leads'/>">
                        <i class="bi bi-graph-up-arrow me-2"></i>CRM / Lead
                    </a>
                </li>
            </s:if>
            <s:if test="#session.enabledModules['CORE']">
                <li class="nav-item">
                    <a class="nav-link" href="<s:url action='list' namespace='/app/prodotti'/>">
                        <i class="bi bi-box-seam me-2"></i>Prodotti
                    </a>
                </li>
                <li class="nav-item mt-3">
                    <h6 class="text-white-50 text-uppercase small px-3">Documenti</h6>
                </li>
                <li class="nav-item">
                    <a class="nav-link" href="<s:url action='preventivi' namespace='/app/documenti'/>">
                        <i class="bi bi-file-earmark-text me-2"></i>Preventivi
                    </a>
                </li>
                <li class="nav-item">
                    <a class="nav-link" href="<s:url action='ordini' namespace='/app/documenti'/>">
                        <i class="bi bi-cart-check me-2"></i>Ordini
                    </a>
                </li>
                <li class="nav-item">
                    <a class="nav-link" href="<s:url action='ddt' namespace='/app/documenti'/>">
                        <i class="bi bi-truck-flatbed me-2"></i>DDT
                    </a>
                </li>
                <li class="nav-item">
                    <a class="nav-link" href="<s:url action='fatture' namespace='/app/documenti'/>">
                        <i class="bi bi-receipt me-2"></i>Fatture
                    </a>
                </li>
            </s:if>
            <s:if test="#session.enabledModules['MAGAZZINO']">
                <li class="nav-item mt-3">
                    <a class="nav-link active" href="<s:url action='list' namespace='/app/magazzino'/>">
                        <i class="bi bi-archive me-2"></i>Magazzino
                    </a>
                </li>
            </s:if>
            <s:if test="#session.enabledModules['PRODUZIONE']">
                <li class="nav-item">
                    <a class="nav-link" href="<s:url action='commesse' namespace='/app/produzione'/>">
                        <i class="bi bi-gear me-2"></i>Produzione / Commesse
                    </a>
                </li>
            </s:if>
            <s:if test="#session.enabledModules['AI']">
                <li class="nav-item">
                    <a class="nav-link" href="<s:url action='index' namespace='/app/ai'/>">
                        <i class="bi bi-cpu me-2"></i>Modulo AI
                    </a>
                </li>
            </s:if>
            <s:if test="#session.enabledModules['CORE']">
                <li class="nav-item">
                    <a class="nav-link" href="<s:url action='vendite' namespace='/app/report'/>">
                        <i class="bi bi-bar-chart me-2"></i>Report
                    </a>
                </li>
            </s:if>
            <s:if test="#session.currentUser.username.toLowerCase() == 'admin'">
                <li class="nav-item mt-3">
                    <a class="nav-link" href="<s:url action='moduli' namespace='/app/admin'/>">
                        <i class="bi bi-sliders me-2"></i>Moduli
                    </a>
                </li>
            </s:if>
            <li class="nav-item mt-5">
                <a class="nav-link" href="<s:url action='logout' namespace='/'/>">
                    <i class="bi bi-box-arrow-right me-2"></i>Logout
                </a>
            </li>
        </ul>

        <div class="mt-auto pt-4 text-center">
            <small class="text-white-50">
                <i class="bi bi-person-circle me-1"></i>
                ${sessionScope.currentUser.nomeCompleto}
            </small>
        </div>
    </div>

    <div class="content-wrapper p-4">
        <div class="container-fluid">
            <h1><i class="bi bi-boxes me-2"></i>Giacenze Magazzino</h1>
            
            <!-- Statistiche Rapide -->
            <div class="row mb-4 mt-4">
                <div class="col-md-4">
                    <div class="card text-white" style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);">
                        <div class="card-body">
                            <h6 class="card-title">Valore Totale Magazzino</h6>
                            <h3><i class="bi bi-currency-euro"></i>
                                <s:if test="valoreTotale != null">
                                    <s:property value="getText('{0,number,#,##0.00}', {valoreTotale})"/>
                                </s:if>
                                <s:else>0.00</s:else>
                            </h3>
                        </div>
                    </div>
                </div>
                <div class="col-md-4">
                    <div class="card text-white" style="background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);">
                        <div class="card-body">
                            <h6 class="card-title">Prodotti in Magazzino</h6>
                            <h3><i class="bi bi-box-seam"></i> <s:property value="giacenze.size()"/></h3>
                        </div>
                    </div>
                </div>
                <div class="col-md-4">
                    <div class="card text-white" style="background: linear-gradient(135deg, #fa709a 0%, #fee140 100%);">
                        <div class="card-body">
                            <h6 class="card-title">Sotto Scorta</h6>
                            <h3><i class="bi bi-exclamation-triangle"></i> <s:property value="sottoScorta.size()"/></h3>
                        </div>
                    </div>
                </div>
            </div>

            <!-- Alert -->
            <s:if test="sottoScorta != null && !sottoScorta.isEmpty()">
                <div class="alert alert-warning alert-dismissible fade show" role="alert">
                    <i class="bi bi-exclamation-triangle-fill me-2"></i>
                    <strong>Attenzione!</strong> <s:property value="sottoScorta.size()"/> prodotti sotto scorta minima.
                    <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
                </div>
            </s:if>

            <!-- Pulsanti Azioni -->
            <div class="row mb-3">
                <div class="col-12">
                    <div class="btn-group" role="group">
                        <a href="<s:url action='nuovoMovimento' namespace='/app/magazzino'><s:param name='tipoMovimento'>CARICO</s:param></s:url>" class="btn btn-success">
                            <i class="bi bi-plus-circle me-2"></i>Carico
                        </a>
                        <a href="<s:url action='nuovoMovimento' namespace='/app/magazzino'><s:param name='tipoMovimento'>SCARICO</s:param></s:url>" class="btn btn-danger">
                            <i class="bi bi-dash-circle me-2"></i>Scarico
                        </a>
                        <a href="<s:url action='scanner' namespace='/app/magazzino'/>" class="btn btn-info">
                            <i class="bi bi-upc-scan me-2"></i>Scanner Barcode
                        </a>
                        <a href="<s:url action='stampaEtichette' namespace='/app/magazzino'/>" class="btn btn-success">
                            <i class="bi bi-printer me-2"></i>Stampa Etichette
                        </a>
                        <a href="<s:url action='storicoMovimenti' namespace='/app/magazzino'/>" class="btn btn-secondary">
                            <i class="bi bi-clock-history me-2"></i>Storico Movimenti
                        </a>
                    </div>
                </div>
            </div>

            <!-- Tabella -->
            <div class="card shadow-sm">
                <div class="card-header bg-white">
                    <h5>Elenco Giacenze</h5>
                </div>
                <div class="card-body">
                    <table class="table table-striped table-hover">
                        <thead>
                            <tr>
                                <th>Codice</th>
                                <th>Prodotto</th>
                                <th>Categoria</th>
                                <th class="text-end">Giacenza</th>
                                <th class="text-end">Disponibile</th>
                                <th class="text-end">Valore €</th>
                            </tr>
                        </thead>
                        <tbody>
                            <s:iterator value="giacenze" var="g">
                                <tr class="<s:if test='#g.giacenzaDisponibile &lt; #g.prodotto.giacenzaMinima && #g.prodotto.giacenzaMinima > 0'>table-warning</s:if>">
                                    <td><code><s:property value="#g.prodotto.codice"/></code></td>
                                    <td><strong><s:property value="#g.prodotto.nome"/></strong></td>
                                    <td><s:property value="#g.prodotto.categoria"/></td>
                                    <td class="text-end"><s:property value="#g.giacenzaAttuale"/></td>
                                    <td class="text-end"><s:property value="#g.giacenzaDisponibile"/></td>
                                    <td class="text-end">
                                        <s:if test="#g.valoreMagazzino != null">
                                            <s:property value="getText('{0,number,#,##0.00}', {#g.valoreMagazzino})"/>
                                        </s:if>
                                    </td>
                                </tr>
                            </s:iterator>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
