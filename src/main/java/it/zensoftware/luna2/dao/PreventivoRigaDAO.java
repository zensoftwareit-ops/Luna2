package it.zensoftware.luna2.dao;

import it.zensoftware.luna2.model.PreventivoRiga;
import org.hibernate.Session;
import org.hibernate.query.Query;

import java.util.List;

/**
 * DAO for PreventivoRiga entity
 */
public class PreventivoRigaDAO extends GenericDAOImpl<PreventivoRiga, Long> {
    
    public PreventivoRigaDAO() {
        super(PreventivoRiga.class);
    }
    
    /**
     * Find all righe for a preventivo
     */
    public List<PreventivoRiga> findByPreventivoId(Long preventivoId) {
        Session session = getSession();
        Query<PreventivoRiga> query = session.createQuery(
            "FROM PreventivoRiga r WHERE r.preventivo.id = :preventivoId ORDER BY r.rigaNumero", 
            PreventivoRiga.class
        );
        query.setParameter("preventivoId", preventivoId);
        return query.getResultList();
    }
    
    /**
     * Get next riga numero for a preventivo
     */
    public Integer getNextRigaNumero(Long preventivoId) {
        Session session = getSession();
        Query<Integer> query = session.createQuery(
            "SELECT COALESCE(MAX(r.rigaNumero), 0) + 1 FROM PreventivoRiga r WHERE r.preventivo.id = :preventivoId", 
            Integer.class
        );
        query.setParameter("preventivoId", preventivoId);
        Integer result = query.uniqueResult();
        return result != null ? result : 1;
    }
    
    /**
     * Delete all righe for a preventivo
     */
    public void deleteByPreventivoId(Long preventivoId) {
        Session session = getSession();
        Query<?> query = session.createQuery(
            "DELETE FROM PreventivoRiga r WHERE r.preventivo.id = :preventivoId"
        );
        query.setParameter("preventivoId", preventivoId);
        query.executeUpdate();
    }
    
    /**
     * Find riga by preventivo and riga numero
     */
    public PreventivoRiga findByPreventivoAndRigaNumero(Long preventivoId, Integer rigaNumero) {
        Session session = getSession();
        Query<PreventivoRiga> query = session.createQuery(
            "FROM PreventivoRiga r WHERE r.preventivo.id = :preventivoId AND r.rigaNumero = :rigaNumero", 
            PreventivoRiga.class
        );
        query.setParameter("preventivoId", preventivoId);
        query.setParameter("rigaNumero", rigaNumero);
        return query.uniqueResult();
    }
}
