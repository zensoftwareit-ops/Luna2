package it.zensoftware.luna2.model;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Entity
@Table(name = "payroll_employee_configs")
public class PayrollEmployeeConfig implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "hourly_rate", precision = 10, scale = 2, nullable = false)
    private BigDecimal hourlyRate = BigDecimal.valueOf(15);

    @Column(name = "overtime_rate_multiplier", precision = 6, scale = 2, nullable = false)
    private BigDecimal overtimeRateMultiplier = BigDecimal.valueOf(1.30);

    @Column(name = "tax_rate", precision = 5, scale = 4, nullable = false)
    private BigDecimal taxRate = BigDecimal.valueOf(0.20);

    @Column(name = "social_security_rate", precision = 5, scale = 4, nullable = false)
    private BigDecimal socialSecurityRate = BigDecimal.valueOf(0.0919);

    @Column(name = "insurance_rate", precision = 5, scale = 4, nullable = false)
    private BigDecimal insuranceRate = BigDecimal.valueOf(0.0100);

    @Column(name = "fixed_allowance", precision = 10, scale = 2, nullable = false)
    private BigDecimal fixedAllowance = BigDecimal.ZERO;

    @Column(name = "fixed_deduction", precision = 10, scale = 2, nullable = false)
    private BigDecimal fixedDeduction = BigDecimal.ZERO;

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
    public Long getCompanyId() { return companyId; }
    public void setCompanyId(Long companyId) { this.companyId = companyId; }
    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
    public BigDecimal getHourlyRate() { return hourlyRate; }
    public void setHourlyRate(BigDecimal hourlyRate) { this.hourlyRate = hourlyRate; }
    public BigDecimal getOvertimeRateMultiplier() { return overtimeRateMultiplier; }
    public void setOvertimeRateMultiplier(BigDecimal overtimeRateMultiplier) { this.overtimeRateMultiplier = overtimeRateMultiplier; }
    public BigDecimal getTaxRate() { return taxRate; }
    public void setTaxRate(BigDecimal taxRate) { this.taxRate = taxRate; }
    public BigDecimal getSocialSecurityRate() { return socialSecurityRate; }
    public void setSocialSecurityRate(BigDecimal socialSecurityRate) { this.socialSecurityRate = socialSecurityRate; }
    public BigDecimal getInsuranceRate() { return insuranceRate; }
    public void setInsuranceRate(BigDecimal insuranceRate) { this.insuranceRate = insuranceRate; }
    public BigDecimal getFixedAllowance() { return fixedAllowance; }
    public void setFixedAllowance(BigDecimal fixedAllowance) { this.fixedAllowance = fixedAllowance; }
    public BigDecimal getFixedDeduction() { return fixedDeduction; }
    public void setFixedDeduction(BigDecimal fixedDeduction) { this.fixedDeduction = fixedDeduction; }
}
