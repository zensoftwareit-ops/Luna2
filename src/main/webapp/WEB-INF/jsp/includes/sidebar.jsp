<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="s" uri="/struts-tags" %>
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

<div class="container-fluid">
    <div class="row">
        <div class="col-md-2 sidebar p-4">
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
                        <a class="nav-link" href="<s:url action='list' namespace='/app/lead'/>">
                            <i class="bi bi-graph-up-arrow me-2"></i>CRM / Lead
                        </a>
                    </li>
                    <s:if test="#session.enabledModules['CRM_BROKER_AUTO']">
                        <li class="nav-item">
                            <a class="nav-link" href="<s:url action='list' namespace='/app/lead'/>">
                                <i class="bi bi-car-front me-2"></i>CRM Broker Auto
                            </a>
                        </li>
                    </s:if>
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
                        <a class="nav-link" href="<s:url action='list' namespace='/app/magazzino'/>">
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
