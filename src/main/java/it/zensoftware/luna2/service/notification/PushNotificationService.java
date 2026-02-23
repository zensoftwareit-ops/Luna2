package it.zensoftware.luna2.service.notification;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * PushNotificationService gestisce notifiche push ai client.
 * Supporta WebSocket e Server-Sent Events (SSE).
 * 
 * Utilizzo WebSocket:
 * - Client connette al WebSocket endpoint: /ws/notifications/{userId}
 * - Server invia notifiche quando evento è pubblicato
 * - Notifiche in tempo reale con latenza < 100ms
 * 
 * Utilizzo SSE:
 * - Client connette a: /api/notifications/stream/{userId}
 * - Server invia notifiche via Server-Sent Events
 * - Compatible con browser pure HTTP (no WebSocket)
 * 
 * @author Notification System
 * @version 1.0
 */
public class PushNotificationService {
    
    private static final Logger logger = LogManager.getLogger(PushNotificationService.class);
    
    // Map di userId -> lista notifiche in coda
    private final Map<String, Queue<PushNotification>> userNotificationQueues = new ConcurrentHashMap<>();
    
    // Map di userId -> connessioni WebSocket attive
    private final Map<String, List<PushConnection>> userConnections = new ConcurrentHashMap<>();
    
    // Map di userId -> lista di notifiche non ancora consegnate (per SSE retry)
    private final Map<String, List<PushNotification>> undeliveredNotifications = new ConcurrentHashMap<>();
    
    private static volatile PushNotificationService instance;
    
    /**
     * Singleton pattern
     */
    public static PushNotificationService getInstance() {
        if (instance == null) {
            synchronized (PushNotificationService.class) {
                if (instance == null) {
                    instance = new PushNotificationService();
                }
            }
        }
        return instance;
    }
    
    private PushNotificationService() {
    }
    
    /**
     * Registra una nuova connessione WebSocket per utente
     * 
     * @param userId ID utente
     * @param connection Connessione WebSocket
     */
    public void registerConnection(String userId, PushConnection connection) {
        userConnections.computeIfAbsent(userId, k -> Collections.synchronizedList(new ArrayList<>()))
                .add(connection);
        
        logger.info("WebSocket connessione registrata per utente: " + userId);
        
        // Invia notifiche in coda non ancora consegnate
        sendPendingNotifications(userId);
    }
    
    /**
     * Deregistra connessione WebSocket
     */
    public void unregisterConnection(String userId, PushConnection connection) {
        List<PushConnection> connections = userConnections.get(userId);
        if (connections != null) {
            connections.remove(connection);
            if (connections.isEmpty()) {
                userConnections.remove(userId);
            }
        }
        
        logger.info("WebSocket connessione rimossa per utente: " + userId);
    }
    
    /**
     * Invia notifica push a utente specifico
     * 
     * @param userId ID utente destinatario
     * @param notification Notifica da inviare
     */
    public void sendNotificationToUser(String userId, PushNotification notification) {
        List<PushConnection> connections = userConnections.get(userId);
        
        if (connections == null || connections.isEmpty()) {
            // Nessuna connessione attiva, salva in coda
            logger.debug("Nessuna connessione attiva per utente " + userId + ", notifica in coda");
            queueNotification(userId, notification);
            return;
        }
        
        // Invia a tutte le connessioni attive dell'utente
        for (PushConnection connection : new ArrayList<>(connections)) {
            try {
                connection.send(notification.toJson());
            } catch (Exception e) {
                logger.error("Errore invio notifica push", e);
            }
        }
    }
    
    /**
     * Invia notifica a tutti gli utenti (broadcast)
     */
    public void broadcastNotification(PushNotification notification) {
        userConnections.forEach((userId, connections) -> {
            for (PushConnection connection : new ArrayList<>(connections)) {
                try {
                    connection.send(notification.toJson());
                } catch (Exception e) {
                    logger.error("Errore broadcast notifica", e);
                }
            }
        });
    }
    
