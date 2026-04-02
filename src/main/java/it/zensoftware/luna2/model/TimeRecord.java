package it.zensoftware.luna2.model;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Entity
@Table(name = "time_records", uniqueConstraints = {
        @UniqueConstraint(name = "uk_employee_date", columnNames = {"employee_id", "record_date"})
})
public class TimeRecord implements Serializable {

    public enum AbsenceType {
        NONE,
        FERIE,
        PERMESSO,
        MALATTIA
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Temporal(TemporalType.DATE)
    @Column(name = "record_date", nullable = false)
    private Date recordDate;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "check_in_time")
    private Date checkInTime;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "check_out_time")
    private Date checkOutTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "absence_type", nullable = false, length = 20)
    private AbsenceType absenceType = AbsenceType.NONE;

    @Column(name = "hours_worked", precision = 5, scale = 2)
    private BigDecimal hoursWorked = BigDecimal.ZERO;

    @Column(name = "hours_overtime", precision = 5, scale = 2)
    private BigDecimal hoursOvertime = BigDecimal.ZERO;

    @Column(name = "notes", length = 500)
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
    public Long getCompanyId() { return companyId; }
    public void setCompanyId(Long companyId) { this.companyId = companyId; }
    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
    public Date getRecordDate() { return recordDate; }
    public void setRecordDate(Date recordDate) { this.recordDate = recordDate; }
    public Date getCheckInTime() { return checkInTime; }
    public void setCheckInTime(Date checkInTime) { this.checkInTime = checkInTime; }
    public Date getCheckOutTime() { return checkOutTime; }
    public void setCheckOutTime(Date checkOutTime) { this.checkOutTime = checkOutTime; }
    public AbsenceType getAbsenceType() { return absenceType; }
    public void setAbsenceType(AbsenceType absenceType) { this.absenceType = absenceType; }
    public BigDecimal getHoursWorked() { return hoursWorked; }
    public void setHoursWorked(BigDecimal hoursWorked) { this.hoursWorked = hoursWorked; }
    public BigDecimal getHoursOvertime() { return hoursOvertime; }
    public void setHoursOvertime(BigDecimal hoursOvertime) { this.hoursOvertime = hoursOvertime; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
