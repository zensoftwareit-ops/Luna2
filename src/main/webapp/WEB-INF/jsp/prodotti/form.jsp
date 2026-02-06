<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="s" uri="/struts-tags" %>
<!DOCTYPE html>
<html lang="it">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><s:if test="prodotto.id == null">Nuovo</s:if><s:else>Modifica</s:else> Prodotto - Luna2</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.10.0/font/bootstrap-icons.css">
    <style>
        .tipo-prodotto-section { display: none; }
        .tipo-prodotto-section.active { display: block; }
    </style>
</head>
<body>
    <%@ include file="../includes/sidebar.jsp" %>

    <div class="col-md-10 content-wrapper p-4">
        <div class="container-fluid">
            <div class="d-flex justify-content-between align-items-center mb-4">
                <h1 class="h3">
                    <i class="bi bi-box-seam me-2"></i>
                    <s:if test="prodotto.id == null">Nuovo Prodotto</s:if>
                    <s:else>Modifica Prodotto</s:else>
                </h1>
                <a href="<s:url action='list' namespace='/app/prodotti'/>" class="btn btn-secondary">
                    <i class="bi bi-arrow-left me-2"></i>Torna alla lista
                </a>
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

            <div class="card">
                <div class="card-body">
                    <s:form action="save" namespace="/app/prodotti" method="post" theme="simple" cssClass="needs-validation">
                        <s:hidden name="prodotto.id"/>
                        
                        <!-- Informazioni Base -->
                        <h5 class="border-bottom pb-2 mb-3">Informazioni Base</h5>
                        <div class="row mb-3">
                            <div class="col-md-3">
                                <label class="form-label">Codice <span class="text-danger">*</span></label>
                                <s:textfield name="prodotto.codice" cssClass="form-control" required="true"/>
                            </div>
                            <div class="col-md-9">
                                <label class="form-label">Nome <span class="text-danger">*</span></label>
                                <s:textfield name="prodotto.nome" cssClass="form-control" required="true"/>
                            </div>
                        </div>

                        <div class="row mb-3">
                            <div class="col-md-12">
                                <label class="form-label">Descrizione</label>
                                <s:textarea name="prodotto.descrizione" cssClass="form-control" rows="3"/>
                            </div>
                        </div>

                        <div class="row mb-3">
                            <div class="col-md-4">
                                <label class="form-label">Categoria</label>
                                <s:textfield name="prodotto.categoria" cssClass="form-control" placeholder="es. Elettronica, Abbigliamento..."/>
                            </div>
                            <div class="col-md-4">
                                <label class="form-label">Tipo Prodotto <span class="text-danger">*</span></label>
                                <s:select name="prodotto.tipoProdotto" 
                                         list="tipiProdotto" 
                                         cssClass="form-select"
                                         id="tipoProdottoSelect"
                                         required="true"/>
                            </div>
                            <div class="col-md-4">
                                <label class="form-label">Unità di Misura <span class="text-danger">*</span></label>
                                <s:select name="prodotto.unitaMisura" 
                                         list="unitaMisura" 
                                         cssClass="form-select"
                                         required="true"/>
                            </div>
                        </div>

                        <!-- Prezzi e Costi -->
                        <h5 class="border-bottom pb-2 mb-3 mt-4">Prezzi e Costi</h5>
                        <div class="row mb-3">
                            <div class="col-md-3">
                                <label class="form-label">Prezzo Base <span class="text-danger">*</span></label>
                                <div class="input-group">
                                    <span class="input-group-text">€</span>
                                    <s:textfield name="prodotto.prezzoBase" cssClass="form-control" required="true" type="number" step="0.01"/>
                                </div>
                            </div>
                            <div class="col-md-3">
                                <label class="form-label">Costo Acquisto</label>
                                <div class="input-group">
                                    <span class="input-group-text">€</span>
                                    <s:textfield name="prodotto.costoAcquisto" cssClass="form-control" type="number" step="0.01"/>
                                </div>
                            </div>
                            <div class="col-md-3">
                                <label class="form-label">IVA %</label>
                                <div class="input-group">
                                    <s:textfield name="prodotto.ivaPercentuale" cssClass="form-control" type="number" step="0.01"/>
                                    <span class="input-group-text">%</span>
                                </div>
                            </div>
                            <div class="col-md-3">
                                <label class="form-label">Sconto Massimo %</label>
                                <div class="input-group">
                                    <s:textfield name="prodotto.scontoMassimo" cssClass="form-control" type="number" step="0.01"/>
                                    <span class="input-group-text">%</span>
                                </div>
                            </div>
                        </div>

                        <!-- Sezione Prodotto A Misura -->
                        <div id="sezioneMisura" class="tipo-prodotto-section">
                            <h5 class="border-bottom pb-2 mb-3 mt-4">Informazioni Prodotto a Misura</h5>
                            <div class="alert alert-info">
                                <i class="bi bi-info-circle me-2"></i>
                                Per prodotti a misura, il prezzo base rappresenta il costo per unità di misura selezionata (kg, metro, mq, ecc.)
                            </div>
                        </div>

                        <!-- Sezione Prodotto Composto -->
                        <div id="sezioneComposto" class="tipo-prodotto-section">
                            <h5 class="border-bottom pb-2 mb-3 mt-4">Componenti del Prodotto</h5>
                            <div class="alert alert-info">
                                <i class="bi bi-info-circle me-2"></i>
                                I componenti possono essere aggiunti dopo aver salvato il prodotto principale
                            </div>
                        </div>

                        <!-- Sezione Prodotto Variabile -->
                        <div id="sezioneVariabile" class="tipo-prodotto-section">
                            <h5 class="border-bottom pb-2 mb-3 mt-4">Varianti del Prodotto</h5>
                            <div class="alert alert-info">
                                <i class="bi bi-info-circle me-2"></i>
                                Le varianti (taglie, colori, ecc.) possono essere aggiunte dopo aver salvato il prodotto principale
                            </div>
                        </div>

                        <!-- Altre Informazioni -->
                        <h5 class="border-bottom pb-2 mb-3 mt-4">Altre Informazioni</h5>
                        <div class="row mb-3">
                            <div class="col-md-3">
                                <label class="form-label">Peso (kg)</label>
                                <s:textfield name="prodotto.peso" cssClass="form-control" type="number" step="0.001"/>
                            </div>
                            <div class="col-md-3">
                                <label class="form-label">Volume (mc)</label>
                                <s:textfield name="prodotto.volume" cssClass="form-control" type="number" step="0.001"/>
                            </div>
                            <div class="col-md-3">
                                <label class="form-label">Codice EAN</label>
                                <s:textfield name="prodotto.codiceEan" cssClass="form-control"/>
                            </div>
                            <div class="col-md-3">
                                <label class="form-label">Giacenza Minima</label>
                                <s:textfield name="prodotto.giacenzaMinima" cssClass="form-control" type="number" step="0.01"/>
                            </div>
                        </div>

                        <div class="row mb-3">
                            <div class="col-md-12">
                                <label class="form-label">Note</label>
                                <s:textarea name="prodotto.note" cssClass="form-control" rows="2"/>
                            </div>
                        </div>

                        <div class="row mb-3">
                            <div class="col-md-3">
                                <div class="form-check form-switch">
                                    <s:checkbox name="prodotto.gestioneMagazzino" cssClass="form-check-input" id="gestioneMagazzino"/>
                                    <label class="form-check-label" for="gestioneMagazzino">Gestione Magazzino</label>
                                </div>
                            </div>
                            <div class="col-md-3">
                                <div class="form-check form-switch">
                                    <s:checkbox name="prodotto.attivo" cssClass="form-check-input" id="attivo"/>
                                    <label class="form-check-label" for="attivo">Attivo</label>
                                </div>
                            </div>
                        </div>

                        <div class="mt-4">
                            <button type="submit" class="btn btn-primary">
                                <i class="bi bi-check-circle me-2"></i>Salva Prodotto
                            </button>
                            <a href="<s:url action='list' namespace='/app/prodotti'/>" class="btn btn-secondary">
                                <i class="bi bi-x-circle me-2"></i>Annulla
                            </a>
                        </div>
                    </s:form>
                </div>
            </div>
        </div>
    </div>

    </div>
</div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
    <script>
        document.getElementById('tipoProdottoSelect').addEventListener('change', function() {
            // Nascondi tutte le sezioni
            document.querySelectorAll('.tipo-prodotto-section').forEach(el => el.classList.remove('active'));
            
            // Mostra la sezione appropriata
            const tipo = this.value;
            if (tipo === 'A_MISURA') {
                document.getElementById('sezioneMisura').classList.add('active');
            } else if (tipo === 'COMPOSTO') {
                document.getElementById('sezioneComposto').classList.add('active');
            } else if (tipo === 'VARIABILE') {
                document.getElementById('sezioneVariabile').classList.add('active');
            }
        });
        
        // Trigger al caricamento della pagina
        document.getElementById('tipoProdottoSelect').dispatchEvent(new Event('change'));
    </script>
</body>
</html>
