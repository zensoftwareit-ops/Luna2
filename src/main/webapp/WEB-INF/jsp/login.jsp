<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="s" uri="/struts-tags" %>
<!DOCTYPE html>
<html lang="it">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Luna2 - Login</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.10.0/font/bootstrap-icons.css">
    <style>
        body {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            min-height: 100vh;
            display: flex;
            align-items: center;
        }
        .login-card {
            max-width: 400px;
            margin: 0 auto;
        }
        .card {
            border: none;
            border-radius: 15px;
            box-shadow: 0 10px 40px rgba(0,0,0,0.2);
        }
        .logo-section {
            text-align: center;
            padding: 30px 0;
        }
        .logo-text {
            font-size: 2.5rem;
            font-weight: 700;
            color: #667eea;
            margin: 0;
        }
        .logo-subtitle {
            color: #6c757d;
            font-size: 0.9rem;
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="login-card">
            <div class="card">
                <div class="card-body p-5">
                    <div class="logo-section">
                        <img src="${pageContext.request.contextPath}/assets/images/logo.png" alt="Luna2" class="img-fluid mb-3" style="max-height: 80px;">
                        <p class="logo-subtitle">Gestionale in cloud</p>
                    </div>

                    <s:if test="hasActionMessages()">
                        <div class="alert alert-success">
                            <s:actionmessage/>
                        </div>
                    </s:if>

                    <s:if test="hasActionErrors()">
                        <div class="alert alert-danger">
                            <s:actionerror/>
                        </div>
                    </s:if>

                    <s:form action="authenticate" method="post" theme="simple">
                        <div class="mb-3">
                            <label for="username" class="form-label">
                                <i class="bi bi-person me-2"></i>Username
                            </label>
                            <s:textfield name="username" cssClass="form-control" placeholder="Inserisci username" required="true" id="username"/>
                            <s:fielderror fieldName="username" cssClass="text-danger small"/>
                        </div>

                        <div class="mb-4">
                            <label for="password" class="form-label">
                                <i class="bi bi-lock me-2"></i>Password
                            </label>
                            <s:password name="password" cssClass="form-control" placeholder="Inserisci password" required="true" id="password"/>
                            <s:fielderror fieldName="password" cssClass="text-danger small"/>
                        </div>

                        <div class="d-grid">
                            <button type="submit" class="btn btn-primary btn-lg">
                                <i class="bi bi-box-arrow-in-right me-2"></i>Accedi
                            </button>
                        </div>
                    </s:form>

                    <div class="text-center mt-4">
                        <small class="text-muted">
                            &copy; 2026 Zen Software - Tutti i diritti riservati
                        </small>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
