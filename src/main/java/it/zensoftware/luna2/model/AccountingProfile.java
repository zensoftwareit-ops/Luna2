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
import java.util.Date;

@Entity
@Table(name = "accounting_profiles")
public class AccountingProfile implements Serializable {

    public enum BusinessForm {
        PROFESSIONISTA,
        DITTA_INDIVIDUALE,
        SOCIETA_SEMPLICE,
        SRL
    }

    public enum TaxRegime {
        FORFETTARIO,
        ORDINARIO
    }

    public enum VatFrequency {
        MENSILE,
        TRIMESTRALE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_name", nullable = false, length = 150)
    private String companyName;

    @Column(name = "vat_number", length = 30)
    private String vatNumber;

    @Column(name = "tax_code", length = 30)
    private String taxCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "business_form", nullable = false, length = 30)
    private BusinessForm businessForm = BusinessForm.PROFESSIONISTA;

    @Enumerated(EnumType.STRING)
    @Column(name = "tax_regime", nullable = false, length = 20)
    private TaxRegime taxRegime = TaxRegime.FORFETTARIO;

    @Enumerated(EnumType.STRING)
    @Column(name = "vat_frequency", nullable = false, length = 20)
    private VatFrequency vatFrequency = VatFrequency.TRIMESTRALE;

    @Column(name = "fiscal_year_start_month", nullable = false)
    private Integer fiscalYearStartMonth = 1;

    @Column(name = "contribution_rate", precision = 8, scale = 2)
    private java.math.BigDecimal contributionRate;

    @Column(name = "substitute_tax_rate", precision = 8, scale = 2)
    private java.math.BigDecimal substituteTaxRate;

    @Column(name = "accountant_email", length = 150)
    private String accountantEmail;

    @Column(name = "pec_email", length = 150)
    private String pecEmail;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "logo_path", length = 500)
    private String logoPath;

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
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public String getVatNumber() { return vatNumber; }
    public void setVatNumber(String vatNumber) { this.vatNumber = vatNumber; }
    public String getTaxCode() { return taxCode; }
    public void setTaxCode(String taxCode) { this.taxCode = taxCode; }
    public BusinessForm getBusinessForm() { return businessForm; }
    public void setBusinessForm(BusinessForm businessForm) { this.businessForm = businessForm; }
    public TaxRegime getTaxRegime() { return taxRegime; }
    public void setTaxRegime(TaxRegime taxRegime) { this.taxRegime = taxRegime; }
    public VatFrequency getVatFrequency() { return vatFrequency; }
    public void setVatFrequency(VatFrequency vatFrequency) { this.vatFrequency = vatFrequency; }
    public Integer getFiscalYearStartMonth() { return fiscalYearStartMonth; }
    public void setFiscalYearStartMonth(Integer fiscalYearStartMonth) { this.fiscalYearStartMonth = fiscalYearStartMonth; }
    public java.math.BigDecimal getContributionRate() { return contributionRate; }
    public void setContributionRate(java.math.BigDecimal contributionRate) { this.contributionRate = contributionRate; }
    public java.math.BigDecimal getSubstituteTaxRate() { return substituteTaxRate; }
    public void setSubstituteTaxRate(java.math.BigDecimal substituteTaxRate) { this.substituteTaxRate = substituteTaxRate; }
    public String getAccountantEmail() { return accountantEmail; }
    public void setAccountantEmail(String accountantEmail) { this.accountantEmail = accountantEmail; }
    public String getPecEmail() { return pecEmail; }
    public void setPecEmail(String pecEmail) { this.pecEmail = pecEmail; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public String getLogoPath() { return logoPath; }
    public void setLogoPath(String logoPath) { this.logoPath = logoPath; }
    public Date getCreatedAt() { return createdAt; }
    public Date getUpdatedAt() { return updatedAt; }
}