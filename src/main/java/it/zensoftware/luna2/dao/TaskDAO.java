package it.zensoftware.luna2.dao;

import it.zensoftware.luna2.model.*;
import org.hibernate.Session;
import org.hibernate.query.Query;
import java.util.*;

/**
 * TaskDAO - Accesso dati per task/follow-up CRM
 */
public class TaskDAO extends GenericDAOImpl<Task, Long> {

    public TaskDAO() {
        super(Task.class);
    }

    public List<Task> findByLead(Long leadId) {
        Session session = getSession();
        String hql = "FROM Task WHERE lead.id = :leadId ORDER BY dataScadenza ASC";
        Query<Task> query = session.createQuery(hql, Task.class);
        query.setParameter("leadId", leadId);
        return query.list();
    }

    public List<Task> findByLead(Lead lead) {
        return findByLead(lead.getId());
    }

    public List<Task> findByAssegnatoA(User utente) {
        Session session = getSession();
        String hql = "FROM Task WHERE assegnatoA.id = :utenteId ORDER BY dataScadenza ASC";
        Query<Task> query = session.createQuery(hql, Task.class);
        query.setParameter("utenteId", utente.getId());
        return query.list();
    }

    public List<Task> findOpenByAssegnatoA(User utente) {
        Session session = getSession();
        String hql = "FROM Task WHERE assegnatoA.id = :utenteId AND stato != :completato ORDER BY dataScadenza ASC";
        Query<Task> query = session.createQuery(hql, Task.class);
        query.setParameter("utenteId", utente.getId());
        query.setParameter("completato", Task.TaskStatus.COMPLETED);
        return query.list();
    }

    public List<Task> findOverdue() {
        Session session = getSession();
        String hql = "FROM Task WHERE dataScadenza < CURRENT_TIMESTAMP AND stato != :completato AND stato != :cancelled ORDER BY dataScadenza ASC";
        Query<Task> query = session.createQuery(hql, Task.class);
        query.setParameter("completato", Task.TaskStatus.COMPLETED);
        query.setParameter("cancelled", Task.TaskStatus.CANCELLED);
        return query.list();
    }

    public List<Task> findOpen() {
        Session session = getSession();
        String hql = "FROM Task WHERE stato IN (:open, :inProgress) ORDER BY dataScadenza ASC";
        Query<Task> query = session.createQuery(hql, Task.class);
        query.setParameterList("open", Arrays.asList(Task.TaskStatus.OPEN, Task.TaskStatus.IN_PROGRESS));
        return query.list();
    }

    public List<Task> findByPriority(Task.TaskPriority priorita) {
        Session session = getSession();
        String hql = "FROM Task WHERE priorita = :priorita ORDER BY dataScadenza ASC";
        Query<Task> query = session.createQuery(hql, Task.class);
        query.setParameter("priorita", priorita);
        return query.list();
    }

    public List<Task> findByStatus(Task.TaskStatus stato) {
        Session session = getSession();
        String hql = "FROM Task WHERE stato = :stato ORDER BY dataScadenza ASC";
        Query<Task> query = session.createQuery(hql, Task.class);
        query.setParameter("stato", stato);
        return query.list();
    }

    public Long countOpenByLead(Long leadId) {
        Session session = getSession();
        String hql = "SELECT COUNT(*) FROM Task WHERE lead.id = :leadId AND stato IN ('OPEN', 'IN_PROGRESS')";
        Query<Long> query = session.createQuery(hql, Long.class);
        query.setParameter("leadId", leadId);
        return query.uniqueResult();
    }

    public Long countOverdue() {
        Session session = getSession();
        String hql = "SELECT COUNT(*) FROM Task WHERE dataScadenza < CURRENT_TIMESTAMP AND stato != 'COMPLETED' AND stato != 'CANCELLED'";
        return session.createQuery(hql, Long.class).uniqueResult();
    }

    public List<Task> findAll() {
        Session session = getSession();
        String hql = "FROM Task ORDER BY dataScadenza ASC";
        return session.createQuery(hql, Task.class).list();
    }

    public List<Task> findAll(int offset, int limit) {
        Session session = getSession();
        String hql = "FROM Task ORDER BY dataScadenza ASC";
        Query<Task> query = session.createQuery(hql, Task.class);
        query.setFirstResult(offset);
        query.setMaxResults(limit);
        return query.list();
    }
}
