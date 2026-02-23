package it.zensoftware.luna2.dao;

import it.zensoftware.luna2.model.NotificationHistory;

public class NotificationHistoryDAO extends GenericDAOImpl<NotificationHistory, Long> {

    public NotificationHistoryDAO() {
        super(NotificationHistory.class);
    }

    public java.util.List<NotificationHistory> findAfterId(Long lastId) {
        try (org.hibernate.Session session = getSession()) {
            org.hibernate.query.Query<NotificationHistory> query = session.createQuery(
                    "FROM NotificationHistory WHERE id > :lastId ORDER BY id ASC",
                    NotificationHistory.class);
            query.setParameter("lastId", lastId != null ? lastId : 0L);
            return query.list();
        }
    }
}
