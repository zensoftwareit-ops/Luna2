package it.zensoftware.luna2.dao;

import it.zensoftware.luna2.model.NoleggioLead;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

/**
 * DAO for NoleggioLead entity
 * Manages CRUD operations and specialized queries for car rental leads
 */
public class NoleggioLeadDAO extends GenericDAOImpl<NoleggioLead, Long> {
    
    private static final Logger logger = LogManager.getLogger(NoleggioLeadDAO.class);

    public NoleggioLeadDAO() {
        super(NoleggioLead.class);
    }

    /**
     * Find all leads in a specific phase
     */
    public List<NoleggioLead> findByFase(NoleggioLead.Fase fase) {
        try (Session session = getSession()) {
            Query<NoleggioLead> query = session.createQuery(
                "FROM NoleggioLead WHERE fase = :fase ORDER BY createdAt DESC", 
                NoleggioLead.class);
            query.setParameter("fase", fase);
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding leads by fase", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Find leads by rental type (LUNGO_TERMINE / BREVE_TERMINE)
     */
    public List<NoleggioLead> findByTipoNoleggio(NoleggioLead.TipoNoleggio tipo) {
        try (Session session = getSession()) {
            Query<NoleggioLead> query = session.createQuery(
                "FROM NoleggioLead WHERE tipoNoleggio = :tipo ORDER BY createdAt DESC", 
                NoleggioLead.class);
            query.setParameter("tipo", tipo);
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding leads by tipo noleggio", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Find leads with referrer (segnalatore)
     */
    public List<NoleggioLead> findWithSegnalatore() {
        try (Session session = getSession()) {
            Query<NoleggioLead> query = session.createQuery(
                "FROM NoleggioLead WHERE origine = :origine ORDER BY createdAt DESC", 
                NoleggioLead.class);
            query.setParameter("origine", NoleggioLead.OrigineTipo.SEGNALATORE);
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding leads with segnalatore", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Find leads by assigned user
     */
    public List<NoleggioLead> findByUtenteAssegnato(Long utenteId) {
        try (Session session = getSession()) {
            Query<NoleggioLead> query = session.createQuery(
                "FROM NoleggioLead WHERE utenteAssegnatoId = :utenteId ORDER BY fase ASC, createdAt DESC", 
                NoleggioLead.class);
            query.setParameter("utenteId", utenteId);
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding leads by utente", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Find leads in preventivazione phase (for follow-up automation)
     */
    public List<NoleggioLead> findInPreventivazione() {
        return findByFase(NoleggioLead.Fase.PREVENTIVAZIONE);
    }

    /**
     * Count leads by phase (for dashboard stats)
     */
    public long countByFase(NoleggioLead.Fase fase) {
        try (Session session = getSession()) {
            Query<Long> query = session.createQuery(
                "SELECT COUNT(*) FROM NoleggioLead WHERE fase = :fase", Long.class);
            query.setParameter("fase", fase);
            return query.uniqueResult();
        } catch (Exception e) {
            logger.error("Error counting leads by fase", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Find leads by brand/model preference (for vehicle matching)
     */
    public List<NoleggioLead> findByVeicoloPreferito(String marca, String modello) {
        try (Session session = getSession()) {
            Query<NoleggioLead> query = session.createQuery(
                "FROM NoleggioLead WHERE marcaPreferita = :marca AND modelloPreferito = :modello ORDER BY createdAt DESC", 
                NoleggioLead.class);
            query.setParameter("marca", marca);
            query.setParameter("modello", modello);
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding leads by veicolo", e);
            throw new RuntimeException(e);
        }
    }
}
