<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
<!DOCTYPE html>
<html lang="it">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Pending Approvazioni - Luna2</title>
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.0/font/bootstrap-icons.css">
</head>
<body>
<jsp:include page="/WEB-INF/jsp/includes/sidebar.jsp"/>

<div class="container-fluid" style="margin-left: 260px; padding: 24px; max-width: calc(100% - 260px);">
    <div class="d-flex justify-content-between align-items-center mb-4">
        <div>
            <h1 class="h3 mb-1"><i class="bi bi-check2-square me-2"></i>Richieste pendenti</h1>
            <p class="text-muted mb-0">Richieste ferie da approvare o rifiutare</p>
        </div>
        <div class="badge bg-dark fs-6">Totale: <s:property value="pendingCount"/></div>
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
                            <th>Dipendente</th>
                            <th>Periodo</th>
                            <th>Giorni</th>
                            <th>Descrizione</th>
                            <th class="text-end">Decisione</th>
                        </tr>
                        </thead>
                        <tbody>
                        <s:iterator value="approvalRequests" var="r">
                            <tr>
                                <td><s:property value="#r.id"/></td>
                                <td><s:property value="#r.requesterId"/></td>
                                <td>
                                    <strong><s:date name="#r.startDate" format="dd/MM/yyyy"/></strong>
                                    -
                                    <strong><s:date name="#r.endDate" format="dd/MM/yyyy"/></strong>
                                </td>
                                <td><s:property value="#r.daysRequested"/></td>
                                <td><s:property value="#r.description" default="-"/></td>
                                <td class="text-end">
                                    <form action="approve" method="post" style="display:inline;">
                                        <input type="hidden" name="requestId" value="<s:property value='#r.id'/>"/>
                                        <button class="btn btn-sm btn-success" type="submit">
                                            <i class="bi bi-check-circle me-1"></i>Approva
                                        </button>
                                    </form>

                                    <button class="btn btn-sm btn-outline-danger" type="button"
                                            data-bs-toggle="collapse" data-bs-target="#reject-<s:property value='#r.id'/>"
                                            aria-expanded="false" aria-controls="reject-<s:property value='#r.id'/>">
                                        <i class="bi bi-x-circle me-1"></i>Rifiuta
                                    </button>
                                </td>
                            </tr>
                            <tr>
                                <td colspan="6" class="p-0 border-0">
                                    <div class="collapse" id="reject-<s:property value='#r.id'/>">
                                        <div class="p-3 bg-light border-top border-bottom">
                                            <form action="reject" method="post" class="row g-2 align-items-end">
                                                <input type="hidden" name="requestId" value="<s:property value='#r.id'/>"/>
                                                <div class="col-md-9">
                                                    <label class="form-label mb-1">Motivo rifiuto</label>
                                                    <input type="text" name="rejectionReason" class="form-control" maxlength="500" required>
                                                </div>
                                                <div class="col-md-3 d-grid">
                                                    <button type="submit" class="btn btn-danger">
                                                        <i class="bi bi-send me-1"></i>Conferma rifiuto
                                                    </button>
                                                </div>
                                            </form>
                                        </div>
                                    </div>
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
                        <i class="bi bi-check-all me-1"></i>Nessuna richiesta pendente.
                    </div>
                </div>
            </s:else>
        </div>
    </div>
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
