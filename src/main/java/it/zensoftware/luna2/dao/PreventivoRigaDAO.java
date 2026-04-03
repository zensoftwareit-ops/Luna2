package it.zensoftware.luna2.dao;

import it.zensoftware.luna2.model.PreventivoRiga;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import java.util.List;

/**
 * DAO for PreventivoRiga entity
 */
public class PreventivoRigaDAO extends GenericDAOImpl<PreventivoRiga, Long> {
    
    private static final Logger logger = LogManager.getLogger(PreventivoRigaDAO.class);

    public PreventivoRigaDAO() {
        super(PreventivoRiga.class);
    }
    
    /**
     * Find all righe for a preventivo
     */
    public List<PreventivoRiga> findByPreventivoId(Long preventivoId) {
        try (Session session = getSession()) {
            Query<PreventivoRiga> query = session.createQuery(
                "FROM PreventivoRiga r WHERE r.preventivo.id = :preventivoId ORDER BY r.rigaNumero",
                PreventivoRiga.class
            );
            query.setParameter("preventivoId", preventivoId);
            return query.getResultList();
        } catch (Exception e) {
            logger.error("Error finding righe by preventivoId", e);
            throw new RuntimeException("Error finding righe", e);
        }
    }
    
    /**
     * Get next riga numero for a preventivo
     */
    public Integer getNextRigaNumero(Long preventivoId) {
        try (Session session = getSession()) {
            Query<Integer> query = session.createQuery(
                "SELECT COALESCE(MAX(r.rigaNumero), 0) + 1 FROM PreventivoRiga r WHERE r.preventivo.id = :preventivoId",
                Integer.class
            );
            query.setParameter("preventivoId", preventivoId);
            Integer result = query.uniqueResult();
            return result != null ? result : 1;
        } catch (Exception e) {
            logger.error("Error getting next riga numero", e);
            return 1;
        }
    }
    
    /**
     * Delete all righe for a preventivo
     */
    public void deleteByPreventivoId(Long preventivoId) {
        Transaction transaction = null;
        try (Session session = getSession()) {
            transaction = session.beginTransaction();
            Query<?> query = session.createQuery(
                "DELETE FROM PreventivoRiga r WHERE r.preventivo.id = :preventivoId"
            );
            query.setParameter("preventivoId", preventivoId);
            query.executeUpdate();
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            logger.error("Error deleting righe by preventivoId", e);
            throw new RuntimeException("Error deleting righe", e);
        }
    }
    
    /**
     * Find riga by preventivo and riga numero
     */
    public PreventivoRiga findByPreventivoAndRigaNumero(Long preventivoId, Integer rigaNumero) {
        try (Session session = getSession()) {
            Query<PreventivoRiga> query = session.createQuery(
                "FROM PreventivoRiga r WHERE r.preventivo.id = :preventivoId AND r.rigaNumero = :rigaNumero",
                PreventivoRiga.class
            );
            query.setParameter("preventivoId", preventivoId);
            query.setParameter("rigaNumero", rigaNumero);
            return query.uniqueResult();
        } catch (Exception e) {
            logger.error("Error finding riga by preventivo and numero", e);
            return null;
        }
    }
}
