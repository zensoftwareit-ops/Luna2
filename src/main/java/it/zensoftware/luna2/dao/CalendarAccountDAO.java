package it.zensoftware.luna2.dao;

import it.zensoftware.luna2.model.CalendarAccount;
import org.hibernate.Session;
import org.hibernate.query.Query;

import java.util.List;

public class CalendarAccountDAO extends GenericDAOImpl<CalendarAccount, Long> {

    public CalendarAccountDAO() {
        super(CalendarAccount.class);
    }

    public List<CalendarAccount> findByUserId(String userId) {
        try (Session session = getSession()) {
            Query<CalendarAccount> query = session.createQuery(
                    "FROM CalendarAccount WHERE userId = :userId", CalendarAccount.class);
            query.setParameter("userId", userId);
            return query.list();
        }
    }

    public List<CalendarAccount> findEnabled() {
        try (Session session = getSession()) {
            Query<CalendarAccount> query = session.createQuery(
                    "FROM CalendarAccount WHERE syncEnabled = true", CalendarAccount.class);
            return query.list();
        }
    }

    public CalendarAccount findByUserAndProvider(String userId, CalendarAccount.Provider provider) {
        try (Session session = getSession()) {
            Query<CalendarAccount> query = session.createQuery(
                    "FROM CalendarAccount WHERE userId = :userId AND provider = :provider", CalendarAccount.class);
            query.setParameter("userId", userId);
            query.setParameter("provider", provider);
            return query.uniqueResult();
        }
    }
}
