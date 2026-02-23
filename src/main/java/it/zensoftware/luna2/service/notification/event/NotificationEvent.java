package it.zensoftware.luna2.service.notification.event;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * NotificationEvent base class per tutti gli eventi di notifica.
 * Implementa il pattern Observer per loosely coupled components.
 * 
 * Utilizzo:
 * FatturaArrivataEvent event = new FatturaArrivataEvent("12345", "Acme Corp", 1500.00);
 * EventPublisher.getInstance().publishEvent(event);
 * 
 * @author Notification System
 * @version 1.0
 */
public abstract class NotificationEvent {
    
    private final String eventId;
    private final LocalDateTime timestamp;
    private final String eventType;
    private final Map<String, Object> data;
    private final String userId;
    
    /**
     * Costruttore base evento
     * 
     * @param eventType Tipo evento (es: FATTURA_ARRIVATA, PREVENTIVO_APERTO)
     * @param userId ID utente destinatario notifica
     */
    public NotificationEvent(String eventType, String userId) {
        this.eventId = java.util.UUID.randomUUID().toString();
        this.timestamp = LocalDateTime.now();
        this.eventType = eventType;
        this.userId = userId;
        this.data = new HashMap<>();
    }
    
    // Getters
    public String getEventId() { return eventId; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getEventType() { return eventType; }
    public String getUserId() { return userId; }
    public Map<String, Object> getData() { return data; }
    
    /**
     * Aggiungi dato all'evento
     */
    public void addData(String key, Object value) {
        data.put(key, value);
    }
    
    /**
     * Template per email notification
     * @return Nome template (es: "email_fattura_arrivata.html")
     */
    public abstract String getEmailTemplate();
    
    /**
     * Subject per email
     */
    public abstract String getEmailSubject();
    
    /**
     * Descrizione breve per push notification
     */
    public abstract String getPushTitle();
    
    /**
     * Messaggio per push notification
     */
    public abstract String getPushMessage();
    
    /**
     * Priorità notifica (HIGH, NORMAL, LOW)
     */
    public abstract Priority getPriority();
    
    public enum Priority {
        HIGH, NORMAL, LOW
    }
    
    @Override
    public String toString() {
        return "NotificationEvent{" +
                "eventId='" + eventId + '\'' +
                ", eventType='" + eventType + '\'' +
                ", userId='" + userId + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
