<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="s" uri="/struts-tags" %>
<!DOCTYPE html>
<html lang="it">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Fornitori - Luna2</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.10.0/font/bootstrap-icons.css">
    <link rel="stylesheet" href="https://cdn.datatables.net/1.13.4/css/dataTables.bootstrap5.min.css">
</head>
<body>
    <%@ include file="../includes/sidebar.jsp" %>
    
    <div class="col-md-10 content-wrapper p-4">
        <div class="container-fluid">
            <div class="d-flex justify-content-between align-items-center mb-4">
                <h1 class="h3"><i class="bi bi-truck me-2"></i>Gestione Fornitori</h1>
                <div>
                    <a href="<s:url action='importForm' namespace='/app/fornitori'/>" class="btn btn-success me-2">
                        <i class="bi bi-upload me-2"></i>Importa
                    </a>
                    <a href="<s:url action='create' namespace='/app/fornitori'/>" class="btn btn-primary">
                        <i class="bi bi-plus-circle me-2"></i>Nuovo Fornitore
                    </a>
                </div>
            </div>

            <s:if test="hasActionMessages()">
                <div class="alert alert-success alert-dismissible fade show">
                    <s:actionmessage/>
                    <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
                </div>
            </s:if>

            <!-- Filtri Avanzati -->
            <div class="card mb-3">
                <div class="card-header bg-light">
                    <h5 class="mb-0"><i class="bi bi-funnel me-2"></i>Filtri Ricerca</h5>
                </div>
                <div class="card-body">
                    <s:form action="list" method="post" theme="simple" cssClass="row g-3">
                        <div class="col-md-5">
                            <label class="form-label">Ricerca Nome/Ragione Sociale</label>
                            <s:textfield name="searchTerm" cssClass="form-control" placeholder="Cerca..."/>
                        </div>
                        <div class="col-md-3">
                            <label class="form-label">Città</label>
                            <s:textfield name="cittaFiltro" cssClass="form-control" placeholder="Es. Milano"/>
                        </div>
                        <div class="col-md-2">
                            <label class="form-label">Provincia</label>
                            <s:textfield name="provinciaFiltro" cssClass="form-control" placeholder="Es. MI" maxlength="2"/>
                        </div>
                        <div class="col-md-12">
                            <button type="submit" class="btn btn-primary">
                                <i class="bi bi-search me-2"></i>Filtra
                            </button>
                            <a href="<s:url action='list'/>" class="btn btn-secondary">
                                <i class="bi bi-arrow-clockwise me-2"></i>Reset Filtri
                            </a>
                        </div>
                    </s:form>
                </div>
            </div>

            <div class="card">
                <div class="card-header bg-light d-flex justify-content-between align-items-center">
                    <h5 class="mb-0">Elenco Fornitori</h5>
                    <div>
                        <a href="<s:url action='exportExcel' namespace='/app/fornitori'/>" class="btn btn-sm btn-success" title="Esporta Excel">
                            <i class="bi bi-file-earmark-spreadsheet me-1"></i>Excel
                        </a>
                        <a href="<s:url action='exportCsv' namespace='/app/fornitori'/>" class="btn btn-sm btn-info" title="Esporta CSV">
                            <i class="bi bi-file-earmark-csv me-1"></i>CSV
                        </a>
                    </div>
                </div>
                <div class="card-body">
                    <div class="table-responsive">
                        <table id="fornitoriTable" class="table table-striped table-hover">
                            <thead>
                                <tr>
                                    <th>Codice</th>
                                    <th>Ragione Sociale</th>
                                    <th>P.IVA</th>
                                    <th>Email</th>
                                    <th>Telefono</th>
                                    <th>Città</th>
                                    <th>Sito Web</th>
                                    <th>Azioni</th>
                                </tr>
                            </thead>
                            <tbody>
                                <s:iterator value="fornitori">
                                    <tr>
                                        <td><strong><s:property value="codiceFornitore"/></strong></td>
                                        <td><s:property value="ragioneSociale"/></td>
                                        <td><s:property value="partitaIva"/></td>
                                        <td><s:property value="email"/></td>
                                        <td><s:property value="telefono"/></td>
                                        <td><s:property value="citta"/></td>
                                        <td>
                                            <s:if test="sitoWeb != null">
                                                <a href="<s:property value='sitoWeb'/>" target="_blank" class="text-decoration-none">
                                                    <i class="bi bi-globe"></i>
                                                </a>
                                            </s:if>
                                        </td>
                                        <td>
                                            <div class="btn-group btn-group-sm">
                                                <a href="<s:url action='view'><s:param name='id' value='id'/></s:url>" 
                                                   class="btn btn-outline-info" title="Visualizza">
                                                    <i class="bi bi-eye"></i>
                                                </a>
                                                <a href="<s:url action='edit'><s:param name='id' value='id'/></s:url>" 
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
            $('#fornitoriTable').DataTable({
                language: {
                    url: 'https://cdn.datatables.net/plug-ins/1.13.4/i18n/it-IT.json'
                },
                pageLength: 25
            });
        });

        function confirmDelete(id) {
            if (confirm('Sei sicuro di voler eliminare questo fornitore?')) {
                window.location.href = '<s:url action="delete"/>' + '?id=' + id;
            }
        }
    </script>
</body>
</html>
