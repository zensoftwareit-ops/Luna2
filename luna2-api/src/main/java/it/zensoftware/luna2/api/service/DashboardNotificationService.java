package it.zensoftware.luna2.api.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DashboardNotificationService - Bridgea tra EventPublisher (Luna2)
 * e WebSocket (luna2-api) per notifiche real-time.
 * 
 * Utilizzo dal notification system:
 * ```
 * DashboardNotificationService dashboardService = new DashboardNotificationService(messagingTemplate);
 * dashboardService.notifyUser("user123", "Nuovo ordine ricevuto", "ordine", 1L);
 * ```
 */
@Service
public class DashboardNotificationService {

    private static final Logger logger = LogManager.getLogger(DashboardNotificationService.class);
    private final SimpMessagingTemplate messagingTemplate;

    public DashboardNotificationService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Invia notifica a un utente specifico via WebSocket.
     */
    public void notifyUser(String userId, String title, String message, String entityType, Long entityId) {
        try {
            DashboardNotification notification = new DashboardNotification(
                UUID.randomUUID().toString(),
                title,
                message,
                getNotificationType(entityType),
                entityType,
                entityId,
                LocalDateTime.now()
            );

            messagingTemplate.convertAndSendToUser(
                userId,
                "/queue/notifications",
                notification
            );

            logger.info("Notifica dashboard inviata a {}: {}", userId, title);
        } catch (Exception e) {
            logger.error("Errore invio notifica dashboard", e);
        }
    }

    /**
     * Notifica broadcast a tutti gli utenti connessi.
     */
    public void broadcastNotification(String title, String message, String entityType) {
        try {
            DashboardNotification notification = new DashboardNotification(
                UUID.randomUUID().toString(),
                title,
                message,
                getNotificationType(entityType),
                entityType,
                null,
                LocalDateTime.now()
            );

            messagingTemplate.convertAndSend("/topic/notifications", notification);
            logger.info("Notifica broadcast inviata: {}", title);
        } catch (Exception e) {
            logger.error("Errore invio notifica broadcast", e);
        }
    }

    /**
     * Notifica di warning/alert.
     */
    public void alertUser(String userId, String title, String message) {
        try {
            DashboardNotification notification = new DashboardNotification(
                UUID.randomUUID().toString(),
                title,
                message,
                "warning",
                "alert",
                null,
                LocalDateTime.now()
            );

            messagingTemplate.convertAndSendToUser(userId, "/queue/notifications", notification);
            logger.warn("Alert inviato a {}: {}", userId, title);
        } catch (Exception e) {
            logger.error("Errore invio alert", e);
        }
    }

    public String getNotificationType(String entityType) {
        if (entityType == null) {
            return "info";
        }
        
        switch (entityType.toLowerCase()) {
            case "ordine":
            case "fattura":
            case "preventivo":
                return "success";
            case "scadenza":
            case "reminder":
            case "task":
                return "warning";
            case "errore":
            case "alert":
                return "error";
            default:
                return "info";
        }
    }

    /**
     * Modello notifica per WebSocket.
     */
    
    public static class DashboardNotification {
        public String id;
        public String title;
        public String message;
        public String type;  // info, success, warning, error
        public String entityType;  // cliente, ordine, fattura, etc
        public Long entityId;
        public LocalDateTime timestamp;

        public DashboardNotification(String id, String title, String message, 
                                      String type, String entityType, Long entityId, 
                                      LocalDateTime timestamp) {
            this.id = id;
            this.title = title;
            this.message = message;
            this.type = type;
            this.entityType = entityType;
            this.entityId = entityId;
            this.timestamp = timestamp;
        }
    }
}
