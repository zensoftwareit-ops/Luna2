package it.zensoftware.luna2.dao;

import it.zensoftware.luna2.model.AuditLog;
import it.zensoftware.luna2.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;

/**
 * DAO per AuditLog
 * Gestisce persistenza log di audit per tracciamento operazioni sensibili
 */
public class AuditLogDAO extends GenericDAOImpl<AuditLog, Long> {

    private static final Logger log = LoggerFactory.getLogger(AuditLogDAO.class);

    public AuditLogDAO() {
        super(AuditLog.class);
    }

    /**
     * Trova tutti i log di audit per uno specifico utente
     */
    public List<AuditLog> findByUserName(String userName) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<AuditLog> query = session.createQuery(
                "FROM AuditLog WHERE userName = :userName ORDER BY timestamp DESC",
                AuditLog.class
            );
            query.setParameter("userName", userName);
            return query.getResultList();
        } catch (Exception e) {
            log.error("Errore nel recupero log per utente: {}", userName, e);
            return List.of();
        }
    }

    /**
     * Trova tutti i log di audit per una specifica azione
     */
    public List<AuditLog> findByAction(String action) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<AuditLog> query = session.createQuery(
                "FROM AuditLog WHERE action = :action ORDER BY timestamp DESC",
                AuditLog.class
            );
            query.setParameter("action", action);
            return query.getResultList();
        } catch (Exception e) {
            log.error("Errore nel recupero log per azione: {}", action, e);
            return List.of();
        }
    }

    /**
     * Trova log di audit per tipo di entità
     */
    public List<AuditLog> findByEntityType(String entityType) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<AuditLog> query = session.createQuery(
                "FROM AuditLog WHERE entityType = :entityType ORDER BY timestamp DESC",
                AuditLog.class
            );
            query.setParameter("entityType", entityType);
            return query.getResultList();
        } catch (Exception e) {
            log.error("Errore nel recupero log per tipo entità: {}", entityType, e);
            return List.of();
        }
    }

    /**
     * Trova log di audit per intervallo di date
     */
    public List<AuditLog> findByDateRange(Date startDate, Date endDate) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<AuditLog> query = session.createQuery(
                "FROM AuditLog WHERE timestamp BETWEEN :startDate AND :endDate ORDER BY timestamp DESC",
                AuditLog.class
            );
            query.setParameter("startDate", startDate);
            query.setParameter("endDate", endDate);
            return query.getResultList();
        } catch (Exception e) {
            log.error("Errore nel recupero log per intervallo date", e);
            return List.of();
        }
    }

    /**
     * Trova log di audit per utente e azione
     */
    public List<AuditLog> findByUserNameAndAction(String userName, String action) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<AuditLog> query = session.createQuery(
                "FROM AuditLog WHERE userName = :userName AND action = :action ORDER BY timestamp DESC",
                AuditLog.class
            );
            query.setParameter("userName", userName);
            query.setParameter("action", action);
            return query.getResultList();
        } catch (Exception e) {
            log.error("Errore nel recupero log per utente e azione", e);
            return List.of();
        }
    }

    /**
     * Trova log per risultato (SUCCESS, FAILURE, ERROR)
     */
    public List<AuditLog> findByResult(String result) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<AuditLog> query = session.createQuery(
                "FROM AuditLog WHERE result = :result ORDER BY timestamp DESC",
                AuditLog.class
            );
            query.setParameter("result", result);
            return query.getResultList();
        } catch (Exception e) {
            log.error("Errore nel recupero log per risultato: {}", result, e);
            return List.of();
        }
    }

    /**
     * Conta log di audit per azione
     */
    public Long countByAction(String action) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<Long> query = session.createQuery(
                "SELECT COUNT(*) FROM AuditLog WHERE action = :action",
                Long.class
            );
            query.setParameter("action", action);
            return query.uniqueResult();
        } catch (Exception e) {
            log.error("Errore nel conteggio log per azione: {}", action, e);
            return 0L;
        }
    }

    /**
     * Conta log di audit per stato
     */
    public Long countByResult(String result) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<Long> query = session.createQuery(
                "SELECT COUNT(*) FROM AuditLog WHERE result = :result",
                Long.class
            );
            query.setParameter("result", result);
            return query.uniqueResult();
        } catch (Exception e) {
            log.error("Errore nel conteggio log per risultato: {}", result, e);
            return 0L;
        }
    }

    /**
     * Ottiene statistiche di audit
     */
    public AuditLogStatistics getStatistics() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            AuditLogStatistics stats = new AuditLogStatistics();

            Query<Long> totalQuery = session.createQuery(
                "SELECT COUNT(*) FROM AuditLog",
                Long.class
            );
            stats.totalLogs = totalQuery.uniqueResult();

            stats.successCount = countByResult("SUCCESS");
            stats.failureCount = countByResult("FAILURE");
            stats.errorCount = countByResult("ERROR");

            return stats;
        } catch (Exception e) {
            log.error("Errore nel calcolo statistiche audit", e);
            return new AuditLogStatistics();
        }
    }

    /**
     * Elimina log di audit più vecchi di una certa data (pulizia periodica)
     */
    public int deleteOlderThan(Date date) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<AuditLog> query = session.createQuery(
                "DELETE FROM AuditLog WHERE timestamp < :date",
                AuditLog.class
            );
            query.setParameter("date", date);
            return query.executeUpdate();
        } catch (Exception e) {
            log.error("Errore nell'eliminazione log vecchi", e);
            return 0;
        }
    }

    /**
     * Statistiche di audit
     */
    public static class AuditLogStatistics {
        public Long totalLogs = 0L;
        public Long successCount = 0L;
        public Long failureCount = 0L;
        public Long errorCount = 0L;

        public float getSuccessRate() {
            if (totalLogs == 0) return 0f;
            return ((float) successCount / totalLogs) * 100;
        }

        public float getFailureRate() {
            if (totalLogs == 0) return 0f;
            return ((float) failureCount / totalLogs) * 100;
        }

        public float getErrorRate() {
            if (totalLogs == 0) return 0f;
            return ((float) errorCount / totalLogs) * 100;
        }

        @Override
        public String toString() {
            return String.format(
                "AuditStats: total=%d, success=%d (%.1f%%), failure=%d (%.1f%%), error=%d (%.1f%%)",
                totalLogs, successCount, getSuccessRate(), failureCount, getFailureRate(),
                errorCount, getErrorRate()
            );
        }
    }
}
