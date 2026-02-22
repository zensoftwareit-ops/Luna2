package it.zensoftware.luna2.dao;

import it.zensoftware.luna2.model.*;
import org.hibernate.Session;
import org.hibernate.query.Query;
import java.util.*;

/**
 * PipelineStageDAO - Accesso dati per stage della pipeline
 */
public class PipelineStageDAO extends GenericDAOImpl<PipelineStage, Long> {

    public PipelineStageDAO() {
        super(PipelineStage.class);
    }

    public PipelineStage findByName(String nome) {
        Session session = getSession();
        String hql = "FROM PipelineStage WHERE nome = :nome";
        Query<PipelineStage> query = session.createQuery(hql, PipelineStage.class);
        query.setParameter("nome", nome);
        List<PipelineStage> results = query.list();
        return results.isEmpty() ? null : results.get(0);
    }

    public List<PipelineStage> findAll() {
        Session session = getSession();
        String hql = "FROM PipelineStage ORDER BY sequenza ASC";
        return session.createQuery(hql, PipelineStage.class).list();
    }

    public List<PipelineStage> findActive() {
        Session session = getSession();
        String hql = "FROM PipelineStage WHERE attivo = true ORDER BY sequenza ASC";
        return session.createQuery(hql, PipelineStage.class).list();
    }

    public List<PipelineStage> findFinalStages() {
        Session session = getSession();
        String hql = "FROM PipelineStage WHERE isFinalStage = true ORDER BY sequenza ASC";
        return session.createQuery(hql, PipelineStage.class).list();
    }

    public Long countLeadsByStage(Long stageId) {
        Session session = getSession();
        String hql = "SELECT COUNT(*) FROM Lead WHERE pipelineStage.id = :stageId";
        Query<Long> query = session.createQuery(hql, Long.class);
        query.setParameter("stageId", stageId);
        return query.uniqueResult();
    }

    public List<Lead> findLeadsByStage(Long stageId) {
        Session session = getSession();
        String hql = "FROM Lead WHERE pipelineStage.id = :stageId ORDER BY dataContatto DESC";
        Query<Lead> query = session.createQuery(hql, Lead.class);
        query.setParameter("stageId", stageId);
        return query.list();
    }

    public Map<String, Long> getLeadCountByStage() {
        Session session = getSession();
        String hql = "SELECT ps.nome, COUNT(l) FROM PipelineStage ps LEFT JOIN ps.leads l GROUP BY ps.id, ps.nome ORDER BY ps.sequenza";
        List<Object[]> results = session.createQuery(hql).list();
        
        Map<String, Long> map = new LinkedHashMap<>();
        for (Object[] row : results) {
            map.put((String) row[0], (Long) row[1]);
        }
        return map;
    }

    public Double getAverageValueByStage(Long stageId) {
        Session session = getSession();
        String hql = "SELECT AVG(CAST(l.budgetStimato * l.probabilitaChiusura / 100 AS double)) FROM Lead l WHERE l.pipelineStage.id = :stageId";
        Query<Double> query = session.createQuery(hql, Double.class);
        query.setParameter("stageId", stageId);
        Double result = query.uniqueResult();
        return result != null ? result : 0.0;
    }

    public Double getTotalValueByStage(Long stageId) {
        Session session = getSession();
        String hql = "SELECT SUM(CAST(l.budgetStimato * l.probabilitaChiusura / 100 AS double)) FROM Lead l WHERE l.pipelineStage.id = :stageId";
        Query<Double> query = session.createQuery(hql, Double.class);
        query.setParameter("stageId", stageId);
        Double result = query.uniqueResult();
        return result != null ? result : 0.0;
    }

    public PipelineStage findNextStage(Long currentStageId) {
        PipelineStage current = findById(currentStageId);
        if (current == null) return null;
        
        Session session = getSession();
        String hql = "FROM PipelineStage WHERE sequenza > :sequenza AND attivo = true ORDER BY sequenza ASC";
        Query<PipelineStage> query = session.createQuery(hql, PipelineStage.class);
        query.setParameter("sequenza", current.getSequenza());
        query.setMaxResults(1);
        
        List<PipelineStage> results = query.list();
        return results.isEmpty() ? null : results.get(0);
    }

    public PipelineStage findPreviousStage(Long currentStageId) {
        PipelineStage current = findById(currentStageId);
        if (current == null) return null;
        
        Session session = getSession();
        String hql = "FROM PipelineStage WHERE sequenza < :sequenza AND attivo = true ORDER BY sequenza DESC";
        Query<PipelineStage> query = session.createQuery(hql, PipelineStage.class);
        query.setParameter("sequenza", current.getSequenza());
        query.setMaxResults(1);
        
        List<PipelineStage> results = query.list();
        return results.isEmpty() ? null : results.get(0);
    }

    public Integer getMaxSequenza() {
        Session session = getSession();
        String hql = "SELECT MAX(sequenza) FROM PipelineStage";
        Integer result = (Integer) session.createQuery(hql).uniqueResult();
        return result != null ? result : 0;
    }
}
