<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="s" uri="/struts-tags" %>
<!DOCTYPE html>
<html lang="it">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Pipeline CRM - Luna2</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.10.0/font/bootstrap-icons.css">
    <style>
        body {
            overflow-x: auto;
        }
        .pipeline-container {
            display: flex;
            gap: 20px;
            min-width: max-content;
            padding-bottom: 20px;
        }
        .pipeline-column {
            flex: 0 0 300px;
            background-color: #f8f9fa;
            border-radius: 12px;
            padding: 15px;
            min-height: 600px;
            box-shadow: 0 2px 8px rgba(0,0,0,0.1);
        }
        .pipeline-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 15px;
            padding-bottom: 10px;
            border-bottom: 3px solid;
        }
        .pipeline-header h5 {
            margin: 0;
            font-size: 1rem;
            font-weight: 600;
        }
        .pipeline-count {
            background: white;
            padding: 4px 12px;
            border-radius: 20px;
            font-weight: 600;
            font-size: 0.85rem;
        }
        .lead-card {
            background: white;
            border-radius: 8px;
            padding: 15px;
            margin-bottom: 12px;
            cursor: grab;
            transition: all 0.3s;
            border-left: 4px solid;
            box-shadow: 0 2px 4px rgba(0,0,0,0.08);
        }
        .lead-card:hover {
            box-shadow: 0 4px 12px rgba(0,0,0,0.15);
            transform: translateY(-2px);
        }
        .lead-card:active {
            cursor: grabbing;
            opacity: 0.8;
        }
        .lead-card.dragging {
            opacity: 0.5;
        }
        .lead-card-header {
            font-weight: 600;
            font-size: 0.95rem;
            margin-bottom: 8px;
            color: #212529;
        }
        .lead-card-company {
            font-size: 0.85rem;
            color: #6c757d;
            margin-bottom: 8px;
        }
        .lead-card-value {
            font-size: 1.1rem;
            font-weight: 700;
            color: #0d6efd;
            margin-bottom: 10px;
        }
        .lead-card-footer {
            display: flex;
            justify-content: space-between;
            align-items: center;
            font-size: 0.75rem;
            color: #6c757d;
            border-top: 1px solid #e9ecef;
            padding-top: 8px;
            margin-top: 8px;
        }
        .probability-bar {
            height: 6px;
            background: #e9ecef;
            border-radius: 3px;
            overflow: hidden;
            margin-bottom: 8px;
        }
        .probability-fill {
            height: 100%;
            transition: width 0.3s;
        }
        
        /* Colori per colonna */
        .col-nuovo { border-bottom-color: #6c757d; }
        .col-nuovo .lead-card { border-left-color: #6c757d; }
        
        .col-contattato { border-bottom-color: #0dcaf0; }
        .col-contattato .lead-card { border-left-color: #0dcaf0; }
        
        .col-qualificato { border-bottom-color: #0d6efd; }
        .col-qualificato .lead-card { border-left-color: #0d6efd; }
        
        .col-preventivo { border-bottom-color: #ffc107; }
        .col-preventivo .lead-card { border-left-color: #ffc107; }
        
        .col-negoziazione { border-bottom-color: #fd7e14; }
        .col-negoziazione .lead-card { border-left-color: #fd7e14; }
        
        .col-vinto { border-bottom-color: #198754; }
        .col-vinto .lead-card { border-left-color: #198754; }
        
        .col-perso { border-bottom-color: #dc3545; }
        .col-perso .lead-card { border-left-color: #dc3545; }
        
        .drop-zone {
            min-height: 100px;
        }
        .drop-zone.drag-over {
            background: #e7f3ff;
            border: 2px dashed #0d6efd;
            border-radius: 8px;
        }
        .total-value {
            font-size: 0.75rem;
            color: #198754;
            font-weight: 600;
            margin-top: 5px;
        }
    </style>
</head>
<body>
    <%@ include file="../includes/sidebar.jsp" %>
    
    <div class="col-md-10 content-wrapper p-4">
        <div class="container-fluid">
            <!-- Header -->
            <div class="d-flex justify-content-between align-items-center mb-4">
                <div>
                    <h1 class="h3 mb-1"><i class="bi bi-kanban me-2"></i>Pipeline CRM</h1>
                    <p class="text-muted mb-0">Trascina i lead per modificare lo stato</p>
                </div>
                <div>
                    <a href="<s:url action='list' namespace='/app/lead'/>" class="btn btn-outline-secondary me-2">
                        <i class="bi bi-list-ul me-1"></i>Vista Lista
                    </a>
                    <a href="<s:url action='create' namespace='/app/lead'/>" class="btn btn-primary">
                        <i class="bi bi-plus-circle me-1"></i>Nuovo Lead
                    </a>
                </div>
            </div>

            <!-- KPI Summary -->
            <div class="row mb-4">
                <div class="col-md-3">
                    <div class="card border-primary">
                        <div class="card-body text-center">
                            <h6 class="text-muted mb-1">Lead Totali</h6>
                            <h2 class="mb-0 text-primary"><s:property value="leads.size()"/></h2>
                        </div>
                    </div>
                </div>
                <div class="col-md-3">
                    <div class="card border-warning">
                        <div class="card-body text-center">
                            <h6 class="text-muted mb-1">In Negoziazione</h6>
                            <h2 class="mb-0 text-warning">
                                <s:property value="leads.{? #this.stato == @it.zensoftware.luna2.model.Lead$Stato@NEGOZIAZIONE}.size()"/>
                            </h2>
                        </div>
                    </div>
                </div>
                <div class="col-md-3">
                    <div class="card border-success">
                        <div class="card-body text-center">
                            <h6 class="text-muted mb-1">Tasso Conversione</h6>
                            <h2 class="mb-0 text-success">
                                <s:if test="leads.{? #this.stato == @it.zensoftware.luna2.model.Lead$Stato@QUALIFICATO}.size() > 0">
                                    <s:property value="(leads.{? #this.stato == @it.zensoftware.luna2.model.Lead$Stato@VINTO}.size() * 100) / leads.{? #this.stato == @it.zensoftware.luna2.model.Lead$Stato@QUALIFICATO}.size()"/>%
                                </s:if>
                                <s:else>0%</s:else>
                            </h2>
                        </div>
                    </div>
                </div>
                <div class="col-md-3">
                    <div class="card border-info">
                        <div class="card-body text-center">
                            <h6 class="text-muted mb-1">Valore Pipeline</h6>
                            <h2 class="mb-0 text-info">
                                € <s:property value="getText('{0,number,#,##0}', {leads.{budgetStimato}.sum()})"/>
                            </h2>
                        </div>
                    </div>
                </div>
            </div>

            <!-- Pipeline Board -->
            <div class="pipeline-container">
                <!-- Colonna NUOVO -->
                <div class="pipeline-column col-nuovo" data-stato="NUOVO">
                    <div class="pipeline-header">
                        <h5><i class="bi bi-inbox me-2"></i>Nuovo</h5>
                        <span class="pipeline-count"><s:property value="leads.{? #this.stato == @it.zensoftware.luna2.model.Lead$Stato@NUOVO}.size()"/></span>
                    </div>
                    <div class="drop-zone">
                        <s:iterator value="leads.{? #this.stato == @it.zensoftware.luna2.model.Lead$Stato@NUOVO}" var="lead">
                            <div class="lead-card" draggable="true" data-lead-id="<s:property value='#lead.id'/>" 
                                 onclick="window.location='<s:url action='view' namespace='/app/lead'><s:param name='id' value='#lead.id'/></s:url>'">
                                <div class="lead-card-header"><s:property value="#lead.nomeContatto"/> <s:property value="#lead.cognomeContatto"/></div>
                                <div class="lead-card-company"><i class="bi bi-building me-1"></i><s:property value="#lead.azienda"/></div>
                                <s:if test="#lead.budgetStimato != null">
                                    <div class="lead-card-value">€ <s:text name="format.decimal"><s:param value="#lead.budgetStimato"/></s:text></div>
                                </s:if>
                                <div class="probability-bar">
                                    <div class="probability-fill bg-secondary" style="width: <s:property value='#lead.probabilitaChiusura'/>%"></div>
                                </div>
                                <div class="lead-card-footer">
                                    <span><i class="bi bi-calendar3"></i> <s:date name="#lead.dataContatto" format="dd/MM"/></span>
                                    <span><s:property value="#lead.probabilitaChiusura"/>%</span>
                                </div>
                            </div>
                        </s:iterator>
                    </div>
                    <div class="total-value">
                        Tot: € <s:property value="getText('{0,number,#,##0}', {leads.{? #this.stato == @it.zensoftware.luna2.model.Lead$Stato@NUOVO}.{budgetStimato}.sum()})"/>
                    </div>
                </div>

                <!-- Colonna CONTATTATO -->
                <div class="pipeline-column col-contattato" data-stato="CONTATTATO">
                    <div class="pipeline-header">
                        <h5><i class="bi bi-telephone me-2"></i>Contattato</h5>
                        <span class="pipeline-count"><s:property value="leads.{? #this.stato == @it.zensoftware.luna2.model.Lead$Stato@CONTATTATO}.size()"/></span>
                    </div>
                    <div class="drop-zone">
                        <s:iterator value="leads.{? #this.stato == @it.zensoftware.luna2.model.Lead$Stato@CONTATTATO}" var="lead">
                            <div class="lead-card" draggable="true" data-lead-id="<s:property value='#lead.id'/>"
                                 onclick="window.location='<s:url action='view' namespace='/app/lead'><s:param name='id' value='#lead.id'/></s:url>'">
                                <div class="lead-card-header"><s:property value="#lead.nomeContatto"/> <s:property value="#lead.cognomeContatto"/></div>
                                <div class="lead-card-company"><i class="bi bi-building me-1"></i><s:property value="#lead.azienda"/></div>
                                <s:if test="#lead.budgetStimato != null">
                                    <div class="lead-card-value">€ <s:text name="format.decimal"><s:param value="#lead.budgetStimato"/></s:text></div>
                                </s:if>
                                <div class="probability-bar">
                                    <div class="probability-fill bg-info" style="width: <s:property value='#lead.probabilitaChiusura'/>%"></div>
                                </div>
                                <div class="lead-card-footer">
                                    <span><i class="bi bi-calendar3"></i> <s:date name="#lead.dataContatto" format="dd/MM"/></span>
                                    <span><s:property value="#lead.probabilitaChiusura"/>%</span>
                                </div>
                            </div>
                        </s:iterator>
                    </div>
                    <div class="total-value">
                        Tot: € <s:property value="getText('{0,number,#,##0}', {leads.{? #this.stato == @it.zensoftware.luna2.model.Lead$Stato@CONTATTATO}.{budgetStimato}.sum()})"/>
                    </div>
                </div>

                <!-- Colonna QUALIFICATO -->
                <div class="pipeline-column col-qualificato" data-stato="QUALIFICATO">
                    <div class="pipeline-header">
                        <h5><i class="bi bi-check-circle me-2"></i>Qualificato</h5>
                        <span class="pipeline-count"><s:property value="leads.{? #this.stato == @it.zensoftware.luna2.model.Lead$Stato@QUALIFICATO}.size()"/></span>
                    </div>
                    <div class="drop-zone">
                        <s:iterator value="leads.{? #this.stato == @it.zensoftware.luna2.model.Lead$Stato@QUALIFICATO}" var="lead">
                            <div class="lead-card" draggable="true" data-lead-id="<s:property value='#lead.id'/>"
                                 onclick="window.location='<s:url action='view' namespace='/app/lead'><s:param name='id' value='#lead.id'/></s:url>'">
                                <div class="lead-card-header"><s:property value="#lead.nomeContatto"/> <s:property value="#lead.cognomeContatto"/></div>
                                <div class="lead-card-company"><i class="bi bi-building me-1"></i><s:property value="#lead.azienda"/></div>
                                <s:if test="#lead.budgetStimato != null">
                                    <div class="lead-card-value">€ <s:text name="format.decimal"><s:param value="#lead.budgetStimato"/></s:text></div>
                                </s:if>
                                <div class="probability-bar">
                                    <div class="probability-fill bg-primary" style="width: <s:property value='#lead.probabilitaChiusura'/>%"></div>
                                </div>
                                <div class="lead-card-footer">
                                    <span><i class="bi bi-calendar3"></i> <s:date name="#lead.dataContatto" format="dd/MM"/></span>
                                    <span><s:property value="#lead.probabilitaChiusura"/>%</span>
                                </div>
                            </div>
                        </s:iterator>
                    </div>
                    <div class="total-value">
                        Tot: € <s:property value="getText('{0,number,#,##0}', {leads.{? #this.stato == @it.zensoftware.luna2.model.Lead$Stato@QUALIFICATO}.{budgetStimato}.sum()})"/>
                    </div>
                </div>

                <!-- Colonna PREVENTIVO -->
                <div class="pipeline-column col-preventivo" data-stato="PREVENTIVO">
                    <div class="pipeline-header">
                        <h5><i class="bi bi-file-text me-2"></i>Preventivo</h5>
                        <span class="pipeline-count"><s:property value="leads.{? #this.stato == @it.zensoftware.luna2.model.Lead$Stato@PREVENTIVO}.size()"/></span>
                    </div>
                    <div class="drop-zone">
                        <s:iterator value="leads.{? #this.stato == @it.zensoftware.luna2.model.Lead$Stato@PREVENTIVO}" var="lead">
                            <div class="lead-card" draggable="true" data-lead-id="<s:property value='#lead.id'/>"
                                 onclick="window.location='<s:url action='view' namespace='/app/lead'><s:param name='id' value='#lead.id'/></s:url>'">
                                <div class="lead-card-header"><s:property value="#lead.nomeContatto"/> <s:property value="#lead.cognomeContatto"/></div>
                                <div class="lead-card-company"><i class="bi bi-building me-1"></i><s:property value="#lead.azienda"/></div>
                                <s:if test="#lead.budgetStimato != null">
                                    <div class="lead-card-value">€ <s:text name="format.decimal"><s:param value="#lead.budgetStimato"/></s:text></div>
                                </s:if>
                                <div class="probability-bar">
                                    <div class="probability-fill bg-warning" style="width: <s:property value='#lead.probabilitaChiusura'/>%"></div>
                                </div>
                                <div class="lead-card-footer">
                                    <span><i class="bi bi-calendar3"></i> <s:date name="#lead.dataContatto" format="dd/MM"/></span>
                                    <span><s:property value="#lead.probabilitaChiusura"/>%</span>
                                </div>
                            </div>
                        </s:iterator>
                    </div>
                    <div class="total-value">
                        Tot: € <s:property value="getText('{0,number,#,##0}', {leads.{? #this.stato == @it.zensoftware.luna2.model.Lead$Stato@PREVENTIVO}.{budgetStimato}.sum()})"/>
                    </div>
                </div>

                <!-- Colonna NEGOZIAZIONE -->
                <div class="pipeline-column col-negoziazione" data-stato="NEGOZIAZIONE">
                    <div class="pipeline-header">
                        <h5><i class="bi bi-chat-dots me-2"></i>Negoziazione</h5>
                        <span class="pipeline-count"><s:property value="leads.{? #this.stato == @it.zensoftware.luna2.model.Lead$Stato@NEGOZIAZIONE}.size()"/></span>
                    </div>
                    <div class="drop-zone">
                        <s:iterator value="leads.{? #this.stato == @it.zensoftware.luna2.model.Lead$Stato@NEGOZIAZIONE}" var="lead">
                            <div class="lead-card" draggable="true" data-lead-id="<s:property value='#lead.id'/>"
                                 onclick="window.location='<s:url action='view' namespace='/app/lead'><s:param name='id' value='#lead.id'/></s:url>'">
                                <div class="lead-card-header"><s:property value="#lead.nomeContatto"/> <s:property value="#lead.cognomeContatto"/></div>
                                <div class="lead-card-company"><i class="bi bi-building me-1"></i><s:property value="#lead.azienda"/></div>
                                <s:if test="#lead.budgetStimato != null">
                                    <div class="lead-card-value">€ <s:text name="format.decimal"><s:param value="#lead.budgetStimato"/></s:text></div>
                                </s:if>
                                <div class="probability-bar">
                                    <div class="probability-fill bg-warning" style="width: <s:property value='#lead.probabilitaChiusura'/>%"></div>
                                </div>
                                <div class="lead-card-footer">
                                    <span><i class="bi bi-calendar3"></i> <s:date name="#lead.dataContatto" format="dd/MM"/></span>
                                    <span><s:property value="#lead.probabilitaChiusura"/>%</span>
                                </div>
                            </div>
                        </s:iterator>
                    </div>
                    <div class="total-value">
                        Tot: € <s:property value="getText('{0,number,#,##0}', {leads.{? #this.stato == @it.zensoftware.luna2.model.Lead$Stato@NEGOZIAZIONE}.{budgetStimato}.sum()})"/>
                    </div>
                </div>

                <!-- Colonna VINTO -->
                <div class="pipeline-column col-vinto" data-stato="VINTO">
                    <div class="pipeline-header">
                        <h5><i class="bi bi-trophy me-2"></i>Vinto</h5>
                        <span class="pipeline-count"><s:property value="leads.{? #this.stato == @it.zensoftware.luna2.model.Lead$Stato@VINTO}.size()"/></span>
                    </div>
                    <div class="drop-zone">
                        <s:iterator value="leads.{? #this.stato == @it.zensoftware.luna2.model.Lead$Stato@VINTO}" var="lead">
                            <div class="lead-card" draggable="true" data-lead-id="<s:property value='#lead.id'/>"
                                 onclick="window.location='<s:url action='view' namespace='/app/lead'><s:param name='id' value='#lead.id'/></s:url>'">
                                <div class="lead-card-header"><s:property value="#lead.nomeContatto"/> <s:property value="#lead.cognomeContatto"/></div>
                                <div class="lead-card-company"><i class="bi bi-building me-1"></i><s:property value="#lead.azienda"/></div>
                                <s:if test="#lead.budgetStimato != null">
                                    <div class="lead-card-value">€ <s:text name="format.decimal"><s:param value="#lead.budgetStimato"/></s:text></div>
                                </s:if>
                                <div class="probability-bar">
                                    <div class="probability-fill bg-success" style="width: 100%"></div>
                                </div>
                                <div class="lead-card-footer">
                                    <span><i class="bi bi-calendar3"></i> <s:date name="#lead.dataContatto" format="dd/MM"/></span>
                                    <span>100%</span>
                                </div>
                            </div>
                        </s:iterator>
                    </div>
                    <div class="total-value">
                        Tot: € <s:property value="getText('{0,number,#,##0}', {leads.{? #this.stato == @it.zensoftware.luna2.model.Lead$Stato@VINTO}.{budgetStimato}.sum()})"/>
                    </div>
                </div>

                <!-- Colonna PERSO -->
                <div class="pipeline-column col-perso" data-stato="PERSO">
                    <div class="pipeline-header">
                        <h5><i class="bi bi-x-circle me-2"></i>Perso</h5>
                        <span class="pipeline-count"><s:property value="leads.{? #this.stato == @it.zensoftware.luna2.model.Lead$Stato@PERSO}.size()"/></span>
                    </div>
                    <div class="drop-zone">
                        <s:iterator value="leads.{? #this.stato == @it.zensoftware.luna2.model.Lead$Stato@PERSO}" var="lead">
                            <div class="lead-card" draggable="true" data-lead-id="<s:property value='#lead.id'/>"
                                 onclick="window.location='<s:url action='view' namespace='/app/lead'><s:param name='id' value='#lead.id'/></s:url>'">
                                <div class="lead-card-header"><s:property value="#lead.nomeContatto"/> <s:property value="#lead.cognomeContatto"/></div>
                                <div class="lead-card-company"><i class="bi bi-building me-1"></i><s:property value="#lead.azienda"/></div>
                                <s:if test="#lead.budgetStimato != null">
                                    <div class="lead-card-value text-muted">€ <s:text name="format.decimal"><s:param value="#lead.budgetStimato"/></s:text></div>
                                </s:if>
                                <div class="probability-bar">
                                    <div class="probability-fill bg-danger" style="width: 0%"></div>
                                </div>
                                <div class="lead-card-footer">
                                    <span><i class="bi bi-calendar3"></i> <s:date name="#lead.dataContatto" format="dd/MM"/></span>
                                    <span>0%</span>
                                </div>
                            </div>
                        </s:iterator>
                    </div>
                    <div class="total-value text-danger">
                        Tot: € <s:property value="getText('{0,number,#,##0}', {leads.{? #this.stato == @it.zensoftware.luna2.model.Lead$Stato@PERSO}.{budgetStimato}.sum()})"/>
                    </div>
                </div>
            </div>
        </div>
    </div>

    </div>
</div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
    <script>
        // Drag and Drop functionality
        let draggedCard = null;

        document.querySelectorAll('.lead-card').forEach(card => {
            card.addEventListener('dragstart', function(e) {
                draggedCard = this;
                this.classList.add('dragging');
                e.dataTransfer.effectAllowed = 'move';
                e.dataTransfer.setData('text/html', this.innerHTML);
                // Prevent click event when dragging
                this.onclick = null;
            });

            card.addEventListener('dragend', function(e) {
                this.classList.remove('dragging');
                // Restore click event
                setTimeout(() => {
                    this.onclick = function() {
                        const leadId = this.getAttribute('data-lead-id');
                        window.location = '/app/lead/view?id=' + leadId;
                    };
                }, 100);
            });
        });

        document.querySelectorAll('.drop-zone').forEach(zone => {
            zone.addEventListener('dragover', function(e) {
                e.preventDefault();
                e.dataTransfer.dropEffect = 'move';
                this.classList.add('drag-over');
            });

            zone.addEventListener('dragleave', function(e) {
                this.classList.remove('drag-over');
            });

            zone.addEventListener('drop', function(e) {
                e.preventDefault();
                this.classList.remove('drag-over');
                
                if (draggedCard) {
                    const leadId = draggedCard.getAttribute('data-lead-id');
                    const newStato = this.closest('.pipeline-column').getAttribute('data-stato');
                    
                    // Confirm change
                    if (confirm('Cambiare lo stato del lead a ' + newStato + '?')) {
                        // Submit form to change stage
                        const form = document.createElement('form');
                        form.method = 'POST';
                        form.action = '/app/lead/changeStage';
                        
                        const idInput = document.createElement('input');
                        idInput.type = 'hidden';
                        idInput.name = 'id';
                        idInput.value = leadId;
                        
                        const statoInput = document.createElement('input');
                        statoInput.type = 'hidden';
                        statoInput.name = 'newStage';
                        statoInput.value = newStato;
                        
                        form.appendChild(idInput);
                        form.appendChild(statoInput);
                        document.body.appendChild(form);
                        form.submit();
                    }
                }
            });
        });
    </script>
</body>
</html>

            <s:if test="hasActionMessages()">
                <div class="alert alert-success alert-dismissible fade show">
                    <s:actionmessage/>
                    <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
                </div>
            </s:if>

            <!-- Kanban View -->
            <div id="kanban-view">
                <div class="row g-3">
                    <!-- Nuovo -->
                    <div class="col-md-2">
                        <div class="kanban-column">
                            <h6 class="text-center mb-3">
                                <span class="badge badge-nuovo w-100">NUOVO</span>
                            </h6>
                            <s:iterator value="leads">
                                <s:if test="stato.name() == 'NUOVO'">
                                    <div class="card lead-card mb-2" onclick="viewLead(<s:property value='id'/>)">
                                        <div class="card-body p-2">
                                            <h6 class="card-title mb-1 small"><s:property value="azienda"/></h6>
                                            <p class="card-text small text-muted mb-1">
                                                <i class="bi bi-person"></i> <s:property value="nome"/> <s:property value="cognome"/>
                                            </p>
                                            <p class="card-text small mb-0">
                                                <strong>€ <s:property value="valoreStimato"/></strong>
                                            </p>
                                        </div>
                                    </div>
                                </s:if>
                            </s:iterator>
                        </div>
                    </div>

                    <!-- Contattato -->
                    <div class="col-md-2">
                        <div class="kanban-column">
                            <h6 class="text-center mb-3">
                                <span class="badge badge-contattato w-100">CONTATTATO</span>
                            </h6>
                            <s:iterator value="leads">
                                <s:if test="stato.name() == 'CONTATTATO'">
                                    <div class="card lead-card mb-2" onclick="viewLead(<s:property value='id'/>)">
                                        <div class="card-body p-2">
                                            <h6 class="card-title mb-1 small"><s:property value="azienda"/></h6>
                                            <p class="card-text small text-muted mb-1">
                                                <i class="bi bi-person"></i> <s:property value="nome"/> <s:property value="cognome"/>
                                            </p>
                                            <p class="card-text small mb-0">
                                                <strong>€ <s:property value="valoreStimato"/></strong>
                                            </p>
                                        </div>
                                    </div>
                                </s:if>
                            </s:iterator>
                        </div>
                    </div>

                    <!-- Qualificato -->
                    <div class="col-md-2">
                        <div class="kanban-column">
                            <h6 class="text-center mb-3">
                                <span class="badge badge-qualificato w-100">QUALIFICATO</span>
                            </h6>
                            <s:iterator value="leads">
                                <s:if test="stato.name() == 'QUALIFICATO'">
                                    <div class="card lead-card mb-2" onclick="viewLead(<s:property value='id'/>)">
                                        <div class="card-body p-2">
                                            <h6 class="card-title mb-1 small"><s:property value="azienda"/></h6>
                                            <p class="card-text small text-muted mb-1">
                                                <i class="bi bi-person"></i> <s:property value="nome"/> <s:property value="cognome"/>
                                            </p>
                                            <p class="card-text small mb-0">
                                                <strong>€ <s:property value="valoreStimato"/></strong>
                                            </p>
                                        </div>
                                    </div>
                                </s:if>
                            </s:iterator>
                        </div>
                    </div>

                    <!-- Proposta -->
                    <div class="col-md-2">
                        <div class="kanban-column">
                            <h6 class="text-center mb-3">
                                <span class="badge badge-proposta w-100">PROPOSTA</span>
                            </h6>
                            <s:iterator value="leads">
                                <s:if test="stato.name() == 'PROPOSTA'">
                                    <div class="card lead-card mb-2" onclick="viewLead(<s:property value='id'/>)">
                                        <div class="card-body p-2">
                                            <h6 class="card-title mb-1 small"><s:property value="azienda"/></h6>
                                            <p class="card-text small text-muted mb-1">
                                                <i class="bi bi-person"></i> <s:property value="nome"/> <s:property value="cognome"/>
                                            </p>
                                            <p class="card-text small mb-0">
                                                <strong>€ <s:property value="valoreStimato"/></strong>
                                            </p>
                                        </div>
                                    </div>
                                </s:if>
                            </s:iterator>
                        </div>
                    </div>

                    <!-- Negoziazione -->
                    <div class="col-md-2">
                        <div class="kanban-column">
                            <h6 class="text-center mb-3">
                                <span class="badge badge-negoziazione w-100">NEGOZIAZIONE</span>
                            </h6>
                            <s:iterator value="leads">
                                <s:if test="stato.name() == 'NEGOZIAZIONE'">
                                    <div class="card lead-card mb-2" onclick="viewLead(<s:property value='id'/>)">
                                        <div class="card-body p-2">
                                            <h6 class="card-title mb-1 small"><s:property value="azienda"/></h6>
                                            <p class="card-text small text-muted mb-1">
                                                <i class="bi bi-person"></i> <s:property value="nome"/> <s:property value="cognome"/>
                                            </p>
                                            <p class="card-text small mb-0">
                                                <strong>€ <s:property value="valoreStimato"/></strong>
                                            </p>
                                        </div>
                                    </div>
                                </s:if>
                            </s:iterator>
                        </div>
                    </div>

                    <!-- Chiusi (Vinto/Perso) -->
                    <div class="col-md-2">
                        <div class="kanban-column">
                            <h6 class="text-center mb-3">
                                <span class="badge bg-secondary w-100">CHIUSI</span>
                            </h6>
                            <s:iterator value="leads">
                                <s:if test="stato.name() == 'VINTO' || stato.name() == 'PERSO'">
                                    <div class="card lead-card mb-2" onclick="viewLead(<s:property value='id'/>)">
                                        <div class="card-body p-2">
                                            <div class="d-flex justify-content-between align-items-start">
                                                <h6 class="card-title mb-1 small"><s:property value="azienda"/></h6>
                                                <span class="badge badge-<s:property value='stato.name().toLowerCase()'/>">
                                                    <s:if test="stato.name() == 'VINTO'">
                                                        <i class="bi bi-check-circle"></i>
                                                    </s:if>
                                                    <s:else>
                                                        <i class="bi bi-x-circle"></i>
                                                    </s:else>
                                                </span>
                                            </div>
                                            <p class="card-text small text-muted mb-1">
                                                <i class="bi bi-person"></i> <s:property value="nome"/> <s:property value="cognome"/>
                                            </p>
                                            <p class="card-text small mb-0">
                                                <strong>€ <s:property value="valoreStimato"/></strong>
                                            </p>
                                        </div>
                                    </div>
                                </s:if>
                            </s:iterator>
                        </div>
                    </div>
                </div>
            </div>

            <!-- Statistics -->
            <div class="row mt-4">
                <div class="col-md-12">
                    <div class="card">
                        <div class="card-body">
                            <h5 class="card-title">Pipeline Overview</h5>
                            <div class="row text-center">
                                <div class="col-md-2">
                                    <h3 class="text-primary"><s:property value="leadAperti"/></h3>
                                    <p class="text-muted">Lead Aperti</p>
                                </div>
                                <div class="col-md-2">
                                    <h3 class="text-success">€ <s:property value="valoreStimato"/></h3>
                                    <p class="text-muted">Valore Pipeline</p>
                                </div>
                                <div class="col-md-2">
                                    <h3 class="text-success"><s:property value="leadVinti"/></h3>
                                    <p class="text-muted">Lead Vinti (mese)</p>
                                </div>
                                <div class="col-md-2">
                                    <h3 class="text-info"><s:property value="tassoConversione"/>%</h3>
                                    <p class="text-muted">Tasso Conversione</p>
                                </div>
                                <div class="col-md-2">
                                    <h3 class="text-warning"><s:property value="tempoMedioChiusura"/></h3>
                                    <p class="text-muted">Giorni Medi Chiusura</p>
                                </div>
                                <div class="col-md-2">
                                    <h3 class="text-success">€ <s:property value="valoreMedioVinto"/></h3>
                                    <p class="text-muted">Valore Medio Vinto</p>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>

    </div>
</div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
    <script>
        function viewLead(id) {
            window.location.href = '<s:url action="view" namespace="/app/lead"/>' + '?id=' + id;
        }

        function toggleView() {
            window.location.href = '<s:url action="list" namespace="/app/lead"/>';
        }
    </script>
</body>
</html>
