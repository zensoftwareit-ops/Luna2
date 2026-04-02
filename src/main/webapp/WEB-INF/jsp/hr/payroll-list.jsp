<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
<!DOCTYPE html>
<html lang="it">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Payroll - Luna2</title>
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.0/font/bootstrap-icons.css">
</head>
<body>
<jsp:include page="/WEB-INF/jsp/includes/sidebar.jsp"/>

<div class="container-fluid" style="margin-left:250px; padding:24px; max-width: calc(100% - 250px);">
    <div class="d-flex justify-content-between align-items-center mb-4">
        <div>
            <h1 class="h3 mb-1"><i class="bi bi-cash-coin me-2"></i>Payroll</h1>
            <p class="text-muted mb-0">Elaborazione cedolini base dal registro presenze</p>
        </div>
        <div class="d-flex gap-2">
            <form action="payroll-generate" method="post">
                <button class="btn btn-primary" type="submit"><i class="bi bi-play-circle me-1"></i>Ricalcola mese corrente</button>
            </form>
            <form action="payroll-confirm" method="post">
                <button class="btn btn-outline-success" type="submit"><i class="bi bi-lock me-1"></i>Conferma e blocca</button>
            </form>
            <form action="payroll-export" method="post">
                <button class="btn btn-outline-secondary" type="submit"><i class="bi bi-download me-1"></i>Export azienda CSV</button>
            </form>
            <form action="payroll-export" method="post" class="d-flex gap-2">
                <select name="selectedEmployeeId" class="form-select">
                    <option value="">Cedolino singolo...</option>
                    <s:iterator value="employees" var="e">
                        <option value="<s:property value='#e.id'/>"><s:property value="#e.nomeCompleto"/></option>
                    </s:iterator>
                </select>
                <button class="btn btn-outline-dark" type="submit">Export</button>
            </form>
        </div>
    </div>

    <div class="card border-0 shadow-sm mb-4">
        <div class="card-header bg-white"><strong>Parametri retributivi dipendente</strong></div>
        <div class="card-body">
            <form action="payroll-save-config" method="post" class="row g-3 align-items-end">
                <div class="col-lg-2">
                    <label class="form-label">Dipendente</label>
                    <select name="configEmployeeId" class="form-select" required>
                        <option value="">Seleziona...</option>
                        <s:iterator value="employees" var="e">
                            <option value="<s:property value='#e.id'/>"><s:property value="#e.nomeCompleto"/></option>
                        </s:iterator>
                    </select>
                </div>
                <div class="col-lg-2">
                    <label class="form-label">Tariffa oraria</label>
                    <input type="number" step="0.01" min="0" name="configHourlyRate" class="form-control" placeholder="15.00">
                </div>
                <div class="col-lg-2">
                    <label class="form-label">Moltiplicatore straord.</label>
                    <input type="number" step="0.01" min="0" name="configOvertimeMultiplier" class="form-control" placeholder="1.30">
                </div>
                <div class="col-lg-1">
                    <label class="form-label">Aliquota</label>
                    <input type="number" step="0.0001" min="0" max="1" name="configTaxRate" class="form-control" placeholder="0.20">
                </div>
                <div class="col-lg-1">
                    <label class="form-label">INPS</label>
                    <input type="number" step="0.0001" min="0" max="1" name="configSocialSecurityRate" class="form-control" placeholder="0.0919">
                </div>
                <div class="col-lg-1">
                    <label class="form-label">INAIL</label>
                    <input type="number" step="0.0001" min="0" max="1" name="configInsuranceRate" class="form-control" placeholder="0.0100">
                </div>
                <div class="col-lg-2">
                    <label class="form-label">Indennita</label>
                    <input type="number" step="0.01" min="0" name="configAllowance" class="form-control" placeholder="0.00">
                </div>
                <div class="col-lg-2">
                    <label class="form-label">Trattenute</label>
                    <input type="number" step="0.01" min="0" name="configDeduction" class="form-control" placeholder="0.00">
                </div>
                <div class="col-lg-1 d-grid">
                    <button class="btn btn-dark" type="submit">Salva</button>
                </div>
            </form>
        </div>
    </div>

    <s:if test="hasActionMessages()"><div class="alert alert-success"><s:actionmessage/></div></s:if>
    <s:if test="hasActionErrors()"><div class="alert alert-danger"><s:actionerror/></div></s:if>

    <div class="card border-0 shadow-sm mb-4">
        <div class="card-header bg-white"><strong>Elaborazioni</strong></div>
        <div class="table-responsive">
            <table class="table table-hover mb-0 align-middle">
                <thead class="table-light">
                <tr>
                    <th>Periodo</th>
                    <th>Versione</th>
                    <th>Stato</th>
                    <th>Totale Lordo</th>
                    <th>Totale Netto</th>
                </tr>
                </thead>
                <tbody>
                <s:iterator value="runs" var="r">
                    <tr>
                        <td><s:property value="#r.month"/>/<s:property value="#r.year"/></td>
                        <td>v<s:property value="#r.revision"/></td>
                        <td><span class="badge text-bg-secondary"><s:property value="#r.status"/></span></td>
                        <td>€ <s:property value="#r.totalGross"/></td>
                        <td>€ <s:property value="#r.totalNet"/></td>
                    </tr>
                </s:iterator>
                </tbody>
            </table>
        </div>
    </div>

    <div class="card border-0 shadow-sm">
        <div class="card-header bg-white"><strong>Dettaglio ultima elaborazione</strong></div>
        <div class="table-responsive">
            <table class="table table-hover mb-0 align-middle">
                <thead class="table-light">
                <tr>
                    <th>Dipendente</th>
                    <th>Ore</th>
                    <th>Straordinario</th>
                    <th>Lordo</th>
                    <th>Tasse</th>
                    <th>Contributi</th>
                    <th>Assicurazione</th>
                    <th>Indennita</th>
                    <th>Trattenute</th>
                    <th>Netto</th>
                </tr>
                </thead>
                <tbody>
                <s:iterator value="details" var="d">
                    <tr>
                        <td><s:property value="#d.employeeName"/></td>
                        <td><s:property value="#d.hoursWorked"/></td>
                        <td><s:property value="#d.hoursOvertime"/></td>
                        <td>€ <s:property value="#d.grossAmount"/></td>
                        <td>€ <s:property value="#d.taxAmount"/></td>
                        <td>€ <s:property value="#d.socialSecurityAmount"/></td>
                        <td>€ <s:property value="#d.insuranceAmount"/></td>
                        <td>€ <s:property value="#d.allowanceAmount"/></td>
                        <td>€ <s:property value="#d.deductionAmount"/></td>
                        <td>€ <s:property value="#d.netAmount"/></td>
                    </tr>
                </s:iterator>
                </tbody>
            </table>
        </div>
    </div>
</div>
</body>
</html>
