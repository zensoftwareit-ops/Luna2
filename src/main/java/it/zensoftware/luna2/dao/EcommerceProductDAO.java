package it.zensoftware.luna2.dao;

import it.zensoftware.luna2.model.EcommerceProduct;
import it.zensoftware.luna2.model.EcommercePlatform;
import org.hibernate.Session;
import org.hibernate.query.Query;
import java.util.List;

public class EcommerceProductDAO extends GenericDAOImpl<EcommerceProduct, Long> {

    public EcommerceProductDAO() {
        super(EcommerceProduct.class);
    }

    public EcommerceProduct findByPlatformAndExternalId(EcommercePlatform.PlatformType platformType, String externalProductId) {
        try (Session session = getSession()) {
            Query<EcommerceProduct> query = session.createQuery(
                    "FROM EcommerceProduct WHERE platformType = :platformType AND externalProductId = :externalProductId",
                    EcommerceProduct.class);
            query.setParameter("platformType", platformType);
            query.setParameter("externalProductId", externalProductId);
            List<EcommerceProduct> results = query.getResultList();
            return results.isEmpty() ? null : results.get(0);
        }
    }

    public List<EcommerceProduct> findByPlatform(Long platformId) {
        try (Session session = getSession()) {
            Query<EcommerceProduct> query = session.createQuery(
                    "FROM EcommerceProduct WHERE ecommercePlatform.id = :platformId ORDER BY productName",
                    EcommerceProduct.class);
            query.setParameter("platformId", platformId);
            return query.getResultList();
        }
    }

    public List<EcommerceProduct> findBySku(String sku) {
        return findByProperty("sku", sku);
    }

    public List<EcommerceProduct> findLowStock(Integer threshold) {
        try (Session session = getSession()) {
            Query<EcommerceProduct> query = session.createQuery(
                    "FROM EcommerceProduct WHERE stockQuantity < :threshold ORDER BY stockQuantity ASC",
                    EcommerceProduct.class);
            query.setParameter("threshold", threshold);
            return query.getResultList();
        }
    }

    public List<EcommerceProduct> findByName(String searchTerm) {
        try (Session session = getSession()) {
            Query<EcommerceProduct> query = session.createQuery(
                    "FROM EcommerceProduct WHERE LOWER(productName) LIKE :search ORDER BY productName",
                    EcommerceProduct.class);
            String searchPattern = "%" + searchTerm.toLowerCase() + "%";
            query.setParameter("search", searchPattern);
            return query.getResultList();
        }
    }

    public List<EcommerceProduct> findByPlatformType(EcommercePlatform.PlatformType platformType) {
        return findByProperty("platformType", platformType);
    }
}

