<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
<!DOCTYPE html>
<html lang="it">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Le Mie Richieste Ferie - Luna2</title>
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.0/font/bootstrap-icons.css">
</head>
<body>
<jsp:include page="/WEB-INF/jsp/includes/sidebar.jsp"/>

<div class="container-fluid" style="margin-left: 260px; padding: 24px; max-width: calc(100% - 260px);">
    <div class="d-flex justify-content-between align-items-center mb-4">
        <div>
            <h1 class="h3 mb-1"><i class="bi bi-calendar3 me-2"></i>Le mie richieste ferie</h1>
            <p class="text-muted mb-0">Storico e stato delle tue richieste</p>
        </div>
        <a href="ferie-form" class="btn btn-primary">
            <i class="bi bi-plus-circle me-1"></i>Nuova richiesta
        </a>
    </div>

    <s:if test="hasActionMessages()">
        <div class="alert alert-success"><s:actionmessage/></div>
    </s:if>

    <s:if test="hasActionErrors()">
        <div class="alert alert-danger"><s:actionerror/></div>
    </s:if>

    <div class="card shadow-sm border-0">
        <div class="card-body p-0">
            <s:if test="approvalRequests != null && !approvalRequests.isEmpty()">
                <div class="table-responsive">
                    <table class="table table-hover mb-0 align-middle">
                        <thead class="table-light">
                        <tr>
                            <th>#</th>
                            <th>Periodo</th>
                            <th>Giorni</th>
                            <th>Stato</th>
                            <th>Descrizione</th>
                            <th class="text-end">Azioni</th>
                        </tr>
                        </thead>
                        <tbody>
                        <s:iterator value="approvalRequests" var="r">
                            <tr>
                                <td><s:property value="#r.id"/></td>
                                <td>
                                    <strong><s:date name="#r.startDate" format="dd/MM/yyyy"/></strong>
                                    -
                                    <strong><s:date name="#r.endDate" format="dd/MM/yyyy"/></strong>
                                </td>
                                <td><s:property value="#r.daysRequested"/></td>
                                <td>
                                    <s:if test="#r.status == 'DRAFT'"><span class="badge bg-secondary">DRAFT</span></s:if>
                                    <s:elseif test="#r.status == 'SUBMITTED'"><span class="badge bg-warning text-dark">SUBMITTED</span></s:elseif>
                                    <s:elseif test="#r.status == 'APPROVED'"><span class="badge bg-success">APPROVED</span></s:elseif>
                                    <s:elseif test="#r.status == 'REJECTED'"><span class="badge bg-danger">REJECTED</span></s:elseif>
                                    <s:else><span class="badge bg-info">COMPLETED</span></s:else>
                                </td>
                                <td><s:property value="#r.description" default="-"/></td>
                                <td class="text-end">
                                    <s:if test="#r.status == 'DRAFT'">
                                        <form action="ferie-submit" method="post" style="display:inline;">
                                            <input type="hidden" name="requestId" value="<s:property value='#r.id'/>"/>
                                            <button type="submit" class="btn btn-sm btn-outline-primary">
                                                <i class="bi bi-send me-1"></i>Sottoponi
                                            </button>
                                        </form>
                                    </s:if>
                                    <s:if test="#r.status == 'REJECTED' && #r.rejectionReason != null">
                                        <button class="btn btn-sm btn-outline-danger" type="button" data-bs-toggle="tooltip" title="<s:property value='#r.rejectionReason'/>">
                                            <i class="bi bi-info-circle"></i>
                                        </button>
                                    </s:if>
                                </td>
                            </tr>
                        </s:iterator>
                        </tbody>
                    </table>
                </div>
            </s:if>
            <s:else>
                <div class="p-4">
                    <div class="alert alert-info mb-0">
                        <i class="bi bi-info-circle me-1"></i>Nessuna richiesta ferie presente.
                    </div>
                </div>
            </s:else>
        </div>
    </div>
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
<script>
    var tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
    tooltipTriggerList.forEach(function(el) {
        new bootstrap.Tooltip(el);
    });
</script>
</body>
</html>
