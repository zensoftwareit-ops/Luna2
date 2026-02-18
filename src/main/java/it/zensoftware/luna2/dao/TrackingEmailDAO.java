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

    public Long countEmailsForPreventivo(Long preventivoId) {
        Session session = getSession();
        try {
            Query<Long> query = session.createQuery("SELECT COUNT(*) FROM TrackingEmail WHERE preventivo.id = :preventivoId", Long.class);
            query.setParameter("preventivoId", preventivoId);
            return query.uniqueResult();
        } finally {
            session.close();
        }
    }

    public Long countOpensForPreventivo(Long preventivoId) {
        Session session = getSession();
        try {
            Query<Long> query = session.createQuery("SELECT COUNT(*) FROM TrackingEmail WHERE preventivo.id = :preventivoId AND aperto = true", Long.class);
            query.setParameter("preventivoId", preventivoId);
            return query.uniqueResult();
        } finally {
            session.close();
        }
    }

    public Long countDownloadsForPreventivo(Long preventivoId) {
        Session session = getSession();
        try {
            Query<Long> query = session.createQuery("SELECT COUNT(*) FROM TrackingEmail WHERE preventivo.id = :preventivoId AND clickDownload > 0", Long.class);
            query.setParameter("preventivoId", preventivoId);
            return query.uniqueResult();
        } finally {
            session.close();
        }
    }

    public Long getTotalDownloadCount(Long preventivoId) {
        Session session = getSession();
        try {
            Query<Long> query = session.createQuery("SELECT SUM(clickDownload) FROM TrackingEmail WHERE preventivo.id = :preventivoId", Long.class);
            query.setParameter("preventivoId", preventivoId);
            Long result = query.uniqueResult();
            return result != null ? result : 0L;
        } finally {
            session.close();
        }
    }

    public List<TrackingEmail> findByOrdineId(Long ordineId) {
        Session session = getSession();
        try {
            Query<TrackingEmail> query = session.createQuery("FROM TrackingEmail WHERE ordine.id = :ordineId ORDER BY dataInvio DESC", TrackingEmail.class);
            query.setParameter("ordineId", ordineId);
            return query.list();
        } finally {
            session.close();
        }
    }

    public Long countEmailsForOrdine(Long ordineId) {
        Session session = getSession();
        try {
            Query<Long> query = session.createQuery("SELECT COUNT(*) FROM TrackingEmail WHERE ordine.id = :ordineId", Long.class);
            query.setParameter("ordineId", ordineId);
            return query.uniqueResult();
        } finally {
            session.close();
        }
    }

    public Long countOpensForOrdine(Long ordineId) {
        Session session = getSession();
        try {
            Query<Long> query = session.createQuery("SELECT COUNT(*) FROM TrackingEmail WHERE ordine.id = :ordineId AND aperto = true", Long.class);
            query.setParameter("ordineId", ordineId);
            return query.uniqueResult();
        } finally {
            session.close();
        }
    }

    public Long countDownloadsForOrdine(Long ordineId) {
        Session session = getSession();
        try {
            Query<Long> query = session.createQuery("SELECT COUNT(*) FROM TrackingEmail WHERE ordine.id = :ordineId AND clickDownload > 0", Long.class);
            query.setParameter("ordineId", ordineId);
            return query.uniqueResult();
        } finally {
            session.close();
        }
    }

    public Long getTotalDownloadCountOrdine(Long ordineId) {
        Session session = getSession();
        try {
            Query<Long> query = session.createQuery("SELECT SUM(clickDownload) FROM TrackingEmail WHERE ordine.id = :ordineId", Long.class);
            query.setParameter("ordineId", ordineId);
            Long result = query.uniqueResult();
            return result != null ? result : 0L;
        } finally {
            session.close();
        }
    }
}
