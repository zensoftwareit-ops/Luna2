package it.zensoftware.luna2.model;

import javax.persistence.*;
import java.util.Date;

/**
 * JPA Entity per Audit logging
 * Traccia tutte le operazioni sensibili (create/update/delete)
 * Per conformità normativa e debugging
 */
@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_user", columnList = "user_name"),
    @Index(name = "idx_action", columnList = "action"),
    @Index(name = "idx_timestamp", columnList = "timestamp")
})
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_name", nullable = false)
    private String userName;

    @Column(name = "action", nullable = false, length = 100)
    private String action;

    @Column(name = "entity_type", length = 100)
    private String entityType;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(name = "timestamp", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date timestamp;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "http_method", length = 10)
    private String httpMethod;

    @Column(name = "endpoint", length = 500)
    private String endpoint;

    @Column(name = "status_code")
    private Integer statusCode;

    @Column(name = "result", length = 20)
    private String result; // SUCCESS, FAILURE, ERROR

    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    // ==================== CONSTRUCTOR ====================

    public AuditLog() {
        this.timestamp = new Date();
        this.result = "SUCCESS";
    }

    public AuditLog(String userName, String action, String entityType, Long entityId) {
        this();
        this.userName = userName;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
    }

    // ==================== GETTERS & SETTERS ====================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }

    public Long getEntityId() { return entityId; }
    public void setEntityId(Long entityId) { this.entityId = entityId; }

    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getHttpMethod() { return httpMethod; }
    public void setHttpMethod(String httpMethod) { this.httpMethod = httpMethod; }

    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }

    public Integer getStatusCode() { return statusCode; }
    public void setStatusCode(Integer statusCode) { this.statusCode = statusCode; }

    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    @Override
    public String toString() {
        return String.format(
            "AuditLog[user=%s, action=%s, entity=%s(%d), endpoint=%s, status=%d, result=%s, timestamp=%s]",
            userName, action, entityType, entityId, endpoint, statusCode, result, timestamp
        );
    }
}
