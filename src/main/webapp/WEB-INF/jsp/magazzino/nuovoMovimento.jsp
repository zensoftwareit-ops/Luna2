<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="s" uri="/struts-tags" %>
<!DOCTYPE html>
<html lang="it">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Movimento Magazzino - Luna2</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.10.0/font/bootstrap-icons.css">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/select2@4.1.0-rc.0/dist/css/select2.min.css" />
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/select2-bootstrap-5-theme@1.3.0/dist/select2-bootstrap-5-theme.min.css" />
    <style>
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
        }
        .sidebar {
            position: fixed;
            top: 0;
            left: 0;
            height: 100vh;
            width: 250px;
            background: linear-gradient(180deg, #667eea 0%, #764ba2 100%);
            overflow-y: auto;
            z-index: 1000;
        }
        .sidebar .nav-link {
            color: rgba(255,255,255,0.8);
            padding: 12px 20px;
            border-radius: 8px;
            margin: 4px 0;
            transition: all 0.3s;
        }
        .sidebar .nav-link:hover,
        .sidebar .nav-link.active {
            background: rgba(255,255,255,0.2);
            color: white;
        }
        .logo-text {
            font-size: 1.8rem;
            font-weight: 700;
            color: white;
        }
        .content-wrapper {
            margin-left: 250px;
            width: calc(100% - 250px);
            background-color: #f8f9fa;
            min-height: 100vh;
        }
        @media (max-width: 991.98px) {
            .sidebar {
                position: static;
                width: 100%;
                height: auto;
            }
            .content-wrapper {
                margin-left: 0;
            }
        }
    </style>
</head>
<body>
    <div class="sidebar p-4">
        <div class="text-center mb-4">
            <img src="${pageContext.request.contextPath}/assets/images/logo.png" alt="Luna2" class="img-fluid mb-2" style="max-height: 60px;">
            <p class="text-white-50 small">Gestionale Cloud</p>
        </div>

        <ul class="nav flex-column">
            <li class="nav-item">
                <a class="nav-link" href="<s:url action='dashboard' namespace='/app'/>"><i class="bi bi-speedometer2 me-2"></i>Dashboard</a>
            </li>
            <s:if test="#session.enabledModules['CORE']">
                <li class="nav-item">
                    <a class="nav-link" href="<s:url action='list' namespace='/app/clienti'/>"><i class="bi bi-people me-2"></i>Clienti</a>
                </li>
                <li class="nav-item">
                    <a class="nav-link" href="<s:url action='list' namespace='/app/fornitori'/>"><i class="bi bi-truck me-2"></i>Fornitori</a>
                </li>
                <li class="nav-item">
                    <a class="nav-link" href="<s:url action='list' namespace='/app/prodotti'/>"><i class="bi bi-box-seam me-2"></i>Prodotti</a>
                </li>
                <li class="nav-item mt-3">
                    <h6 class="text-white-50 text-uppercase small px-3">Documenti</h6>
                </li>
                <li class="nav-item">
                    <a class="nav-link" href="<s:url action='preventivi' namespace='/app/documenti'/>"><i class="bi bi-file-earmark-text me-2"></i>Preventivi</a>
                </li>
                <li class="nav-item">
                    <a class="nav-link" href="<s:url action='ordini' namespace='/app/documenti'/>"><i class="bi bi-cart-check me-2"></i>Ordini</a>
                </li>
                <li class="nav-item">
                    <a class="nav-link" href="<s:url action='ddt' namespace='/app/documenti'/>"><i class="bi bi-truck-flatbed me-2"></i>DDT</a>
                </li>
                <li class="nav-item">
                    <a class="nav-link" href="<s:url action='fatture' namespace='/app/documenti'/>"><i class="bi bi-receipt me-2"></i>Fatture</a>
                </li>
            </s:if>
            <s:if test="#session.enabledModules['MAGAZZINO']">
                <li class="nav-item mt-3">
                    <a class="nav-link active" href="<s:url action='list' namespace='/app/magazzino'/>"><i class="bi bi-archive me-2"></i>Magazzino</a>
                </li>
            </s:if>
            <s:if test="#session.enabledModules['PRODUZIONE']">
                <li class="nav-item">
                    <a class="nav-link" href="<s:url action='commesse' namespace='/app/produzione'/>"><i class="bi bi-gear me-2"></i>Produzione</a>
                </li>
            </s:if>
            <s:if test="#session.enabledModules['AI']">
                <li class="nav-item">
                    <a class="nav-link" href="<s:url action='index' namespace='/app/ai'/>"><i class="bi bi-cpu me-2"></i>Modulo AI</a>
                </li>
            </s:if>
            <s:if test="#session.enabledModules['CORE']">
                <li class="nav-item">
                    <a class="nav-link" href="<s:url action='vendite' namespace='/app/report'/>"><i class="bi bi-bar-chart me-2"></i>Report</a>
                </li>
            </s:if>
            <s:if test="#session.currentUser != null && #session.currentUser.username != null && #session.currentUser.username.equalsIgnoreCase('admin')">
                <li class="nav-item mt-3">
                    <a class="nav-link" href="<s:url action='moduli' namespace='/app/admin'/>"><i class="bi bi-sliders me-2"></i>Moduli</a>
                </li>
            </s:if>
            <li class="nav-item mt-5">
                <a class="nav-link" href="<s:url action='logout' namespace='/'/>"><i class="bi bi-box-arrow-right me-2"></i>Logout</a>
            </li>
        </ul>

        <div class="mt-auto pt-4 text-center">
            <small class="text-white-50"><i class="bi bi-person-circle me-1"></i>${sessionScope.currentUser.nomeCompleto}</small>
        </div>
    </div>

    <div class="content-wrapper p-4">
        <div class="container-fluid">
            <div class="row">
                <div class="col-lg-8 offset-lg-2">
                    <div class="card shadow-sm">
                        <div class="card-header bg-gradient-primary text-white">
                            <h4 class="mb-0">
                                <s:if test='tipoMovimento == "CARICO"'>
                                    <i class="bi bi-plus-circle me-2"></i>Carico Merce
                                </s:if>
                            <s:else>
                                <i class="bi bi-dash-circle me-2"></i>Scarico Merce
                            </s:else>
                        </h4>
                    </div>
                    <div class="card-body">
                        <s:if test="hasActionErrors()">
                            <div class="alert alert-danger alert-dismissible fade show">
                                <s:actionerror />
                                <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
                            </div>
                        </s:if>

                        <s:form action="%{tipoMovimento == 'CARICO' ? 'carico' : 'scarico'}" namespace="/app/magazzino" method="post" cssClass="needs-validation" theme="simple">
                            <!-- Prodotto -->
                            <div class="mb-3">
                                <label class="form-label fw-bold">Prodotto *</label>
                                <select name="prodottoId" class="form-select select2-prodotto" required>
                                    <option value="">-- Cerca per codice, nome o barcode --</option>
                                    <s:iterator value="prodotti" var="p">
                                        <option value="<s:property value='#p.id'/>"
                                            data-codice="<s:property value='#p.codice'/>"
                                            data-ean="<s:property value='#p.codiceEan'/>"
                                            data-um="<s:property value='#p.unitaMisura'/>"
                                            data-default-fornitore-id="<s:property value='#p.fornitore != null ? #p.fornitore.id : ""'/>"
                                            data-costo-default="<s:property value='#p.costoAcquisto != null ? #p.costoAcquisto : ""'/>"
                                            data-giacenza="<s:property value='giacenzeByProdottoId[#p.id] != null ? giacenzeByProdottoId[#p.id] : 0'/>"
                                            <s:if test="prodottoId != null && prodottoId == #p.id">selected</s:if>>
                                            <s:property value="#p.codice"/> - <s:property value="#p.nome"/>
                                            <s:if test="#p.codiceEan != null && #p.codiceEan.trim().length() > 0">
                                                [<s:property value="#p.codiceEan"/>]
                                            </s:if>
                                        </option>
                                    </s:iterator>
                                </select>
                                <small class="form-text text-muted">
                                    <i class="bi bi-search me-1"></i>
                                    Digita per cercare, oppure usa la pistola barcode nel campo sotto.
                                </small>
                            </div>

                            <div class="row mb-3" id="productMetaRow" style="display:none;">
                                <div class="col-md-6">
                                    <div class="alert alert-light border mb-0">
                                        <strong>Giacenza attuale:</strong> <span id="selectedProdottoGiacenza">-</span>
                                    </div>
                                </div>
                                <div class="col-md-6">
                                    <div class="alert alert-light border mb-0">
                                        <strong>Unità di misura:</strong> <span id="selectedProdottoUm">-</span>
                                    </div>
                                </div>
                            </div>

                            <!-- Scanner barcode -->
                            <div class="mb-3">
                                <label class="form-label fw-bold">Barcode scanner</label>
                                <div class="input-group">
                                    <span class="input-group-text"><i class="bi bi-upc-scan"></i></span>
                                    <input type="text" id="barcodeInputQuick" class="form-control"
                                           placeholder="Scansiona EAN/Codice prodotto e premi Invio">
                                </div>
                                <small class="form-text text-muted">
                                    Con scanner USB/HID il codice viene inserito come tastiera e seleziona automaticamente il prodotto.
                                </small>
                            </div>

                            <!-- Data Movimento -->
                            <div class="row mb-3">
                                <div class="col-md-6">
                                    <label class="form-label fw-bold">Data Movimento *</label>
                                    <s:textfield name="dataMovimento" type="date" cssClass="form-control" 
                                                value="%{new java.text.SimpleDateFormat('yyyy-MM-dd').format(new java.util.Date())}"/>
                                </div>
                                <div class="col-md-6">
                                    <label class="form-label fw-bold">Quantità *</label>
                                    <s:textfield name="quantita" type="number" step="0.001" cssClass="form-control" 
                                                placeholder="Es: 10" required="true" min="0.001"/>
                                </div>
                            </div>

                            <!-- Fornitore (solo carico) -->
                            <s:if test='tipoMovimento == "CARICO"'>
                                <div class="mb-3">
                                    <label class="form-label fw-bold">Fornitore *</label>
                                    <select name="fornitoreId" id="fornitoreId" class="form-select" required>
                                        <option value="">-- Seleziona fornitore dal listino prodotto --</option>
                                    </select>
                                    <small class="form-text text-muted">
                                        Il prezzo di acquisto viene compilato automaticamente dal listino fornitore, se presente.
                                    </small>
                                </div>
                            </s:if>

                            <!-- Costo Unitario (solo per carico) -->
                            <s:if test='tipoMovimento == "CARICO"'>
                                <div class="mb-3">
                                    <label class="form-label fw-bold">Costo Unitario</label>
                                    <div class="input-group">
                                        <span class="input-group-text">€</span>
                                        <s:textfield name="costoUnitario" type="number" step="0.01" cssClass="form-control" 
                                                    placeholder="Es: 15.50"/>
                                    </div>
                                    <small class="form-text text-muted">
                                        <i class="bi bi-info-circle me-1"></i>
                                        Opzionale - serve per calcolare il valore di magazzino
                                    </small>
                                </div>
                            </s:if>

                            <!-- Causale -->
                            <div class="mb-3">
                                <label class="form-label fw-bold">Causale</label>
                                <s:textfield name="causale" cssClass="form-control" 
                                            placeholder='Es: "Acquisto fornitore", "Reso cliente", "Produzione", ecc.' 
                                            maxlength="255"/>
                            </div>

                            <!-- Note -->
                            <div class="mb-3">
                                <label class="form-label fw-bold">Note</label>
                                <s:textarea name="note" cssClass="form-control" rows="3" 
                                           placeholder="Note aggiuntive sul movimento (opzionale)"/>
                            </div>

                            <!-- Azioni -->
                            <div class="d-flex justify-content-between mt-4">
                                <a href="<s:url action='list' namespace='/app/magazzino'/>" class="btn btn-outline-secondary">
                                    <i class="bi bi-arrow-left me-2"></i>Annulla
                                </a>
                                <button type="submit" class="btn btn-primary">
                                    <s:if test='tipoMovimento == "CARICO"'>
                                        <i class="bi bi-check-circle me-2"></i>Registra Carico
                                    </s:if>
                                    <s:else>
                                        <i class="bi bi-check-circle me-2"></i>Registra Scarico
                                    </s:else>
                                </button>
                            </div>
                        </s:form>
                    </div>
                </div>
            </div>
        </div>
        </div>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/jquery@3.6.0/dist/jquery.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/select2@4.1.0-rc.0/dist/js/select2.min.js"></script>
    <script>
        $(document).ready(function() {
            const $prodotto = $('.select2-prodotto');
            const $barcode = $('#barcodeInputQuick');
            const $fornitore = $('#fornitoreId');
            const $costo = $('input[name="costoUnitario"]');
            const $metaRow = $('#productMetaRow');
            const $metaGiacenza = $('#selectedProdottoGiacenza');
            const $metaUm = $('#selectedProdottoUm');
            const initialFornitoreId = '<s:property value="fornitoreId"/>';

            const listini = [
                <s:iterator value="listiniFornitore" var="lf" status="st">
                {
                    prodottoId: '<s:property value="#lf.prodotto.id"/>',
                    fornitoreId: '<s:property value="#lf.fornitore.id"/>',
                    fornitoreNome: '<s:property value="#lf.fornitore.ragioneSociale" escapeJavaScript="true"/>',
                    prezzoAcquisto: '<s:property value="#lf.prezzoAcquisto"/>'
                }<s:if test="!#st.last">,</s:if>
                </s:iterator>
            ];

            $prodotto.select2({
                theme: 'bootstrap-5',
                placeholder: '-- Cerca per codice, nome o barcode --',
                width: '100%'
            });

            function updateProductMeta() {
                const selected = $prodotto.find('option:selected');
                const id = selected.val();
                if (!id) {
                    $metaRow.hide();
                    return;
                }
                const giacenza = selected.attr('data-giacenza') || '0';
                const um = selected.attr('data-um') || '-';
                $metaGiacenza.text(giacenza);
                $metaUm.text(um);
                $metaRow.show();
            }

            function populateFornitoriFromListino() {
                if (!$fornitore.length) return;
                const selected = $prodotto.find('option:selected');
                const prodottoId = selected.val();
                const defaultFornitoreId = selected.attr('data-default-fornitore-id');
                const defaultCosto = selected.attr('data-costo-default');

                $fornitore.empty();
                $fornitore.append('<option value="">-- Seleziona fornitore dal listino prodotto --</option>');

                const options = listini.filter(l => String(l.prodottoId) === String(prodottoId));
                options.forEach(l => {
                    $fornitore.append(`<option value="${l.fornitoreId}" data-prezzo="${l.prezzoAcquisto}">${l.fornitoreNome}</option>`);
                });

                if (defaultFornitoreId) {
                    $fornitore.val(defaultFornitoreId);
                }
                if (initialFornitoreId) {
                    $fornitore.val(initialFornitoreId);
                }
                if (!$fornitore.val() && options.length > 0) {
                    $fornitore.val(options[0].fornitoreId);
                }

                const selectedFornitoreOpt = $fornitore.find('option:selected');
                const prezzo = selectedFornitoreOpt.attr('data-prezzo');
                if (prezzo) {
                    $costo.val(prezzo);
                } else if (defaultCosto && !$costo.val()) {
                    $costo.val(defaultCosto);
                }
            }

            function normalize(v) {
                return (v || '').toString().trim().toLowerCase();
            }

            function selectProductByCodeOrBarcode(scanned) {
                const key = normalize(scanned);
                if (!key) return false;

                let found = false;
                $prodotto.find('option').each(function() {
                    const text = normalize($(this).text());
                    const code = normalize($(this).attr('data-codice'));
                    const ean = normalize($(this).attr('data-ean'));
                    if (text.includes(key) || code === key || ean === key) {
                        $prodotto.val($(this).val()).trigger('change');
                        found = true;
                        return false;
                    }
                });
                return found;
            }

            $prodotto.on('change', function() {
                updateProductMeta();
                populateFornitoriFromListino();
            });

            $fornitore.on('change', function() {
                const prezzo = $(this).find('option:selected').attr('data-prezzo');
                if (prezzo) {
                    $costo.val(prezzo);
                }
            });

            $barcode.on('keydown', function(e) {
                if (e.key === 'Enter') {
                    e.preventDefault();
                    const scanned = $(this).val();
                    const ok = selectProductByCodeOrBarcode(scanned);
                    if (!ok) {
                        alert('Prodotto non trovato per codice/barcode: ' + scanned);
                    }
                    $(this).val('');
                }
            });

            // inizializzazione stato pagina
            updateProductMeta();
            populateFornitoriFromListino();
        });
    </script>
</body>
</html>
