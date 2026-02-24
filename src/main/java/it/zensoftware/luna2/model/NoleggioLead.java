package it.zensoftware.luna2.model;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Entity NoleggioLead - Estensione Lead CRM per broker noleggio auto
 * Requisito: Modulo CRM attivo
 * 
 * @author Luna2 Team
 * @version 1.0
 */
@Entity
@Table(name = "noleggio_leads")
public class NoleggioLead implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Riferimento al Lead CRM principale
     */
    @Column(name = "lead_id", nullable = false)
    private Long leadId;

    /**
     * Origine Lead
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "origine_tipo", nullable = false, length = 20)
    private OrigineTipo origineTipo;

    @Column(name = "segnalatore_id")
    private Long segnalatoreId;  // FK to clienti

    @Column(name = "commissione_segnalatore", precision = 5, scale = 2)
    private BigDecimal commissioneSegnalatore;  // % commissione (es: 5.50 = 5.5%)

    /**
     * Esigenze Cliente
     */
    @Column(name = "modello_richiesto", length = 200)
    private String modelloRichiesto;

    @Column(name = "budget_mensile", precision = 10, scale = 2)
    private BigDecimal budgetMensile;

    @Column(name = "durata_mesi")
    private Integer durataMesi;

    @Column(name = "km_annui")
    private Integer kmAnnui;

    /**
     * Tipo Noleggio
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_noleggio", nullable = false, length = 20)
    private TipoNoleggio tipoNoleggio;

    /**
     * Fase attuale del processo (workflow)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "fase", nullable = false, length = 30)
    private Fase fase;

    /**
     * Timestamps
     */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "data_creazione", nullable = false, updatable = false)
    private Date dataCreazione;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "data_ultima_modifica")
    private Date dataUltimaModifica;

    // =========================================================================
    // ENUMS
    // =========================================================================

    public enum OrigineTipo {
        DIRETTO,
        SEGNALATORE
    }

    public enum TipoNoleggio {
        LUNGO_TERMINE,
        BREVE_TERMINE
    }

    public enum Fase {
        PREVENTIVAZIONE,   // Fase 1: Acquisizione e preventivazione
        ISTRUTTORIA,       // Fase 2: Raccolta documenti e valutazione
        ORDINE,            // Fase 3: Ordine e logistica
        POST_VENDITA,      // Fase 4: Assistenza e ticketing
        SCADENZARIO        // Fase 5: Gestione contratto e rinnovi
    }

    // =========================================================================
    // LIFECYCLE CALLBACKS
    // =========================================================================

    @PrePersist
    protected void onCreate() {
        dataCreazione = new Date();
        dataUltimaModifica = new Date();
    }

    @PreUpdate
    protected void onUpdate() {
        dataUltimaModifica = new Date();
    }

    // =========================================================================
    // BUSINESS METHODS
    // =========================================================================

    /**
     * Verifica se il lead ha un segnalatore
     */
    public boolean hasSegnalatore() {
        return origineTipo == OrigineTipo.SEGNALATORE && segnalatoreId != null;
    }

    /**
     * Calcola la commissione in euro (se presente)
     */
    public BigDecimal calcolaCommissioneEuro() {
        if (commissioneSegnalatore == null || budgetMensile == null || durataMesi == null) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal valoreContratto = budgetMensile.multiply(new BigDecimal(durataMesi));
        return valoreContratto.multiply(commissioneSegnalatore).divide(new BigDecimal(100));
    }

    /**
     * Verifica se è un noleggio lungo termine
     */
    public boolean isLungoTermine() {
        return tipoNoleggio == TipoNoleggio.LUNGO_TERMINE;
    }

    /**
     * Avanza alla fase successiva
     */
    public void avanzaFase() {
        switch (fase) {
            case PREVENTIVAZIONE:
                fase = Fase.ISTRUTTORIA;
                break;
            case ISTRUTTORIA:
                fase = Fase.ORDINE;
                break;
            case ORDINE:
                fase = Fase.POST_VENDITA;
                break;
            case POST_VENDITA:
                fase = Fase.SCADENZARIO;
                break;
            case SCADENZARIO:
                // Finale, non avanza
                break;
        }
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

    public Long getLeadId() {
        return leadId;
    }

    public void setLeadId(Long leadId) {
        this.leadId = leadId;
    }

    public OrigineTipo getOrigineTipo() {
        return origineTipo;
    }

    public void setOrigineTipo(OrigineTipo origineTipo) {
        this.origineTipo = origineTipo;
    }

    public Long getSegnalatoreId() {
        return segnalatoreId;
    }

    public void setSegnalatoreId(Long segnalatoreId) {
        this.segnalatoreId = segnalatoreId;
    }

    public BigDecimal getCommissioneSegnalatore() {
        return commissioneSegnalatore;
    }

    public void setCommissioneSegnalatore(BigDecimal commissioneSegnalatore) {
        this.commissioneSegnalatore = commissioneSegnalatore;
    }

    public String getModelloRichiesto() {
        return modelloRichiesto;
    }

    public void setModelloRichiesto(String modelloRichiesto) {
        this.modelloRichiesto = modelloRichiesto;
    }

    public BigDecimal getBudgetMensile() {
        return budgetMensile;
    }

    public void setBudgetMensile(BigDecimal budgetMensile) {
        this.budgetMensile = budgetMensile;
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

    public TipoNoleggio getTipoNoleggio() {
        return tipoNoleggio;
    }

    public void setTipoNoleggio(TipoNoleggio tipoNoleggio) {
        this.tipoNoleggio = tipoNoleggio;
    }

    public Fase getFase() {
        return fase;
    }

    public void setFase(Fase fase) {
        this.fase = fase;
    }

    public Date getDataCreazione() {
        return dataCreazione;
    }

    public void setDataCreazione(Date dataCreazione) {
        this.dataCreazione = dataCreazione;
    }

    public Date getDataUltimaModifica() {
        return dataUltimaModifica;
    }

    public void setDataUltimaModifica(Date dataUltimaModifica) {
        this.dataUltimaModifica = dataUltimaModifica;
    }

    @Override
    public String toString() {
        return "NoleggioLead{" +
                "id=" + id +
                ", leadId=" + leadId +
                ", origineTipo=" + origineTipo +
                ", tipoNoleggio=" + tipoNoleggio +
                ", fase=" + fase +
                ", modelloRichiesto='" + modelloRichiesto + '\'' +
                ", budgetMensile=" + budgetMensile +
                ", durataMesi=" + durataMesi +
                '}';
    }
}
