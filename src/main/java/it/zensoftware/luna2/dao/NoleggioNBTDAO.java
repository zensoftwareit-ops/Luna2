package it.zensoftware.luna2.dao;

import it.zensoftware.luna2.model.NoleggioNBT;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Date;
import java.util.List;

/**
 * DAO for NoleggioNBT entity
 * Manages short-term rentals (1-30 days)
 */
public class NoleggioNBTDAO extends GenericDAOImpl<NoleggioNBT, Long> {
    
    private static final Logger logger = LogManager.getLogger(NoleggioNBTDAO.class);

    public NoleggioNBTDAO() {
        super(NoleggioNBT.class);
    }

    /**
     * Find NBT by numero pratica (unique identifier)
     */
    public NoleggioNBT findByNumero(String numeroPratica) {
        try (Session session = getSession()) {
            Query<NoleggioNBT> query = session.createQuery(
                "FROM NoleggioNBT WHERE numeroPratica = :numero", 
                NoleggioNBT.class);
            query.setParameter("numero", numeroPratica);
            return query.uniqueResult();
        } catch (Exception e) {
            logger.error("Error finding NBT by numero", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Find NBT by status
     */
    public List<NoleggioNBT> findByStatus(NoleggioNBT.Status status) {
        try (Session session = getSession()) {
            Query<NoleggioNBT> query = session.createQuery(
                "FROM NoleggioNBT WHERE status = :status ORDER BY dataInizioNoleggio ASC", 
                NoleggioNBT.class);
            query.setParameter("status", status);
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding NBT by status", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Find quotes requiring 24h follow-up (sent without confirmation)
     * AUTOMATION: Called by NoleggioAutomationService
     */
    public List<NoleggioNBT> findRequiringFollowup24h() {
        try (Session session = getSession()) {
            Date twentyFourHoursAgo = new Date(System.currentTimeMillis() - (24L * 60 * 60 * 1000));
            
            Query<NoleggioNBT> query = session.createQuery(
                "FROM NoleggioNBT WHERE status = :status " +
                "AND alertFollowup24hInviato = false " +
                "AND dataInvioPreventivo IS NOT NULL " +
                "AND dataInvioPreventivo <= :timeThreshold " +
                "ORDER BY dataInvioPreventivo ASC", 
                NoleggioNBT.class);
            query.setParameter("status", NoleggioNBT.Status.PREVENTIVO_INVIATO);
            query.setParameter("timeThreshold", twentyFourHoursAgo);
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding NBT requiring followup", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Find rentals requiring return alert (1 day before end)
     * AUTOMATION: Notify customer
     */
    public List<NoleggioNBT> findRequiringReturnAlert() {
        try (Session session = getSession()) {
            Date twentyFourHoursFromNow = new Date(System.currentTimeMillis() + (24L * 60 * 60 * 1000));
            Date now = new Date();
            
            Query<NoleggioNBT> query = session.createQuery(
                "FROM NoleggioNBT WHERE status = :status " +
                "AND alertRestituzioneInviato = false " +
                "AND dataFineNoleggio BETWEEN :now AND :tomorrow " +
                "ORDER BY dataFineNoleggio ASC", 
                NoleggioNBT.class);
            query.setParameter("status", NoleggioNBT.Status.IN_CORSO);
            query.setParameter("now", now);
            query.setParameter("tomorrow", twentyFourHoursFromNow);
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding NBT requiring return alert", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Find expired quotes (sent > 48h ago, not confirmed)
     */
    public List<NoleggioNBT> findExpiredQuotes() {
        try (Session session = getSession()) {
            Date now = new Date();
            
            Query<NoleggioNBT> query = session.createQuery(
                "FROM NoleggioNBT WHERE status = :status " +
                "AND preventivoValidoFino IS NOT NULL " +
                "AND preventivoValidoFino < :now " +
                "ORDER BY preventivoValidoFino ASC", 
                NoleggioNBT.class);
            query.setParameter("status", NoleggioNBT.Status.PREVENTIVO_INVIATO);
            query.setParameter("now", now);
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding expired NBT quotes", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Find active rentals
     */
    public List<NoleggioNBT> findActive() {
        return findByStatus(NoleggioNBT.Status.IN_CORSO);
    }

    /**
     * Find rentals ready for pickup/delivery
     */
    public List<NoleggioNBT> findReadyForDelivery() {
        return findByStatus(NoleggioNBT.Status.PRONTA_CONSEGNA);
    }

    /**
     * Find rentals by date range
     */
    public List<NoleggioNBT> findByDateRange(Date startDate, Date endDate) {
        try (Session session = getSession()) {
            Query<NoleggioNBT> query = session.createQuery(
                "FROM NoleggioNBT WHERE dataInizioNoleggio >= :startDate " +
                "AND dataInizioNoleggio <= :endDate " +
                "ORDER BY dataInizioNoleggio ASC", 
                NoleggioNBT.class);
            query.setParameter("startDate", startDate);
            query.setParameter("endDate", endDate);
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding NBT by date range", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Find rentals by vehicle category (for availability checks)
     */
    public List<NoleggioNBT> findByCategory(NoleggioNBT.CategoriaVeicolo categoria) {
        try (Session session = getSession()) {
            Query<NoleggioNBT> query = session.createQuery(
                "FROM NoleggioNBT WHERE categoriaRichiesta = :categoria " +
                "AND status IN (:activeStatuses) " +
                "ORDER BY dataInizioNoleggio ASC", 
                NoleggioNBT.class);
            query.setParameter("categoria", categoria);
            query.setParameterList("activeStatuses", 
                List.of(NoleggioNBT.Status.PRENOTAZIONE_CONFERMATA, 
                       NoleggioNBT.Status.VEICOLO_ASSEGNATO,
                       NoleggioNBT.Status.IN_CORSO));
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding NBT by category", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Find rentals by customer
     */
    public List<NoleggioNBT> findByCliente(Long clienteId) {
        try (Session session = getSession()) {
            Query<NoleggioNBT> query = session.createQuery(
                "FROM NoleggioNBT WHERE clienteId = :clienteId ORDER BY dataRichiesta DESC", 
                NoleggioNBT.class);
            query.setParameter("clienteId", clienteId);
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding NBT by cliente", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Count NBT by status (for dashboard)
     */
    public long countByStatus(NoleggioNBT.Status status) {
        try (Session session = getSession()) {
            Query<Long> query = session.createQuery(
                "SELECT COUNT(*) FROM NoleggioNBT WHERE status = :status", Long.class);
            query.setParameter("status", status);
            return query.uniqueResult();
        } catch (Exception e) {
            logger.error("Error counting NBT", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Calculate total revenue (for analytics)
     */
    public Double calculateTotalRevenue() {
        try (Session session = getSession()) {
            Query<Double> query = session.createQuery(
                "SELECT SUM(importoPagato) FROM NoleggioNBT WHERE status = :status", 
                Double.class);
            query.setParameter("status", NoleggioNBT.Status.COMPLETATO);
            Double result = query.uniqueResult();
            return result != null ? result : 0.0;
        } catch (Exception e) {
            logger.error("Error calculating NBT revenue", e);
            throw new RuntimeException(e);
        }
    }
}
