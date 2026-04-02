<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
<!DOCTYPE html>
<html lang="it">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Amministrazione Operativa - Luna2</title>
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.0/font/bootstrap-icons.css">
</head>
<body>
<jsp:include page="/WEB-INF/jsp/includes/sidebar.jsp"/>

<div class="container-fluid" style="margin-left:250px; padding:24px; max-width: calc(100% - 250px);">
    <div class="mb-4 d-flex justify-content-between align-items-center">
        <div>
            <h1 class="h3 mb-1"><i class="bi bi-building-gear me-2"></i>Amministrazione Operativa</h1>
            <p class="text-muted mb-0">Workflow integrato sui 7 cantieri: payroll, fiscale, tesoreria, passivo, chiusure, compliance e KPI.</p>
        </div>
    </div>

    <s:if test="hasActionMessages()"><div class="alert alert-success"><s:actionmessage/></div></s:if>
    <s:if test="hasActionErrors()"><div class="alert alert-danger"><s:actionerror/></div></s:if>

    <div class="row g-3 mb-4">
        <div class="col-md-4"><div class="card border-0 shadow-sm"><div class="card-body"><small class="text-muted">Da avviare</small><div class="h4 mb-0"><s:property value="totalOpen"/></div></div></div></div>
        <div class="col-md-4"><div class="card border-0 shadow-sm"><div class="card-body"><small class="text-muted">In corso</small><div class="h4 mb-0"><s:property value="totalInProgress"/></div></div></div></div>
        <div class="col-md-4"><div class="card border-0 shadow-sm"><div class="card-body"><small class="text-muted">Scadute</small><div class="h4 mb-0 text-danger"><s:property value="totalOverdue"/></div></div></div></div>
    </div>

    <div class="card border-0 shadow-sm mb-4">
        <div class="card-header bg-white"><strong>Nuova operazione amministrativa</strong></div>
        <div class="card-body">
            <form action="ops-save" method="post" class="row g-3 align-items-end">
                <div class="col-md-3">
                    <label class="form-label">Area</label>
                    <select name="operation.area" class="form-select" required>
                        <s:iterator value="areas" var="a"><option value="<s:property/>"><s:property/></option></s:iterator>
                    </select>
                </div>
                <div class="col-md-3">
                    <label class="form-label">Tipo</label>
                    <input type="text" name="operation.operationType" class="form-control" required placeholder="F24 / LIPE / BANK_RECON ...">
                </div>
                <div class="col-md-3">
                    <label class="form-label">Titolo</label>
                    <input type="text" name="operation.title" class="form-control" required>
                </div>
                <div class="col-md-2">
                    <label class="form-label">Scadenza</label>
                    <input type="date" name="dueDateInput" class="form-control">
                </div>
                <div class="col-md-1 d-grid">
                    <button class="btn btn-primary" type="submit">Salva</button>
                </div>
                <div class="col-md-2">
                    <label class="form-label">Stato</label>
                    <select name="operation.status" class="form-select">
                        <s:iterator value="statuses" var="st"><option value="<s:property/>"><s:property/></option></s:iterator>
                    </select>
                </div>
                <div class="col-md-2">
                    <label class="form-label">Importo</label>
                    <input type="number" step="0.01" min="0" name="operation.amountDue" class="form-control">
                </div>
                <div class="col-md-2">
                    <label class="form-label">Controparte</label>
                    <input type="text" name="operation.counterparty" class="form-control">
                </div>
                <div class="col-md-2">
                    <label class="form-label">Riferimento</label>
                    <input type="text" name="operation.referenceCode" class="form-control">
                </div>
                <div class="col-md-4">
                    <label class="form-label">Note</label>
                    <input type="text" name="operation.notes" class="form-control">
                </div>
            </form>
        </div>
    </div>

    <div class="card border-0 shadow-sm">
        <div class="card-header bg-white"><strong>Backlog operativo integrato</strong></div>
        <div class="table-responsive">
            <table class="table table-hover align-middle mb-0">
                <thead class="table-light">
                    <tr>
                        <th>Area</th>
                        <th>Tipo</th>
                        <th>Titolo</th>
                        <th>Scadenza</th>
                        <th>Importo</th>
                        <th>Stato</th>
                        <th>Azione</th>
                    </tr>
                </thead>
                <tbody>
                    <s:iterator value="operations" var="o">
                        <tr>
                            <td><span class="badge text-bg-light border"><s:property value="#o.area"/></span></td>
                            <td><s:property value="#o.operationType"/></td>
                            <td><s:property value="#o.title"/></td>
                            <td><s:date name="#o.dueDate" format="dd/MM/yyyy"/></td>
                            <td>€ <s:property value="#o.amountDue"/></td>
                            <td><span class="badge text-bg-secondary"><s:property value="#o.status"/></span></td>
                            <td>
                                <s:if test="#o.status.toString() != 'COMPLETED'">
                                    <form action="ops-complete" method="post" class="d-inline">
                                        <input type="hidden" name="id" value="<s:property value='#o.id'/>">
                                        <button class="btn btn-sm btn-outline-success" type="submit">Completa</button>
                                    </form>
                                </s:if>
                            </td>
                        </tr>
                    </s:iterator>
                </tbody>
            </table>
        </div>
    </div>
</div>
</body>
</html>
