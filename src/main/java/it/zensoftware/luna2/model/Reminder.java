package it.zensoftware.luna2.model;

import javax.persistence.*;
import java.io.Serializable;
import java.util.*;

/**
 * Reminder - Notifiche e promemoria per attività e task
 */
@Entity
@Table(name = "reminder")
public class Reminder implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum ReminderType {
        ACTIVITY,       // Notifica per attività
        TASK,          // Notifica per task
        FOLLOW_UP,     // Reminder follow-up
        BIRTHDAY,      // Compleanno cliente
        MEETING        // Riunione programmata
    }

    public enum ReminderStatus {
        PENDING,       // In sospeso
        SENT,         // Inviata
        VIEWED,       // Visualizzata
        DISMISSED     // Dismissa
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id")
    private Activity activity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id")
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lead_id", nullable = false)
    private Lead lead;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utente_id", nullable = false)
    private User utente;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "data_notifica", nullable = false)
    private Date dataNotifica;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "data_invio")
    private Date dataInvio;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "data_visualizzazione")
    private Date dataVisualizzazione;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReminderType tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReminderStatus stato = ReminderStatus.PENDING;

    @Column(length = 300)
    private String titolo;

    @Column(columnDefinition = "TEXT")
    private String messaggio;

    @Column(length = 100)
    private String canale;  // email, sms, push, sistema

    @Column(name = "letto", columnDefinition = "BOOLEAN DEFAULT false")
    private Boolean letto = false;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "data_creazione", updatable = false)
    private Date dataCreazione;

    // Constructors
    public Reminder() {}

    public Reminder(Lead lead, User utente, ReminderType tipo, String titolo, Date dataNotifica) {
        this.lead = lead;
        this.utente = utente;
        this.tipo = tipo;
        this.titolo = titolo;
        this.dataNotifica = dataNotifica;
        this.dataCreazione = new Date();
    }

    @PrePersist
    protected void onCreate() {
        if (dataCreazione == null) dataCreazione = new Date();
    }

    // Utility methods
    public boolean isScaduto() {
        return stato == ReminderStatus.PENDING && new Date().after(dataNotifica);
    }

    public long getMinutiRimanenti() {
        if (isScaduto()) return 0;
        return (dataNotifica.getTime() - System.currentTimeMillis()) / (1000 * 60);
    }

    public void markAsViewed() {
        this.stato = ReminderStatus.VIEWED;
        this.dataVisualizzazione = new Date();
        this.letto = true;
    }

    public String getReferenza() {
        if (activity != null) {
            return "Activity: " + activity.getTitolo();
        }
        if (task != null) {
            return "Task: " + task.getDescrizione();
        }
        return "Lead: " + lead.getNomeContatto();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Activity getActivity() {
        return activity;
    }

    public void setActivity(Activity activity) {
        this.activity = activity;
    }

    public Task getTask() {
        return task;
    }

    public void setTask(Task task) {
        this.task = task;
    }

    public Lead getLead() {
        return lead;
    }

    public void setLead(Lead lead) {
        this.lead = lead;
    }

    public User getUtente() {
        return utente;
    }

    public void setUtente(User utente) {
        this.utente = utente;
    }

    public Date getDataNotifica() {
        return dataNotifica;
    }

    public void setDataNotifica(Date dataNotifica) {
        this.dataNotifica = dataNotifica;
    }

    public Date getDataInvio() {
        return dataInvio;
    }

    public void setDataInvio(Date dataInvio) {
        this.dataInvio = dataInvio;
    }

    public Date getDataVisualizzazione() {
        return dataVisualizzazione;
    }

    public void setDataVisualizzazione(Date dataVisualizzazione) {
        this.dataVisualizzazione = dataVisualizzazione;
    }

    public ReminderType getTipo() {
        return tipo;
    }

    public void setTipo(ReminderType tipo) {
        this.tipo = tipo;
    }

    public ReminderStatus getStato() {
        return stato;
    }

    public void setStato(ReminderStatus stato) {
        this.stato = stato;
    }

    public String getTitolo() {
        return titolo;
    }

    public void setTitolo(String titolo) {
        this.titolo = titolo;
    }

    public String getMessaggio() {
        return messaggio;
    }

    public void setMessaggio(String messaggio) {
        this.messaggio = messaggio;
    }

    public String getCanale() {
        return canale;
    }

    public void setCanale(String canale) {
        this.canale = canale;
    }

    public Boolean getLetto() {
        return letto;
    }

    public void setLetto(Boolean letto) {
        this.letto = letto;
    }

    public Date getDataCreazione() {
        return dataCreazione;
    }

    public void setDataCreazione(Date dataCreazione) {
        this.dataCreazione = dataCreazione;
    }

    @Override
    public String toString() {
        return "Reminder{" +
                "id=" + id +
                ", tipo=" + tipo +
                ", titolo='" + titolo + '\'' +
                ", stato=" + stato +
                '}';
    }
}
