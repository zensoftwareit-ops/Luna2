package it.zensoftware.luna2.dao;

import it.zensoftware.luna2.model.FatturaRiga;
import org.hibernate.Session;
import org.hibernate.query.Query;

import java.util.List;

public class FatturaRigaDAO extends GenericDAOImpl<FatturaRiga, Long> {

    public FatturaRigaDAO() {
        super(FatturaRiga.class);
    }

    /**
     * Finds all line items for a specific fattura
     */
    public List<FatturaRiga> findByFatturaId(Long fatturaId) {
        try (Session session = getSession()) {
            Query<FatturaRiga> query = session.createQuery(
                    "FROM FatturaRiga WHERE fattura.id = :fatturaId ORDER BY rigaNumero ASC",
                    FatturaRiga.class);
            query.setParameter("fatturaId", fatturaId);
            return query.getResultList();
        }
    }

    /**
     * Deletes all line items for a specific fattura
     */
    public void deleteByFatturaId(Long fatturaId) {
        try (Session session = getSession()) {
            Query query = session.createQuery("DELETE FROM FatturaRiga WHERE fattura.id = :fatturaId");
            query.setParameter("fatturaId", fatturaId);
            query.executeUpdate();
        }
    }
}
