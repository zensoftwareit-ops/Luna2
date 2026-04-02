package it.zensoftware.luna2.action;

import it.zensoftware.luna2.dao.PayrollDetailDAO;
import it.zensoftware.luna2.dao.PayrollEmployeeConfigDAO;
import it.zensoftware.luna2.dao.PayrollRunDAO;
import it.zensoftware.luna2.dao.TimeRecordDAO;
import it.zensoftware.luna2.dao.UserDAO;
import it.zensoftware.luna2.model.PayrollDetail;
import it.zensoftware.luna2.model.PayrollEmployeeConfig;
import it.zensoftware.luna2.model.PayrollRun;
import it.zensoftware.luna2.model.TimeRecord;
import it.zensoftware.luna2.model.User;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class PayrollActionTest {

    private static class FakePayrollRunDAO extends PayrollRunDAO {
        private final Map<String, PayrollRun> byPeriod = new HashMap<>();
        private final Map<Long, PayrollRun> byId = new HashMap<>();
        private long seq = 1;

        private String key(Long companyId, Integer month, Integer year) {
            return companyId + ":" + month + ":" + year;
        }

        @Override
        public PayrollRun save(PayrollRun entity) {
            if (entity.getId() == null) {
                entity.setId(seq++);
            }
            byPeriod.put(key(entity.getCompanyId(), entity.getMonth(), entity.getYear()), entity);
            byId.put(entity.getId(), entity);
            return entity;
        }

        @Override
        public void update(PayrollRun entity) {
            byPeriod.put(key(entity.getCompanyId(), entity.getMonth(), entity.getYear()), entity);
            byId.put(entity.getId(), entity);
        }

        @Override
        public PayrollRun findByPeriod(Long companyId, Integer month, Integer year) {
            return byPeriod.get(key(companyId, month, year));
        }

        @Override
        public List<PayrollRun> findByCompany(Long companyId) {
            List<PayrollRun> out = new ArrayList<>();
            for (PayrollRun run : byId.values()) {
                if (companyId.equals(run.getCompanyId())) {
                    out.add(run);
                }
            }
            out.sort((a, b) -> Long.compare(b.getId(), a.getId()));
            return out;
        }

        @Override
        public PayrollRun findLatestByCompany(Long companyId) {
            List<PayrollRun> list = findByCompany(companyId);
            return list.isEmpty() ? null : list.get(0);
        }

        @Override
        public PayrollRun findById(Long id) {
            return byId.get(id);
        }
    }

    private static class FakePayrollDetailDAO extends PayrollDetailDAO {
        private final Map<String, PayrollDetail> byRunEmployee = new HashMap<>();
        private long seq = 1;

        private String key(Long runId, Long employeeId) {
            return runId + ":" + employeeId;
        }

        @Override
        public PayrollDetail findByRunAndEmployee(Long payrollRunId, Long employeeId) {
            return byRunEmployee.get(key(payrollRunId, employeeId));
        }

        @Override
        public PayrollDetail save(PayrollDetail entity) {
            if (entity.getId() == null) {
                entity.setId(seq++);
            }
            byRunEmployee.put(key(entity.getPayrollRunId(), entity.getEmployeeId()), entity);
            return entity;
        }

        @Override
        public void update(PayrollDetail entity) {
            byRunEmployee.put(key(entity.getPayrollRunId(), entity.getEmployeeId()), entity);
        }

        @Override
        public List<PayrollDetail> findByRun(Long payrollRunId) {
            List<PayrollDetail> out = new ArrayList<>();
            for (PayrollDetail detail : byRunEmployee.values()) {
                if (payrollRunId.equals(detail.getPayrollRunId())) {
                    out.add(detail);
                }
            }
            out.sort(Comparator.comparing(PayrollDetail::getEmployeeId));
            return out;
        }

        @Override
        public List<PayrollDetail> findByRunAndEmployeeFilter(Long payrollRunId, Long employeeId) {
            if (employeeId == null) {
                return findByRun(payrollRunId);
            }
            PayrollDetail detail = byRunEmployee.get(key(payrollRunId, employeeId));
            if (detail == null) {
                return Collections.emptyList();
            }
            return Collections.singletonList(detail);
        }
    }

    private static class FakePayrollEmployeeConfigDAO extends PayrollEmployeeConfigDAO {
        private final Map<String, PayrollEmployeeConfig> byCompanyEmployee = new HashMap<>();
        private long seq = 1;

        private String key(Long companyId, Long employeeId) {
            return companyId + ":" + employeeId;
        }

        @Override
        public PayrollEmployeeConfig findByCompanyAndEmployee(Long companyId, Long employeeId) {
            return byCompanyEmployee.get(key(companyId, employeeId));
        }

        @Override
        public PayrollEmployeeConfig save(PayrollEmployeeConfig entity) {
            if (entity.getId() == null) {
                entity.setId(seq++);
            }
            byCompanyEmployee.put(key(entity.getCompanyId(), entity.getEmployeeId()), entity);
            return entity;
        }

        @Override
        public void update(PayrollEmployeeConfig entity) {
            byCompanyEmployee.put(key(entity.getCompanyId(), entity.getEmployeeId()), entity);
        }
    }

    private static class FakeTimeRecordDAO extends TimeRecordDAO {
        private final Map<Long, List<TimeRecord>> byEmployee = new HashMap<>();

        public void putRecords(Long employeeId, List<TimeRecord> rows) {
            byEmployee.put(employeeId, rows);
        }

        @Override
        public List<TimeRecord> findByEmployee(Long companyId, Long employeeId, Date from, Date to) {
            return byEmployee.getOrDefault(employeeId, Collections.emptyList());
        }
    }

    private static class FakeUserDAO extends UserDAO {
        private final List<User> users;

        private FakeUserDAO(List<User> users) {
            this.users = users;
        }

        @Override
        public List<User> findActivePayrollUsers() {
            return users;
        }
    }

    private static class TestablePayrollAction extends PayrollAction {
        @Override
        protected Long getCurrentCompanyId() {
            return 1L;
        }

        @Override
        protected Long getCurrentUserId() {
            return 999L;
        }
    }

    @Test
    public void generateCurrentMonthShouldCalculateAllEmployees() {
        TestablePayrollAction action = new TestablePayrollAction();

        FakePayrollRunDAO runDAO = new FakePayrollRunDAO();
        FakePayrollDetailDAO detailDAO = new FakePayrollDetailDAO();
        FakePayrollEmployeeConfigDAO configDAO = new FakePayrollEmployeeConfigDAO();
        FakeTimeRecordDAO timeDAO = new FakeTimeRecordDAO();

        User u1 = user(10L, "Mario", "Rossi");
        User u2 = user(11L, "Luigi", "Bianchi");
        action.setUserDAO(new FakeUserDAO(Arrays.asList(u1, u2)));

        timeDAO.putRecords(10L, Arrays.asList(row(8, 1), row(8, 0)));
        timeDAO.putRecords(11L, Arrays.asList(row(7, 0), row(9, 2)));

        PayrollEmployeeConfig cfg2 = new PayrollEmployeeConfig();
        cfg2.setCompanyId(1L);
        cfg2.setEmployeeId(11L);
        cfg2.setHourlyRate(new BigDecimal("20.00"));
        cfg2.setOvertimeRateMultiplier(new BigDecimal("1.50"));
        cfg2.setTaxRate(new BigDecimal("0.10"));
        cfg2.setFixedAllowance(new BigDecimal("50.00"));
        cfg2.setFixedDeduction(new BigDecimal("10.00"));
        configDAO.save(cfg2);

        action.setPayrollRunDAO(runDAO);
        action.setPayrollDetailDAO(detailDAO);
        action.setPayrollEmployeeConfigDAO(configDAO);
        action.setTimeRecordDAO(timeDAO);

        String result = action.generateCurrentMonth();

        assertEquals("success", result);
        PayrollRun run = runDAO.findLatestByCompany(1L);
        assertNotNull(run);
        assertEquals(PayrollRun.RunStatus.CALCULATED, run.getStatus());
        assertEquals(2, detailDAO.findByRun(run.getId()).size());
        assertTrue(run.getTotalGross().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(run.getTotalNet().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    public void confirmShouldLockRunFromFurtherRecalculation() {
        TestablePayrollAction action = setupSimpleScenario();

        assertEquals("success", action.generateCurrentMonth());
        assertEquals("success", action.confirmCurrentMonth());
        assertEquals("input", action.generateCurrentMonth());
    }

    @Test
    public void exportCsvShouldReturnStreamResult() throws Exception {
        TestablePayrollAction action = setupSimpleScenario();

        assertEquals("success", action.generateCurrentMonth());
        String exportResult = action.exportCurrentMonthCsv();

        assertEquals("csv", exportResult);
        InputStream is = action.getInputStream();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int read;
        while ((read = is.read(buffer)) != -1) {
            bos.write(buffer, 0, read);
        }
        String content = bos.toString(StandardCharsets.UTF_8.name());
        assertTrue(content.contains("run_id;revisione;periodo"));
        assertTrue(content.contains("Mario Rossi"));
    }

    private TestablePayrollAction setupSimpleScenario() {
        TestablePayrollAction action = new TestablePayrollAction();
        FakePayrollRunDAO runDAO = new FakePayrollRunDAO();
        FakePayrollDetailDAO detailDAO = new FakePayrollDetailDAO();
        FakePayrollEmployeeConfigDAO configDAO = new FakePayrollEmployeeConfigDAO();
        FakeTimeRecordDAO timeDAO = new FakeTimeRecordDAO();

        action.setPayrollRunDAO(runDAO);
        action.setPayrollDetailDAO(detailDAO);
        action.setPayrollEmployeeConfigDAO(configDAO);
        action.setTimeRecordDAO(timeDAO);
        action.setUserDAO(new FakeUserDAO(Collections.singletonList(user(10L, "Mario", "Rossi"))));

        timeDAO.putRecords(10L, Collections.singletonList(row(8, 0)));
        return action;
    }

    private static User user(Long id, String nome, String cognome) {
        User u = new User();
        u.setId(id);
        u.setNome(nome);
        u.setCognome(cognome);
        u.setAttivo(true);
        u.setRuolo(User.Ruolo.USER);
        return u;
    }

    private static TimeRecord row(double hours, double overtime) {
        TimeRecord r = new TimeRecord();
        r.setHoursWorked(BigDecimal.valueOf(hours));
        r.setHoursOvertime(BigDecimal.valueOf(overtime));
        return r;
    }
}
