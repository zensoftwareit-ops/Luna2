package it.zensoftware.luna2.dao;

import it.zensoftware.luna2.model.NoleggioValutazione;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Date;
import java.util.List;

/**
 * DAO for NoleggioValutazione entity
 * Manages financial evaluation process with document solicitation
 */
public class NoleggioValutazioneDAO extends GenericDAOImpl<NoleggioValutazione, Long> {
    
    private static final Logger logger = LogManager.getLogger(NoleggioValutazioneDAO.class);

    public NoleggioValutazioneDAO() {
        super(NoleggioValutazione.class);
    }

    /**
     * Find valuation by lead
     */
    public NoleggioValutazione findByLead(Long leadId) {
        try (Session session = getSession()) {
            Query<NoleggioValutazione> query = session.createQuery(
                "FROM NoleggioValutazione WHERE lead.id = :leadId", 
                NoleggioValutazione.class);
            query.setParameter("leadId", leadId);
            return query.uniqueResult();
        } catch (Exception e) {
            logger.error("Error finding valutazione by lead", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Find valuations by status
     */
    public List<NoleggioValutazione> findByStatus(NoleggioValutazione.Status status) {
        try (Session session = getSession()) {
            Query<NoleggioValutazione> query = session.createQuery(
                "FROM NoleggioValutazione WHERE status = :status ORDER BY dataAvvioIstruttoria ASC", 
                NoleggioValutazione.class);
            query.setParameter("status", status);
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding valutazioni by status", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Find valuations requiring document solicitation (every 4-5 days)
     * AUTOMATION: Called by NoleggioAutomationService
     */
    public List<NoleggioValutazione> findRequiringSollecito(int giorniSoglia) {
        try (Session session = getSession()) {
            Date soglia = new Date(System.currentTimeMillis() - (giorniSoglia * 24L * 60 * 60 * 1000));
            
            Query<NoleggioValutazione> query = session.createQuery(
                "FROM NoleggioValutazione WHERE status IN (:statuses) " +
                "AND documentiMancantiCount > 0 " +
                "AND (dataUltimoSollecito IS NULL OR dataUltimoSollecito <= :soglia) " +
                "ORDER BY dataAvvioIstruttoria ASC", 
                NoleggioValutazione.class);
            query.setParameterList("statuses", 
                List.of(NoleggioValutazione.Status.DOCUMENTI_MANCANTI, 
                       NoleggioValutazione.Status.SOLLECITI_INVIATI));
            query.setParameter("soglia", soglia);
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding valutazioni requiring sollecito", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Find stalled valuations (> 7 days without complete documents)
     * AUTOMATION: Alert for operator
     */
    public List<NoleggioValutazione> findStalled(int giorniSoglia) {
        try (Session session = getSession()) {
            Date soglia = new Date(System.currentTimeMillis() - (giorniSoglia * 24L * 60 * 60 * 1000));
            
            Query<NoleggioValutazione> query = session.createQuery(
                "FROM NoleggioValutazione WHERE status IN (:statuses) " +
                "AND dataAvvioIstruttoria <= :soglia " +
                "ORDER BY dataAvvioIstruttoria ASC", 
                NoleggioValutazione.class);
            query.setParameterList("statuses", 
                List.of(NoleggioValutazione.Status.DOCUMENTI_MANCANTI, 
                       NoleggioValutazione.Status.SOLLECITI_INVIATI));
            query.setParameter("soglia", soglia);
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding stalled valutazioni", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Find valuations in progress (documents complete, evaluating)
     */
    public List<NoleggioValutazione> findInProgress() {
        return findByStatus(NoleggioValutazione.Status.IN_VALUTAZIONE);
    }

    /**
     * Find approved valuations
     */
    public List<NoleggioValutazione> findApproved() {
        return findByStatus(NoleggioValutazione.Status.VALUTAZIONE_POSITIVA);
    }

    /**
     * Find valuations by risk assessment
     */
    public List<NoleggioValutazione> findByRiskLevel(NoleggioValutazione.EsitoRischioCredito rischio) {
        try (Session session = getSession()) {
            Query<NoleggioValutazione> query = session.createQuery(
                "FROM NoleggioValutazione WHERE esitoRischioCredito = :rischio " +
                "ORDER BY dataValutazione DESC", 
                NoleggioValutazione.class);
            query.setParameter("rischio", rischio);
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding valutazioni by risk", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Count valuations by status (for dashboard)
     */
    public long countByStatus(NoleggioValutazione.Status status) {
        try (Session session = getSession()) {
            Query<Long> query = session.createQuery(
                "SELECT COUNT(*) FROM NoleggioValutazione WHERE status = :status", Long.class);
            query.setParameter("status", status);
            return query.uniqueResult();
        } catch (Exception e) {
            logger.error("Error counting valutazioni", e);
            throw new RuntimeException(e);
        }
    }
}
