package it.zensoftware.luna2.dao;

import it.zensoftware.luna2.model.TrackingEmail;
import org.hibernate.Session;
import org.hibernate.query.Query;

import java.util.List;

public class TrackingEmailDAO extends GenericDAOImpl<TrackingEmail, Long> {
    
    public TrackingEmailDAO() {
        super(TrackingEmail.class);
    }

    public TrackingEmail findByTrackingId(String trackingId) {
        Session session = getSession();
        try {
            Query<TrackingEmail> query = session.createQuery("FROM TrackingEmail WHERE trackingId = :trackingId", TrackingEmail.class);
            query.setParameter("trackingId", trackingId);
            return query.uniqueResult();
        } finally {
            session.close();
        }
    }

    public List<TrackingEmail> findByPreventivoId(Long preventivoId) {
        Session session = getSession();
        try {
            Query<TrackingEmail> query = session.createQuery("FROM TrackingEmail WHERE preventivo.id = :preventivoId ORDER BY dataInvio DESC", TrackingEmail.class);
            query.setParameter("preventivoId", preventivoId);
            return query.list();
        } finally {
            session.close();
        }
    }

    public List<TrackingEmail> findByEmailDestinatario(String email) {
        Session session = getSession();
        try {
            Query<TrackingEmail> query = session.createQuery("FROM TrackingEmail WHERE emailDestinatario = :email ORDER BY dataInvio DESC", TrackingEmail.class);
            query.setParameter("email", email);
            return query.list();
        } finally {
            session.close();
        }
    }
}
