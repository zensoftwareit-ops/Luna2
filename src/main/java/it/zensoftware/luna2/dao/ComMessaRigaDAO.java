package it.zensoftware.luna2.dao;

import it.zensoftware.luna2.model.ComMessaRiga;
import it.zensoftware.luna2.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.query.Query;

import java.util.List;

/**
 * DAO per le righe di Commessa
 */
public class ComMessaRigaDAO {

    private Session getSession() {
        return HibernateUtil.getSessionFactory().getCurrentSession();
    }

    public void save(ComMessaRiga riga) {
        try (Session session = getSession()) {
            session.beginTransaction();
            session.save(riga);
            session.getTransaction().commit();
        }
    }

    public void update(ComMessaRiga riga) {
        try (Session session = getSession()) {
            session.beginTransaction();
            session.update(riga);
            session.getTransaction().commit();
        }
    }

    public void delete(ComMessaRiga riga) {
        try (Session session = getSession()) {
            session.beginTransaction();
            session.delete(riga);
            session.getTransaction().commit();
        }
    }

    public ComMessaRiga findById(Long id) {
        try (Session session = getSession()) {
            return session.get(ComMessaRiga.class, id);
        }
    }

    public List<ComMessaRiga> findByCommessa(Long comMessaId) {
        try (Session session = getSession()) {
            Query<ComMessaRiga> query = session.createQuery(
                "FROM ComMessaRiga WHERE commessa.id = :comMessaId ORDER BY numeroRiga", ComMessaRiga.class);
            query.setParameter("comMessaId", comMessaId);
            return query.list();
        }
    }
}
