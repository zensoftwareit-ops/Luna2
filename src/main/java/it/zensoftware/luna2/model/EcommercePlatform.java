package it.zensoftware.luna2.model;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

/**
 * Entity representing a connected eCommerce platform (WooCommerce, Shopify, Amazon, eBay)
 */
@Entity
@Table(name = "ecommerce_platforms", indexes = {
    @Index(name = "idx_platform_type", columnList = "platform_type"),
    @Index(name = "idx_is_active", columnList = "is_active")
})
public class EcommercePlatform implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum PlatformType {
        WOOCOMMERCE, SHOPIFY, AMAZON, EBAY
    }

    public enum SyncStatus {
        PENDING, SUCCESS, FAILED, PARTIAL
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlatformType platformType;

    @Column(nullable = false, length = 100)
    private String storeName;

    @Column(nullable = false, length = 255)
    private String storeUrl;

    @Column(nullable = false, length = 500)
    private String apiKey;

    @Column(length = 500)
    private String apiSecret;

    @Column(nullable = false)
    private Boolean isActive = true;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "last_sync")
    private Date lastSync;

    @Enumerated(EnumType.STRING)
    @Column(name = "last_sync_status")
    private SyncStatus lastSyncStatus = SyncStatus.PENDING;

    @Column(name = "sync_frequency_minutes")
    private Integer syncFrequencyMinutes = 15;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "data_creazione", updatable = false)
    private Date dataCreazione;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "data_modifica")
    private Date dataModifica;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @PrePersist
    protected void onCreate() {
        dataCreazione = new Date();
        dataModifica = new Date();
    }

    @PreUpdate
    protected void onUpdate() {
        dataModifica = new Date();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PlatformType getPlatformType() {
        return platformType;
    }

    public void setPlatformType(PlatformType platformType) {
        this.platformType = platformType;
    }

    public String getStoreName() {
        return storeName;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }

    public String getStoreUrl() {
        return storeUrl;
    }

    public void setStoreUrl(String storeUrl) {
        this.storeUrl = storeUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getApiSecret() {
        return apiSecret;
    }

    public void setApiSecret(String apiSecret) {
        this.apiSecret = apiSecret;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Date getLastSync() {
        return lastSync;
    }

    public void setLastSync(Date lastSync) {
        this.lastSync = lastSync;
    }

    public SyncStatus getLastSyncStatus() {
        return lastSyncStatus;
    }

    public void setLastSyncStatus(SyncStatus lastSyncStatus) {
        this.lastSyncStatus = lastSyncStatus;
    }

    public Integer getSyncFrequencyMinutes() {
        return syncFrequencyMinutes;
    }

    public void setSyncFrequencyMinutes(Integer syncFrequencyMinutes) {
        this.syncFrequencyMinutes = syncFrequencyMinutes;
    }

    public Date getDataCreazione() {
        return dataCreazione;
    }

    public void setDataCreazione(Date dataCreazione) {
        this.dataCreazione = dataCreazione;
    }

    public Date getDataModifica() {
        return dataModifica;
    }

    public void setDataModifica(Date dataModifica) {
        this.dataModifica = dataModifica;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    @Override
    public String toString() {
        return "EcommercePlatform{" +
                "id=" + id +
                ", platformType=" + platformType +
                ", storeName='" + storeName + '\'' +
                ", storeUrl='" + storeUrl + '\'' +
                ", isActive=" + isActive +
                '}';
    }
}
