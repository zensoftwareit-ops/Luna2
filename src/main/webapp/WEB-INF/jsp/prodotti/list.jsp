<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="s" uri="/struts-tags" %>
<!DOCTYPE html>
<html lang="it">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Prodotti - Luna2</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.10.0/font/bootstrap-icons.css">
    <link rel="stylesheet" href="https://cdn.datatables.net/1.13.4/css/dataTables.bootstrap5.min.css">
</head>
<body>
    <%@ include file="../includes/sidebar.jsp" %>
    
    <div class="col-md-10 content-wrapper p-4">
        <div class="container-fluid">
            <div class="d-flex justify-content-between align-items-center mb-4">
                <h1 class="h3"><i class="bi bi-box-seam me-2"></i>Catalogo Prodotti</h1>
                <div class="btn-group">
                    <a href="<s:url action='create' namespace='/app/prodotti'/>" class="btn btn-primary">
                        <i class="bi bi-plus-circle me-2"></i>Nuovo Prodotto
                    </a>
                    <button type="button" class="btn btn-success dropdown-toggle dropdown-toggle-split" data-bs-toggle="dropdown" aria-expanded="false">
                        <i class="bi bi-file-earmark-spreadsheet me-1"></i>Import/Export
                    </button>
                    <ul class="dropdown-menu dropdown-menu-end">
                        <li><a class="dropdown-item" href="<s:url action='importPage' namespace='/app/prodotti'/>">
                            <i class="bi bi-upload me-2"></i>Import da Excel
                        </a></li>
                        <li><a class="dropdown-item" href="<s:url action='exportCatalog' namespace='/app/prodotti'/>">
                            <i class="bi bi-download me-2"></i>Export in Excel
                        </a></li>
                        <li><hr class="dropdown-divider"></li>
                        <li><a class="dropdown-item" href="<s:url action='downloadTemplate' namespace='/app/prodotti'/>">
                            <i class="bi bi-file-earmark-text me-2"></i>Scarica Template
                        </a></li>
                    </ul>
                </div>
            </div>

            <s:if test="hasActionMessages()">
                <div class="alert alert-success alert-dismissible fade show">
                    <s:actionmessage/>
                    <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
                </div>
            </s:if>

            <!-- Search Box -->
            <div class="card mb-3">
                <div class="card-body">
                    <s:form action="search" method="post" theme="simple" cssClass="row g-3">
                        <div class="col-md-4">
                            <s:textfield name="searchTerm" cssClass="form-control" placeholder="Cerca per nome o codice"/>
                        </div>
                        <div class="col-md-3">
                            <s:textfield name="categoria" cssClass="form-control" placeholder="Categoria"/>
                        </div>
                        <div class="col-md-3">
                            <button type="submit" class="btn btn-primary">
                                <i class="bi bi-search me-2"></i>Cerca
                            </button>
                            <a href="<s:url action='list'/>" class="btn btn-secondary">Reset</a>
                        </div>
                    </s:form>
                </div>
            </div>

            <div class="card">
                <div class="card-body">
                    <div class="table-responsive">
                        <table id="prodottiTable" class="table table-striped table-hover">
                            <thead>
                                <tr>
                                    <th>Codice</th>
                                    <th>Nome</th>
                                    <th>Tipo</th>
                                    <th>Categoria</th>
                                    <th>Fornitore</th>
                                    <th>Prezzo Base</th>
                                    <th>U.M.</th>
                                    <th>Attivo</th>
                                    <th>Azioni</th>
                                </tr>
                            </thead>
                            <tbody>
                                <s:iterator value="prodotti">
                                    <tr>
                                        <td><strong><s:property value="codice"/></strong></td>
                                        <td><s:property value="nome"/></td>
                                        <td>
                                            <s:set var="tipoBadge" value="'secondary'"/>
                                            <s:if test="tipoProdotto.name() == 'STANDARD'"><s:set var="tipoBadge" value="'primary'"/></s:if>
                                            <s:elseif test="tipoProdotto.name() == 'A_MISURA'"><s:set var="tipoBadge" value="'info'"/></s:elseif>
                                            <s:elseif test="tipoProdotto.name() == 'COMPOSTO'"><s:set var="tipoBadge" value="'warning'"/></s:elseif>
                                            <s:elseif test="tipoProdotto.name() == 'VARIABILE'"><s:set var="tipoBadge" value="'success'"/></s:elseif>
                                            <s:elseif test="tipoProdotto.name() == 'SERVIZIO'"><s:set var="tipoBadge" value="'dark'"/></s:elseif>
                                            <s:set var="tipoBadgeClass" value="'badge bg-' + tipoBadge"/>
                                            <span class="${tipoBadgeClass}">
                                                <s:property value="tipoProdotto"/>
                                            </span>
                                        </td>
                                        <td><s:property value="categoria" default="-"/></td>
                                        <td>
                                            <s:if test="fornitore != null">
                                                <small><s:property value="fornitore.ragioneSociale"/></small>
                                            </s:if>
                                            <s:else>
                                                <span class="text-muted">-</span>
                                            </s:else>
                                        </td>
                                        <td>€ <s:property value="prezzoBase"/></td>
                                        <td><s:property value="unitaMisura"/></td>
                                        <td>
                                            <s:if test="attivo">
                                                <span class="badge bg-success">Sì</span>
                                            </s:if>
                                            <s:else>
                                                <span class="badge bg-secondary">No</span>
                                            </s:else>
                                        </td>
                                        <td>
                                            <div class="btn-group btn-group-sm">
                                                <a href="<s:url action='edit' namespace='/app/prodotti'><s:param name='id' value='id'/></s:url>" 
                                                   class="btn btn-outline-primary" title="Modifica">
                                                    <i class="bi bi-pencil"></i>
                                                </a>
                                                <button type="button" class="btn btn-outline-danger" 
                                                        onclick="confirmDelete(<s:property value='id'/>)" title="Elimina">
                                                    <i class="bi bi-trash"></i>
                                                </button>
                                            </div>
                                        </td>
                                    </tr>
                                </s:iterator>
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>
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
            $('#prodottiTable').DataTable({
                language: {
                    url: 'https://cdn.datatables.net/plug-ins/1.13.4/i18n/it-IT.json'
                },
                pageLength: 25,
                order: [[1, 'asc']]
            });
        });

        function confirmDelete(id) {
            if (confirm('Sei sicuro di voler disattivare questo prodotto?')) {
                window.location.href = '<s:url action="delete" namespace="/app/prodotti"/>?id=' + id;
            }
        }
                window.location.href = '<s:url action="delete"/>' + '?id=' + id;
            }
        }
    </script>
</body>
</html>
