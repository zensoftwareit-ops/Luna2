package it.zensoftware.luna2.action;

import it.zensoftware.luna2.dao.TimeRecordDAO;
import it.zensoftware.luna2.model.TimeRecord;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class PresenzeActionTest {

    private static class FakeTimeRecordDAO extends TimeRecordDAO {
        private final Map<String, TimeRecord> byDay = new HashMap<>();
        private final List<TimeRecord> list = new ArrayList<>();

        private String key(Long companyId, Long employeeId, Date date) {
            return companyId + ":" + employeeId + ":" + date.getTime();
        }

        @Override
        public List<TimeRecord> findByEmployee(Long companyId, Long employeeId, Date from, Date to) {
            return list;
        }

        @Override
        public TimeRecord findByEmployeeAndDate(Long companyId, Long employeeId, Date date) {
            return byDay.get(key(companyId, employeeId, date));
        }

        @Override
        public TimeRecord save(TimeRecord entity) {
            byDay.put(key(entity.getCompanyId(), entity.getEmployeeId(), entity.getRecordDate()), entity);
            list.add(entity);
            return entity;
        }

        @Override
        public void update(TimeRecord entity) {
            byDay.put(key(entity.getCompanyId(), entity.getEmployeeId(), entity.getRecordDate()), entity);
        }
    }

    private static class TestablePresenzeAction extends PresenzeAction {
        @Override
        protected Long getCurrentCompanyId() {
            return 1L;
        }

        @Override
        protected Long getCurrentUserId() {
            return 10L;
        }
    }

    @Test
    public void listShouldReturnSuccess() {
        TestablePresenzeAction action = new TestablePresenzeAction();
        FakeTimeRecordDAO dao = new FakeTimeRecordDAO();
        action.setTimeRecordDAO(dao);

        String result = action.list();

        assertEquals("success", result);
        assertNotNull(action.getRecords());
    }

    @Test
    public void saveAbsenceShouldCreateRecord() {
        TestablePresenzeAction action = new TestablePresenzeAction();
        FakeTimeRecordDAO dao = new FakeTimeRecordDAO();
        action.setTimeRecordDAO(dao);

        TimeRecord r = new TimeRecord();
        r.setAbsenceType(TimeRecord.AbsenceType.FERIE);
        r.setNotes("Test ferie");
        action.setRecord(r);
        action.setRecordDateInput("2026-04-10");

        String result = action.saveAbsence();

        assertEquals("success", result);
    }

    @Test
    public void saveAbsenceWithInvalidDateShouldReturnInput() {
        TestablePresenzeAction action = new TestablePresenzeAction();
        action.setTimeRecordDAO(new FakeTimeRecordDAO());

        TimeRecord r = new TimeRecord();
        r.setAbsenceType(TimeRecord.AbsenceType.PERMESSO);
        action.setRecord(r);
        action.setRecordDateInput("not-a-date");

        String result = action.saveAbsence();

        assertEquals("input", result);
    }
}
