package it.zensoftware.luna2.dao;

import it.zensoftware.luna2.model.*;
import org.hibernate.Session;
import org.hibernate.query.Query;
import javax.persistence.criteria.*;
import java.util.*;

/**
 * ActivityDAO - Accesso dati per attività CRM
 */
public class ActivityDAO extends GenericDAOImpl<Activity, Long> {

    public ActivityDAO() {
        super(Activity.class);
    }

    public List<Activity> findByLead(Long leadId) {
        Session session = getSession();
        String hql = "FROM Activity WHERE lead.id = :leadId ORDER BY dataAttivita DESC";
        Query<Activity> query = session.createQuery(hql, Activity.class);
        query.setParameter("leadId", leadId);
        return query.list();
    }

    public List<Activity> findByLead(Lead lead) {
        return findByLead(lead.getId());
    }

    public List<Activity> findByUtente(Long utenteId) {
        Session session = getSession();
        String hql = "FROM Activity WHERE utente.id = :utenteId ORDER BY dataAttivita DESC";
        Query<Activity> query = session.createQuery(hql, Activity.class);
        query.setParameter("utenteId", utenteId);
        return query.list();
    }

    public List<Activity> findByTipo(Activity.ActivityType tipo) {
        Session session = getSession();
        String hql = "FROM Activity WHERE tipo = :tipo ORDER BY dataAttivita DESC";
        Query<Activity> query = session.createQuery(hql, Activity.class);
        query.setParameter("tipo", tipo);
        return query.list();
    }

    public List<Activity> findPending() {
        Session session = getSession();
        String hql = "FROM Activity WHERE stato = :stato ORDER BY dataAttivita ASC";
        Query<Activity> query = session.createQuery(hql, Activity.class);
        query.setParameter("stato", Activity.ActivityStatus.PENDING);
        return query.list();
    }

    public List<Activity> findPendingByUtente(Long utenteId) {
        Session session = getSession();
        String hql = "FROM Activity WHERE stato = :stato AND utente.id = :utenteId ORDER BY dataAttivita ASC";
        Query<Activity> query = session.createQuery(hql, Activity.class);
        query.setParameter("stato", Activity.ActivityStatus.PENDING);
        query.setParameter("utenteId", utenteId);
        return query.list();
    }

    public List<Activity> findBetweenDates(Date dataInizio, Date dataFine) {
        Session session = getSession();
        String hql = "FROM Activity WHERE dataAttivita BETWEEN :dataInizio AND :dataFine ORDER BY dataAttivita DESC";
        Query<Activity> query = session.createQuery(hql, Activity.class);
        query.setParameter("dataInizio", dataInizio);
        query.setParameter("dataFine", dataFine);
        return query.list();
    }

    public Long countByLead(Long leadId) {
        Session session = getSession();
        String hql = "SELECT COUNT(*) FROM Activity WHERE lead.id = :leadId";
        Query<Long> query = session.createQuery(hql, Long.class);
        query.setParameter("leadId", leadId);
        return query.uniqueResult();
    }

    public List<Activity> findAll() {
        Session session = getSession();
        String hql = "FROM Activity ORDER BY dataAttivita DESC";
        return session.createQuery(hql, Activity.class).list();
    }

    public List<Activity> findAll(int offset, int limit) {
        Session session = getSession();
        String hql = "FROM Activity ORDER BY dataAttivita DESC";
        Query<Activity> query = session.createQuery(hql, Activity.class);
        query.setFirstResult(offset);
        query.setMaxResults(limit);
        return query.list();
    }
}
