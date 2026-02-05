package it.zensoftware.luna2.model;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.*;

/**
 * Entity class for Lead
 */
@Entity
@Table(name = "lead")
public class Lead implements Serializable {
    
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Origine origine;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Stato stato = Stato.NUOVO;

    @Column(nullable = false)
    private String azienda;

    @Column(name = "nome_contatto", length = 100)
    private String nomeContatto;

    @Column(name = "cognome_contatto", length = 100)
    private String cognomeContatto;

    @Column(length = 30)
    private String telefono;

    @Column(length = 100)
    private String email;

    private String indirizzo;

    @Column(length = 100)
    private String citta;

    @Column(length = 2)
    private String provincia;

    @Column(length = 10)
    private String cap;

    @Column(columnDefinition = "TEXT")
    private String esigenza;

    @Column(name = "budget_stimato", precision = 15, scale = 2)
    private BigDecimal budgetStimato;

    @Column(name = "probabilita_chiusura")
    private Integer probabilitaChiusura = 0;

    @Temporal(TemporalType.DATE)
    @Column(name = "data_contatto")
    private Date dataContatto;

    @Temporal(TemporalType.DATE)
    @Column(name = "data_prossimo_followup")
    private Date dataProssimoFollowup;

    @Column(columnDefinition = "TEXT")
    private String note;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
        name = "lead_tag",
        joinColumns = @JoinColumn(name = "lead_id"),
        inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<Tag> tags = new HashSet<>();

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "data_creazione", updatable = false)
    private Date dataCreazione;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "data_modifica")
    private Date dataModifica;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "modified_by")
    private User modifiedBy;

    public enum Origine {
        FIERA, CAMPAGNA, PASSAPAROLA, WEBSITE, EMAIL, TELEFONO, ALTRO
    }

    public enum Stato {
        NUOVO, CONTATTATO, QUALIFICATO, PREVENTIVO, NEGOZIAZIONE, VINTO, PERSO
    }

    @PrePersist
    protected void onCreate() {
        dataCreazione = new Date();
        dataModifica = new Date();
    }

    @PreUpdate
    protected void onUpdate() {
        dataModifica = new Date();
    }

    // Constructors
    public Lead() {}

    public Lead(String azienda, Origine origine) {
        this.azienda = azienda;
        this.origine = origine;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Origine getOrigine() {
        return origine;
    }

    public void setOrigine(Origine origine) {
        this.origine = origine;
    }

    public Stato getStato() {
        return stato;
    }

    public void setStato(Stato stato) {
        this.stato = stato;
    }

    public String getAzienda() {
        return azienda;
    }

    public void setAzienda(String azienda) {
        this.azienda = azienda;
    }

    public String getNomeContatto() {
        return nomeContatto;
    }

    public void setNomeContatto(String nomeContatto) {
        this.nomeContatto = nomeContatto;
    }

    public String getCognomeContatto() {
        return cognomeContatto;
    }

    public void setCognomeContatto(String cognomeContatto) {
        this.cognomeContatto = cognomeContatto;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getIndirizzo() {
        return indirizzo;
    }

    public void setIndirizzo(String indirizzo) {
        this.indirizzo = indirizzo;
    }

    public String getCitta() {
        return citta;
    }

    public void setCitta(String citta) {
        this.citta = citta;
    }

    public String getProvincia() {
        return provincia;
    }

    public void setProvincia(String provincia) {
        this.provincia = provincia;
    }

    public String getCap() {
        return cap;
    }

    public void setCap(String cap) {
        this.cap = cap;
    }

    public String getEsigenza() {
        return esigenza;
    }

    public void setEsigenza(String esigenza) {
        this.esigenza = esigenza;
    }

    public BigDecimal getBudgetStimato() {
        return budgetStimato;
    }

    public void setBudgetStimato(BigDecimal budgetStimato) {
        this.budgetStimato = budgetStimato;
    }

    public Integer getProbabilitaChiusura() {
        return probabilitaChiusura;
    }

    public void setProbabilitaChiusura(Integer probabilitaChiusura) {
        this.probabilitaChiusura = probabilitaChiusura;
    }

    public Date getDataContatto() {
        return dataContatto;
    }

    public void setDataContatto(Date dataContatto) {
        this.dataContatto = dataContatto;
    }

    public Date getDataProssimoFollowup() {
        return dataProssimoFollowup;
    }

    public void setDataProssimoFollowup(Date dataProssimoFollowup) {
        this.dataProssimoFollowup = dataProssimoFollowup;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }

    public Set<Tag> getTags() {
        return tags;
    }

    public void setTags(Set<Tag> tags) {
        this.tags = tags;
    }

    public void addTag(Tag tag) {
        tags.add(tag);
        tag.getLeads().add(this);
    }

    public void removeTag(Tag tag) {
        tags.remove(tag);
        tag.getLeads().remove(this);
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

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public User getModifiedBy() {
        return modifiedBy;
    }

    public void setModifiedBy(User modifiedBy) {
        this.modifiedBy = modifiedBy;
    }

    public String getNomeContattoCompleto() {
        if (nomeContatto != null && cognomeContatto != null) {
            return nomeContatto + " " + cognomeContatto;
        }
        return "";
    }

    @Override
    public String toString() {
        return "Lead{" +
                "id=" + id +
                ", azienda='" + azienda + '\'' +
                ", stato=" + stato +
                ", origine=" + origine +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Lead)) return false;
        Lead lead = (Lead) o;
        return id != null && id.equals(lead.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
