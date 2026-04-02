package it.zensoftware.luna2.dao;

import it.zensoftware.luna2.dto.AccountingBalanceRow;
import it.zensoftware.luna2.model.AccountingAccount;
import it.zensoftware.luna2.model.AccountingEntry;
import it.zensoftware.luna2.model.AccountingEntryLine;
import it.zensoftware.luna2.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AccountingEntryDAO extends GenericDAOImpl<AccountingEntry, Long> {

    public AccountingEntryDAO() {
        super(AccountingEntry.class);
    }

    public List<AccountingEntry> findRecent(int limit) {
        try (Session session = getSession()) {
            return session.createQuery("FROM AccountingEntry e ORDER BY e.entryDate DESC, e.id DESC", AccountingEntry.class)
                    .setMaxResults(limit)
                    .getResultList();
        }
    }

    public List<AccountingEntry> findAllOrdered() {
        try (Session session = getSession()) {
            return session.createQuery("FROM AccountingEntry e ORDER BY e.entryDate ASC, e.id ASC", AccountingEntry.class)
                    .getResultList();
        }
    }

    public List<AccountingEntry> findPostedByYear(int year) {
        try (Session session = getSession()) {
            return session.createQuery(
                            "FROM AccountingEntry e WHERE e.status = :status AND year(e.entryDate) = :year ORDER BY e.entryDate ASC, e.id ASC",
                            AccountingEntry.class)
                    .setParameter("status", AccountingEntry.EntryStatus.POSTED)
                    .setParameter("year", year)
                    .getResultList();
        }
    }

    public AccountingEntry findWithLines(Long id) {
        if (id == null) {
            return null;
        }
        try (Session session = getSession()) {
            AccountingEntry entry = session.get(AccountingEntry.class, id);
            if (entry != null) {
                entry.getLines().size();
            }
            return entry;
        }
    }

    public void saveOrUpdateEntry(AccountingEntry entry) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.saveOrUpdate(entry);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw new RuntimeException("Error saving accounting entry", e);
        }
    }

    public String getNextProtocolNumber() {
        int year = Calendar.getInstance().get(Calendar.YEAR);
        try (Session session = getSession()) {
            Long current = session.createQuery(
                    "SELECT COUNT(e.id) FROM AccountingEntry e WHERE year(e.entryDate) = :year", Long.class)
                    .setParameter("year", year)
                    .uniqueResult();
            long progressive = (current == null ? 0L : current) + 1L;
            return String.format("PN-%04d-%05d", year, progressive);
        }
    }

    public AccountingEntry findBySource(String sourceType, Long sourceId) {
        if (sourceType == null || sourceId == null) {
            return null;
        }
        try (Session session = getSession()) {
            return session.createQuery(
                            "FROM AccountingEntry e WHERE e.sourceType = :sourceType AND e.sourceId = :sourceId",
                            AccountingEntry.class)
                    .setParameter("sourceType", sourceType)
                    .setParameter("sourceId", sourceId)
                    .uniqueResult();
        }
    }

    public List<AccountingBalanceRow> computeTrialBalance() {
        try (Session session = getSession()) {
            List<AccountingAccount> accounts = session.createQuery(
                            "FROM AccountingAccount a WHERE a.enabled = true ORDER BY a.code", AccountingAccount.class)
                    .getResultList();

            Map<Long, AccountingBalanceRow> rows = new HashMap<>();
            for (AccountingAccount account : accounts) {
                rows.put(account.getId(), new AccountingBalanceRow(account.getId(), account.getCode(), account.getName()));
            }

            List<Object[]> lineTotals = session.createQuery(
                            "SELECT l.account.id, l.lineType, COALESCE(SUM(l.amount), 0) " +
                                    "FROM AccountingEntryLine l " +
                                    "JOIN l.entry e " +
                                    "WHERE e.status = :status " +
                                    "GROUP BY l.account.id, l.lineType", Object[].class)
                    .setParameter("status", AccountingEntry.EntryStatus.POSTED)
                    .getResultList();

            for (Object[] row : lineTotals) {
                Long accountId = (Long) row[0];
                AccountingEntryLine.LineType lineType = (AccountingEntryLine.LineType) row[1];
                BigDecimal amount = (BigDecimal) row[2];

                AccountingBalanceRow balanceRow = rows.get(accountId);
                if (balanceRow == null) {
                    continue;
                }
                if (lineType == AccountingEntryLine.LineType.DEBIT) {
                    balanceRow.setTotalDebit(balanceRow.getTotalDebit().add(amount));
                } else {
                    balanceRow.setTotalCredit(balanceRow.getTotalCredit().add(amount));
                }
                balanceRow.setBalance(balanceRow.getTotalDebit().subtract(balanceRow.getTotalCredit()));
            }

            return new ArrayList<>(rows.values());
        }
    }

    public List<AccountingBalanceRow> computeTrialBalanceByYear(int year) {
        try (Session session = getSession()) {
            List<AccountingAccount> accounts = session.createQuery(
                            "FROM AccountingAccount a WHERE a.enabled = true ORDER BY a.code", AccountingAccount.class)
                    .getResultList();

            Map<Long, AccountingBalanceRow> rows = new HashMap<>();
            for (AccountingAccount account : accounts) {
                rows.put(account.getId(), new AccountingBalanceRow(account.getId(), account.getCode(), account.getName()));
            }

            List<Object[]> lineTotals = session.createQuery(
                            "SELECT l.account.id, l.lineType, COALESCE(SUM(l.amount), 0) " +
                                    "FROM AccountingEntryLine l " +
                                    "JOIN l.entry e " +
                                    "WHERE e.status = :status AND year(e.entryDate) = :year " +
                                    "GROUP BY l.account.id, l.lineType", Object[].class)
                    .setParameter("status", AccountingEntry.EntryStatus.POSTED)
                    .setParameter("year", year)
                    .getResultList();

            for (Object[] row : lineTotals) {
                Long accountId = (Long) row[0];
                AccountingEntryLine.LineType lineType = (AccountingEntryLine.LineType) row[1];
                BigDecimal amount = (BigDecimal) row[2];

                AccountingBalanceRow balanceRow = rows.get(accountId);
                if (balanceRow == null) {
                    continue;
                }
                if (lineType == AccountingEntryLine.LineType.DEBIT) {
                    balanceRow.setTotalDebit(balanceRow.getTotalDebit().add(amount));
                } else {
                    balanceRow.setTotalCredit(balanceRow.getTotalCredit().add(amount));
                }
                balanceRow.setBalance(balanceRow.getTotalDebit().subtract(balanceRow.getTotalCredit()));
            }

            return new ArrayList<>(rows.values());
        }
    }

    public List<AccountingEntryLine> findLedgerLines(Long accountId) {
        if (accountId == null) {
            return new ArrayList<>();
        }
        try (Session session = getSession()) {
            List<AccountingEntryLine> lines = session.createQuery(
                            "FROM AccountingEntryLine l " +
                                    "JOIN FETCH l.entry e " +
                                    "JOIN FETCH l.account a " +
                                    "WHERE a.id = :accountId " +
                                    "ORDER BY e.entryDate ASC, e.id ASC, l.id ASC", AccountingEntryLine.class)
                    .setParameter("accountId", accountId)
                    .getResultList();
            return lines;
        }
    }

    public BigDecimal sumByLineType(String lineType) {
        try (Session session = getSession()) {
            BigDecimal value = session.createQuery(
                    "SELECT COALESCE(SUM(l.amount), 0) FROM AccountingEntryLine l WHERE l.lineType = :lineType",
                    BigDecimal.class)
                    .setParameter("lineType", it.zensoftware.luna2.model.AccountingEntryLine.LineType.valueOf(lineType))
                    .uniqueResult();
            return value == null ? BigDecimal.ZERO : value;
        }
    }

    public Long countPosted() {
        try (Session session = getSession()) {
            return session.createQuery(
                    "SELECT COUNT(e.id) FROM AccountingEntry e WHERE e.status = :status", Long.class)
                    .setParameter("status", AccountingEntry.EntryStatus.POSTED)
                    .uniqueResult();
        }
    }
}