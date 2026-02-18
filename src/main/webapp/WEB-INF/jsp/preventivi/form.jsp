<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="s" uri="/struts-tags" %>
<!DOCTYPE html>
<html lang="it">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><s:if test="preventivo.id != null">Modifica</s:if><s:else>Nuovo</s:else> Preventivo - Luna2</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.10.0/font/bootstrap-icons.css">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/select2@4.1.0-rc.0/dist/css/select2.min.css" />
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/select2-bootstrap-5-theme@1.3.0/dist/select2-bootstrap-5-theme.min.css" />
    <style>
        .riga-row:hover {
            background-color: #f8f9fa;
        }
    </style>
</head>
<body>
    <%@ include file="../includes/sidebar.jsp" %>

    <div class="col-md-10 content-wrapper p-4">
        <div class="container-fluid">
            <div class="d-flex justify-content-between align-items-center mb-4">
                <h1 class="h3">
                    <i class="bi bi-file-earmark-text me-2"></i>
                    <s:if test="preventivo.id != null">
                        Modifica Preventivo <code><s:property value="preventivo.numero"/></code>
                    </s:if>
                    <s:else>Nuovo Preventivo</s:else>
                </h1>
                <div>
                    <s:if test="preventivo.id != null">
                        <a href="<s:url action='preventivi-pdf' namespace='/app/documenti'><s:param name='id' value='preventivo.id'/></s:url>" 
                           class="btn btn-success me-2" target="_blank" title="Scarica PDF">
                            <i class="bi bi-file-pdf me-2"></i>Scarica PDF
                        </a>
                    </s:if>
                    <a href="<s:url action='preventivi' namespace='/app/documenti'/>" class="btn btn-secondary">
                        <i class="bi bi-arrow-left me-2"></i>Torna alla Lista
                    </a>
                </div>
            </div>

            <s:if test="hasActionErrors()">
                <div class="alert alert-danger alert-dismissible fade show">
                    <s:actionerror/>
                    <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
                </div>
            </s:if>

            <form action="<s:url action='preventivi-save' namespace='/app/documenti'/>" method="post" id="preventivoForm">
                <s:hidden name="preventivo.id"/>
                <s:hidden name="preventivo.numero"/>
                <s:hidden name="preventivo.anno"/>

                <!-- Dati Generali -->
                <div class="card mb-3">
                    <div class="card-header">
                        <h5 class="mb-0"><i class="bi bi-info-circle me-2"></i>Dati Generali</h5>
                    </div>
                    <div class="card-body">
                        <div class="row">
                            <div class="col-md-3">
                                <div class="mb-3">
                                    <label class="form-label">Numero</label>
                                    <input type="text" class="form-control" value="<s:property value='preventivo.numero'/>" readonly>
                                </div>
                            </div>
                            <div class="col-md-3">
                                <div class="mb-3">
                                    <label class="form-label">Data *</label>
                                    <s:textfield name="preventivo.dataPreventivo" cssClass="form-control" type="date" required="true"/>
                                </div>
                            </div>
                            <div class="col-md-3">
                                <div class="mb-3">
                                    <label class="form-label">Validità (giorni)</label>
                                    <s:textfield name="preventivo.validitaGiorni" cssClass="form-control" type="number" value="30"/>
                                </div>
                            </div>
                            <div class="col-md-3">
                                <div class="mb-3">
                                    <label class="form-label">Stato</label>
                                    <s:select name="preventivo.stato" cssClass="form-select" list="@it.zensoftware.luna2.model.Preventivo$Stato@values()" listKey="name()" listValue="name()"/>
                                </div>
                            </div>
                        </div>
                        <div class="row">
                            <div class="col-md-6">
                                <div class="mb-3">
                                    <label class="form-label">Cliente *</label>
                                    <s:select name="preventivo.cliente.id" cssClass="form-select select2-cliente" 
                                              list="clienti" listKey="id" listValue="ragioneSociale" 
                                              headerKey="" headerValue="-- Seleziona Cliente --" required="true"/>
                                </div>
                            </div>
                            <div class="col-md-6">
                                <div class="mb-3">
                                    <label class="form-label">Oggetto *</label>
                                    <s:textfield name="preventivo.oggetto" cssClass="form-control" required="true" placeholder="Oggetto del preventivo"/>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- Righe Preventivo -->
                <div class="card mb-3">
                    <div class="card-header d-flex justify-content-between align-items-center">
                        <h5 class="mb-0"><i class="bi bi-list-ul me-2"></i>Righe Preventivo</h5>
                        <button type="button" class="btn btn-sm btn-success" id="addRigaBtn">
                            <i class="bi bi-plus-circle"></i> Aggiungi Riga
                        </button>
                    </div>
                    <div class="card-body">
                        <div class="table-responsive">
                            <table class="table table-sm table-hover" id="righeTable">
                                <thead>
                                    <tr>
                                        <th style="width: 5%">#</th>
                                        <th style="width: 30%">Descrizione</th>
                                        <th style="width: 10%">Q.tà</th>
                                        <th style="width: 10%">Prezzo Unit.</th>
                                        <th style="width: 10%">Sconto %</th>
                                        <th style="width: 10%">IVA %</th>
                                        <th style="width: 15%">Totale</th>
                                        <th style="width: 10%">Azioni</th>
                                    </tr>
                                </thead>
                                <tbody id="righeBody">
                                    <s:if test="righe != null && !righe.isEmpty()">
                                        <s:iterator value="righe" var="r" status="st">
                                            <tr class="riga-row" data-riga-id="<s:property value='#r.id'/>">
                                                <td><s:property value="#r.rigaNumero"/></td>
                                                <td><s:property value="#r.descrizione"/></td>
                                                <td class="text-end"><s:property value="#r.quantita"/></td>
                                                <td class="text-end">€ <s:text name="format.number"><s:param value="#r.prezzoUnitario"/></s:text></td>
                                                <td class="text-end"><s:property value="#r.scontoPercentuale"/>%</td>
                                                <td class="text-end"><s:property value="#r.ivaPercentuale"/>%</td>
                                                <td class="text-end">
                                                    <strong>€ <s:text name="format.number"><s:param value="#r.totaleRiga"/></s:text></strong>
                                                </td>
                                                <td>
                                                    <button type="button" class="btn btn-sm btn-outline-primary edit-riga-btn">
                                                        <i class="bi bi-pencil"></i>
                                                    </button>
                                                    <button type="button" class="btn btn-sm btn-outline-danger delete-riga-btn">
                                                        <i class="bi bi-trash"></i>
                                                    </button>
                                                </td>
                                            </tr>
                                        </s:iterator>
                                    </s:if>
                                    <s:else>
                                        <tr id="noRigheRow">
                                            <td colspan="8" class="text-center text-muted py-4">
                                                <i class="bi bi-inbox fs-3 d-block mb-2"></i>
                                                Nessuna riga inserita. Clicca su "Aggiungi Riga" per iniziare.
                                            </td>
                                        </tr>
                                    </s:else>
                                </tbody>
                                <tfoot>
                                    <tr class="table-light">
                                        <td colspan="6" class="text-end"><strong>Subtotale:</strong></td>
                                        <td class="text-end"><strong id="subtotale">€ 0,00</strong></td>
                                        <td></td>
                                    </tr>
                                    <tr class="table-light">
                                        <td colspan="6" class="text-end">
                                            <strong>Sconto:</strong>
                                            <input type="number" name="preventivo.scontoPercentuale" id="scontoPercentuale" 
                                                   value="<s:property value='preventivo.scontoPercentuale'/>" 
                                                   class="form-control form-control-sm d-inline-block" style="width: 80px;">%
                                        </td>
                                        <td class="text-end"><strong id="scontoImporto">€ 0,00</strong></td>
                                        <td></td>
                                    </tr>
                                    <tr class="table-light">
                                        <td colspan="6" class="text-end">
                                            <strong>Spese Trasporto:</strong>
                                        </td>
                                        <td class="text-end">
                                            <input type="number" name="preventivo.speseTrasporto" id="speseTrasporto" 
                                                   value="<s:property value='preventivo.speseTrasporto'/>" 
                                                   class="form-control form-control-sm" step="0.01">
                                        </td>
                                        <td></td>
                                    </tr>
                                    <tr class="table-light">
                                        <td colspan="6" class="text-end"><strong>IVA:</strong></td>
                                        <td class="text-end"><strong id="ivaTotal">€ 0,00</strong></td>
                                        <td></td>
                                    </tr>
                                    <tr class="table-light fw-bold">
                                        <td colspan="6" class="text-end"><strong>TOTALE:</strong></td>
                                        <td class="text-end fs-5"><strong id="totaleGeneral">€ 0,00</strong></td>
                                        <td></td>
                                    </tr>
                                </tfoot>
                            </table>
                        </div>
                    </div>
                </div>

                <!-- Note e Condizioni -->
                <div class="card mb-3">
                    <div class="card-header">
                        <h5 class="mb-0"><i class="bi bi-card-text me-2"></i>Note e Condizioni</h5>
                    </div>
                    <div class="card-body">
                        <div class="row">
                            <div class="col-md-6">
                                <div class="mb-3">
                                    <label class="form-label">Note Intestazione</label>
                                    <s:textarea name="preventivo.noteIntestazione" cssClass="form-control" rows="3"/>
                                </div>
                                <div class="mb-3">
                                    <label class="form-label">Condizioni di Pagamento</label>
                                    <s:textfield name="preventivo.condizioniPagamento" cssClass="form-control" placeholder="es. 30 giorni data fattura"/>
                                </div>
                            </div>
                            <div class="col-md-6">
                                <div class="mb-3">
                                    <label class="form-label">Note Piè di Pagina</label>
                                    <s:textarea name="preventivo.notePiede" cssClass="form-control" rows="3"/>
                                </div>
                                <div class="mb-3">
                                    <label class="form-label">Tempi di Consegna</label>
                                    <s:textfield name="preventivo.tempiConsegna" cssClass="form-control" placeholder="es. 15 giorni lavorativi"/>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- Actions -->
                <div class="d-flex justify-content-end gap-2 mb-4">
                    <a href="<s:url action='preventivi' namespace='/app/documenti'/>" class="btn btn-secondary">
                        <i class="bi bi-x-circle me-2"></i>Annulla
                    </a>
                    <button type="submit" class="btn btn-primary">
                        <i class="bi bi-save me-2"></i>Salva Preventivo
                    </button>
                </div>
            </form>
        </div>
    </div>

    </div>
