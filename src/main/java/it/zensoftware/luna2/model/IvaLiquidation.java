package it.zensoftware.luna2.model;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Entity
@Table(name = "iva_liquidations")
public class IvaLiquidation implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum PeriodType {
        MONTHLY,
        QUARTERLY
    }

    public enum LiquidationStatus {
        DRAFT,
        CALCULATED,
        SUBMITTED,
        PAID,
        OVERDUE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "liquidation_period", nullable = false, length = 20)
    private String liquidationPeriod; // e.g., "2026-01" o "2026-Q1"

    @Temporal(TemporalType.DATE)
    @Column(name = "liquidation_date", nullable = false)
    private Date liquidationDate; // Data inizio periodo

    @Temporal(TemporalType.DATE)
    @Column(name = "end_date", nullable = false)
    private Date endDate; // Data fine periodo

    @Enumerated(EnumType.STRING)
    @Column(name = "period_type", nullable = false, length = 20)
    private PeriodType periodType;

    @Column(name = "iva_invoices_amount", precision = 15, scale = 2)
    private BigDecimal ivaInvoicesAmount = BigDecimal.ZERO; // IVA fatture attive (incasso)

    @Column(name = "iva_costs_amount", precision = 15, scale = 2)
    private BigDecimal ivaCostsAmount = BigDecimal.ZERO; // IVA fatture passive (deducibile)

    @Column(name = "net_iva_amount", precision = 15, scale = 2)
    private BigDecimal netIvaAmount = BigDecimal.ZERO; // IVA netta = invoices - costs

    @Column(name = "deductible_credits_amount", precision = 15, scale = 2)
    private BigDecimal deductibleCreditsAmount = BigDecimal.ZERO; // Crediti IVA da periodi precedenti

    @Column(name = "amount_due", precision = 15, scale = 2)
    private BigDecimal amountDue = BigDecimal.ZERO; // Importo netto da pagare

    @Temporal(TemporalType.DATE)
    @Column(name = "due_date")
    private Date dueDate; // Scadenza pagamento

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private LiquidationStatus status = LiquidationStatus.DRAFT;

    @Column(columnDefinition = "TEXT")
    private String notes;

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

    public String getLiquidationPeriod() { return liquidationPeriod; }
    public void setLiquidationPeriod(String liquidationPeriod) { this.liquidationPeriod = liquidationPeriod; }

    public Date getLiquidationDate() { return liquidationDate; }
    public void setLiquidationDate(Date liquidationDate) { this.liquidationDate = liquidationDate; }

    public Date getEndDate() { return endDate; }
    public void setEndDate(Date endDate) { this.endDate = endDate; }

    public PeriodType getPeriodType() { return periodType; }
    public void setPeriodType(PeriodType periodType) { this.periodType = periodType; }

    public BigDecimal getIvaInvoicesAmount() { return ivaInvoicesAmount; }
    public void setIvaInvoicesAmount(BigDecimal ivaInvoicesAmount) {
        this.ivaInvoicesAmount = ivaInvoicesAmount != null ? ivaInvoicesAmount : BigDecimal.ZERO;
    }

    public BigDecimal getIvaCostsAmount() { return ivaCostsAmount; }
    public void setIvaCostsAmount(BigDecimal ivaCostsAmount) {
        this.ivaCostsAmount = ivaCostsAmount != null ? ivaCostsAmount : BigDecimal.ZERO;
    }

    public BigDecimal getNetIvaAmount() { return netIvaAmount; }
    public void setNetIvaAmount(BigDecimal netIvaAmount) {
        this.netIvaAmount = netIvaAmount != null ? netIvaAmount : BigDecimal.ZERO;
    }

    public BigDecimal getDeductibleCreditsAmount() { return deductibleCreditsAmount; }
    public void setDeductibleCreditsAmount(BigDecimal deductibleCreditsAmount) {
        this.deductibleCreditsAmount = deductibleCreditsAmount != null ? deductibleCreditsAmount : BigDecimal.ZERO;
    }

    public BigDecimal getAmountDue() { return amountDue; }
    public void setAmountDue(BigDecimal amountDue) {
        this.amountDue = amountDue != null ? amountDue : BigDecimal.ZERO;
    }

    public Date getDueDate() { return dueDate; }
    public void setDueDate(Date dueDate) { this.dueDate = dueDate; }

    public LiquidationStatus getStatus() { return status; }
    public void setStatus(LiquidationStatus status) { this.status = status; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Date getCreatedAt() { return createdAt; }
    public Date getUpdatedAt() { return updatedAt; }

    @Override
    public String toString() {
        return "IvaLiquidation{" +
                "id=" + id +
                ", period=" + liquidationPeriod +
                ", amount=" + amountDue +
                ", status=" + status +
                '}';
    }
}
