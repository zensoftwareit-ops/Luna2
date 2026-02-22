package it.zensoftware.luna2.dao;

import it.zensoftware.luna2.model.Giacenza;
import it.zensoftware.luna2.model.Prodotto;
import it.zensoftware.luna2.model.Warehouse;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * DAO per la gestione delle giacenze (stock)
 */
public class GiacenzaDAO extends GenericDAOImpl<Giacenza, Long> {
    
    private static final Logger logger = LogManager.getLogger(GiacenzaDAO.class);

    public GiacenzaDAO() {
        super(Giacenza.class);
    }

    /**
     * Trova giacenza per warehouse e prodotto
     */
    public Giacenza findByWarehouseAndProdotto(Long warehouseId, Long prodottoId) {
        try (Session session = getSession()) {
            Query<Giacenza> query = session.createQuery(
                "FROM Giacenza WHERE warehouse.id = :warehouseId AND prodotto.id = :prodottoId", 
                Giacenza.class);
            query.setParameter("warehouseId", warehouseId);
            query.setParameter("prodottoId", prodottoId);
            return query.uniqueResult();
        } catch (Exception e) {
            logger.error("Error finding giacenza", e);
            return null;
        }
    }

    /**
     * Trova tutte le giacenze di un warehouse
     */
    public List<Giacenza> findByWarehouse(Long warehouseId) {
        try (Session session = getSession()) {
            Query<Giacenza> query = session.createQuery(
                "FROM Giacenza g " +
                "LEFT JOIN FETCH g.prodotto " +
                "LEFT JOIN FETCH g.posizione " +
                "WHERE g.warehouse.id = :warehouseId " +
                "ORDER BY g.prodotto.codice ASC", 
                Giacenza.class);
            query.setParameter("warehouseId", warehouseId);
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding giacenze by warehouse", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Trova tutte le giacenze di un prodotto (in tutti i warehouse)
     */
    public List<Giacenza> findByProdotto(Long prodottoId) {
        try (Session session = getSession()) {
            Query<Giacenza> query = session.createQuery(
                "FROM Giacenza g " +
                "LEFT JOIN FETCH g.warehouse " +
                "LEFT JOIN FETCH g.posizione " +
                "WHERE g.prodotto.id = :prodottoId " +
                "ORDER BY g.quantitaDisponibile DESC", 
                Giacenza.class);
            query.setParameter("prodottoId", prodottoId);
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding giacenze by prodotto", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Trova giacenze sotto scorta (quantità < minima)
     */
    public List<Giacenza> findSottoScorta() {
        try (Session session = getSession()) {
            Query<Giacenza> query = session.createQuery(
                "FROM Giacenza g " +
                "LEFT JOIN FETCH g.prodotto " +
                "LEFT JOIN FETCH g.warehouse " +
                "WHERE g.quantitaMinima IS NOT NULL " +
                "AND g.quantitaAttuale < g.quantitaMinima " +
                "ORDER BY g.warehouse.nome, g.prodotto.codice", 
                Giacenza.class);
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding giacenze sotto scorta", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Trova giacenze sotto scorta per un warehouse specifico
     */
    public List<Giacenza> findSottoScorta(Long warehouseId) {
        if (warehouseId == null) {
            return findSottoScorta();
        }
        try (Session session = getSession()) {
            Query<Giacenza> query = session.createQuery(
                "FROM Giacenza g " +
                "LEFT JOIN FETCH g.prodotto " +
                "WHERE g.warehouse.id = :warehouseId " +
                "AND g.quantitaMinima IS NOT NULL " +
                "AND g.quantitaAttuale < g.quantitaMinima " +
                "ORDER BY g.prodotto.codice", 
                Giacenza.class);
            query.setParameter("warehouseId", warehouseId);
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding giacenze sotto scorta by warehouse", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Trova giacenze da riordinare (quantità <= punto riordino)
     */
    public List<Giacenza> findDaRiordinare() {
        try (Session session = getSession()) {
            Query<Giacenza> query = session.createQuery(
                "FROM Giacenza g " +
                "LEFT JOIN FETCH g.prodotto " +
                "LEFT JOIN FETCH g.warehouse " +
                "WHERE g.puntoRiordino IS NOT NULL " +
                "AND g.quantitaAttuale <= g.puntoRiordino " +
                "ORDER BY g.warehouse.nome, g.prodotto.codice",
                Giacenza.class);
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding giacenze da riordinare", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Trova giacenze da riordinare per un warehouse specifico
     */
    public List<Giacenza> findDaRiordinare(Long warehouseId) {
        if (warehouseId == null) {
            return findDaRiordinare();
        }
        try (Session session = getSession()) {
            Query<Giacenza> query = session.createQuery(
                "FROM Giacenza g " +
                "LEFT JOIN FETCH g.prodotto " +
                "WHERE g.warehouse.id = :warehouseId " +
                "AND g.puntoRiordino IS NOT NULL " +
                "AND g.quantitaAttuale <= g.puntoRiordino " +
                "ORDER BY g.prodotto.codice",
                Giacenza.class);
            query.setParameter("warehouseId", warehouseId);
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding giacenze da riordinare by warehouse", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Trova giacenze positive (con stock > 0)
     */
    public List<Giacenza> findPositive(Long warehouseId) {
        try (Session session = getSession()) {
            String hql = "FROM Giacenza g " +
                        "LEFT JOIN FETCH g.prodotto " +
                        "WHERE g.quantitaAttuale > 0 ";
            if (warehouseId != null) {
                hql += "AND g.warehouse.id = :warehouseId ";
            }
            hql += "ORDER BY g.prodotto.codice ASC";
            
            Query<Giacenza> query = session.createQuery(hql, Giacenza.class);
            if (warehouseId != null) {
                query.setParameter("warehouseId", warehouseId);
            }
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding positive giacenze", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Calcola il valore totale del magazzino
     */
    public BigDecimal calcolaValoreTotale(Long warehouseId) {
        try (Session session = getSession()) {
            String hql = "SELECT SUM(g.valoreGiacenza) FROM Giacenza g WHERE 1=1 ";
            if (warehouseId != null) {
                hql += "AND g.warehouse.id = :warehouseId";
            }
            
            Query<BigDecimal> query = session.createQuery(hql, BigDecimal.class);
            if (warehouseId != null) {
                query.setParameter("warehouseId", warehouseId);
            }
            BigDecimal result = query.uniqueResult();
            return result != null ? result : BigDecimal.ZERO;
        } catch (Exception e) {
            logger.error("Error calculating total warehouse value", e);
            return BigDecimal.ZERO;
        }
    }

    /**
     * Conta i prodotti con giacenza > 0 in un warehouse
     */
    public long countProdottiInStock(Long warehouseId) {
        try (Session session = getSession()) {
            String hql = "SELECT COUNT(g) FROM Giacenza g " +
                        "WHERE g.quantitaAttuale > 0 ";
            if (warehouseId != null) {
                hql += "AND g.warehouse.id = :warehouseId";
            }
            
            Query<Long> query = session.createQuery(hql, Long.class);
            if (warehouseId != null) {
                query.setParameter("warehouseId", warehouseId);
            }
            return query.uniqueResult();
        } catch (Exception e) {
            logger.error("Error counting products in stock", e);
            return 0;
        }
    }

    /**
     * Cerca giacenze per prodotto (codice o nome)
     */
    public List<Giacenza> searchByProdotto(Long warehouseId, String searchTerm) {
        try (Session session = getSession()) {
            String hql = "FROM Giacenza g " +
                        "LEFT JOIN FETCH g.prodotto p " +
                        "WHERE (LOWER(p.codice) LIKE LOWER(:searchTerm) " +
                        "OR LOWER(p.nome) LIKE LOWER(:searchTerm)) ";
            if (warehouseId != null) {
                hql += "AND g.warehouse.id = :warehouseId ";
            }
            hql += "ORDER BY p.codice ASC";
            
            Query<Giacenza> query = session.createQuery(hql, Giacenza.class);
            query.setParameter("searchTerm", "%" + searchTerm + "%");
            if (warehouseId != null) {
                query.setParameter("warehouseId", warehouseId);
            }
            return query.list();
        } catch (Exception e) {
            logger.error("Error searching giacenze", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Crea o aggiorna una giacenza
     */
    public Giacenza createOrUpdate(Warehouse warehouse, Prodotto prodotto, BigDecimal quantita, BigDecimal costoUnitario) {
        Giacenza giacenza = findByWarehouseAndProdotto(warehouse.getId(), prodotto.getId());
        
        if (giacenza == null) {
            giacenza = new Giacenza(warehouse, prodotto);
            giacenza.setQuantitaAttuale(quantita);
            if (costoUnitario != null) {
                giacenza.setCostoMedioPonderato(costoUnitario);
            }
            giacenza.ricalcolaDisponibile();
            giacenza.calcolaValoreGiacenza();
            save(giacenza);
        } else {
            giacenza.aggiornaDopoMovimento(quantita, costoUnitario);
            update(giacenza);
        }
        
        return giacenza;
    }

    /**
     * Ottiene la disponibilità totale di un prodotto (somma di tutti i warehouse)
     */
    public BigDecimal getDisponibilitaTotale(Long prodottoId) {
        try (Session session = getSession()) {
            Query<BigDecimal> query = session.createQuery(
                "SELECT SUM(g.quantitaDisponibile) FROM Giacenza g " +
                "WHERE g.prodotto.id = :prodottoId", 
                BigDecimal.class);
            query.setParameter("prodottoId", prodottoId);
            BigDecimal result = query.uniqueResult();
            return result != null ? result : BigDecimal.ZERO;
        } catch (Exception e) {
            logger.error("Error getting total disponibilità", e);
            return BigDecimal.ZERO;
        }
    }

    /**
     * Aggiorna la quantità impegnata
     */
    public void aggiornaQuantitaImpegnata(Long giacenzaId, BigDecimal delta) {
        Session session = null;
        try {
            session = getSession();
            session.beginTransaction();
            
            Giacenza giacenza = session.get(Giacenza.class, giacenzaId);
            if (giacenza != null) {
                BigDecimal nuovaImpegnata = giacenza.getQuantitaImpegnata().add(delta);
                giacenza.setQuantitaImpegnata(nuovaImpegnata);
                giacenza.ricalcolaDisponibile();
                session.update(giacenza);
            }
            
            session.getTransaction().commit();
        } catch (Exception e) {
            if (session != null && session.getTransaction().isActive()) {
                session.getTransaction().rollback();
            }
            logger.error("Error updating quantità impegnata", e);
            throw new RuntimeException(e);
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }
}
