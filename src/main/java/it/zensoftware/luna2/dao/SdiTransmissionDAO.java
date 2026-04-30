package it.zensoftware.luna2.dao;

import it.zensoftware.luna2.model.SdiTransmission;
import it.zensoftware.luna2.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * DAO per SdiTransmission
 * Gestisce persistenza trasmissioni E-invoicing
 */
public class SdiTransmissionDAO extends GenericDAOImpl<SdiTransmission, Long> {

    private static final Logger log = LoggerFactory.getLogger(SdiTransmissionDAO.class);

    public SdiTransmissionDAO() {
        super(SdiTransmission.class);
    }

    public SdiTransmission findByIdTrasmissione(String idTrasmissione) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<SdiTransmission> query = session.createQuery(
                "FROM SdiTransmission WHERE idTrasmissione = :idTrasmissione",
                SdiTransmission.class
            );
            query.setParameter("idTrasmissione", idTrasmissione);
            return query.uniqueResult();
        } catch (Exception e) {
            log.error("Errore nel recupero trasmissione per ID: {}", idTrasmissione, e);
            return null;
        }
    }

    public List<SdiTransmission> findByFatturaId(Long fatturaId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<SdiTransmission> query = session.createQuery(
                "FROM SdiTransmission WHERE fatturaId = :fatturaId ORDER BY dataCreazione DESC",
                SdiTransmission.class
            );
            query.setParameter("fatturaId", fatturaId);
            return query.getResultList();
        } catch (Exception e) {
            log.error("Errore nel recupero trasmissioni per fattura: {}", fatturaId, e);
            return List.of();
        }
    }

    public List<SdiTransmission> findPendingTransmissions() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<SdiTransmission> query = session.createQuery(
                "FROM SdiTransmission WHERE stato IN ('SUBMITTED', 'PENDING') ORDER BY dataCreazione ASC",
                SdiTransmission.class
            );
            return query.getResultList();
        } catch (Exception e) {
            log.error("Errore nel recupero trasmissioni pending", e);
            return List.of();
        }
    }

    public List<SdiTransmission> findErrorTransmissions() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<SdiTransmission> query = session.createQuery(
                "FROM SdiTransmission WHERE stato IN ('REJECTED', 'ERROR') ORDER BY dataCreazione DESC",
                SdiTransmission.class
            );
            return query.getResultList();
        } catch (Exception e) {
            log.error("Errore nel recupero trasmissioni fallite", e);
            return List.of();
        }
    }

    public Long countByStato(SdiTransmission.StatoTrasmissione stato) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<Long> query = session.createQuery(
                "SELECT COUNT(*) FROM SdiTransmission WHERE stato = :stato",
                Long.class
            );
            query.setParameter("stato", stato);
            return query.uniqueResult();
        } catch (Exception e) {
            log.error("Errore nel conteggio trasmissioni per stato: {}", stato, e);
            return 0L;
        }
    }

    public SdiTransmissionStats getStatistics() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            SdiTransmissionStats stats = new SdiTransmissionStats();

            Query<Long> totalQuery = session.createQuery(
                "SELECT COUNT(*) FROM SdiTransmission",
                Long.class
            );
            stats.totale = totalQuery.uniqueResult();

            stats.submitted = countByStato(SdiTransmission.StatoTrasmissione.SUBMITTED);
            stats.accepted = countByStato(SdiTransmission.StatoTrasmissione.ACCEPTED);
            stats.rejected = countByStato(SdiTransmission.StatoTrasmissione.REJECTED);
            stats.delivered = countByStato(SdiTransmission.StatoTrasmissione.DELIVERED);

            return stats;
        } catch (Exception e) {
            log.error("Errore nel calcolo statistiche trasmissioni", e);
            return new SdiTransmissionStats();
        }
    }

    public static class SdiTransmissionStats {
        public Long totale = 0L;
        public Long submitted = 0L;
        public Long accepted = 0L;
        public Long rejected = 0L;
        public Long delivered = 0L;

        public float getSuccessRate() {
            if (totale == 0) return 0f;
            return ((float) (accepted + delivered) / totale) * 100;
        }

        public float getErrorRate() {
            if (totale == 0) return 0f;
            return ((float) rejected / totale) * 100;
        }

        @Override
        public String toString() {
            return String.format(
                "SdiStats: total=%d, submitted=%d, accepted=%d, rejected=%d, delivered=%d, success=%.1f%%, error=%.1f%%",
                totale, submitted, accepted, rejected, delivered, getSuccessRate(), getErrorRate()
            );
        }
    }
}
