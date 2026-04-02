package it.zensoftware.luna2.model;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * ApprovalRequest - Richieste di approvazione (ferie, permessi, spese)
 * 
 * Sprint 1: Q2 2026 - Workflow Approvazione
 * Status: DRAFT
 */
@Entity
@Table(name = "approval_requests", indexes = {
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_requester", columnList = "requester_id"),
    @Index(name = "idx_approver", columnList = "approver_id")
})
public class ApprovalRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "requester_id", nullable = false)
    private Long requesterId;

    @Column(name = "approver_id")
    private Long approverId;

    @Column(name = "request_type", nullable = false, length = 50)
    private String requestType; // FERIE, PERMESSO, SPESA

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "days_requested")
    private Integer daysRequested;

    @Column(name = "amount_requested", precision = 10, scale = 2)
    private BigDecimal amountRequested;

    @Column(name = "status", length = 20, nullable = false)
    private String status = "DRAFT"; // DRAFT, SUBMITTED, APPROVED, REJECTED, COMPLETED

    @Column(name = "approval_date")
    private LocalDateTime approvalDate;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDateTime createdDate = LocalDateTime.now();

    @Column(name = "submitted_date")
    private LocalDateTime submittedDate;

    @Column(name = "updated_date")
    private LocalDateTime updatedDate = LocalDateTime.now();

    // Constructors
    public ApprovalRequest() {
    }

    public ApprovalRequest(Long companyId, Long requesterId, String requestType, String description) {
        this.companyId = companyId;
        this.requesterId = requesterId;
        this.requestType = requestType;
        this.description = description;
        this.status = "DRAFT";
        this.createdDate = LocalDateTime.now();
        this.updatedDate = LocalDateTime.now();
    }

    // Getters & Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }

    public Long getRequesterId() {
        return requesterId;
    }

    public void setRequesterId(Long requesterId) {
        this.requesterId = requesterId;
    }

    public Long getApproverId() {
        return approverId;
    }

    public void setApproverId(Long approverId) {
        this.approverId = approverId;
    }

    public String getRequestType() {
        return requestType;
    }

    public void setRequestType(String requestType) {
        this.requestType = requestType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public Integer getDaysRequested() {
        return daysRequested;
    }

    public void setDaysRequested(Integer daysRequested) {
        this.daysRequested = daysRequested;
    }

    public BigDecimal getAmountRequested() {
        return amountRequested;
    }

    public void setAmountRequested(BigDecimal amountRequested) {
        this.amountRequested = amountRequested;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getApprovalDate() {
        return approvalDate;
    }

    public void setApprovalDate(LocalDateTime approvalDate) {
        this.approvalDate = approvalDate;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public LocalDateTime getSubmittedDate() {
        return submittedDate;
    }

    public void setSubmittedDate(LocalDateTime submittedDate) {
        this.submittedDate = submittedDate;
    }

    public LocalDateTime getUpdatedDate() {
        return updatedDate;
    }

    public void setUpdatedDate(LocalDateTime updatedDate) {
        this.updatedDate = updatedDate;
    }

    @Override
    public String toString() {
        return "ApprovalRequest{" +
                "id=" + id +
                ", companyId=" + companyId +
                ", requesterId=" + requesterId +
                ", requestType='" + requestType + '\'' +
                ", status='" + status + '\'' +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", daysRequested=" + daysRequested +
                ", createdDate=" + createdDate +
                '}';
    }
}
