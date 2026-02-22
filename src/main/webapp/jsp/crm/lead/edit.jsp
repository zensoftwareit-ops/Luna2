<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib uri="http://struts.apache.org/tags-struts2" prefix="s" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><s:if test="lead.id != null">Modifica</s:if><s:else>Nuovo</s:else> Lead - Luna2 CRM</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css" rel="stylesheet">
    <style>
        .form-container { max-width: 900px; margin: 30px auto; background: white; padding: 30px; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
        .section-title { font-weight: 700; color: #2c3e50; margin-top: 20px; margin-bottom: 15px; padding-bottom: 10px; border-bottom: 2px solid #667eea; }
        .form-label { font-weight: 600; color: #495057; margin-bottom: 8px; }
        .required::after { content: " *"; color: #dc3545; }
    </style>
</head>
<body style="background: #f8f9fa;">
<!-- NAVBAR -->
<nav class="navbar navbar-dark bg-dark">
    <div class="container-fluid">
        <a class="navbar-brand" href="/crm/lead!list.action"><i class="fas fa-arrow-left"></i> Back to Leads</a>
        <h5 class="text-white mb-0"><i class="fas fa-user-plus"></i> <s:if test="lead.id != null">Edit Lead</s:if><s:else>New Lead</s:else></h5>
    </div>
</nav>

<!-- FORM -->
<div class="form-container">
    <s:form action="lead!save" method="post" theme="bootstrap">
        <!-- Hidden ID field for update -->
        <s:if test="lead.id != null">
            <s:hidden name="lead.id" />
        </s:if>

        <!-- CONTACT INFORMATION -->
        <div class="section-title"><i class="fas fa-user-circle"></i> Contact Information</div>
        <div class="row">
            <div class="col-md-6 mb-3">
                <label class="form-label required">First Name</label>
                <s:textfield name="lead.nomeContatto" class="form-control" required="true" placeholder="Nome" />
                <s:fielderror fieldName="lead.nomeContatto" />
            </div>
            <div class="col-md-6 mb-3">
                <label class="form-label">Last Name</label>
                <s:textfield name="lead.cognomeContatto" class="form-control" placeholder="Cognome" />
                <s:fielderror fieldName="lead.cognomeContatto" />
            </div>
        </div>

        <div class="row">
            <div class="col-md-6 mb-3">
                <label class="form-label required">Email</label>
                <s:textfield name="lead.email" class="form-control" type="email" required="true" placeholder="email@example.com" />
                <s:fielderror fieldName="lead.email" />
            </div>
            <div class="col-md-6 mb-3">
                <label class="form-label required">Phone</label>
                <s:textfield name="lead.telefono" class="form-control" required="true" placeholder="+39 123456789" />
                <s:fielderror fieldName="lead.telefono" />
            </div>
        </div>

        <!-- COMPANY INFORMATION -->
        <div class="section-title"><i class="fas fa-building"></i> Company Information</div>
        <div class="mb-3">
            <label class="form-label required">Company Name</label>
            <s:textfield name="lead.azienda" class="form-control" required="true" placeholder="Azienda" />
            <s:fielderror fieldName="lead.azienda" />
        </div>

        <div class="row">
            <div class="col-md-6 mb-3">
                <label class="form-label">Address</label>
                <s:textfield name="lead.indirizzo" class="form-control" placeholder="Via/Piazza" />
            </div>
            <div class="col-md-2 mb-3">
                <label class="form-label">City</label>
                <s:textfield name="lead.citta" class="form-control" placeholder="Città" />
            </div>
            <div class="col-md-2 mb-3">
                <label class="form-label">Province</label>
                <s:textfield name="lead.provincia" class="form-control" placeholder="Provincia" />
            </div>
            <div class="col-md-2 mb-3">
                <label class="form-label">ZIP Code</label>
                <s:textfield name="lead.cap" class="form-control" placeholder="CAP" />
            </div>
        </div>

        <!-- LEAD ORIGIN & INFORMATION -->
        <div class="section-title"><i class="fas fa-route"></i> Lead Origin & Info</div>
        <div class="row">
            <div class="col-md-6 mb-3">
                <label class="form-label">Lead Source (Origine)</label>
                <s:select name="lead.origine" list="originiLead" class="form-select" />
                <s:fielderror fieldName="lead.origine" />
            </div>
            <div class="col-md-6 mb-3">
                <label class="form-label required">Status</label>
                <s:select name="lead.stato" list="statiLead" class="form-select" required="true" />
                <s:fielderror fieldName="lead.stato" />
            </div>
        </div>

        <div class="mb-3">
            <label class="form-label">Requirements/Notes</label>
            <s:textarea name="lead.esigenza" class="form-control" rows="3" placeholder="Descrivi le esigenze del lead..." />
        </div>

        <!-- BUDGET & PROBABILITY -->
        <div class="section-title"><i class="fas fa-euro-sign"></i> Budget & Probability</div>
        <div class="row">
            <div class="col-md-4 mb-3">
                <label class="form-label">Estimated Budget (€)</label>
                <s:textfield name="lead.budgetStimato" class="form-control" type="number" placeholder="0" />
                <s:fielderror fieldName="lead.budgetStimato" />
            </div>
            <div class="col-md-4 mb-3">
                <label class="form-label">Closure Probability (%)</label>
                <div class="input-group">
                    <s:textfield name="lead.probabilitaChiusura" class="form-control" type="number" min="0" max="100" placeholder="0" />
                    <span class="input-group-text">%</span>
                </div>
                <s:fielderror fieldName="lead.probabilitaChiusura" />
            </div>
            <div class="col-md-4 mb-3">
                <label class="form-label">Estimated Value</label>
                <div class="input-group">
                    <span class="input-group-text">€</span>
                    <input type="text" class="form-control" id="estimatedValue" disabled value="0">
                </div>
                <small class="text-muted">Budget × Probability ÷ 100</small>
            </div>
        </div>

        <!-- DATES -->
        <div class="section-title"><i class="fas fa-calendar"></i> Timeline</div>
        <div class="row">
            <div class="col-md-6 mb-3">
                <label class="form-label">Contact Date</label>
                <s:textfield name="lead.dataContatto" class="form-control" type="date" />
                <s:fielderror fieldName="lead.dataContatto" />
            </div>
            <div class="col-md-6 mb-3">
                <label class="form-label">Next Follow-up Date</label>
                <s:textfield name="lead.dataProssimoFollowup" class="form-control" type="date" />
                <s:fielderror fieldName="lead.dataProssimoFollowup" />
            </div>
        </div>

        <!-- BUTTONS -->
        <div class="row mt-4">
            <div class="col-md-8">
                <s:submit value="Save Lead" class="btn btn-primary btn-lg" />
                <a href="/crm/lead!list.action" class="btn btn-secondary btn-lg">Cancel</a>
            </div>
            <s:if test="lead.id != null">
                <div class="col-md-4 text-end">
                    <button type="button" class="btn btn-danger btn-lg" onclick="if(confirm('Delete this lead?')) { window.location.href='/crm/lead!delete.action?id=<s:property value='lead.id' />'; }">
                        <i class="fas fa-trash"></i> Delete
                    </button>
                </div>
            </s:if>
        </div>

        <!-- ACTION MESSAGES -->
        <s:if test="hasActionMessages()">
            <div class="alert alert-success mt-3">
                <s:actionmessage />
            </div>
        </s:if>
        <s:if test="hasActionErrors()">
            <div class="alert alert-danger mt-3">
                <s:actionerror />
            </div>
        </s:if>
    </s:form>
</div>

<script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>

<script>
// Calculate estimated value dynamically
function calculateEstimatedValue() {
    const budget = parseFloat(document.querySelector('[name="lead.budgetStimato"]').value) || 0;
    const probability = parseFloat(document.querySelector('[name="lead.probabilitaChiusura"]').value) || 0;
    const value = (budget * probability) / 100;
    document.getElementById('estimatedValue').value = value.toFixed(2);
}

// Add event listeners
document.querySelector('[name="lead.budgetStimato"]').addEventListener('input', calculateEstimatedValue);
document.querySelector('[name="lead.probabilitaChiusura"]').addEventListener('input', calculateEstimatedValue);

// Initial calculation
calculateEstimatedValue();
</script>
</body>
</html>
