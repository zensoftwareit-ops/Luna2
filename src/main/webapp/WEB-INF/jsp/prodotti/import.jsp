<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="s" uri="/struts-tags" %>
<!DOCTYPE html>
<html lang="it">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Import/Export Prodotti - Luna2</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.10.0/font/bootstrap-icons.css">
</head>
<body>
    <%@ include file="../includes/sidebar.jsp" %>

    <div class="col-md-10 content-wrapper p-4">
        <div class="container-fluid">
            <div class="d-flex justify-content-between align-items-center mb-4">
                <h1 class="h3">
                    <i class="bi bi-file-earmark-spreadsheet me-2"></i>Import/Export Catalogo Prodotti
                </h1>
                <a href="<s:url action='list' namespace='/app/prodotti'/>" class="btn btn-secondary">
                    <i class="bi bi-arrow-left me-2"></i>Torna alla lista
                </a>
            </div>

            <s:if test="hasActionMessages()">
                <div class="alert alert-success alert-dismissible fade show">
                    <i class="bi bi-check-circle me-2"></i>
                    <s:actionmessage/>
                    <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
                </div>
            </s:if>

            <s:if test="hasActionErrors()">
                <div class="alert alert-danger alert-dismissible fade show">
                    <i class="bi bi-exclamation-triangle me-2"></i>
                    <s:actionerror/>
                    <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
                </div>
            </s:if>

            <!-- Export Section -->
            <div class="card mb-4">
                <div class="card-header bg-primary text-white">
                    <h5 class="mb-0">
                        <i class="bi bi-download me-2"></i>Export Catalogo
                    </h5>
                </div>
                <div class="card-body">
                    <p class="text-muted">
                        Scarica l'intero catalogo prodotti in formato Excel per backup o modifica massiva.
                    </p>
                    <div class="d-grid gap-2 d-md-flex">
                        <a href="<s:url action='exportCatalog' namespace='/app/prodotti'/>" class="btn btn-primary">
                            <i class="bi bi-file-earmark-excel me-2"></i>Esporta Catalogo Completo
                        </a>
                        <a href="<s:url action='downloadTemplate' namespace='/app/prodotti'/>" class="btn btn-outline-primary">
                            <i class="bi bi-file-earmark-text me-2"></i>Scarica Template Vuoto
                        </a>
                    </div>
                </div>
            </div>

            <!-- Import Section -->
            <div class="card">
                <div class="card-header bg-success text-white">
                    <h5 class="mb-0">
                        <i class="bi bi-upload me-2"></i>Import Catalogo
                    </h5>
                </div>
                <div class="card-body">
                    <div class="alert alert-info">
                        <h6 class="alert-heading">
                            <i class="bi bi-info-circle me-2"></i>Istruzioni per l'import
                        </h6>
                        <ul class="mb-0">
                            <li>Scarica il <strong>template vuoto</strong> o <strong>esporta il catalogo</strong> esistente</li>
                            <li>Compila/modifica il file Excel seguendo le istruzioni nel foglio "Istruzioni"</li>
                            <li>Campi obbligatori: Codice, Nome, TipoProdotto, UnitaMisura, PrezzoBase</li>
                            <li>Se il <strong>Codice</strong> esiste già, il prodotto verrà <strong>aggiornato</strong></li>
                            <li>Dimensione massima file: 10 MB</li>
                        </ul>
                    </div>

                    <s:form action="importCatalog" namespace="/app/prodotti" method="post" enctype="multipart/form-data" theme="simple">
                        <div class="mb-3">
                            <label for="uploadFile" class="form-label">
                                <i class="bi bi-file-earmark-excel me-2"></i>Seleziona file Excel (.xlsx)
                            </label>
                            <s:file name="uploadFile" id="uploadFile" cssClass="form-control" accept=".xlsx,.xls" required="true"/>
                            <div class="form-text">
                                Formati supportati: .xlsx, .xls (Excel)
                            </div>
                        </div>

                        <div class="d-grid gap-2 d-md-flex">
                            <button type="submit" class="btn btn-success">
                                <i class="bi bi-upload me-2"></i>Importa Prodotti
                            </button>
                            <button type="reset" class="btn btn-outline-secondary">
                                <i class="bi bi-x-circle me-2"></i>Reset
                            </button>
                        </div>
                    </s:form>
                </div>
            </div>

            <!-- Info tipologie prodotto -->
            <div class="card mt-4">
                <div class="card-header">
                    <h6 class="mb-0">
                        <i class="bi bi-info-circle me-2"></i>Tipologie Prodotto Supportate
                    </h6>
                </div>
                <div class="card-body">
                    <div class="row">
                        <div class="col-md-6">
                            <h6><span class="badge bg-primary">STANDARD</span></h6>
                            <p class="small">Prodotto standard venduto a unità</p>

                            <h6><span class="badge bg-info">A_MISURA</span></h6>
                            <p class="small">Prodotto venduto a misura (peso, lunghezza, area, ecc.)</p>

                            <h6><span class="badge bg-warning">COMPOSTO</span></h6>
                            <p class="small">Prodotto composto da sottoprodotti</p>
                        </div>
                        <div class="col-md-6">
                            <h6><span class="badge bg-success">VARIABILE</span></h6>
                            <p class="small">Prodotto con varianti (taglie, colori, ecc.)</p>

                            <h6><span class="badge bg-dark">SERVIZIO</span></h6>
                            <p class="small">Servizio (consulenza, assistenza, ore lavoro)</p>
                        </div>
                    </div>

                    <hr>

                    <h6 class="mb-2">Unità di Misura Supportate:</h6>
                    <div class="d-flex flex-wrap gap-2">
                        <span class="badge bg-secondary">PEZZO</span>
                        <span class="badge bg-secondary">KG</span>
                        <span class="badge bg-secondary">LITRO</span>
                        <span class="badge bg-secondary">METRO</span>
                        <span class="badge bg-secondary">MQ</span>
                        <span class="badge bg-secondary">MC</span>
                        <span class="badge bg-secondary">ORA</span>
                    </div>
                </div>
            </div>
        </div>
    </div>

    </div>
</div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
    <script>
        // Mostra nome file selezionato
        document.getElementById('uploadFile').addEventListener('change', function(e) {
            var fileName = e.target.files[0]?.name || 'Nessun file selezionato';
            console.log('File selezionato:', fileName);
        });
    </script>
</body>
</html>
