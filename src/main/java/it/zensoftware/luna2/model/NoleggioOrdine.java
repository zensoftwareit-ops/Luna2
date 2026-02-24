package it.zensoftware.luna2.model;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * NoleggioOrdine - Gestione ordine e logistica (Fase 3)
 * 
 * Gestisce l'ordine del veicolo presso il fornitore e il tracking della consegna,
 * con sistema di care call automatici ogni 20-30 giorni per aggiornare cliente.
 * 
 * LOGICA AUTOMAZIONE (Fase 3):
 * - Care call ogni 20-30 giorni fino a consegna
 * - Alert per ordini oltre 90 giorni senza consegna
 * - Push notification al cliente per cambio status ordine
 * - Notifica 7 giorni prima ETA stimato
 * 
 * DB: noleggio_ordini
 * 
 * @author Luna2 CRM - Modulo Broker Noleggio
 */
@Entity
@Table(name = "noleggio_ordini")
public class NoleggioOrdine implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    // ==================== ENUMERATIONS ====================
    
    /**
     * Status - Stati dell'ordine
     */
    public enum Status {
        ORDINE_CREATO,          // Ordine appena creato
        IN_ATTESA_FORNITORE,    // In attesa conferma fornitore
        CONFERMATO_FORNITORE,   // Fornitore ha confermato ordine
        IN_PRODUZIONE,          // Veicolo in produzione
        IN_TRANSITO,            // In spedizione verso deposito
        IN_DEPOSITO,            // Arrivato in deposito
        CONSEGNATO,             // Consegnato al cliente
        ANNULLATO               // Ordine annullato
    }
    
    /**
     * TipoFornitore - Tipo di fornitore
     */
    public enum TipoFornitore {
        COSTRUTTORE_DIRETTO,    // Costruttore (es. BMW, Mercedes)
        DEALER_UFFICIALE,       // Dealer ufficiale
        IMPORTATORE,            // Importatore nazionale
        GRUPPO_ACQUISTO,        // Gruppo d'acquisto
        FLEET_COMPANY           // Fleet management company
    }
    
    /**
     * Modalita Consegna - Come verrà consegnato il veicolo
     */
    public enum ModalitaConsegna {
        RITIRO_DEALER,          // Cliente ritira presso dealer
        CONSEGNA_DOMICILIO,     // Consegna a domicilio/azienda
        CONSEGNA_SEDE_BROKER    // Consegna presso broker
    }
    
    // ==================== PRIMARY KEY ====================
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    
    // ==================== FOREIGN KEYS ====================
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lead_id", nullable = false)
    private NoleggioLead lead;
    
    @Column(name = "preventivo_id", nullable = false)
    private Long preventivoId;  // FK a noleggio_preventivi (preventivo accettato)
    
    @Column(name = "valutazione_id", nullable = false)
    private Long valutazioneId;  // FK a noleggio_valutazioni (valutazione positiva)
    
    @Column(name = "utente_responsabile_id")
    private Long utenteResponsabileId;  // FK a users (responsabile pratica)
    
    // ==================== CORE FIELDS ====================
    
    @Column(name = "numero_ordine", unique = true, length = 50)
    private String numeroOrdine;  // Es: ORD-2025-00042
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private Status status;
    
    @Column(name = "data_ordine", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date dataOrdine;
    
    @Column(name = "data_conferma_fornitore")
    @Temporal(TemporalType.TIMESTAMP)
    private Date dataConfermaFornitore;
    
    @Column(name = "data_consegna_stimata")
    @Temporal(TemporalType.DATE)
    private Date dataConsegnaStimata;  // ETA (Estimated Time of Arrival)
    
    @Column(name = "data_consegna_effettiva")
    @Temporal(TemporalType.DATE)
    private Date dataConsegnaEffettiva;
    
    // ==================== VEICOLO & FORNITORE ====================
    
    @Column(name = "marca", nullable = false, length = 100)
    private String marca;
    
    @Column(name = "modello", nullable = false, length = 100)
    private String modello;
    
    @Column(name = "allestimento", length = 150)
    private String allestimento;
    
    @Column(name = "colore", length = 100)
    private String colore;
    
    @Column(name = "optional", columnDefinition = "TEXT")
    private String optional;  // Lista optional richiesti (JSON o CSV)
    
    @Column(name = "vin", length = 50)
    private String vin;  // Vehicle Identification Number (quando disponibile)
    
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_fornitore", length = 50)
    private TipoFornitore tipoFornitore;
    
    @Column(name = "nome_fornitore", length = 200)
    private String nomeFornitore;
    
    @Column(name = "contatto_fornitore", length = 200)
    private String contattoFornitore;  // Email/telefono referente fornitore
    
    @Column(name = "codice_ordine_fornitore", length = 100)
    private String codiceOrdineFornitore;  // Codice ordine presso il fornitore
    
    // ==================== COSTI ====================
    
    @Column(name = "prezzo_veicolo", nullable = false, precision = 10, scale = 2)
    private BigDecimal prezzoVeicolo;
    
    @Column(name = "costi_trasporto", precision = 10, scale = 2)
    private BigDecimal costiTrasporto;
    
    @Column(name = "costi_immatricolazione", precision = 10, scale = 2)
    private BigDecimal costiImmatricolazione;
    
    @Column(name = "costo_totale", precision = 10, scale = 2)
    private BigDecimal costoTotale;
    
    @Column(name = "canone_mensile_cliente", precision = 10, scale = 2)
    private BigDecimal canoneMensileCliente;  // Canone che pagherà il cliente
    
    // ==================== CONSEGNA ====================
    
    @Enumerated(EnumType.STRING)
    @Column(name = "modalita_consegna", length = 50)
    private ModalitaConsegna modalitaConsegna;
    
    @Column(name = "indirizzo_consegna", columnDefinition = "TEXT")
    private String indirizzoConsegna;
    
    @Column(name = "note_consegna", columnDefinition = "TEXT")
    private String noteConsegna;
    
    // ==================== CARE CALL SYSTEM ====================
    
    @Column(name = "numero_care_call_effettuati")
    private Integer numeroCareCallEffettuati;
    
    @Column(name = "data_ultimo_care_call")
    @Temporal(TemporalType.TIMESTAMP)
    private Date dataUltimoCareCall;
    
    @Column(name = "prossimo_care_call_previsto")
    @Temporal(TemporalType.TIMESTAMP)
    private Date prossimoCareCallPrevisto;
    
    @Column(name = "giorni_tra_care_call")
    private Integer giorniTraCareCall;  // Default 25 giorni (20-30 range)
    
    // ==================== TRACKING & NOTIFICHE ====================
    
    @Column(name = "tracking_url", length = 500)
    private String trackingUrl;  // URL tracking fornitore/corriere
    
    @Column(name = "note_tracking", columnDefinition = "TEXT")
    private String noteTracking;  // Note aggiornamenti tracking
    
    @Column(name = "notifica_eta_inviata")
    private Boolean notificaEtaInviata;  // Notifica 7 giorni prima ETA
    
    @Column(name = "ultima_notifica_cliente")
    @Temporal(TemporalType.TIMESTAMP)
    private Date ultimaNotificaCliente;
    
    // ==================== NOTE ====================
    
    @Column(name = "note_ordine", columnDefinition = "TEXT")
    private String noteOrdine;
    
    @Column(name = "note_interne", columnDefinition = "TEXT")
    private String noteInterne;
    
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
            status = Status.ORDINE_CREATO;
        }
        if (dataOrdine == null) {
            dataOrdine = new Date();
        }
        if (numeroCareCallEffettuati == null) {
            numeroCareCallEffettuati = 0;
        }
        if (giorniTraCareCall == null) {
            giorniTraCareCall = 25;  // Default 25 giorni
        }
        if (notificaEtaInviata == null) {
            notificaEtaInviata = false;
        }
        
        // Genera numero ordine automatico
        if (numeroOrdine == null) {
            numeroOrdine = generaNumeroOrdine();
        }
        
        // Primo care call fra 25 giorni
        prossimoCareCallPrevisto = aggiungiGiorni(dataOrdine, giorniTraCareCall);
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = new Date();
        
        // Ricalcola costo totale
        costoTotale = calcolaCostoTotale();
    }
    
    // ==================== BUSINESS METHODS ====================
    
    /**
     * Genera numero ordine univoco formato: ORD-YYYY-NNNNN
     */
    private String generaNumeroOrdine() {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        int year = cal.get(java.util.Calendar.YEAR);
        long timestamp = System.currentTimeMillis() % 100000;
        return String.format("ORD-%d-%05d", year, timestamp);
    }
    
    /**
     * Calcola costo totale (veicolo + trasporto + immatricolazione).
     */
    public BigDecimal calcolaCostoTotale() {
        BigDecimal totale = prezzoVeicolo != null ? prezzoVeicolo : BigDecimal.ZERO;
        
        if (costiTrasporto != null) {
            totale = totale.add(costiTrasporto);
        }
        
        if (costiImmatricolazione != null) {
            totale = totale.add(costiImmatricolazione);
        }
        
        return totale;
    }
    
    /**
     * Verifica se l'ordine richiede care call (oltre X giorni da ultimo).
     * AUTOMAZIONE: Chiamato da NoleggioAutomationService ogni notte.
     * 
     * @return true se richiede care call
     */
    public boolean richiedeCareCall() {
        // Non fare care call se ordine già consegnato/annullato
        if (status == Status.CONSEGNATO || status == Status.ANNULLATO) {
            return false;
        }
        
        Date riferimento = dataUltimoCareCall != null ? dataUltimoCareCall : dataOrdine;
        long giorniDaUltimoCareCall = calcolaGiorniDa(riferimento);
        
        return giorniDaUltimoCareCall >= giorniTraCareCall;
    }
    
    /**
     * Registra care call effettuato.
     * 
     * @return messaggio care call generato
     */
    public String registraCareCallEffettuato() {
        this.numeroCareCallEffettuati++;
        this.dataUltimoCareCall = new Date();
        this.prossimoCareCallPrevisto = aggiungiGiorni(new Date(), giorniTraCareCall);
        
        String messaggioCareCall = "Gentile cliente, le diamo un aggiornamento sul suo ordine #" + 
                                   numeroOrdine + " (" + marca + " " + modello + ").\n\n";
        
        switch (status) {
            case IN_ATTESA_FORNITORE:
                messaggioCareCall += "⏳ Siamo in attesa di conferma dal fornitore. " +
                                    "Le comunicheremo l'ETA non appena ricevuto feedback.";
                break;
            case CONFERMATO_FORNITORE:
            case IN_PRODUZIONE:
                if (dataConsegnaStimata != null) {
                    messaggioCareCall += "✅ Ordine confermato! Consegna prevista: " + 
                                        formatDate(dataConsegnaStimata) + ".";
                } else {
                    messaggioCareCall += "✅ Ordine confermato dal fornitore. " +
                                        "Stiamo definendo i tempi di consegna.";
                }
                break;
            case IN_TRANSITO:
                messaggioCareCall += "🚚 Il veicolo è in transito verso il deposito. " +
                                    "Consegna prevista: " + formatDate(dataConsegnaStimata) + ".";
                if (trackingUrl != null) {
                    messaggioCareCall += "\nTracking: " + trackingUrl;
                }
                break;
            case IN_DEPOSITO:
                messaggioCareCall += "📦 Il veicolo è arrivato in deposito! " +
                                    "Stiamo completando le pratiche di immatricolazione. " +
                                    "La contatteremo a breve per la consegna.";
                break;
            case ORDINE_CREATO:
            case CONSEGNATO:
            case ANNULLATO:
                // Nessun messaggio specifico
                break;
        }
        
        messaggioCareCall += "\n\nPer qualsiasi domanda siamo a sua disposizione!";
        
        return messaggioCareCall;
    }
    
    /**
     * Verifica se ordine è in ritardo (oltre 90 giorni senza consegna).
     * AUTOMAZIONE: Alert per operatore.
     * 
     * @return true se in ritardo critico
     */
    public boolean isInRitardoCritico() {
        if (status == Status.CONSEGNATO || status == Status.ANNULLATO) {
            return false;
        }
        
        long giorniDaOrdine = calcolaGiorniDa(dataOrdine);
        return giorniDaOrdine >= 90;
    }
    
    /**
     * Verifica se manca 1 settimana all'ETA (per notifica).
     * AUTOMAZIONE: Notifica al cliente.
     * 
     * @return true se mancano 7 giorni all'ETA
     */
    public boolean richiedeNotificaEtaImminente() {
        if (notificaEtaInviata || dataConsegnaStimata == null) {
            return false;
        }
        
        if (status == Status.CONSEGNATO || status == Status.ANNULLATO) {
            return false;
        }
        
        long giorniAdEta = calcolaGiorniFino(dataConsegnaStimata);
        return giorniAdEta <= 7 && giorniAdEta >= 0;
    }
    
    /**
     * Avanza status ordine.
     */
    public void avanzaStatus(Status nuovoStatus) {
        this.status = nuovoStatus;
        
        if (nuovoStatus == Status.CONFERMATO_FORNITORE) {
            this.dataConfermaFornitore = new Date();
        }
        
        if (nuovoStatus == Status.CONSEGNATO) {
            this.dataConsegnaEffettiva = new Date();
        }
    }
    
    /**
     * Calcola giorni trascorsi da una data.
     */
    private long calcolaGiorniDa(Date data) {
        if (data == null) return 0;
        long diff = new Date().getTime() - data.getTime();
        return diff / (24 * 60 * 60 * 1000);
    }
    
    /**
     * Calcola giorni mancanti fino a una data.
     */
    private long calcolaGiorniFino(Date data) {
        if (data == null) return Long.MAX_VALUE;
        long diff = data.getTime() - new Date().getTime();
        return diff / (24 * 60 * 60 * 1000);
    }
    
    /**
     * Aggiunge giorni a una data.
     */
    private Date aggiungiGiorni(Date data, int giorni) {
        return new Date(data.getTime() + (giorni * 24L * 60 * 60 * 1000));
    }
    
    /**
     * Formatta data in stringa leggibile.
     */
    private String formatDate(Date data) {
        if (data == null) return "N/D";
        return new java.text.SimpleDateFormat("dd/MM/yyyy").format(data);
    }
    
    // ==================== GETTERS & SETTERS ====================
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public NoleggioLead getLead() {
        return lead;
    }
    
    public void setLead(NoleggioLead lead) {
        this.lead = lead;
    }
    
    public Long getPreventivoId() {
        return preventivoId;
    }
    
    public void setPreventivoId(Long preventivoId) {
        this.preventivoId = preventivoId;
    }
    
    public Long getValutazioneId() {
        return valutazioneId;
    }
    
    public void setValutazioneId(Long valutazioneId) {
        this.valutazioneId = valutazioneId;
    }
    
    public Long getUtenteResponsabileId() {
        return utenteResponsabileId;
    }
    
    public void setUtenteResponsabileId(Long utenteResponsabileId) {
        this.utenteResponsabileId = utenteResponsabileId;
    }
    
    public String getNumeroOrdine() {
        return numeroOrdine;
    }
    
    public void setNumeroOrdine(String numeroOrdine) {
        this.numeroOrdine = numeroOrdine;
    }
    
    public Status getStatus() {
        return status;
    }
    
    public void setStatus(Status status) {
        this.status = status;
    }
    
    public Date getDataOrdine() {
        return dataOrdine;
    }
    
    public void setDataOrdine(Date dataOrdine) {
        this.dataOrdine = dataOrdine;
    }
    
    public Date getDataConfermaFornitore() {
        return dataConfermaFornitore;
    }
    
    public void setDataConfermaFornitore(Date dataConfermaFornitore) {
        this.dataConfermaFornitore = dataConfermaFornitore;
    }
    
    public Date getDataConsegnaStimata() {
        return dataConsegnaStimata;
    }
    
    public void setDataConsegnaStimata(Date dataConsegnaStimata) {
        this.dataConsegnaStimata = dataConsegnaStimata;
    }
    
    public Date getDataConsegnaEffettiva() {
        return dataConsegnaEffettiva;
    }
    
    public void setDataConsegnaEffettiva(Date dataConsegnaEffettiva) {
        this.dataConsegnaEffettiva = dataConsegnaEffettiva;
    }
    
    public String getMarca() {
        return marca;
    }
    
    public void setMarca(String marca) {
        this.marca = marca;
    }
    
    public String getModello() {
        return modello;
    }
    
    public void setModello(String modello) {
        this.modello = modello;
    }
    
    public String getAllestimento() {
        return allestimento;
    }
    
    public void setAllestimento(String allestimento) {
        this.allestimento = allestimento;
    }
    
    public String getColore() {
        return colore;
    }
    
    public void setColore(String colore) {
        this.colore = colore;
    }
    
    public String getOptional() {
        return optional;
    }
    
    public void setOptional(String optional) {
        this.optional = optional;
    }
    
    public String getVin() {
        return vin;
    }
    
    public void setVin(String vin) {
        this.vin = vin;
    }
    
    public TipoFornitore getTipoFornitore() {
        return tipoFornitore;
    }
    
    public void setTipoFornitore(TipoFornitore tipoFornitore) {
        this.tipoFornitore = tipoFornitore;
    }
    
    public String getNomeFornitore() {
        return nomeFornitore;
    }
    
    public void setNomeFornitore(String nomeFornitore) {
        this.nomeFornitore = nomeFornitore;
    }
    
    public String getContattoFornitore() {
        return contattoFornitore;
    }
    
    public void setContattoFornitore(String contattoFornitore) {
        this.contattoFornitore = contattoFornitore;
    }
    
    public String getCodiceOrdineFornitore() {
        return codiceOrdineFornitore;
    }
    
    public void setCodiceOrdineFornitore(String codiceOrdineFornitore) {
        this.codiceOrdineFornitore = codiceOrdineFornitore;
    }
    
    public BigDecimal getPrezzoVeicolo() {
        return prezzoVeicolo;
    }
    
    public void setPrezzoVeicolo(BigDecimal prezzoVeicolo) {
        this.prezzoVeicolo = prezzoVeicolo;
    }
    
    public BigDecimal getCostiTrasporto() {
        return costiTrasporto;
    }
    
    public void setCostiTrasporto(BigDecimal costiTrasporto) {
        this.costiTrasporto = costiTrasporto;
    }
    
    public BigDecimal getCostiImmatricolazione() {
        return costiImmatricolazione;
    }
    
    public void setCostiImmatricolazione(BigDecimal costiImmatricolazione) {
        this.costiImmatricolazione = costiImmatricolazione;
    }
    
    public BigDecimal getCostoTotale() {
        return costoTotale;
    }
    
    public void setCostoTotale(BigDecimal costoTotale) {
        this.costoTotale = costoTotale;
    }
    
    public BigDecimal getCanoneMensileCliente() {
        return canoneMensileCliente;
    }
    
    public void setCanoneMensileCliente(BigDecimal canoneMensileCliente) {
        this.canoneMensileCliente = canoneMensileCliente;
    }
    
    public ModalitaConsegna getModalitaConsegna() {
        return modalitaConsegna;
    }
    
    public void setModalitaConsegna(ModalitaConsegna modalitaConsegna) {
        this.modalitaConsegna = modalitaConsegna;
    }
    
    public String getIndirizzoConsegna() {
        return indirizzoConsegna;
    }
    
    public void setIndirizzoConsegna(String indirizzoConsegna) {
        this.indirizzoConsegna = indirizzoConsegna;
    }
    
    public String getNoteConsegna() {
        return noteConsegna;
    }
    
    public void setNoteConsegna(String noteConsegna) {
        this.noteConsegna = noteConsegna;
    }
    
    public Integer getNumeroCareCallEffettuati() {
        return numeroCareCallEffettuati;
    }
    
    public void setNumeroCareCallEffettuati(Integer numeroCareCallEffettuati) {
        this.numeroCareCallEffettuati = numeroCareCallEffettuati;
    }
    
    public Date getDataUltimoCareCall() {
        return dataUltimoCareCall;
    }
    
    public void setDataUltimoCareCall(Date dataUltimoCareCall) {
        this.dataUltimoCareCall = dataUltimoCareCall;
    }
    
    public Date getProssimoCareCallPrevisto() {
        return prossimoCareCallPrevisto;
    }
    
    public void setProssimoCareCallPrevisto(Date prossimoCareCallPrevisto) {
        this.prossimoCareCallPrevisto = prossimoCareCallPrevisto;
    }
    
    public Integer getGiorniTraCareCall() {
        return giorniTraCareCall;
    }
    
    public void setGiorniTraCareCall(Integer giorniTraCareCall) {
        this.giorniTraCareCall = giorniTraCareCall;
    }
    
    public String getTrackingUrl() {
        return trackingUrl;
    }
    
    public void setTrackingUrl(String trackingUrl) {
        this.trackingUrl = trackingUrl;
    }
    
    public String getNoteTracking() {
        return noteTracking;
    }
    
    public void setNoteTracking(String noteTracking) {
        this.noteTracking = noteTracking;
    }
    
    public Boolean getNotificaEtaInviata() {
        return notificaEtaInviata;
    }
    
    public void setNotificaEtaInviata(Boolean notificaEtaInviata) {
        this.notificaEtaInviata = notificaEtaInviata;
    }
    
    public Date getUltimaNotificaCliente() {
        return ultimaNotificaCliente;
    }
    
    public void setUltimaNotificaCliente(Date ultimaNotificaCliente) {
        this.ultimaNotificaCliente = ultimaNotificaCliente;
    }
    
    public String getNoteOrdine() {
        return noteOrdine;
    }
    
    public void setNoteOrdine(String noteOrdine) {
        this.noteOrdine = noteOrdine;
    }
    
    public String getNoteInterne() {
        return noteInterne;
    }
    
    public void setNoteInterne(String noteInterne) {
        this.noteInterne = noteInterne;
    }
    
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
}
