package it.zensoftware.luna2.model;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

/**
 * Entity for tracking eCommerce sync operations (audit trail)
 */
@Entity
@Table(name = "ecommerce_sync_logs", indexes = {
    @Index(name = "idx_platform_type", columnList = "platform_type"),
    @Index(name = "idx_sync_type", columnList = "sync_type"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_start_time", columnList = "start_time")
})
public class EcommerceSyncLog implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum SyncType {
        ORDERS, PRODUCTS
    }

    public enum SyncStatus {
        SUCCESS, PARTIAL, FAILED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform_type", nullable = false)
    private EcommercePlatform.PlatformType platformType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "platform_id")
    private EcommercePlatform ecommercePlatform;

    @Enumerated(EnumType.STRING)
    @Column(name = "sync_type", nullable = false)
    private SyncType syncType;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "start_time", nullable = false)
    private Date startTime;

    @Temporal(TemporalType.TIMESTAMP)
    private Date endTime;

    @Column(name = "records_processed")
    private Integer recordsProcessed = 0;

    @Column(name = "records_inserted")
    private Integer recordsInserted = 0;

    @Column(name = "records_updated")
    private Integer recordsUpdated = 0;

    @Column(name = "records_failed")
    private Integer recordsFailed = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SyncStatus status = SyncStatus.SUCCESS;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Column(columnDefinition = "LONGTEXT")
    private String details;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "data_creazione", updatable = false)
    private Date dataCreazione;

    @PrePersist
    protected void onCreate() {
        dataCreazione = new Date();
        if (startTime == null) {
            startTime = new Date();
        }
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public EcommercePlatform.PlatformType getPlatformType() {
        return platformType;
    }

    public void setPlatformType(EcommercePlatform.PlatformType platformType) {
        this.platformType = platformType;
    }

    public EcommercePlatform getEcommercePlatform() {
        return ecommercePlatform;
    }

    public void setEcommercePlatform(EcommercePlatform ecommercePlatform) {
        this.ecommercePlatform = ecommercePlatform;
    }

    public SyncType getSyncType() {
        return syncType;
    }

    public void setSyncType(SyncType syncType) {
        this.syncType = syncType;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public Integer getRecordsProcessed() {
        return recordsProcessed;
    }

    public void setRecordsProcessed(Integer recordsProcessed) {
        this.recordsProcessed = recordsProcessed;
    }

    public Integer getRecordsInserted() {
        return recordsInserted;
    }

    public void setRecordsInserted(Integer recordsInserted) {
        this.recordsInserted = recordsInserted;
    }

    public Integer getRecordsUpdated() {
        return recordsUpdated;
    }

    public void setRecordsUpdated(Integer recordsUpdated) {
        this.recordsUpdated = recordsUpdated;
    }

    public Integer getRecordsFailed() {
        return recordsFailed;
    }

    public void setRecordsFailed(Integer recordsFailed) {
        this.recordsFailed = recordsFailed;
    }

    public SyncStatus getStatus() {
        return status;
    }

    public void setStatus(SyncStatus status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public Date getDataCreazione() {
        return dataCreazione;
    }

    public void setDataCreazione(Date dataCreazione) {
        this.dataCreazione = dataCreazione;
    }

    @Override
    public String toString() {
        return "EcommerceSyncLog{" +
                "id=" + id +
                ", platformType=" + platformType +
                ", syncType=" + syncType +
                ", status=" + status +
                ", recordsProcessed=" + recordsProcessed +
                '}';
    }
}
