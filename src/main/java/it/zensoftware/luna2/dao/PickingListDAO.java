package it.zensoftware.luna2.dao;

import it.zensoftware.luna2.model.PickingList;
import it.zensoftware.luna2.model.PickingList.StatoPicking;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Date;
import java.util.List;

/**
 * DAO per la gestione delle picking lists
 */
public class PickingListDAO extends GenericDAOImpl<PickingList, Long> {
    
    private static final Logger logger = LogManager.getLogger(PickingListDAO.class);

    public PickingListDAO() {
        super(PickingList.class);
    }

    /**
     * Trova picking list con tutti gli items caricati
     */
    public PickingList findWithItems(Long id) {
        try (Session session = getSession()) {
            Query<PickingList> query = session.createQuery(
                "FROM PickingList p " +
                "LEFT JOIN FETCH p.items i " +
                "LEFT JOIN FETCH i.prodotto " +
                "WHERE p.id = :id", 
                PickingList.class);
            query.setParameter("id", id);
            return query.uniqueResult();
        } catch (Exception e) {
            logger.error("Error finding picking list with items", e);
            return null;
        }
    }

    /**
     * Trova picking lists per ordine
     */
    public List<PickingList> findByOrdine(Long ordineId) {
        try (Session session = getSession()) {
            Query<PickingList> query = session.createQuery(
                "FROM PickingList WHERE ordine.id = :ordineId ORDER BY dataCreazione DESC", 
                PickingList.class);
            query.setParameter("ordineId", ordineId);
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding picking lists by ordine", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Trova picking lists per warehouse
     */
    public List<PickingList> findByWarehouse(Long warehouseId, StatoPicking stato) {
        try (Session session = getSession()) {
            String hql = "FROM PickingList p " +
                        "LEFT JOIN FETCH p.ordine o " +
                        "WHERE p.warehouse.id = :warehouseId ";
            if (stato != null) {
                hql += "AND p.stato = :stato ";
            }
            hql += "ORDER BY p.priorita DESC, p.dataCreazione ASC";
            
            Query<PickingList> query = session.createQuery(hql, PickingList.class);
            query.setParameter("warehouseId", warehouseId);
            if (stato != null) {
                query.setParameter("stato", stato);
            }
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding picking lists by warehouse", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Trova picking lists per utente assegnato
     */
    public List<PickingList> findByUtente(Long utenteId, StatoPicking stato) {
        try (Session session = getSession()) {
            String hql = "FROM PickingList p " +
                        "LEFT JOIN FETCH p.ordine " +
                        "WHERE p.assegnatoA.id = :utenteId ";
            if (stato != null) {
                hql += "AND p.stato = :stato ";
            }
            hql += "ORDER BY p.priorita DESC, p.dataCreazione ASC";
            
            Query<PickingList> query = session.createQuery(hql, PickingList.class);
            query.setParameter("utenteId", utenteId);
            if (stato != null) {
                query.setParameter("stato", stato);
            }
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding picking lists by utente", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Trova picking lists in attesa (DRAFT o ASSEGNATO)
     */
    public List<PickingList> findInAttesa(Long warehouseId) {
        try (Session session = getSession()) {
            String hql = "FROM PickingList p " +
                        "LEFT JOIN FETCH p.ordine " +
                        "WHERE p.stato IN (:stati) ";
            if (warehouseId != null) {
                hql += "AND p.warehouse.id = :warehouseId ";
            }
            hql += "ORDER BY p.priorita DESC, p.dataCreazione ASC";
            
            Query<PickingList> query = session.createQuery(hql, PickingList.class);
            query.setParameterList("stati", List.of(StatoPicking.DRAFT, StatoPicking.ASSEGNATO));
            if (warehouseId != null) {
                query.setParameter("warehouseId", warehouseId);
            }
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding picking lists in attesa", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Trova picking lists in corso
     */
    public List<PickingList> findInCorso(Long warehouseId) {
        try (Session session = getSession()) {
            String hql = "FROM PickingList p " +
                        "LEFT JOIN FETCH p.ordine " +
                        "LEFT JOIN FETCH p.assegnatoA " +
                        "WHERE p.stato = :stato ";
            if (warehouseId != null) {
                hql += "AND p.warehouse.id = :warehouseId ";
            }
            hql += "ORDER BY p.priorita DESC, p.dataInizio ASC";
            
            Query<PickingList> query = session.createQuery(hql, PickingList.class);
            query.setParameter("stato", StatoPicking.IN_PROGRESS);
            if (warehouseId != null) {
                query.setParameter("warehouseId", warehouseId);
            }
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding picking lists in corso", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Trova picking lists completate in un periodo
     */
    public List<PickingList> findCompletate(Date dataInizio, Date dataFine, Long warehouseId) {
        try (Session session = getSession()) {
            String hql = "FROM PickingList p " +
                        "LEFT JOIN FETCH p.ordine " +
                        "LEFT JOIN FETCH p.completatoDa " +
                        "WHERE p.stato IN (:stati) " +
                        "AND p.dataCompletamento >= :dataInizio " +
                        "AND p.dataCompletamento < :dataFine ";
            if (warehouseId != null) {
                hql += "AND p.warehouse.id = :warehouseId ";
            }
            hql += "ORDER BY p.dataCompletamento DESC";
            
            Query<PickingList> query = session.createQuery(hql, PickingList.class);
            query.setParameterList("stati", List.of(StatoPicking.COMPLETATO, StatoPicking.PARZIALE));
            query.setParameter("dataInizio", dataInizio);
            query.setParameter("dataFine", dataFine);
            if (warehouseId != null) {
                query.setParameter("warehouseId", warehouseId);
            }
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding picking lists completate", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Cerca picking lists per numero ordine o numero picking
     */
    public List<PickingList> searchByNumero(String searchTerm, Long warehouseId) {
        try (Session session = getSession()) {
            String hql = "FROM PickingList p " +
                        "LEFT JOIN FETCH p.ordine o " +
                        "WHERE (LOWER(p.numero) LIKE LOWER(:searchTerm) " +
                        "OR LOWER(o.numero) LIKE LOWER(:searchTerm)) ";
            if (warehouseId != null) {
                hql += "AND p.warehouse.id = :warehouseId ";
            }
            hql += "ORDER BY p.dataCreazione DESC";
            
            Query<PickingList> query = session.createQuery(hql, PickingList.class);
            query.setParameter("searchTerm", "%" + searchTerm + "%");
            if (warehouseId != null) {
                query.setParameter("warehouseId", warehouseId);
            }
            return query.list();
        } catch (Exception e) {
            logger.error("Error searching picking lists", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Conta picking lists per stato
     */
    public long countByStato(StatoPicking stato, Long warehouseId) {
        try (Session session = getSession()) {
            String hql = "SELECT COUNT(p) FROM PickingList p WHERE p.stato = :stato ";
            if (warehouseId != null) {
                hql += "AND p.warehouse.id = :warehouseId";
            }
            
            Query<Long> query = session.createQuery(hql, Long.class);
            query.setParameter("stato", stato);
            if (warehouseId != null) {
                query.setParameter("warehouseId", warehouseId);
            }
            return query.uniqueResult();
        } catch (Exception e) {
            logger.error("Error counting picking lists by stato", e);
            return 0;
        }
    }

    /**
     * Trova picking list per numero
     */
    public PickingList findByNumero(String numero) {
        try (Session session = getSession()) {
            Query<PickingList> query = session.createQuery(
                "FROM PickingList WHERE numero = :numero", 
                PickingList.class);
            query.setParameter("numero", numero);
            return query.uniqueResult();
        } catch (Exception e) {
            logger.error("Error finding picking list by numero", e);
            return null;
        }
    }

    /**
     * Calcola il tempo medio di picking per warehouse
     */
    public Double getTempoMedioPicking(Long warehouseId, Date dataInizio, Date dataFine) {
        try (Session session = getSession()) {
            String hql = "SELECT AVG(TIMESTAMPDIFF(MINUTE, p.dataInizio, p.dataCompletamento)) " +
                        "FROM PickingList p " +
                        "WHERE p.stato IN (:stati) " +
                        "AND p.dataCompletamento >= :dataInizio " +
                        "AND p.dataCompletamento < :dataFine ";
            if (warehouseId != null) {
                hql += "AND p.warehouse.id = :warehouseId";
            }
            
            Query<Double> query = session.createQuery(hql, Double.class);
            query.setParameterList("stati", List.of(StatoPicking.COMPLETATO, StatoPicking.PARZIALE));
            query.setParameter("dataInizio", dataInizio);
            query.setParameter("dataFine", dataFine);
            if (warehouseId != null) {
                query.setParameter("warehouseId", warehouseId);
            }
            
            Double result = query.uniqueResult();
            return result != null ? result : 0.0;
        } catch (Exception e) {
            logger.error("Error calculating tempo medio picking", e);
            return 0.0;
        }
    }
}
