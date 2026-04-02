package it.zensoftware.luna2.action;

import com.opensymphony.xwork2.ActionSupport;
import it.zensoftware.luna2.dao.TimeRecordDAO;
import it.zensoftware.luna2.model.TimeRecord;
import it.zensoftware.luna2.model.User;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

public class PresenzeAction extends ActionSupport {

    private TimeRecordDAO timeRecordDAO = new TimeRecordDAO();

    private List<TimeRecord> records = new ArrayList<>();
    private TimeRecord record;
    private String recordDateInput;
    private String actionType;

    public String list() {
        try {
            Long companyId = getCurrentCompanyId();
            Long employeeId = getCurrentUserId();

            Calendar from = Calendar.getInstance();
            from.set(Calendar.DAY_OF_MONTH, 1);
            setStartOfDay(from);

            Calendar to = Calendar.getInstance();
            to.set(Calendar.DAY_OF_MONTH, to.getActualMaximum(Calendar.DAY_OF_MONTH));
            setEndOfDay(to);

            records = timeRecordDAO.findByEmployee(companyId, employeeId, from.getTime(), to.getTime());
            return SUCCESS;
        } catch (Exception ex) {
            addActionError("Errore caricamento presenze");
            return ERROR;
        }
    }

    public String checkin() {
        return savePunch(true);
    }

    public String checkout() {
        return savePunch(false);
    }

    private String savePunch(boolean checkin) {
        try {
            Long companyId = getCurrentCompanyId();
            Long employeeId = getCurrentUserId();
            Date now = new Date();
            Date day = floorToDate(now);

            TimeRecord row = timeRecordDAO.findByEmployeeAndDate(companyId, employeeId, day);
            if (row == null) {
                row = new TimeRecord();
                row.setCompanyId(companyId);
                row.setEmployeeId(employeeId);
                row.setRecordDate(day);
                row.setAbsenceType(TimeRecord.AbsenceType.NONE);
            }

            if (checkin) {
                row.setCheckInTime(now);
                addActionMessage("Check-in registrato");
            } else {
                row.setCheckOutTime(now);
                computeWorkedHours(row);
                addActionMessage("Check-out registrato");
            }

            if (row.getId() == null) {
                timeRecordDAO.save(row);
            } else {
                timeRecordDAO.update(row);
            }

            return SUCCESS;
        } catch (Exception ex) {
            addActionError("Errore registrazione timbratura");
            return ERROR;
        }
    }

    public String saveAbsence() {
        try {
            if (record == null || record.getAbsenceType() == null || record.getAbsenceType() == TimeRecord.AbsenceType.NONE) {
                addActionError("Tipo assenza obbligatorio");
                return INPUT;
            }
            Date day = parseInputDate(recordDateInput);
            if (day == null) {
                addActionError("Data non valida");
                return INPUT;
            }

            Long companyId = getCurrentCompanyId();
            Long employeeId = getCurrentUserId();
            TimeRecord row = timeRecordDAO.findByEmployeeAndDate(companyId, employeeId, day);
            if (row == null) {
                row = new TimeRecord();
                row.setCompanyId(companyId);
                row.setEmployeeId(employeeId);
                row.setRecordDate(day);
            }
            row.setAbsenceType(record.getAbsenceType());
            row.setNotes(record.getNotes());
            row.setCheckInTime(null);
            row.setCheckOutTime(null);
            row.setHoursWorked(BigDecimal.ZERO);
            row.setHoursOvertime(BigDecimal.ZERO);

            if (row.getId() == null) {
                timeRecordDAO.save(row);
            } else {
                timeRecordDAO.update(row);
            }

            addActionMessage("Assenza registrata");
            return SUCCESS;
        } catch (Exception ex) {
            addActionError("Errore salvataggio assenza");
            return ERROR;
        }
    }

    private void computeWorkedHours(TimeRecord row) {
        if (row.getCheckInTime() == null || row.getCheckOutTime() == null) {
            return;
        }
        long diffMs = row.getCheckOutTime().getTime() - row.getCheckInTime().getTime();
        if (diffMs <= 0) {
            row.setHoursWorked(BigDecimal.ZERO);
            row.setHoursOvertime(BigDecimal.ZERO);
            return;
        }
        BigDecimal hours = BigDecimal.valueOf(diffMs)
                .divide(BigDecimal.valueOf(1000 * 60 * 60), 2, RoundingMode.HALF_UP);
        BigDecimal overtime = hours.subtract(BigDecimal.valueOf(8));
        if (overtime.compareTo(BigDecimal.ZERO) < 0) {
            overtime = BigDecimal.ZERO;
        }
        row.setHoursWorked(hours);
        row.setHoursOvertime(overtime);
    }

    protected Long getCurrentCompanyId() {
        return 1L;
    }

    protected Long getCurrentUserId() {
        User user = getCurrentUser();
        if (user == null || user.getId() == null) {
            throw new IllegalStateException("Utente non autenticato");
        }
        return user.getId();
    }

    protected User getCurrentUser() {
        Map<String, Object> session = com.opensymphony.xwork2.ActionContext.getContext().getSession();
        return (User) session.get("currentUser");
    }

    private Date floorToDate(Date date) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        setStartOfDay(c);
        return c.getTime();
    }

    private Date parseInputDate(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            String[] parts = value.split("-");
            if (parts.length != 3) {
                return null;
            }
            Calendar c = Calendar.getInstance();
            c.set(Calendar.YEAR, Integer.parseInt(parts[0]));
            c.set(Calendar.MONTH, Integer.parseInt(parts[1]) - 1);
            c.set(Calendar.DAY_OF_MONTH, Integer.parseInt(parts[2]));
            setStartOfDay(c);
            return c.getTime();
        } catch (Exception ex) {
            return null;
        }
    }

    private void setStartOfDay(Calendar c) {
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
    }

    private void setEndOfDay(Calendar c) {
        c.set(Calendar.HOUR_OF_DAY, 23);
        c.set(Calendar.MINUTE, 59);
        c.set(Calendar.SECOND, 59);
        c.set(Calendar.MILLISECOND, 999);
    }

    public List<TimeRecord> getRecords() { return records; }
    public TimeRecord getRecord() { return record; }
    public void setRecord(TimeRecord record) { this.record = record; }
    public String getRecordDateInput() { return recordDateInput; }
    public void setRecordDateInput(String recordDateInput) { this.recordDateInput = recordDateInput; }
    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }
    public void setTimeRecordDAO(TimeRecordDAO timeRecordDAO) { this.timeRecordDAO = timeRecordDAO; }
}
