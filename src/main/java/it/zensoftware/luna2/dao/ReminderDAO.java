package it.zensoftware.luna2.dao;

import it.zensoftware.luna2.model.*;
import org.hibernate.Session;
import org.hibernate.query.Query;
import java.util.*;

/**
 * ReminderDAO - Accesso dati per reminder/notifiche
 */
public class ReminderDAO extends GenericDAOImpl<Reminder, Long> {

    public ReminderDAO() {
        super(Reminder.class);
    }

    public List<Reminder> findByUtente(Long utenteId) {
        Session session = getSession();
        String hql = "FROM Reminder WHERE utente.id = :utenteId ORDER BY dataNotifica ASC";
        Query<Reminder> query = session.createQuery(hql, Reminder.class);
        query.setParameter("utenteId", utenteId);
        return query.list();
    }

    public List<Reminder> findByUtente(User utente) {
        return findByUtente(utente.getId());
    }

    public List<Reminder> findPendingByUtente(Long utenteId) {
        Session session = getSession();
        String hql = "FROM Reminder WHERE utente.id = :utenteId AND stato = :pendente ORDER BY dataNotifica ASC";
        Query<Reminder> query = session.createQuery(hql, Reminder.class);
        query.setParameter("utenteId", utenteId);
        query.setParameter("pendente", Reminder.ReminderStatus.PENDING);
        return query.list();
    }

    public List<Reminder> findUnread() {
        Session session = getSession();
        String hql = "FROM Reminder WHERE letto = false AND stato != :dismissed ORDER BY dataNotifica ASC";
        Query<Reminder> query = session.createQuery(hql, Reminder.class);
        query.setParameter("dismissed", Reminder.ReminderStatus.DISMISSED);
        return query.list();
    }

    public List<Reminder> findUnreadByUtente(Long utenteId) {
        Session session = getSession();
        String hql = "FROM Reminder WHERE utente.id = :utenteId AND letto = false ORDER BY dataNotifica ASC";
        Query<Reminder> query = session.createQuery(hql, Reminder.class);
        query.setParameter("utenteId", utenteId);
        return query.list();
    }

    public List<Reminder> findDueReminders() {
        Session session = getSession();
        String hql = "FROM Reminder WHERE dataNotifica <= CURRENT_TIMESTAMP AND stato = :pendente ORDER BY dataNotifica ASC";
        Query<Reminder> query = session.createQuery(hql, Reminder.class);
        query.setParameter("pendente", Reminder.ReminderStatus.PENDING);
        return query.list();
    }

    public List<Reminder> findByLead(Long leadId) {
        Session session = getSession();
        String hql = "FROM Reminder WHERE lead.id = :leadId ORDER BY dataNotifica DESC";
        Query<Reminder> query = session.createQuery(hql, Reminder.class);
        query.setParameter("leadId", leadId);
        return query.list();
    }

    public List<Reminder> findByActivity(Long activityId) {
        Session session = getSession();
        String hql = "FROM Reminder WHERE activity.id = :activityId ORDER BY dataNotifica DESC";
        Query<Reminder> query = session.createQuery(hql, Reminder.class);
        query.setParameter("activityId", activityId);
        return query.list();
    }

    public List<Reminder> findByTask(Long taskId) {
        Session session = getSession();
        String hql = "FROM Reminder WHERE task.id = :taskId ORDER BY dataNotifica DESC";
        Query<Reminder> query = session.createQuery(hql, Reminder.class);
        query.setParameter("taskId", taskId);
        return query.list();
    }

    public List<Reminder> findByTipo(Reminder.ReminderType tipo) {
        Session session = getSession();
        String hql = "FROM Reminder WHERE tipo = :tipo ORDER BY dataNotifica DESC";
        Query<Reminder> query = session.createQuery(hql, Reminder.class);
        query.setParameter("tipo", tipo);
        return query.list();
    }

    public Long countUnreadByUtente(Long utenteId) {
        Session session = getSession();
        String hql = "SELECT COUNT(*) FROM Reminder WHERE utente.id = :utenteId AND letto = false";
        Query<Long> query = session.createQuery(hql, Long.class);
        query.setParameter("utenteId", utenteId);
        return query.uniqueResult();
    }

    public Long countPendingByUtente(Long utenteId) {
        Session session = getSession();
        String hql = "SELECT COUNT(*) FROM Reminder WHERE utente.id = :utenteId AND stato = :pendente";
        Query<Long> query = session.createQuery(hql, Long.class);
        query.setParameter("utenteId", utenteId);
        query.setParameter("pendente", Reminder.ReminderStatus.PENDING);
        return query.uniqueResult();
    }

    public List<Reminder> findBetweenDates(Date dataInizio, Date dataFine) {
        Session session = getSession();
        String hql = "FROM Reminder WHERE dataNotifica BETWEEN :dataInizio AND :dataFine ORDER BY dataNotifica DESC";
        Query<Reminder> query = session.createQuery(hql, Reminder.class);
        query.setParameter("dataInizio", dataInizio);
        query.setParameter("dataFine", dataFine);
        return query.list();
    }

    public List<Reminder> findAll() {
        Session session = getSession();
        String hql = "FROM Reminder ORDER BY dataNotifica DESC";
        return session.createQuery(hql, Reminder.class).list();
    }

    public List<Reminder> findAll(int offset, int limit) {
        Session session = getSession();
        String hql = "FROM Reminder ORDER BY dataNotifica DESC";
        Query<Reminder> query = session.createQuery(hql, Reminder.class);
        query.setFirstResult(offset);
        query.setMaxResults(limit);
        return query.list();
    }
}
