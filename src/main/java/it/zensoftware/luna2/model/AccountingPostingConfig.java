package it.zensoftware.luna2.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.UniqueConstraint;
import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "accounting_posting_config",
       uniqueConstraints = @UniqueConstraint(columnNames = {"logical_key", "tax_regime", "business_form"}))
public class AccountingPostingConfig implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "logical_key", nullable = false, length = 60)
    private String logicalKey;

    @Column(name = "tax_regime", length = 30)
    private String taxRegime;

    @Column(name = "business_form", length = 30)
    private String businessForm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private AccountingAccount account;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(length = 255)
    private String description;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "updated_at")
    private Date updatedAt;

    @javax.persistence.PrePersist
    @javax.persistence.PreUpdate
    protected void onUpdate() {
        updatedAt = new Date();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getLogicalKey() { return logicalKey; }
    public void setLogicalKey(String logicalKey) { this.logicalKey = logicalKey; }
    public String getTaxRegime() { return taxRegime; }
    public void setTaxRegime(String taxRegime) { this.taxRegime = taxRegime; }
    public String getBusinessForm() { return businessForm; }
    public void setBusinessForm(String businessForm) { this.businessForm = businessForm; }
    public AccountingAccount getAccount() { return account; }
    public void setAccount(AccountingAccount account) { this.account = account; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Date getUpdatedAt() { return updatedAt; }
}