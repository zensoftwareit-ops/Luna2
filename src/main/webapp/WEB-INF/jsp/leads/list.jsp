<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="s" uri="/struts-tags" %>
<!DOCTYPE html>
<html lang="it">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Leads - Luna2</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.10.0/font/bootstrap-icons.css">
    <link rel="stylesheet" href="https://cdn.datatables.net/1.13.4/css/dataTables.bootstrap5.min.css">
</head>
<body>
    <%@ include file="../includes/sidebar.jsp" %>
    
    <div class="col-md-10 content-wrapper p-4">
        <div class="container-fluid">
            <div class="d-flex justify-content-between align-items-center mb-4">
                <h1 class="h3">
                    <i class="bi bi-person-lines-fill me-2"></i>
                    Lead CRM
                </h1>
                <a href="<s:url action='create' namespace='/app/leads'/>" class="btn btn-success">
                    <i class="bi bi-plus-circle me-2"></i>Nuovo Lead
                </a>
            </div>

            <s:if test="hasActionMessages()">
                <div class="alert alert-success alert-dismissible fade show">
                    <s:actionmessage/>
                    <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
                </div>
            </s:if>

            <s:if test="hasActionErrors()">
                <div class="alert alert-danger alert-dismissible fade show">
                    <s:actionerror/>
                    <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
                </div>
            </s:if>

            <div class="card mb-4">
                <div class="card-header bg-light">
                    <div class="row align-items-center">
                        <div class="col-md-6">
                            <h5 class="mb-0"><i class="bi bi-funnel me-2"></i>Filtri Ricerca</h5>
                        </div>
                        <div class="col-md-6 text-end">
                            <button type="button" class="btn btn-sm btn-outline-secondary" data-bs-toggle="collapse" data-bs-target="#filterPanel">
                                <i class="bi bi-chevron-down me-1"></i>Mostra/Nascondi
                            </button>
                        </div>
                    </div>
                </div>
                <div class="card-body collapse show" id="filterPanel">
                    <s:form action="list" method="get" theme="simple" namespace="/app/leads">
                        <div class="row mb-3">
                            <div class="col-md-6">
                                <label class="form-label">Ricerca Azienda/Contatto</label>
                                <s:textfield name="searchTerm" cssClass="form-control" placeholder="Nome azienda o contatto..."/>
                            </div>
                            <div class="col-md-6">
                                <label class="form-label">Stato Lead</label>
                                <s:select name="statoFiltro" 
                                         list="statiLead" 
                                         cssClass="form-select"
                                         listKey="name()" listValue="name()"
                                         headerKey="" headerValue="-- Tutti gli Stati --"/>
                            </div>
                        </div>
                        <button type="submit" class="btn btn-primary">
                            <i class="bi bi-search me-2"></i>Filtra
                        </button>
                        <a href="<s:url action='list' namespace='/app/leads'/>" class="btn btn-secondary">
                            <i class="bi bi-arrow-clockwise me-2"></i>Azzera
                        </a>
                    </s:form>
                </div>
            </div>

            <div class="card">
                <div class="card-body">
                    <div class="table-responsive">
                        <table id="leadsTable" class="table table-striped table-hover">
                            <thead>
                                <tr>
                                    <th>Azienda</th>
                                    <th>Contatto</th>
                                    <th>Email</th>
                                    <th>Telefono</th>
                                    <th>Stato</th>
                                    <th>Budget Stimato</th>
                                    <th>Probabilità</th>
                                    <th>Data Contatto</th>
                                    <th>Azioni</th>
                                </tr>
                            </thead>
                            <tbody>
                                <s:iterator value="leads">
                                    <tr>
                                        <td><strong><s:property value="azienda"/></strong></td>
                                        <td><s:property value="nomeContattoCompleto"/></td>
                                        <td><s:property value="email"/></td>
                                        <td><s:property value="telefono" default="-"/></td>
                                        <td>
                                            <s:set var="badgeColor" value="'dark'"/>
                                            <s:if test="stato.name() == 'NUOVO'"><s:set var="badgeColor" value="'secondary'"/></s:if>
                                            <s:elseif test="stato.name() == 'CONTATTATO'"><s:set var="badgeColor" value="'info'"/></s:elseif>
                                            <s:elseif test="stato.name() == 'QUALIFICATO'"><s:set var="badgeColor" value="'primary'"/></s:elseif>
                                            <s:elseif test="stato.name() == 'PREVENTIVO'"><s:set var="badgeColor" value="'warning'"/></s:elseif>
                                            <s:elseif test="stato.name() == 'NEGOZIAZIONE'"><s:set var="badgeColor" value="'danger'"/></s:elseif>
                                            <s:elseif test="stato.name() == 'VINTO'"><s:set var="badgeColor" value="'success'"/></s:elseif>
                                            <s:set var="badgeClass" value="'badge bg-' + badgeColor"/>
                                            <span class="${badgeClass}">
                                                <s:property value="stato"/>
                                            </span>
                                        </td>
                                        <td>
                                            <s:if test="budgetStimato != null">
                                                € <s:property value="budgetStimato"/>
                                            </s:if>
                                            <s:else>-</s:else>
                                        </td>
                                        <td>
                                            <div class="progress" style="height: 20px;">
                                                <div class="progress-bar" role="progressbar" 
                                                     style="width: <s:property value='probabilitaChiusura'/>%">
                                                    <s:property value="probabilitaChiusura"/>%
                                                </div>
                                            </div>
                                        </td>
                                        <td>
                                            <s:if test="dataContatto != null">
                                                <s:date name="dataContatto" format="dd/MM/yyyy" />
                                            </s:if>
                                            <s:else>-</s:else>
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
            $('#leadsTable').DataTable({
                language: {
                    url: 'https://cdn.datatables.net/plug-ins/1.13.4/i18n/it-IT.json'
                },
                pageLength: 25
            });
        });

        function confirmDelete(id) {
            if (confirm('Sei sicuro di voler eliminare questo lead?')) {
                window.location.href = '<s:url action="delete" namespace="/app/leads"/>' + '?id=' + id;
            }
        }
    </script>
</body>
</html>
