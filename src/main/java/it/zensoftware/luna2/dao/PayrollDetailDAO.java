package it.zensoftware.luna2.dao;

import it.zensoftware.luna2.model.PayrollDetail;
import org.hibernate.Session;

import java.util.List;

public class PayrollDetailDAO extends GenericDAOImpl<PayrollDetail, Long> {

    public PayrollDetailDAO() {
        super(PayrollDetail.class);
    }

    public List<PayrollDetail> findByRun(Long payrollRunId) {
        try (Session session = getSession()) {
            return session.createQuery(
                            "FROM PayrollDetail d WHERE d.payrollRunId = :runId ORDER BY d.employeeId ASC",
                            PayrollDetail.class)
                    .setParameter("runId", payrollRunId)
                    .getResultList();
        }
    }

    public PayrollDetail findByRunAndEmployee(Long payrollRunId, Long employeeId) {
        try (Session session = getSession()) {
            return session.createQuery(
                            "FROM PayrollDetail d WHERE d.payrollRunId = :runId AND d.employeeId = :employeeId",
                            PayrollDetail.class)
                    .setParameter("runId", payrollRunId)
                    .setParameter("employeeId", employeeId)
                    .setMaxResults(1)
                    .uniqueResult();
        }
    }

    public List<PayrollDetail> findByRunAndEmployeeFilter(Long payrollRunId, Long employeeId) {
        try (Session session = getSession()) {
            if (employeeId == null) {
                return findByRun(payrollRunId);
            }
            return session.createQuery(
                            "FROM PayrollDetail d WHERE d.payrollRunId = :runId AND d.employeeId = :employeeId ORDER BY d.employeeId ASC",
                            PayrollDetail.class)
                    .setParameter("runId", payrollRunId)
                    .setParameter("employeeId", employeeId)
                    .getResultList();
        }
    }
}