</div>

    <!-- Modal Add/Edit Riga -->
    <div class="modal fade" id="rigaModal" tabindex="-1">
        <div class="modal-dialog modal-lg">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title" id="rigaModalTitle">Aggiungi Riga</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                </div>
                <div class="modal-body">
                    <form id="rigaForm">
                        <input type="hidden" id="rigaId">
                        <div class="row">
                            <div class="col-md-12">
                                <div class="mb-3">
                                    <label class="form-label">Prodotto (opzionale)</label>
                                    <select id="rigaProdotto" class="form-select select2-prodotto">
                                        <option value="">-- Descrizione manuale --</option>
                                        <s:iterator value="prodotti" var="prod">
                                            <option value="<s:property value='#prod.id'/>" 
                                                    data-prezzo="<s:property value='#prod.prezzoBase'/>"
                                                    data-um="<s:property value='#prod.unitaMisura'/>">
                                                <s:property value="#prod.codice"/> - <s:property value="#prod.nome"/>
                                            </option>
                                        </s:iterator>
                                    </select>
                                </div>
                            </div>
                        </div>
                        <div class="row">
                            <div class="col-md-12">
                                <div class="mb-3">
                                    <label class="form-label">Descrizione *</label>
                                    <textarea id="rigaDescrizione" class="form-control" rows="2" required></textarea>
                                </div>
                            </div>
                        </div>
                        <div class="row">
                            <div class="col-md-3">
                                <div class="mb-3">
                                    <label class="form-label">Quantità *</label>
                                    <input type="number" id="rigaQuantita" class="form-control" step="0.01" value="1" required>
                                </div>
                            </div>
                            <div class="col-md-3">
                                <div class="mb-3">
                                    <label class="form-label">Unità Misura</label>
                                    <input type="text" id="rigaUnitaMisura" class="form-control" value="PEZZO">
                                </div>
                            </div>
                            <div class="col-md-3">
                                <div class="mb-3">
                                    <label class="form-label">Prezzo Unit. *</label>
                                    <input type="number" id="rigaPrezzoUnitario" class="form-control" step="0.01" required>
                                </div>
                            </div>
                            <div class="col-md-3">
                                <div class="mb-3">
                                    <label class="form-label">Sconto %</label>
                                    <input type="number" id="rigaScontoPercentuale" class="form-control" step="0.01" value="0">
                                </div>
                            </div>
                        </div>
                        <div class="row">
                            <div class="col-md-3">
                                <div class="mb-3">
                                    <label class="form-label">IVA %</label>
                                    <input type="number" id="rigaIvaPercentuale" class="form-control" step="0.01" value="22">
                                </div>
                            </div>
                            <div class="col-md-9">
                                <div class="mb-3">
                                    <label class="form-label">Note</label>
                                    <input type="text" id="rigaNote" class="form-control">
                                </div>
                            </div>
                        </div>
                    </form>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Annulla</button>
                    <button type="button" class="btn btn-primary" id="saveRigaBtn">Salva Riga</button>
                </div>
            </div>
        </div>
    </div>

    <script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/select2@4.1.0-rc.0/dist/js/select2.min.js"></script>
    <script>
        // Initialize Select2
        $(document).ready(function() {
            $('.select2-cliente').select2({
                theme: 'bootstrap-5',
                placeholder: '-- Seleziona Cliente --'
            });
            
            $('.select2-prodotto').select2({
                theme: 'bootstrap-5',
                placeholder: '-- Descrizione manuale --',
                dropdownParent: $('#rigaModal')
            });
        });

        // Righe management
        let righe = [];
        let rigaCounter = <s:if test="righe != null && !righe.isEmpty()"><s:property value="righe.size()"/></s:if><s:else>0</s:else>;

        // Load existing righe
        <s:if test="righe != null && !righe.isEmpty()">
            <s:iterator value="righe" var="r">
                righe.push({
                    id: <s:property value="#r.id"/>,
                    rigaNumero: <s:property value="#r.rigaNumero"/>,
                    descrizione: '<s:property value="#r.descrizione" escapeHtml="true"/>',
                    quantita: <s:property value="#r.quantita"/>,
                    unitaMisura: '<s:property value="#r.unitaMisura"/>',
                    prezzoUnitario: <s:property value="#r.prezzoUnitario"/>,
                    scontoPercentuale: <s:property value="#r.scontoPercentuale"/>,
                    ivaPercentuale: <s:property value="#r.ivaPercentuale"/>,
                    note: '<s:property value="#r.note" escapeHtml="true"/>'
                });
            </s:iterator>
        </s:if>

        // Add Riga
        $('#addRigaBtn').click(function() {
            $('#rigaModalTitle').text('Aggiungi Riga');
            $('#rigaForm')[0].reset();
            $('#rigaId').val('');
            $('#rigaQuantita').val(1);
            $('#rigaIvaPercentuale').val(22);
            $('#rigaScontoPercentuale').val(0);
            $('#rigaProdotto').val('').trigger('change');
            $('#rigaModal').modal('show');
        });

        // When prodotto is selected, fill fields
        $('#rigaProdotto').change(function() {
            const selected = $(this).find(':selected');
            if (selected.val()) {
                const prodottoNome = selected.text().split(' - ')[1];
                const prezzo = selected.data('prezzo');
                const um = selected.data('um');
                
                $('#rigaDescrizione').val(prodottoNome);
                $('#rigaPrezzoUnitario').val(prezzo || 0);
                $('#rigaUnitaMisura').val(um || 'PEZZO');
            }
        });

        // Save Riga
        $('#saveRigaBtn').click(function() {
            const rigaId = $('#rigaId').val();
            const riga = {
                id: rigaId || null,
                rigaNumero: rigaId ? righe.find(r => r.id == rigaId).rigaNumero : (++rigaCounter),
                descrizione: $('#rigaDescrizione').val(),
                quantita: parseFloat($('#rigaQuantita').val()) || 0,
                unitaMisura: $('#rigaUnitaMisura').val(),
                prezzoUnitario: parseFloat($('#rigaPrezzoUnitario').val()) || 0,
                scontoPercentuale: parseFloat($('#rigaScontoPercentuale').val()) || 0,
                ivaPercentuale: parseFloat($('#rigaIvaPercentuale').val()) || 22,
                note: $('#rigaNote').val()
            };

            if (!riga.descrizione) {
                alert('Inserisci la descrizione');
                return;
            }

            if (rigaId) {
                // Update existing
                const index = righe.findIndex(r => r.id == rigaId);
                righe[index] = riga;
            } else {
                // Add new
                riga.id = 'new_' + rigaCounter;
                righe.push(riga);
            }

            renderRighe();
            ricalcolaTotaliCliente();
            $('#rigaModal').modal('hide');
        });

        // Edit Riga
        $(document).on('click', '.edit-riga-btn', function() {
            const row = $(this).closest('tr');
            const rigaId = row.data('riga-id');
            const riga = righe.find(r => r.id == rigaId);

            if (riga) {
                $('#rigaModalTitle').text('Modifica Riga');
                $('#rigaId').val(riga.id);
                $('#rigaDescrizione').val(riga.descrizione);
                $('#rigaQuantita').val(riga.quantita);
                $('#rigaUnitaMisura').val(riga.unitaMisura);
                $('#rigaPrezzoUnitario').val(riga.prezzoUnitario);
                $('#rigaScontoPercentuale').val(riga.scontoPercentuale);
                $('#rigaIvaPercentuale').val(riga.ivaPercentuale);
                $('#rigaNote').val(riga.note || '');
                $('#rigaModal').modal('show');
            }
        });

        // Delete Riga
        $(document).on('click', '.delete-riga-btn', function() {
            if (confirm('Eliminare questa riga?')) {
                const row = $(this).closest('tr');
                const rigaId = row.data('riga-id');
                righe = righe.filter(r => r.id != rigaId);
                renderRighe();
                ricalcolaTotaliCliente();
            }
        });

        // Render Righe Table
        function renderRighe() {
            const tbody = $('#righeBody');
            tbody.empty();

            if (righe.length === 0) {
                tbody.append(`
                    <tr id="noRigheRow">
                        <td colspan="8" class="text-center text-muted py-4">
                            <i class="bi bi-inbox fs-3 d-block mb-2"></i>
                            Nessuna riga inserita. Clicca su "Aggiungi Riga" per iniziare.
                        </td>
                    </tr>
                `);
                return;
            }

            righe.forEach(riga => {
                const imponibileRiga = riga.quantita * riga.prezzoUnitario;
                const scontoImportoRiga = imponibileRiga * (riga.scontoPercentuale / 100);
                const imponibileScontato = imponibileRiga - scontoImportoRiga;
                const ivaImportoRiga = imponibileScontato * (riga.ivaPercentuale / 100);
                const totaleRiga = imponibileScontato + ivaImportoRiga;

                tbody.append(`
                    <tr class="riga-row" data-riga-id="${riga.id}">
                        <td>${riga.rigaNumero}</td>
                        <td>${riga.descrizione}</td>
                        <td class="text-end">${riga.quantita.toFixed(2)}</td>
                        <td class="text-end">€ ${riga.prezzoUnitario.toFixed(2)}</td>
                        <td class="text-end">${riga.scontoPercentuale.toFixed(2)}%</td>
                        <td class="text-end">${riga.ivaPercentuale.toFixed(2)}%</td>
                        <td class="text-end"><strong>€ ${totaleRiga.toFixed(2)}</strong></td>
                        <td>
                            <button type="button" class="btn btn-sm btn-outline-primary edit-riga-btn">
                                <i class="bi bi-pencil"></i>
                            </button>
                            <button type="button" class="btn btn-sm btn-outline-danger delete-riga-btn">
                                <i class="bi bi-trash"></i>
                            </button>
                        </td>
                    </tr>
                `);
            });
        }

        // Ricalcola Totali
        function ricalcolaTotaliCliente() {
            let subtotale = 0;
            let ivaTotal = 0;

            righe.forEach(riga => {
                const imponibileRiga = riga.quantita * riga.prezzoUnitario;
                const scontoImportoRiga = imponibileRiga * (riga.scontoPercentuale / 100);
                const imponibileScontato = imponibileRiga - scontoImportoRiga;
                const ivaImportoRiga = imponibileScontato * (riga.ivaPercentuale / 100);
                
                subtotale += imponibileScontato;
                ivaTotal += ivaImportoRiga;
            });

            // Sconto globale
            const scontoPerc = parseFloat($('#scontoPercentuale').val()) || 0;
            const scontoImporto = subtotale * (scontoPerc / 100);
            subtotale -= scontoImporto;

            // Spese trasporto
            const speseTrasporto = parseFloat($('#speseTrasporto').val()) || 0;
            subtotale += speseTrasporto;

            const totale = subtotale + ivaTotal;

            $('#subtotale').text('€ ' + subtotale.toFixed(2));
            $('#scontoImporto').text('€ ' + scontoImporto.toFixed(2));
            $('#ivaTotal').text('€ ' + ivaTotal.toFixed(2));
            $('#totaleGeneral').text('€ ' + totale.toFixed(2));
        }

        // Ricalcola totali quando cambiano sconto o spese
        $('#scontoPercentuale, #speseTrasporto').on('input', ricalcolaTotaliCliente);

        // Initial calculation
        $(document).ready(function() {
            ricalcolaTotaliCliente();
        });

        // Before submit, add righe as hidden fields
        $('#preventivoForm').submit(function(e) {
            // Remove old righe fields
            $('input[name^="righe["]').remove();

            // Add current righe
            righe.forEach((riga, index) => {
                Object.keys(riga).forEach(key => {
                    $(this).append(`<input type="hidden" name="righe[${index}].${key}" value="${riga[key]}">`);
                });
            });
        });
    </script>
</body>
</html>
