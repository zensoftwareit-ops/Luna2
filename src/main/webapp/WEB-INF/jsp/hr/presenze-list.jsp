<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
<!DOCTYPE html>
<html lang="it">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Presenze - Luna2</title>
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.0/font/bootstrap-icons.css">
</head>
<body>
<jsp:include page="/WEB-INF/jsp/includes/sidebar.jsp"/>

<div class="container-fluid" style="margin-left:250px; padding:24px; max-width: calc(100% - 250px);">
    <div class="d-flex justify-content-between align-items-center mb-4">
        <div>
            <h1 class="h3 mb-1"><i class="bi bi-clock-history me-2"></i>Presenze</h1>
            <p class="text-muted mb-0">Timbrature e assenze del mese corrente</p>
        </div>
        <div class="d-flex gap-2">
            <form action="checkin" method="post">
                <button class="btn btn-success" type="submit"><i class="bi bi-box-arrow-in-right me-1"></i>Check-in</button>
            </form>
            <form action="checkout" method="post">
                <button class="btn btn-danger" type="submit"><i class="bi bi-box-arrow-right me-1"></i>Check-out</button>
            </form>
        </div>
    </div>

    <s:if test="hasActionMessages()"><div class="alert alert-success"><s:actionmessage/></div></s:if>
    <s:if test="hasActionErrors()"><div class="alert alert-danger"><s:actionerror/></div></s:if>

    <div class="card border-0 shadow-sm mb-4">
        <div class="card-header bg-white"><strong>Registra assenza</strong></div>
        <div class="card-body">
            <form action="save-absence" method="post" class="row g-3 align-items-end">
                <div class="col-md-3">
                    <label class="form-label">Data</label>
                    <input type="date" name="recordDateInput" class="form-control" required>
                </div>
                <div class="col-md-3">
                    <label class="form-label">Tipo</label>
                    <select name="record.absenceType" class="form-select" required>
                        <option value="FERIE">FERIE</option>
                        <option value="PERMESSO">PERMESSO</option>
                        <option value="MALATTIA">MALATTIA</option>
                    </select>
                </div>
                <div class="col-md-4">
                    <label class="form-label">Note</label>
                    <input type="text" name="record.notes" class="form-control" maxlength="500">
                </div>
                <div class="col-md-2 d-grid">
                    <button class="btn btn-primary" type="submit">Salva assenza</button>
                </div>
            </form>
        </div>
    </div>

    <div class="card border-0 shadow-sm">
        <div class="table-responsive">
            <table class="table table-hover mb-0 align-middle">
                <thead class="table-light">
                <tr>
                    <th>Data</th>
                    <th>Check-in</th>
                    <th>Check-out</th>
                    <th>Ore lavorate</th>
                    <th>Straordinario</th>
                    <th>Assenza</th>
                    <th>Note</th>
                </tr>
                </thead>
                <tbody>
                <s:iterator value="records" var="r">
                    <tr>
                        <td><s:date name="#r.recordDate" format="dd/MM/yyyy"/></td>
                        <td><s:date name="#r.checkInTime" format="HH:mm"/></td>
                        <td><s:date name="#r.checkOutTime" format="HH:mm"/></td>
                        <td><s:property value="#r.hoursWorked"/></td>
                        <td><s:property value="#r.hoursOvertime"/></td>
                        <td><s:property value="#r.absenceType"/></td>
                        <td><s:property value="#r.notes"/></td>
                    </tr>
                </s:iterator>
                </tbody>
            </table>
        </div>
    </div>
</div>
</body>
</html>
