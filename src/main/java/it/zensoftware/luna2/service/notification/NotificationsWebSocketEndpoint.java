package it.zensoftware.luna2.service.notification;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;

/**
 * WebSocket endpoint per notifiche real-time.
 */
@ServerEndpoint("/ws/notifications/{userId}")
public class NotificationsWebSocketEndpoint {

    private static final Logger logger = LogManager.getLogger(NotificationsWebSocketEndpoint.class);

    private String userId;
    private PushConnection connection;

    @OnOpen
    public void onOpen(Session session, @PathParam("userId") String userId) {
        this.userId = userId;
        this.connection = new WebSocketPushConnection(session);
        PushNotificationService.getInstance().registerConnection(userId, connection);
        logger.info("WebSocket aperta per utente: " + userId);
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        // Echo o ping handler: mantenerlo leggero
        logger.debug("WebSocket message ricevuto da " + userId + ": " + message);
    }

    @OnClose
    public void onClose(Session session, CloseReason reason) {
        if (userId != null && connection != null) {
            PushNotificationService.getInstance().unregisterConnection(userId, connection);
        }
        logger.info("WebSocket chiusa per utente: " + userId + " reason=" + reason);
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        logger.warn("Errore WebSocket per utente: " + userId, throwable);
        if (userId != null && connection != null) {
            PushNotificationService.getInstance().unregisterConnection(userId, connection);
        }
        try {
            session.close();
        } catch (IOException e) {
            logger.debug("Errore chiusura session WebSocket", e);
        }
    }

    private static class WebSocketPushConnection implements PushConnection {
        private final Session session;

        private WebSocketPushConnection(Session session) {
            this.session = session;
        }

        @Override
        public void send(String message) throws Exception {
            if (session.isOpen()) {
                session.getBasicRemote().sendText(message);
            }
        }

        @Override
        public void close() throws Exception {
            if (session.isOpen()) {
                session.close();
            }
        }

        @Override
        public boolean isOpen() {
            return session.isOpen();
        }
    }
}
