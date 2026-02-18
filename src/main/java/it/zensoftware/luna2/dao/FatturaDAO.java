package it.zensoftware.luna2.dao;

import it.zensoftware.luna2.model.Fattura;
import it.zensoftware.luna2.model.Fattura.TipoFattura;
import org.hibernate.Session;
import org.hibernate.query.Query;

import java.util.List;

public class FatturaDAO extends GenericDAOImpl<Fattura, Long> {

    public FatturaDAO() {
        super(Fattura.class);
    }

    /**
     * Finds all invoices for a specific year
     */
    public List<Fattura> findByAnno(Integer anno) {
        if (anno == null) {
            return findAll();
        }
        try (Session session = getSession()) {
            Query<Fattura> query = session.createQuery(
                    "FROM Fattura f WHERE f.anno = :anno ORDER BY f.numero DESC",
                    Fattura.class);
            query.setParameter("anno", anno);
            return query.getResultList();
        }
    }

    /**
     * Finds all invoices with a specific type (PROFORMA or REALE)
     */
    public List<Fattura> findByTipo(TipoFattura tipo) {
        try (Session session = getSession()) {
            Query<Fattura> query = session.createQuery(
                    "FROM Fattura f WHERE f.tipoFattura = :tipo ORDER BY f.dataFattura DESC",
                    Fattura.class);
            query.setParameter("tipo", tipo);
            return query.getResultList();
        }
    }

    /**
     * Gets the next invoice number for a specific year
     */
    public Integer getNextNumero(Integer anno) {
        try (Session session = getSession()) {
            Query<Integer> query = session.createQuery(
                    "SELECT MAX(CAST(SUBSTRING(f.numero, 5) AS Integer)) FROM Fattura f WHERE f.anno = :anno",
                    Integer.class);
            query.setParameter("anno", anno);
            Integer maxNumero = query.uniqueResult();
            return (maxNumero == null) ? 1 : (maxNumero + 1);
        }
    }

    /**
     * Finds an invoice with all its line items (righe)
     */
    public Fattura findWithRighe(Long id) {
        if (id == null) {
            return null;
        }
        Fattura fattura = super.findById(id);
        if (fattura != null) {
            // Force lazy loading of righe
            fattura.getRighe().size();
        }
        return fattura;
    }
}
