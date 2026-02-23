package it.zensoftware.luna2.service.notification;

import it.zensoftware.luna2.service.email.EmailService;
import it.zensoftware.luna2.service.notification.listeners.NotificationListenerFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * Bootstrap per registrare i listener del notification system all'avvio.
 */
public final class NotificationSystemBootstrap {

    private static final Logger logger = LogManager.getLogger(NotificationSystemBootstrap.class);
    private static volatile boolean initialized = false;

    private NotificationSystemBootstrap() {
    }

    public static void initialize() {
        if (initialized) {
            return;
        }

        synchronized (NotificationSystemBootstrap.class) {
            if (initialized) {
                return;
            }

            Properties props = loadProperties();
            EmailService emailService = buildEmailService(props);

            EventPublisher publisher = EventPublisher.getInstance();
            List<String> eventTypes = Arrays.asList(
                    "FATTURA_ARRIVATA",
                    "NOTIFICA_SDI",
                    "PREVENTIVO_APERTO",
                    "PREVENTIVO_LETTO",
                    "ORDINE_CONFERMATO",
                    "SCADENZA_IMMINENTE",
                    "MERCE_IN_MAGAZZINO"
            );

            eventTypes.forEach(eventType -> {
                publisher.subscribe(eventType, NotificationListenerFactory.emailListener(emailService));
                publisher.subscribe(eventType, NotificationListenerFactory.pushListener());
                publisher.subscribe(eventType, NotificationListenerFactory.databaseListener());
            });

            String slackWebhook = props.getProperty("slack.webhook.url", "");
            if (!slackWebhook.isEmpty()) {
                eventTypes.forEach(eventType -> publisher.subscribe(eventType, NotificationListenerFactory.slackListener(slackWebhook)));
            }

            String twilioApiKey = props.getProperty("twilio.api.key", "");
            if (!twilioApiKey.isEmpty()) {
                eventTypes.forEach(eventType -> publisher.subscribe(eventType, NotificationListenerFactory.smsListener(twilioApiKey)));
            }

            initialized = true;
            logger.info("Notification system bootstrap completato: " + publisher.getTotalListenerCount() + " listeners");
        }
    }

    private static EmailService buildEmailService(Properties props) {
        String host = props.getProperty("smtp.host", "");
        int port = Integer.parseInt(props.getProperty("smtp.port", "587"));
        String username = props.getProperty("smtp.username", "");
        String password = props.getProperty("smtp.password", "");
        boolean tls = Boolean.parseBoolean(props.getProperty("smtp.starttls.enable", "true"));
        String from = props.getProperty("smtp.username", "");

        return new EmailService(host, port, from, username, password, tls);
    }

    private static Properties loadProperties() {
        Properties props = new Properties();
        try (InputStream is = NotificationSystemBootstrap.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (is != null) {
                props.load(is);
            }
        } catch (Exception e) {
            logger.warn("Impossibile leggere application.properties", e);
        }
        return props;
    }
}
