package it.zensoftware.luna2.model;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Entity NoleggioPreventivo - Gestione preventivi noleggio auto
 * Supporta versioning per tracking revisioni
 * 
 * @author Luna2 Team
 * @version 1.0
 */
@Entity
@Table(name = "noleggio_preventivi")
public class NoleggioPreventivo implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "noleggio_lead_id", nullable = false)
    private Long noleggioLeadId;

    /**
     * Dati Preventivo
     */
    @Column(name = "numero_preventivo", unique = true, length = 50)
    private String numeroPreventivo;

    @Column(name = "versione", nullable = false)
    private Integer versione = 1;

    /**
     * Dettagli Auto
     */
    @Column(name = "marca", length = 100)
    private String marca;

    @Column(name = "modello", length = 200)
    private String modello;

    @Column(name = "allestimento", length = 200)
    private String allestimento;

    @Column(name = "alimentazione", length = 50)
    private String alimentazione;

    /**
     * Condizioni Economiche
     */
    @Column(name = "canone_mensile", precision = 10, scale = 2)
    private BigDecimal canoneMensile;

    @Column(name = "anticipo", precision = 10, scale = 2)
    private BigDecimal anticipo;

    @Column(name = "durata_mesi")
    private Integer durataMesi;

    @Column(name = "km_annui")
    private Integer kmAnnui;

    @Column(name = "km_extra_costo", precision = 6, scale = 3)
    private BigDecimal kmExtraCosto;  // € per km extra

    /**
     * Servizi Inclusi (JSON array)
     * Es: ["Manutenzione Ordinaria", "Bollo Auto", "Assicurazione RCA", "Assistenza Stradale"]
     */
    @Column(name = "servizi_inclusi", columnDefinition = "TEXT")
    private String serviziInclusi;

    /**
     * Status Preventivo
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status;

    @Column(name = "motivo_rifiuto", columnDefinition = "TEXT")
    private String motivoRifiuto;

    /**
     * File PDF preventivo
     */
    @Column(name = "file_path", length = 500)
    private String filePath;

    /**
     * Dates
     */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "data_elaborazione")
    private Date dataElaborazione;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "data_invio")
    private Date dataInvio;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "data_risposta")
    private Date dataRisposta;

    @Temporal(TemporalType.DATE)
    @Column(name = "scadenza_validita")
    private Date scadenzaValidita;

    /**
     * Tracking
     */
    @Column(name = "utente_creazione_id")
    private Long utenteCreazioneId;

    // =========================================================================
    // ENUMS
    // =========================================================================

    public enum Status {
        BOZZA,           // Preventivo in preparazione
        INVIATO,         // Inviato al cliente (trigger automation: follow-up 48h)
        ACCETTATO,       // Cliente ha accettato
        RIFIUTATO,       // Cliente ha rifiutato (trigger: proporre nuova soluzione)
        SOSTITUITO       // Sostituito da versione successiva
    }

    // =========================================================================
    // LIFECYCLE CALLBACKS
    // =========================================================================

    @PrePersist
    protected void onCreate() {
        if (dataElaborazione == null) {
            dataElaborazione = new Date();
        }
        if (versione == null) {
            versione = 1;
        }
    }

    // =========================================================================
    // BUSINESS METHODS
    // =========================================================================

    /**
     * Genera numero preventivo univoco
     * Formato: PREV-YYYYMM-XXXXX
     */
    public void generaNumeroPreventivo() {
        // Formato: PREV-202602-00001
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyyMM");
        String yearMonth = sdf.format(new Date());
        
        // Il numero sequenziale verrà generato dal service in base all'ultimo numero del mese
        this.numeroPreventivo = "PREV-" + yearMonth + "-" + 
            String.format("%05d", System.nanoTime() % 100000);
    }

    /**
     * Calcola il costo totale del contratto
     */
    public BigDecimal calcolaCostoTotale() {
        if (canoneMensile == null || durataMesi == null) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal costoCanoni = canoneMensile.multiply(new BigDecimal(durataMesi));
        BigDecimal costoAnticipo = anticipo != null ? anticipo : BigDecimal.ZERO;
        
        return costoCanoni.add(costoAnticipo);
    }

    /**
     * Verifica se il preventivo è scaduto
     */
    public boolean isScaduto() {
        if (scadenzaValidita == null) {
            return false;
        }
        return scadenzaValidita.before(new Date());
    }

    /**
     * Verifica se il preventivo è in attesa di risposta
     */
    public boolean isInAttesaRisposta() {
        return status == Status.INVIATO && dataRisposta == null;
    }

    /**
     * Calcola i giorni trascorsi dall'invio
     */
    public long giorniDaInvio() {
        if (dataInvio == null) {
            return 0;
        }
        
        long diffMillis = new Date().getTime() - dataInvio.getTime();
        return diffMillis / (24 * 60 * 60 * 1000);
    }

    /**
     * Verifica se serve follow-up (48h passate)
     */
    public boolean richiedeFollowUp() {
        return isInAttesaRisposta() && giorniDaInvio() >= 2;
    }

    /**
     * Marca come inviato
     */
    public void marcaInviato() {
        this.status = Status.INVIATO;
        this.dataInvio = new Date();
    }

    /**
     * Marca come accettato
     */
    public void marcaAccettato() {
        this.status = Status.ACCETTATO;
        this.dataRisposta = new Date();
    }

    /**
     * Marca come rifiutato
     */
    public void marcaRifiutato(String motivo) {
        this.status = Status.RIFIUTATO;
        this.motivoRifiuto = motivo;
        this.dataRisposta = new Date();
    }

    // =========================================================================
    // GETTERS & SETTERS
    // =========================================================================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getNoleggioLeadId() {
        return noleggioLeadId;
    }

    public void setNoleggioLeadId(Long noleggioLeadId) {
        this.noleggioLeadId = noleggioLeadId;
    }

    public String getNumeroPreventivo() {
        return numeroPreventivo;
    }

    public void setNumeroPreventivo(String numeroPreventivo) {
        this.numeroPreventivo = numeroPreventivo;
    }

    public Integer getVersione() {
        return versione;
    }

    public void setVersione(Integer versione) {
        this.versione = versione;
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

    public String getAlimentazione() {
        return alimentazione;
    }

    public void setAlimentazione(String alimentazione) {
        this.alimentazione = alimentazione;
    }

    public BigDecimal getCanoneMensile() {
        return canoneMensile;
    }

    public void setCanoneMensile(BigDecimal canoneMensile) {
        this.canoneMensile = canoneMensile;
    }

    public BigDecimal getAnticipo() {
        return anticipo;
    }

    public void setAnticipo(BigDecimal anticipo) {
        this.anticipo = anticipo;
    }

    public Integer getDurataMesi() {
        return durataMesi;
    }

    public void setDurataMesi(Integer durataMesi) {
        this.durataMesi = durataMesi;
    }

    public Integer getKmAnnui() {
        return kmAnnui;
    }

    public void setKmAnnui(Integer kmAnnui) {
        this.kmAnnui = kmAnnui;
    }

    public BigDecimal getKmExtraCosto() {
        return kmExtraCosto;
    }

    public void setKmExtraCosto(BigDecimal kmExtraCosto) {
        this.kmExtraCosto = kmExtraCosto;
    }

    public String getServiziInclusi() {
        return serviziInclusi;
    }

    public void setServiziInclusi(String serviziInclusi) {
        this.serviziInclusi = serviziInclusi;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getMotivoRifiuto() {
        return motivoRifiuto;
    }

    public void setMotivoRifiuto(String motivoRifiuto) {
        this.motivoRifiuto = motivoRifiuto;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public Date getDataElaborazione() {
        return dataElaborazione;
    }

    public void setDataElaborazione(Date dataElaborazione) {
        this.dataElaborazione = dataElaborazione;
    }

    public Date getDataInvio() {
        return dataInvio;
    }

    public void setDataInvio(Date dataInvio) {
        this.dataInvio = dataInvio;
    }

    public Date getDataRisposta() {
        return dataRisposta;
    }

    public void setDataRisposta(Date dataRisposta) {
        this.dataRisposta = dataRisposta;
    }

    public Date getScadenzaValidita() {
        return scadenzaValidita;
    }

    public void setScadenzaValidita(Date scadenzaValidita) {
        this.scadenzaValidita = scadenzaValidita;
    }

public Long getUtenteCreazioneId() {
        return utenteCreazioneId;
    }
    
    public void setUtenteCreazioneId(Long utenteCreazioneId) {
        this.utenteCreazioneId = utenteCreazioneId;
    }

    @Override
    public String toString() {
        return "NoleggioPreventivo{" +
                "id=" + id +
                ", numeroPreventivo='" + numeroPreventivo + '\'' +
                ", versione=" + versione +
                ", marca='" + marca + '\'' +
                ", modello='" + modello + '\'' +
                ", canoneMensile=" + canoneMensile +
                ", status=" + status +
                '}';
    }
}
