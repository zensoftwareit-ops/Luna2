package it.zensoftware.luna2.dao;

import it.zensoftware.luna2.model.EcommerceSyncLog;
import it.zensoftware.luna2.model.EcommercePlatform;
import org.hibernate.Session;
import org.hibernate.query.Query;
import java.util.Date;
import java.util.List;

public class EcommerceSyncLogDAO extends GenericDAOImpl<EcommerceSyncLog, Long> {

    public EcommerceSyncLogDAO() {
        super(EcommerceSyncLog.class);
    }

    public EcommerceSyncLog findLatestSync(EcommercePlatform.PlatformType platformType) {
        try (Session session = getSession()) {
            Query<EcommerceSyncLog> query = session.createQuery(
                    "FROM EcommerceSyncLog WHERE platformType = :platformType ORDER BY startTime DESC",
                    EcommerceSyncLog.class);
            query.setParameter("platformType", platformType);
            List<EcommerceSyncLog> results = query.getResultList();
            return results.isEmpty() ? null : results.get(0);
        }
    }

    public EcommerceSyncLog findLatestSyncByPlatform(Long platformId) {
        try (Session session = getSession()) {
            Query<EcommerceSyncLog> query = session.createQuery(
                    "FROM EcommerceSyncLog WHERE ecommercePlatform.id = :platformId ORDER BY startTime DESC",
                    EcommerceSyncLog.class);
            query.setParameter("platformId", platformId);
            List<EcommerceSyncLog> results = query.getResultList();
            return results.isEmpty() ? null : results.get(0);
        }
    }

    public List<EcommerceSyncLog> findSyncsByDateRange(Date from, Date to) {
        try (Session session = getSession()) {
            Query<EcommerceSyncLog> query = session.createQuery(
                    "FROM EcommerceSyncLog WHERE startTime >= :from AND startTime <= :to ORDER BY startTime DESC",
                    EcommerceSyncLog.class);
            query.setParameter("from", from);
            query.setParameter("to", to);
            return query.getResultList();
        }
    }

    public List<EcommerceSyncLog> findFailedSyncs() {
        try (Session session = getSession()) {
            Query<EcommerceSyncLog> query = session.createQuery(
                    "FROM EcommerceSyncLog WHERE status = :status ORDER BY startTime DESC",
                    EcommerceSyncLog.class);
            query.setParameter("status", EcommerceSyncLog.SyncStatus.FAILED);
            return query.getResultList();
        }
    }

    public List<EcommerceSyncLog> findSyncsByType(EcommerceSyncLog.SyncType syncType) {
        return findByProperty("syncType", syncType);
    }

    public List<EcommerceSyncLog> findSyncsByPlatformAndType(Long platformId, EcommerceSyncLog.SyncType syncType) {
        try (Session session = getSession()) {
            String hql = "FROM EcommerceSyncLog WHERE ecommercePlatform.id = :platformId ORDER BY startTime DESC";
            if (syncType != null) {
                hql = "FROM EcommerceSyncLog WHERE ecommercePlatform.id = :platformId AND syncType = :syncType ORDER BY startTime DESC";
            }
            Query<EcommerceSyncLog> query = session.createQuery(hql, EcommerceSyncLog.class);
            query.setParameter("platformId", platformId);
            if (syncType != null) {
                query.setParameter("syncType", syncType);
            }
            return query.getResultList();
        }
    }
}

