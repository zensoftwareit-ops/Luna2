<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
<!DOCTYPE html>
<html lang="it">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Scanner Barcode - Luna2</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.0/font/bootstrap-icons.css">
    <style>
        #barcodeInput {
            font-size: 1.5rem;
            text-align: center;
            letter-spacing: 0.1em;
        }
        .scan-area {
            border: 3px dashed #0d6efd;
            border-radius: 10px;
            padding: 2rem;
            margin: 2rem 0;
            background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
        }
        .product-card {
            transition: all 0.3s ease;
        }
        .product-card:hover {
            transform: translateY(-5px);
            box-shadow: 0 8px 16px rgba(0,0,0,0.1);
        }
        .barcode-icon {
            font-size: 4rem;
            color: #0d6efd;
            animation: pulse 2s infinite;
        }
        @keyframes pulse {
            0%, 100% { opacity: 1; }
            50% { opacity: 0.5; }
        }
    </style>
</head>
<body>

<%@ include file="../includes/sidebar.jsp" %>

<div class="content-wrapper p-4">
    <div class="container-fluid">
        <div class="d-flex justify-content-between align-items-center mb-4">
            <h1 class="h3">
                <i class="bi bi-upc-scan me-2"></i>Scanner Barcode
            </h1>
        </div>

        <div class="row">
            <div class="col-lg-10 offset-lg-1">
                <!-- Area di Scansione -->
                <div class="card shadow-sm mb-4">
                    <div class="card-body">
                        <div class="scan-area text-center">
                            <div class="barcode-icon mb-3">
                                <i class="bi bi-upc-scan"></i>
                            </div>
                            <h5 class="mb-3">Scansiona il barcode del prodotto</h5>
                            
                            <form id="scanForm" class="mb-3">
                                <div class="row justify-content-center">
                                    <div class="col-md-6">
                                        <input type="text" 
                                               id="barcodeInput" 
                                               name="barcodeInput" 
                                               class="form-control form-control-lg" 
                                               placeholder="Inquadra il barcode..."
                                               autocomplete="off"
                                               autofocus />
                                    </div>
                                </div>
                                <small class="text-muted d-block mt-2">
                                    <i class="bi bi-info-circle me-1"></i>
                                    Usa lo scanner barcode o inserisci manualmente il codice
                                </small>
                            </form>

                            <div class="btn-group mt-3">
                                <button type="button" class="btn btn-success" id="btnCarico" disabled>
                                    <i class="bi bi-plus-circle me-2"></i>Carico
                                </button>
                                <button type="button" class="btn btn-danger" id="btnScarico" disabled>
                                    <i class="bi bi-dash-circle me-2"></i>Scarico
                                </button>
                                <button type="button" class="btn btn-info" id="btnStorico" disabled>
                                    <i class="bi bi-clock-history me-2"></i>Storico
                                </button>
                                <button type="button" class="btn btn-secondary" id="btnEtichetta" disabled>
                                    <i class="bi bi-printer me-2"></i>Stampa Etichetta
                                </button>
                            </div>
                        </div>

                        <!-- Risultato Scansione -->
                        <div id="scanResult" style="display: none;"></div>
                    </div>
                </div>

                <!-- Card Prodotto Scansionato -->
                <div id="productCard" style="display: none;" class="card shadow-sm product-card">
                    <div class="card-header bg-success text-white">
                        <h5 class="mb-0">
                            <i class="bi bi-check-circle me-2"></i>Prodotto Trovato
                        </h5>
                    </div>
                    <div class="card-body">
                        <div class="row">
                            <div class="col-md-8">
                                <h4 id="productName"></h4>
                                <div class="mb-2">
                                    <strong>Codice:</strong> <code id="productCode"></code>
                                </div>
                                <div class="mb-2" id="productEanDiv" style="display: none;">
                                    <strong>EAN:</strong> <code id="productEan"></code>
                                </div>
                                <div class="mb-2" id="productCategoriaDiv" style="display: none;">
                                    <strong>Categoria:</strong> <span class="badge bg-secondary" id="productCategoria"></span>
                                </div>
                                <div class="mb-2" id="productDescrizioneDiv" style="display: none;">
                                    <strong>Descrizione:</strong> <p id="productDescrizione"></p>
                                </div>
                            </div>
                            <div class="col-md-4 text-end">
                                <div class="mb-2">
                                    <small class="text-muted">Prezzo Base</small>
                                    <h5 id="productPrezzo" class="text-primary"></h5>
                                </div>
                                <div class="mb-2" id="productGiacenzaDiv">
                                    <small class="text-muted">Giacenza Attuale</small>
                                    <h5 id="productGiacenza" class="text-success"></h5>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>

    
    <script>
        $(document).ready(function() {
            var currentProductId = null;
            var scanTimeout = null;

            // Focus automatico sul campo di input
            $('#barcodeInput').focus();

            // Gestione scansione (sia manuale che scanner)
            $('#barcodeInput').on('input', function() {
                var barcode = $(this).val().trim();
                
                // Pulisci il timeout precedente
                if (scanTimeout) {
                    clearTimeout(scanTimeout);
                }

                // Se il campo ha almeno 3 caratteri, cerca dopo 500ms
                if (barcode.length >= 3) {
                    scanTimeout = setTimeout(function() {
                        cercaProdotto(barcode);
                    }, 500);
                }
            });

            // Submit form
            $('#scanForm').on('submit', function(e) {
                e.preventDefault();
                var barcode = $('#barcodeInput').val().trim();
                if (barcode) {
                    cercaProdotto(barcode);
                }
            });

            // Funzione per cercare il prodotto
            function cercaProdotto(barcode) {
                $.ajax({
                    url: '<s:url action="cercaBarcode" namespace="/app/magazzino"/>',
                    type: 'POST',
                    data: { barcodeInput: barcode },
                    dataType: 'json',
                    success: function(response) {
                        if (response.scanResult === 'SUCCESS') {
                            mostraProdotto(response.prodottoScansionato, response.prodottoId);
                            $('#barcodeInput').val('').focus();
                        } else {
                            mostraErrore('Prodotto non trovato per il codice: ' + barcode);
                            $('#barcodeInput').select();
                        }
                    },
                    error: function() {
                        mostraErrore('Errore durante la ricerca del prodotto');
                    }
                });
            }

            // Mostra i dati del prodotto
            function mostraProdotto(prodotto, prodottoId) {
                currentProductId = prodottoId;

                $('#productName').text(prodotto.nome || '');
                $('#productCode').text(prodotto.codice || '');
                
                if (prodotto.codiceEan) {
                    $('#productEan').text(prodotto.codiceEan);
                    $('#productEanDiv').show();
                } else {
                    $('#productEanDiv').hide();
                }

                if (prodotto.categoria) {
                    $('#productCategoria').text(prodotto.categoria);
                    $('#productCategoriaDiv').show();
                } else {
                    $('#productCategoriaDiv').hide();
                }

                if (prodotto.descrizione) {
                    $('#productDescrizione').text(prodotto.descrizione);
                    $('#productDescrizioneDiv').show();
                } else {
                    $('#productDescrizioneDiv').hide();
                }

                if (prodotto.prezzoBase) {
                    $('#productPrezzo').text('€ ' + parseFloat(prodotto.prezzoBase).toFixed(2));
                } else {
                    $('#productPrezzo').text('-');
                }

                $('#productCard').fadeIn();
                $('#scanResult').hide();

                // Abilita i pulsanti
                $('#btnCarico, #btnScarico, #btnStorico, #btnEtichetta').prop('disabled', false);
            }

            // Mostra errore
            function mostraErrore(messaggio) {
                $('#productCard').hide();
                $('#scanResult').html(
                    '<div class="alert alert-warning alert-dismissible fade show">' +
                    '<i class="bi bi-exclamation-triangle me-2"></i>' + messaggio +
                    '<button type="button" class="btn-close" data-bs-dismiss="alert"></button>' +
                    '</div>'
                ).fadeIn();
                
                currentProductId = null;
                $('#btnCarico, #btnScarico, #btnStorico, #btnEtichetta').prop('disabled', true);
            }

            // Pulsanti azioni
            $('#btnCarico').on('click', function() {
                if (currentProductId) {
                    window.location.href = '<s:url action="nuovoMovimento" namespace="/app/magazzino"/>' +
                        '?prodottoId=' + currentProductId + '&tipoMovimento=CARICO';
                }
            });

            $('#btnScarico').on('click', function() {
                if (currentProductId) {
                    window.location.href = '<s:url action="nuovoMovimento" namespace="/app/magazzino"/>' +
                        '?prodottoId=' + currentProductId + '&tipoMovimento=SCARICO';
                }
            });

            $('#btnStorico').on('click', function() {
                if (currentProductId) {
                    window.location.href = '<s:url action="storicoMovimenti" namespace="/app/magazzino"/>' +
                        '?prodottoId=' + currentProductId;
                }
            });

            $('#btnEtichetta').on('click', function() {
                if (currentProductId) {
                    window.open('<s:url action="stampaEtichettaSingola" namespace="/app/magazzino"/>' +
                        '?prodottoId=' + currentProductId, '_blank');
                }
            });

            // Riporta focus all'input quando si chiude l'alert
            $(document).on('closed.bs.alert', function() {
                $('#barcodeInput').focus();
            });
        });
    </script>
    </div>
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
