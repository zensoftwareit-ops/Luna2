<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="s" uri="/struts-tags"%>
<!DOCTYPE html>
<html lang="it">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Centralizzazione eCommerce - Dashboard</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.0/font/bootstrap-icons.css" rel="stylesheet">
    <style>
        .stat-card {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            padding: 20px;
            border-radius: 8px;
            margin-bottom: 20px;
        }
        .stat-card.orders {
            background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
        }
        .stat-card.products {
            background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
        }
        .stat-number {
            font-size: 2.5rem;
            font-weight: bold;
        }
        .platform-badge {
            display: inline-block;
            padding: 5px 10px;
            border-radius: 4px;
            font-size: 0.85rem;
            margin-right: 5px;
        }
        .platform-woocommerce {
            background-color: #96588a;
            color: white;
        }
        .platform-shopify {
            background-color: #96bf48;
            color: white;
        }
        .platform-amazon {
            background-color: #ff9900;
            color: white;
        }
        .platform-ebay {
            background-color: #e53238;
            color: white;
        }
    </style>
</head>
<body>
    <div class="container-fluid mt-4">
        <div class="row mb-4">
            <div class="col-md-8">
                <h1><i class="bi bi-globe"></i> Centralizzazione eCommerce</h1>
            </div>
            <div class="col-md-4 text-end">
                <a href="<s:url action='platforms' namespace='/app/ecommerce'/>" class="btn btn-primary me-2">
                    <i class="bi bi-gear"></i> Configura Piattaforme
                </a>
                <button class="btn btn-success" onclick="syncNow()">
                    <i class="bi bi-arrow-repeat"></i> Sincronizza Ora
                </button>
            </div>
        </div>

        <!-- Piattaforme Connesse -->
        <div class="row mb-4">
            <div class="col-12">
                <div class="card">
                    <div class="card-header bg-light">
                        <h5 class="mb-0"><i class="bi bi-shop"></i> Piattaforme Connesse</h5>
                    </div>
                    <div class="card-body">
                        <s:if test="platforms != null && platforms.size() > 0">
                            <div class="row">
                                <s:iterator value="platforms">
                                    <div class="col-md-3 mb-3">
                                        <div class="card border-light">
                                            <div class="card-body">
                                                <h6 class="card-title"><s:property value="storeName"/></h6>
                                                <span class="platform-badge platform-<s:property value='platformType.toString().toLowerCase()'/>">
                                                    <s:property value="platformType"/>
                                                </span>
                                                <div class="mt-2 small">
                                                    <div><strong>Stato:</strong> <s:if test="isActive"><span class="badge bg-success">Attivo</span></s:if><s:else><span class="badge bg-danger">Inattivo</span></s:else></div>
                                                    <div><strong>Ultimo sync:</strong> <s:property value="lastSync" format="dd/MM/yyyy HH:mm"/></div>
                                                    <div><strong>Stato sync:</strong> <span class="badge bg-<s:if test='lastSyncStatus.toString() == "SUCCESS"'>success</s:if><s:else>danger</s:else>"><s:property value="lastSyncStatus"/></span></div>
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                </s:iterator>
                            </div>
                        </s:if>
                        <s:else>
                            <div class="alert alert-info">
                                Nessuna piattaforma connessa. <a href="<s:url action='platforms' namespace='/app/ecommerce'/>">Aggiungine una</a>
                            </div>
                        </s:else>
                    </div>
                </div>
            </div>
        </div>

        <!-- Statistiche -->
        <div class="row mb-4">
            <div class="col-md-3">
                <div class="stat-card">
                    <div class="stat-number"><s:property value="orders != null ? orders.size() : 0"/></div>
                    <div>Ordini Totali</div>
                </div>
            </div>
            <div class="col-md-3">
                <div class="stat-card products">
                    <div class="stat-number"><s:property value="products != null ? products.size() : 0"/></div>
                    <div>Prodotti Totali</div>
                </div>
            </div>
            <div class="col-md-3">
                <div class="stat-card orders">
                    <div class="stat-number"><s:property value="platforms != null ? platforms.size() : 0"/></div>
                    <div>Piattaforme</div>
                </div>
            </div>
            <div class="col-md-3">
                <div class="stat-card">
                    <div class="stat-number">
                        <s:if test="orders != null">
                            € <s:property value="orders.{^totalAmount}.sum()"/>
                        </s:if>
                        <s:else>
                            €0
                        </s:else>
                    </div>
                    <div>Valore Ordini</div>
                </div>
            </div>
        </div>

        <!-- Ordini Recenti -->
        <div class="row">
            <div class="col-md-6">
                <div class="card">
                    <div class="card-header bg-light">
                        <h5 class="mb-0"><i class="bi bi-box-seam"></i> Ordini Recenti</h5>
                    </div>
                    <div class="card-body">
                        <s:if test="orders != null && orders.size() > 0">
                            <div class="table-responsive">
                                <table class="table table-sm table-hover">
                                    <thead>
                                        <tr>
                                            <th>Ordine</th>
                                            <th>Cliente</th>
                                            <th>Importo</th>
                                            <th>Stato</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        <s:iterator value="orders">
                                            <tr>
                                                <td><small><s:property value="orderNumber"/></small></td>
                                                <td><small><s:property value="customerName"/></small></td>
                                                <td><small><s:property value="totalAmount"/> <s:property value="currency"/></small></td>
                                                <td><small><span class="badge bg-info"><s:property value="orderStatus"/></span></small></td>
                                            </tr>
                                        </s:iterator>
                                    </tbody>
                                </table>
                            </div>
                            <div class="mt-2">
                                <a href="<s:url action='orders' namespace='/app/ecommerce'/>" class="btn btn-sm btn-outline-primary">
                                    Visualizza Tutti
                                </a>
                            </div>
                        </s:if>
                        <s:else>
                            <div class="alert alert-info mb-0">Nessun ordine</div>
                        </s:else>
                    </div>
                </div>
            </div>

            <div class="col-md-6">
                <div class="card">
                    <div class="card-header bg-light">
                        <h5 class="mb-0"><i class="bi bi-bag"></i> Prodotti Recenti</h5>
                    </div>
                    <div class="card-body">
                        <s:if test="products != null && products.size() > 0">
                            <div class="table-responsive">
                                <table class="table table-sm table-hover">
                                    <thead>
                                        <tr>
                                            <th>Prodotto</th>
                                            <th>SKU</th>
                                            <th>Prezzo</th>
                                            <th>Stock</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        <s:iterator value="products">
                                            <tr>
                                                <td><small><s:property value="productName"/></small></td>
                                                <td><small><s:property value="sku"/></small></td>
                                                <td><small><s:property value="price"/> <s:property value="currency"/></small></td>
                                                <td><small><span class="badge bg-<s:if test='stockQuantity > 10'>success</s:if><s:elseif test='stockQuantity > 0'>warning</s:elseif><s:else>danger</s:else>"><s:property value="stockQuantity"/></span></small></td>
                                            </tr>
                                        </s:iterator>
                                    </tbody>
                                </table>
                            </div>
                            <div class="mt-2">
                                <a href="<s:url action='products' namespace='/app/ecommerce'/>" class="btn btn-sm btn-outline-primary">
                                    Visualizza Catalogo
                                </a>
                            </div>
                        </s:if>
                        <s:else>
                            <div class="alert alert-info mb-0">Nessun prodotto</div>
                        </s:else>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
    <script>
        function syncNow() {
            if (confirm('Avviare la sincronizzazione con tutte le piattaforme?')) {
                fetch('<s:url action="sync-now" namespace="/app/ecommerce"/>', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    }
                })
                .then(response => response.json())
                .then(data => {
                    if (data.success) {
                        alert('Sincronizzazione avviata. La pagina si aggiornerà tra poco.');
                        setTimeout(() => location.reload(), 2000);
                    } else {
                        alert('Errore: ' + data.message);
                    }
                })
                .catch(error => alert('Errore: ' + error));
            }
        }
    </script>
</body>
</html>
