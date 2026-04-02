package it.zensoftware.luna2.dao;

import it.zensoftware.luna2.model.PayrollEmployeeConfig;
import org.hibernate.Session;

public class PayrollEmployeeConfigDAO extends GenericDAOImpl<PayrollEmployeeConfig, Long> {

    public PayrollEmployeeConfigDAO() {
        super(PayrollEmployeeConfig.class);
    }

    public PayrollEmployeeConfig findByCompanyAndEmployee(Long companyId, Long employeeId) {
        try (Session session = getSession()) {
            return session.createQuery(
                            "FROM PayrollEmployeeConfig c WHERE c.companyId = :companyId AND c.employeeId = :employeeId",
                            PayrollEmployeeConfig.class)
                    .setParameter("companyId", companyId)
                    .setParameter("employeeId", employeeId)
                    .setMaxResults(1)
                    .uniqueResult();
        }
    }
}
