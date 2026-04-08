package it.zensoftware.luna2.dao;

import it.zensoftware.luna2.model.Fornitore;
import it.zensoftware.luna2.model.Prodotto;
import it.zensoftware.luna2.model.ProdottoFornitoreListino;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ProdottoFornitoreListinoDAO extends GenericDAOImpl<ProdottoFornitoreListino, Long> {

    private static final Logger logger = LogManager.getLogger(ProdottoFornitoreListinoDAO.class);

    public ProdottoFornitoreListinoDAO() {
        super(ProdottoFornitoreListino.class);
    }

    public List<ProdottoFornitoreListino> findByProdottoId(Long prodottoId) {
        try (Session session = getSession()) {
            Query<ProdottoFornitoreListino> query = session.createQuery(
                "FROM ProdottoFornitoreListino l WHERE l.prodotto.id = :prodottoId AND l.attivo = true ORDER BY l.fornitore.ragioneSociale",
                ProdottoFornitoreListino.class
            );
            query.setParameter("prodottoId", prodottoId);
            return query.list();
        } catch (Exception e) {
            logger.error("Errore caricando listini fornitore per prodotto {}", prodottoId, e);
            return new ArrayList<>();
        }
    }

    public List<ProdottoFornitoreListino> findAllActive() {
        try (Session session = getSession()) {
            Query<ProdottoFornitoreListino> query = session.createQuery(
                "FROM ProdottoFornitoreListino l WHERE l.attivo = true",
                ProdottoFornitoreListino.class
            );
            return query.list();
        } catch (Exception e) {
            logger.error("Errore caricando listini fornitore attivi", e);
            return new ArrayList<>();
        }
    }

    public ProdottoFornitoreListino findByProdottoAndFornitore(Long prodottoId, Long fornitoreId) {
        try (Session session = getSession()) {
            Query<ProdottoFornitoreListino> query = session.createQuery(
                "FROM ProdottoFornitoreListino l WHERE l.prodotto.id = :prodottoId AND l.fornitore.id = :fornitoreId AND l.attivo = true",
                ProdottoFornitoreListino.class
            );
            query.setParameter("prodottoId", prodottoId);
            query.setParameter("fornitoreId", fornitoreId);
            return query.uniqueResult();
        } catch (Exception e) {
            logger.error("Errore caricando listino per prodotto {} fornitore {}", prodottoId, fornitoreId, e);
            return null;
        }
    }

    public void replaceListinoProdotto(Prodotto prodotto, List<Long> fornitoreIds, List<BigDecimal> prezziAcquisto) {
        Transaction tx = null;
        try (Session session = getSession()) {
            tx = session.beginTransaction();

            Query<?> deleteQ = session.createQuery("DELETE FROM ProdottoFornitoreListino l WHERE l.prodotto.id = :prodottoId");
            deleteQ.setParameter("prodottoId", prodotto.getId());
            deleteQ.executeUpdate();

            if (fornitoreIds != null && prezziAcquisto != null) {
                int len = Math.min(fornitoreIds.size(), prezziAcquisto.size());
                for (int i = 0; i < len; i++) {
                    Long fornitoreId = fornitoreIds.get(i);
                    BigDecimal prezzo = prezziAcquisto.get(i);
                    if (fornitoreId == null || prezzo == null || prezzo.compareTo(BigDecimal.ZERO) < 0) {
                        continue;
                    }
                    Fornitore fornitoreRef = session.get(Fornitore.class, fornitoreId);
                    if (fornitoreRef == null) {
                        continue;
                    }
                    ProdottoFornitoreListino row = new ProdottoFornitoreListino();
                    row.setProdotto(prodotto);
                    row.setFornitore(fornitoreRef);
                    row.setPrezzoAcquisto(prezzo);
                    row.setAttivo(true);
                    session.save(row);
                }
            }

            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            logger.error("Errore sostituendo listino fornitore del prodotto {}", prodotto != null ? prodotto.getId() : null, e);
            throw new RuntimeException("Errore salvataggio listini fornitore", e);
        }
    }
}
