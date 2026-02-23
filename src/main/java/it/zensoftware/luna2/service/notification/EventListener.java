package it.zensoftware.luna2.service.notification;

import it.zensoftware.luna2.service.notification.event.NotificationEvent;

/**
 * EventListener interface - Base per tutti i listener di notifiche.
 * Implementare per creare listener custom per specifici tipi di evento.
 * 
 * @author Notification System
 * @version 1.0
 */
public interface EventListener {
    
    /**
     * Viene chiamato quando un evento è pubblicato
     * 
     * @param event Evento pubblicato
     */
    void onEventPublished(NotificationEvent event);
}
