package it.zensoftware.luna2.dao;

import it.zensoftware.luna2.model.AccountingPostingConfig;
import it.zensoftware.luna2.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;

public class AccountingPostingConfigDAO extends GenericDAOImpl<AccountingPostingConfig, Long> {

    public AccountingPostingConfigDAO() {
        super(AccountingPostingConfig.class);
    }

    public AccountingPostingConfig findConfig(String logicalKey, String taxRegime, String businessForm) {
        try (Session session = getSession()) {
            AccountingPostingConfig exact = session.createQuery(
                            "FROM AccountingPostingConfig c " +
                                    "WHERE c.active = true AND c.logicalKey = :logicalKey " +
                                    "AND c.taxRegime = :taxRegime AND c.businessForm = :businessForm",
                            AccountingPostingConfig.class)
                    .setParameter("logicalKey", logicalKey)
                    .setParameter("taxRegime", taxRegime)
                    .setParameter("businessForm", businessForm)
                    .uniqueResult();
            if (exact != null) {
                return exact;
            }

            AccountingPostingConfig byRegime = session.createQuery(
                            "FROM AccountingPostingConfig c " +
                                    "WHERE c.active = true AND c.logicalKey = :logicalKey " +
                                    "AND c.taxRegime = :taxRegime AND c.businessForm IS NULL",
                            AccountingPostingConfig.class)
                    .setParameter("logicalKey", logicalKey)
                    .setParameter("taxRegime", taxRegime)
                    .uniqueResult();
            if (byRegime != null) {
                return byRegime;
            }

            return session.createQuery(
                            "FROM AccountingPostingConfig c " +
                                    "WHERE c.active = true AND c.logicalKey = :logicalKey " +
                                    "AND c.taxRegime IS NULL AND c.businessForm IS NULL",
                            AccountingPostingConfig.class)
                    .setParameter("logicalKey", logicalKey)
                    .uniqueResult();
        }
    }

    public List<AccountingPostingConfig> findAllOrdered() {
        try (Session session = getSession()) {
            return session.createQuery(
                            "FROM AccountingPostingConfig c ORDER BY c.logicalKey, c.taxRegime, c.businessForm",
                            AccountingPostingConfig.class)
                    .getResultList();
        }
    }

    public void saveOrUpdate(AccountingPostingConfig config) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.saveOrUpdate(config);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw new RuntimeException("Error saving posting config", e);
        }
    }
}