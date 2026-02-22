<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib uri="http://struts.apache.org/tags-struts2" prefix="s" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Activity Timeline - Luna2 CRM</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css" rel="stylesheet">
    <style>
        .timeline { position: relative; padding: 20px 0; }
        .timeline-item { display: flex; margin-bottom: 30px; position: relative; padding-left: 50px; }
        .timeline-marker { 
            position: absolute; 
            left: 0; 
            top: 0; 
            width: 40px; 
            height: 40px; 
            background: #667eea; 
            border-radius: 50%; 
            display: flex; 
            align-items: center; 
            justify-content: center; 
            color: white; 
            font-weight: bold;
            border: 3px solid #fff;
            box-shadow: 0 0 0 3px #667eea;
        }
        .timeline-marker.call { background: #3498db; box-shadow: 0 0 0 3px #3498db; }
        .timeline-marker.email { background: #f39c12; box-shadow: 0 0 0 3px #f39c12; }
        .timeline-marker.meeting { background: #e74c3c; box-shadow: 0 0 0 3px #e74c3c; }
        .timeline-marker.note { background: #9b59b6; box-shadow: 0 0 0 3px #9b59b6; }
        
        .timeline-content { flex: 1; background: white; padding: 20px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
        .timeline-date { font-size: 0.85rem; color: #7f8c8d; font-weight: 600; margin-bottom: 8px; }
        .timeline-title { font-weight: 700; font-size: 1.05rem; color: #2c3e50; margin-bottom: 8px; display: flex; align-items: center; gap: 8px; }
        .activity-type { display: inline-block; padding: 4px 10px; border-radius: 20px; font-size: 0.8rem; font-weight: 600; margin-bottom: 10px; }
        .badge-call { background: #d4edff; color: #0c5aa0; }
        .badge-email { background: #fef3cd; color: #664d03; }
        .badge-note { background: #e2e3e5; color: #383d41; }
        .badge-meeting { background: #d1ecf1; color: #055160; }
        
        .timeline-description { color: #495057; line-height: 1.6; margin-bottom: 12px; }
        .timeline-user { font-size: 0.85rem; color: #7f8c8d; margin-top: 12px; padding-top: 12px; border-top: 1px solid #ecf0f1; }
        .timeline-actions { margin-top: 15px; }
        .timeline-line { 
            position: absolute; 
            left: 19px; 
            top: 45px; 
            width: 2px; 
            height: calc(100% + 15px); 
            background: #ecf0f1;
        }
        .timeline-item:last-child .timeline-line { display: none; }
        
        .filter-bar { background: white; padding: 20px; border-radius: 8px; margin-bottom: 20px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
        .container-main { max-width: 800px; margin: 0 auto; padding: 20px; }
        .page-header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 30px 0; margin-bottom: 30px; }
    </style>
</head>
<body style="background: #f8f9fa;">

<!-- PAGE HEADER -->
<div class="page-header">
    <div class="container-main">
        <h2><i class="fas fa-history"></i> Activity Timeline</h2>
        <p class="mb-0">Traccia completa delle attività e interazioni</p>
    </div>
</div>

<!-- MAIN CONTENT -->
<div class="container-main">
    <!-- FILTER BAR -->
    <div class="filter-bar">
        <div class="row g-3 align-items-end">
            <div class="col-md-3">
                <label class="form-label">Filter by Type</label>
                <select id="filterType" class="form-select form-select-sm">
                    <option value="">All Types</option>
                    <option value="CALL">☎️ Calls</option>
                    <option value="EMAIL">✉️ Emails</option>
                    <option value="MEETING">👥 Meetings</option>
                    <option value="NOTE">📝 Notes</option>
                </select>
            </div>
            <div class="col-md-3">
                <label class="form-label">Filter by Status</label>
                <select id="filterStatus" class="form-select form-select-sm">
                    <option value="">All</option>
                    <option value="PENDING">Pending</option>
                    <option value="COMPLETED">Completed</option>
                    <option value="CANCELLED">Cancelled</option>
                </select>
            </div>
            <div class="col-md-3">
                <label class="form-label">From Date</label>
                <input type="date" id="filterDateFrom" class="form-control form-control-sm">
            </div>
            <div class="col-md-2">
                <button class="btn btn-sm btn-primary w-100" onclick="applyFilters()">
                    <i class="fas fa-filter"></i> Filter
                </button>
                <button class="btn btn-sm btn-outline-secondary w-100 mt-2" onclick="resetFilters()">
                    <i class="fas fa-redo"></i> Reset
                </button>
            </div>
        </div>
    </div>

    <!-- TIMELINE -->
    <div class="timeline">
        <s:iterator value="activities" var="activity" status="stat">
            <div class="timeline-item" data-type="<s:property value='#activity.tipo' />" data-status="<s:property value='#activity.stato' />">
                <!-- TIMELINE LINE (background) -->
                <div class="timeline-line"></div>
                
                <!-- TIMELINE MARKER -->
                <div class="timeline-marker <s:property value='#activity.tipo.toString().toLowerCase()' />">
                    <s:if test="#activity.tipo.toString() == 'CALL'">
                        <i class="fas fa-phone"></i>
                    </s:if>
                    <s:elseif test="#activity.tipo.toString() == 'EMAIL'">
                        <i class="fas fa-envelope"></i>
                    </s:elseif>
                    <s:elseif test="#activity.tipo.toString() == 'MEETING'">
                        <i class="fas fa-users"></i>
                    </s:elseif>
                    <s:else>
                        <i class="fas fa-sticky-note"></i>
                    </s:else>
                </div>
                
                <!-- TIMELINE CONTENT -->
                <div class="timeline-content">
                    <div class="timeline-date">
                        <i class="fas fa-calendar"></i> 
                        <s:date name="#activity.dataAttivita" format="dd/MM/yyyy HH:mm" />
                        <span class="ms-2">
                            <s:if test="#activity.stato.toString() == 'COMPLETED'">
                                <span class="badge bg-success">✓ Completed</span>
                            </s:if>
                            <s:elseif test="#activity.stato.toString() == 'PENDING'">
                                <span class="badge bg-warning text-dark">⏳ Pending</span>
                            </s:elseif>
                            <s:else>
                                <span class="badge bg-secondary">Cancelled</span>
                            </s:else>
                        </span>
                    </div>
                    
                    <div class="activity-type badge-<s:property value='#activity.tipo.toString().toLowerCase()' />">
                        <s:if test="#activity.tipo.toString() == 'CALL'">☎️ Telefonata</s:if>
                        <s:elseif test="#activity.tipo.toString() == 'EMAIL'">✉️ Email</s:elseif>
                        <s:elseif test="#activity.tipo.toString() == 'MEETING'">👥 Riunione</s:elseif>
                        <s:else>📝 Nota</s:else>
                    </div>
                    
                    <div class="timeline-title">
                        <s:property value="#activity.titolo" />
                    </div>
                    
                    <div class="timeline-description">
                        <s:property value="#activity.descrizione" />
                    </div>
                    
                    <div class="timeline-user">
                        <i class="fas fa-user-circle"></i> 
                        <s:property value="#activity.utente.nome" /> <s:property value="#activity.utente.cognome" />
                        <s:if test="#activity.dataProssimaAttivita != null">
                            <br>
                            <i class="fas fa-arrow-right text-info"></i> 
                            <strong>Next Activity:</strong> 
                            <s:date name="#activity.dataProssimaAttivita" format="dd/MM/yyyy HH:mm" />
                        </s:if>
                    </div>
                    
                    <!-- ACTION BUTTONS -->
                    <div class="timeline-actions">
                        <s:if test="#activity.stato.toString() != 'COMPLETED'">
                            <button class="btn btn-sm btn-success" onclick="markAsCompleted(<s:property value='#activity.id'/>)">
                                <i class="fas fa-check"></i> Mark Complete
                            </button>
                        </s:if>
                        <button class="btn btn-sm btn-info" data-bs-toggle="modal" data-bs-target="#editModal" 
                                onclick="editActivity(<s:property value='#activity.id'/>)">
                            <i class="fas fa-edit"></i> Edit
                        </button>
                        <button class="btn btn-sm btn-danger" onclick="deleteActivity(<s:property value='#activity.id'/>)">
                            <i class="fas fa-trash"></i> Delete
                        </button>
                    </div>
                </div>
            </div>
        </s:iterator>
    </div>

    <!-- EMPTY STATE -->
    <s:if test="activities.size() == 0">
        <div class="alert alert-info text-center mt-5">
            <i class="fas fa-inbox" style="font-size: 2rem;"></i>
            <p class="mt-2">No activities recorded yet</p>
        </div>
    </s:if>
</div>

<!-- MODALS -->
<!-- Edit Activity Modal -->
<div class="modal fade" id="editModal" tabindex="-1">
    <div class="modal-dialog">
        <div class="modal-content">
            <div class="modal-header">
                <h5 class="modal-title">Edit Activity</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
            </div>
            <div class="modal-body" id="editContent">
                <!-- Dynamically populated -->
            </div>
        </div>
    </div>
</div>

<script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>

<script>
function markAsCompleted(activityId) {
    $.ajax({
        url: '/crm/activity!completeActivity.action',
        type: 'POST',
        data: { id: activityId },
        success: function() {
            location.reload();
        },
        error: function() {
            alert('Error marking activity as completed');
        }
    });
}

function deleteActivity(activityId) {
    if (confirm('Are you sure you want to delete this activity?')) {
        $.ajax({
            url: '/crm/activity!deleteActivity.action',
            type: 'POST',
            data: { id: activityId },
            success: function() {
                location.reload();
            },
            error: function() {
                alert('Error deleting activity');
            }
        });
    }
}

function editActivity(activityId) {
    // Load edit form in modal (future implementation)
    console.log('Edit activity: ' + activityId);
}

function applyFilters() {
    const type = document.getElementById('filterType').value;
    const status = document.getElementById('filterStatus').value;
    
    document.querySelectorAll('.timeline-item').forEach(item => {
        let show = true;
        
        if (type && item.getAttribute('data-type') !== type) show = false;
        if (status && item.getAttribute('data-status') !== status) show = false;
        
        item.style.display = show ? 'flex' : 'none';
    });
}

function resetFilters() {
    document.getElementById('filterType').value = '';
    document.getElementById('filterStatus').value = '';
    document.getElementById('filterDateFrom').value = '';
    
    document.querySelectorAll('.timeline-item').forEach(item => {
        item.style.display = 'flex';
    });
}
</script>
</body>
</html>
