package it.zensoftware.luna2.action;

import com.opensymphony.xwork2.ActionSupport;
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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class PayrollAction extends ActionSupport {

    private static final BigDecimal DEFAULT_HOURLY_RATE = BigDecimal.valueOf(15);
    private static final BigDecimal DEFAULT_OVERTIME_MULTIPLIER = BigDecimal.valueOf(1.30);
    private static final BigDecimal DEFAULT_TAX_RATE = BigDecimal.valueOf(0.20);

    private PayrollRunDAO payrollRunDAO = new PayrollRunDAO();
    private PayrollDetailDAO payrollDetailDAO = new PayrollDetailDAO();
    private PayrollEmployeeConfigDAO payrollEmployeeConfigDAO = new PayrollEmployeeConfigDAO();
    private TimeRecordDAO timeRecordDAO = new TimeRecordDAO();
    private UserDAO userDAO = new UserDAO();

    private List<PayrollRun> runs = new ArrayList<>();
    private List<PayrollDetail> details = new ArrayList<>();
    private List<User> employees = new ArrayList<>();

    private Long configEmployeeId;
    private BigDecimal configHourlyRate;
    private BigDecimal configOvertimeMultiplier;
    private BigDecimal configTaxRate;
    private BigDecimal configSocialSecurityRate;
    private BigDecimal configInsuranceRate;
    private BigDecimal configAllowance;
    private BigDecimal configDeduction;

    private Long exportRunId;
    private Long selectedEmployeeId;
    private InputStream inputStream;
    private String contentDisposition;

    public String list() {
        try {
            Long companyId = getCurrentCompanyId();
            runs = payrollRunDAO.findByCompany(companyId);
            employees = userDAO.findActivePayrollUsers();
            if (!runs.isEmpty()) {
                details = payrollDetailDAO.findByRun(runs.get(0).getId());
            }
            return SUCCESS;
        } catch (Exception ex) {
            addActionError("Errore caricamento payroll");
            return ERROR;
        }
    }

    public String generateCurrentMonth() {
        try {
            Long companyId = getCurrentCompanyId();

            Calendar now = Calendar.getInstance();
            Integer month = now.get(Calendar.MONTH) + 1;
            Integer year = now.get(Calendar.YEAR);

            PayrollRun run = payrollRunDAO.findByPeriod(companyId, month, year);
            if (run == null) {
                run = new PayrollRun();
                run.setCompanyId(companyId);
                run.setMonth(month);
                run.setYear(year);
                run.setStatus(PayrollRun.RunStatus.DRAFT);
                run.setRevision(1);
                payrollRunDAO.save(run);
            } else if (run.getStatus() == PayrollRun.RunStatus.CONFIRMED) {
                addActionError("Payroll già confermato per il periodo corrente");
                return INPUT;
            } else {
                run.setRevision((run.getRevision() == null ? 0 : run.getRevision()) + 1);
            }

            Calendar from = Calendar.getInstance();
            from.set(Calendar.YEAR, year);
            from.set(Calendar.MONTH, month - 1);
            from.set(Calendar.DAY_OF_MONTH, 1);
            setStartOfDay(from);

            Calendar to = Calendar.getInstance();
            to.set(Calendar.YEAR, year);
            to.set(Calendar.MONTH, month - 1);
            to.set(Calendar.DAY_OF_MONTH, to.getActualMaximum(Calendar.DAY_OF_MONTH));
            setEndOfDay(to);

            BigDecimal totalGross = BigDecimal.ZERO;
            BigDecimal totalNet = BigDecimal.ZERO;

            List<User> payrollUsers = userDAO.findActivePayrollUsers();
            for (User user : payrollUsers) {
                List<TimeRecord> rows = timeRecordDAO.findByEmployee(companyId, user.getId(), from.getTime(), to.getTime());
                BigDecimal totalHours = sumWorkedHours(rows);
                BigDecimal overtimeHours = sumOvertimeHours(rows);
                BigDecimal regularHours = totalHours.subtract(overtimeHours);
                if (regularHours.compareTo(BigDecimal.ZERO) < 0) {
                    regularHours = BigDecimal.ZERO;
                }

                PayrollEmployeeConfig cfg = getOrCreateConfig(companyId, user.getId());
                BigDecimal overtimeRate = cfg.getHourlyRate().multiply(cfg.getOvertimeRateMultiplier());
                BigDecimal gross = regularHours.multiply(cfg.getHourlyRate())
                        .add(overtimeHours.multiply(overtimeRate))
                        .add(cfg.getFixedAllowance())
                        .setScale(2, RoundingMode.HALF_UP);
                BigDecimal tax = gross.multiply(cfg.getTaxRate()).setScale(2, RoundingMode.HALF_UP);
                BigDecimal socialSecurity = gross.multiply(cfg.getSocialSecurityRate()).setScale(2, RoundingMode.HALF_UP);
                BigDecimal insurance = gross.multiply(cfg.getInsuranceRate()).setScale(2, RoundingMode.HALF_UP);
                BigDecimal net = gross.subtract(tax)
                    .subtract(socialSecurity)
                    .subtract(insurance)
                    .subtract(cfg.getFixedDeduction())
                    .setScale(2, RoundingMode.HALF_UP);

                PayrollDetail detail = payrollDetailDAO.findByRunAndEmployee(run.getId(), user.getId());
                if (detail == null) {
                    detail = new PayrollDetail();
                    detail.setPayrollRunId(run.getId());
                    detail.setEmployeeId(user.getId());
                }
                detail.setEmployeeName(user.getNomeCompleto());
                detail.setHoursWorked(totalHours);
                detail.setHoursOvertime(overtimeHours);
                detail.setGrossAmount(gross);
                detail.setTaxAmount(tax);
                detail.setSocialSecurityAmount(socialSecurity);
                detail.setInsuranceAmount(insurance);
                detail.setAllowanceAmount(cfg.getFixedAllowance());
                detail.setDeductionAmount(cfg.getFixedDeduction());
                detail.setNetAmount(net);
                if (detail.getId() == null) {
                    payrollDetailDAO.save(detail);
                } else {
                    payrollDetailDAO.update(detail);
                }

                totalGross = totalGross.add(gross);
                totalNet = totalNet.add(net);
            }

            run.setTotalGross(totalGross.setScale(2, RoundingMode.HALF_UP));
            run.setTotalNet(totalNet.setScale(2, RoundingMode.HALF_UP));
            run.setStatus(PayrollRun.RunStatus.CALCULATED);
            payrollRunDAO.update(run);

            addActionMessage("Payroll mensile calcolato per " + payrollUsers.size() + " dipendenti");
            return SUCCESS;
        } catch (Exception ex) {
            addActionError("Errore generazione payroll");
            return ERROR;
        }
    }

    public String confirmCurrentMonth() {
        try {
            Long companyId = getCurrentCompanyId();
            Calendar now = Calendar.getInstance();
            Integer month = now.get(Calendar.MONTH) + 1;
            Integer year = now.get(Calendar.YEAR);

            PayrollRun run = payrollRunDAO.findByPeriod(companyId, month, year);
            if (run == null) {
                addActionError("Nessun payroll da confermare per il periodo corrente");
                return INPUT;
            }
            if (run.getStatus() == PayrollRun.RunStatus.CONFIRMED) {
                addActionMessage("Payroll già confermato");
                return SUCCESS;
            }
            run.setStatus(PayrollRun.RunStatus.CONFIRMED);
            run.setConfirmedAt(new Date());
            payrollRunDAO.update(run);
            addActionMessage("Payroll confermato: il periodo è ora bloccato");
            return SUCCESS;
        } catch (Exception ex) {
            addActionError("Errore conferma payroll");
            return ERROR;
        }
    }

    public String saveEmployeeConfig() {
        try {
            if (configEmployeeId == null) {
                addActionError("Dipendente obbligatorio");
                return INPUT;
            }
            Long companyId = getCurrentCompanyId();
            PayrollEmployeeConfig cfg = payrollEmployeeConfigDAO.findByCompanyAndEmployee(companyId, configEmployeeId);
            if (cfg == null) {
                cfg = new PayrollEmployeeConfig();
                cfg.setCompanyId(companyId);
                cfg.setEmployeeId(configEmployeeId);
            }

            cfg.setHourlyRate(defaultIfNull(configHourlyRate, DEFAULT_HOURLY_RATE));
            cfg.setOvertimeRateMultiplier(defaultIfNull(configOvertimeMultiplier, DEFAULT_OVERTIME_MULTIPLIER));
            cfg.setTaxRate(defaultIfNull(configTaxRate, DEFAULT_TAX_RATE));
            cfg.setSocialSecurityRate(defaultIfNull(configSocialSecurityRate, BigDecimal.valueOf(0.0919)));
            cfg.setInsuranceRate(defaultIfNull(configInsuranceRate, BigDecimal.valueOf(0.0100)));
            cfg.setFixedAllowance(defaultIfNull(configAllowance, BigDecimal.ZERO));
            cfg.setFixedDeduction(defaultIfNull(configDeduction, BigDecimal.ZERO));

            if (cfg.getId() == null) {
                payrollEmployeeConfigDAO.save(cfg);
            } else {
                payrollEmployeeConfigDAO.update(cfg);
            }

            addActionMessage("Parametri retributivi salvati");
            return SUCCESS;
        } catch (Exception ex) {
            addActionError("Errore salvataggio parametri retributivi");
            return ERROR;
        }
    }

    public String exportCurrentMonthCsv() {
        try {
            Long companyId = getCurrentCompanyId();
            PayrollRun run = exportRunId != null ? payrollRunDAO.findById(exportRunId) : payrollRunDAO.findLatestByCompany(companyId);
            if (run == null) {
                addActionError("Nessuna elaborazione payroll disponibile");
                return INPUT;
            }

            List<PayrollDetail> exportRows = payrollDetailDAO.findByRunAndEmployeeFilter(run.getId(), selectedEmployeeId);
            StringBuilder csv = new StringBuilder();
            csv.append("run_id;revisione;periodo;dipendente_id;dipendente;ore;straordinario;lordo;tasse;indennita;trattenute;netto\n");
            String period = String.format("%02d/%04d", run.getMonth(), run.getYear());
            for (PayrollDetail row : exportRows) {
                csv.append(run.getId()).append(';')
                        .append(run.getRevision()).append(';')
                        .append(period).append(';')
                        .append(row.getEmployeeId()).append(';')
                        .append(sanitizeCsv(row.getEmployeeName())).append(';')
                        .append(value(row.getHoursWorked())).append(';')
                        .append(value(row.getHoursOvertime())).append(';')
                        .append(value(row.getGrossAmount())).append(';')
                        .append(value(row.getTaxAmount())).append(';')
                        .append(value(row.getAllowanceAmount())).append(';')
                        .append(value(row.getDeductionAmount())).append(';')
                        .append(value(row.getNetAmount()))
                        .append('\n');
            }

            String scope = selectedEmployeeId == null ? "company" : "employee_" + selectedEmployeeId;
            contentDisposition = "attachment;filename=payroll_" + run.getYear() + "_" + run.getMonth() + "_" + scope + ".csv";
            inputStream = new ByteArrayInputStream(csv.toString().getBytes(StandardCharsets.UTF_8));
            return "csv";
        } catch (Exception ex) {
            addActionError("Errore export payroll");
            return ERROR;
        }
    }

    private BigDecimal sumWorkedHours(List<TimeRecord> rows) {
        BigDecimal total = BigDecimal.ZERO;
        for (TimeRecord row : rows) {
            if (row.getHoursWorked() != null) {
                total = total.add(row.getHoursWorked());
            }
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal sumOvertimeHours(List<TimeRecord> rows) {
        BigDecimal total = BigDecimal.ZERO;
        for (TimeRecord row : rows) {
            if (row.getHoursOvertime() != null) {
                total = total.add(row.getHoursOvertime());
            }
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    private PayrollEmployeeConfig getOrCreateConfig(Long companyId, Long employeeId) {
        PayrollEmployeeConfig cfg = payrollEmployeeConfigDAO.findByCompanyAndEmployee(companyId, employeeId);
        if (cfg != null) {
            return cfg;
        }
        cfg = new PayrollEmployeeConfig();
        cfg.setCompanyId(companyId);
        cfg.setEmployeeId(employeeId);
        cfg.setHourlyRate(DEFAULT_HOURLY_RATE);
        cfg.setOvertimeRateMultiplier(DEFAULT_OVERTIME_MULTIPLIER);
        cfg.setTaxRate(DEFAULT_TAX_RATE);
        cfg.setSocialSecurityRate(BigDecimal.valueOf(0.0919));
        cfg.setInsuranceRate(BigDecimal.valueOf(0.0100));
        cfg.setFixedAllowance(BigDecimal.ZERO);
        cfg.setFixedDeduction(BigDecimal.ZERO);
        payrollEmployeeConfigDAO.save(cfg);
        return cfg;
    }

    private BigDecimal defaultIfNull(BigDecimal val, BigDecimal fallback) {
        return val == null ? fallback : val;
    }

    private String sanitizeCsv(String val) {
        if (val == null) {
            return "";
        }
        return val.replace(';', ',').replace('\n', ' ').replace('\r', ' ');
    }

    private String value(BigDecimal val) {
        return val == null ? "0.00" : val.setScale(2, RoundingMode.HALF_UP).toPlainString();
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

    public List<PayrollRun> getRuns() { return runs; }
    public List<PayrollDetail> getDetails() { return details; }
    public List<User> getEmployees() { return employees; }
    public InputStream getInputStream() { return inputStream; }
    public String getContentDisposition() { return contentDisposition; }

    public Long getConfigEmployeeId() { return configEmployeeId; }
    public void setConfigEmployeeId(Long configEmployeeId) { this.configEmployeeId = configEmployeeId; }
    public BigDecimal getConfigHourlyRate() { return configHourlyRate; }
    public void setConfigHourlyRate(BigDecimal configHourlyRate) { this.configHourlyRate = configHourlyRate; }
    public BigDecimal getConfigOvertimeMultiplier() { return configOvertimeMultiplier; }
    public void setConfigOvertimeMultiplier(BigDecimal configOvertimeMultiplier) { this.configOvertimeMultiplier = configOvertimeMultiplier; }
    public BigDecimal getConfigTaxRate() { return configTaxRate; }
    public void setConfigTaxRate(BigDecimal configTaxRate) { this.configTaxRate = configTaxRate; }
    public BigDecimal getConfigSocialSecurityRate() { return configSocialSecurityRate; }
    public void setConfigSocialSecurityRate(BigDecimal configSocialSecurityRate) { this.configSocialSecurityRate = configSocialSecurityRate; }
    public BigDecimal getConfigInsuranceRate() { return configInsuranceRate; }
    public void setConfigInsuranceRate(BigDecimal configInsuranceRate) { this.configInsuranceRate = configInsuranceRate; }
    public BigDecimal getConfigAllowance() { return configAllowance; }
    public void setConfigAllowance(BigDecimal configAllowance) { this.configAllowance = configAllowance; }
    public BigDecimal getConfigDeduction() { return configDeduction; }
    public void setConfigDeduction(BigDecimal configDeduction) { this.configDeduction = configDeduction; }
    public Long getExportRunId() { return exportRunId; }
    public void setExportRunId(Long exportRunId) { this.exportRunId = exportRunId; }
    public Long getSelectedEmployeeId() { return selectedEmployeeId; }
    public void setSelectedEmployeeId(Long selectedEmployeeId) { this.selectedEmployeeId = selectedEmployeeId; }

    public void setPayrollRunDAO(PayrollRunDAO payrollRunDAO) { this.payrollRunDAO = payrollRunDAO; }
    public void setPayrollDetailDAO(PayrollDetailDAO payrollDetailDAO) { this.payrollDetailDAO = payrollDetailDAO; }
    public void setPayrollEmployeeConfigDAO(PayrollEmployeeConfigDAO payrollEmployeeConfigDAO) { this.payrollEmployeeConfigDAO = payrollEmployeeConfigDAO; }
    public void setTimeRecordDAO(TimeRecordDAO timeRecordDAO) { this.timeRecordDAO = timeRecordDAO; }
    public void setUserDAO(UserDAO userDAO) { this.userDAO = userDAO; }
}
