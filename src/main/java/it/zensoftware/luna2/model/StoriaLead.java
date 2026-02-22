package it.zensoftware.luna2.model;

import javax.persistence.*;
import java.io.Serializable;
import java.util.*;

/**
 * StoriaLead - Audit trail: traccia tutti i cambiamenti di stato/stage del lead
 */
@Entity
@Table(name = "storia_lead")
public class StoriaLead implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lead_id", nullable = false)
    private Lead lead;

    @Column(name = "stage_da", length = 100)
    private String stageDa;

    @Column(name = "stage_a", length = 100, nullable = false)
    private String stageA;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "data_cambio", nullable = false, updatable = false)
    private Date dataCambio;

    @Column(columnDefinition = "TEXT")
    private String motivo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utente_id")
    private User utente;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "valore_lead")
    private Double valoreLeadAlCambio;

    @Column(name = "probabilita_chiusura")
    private Integer probabilitaChiusuraAlCambio;

    // Constructors
    public StoriaLead() {}

    public StoriaLead(Lead lead, String stageDa, String stageA, String motivo) {
        this.lead = lead;
        this.stageDa = stageDa;
        this.stageA = stageA;
        this.motivo = motivo;
        this.dataCambio = new Date();
    }

    public StoriaLead(Lead lead, String stageDa, String stageA) {
        this(lead, stageDa, stageA, null);
    }

    @PrePersist
    protected void onCreate() {
        if (dataCambio == null) dataCambio = new Date();
    }

    // Utility methods
    public String getDescrizione() {
        return String.format("%s → %s", stageDa != null ? stageDa : "START", stageA);
    }

    public boolean isProgresso() {
        // Sequenza: BOZZA < QUALIFICATO < PROPOSTA < NEGOZIAZIONE < VINTO/PERSO
        int stages = 0;
        String[] pipeline = {"BOZZA", "QUALIFICATO", "PROPOSTA", "NEGOZIAZIONE", "VINTO", "PERSO"};
        int indexDa = -1, indexA = -1;
        
        for (int i = 0; i < pipeline.length; i++) {
            if (pipeline[i].equals(stageDa)) indexDa = i;
            if (pipeline[i].equals(stageA)) indexA = i;
        }
        
        if (indexDa == -1 || indexA == -1) return false;
        return indexA > indexDa;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Lead getLead() {
        return lead;
    }

    public void setLead(Lead lead) {
        this.lead = lead;
    }

    public String getStageDa() {
        return stageDa;
    }

    public void setStageDa(String stageDa) {
        this.stageDa = stageDa;
    }

    public String getStageA() {
        return stageA;
    }

    public void setStageA(String stageA) {
        this.stageA = stageA;
    }

    public Date getDataCambio() {
        return dataCambio;
    }

    public void setDataCambio(Date dataCambio) {
        this.dataCambio = dataCambio;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public User getUtente() {
        return utente;
    }

    public void setUtente(User utente) {
        this.utente = utente;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Double getValoreLeadAlCambio() {
        return valoreLeadAlCambio;
    }

    public void setValoreLeadAlCambio(Double valoreLeadAlCambio) {
        this.valoreLeadAlCambio = valoreLeadAlCambio;
    }

    public Integer getProbabilitaChiusuraAlCambio() {
        return probabilitaChiusuraAlCambio;
    }

    public void setProbabilitaChiusuraAlCambio(Integer probabilitaChiusuraAlCambio) {
        this.probabilitaChiusuraAlCambio = probabilitaChiusuraAlCambio;
    }

    @Override
    public String toString() {
        return "StoriaLead{" +
                "id=" + id +
                ", descrizione='" + getDescrizione() + '\'' +
                ", dataCambio=" + dataCambio +
                '}';
    }
}
