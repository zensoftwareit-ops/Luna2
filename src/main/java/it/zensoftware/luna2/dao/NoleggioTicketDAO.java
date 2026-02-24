package it.zensoftware.luna2.dao;

import it.zensoftware.luna2.model.NoleggioTicket;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Date;
import java.util.List;

/**
 * DAO for NoleggioTicket entity
 * Manages after-sales ticketing with anti-bounce detection
 */
public class NoleggioTicketDAO extends GenericDAOImpl<NoleggioTicket, Long> {
    
    private static final Logger logger = LogManager.getLogger(NoleggioTicketDAO.class);

    public NoleggioTicketDAO() {
        super(NoleggioTicket.class);
    }

    /**
     * Find ticket by numero ticket (unique identifier)
     */
    public NoleggioTicket findByNumero(String numeroTicket) {
        try (Session session = getSession()) {
            Query<NoleggioTicket> query = session.createQuery(
                "FROM NoleggioTicket WHERE numeroTicket = :numero", 
                NoleggioTicket.class);
            query.setParameter("numero", numeroTicket);
            return query.uniqueResult();
        } catch (Exception e) {
            logger.error("Error finding ticket by numero", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Find all tickets for a contract
     */
    public List<NoleggioTicket> findByContratto(Long contrattoId) {
        try (Session session = getSession()) {
            Query<NoleggioTicket> query = session.createQuery(
                "FROM NoleggioTicket WHERE contratto.id = :contrattoId ORDER BY dataApertura DESC", 
                NoleggioTicket.class);
            query.setParameter("contrattoId", contrattoId);
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding tickets by contratto", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Find tickets by status
     */
    public List<NoleggioTicket> findByStatus(NoleggioTicket.Status status) {
        try (Session session = getSession()) {
            Query<NoleggioTicket> query = session.createQuery(
                "FROM NoleggioTicket WHERE status = :status ORDER BY priorita ASC, dataApertura ASC", 
                NoleggioTicket.class);
            query.setParameter("status", status);
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding tickets by status", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Find tickets by priority
     */
    public List<NoleggioTicket> findByPriority(NoleggioTicket.Priorita priorita) {
        try (Session session = getSession()) {
            Query<NoleggioTicket> query = session.createQuery(
                "FROM NoleggioTicket WHERE priorita = :priorita AND status NOT IN (:closedStatuses) " +
                "ORDER BY dataApertura ASC", 
                NoleggioTicket.class);
            query.setParameter("priorita", priorita);
            query.setParameterList("closedStatuses", 
                List.of(NoleggioTicket.Status.CHIUSO));
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding tickets by priority", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Find tickets with anti-bounce flag (reopened within 48-72h)
     * AUTOMATION: Escalate to manager
     */
    public List<NoleggioTicket> findWithAntiBounce() {
        try (Session session = getSession()) {
            Query<NoleggioTicket> query = session.createQuery(
                "FROM NoleggioTicket WHERE flagAntiRimbalzo = true " +
                "AND status = :status ORDER BY dataRiapertura DESC", 
                NoleggioTicket.class);
            query.setParameter("status", NoleggioTicket.Status.ESCALATO);
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding anti-bounce tickets", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Find tickets with exceeded response SLA
     * AUTOMATION: Alert operator
     */
    public List<NoleggioTicket> findWithSlaRispostaExceeded() {
        try (Session session = getSession()) {
            Query<NoleggioTicket> query = session.createQuery(
                "FROM NoleggioTicket WHERE slaRispostaScaduto = false " +
                "AND status NOT IN (:closedStatuses) " +
                "AND dataApertura <= :checkTime " +
                "ORDER BY dataApertura ASC", 
                NoleggioTicket.class);
            query.setParameterList("closedStatuses", 
                List.of(NoleggioTicket.Status.CHIUSO));
            
            // Check tickets older than their SLA - we'll verify in service layer
            Date checkTime = new Date(System.currentTimeMillis() - (2L * 60 * 60 * 1000)); // 2h ago
            query.setParameter("checkTime", checkTime);
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding SLA rispostaexceeded tickets", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Find tickets with exceeded resolution SLA
     * AUTOMATION: Alert manager
     */
    public List<NoleggioTicket> findWithSlaRisoluzioneExceeded() {
        try (Session session = getSession()) {
            Query<NoleggioTicket> query = session.createQuery(
                "FROM NoleggioTicket WHERE slaRisoluzioneScaduto = false " +
                "AND status NOT IN (:closedStatuses) " +
                "AND dataApertura <= :checkTime " +
                "ORDER BY dataApertura ASC", 
                NoleggioTicket.class);
            query.setParameterList("closedStatuses", 
                List.of(NoleggioTicket.Status.CHIUSO));
            
            // Check tickets older than 24h
            Date checkTime = new Date(System.currentTimeMillis() - (24L * 60 * 60 * 1000));
            query.setParameter("checkTime", checkTime);
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding SLA risoluzione exceeded tickets", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Find open tickets assigned to operator
     */
    public List<NoleggioTicket> findByOperatore(Long operatoreId) {
        try (Session session = getSession()) {
            Query<NoleggioTicket> query = session.createQuery(
                "FROM NoleggioTicket WHERE operatoreAssegnatoId = :operatoreId " +
                "AND status NOT IN (:closedStatuses) " +
                "ORDER BY priorita ASC, dataApertura ASC", 
                NoleggioTicket.class);
            query.setParameter("operatoreId", operatoreId);
            query.setParameterList("closedStatuses", 
                List.of(NoleggioTicket.Status.CHIUSO));
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding tickets by operatore", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Find escalated tickets
     */
    public List<NoleggioTicket> findEscalated() {
        return findByStatus(NoleggioTicket.Status.ESCALATO);
    }

    /**
     * Count tickets by status (for dashboard)
     */
    public long countByStatus(NoleggioTicket.Status status) {
        try (Session session = getSession()) {
            Query<Long> query = session.createQuery(
                "SELECT COUNT(*) FROM NoleggioTicket WHERE status = :status", Long.class);
            query.setParameter("status", status);
            return query.uniqueResult();
        } catch (Exception e) {
            logger.error("Error counting tickets", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Count tickets by category (for analytics)
     */
    public long countByCategoria(NoleggioTicket.Categoria categoria) {
        try (Session session = getSession()) {
            Query<Long> query = session.createQuery(
                "SELECT COUNT(*) FROM NoleggioTicket WHERE categoria = :categoria", Long.class);
            query.setParameter("categoria", categoria);
            return query.uniqueResult();
        } catch (Exception e) {
            logger.error("Error counting tickets by categoria", e);
            throw new RuntimeException(e);
        }
    }
}
