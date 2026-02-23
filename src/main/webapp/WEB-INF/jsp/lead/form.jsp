<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="s" uri="/struts-tags" %>
<!DOCTYPE html>
<html lang="it">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><s:if test="lead.id != null">Modifica</s:if><s:else>Nuovo</s:else> Lead - Luna2</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.10.0/font/bootstrap-icons.css">
</head>
<body>
    <%@ include file="../includes/sidebar.jsp" %>

    <div class="col-md-10 content-wrapper p-4">
        <div class="container-fluid">
            <!-- Header -->
            <div class="d-flex justify-content-between align-items-center mb-4">
                <div>
                    <nav aria-label="breadcrumb">
                        <ol class="breadcrumb mb-2">
                            <li class="breadcrumb-item"><a href="<s:url action='list' namespace='/app/lead'/>">Lead</a></li>
                            <li class="breadcrumb-item active">
                                <s:if test="lead.id != null">Modifica</s:if><s:else>Nuovo</s:else>
                            </li>
                        </ol>
                    </nav>
                    <h1 class="h3 mb-0">
                        <i class="bi bi-graph-up-arrow me-2"></i>
                        <s:if test="lead.id != null">Modifica Lead</s:if><s:else>Nuovo Lead</s:else>
                    </h1>
                </div>
            </div>

            <!-- Messages -->
            <s:if test="hasActionErrors()">
                <div class="alert alert-danger alert-dismissible fade show" role="alert">
                    <i class="bi bi-exclamation-triangle-fill me-2"></i>
                    <s:actionerror/>
                    <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
                </div>
            </s:if>
            <s:if test="hasActionMessages()">
                <div class="alert alert-success alert-dismissible fade show" role="alert">
                    <i class="bi bi-check-circle-fill me-2"></i>
                    <s:actionmessage/>
                    <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
                </div>
            </s:if>

            <!-- Form -->
            <form action="<s:url action='save' namespace='/app/lead'/>" method="post">
                <!-- CSRF Protection Token -->
                <s:token/>
                
                <s:if test="lead.id != null">
                    <input type="hidden" name="lead.id" value="<s:property value='lead.id'/>">
                </s:if>

                <div class="row">
                    <!-- Colonna Sinistra -->
                    <div class="col-md-8">
                        <!-- Informazioni Azienda -->
                        <div class="card mb-3">
                            <div class="card-header bg-primary text-white">
                                <h5 class="mb-0"><i class="bi bi-building me-2"></i>Informazioni Azienda</h5>
                            </div>
                            <div class="card-body">
                                <div class="row">
                                    <div class="col-md-6 mb-3">
                                        <label class="form-label">Azienda <span class="text-danger">*</span></label>
                                        <input type="text" name="lead.azienda" class="form-control" 
                                               value="<s:property value='lead.azienda'/>" required>
                                    </div>
                                    <div class="col-md-6 mb-3">
                                        <label class="form-label">Origine <span class="text-danger">*</span></label>
                                        <select name="lead.origine" class="form-select" required>
                                            <option value="">Seleziona...</option>
                                            <option value="FIERA" <s:if test="lead.origine == @it.zensoftware.luna2.model.Lead$Origine@FIERA">selected</s:if>>Fiera</option>
                                            <option value="CAMPAGNA" <s:if test="lead.origine == @it.zensoftware.luna2.model.Lead$Origine@CAMPAGNA">selected</s:if>>Campagna Marketing</option>
                                            <option value="PASSAPAROLA" <s:if test="lead.origine == @it.zensoftware.luna2.model.Lead$Origine@PASSAPAROLA">selected</s:if>>Passaparola</option>
                                            <option value="WEBSITE" <s:if test="lead.origine == @it.zensoftware.luna2.model.Lead$Origine@WEBSITE">selected</s:if>>Sito Web</option>
                                            <option value="EMAIL" <s:if test="lead.origine == @it.zensoftware.luna2.model.Lead$Origine@EMAIL">selected</s:if>>Email</option>
                                            <option value="TELEFONO" <s:if test="lead.origine == @it.zensoftware.luna2.model.Lead$Origine@TELEFONO">selected</s:if>>Telefono</option>
                                            <option value="ALTRO" <s:if test="lead.origine == @it.zensoftware.luna2.model.Lead$Origine@ALTRO">selected</s:if>>Altro</option>
                                        </select>
                                    </div>
                                </div>
                            </div>
                        </div>

                        <!-- Informazioni Contatto -->
                        <div class="card mb-3">
                            <div class="card-header bg-info text-white">
                                <h5 class="mb-0"><i class="bi bi-person me-2"></i>Informazioni Contatto</h5>
                            </div>
                            <div class="card-body">
                                <div class="row">
                                    <div class="col-md-6 mb-3">
                                        <label class="form-label">Nome</label>
                                        <input type="text" name="lead.nomeContatto" class="form-control" 
                                               value="<s:property value='lead.nomeContatto'/>">
                                    </div>
                                    <div class="col-md-6 mb-3">
                                        <label class="form-label">Cognome</label>
                                        <input type="text" name="lead.cognomeContatto" class="form-control" 
                                               value="<s:property value='lead.cognomeContatto'/>">
                                    </div>
                                    <div class="col-md-6 mb-3">
                                        <label class="form-label">Telefono</label>
                                        <div class="input-group">
                                            <span class="input-group-text"><i class="bi bi-telephone"></i></span>
                                            <input type="text" name="lead.telefono" class="form-control" 
                                                   value="<s:property value='lead.telefono'/>" placeholder="+39 333 1234567">
                                        </div>
                                    </div>
                                    <div class="col-md-6 mb-3">
                                        <label class="form-label">Email</label>
                                        <div class="input-group">
                                            <span class="input-group-text"><i class="bi bi-envelope"></i></span>
                                            <input type="email" name="lead.email" class="form-control" 
                                                   value="<s:property value='lead.email'/>" placeholder="contatto@azienda.it">
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>

                        <!-- Indirizzo -->
                        <div class="card mb-3">
                            <div class="card-header bg-secondary text-white">
                                <h5 class="mb-0"><i class="bi bi-geo-alt me-2"></i>Indirizzo</h5>
                            </div>
                            <div class="card-body">
                                <div class="row">
                                    <div class="col-md-12 mb-3">
                                        <label class="form-label">Via/Piazza</label>
                                        <input type="text" name="lead.indirizzo" class="form-control" 
                                               value="<s:property value='lead.indirizzo'/>">
                                    </div>
                                    <div class="col-md-6 mb-3">
                                        <label class="form-label">Città</label>
                                        <input type="text" name="lead.citta" class="form-control" 
                                               value="<s:property value='lead.citta'/>">
                                    </div>
                                    <div class="col-md-3 mb-3">
                                        <label class="form-label">Provincia</label>
                                        <input type="text" name="lead.provincia" class="form-control" 
                                               value="<s:property value='lead.provincia'/>" maxlength="2" placeholder="RM">
                                    </div>
                                    <div class="col-md-3 mb-3">
                                        <label class="form-label">CAP</label>
                                        <input type="text" name="lead.cap" class="form-control" 
                                               value="<s:property value='lead.cap'/>" maxlength="10" placeholder="00100">
                                    </div>
                                </div>
                            </div>
                        </div>

                        <!-- Esigenza e Note -->
                        <div class="card mb-3">
                            <div class="card-header bg-warning">
                                <h5 class="mb-0"><i class="bi bi-chat-left-text me-2"></i>Esigenza e Note</h5>
                            </div>
                            <div class="card-body">
                                <div class="mb-3">
                                    <label class="form-label">Esigenza</label>
                                    <textarea name="lead.esigenza" class="form-control" rows="4" 
                                              placeholder="Descrivi l'esigenza del cliente..."><s:property value='lead.esigenza'/></textarea>
                                </div>
                                <div class="mb-3">
                                    <label class="form-label">Note Interne</label>
                                    <textarea name="lead.note" class="form-control" rows="4" 
                                              placeholder="Note riservate..."><s:property value='lead.note'/></textarea>
                                </div>
                            </div>
                        </div>
                    </div>

                    <!-- Colonna Destra -->
                    <div class="col-md-4">
                        <!-- Stato e Probabilità -->
                        <div class="card mb-3">
                            <div class="card-header bg-success text-white">
                                <h5 class="mb-0"><i class="bi bi-trophy me-2"></i>Stato Lead</h5>
                            </div>
                            <div class="card-body">
                                <div class="mb-3">
                                    <label class="form-label">Stato <span class="text-danger">*</span></label>
                                    <select name="lead.stato" class="form-select" required>
                                        <option value="NUOVO" <s:if test="lead.stato == @it.zensoftware.luna2.model.Lead$Stato@NUOVO || lead.stato == null">selected</s:if>>Nuovo</option>
                                        <option value="CONTATTATO" <s:if test="lead.stato == @it.zensoftware.luna2.model.Lead$Stato@CONTATTATO">selected</s:if>>Contattato</option>
                                        <option value="QUALIFICATO" <s:if test="lead.stato == @it.zensoftware.luna2.model.Lead$Stato@QUALIFICATO">selected</s:if>>Qualificato</option>
                                        <option value="PREVENTIVO" <s:if test="lead.stato == @it.zensoftware.luna2.model.Lead$Stato@PREVENTIVO">selected</s:if>>Preventivo</option>
                                        <option value="NEGOZIAZIONE" <s:if test="lead.stato == @it.zensoftware.luna2.model.Lead$Stato@NEGOZIAZIONE">selected</s:if>>Negoziazione</option>
                                        <option value="VINTO" <s:if test="lead.stato == @it.zensoftware.luna2.model.Lead$Stato@VINTO">selected</s:if>>Vinto</option>
                                        <option value="PERSO" <s:if test="lead.stato == @it.zensoftware.luna2.model.Lead$Stato@PERSO">selected</s:if>>Perso</option>
                                    </select>
                                </div>
                                <div class="mb-3">
                                    <label class="form-label">Probabilità Chiusura (%)</label>
                                    <input type="number" name="lead.probabilitaChiusura" class="form-control" 
                                           value="<s:property value='lead.probabilitaChiusura'/>" min="0" max="100">
                                    <div class="form-text">0-100%, default: 0</div>
                                </div>
                            </div>
                        </div>

                        <!-- Budget e Date -->
                        <div class="card mb-3">
                            <div class="card-header bg-dark text-white">
                                <h5 class="mb-0"><i class="bi bi-cash-stack me-2"></i>Budget e Date</h5>
                            </div>
                            <div class="card-body">
                                <div class="mb-3">
                                    <label class="form-label">Budget Stimato (€)</label>
                                    <input type="number" name="lead.budgetStimato" class="form-control" 
                                           value="<s:property value='lead.budgetStimato'/>" step="0.01" min="0">
                                </div>
                                <div class="mb-3">
                                    <label class="form-label">Data Contatto</label>
                                    <input type="date" name="lead.dataContatto" class="form-control" 
                                           value="<s:date name='lead.dataContatto' format='yyyy-MM-dd'/>">
                                </div>
                                <div class="mb-3">
                                    <label class="form-label">Prossimo Follow-up</label>
                                    <input type="datetime-local" name="lead.dataProssimoFollowup" class="form-control" 
                                           value="<s:date name='lead.dataProssimoFollowup' format='yyyy-MM-dd\'T\'HH:mm'/>">
                                    <div class="form-text">Imposta reminder follow-up</div>
                                </div>
                            </div>
                        </div>

                        <!-- Tag (Optional) -->
                        <div class="card mb-3">
                            <div class="card-header bg-light">
                                <h6 class="mb-0"><i class="bi bi-tags me-2"></i>Tag (opzionale)</h6>
                            </div>
                            <div class="card-body">
                                <small class="text-muted">Funzionalità tag disponibile dopo creazione lead</small>
                            </div>
                        </div>

                        <!-- Pulsanti Azioni -->
                        <div class="card">
                            <div class="card-body">
                                <button type="submit" class="btn btn-primary w-100 mb-2">
                                    <i class="bi bi-save me-1"></i>Salva Lead
                                </button>
                                <a href="<s:url action='list' namespace='/app/lead'/>" class="btn btn-secondary w-100">
                                    <i class="bi bi-x-circle me-1"></i>Annulla
                                </a>
                            </div>
                        </div>
                    </div>
                </div>
            </form>
        </div>
    </div>

    </div>
</div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
    <script>
        // Auto-set data contatto to today if empty and creating new lead
        document.addEventListener('DOMContentLoaded', function() {
            const dataContattoInput = document.querySelector('input[name="lead.dataContatto"]');
            if (dataContattoInput && !dataContattoInput.value) {
                const today = new Date().toISOString().split('T')[0];
                dataContattoInput.value = today;
            }
        });
    </script>
</body>
</html>
