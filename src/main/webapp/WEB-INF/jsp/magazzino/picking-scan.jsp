<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="/struts-tags" prefix="s" %>
<!DOCTYPE html>
<html>
<head>
    <title>Scanner Picking - <s:property value="pickingList.numero"/></title>
    <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
    <link rel="stylesheet" href="<s:url value='/assets/css/bootstrap.min.css'/>">
    <style>
        body { padding-bottom: 80px; background-color: #f8f9fa; }
        .scanner-header { 
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            padding: 20px;
            margin: -20px -20px 20px -20px;
            border-radius: 0 0 20px 20px;
        }
        .scanner-input { 
            font-size: 1.5rem; 
            text-align: center;
            border: 3px solid #667eea;
            border-radius: 10px;
            padding: 15px;
        }
        .scanner-input:focus {
            box-shadow: 0 0 20px rgba(102, 126, 234, 0.5);
        }
        .item-card { 
            border-radius: 15px;
            margin-bottom: 15px;
            border-left: 5px solid #ccc;
            transition: all 0.3s;
        }
        .item-card.pending { border-left-color: #6c757d; }
        .item-card.in-picking { border-left-color: #ffc107; }
        .item-card.picked { border-left-color: #28a745; }
        .item-card:active { transform: scale(0.98); }
        .progress-item { height: 30px; font-size: 16px; font-weight: bold; }
        .floating-action { 
            position: fixed;
            bottom: 20px;
            right: 20px;
            z-index: 1000;
        }
        .summary-bar {
            position: fixed;
            bottom: 0;
            left: 0;
            right: 0;
            background: white;
            padding: 15px;
            box-shadow: 0 -2px 10px rgba(0,0,0,0.1);
            z-index: 999;
        }
        .beep-animation { animation: beep 0.3s; }
        @keyframes beep {
            0%, 100% { background-color: white; }
            50% { background-color: #28a745; }
        }
    </style>
</head>
<body>
    <div class="container mt-4">
        <div class="scanner-header">
            <h3 class="mb-2"><s:property value="pickingList.numero"/></h3>
            <p class="mb-0">
                <i class="fas fa-box"></i> Ordine: <s:property value="pickingList.ordine.numero"/>
            </p>
        </div>
        
        <s:if test="hasActionMessages()">
            <div class="alert alert-success beep-animation"><s:actionmessage/></div>
        </s:if>
        <s:if test="hasActionErrors()">
            <div class="alert alert-danger"><s:actionerror/></div>
        </s:if>
        
        <!-- Scanner Input -->
        <div class="card mb-4">
            <div class="card-body">
                <s:form action="picking-scan" method="post" id="scanForm">
                    <s:hidden name="pickingListId"/>
                    <div class="form-group">
                        <label for="barcode"><i class="fas fa-barcode"></i> Scansiona Barcode</label>
                        <s:textfield name="barcode" id="barcode" cssClass="form-control scanner-input" 
                                   placeholder="Scansiona o inserisci barcode" autofocus="true"/>
                    </div>
                    <div class="form-group">
                        <label for="quantita">Quantità</label>
                        <s:textfield name="quantita" id="quantita" cssClass="form-control text-center" 
                                   value="1" type="number" step="0.01"/>
                    </div>
                    <button type="submit" class="btn btn-primary btn-lg btn-block">
                        <i class="fas fa-check-circle"></i> Conferma Prelievo
                    </button>
                </s:form>
            </div>
        </div>
        
        <!-- Items List -->
        <h5 class="mb-3">Articoli da Prelevare (<s:property value="items.size()"/>)</h5>
        
        <s:iterator value="items" var="item">
            <div class="card item-card <s:property value='#item.stato.name().toLowerCase()'/>">
                <div class="card-body">
                    <div class="row align-items-center">
                        <div class="col-8">
                            <h6 class="mb-1"><strong><s:property value="#item.prodotto.codice"/></strong></h6>
                            <p class="mb-1 text-muted"><s:property value="#item.prodotto.descrizione"/></p>
                            <small>
                                <s:if test="#item.posizione != null">
                                    <i class="fas fa-map-marker-alt"></i> <s:property value="#item.posizione.posizioneCompleta"/>
                                </s:if>
                            </small>
                        </div>
                        <div class="col-4 text-right">
                            <s:if test="#item.stato.name() == 'PICKED' || #item.stato.name() == 'VERIFICATO'">
                                <i class="fas fa-check-circle fa-3x text-success"></i>
                            </s:if>
                            <s:elseif test="#item.stato.name() == 'IN_PICKING'">
                                <i class="fas fa-spinner fa-spin fa-3x text-warning"></i>
                            </s:elseif>
                            <s:else>
                                <i class="fas fa-circle fa-3x text-muted"></i>
                            </s:else>
                        </div>
                    </div>
                    
                    <div class="mt-3">
                        <div class="d-flex justify-content-between mb-1">
                            <span>
                                Prelevata: <strong><s:property value="#item.quantitaPrelevata"/></strong> / 
                                <s:property value="#item.quantitaRichiesta"/>
                            </span>
                            <span><s:property value="#item.percentualeCompletamento"/>%</span>
                        </div>
                        <div class="progress progress-item">
                            <div class="progress-bar <s:if test='#item.stato.name() == \"PICKED\"'>bg-success</s:if>" 
                                 role="progressbar" 
                                 style="width: <s:property value='#item.percentualeCompletamento'/>%">
                                <s:property value="#item.quantitaPrelevata"/> / <s:property value="#item.quantitaRichiesta"/>
                            </div>
                        </div>
                    </div>
                    
                    <s:if test="#item.prodotto.codiceEan != null">
                        <div class="mt-2">
                            <small class="text-muted">
                                <i class="fas fa-barcode"></i> EAN: <s:property value="#item.prodotto.codiceEan"/>
                            </small>
                        </div>
                    </s:if>
                </div>
            </div>
        </s:iterator>
        
        <!-- Floating Action Button -->
        <s:if test="pickingList.stato.name() == 'IN_PROGRESS'">
            <div class="floating-action">
                <s:a action="picking-completa" cssClass="btn btn-success btn-lg rounded-circle" 
                     style="width: 70px; height: 70px;">
                    <s:param name="pickingListId" value="pickingList.id"/>
                    <i class="fas fa-check fa-2x"></i>
                </s:a>
            </div>
        </s:if>
    </div>
    
    <!-- Summary Bar -->
    <div class="summary-bar">
        <div class="row text-center">
            <div class="col-4">
                <small class="text-muted">Completamento</small><br/>
                <strong><s:property value="pickingList.percentualeCompletamento"/>%</strong>
            </div>
            <div class="col-4">
                <small class="text-muted">Articoli</small><br/>
                <strong><s:property value="items.size()"/></strong>
            </div>
            <div class="col-4">
                <small class="text-muted">Tempo</small><br/>
                <strong>
                    <s:if test="pickingList.dataInizio != null">
                        <span id="timer">00:00</span>
                    </s:if>
                    <s:else>--:--</s:else>
                </strong>
            </div>
        </div>
    </div>
    
    <script src="<s:url value='/assets/js/jquery.min.js'/>"></script>
    <script src="<s:url value='/assets/js/bootstrap.bundle.min.js'/>"></script>
    <script>
        // Auto-focus su barcode dopo submit
        $(document).ready(function() {
            $('#scanForm').on('submit', function() {
                setTimeout(function() {
                    $('#barcode').focus();
                }, 100);
            });
            
            // Timer
            <s:if test="pickingList.dataInizio != null">
                var startTime = <s:property value="pickingList.dataInizio.time"/>;
                setInterval(function() {
                    var now = new Date().getTime();
                    var elapsed = Math.floor((now - startTime) / 1000);
                    var minutes = Math.floor(elapsed / 60);
                    var seconds = elapsed % 60;
                    $('#timer').text(
                        (minutes < 10 ? '0' : '') + minutes + ':' + 
                        (seconds < 10 ? '0' : '') + seconds
                    );
                }, 1000);
            </s:if>
            
            // Beep sound su success (opzionale - richiede Web Audio API)
            <s:if test="hasActionMessages()">
                if (window.AudioContext || window.webkitAudioContext) {
                    var audioCtx = new (window.AudioContext || window.webkitAudioContext)();
                    var oscillator = audioCtx.createOscillator();
                    oscillator.frequency.value = 800;
                    oscillator.connect(audioCtx.destination);
                    oscillator.start();
                    setTimeout(function() { oscillator.stop(); }, 200);
                }
            </s:if>
        });
    </script>
</body>
</html>
