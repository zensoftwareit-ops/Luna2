<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="s" uri="/struts-tags"%>
<!DOCTYPE html>
<html lang="it">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Ordini - Centralizzazione eCommerce</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.0/font/bootstrap-icons.css" rel="stylesheet">
</head>
<body>
    <div class="container-fluid mt-4">
        <div class="row mb-4">
            <div class="col-md-8">
                <h1><i class="bi bi-box-seam"></i> Ordini eCommerce</h1>
            </div>
            <div class="col-md-4 text-end">
                <a href="<s:url action='dashboard' namespace='/app/ecommerce'/>" class="btn btn-secondary">
                    <i class="bi bi-arrow-left"></i> Indietro
                </a>
            </div>
        </div>

        <!-- Filtri -->
        <div class="card mb-4">
            <div class="card-header bg-light">
                <h5 class="mb-0">Filtri</h5>
            </div>
            <div class="card-body">
                <form method="get" action="<s:url action='orders' namespace='/app/ecommerce'/>">
                    <div class="row">
                        <div class="col-md-4">
                            <label class="form-label">Piattaforma</label>
                            <select name="platform" class="form-select">
                                <option value="">Tutte</option>
                                <option value="woocommerce" <s:if test='platform == "woocommerce"'>selected</s:if>>WooCommerce</option>
                                <option value="shopify" <s:if test='platform == "shopify"'>selected</s:if>>Shopify</option>
                                <option value="amazon" <s:if test='platform == "amazon"'>selected</s:if>>Amazon</option>
                                <option value="ebay" <s:if test='platform == "ebay"'>selected</s:if>>eBay</option>
                            </select>
                        </div>
                        <div class="col-md-4">
                            <label class="form-label">Stato Ordine</label>
                            <select name="status" class="form-select">
                                <option value="">Tutti</option>
                                <option value="pending" <s:if test='status == "pending"'>selected</s:if>>In Sospeso</option>
                                <option value="processing" <s:if test='status == "processing"'>selected</s:if>>In Elaborazione</option>
                                <option value="completed" <s:if test='status == "completed"'>selected</s:if>>Completato</option>
                                <option value="cancelled" <s:if test='status == "cancelled"'>selected</s:if>>Annullato</option>
                            </select>
                        </div>
                        <div class="col-md-4">
                            <label class="form-label">&nbsp;</label>
                            <button type="submit" class="btn btn-primary w-100">
                                <i class="bi bi-search"></i> Filtra
                            </button>
                        </div>
                    </div>
                </form>
            </div>
        </div>

        <!-- Tabella Ordini -->
        <div class="card">
            <div class="card-header bg-light">
                <h5 class="mb-0">Elenco Ordini</h5>
            </div>
            <div class="card-body">
                <s:if test="orders != null && orders.size() > 0">
                    <div class="table-responsive">
                        <table class="table table-hover table-striped">
                            <thead class="table-light">
                                <tr>
                                    <th>Numero Ordine</th>
                                    <th>Piattaforma</th>
                                    <th>Cliente</th>
                                    <th>Email</th>
                                    <th>Data</th>
                                    <th>Importo</th>
                                    <th>Stato</th>
                                    <th>Azioni</th>
                                </tr>
                            </thead>
                            <tbody>
                                <s:iterator value="orders">
                                    <tr>
                                        <td>
                                            <strong><s:property value="orderNumber"/></strong>
                                            <br/>
                                            <small class="text-muted"><s:property value="externalOrderId"/></small>
                                        </td>
                                        <td>
                                            <span class="badge bg-info"><s:property value="platformType.toString().toUpperCase()"/></span>
                                        </td>
                                        <td><s:property value="customerName"/></td>
                                        <td><small><s:property value="customerEmail"/></small></td>
                                        <td><s:property value="orderDate" format="dd/MM/yyyy HH:mm"/></td>
                                        <td>
                                            <strong><s:property value="totalAmount"/> <s:property value="currency"/></strong>
                                        </td>
                                        <td>
                                            <span class="badge bg-<s:if test='orderStatus == "completed" || orderStatus == "processing"'>success</s:if><s:elseif test='orderStatus == "pending"'>warning</s:elseif><s:else>danger</s:else>">
                                                <s:property value="orderStatus"/>
                                            </span>
                                        </td>
                                        <td>
                                            <button class="btn btn-sm btn-info" onclick="viewDetails(<s:property value='id'/>)">
                                                <i class="bi bi-eye"></i> Dettagli
                                            </button>
                                        </td>
                                    </tr>
                                </s:iterator>
                            </tbody>
                        </table>
                    </div>

                    <!-- Paginazione -->
                    <nav aria-label="Page navigation" class="mt-4">
                        <ul class="pagination justify-content-center">
                            <li class="page-item <s:if test='currentPage <= 1'>disabled</s:if>">
                                <a class="page-link" href="<s:url action='orders' namespace='/app/ecommerce'><s:param name='currentPage'>1</s:param></s:url>">Primo</a>
                            </li>
                            <li class="page-item <s:if test='currentPage <= 1'>disabled</s:if>">
                                <a class="page-link" href="<s:url action='orders' namespace='/app/ecommerce'><s:param name='currentPage'><s:property value='currentPage - 1'/></s:param></s:url>">Precedente</a>
                            </li>
                            <li class="page-item active">
                                <span class="page-link">Pagina <s:property value="currentPage"/></span>
                            </li>
                            <li class="page-item">
                                <a class="page-link" href="<s:url action='orders' namespace='/app/ecommerce'><s:param name='currentPage'><s:property value='currentPage + 1'/></s:param></s:url>">Successiva</a>
                            </li>
                        </ul>
                    </nav>
                </s:if>
                <s:else>
                    <div class="alert alert-info mb-0">
                        Nessun ordine trovato. <a href="<s:url action='dashboard' namespace='/app/ecommerce'/>">Torna alla dashboard</a>
                    </div>
                </s:else>
            </div>
        </div>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
    <script>
        function viewDetails(orderId) {
            alert('Dettagli ordine: ' + orderId);
            // Implementare modale con dettagli
        }
    </script>
</body>
</html>
