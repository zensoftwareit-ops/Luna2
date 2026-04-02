package it.zensoftware.luna2.dao;

import it.zensoftware.luna2.model.AccountingAccount;
import org.hibernate.Session;
import org.hibernate.query.Query;

import java.util.List;

public class AccountingAccountDAO extends GenericDAOImpl<AccountingAccount, Long> {

    public AccountingAccountDAO() {
        super(AccountingAccount.class);
    }

    public List<AccountingAccount> findEnabled() {
        try (Session session = getSession()) {
            return session.createQuery("FROM AccountingAccount WHERE enabled = true ORDER BY code", AccountingAccount.class)
                    .getResultList();
        }
    }

    public List<AccountingAccount> findAllOrdered() {
        try (Session session = getSession()) {
            return session.createQuery("FROM AccountingAccount ORDER BY code", AccountingAccount.class)
                    .getResultList();
        }
    }

    public AccountingAccount findByCode(String code) {
        try (Session session = getSession()) {
            Query<AccountingAccount> query = session.createQuery(
                    "FROM AccountingAccount WHERE code = :code", AccountingAccount.class);
            query.setParameter("code", code);
            return query.uniqueResult();
        }
    }
}