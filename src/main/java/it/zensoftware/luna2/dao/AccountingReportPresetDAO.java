package it.zensoftware.luna2.dao;

import it.zensoftware.luna2.model.AccountingReportPreset;
import org.hibernate.Session;

import java.util.List;

public class AccountingReportPresetDAO extends GenericDAOImpl<AccountingReportPreset, Long> {

    public AccountingReportPresetDAO() {
        super(AccountingReportPreset.class);
    }

    public List<AccountingReportPreset> findByUser(Long userId) {
        try (Session session = getSession()) {
            return session.createQuery(
                            "FROM AccountingReportPreset p WHERE p.user.id = :userId ORDER BY p.name ASC, p.id DESC",
                            AccountingReportPreset.class)
                    .setParameter("userId", userId)
                    .getResultList();
        }
    }

    public AccountingReportPreset findByUserAndName(Long userId, String name) {
        if (userId == null || name == null || name.trim().isEmpty()) {
            return null;
        }
        try (Session session = getSession()) {
            return session.createQuery(
                            "FROM AccountingReportPreset p WHERE p.user.id = :userId AND UPPER(p.name) = :name",
                            AccountingReportPreset.class)
                    .setParameter("userId", userId)
                    .setParameter("name", name.trim().toUpperCase())
                    .setMaxResults(1)
                    .uniqueResult();
        }
    }

    public void saveOrUpdate(AccountingReportPreset preset) {
        if (preset.getId() == null) {
            save(preset);
        } else {
            update(preset);
        }
    }
}
