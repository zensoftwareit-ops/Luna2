package it.zensoftware.luna2.model;

import javax.persistence.*;
import java.io.Serializable;
import java.util.*;

/**
 * PipelineStage - Rappresenta uno stadio nella pipeline di vendita CRM
 */
@Entity
@Table(name = "pipeline_stage")
public class PipelineStage implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String nome;

    @Column(length = 255)
    private String descrizione;

    @Column
    private Integer sequenza;

    @Column(length = 10)
    private String colore; // Hex color for visual (es: #FF5733)

    @Column
    private Boolean isFinalStage = false; // VINTO o PERSO

    @Column
    private Boolean attivo = true;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "data_creazione", updatable = false)
    private Date dataCreazione;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "data_modifica")
    private Date dataModifica;

    @OneToMany(mappedBy = "pipelineStage", cascade = CascadeType.ALL, orphanRemoval = false)
    private List<Lead> leads = new ArrayList<>();

    // Constructors
    public PipelineStage() {}

    public PipelineStage(String nome, Integer sequenza) {
        this.nome = nome;
        this.sequenza = sequenza;
        this.dataCreazione = new Date();
        this.dataModifica = new Date();
    }

    @PrePersist
    protected void onCreate() {
        if (dataCreazione == null) dataCreazione = new Date();
        if (dataModifica == null) dataModifica = new Date();
    }

    @PreUpdate
    protected void onUpdate() {
        dataModifica = new Date();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getDescrizione() {
        return descrizione;
    }

    public void setDescrizione(String descrizione) {
        this.descrizione = descrizione;
    }

    public Integer getSequenza() {
        return sequenza;
    }

    public void setSequenza(Integer sequenza) {
        this.sequenza = sequenza;
    }

    public String getColore() {
        return colore;
    }

    public void setColore(String colore) {
        this.colore = colore;
    }

    public Boolean getIsFinalStage() {
        return isFinalStage;
    }

    public void setIsFinalStage(Boolean isFinalStage) {
        this.isFinalStage = isFinalStage;
    }

    public Boolean getAttivo() {
        return attivo;
    }

    public void setAttivo(Boolean attivo) {
        this.attivo = attivo;
    }

    public Date getDataCreazione() {
        return dataCreazione;
    }

    public void setDataCreazione(Date dataCreazione) {
        this.dataCreazione = dataCreazione;
    }

    public Date getDataModifica() {
        return dataModifica;
    }

    public void setDataModifica(Date dataModifica) {
        this.dataModifica = dataModifica;
    }

    public List<Lead> getLeads() {
        return leads;
    }

    public void setLeads(List<Lead> leads) {
        this.leads = leads;
    }

    @Override
    public String toString() {
        return "PipelineStage{" +
                "id=" + id +
                ", nome='" + nome + '\'' +
                ", sequenza=" + sequenza +
                '}';
    }
}
