<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="s" uri="/struts-tags"%>
<!DOCTYPE html>
<html lang="it">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Prodotti - Centralizzazione eCommerce</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.0/font/bootstrap-icons.css" rel="stylesheet">
    <style>
        .product-card {
            transition: transform 0.2s;
        }
        .product-card:hover {
            transform: translateY(-5px);
            box-shadow: 0 4px 12px rgba(0,0,0,0.15);
        }
    </style>
</head>
<body>
    <div class="container-fluid mt-4">
        <div class="row mb-4">
            <div class="col-md-8">
                <h1><i class="bi bi-bag"></i> Catalogo Prodotti</h1>
            </div>
            <div class="col-md-4 text-end">
                <a href="<s:url action='dashboard' namespace='/app/ecommerce'/>" class="btn btn-secondary">
                    <i class="bi bi-arrow-left"></i> Indietro
                </a>
            </div>
        </div>

        <!-- Ricerca e Filtri -->
        <div class="card mb-4">
            <div class="card-header bg-light">
                <h5 class="mb-0">Ricerca e Filtri</h5>
            </div>
            <div class="card-body">
                <form method="get" action="<s:url action='products' namespace='/app/ecommerce'/>">
                    <div class="row">
                        <div class="col-md-5">
                            <label class="form-label">Ricerca Prodotto</label>
                            <input type="text" name="search" class="form-control" placeholder="Nome o SKU..." value="<s:property value='search'/>">
                        </div>
                        <div class="col-md-5">
                            <label class="form-label">Piattaforma</label>
                            <select name="platform" class="form-select">
                                <option value="">Tutte</option>
                                <option value="woocommerce" <s:if test='platform == "woocommerce"'>selected</s:if>>WooCommerce</option>
                                <option value="shopify" <s:if test='platform == "shopify"'>selected</s:if>>Shopify</option>
                                <option value="amazon" <s:if test='platform == "amazon"'>selected</s:if>>Amazon</option>
                                <option value="ebay" <s:if test='platform == "ebay"'>selected</s:if>>eBay</option>
                            </select>
                        </div>
                        <div class="col-md-2">
                            <label class="form-label">&nbsp;</label>
                            <button type="submit" class="btn btn-primary w-100">
                                <i class="bi bi-search"></i> Cerca
                            </button>
                        </div>
                    </div>
                </form>
            </div>
        </div>

        <!-- Griglia Prodotti -->
        <div class="row">
            <s:if test="products != null && products.size() > 0">
                <s:iterator value="products">
                    <div class="col-md-3 mb-4">
                        <div class="card product-card h-100">
                            <div class="card-body">
                                <h6 class="card-title">
                                    <s:property value="productName"/>
                                </h6>
                                <p class="card-text text-muted small">
                                    <s:if test="description != null">
                                        <s:property value="description.substring(0, Math.min(100, description.length()))"/>...
                                    </s:if>
                                    <s:else>
                                        <em>Nessuna descrizione</em>
                                    </s:else>
                                </p>

                                <hr/>

                                <div class="mb-2">
                                    <strong>SKU:</strong> <small class="text-muted"><s:property value="sku"/></small>
                                </div>

                                <div class="mb-2">
                                    <strong>Prezzo:</strong>
                                    <span class="badge bg-success">
                                        <s:property value="price"/> <s:property value="currency"/>
                                    </span>
                                </div>

                                <div class="mb-2">
                                    <strong>Stock:</strong>
                                    <span class="badge bg-<s:if test='stockQuantity > 10'>success</s:if><s:elseif test='stockQuantity > 0'>warning</s:elseif><s:else>danger</s:else>">
                                        <s:property value="stockQuantity"/> pz
                                    </span>
                                </div>

                                <div class="mb-3">
                                    <strong>Piattaforma:</strong>
                                    <br/>
                                    <span class="badge bg-info">
                                        <s:property value="platformType.toString().toUpperCase()"/>
                                    </span>
                                </div>

                                <div class="small text-muted">
                                    <strong>Codice Esterno:</strong> <s:property value="externalProductId"/>
                                </div>
                            </div>
                            <div class="card-footer bg-light">
                                <button class="btn btn-sm btn-outline-primary w-100" onclick="viewProductDetails(<s:property value='id'/>)">
                                    <i class="bi bi-eye"></i> Dettagli
                                </button>
                            </div>
                        </div>
                    </div>
                </s:iterator>
            </s:if>
            <s:else>
                <div class="col-12">
                    <div class="alert alert-info">
                        Nessun prodotto trovato. <a href="<s:url action='dashboard' namespace='/app/ecommerce'/>">Torna alla dashboard</a>
                    </div>
                </div>
            </s:else>
        </div>

        <!-- Paginazione -->
        <s:if test="products != null && products.size() > 0">
            <nav aria-label="Page navigation" class="mt-4">
                <ul class="pagination justify-content-center">
                    <li class="page-item <s:if test='currentPage <= 1'>disabled</s:if>">
                        <a class="page-link" href="<s:url action='products' namespace='/app/ecommerce'><s:param name='currentPage'>1</s:param></s:url>">Primo</a>
                    </li>
                    <li class="page-item <s:if test='currentPage <= 1'>disabled</s:if>">
                        <a class="page-link" href="<s:url action='products' namespace='/app/ecommerce'><s:param name='currentPage'><s:property value='currentPage - 1'/></s:param></s:url>">Precedente</a>
                    </li>
                    <li class="page-item active">
                        <span class="page-link">Pagina <s:property value="currentPage"/></span>
                    </li>
                    <li class="page-item">
                        <a class="page-link" href="<s:url action='products' namespace='/app/ecommerce'><s:param name='currentPage'><s:property value='currentPage + 1'/></s:param></s:url>">Successiva</a>
                    </li>
                </ul>
            </nav>
        </s:if>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
    <script>
        function viewProductDetails(productId) {
            alert('Dettagli prodotto: ' + productId);
            // Implementare modale con dettagli
        }
    </script>
</body>
</html>
