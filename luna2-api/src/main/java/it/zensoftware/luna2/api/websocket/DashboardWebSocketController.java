package it.zensoftware.luna2.api.websocket;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import java.time.LocalDateTime;

/**
 * Controller STOMP per notifiche dashboard real-time.
 * 
 * Utilizzo lato client (JavaScript):
 * ```
 * var client = new StompClient();
 * client.connect({}, function() {
 *   client.subscribe('/topic/notifications', function(message) {
 *     console.log("Notifica ricevuta:", message.body);
 *   });
 * });
 * ```
 */
@Controller
public class DashboardWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;

    public DashboardWebSocketController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Riceve messaggi di test dal client e li invia al topic broadcast.
     */
    @MessageMapping("/dashboard/ping")
    @SendTo("/topic/notifications")
    public DashboardMessage ping(DashboardMessage message) {
        message.timestamp = LocalDateTime.now();
        message.type = "ping_response";
        return message;
    }

    /**
     * Invia notifica a un utente specifico.
     * Dal backend: messagingTemplate.convertAndSendToUser(userId, "/queue/notifications", message);
     */
    public void sendUserNotification(String userId, DashboardNotification notification) {
        notification.timestamp = LocalDateTime.now();
        messagingTemplate.convertAndSendToUser(
            userId,
            "/queue/notifications",
            notification
        );
    }

    /**
     * Invia notifica broadcast a tutti gli utenti.
     */
    public void broadcastNotification(DashboardNotification notification) {
        notification.timestamp = LocalDateTime.now();
        messagingTemplate.convertAndSend("/topic/notifications", notification);
    }

    public static class DashboardMessage {
        public String userId;
        public String message;
        public String type;
        public LocalDateTime timestamp;

        public DashboardMessage() {}

        public DashboardMessage(String userId, String message, String type, LocalDateTime timestamp) {
            this.userId = userId;
            this.message = message;
            this.type = type;
            this.timestamp = timestamp;
        }
    }

    public static class DashboardNotification {
        public String id;
        public String title;
        public String message;
        public String type;
        public String entityType;
        public Long entityId;
        public LocalDateTime timestamp;

        public DashboardNotification() {
            this.timestamp = LocalDateTime.now();
        }

        public DashboardNotification(String id, String title, String message, String type,
                                     String entityType, Long entityId, LocalDateTime timestamp) {
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
