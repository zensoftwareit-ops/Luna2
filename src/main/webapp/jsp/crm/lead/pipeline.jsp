<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib uri="http://struts.apache.org/tags-struts2" prefix="s" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Pipeline Kanban - Luna2 CRM</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/sortablejs@1.15.0/Sortable.css" rel="stylesheet">
    <style>
        .kanban-container { display: flex; gap: 20px; overflow-x: auto; padding: 20px; min-height: 100vh; background: #f8f9fa; }
        .kanban-column { flex: 0 0 350px; background: #ecf0f1; border-radius: 8px; display: flex; flex-direction: column; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
        .kanban-header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 15px; border-radius: 8px 8px 0 0; }
        .kanban-header.bozza { background: #3498db; }
        .kanban-header.qualificato { background: #27ae60; }
        .kanban-header.proposta { background: #f39c12; }
        .kanban-header.negoziazione { background: #e74c3c; }
        .kanban-header.vinto { background: #16a085; }
        .kanban-header.perso { background: #95a5a6; }
        .kanban-body { flex: 1; padding: 12px; overflow-y: auto; }
        .kanban-card { 
            background: white; 
            padding: 12px; 
            border-radius: 6px; 
            margin-bottom: 12px; 
            cursor: grab; 
            box-shadow: 0 1px 3px rgba(0,0,0,0.12); 
            transition: all 0.2s ease;
            border-left: 4px solid #667eea;
        }
        .kanban-card:hover { box-shadow: 0 2px 6px rgba(0,0,0,0.15); transform: translateY(-2px); }
        .kanban-card:active { cursor: grabbing; opacity: 0.8; }
        .kanban-card.drag-over { box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3); }
        .card-title { font-weight: 600; font-size: 0.95rem; margin-bottom: 8px; color: #2c3e50; }
        .card-company { font-size: 0.85rem; color: #7f8c8d; margin-bottom: 6px; }
        .card-footer { display: flex; justify-content: space-between; align-items: center; margin-top: 8px; padding-top: 8px; border-top: 1px solid #ecf0f1; }
        .card-value { font-weight: 600; font-size: 0.9rem; color: #667eea; }
        .card-prob { font-size: 0.8rem; background: #ecf0f1; padding: 2px 6px; border-radius: 3px; }
        .stage-title { font-weight: 700; display: flex; justify-content: space-between; align-items: center; }
        .stage-count { background: rgba(255,255,255,0.3); padding: 2px 8px; border-radius: 12px; font-size: 0.85rem; }
        .stats-panel { background: white; padding: 20px; border-radius: 8px; margin: 20px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
        .stat-item { display: inline-block; margin-right: 30px; }
        .stat-label { font-size: 0.85rem; color: #7f8c8d; text-transform: uppercase; }
        .stat-value { font-size: 1.5rem; font-weight: 700; color: #2c3e50; }
    </style>
</head>
<body>
<!-- NAVBAR -->
<nav class="navbar navbar-dark bg-dark">
    <div class="container-fluid">
        <a class="navbar-brand" href="#">Luna2 CRM - Pipeline</a>
        <div>
            <a href="/crm/lead!list.action" class="btn btn-sm btn-outline-light">
                <i class="fas fa-list"></i> View List
            </a>
        </div>
    </div>
</nav>

<!-- STATS PANEL -->
<div class="stats-panel">
    <h5><i class="fas fa-chart-line"></i> Pipeline Overview</h5>
    <hr>
    <div class="row">
        <div class="col-md-3">
            <div class="stat-item">
                <div class="stat-label">Total Leads</div>
                <div class="stat-value"><s:property value="leads.size()" /></div>
            </div>
        </div>
        <div class="col-md-3">
            <div class="stat-item">
                <div class="stat-label">Conversion Rate</div>
                <div class="stat-value"><s:property value="conversionRate" /> %</div>
            </div>
        </div>
        <div class="col-md-3">
            <div class="stat-item">
                <div class="stat-label">Top Opportunity</div>
                <div class="stat-value">
                    <s:if test="topOpportunities != null && topOpportunities.size() > 0">
                        € <s:property value="topOpportunities.get(0).budgetStimato" />
                    </s:if>
                    <s:else>-</s:else>
                </div>
            </div>
        </div>
        <div class="col-md-3">
            <div class="stat-item">
                <div class="stat-label">Total Pipeline Value</div>
                <div class="stat-value">€ <span id="totalValue">0</span></div>
            </div>
        </div>
    </div>
</div>

<!-- KANBAN BOARD -->
<div class="kanban-container">
    <!-- BOZZA Stage -->
    <div class="kanban-column">
        <div class="kanban-header bozza">
            <div class="stage-title">
                <span><i class="fas fa-circle" style="margin-right: 8px;"></i> BOZZA</span>
                <span class="stage-count"><s:property value="stats['BOZZA'] ?: 0" /></span>
            </div>
        </div>
        <div class="kanban-body" data-stage="BOZZA">
            <s:iterator value="pipeline['NUOVO'] ?: #list()" var="lead">
                <div class="kanban-card" draggable="true" data-lead-id="<s:property value='#lead.id' />">
                    <div class="card-title"><s:property value="#lead.nomeContatto" /> <s:property value="#lead.cognomeContatto" /></div>
                    <div class="card-company"><s:property value="#lead.azienda" /></div>
                    <div class="card-footer">
                        <div class="card-value">€ <s:property value="#lead.budgetStimato" /></div>
                        <div class="card-prob"><s:property value="#lead.probabilitaChiusura ?: 0" />%</div>
                    </div>
                </div>
            </s:iterator>
        </div>
    </div>

    <!-- QUALIFICATO Stage -->
    <div class="kanban-column">
        <div class="kanban-header qualificato">
            <div class="stage-title">
                <span><i class="fas fa-check-circle" style="margin-right: 8px;"></i> QUALIFICATO</span>
                <span class="stage-count"><s:property value="stats['QUALIFICATO'] ?: 0" /></span>
            </div>
        </div>
        <div class="kanban-body" data-stage="QUALIFICATO">
            <s:iterator value="pipeline['QUALIFICATO'] ?: #list()" var="lead">
                <div class="kanban-card" draggable="true" data-lead-id="<s:property value='#lead.id' />">
                    <div class="card-title"><s:property value="#lead.nomeContatto" /> <s:property value="#lead.cognomeContatto" /></div>
                    <div class="card-company"><s:property value="#lead.azienda" /></div>
                    <div class="card-footer">
                        <div class="card-value">€ <s:property value="#lead.budgetStimato" /></div>
                        <div class="card-prob"><s:property value="#lead.probabilitaChiusura ?: 0" />%</div>
                    </div>
                </div>
            </s:iterator>
        </div>
    </div>

    <!-- PROPOSTA Stage -->
    <div class="kanban-column">
        <div class="kanban-header proposta">
            <div class="stage-title">
                <span><i class="fas fa-file-alt" style="margin-right: 8px;"></i> PROPOSTA</span>
                <span class="stage-count"><s:property value="stats['CONTATTATO'] ?: 0" /></span>
            </div>
        </div>
        <div class="kanban-body" data-stage="PROPOSTA">
            <s:iterator value="pipeline['CONTATTATO'] ?: #list()" var="lead">
                <div class="kanban-card" draggable="true" data-lead-id="<s:property value='#lead.id' />">
                    <div class="card-title"><s:property value="#lead.nomeContatto" /> <s:property value="#lead.cognomeContatto" /></div>
                    <div class="card-company"><s:property value="#lead.azienda" /></div>
                    <div class="card-footer">
                        <div class="card-value">€ <s:property value="#lead.budgetStimato" /></div>
                        <div class="card-prob"><s:property value="#lead.probabilitaChiusura ?: 0" />%</div>
                    </div>
                </div>
            </s:iterator>
        </div>
    </div>

    <!-- NEGOZIAZIONE Stage -->
    <div class="kanban-column">
        <div class="kanban-header negoziazione">
            <div class="stage-title">
                <span><i class="fas fa-handshake" style="margin-right: 8px;"></i> NEGOZIAZIONE</span>
                <span class="stage-count">0</span>
            </div>
        </div>
        <div class="kanban-body" data-stage="NEGOZIAZIONE">
            <!-- Negoziazione leads would go here -->
        </div>
    </div>

    <!-- VINTO Stage -->
    <div class="kanban-column">
        <div class="kanban-header vinto">
            <div class="stage-title">
                <span><i class="fas fa-trophy" style="margin-right: 8px;"></i> VINTO</span>
                <span class="stage-count">0</span>
            </div>
        </div>
        <div class="kanban-body" data-stage="VINTO">
            <!-- Won leads would go here -->
        </div>
    </div>

    <!-- PERSO Stage -->
    <div class="kanban-column">
        <div class="kanban-header perso">
            <div class="stage-title">
                <span><i class="fas fa-times-circle" style="margin-right: 8px;"></i> PERSO</span>
                <span class="stage-count">0</span>
            </div>
        </div>
        <div class="kanban-body" data-stage="PERSO">
            <!-- Lost leads would go here -->
        </div>
    </div>
</div>

<script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
<script src="https://cdn.jsdelivr.net/npm/sortablejs@1.15.0/Sortable.min.js"></script>

<script>
// Initialize Sortable for drag-drop between columns
document.querySelectorAll('.kanban-body').forEach(column => {
    new Sortable(column, {
        group: 'leads',
        animation: 150,
        ghostClass: 'drag-over',
        delay: 100,
        delayOnTouchOnly: true,
        onEnd: function(evt) {
            const leadId = evt.item.getAttribute('data-lead-id');
            const newStage = evt.to.getAttribute('data-stage');
            
            // Call API to change stage
            $.ajax({
                url: '/crm/lead!changeStage.action',
                type: 'POST',
                data: {
                    id: leadId,
                    newStage: newStage,
                    stageChangeReason: 'Drag-drop from Kanban board'
                },
                success: function(response) {
                    if (!response.success) {
                        // Revert if failed
                        evt.from.insertBefore(evt.item, evt.item);
                        alert('Errore nel cambio stage');
                    }
                }
            });
        }
    });
});

// Calculate and display total pipeline value
function updateTotalValue() {
    let total = 0;
    document.querySelectorAll('.kanban-card').forEach(card => {
        const valueText = card.querySelector('.card-value').textContent.replace('€ ', '').replace('.', '');
        total += parseFloat(valueText) || 0;
    });
    document.getElementById('totalValue').textContent = total.toLocaleString('it-IT');
}

updateTotalValue();
</script>
</body>
</html>
