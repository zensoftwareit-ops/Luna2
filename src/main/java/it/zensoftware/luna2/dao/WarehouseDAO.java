package it.zensoftware.luna2.dao;

import it.zensoftware.luna2.model.Warehouse;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

/**
 * DAO per la gestione dei magazzini (warehouses)
 */
public class WarehouseDAO extends GenericDAOImpl<Warehouse, Long> {
    
    private static final Logger logger = LogManager.getLogger(WarehouseDAO.class);

    public WarehouseDAO() {
        super(Warehouse.class);
    }

    /**
     * Trova tutti i warehouse attivi
     */
    public List<Warehouse> findAllActive() {
        try (Session session = getSession()) {
            Query<Warehouse> query = session.createQuery(
                "FROM Warehouse WHERE attivo = true ORDER BY principale DESC, nome ASC", 
                Warehouse.class);
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding active warehouses", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Trova il warehouse principale
     */
    public Warehouse findPrincipale() {
        try (Session session = getSession()) {
            Query<Warehouse> query = session.createQuery(
                "FROM Warehouse WHERE principale = true AND attivo = true", 
                Warehouse.class);
            query.setMaxResults(1);
            return query.uniqueResult();
        } catch (Exception e) {
            logger.error("Error finding main warehouse", e);
            return null;
        }
    }

    /**
     * Trova warehouse per nome
     */
    public Warehouse findByNome(String nome) {
        try (Session session = getSession()) {
            Query<Warehouse> query = session.createQuery(
                "FROM Warehouse WHERE nome = :nome", 
                Warehouse.class);
            query.setParameter("nome", nome);
            return query.uniqueResult();
        } catch (Exception e) {
            logger.error("Error finding warehouse by name", e);
            return null;
        }
    }

    /**
     * Trova warehouse per codice
     */
    public Warehouse findByCodice(String codice) {
        try (Session session = getSession()) {
            Query<Warehouse> query = session.createQuery(
                "FROM Warehouse WHERE codice = :codice", 
                Warehouse.class);
            query.setParameter("codice", codice);
            return query.uniqueResult();
        } catch (Exception e) {
            logger.error("Error finding warehouse by code", e);
            return null;
        }
    }

    /**
     * Cerca warehouse per nome o città
     */
    public List<Warehouse> searchByNameOrCity(String searchTerm) {
        try (Session session = getSession()) {
            Query<Warehouse> query = session.createQuery(
                "FROM Warehouse WHERE LOWER(nome) LIKE LOWER(:searchTerm) " +
                "OR LOWER(citta) LIKE LOWER(:searchTerm) " +
                "ORDER BY nome ASC", 
                Warehouse.class);
            query.setParameter("searchTerm", "%" + searchTerm + "%");
            return query.list();
        } catch (Exception e) {
            logger.error("Error searching warehouses", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Conta i warehouse attivi
     */
    public long countActive() {
        try (Session session = getSession()) {
            Query<Long> query = session.createQuery(
                "SELECT COUNT(w) FROM Warehouse w WHERE w.attivo = true", 
                Long.class);
            return query.uniqueResult();
        } catch (Exception e) {
            logger.error("Error counting active warehouses", e);
            return 0;
        }
    }

    /**
     * Imposta un warehouse come principale, rimuovendo il flag dagli altri
     */
    public void setPrincipale(Long warehouseId) {
        Session session = null;
        try {
            session = getSession();
            session.beginTransaction();
            
            // Rimuovi il flag principale da tutti
            Query<?> updateQuery = session.createQuery(
                "UPDATE Warehouse SET principale = false WHERE principale = true");
            updateQuery.executeUpdate();
            
            // Imposta il nuovo principale
            if (warehouseId != null) {
                Query<?> setPrincipalQuery = session.createQuery(
                    "UPDATE Warehouse SET principale = true WHERE id = :id");
                setPrincipalQuery.setParameter("id", warehouseId);
                setPrincipalQuery.executeUpdate();
            }
            
            session.getTransaction().commit();
        } catch (Exception e) {
            if (session != null && session.getTransaction().isActive()) {
                session.getTransaction().rollback();
            }
            logger.error("Error setting main warehouse", e);
            throw new RuntimeException(e);
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }
}
