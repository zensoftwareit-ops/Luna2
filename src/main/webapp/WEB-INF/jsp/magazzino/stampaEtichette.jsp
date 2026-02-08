<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="s" uri="/struts-tags" %>
<!DOCTYPE html>
<html lang="it">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Stampa Etichette - Luna2</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.10.0/font/bootstrap-icons.css">
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
        .content-wrapper {
            margin-left: 250px;
            width: calc(100% - 250px);
            background-color: #f8f9fa;
            min-height: 100vh;
        }
        .product-card:hover {
            transform: translateY(-2px);
            box-shadow: 0 4px 12px rgba(0,0,0,0.15);
            transition: all 0.3s;
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
    <%@ include file="../includes/sidebar.jsp" %>

    <div class="content-wrapper p-4">
        <div class="container-fluid">
            <div class="d-flex justify-content-between align-items-center mb-4">
                <h1 class="h3">
                    <i class="bi bi-printer me-2"></i>Stampa Etichette Barcode
                </h1>
                <a href="<s:url action='list' namespace='/app/magazzino'/>" class="btn btn-outline-secondary">
                    <i class="bi bi-arrow-left me-2"></i>Torna a Giacenze
                </a>
            </div>

            <div class="card shadow-sm mb-4">
                <div class="card-body">
                    <h5 class="card-title">Seleziona Prodotti</h5>
                    <p class="text-muted">Seleziona i prodotti per cui desideri stampare le etichette barcode.</p>
                    
                    <s:form action="stampaEtichetteBatch" namespace="/app/magazzino" method="post">
                        <div class="row mb-3">
                            <div class="col-md-6">
                                <label class="form-label">Copie per Etichetta</label>
                                <s:textfield name="copiePerEtichetta" type="number" min="1" max="100" value="1" cssClass="form-control"/>
                            </div>
                        </div>

                        <table id="tableProdotti" class="table table-hover">
                            <thead>
                                <tr>
                                    <th width="50">
                                        <input type="checkbox" id="selectAll" class="form-check-input"/>
                                    </th>
                                    <th>Codice</th>
                                    <th>Prodotto</th>
                                    <th>Categoria</th>
                                    <th>Tipo</th>
                                </tr>
                            </thead>
                            <tbody>
                                <s:iterator value="prodotti" var="p">
                                    <tr>
                                        <td>
                                            <input type="checkbox" name="prodottiSelezionati" value="<s:property value='#p.id'/>" class="form-check-input product-checkbox"/>
                                        </td>
                                        <td><code><s:property value="#p.codice"/></code></td>
                                        <td><strong><s:property value="#p.nome"/></strong></td>
                                        <td><s:property value="#p.categoria"/></td>
                                        <td><s:property value="#p.tipoProdotto"/></td>
                                    </tr>
                                </s:iterator>
                            </tbody>
                        </table>

                        <div class="d-flex justify-content-between mt-4">
                            <span id="selectedCount" class="text-muted">Nessun prodotto selezionato</span>
                            <button type="submit" class="btn btn-success" id="btnStampa" disabled>
                                <i class="bi bi-printer me-2"></i>Stampa Etichette
                            </button>
                        </div>
                    </s:form>
                </div>
            </div>
        </div>
    </div>

    <script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
    <script src="https://cdn.datatables.net/1.13.4/js/jquery.dataTables.min.js"></script>
    <script src="https://cdn.datatables.net/1.13.4/js/dataTables.bootstrap5.min.js"></script>
    <script>
        $(document).ready(function() {
            $('#tableProdotti').DataTable({
                language: {
                    url: 'https://cdn.datatables.net/plug-ins/1.13.4/i18n/it-IT.json'
                },
                pageLength: 25
            });

            // Select all checkbox
            $('#selectAll').on('change', function() {
                $('.product-checkbox').prop('checked', this.checked);
                updateSelectedCount();
            });

            $('.product-checkbox').on('change', function() {
                updateSelectedCount();
            });

            function updateSelectedCount() {
                const count = $('.product-checkbox:checked').length;
                $('#selectedCount').text(count > 0 ? count + ' prodotto/i selezionato/i' : 'Nessun prodotto selezionato');
                $('#btnStampa').prop('disabled', count === 0);
            }
        });
    </script>
</body>
</html>
