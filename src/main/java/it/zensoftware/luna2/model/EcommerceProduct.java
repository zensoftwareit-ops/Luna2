package it.zensoftware.luna2.model;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Entity representing a unified product from any eCommerce platform
 */
@Entity
@Table(name = "ecommerce_products", indexes = {
    @Index(name = "idx_platform_type", columnList = "platform_type"),
    @Index(name = "idx_external_product_id", columnList = "external_product_id"),
    @Index(name = "idx_sku", columnList = "sku"),
    @Index(name = "idx_product_name", columnList = "product_name")
})
public class EcommerceProduct implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EcommercePlatform.PlatformType platformType;

    @Column(nullable = false, length = 100)
    private String externalProductId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "platform_id", nullable = false)
    private EcommercePlatform ecommercePlatform;

    @Column(length = 100)
    private String sku;

    @Column(nullable = false, length = 255)
    private String productName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal price = BigDecimal.ZERO;

    @Column(length = 10)
    private String currency = "EUR";

    @Column(name = "stock_quantity")
    private Integer stockQuantity = 0;

    @Column(columnDefinition = "LONGTEXT")
    private String images;

    @Column(columnDefinition = "LONGTEXT")
    private String externalJson;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "last_sync_at")
    private Date lastSyncAt;

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
        lastSyncAt = new Date();
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

    public EcommercePlatform.PlatformType getPlatformType() {
        return platformType;
    }

    public void setPlatformType(EcommercePlatform.PlatformType platformType) {
        this.platformType = platformType;
    }

    public String getExternalProductId() {
        return externalProductId;
    }

    public void setExternalProductId(String externalProductId) {
        this.externalProductId = externalProductId;
    }

    public EcommercePlatform getEcommercePlatform() {
        return ecommercePlatform;
    }

    public void setEcommercePlatform(EcommercePlatform ecommercePlatform) {
        this.ecommercePlatform = ecommercePlatform;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Integer getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(Integer stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    public String getImages() {
        return images;
    }

    public void setImages(String images) {
        this.images = images;
    }

    public String getExternalJson() {
        return externalJson;
    }

    public void setExternalJson(String externalJson) {
        this.externalJson = externalJson;
    }

    public Date getLastSyncAt() {
        return lastSyncAt;
    }

    public void setLastSyncAt(Date lastSyncAt) {
        this.lastSyncAt = lastSyncAt;
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
        return "EcommerceProduct{" +
                "id=" + id +
                ", platformType=" + platformType +
                ", sku='" + sku + '\'' +
                ", productName='" + productName + '\'' +
                ", price=" + price +
                '}';
    }
}
