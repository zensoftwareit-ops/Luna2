package it.zensoftware.luna2.dao;

import it.zensoftware.luna2.model.PayrollRun;
import org.hibernate.Session;

import java.util.List;

public class PayrollRunDAO extends GenericDAOImpl<PayrollRun, Long> {

    public PayrollRunDAO() {
        super(PayrollRun.class);
    }

    public List<PayrollRun> findByCompany(Long companyId) {
        try (Session session = getSession()) {
            return session.createQuery(
                            "FROM PayrollRun p WHERE p.companyId = :companyId ORDER BY p.year DESC, p.month DESC, p.id DESC",
                            PayrollRun.class)
                    .setParameter("companyId", companyId)
                    .getResultList();
        }
    }

    public PayrollRun findByPeriod(Long companyId, Integer month, Integer year) {
        try (Session session = getSession()) {
            return session.createQuery(
                            "FROM PayrollRun p WHERE p.companyId = :companyId AND p.month = :month AND p.year = :year",
                            PayrollRun.class)
                    .setParameter("companyId", companyId)
                    .setParameter("month", month)
                    .setParameter("year", year)
                    .setMaxResults(1)
                    .uniqueResult();
        }
    }

    public PayrollRun findLatestByCompany(Long companyId) {
        try (Session session = getSession()) {
            return session.createQuery(
                            "FROM PayrollRun p WHERE p.companyId = :companyId ORDER BY p.year DESC, p.month DESC, p.id DESC",
                            PayrollRun.class)
                    .setParameter("companyId", companyId)
                    .setMaxResults(1)
                    .uniqueResult();
        }
    }
}
