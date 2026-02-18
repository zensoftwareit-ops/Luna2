package it.zensoftware.luna2.dao;

import it.zensoftware.luna2.model.OrdineRiga;
import org.hibernate.Session;
import org.hibernate.query.Query;

import java.util.List;

/**
 * Data Access Object for OrdineRiga
 */
public class OrdineRigaDAO extends GenericDAOImpl<OrdineRiga, Long> {
    
    public OrdineRigaDAO() {
        super(OrdineRiga.class);
    }

    public List<OrdineRiga> findByOrdineId(Long ordineId) {
        Session session = getSession();
        try {
            Query<OrdineRiga> query = session.createQuery("FROM OrdineRiga WHERE ordine.id = :ordineId ORDER BY rigaNumero ASC", OrdineRiga.class);
            query.setParameter("ordineId", ordineId);
            return query.list();
        } finally {
            session.close();
        }
    }

    public void deleteByOrdineId(Long ordineId) {
        Session session = getSession();
        try {
            Query<?> query = session.createQuery("DELETE FROM OrdineRiga WHERE ordine.id = :ordineId");
            query.setParameter("ordineId", ordineId);
            query.executeUpdate();
        } finally {
            session.close();
        }
    }
}
