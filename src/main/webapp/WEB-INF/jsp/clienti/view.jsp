<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="s" uri="/struts-tags" %>
<!DOCTYPE html>
<html lang="it">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Dettaglio Cliente - Luna2</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.10.0/font/bootstrap-icons.css">
</head>
<body>
    <%@ include file="../includes/sidebar.jsp" %>
    
    <div class="col-md-10 content-wrapper p-4">
        <div class="container-fluid">
            <div class="d-flex justify-content-between align-items-center mb-4">
                <h1 class="h3">
                    <i class="bi bi-person-badge me-2"></i>
                    Dettaglio Cliente
                </h1>
                <div>
                    <a href="<s:url action='edit'><s:param name='id' value='cliente.id'/></s:url>" class="btn btn-primary">
                        <i class="bi bi-pencil me-2"></i>Modifica
                    </a>
                    <a href="<s:url action='list' namespace='/app/clienti'/>" class="btn btn-secondary">
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
                                    <label class="text-muted small">Codice Cliente</label>
                                    <p class="h5"><s:property value="cliente.codiceCliente"/></p>
                                </div>
                                <div class="col-md-6">
                                    <label class="text-muted small">Tipo</label>
                                    <p>
                                        <span class="badge bg-primary fs-6">
                                            <s:property value="cliente.tipoAnagrafica"/>
                                        </span>
                                    </p>
                                </div>
                            </div>

                            <div class="row mb-3">
                                <div class="col-md-12">
                                    <label class="text-muted small">Ragione Sociale</label>
                                    <p class="h4"><s:property value="cliente.ragioneSociale"/></p>
                                </div>
                            </div>

                            <div class="row mb-3">
                                <div class="col-md-6">
                                    <label class="text-muted small">Partita IVA</label>
                                    <p><s:property value="cliente.partitaIva" default="-"/></p>
                                </div>
                                <div class="col-md-6">
                                    <label class="text-muted small">Codice Fiscale</label>
                                    <p><s:property value="cliente.codiceFiscale" default="-"/></p>
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
                                    <p><s:property value="cliente.indirizzo" default="-"/></p>
                                    <p><s:property value="cliente.citta" default="-"/> (<s:property value="cliente.provincia" default="-"/>), <s:property value="cliente.cap" default="-"/></p>
                                    <p><s:property value="cliente.paese" default="Italia"/></p>
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
                                <p><s:property value="cliente.email" default="-"/></p>
                            </div>
                            <div class="mb-3">
                                <label class="text-muted small">Telefono</label>
                                <p><s:property value="cliente.telefono" default="-"/></p>
                            </div>
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
