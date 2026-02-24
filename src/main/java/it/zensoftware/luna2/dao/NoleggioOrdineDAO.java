package it.zensoftware.luna2.dao;

import it.zensoftware.luna2.model.NoleggioOrdine;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Date;
import java.util.List;

/**
 * DAO for NoleggioOrdine entity
 * Manages vehicle orders with care call automation
 */
public class NoleggioOrdineDAO extends GenericDAOImpl<NoleggioOrdine, Long> {
    
    private static final Logger logger = LogManager.getLogger(NoleggioOrdineDAO.class);

    public NoleggioOrdineDAO() {
        super(NoleggioOrdine.class);
    }

    /**
     * Find order by lead
     */
    public NoleggioOrdine findByLead(Long leadId) {
        try (Session session = getSession()) {
            Query<NoleggioOrdine> query = session.createQuery(
                "FROM NoleggioOrdine WHERE lead.id = :leadId", 
                NoleggioOrdine.class);
            query.setParameter("leadId", leadId);
            return query.uniqueResult();
        } catch (Exception e) {
            logger.error("Error finding ordine by lead", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Find order by numero ordine (unique identifier)
     */
    public NoleggioOrdine findByNumero(String numeroOrdine) {
        try (Session session = getSession()) {
            Query<NoleggioOrdine> query = session.createQuery(
                "FROM NoleggioOrdine WHERE numeroOrdine = :numero", 
                NoleggioOrdine.class);
            query.setParameter("numero", numeroOrdine);
            return query.uniqueResult();
        } catch (Exception e) {
            logger.error("Error finding ordine by numero", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Find orders by status
     */
    public List<NoleggioOrdine> findByStatus(NoleggioOrdine.Status status) {
        try (Session session = getSession()) {
            Query<NoleggioOrdine> query = session.createQuery(
                "FROM NoleggioOrdine WHERE status = :status ORDER BY dataOrdine ASC", 
                NoleggioOrdine.class);
            query.setParameter("status", status);
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding ordini by status", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Find orders requiring care call (every 20-30 days)
     * AUTOMATION: Called by NoleggioAutomationService
     */
    public List<NoleggioOrdine> findRequiringCareCall() {
        try (Session session = getSession()) {
            Query<NoleggioOrdine> query = session.createQuery(
                "FROM NoleggioOrdine WHERE status NOT IN (:excludedStatuses) " +
                "AND prossimoCareCallPrevisto <= :oggi " +
                "ORDER BY prossimoCareCallPrevisto ASC", 
                NoleggioOrdine.class);
            query.setParameterList("excludedStatuses", 
                List.of(NoleggioOrdine.Status.CONSEGNATO, NoleggioOrdine.Status.ANNULLATO));
            query.setParameter("oggi", new Date());
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding ordini requiring care call", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Find orders critically delayed (> 90 days without delivery)
     * AUTOMATION: Alert for manager
     */
    public List<NoleggioOrdine> findCriticallyDelayed(int giorniSoglia) {
        try (Session session = getSession()) {
            Date soglia = new Date(System.currentTimeMillis() - (giorniSoglia * 24L * 60 * 60 * 1000));
            
            Query<NoleggioOrdine> query = session.createQuery(
                "FROM NoleggioOrdine WHERE status NOT IN (:excludedStatuses) " +
                "AND dataOrdine <= :soglia " +
                "ORDER BY dataOrdine ASC", 
                NoleggioOrdine.class);
            query.setParameterList("excludedStatuses", 
                List.of(NoleggioOrdine.Status.CONSEGNATO, NoleggioOrdine.Status.ANNULLATO));
            query.setParameter("soglia", soglia);
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding critically delayed ordini", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Find orders requiring ETA notification (7 days before delivery)
     * AUTOMATION: Notify customer
     */
    public List<NoleggioOrdine> findRequiringEtaNotification() {
        try (Session session = getSession()) {
            Date sevenDaysFromNow = new Date(System.currentTimeMillis() + (7L * 24 * 60 * 60 * 1000));
            Date today = new Date();
            
            Query<NoleggioOrdine> query = session.createQuery(
                "FROM NoleggioOrdine WHERE status NOT IN (:excludedStatuses) " +
                "AND notificaEtaInviata = false " +
                "AND dataConsegnaStimata IS NOT NULL " +
                "AND dataConsegnaStimata BETWEEN :today AND :sevenDays " +
                "ORDER BY dataConsegnaStimata ASC", 
                NoleggioOrdine.class);
            query.setParameterList("excludedStatuses", 
                List.of(NoleggioOrdine.Status.CONSEGNATO, NoleggioOrdine.Status.ANNULLATO));
            query.setParameter("today", today);
            query.setParameter("sevenDays", sevenDaysFromNow);
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding ordini requiring ETA notification", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Find active orders (not delivered, not cancelled)
     */
    public List<NoleggioOrdine> findActive() {
        try (Session session = getSession()) {
            Query<NoleggioOrdine> query = session.createQuery(
                "FROM NoleggioOrdine WHERE status NOT IN (:excludedStatuses) " +
                "ORDER BY dataOrdine DESC", 
                NoleggioOrdine.class);
            query.setParameterList("excludedStatuses", 
                List.of(NoleggioOrdine.Status.CONSEGNATO, NoleggioOrdine.Status.ANNULLATO));
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding active ordini", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Find orders by supplier
     */
    public List<NoleggioOrdine> findByFornitore(String nomeFornitore) {
        try (Session session = getSession()) {
            Query<NoleggioOrdine> query = session.createQuery(
                "FROM NoleggioOrdine WHERE nomeFornitore = :fornitore ORDER BY dataOrdine DESC", 
                NoleggioOrdine.class);
            query.setParameter("fornitore", nomeFornitore);
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding ordini by fornitore", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Count orders by status (for dashboard)
     */
    public long countByStatus(NoleggioOrdine.Status status) {
        try (Session session = getSession()) {
            Query<Long> query = session.createQuery(
                "SELECT COUNT(*) FROM NoleggioOrdine WHERE status = :status", Long.class);
            query.setParameter("status", status);
            return query.uniqueResult();
        } catch (Exception e) {
            logger.error("Error counting ordini", e);
            throw new RuntimeException(e);
        }
    }
}
