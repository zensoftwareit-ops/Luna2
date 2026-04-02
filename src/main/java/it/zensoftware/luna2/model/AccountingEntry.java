package it.zensoftware.luna2.model;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "accounting_entries")
public class AccountingEntry implements Serializable {

    public enum EntryType {
        GENERALE,
        FATTURA_ATTIVA,
        FATTURA_PASSIVA,
        INCASSO,
        PAGAMENTO,
        TRIBUTO,
        RETRIBUZIONI,
        ASSESTAMENTO,
        CHIUSURA,
        APERTURA
    }

    public enum EntryStatus {
        DRAFT,
        POSTED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "protocol_number", nullable = false, unique = true, length = 30)
    private String protocolNumber;

    @Temporal(TemporalType.DATE)
    @Column(name = "entry_date", nullable = false)
    private Date entryDate;

    @Temporal(TemporalType.DATE)
    @Column(name = "competence_date")
    private Date competenceDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EntryType type = EntryType.GENERALE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EntryStatus status = EntryStatus.DRAFT;

    @Column(nullable = false, length = 255)
    private String description;

    @Column(name = "document_number", length = 60)
    private String documentNumber;

    @Column(name = "source_type", length = 30)
    private String sourceType;

    @Column(name = "source_id")
    private Long sourceId;

    @Column(length = 150)
    private String counterparty;

    @Column(name = "total_debit", precision = 15, scale = 2, nullable = false)
    private BigDecimal totalDebit = BigDecimal.ZERO;

    @Column(name = "total_credit", precision = 15, scale = 2, nullable = false)
    private BigDecimal totalCredit = BigDecimal.ZERO;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @OneToMany(mappedBy = "entry", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<AccountingEntryLine> lines = new ArrayList<>();

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

    public void addLine(AccountingEntryLine line) {
        line.setEntry(this);
        lines.add(line);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getProtocolNumber() { return protocolNumber; }
    public void setProtocolNumber(String protocolNumber) { this.protocolNumber = protocolNumber; }
    public Date getEntryDate() { return entryDate; }
    public void setEntryDate(Date entryDate) { this.entryDate = entryDate; }
    public Date getCompetenceDate() { return competenceDate; }
    public void setCompetenceDate(Date competenceDate) { this.competenceDate = competenceDate; }
    public EntryType getType() { return type; }
    public void setType(EntryType type) { this.type = type; }
    public EntryStatus getStatus() { return status; }
    public void setStatus(EntryStatus status) { this.status = status; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getDocumentNumber() { return documentNumber; }
    public void setDocumentNumber(String documentNumber) { this.documentNumber = documentNumber; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public Long getSourceId() { return sourceId; }
    public void setSourceId(Long sourceId) { this.sourceId = sourceId; }
    public String getCounterparty() { return counterparty; }
    public void setCounterparty(String counterparty) { this.counterparty = counterparty; }
    public BigDecimal getTotalDebit() { return totalDebit; }
    public void setTotalDebit(BigDecimal totalDebit) { this.totalDebit = totalDebit; }
    public BigDecimal getTotalCredit() { return totalCredit; }
    public void setTotalCredit(BigDecimal totalCredit) { this.totalCredit = totalCredit; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }
    public List<AccountingEntryLine> getLines() { return lines; }
    public void setLines(List<AccountingEntryLine> lines) { this.lines = lines; }
    public Date getCreatedAt() { return createdAt; }
    public Date getUpdatedAt() { return updatedAt; }
}