package it.zensoftware.luna2.dao;

import it.zensoftware.luna2.model.CalendarAccount;
import it.zensoftware.luna2.model.CalendarEvent;
import org.hibernate.Session;
import org.hibernate.query.Query;

import java.util.List;

public class CalendarEventDAO extends GenericDAOImpl<CalendarEvent, Long> {

    public CalendarEventDAO() {
        super(CalendarEvent.class);
    }

    public List<CalendarEvent> findByUserId(String userId) {
        try (Session session = getSession()) {
            Query<CalendarEvent> query = session.createQuery(
                    "FROM CalendarEvent WHERE userId = :userId", CalendarEvent.class);
            query.setParameter("userId", userId);
            return query.list();
        }
    }

    public CalendarEvent findByExternalId(String userId, CalendarAccount.Provider provider, String externalId) {
        try (Session session = getSession()) {
            Query<CalendarEvent> query = session.createQuery(
                    "FROM CalendarEvent WHERE userId = :userId AND provider = :provider AND externalEventId = :externalId",
                    CalendarEvent.class);
            query.setParameter("userId", userId);
            query.setParameter("provider", provider);
            query.setParameter("externalId", externalId);
            return query.uniqueResult();
        }
    }

    public CalendarEvent findBySource(String userId, CalendarAccount.Provider provider,
                                      CalendarEvent.SourceType sourceType, Long sourceId) {
        try (Session session = getSession()) {
            Query<CalendarEvent> query = session.createQuery(
                    "FROM CalendarEvent WHERE userId = :userId AND provider = :provider AND sourceType = :sourceType AND sourceId = :sourceId",
                    CalendarEvent.class);
            query.setParameter("userId", userId);
            query.setParameter("provider", provider);
            query.setParameter("sourceType", sourceType);
            query.setParameter("sourceId", sourceId);
            return query.uniqueResult();
        }
    }
}
