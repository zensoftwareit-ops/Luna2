<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="s" uri="/struts-tags" %>
<!DOCTYPE html>
<html lang="it">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Import Massivo Clienti - Luna2</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.10.0/font/bootstrap-icons.css">
</head>
<body>
    <%@ include file="../includes/sidebar.jsp" %>
    
    <div class="col-md-10 content-wrapper p-4">
        <div class="container-fluid">
            <div class="d-flex justify-content-between align-items-center mb-4">
                <h1 class="h3">
                    <i class="bi bi-upload me-2"></i>Import Massivo Clienti
                </h1>
                <a href="<s:url action='list' namespace='/app/clienti'/>" class="btn btn-secondary">
                    <i class="bi bi-arrow-left me-2"></i>Torna all'elenco
                </a>
            </div>

            <s:if test="hasActionErrors()">
                <div class="alert alert-danger alert-dismissible fade show">
                    <s:actionerror/>
                    <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
                </div>
            </s:if>

            <s:if test="hasActionMessages()">
                <div class="alert alert-success alert-dismissible fade show">
                    <s:actionmessage/>
                    <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
                </div>
            </s:if>

            <s:if test="importedCount > 0 || errorCount > 0">
                <div class="card mb-4">
                    <div class="card-body">
                        <h5 class="card-title">Risultati Import</h5>
                        <p class="mb-2">
                            <span class="badge bg-success">Importati: <s:property value="importedCount"/></span>
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

            <s:if test="previewData == null || previewData.isEmpty()">
                <!-- Step 1: Upload File -->
                <div class="card">
                    <div class="card-header bg-primary text-white">
                        <h5 class="mb-0"><i class="bi bi-1-circle me-2"></i>Carica File</h5>
                    </div>
                    <div class="card-body">
                        <div class="alert alert-info">
                            <i class="bi bi-info-circle me-2"></i>
                            <strong>Formati supportati:</strong> CSV (UTF-8) o Excel (.xlsx, .xls)
                            <br>
                            Il file deve contenere una riga di intestazione con i nomi delle colonne.
                        </div>

                        <s:form action="parseFile" method="post" enctype="multipart/form-data" theme="simple">
                            <div class="mb-3">
                                <label class="form-label">Seleziona file da importare:</label>
                                <s:file name="fileImport" cssClass="form-control" accept=".csv,.xlsx,.xls" required="true"/>
                            </div>
                            <button type="submit" class="btn btn-primary">
                                <i class="bi bi-arrow-right me-2"></i>Procedi con Anteprima
                            </button>
                        </s:form>
                    </div>
                </div>
            </s:if>
            <s:else>
                <!-- Step 2: Map Fields and Import -->
                <div class="card">
                    <div class="card-header bg-primary text-white">
                        <h5 class="mb-0"><i class="bi bi-2-circle me-2"></i>Mappatura Campi e Anteprima</h5>
                    </div>
                    <div class="card-body">
                        <div class="alert alert-success">
                            <i class="bi bi-check-circle me-2"></i>
                            File caricato con successo! Trovate <strong><s:property value="previewData.size()"/></strong> righe.
                        </div>

                        <s:form action="processImport" method="post" theme="simple" id="importForm">
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
                                                        <option value="codiceCliente">Codice Cliente</option>
                                                        <option value="ragioneSociale">Ragione Sociale *</option>
                                                        <option value="tipoAnagrafica">Tipo (CLIENTE/PROSPECT)</option>
                                                        <option value="partitaIva">Partita IVA</option>
                                                        <option value="codiceFiscale">Codice Fiscale</option>
                                                        <option value="indirizzo">Indirizzo</option>
                                                        <option value="citta">Città</option>
                                                        <option value="provincia">Provincia</option>
                                                        <option value="cap">CAP</option>
                                                        <option value="paese">Nazione</option>
                                                        <option value="telefono">Telefono</option>
                                                        <option value="email">Email</option>
                                                        <option value="pec">PEC</option>
                                                        <option value="codiceSdi">Codice SDI</option>
                                                        <option value="sitoWeb">Sito Web</option>
                                                        <option value="condizioniPagamento">Condizioni Pagamento</option>
                                                        <option value="listinoDefault">Listino Default</option>
                                                        <option value="scontoPercentuale">Sconto Percentuale (%)</option>
                                                        <option value="fidoMassimo">Fido Massimo (€)</option>
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
                                    <li>Il campo <strong>Ragione Sociale</strong> è obbligatorio</li>
                                    <li>Il <strong>Codice Cliente</strong> verrà generato automaticamente se non specificato (CLI001, CLI002...)</li>
                                    <li>Le prime 100 righe sono mostrate in anteprima, ma verrà importato l'intero file</li>
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
                                <a href="<s:url action='importForm'/>" class="btn btn-secondary btn-lg ms-2">
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
    <script>
        // Auto-map common field names
        document.addEventListener('DOMContentLoaded', function() {
            const mappings = {
                'codice': 'codiceCliente',
                'codice cliente': 'codiceCliente',
                'ragione sociale': 'ragioneSociale',
                'ragionesociale': 'ragioneSociale',
                'tipo': 'tipoAnagrafica',
                'partita iva': 'partitaIva',
                'p.iva': 'partitaIva',
                'piva': 'partitaIva',
                'codice fiscale': 'codiceFiscale',
                'cf': 'codiceFiscale',
                'indirizzo': 'indirizzo',
                'città': 'citta',
                'citta': 'citta',
                'provincia': 'provincia',
                'prov': 'provincia',
                'cap': 'cap',
                'nazione': 'paese',
                'paese': 'paese',
                'telefono': 'telefono',
                'tel': 'telefono',
                'email': 'email',
                'pec': 'pec',
                'sdi': 'codiceSdi',
                'codice sdi': 'codiceSdi',
                'sito': 'sitoWeb',
                'sito web': 'sitoWeb',
                'pagamento': 'condizioniPagamento',
                'note': 'note'
            };

            document.querySelectorAll('select[name^="fieldMapping"]').forEach(select => {
                const headerText = select.closest('tr').querySelector('td:first-child strong').textContent.toLowerCase();
                if (mappings[headerText]) {
                    select.value = mappings[headerText];
                }
            });
        });
    </script>
</body>
</html>
