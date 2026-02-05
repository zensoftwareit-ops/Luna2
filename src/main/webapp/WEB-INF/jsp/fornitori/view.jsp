<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="s" uri="/struts-tags" %>
<!DOCTYPE html>
<html lang="it">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Dettaglio Fornitore - Luna2</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.10.0/font/bootstrap-icons.css">
</head>
<body>
    <%@ include file="../includes/sidebar.jsp" %>
    
    <div class="col-md-10 content-wrapper p-4">
        <div class="container-fluid">
            <div class="d-flex justify-content-between align-items-center mb-4">
                <h1 class="h3">
                    <i class="bi bi-truck me-2"></i>
                    Dettaglio Fornitore
                </h1>
                <div>
                    <a href="<s:url action='edit'><s:param name='id' value='fornitore.id'/></s:url>" class="btn btn-primary">
                        <i class="bi bi-pencil me-2"></i>Modifica
                    </a>
                    <a href="<s:url action='list' namespace='/app/fornitori'/>" class="btn btn-secondary">
                        <i class="bi bi-arrow-left me-2"></i>Torna all'elenco
                    </a>
                </div>
            </div>

            <div class="row">
                <div class="col-md-8">
                    <div class="card mb-4">
                        <div class="card-header bg-primary text-white">
                            <h5 class="mb-0"><i class="bi bi-info-circle me-2"></i>Informazioni Generali</h5>
                        </div>
                        <div class="card-body">
                            <div class="row mb-3">
                                <div class="col-md-6">
                                    <label class="text-muted small">Codice Fornitore</label>
                                    <p class="h5"><s:property value="fornitore.codiceFornitore"/></p>
                                </div>
                            </div>

                            <div class="row mb-3">
                                <div class="col-md-12">
                                    <label class="text-muted small">Ragione Sociale</label>
                                    <p class="h4"><s:property value="fornitore.ragioneSociale"/></p>
                                </div>
                            </div>

                            <div class="row mb-3">
                                <div class="col-md-6">
                                    <label class="text-muted small">Partita IVA</label>
                                    <p><s:property value="fornitore.partitaIva" default="-"/></p>
                                </div>
                                <div class="col-md-6">
                                    <label class="text-muted small">Codice Fiscale</label>
                                    <p><s:property value="fornitore.codiceFiscale" default="-"/></p>
                                </div>
                            </div>
                        </div>
                    </div>

                    <div class="card mb-4">
                        <div class="card-header bg-info text-white">
                            <h5 class="mb-0"><i class="bi bi-geo-alt me-2"></i>Indirizzo</h5>
                        </div>
                        <div class="card-body">
                            <div class="row mb-3">
                                <div class="col-md-12">
                                    <p><s:property value="fornitore.indirizzo" default="-"/></p>
                                    <p><s:property value="fornitore.citta" default="-"/> (<s:property value="fornitore.provincia" default="-"/>), <s:property value="fornitore.cap" default="-"/></p>
                                    <p><s:property value="fornitore.paese" default="Italia"/></p>
                                </div>
                            </div>
                        </div>
                    </div>

                    <div class="card mb-4">
                        <div class="card-header bg-success text-white">
                            <h5 class="mb-0"><i class="bi bi-cash-coin me-2"></i>Dati Commerciali</h5>
                        </div>
                        <div class="card-body">
                            <div class="row mb-3">
                                <div class="col-md-12">
                                    <label class="text-muted small">Condizioni Pagamento</label>
                                    <p><s:property value="fornitore.condizioniPagamento" default="-"/></p>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>

                <div class="col-md-4">
                    <div class="card mb-4">
                        <div class="card-header bg-warning text-dark">
                            <h5 class="mb-0"><i class="bi bi-telephone me-2"></i>Contatti</h5>
                        </div>
                        <div class="card-body">
                            <div class="mb-3">
                                <label class="text-muted small">Email</label>
                                <p><s:property value="fornitore.email" default="-"/></p>
                            </div>
                            <div class="mb-3">
                                <label class="text-muted small">Telefono</label>
                                <p><s:property value="fornitore.telefono" default="-"/></p>
                            </div>
                            <div class="mb-3">
                                <label class="text-muted small">Sito Web</label>
                                <p>
                                    <s:if test="fornitore.sitoWeb != null">
                                        <a href="<s:property value='fornitore.sitoWeb'/>" target="_blank">
                                            <i class="bi bi-globe me-1"></i><s:property value="fornitore.sitoWeb"/>
                                        </a>
                                    </s:if>
                                    <s:else>-</s:else>
                                </p>
                            </div>
                        </div>
                    </div>

                    <div class="card mb-4">
                        <div class="card-header bg-secondary text-white">
                            <h5 class="mb-0"><i class="bi bi-sticky me-2"></i>Note</h5>
                        </div>
                        <div class="card-body">
                            <p class="text-muted">
                                <s:if test="fornitore.note != null && !fornitore.note.trim().isEmpty()">
                                    <s:property value="fornitore.note" escapeHtml="false"/>
                                </s:if>
                                <s:else>
                                    <em>Nessuna nota</em>
                                </s:else>
                            </p>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>

    </div>
</div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
