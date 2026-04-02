package it.zensoftware.luna2.model;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Entity
@Table(name = "admin_operations")
public class AdminOperation implements Serializable {

    public enum Area {
        PAYROLL_NORMATIVO,
        ADEMPIMENTI_FISCALI,
        TESORERIA_RICONCILIAZIONE,
        CICLO_PASSIVO,
        CHIUSURE_CONTABILI,
        COMPLIANCE_AUDIT,
        MONITORING_KPI
    }

    public enum Status {
        DRAFT,
        READY,
        IN_PROGRESS,
        COMPLETED,
        BLOCKED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private Area area;

    @Column(nullable = false, length = 180)
    private String title;

    @Column(name = "operation_type", nullable = false, length = 60)
    private String operationType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.DRAFT;

    @Temporal(TemporalType.DATE)
    @Column(name = "due_date")
    private Date dueDate;

    @Column(name = "counterparty", length = 150)
    private String counterparty;

    @Column(name = "reference_code", length = 80)
    private String referenceCode;

    @Column(name = "amount_due", precision = 15, scale = 2)
    private BigDecimal amountDue;

    @Column(name = "owner_user_id")
    private Long ownerUserId;

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
    public Area getArea() { return area; }
    public void setArea(Area area) { this.area = area; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getOperationType() { return operationType; }
    public void setOperationType(String operationType) { this.operationType = operationType; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public Date getDueDate() { return dueDate; }
    public void setDueDate(Date dueDate) { this.dueDate = dueDate; }
    public String getCounterparty() { return counterparty; }
    public void setCounterparty(String counterparty) { this.counterparty = counterparty; }
    public String getReferenceCode() { return referenceCode; }
    public void setReferenceCode(String referenceCode) { this.referenceCode = referenceCode; }
    public BigDecimal getAmountDue() { return amountDue; }
    public void setAmountDue(BigDecimal amountDue) { this.amountDue = amountDue; }
    public Long getOwnerUserId() { return ownerUserId; }
    public void setOwnerUserId(Long ownerUserId) { this.ownerUserId = ownerUserId; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public Date getCompletedAt() { return completedAt; }
    public void setCompletedAt(Date completedAt) { this.completedAt = completedAt; }
}
