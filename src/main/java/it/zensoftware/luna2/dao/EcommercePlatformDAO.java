package it.zensoftware.luna2.dao;

import it.zensoftware.luna2.model.EcommercePlatform;
import org.hibernate.Session;
import org.hibernate.query.Query;
import java.util.List;

public class EcommercePlatformDAO extends GenericDAOImpl<EcommercePlatform, Long> {

    public EcommercePlatformDAO() {
        super(EcommercePlatform.class);
    }

    public List<EcommercePlatform> findByPlatformType(EcommercePlatform.PlatformType platformType) {
        return findByProperty("platformType", platformType);
    }

    public List<EcommercePlatform> findActive() {
        return findByProperty("isActive", true);
    }

    public EcommercePlatform findByApiKey(String apiKey) {
        try (Session session = getSession()) {
            Query<EcommercePlatform> query = session.createQuery(
                    "FROM EcommercePlatform WHERE apiKey = :apiKey",
                    EcommercePlatform.class);
            query.setParameter("apiKey", apiKey);
            List<EcommercePlatform> results = query.getResultList();
            return results.isEmpty() ? null : results.get(0);
        }
    }
}

