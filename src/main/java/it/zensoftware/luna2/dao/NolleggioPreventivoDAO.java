package it.zensoftware.luna2.dao;

import it.zensoftware.luna2.model.NoleggioPreventivo;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Date;
import java.util.List;

/**
 * DAO for NoleggioPreventivo entity
 * Manages quotes with versioning support
 */
public class NolleggioPreventivoDAO extends GenericDAOImpl<NoleggioPreventivo, Long> {
    
    private static final Logger logger = LogManager.getLogger(NolleggioPreventivoDAO.class);

    public NolleggioPreventivoDAO() {
        super(NoleggioPreventivo.class);
    }

    /**
     * Find all quotes for a specific lead
     */
    public List<NoleggioPreventivo> findByLead(Long leadId) {
        try (Session session = getSession()) {
            Query<NoleggioPreventivo> query = session.createQuery(
                "FROM NoleggioPreventivo WHERE lead.id = :leadId ORDER BY numeroVersione DESC, createdAt DESC", 
                NoleggioPreventivo.class);
            query.setParameter("leadId", leadId);
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding preventivi by lead", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Find quotes by status
     */
    public List<NoleggioPreventivo> findByStatus(NoleggioPreventivo.Status status) {
        try (Session session = getSession()) {
            Query<NoleggioPreventivo> query = session.createQuery(
                "FROM NoleggioPreventivo WHERE status = :status ORDER BY dataCreazione DESC", 
                NoleggioPreventivo.class);
            query.setParameter("status", status);
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding preventivi by status", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Find quotes requiring 48h follow-up (sent > 2 days ago, not responded)
     * AUTOMATION: Called by NoleggioAutomationService
     */
    public List<NoleggioPreventivo> findRequiringFollowup() {
        try (Session session = getSession()) {
            Date twoDaysAgo = new Date(System.currentTimeMillis() - (48L * 60 * 60 * 1000));
            
            Query<NoleggioPreventivo> query = session.createQuery(
                "FROM NoleggioPreventivo WHERE status = :status AND dataInvio <= :twoDaysAgo " +
                "AND ultimoFollowup IS NULL ORDER BY dataInvio ASC", 
                NoleggioPreventivo.class);
            query.setParameter("status", NoleggioPreventivo.Status.INVIATO);
            query.setParameter("twoDaysAgo", twoDaysAgo);
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding preventivi requiring followup", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Find latest version of quote for a lead
     */
    public NoleggioPreventivo findLatestVersionByLead(Long leadId) {
        try (Session session = getSession()) {
            Query<NoleggioPreventivo> query = session.createQuery(
                "FROM NoleggioPreventivo WHERE lead.id = :leadId ORDER BY numeroVersione DESC", 
                NoleggioPreventivo.class);
            query.setParameter("leadId", leadId);
            query.setMaxResults(1);
            return query.uniqueResult();
        } catch (Exception e) {
            logger.error("Error finding latest preventivo version", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Find quote by numero preventivo (unique identifier)
     */
    public NoleggioPreventivo findByNumero(String numeroPreventivo) {
        try (Session session = getSession()) {
            Query<NoleggioPreventivo> query = session.createQuery(
                "FROM NoleggioPreventivo WHERE numeroPreventivo = :numero", 
                NoleggioPreventivo.class);
            query.setParameter("numero", numeroPreventivo);
            return query.uniqueResult();
        } catch (Exception e) {
            logger.error("Error finding preventivo by numero", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Find accepted quotes (for conversion to orders)
     */
    public List<NoleggioPreventivo> findAccepted() {
        return findByStatus(NoleggioPreventivo.Status.ACCETTATO);
    }

    /**
     * Count quotes by status (for dashboard)
     */
    public long countByStatus(NoleggioPreventivo.Status status) {
        try (Session session = getSession()) {
            Query<Long> query = session.createQuery(
                "SELECT COUNT(*) FROM NoleggioPreventivo WHERE status = :status", Long.class);
            query.setParameter("status", status);
            return query.uniqueResult();
        } catch (Exception e) {
            logger.error("Error counting preventivi by status", e);
            throw new RuntimeException(e);
        }
    }
}
