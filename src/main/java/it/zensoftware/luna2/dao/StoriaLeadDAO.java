package it.zensoftware.luna2.dao;

import it.zensoftware.luna2.model.*;
import org.hibernate.Session;
import org.hibernate.query.Query;
import java.util.*;

/**
 * StoriaLeadDAO - Accesso dati per audit trail di lead (cambio stage)
 */
public class StoriaLeadDAO extends GenericDAOImpl<StoriaLead, Long> {

    public StoriaLeadDAO() {
        super(StoriaLead.class);
    }

    public List<StoriaLead> findByLead(Long leadId) {
        Session session = getSession();
        String hql = "FROM StoriaLead WHERE lead.id = :leadId ORDER BY dataCambio DESC";
        Query<StoriaLead> query = session.createQuery(hql, StoriaLead.class);
        query.setParameter("leadId", leadId);
        return query.list();
    }

    public List<StoriaLead> findByLead(Lead lead) {
        return findByLead(lead.getId());
    }

    public List<StoriaLead> findByLeadAndStage(Long leadId, String stage) {
        Session session = getSession();
        String hql = "FROM StoriaLead WHERE lead.id = :leadId AND (stageDa = :stage OR stageA = :stage) ORDER BY dataCambio DESC";
        Query<StoriaLead> query = session.createQuery(hql, StoriaLead.class);
        query.setParameter("leadId", leadId);
        query.setParameter("stage", stage);
        return query.list();
    }

    public StoriaLead findLastStageBylead(Long leadId) {
        Session session = getSession();
        String hql = "FROM StoriaLead WHERE lead.id = :leadId ORDER BY dataCambio DESC";
        Query<StoriaLead> query = session.createQuery(hql, StoriaLead.class);
        query.setParameter("leadId", leadId);
        query.setMaxResults(1);
        List<StoriaLead> results = query.list();
        return results.isEmpty() ? null : results.get(0);
    }

    public List<StoriaLead> findByStageA(String stage) {
        Session session = getSession();
        String hql = "FROM StoriaLead WHERE stageA = :stage ORDER BY dataCambio DESC";
        Query<StoriaLead> query = session.createQuery(hql, StoriaLead.class);
        query.setParameter("stage", stage);
        return query.list();
    }

    public List<StoriaLead> findByUtente(Long utenteId) {
        Session session = getSession();
        String hql = "FROM StoriaLead WHERE utente.id = :utenteId ORDER BY dataCambio DESC";
        Query<StoriaLead> query = session.createQuery(hql, StoriaLead.class);
        query.setParameter("utenteId", utenteId);
        return query.list();
    }

    public List<StoriaLead> findBetweenDates(Date dataInizio, Date dataFine) {
        Session session = getSession();
        String hql = "FROM StoriaLead WHERE dataCambio BETWEEN :dataInizio AND :dataFine ORDER BY dataCambio DESC";
        Query<StoriaLead> query = session.createQuery(hql, StoriaLead.class);
        query.setParameter("dataInizio", dataInizio);
        query.setParameter("dataFine", dataFine);
        return query.list();
    }

    public Long countByLead(Long leadId) {
        Session session = getSession();
        String hql = "SELECT COUNT(*) FROM StoriaLead WHERE lead.id = :leadId";
        Query<Long> query = session.createQuery(hql, Long.class);
        query.setParameter("leadId", leadId);
        return query.uniqueResult();
    }

    public Long countByStageA(String stage) {
        Session session = getSession();
        String hql = "SELECT COUNT(*) FROM StoriaLead WHERE stageA = :stage";
        Query<Long> query = session.createQuery(hql, Long.class);
        query.setParameter("stage", stage);
        return query.uniqueResult();
    }

    /**
     * Restituisce il numero di transizioni verso stage vincente (VINTO, PERSO)
     */
    public Long countByFinalStages() {
        Session session = getSession();
        String hql = "SELECT COUNT(*) FROM StoriaLead WHERE stageA IN ('VINTO', 'PERSO')";
        return session.createQuery(hql, Long.class).uniqueResult();
    }

    /**
     * Calcola il tasso di conversione QUALIFICATO -> VINTO vs PERSO
     */
    public Map<String, Long> getConversionRateByStage() {
        Session session = getSession();
        String hql = "SELECT stageA, COUNT(*) FROM StoriaLead WHERE stageA IN ('VINTO', 'PERSO') GROUP BY stageA";
        List<Object[]> results = session.createQuery(hql).list();
        
        Map<String, Long> map = new HashMap<>();
        for (Object[] row : results) {
            map.put((String) row[0], (Long) row[1]);
        }
        return map;
    }

    public List<StoriaLead> findAll() {
        Session session = getSession();
        String hql = "FROM StoriaLead ORDER BY dataCambio DESC";
        return session.createQuery(hql, StoriaLead.class).list();
    }

    public List<StoriaLead> findAll(int offset, int limit) {
        Session session = getSession();
        String hql = "FROM StoriaLead ORDER BY dataCambio DESC";
        Query<StoriaLead> query = session.createQuery(hql, StoriaLead.class);
        query.setFirstResult(offset);
        query.setMaxResults(limit);
        return query.list();
    }
}
