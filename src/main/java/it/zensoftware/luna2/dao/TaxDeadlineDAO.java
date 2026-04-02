package it.zensoftware.luna2.dao;

import it.zensoftware.luna2.model.TaxDeadline;
import it.zensoftware.luna2.model.TaxDeadline.DeadlineStatus;
import it.zensoftware.luna2.model.TaxDeadline.DeadlineType;
import org.hibernate.Session;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class TaxDeadlineDAO extends GenericDAOImpl<TaxDeadline, Long> {

    public TaxDeadlineDAO() {
        super(TaxDeadline.class);
    }

    public List<TaxDeadline> findUpcoming(int limit) {
        try (Session session = getSession()) {
            return session.createQuery(
                    "FROM TaxDeadline t ORDER BY t.deadlineDate ASC, t.id ASC", TaxDeadline.class)
                    .setMaxResults(limit)
                    .getResultList();
        }
    }

    public List<TaxDeadline> findAllOrdered() {
        try (Session session = getSession()) {
            return session.createQuery(
                    "FROM TaxDeadline t ORDER BY t.deadlineDate ASC, t.id DESC", TaxDeadline.class)
                    .getResultList();
        }
    }

    public long countOpen() {
        try (Session session = getSession()) {
            return session.createQuery(
                    "SELECT COUNT(t.id) FROM TaxDeadline t WHERE t.status = :status", Long.class)
                    .setParameter("status", TaxDeadline.DeadlineStatus.OPEN)
                    .uniqueResult();
        }
    }

    public long countOverdue(Date today) {
        try (Session session = getSession()) {
            return session.createQuery(
                    "SELECT COUNT(t.id) FROM TaxDeadline t WHERE t.status <> :completed AND t.deadlineDate < :today", Long.class)
                    .setParameter("completed", TaxDeadline.DeadlineStatus.COMPLETED)
                    .setParameter("today", today)
                    .uniqueResult();
        }
    }

    /**
     * Trova tutte le scadenze aperte ordinate per data
     */
    public List<TaxDeadline> findOpenDeadlines() {
        try (Session session = getSession()) {
            return session.createQuery(
                    "FROM TaxDeadline t WHERE t.status = :status ORDER BY t.deadlineDate ASC",
                    TaxDeadline.class)
                    .setParameter("status", DeadlineStatus.OPEN)
                    .getResultList();
        }
    }

    /**
     * Trova scadenze scadute
     */
    public List<TaxDeadline> findOverdueDeadlines(Date beforeDate) {
        try (Session session = getSession()) {
            return session.createQuery(
                    "FROM TaxDeadline t WHERE t.status = :status AND t.deadlineDate <= :date ORDER BY t.deadlineDate ASC",
                    TaxDeadline.class)
                    .setParameter("status", DeadlineStatus.OPEN)
                    .setParameter("date", beforeDate)
                    .getResultList();
        }
    }

    /**
     * Trova scadenze imminenti nei prossimi N giorni
     */
    public List<TaxDeadline> findUpcomingDeadlines(int daysAhead) {
        Calendar cal = Calendar.getInstance();
        Date today = new Date();
        cal.setTime(today);
        cal.add(Calendar.DAY_OF_MONTH, daysAhead);
        Date futureDate = cal.getTime();

        try (Session session = getSession()) {
            return session.createQuery(
                    "FROM TaxDeadline t WHERE t.status = :status AND t.deadlineDate >= :today AND t.deadlineDate <= :future ORDER BY t.deadlineDate ASC",
                    TaxDeadline.class)
                    .setParameter("status", DeadlineStatus.OPEN)
                    .setParameter("today", today)
                    .setParameter("future", futureDate)
                    .getResultList();
        }
    }

    /**
     * Conta scadenze per tipo
     */
    public long countByType(DeadlineType type) {
        try (Session session = getSession()) {
            return session.createQuery(
                    "SELECT COUNT(t) FROM TaxDeadline t WHERE t.type = :type AND t.status = :status",
                    Long.class)
                    .setParameter("type", type)
                    .setParameter("status", DeadlineStatus.OPEN)
                    .uniqueResult();
        }
    }

    /**
     * Conta scadenze nei prossimi N giorni
     */
    public long countUpcoming(int days) {
        Calendar cal = Calendar.getInstance();
        Date today = new Date();
        cal.setTime(today);
        cal.add(Calendar.DAY_OF_MONTH, days);
        Date futureDate = cal.getTime();

        try (Session session = getSession()) {
            return session.createQuery(
                    "SELECT COUNT(t) FROM TaxDeadline t WHERE t.status = :status AND t.deadlineDate >= :today AND t.deadlineDate <= :future",
                    Long.class)
                    .setParameter("status", DeadlineStatus.OPEN)
                    .setParameter("today", today)
                    .setParameter("future", futureDate)
                    .uniqueResult();
        }
    }

    /**
     * Marca una scadenza come completata
     */
    public void completeDeadline(Long deadlineId) {
        try (Session session = getSession()) {
            session.beginTransaction();
            TaxDeadline deadline = session.find(TaxDeadline.class, deadlineId);
            if (deadline != null) {
                deadline.setStatus(DeadlineStatus.COMPLETED);
                deadline.setCompletedAt(new Date());
                session.merge(deadline);
            }
            session.getTransaction().commit();
        }
    }

    /**
     * Cerca scadenze per tipo nell'anno corrente
     */
    public List<TaxDeadline> findByTypeAndYear(DeadlineType type, Integer year) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, Calendar.JANUARY);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        Date startYear = cal.getTime();

        cal.set(Calendar.MONTH, Calendar.DECEMBER);
        cal.set(Calendar.DAY_OF_MONTH, 31);
        Date endYear = cal.getTime();

        try (Session session = getSession()) {
            return session.createQuery(
                    "FROM TaxDeadline t WHERE t.type = :type AND t.deadlineDate >= :start AND t.deadlineDate <= :end ORDER BY t.deadlineDate ASC",
                    TaxDeadline.class)
                    .setParameter("type", type)
                    .setParameter("start", startYear)
                    .setParameter("end", endYear)
                    .getResultList();
        }
    }
}