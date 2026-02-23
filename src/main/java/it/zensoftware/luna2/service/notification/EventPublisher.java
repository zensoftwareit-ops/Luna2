package it.zensoftware.luna2.service.notification;

import it.zensoftware.luna2.service.notification.event.NotificationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.*;

/**
 * EventPublisher implementa il pattern Publisher-Subscriber (Observer).
 * Consente ai listeners di registrarsi a specifici tipi di evento.
 * 
 * Utilizzo:
 * EventPublisher pub = EventPublisher.getInstance();
 * pub.subscribe("FATTURA_ARRIVATA", new EmailNotificationListener());
 * pub.subscribe("FATTURA_ARRIVATA", new PushNotificationListener());
 * 
 * FatturaArrivataEvent event = new FatturaArrivataEvent(...);
 * pub.publishEvent(event);
 * 
 * @author Notification System
 * @version 1.0
 */
public class EventPublisher {
    
    private static final Logger logger = LogManager.getLogger(EventPublisher.class);
    private static volatile EventPublisher instance;
    
    // Map di event type -> lista di listeners
    private final Map<String, List<EventListener>> listeners = new ConcurrentHashMap<>();
    
    // Executor per elaborazione asincrona eventi
    private final ExecutorService executorService = Executors.newFixedThreadPool(5);
    
    /**
     * Singleton pattern con double-checked locking
     */
    public static EventPublisher getInstance() {
        if (instance == null) {
            synchronized (EventPublisher.class) {
                if (instance == null) {
                    instance = new EventPublisher();
                }
            }
        }
        return instance;
    }
    
    private EventPublisher() {
    }
    
    /**
     * Registra un listener per un tipo di evento
     * 
     * @param eventType Tipo evento (es: "FATTURA_ARRIVATA")
     * @param listener Listener che riceverà notifiche
     */
    public void subscribe(String eventType, EventListener listener) {
        listeners.computeIfAbsent(eventType, k -> Collections.synchronizedList(new ArrayList<>()))
                .add(listener);
        
        logger.info("Listener registrato: " + listener.getClass().getSimpleName() + 
                   " per evento: " + eventType);
    }
    
    /**
     * Deregistra un listener
     */
    public void unsubscribe(String eventType, EventListener listener) {
        List<EventListener> eventListeners = listeners.get(eventType);
        if (eventListeners != null) {
            eventListeners.remove(listener);
        }
    }
    
    /**
     * Pubblica un evento a tutti i listeners registrati
     * Elaborazione ASINCRONA per non bloccare il flusso principale
     * 
     * @param event Evento da pubblicare
     */
    public void publishEvent(NotificationEvent event) {
        String eventType = event.getEventType();
        
        List<EventListener> eventListeners = listeners.get(eventType);
        
        if (eventListeners == null || eventListeners.isEmpty()) {
            logger.debug("Nessun listener registrato per evento: " + eventType);
            return;
        }
        
        // Elaborazione asincrona
        executorService.submit(() -> {
            logger.info("Publishing event: " + event.toString() + 
                       " | Listeners: " + eventListeners.size());
            
            for (EventListener listener : new ArrayList<>(eventListeners)) {
                try {
                    listener.onEventPublished(event);
                } catch (Exception e) {
                    logger.error("Error in listener: " + listener.getClass().getSimpleName(), e);
                }
            }
        });
    }
    
    /**
     * Pubblica evento in modo SINCRONO (blocca il caller)
     * Utilizzare solo per operazioni critiche
     */
    public void publishEventSync(NotificationEvent event) {
        String eventType = event.getEventType();
        List<EventListener> eventListeners = listeners.get(eventType);
        
        if (eventListeners == null || eventListeners.isEmpty()) {
            logger.debug("Nessun listener per evento: " + eventType);
            return;
        }
        
        for (EventListener listener : new ArrayList<>(eventListeners)) {
            try {
                listener.onEventPublished(event);
            } catch (Exception e) {
                logger.error("Errore in listener: " + listener.getClass().getSimpleName(), e);
            }
        }
    }
    
    /**
     * Ritorna numero listeners per un evento
     */
    public int getListenerCount(String eventType) {
        List<EventListener> eventListeners = listeners.get(eventType);
        return eventListeners != null ? eventListeners.size() : 0;
    }
    
    /**
     * Ritorna numero totale listeners registrati
     */
    public int getTotalListenerCount() {
        return listeners.values().stream().mapToInt(List::size).sum();
    }
    
    /**
     * Shutdown dell'executor (chiamare al exit dell'app)
     */
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