    /**
     * Mette notifica in coda se utente non connesso
     */
    private void queueNotification(String userId, PushNotification notification) {
        Queue<PushNotification> queue = userNotificationQueues.computeIfAbsent(userId, 
                k -> new ConcurrentLinkedQueue<>());
        queue.offer(notification);
        
        // Limita dimensione coda a 100 notifiche
        if (queue.size() > 100) {
            queue.poll();  // Rimuove la più vecchia
        }
    }
    
    /**
     * Invia notifiche in coda quando utente si connette
     */
    private void sendPendingNotifications(String userId) {
        Queue<PushNotification> queue = userNotificationQueues.get(userId);
        if (queue == null || queue.isEmpty()) {
            return;
        }
        
        List<PushConnection> connections = userConnections.get(userId);
        if (connections == null || connections.isEmpty()) {
            return;
        }
        
        PushNotification notification;
        while ((notification = queue.poll()) != null) {
            for (PushConnection connection : new ArrayList<>(connections)) {
                try {
                    connection.send(notification.toJson());
                } catch (Exception e) {
                    logger.error("Errore invio notifica pending", e);
                    queue.offer(notification);  // Rimetti in coda
                }
            }
        }
    }
    
    /**
     * Ritorna numero di utenti con connessioni attive
     */
    public int getActiveConnectionCount() {
        return userConnections.size();
    }
    
    /**
     * Ritorna numero di notifiche in coda per utente
     */
    public int getPendingNotificationCount(String userId) {
        Queue<PushNotification> queue = userNotificationQueues.get(userId);
        return queue != null ? queue.size() : 0;
    }
}

/**
 * PushNotification rappresenta una singola notifica push
 */
class PushNotification {
    private final String id;
    private final String userId;
    private final String title;
    private final String message;
    private final String icon;  // URL icona (emoji o immagine)
    private final String action;  // URL azione al click
    private final Map<String, Object> data;  // Dati custom
    private final long timestamp;
    private final String priority;  // HIGH, NORMAL, LOW
    
    public PushNotification(String userId, String title, String message, String priority) {
        this.id = java.util.UUID.randomUUID().toString();
        this.userId = userId;
        this.title = title;
        this.message = message;
        this.icon = null;
        this.action = null;
        this.data = new HashMap<>();
        this.timestamp = System.currentTimeMillis();
        this.priority = priority;
    }
    
    public void addData(String key, Object value) {
        data.put(key, value);
    }
    
    /**
     * Converti a JSON per WebSocket
     */
    public String toJson() {
        return "{" +
                "\"id\": \"" + id + "\"," +
                "\"title\": \"" + title + "\"," +
                "\"message\": \"" + message + "\"," +
                "\"priority\": \"" + priority + "\"," +
                "\"timestamp\": " + timestamp + "," +
                "\"icon\": " + (icon != null ? "\"" + icon + "\"" : "null") + "," +
                "\"action\": " + (action != null ? "\"" + action + "\"" : "null") + "," +
                "\"data\": " + dataToJson() +
                "}";
    }
    
    private String dataToJson() {
        StringBuilder sb = new StringBuilder("{");
        data.forEach((key, value) -> {
            if (value instanceof String) {
                sb.append("\"").append(key).append("\": \"").append(value).append("\",");
            } else {
                sb.append("\"").append(key).append("\": ").append(value).append(",");
            }
        });
        if (data.size() > 0) {
            sb.setLength(sb.length() - 1);  // Rimuovi ultima virgola
        }
        sb.append("}");
        return sb.toString();
    }
    
    // Getters
    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
}

/**
 * PushConnection rappresenta una connessione WebSocket/SSE attiva
 */
interface PushConnection {
    void send(String message) throws Exception;
    void close() throws Exception;
    boolean isOpen();
}
