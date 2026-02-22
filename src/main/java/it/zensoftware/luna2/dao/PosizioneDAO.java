package it.zensoftware.luna2.dao;

import it.zensoftware.luna2.model.Posizione;
import it.zensoftware.luna2.model.Posizione.TipoPosizione;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

/**
 * DAO per la gestione delle posizioni fisiche nel magazzino
 */
public class PosizioneDAO extends GenericDAOImpl<Posizione, Long> {
    
    private static final Logger logger = LogManager.getLogger(PosizioneDAO.class);

    public PosizioneDAO() {
        super(Posizione.class);
    }

    /**
     * Trova tutte le posizioni attive di un warehouse
     */
    public List<Posizione> findByWarehouse(Long warehouseId) {
        try (Session session = getSession()) {
            Query<Posizione> query = session.createQuery(
                "FROM Posizione WHERE warehouse.id = :warehouseId AND attiva = true " +
                "ORDER BY zona, corsia, scaffale, ripiano", 
                Posizione.class);
            query.setParameter("warehouseId", warehouseId);
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding posizioni by warehouse", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Trova una posizione per codice in un warehouse specifico
     */
    public Posizione findByWarehouseAndCodice(Long warehouseId, String codice) {
        try (Session session = getSession()) {
            Query<Posizione> query = session.createQuery(
                "FROM Posizione WHERE warehouse.id = :warehouseId AND codice = :codice", 
                Posizione.class);
            query.setParameter("warehouseId", warehouseId);
            query.setParameter("codice", codice);
            return query.uniqueResult();
        } catch (Exception e) {
            logger.error("Error finding posizione by code", e);
            return null;
        }
    }

    /**
     * Trova posizioni per zona
     */
    public List<Posizione> findByZona(Long warehouseId, String zona) {
        try (Session session = getSession()) {
            Query<Posizione> query = session.createQuery(
                "FROM Posizione WHERE warehouse.id = :warehouseId AND zona = :zona AND attiva = true " +
                "ORDER BY corsia, scaffale, ripiano", 
                Posizione.class);
            query.setParameter("warehouseId", warehouseId);
            query.setParameter("zona", zona);
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding posizioni by zona", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Trova posizioni per tipo
     */
    public List<Posizione> findByTipo(Long warehouseId, TipoPosizione tipoPosizione) {
        try (Session session = getSession()) {
            String hql = "FROM Posizione WHERE tipoPosizione = :tipoPosizione AND attiva = true ";
            if (warehouseId != null) {
                hql += "AND warehouse.id = :warehouseId ";
            }
            hql += "ORDER BY zona, scaffale";
            
            Query<Posizione> query = session.createQuery(hql, Posizione.class);
            query.setParameter("tipoPosizione", tipoPosizione);
            if (warehouseId != null) {
                query.setParameter("warehouseId", warehouseId);
            }
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding posizioni by tipo", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Cerca posizioni per codice o descrizione
     */
    public List<Posizione> searchByCodeOrDescription(Long warehouseId, String searchTerm) {
        try (Session session = getSession()) {
            String hql = "FROM Posizione WHERE attiva = true " +
                        "AND (LOWER(codice) LIKE LOWER(:searchTerm) " +
                        "OR LOWER(descrizione) LIKE LOWER(:searchTerm)) ";
            if (warehouseId != null) {
                hql += "AND warehouse.id = :warehouseId ";
            }
            hql += "ORDER BY codice ASC";
            
            Query<Posizione> query = session.createQuery(hql, Posizione.class);
            query.setParameter("searchTerm", "%" + searchTerm + "%");
            if (warehouseId != null) {
                query.setParameter("warehouseId", warehouseId);
            }
            return query.list();
        } catch (Exception e) {
            logger.error("Error searching posizioni", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Ottiene tutte le zone distinte di un warehouse
     */
    public List<String> getZoneDistinte(Long warehouseId) {
        try (Session session = getSession()) {
            Query<String> query = session.createQuery(
                "SELECT DISTINCT zona FROM Posizione " +
                "WHERE warehouse.id = :warehouseId AND zona IS NOT NULL AND attiva = true " +
                "ORDER BY zona", 
                String.class);
            query.setParameter("warehouseId", warehouseId);
            return query.list();
        } catch (Exception e) {
            logger.error("Error getting zone distinte", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Conta le posizioni attive in un warehouse
     */
    public long countByWarehouse(Long warehouseId) {
        try (Session session = getSession()) {
            Query<Long> query = session.createQuery(
                "SELECT COUNT(p) FROM Posizione p " +
                "WHERE p.warehouse.id = :warehouseId AND p.attiva = true", 
                Long.class);
            query.setParameter("warehouseId", warehouseId);
            return query.uniqueResult();
        } catch (Exception e) {
            logger.error("Error counting posizioni", e);
            return 0;
        }
    }

    /**
     * Trova posizioni disponibili (senza giacenza assegnata o con capacità residua)
     */
    public List<Posizione> findDisponibili(Long warehouseId) {
        try (Session session = getSession()) {
            Query<Posizione> query = session.createQuery(
                "FROM Posizione p WHERE p.warehouse.id = :warehouseId AND p.attiva = true " +
                "AND NOT EXISTS (SELECT 1 FROM Giacenza g WHERE g.posizione.id = p.id) " +
                "ORDER BY p.zona, p.scaffale", 
                Posizione.class);
            query.setParameter("warehouseId", warehouseId);
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding available posizioni", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Trova la prossima posizione libera per zona/tipo
     */
    public Posizione findProssimaLibera(Long warehouseId, String zona, TipoPosizione tipoPosizione) {
        try (Session session = getSession()) {
            String hql = "FROM Posizione p WHERE p.warehouse.id = :warehouseId " +
                        "AND p.attiva = true " +
                        "AND NOT EXISTS (SELECT 1 FROM Giacenza g WHERE g.posizione.id = p.id) ";
            
            if (zona != null) {
                hql += "AND p.zona = :zona ";
            }
            if (tipoPosizione != null) {
                hql += "AND p.tipoPosizione = :tipoPosizione ";
            }
            hql += "ORDER BY p.zona, p.scaffale, p.ripiano";
            
            Query<Posizione> query = session.createQuery(hql, Posizione.class);
            query.setParameter("warehouseId", warehouseId);
            if (zona != null) {
                query.setParameter("zona", zona);
            }
            if (tipoPosizione != null) {
                query.setParameter("tipoPosizione", tipoPosizione);
            }
            query.setMaxResults(1);
            
            List<Posizione> results = query.list();
            return results.isEmpty() ? null : results.get(0);
        } catch (Exception e) {
            logger.error("Error finding next available posizione", e);
            return null;
        }
    }
}
