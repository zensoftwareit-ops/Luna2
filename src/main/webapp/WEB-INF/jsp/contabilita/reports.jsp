<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="s" uri="/struts-tags" %>
<!DOCTYPE html>
<html lang="it">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Report Contabilita - Luna2</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.10.0/font/bootstrap-icons.css">
    <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
</head>
<body>
<%@ include file="../includes/sidebar.jsp" %>

<div class="col-md-10 content-wrapper p-4">
    <div class="container-fluid">
        <div class="d-flex justify-content-between align-items-center mb-4">
            <h1 class="h3"><i class="bi bi-file-earmark-bar-graph me-2"></i>Centro Report Contabilita</h1>
            <a href="<s:url action='dashboard' namespace='/app/contabilita'/>" class="btn btn-outline-secondary">Dashboard</a>
        </div>

        <s:if test="hasActionMessages()"><div class="alert alert-success"><s:actionmessage/></div></s:if>
        <s:if test="hasActionErrors()"><div class="alert alert-danger"><s:actionerror/></div></s:if>

        <div class="card border-0 shadow-sm mb-4">
            <div class="card-body">
                <h5 class="mb-3">Selezione Report e Filtri Avanzati</h5>
                <form action="<s:url action='report-preset-apply' namespace='/app/contabilita'/>" method="post" class="row g-2 align-items-end mb-3">
                    <div class="col-md-4">
                        <label class="form-label">Preset salvati</label>
                        <s:select name="selectedPreset" list="availableReportPresets" headerKey="" headerValue="Seleziona preset" cssClass="form-select"/>
                    </div>
                    <div class="col-md-8 d-flex gap-2">
                        <button type="submit" class="btn btn-outline-primary">Applica preset</button>
                        <button type="submit" formaction="<s:url action='report-preset-delete' namespace='/app/contabilita'/>" class="btn btn-outline-danger">Elimina preset</button>
                    </div>
                </form>

                <form action="<s:url action='reports' namespace='/app/contabilita'/>" method="get" class="row g-3 align-items-end">
                    <div class="col-md-3">
                        <label class="form-label">Report</label>
                        <s:select name="reportType" list="availableReportTypes" cssClass="form-select"/>
                    </div>
                    <div class="col-md-2">
                        <label class="form-label">Anno</label>
                        <input type="number" min="2000" max="2100" class="form-control" name="annoEsercizio" value="<s:property value='annoEsercizio'/>">
                    </div>
                    <div class="col-md-2">
                        <label class="form-label">Data da</label>
                        <input type="date" class="form-control" name="reportDateFrom" value="<s:property value='reportDateFrom'/>">
                    </div>
                    <div class="col-md-2">
                        <label class="form-label">Data a</label>
                        <input type="date" class="form-control" name="reportDateTo" value="<s:property value='reportDateTo'/>">
                    </div>
                    <div class="col-md-2">
                        <label class="form-label">Stato</label>
                        <s:select name="reportStatus" list="availableReportStatuses" cssClass="form-select"/>
                    </div>
                    <div class="col-md-3">
                        <label class="form-label">Conto</label>
                        <select name="reportAccountId" class="form-select">
                            <option value="">Tutti</option>
                            <s:iterator value="reportAccounts" var="acc">
                                <option value="<s:property value='#acc.id'/>" <s:if test="#acc.id == reportAccountId">selected</s:if>>
                                    <s:property value="#acc.code"/> - <s:property value="#acc.name"/>
                                </option>
                            </s:iterator>
                        </select>
                    </div>
                    <div class="col-md-3">
                        <label class="form-label">Tipo documento</label>
                        <s:select name="reportDocumentType" list="availableDocumentTypes" cssClass="form-select"/>
                    </div>
                    <div class="col-md-6 d-flex gap-2">
                        <button type="submit" name="preview" value="1" class="btn btn-primary">
                            <i class="bi bi-eye me-1"></i>Anteprima
                        </button>
                        <button type="submit" name="preview" value="1" class="btn btn-outline-secondary" formaction="<s:url action='report-preset-save' namespace='/app/contabilita'/>" formmethod="post">
                            <i class="bi bi-save me-1"></i>Salva preset
                        </button>
                        <input type="text" name="presetName" class="form-control" style="max-width: 240px;" placeholder="Nome preset" value="<s:property value='presetName'/>">
                        <a class="btn btn-outline-primary"
                           href="<s:url action='report-pdf' namespace='/app/contabilita'><s:param name='reportType' value='reportType'/><s:param name='annoEsercizio' value='annoEsercizio'/><s:param name='reportDateFrom' value='reportDateFrom'/><s:param name='reportDateTo' value='reportDateTo'/><s:param name='reportStatus' value='reportStatus'/><s:param name='reportAccountId' value='reportAccountId'/><s:param name='reportDocumentType' value='reportDocumentType'/></s:url>">
                            <i class="bi bi-filetype-pdf me-1"></i>Scarica PDF
                        </a>
                        <a class="btn btn-outline-success"
                           href="<s:url action='report-excel' namespace='/app/contabilita'><s:param name='reportType' value='reportType'/><s:param name='annoEsercizio' value='annoEsercizio'/><s:param name='reportDateFrom' value='reportDateFrom'/><s:param name='reportDateTo' value='reportDateTo'/><s:param name='reportStatus' value='reportStatus'/><s:param name='reportAccountId' value='reportAccountId'/><s:param name='reportDocumentType' value='reportDocumentType'/></s:url>">
                            <i class="bi bi-file-earmark-spreadsheet me-1"></i>Scarica XLSX
                        </a>
                        <a class="btn btn-outline-dark"
                           href="<s:url action='report-csv' namespace='/app/contabilita'><s:param name='reportType' value='reportType'/><s:param name='annoEsercizio' value='annoEsercizio'/><s:param name='reportDateFrom' value='reportDateFrom'/><s:param name='reportDateTo' value='reportDateTo'/><s:param name='reportStatus' value='reportStatus'/><s:param name='reportAccountId' value='reportAccountId'/><s:param name='reportDocumentType' value='reportDocumentType'/></s:url>">
                            <i class="bi bi-filetype-csv me-1"></i>Scarica CSV
                        </a>
                    </div>
                </form>
            </div>
        </div>

        <s:if test="previewHeaders != null && !previewHeaders.isEmpty()">
        <div class="card border-0 shadow-sm mb-4">
            <div class="card-header bg-white">
                <h5 class="mb-0">Anteprima: <s:property value="previewTitle"/></h5>
            </div>
            <div class="card-body">
                <div class="d-flex justify-content-between align-items-center mb-3">
                    <div>
                        <strong>Righe totali:</strong> <s:property value="previewTotalRows"/> |
                        <strong>Pagina:</strong> <s:property value="previewPage"/> / <s:property value="previewTotalPages"/>
                    </div>
                    <form action="<s:url action='reports' namespace='/app/contabilita'/>" method="get" class="d-flex gap-2 align-items-center">
                        <input type="hidden" name="preview" value="1">
                        <input type="hidden" name="reportType" value="<s:property value='reportType'/>">
                        <input type="hidden" name="annoEsercizio" value="<s:property value='annoEsercizio'/>">
                        <input type="hidden" name="reportDateFrom" value="<s:property value='reportDateFrom'/>">
                        <input type="hidden" name="reportDateTo" value="<s:property value='reportDateTo'/>">
                        <input type="hidden" name="reportStatus" value="<s:property value='reportStatus'/>">
                        <input type="hidden" name="reportAccountId" value="<s:property value='reportAccountId'/>">
                        <input type="hidden" name="reportDocumentType" value="<s:property value='reportDocumentType'/>">
                        <label class="small text-muted mb-0">Righe/pagina</label>
                        <s:select name="previewPageSize" list="availablePreviewPageSizes" cssClass="form-select form-select-sm"/>
                        <button type="submit" class="btn btn-sm btn-outline-secondary">Aggiorna</button>
                    </form>
                </div>
                <div class="table-responsive" style="max-height: 460px; overflow: auto;">
                    <table class="table table-sm table-striped mb-0">
                        <thead class="table-light">
                        <tr>
                            <s:iterator value="previewHeaders" var="h"><th><s:property value="#h"/></th></s:iterator>
                        </tr>
                        </thead>
                        <tbody>
                        <s:iterator value="previewRows" var="row">
                            <tr>
                                <s:iterator value="#row" var="cell"><td><s:property value="#cell"/></td></s:iterator>
                            </tr>
                        </s:iterator>
                        </tbody>
                    </table>
                </div>
                <div class="mt-3 d-flex gap-2">
                    <a class="btn btn-sm btn-outline-secondary <s:if test='previewPage <= 1'>disabled</s:if>"
                       href="<s:url action='reports' namespace='/app/contabilita'><s:param name='preview' value='%{"1"}'/><s:param name='previewPage' value='%{previewPage - 1}'/><s:param name='previewPageSize' value='previewPageSize'/><s:param name='reportType' value='reportType'/><s:param name='annoEsercizio' value='annoEsercizio'/><s:param name='reportDateFrom' value='reportDateFrom'/><s:param name='reportDateTo' value='reportDateTo'/><s:param name='reportStatus' value='reportStatus'/><s:param name='reportAccountId' value='reportAccountId'/><s:param name='reportDocumentType' value='reportDocumentType'/></s:url>">Precedente</a>
                    <a class="btn btn-sm btn-outline-secondary <s:if test='previewPage >= previewTotalPages'>disabled</s:if>"
                       href="<s:url action='reports' namespace='/app/contabilita'><s:param name='preview' value='%{"1"}'/><s:param name='previewPage' value='%{previewPage + 1}'/><s:param name='previewPageSize' value='previewPageSize'/><s:param name='reportType' value='reportType'/><s:param name='annoEsercizio' value='annoEsercizio'/><s:param name='reportDateFrom' value='reportDateFrom'/><s:param name='reportDateTo' value='reportDateTo'/><s:param name='reportStatus' value='reportStatus'/><s:param name='reportAccountId' value='reportAccountId'/><s:param name='reportDocumentType' value='reportDocumentType'/></s:url>">Successiva</a>
                </div>
            </div>
        </div>
        </s:if>

        <s:if test="reportType == 'CONSOLIDATO_MENSILE' && trendLabels != null && !trendLabels.isEmpty()">
        <div class="card border-0 shadow-sm mb-4">
            <div class="card-header bg-white"><h5 class="mb-0">Trend Consolidato Mensile</h5></div>
            <div class="card-body">
                <canvas id="trendChart" height="120"></canvas>
            </div>
        </div>
        </s:if>

        <div class="card border-0 shadow-sm">
            <div class="card-header bg-white"><h5 class="mb-0">Catalogo Report Disponibili</h5></div>
            <div class="table-responsive">
                <table class="table table-striped mb-0">
                    <thead class="table-light"><tr><th>Report</th><th>Descrizione</th></tr></thead>
                    <tbody>
                    <s:iterator value="reportDefinitions" var="rep">
                        <tr><td><strong><s:property value="#rep.title"/></strong></td><td><s:property value="#rep.description"/></td></tr>
                    </s:iterator>
                    </tbody>
                </table>
            </div>
        </div>
    </div>
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
<s:if test="reportType == 'CONSOLIDATO_MENSILE' && trendLabels != null && !trendLabels.isEmpty()">
<script>
    const trendLabels = [<s:iterator value="trendLabels" var="l" status="st">'<s:property value="#l"/>'<s:if test="!#st.last">,</s:if></s:iterator>];
    const trendRicavi = [<s:iterator value="trendRicavi" var="v" status="st"><s:property value="#v"/><s:if test="!#st.last">,</s:if></s:iterator>];
    const trendCosti = [<s:iterator value="trendCosti" var="v" status="st"><s:property value="#v"/><s:if test="!#st.last">,</s:if></s:iterator>];
    const trendRisultato = [<s:iterator value="trendRisultato" var="v" status="st"><s:property value="#v"/><s:if test="!#st.last">,</s:if></s:iterator>];

    const ctx = document.getElementById('trendChart');
    if (ctx) {
        new Chart(ctx, {
            type: 'line',
            data: {
                labels: trendLabels,
                datasets: [
                    {label: 'Ricavi', data: trendRicavi, borderColor: '#198754', backgroundColor: 'rgba(25,135,84,0.15)', tension: 0.25},
                    {label: 'Costi', data: trendCosti, borderColor: '#dc3545', backgroundColor: 'rgba(220,53,69,0.15)', tension: 0.25},
                    {label: 'Risultato', data: trendRisultato, borderColor: '#0d6efd', backgroundColor: 'rgba(13,110,253,0.15)', tension: 0.25}
                ]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                scales: { y: { beginAtZero: true } }
            }
        });
    }
</script>
</s:if>
</body>
</html>
