package it.zensoftware.luna2.model;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

/**
 * Entity per i magazzini fisici (multi-warehouse support)
 */
@Entity
@Table(name = "warehouses")
public class Warehouse implements Serializable {
    
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String nome;

    @Column(length = 50)
    private String codice;

    @Column(columnDefinition = "TEXT")
    private String indirizzo;

    @Column(length = 50)
    private String citta;

    @Column(length = 10)
    private String cap;

    @Column(length = 50)
    private String provincia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private User manager;

    @Column(nullable = false)
    private Boolean attivo = true;

    @Column(nullable = false)
    private Boolean principale = false;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "data_creazione")
    private Date dataCreazione;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "data_modifica")
    private Date dataModifica;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    // Costruttori
    public Warehouse() {
        this.dataCreazione = new Date();
    }

    public Warehouse(String nome) {
        this();
        this.nome = nome;
    }

    // Metodi di utilità
    @PreUpdate
    protected void onUpdate() {
        this.dataModifica = new Date();
    }

    public String getIndirizzoCompleto() {
        StringBuilder sb = new StringBuilder();
        if (indirizzo != null && !indirizzo.isEmpty()) {
            sb.append(indirizzo);
        }
        if (citta != null && !citta.isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(citta);
        }
        if (provincia != null && !provincia.isEmpty()) {
            if (sb.length() > 0) sb.append(" (").append(provincia).append(")");
        }
        if (cap != null && !cap.isEmpty()) {
            if (sb.length() > 0) sb.append(" - ");
            sb.append(cap);
        }
        return sb.toString();
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

    public String getCodice() {
        return codice;
    }

    public void setCodice(String codice) {
        this.codice = codice;
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

    public String getCap() {
        return cap;
    }

    public void setCap(String cap) {
        this.cap = cap;
    }

    public String getProvincia() {
        return provincia;
    }

    public void setProvincia(String provincia) {
        this.provincia = provincia;
    }

    public User getManager() {
        return manager;
    }

    public void setManager(User manager) {
        this.manager = manager;
    }

    public Boolean getAttivo() {
        return attivo;
    }

    public void setAttivo(Boolean attivo) {
        this.attivo = attivo;
    }

    public Boolean getPrincipale() {
        return principale;
    }

    public boolean isPrincipale() {
        return principale != null && principale;
    }

    public void setPrincipale(Boolean principale) {
        this.principale = principale;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
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

    @Override
    public String toString() {
        return "Warehouse{" +
                "id=" + id +
                ", nome='" + nome + '\'' +
                ", codice='" + codice + '\'' +
                ", attivo=" + attivo +
                ", principale=" + principale +
                '}';
    }
}
