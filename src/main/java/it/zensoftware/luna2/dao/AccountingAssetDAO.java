package it.zensoftware.luna2.dao;

import it.zensoftware.luna2.model.AccountingAsset;
import org.hibernate.Session;

import java.util.List;

public class AccountingAssetDAO extends GenericDAOImpl<AccountingAsset, Long> {

    public AccountingAssetDAO() {
        super(AccountingAsset.class);
    }

    public List<AccountingAsset> findAllOrdered() {
        try (Session session = getSession()) {
            return session.createQuery(
                            "FROM AccountingAsset a ORDER BY a.code ASC, a.id ASC",
                            AccountingAsset.class)
                    .getResultList();
        }
    }

    public AccountingAsset findByCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return null;
        }
        try (Session session = getSession()) {
            return session.createQuery(
                            "FROM AccountingAsset a WHERE UPPER(a.code) = :code",
                            AccountingAsset.class)
                    .setParameter("code", code.trim().toUpperCase())
                    .setMaxResults(1)
                    .uniqueResult();
        }
    }

    public List<AccountingAsset> findActive() {
        try (Session session = getSession()) {
            return session.createQuery(
                            "FROM AccountingAsset a WHERE a.status = :status ORDER BY a.code ASC",
                            AccountingAsset.class)
                    .setParameter("status", AccountingAsset.AssetStatus.ACTIVE)
                    .getResultList();
        }
    }
}
