package it.zensoftware.luna2.dao;

import it.zensoftware.luna2.model.Magazzino;
import it.zensoftware.luna2.model.Prodotto;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

public class MagazzinoDAO extends GenericDAOImpl<Magazzino, Long> {
    private static final Logger logger = LogManager.getLogger(MagazzinoDAO.class);

    public MagazzinoDAO() {
        super(Magazzino.class);
    }

    public void saveOrUpdate(Magazzino magazzino) {
        try (Session session = getSession()) {
            session.beginTransaction();
            magazzino.setDataAggiornamento(new Date());
            session.saveOrUpdate(magazzino);
            session.getTransaction().commit();
            logger.info("Magazzino salvato per prodotto ID: {}", magazzino.getProdotto().getId());
        } catch (Exception e) {
            logger.error("Errore durante il salvataggio del magazzino", e);
            throw new RuntimeException("Errore durante il salvataggio del magazzino", e);
        }
    }

    public Magazzino findById(Long id) {
        try (Session session = getSession()) {
            return session.get(Magazzino.class, id);
        } catch (Exception e) {
            logger.error("Errore durante il recupero del magazzino ID: {}", id, e);
            return null;
        }
    }

    public Magazzino findByProdotto(Long prodottoId) {
        try (Session session = getSession()) {
            Query<Magazzino> query = session.createQuery(
                "FROM Magazzino m WHERE m.prodotto.id = :prodottoId",
                Magazzino.class
            );
            query.setParameter("prodottoId", prodottoId);
            return query.uniqueResult();
        } catch (Exception e) {
            logger.error("Errore durante il recupero del magazzino per prodotto ID: {}", prodottoId, e);
            return null;
        }
    }

    public Magazzino getOrCreateByProdotto(Prodotto prodotto) {
        Magazzino magazzino = findByProdotto(prodotto.getId());
        if (magazzino == null) {
            magazzino = new Magazzino();
            magazzino.setProdotto(prodotto);
            magazzino.setGiacenzaAttuale(BigDecimal.ZERO);
            magazzino.setGiacenzaDisponibile(BigDecimal.ZERO);
            magazzino.setGiacenzaImpegnata(BigDecimal.ZERO);
            magazzino.setGiacenzaInOrdine(BigDecimal.ZERO);
            magazzino.setValoreMagazzino(BigDecimal.ZERO);
            saveOrUpdate(magazzino);
        }
        return magazzino;
    }

    public List<Magazzino> findAll() {
        try (Session session = getSession()) {
            Query<Magazzino> query = session.createQuery(
                "FROM Magazzino m ORDER BY m.prodotto.nome",
                Magazzino.class
            );
            return query.list();
        } catch (Exception e) {
            logger.error("Errore durante il recupero di tutti i magazzini", e);
            return List.of();
        }
    }

    public List<Magazzino> findAllWithGiacenza() {
        try (Session session = getSession()) {
            Query<Magazzino> query = session.createQuery(
                "FROM Magazzino m WHERE m.giacenzaAttuale > 0 ORDER BY m.prodotto.nome",
                Magazzino.class
            );
            return query.list();
        } catch (Exception e) {
            logger.error("Errore durante il recupero dei magazzini con giacenza", e);
            return List.of();
        }
    }

    public List<Magazzino> findSottoScorta() {
        try (Session session = getSession()) {
            Query<Magazzino> query = session.createQuery(
                "FROM Magazzino m WHERE m.giacenzaDisponibile < m.prodotto.giacenzaMinima AND m.prodotto.giacenzaMinima > 0 ORDER BY m.prodotto.nome",
                Magazzino.class
            );
            return query.list();
        } catch (Exception e) {
            logger.error("Errore durante il recupero dei prodotti sotto scorta", e);
            return List.of();
        }
    }

    public void aggiornaGiacenza(Long prodottoId, BigDecimal quantita) {
        try (Session session = getSession()) {
            session.beginTransaction();
            
            Magazzino magazzino = findByProdotto(prodottoId);
            if (magazzino == null) {
                logger.warn("Magazzino non trovato per prodotto ID: {}", prodottoId);
                session.getTransaction().rollback();
                return;
            }
            
            BigDecimal nuovaGiacenza = magazzino.getGiacenzaAttuale().add(quantita);
            magazzino.setGiacenzaAttuale(nuovaGiacenza);
            
            // Ricalcola giacenza disponibile
            BigDecimal disponibile = nuovaGiacenza.subtract(magazzino.getGiacenzaImpegnata());
            magazzino.setGiacenzaDisponibile(disponibile);
            
            magazzino.setDataAggiornamento(new Date());
            session.update(magazzino);
            
            session.getTransaction().commit();
            logger.info("Giacenza aggiornata per prodotto ID {}: {} -> {}", prodottoId, magazzino.getGiacenzaAttuale().subtract(quantita), nuovaGiacenza);
        } catch (Exception e) {
            logger.error("Errore durante l'aggiornamento della giacenza per prodotto ID: {}", prodottoId, e);
            throw new RuntimeException("Errore durante l'aggiornamento della giacenza", e);
        }
    }

    public void ricalcolaValoreMagazzino(Long prodottoId, BigDecimal costoMedio) {
        try (Session session = getSession()) {
            session.beginTransaction();
            
            Magazzino magazzino = findByProdotto(prodottoId);
            if (magazzino != null) {
                BigDecimal valore = magazzino.getGiacenzaAttuale().multiply(costoMedio);
                magazzino.setValoreMagazzino(valore);
                magazzino.setDataAggiornamento(new Date());
                session.update(magazzino);
            }
            
            session.getTransaction().commit();
        } catch (Exception e) {
            logger.error("Errore durante il ricalcolo del valore magazzino per prodotto ID: {}", prodottoId, e);
        }
    }

    public BigDecimal getValoreTotaleMagazzino() {
        try (Session session = getSession()) {
            Query<BigDecimal> query = session.createQuery(
                "SELECT SUM(m.valoreMagazzino) FROM Magazzino m",
                BigDecimal.class
            );
            BigDecimal totale = query.uniqueResult();
            return totale != null ? totale : BigDecimal.ZERO;
        } catch (Exception e) {
            logger.error("Errore durante il calcolo del valore totale magazzino", e);
            return BigDecimal.ZERO;
        }
    }

    public long countProdottiInMagazzino() {
        try (Session session = getSession()) {
            Query<Long> query = session.createQuery(
                "SELECT COUNT(m) FROM Magazzino m WHERE m.giacenzaAttuale > 0",
                Long.class
            );
            return query.uniqueResult();
        } catch (Exception e) {
            logger.error("Errore durante il conteggio dei prodotti in magazzino", e);
            return 0L;
        }
    }
}
