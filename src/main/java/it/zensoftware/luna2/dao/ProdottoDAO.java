package it.zensoftware.luna2.dao;

import it.zensoftware.luna2.model.Prodotto;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class ProdottoDAO extends GenericDAOImpl<Prodotto, Long> {
    
    private static final Logger logger = LogManager.getLogger(ProdottoDAO.class);

    public ProdottoDAO() {
        super(Prodotto.class);
    }

    public List<Prodotto> findAllActive() {
        try (Session session = getSession()) {
            Query<Prodotto> query = session.createQuery(
                "FROM Prodotto WHERE attivo = true ORDER BY nome", Prodotto.class);
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding active products", e);
            throw new RuntimeException(e);
        }
    }

    public List<Prodotto> searchByNameOrCode(String searchTerm) {
        try (Session session = getSession()) {
            Query<Prodotto> query = session.createQuery(
                "FROM Prodotto WHERE LOWER(nome) LIKE LOWER(:searchTerm) OR LOWER(codice) LIKE LOWER(:searchTerm)", 
                Prodotto.class);
            query.setParameter("searchTerm", "%" + searchTerm + "%");
            return query.list();
        } catch (Exception e) {
            logger.error("Error searching products", e);
            throw new RuntimeException(e);
        }
    }

    public Prodotto findByCodice(String codice) {
        try (Session session = getSession()) {
            Query<Prodotto> query = session.createQuery(
                "FROM Prodotto WHERE codice = :codice", Prodotto.class);
            query.setParameter("codice", codice);
            return query.uniqueResult();
        } catch (Exception e) {
            logger.error("Error finding product by code", e);
            return null;
        }
    }

    public Prodotto findByCodiceEan(String codiceEan) {
        try (Session session = getSession()) {
            Query<Prodotto> query = session.createQuery(
                "FROM Prodotto WHERE codiceEan = :codiceEan", Prodotto.class);
            query.setParameter("codiceEan", codiceEan);
            return query.uniqueResult();
        } catch (Exception e) {
            logger.error("Error finding product by EAN code", e);
            return null;
        }
    }

    public List<Prodotto> findByCategoria(String categoria) {
        try (Session session = getSession()) {
            Query<Prodotto> query = session.createQuery(
                "FROM Prodotto WHERE categoria = :categoria AND attivo = true ORDER BY nome", 
                Prodotto.class);
            query.setParameter("categoria", categoria);
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding products by category", e);
            throw new RuntimeException(e);
        }
    }
}
