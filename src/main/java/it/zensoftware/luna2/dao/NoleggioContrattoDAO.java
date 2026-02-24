package it.zensoftware.luna2.dao;

import it.zensoftware.luna2.model.NoleggioContratto;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Date;
import java.util.List;

/**
 * DAO for NoleggioContratto entity
 * Manages active contracts with calendar integration for renewal/maintenance scheduling
 */
public class NoleggioContrattoDAO extends GenericDAOImpl<NoleggioContratto, Long> {
    
    private static final Logger logger = LogManager.getLogger(NoleggioContrattoDAO.class);

    public NoleggioContrattoDAO() {
        super(NoleggioContratto.class);
    }

    /**
     * Find contract by numero contratto (unique identifier)
     */
    public NoleggioContratto findByNumero(String numeroContratto) {
        try (Session session = getSession()) {
            Query<NoleggioContratto> query = session.createQuery(
                "FROM NoleggioContratto WHERE numeroContratto = :numero", 
                NoleggioContratto.class);
            query.setParameter("numero", numeroContratto);
            return query.uniqueResult();
        } catch (Exception e) {
            logger.error("Error finding contratto by numero", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Find contract by targa (vehicle plate)
     */
    public NoleggioContratto findByTarga(String targa) {
        try (Session session = getSession()) {
            Query<NoleggioContratto> query = session.createQuery(
                "FROM NoleggioContratto WHERE targa = :targa AND status != :scaduto", 
                NoleggioContratto.class);
            query.setParameter("targa", targa);
            query.setParameter("scaduto", NoleggioContratto.Status.SCADUTO);
            return query.uniqueResult();
        } catch (Exception e) {
            logger.error("Error finding contratto by targa", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Find contracts by status
     */
    public List<NoleggioContratto> findByStatus(NoleggioContratto.Status status) {
        try (Session session = getSession()) {
            Query<NoleggioContratto> query = session.createQuery(
                "FROM NoleggioContratto WHERE status = :status ORDER BY dataFine ASC", 
                NoleggioContratto.class);
            query.setParameter("status", status);
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding contratti by status", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Find active contracts
     */
    public List<NoleggioContratto> findActive() {
        return findByStatus(NoleggioContratto.Status.ATTIVO);
    }

    /**
     * Find contracts expiring soon (for renewal automation)
     * AUTOMATION: Called by NoleggioScadenzarioService
     */
    public List<NoleggioContratto> findExpiringWithin(int giorniAnticipo) {
        try (Session session = getSession()) {
            Date dataRiferimento = new Date(System.currentTimeMillis() + (giorniAnticipo * 24L * 60 * 60 * 1000));
            Date oggi = new Date();
            
            Query<NoleggioContratto> query = session.createQuery(
                "FROM NoleggioContratto WHERE status = :status " +
                "AND allarmeRinnovoAttivato = false " +
                "AND dataFine BETWEEN :oggi AND :dataRiferimento " +
                "ORDER BY dataFine ASC", 
                NoleggioContratto.class);
            query.setParameter("status", NoleggioContratto.Status.ATTIVO);
            query.setParameter("oggi", oggi);
            query.setParameter("dataRiferimento", dataRiferimento);
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding expiring contratti", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Find contracts requiring km verification
     * AUTOMATION: Create calendar event every 6 months
     */
    public List<NoleggioContratto> findRequiringKmVerification() {
        try (Session session = getSession()) {
            Date oggi = new Date();
            
            Query<NoleggioContratto> query = session.createQuery(
                "FROM NoleggioContratto WHERE status = :status " +
                "AND prossimaVerificaKm IS NOT NULL " +
                "AND prossimaVerificaKm <= :oggi " +
                "ORDER BY prossimaVerificaKm ASC", 
                NoleggioContratto.class);
            query.setParameter("status", NoleggioContratto.Status.ATTIVO);
            query.setParameter("oggi", oggi);
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding contratti requiring km verification", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Find contracts with upcoming vehicle inspection (revisione)
     * AUTOMATION: Create calendar event
     */
    public List<NoleggioContratto> findWithUpcomingRevisione(int giorniAnticipo) {
        try (Session session = getSession()) {
            Date dataRiferimento = new Date(System.currentTimeMillis() + (giorniAnticipo * 24L * 60 * 60 * 1000));
            Date oggi = new Date();
            
            Query<NoleggioContratto> query = session.createQuery(
                "FROM NoleggioContratto WHERE status = :status " +
                "AND dataProssimaRevisione IS NOT NULL " +
                "AND dataProssimaRevisione BETWEEN :oggi AND :dataRiferimento " +
                "ORDER BY dataProssimaRevisione ASC", 
                NoleggioContratto.class);
            query.setParameter("status", NoleggioContratto.Status.ATTIVO);
            query.setParameter("oggi", oggi);
            query.setParameter("dataRiferimento", dataRiferimento);
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding contratti with upcoming revisione", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Find contracts with upcoming maintenance (tagliando)
     * AUTOMATION: Create calendar event
     */
    public List<NoleggioContratto> findWithUpcomingTagliando(int giorniAnticipo) {
        try (Session session = getSession()) {
            Date dataRiferimento = new Date(System.currentTimeMillis() + (giorniAnticipo * 24L * 60 * 60 * 1000));
            Date oggi = new Date();
            
            Query<NoleggioContratto> query = session.createQuery(
                "FROM NoleggioContratto WHERE status = :status " +
                "AND dataProssimoTagliando IS NOT NULL " +
                "AND dataProssimoTagliando BETWEEN :oggi AND :dataRiferimento " +
                "ORDER BY dataProssimoTagliando ASC", 
                NoleggioContratto.class);
            query.setParameter("status", NoleggioContratto.Status.ATTIVO);
            query.setParameter("oggi", oggi);
            query.setParameter("dataRiferimento", dataRiferimento);
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding contratti with upcoming tagliando", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Find contracts with driver license expiring soon
     * AUTOMATION: Create calendar event
     */
    public List<NoleggioContratto> findWithExpiringLicense(int giorniAnticipo) {
        try (Session session = getSession()) {
            Date dataRiferimento = new Date(System.currentTimeMillis() + (giorniAnticipo * 24L * 60 * 60 * 1000));
            Date oggi = new Date();
            
            Query<NoleggioContratto> query = session.createQuery(
                "FROM NoleggioContratto WHERE status = :status " +
                "AND dataScadenzaPatenteConducente IS NOT NULL " +
                "AND dataScadenzaPatenteConducente BETWEEN :oggi AND :dataRiferimento " +
                "ORDER BY dataScadenzaPatenteConducente ASC", 
                NoleggioContratto.class);
            query.setParameter("status", NoleggioContratto.Status.ATTIVO);
            query.setParameter("oggi", oggi);
            query.setParameter("dataRiferimento", dataRiferimento);
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding contratti with expiring license", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Find contracts with insurance expiring soon
     * AUTOMATION: Create calendar event
     */
    public List<NoleggioContratto> findWithExpiringInsurance(int giorniAnticipo) {
        try (Session session = getSession()) {
            Date dataRiferimento = new Date(System.currentTimeMillis() + (giorniAnticipo * 24L * 60 * 60 * 1000));
            Date oggi = new Date();
            
            Query<NoleggioContratto> query = session.createQuery(
                "FROM NoleggioContratto WHERE status = :status " +
                "AND dataScadenzaAssicurazione IS NOT NULL " +
                "AND dataScadenzaAssicurazione BETWEEN :oggi AND :dataRiferimento " +
                "ORDER BY dataScadenzaAssicurazione ASC", 
                NoleggioContratto.class);
            query.setParameter("status", NoleggioContratto.Status.ATTIVO);
            query.setParameter("oggi", oggi);
            query.setParameter("dataRiferimento", dataRiferimento);
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding contratti with expiring insurance", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Find contracts by assigned user
     */
    public List<NoleggioContratto> findByUtenteAssegnato(Long utenteId) {
        try (Session session = getSession()) {
            Query<NoleggioContratto> query = session.createQuery(
                "FROM NoleggioContratto WHERE utenteAssegnatoId = :utenteId " +
                "AND status IN (:activeStatuses) ORDER BY dataFine ASC", 
                NoleggioContratto.class);
            query.setParameter("utenteId", utenteId);
            query.setParameterList("activeStatuses", 
                List.of(NoleggioContratto.Status.ATTIVO, NoleggioContratto.Status.IN_SCADENZA));
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding contratti by utente", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Find contracts exceeding km limit
     */
    public List<NoleggioContratto> findWithExceededKm() {
        try (Session session = getSession()) {
            Query<NoleggioContratto> query = session.createQuery(
                "FROM NoleggioContratto WHERE status = :status " +
                "AND kmAttuali IS NOT NULL AND kmTotaliPrevisti IS NOT NULL AND kmIniziali IS NOT NULL " +
                "AND (kmAttuali - kmIniziali) > kmTotaliPrevisti " +
                "ORDER BY dataFine ASC", 
                NoleggioContratto.class);
            query.setParameter("status", NoleggioContratto.Status.ATTIVO);
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding contratti with exceeded km", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Count contracts by status (for dashboard)
     */
    public long countByStatus(NoleggioContratto.Status status) {
        try (Session session = getSession()) {
            Query<Long> query = session.createQuery(
                "SELECT COUNT(*) FROM NoleggioContratto WHERE status = :status", Long.class);
            query.setParameter("status", status);
            return query.uniqueResult();
        } catch (Exception e) {
            logger.error("Error counting contratti", e);
            throw new RuntimeException(e);
        }
    }
}
