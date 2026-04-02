package it.zensoftware.luna2.dao;

import it.zensoftware.luna2.model.AdminOperation;
import org.hibernate.Session;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

public class AdminOperationDAO extends GenericDAOImpl<AdminOperation, Long> {

    public AdminOperationDAO() {
        super(AdminOperation.class);
    }

    public List<AdminOperation> findAllOrdered() {
        try (Session session = getSession()) {
            return session.createQuery(
                            "FROM AdminOperation a ORDER BY a.dueDate ASC, a.id DESC",
                            AdminOperation.class)
                    .getResultList();
        }
    }

    public List<AdminOperation> findByArea(AdminOperation.Area area) {
        try (Session session = getSession()) {
            return session.createQuery(
                            "FROM AdminOperation a WHERE a.area = :area ORDER BY a.dueDate ASC, a.id DESC",
                            AdminOperation.class)
                    .setParameter("area", area)
                    .getResultList();
        }
    }

    public long countByStatus(AdminOperation.Status status) {
        try (Session session = getSession()) {
            return session.createQuery(
                            "SELECT COUNT(a.id) FROM AdminOperation a WHERE a.status = :status",
                            Long.class)
                    .setParameter("status", status)
                    .uniqueResult();
        }
    }

    public long countOverdue(Date today) {
        try (Session session = getSession()) {
            return session.createQuery(
                            "SELECT COUNT(a.id) FROM AdminOperation a WHERE a.status <> :completed AND a.dueDate < :today",
                            Long.class)
                    .setParameter("completed", AdminOperation.Status.COMPLETED)
                    .setParameter("today", today)
                    .uniqueResult();
        }
    }

    public BigDecimal sumOpenAmountByArea(AdminOperation.Area area) {
        try (Session session = getSession()) {
            BigDecimal sum = session.createQuery(
                            "SELECT COALESCE(SUM(a.amountDue), 0) FROM AdminOperation a WHERE a.area = :area AND a.status <> :completed",
                            BigDecimal.class)
                    .setParameter("area", area)
                    .setParameter("completed", AdminOperation.Status.COMPLETED)
                    .uniqueResult();
            return sum == null ? BigDecimal.ZERO : sum;
        }
    }
}
