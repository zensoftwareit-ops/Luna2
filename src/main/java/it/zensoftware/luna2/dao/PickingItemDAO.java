package it.zensoftware.luna2.dao;

import it.zensoftware.luna2.model.PickingItem;
import it.zensoftware.luna2.model.PickingItem.StatoItem;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

/**
 * DAO per la gestione dei picking items
 */
public class PickingItemDAO extends GenericDAOImpl<PickingItem, Long> {
    
    private static final Logger logger = LogManager.getLogger(PickingItemDAO.class);

    public PickingItemDAO() {
        super(PickingItem.class);
    }

    /**
     * Trova tutti gli items di una picking list
     */
    public List<PickingItem> findByPickingList(Long pickingListId) {
        try (Session session = getSession()) {
            Query<PickingItem> query = session.createQuery(
                "FROM PickingItem i " +
                "LEFT JOIN FETCH i.prodotto " +
                "LEFT JOIN FETCH i.posizione " +
                "LEFT JOIN FETCH i.giacenza " +
                "WHERE i.pickingList.id = :pickingListId " +
                "ORDER BY i.numeroLinea ASC", 
                PickingItem.class);
            query.setParameter("pickingListId", pickingListId);
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding picking items by list", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Trova items per prodotto in tutte le picking lists
     */
    public List<PickingItem> findByProdotto(Long prodottoId) {
        try (Session session = getSession()) {
            Query<PickingItem> query = session.createQuery(
                "FROM PickingItem i " +
                "LEFT JOIN FETCH i.pickingList p " +
                "WHERE i.prodotto.id = :prodottoId " +
                "ORDER BY p.dataCreazione DESC", 
                PickingItem.class);
            query.setParameter("prodottoId", prodottoId);
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding picking items by prodotto", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Trova items pending (non ancora prelevati)
     */
    public List<PickingItem> findPending(Long pickingListId) {
        try (Session session = getSession()) {
            String hql = "FROM PickingItem i " +
                        "LEFT JOIN FETCH i.prodotto " +
                        "LEFT JOIN FETCH i.posizione " +
                        "WHERE i.stato IN (:stati) ";
            if (pickingListId != null) {
                hql += "AND i.pickingList.id = :pickingListId ";
            }
            hql += "ORDER BY i.prioritaPrelievo DESC, i.numeroLinea ASC";
            
            Query<PickingItem> query = session.createQuery(hql, PickingItem.class);
            query.setParameterList("stati", List.of(StatoItem.PENDING, StatoItem.IN_PICKING));
            if (pickingListId != null) {
                query.setParameter("pickingListId", pickingListId);
            }
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding pending picking items", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Trova items completati
     */
    public List<PickingItem> findCompletati(Long pickingListId) {
        try (Session session = getSession()) {
            Query<PickingItem> query = session.createQuery(
                "FROM PickingItem i " +
                "LEFT JOIN FETCH i.prodotto " +
                "WHERE i.pickingList.id = :pickingListId " +
                "AND i.stato IN (:stati) " +
                "ORDER BY i.numeroLinea ASC", 
                PickingItem.class);
            query.setParameter("pickingListId", pickingListId);
            query.setParameterList("stati", List.of(StatoItem.PICKED, StatoItem.VERIFICATO));
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding completed picking items", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Trova items per posizione
     */
    public List<PickingItem> findByPosizione(Long posizioneId, StatoItem stato) {
        try (Session session = getSession()) {
            String hql = "FROM PickingItem i " +
                        "LEFT JOIN FETCH i.prodotto " +
                        "LEFT JOIN FETCH i.pickingList p " +
                        "WHERE i.posizione.id = :posizioneId ";
            if (stato != null) {
                hql += "AND i.stato = :stato ";
            }
            hql += "ORDER BY p.priorita DESC, i.numeroLinea ASC";
            
            Query<PickingItem> query = session.createQuery(hql, PickingItem.class);
            query.setParameter("posizioneId", posizioneId);
            if (stato != null) {
                query.setParameter("stato", stato);
            }
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding picking items by posizione", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Conta gli items per stato in una picking list
     */
    public long countByStato(Long pickingListId, StatoItem stato) {
        try (Session session = getSession()) {
            Query<Long> query = session.createQuery(
                "SELECT COUNT(i) FROM PickingItem i " +
                "WHERE i.pickingList.id = :pickingListId AND i.stato = :stato", 
                Long.class);
            query.setParameter("pickingListId", pickingListId);
            query.setParameter("stato", stato);
            return query.uniqueResult();
        } catch (Exception e) {
            logger.error("Error counting picking items by stato", e);
            return 0;
        }
    }

    /**
     * Trova item per barcode scansionato
     */
    public PickingItem findByBarcode(Long pickingListId, String barcode) {
        try (Session session = getSession()) {
            Query<PickingItem> query = session.createQuery(
                "FROM PickingItem i " +
                "LEFT JOIN FETCH i.prodotto p " +
                "WHERE i.pickingList.id = :pickingListId " +
                "AND (p.codiceEan = :barcode OR i.barcodeScansionato = :barcode)", 
                PickingItem.class);
            query.setParameter("pickingListId", pickingListId);
            query.setParameter("barcode", barcode);
            return query.uniqueResult();
        } catch (Exception e) {
            logger.error("Error finding picking item by barcode", e);
            return null;
        }
    }

    /**
     * Aggiorna lo stato di tutti gli items di una picking list
     */
    public void updateStatoByPickingList(Long pickingListId, StatoItem statoOld, StatoItem statoNew) {
        Session session = null;
        try {
            session = getSession();
            session.beginTransaction();
            
            Query<?> query = session.createQuery(
                "UPDATE PickingItem SET stato = :statoNew " +
                "WHERE pickingList.id = :pickingListId AND stato = :statoOld");
            query.setParameter("statoNew", statoNew);
            query.setParameter("pickingListId", pickingListId);
            query.setParameter("statoOld", statoOld);
            query.executeUpdate();
            
            session.getTransaction().commit();
        } catch (Exception e) {
            if (session != null && session.getTransaction().isActive()) {
                session.getTransaction().rollback();
            }
            logger.error("Error updating picking items stato", e);
            throw new RuntimeException(e);
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    /**
     * Elimina tutti gli items di una picking list
     */
    public void deleteByPickingList(Long pickingListId) {
        Session session = null;
        try {
            session = getSession();
            session.beginTransaction();
            
            Query<?> query = session.createQuery(
                "DELETE FROM PickingItem WHERE pickingList.id = :pickingListId");
            query.setParameter("pickingListId", pickingListId);
            query.executeUpdate();
            
            session.getTransaction().commit();
        } catch (Exception e) {
            if (session != null && session.getTransaction().isActive()) {
                session.getTransaction().rollback();
            }
            logger.error("Error deleting picking items", e);
            throw new RuntimeException(e);
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    /**
     * Verifica se un prodotto è presente in picking lists attive
     */
    public boolean isProdottoInPickingAttivo(Long prodottoId) {
        try (Session session = getSession()) {
            Query<Long> query = session.createQuery(
                "SELECT COUNT(i) FROM PickingItem i " +
                "JOIN i.pickingList p " +
                "WHERE i.prodotto.id = :prodottoId " +
                "AND p.stato IN (:stati)", 
                Long.class);
            query.setParameter("prodottoId", prodottoId);
            query.setParameterList("stati", List.of(
                it.zensoftware.luna2.model.PickingList.StatoPicking.DRAFT,
                it.zensoftware.luna2.model.PickingList.StatoPicking.ASSEGNATO,
                it.zensoftware.luna2.model.PickingList.StatoPicking.IN_PROGRESS
            ));
            return query.uniqueResult() > 0;
        } catch (Exception e) {
            logger.error("Error checking if prodotto is in active picking", e);
            return false;
        }
    }
}
