package it.zensoftware.luna2.dao;

import it.zensoftware.luna2.model.EcommerceOrder;
import it.zensoftware.luna2.model.EcommercePlatform;
import org.hibernate.Session;
import org.hibernate.query.Query;
import java.util.Date;
import java.util.List;

public class EcommerceOrderDAO extends GenericDAOImpl<EcommerceOrder, Long> {

    public EcommerceOrderDAO() {
        super(EcommerceOrder.class);
    }

    public EcommerceOrder findByPlatformAndExternalId(EcommercePlatform.PlatformType platformType, String externalOrderId) {
        try (Session session = getSession()) {
            Query<EcommerceOrder> query = session.createQuery(
                    "FROM EcommerceOrder WHERE platformType = :platformType AND externalOrderId = :externalOrderId",
                    EcommerceOrder.class);
            query.setParameter("platformType", platformType);
            query.setParameter("externalOrderId", externalOrderId);
            List<EcommerceOrder> results = query.getResultList();
            return results.isEmpty() ? null : results.get(0);
        }
    }

    public List<EcommerceOrder> findByPlatform(Long platformId) {
        try (Session session = getSession()) {
            Query<EcommerceOrder> query = session.createQuery(
                    "FROM EcommerceOrder WHERE ecommercePlatform.id = :platformId ORDER BY orderDate DESC",
                    EcommerceOrder.class);
            query.setParameter("platformId", platformId);
            return query.getResultList();
        }
    }

    public List<EcommerceOrder> findByDateRange(Date from, Date to) {
        try (Session session = getSession()) {
            Query<EcommerceOrder> query = session.createQuery(
                    "FROM EcommerceOrder WHERE orderDate >= :from AND orderDate <= :to ORDER BY orderDate DESC",
                    EcommerceOrder.class);
            query.setParameter("from", from);
            query.setParameter("to", to);
            return query.getResultList();
        }
    }

    public List<EcommerceOrder> findByOrderStatus(String status) {
        return findByProperty("orderStatus", status);
    }

    public Long countByStatus(String status) {
        try (Session session = getSession()) {
            Query<Long> query = session.createQuery(
                    "SELECT COUNT(*) FROM EcommerceOrder WHERE orderStatus = :status",
                    Long.class);
            query.setParameter("status", status);
            return query.uniqueResult();
        }
    }

    public List<EcommerceOrder> findByPlatformType(EcommercePlatform.PlatformType platformType) {
        return findByProperty("platformType", platformType);
    }
}

