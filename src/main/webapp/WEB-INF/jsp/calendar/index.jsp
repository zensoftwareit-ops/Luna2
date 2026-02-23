<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="s" uri="/struts-tags" %>
<!DOCTYPE html>
<html lang="it">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Calendario - Luna2</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.10.0/font/bootstrap-icons.css">
</head>
<body>
    <%@ include file="../includes/sidebar.jsp" %>

    <div class="col-md-10 content-wrapper p-4">
        <div class="container-fluid">
            <div class="d-flex justify-content-between align-items-center mb-4">
                <h1 class="h3"><i class="bi bi-calendar-event me-2"></i>Calendario</h1>
                <div class="btn-group">
                    <a href="<s:url action='calendar-sync' namespace='/app/calendar'/>" class="btn btn-outline-primary">
                        <i class="bi bi-arrow-repeat me-2"></i>Sincronizza
                    </a>
                    <a href="<s:url action='calendar-connect-google' namespace='/app/calendar'/>" class="btn btn-outline-success">
                        <i class="bi bi-google me-2"></i>Connetti Google
                    </a>
                    <a href="<s:url action='calendar-connect-icloud' namespace='/app/calendar'/>" class="btn btn-outline-info">
                        <i class="bi bi-apple me-2"></i>Connetti iCloud
                    </a>
                </div>
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

            <s:if test="googleAuthUrl != null && !googleAuthUrl.isEmpty()">
                <div class="alert alert-info">
                    <div class="fw-semibold">OAuth URL generato</div>
                    <a href="<s:property value='googleAuthUrl'/>" target="_blank">
                        <s:property value="googleAuthUrl"/>
                    </a>
                </div>
            </s:if>

            <div class="card">
                <div class="card-body">
                    <s:if test="accounts != null && !accounts.isEmpty()">
                        <div class="table-responsive">
                            <table class="table table-striped">
                                <thead>
                                    <tr>
                                        <th>Provider</th>
                                        <th>Calendar ID</th>
                                        <th>Time Zone</th>
                                        <th>Sync</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <s:iterator value="accounts" var="acc">
                                        <tr>
                                            <td><s:property value="#acc.provider"/></td>
                                            <td><s:property value="#acc.calendarId"/></td>
                                            <td><s:property value="#acc.timeZone"/></td>
                                            <td>
                                                <s:if test="#acc.syncEnabled">
                                                    <span class="badge bg-success">ON</span>
                                                </s:if>
                                                <s:else>
                                                    <span class="badge bg-secondary">OFF</span>
                                                </s:else>
                                            </td>
                                        </tr>
                                    </s:iterator>
                                </tbody>
                            </table>
                        </div>
                    </s:if>
                    <s:else>
                        <div class="text-muted">Nessun account calendario configurato.</div>
                    </s:else>
                </div>
            </div>
        </div>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
