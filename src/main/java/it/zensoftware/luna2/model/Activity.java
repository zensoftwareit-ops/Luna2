package it.zensoftware.luna2.model;

import javax.persistence.*;
import java.io.Serializable;
import java.util.*;

/**
 * Activity - Traccia attività CRM (chiamate, email, note, riunioni)
 */
@Entity
@Table(name = "activity")
public class Activity implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum ActivityType {
        CALL,       // Telefonata
        EMAIL,      // Email
        MEETING,    // Riunione
        NOTE        // Nota interna
    }

    public enum ActivityStatus {
        PENDING,    // Da fare
        COMPLETED,  // Completata
        CANCELLED   // Annullata
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lead_id", nullable = false)
    private Lead lead;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActivityType tipo;

    @Column(nullable = false, length = 200)
    private String titolo;

    @Column(columnDefinition = "TEXT")
    private String descrizione;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "data_attivita")
    private Date dataAttivita;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "data_prossima_attivita")
    private Date dataProssimaAttivita;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActivityStatus stato = ActivityStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utente_id")
    private User utente;

    @Column
    private Boolean notificare = true;

    @Column(length = 100)
    private String reference_telefono;

    @Column(length = 100)
    private String reference_email;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "data_creazione", updatable = false)
    private Date dataCreazione;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "data_modifica")
    private Date dataModifica;

    // Constructors
    public Activity() {}

    public Activity(Lead lead, ActivityType tipo, String titolo) {
        this.lead = lead;
        this.tipo = tipo;
        this.titolo = titolo;
        this.dataAttivita = new Date();
        this.dataCreazione = new Date();
        this.dataModifica = new Date();
    }

    @PrePersist
    protected void onCreate() {
        if (dataCreazione == null) dataCreazione = new Date();
        if (dataModifica == null) dataModifica = new Date();
        if (dataAttivita == null) dataAttivita = new Date();
    }

    @PreUpdate
    protected void onUpdate() {
        dataModifica = new Date();
    }

    // Getters and Se tters
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

    public ActivityType getTipo() {
        return tipo;
    }

    public void setTipo(ActivityType tipo) {
        this.tipo = tipo;
    }

    public String getTitolo() {
        return titolo;
    }

    public void setTitolo(String titolo) {
        this.titolo = titolo;
    }

    public String getDescrizione() {
        return descrizione;
    }

    public void setDescrizione(String descrizione) {
        this.descrizione = descrizione;
    }

    public Date getDataAttivita() {
        return dataAttivita;
    }

    public void setDataAttivita(Date dataAttivita) {
        this.dataAttivita = dataAttivita;
    }

    public Date getDataProssimaAttivita() {
        return dataProssimaAttivita;
    }

    public void setDataProssimaAttivita(Date dataProssimaAttivita) {
        this.dataProssimaAttivita = dataProssimaAttivita;
    }

    public ActivityStatus getStato() {
        return stato;
    }

    public void setStato(ActivityStatus stato) {
        this.stato = stato;
    }

    public User getUtente() {
        return utente;
    }

    public void setUtente(User utente) {
        this.utente = utente;
    }

    public Boolean getNotificare() {
        return notificare;
    }

    public void setNotificare(Boolean notificare) {
        this.notificare = notificare;
    }

    public String getReference_telefono() {
        return reference_telefono;
    }

    public void setReference_telefono(String reference_telefono) {
        this.reference_telefono = reference_telefono;
    }

    public String getReference_email() {
        return reference_email;
    }

    public void setReference_email(String reference_email) {
        this.reference_email = reference_email;
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

    @Override
    public String toString() {
        return "Activity{" +
                "id=" + id +
                ", tipo=" + tipo +
                ", titolo='" + titolo + '\'' +
                ", stato=" + stato +
                '}';
    }
}
