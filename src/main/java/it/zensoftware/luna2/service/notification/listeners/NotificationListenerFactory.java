package it.zensoftware.luna2.service.notification.listeners;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import it.zensoftware.luna2.dao.NotificationHistoryDAO;
import it.zensoftware.luna2.dao.NotificationPreferenceDAO;
import it.zensoftware.luna2.dao.UserDAO;
import it.zensoftware.luna2.model.NotificationHistory;
import it.zensoftware.luna2.model.NotificationPreference;
import it.zensoftware.luna2.model.User;
import it.zensoftware.luna2.service.email.EmailService;
import it.zensoftware.luna2.service.notification.EventListener;
import it.zensoftware.luna2.service.notification.PushNotificationService;
import it.zensoftware.luna2.service.notification.event.NotificationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

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
    private static final Configuration TEMPLATE_CONFIG = buildTemplateConfig();
    private final EmailService emailService;
    private final UserDAO userDAO = new UserDAO();
    private final NotificationPreferenceDAO preferenceDAO = new NotificationPreferenceDAO();
    private final NotificationHistoryDAO historyDAO = new NotificationHistoryDAO();
    
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

            if (!shouldSendEmail(event)) {
                logger.debug("Email disabilitata per utente: " + event.getUserId() + " evento: " + event.getEventType());
                return;
            }
            
            String emailTemplate = event.getEmailTemplate();
            String subject = event.getEmailSubject();
            String htmlBody = renderTemplate(emailTemplate, event);
            
            boolean sent = emailService.sendHtmlEmail(userEmail, subject, htmlBody);

            saveHistory(event, subject, htmlBody, sent ? NotificationHistory.Status.SENT : NotificationHistory.Status.FAILED,
                    sent ? null : "Errore invio email");
            
            if (sent) {
                logger.info("Email notifica inviata: " + userEmail + " | Evento: " + 
                           event.getEventType());
            } else {
                logger.error("Fallimento invio email per evento: " + event.getEventType());
            }
            
        } catch (Exception e) {
            logger.error("Errore in EmailNotificationListener", e);
            saveHistory(event, event.getEmailSubject(), null, NotificationHistory.Status.FAILED, e.getMessage());
        }
    }
    
    /**
     * Recupera email utente dal database
     * TODO: Implementare accesso al database
     */
    private String getUserEmailById(String userId) {
        try {
            Long id = Long.parseLong(userId);
            User user = userDAO.findById(id);
            return user != null ? user.getEmail() : null;
        } catch (Exception e) {
            logger.warn("Impossibile risolvere email per utente: " + userId, e);
            return null;
        }
    }
    
    /**
     * Renderizza template email con dati evento
     * TODO: Implementare con Freemarker o Velocity
     */
    private String renderTemplate(String templateName, NotificationEvent event) {
        try {
            Template template = TEMPLATE_CONFIG.getTemplate(templateName);
            Map<String, Object> model = new HashMap<>(event.getData());
            model.put("subject", event.getEmailSubject());
            model.put("eventType", event.getEventType());
            model.put("timestamp", event.getTimestamp());

            StringWriter writer = new StringWriter();
            template.process(model, writer);
            return writer.toString();
        } catch (IOException | TemplateException e) {
            logger.warn("Errore rendering template email: " + templateName, e);
            return fallbackHtml(event);
        }
    }

    private boolean shouldSendEmail(NotificationEvent event) {
        NotificationPreference preference = preferenceDAO.findByUserId(event.getUserId());
        if (preference == null) {
            return true;
        }
        if (!Boolean.TRUE.equals(preference.getEmailEnabled())) {
            return false;
        }
        if (preference.isInQuietPeriod()) {
            return false;
        }
        return preference.isEventTypeEnabled(event.getEventType());
    }

    private void saveHistory(NotificationEvent event, String subject, String message,
                             NotificationHistory.Status status, String errorMessage) {
        try {
            NotificationHistory history = new NotificationHistory();
            history.setUserId(event.getUserId());
            history.setEventType(event.getEventType());
            history.setChannel(NotificationHistory.Channel.EMAIL);
            history.setSubject(subject != null ? subject : event.getEmailSubject());
            history.setMessage(message);
            history.setStatus(status);
            history.setErrorMessage(errorMessage);
            history.setSentAt(LocalDateTime.now());
            historyDAO.save(history);
        } catch (Exception e) {
            logger.warn("Impossibile salvare storico email", e);
        }
    }

    private static Configuration buildTemplateConfig() {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_32);
        cfg.setClassLoaderForTemplateLoading(EmailNotificationListener.class.getClassLoader(), "email-templates");
        cfg.setDefaultEncoding("UTF-8");
        return cfg;
    }

    private String fallbackHtml(NotificationEvent event) {
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

            if (!shouldSendPush(event)) {
                logger.debug("Push disabilitata per utente: " + userId + " evento: " + event.getEventType());
                return;
            }

            PushNotificationService.getInstance().sendNotificationToUser(userId, title, message, event.getPriority());

            NotificationHistory history = new NotificationHistory();
            history.setUserId(userId);
            history.setEventType(event.getEventType());
            history.setChannel(NotificationHistory.Channel.PUSH);
            history.setSubject(title);
            history.setMessage(message);
            history.setStatus(NotificationHistory.Status.SENT);
            history.setSentAt(LocalDateTime.now());
            new NotificationHistoryDAO().save(history);

            logger.info("Push notification inviata per utente: " + userId +
                       " | Titolo: " + title + " | Messaggio: " + message);
            
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

    private boolean shouldSendPush(NotificationEvent event) {
        NotificationPreferenceDAO preferenceDAO = new NotificationPreferenceDAO();
        NotificationPreference preference = preferenceDAO.findByUserId(event.getUserId());
        if (preference == null) {
            return true;
        }
        if (!Boolean.TRUE.equals(preference.getPushEnabled())) {
            return false;
        }
        if (preference.isInQuietPeriod()) {
            return false;
        }
        return preference.isEventTypeEnabled(event.getEventType());
    }
}

/**
 * DatabaseNotificationListener implementa la persistenza delle notifiche nel database.
 * Utile per history e tracking notifiche.
 */
class DatabaseNotificationListener implements EventListener {
    
    private static final Logger logger = LogManager.getLogger(DatabaseNotificationListener.class);
    private final NotificationHistoryDAO historyDAO = new NotificationHistoryDAO();
    
    @Override
    public void onEventPublished(NotificationEvent event) {
        try {
            NotificationHistory history = new NotificationHistory();
            history.setUserId(event.getUserId());
            history.setEventType(event.getEventType());
            history.setChannel(NotificationHistory.Channel.DATABASE);
            history.setSubject(event.getEmailSubject());
            history.setMessage(event.getPushMessage());
            history.setStatus(NotificationHistory.Status.SENT);
            history.setSentAt(LocalDateTime.now());
            historyDAO.save(history);
            
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

/**
 * Factory pubblica per creare listener (package-private) da altri package.
 */
public final class NotificationListenerFactory {

    private NotificationListenerFactory() {
    }

    public static EventListener emailListener(EmailService emailService) {
        return new EmailNotificationListener(emailService);
    }

    public static EventListener pushListener() {
        return new PushNotificationListener();
    }

    public static EventListener databaseListener() {
        return new DatabaseNotificationListener();
    }

    public static EventListener slackListener(String webhookUrl) {
        return new SlackNotificationListener(webhookUrl);
    }

    public static EventListener smsListener(String apiKey) {
        return new SMSNotificationListener(apiKey);
    }
}
