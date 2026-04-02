<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
<!DOCTYPE html>
<html lang="it">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Scadenzario Fiscale - Luna2</title>
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.0/font/bootstrap-icons.css">
    <style>
        body { background-color: #f8f9fa; }
        .content-wrapper { margin-left: 260px; padding: 20px; }
        .stat-card { border-left: 4px solid #007bff; box-shadow: 0 1px 3px rgba(0,0,0,0.1); }
        .stat-card.overdue { border-left-color: #dc3545; }
        .stat-card.upcoming { border-left-color: #ffc107; }
        .deadline-row { padding: 12px; border-bottom: 1px solid #e9ecef; }
        .deadline-row:hover { background-color: #f8f9fa; }
        .deadline-type { display: inline-block; padding: 4px 8px; border-radius: 3px; font-size: 0.85rem; }
        .type-iva { background-color: #e7f3ff; color: #004085; }
        .type-inps { background-color: #fff3cd; color: #856404; }
        .type-irpef { background-color: #d4edda; color: #155724; }
        .type-f24 { background-color: #f8d7da; color: #721c24; }
        .action-buttons { text-align: right; }
        .btn-sm { padding: 4px 8px; font-size: 0.85rem; }
    </style>
</head>
<body>
    <!-- Sidebar (incluso dal layout master) -->
    <jsp:include page="/WEB-INF/jsp/includes/sidebar.jsp"/>

    <div class="content-wrapper">
        <div class="container-fluid">
            <!-- Header -->
            <div class="d-flex justify-content-between align-items-center mb-4">
                <div>
                    <h1 class="h3"><i class="bi bi-calendar2-check me-2"></i>Scadenzario Fiscale</h1>
                    <p class="text-muted">Gestione scadenze fiscali obbligatorie</p>
                </div>
                <button class="btn btn-primary" data-bs-toggle="modal" data-bs-target="#createDeadlineModal">
                    <i class="bi bi-plus-circle me-1"></i>Nuova Scadenza
                </button>
            </div>

            <!-- Messages -->
            <s:if test="hasActionMessages()">
                <div class="alert alert-success alert-dismissible fade show" role="alert">
                    <s:actionmessage/>
                    <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
                </div>
            </s:if>
            <s:if test="hasActionErrors()">
                <div class="alert alert-danger alert-dismissible fade show" role="alert">
                    <s:actionerror/>
                    <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
                </div>
            </s:if>

            <!-- KPI Cards -->
            <div class="row mb-4">
                <div class="col-md-4">
                    <div class="card stat-card">
                        <div class="card-body">
                            <h6 class="text-muted mb-2">Scadute</h6>
                            <h2 class="text-danger mb-0"><s:property value="overdueDeadlines.size()"/></h2>
                            <small class="text-muted"><i class="bi bi-exclamation-circle"></i> Richiede attenzione</small>
                        </div>
                    </div>
                </div>
                <div class="col-md-4">
                    <div class="card stat-card upcoming">
                        <div class="card-body">
                            <h6 class="text-muted mb-2">Prossimi 7 Giorni</h6>
                            <h2 class="text-warning mb-0"><s:property value="upcomingDeadlines.size()"/></h2>
                            <small class="text-muted"><i class="bi bi-hourglass-split"></i> Imminenti</small>
                        </div>
                    </div>
                </div>
                <div class="col-md-4">
                    <div class="card stat-card">
                        <div class="card-body">
                            <h6 class="text-muted mb-2">Totale Aperte</h6>
                            <h2 class="text-primary mb-0"><s:property value="deadlines.size()"/></h2>
                            <small class="text-muted"><i class="bi bi-calendar"></i> In gestione</small>
                        </div>
                    </div>
                </div>
            </div>

            <!-- Filtri -->
            <div class="card mb-3">
                <div class="card-body">
                    <form action="scadenzario-list" method="get" class="row g-3">
                        <div class="col-md-4">
                            <label class="form-label">Stato</label>
                            <select name="filterStatus" class="form-select">
                                <option value="">Tutti</option>
                                <option value="OVERDUE" <s:if test="filterStatus == 'OVERDUE'">selected</s:if>>Scadute</option>
                                <option value="UPCOMING" <s:if test="filterStatus == 'UPCOMING'">selected</s:if>>Prossimi 30 gg</option>
                            </select>
                        </div>
                        <div class="col-md-4">
                            <label class="form-label">Tipo</label>
                            <select name="filterType" class="form-select">
                                <option value="">Tutti i tipi</option>
                                <option value="IVA" <s:if test="filterType == 'IVA'">selected</s:if>>IVA</option>
                                <option value="INPS" <s:if test="filterType == 'INPS'">selected</s:if>>INPS</option>
                                <option value="IRPEF" <s:if test="filterType == 'IRPEF'">selected</s:if>>IRPEF</option>
                                <option value="F24" <s:if test="filterType == 'F24'">selected</s:if>>F24</option>
                            </select>
                        </div>
                        <div class="col-md-4">
                            <label class="form-label">&nbsp;</label>
                            <button type="submit" class="btn btn-outline-secondary w-100">
                                <i class="bi bi-funnel me-1"></i>Filtra
                            </button>
                        </div>
                    </form>
                </div>
            </div>

            <!-- Tabella Scadenze -->
            <div class="card">
                <div class="card-header bg-light">
                    <h5 class="mb-0">Scadenze Fiscali</h5>
                </div>
                <div class="card-body">
                    <s:if test="deadlines != null && deadlines.size() > 0">
                        <div class="table-responsive">
                            <table class="table table-sm table-hover mb-0">
                                <thead class="table-light">
                                    <tr>
                                        <th>Data Scadenza</th>
                                        <th>Titolo</th>
                                        <th>Tipo</th>
                                        <th>Stato</th>
                                        <th>Frequenza</th>
                                        <th>Importo</th>
                                        <th>Azioni</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <s:iterator value="deadlines" var="deadline">
                                        <tr class="deadline-row">
                                            <td>
                                                <strong><s:date name="deadlineDate" format="dd/MM/yyyy"/></strong>
                                            </td>
                                            <td>
                                                <s:property value="title"/>
                                                <s:if test="notes != null && !notes.isEmpty()">
                                                    <br><small class="text-muted"><s:property value="notes"/></small>
                                                </s:if>
                                            </td>
                                            <td>
                                                <span class="deadline-type type-<s:property value='#deadline.type.name().toLowerCase()'/>">
                                                    <s:property value="#deadline.type.name()"/>
                                                </span>
                                            </td>
                                            <td>
                                                <s:if test="#deadline.status.name() == 'OPEN'">
                                                    <span class="badge bg-warning">APERTA</span>
                                                </s:if>
                                                <s:elseif test="#deadline.status.name() == 'OVERDUE'">
                                                    <span class="badge bg-danger">SCADUTA</span>
                                                </s:elseif>
                                                <s:else>
                                                    <span class="badge bg-success">COMPLETATA</span>
                                                </s:else>
                                            </td>
                                            <td>
                                                <small><s:property value="#deadline.frequency.name()"/></small>
                                            </td>
                                            <td>
                                                <s:if test="amountDue != null">
                                                    € <s:text name="format.decimal"><s:param value="amountDue"/></s:text>
                                                </s:if>
                                                <s:else>-</s:else>
                                            </td>
                                            <td class="action-buttons">
                                                <button class="btn btn-sm btn-outline-primary" data-bs-toggle="modal" data-bs-target="#editDeadlineModal" 
                                                        onclick="editDeadline(<s:property value='id'/>)">
                                                    <i class="bi bi-pencil"></i>
                                                </button>
                                                <s:if test="#deadline.status.name() == 'OPEN'">
                                                    <form action="scadenzario-complete" method="post" style="display:inline;">
                                                        <input type="hidden" name="id" value="<s:property value='id'/>"/>
                                                        <button type="submit" class="btn btn-sm btn-outline-success" title="Segna come completata">
                                                            <i class="bi bi-check-circle"></i>
                                                        </button>
                                                    </form>
                                                </s:if>
                                                <form action="scadenzario-delete" method="post" style="display:inline;" 
                                                      onsubmit="return confirm('Confermi eliminazione?');">
                                                    <input type="hidden" name="id" value="<s:property value='id'/>"/>
                                                    <button type="submit" class="btn btn-sm btn-outline-danger">
                                                        <i class="bi bi-trash"></i>
                                                    </button>
                                                </form>
                                            </td>
                                        </tr>
                                    </s:iterator>
                                </tbody>
                            </table>
                        </div>
                    </s:if>
                    <s:else>
                        <div class="alert alert-info mb-0">
                            <i class="bi bi-info-circle me-2"></i>Nessuna scadenza trovata
                        </div>
                    </s:else>
                </div>
            </div>

            <!-- Seed Data Button (solo admin) -->
            <s:if test="#session.currentUser != null && #session.currentUser.username == 'admin'">
                <div class="mt-3">
                    <form action="scadenzario-seed" method="post" onsubmit="return confirm('Popolo il scadenzario con le scadenze fiscali standard italiane?');">
                        <button type="submit" class="btn btn-outline-info">
                            <i class="bi bi-arrow-repeat me-1"></i>Popola Scadenzario Standard
                        </button>
                    </form>
                </div>
            </s:if>
        </div>
    </div>

    <!-- Modal Crea Scadenza -->
    <div class="modal fade" id="createDeadlineModal" tabindex="-1">
        <div class="modal-dialog">
            <div class="modal-content">
                <form action="scadenzario-save" method="post">
                    <div class="modal-header">
                        <h5 class="modal-title">Nuova Scadenza Fiscale</h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                    </div>
                    <div class="modal-body">
                        <div class="mb-3">
                            <label class="form-label">Titolo</label>
                            <input type="text" name="taxDeadline.title" class="form-control" placeholder="Es: F24 IVA Marzo" required>
                        </div>
                        <div class="mb-3">
                            <label class="form-label">Data Scadenza</label>
                            <input type="date" name="taxDeadline.deadlineDate" class="form-control" required>
                        </div>
                        <div class="mb-3">
                            <label class="form-label">Tipo</label>
                            <select name="taxDeadline.type" class="form-select" required>
                                <option value=""></option>
                                <option value="IVA">IVA</option>
                                <option value="INPS">INPS</option>
                                <option value="IRPEF">IRPEF</option>
                                <option value="IRES">IRES</option>
                                <option value="IRAP">IRAP</option>
                                <option value="F24">F24</option>
                                <option value="BOLLO">Bollo</option>
                                <option value="ALTRO">Altro</option>
                            </select>
                        </div>
                        <div class="mb-3">
                            <label class="form-label">Importo (opzionale)</label>
                            <input type="number" name="taxDeadline.amountDue" class="form-control" step="0.01" placeholder="0.00">
                        </div>
                        <div class="mb-3">
                            <label class="form-label">Note</label>
                            <textarea name="taxDeadline.notes" class="form-control" rows="3" placeholder="Note o istruzioni"></textarea>
                        </div>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Annulla</button>
                        <button type="submit" class="btn btn-primary">Salva Scadenza</button>
                    </div>
                </form>
            </div>
        </div>
    </div>

    <!-- Modal Modifica Scadenza -->
    <div class="modal fade" id="editDeadlineModal" tabindex="-1">
        <div class="modal-dialog">
            <div class="modal-content">
                <form action="scadenzario-edit" method="post" id="editForm">
                    <div class="modal-header">
                        <h5 class="modal-title">Modifica Scadenza</h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                    </div>
                    <div class="modal-body" id="editModalBody">
                        <!-- Popolato via JavaScript -->
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Annulla</button>
                        <button type="submit" class="btn btn-primary">Salva Modifiche</button>
                    </div>
                </form>
            </div>
        </div>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
    <script>
        function editDeadline(id) {
            // In una vera implementazione, caricheremmo i dati da un AJAX endpoint
            console.log("Edit deadline: " + id);
        }
    </script>
</body>
</html>
