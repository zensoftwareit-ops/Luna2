package it.zensoftware.luna2.model;

import javax.persistence.*;
import java.io.Serializable;
import java.util.*;

/**
 * Task - Attività di follow-up nel CRM
 */
@Entity
@Table(name = "task")
public class Task implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum TaskPriority {
        BASSA,      // Bassa priorità
        MEDIA,      // Media priorità
        ALTA,       // Alta priorità
        URGENTE     // Urgente
    }

    public enum TaskStatus {
        OPEN,       // Aperto
        IN_PROGRESS,// In corso
        COMPLETED,  // Completato
        CANCELLED   // Annullato
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lead_id", nullable = false)
    private Lead lead;

    @Column(nullable = false, length = 300)
    private String descrizione;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "data_scadenza", nullable = false)
    private Date dataScadenza;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "data_completamento")
    private Date dataCompletamento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskPriority priorita = TaskPriority.MEDIA;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus stato = TaskStatus.OPEN;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assegnato_a")
    private User assegnatoA;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creato_da")
    private User creatoDa;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "data_creazione", updatable = false)
    private Date dataCreazione;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "data_modifica")
    private Date dataModifica;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column
    private Boolean notificare = true;

    // Constructors
    public Task() {}

    public Task(Lead lead, String descrizione, Date dataScadenza) {
        this.lead = lead;
        this.descrizione = descrizione;
        this.dataScadenza = dataScadenza;
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

    // Utility methods
    public boolean isScaduto() {
        return stato == TaskStatus.OPEN && new Date().after(dataScadenza);
    }

    public boolean isCompletato() {
        return stato == TaskStatus.COMPLETED;
    }

    public long getGiorniRimanenti() {
        if (isCompletato()) return 0;
        return (dataScadenza.getTime() - System.currentTimeMillis()) / (1000 * 60 * 60 * 24);
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

    public String getDescrizione() {
        return descrizione;
    }

    public void setDescrizione(String descrizione) {
        this.descrizione = descrizione;
    }

    public Date getDataScadenza() {
        return dataScadenza;
    }

    public void setDataScadenza(Date dataScadenza) {
        this.dataScadenza = dataScadenza;
    }

    public Date getDataCompletamento() {
        return dataCompletamento;
    }

    public void setDataCompletamento(Date dataCompletamento) {
        this.dataCompletamento = dataCompletamento;
    }

    public TaskPriority getPriorita() {
        return priorita;
    }

    public void setPriorita(TaskPriority priorita) {
        this.priorita = priorita;
    }

    public TaskStatus getStato() {
        return stato;
    }

    public void setStato(TaskStatus stato) {
        this.stato = stato;
    }

    public User getAssegnatoA() {
        return assegnatoA;
    }

    public void setAssegnatoA(User assegnatoA) {
        this.assegnatoA = assegnatoA;
    }

    public User getCreatoDa() {
        return creatoDa;
    }

    public void setCreatoDa(User creatoDa) {
        this.creatoDa = creatoDa;
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

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Boolean getNotificare() {
        return notificare;
    }

    public void setNotificare(Boolean notificare) {
        this.notificare = notificare;
    }

    @Override
    public String toString() {
        return "Task{" +
                "id=" + id +
                ", descrizione='" + descrizione + '\'' +
                ", priorita=" + priorita +
                ", stato=" + stato +
                '}';
    }
}
