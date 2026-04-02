<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="s" uri="/struts-tags" %>

<!DOCTYPE html>
<html lang="it">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Richiesta Ferie - Luna2</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.0/font/bootstrap-icons.css">
</head>
<body>
<jsp:include page="/WEB-INF/jsp/includes/sidebar.jsp"/>

<div class="container mt-5" style="margin-left: 260px; max-width: 900px;">
    <div class="row">
        <div class="col-md-8 offset-md-2">
            <!-- Header -->
            <div class="mb-4">
                <h1 class="h3"><i class="bi bi-calendar-check me-2"></i>Richiesta Ferie</h1>
                <p class="text-muted">Compila il modulo per richiedere ferie al tuo manager</p>
            </div>

            <!-- Alerts -->
            <s:if test="hasActionMessages()">
                <div class="alert alert-success alert-dismissible fade show" role="alert">
                    <s:iterator value="actionMessages">
                        <div><s:property/></div>
                    </s:iterator>
                    <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
                </div>
            </s:if>

            <s:if test="hasActionErrors()">
                <div class="alert alert-danger alert-dismissible fade show" role="alert">
                    <s:iterator value="actionErrors">
                        <div><s:property/></div>
                    </s:iterator>
                    <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
                </div>
            </s:if>

            <!-- Form -->
            <form action="ferie-create" method="post" id="ferieForm" class="card border-0 shadow-sm">
                <div class="card-body">
                    
                    <!-- Data Inizio -->
                    <div class="mb-3">
                        <label for="startDate" class="form-label">
                            <i class="bi bi-calendar-event me-1"></i>Data Inizio
                        </label>
                        <input type="date" name="approvalRequest.startDate" id="startDate" class="form-control" required>
                        <small class="text-muted">Seleziona il primo giorno di ferie</small>
                    </div>

                    <!-- Data Fine -->
                    <div class="mb-3">
                        <label for="endDate" class="form-label">
                            <i class="bi bi-calendar-event me-1"></i>Data Fine
                        </label>
                        <input type="date" name="approvalRequest.endDate" id="endDate" class="form-control" required>
                        <small class="text-muted">Seleziona l'ultimo giorno di ferie</small>
                    </div>

                    <!-- Giorni Calcolati (readonly) -->
                    <div class="mb-3">
                        <label for="daysCalculated" class="form-label">
                            <i class="bi bi-list-check me-1"></i>Giorni Ferie
                        </label>
                        <input type="text" id="daysCalculated" class="form-control-plaintext" 
                            readonly value="Seleziona le date" />
                        <small class="text-muted">Calcolato automaticamente</small>
                    </div>

                    <!-- Motivo (opzionale) -->
                    <div class="mb-3">
                        <label for="description" class="form-label">
                            <i class="bi bi-chat-dots me-1"></i>Motivo (opzionale)
                        </label>
                        <textarea name="approvalRequest.description" id="description" 
                            class="form-control" rows="3" 
                            placeholder="Es. Vacanza, Motivi personali..."></textarea>
                        <small class="text-muted">Max 500 caratteri</small>
                    </div>

                    <!-- Info Saldo Ferie -->
                    <div class="alert alert-info" role="alert">
                        <i class="bi bi-info-circle me-2"></i>
                        <strong>Saldo ferie:</strong> 
                        <span id="vacationBalance">Caricamento...</span>
                    </div>

                </div>

                <!-- Footer con bottoni -->
                <div class="card-footer bg-light">
                    <button type="submit" class="btn btn-primary" id="submitBtn">
                        <i class="bi bi-check-circle me-1"></i>Crea Richiesta
                    </button>
                    <a href="ferie-list" class="btn btn-secondary">
                        <i class="bi bi-arrow-left me-1"></i>Indietro
                    </a>
                    <a href="pending-approvals" class="btn btn-outline-dark">
                        <i class="bi bi-list-check me-1"></i>Pending approvazioni
                    </a>
                </div>
            </form>

        </div>
    </div>
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
<script>
// Calcolo automatico giorni ferie
document.getElementById('startDate').addEventListener('change', calculateDays);
document.getElementById('endDate').addEventListener('change', calculateDays);

function calculateDays() {
    const startDate = new Date(document.getElementById('startDate').value);
    const endDate = new Date(document.getElementById('endDate').value);
    
    if (startDate && endDate && startDate <= endDate) {
        const days = Math.floor((endDate - startDate) / (1000 * 60 * 60 * 24)) + 1;
        document.getElementById('daysCalculated').value = days + ' giorni';
    }
}

// Carica saldo ferie via AJAX
document.addEventListener('DOMContentLoaded', function() {
    // TODO: Carica saldo ferie da API /api/vacation-balance
    document.getElementById('vacationBalance').innerText = '26 giorni disponibili';
});

// Validazione form prima submit
document.getElementById('ferieForm').addEventListener('submit', function(e) {
    const startDate = document.getElementById('startDate').value;
    const endDate = document.getElementById('endDate').value;
    
    if (!startDate || !endDate) {
        e.preventDefault();
        alert('Seleziona data inizio e fine');
        return false;
    }
    
    if (new Date(startDate) > new Date(endDate)) {
        e.preventDefault();
        alert('Data inizio non può essere dopo data fine');
        return false;
    }
});
</script>

</body>
</html>
