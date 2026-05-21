package it.zensoftware.luna2.model;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Entity representing a unified order from any eCommerce platform
 */
@Entity
@Table(name = "ecommerce_orders", indexes = {
    @Index(name = "idx_platform_type", columnList = "platform_type"),
    @Index(name = "idx_external_order_id", columnList = "external_order_id"),
    @Index(name = "idx_order_date", columnList = "order_date"),
    @Index(name = "idx_order_status", columnList = "order_status")
})
public class EcommerceOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EcommercePlatform.PlatformType platformType;

    @Column(nullable = false, length = 100)
    private String externalOrderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "platform_id", nullable = false)
    private EcommercePlatform ecommercePlatform;

    @Column(nullable = false, length = 50)
    private String orderNumber;

    @Temporal(TemporalType.DATE)
    @Column(nullable = false)
    private Date orderDate;

    @Column(length = 100)
    private String customerEmail;

    @Column(length = 255)
    private String customerName;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(length = 10)
    private String currency = "EUR";

    @Column(length = 50)
    private String orderStatus;

    @Column(columnDefinition = "LONGTEXT")
    private String lineItems;

    @Column(columnDefinition = "LONGTEXT")
    private String externalJson;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "synced_at")
    private Date syncedAt;

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
        syncedAt = new Date();
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

    public String getExternalOrderId() {
        return externalOrderId;
    }

    public void setExternalOrderId(String externalOrderId) {
        this.externalOrderId = externalOrderId;
    }

    public EcommercePlatform getEcommercePlatform() {
        return ecommercePlatform;
    }

    public void setEcommercePlatform(EcommercePlatform ecommercePlatform) {
        this.ecommercePlatform = ecommercePlatform;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public Date getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(Date orderDate) {
        this.orderDate = orderDate;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(String orderStatus) {
        this.orderStatus = orderStatus;
    }

    public String getLineItems() {
        return lineItems;
    }

    public void setLineItems(String lineItems) {
        this.lineItems = lineItems;
    }

    public String getExternalJson() {
        return externalJson;
    }

    public void setExternalJson(String externalJson) {
        this.externalJson = externalJson;
    }

    public Date getSyncedAt() {
        return syncedAt;
    }

    public void setSyncedAt(Date syncedAt) {
        this.syncedAt = syncedAt;
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
        return "EcommerceOrder{" +
                "id=" + id +
                ", platformType=" + platformType +
                ", orderNumber='" + orderNumber + '\'' +
                ", totalAmount=" + totalAmount +
                ", orderStatus='" + orderStatus + '\'' +
                '}';
    }
}
