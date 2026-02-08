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

            <s:if test="importedCount > 0 || updatedCount > 0 || errorCount > 0">
                <div class="card mb-4">
                    <div class="card-body">
                        <h5 class="card-title">Risultati Import</h5>
                        <p class="mb-2">
                            <span class="badge bg-success">Importati: <s:property value="importedCount"/></span>
                            <span class="badge bg-info ms-2">Aggiornati: <s:property value="updatedCount"/></span>
                            <span class="badge bg-danger ms-2">Errori: <s:property value="errorCount"/></span>
                        </p>
                        <s:if test="importErrors != null && !importErrors.isEmpty()">
                            <div class="alert alert-warning mt-3">
                                <strong>Dettaglio errori:</strong>
                                <ul class="mb-0 mt-2">
                                    <s:iterator value="importErrors">
                                        <li><s:property/></li>
                                    </s:iterator>
                                </ul>
                            </div>
                        </s:if>
                    </div>
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

            <s:if test="previewData == null || previewData.isEmpty()">
                <!-- Step 1: Upload File -->
                <div class="card">
                    <div class="card-header bg-success text-white">
                        <h5 class="mb-0"><i class="bi bi-1-circle me-2"></i>Carica File</h5>
                    </div>
                    <div class="card-body">
                        <div class="alert alert-info">
                            <i class="bi bi-info-circle me-2"></i>
                            <strong>Formati supportati:</strong> CSV (UTF-8) o Excel (.xlsx, .xls)
                            <br>
                            Il file deve contenere una riga di intestazione con i nomi delle colonne.
                        </div>

                        <s:form action="parseFile" namespace="/app/prodotti" method="post" enctype="multipart/form-data" theme="simple">
                            <div class="mb-3">
                                <label class="form-label">Seleziona file da importare:</label>
                                <s:file name="fileImport" cssClass="form-control" accept=".csv,.xlsx,.xls" required="true"/>
                            </div>
                            <button type="submit" class="btn btn-success">
                                <i class="bi bi-arrow-right me-2"></i>Procedi con Anteprima
                            </button>
                        </s:form>
                    </div>
                </div>
            </s:if>
            <s:else>
                <!-- Step 2: Map Fields and Import -->
                <div class="card">
                    <div class="card-header bg-success text-white">
                        <h5 class="mb-0"><i class="bi bi-2-circle me-2"></i>Mappatura Campi e Anteprima</h5>
                    </div>
                    <div class="card-body">
                        <div class="alert alert-success">
                            <i class="bi bi-check-circle me-2"></i>
                            File caricato con successo! Trovate <strong><s:property value="previewData.size()"/></strong> righe.
                        </div>

                        <s:form action="processImport" namespace="/app/prodotti" method="post" theme="simple" id="importForm">
                            <h5 class="mb-3">Mappatura Colonne</h5>
                            <p class="text-muted">Associa ogni colonna del file a un campo del database:</p>

                            <div class="table-responsive mb-4">
                                <table class="table table-bordered">
                                    <thead class="table-light">
                                        <tr>
                                            <th style="width: 30%">Colonna File</th>
                                            <th style="width: 30%">Campo Database</th>
                                            <th style="width: 40%">Esempio Valore</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        <s:iterator value="fileHeaders" var="header" status="stat">
                                            <tr>
                                                <td><strong><s:property value="#header"/></strong></td>
                                                <td>
                                                    <select name="fieldMapping['<s:property value="#header"/>']" class="form-select form-select-sm">
                                                        <option value="">-- Ignora --</option>
                                                        <option value="codice">Codice *</option>
                                                        <option value="nome">Nome *</option>
                                                        <option value="descrizione">Descrizione</option>
                                                        <option value="categoria">Categoria</option>
                                                        <option value="codiceFornitore">Codice Fornitore</option>
                                                        <option value="tipoProdotto">TipoProdotto *</option>
                                                        <option value="unitaMisura">UnitaMisura *</option>
                                                        <option value="prezzoBase">PrezzoBase *</option>
                                                        <option value="costoAcquisto">CostoAcquisto</option>
                                                        <option value="ivaPercentuale">IvaPercentuale</option>
                                                        <option value="scontoMassimo">ScontoMassimo</option>
                                                        <option value="peso">Peso</option>
                                                        <option value="volume">Volume</option>
                                                        <option value="codiceEan">CodiceEAN</option>
                                                        <option value="giacenzaMinima">GiacenzaMinima</option>
                                                        <option value="gestioneMagazzino">GestioneMagazzino (SI/NO)</option>
                                                        <option value="attivo">Attivo (SI/NO)</option>
                                                        <option value="note">Note</option>
                                                    </select>
                                                </td>
                                                <td class="text-muted small">
                                                    <s:if test="previewData.size() > 0">
                                                        <s:property value="previewData[0][#header]"/>
                                                    </s:if>
                                                </td>
                                            </tr>
                                        </s:iterator>
                                    </tbody>
                                </table>
                            </div>

                            <div class="alert alert-warning">
                                <i class="bi bi-exclamation-triangle me-2"></i>
                                <strong>Note:</strong>
                                <ul class="mb-0">
                                    <li>I campi <strong>Codice, Nome, TipoProdotto, UnitaMisura, PrezzoBase</strong> sono obbligatori</li>
                                    <li>TipoProdotto: STANDARD, A_MISURA, COMPOSTO, VARIABILE, SERVIZIO</li>
                                    <li>UnitaMisura: PEZZO, KG, LITRO, METRO, MQ, MC, ORA</li>
                                    <li>GestioneMagazzino/Attivo accettano SI/NO, TRUE/FALSE, 1/0</li>
                                    <li>Se il <strong>Codice</strong> esiste gia', il prodotto verra' aggiornato</li>
                                    <li>Le prime 100 righe sono mostrate in anteprima, ma verra' importato l'intero file</li>
                                </ul>
                            </div>

                            <h5 class="mb-3 mt-4">Anteprima Dati (prime 10 righe)</h5>
                            <div class="table-responsive">
                                <table class="table table-sm table-striped">
                                    <thead class="table-dark">
                                        <tr>
                                            <th>#</th>
                                            <s:iterator value="fileHeaders" var="header">
                                                <th><s:property value="#header"/></th>
                                            </s:iterator>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        <s:iterator value="previewData" var="row" status="stat" begin="0" end="9">
                                            <tr>
                                                <td><s:property value="#stat.index + 1"/></td>
                                                <s:iterator value="fileHeaders" var="header">
                                                    <td><s:property value="#row[#header]"/></td>
                                                </s:iterator>
                                            </tr>
                                        </s:iterator>
                                    </tbody>
                                </table>
                            </div>

                            <div class="mt-4">
                                <button type="submit" class="btn btn-success btn-lg">
                                    <i class="bi bi-check-circle me-2"></i>Conferma e Importa
                                </button>
                                <a href="<s:url action='importPage' namespace='/app/prodotti'/>" class="btn btn-secondary btn-lg ms-2">
                                    <i class="bi bi-x-circle me-2"></i>Annulla
                                </a>
                            </div>
                        </s:form>
                    </div>
                </div>
            </s:else>
        </div>
    </div>

    </div>
</div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>