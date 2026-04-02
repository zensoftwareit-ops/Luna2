package it.zensoftware.luna2.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Entity
@Table(name = "tax_deadlines")
public class TaxDeadline implements Serializable {

    public enum DeadlineType {
        IVA,
        LIPE,
        CU,
        MODELLO_770,
        DICHIARAZIONE_IVA,
        DICHIARAZIONE_REDDITI,
        INPS,
        IRPEF,
        IRES,
        IRAP,
        F24,
        BOLLO,
        ALTRO
    }

    public enum DeadlineStatus {
        OPEN,
        VALIDATED,
        SUBMITTED,
        COMPLETED,
        OVERDUE
    }

    public enum Frequency {
        MONTHLY,
        QUARTERLY,
        ANNUAL,
        ONCE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DeadlineType type = DeadlineType.ALTRO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DeadlineStatus status = DeadlineStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Frequency frequency = Frequency.ONCE;

    @Temporal(TemporalType.DATE)
    @Column(name = "deadline_date", nullable = false)
    private Date deadlineDate;

    @Column(name = "amount_due", precision = 15, scale = 2)
    private BigDecimal amountDue;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "completed_at")
    private Date completedAt;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_at", updatable = false)
    private Date createdAt;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "updated_at")
    private Date updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = new Date();
        updatedAt = new Date();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = new Date();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public DeadlineType getType() { return type; }
    public void setType(DeadlineType type) { this.type = type; }
    public DeadlineStatus getStatus() { return status; }
    public void setStatus(DeadlineStatus status) { this.status = status; }
    public Frequency getFrequency() { return frequency; }
    public void setFrequency(Frequency frequency) { this.frequency = frequency; }
    public Date getDeadlineDate() { return deadlineDate; }
    public void setDeadlineDate(Date deadlineDate) { this.deadlineDate = deadlineDate; }
    public BigDecimal getAmountDue() { return amountDue; }
    public void setAmountDue(BigDecimal amountDue) { this.amountDue = amountDue; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public Date getCompletedAt() { return completedAt; }
    public void setCompletedAt(Date completedAt) { this.completedAt = completedAt; }
    public Date getCreatedAt() { return createdAt; }
    public Date getUpdatedAt() { return updatedAt; }
}