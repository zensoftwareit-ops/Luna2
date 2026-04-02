package it.zensoftware.luna2.dao;

import it.zensoftware.luna2.model.TimeRecord;
import org.hibernate.Session;

import java.util.Date;
import java.util.List;

public class TimeRecordDAO extends GenericDAOImpl<TimeRecord, Long> {

    public TimeRecordDAO() {
        super(TimeRecord.class);
    }

    public List<TimeRecord> findByEmployee(Long companyId, Long employeeId, Date from, Date to) {
        try (Session session = getSession()) {
            return session.createQuery(
                            "FROM TimeRecord t WHERE t.companyId = :companyId AND t.employeeId = :employeeId AND t.recordDate >= :from AND t.recordDate <= :to ORDER BY t.recordDate DESC",
                            TimeRecord.class)
                    .setParameter("companyId", companyId)
                    .setParameter("employeeId", employeeId)
                    .setParameter("from", from)
                    .setParameter("to", to)
                    .getResultList();
        }
    }

    public TimeRecord findByEmployeeAndDate(Long companyId, Long employeeId, Date date) {
        try (Session session = getSession()) {
            return session.createQuery(
                            "FROM TimeRecord t WHERE t.companyId = :companyId AND t.employeeId = :employeeId AND t.recordDate = :recordDate",
                            TimeRecord.class)
                    .setParameter("companyId", companyId)
                    .setParameter("employeeId", employeeId)
                    .setParameter("recordDate", date)
                    .setMaxResults(1)
                    .uniqueResult();
        }
    }
}
