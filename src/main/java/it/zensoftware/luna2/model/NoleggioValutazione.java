package it.zensoftware.luna2.model;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * NoleggioValutazione - Valutazione pratica istruttoria (Fase 2)
 * 
 * Gestisce il processo di valutazione del cliente/azienda nella fase di istruttoria,
 * con sistema di solleciti automatici per documentazione mancante.
 * 
 * LOGICA AUTOMAZIONE (Fase 2):
 * - Solleciti documenti mancanti ogni 4-5 giorni
 * - Alert per valutazioni "IN_ATTESA" oltre 7 giorni
 * - Push notification per esito valutazione
 * 
 * DB: noleggio_valutazioni
 * 
 * @author Luna2 CRM - Modulo Broker Noleggio
 */
@Entity
@Table(name = "noleggio_valutazioni")
public class NoleggioValutazione implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    // ==================== ENUMERATIONS ====================
    
    /**
     * Status - Stati del processo di valutazione
     */
    public enum Status {
        DOCUMENTI_MANCANTI,      // Documenti da completare
        IN_VALUTAZIONE,          // Documentazione completa, valutazione in corso
        SOLLECITI_INVIATI,       // Solleciti documenti inviati
        VALUTAZIONE_POSITIVA,    // Cliente approvato
        VALUTAZIONE_NEGATIVA,    // Cliente rifiutato
        SOSPESA,                 // Valutazione sospesa temporaneamente
        ANNULLATA                // Pratica annullata
    }
    
    /**
     * TipoValutazione - Tipo di valutazione richiesta
     */
    public enum TipoValutazione {
        PRIVATO_BUSTA_PAGA,      // Privato con buste paga
        PRIVATO_CUD,             // Privato con CUD
        AZIENDA_BILANCIO,        // Azienda con bilanci
        LIBERO_PROFESSIONISTA,   // Libero professionista con modello UNICO
        STARTUP_SENZA_STORICO    // Startup senza storico (garanzie alternative)
    }
    
    /**
     * EsitoRischioCredito - Esito valutazione rischio credito
     */
    public enum EsitoRischioCredito {
        RISCHIO_BASSO,           // Rischio basso - approvazione automatica
        RISCHIO_MEDIO,           // Rischio medio - approvazione con garanzie
        RISCHIO_ALTO,            // Rischio alto - richiesta analisi manuale
        NON_APPROVABILE          // Non finanziabile
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
    
    @Column(name = "preventivo_id")
    private Long preventivoId;  // FK a noleggio_preventivi (preventivo approvato)
    
    @Column(name = "utente_valutatore_id")
    private Long utenteValutatoreId;  // FK a users (chi ha fatto valutazione)
    
    // ==================== CORE FIELDS ====================
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private Status status;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_valutazione", nullable = false, length = 50)
    private TipoValutazione tipoValutazione;
    
    @Column(name = "data_avvio_istruttoria", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date dataAvvioIstruttoria;
    
    @Column(name = "data_completamento_documenti")
    @Temporal(TemporalType.TIMESTAMP)
    private Date dataCompletamentoDocumenti;
    
    @Column(name = "data_valutazione")
    @Temporal(TemporalType.TIMESTAMP)
    private Date dataValutazione;
    
    // ==================== VALUTAZIONE FIELDS ====================
    
    @Enumerated(EnumType.STRING)
    @Column(name = "esito_rischio_credito", length = 50)
    private EsitoRischioCredito esitoRischioCredito;
    
    @Column(name = "score_creditizio")
    private Integer scoreCreditizio;  // 0-1000 (es. 750 = buon credito)
    
    @Column(name = "reddito_mensile_dichiarato", precision = 10, scale = 2)
    private BigDecimal redditoMensileDichiarato;
    
    @Column(name = "reddito_mensile_verificato", precision = 10, scale = 2)
    private BigDecimal redditoMensileVerificato;
    
    @Column(name = "rata_massima_sostenibile", precision = 10, scale = 2)
    private BigDecimal rataMassimaSostenibile;  // Calcolato: 30% del reddito netto
    
    @Column(name = "garanzia_fideiussoria_richiesta")
    private Boolean garanziaFideiussoriaRichiesta;
    
    @Column(name = "importo_garanzia", precision = 10, scale = 2)
    private BigDecimal importoGaranzia;
    
    @Column(name = "anticipo_richiesto", precision = 10, scale = 2)
    private BigDecimal anticipoRichiesto;  // % del valore veicolo da versare in anticipo
    
    // ==================== SOLLECITI SYSTEM ====================
    
    @Column(name = "numero_solleciti_inviati")
    private Integer numeroSollecitiInviati;
    
    @Column(name = "data_ultimo_sollecito")
    @Temporal(TemporalType.TIMESTAMP)
    private Date dataUltimoSollecito;
    
    @Column(name = "prossimo_sollecito_previsto")
    @Temporal(TemporalType.TIMESTAMP)
    private Date prossimoSollecitoPrevisto;
    
    @Column(name = "documenti_mancanti_count")
    private Integer documentiMancantiCount;  // Count documenti STATUS != VALIDATO
    
    // ==================== NOTE & MOTIVAZIONI ====================
    
    @Column(name = "note_valutatore", columnDefinition = "TEXT")
    private String noteValutatore;
    
    @Column(name = "motivazione_rifiuto", columnDefinition = "TEXT")
    private String motivazioneRifiuto;
    
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
            status = Status.DOCUMENTI_MANCANTI;
        }
        if (dataAvvioIstruttoria == null) {
            dataAvvioIstruttoria = new Date();
        }
        if (numeroSollecitiInviati == null) {
            numeroSollecitiInviati = 0;
        }
        if (garanziaFideiussoriaRichiesta == null) {
            garanziaFideiussoriaRichiesta = false;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = new Date();
    }
    
    // ==================== BUSINESS METHODS ====================
    
    /**
     * Verifica se la valutazione richiede sollecito documenti (oltre 4 giorni da ultimo sollecito).
     * AUTOMAZIONE: Chiamato da NoleggioAutomationService ogni notte.
     * 
     * @return true se richiede sollecito
     */
    public boolean richiedeSollecitoDocumenti() {
        if (status != Status.DOCUMENTI_MANCANTI && status != Status.SOLLECITI_INVIATI) {
            return false;
        }
        
        if (documentiMancantiCount == null || documentiMancantiCount == 0) {
            return false;
        }
        
        Date riferimento = dataUltimoSollecito != null ? dataUltimoSollecito : dataAvvioIstruttoria;
        long giorniDaUltimoSollecito = calcolaGiorniDa(riferimento);
        
        return giorniDaUltimoSollecito >= 4;  // Sollecito ogni 4 giorni
    }
    
    /**
     * Registra invio sollecito documenti.
     * 
     * @return messaggio sollecito generato
     */
    public String registraSollecitoInviato() {
        this.numeroSollecitiInviati++;
        this.dataUltimoSollecito = new Date();
        this.prossimoSollecitoPrevisto = aggiungiGiorni(new Date(), 4);
        this.status = Status.SOLLECITI_INVIATI;
        
        return "Gentile cliente, per procedere con la valutazione della pratica " +
               "abbiamo ancora bisogno di " + documentiMancantiCount + " documento/i. " +
               "Per favore carica i documenti richiesti nella tua area riservata. " +
               "Sollecito " + numeroSollecitiInviati + "/3.";
    }
    
    /**
     * Verifica se valutazione è in stallo (oltre 7 giorni senza documenti completi).
     * AUTOMAZIONE: Alert per operatore.
     * 
     * @return true se in stallo
     */
    public boolean isInStallo() {
        if (status != Status.DOCUMENTI_MANCANTI && status != Status.SOLLECITI_INVIATI) {
            return false;
        }
        
        long giorniDaAvvio = calcolaGiorniDa(dataAvvioIstruttoria);
        return giorniDaAvvio >= 7;
    }
    
    /**
     * Avvia valutazione quando tutti i documenti sono validati.
     */
    public void avviaValutazione() {
        this.status = Status.IN_VALUTAZIONE;
        this.dataCompletamentoDocumenti = new Date();
        this.documentiMancantiCount = 0;
    }
    
    /**
     * Completa valutazione con esito positivo.
     * 
     * @param valutatore ID utente valutatore
     * @param score Score creditizio calcolato
     */
    public void approvaValutazione(Long valutatore, Integer score, EsitoRischioCredito rischio) {
        this.status = Status.VALUTAZIONE_POSITIVA;
        this.dataValutazione = new Date();
        this.utenteValutatoreId = valutatore;
        this.scoreCreditizio = score;
        this.esitoRischioCredito = rischio;
        
        // Calcola rata massima sostenibile (30% del reddito netto)
        if (redditoMensileVerificato != null) {
            this.rataMassimaSostenibile = redditoMensileVerificato.multiply(new BigDecimal("0.30"));
        }
    }
    
    /**
     * Rifiuta valutazione.
     * 
     * @param valutatore ID utente valutatore
     * @param motivazione Motivazione rifiuto
     */
    public void rifiutaValutazione(Long valutatore, String motivazione) {
        this.status = Status.VALUTAZIONE_NEGATIVA;
        this.dataValutazione = new Date();
        this.utenteValutatoreId = valutatore;
        this.motivazioneRifiuto = motivazione;
        this.esitoRischioCredito = EsitoRischioCredito.NON_APPROVABILE;
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
     * Aggiunge giorni a una data.
     */
    private Date aggiungiGiorni(Date data, int giorni) {
        return new Date(data.getTime() + (giorni * 24L * 60 * 60 * 1000));
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
    
    public Status getStatus() {
        return status;
    }
    
    public void setStatus(Status status) {
        this.status = status;
    }
    
    public TipoValutazione getTipoValutazione() {
        return tipoValutazione;
    }
    
    public void setTipoValutazione(TipoValutazione tipoValutazione) {
        this.tipoValutazione = tipoValutazione;
    }
    
    public Date getDataAvvioIstruttoria() {
        return dataAvvioIstruttoria;
    }
    
    public void setDataAvvioIstruttoria(Date dataAvvioIstruttoria) {
        this.dataAvvioIstruttoria = dataAvvioIstruttoria;
    }
    
    public Date getDataCompletamentoDocumenti() {
        return dataCompletamentoDocumenti;
    }
    
    public void setDataCompletamentoDocumenti(Date dataCompletamentoDocumenti) {
        this.dataCompletamentoDocumenti = dataCompletamentoDocumenti;
    }
    
    public Date getDataValutazione() {
        return dataValutazione;
    }
    
    public void setDataValutazione(Date dataValutazione) {
        this.dataValutazione = dataValutazione;
    }
    
    public Long getUtenteValutatoreId() {
        return utenteValutatoreId;
    }
    
    public void setUtenteValutatoreId(Long utenteValutatoreId) {
        this.utenteValutatoreId = utenteValutatoreId;
    }
    
    public EsitoRischioCredito getEsitoRischioCredito() {
        return esitoRischioCredito;
    }
    
    public void setEsitoRischioCredito(EsitoRischioCredito esitoRischioCredito) {
        this.esitoRischioCredito = esitoRischioCredito;
    }
    
    public Integer getScoreCreditizio() {
        return scoreCreditizio;
    }
    
    public void setScoreCreditizio(Integer scoreCreditizio) {
        this.scoreCreditizio = scoreCreditizio;
    }
    
    public BigDecimal getRedditoMensileDichiarato() {
        return redditoMensileDichiarato;
    }
    
    public void setRedditoMensileDichiarato(BigDecimal redditoMensileDichiarato) {
        this.redditoMensileDichiarato = redditoMensileDichiarato;
    }
    
    public BigDecimal getRedditoMensileVerificato() {
        return redditoMensileVerificato;
    }
    
    public void setRedditoMensileVerificato(BigDecimal redditoMensileVerificato) {
        this.redditoMensileVerificato = redditoMensileVerificato;
    }
    
    public BigDecimal getRataMassimaSostenibile() {
        return rataMassimaSostenibile;
    }
    
    public void setRataMassimaSostenibile(BigDecimal rataMassimaSostenibile) {
        this.rataMassimaSostenibile = rataMassimaSostenibile;
    }
    
    public Boolean getGaranziaFideiussoriaRichiesta() {
        return garanziaFideiussoriaRichiesta;
    }
    
    public void setGaranziaFideiussoriaRichiesta(Boolean garanziaFideiussoriaRichiesta) {
        this.garanziaFideiussoriaRichiesta = garanziaFideiussoriaRichiesta;
    }
    
    public BigDecimal getImportoGaranzia() {
        return importoGaranzia;
    }
    
    public void setImportoGaranzia(BigDecimal importoGaranzia) {
        this.importoGaranzia = importoGaranzia;
    }
    
    public BigDecimal getAnticipoRichiesto() {
        return anticipoRichiesto;
    }
    
    public void setAnticipoRichiesto(BigDecimal anticipoRichiesto) {
        this.anticipoRichiesto = anticipoRichiesto;
    }
    
    public Integer getNumeroSollecitiInviati() {
        return numeroSollecitiInviati;
    }
    
    public void setNumeroSollecitiInviati(Integer numeroSollecitiInviati) {
        this.numeroSollecitiInviati = numeroSollecitiInviati;
    }
    
    public Date getDataUltimoSollecito() {
        return dataUltimoSollecito;
    }
    
    public void setDataUltimoSollecito(Date dataUltimoSollecito) {
        this.dataUltimoSollecito = dataUltimoSollecito;
    }
    
    public Date getProssimoSollecitoPrevisto() {
        return prossimoSollecitoPrevisto;
    }
    
    public void setProssimoSollecitoPrevisto(Date prossimoSollecitoPrevisto) {
        this.prossimoSollecitoPrevisto = prossimoSollecitoPrevisto;
    }
    
    public Integer getDocumentiMancantiCount() {
        return documentiMancantiCount;
    }
    
    public void setDocumentiMancantiCount(Integer documentiMancantiCount) {
        this.documentiMancantiCount = documentiMancantiCount;
    }
    
    public String getNoteValutatore() {
        return noteValutatore;
    }
    
    public void setNoteValutatore(String noteValutatore) {
        this.noteValutatore = noteValutatore;
    }
    
    public String getMotivazioneRifiuto() {
        return motivazioneRifiuto;
    }
    
    public void setMotivazioneRifiuto(String motivazioneRifiuto) {
        this.motivazioneRifiuto = motivazioneRifiuto;
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
