package it.zensoftware.luna2.dao;

import it.zensoftware.luna2.model.NotificationPreference;
import org.hibernate.Session;
import org.hibernate.query.Query;

public class NotificationPreferenceDAO extends GenericDAOImpl<NotificationPreference, Long> {

    public NotificationPreferenceDAO() {
        super(NotificationPreference.class);
    }

    public NotificationPreference findByUserId(String userId) {
        try (Session session = getSession()) {
            Query<NotificationPreference> query = session.createQuery(
                    "FROM NotificationPreference WHERE userId = :userId", NotificationPreference.class);
            query.setParameter("userId", userId);
            return query.uniqueResult();
        }
    }
}
