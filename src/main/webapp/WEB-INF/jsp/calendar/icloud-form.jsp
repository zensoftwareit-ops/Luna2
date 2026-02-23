<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>

<!DOCTYPE html>
<html>
<head>
    <title>Connetti Account iCloud</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/bootstrap.min.css">
    <style>
        .container { max-width: 500px; margin: 50px auto; }
        .card { border: 1px solid #ddd; border-radius: 5px; padding: 30px; }
        .btn-primary { background-color: #007bff; }
        .alert { margin-top: 20px; }
    </style>
</head>
<body>
<div class="container">
    <div class="card">
        <h2>Connetti Account iCloud</h2>
        <p class="text-muted">Inserisci le credenziali per sincronizzare il tuo calendario iCloud.</p>

        <!-- Error/Success Messages -->
        <s:if test="hasActionErrors()">
            <div class="alert alert-danger">
                <s:iterator value="actionErrors">
                    <div><s:property/></div>
                </s:iterator>
            </div>
        </s:if>

        <s:if test="hasActionMessages()">
            <div class="alert alert-success">
                <s:iterator value="actionMessages">
                    <div><s:property/></div>
                </s:iterator>
            </div>
        </s:if>

        <!-- Test Connection Result -->
        <s:if test="testConnectionMessage != null">
            <div class="alert <s:if test="testConnectionResult">alert-success</s:if><s:else>alert-warning</s:else>">
                <strong>Risultato test:</strong> <s:property value="testConnectionMessage"/>
            </div>
        </s:if>

        <!-- iCloud Credentials Form -->
        <form action="${pageContext.request.contextPath}/app/calendar/calendar-icloud-test" method="post">
            <div class="form-group">
                <label for="caldavUrl">URL Calendario iCloud (CalDAV)</label>
                <input type="text" class="form-control" name="caldavUrl" id="caldavUrl" 
                       placeholder="https://caldav.icloud.com/..." 
                       value="<s:property value='caldavUrl'/>" required>
                <small class="form-text text-muted">
                    Tipicamente: https://caldav.icloud.com/ACCOUNT_ID/calendar/
                </small>
            </div>

            <div class="form-group">
                <label for="caldavUsername">Username iCloud (email)</label>
                <input type="email" class="form-control" name="caldavUsername" id="caldavUsername" 
                       placeholder="tu@icloud.com" 
                       value="<s:property value='caldavUsername'/>" required>
            </div>

            <div class="form-group">
                <label for="caldavPassword">Password (o App Password)</label>
                <input type="password" class="form-control" name="caldavPassword" id="caldavPassword" 
                       placeholder="Password app iCloud" required>
                <small class="form-text text-muted">
                    Per sicurezza, usa una password specifica dell'app generata da iCloud
                </small>
            </div>

            <div class="form-group">
                <button type="submit" class="btn btn-warning btn-block" name="action" value="test">
                    Testa Connessione
                </button>
            </div>
        </form>

        <!-- Save Account Form (only if test passed) -->
        <s:if test="testConnectionResult">
            <form action="${pageContext.request.contextPath}/app/calendar/calendar-connect-icloud" method="post">
                <input type="hidden" name="caldavUrl" value="<s:property value='caldavUrl'/>">
                <input type="hidden" name="caldavUsername" value="<s:property value='caldavUsername'/>">
                <input type="hidden" name="caldavPassword" value="<s:property value='caldavPassword'/>">

                <button type="submit" class="btn btn-success btn-block">
                    Salva Account iCloud
                </button>
            </form>
        </s:if>

        <hr>
        <a href="${pageContext.request.contextPath}/app/calendar/calendar" class="btn btn-secondary btn-block">
            Indietro ai Calendari
        </a>
    </div>

    <div class="mt-4">
        <h5>Supporto CalDAV</h5>
        <ul class="small">
            <li><strong>iCloud</strong>: Supporta CalDAV nativo. Usa email Apple come username.</li>
            <li><strong>Google Calendar</strong>: Usa OAuth2 (nella sezione principale del calendario).</li>
            <li><strong>Outlook/Microsoft 365</strong>: Supporta CalDAV con credenziali Exchange.</li>
            <li><strong>Nextcloud/ownCloud</strong>: Supporta CalDAV con URL personalizzato.</li>
        </ul>
    </div>
</div>

<script src="${pageContext.request.contextPath}/assets/js/bootstrap.min.js"></script>
</body>
</html>
