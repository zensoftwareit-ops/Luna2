package it.zensoftware.luna2.model;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * NoleggioNBT - Noleggio Breve Termine (NBT Module)
 * 
 * Gestisce richieste e pratiche di noleggio a breve termine (da 1 giorno a 30 giorni),
 * con workflow parallelo rispetto al noleggio a lungo termine.
 * 
 * LOGICA AUTOMAZIONE (NBT):
 * - Reminder giornalieri per chiusura pratica NBT
 * - Alert per preventivi NBT non seguiti entro 24h
 * - Push notification per conferme/cancellazioni
 * - Monitoraggio disponibilità veicoli breve termine
 * - Alert restituzione veicolo (1 giorno prima)
 * 
 * DIFFERENZE vs LUNGO TERMINE:
 * - Durata: 1-30 giorni (vs 12-60 mesi)
 * - Tariffa: Giornaliera (vs canone mensile)
 * - Documenti: Ridotti (patente + carta credito)
 * - Valutazione: No istruttoria finanziaria approfondita
 * - Consegna: Immediata o entro 48h (vs 2-4 mesi)
 * 
 * DB: noleggio_nbt
 * 
 * @author Luna2 CRM - Modulo Broker Noleggio
 */
@Entity
@Table(name = "noleggio_nbt")
public class NoleggioNBT implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    // ==================== ENUMERATIONS ====================
    
    /**
     * Status - Stati pratica NBT
     */
    public enum Status {
        RICHIESTA_RICEVUTA,     // Richiesta appena ricevuta
        PREVENTIVO_INVIATO,     // Preventivo inviato al cliente
        PRENOTAZIONE_CONFERMATA,// Cliente ha confermato prenotazione
        VEICOLO_ASSEGNATO,      // Veicolo assegnato alla prenotazione
        PAGAMENTO_RICEVUTO,     // Pagamento ricevuto
        PRONTA_CONSEGNA,        // Pronta per ritiro/consegna veicolo
        IN_CORSO,               // Noleggio in corso
        IN_SCADENZA,            // Manca 1 giorno alla riconsegna
        COMPLETATO,             // Veicolo riconsegnato, pratica chiusa
        ANNULLATO,              // Prenotazione annullata
        SCADUTA                 // Preventivo scaduto senza conferma
    }
    
    /**
     * TipologiaNoleggio - Tipo di noleggio breve
     */
    public enum TipologiaNoleggio {
        GIORNALIERO,            // 1-3 giorni
        WEEKEND,                // Weekend (Ven-Dom)
        SETTIMANALE,            // 7 giorni
        BISETTIMANALE,          // 14 giorni
        MENSILE_BREVE           // 20-30 giorni
    }
    
    /**
     * CategoriaVeicolo - Categoria veicolo richiesto
     */
    public enum CategoriaVeicolo {
        CITY_CAR,               // Utilitaria (Fiat 500, Smart)
        COMPATTA,               // Compatta (Golf, Focus)
        BERLINA,                // Berlina (Serie 3, Classe C)
        STATION_WAGON,          // Familiare
        SUV_COMPATTO,           // SUV piccolo (Qashqai, Tiguan)
        SUV_GRANDE,             // SUV grande (X5, Q7)
        MONOVOLUME,             // Monovolume (Touran, Scenic)
        LUSSO,                  // Auto lusso (Serie 7, Classe S)
        PREMIUM,                // Premium (Audi, BMW entry)
        COMMERCIALE             // Furgone
    }
    
    /**
     * MotivoNoleggio - Motivo della richiesta
     */
    public enum MotivoNoleggio {
        VACANZA,                // Vacanza/turismo
        BUSINESS,               // Viaggio lavoro
        SOSTITUZIONE_AUTO,      // Auto in riparazione
        EVENTO,                 // Evento speciale (matrimonio, etc.)
        PROVA_AUTO,             // Prova prima acquisto lungo termine
        ALTRO                   // Altro
    }
    
    // ==================== PRIMARY KEY ====================
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    
    // ==================== FOREIGN KEYS ====================
    
    @Column(name = "cliente_id", nullable = false)
    private Long clienteId;  // FK a users/crm_leads
    
    @Column(name = "operatore_assegnato_id")
    private Long operatoreAssegnatoId;  // FK a users (operatore che gestisce)
    
    @Column(name = "veicolo_id")
    private Long veicoloId;  // FK a eventuale tabella veicoli_flotta
    
    // ==================== CORE FIELDS ====================
    
    @Column(name = "numero_pratica", unique = true, length = 50)
    private String numeroPratica;  // Es: NBT-2025-00234
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private Status status;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "tipologia_noleggio", nullable = false, length = 50)
    private TipologiaNoleggio tipologiaNoleggio;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "motivo_noleggio", length = 50)
    private MotivoNoleggio motivoNoleggio;
    
    @Column(name = "data_richiesta", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date dataRichiesta;
    
    // ==================== DATE NOLEGGIO ====================
    
    @Column(name = "data_inizio_noleggio", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date dataInizioNoleggio;
    
    @Column(name = "data_fine_noleggio", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date dataFineNoleggio;
    
    @Column(name = "numero_giorni", nullable = false)
    private Integer numeroGiorni;
    
    @Column(name = "luogo_ritiro", nullable = false, length = 200)
    private String luogoRitiro;
    
    @Column(name = "indirizzo_ritiro", columnDefinition = "TEXT")
    private String indirizzoRitiro;
    
    @Column(name = "luogo_riconsegna", nullable = false, length = 200)
    private String luogoRiconsegna;
    
    @Column(name = "indirizzo_riconsegna", columnDefinition = "TEXT")
    private String indirizzoRiconsegna;
    
    @Column(name = "servizio_consegna_domicilio")
    private Boolean servizioConsegnaDomicilio;  // TRUE se richiesta consegna a casa
    
    // ==================== DATI CLIENTE ====================
    
    @Column(name = "nome_cliente", nullable = false, length = 200)
    private String nomeCliente;
    
    @Column(name = "email", nullable = false, length = 200)
    private String email;
    
    @Column(name = "telefono", nullable = false, length = 50)
    private String telefono;
    
    @Column(name = "numero_patente", length = 50)
    private String numeroPatente;
    
    @Column(name = "data_scadenza_patente")
    @Temporal(TemporalType.DATE)
    private Date dataScadenzaPatente;
    
    @Column(name = "eta_conducente")
    private Integer etaConducente;
    
    // ==================== VEICOLO RICHIESTO ====================
    
    @Enumerated(EnumType.STRING)
    @Column(name = "categoria_richiesta", nullable = false, length = 50)
    private CategoriaVeicolo categoriaRichiesta;
    
    @Column(name = "marca_preferita", length = 100)
    private String marcaPreferita;
    
    @Column(name = "modello_preferito", length = 100)
    private String modelloPreferito;
    
    @Column(name = "cambio_automatico")
    private Boolean cambioAutomatico;
    
    @Column(name = "aria_condizionata")
    private Boolean ariaCondizionata;
    
    @Column(name = "posti_minimi")
    private Integer postiMinimi;
    
    @Column(name = "note_preferenze", columnDefinition = "TEXT")
    private String notePreferenze;
    
    // ==================== VEICOLO ASSEGNATO ====================
    
    @Column(name = "marca_assegnata", length = 100)
    private String marcaAssegnata;
    
    @Column(name = "modello_assegnato", length = 100)
    private String modelloAssegnato;
    
    @Column(name = "targa_assegnata", length = 20)
    private String targaAssegnata;
    
    @Column(name = "colore_assegnato", length = 100)
    private String coloreAssegnato;
    
    @Column(name = "km_ritiro")
    private Integer kmRitiro;
    
    @Column(name = "km_riconsegna")
    private Integer kmRiconsegna;
    
    @Column(name = "km_percorsi")
    private Integer kmPercorsi;
    
    // ==================== TARIFFAZIONE ====================
    
    @Column(name = "tariffa_giornaliera", precision = 10, scale = 2)
    private BigDecimal tariffaGiornaliera;
    
    @Column(name = "costo_consegna_ritiro", precision = 10, scale = 2)
    private BigDecimal costoConsegnaRitiro;
    
    @Column(name = "costo_km_extra", precision = 10, scale = 4)
    private BigDecimal costoKmExtra;  // Costo per km oltre franchigia
    
    @Column(name = "franchigia_km_giornaliera")
    private Integer franchigiaKmGiornaliera;  // Es: 150 km/giorno inclusi
    
    @Column(name = "costo_assicurazione_kasko", precision = 10, scale = 2)
    private BigDecimal costoAssicurazioneKasko;
    
    @Column(name = "costo_conducente_aggiuntivo", precision = 10, scale = 2)
    private BigDecimal costoConducenteAggiuntivo;
    
    @Column(name = "deposito_cauzionale", precision = 10, scale = 2)
    private BigDecimal depositoCauzionale;
    
    @Column(name = "importo_totale", precision = 10, scale = 2)
    private BigDecimal importoTotale;
    
    @Column(name = "importo_pagato", precision = 10, scale = 2)
    private BigDecimal importoPagato;
    
    @Column(name = "saldo_residuo", precision = 10, scale = 2)
    private BigDecimal saldoResiduo;
    
    // ==================== PAGAMENTO ====================
    
    @Column(name = "metodo_pagamento", length = 50)
    private String metodoPagamento;  // CARTA_CREDITO, BONIFICO, CONTANTI
    
    @Column(name = "data_pagamento")
    @Temporal(TemporalType.TIMESTAMP)
    private Date dataPagamento;
    
    @Column(name = "transazione_id", length = 200)
    private String transazioneId;
    
    // ==================== PREVENTIVO ====================
    
    @Column(name = "numero_preventivo", length = 50)
    private String numeroPreventivo;
    
    @Column(name = "data_invio_preventivo")
    @Temporal(TemporalType.TIMESTAMP)
    private Date dataInvioPreventivo;
    
    @Column(name = "preventivo_valido_fino")
    @Temporal(TemporalType.TIMESTAMP)
    private Date preventivoValidoFino;  // Validità 48h
    
    @Column(name = "file_preventivo_path", length = 500)
    private String filePreventivoPath;
    
    // ==================== CONSEGNA/RICONSEGNA ====================
    
    @Column(name = "data_consegna_effettiva")
    @Temporal(TemporalType.TIMESTAMP)
    private Date dataConsegnaEffettiva;
    
    @Column(name = "data_riconsegna_effettiva")
    @Temporal(TemporalType.TIMESTAMP)
    private Date dataRiconsegnaEffettiva;
    
    @Column(name = "note_consegna", columnDefinition = "TEXT")
    private String noteConsegna;
    
    @Column(name = "note_riconsegna", columnDefinition = "TEXT")
    private String noteRiconsegna;
    
    @Column(name = "danni_rilevati", columnDefinition = "TEXT")
    private String danniRilevati;
    
    @Column(name = "foto_consegna", columnDefinition = "TEXT")
    private String fotoConsegna;  // JSON array foto stato veicolo a consegna
    
    @Column(name = "foto_riconsegna", columnDefinition = "TEXT")
    private String fotoRiconsegna;  // JSON array foto stato veicolo a riconsegna
    
    // ==================== REMINDER & ALERT ====================
    
    @Column(name = "alert_followup_24h_inviato")
    private Boolean alertFollowup24hInviato;
    
    @Column(name = "alert_restituzione_inviato")
    private Boolean alertRestituzioneInviato;
    
    @Column(name = "reminder_documenti_inviato")
    private Boolean reminderDocumentiInviato;
    
    // ==================== NOTE ====================
    
    @Column(name = "note_pratica", columnDefinition = "TEXT")
    private String notePratica;
    
    @Column(name = "note_interne", columnDefinition = "TEXT")
    private String noteInterne;
    
    @Column(name = "valutazione_cliente")
    private Integer valutazioneCliente;  // 1-5 stelle
    
    @Column(name = "feedback_cliente", columnDefinition = "TEXT")
    private String feedbackCliente;
    
    // ==================== TIMESTAMPS ====================
    
    @Column(name = "created_at", nullable = false, updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;
    
    @Column(name = "updated_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedAt;
    
    // ==================== LIFECYCLE CALLBACKS ====================
    
    @PrePersist
    protected void onCreate() {
        createdAt = new Date();
        updatedAt = new Date();
        if (status == null) {
            status = Status.RICHIESTA_RICEVUTA;
        }
        if (dataRichiesta == null) {
            dataRichiesta = new Date();
        }
        if (alertFollowup24hInviato == null) {
            alertFollowup24hInviato = false;
        }
        if (alertRestituzioneInviato == null) {
            alertRestituzioneInviato = false;
        }
        if (reminderDocumentiInviato == null) {
            reminderDocumentiInviato = false;
        }
        if (servizioConsegnaDomicilio == null) {
            servizioConsegnaDomicilio = false;
        }
        if (cambioAutomatico == null) {
            cambioAutomatico = false;
        }
        if (ariaCondizionata == null) {
            ariaCondizionata = true;  // Default true
        }
        
        // Genera numero pratica automatico
        if (numeroPratica == null) {
            numeroPratica = generaNumeroPratica();
        }
        
        // Calcola numero giorni
        if (numeroGiorni == null && dataInizioNoleggio != null && dataFineNoleggio != null) {
            numeroGiorni = calcolaGiorni(dataInizioNoleggio, dataFineNoleggio);
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = new Date();
        
        // Ricalcola km percorsi
        if (kmRitiro != null && kmRiconsegna != null) {
            kmPercorsi = kmRiconsegna - kmRitiro;
        }
        
        // Ricalcola saldo residuo
        if (importoTotale != null && importoPagato != null) {
            saldoResiduo = importoTotale.subtract(importoPagato);
        }
    }
    
    // ==================== BUSINESS METHODS ====================
    
    /**
     * Genera numero pratica univoco formato: NBT-YYYY-NNNNN
     */
    private String generaNumeroPratica() {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        int year = cal.get(java.util.Calendar.YEAR);
        long timestamp = System.currentTimeMillis() % 100000;
        return String.format("NBT-%d-%05d", year, timestamp);
    }
    
    /**
     * Calcola giorni tra due date.
     */
    private int calcolaGiorni(Date inizio, Date fine) {
        long diff = fine.getTime() - inizio.getTime();
        return (int) (diff / (24 * 60 * 60 * 1000)) + 1;  // +1 per contare giorno partenza
    }
    
    /**
     * Genera preventivo NBT.
     * 
     * @return numero preventivo generato
     */
    public String generaPreventivo() {
        this.numeroPreventivo = "PREV-NBT-" + System.currentTimeMillis() % 100000;
        this.dataInvioPreventivo = new Date();
        this.preventivoValidoFino = aggiungiOre(new Date(), 48);  // Valido 48h
        this.status = Status.PREVENTIVO_INVIATO;
        
        // Calcola importo totale
        calcolaImportoTotale();
        
        return numeroPreventivo;
    }
    
    /**
     * Calcola importo totale noleggio.
     */
    public void calcolaImportoTotale() {
        BigDecimal totale = BigDecimal.ZERO;
        
        // Tariffa base
        if (tariffaGiornaliera != null && numeroGiorni != null) {
            totale = tariffaGiornaliera.multiply(new BigDecimal(numeroGiorni));
        }
        
        // Consegna/ritiro
        if (costoConsegnaRitiro != null) {
            totale = totale.add(costoConsegnaRitiro);
        }
        
        // Assicurazione kasko
        if (costoAssicurazioneKasko != null) {
            totale = totale.add(costoAssicurazioneKasko);
        }
        
        // Conducente aggiuntivo
        if (costoConducenteAggiuntivo != null) {
            totale = totale.add(costoConducenteAggiuntivo);
        }
        
        this.importoTotale = totale;
    }
    
    /**
     * Conferma prenotazione.
     */
    public void confermaPrenotazione() {
        this.status = Status.PRENOTAZIONE_CONFERMATA;
    }
    
    /**
     * Assegna veicolo alla prenotazione.
     */
    public void assegnaVeicolo(String marca, String modello, String targa, String colore) {
        this.marcaAssegnata = marca;
        this.modelloAssegnato = modello;
        this.targaAssegnata = targa;
        this.coloreAssegnato = colore;
        this.status = Status.VEICOLO_ASSEGNATO;
    }
    
    /**
     * Registra pagamento.
     */
    public void registraPagamento(BigDecimal importo, String metodo, String transazioneId) {
        this.importoPagato = importo;
        this.metodoPagamento = metodo;
        this.transazioneId = transazioneId;
        this.dataPagamento = new Date();
        this.status = Status.PAGAMENTO_RICEVUTO;
        
        // Se pagamento completo, marca come pronta consegna
        if (importoPagato.compareTo(importoTotale) >= 0) {
            this.status = Status.PRONTA_CONSEGNA;
        }
    }
    
    /**
     * Consegna veicolo a cliente.
     */
    public void consegnaVeicolo(int kmRitiro, String noteConsegna) {
        this.kmRitiro = kmRitiro;
        this.noteConsegna = noteConsegna;
        this.dataConsegnaEffettiva = new Date();
        this.status = Status.IN_CORSO;
    }
    
    /**
     * Riconsegna veicolo da cliente.
     */
    public void riconsegnaVeicolo(int kmRiconsegna, String noteRiconsegna, String danni) {
        this.kmRiconsegna = kmRiconsegna;
        this.kmPercorsi = kmRiconsegna - kmRitiro;
        this.noteRiconsegna = noteRiconsegna;
        this.danniRilevati = danni;
        this.dataRiconsegnaEffettiva = new Date();
        this.status = Status.COMPLETATO;
        
        // Calcola costi km extra se superato franchigia
        BigDecimal costiExtra = calcolaCostiKmExtra();
        if (costiExtra.compareTo(BigDecimal.ZERO) > 0) {
            this.importoTotale = this.importoTotale.add(costiExtra);
            this.saldoResiduo = this.importoTotale.subtract(this.importoPagato);
        }
    }
    
    /**
     * Calcola costi km extra.
     */
    public BigDecimal calcolaCostiKmExtra() {
        if (kmPercorsi == null || franchigiaKmGiornaliera == null || 
            costoKmExtra == null || numeroGiorni == null) {
            return BigDecimal.ZERO;
        }
        
        int kmInclusi = franchigiaKmGiornaliera * numeroGiorni;
        int kmExtra = kmPercorsi - kmInclusi;
        
        if (kmExtra <= 0) {
            return BigDecimal.ZERO;
        }
        
        return costoKmExtra.multiply(new BigDecimal(kmExtra));
    }
    
    /**
     * Verifica se preventivo richiede follow-up (oltre 24h senza conferma).
     * AUTOMAZIONE: Chiamato da NoleggioAutomationService ogni giorno.
     * 
     * @return true se richiede follow-up
     */
    public boolean richiedeFollowup24h() {
        if (status != Status.PREVENTIVO_INVIATO || alertFollowup24hInviato) {
            return false;
        }
        
        if (dataInvioPreventivo == null) {
            return false;
        }
        
        long oreTrascorse = calcolaOreTrascorseDa(dataInvioPreventivo);
        return oreTrascorse >= 24;
    }
    
    /**
     * Verifica se richiede alert restituzione (1 giorno prima fine noleggio).
     * AUTOMAZIONE: Chiamato da NoleggioAutomationService ogni giorno.
     * 
     * @return true se richiede alert restituzione
     */
    public boolean richiedeAlertRestituzione() {
        if (status != Status.IN_CORSO || alertRestituzioneInviato) {
            return false;
        }
        
        if (dataFineNoleggio == null) {
            return false;
        }
        
        long oreARestituzione = calcolaOreFinoA(dataFineNoleggio);
        // Alert 24 ore prima
        return oreARestituzione <= 24 && oreARestituzione > 0;
    }
    
    /**
     * Verifica se preventivo è scaduto (oltre 48h senza conferma).
     * 
     * @return true se scaduto
     */
    public boolean isPreventivoScaduto() {
        if (status != Status.PREVENTIVO_INVIATO) {
            return false;
        }
        
        if (preventivoValidoFino == null) {
            return false;
        }
        
        return new Date().after(preventivoValidoFino);
    }
    
    /**
     * Calcola ore trascorse da una data.
     */
    private long calcolaOreTrascorseDa(Date data) {
        if (data == null) return 0;
        long diff = new Date().getTime() - data.getTime();
        return diff / (60 * 60 * 1000);
    }
    
    /**
     * Calcola ore fino a una data.
     */
    private long calcolaOreFinoA(Date data) {
        if (data == null) return Long.MAX_VALUE;
        long diff = data.getTime() - new Date().getTime();
        return diff / (60 * 60 * 1000);
    }
    
    /**
     * Aggiunge ore a una data.
     */
    private Date aggiungiOre(Date data, int ore) {
        return new Date(data.getTime() + (ore * 60L * 60 * 1000));
    }
    
    // ==================== GETTERS & SETTERS ====================
    // (Omessi per brevità - generabili automaticamente da IDE)
    // Includono tutti i campi dichiarati sopra
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getClienteId() {
        return clienteId;
    }
    
    public void setClienteId(Long clienteId) {
        this.clienteId = clienteId;
    }
    
    public String getNumeroPratica() {
        return numeroPratica;
    }
    
    public void setNumeroPratica(String numeroPratica) {
        this.numeroPratica = numeroPratica;
    }
    
    public Date getDataRichiesta() {
        return dataRichiesta;
    }
    
    public void setDataRichiesta(Date dataRichiesta) {
        this.dataRichiesta = dataRichiesta;
    }
    
    public Status getStatus() {
        return status;
    }
    
    public void setStatus(Status status) {
        this.status = status;
    }
    
    public Date getDataInizioNoleggio() {
        return dataInizioNoleggio;
    }
    
    public void setDataInizioNoleggio(Date dataInizioNoleggio) {
        this.dataInizioNoleggio = dataInizioNoleggio;
    }
    
    public Date getDataFineNoleggio() {
        return dataFineNoleggio;
    }
    
    public void setDataFineNoleggio(Date dataFineNoleggio) {
        this.dataFineNoleggio = dataFineNoleggio;
    }
    
    public Integer getNumeroGiorni() {
        return numeroGiorni;
    }
    
    public void setNumeroGiorni(Integer numeroGiorni) {
        this.numeroGiorni = numeroGiorni;
    }
    
    public BigDecimal getImportoTotale() {
        return importoTotale;
    }
    
    public void setImportoTotale(BigDecimal importoTotale) {
        this.importoTotale = importoTotale;
    }
    
    // ... (altri getter/setter per completezza)
    
    public Date getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
    
    public Date getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getNomeCliente() {
        return nomeCliente;
    }

    public void setNomeCliente(String nomeCliente) {
        this.nomeCliente = nomeCliente;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public Date getPreventivoValidoFino() {
        return preventivoValidoFino;
    }

    public void setPreventivoValidoFino(Date preventivoValidoFino) {
        this.preventivoValidoFino = preventivoValidoFino;
    }

    public String getTargaAssegnata() {
        return targaAssegnata;
    }

    public void setTargaAssegnata(String targaAssegnata) {
        this.targaAssegnata = targaAssegnata;
    }

    public String getMarcaPreferita() {
        return marcaPreferita;
    }

    public void setMarcaPreferita(String marcaPreferita) {
        this.marcaPreferita = marcaPreferita;
    }

    public String getModelloPreferito() {
        return modelloPreferito;
    }

    public void setModelloPreferito(String modelloPreferito) {
        this.modelloPreferito = modelloPreferito;
    }

    public TipologiaNoleggio getTipologiaNoleggio() {
        return tipologiaNoleggio;
    }

    public void setTipologiaNoleggio(TipologiaNoleggio tipologiaNoleggio) {
        this.tipologiaNoleggio = tipologiaNoleggio;
    }

    public CategoriaVeicolo getCategoriaRichiesta() {
        return categoriaRichiesta;
    }

    public void setCategoriaRichiesta(CategoriaVeicolo categoriaRichiesta) {
        this.categoriaRichiesta = categoriaRichiesta;
    }

    public String getLuogoRitiro() {
        return luogoRitiro;
    }

    public void setLuogoRitiro(String luogoRitiro) {
        this.luogoRitiro = luogoRitiro;
    }

    public String getLuogoRiconsegna() {
        return luogoRiconsegna;
    }

    public void setLuogoRiconsegna(String luogoRiconsegna) {
        this.luogoRiconsegna = luogoRiconsegna;
    }

    public Boolean getAlertFollowup24hInviato() {
        return alertFollowup24hInviato;
    }

    public void setAlertFollowup24hInviato(Boolean alertFollowup24hInviato) {
        this.alertFollowup24hInviato = alertFollowup24hInviato;
    }

    public Boolean getAlertRestituzioneInviato() {
        return alertRestituzioneInviato;
    }

    public void setAlertRestituzioneInviato(Boolean alertRestituzioneInviato) {
        this.alertRestituzioneInviato = alertRestituzioneInviato;
    }
}
