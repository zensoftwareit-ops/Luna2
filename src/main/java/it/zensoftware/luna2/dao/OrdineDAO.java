package it.zensoftware.luna2.dao;

import it.zensoftware.luna2.model.Ordine;
import it.zensoftware.luna2.model.Ordine.Stato;
import org.hibernate.Session;
import org.hibernate.query.Query;

import java.util.List;

public class OrdineDAO extends GenericDAOImpl<Ordine, Long> {

    public OrdineDAO() {
        super(Ordine.class);
    }

    /**
     * Finds all orders for a specific year
     */
    public List<Ordine> findByAnno(Integer anno) {
        if (anno == null) {
            return findAll();
        }
        try (Session session = getSession()) {
            Query<Ordine> query = session.createQuery(
                    "FROM Ordine o WHERE YEAR(o.dataCreazione) = :anno ORDER BY o.numero DESC",
                    Ordine.class);
            query.setParameter("anno", anno);
            return query.getResultList();
        }
    }

    /**
     * Finds all orders with a specific state
     */
    public List<Ordine> findByStato(Stato stato) {
        try (Session session = getSession()) {
            Query<Ordine> query = session.createQuery(
                    "FROM Ordine o WHERE o.stato = :stato ORDER BY o.dataCreazione DESC",
                    Ordine.class);
            query.setParameter("stato", stato);
            return query.getResultList();
        }
    }

    /**
     * Gets the next order number for a specific year
     */
    public Integer getNextNumero(Integer anno) {
        try (Session session = getSession()) {
            Query<Integer> query = session.createQuery(
                    "SELECT MAX(o.numero) FROM Ordine o WHERE YEAR(o.dataCreazione) = :anno",
                    Integer.class);
            query.setParameter("anno", anno);
            Integer maxNumero = query.uniqueResult();
            return (maxNumero == null) ? 1 : (maxNumero + 1);
        }
    }

    /**
     * Finds an order with all its line items (righe)
     */
    public Ordine findWithRighe(Long id) {
        if (id == null) {
            return null;
        }
        Ordine ordine = super.findById(id);
        if (ordine != null) {
            // Force lazy loading of righe
            ordine.getRighe().size();
        }
        return ordine;
    }

    /**
     * Find order by customer reference (used for eCommerce order linking)
     */
    public Ordine findByRiferimentoCliente(String riferimento) {
        try (Session session = getSession()) {
            Query<Ordine> query = session.createQuery(
                    "FROM Ordine o WHERE o.riferimentoCliente = :riferimento",
                    Ordine.class);
            query.setParameter("riferimento", riferimento);
            return query.uniqueResult();
        } catch (Exception e) {
            return null;
        }
    }
}

