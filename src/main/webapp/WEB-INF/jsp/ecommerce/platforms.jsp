<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="s" uri="/struts-tags"%>
<!DOCTYPE html>
<html lang="it">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Gestione Piattaforme - Centralizzazione eCommerce</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.0/font/bootstrap-icons.css" rel="stylesheet">
</head>
<body>
    <div class="container-fluid mt-4">
        <div class="row mb-4">
            <div class="col-md-8">
                <h1><i class="bi bi-shop"></i> Gestione Piattaforme</h1>
            </div>
            <div class="col-md-4 text-end">
                <a href="<s:url action='dashboard' namespace='/app/ecommerce'/>" class="btn btn-secondary me-2">
                    <i class="bi bi-arrow-left"></i> Indietro
                </a>
                <a href="<s:url action='platform-edit' namespace='/app/ecommerce'/>" class="btn btn-success">
                    <i class="bi bi-plus-circle"></i> Aggiungi Piattaforma
                </a>
            </div>
        </div>

        <!-- Messaggi -->
        <s:if test="hasActionMessages()">
            <div class="alert alert-success alert-dismissible fade show" role="alert">
                <s:iterator value="actionMessages">
                    <s:property/>
                </s:iterator>
                <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
            </div>
        </s:if>
        <s:if test="hasActionErrors()">
            <div class="alert alert-danger alert-dismissible fade show" role="alert">
                <s:iterator value="actionErrors">
                    <s:property/>
                </s:iterator>
                <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
            </div>
        </s:if>

        <!-- Tabella Piattaforme -->
        <div class="card">
            <div class="card-header bg-light">
                <h5 class="mb-0">Piattaforme Configurate</h5>
            </div>
            <div class="card-body">
                <s:if test="platforms != null && platforms.size() > 0">
                    <div class="table-responsive">
                        <table class="table table-hover">
                            <thead class="table-light">
                                <tr>
                                    <th>Tipo Piattaforma</th>
                                    <th>Nome Negozio</th>
                                    <th>URL</th>
                                    <th>Stato</th>
                                    <th>Frequenza Sync</th>
                                    <th>Ultimo Sync</th>
                                    <th>Azioni</th>
                                </tr>
                            </thead>
                            <tbody>
                                <s:iterator value="platforms">
                                    <tr>
                                        <td>
                                            <span class="badge bg-<s:if test='platformType.toString() == "WOOCOMMERCE"'>info</s:if><s:elseif test='platformType.toString() == "SHOPIFY"'>success</s:elseif><s:elseif test='platformType.toString() == "AMAZON"'>warning</s:elseif><s:else>danger</s:else>">
                                                <s:property value="platformType"/>
                                            </span>
                                        </td>
                                        <td><strong><s:property value="storeName"/></strong></td>
                                        <td><small><s:property value="storeUrl"/></small></td>
                                        <td>
                                            <s:if test="isActive">
                                                <span class="badge bg-success">Attivo</span>
                                            </s:if>
                                            <s:else>
                                                <span class="badge bg-danger">Inattivo</span>
                                            </s:else>
                                        </td>
                                        <td><small><s:property value="syncFrequencyMinutes"/> min</small></td>
                                        <td>
                                            <small>
                                                <s:if test="lastSync != null">
                                                    <s:property value="lastSync" format="dd/MM/yyyy HH:mm"/>
                                                    <br/>
                                                    <span class="badge bg-<s:if test='lastSyncStatus.toString() == "SUCCESS"'>success</s:if><s:else>danger</s:else>">
                                                        <s:property value="lastSyncStatus"/>
                                                    </span>
                                                </s:if>
                                                <s:else>
                                                    <em>Mai</em>
                                                </s:else>
                                            </small>
                                        </td>
                                        <td>
                                            <a href="<s:url action='platform-edit' namespace='/app/ecommerce'><s:param name='id'><s:property value='id'/></s:param></s:url>" class="btn btn-sm btn-primary" title="Modifica">
                                                <i class="bi bi-pencil"></i>
                                            </a>
                                            <button class="btn btn-sm btn-info" onclick="testConnection(<s:property value='id'/>, '<s:property value='storeName'/>')" title="Test Connessione">
                                                <i class="bi bi-link-45deg"></i>
                                            </button>
                                            <a href="<s:url action='platform-delete' namespace='/app/ecommerce'><s:param name='id'><s:property value='id'/></s:param></s:url>" class="btn btn-sm btn-danger" onclick="return confirm('Sei sicuro?')" title="Elimina">
                                                <i class="bi bi-trash"></i>
                                            </a>
                                        </td>
                                    </tr>
                                </s:iterator>
                            </tbody>
                        </table>
                    </div>
                </s:if>
                <s:else>
                    <div class="alert alert-info mb-0">
                        Nessuna piattaforma configurata.
                        <a href="<s:url action='platform-edit' namespace='/app/ecommerce'/>">Aggiungine una</a>
                    </div>
                </s:else>
            </div>
        </div>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
    <script>
        function testConnection(platformId, storeName) {
            if (confirm('Testare la connessione con ' + storeName + '?')) {
                const btn = event.target.closest('button');
                btn.disabled = true;
                btn.innerHTML = '<i class="bi bi-hourglass-split"></i>';

                fetch('<s:url action="test-connection" namespace="/app/ecommerce"/>', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify({ id: platformId })
                })
                .then(response => response.json())
                .then(data => {
                    btn.disabled = false;
                    btn.innerHTML = '<i class="bi bi-link-45deg"></i>';
                    if (data.success) {
                        alert('✓ Connessione riuscita con ' + storeName);
                    } else {
                        alert('✗ Errore: ' + data.message);
                    }
                })
                .catch(error => {
                    btn.disabled = false;
                    btn.innerHTML = '<i class="bi bi-link-45deg"></i>';
                    alert('Errore: ' + error);
                });
            }
        }
    </script>
</body>
</html>
