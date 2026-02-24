package it.zensoftware.luna2.dao;

import it.zensoftware.luna2.model.NoleggioDocumento;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Date;
import java.util.List;

/**
 * DAO for NoleggioDocumento entity
 * Manages document checklist for istruttoria phase
 */
public class NoleggioDocumentoDAO extends GenericDAOImpl<NoleggioDocumento, Long> {
    
    private static final Logger logger = LogManager.getLogger(NoleggioDocumentoDAO.class);

    public NoleggioDocumentoDAO() {
        super(NoleggioDocumento.class);
    }

    /**
     * Find all documents for a specific lead
     */
    public List<NoleggioDocumento> findByLead(Long leadId) {
        try (Session session = getSession()) {
            Query<NoleggioDocumento> query = session.createQuery(
                "FROM NoleggioDocumento WHERE lead.id = :leadId ORDER BY tipoDocumento ASC", 
                NoleggioDocumento.class);
            query.setParameter("leadId", leadId);
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding documenti by lead", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Find documents by status
     */
    public List<NoleggioDocumento> findByStatus(NoleggioDocumento.Status status) {
        try (Session session = getSession()) {
            Query<NoleggioDocumento> query = session.createQuery(
                "FROM NoleggioDocumento WHERE status = :status ORDER BY dataRichiesta ASC", 
                NoleggioDocumento.class);
            query.setParameter("status", status);
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding documenti by status", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Find missing documents for a lead (DA_RICHIEDERE, RICHIESTO)
     */
    public List<NoleggioDocumento> findMissingByLead(Long leadId) {
        try (Session session = getSession()) {
            Query<NoleggioDocumento> query = session.createQuery(
                "FROM NoleggioDocumento WHERE lead.id = :leadId AND status IN (:statuses) " +
                "ORDER BY dataRichiesta ASC", 
                NoleggioDocumento.class);
            query.setParameter("leadId", leadId);
            query.setParameterList("statuses", 
                List.of(NoleggioDocumento.Status.DA_RICHIEDERE, NoleggioDocumento.Status.RICHIESTO));
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding missing documenti", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Find documents requiring reminder (requested > N days ago, not received)
     * AUTOMATION: Called by NoleggioAutomationService
     */
    public List<NoleggioDocumento> findRequiringReminder(int giorniSoglia) {
        try (Session session = getSession()) {
            Date soglia = new Date(System.currentTimeMillis() - (giorniSoglia * 24L * 60 * 60 * 1000));
            
            Query<NoleggioDocumento> query = session.createQuery(
                "FROM NoleggioDocumento WHERE status = :status AND dataRichiesta <= :soglia " +
                "ORDER BY dataRichiesta ASC", 
                NoleggioDocumento.class);
            query.setParameter("status", NoleggioDocumento.Status.RICHIESTO);
            query.setParameter("soglia", soglia);
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding documenti requiring reminder", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Find documents expiring soon (for licenses, IDs with expiry date)
     */
    public List<NoleggioDocumento> findExpiringWithin(int giorni) {
        try (Session session = getSession()) {
            Date dataRiferimento = new Date(System.currentTimeMillis() + (giorni * 24L * 60 * 60 * 1000));
            
            Query<NoleggioDocumento> query = session.createQuery(
                "FROM NoleggioDocumento WHERE dataScadenzaDocumento IS NOT NULL " +
                "AND dataScadenzaDocumento <= :dataRiferimento AND status = :status " +
                "ORDER BY dataScadenzaDocumento ASC", 
                NoleggioDocumento.class);
            query.setParameter("dataRiferimento", dataRiferimento);
            query.setParameter("status", NoleggioDocumento.Status.VALIDATO);
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding expiring documenti", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Count missing documents for a lead
     */
    public long countMissingByLead(Long leadId) {
        try (Session session = getSession()) {
            Query<Long> query = session.createQuery(
                "SELECT COUNT(*) FROM NoleggioDocumento WHERE lead.id = :leadId " +
                "AND status IN (:statuses)", Long.class);
            query.setParameter("leadId", leadId);
            query.setParameterList("statuses", 
                List.of(NoleggioDocumento.Status.DA_RICHIEDERE, NoleggioDocumento.Status.RICHIESTO));
            return query.uniqueResult();
        } catch (Exception e) {
            logger.error("Error counting missing documenti", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Check if all documents validated for a lead
     */
    public boolean areAllDocumentsValidated(Long leadId) {
        try (Session session = getSession()) {
            Query<Long> query = session.createQuery(
                "SELECT COUNT(*) FROM NoleggioDocumento WHERE lead.id = :leadId " +
                "AND status != :status", Long.class);
            query.setParameter("leadId", leadId);
            query.setParameter("status", NoleggioDocumento.Status.VALIDATO);
            return query.uniqueResult() == 0;
        } catch (Exception e) {
            logger.error("Error checking documents validation", e);
            throw new RuntimeException(e);
        }
    }
}
