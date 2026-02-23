package it.zensoftware.luna2.model;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * NotificationPreference - Modello per preferences notifiche utente
 * Gestisce quali canali e tipi di notifica preferisce ogni utente
 */
@Entity
@Table(name = "notification_preferences")
public class NotificationPreference {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String userId;
    
    // Abilita/disabilita canali
    @Column(nullable = false)
    private Boolean emailEnabled = true;
    
    @Column(nullable = false)
    private Boolean pushEnabled = true;
    
    @Column(nullable = false)
    private Boolean smsEnabled = false;  // Default disabilitato
    
    // Frequenza digest email
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DigestFrequency digestFrequency = DigestFrequency.IMMEDIATE;
    
    // Tipi di evento da notificare (bitmask o JSON)
    @Column(columnDefinition = "TEXT")
    private String enabledEventTypes;  // Es: "FATTURA_ARRIVATA,PREVENTIVO_APERTO,SCADENZA_IMMINENTE"
    
    // Orario quiet (non inviare notifiche)
    @Column(nullable = false)
    private String quietStartTime = "22:00";  // 22:00 default
    
    @Column(nullable = false)
    private String quietEndTime = "08:00";    // 08:00 default
    
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public Boolean getEmailEnabled() { return emailEnabled; }
    public void setEmailEnabled(Boolean emailEnabled) { this.emailEnabled = emailEnabled; }
    
    public Boolean getPushEnabled() { return pushEnabled; }
    public void setPushEnabled(Boolean pushEnabled) { this.pushEnabled = pushEnabled; }
    
    public Boolean getSmsEnabled() { return smsEnabled; }
    public void setSmsEnabled(Boolean smsEnabled) { this.smsEnabled = smsEnabled; }
    
    public DigestFrequency getDigestFrequency() { return digestFrequency; }
    public void setDigestFrequency(DigestFrequency digestFrequency) { this.digestFrequency = digestFrequency; }
    
    public String getEnabledEventTypes() { return enabledEventTypes; }
    public void setEnabledEventTypes(String enabledEventTypes) { this.enabledEventTypes = enabledEventTypes; }
    
    public String getQuietStartTime() { return quietStartTime; }
    public void setQuietStartTime(String quietStartTime) { this.quietStartTime = quietStartTime; }
    
    public String getQuietEndTime() { return quietEndTime; }
    public void setQuietEndTime(String quietEndTime) { this.quietEndTime = quietEndTime; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    /**
     * Ritorna true se ora corrente è in quiet period
     */
    public boolean isInQuietPeriod() {
        LocalDateTime now = LocalDateTime.now();
        String currentTime = String.format("%02d:%02d", now.getHour(), now.getMinute());
        
        // Se quietStartTime > quietEndTime (es 22:00-08:00 di notte)
        if (quietStartTime.compareTo(quietEndTime) > 0) {
            return currentTime.compareTo(quietStartTime) >= 0 || 
                   currentTime.compareTo(quietEndTime) < 0;
        } else {
            return currentTime.compareTo(quietStartTime) >= 0 && 
                   currentTime.compareTo(quietEndTime) < 0;
        }
    }
    
    /**
     * Ritorna true se evento è abilitato per notifiche
     */
    public boolean isEventTypeEnabled(String eventType) {
        if (enabledEventTypes == null || enabledEventTypes.isEmpty()) {
            return true;  // Default: tutti gli eventi abilitati
        }
        return enabledEventTypes.contains(eventType);
    }
    
    public enum DigestFrequency {
        IMMEDIATE,      // Notizze in tempo reale
        HOURLY,         // Digest ogni ora
        DAILY,          // Digest giornaliero
        WEEKLY,         // Digest settimanale
        NEVER           // No notifiche
    }
}

/**
 * NotificationHistory - Modello per salvare history notifiche inviate
 */
@Entity
@Table(name = "notification_history")
class NotificationHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String userId;
    
    @Column(nullable = false)
    private String eventType;  // FATTURA_ARRIVATA, etc
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Channel channel;  // EMAIL, PUSH, SMS
    
    @Column(nullable = false)
    private String subject;
    
    @Column(columnDefinition = "TEXT")
    private String message;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.SENT;  // SENT, FAILED, BOUNCED
    
    @Column
    private String errorMessage;
    
    @Column(nullable = false)
    private LocalDateTime sentAt = LocalDateTime.now();
    
    @Column
    private LocalDateTime readAt;  // Per tracking lettura email
    
    @Column
    private LocalDateTime clickedAt;  // Per tracking click email
    
    // Getters e Setters omessi per brevità
    public Long getId() { return id; }
    public String getUserId() { return userId; }
    public String getEventType() { return eventType; }
    public Channel getChannel() { return channel; }
    public Status getStatus() { return status; }
    
    public enum Channel {
        EMAIL, PUSH, SMS, SLACK
    }
    
    public enum Status {
        SENT, FAILED, BOUNCED, DELIVERED, READ, CLICKED
    }
}
