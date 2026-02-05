<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="s" uri="/struts-tags" %>
<!DOCTYPE html>
<html lang="it">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Moduli - Luna2</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.10.0/font/bootstrap-icons.css">
</head>
<body>
    <%@ include file="../includes/sidebar.jsp" %>

    <div class="col-md-10 content-wrapper p-4">
        <div class="container-fluid">
            <div class="d-flex justify-content-between align-items-center mb-4">
                <h1 class="h3"><i class="bi bi-sliders me-2"></i>Gestione Moduli</h1>
            </div>

            <s:if test="hasActionMessages()">
                <div class="alert alert-success alert-dismissible fade show">
                    <s:actionmessage/>
                    <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
                </div>
            </s:if>

            <div class="card">
                <div class="card-body">
                    <form method="post" action="<s:url action='moduli-save' namespace='/app/admin'/>">
                        <div class="form-check form-switch mb-3">
                            <s:checkbox name="coreEnabled" cssClass="form-check-input"/>
                            <label class="form-check-label">Core (Anagrafiche, Preventivi, Fatture, Ordini, Prodotti)</label>
                        </div>
                        <div class="form-check form-switch mb-3">
                            <s:checkbox name="magazzinoEnabled" cssClass="form-check-input"/>
                            <label class="form-check-label">Magazzino</label>
                        </div>
                        <div class="form-check form-switch mb-3">
                            <s:checkbox name="crmEnabled" cssClass="form-check-input"/>
                            <label class="form-check-label">CRM</label>
                        </div>
                        <div class="form-check form-switch mb-3">
                            <s:checkbox name="produzioneEnabled" cssClass="form-check-input"/>
                            <label class="form-check-label">Produzione / Commesse</label>
                        </div>
                        <div class="form-check form-switch mb-4">
                            <s:checkbox name="aiEnabled" cssClass="form-check-input"/>
                            <label class="form-check-label">Modulo AI</label>
                        </div>

                        <button type="submit" class="btn btn-primary">
                            <i class="bi bi-save me-2"></i>Salva impostazioni
                        </button>
                    </form>
                </div>
            </div>
        </div>
    </div>

    </div>
</div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
