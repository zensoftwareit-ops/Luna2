<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="s" uri="/struts-tags" %>
<!DOCTYPE html>
<html lang="it">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Dettaglio Commessa - Luna2</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.10.0/font/bootstrap-icons.css">
    <style>
        .stato-badge {
            font-size: 1rem;
            padding: 0.5rem 1rem;
        }
        .stato-APERTA { background-color: #e3f2fd; color: #1565c0; }
        .stato-IN_LAVORAZIONE { background-color: #fff3e0; color: #e65100; }
        .stato-SOSPESA { background-color: #ffebee; color: #c62828; }
        .stato-COMPLETATA { background-color: #e8f5e9; color: #2e7d32; }
        .stato-CHIUSA { background-color: #f3e5f5; color: #6a1b9a; }
        .stato-ANNULLATA { background-color: #f5f5f5; color: #616161; }
        
        .timeline {
            position: relative;
            padding-left: 30px;
        }
        .timeline::before {
            content: '';
            position: absolute;
            left: 10px;
            top: 0;
            bottom: 0;
            width: 2px;
            background: #dee2e6;
        }
        .timeline-item {
            position: relative;
            padding-bottom: 20px;
        }
        .timeline-item::before {
            content: '';
            position: absolute;
            left: -25px;
            top: 5px;
            width: 12px;
            height: 12px;
            border-radius: 50%;
            background: #007bff;
            border: 2px solid #fff;
        }
    </style>
</head>
<body>
    <%@ include file="../includes/sidebar.jsp" %>

    <div class="col-md-10 content-wrapper p-4">
        <div class="container-fluid">
            <!-- Header -->
            <div class="d-flex justify-content-between align-items-center mb-4">
                <h1 class="h3">
                    <i class="bi bi-gear me-2"></i>Dettaglio Commessa <s:property value="commessa.numero"/>
                </h1>
                <a href="produzione-commesse" class="btn btn-secondary">
                    <i class="bi bi-arrow-left me-2"></i>Torna all'elenco
                </a>
            </div>

            <!-- Messages -->
            <s:if test="message != null">
                <div class="alert alert-success alert-dismissible fade show" role="alert">
                    <i class="bi bi-check-circle me-2"></i><s:property value="message"/>
                    <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
                </div>
            </s:if>
            <s:if test="errorMessage != null">
                <div class="alert alert-danger alert-dismissible fade show" role="alert">
                    <i class="bi bi-exclamation-triangle me-2"></i><s:property value="errorMessage"/>
                    <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
                </div>
            </s:if>

            <div class="row">
                <!-- Colonna sinistra: Info commessa -->
                <div class="col-md-8">
                    <!-- Dati principali -->
                    <div class="card mb-4">
                        <div class="card-header d-flex justify-content-between align-items-center">
                            <h5 class="mb-0">Informazioni Generali</h5>
                            <span class="badge stato-badge stato-<s:property value='commessa.stato.name()'/>">
                                <s:property value="commessa.stato.name().replace('_', ' ')"/>
                            </span>
                        </div>
                        <div class="card-body">
                            <div class="row mb-3">
                                <div class="col-md-6">
                                    <strong>Numero:</strong> <s:property value="commessa.numero"/>
                                </div>
                                <div class="col-md-6">
                                    <strong>Anno:</strong> <s:property value="commessa.anno"/>
                                </div>
                            </div>
                            <div class="row mb-3">
                                <div class="col-md-12">
                                    <strong>Descrizione:</strong><br/>
                                    <s:property value="commessa.descrizione"/>
                                </div>
                            </div>
                            <div class="row mb-3">
                                <div class="col-md-6">
                                    <strong>Cliente:</strong><br/>
                                    <s:if test="commessa.preventivo != null && commessa.preventivo.cliente != null">
                                        <s:property value="commessa.preventivo.cliente.ragioneSociale"/>
                                    </s:if>
                                    <s:else>N/A</s:else>
                                </div>
                                <div class="col-md-6">
                                    <strong>Preventivo:</strong><br/>
                                    <s:if test="commessa.preventivo != null">
                                        <a href="preventivi-view?id=<s:property value='commessa.preventivo.id'/>">
                                            <s:property value="commessa.preventivo.numero"/>
                                        </a>
                                    </s:if>
                                    <s:else>N/A</s:else>
                                </div>
                            </div>
                            <div class="row mb-3">
                                <div class="col-md-4">
                                    <strong>Data Apertura:</strong><br/>
                                    <s:date name="commessa.dataApertura" format="dd/MM/yyyy"/>
                                </div>
                                <div class="col-md-4">
                                    <strong>Data Prevista Fine:</strong><br/>
                                    <s:if test="commessa.dataPrevistFine != null">
                                        <s:date name="commessa.dataPrevistFine" format="dd/MM/yyyy"/>
                                    </s:if>
                                    <s:else>Non specificata</s:else>
                                </div>
                                <div class="col-md-4">
                                    <strong>Data Fine Effettiva:</strong><br/>
                                    <s:if test="commessa.dataFineEffettiva != null">
                                        <s:date name="commessa.dataFineEffettiva" format="dd/MM/yyyy"/>
                                    </s:if>
                                    <s:else>-</s:else>
                                </div>
                            </div>
                            
                            <!-- Percentuale completamento -->
                            <div class="mb-3">
                                <strong>Avanzamento Lavori:</strong>
                                <div class="progress" style="height: 30px;">
                                    <div class="progress-bar bg-success" role="progressbar" 
                                         style="width: <s:property value='commessa.percentualeCompletamento != null ? commessa.percentualeCompletamento : 0'/>%">
                                        <s:property value="commessa.percentualeCompletamento != null ? commessa.percentualeCompletamento : 0"/>%
                                    </div>
                                </div>
                                
                                <!-- Form aggiorna percentuale -->
                                <s:if test="commessa.stato.name() == 'IN_LAVORAZIONE'">
                                    <form action="produzione-aggiorna-percentuale" method="post" class="mt-2">
                                        <input type="hidden" name="id" value="<s:property value='commessa.id'/>"/>
                                        <div class="input-group">
                                            <input type="number" name="percentualeCompletamento" 
                                                   class="form-control" min="0" max="100" 
                                                   value="<s:property value='commessa.percentualeCompletamento' default='0'/>"
                                                   placeholder="Percentuale 0-100">
                                            <button type="submit" class="btn btn-sm btn-primary">Aggiorna</button>
                                        </div>
                                    </form>
                                </s:if>
                            </div>

                            <!-- Importi -->
                            <div class="row mb-3">
                                <div class="col-md-4">
                                    <strong>Imponibile:</strong><br/>
                                    &euro; <s:property value="getText('{0,number,#,##0.00}', {commessa.imponibile})"/>
                                </div>
                                <div class="col-md-4">
                                    <strong>IVA:</strong><br/>
                                    &euro; <s:property value="getText('{0,number,#,##0.00}', {commessa.iva})"/>
                                </div>
                                <div class="col-md-4">
                                    <strong>Totale:</strong><br/>
                                    <h4>&euro; <s:property value="getText('{0,number,#,##0.00}', {commessa.totale})"/></h4>
                                </div>
                            </div>

                            <!-- Note -->
                            <s:if test="commessa.note != null && commessa.note.length() > 0">
                                <div class="row">
                                    <div class="col-md-12">
                                        <strong>Note:</strong><br/>
                                        <div class="border p-2 rounded bg-light">
                                            <s:property value="commessa.note" escape="false"/>
                                        </div>
                                    </div>
                                </div>
                            </s:if>
                        </div>
                    </div>

                    <!-- Righe commessa -->
                    <div class="card">
                        <div class="card-header">
                            <h5 class="mb-0">Righe Commessa</h5>
                        </div>
                        <div class="card-body">
                            <s:if test="commessa.righe != null && !commessa.righe.isEmpty()">
                                <div class="table-responsive">
                                    <table class="table">
                                        <thead>
                                            <tr>
                                                <th>#</th>
                                                <th>Descrizione</th>
                                                <th>Q.tà</th>
                                                <th>Importo Unit.</th>
                                                <th>Totale</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            <s:iterator value="commessa.righe" var="riga">
                                                <tr>
                                                    <td><s:property value="#riga.numeroRiga"/></td>
                                                    <td><s:property value="#riga.descrizione"/></td>
                                                    <td><s:property value="#riga.quantita"/></td>
                                                    <td>&euro; <s:property value="getText('{0,number,#,##0.00}', {#riga.importoUnitario})"/></td>
                                                    <td>&euro; <s:property value="getText('{0,number,#,##0.00}', {#riga.importoTotale})"/></td>
                                                </tr>
                                            </s:iterator>
                                        </tbody>
                                    </table>
                                </div>
                            </s:if>
                            <s:else>
                                <p class="text-muted mb-0">Nessuna riga presente</p>
                            </s:else>
                        </div>
                    </div>
                </div>

                <!-- Colonna destra: Azioni e Timeline -->
                <div class="col-md-4">
                    <!-- Azioni -->
                    <div class="card mb-4">
                        <div class="card-header">
                            <h5 class="mb-0">Azioni</h5>
                        </div>
                        <div class="card-body">
                            <div class="d-grid gap-2">
                                <s:if test="commessa.stato.name() == 'APERTA'">
                                    <a href="produzione-avvia?id=<s:property value='commessa.id'/>" 
                                       class="btn btn-success">
                                        <i class="bi bi-play-fill me-2"></i>Avvia Lavorazione
                                    </a>
                                </s:if>
                                
                                <s:if test="commessa.stato.name() == 'IN_LAVORAZIONE'">
                                    <a href="produzione-sospendi?id=<s:property value='commessa.id'/>" 
                                       class="btn btn-warning">
                                        <i class="bi bi-pause-fill me-2"></i>Sospendi
                                    </a>
                                    <a href="produzione-completa?id=<s:property value='commessa.id'/>" 
                                       class="btn btn-success">
                                        <i class="bi bi-check-circle me-2"></i>Segna Completata
                                    </a>
                                </s:if>
                                
                                <s:if test="commessa.stato.name() == 'SOSPESA'">
                                    <a href="produzione-riprendi?id=<s:property value='commessa.id'/>" 
                                       class="btn btn-success">
                                        <i class="bi bi-arrow-clockwise me-2"></i>Riprendi Lavorazione
                                    </a>
                                </s:if>
                                
                                <s:if test="commessa.stato.name() == 'COMPLETATA'">
                                    <a href="produzione-fattura?id=<s:property value='commessa.id'/>" 
                                       class="btn btn-primary">
                                        <i class="bi bi-file-earmark-text me-2"></i>Crea Fattura
                                    </a>
                                </s:if>

                                <s:if test="commessa.stato.name() != 'CHIUSA' && commessa.stato.name() != 'ANNULLATA'">
                                    <a href="produzione-annulla?id=<s:property value='commessa.id'/>" 
                                       class="btn btn-danger" 
                                       onclick="return confirm('Sei sicuro di voler annullare questa commessa?')">
                                        <i class="bi bi-x-circle me-2"></i>Annulla Commessa
                                    </a>
                                </s:if>

                                <s:if test="commessa.fattura != null">
                                    <hr/>
                                    <a href="fatture-view?id=<s:property value='commessa.fattura.id'/>" 
                                       class="btn btn-outline-primary">
                                        <i class="bi bi-file-earmark-text me-2"></i>Vedi Fattura
                                    </a>
                                </s:if>
                            </div>
                        </div>
                    </div>

                    <!-- Timeline -->
                    <div class="card">
                        <div class="card-header">
                            <h5 class="mb-0">Timeline</h5>
                        </div>
                        <div class="card-body">
                            <div class="timeline">
                                <div class="timeline-item">
                                    <strong>Apertura</strong><br/>
                                    <small class="text-muted">
                                        <s:date name="commessa.dataApertura" format="dd/MM/yyyy HH:mm"/>
                                    </small>
                                </div>
                                
                                <s:if test="commessa.dataFineEffettiva != null">
                                    <div class="timeline-item">
                                        <strong>Completata</strong><br/>
                                        <small class="text-muted">
                                            <s:date name="commessa.dataFineEffettiva" format="dd/MM/yyyy HH:mm"/>
                                        </small>
                                    </div>
                                </s:if>
                                
                                <s:if test="commessa.dataChiusura != null">
                                    <div class="timeline-item">
                                        <strong>Chiusura</strong><br/>
                                        <small class="text-muted">
                                            <s:date name="commessa.dataChiusura" format="dd/MM/yyyy HH:mm"/>
                                        </small>
                                    </div>
                                </s:if>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
