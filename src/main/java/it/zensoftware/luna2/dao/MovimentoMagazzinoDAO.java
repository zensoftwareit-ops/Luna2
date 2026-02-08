package it.zensoftware.luna2.dao;

import it.zensoftware.luna2.model.MovimentoMagazzino;
import it.zensoftware.luna2.model.Prodotto;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Date;
import java.util.List;

public class MovimentoMagazzinoDAO extends GenericDAOImpl<MovimentoMagazzino, Long> {
    private static final Logger logger = LogManager.getLogger(MovimentoMagazzinoDAO.class);

    public MovimentoMagazzinoDAO() {
        super(MovimentoMagazzino.class);
    }

    public void saveMovimento(MovimentoMagazzino movimento) {
        try (Session session = getSession()) {
            session.beginTransaction();
            session.save(movimento);
            session.getTransaction().commit();
            logger.info("Movimento magazzino salvato: ID {}", movimento.getId());
        } catch (Exception e) {
            logger.error("Errore durante il salvataggio del movimento magazzino", e);
            throw new RuntimeException("Errore durante il salvataggio del movimento magazzino", e);
        }
    }

    public MovimentoMagazzino findById(Long id) {
        try (Session session = getSession()) {
            return session.get(MovimentoMagazzino.class, id);
        } catch (Exception e) {
            logger.error("Errore durante il recupero del movimento magazzino ID: {}", id, e);
            return null;
        }
    }

    public List<MovimentoMagazzino> findAll() {
        try (Session session = getSession()) {
            Query<MovimentoMagazzino> query = session.createQuery(
                "FROM MovimentoMagazzino ORDER BY dataMovimento DESC, id DESC", 
                MovimentoMagazzino.class
            );
            return query.list();
        } catch (Exception e) {
            logger.error("Errore durante il recupero di tutti i movimenti magazzino", e);
            return List.of();
        }
    }

    public List<MovimentoMagazzino> findByProdotto(Long prodottoId) {
        try (Session session = getSession()) {
            Query<MovimentoMagazzino> query = session.createQuery(
                "FROM MovimentoMagazzino m WHERE m.prodotto.id = :prodottoId ORDER BY m.dataMovimento DESC, m.id DESC",
                MovimentoMagazzino.class
            );
            query.setParameter("prodottoId", prodottoId);
            return query.list();
        } catch (Exception e) {
            logger.error("Errore durante il recupero dei movimenti per prodotto ID: {}", prodottoId, e);
            return List.of();
        }
    }

    public List<MovimentoMagazzino> findByProdotto(Long prodottoId, int maxResults) {
        try (Session session = getSession()) {
            Query<MovimentoMagazzino> query = session.createQuery(
                "FROM MovimentoMagazzino m WHERE m.prodotto.id = :prodottoId ORDER BY m.dataMovimento DESC, m.id DESC",
                MovimentoMagazzino.class
            );
            query.setParameter("prodottoId", prodottoId);
            query.setMaxResults(maxResults);
            return query.list();
        } catch (Exception e) {
            logger.error("Errore durante il recupero dei movimenti per prodotto ID: {}", prodottoId, e);
            return List.of();
        }
    }

    public List<MovimentoMagazzino> findByDateRange(Date dataInizio, Date dataFine) {
        try (Session session = getSession()) {
            Query<MovimentoMagazzino> query = session.createQuery(
                "FROM MovimentoMagazzino m WHERE m.dataMovimento BETWEEN :dataInizio AND :dataFine ORDER BY m.dataMovimento DESC",
                MovimentoMagazzino.class
            );
            query.setParameter("dataInizio", dataInizio);
            query.setParameter("dataFine", dataFine);
            return query.list();
        } catch (Exception e) {
            logger.error("Errore durante il recupero dei movimenti per periodo", e);
            return List.of();
        }
    }

    public List<MovimentoMagazzino> findByTipoMovimento(MovimentoMagazzino.TipoMovimento tipo) {
        try (Session session = getSession()) {
            Query<MovimentoMagazzino> query = session.createQuery(
                "FROM MovimentoMagazzino m WHERE m.tipoMovimento = :tipo ORDER BY m.dataMovimento DESC",
                MovimentoMagazzino.class
            );
            query.setParameter("tipo", tipo);
            return query.list();
        } catch (Exception e) {
            logger.error("Errore durante il recupero dei movimenti per tipo: {}", tipo, e);
            return List.of();
        }
    }

    public void delete(Long id) {
        try (Session session = getSession()) {
            session.beginTransaction();
            MovimentoMagazzino movimento = session.get(MovimentoMagazzino.class, id);
            if (movimento != null) {
                session.delete(movimento);
            }
            session.getTransaction().commit();
            logger.info("Movimento magazzino eliminato: ID {}", id);
        } catch (Exception e) {
            logger.error("Errore durante l'eliminazione del movimento magazzino ID: {}", id, e);
            throw new RuntimeException("Errore durante l'eliminazione del movimento magazzino", e);
        }
    }

    public long countByProdotto(Long prodottoId) {
        try (Session session = getSession()) {
            Query<Long> query = session.createQuery(
                "SELECT COUNT(m) FROM MovimentoMagazzino m WHERE m.prodotto.id = :prodottoId",
                Long.class
            );
            query.setParameter("prodottoId", prodottoId);
            return query.uniqueResult();
        } catch (Exception e) {
            logger.error("Errore durante il conteggio dei movimenti per prodotto ID: {}", prodottoId, e);
            return 0L;
        }
    }
}
