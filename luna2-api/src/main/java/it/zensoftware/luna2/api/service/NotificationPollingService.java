package it.zensoftware.luna2.api.service;

import it.zensoftware.luna2.dao.NotificationHistoryDAO;
import it.zensoftware.luna2.model.NotificationHistory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Polls notification_history and pushes updates to WebSocket clients.
 */
@Service
public class NotificationPollingService {

    private static final Logger logger = LogManager.getLogger(NotificationPollingService.class);
    private final NotificationHistoryDAO historyDAO = new NotificationHistoryDAO();
    private final DashboardNotificationService dashboardNotificationService;

    private volatile long lastId = 0L;

    public NotificationPollingService(DashboardNotificationService dashboardNotificationService) {
        this.dashboardNotificationService = dashboardNotificationService;
    }

    @Scheduled(fixedDelayString = "${dashboard.poll.interval-ms:5000}")
    public void pollNewNotifications() {
        List<NotificationHistory> items = historyDAO.findAfterId(lastId);
        if (items == null || items.isEmpty()) {
            return;
        }

        for (NotificationHistory item : items) {
            lastId = Math.max(lastId, item.getId());

            if (item.getChannel() != NotificationHistory.Channel.PUSH
                    && item.getChannel() != NotificationHistory.Channel.DATABASE) {
                continue;
            }

            dashboardNotificationService.notifyUser(
                item.getUserId(),
                item.getSubject(),
                item.getMessage(),
                item.getEventType(),
                null
            );
        }

        logger.debug("Pushed {} new notifications to WebSocket", items.size());
    }
}
