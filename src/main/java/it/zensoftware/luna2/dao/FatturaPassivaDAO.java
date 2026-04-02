package it.zensoftware.luna2.dao;

import it.zensoftware.luna2.model.FatturaPassiva;
import org.hibernate.Session;
import org.hibernate.query.Query;

import java.util.List;

public class FatturaPassivaDAO extends GenericDAOImpl<FatturaPassiva, Long> {

    public FatturaPassivaDAO() {
        super(FatturaPassiva.class);
    }

    /**
     * Finds all passive invoices for a specific year
     */
    public List<FatturaPassiva> findByAnno(Integer anno) {
        if (anno == null) {
            return findAll();
        }
        try (Session session = getSession()) {
            Query<FatturaPassiva> query = session.createQuery(
                    "FROM FatturaPassiva f WHERE f.anno = :anno ORDER BY f.dataRicezione DESC",
                    FatturaPassiva.class);
            query.setParameter("anno", anno);
            return query.getResultList();
        }
    }

    /**
     * Finds passive invoices by supplier
     */
    public List<FatturaPassiva> findByFornitore(Long fornitoreId) {
        try (Session session = getSession()) {
            Query<FatturaPassiva> query = session.createQuery(
                    "FROM FatturaPassiva f WHERE f.fornitore.id = :fornitoreId ORDER BY f.dataRicezione DESC",
                    FatturaPassiva.class);
            query.setParameter("fornitoreId", fornitoreId);
            return query.getResultList();
        }
    }

    /**
     * Finds passive invoices by supplier PIVA
     */
    public List<FatturaPassiva> findByFornitorePiva(String piva) {
        try (Session session = getSession()) {
            Query<FatturaPassiva> query = session.createQuery(
                    "FROM FatturaPassiva f WHERE f.fornitorePiva = :piva ORDER BY f.dataRicezione DESC",
                    FatturaPassiva.class);
            query.setParameter("piva", piva);
            return query.getResultList();
        }
    }

    /**
     * Finds passive invoice by SDI message ID
     */
    public FatturaPassiva findBySdiIdMessaggio(String idMessaggio) {
        try (Session session = getSession()) {
            Query<FatturaPassiva> query = session.createQuery(
                    "FROM FatturaPassiva f WHERE f.sdiIdMessaggio = :idMessaggio",
                    FatturaPassiva.class);
            query.setParameter("idMessaggio", idMessaggio);
            List<FatturaPassiva> results = query.getResultList();
            return results.isEmpty() ? null : results.get(0);
        }
    }

    /**
     * Finds all passive invoices with specific payment status
     */
    public List<FatturaPassiva> findByStatoPagamento(FatturaPassiva.StatoPagamento stato) {
        try (Session session = getSession()) {
            Query<FatturaPassiva> query = session.createQuery(
                    "FROM FatturaPassiva f WHERE f.statoPagamento = :stato ORDER BY f.dataScadenza ASC",
                    FatturaPassiva.class);
            query.setParameter("stato", stato);
            return query.getResultList();
        }
    }

    /**
     * Finds passive invoices scadute (overdue)
     */
    public List<FatturaPassiva> findScadute() {
        try (Session session = getSession()) {
            Query<FatturaPassiva> query = session.createQuery(
                    "FROM FatturaPassiva f WHERE f.statoPagamento IN (:stati) " +
                    "AND f.dataScadenza < CURRENT_DATE ORDER BY f.dataScadenza ASC",
                    FatturaPassiva.class);
            query.setParameterList("stati", java.util.Arrays.asList(
                    FatturaPassiva.StatoPagamento.DA_PAGARE,
                    FatturaPassiva.StatoPagamento.PARZIALMENTE_PAGATA
            ));
            return query.getResultList();
        }
    }

    /**
     * Gets the next invoice number for a specific year
     */
    public Integer getNextNumero(Integer anno) {
        try (Session session = getSession()) {
            Query<Integer> query = session.createQuery(
                    "SELECT MAX(CAST(SUBSTRING(f.numero, 5) AS Integer)) FROM FatturaPassiva f WHERE f.anno = :anno",
                    Integer.class);
            query.setParameter("anno", anno);
            Integer maxNumero = query.uniqueResult();
            return (maxNumero == null) ? 1 : (maxNumero + 1);
        }
    }

    public List<FatturaPassiva> findAllOrdered() {
        try (Session session = getSession()) {
            return session.createQuery(
                            "FROM FatturaPassiva f ORDER BY f.dataFattura ASC, f.id ASC",
                            FatturaPassiva.class)
                    .getResultList();
        }
    }
}
