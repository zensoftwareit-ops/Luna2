<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="s" uri="/struts-tags"%>
<!DOCTYPE html>
<html lang="it">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><s:if test="platform.id != null">Modifica</s:if><s:else>Aggiungi</s:else> Piattaforma</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.0/font/bootstrap-icons.css" rel="stylesheet">
</head>
<body>
    <div class="container mt-4">
        <div class="row justify-content-center">
            <div class="col-md-8">
                <div class="card">
                    <div class="card-header bg-light">
                        <h4 class="mb-0">
                            <s:if test="platform.id != null">
                                <i class="bi bi-pencil"></i> Modifica Piattaforma
                            </s:if>
                            <s:else>
                                <i class="bi bi-plus-circle"></i> Aggiungi Piattaforma
                            </s:else>
                        </h4>
                    </div>
                    <div class="card-body">
                        <!-- Messaggi di Errore -->
                        <s:if test="hasFieldErrors()">
                            <div class="alert alert-danger">
                                <s:fielderror/>
                            </div>
                        </s:if>
                        <s:if test="hasActionErrors()">
                            <div class="alert alert-danger">
                                <s:iterator value="actionErrors">
                                    <div><s:property/></div>
                                </s:iterator>
                            </div>
                        </s:if>

                        <form method="post" action="<s:url action='platform-save' namespace='/app/ecommerce'/>">
                            <s:if test="platform.id != null">
                                <input type="hidden" name="id" value="<s:property value='platform.id'/>">
                            </s:if>

                            <!-- Tipo Piattaforma -->
                            <div class="mb-3">
                                <label for="platformType" class="form-label">Tipo Piattaforma <span class="text-danger">*</span></label>
                                <select id="platformType" name="platformType" class="form-select" required onchange="updateInfo()">
                                    <option value="">-- Seleziona --</option>
                                    <option value="WOOCOMMERCE" <s:if test='platform.platformType.toString() == "WOOCOMMERCE"'>selected</s:if>>WooCommerce</option>
                                    <option value="SHOPIFY" <s:if test='platform.platformType.toString() == "SHOPIFY"'>selected</s:if>>Shopify</option>
                                    <option value="AMAZON" <s:if test='platform.platformType.toString() == "AMAZON"'>selected</s:if>>Amazon</option>
                                    <option value="EBAY" <s:if test='platform.platformType.toString() == "EBAY"'>selected</s:if>>eBay</option>
                                </select>
                                <div class="form-text" id="platformInfo"></div>
                            </div>

                            <!-- Nome Negozio -->
                            <div class="mb-3">
                                <label for="storeName" class="form-label">Nome Negozio <span class="text-danger">*</span></label>
                                <input type="text" id="storeName" name="storeName" class="form-control"
                                    value="<s:property value='platform.storeName'/>" required
                                    placeholder="Es: My Store">
                                <div class="form-text">Nome identificativo del negozio</div>
                            </div>

                            <!-- URL Negozio -->
                            <div class="mb-3">
                                <label for="storeUrl" class="form-label">URL Negozio <span class="text-danger">*</span></label>
                                <input type="url" id="storeUrl" name="storeUrl" class="form-control"
                                    value="<s:property value='platform.storeUrl'/>"
                                    placeholder="https://mystore.com">
                                <div class="form-text">URL completo del tuo negozio online</div>
                            </div>

                            <!-- API Key -->
                            <div class="mb-3">
                                <label for="apiKey" class="form-label">API Key / Consumer Key <span class="text-danger">*</span></label>
                                <input type="password" id="apiKey" name="apiKey" class="form-control"
                                    value="<s:property value='platform.apiKey'/>" required
                                    placeholder="Inserisci la chiave API">
                                <div class="form-text">Chiave API dalla tua piattaforma eCommerce</div>
                            </div>

                            <!-- API Secret -->
                            <div class="mb-3">
                                <label for="apiSecret" class="form-label">API Secret / Consumer Secret</label>
                                <input type="password" id="apiSecret" name="apiSecret" class="form-control"
                                    value="<s:property value='platform.apiSecret'/>"
                                    placeholder="Inserisci il segreto API (se richiesto)">
                                <div class="form-text">Segreto API (solo per alcune piattaforme)</div>
                            </div>

                            <!-- Frequenza Sync -->
                            <div class="mb-3">
                                <label for="syncFrequencyMinutes" class="form-label">Frequenza Sincronizzazione (minuti) <span class="text-danger">*</span></label>
                                <input type="number" id="syncFrequencyMinutes" name="syncFrequencyMinutes" class="form-control"
                                    value="<s:property value='platform.syncFrequencyMinutes'/>"
                                    min="5" max="1440" required>
                                <div class="form-text">Ogni quanti minuti sincronizzare i dati (min 5, max 1440)</div>
                            </div>

                            <!-- Stato Attivo -->
                            <div class="mb-3">
                                <div class="form-check">
                                    <input class="form-check-input" type="checkbox" id="isActive" name="isActive"
                                        <s:if test="platform.isActive == true">checked</s:if>>
                                    <label class="form-check-label" for="isActive">
                                        Piattaforma Attiva
                                    </label>
                                </div>
                                <div class="form-text">Disattiva per pausare la sincronizzazione</div>
                            </div>

                            <!-- Pulsanti -->
                            <div class="d-grid gap-2 d-md-flex justify-content-md-end">
                                <a href="<s:url action='platforms' namespace='/app/ecommerce'/>" class="btn btn-secondary">
                                    <i class="bi bi-x-circle"></i> Annulla
                                </a>
                                <button type="submit" class="btn btn-primary">
                                    <i class="bi bi-check-circle"></i> Salva
                                </button>
                            </div>
                        </form>
                    </div>
                </div>

                <!-- Istruzioni -->
                <div class="card mt-4">
                    <div class="card-header bg-light">
                        <h5 class="mb-0"><i class="bi bi-info-circle"></i> Come ottenere le credenziali API</h5>
                    </div>
                    <div class="card-body">
                        <div id="instructions"></div>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
    <script>
        const instructions = {
            WOOCOMMERCE: `
                <h6>WooCommerce (via WooCommerce REST API)</h6>
                <ol>
                    <li>Accedi al dashboard WooCommerce</li>
                    <li>Vai a WooCommerce → Impostazioni → API → Chiavi API</li>
                    <li>Crea una nuova API Key con permessi di lettura</li>
                    <li>Copia Consumer Key e Consumer Secret</li>
                </ol>
            `,
            SHOPIFY: `
                <h6>Shopify (App Privata)</h6>
                <ol>
                    <li>Accedi a Shopify Admin</li>
                    <li>Vai a Impostazioni → Apps e integrazioni → App e integrazioni personalizzate</li>
                    <li>Crea una nuova app o usa quella esistente</li>
                    <li>Vai a Configurazione → Admin API</li>
                    <li>Copia il token di accesso (Access Token)</li>
                </ol>
            `,
            AMAZON: `
                <h6>Amazon Seller Central</h6>
                <ol>
                    <li>Accedi a Seller Central</li>
                    <li>Vai a Impostazioni → Autorizzazione dell'applicazione</li>
                    <li>Genera MWS Authorization Token</li>
                    <li>Copia Seller ID e Authorization Token</li>
                </ol>
            `,
            EBAY: `
                <h6>eBay (OAuth)</h6>
                <ol>
                    <li>Accedi a eBay Developer Portal</li>
                    <li>Crea un'applicazione se non esiste</li>
                    <li>Genera un User Token o Authorization Code</li>
                    <li>Copia il token per l'accesso API</li>
                </ol>
            `
        };

        function updateInfo() {
            const select = document.getElementById('platformType');
            const info = document.getElementById('platformInfo');
            const platform = select.value;

            if (instructions[platform]) {
                document.getElementById('instructions').innerHTML = instructions[platform];
            } else {
                document.getElementById('instructions').innerHTML = 'Seleziona una piattaforma per leggere le istruzioni';
            }
        }

        // Aggiorna al caricamento
        document.addEventListener('DOMContentLoaded', updateInfo);
    </script>
</body>
</html>
