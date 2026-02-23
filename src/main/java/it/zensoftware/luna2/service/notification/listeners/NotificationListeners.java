package it.zensoftware.luna2.service.notification.listeners;

import it.zensoftware.luna2.service.email.EmailService;
import it.zensoftware.luna2.service.notification.EventListener;
import it.zensoftware.luna2.service.notification.event.NotificationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * EmailNotificationListener implementa l'invio di email quando un evento è pubblicato.
 * 
 * Utilizzo:
 * EmailNotificationListener emailListener = new EmailNotificationListener(emailService);
 * EventPublisher.getInstance().subscribe("FATTURA_ARRIVATA", emailListener);
 * 
 * @author Notification System
 * @version 1.0
 */
class EmailNotificationListener implements EventListener {
    
    private static final Logger logger = LogManager.getLogger(EmailNotificationListener.class);
    private final EmailService emailService;
    
    public EmailNotificationListener(EmailService emailService) {
        this.emailService = emailService;
    }
    
    @Override
    public void onEventPublished(NotificationEvent event) {
        try {
            String userEmail = getUserEmailById(event.getUserId());
            
            if (userEmail == null || userEmail.isEmpty()) {
                logger.warn("Nessuna email trovata per utente: " + event.getUserId());
                return;
            }
            
            String emailTemplate = event.getEmailTemplate();
            String subject = event.getEmailSubject();
            String htmlBody = renderTemplate(emailTemplate, event);
            
            boolean sent = emailService.sendHtmlEmail(userEmail, subject, htmlBody);
            
            if (sent) {
                logger.info("Email notifica inviata: " + userEmail + " | Evento: " + 
                           event.getEventType());
            } else {
                logger.error("Fallimento invio email per evento: " + event.getEventType());
            }
            
        } catch (Exception e) {
            logger.error("Errore in EmailNotificationListener", e);
        }
    }
    
    /**
     * Recupera email utente dal database
     * TODO: Implementare accesso al database
     */
    private String getUserEmailById(String userId) {
        // TODO: Query database per ottenere email utente
        return "user@example.com";  // Placeholder
    }
    
    /**
     * Renderizza template email con dati evento
     * TODO: Implementare con Freemarker o Velocity
     */
    private String renderTemplate(String templateName, NotificationEvent event) {
        // TODO: Caricare template dal filesystem
        // TODO: Processare con Freemarker/Velocity
        // TODO: Sostituire variabili con dati event
        
        StringBuilder html = new StringBuilder();
        html.append("<html><body>");
        html.append("<h2>").append(event.getEmailSubject()).append("</h2>");
        html.append("<p>Dettagli evento:</p>");
        html.append("<ul>");
        
        event.getData().forEach((key, value) -> {
            html.append("<li><strong>").append(key).append(":</strong> ")
                .append(value).append("</li>");
        });
        
        html.append("</ul>");
        html.append("</body></html>");
        
        return html.toString();
    }
}

/**
 * PushNotificationListener implementa l'invio di notifiche push via WebSocket/SSE.
 * 
 * @author Notification System
 * @version 1.0
 */
class PushNotificationListener implements EventListener {
    
    private static final Logger logger = LogManager.getLogger(PushNotificationListener.class);
    
    @Override
    public void onEventPublished(NotificationEvent event) {
        try {
            String userId = event.getUserId();
            String title = event.getPushTitle();
            String message = event.getPushMessage();
            
            // TODO: Implementare invio push via WebSocket/SSE
            // TODO: Includere dati evento nel payload
            
            logger.info("Push notification preparata per utente: " + userId + 
                       " | Titolo: " + title + " | Messaggio: " + message);
            
            // Placeholder per implementazione SSE
            // WebSocketManager.getInstance().sendToUser(userId, getPushPayload(event));
            
        } catch (Exception e) {
            logger.error("Errore in PushNotificationListener", e);
        }
    }
    
    /**
     * Prepara payload per push notification
     */
    private String getPushPayload(NotificationEvent event) {
        return "{" +
                "\"type\": \"" + event.getEventType() + "\"," +
                "\"title\": \"" + event.getPushTitle() + "\"," +
                "\"message\": \"" + event.getPushMessage() + "\"," +
                "\"priority\": \"" + event.getPriority() + "\"," +
                "\"timestamp\": \"" + event.getTimestamp() + "\"" +
                "}";
    }
}

/**
 * DatabaseNotificationListener implementa la persistenza delle notifiche nel database.
 * Utile per history e tracking notifiche.
 */
class DatabaseNotificationListener implements EventListener {
    
    private static final Logger logger = LogManager.getLogger(DatabaseNotificationListener.class);
    
    @Override
    public void onEventPublished(NotificationEvent event) {
        try {
            // TODO: Salvare notifica nel database
            // INSERT INTO notifications (event_id, user_id, event_type, data, created_at)
            // VALUES (?, ?, ?, ?, NOW())
            
            logger.info("Notifica salvata nel database: " + event.getEventId() + 
                       " | Utente: " + event.getUserId());
            
        } catch (Exception e) {
            logger.error("Errore in DatabaseNotificationListener", e);
        }
    }
}

/**
 * SlackNotificationListener implementa l'invio di notifiche a Slack.
 * Utile per team collaboration e alert.
 */
class SlackNotificationListener implements EventListener {
    
    private static final Logger logger = LogManager.getLogger(SlackNotificationListener.class);
    private final String webhookUrl;
    
    public SlackNotificationListener(String slackWebhookUrl) {
        this.webhookUrl = slackWebhookUrl;
    }
    
    @Override
    public void onEventPublished(NotificationEvent event) {
        try {
            if (webhookUrl == null || webhookUrl.isEmpty()) {
                logger.debug("Slack webhook non configurato, skip");
                return;
            }
            
            // TODO: Implementare invio a Slack via HTTP POST al webhook
            String payload = buildSlackPayload(event);
            // TODO: sendHttpPost(webhookUrl, payload);
            
            logger.info("Notifica Slack inviata per evento: " + event.getEventType());
            
        } catch (Exception e) {
            logger.error("Errore in SlackNotificationListener", e);
        }
    }
    
    private String buildSlackPayload(NotificationEvent event) {
        return "{" +
                "\"text\": \"" + event.getPushTitle() + "\"," +
                "\"blocks\": [{" +
                "\"type\": \"section\"," +
                "\"text\": {\"type\": \"mrkdwn\", \"text\": \"" + event.getPushMessage() + "\"}" +
                "}]" +
                "}";
    }
}

/**
 * SMSNotificationListener implementa l'invio di SMS (via Twilio o simile).
 */
class SMSNotificationListener implements EventListener {
    
    private static final Logger logger = LogManager.getLogger(SMSNotificationListener.class);
    private final String apiKey;  // Twilio API key
    
    public SMSNotificationListener(String twilioApiKey) {
        this.apiKey = twilioApiKey;
    }
    
    @Override
    public void onEventPublished(NotificationEvent event) {
        try {
            // TODO: Solo per notifiche HIGH priority
            if (event.getPriority() != NotificationEvent.Priority.HIGH) {
                return;
            }
            
            // TODO: Recuperare numero telefono utente
            // TODO: Inviare SMS via Twilio API
            
            logger.info("SMS notifica inviata per evento: " + event.getEventType());
            
        } catch (Exception e) {
            logger.error("Errore in SMSNotificationListener", e);
        }
    }
}
