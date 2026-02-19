package it.zensoftware.luna2.dao;

import it.zensoftware.luna2.model.SdiNotifica;
import it.zensoftware.luna2.model.Fattura;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.persistence.Query;
import java.util.List;

public class SdiNotificaDAO {
    
    private static final Logger logger = LogManager.getLogger(SdiNotificaDAO.class);
    private SessionFactory sessionFactory;
    
    public SdiNotificaDAO(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }
    
    public SdiNotifica save(SdiNotifica notifica) {
        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();
            Long id = (Long) session.save(notifica);
            session.getTransaction().commit();
            notifica.setId(id);
            logger.info("SdiNotifica salvata con id=" + id);
            return notifica;
        } catch (Exception e) {
            logger.error("Errore nel salvataggio SdiNotifica", e);
            throw new RuntimeException("Errore nel salvataggio SdiNotifica: " + e.getMessage(), e);
        }
    }
    
    public SdiNotifica update(SdiNotifica notifica) {
        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();
            session.merge(notifica);
            session.getTransaction().commit();
            logger.info("SdiNotifica aggiornata con id=" + notifica.getId());
            return notifica;
        } catch (Exception e) {
            logger.error("Errore nell'aggiornamento SdiNotifica", e);
            throw new RuntimeException("Errore nell'aggiornamento SdiNotifica: " + e.getMessage(), e);
        }
    }
    
    public SdiNotifica findBySdiCodice(String sdiCodice) {
        try (Session session = sessionFactory.openSession()) {
            String hql = "FROM SdiNotifica WHERE sdiCodice = :codice";
            Query query = session.createQuery(hql);
            query.setParameter("codice", sdiCodice);
            List<SdiNotifica> results = query.getResultList();
            return results.isEmpty() ? null : results.get(0);
        } catch (Exception e) {
            logger.error("Errore nella ricerca SdiNotifica per codice=" + sdiCodice, e);
            return null;
        }
    }
    
    public SdiNotifica findByFatturaId(Long fatturaId) {
        try (Session session = sessionFactory.openSession()) {
            String hql = "FROM SdiNotifica WHERE fattura.id = :fatturaId ORDER BY dataRicezione DESC";
            Query query = session.createQuery(hql);
            query.setParameter("fatturaId", fatturaId);
            query.setMaxResults(1);
            List<SdiNotifica> results = query.getResultList();
            return results.isEmpty() ? null : results.get(0);
        } catch (Exception e) {
            logger.error("Errore nella ricerca SdiNotifica per fatturaId=" + fatturaId, e);
            return null;
        }
    }
    
    public List<SdiNotifica> findByFatturaIdAllNotifiche(Long fatturaId) {
        try (Session session = sessionFactory.openSession()) {
            String hql = "FROM SdiNotifica WHERE fattura.id = :fatturaId ORDER BY dataRicezione DESC";
            Query query = session.createQuery(hql);
            query.setParameter("fatturaId", fatturaId);
            return query.getResultList();
        } catch (Exception e) {
            logger.error("Errore nella ricerca SdiNotifiche per fatturaId=" + fatturaId, e);
            return List.of();
        }
    }
    
    public List<SdiNotifica> findByStato(String stato) {
        try (Session session = sessionFactory.openSession()) {
            String hql = "FROM SdiNotifica WHERE stato = :stato ORDER BY dataRicezione DESC";
            Query query = session.createQuery(hql);
            query.setParameter("stato", stato);
            return query.getResultList();
        } catch (Exception e) {
            logger.error("Errore nella ricerca SdiNotifiche per stato=" + stato, e);
            return List.of();
        }
    }
    
    public List<SdiNotifica> findNonLette() {
        try (Session session = sessionFactory.openSession()) {
            String hql = "FROM SdiNotifica WHERE letta = false ORDER BY dataRicezione DESC";
            Query query = session.createQuery(hql);
            return query.getResultList();
        } catch (Exception e) {
            logger.error("Errore nella ricerca SdiNotifiche non lette", e);
            return List.of();
        }
    }
}
