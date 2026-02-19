package it.zensoftware.luna2.dao;

import it.zensoftware.luna2.model.Commessa;
import it.zensoftware.luna2.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.query.Query;

import java.util.List;

/**
 * DAO per la gestione delle Commesse
 */
public class ComMessaDAO {

    private Session getSession() {
        return HibernateUtil.getSessionFactory().getCurrentSession();
    }

    public void save(Commessa commessa) {
        try (Session session = getSession()) {
            session.beginTransaction();
            session.save(commessa);
            session.getTransaction().commit();
        }
    }

    public void update(Commessa commessa) {
        try (Session session = getSession()) {
            session.beginTransaction();
            session.update(commessa);
            session.getTransaction().commit();
        }
    }

    public void delete(Commessa commessa) {
        try (Session session = getSession()) {
            session.beginTransaction();
            session.delete(commessa);
            session.getTransaction().commit();
        }
    }

    public Commessa findById(Long id) {
        try (Session session = getSession()) {
            return session.get(Commessa.class, id);
        }
    }

    public List<Commessa> findAll() {
        try (Session session = getSession()) {
            Query<Commessa> query = session.createQuery("FROM Commessa ORDER BY dataApertura DESC", Commessa.class);
            return query.list();
        }
    }

    public List<Commessa> findByAnno(Integer anno) {
        try (Session session = getSession()) {
            Query<Commessa> query = session.createQuery(
                "FROM Commessa WHERE anno = :anno ORDER BY numero DESC", Commessa.class);
            query.setParameter("anno", anno);
            return query.list();
        }
    }

    public List<Commessa> findByStato(Commessa.StatoCommessa stato) {
        try (Session session = getSession()) {
            Query<Commessa> query = session.createQuery(
                "FROM Commessa WHERE stato = :stato ORDER BY dataApertura DESC", Commessa.class);
            query.setParameter("stato", stato);
            return query.list();
        }
    }

    public List<Commessa> findByPreventivo(Long preventivoId) {
        try (Session session = getSession()) {
            Query<Commessa> query = session.createQuery(
                "FROM Commessa WHERE preventivo.id = :preventivoId ORDER BY dataApertura DESC", Commessa.class);
            query.setParameter("preventivoId", preventivoId);
            return query.list();
        }
    }

    public Commessa findByFattura(Long fatturaId) {
        try (Session session = getSession()) {
            Query<Commessa> query = session.createQuery(
                "FROM Commessa WHERE fattura.id = :fatturaId", Commessa.class);
            query.setParameter("fatturaId", fatturaId);
            return query.uniqueResult();
        }
    }

    public Commessa findByNumero(String numero) {
        try (Session session = getSession()) {
            Query<Commessa> query = session.createQuery(
                "FROM Commessa WHERE numero = :numero", Commessa.class);
            query.setParameter("numero", numero);
            return query.uniqueResult();
        }
    }

    public String getNextNumero(Integer anno) {
        try (Session session = getSession()) {
            Query<Long> query = session.createQuery(
                "SELECT MAX(CAST(SUBSTRING(numero, LOCATE('/', numero) + 1) as long)) FROM Commessa WHERE anno = :anno", 
                Long.class);
            query.setParameter("anno", anno);
            Long maxNum = query.uniqueResult();
            long nextNum = (maxNum != null ? maxNum : 0) + 1;
            return nextNum + "/" + anno;
        }
    }
}
