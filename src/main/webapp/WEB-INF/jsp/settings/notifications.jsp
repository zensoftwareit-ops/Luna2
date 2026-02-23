<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="s" uri="/struts-tags" %>
<!DOCTYPE html>
<html lang="it">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Impostazioni Notifiche - Luna2</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.10.0/font/bootstrap-icons.css">
</head>
<body>
    <%@ include file="../includes/sidebar.jsp" %>

    <div class="col-md-10 content-wrapper p-4">
        <div class="container-fluid">
            <div class="d-flex justify-content-between align-items-center mb-4">
                <h1 class="h3"><i class="bi bi-bell me-2"></i>Impostazioni Notifiche</h1>
            </div>

            <s:if test="hasActionMessages()">
                <div class="alert alert-success alert-dismissible fade show">
                    <s:actionmessage/>
                    <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
                </div>
            </s:if>

            <s:if test="hasActionErrors()">
                <div class="alert alert-danger alert-dismissible fade show">
                    <s:actionerror/>
                    <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
                </div>
            </s:if>

            <form action="<s:url action='notifications-save' namespace='/app/settings'/>" method="post">
                <s:token/>

                <div class="card mb-3">
                    <div class="card-header">
                        <h5 class="mb-0">Canali</h5>
                    </div>
                    <div class="card-body">
                        <div class="form-check form-switch">
                            <s:checkbox name="preference.emailEnabled" cssClass="form-check-input"/>
                            <label class="form-check-label">Email</label>
                        </div>
                        <div class="form-check form-switch">
                            <s:checkbox name="preference.pushEnabled" cssClass="form-check-input"/>
                            <label class="form-check-label">Push</label>
                        </div>
                        <div class="form-check form-switch">
                            <s:checkbox name="preference.smsEnabled" cssClass="form-check-input"/>
                            <label class="form-check-label">SMS</label>
                        </div>
                    </div>
                </div>

                <div class="card mb-3">
                    <div class="card-header">
                        <h5 class="mb-0">Frequenza</h5>
                    </div>
                    <div class="card-body">
                        <label class="form-label">Digest</label>
                        <s:select name="preference.digestFrequency" cssClass="form-select"
                                  list="digestFrequencies" listKey="name()" listValue="name()"/>
                    </div>
                </div>

                <div class="card mb-3">
                    <div class="card-header">
                        <h5 class="mb-0">Quiet Hours</h5>
                    </div>
                    <div class="card-body row">
                        <div class="col-md-6">
                            <label class="form-label">Inizio</label>
                            <s:textfield name="preference.quietStartTime" cssClass="form-control" placeholder="22:00"/>
                        </div>
                        <div class="col-md-6">
                            <label class="form-label">Fine</label>
                            <s:textfield name="preference.quietEndTime" cssClass="form-control" placeholder="08:00"/>
                        </div>
                    </div>
                </div>

                <div class="card mb-3">
                    <div class="card-header">
                        <h5 class="mb-0">Tipi di evento</h5>
                    </div>
                    <div class="card-body">
                        <div class="row">
                            <div class="col-md-6">
                                <div class="form-check">
                                    <input class="form-check-input" type="checkbox" name="eventTypes" value="FATTURA_ARRIVATA" <s:if test="eventTypes != null && eventTypes.contains('FATTURA_ARRIVATA')">checked</s:if> />
                                    <label class="form-check-label">Fattura arrivata</label>
                                </div>
                                <div class="form-check">
                                    <input class="form-check-input" type="checkbox" name="eventTypes" value="NOTIFICA_SDI" <s:if test="eventTypes != null && eventTypes.contains('NOTIFICA_SDI')">checked</s:if> />
                                    <label class="form-check-label">Notifica SDI</label>
                                </div>
                                <div class="form-check">
                                    <input class="form-check-input" type="checkbox" name="eventTypes" value="PREVENTIVO_APERTO" <s:if test="eventTypes != null && eventTypes.contains('PREVENTIVO_APERTO')">checked</s:if> />
                                    <label class="form-check-label">Preventivo aperto</label>
                                </div>
                                <div class="form-check">
                                    <input class="form-check-input" type="checkbox" name="eventTypes" value="PREVENTIVO_LETTO" <s:if test="eventTypes != null && eventTypes.contains('PREVENTIVO_LETTO')">checked</s:if> />
                                    <label class="form-check-label">Preventivo letto</label>
                                </div>
                            </div>
                            <div class="col-md-6">
                                <div class="form-check">
                                    <input class="form-check-input" type="checkbox" name="eventTypes" value="ORDINE_CONFERMATO" <s:if test="eventTypes != null && eventTypes.contains('ORDINE_CONFERMATO')">checked</s:if> />
                                    <label class="form-check-label">Ordine confermato</label>
                                </div>
                                <div class="form-check">
                                    <input class="form-check-input" type="checkbox" name="eventTypes" value="SCADENZA_IMMINENTE" <s:if test="eventTypes != null && eventTypes.contains('SCADENZA_IMMINENTE')">checked</s:if> />
                                    <label class="form-check-label">Scadenza imminente</label>
                                </div>
                                <div class="form-check">
                                    <input class="form-check-input" type="checkbox" name="eventTypes" value="MERCE_IN_MAGAZZINO" <s:if test="eventTypes != null && eventTypes.contains('MERCE_IN_MAGAZZINO')">checked</s:if> />
                                    <label class="form-check-label">Merce in magazzino</label>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>

                <button type="submit" class="btn btn-primary">
                    <i class="bi bi-save me-2"></i>Salva
                </button>
            </form>
        </div>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
