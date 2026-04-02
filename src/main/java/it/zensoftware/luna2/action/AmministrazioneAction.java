package it.zensoftware.luna2.action;

import com.opensymphony.xwork2.ActionSupport;
import it.zensoftware.luna2.dao.AdminOperationDAO;
import it.zensoftware.luna2.model.AdminOperation;
import it.zensoftware.luna2.model.User;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class AmministrazioneAction extends ActionSupport {

    private AdminOperationDAO operationDAO = new AdminOperationDAO();

    private AdminOperation operation;
    private List<AdminOperation> operations = new ArrayList<>();
    private Map<AdminOperation.Area, List<AdminOperation>> operationsByArea = new LinkedHashMap<>();
    private String dueDateInput;
    private Long id;

    private Long totalOpen = 0L;
    private Long totalInProgress = 0L;
    private Long totalOverdue = 0L;
    private Map<AdminOperation.Area, BigDecimal> openAmountByArea = new LinkedHashMap<>();

    public String dashboard() {
        seedIfEmpty();
        operations = operationDAO.findAllOrdered();
        for (AdminOperation.Area area : AdminOperation.Area.values()) {
            operationsByArea.put(area, operationDAO.findByArea(area));
            openAmountByArea.put(area, operationDAO.sumOpenAmountByArea(area));
        }
        totalOpen = operationDAO.countByStatus(AdminOperation.Status.READY) + operationDAO.countByStatus(AdminOperation.Status.DRAFT);
        totalInProgress = operationDAO.countByStatus(AdminOperation.Status.IN_PROGRESS);
        totalOverdue = operationDAO.countOverdue(new Date());

        if (operation == null) {
            operation = new AdminOperation();
            operation.setArea(AdminOperation.Area.ADEMPIMENTI_FISCALI);
            operation.setStatus(AdminOperation.Status.DRAFT);
        }
        dueDateInput = formatDate(operation.getDueDate());
        return SUCCESS;
    }

    public String saveOperation() {
        try {
            if (operation == null || operation.getTitle() == null || operation.getTitle().trim().isEmpty()) {
                addActionError("Titolo operazione obbligatorio");
                return INPUT;
            }
            if (operation.getArea() == null) {
                addActionError("Area operativa obbligatoria");
                return INPUT;
            }
            if (operation.getOperationType() == null || operation.getOperationType().trim().isEmpty()) {
                addActionError("Tipo operazione obbligatorio");
                return INPUT;
            }
            if (operation.getStatus() == null) {
                operation.setStatus(AdminOperation.Status.DRAFT);
            }

            Date due = parseDate(dueDateInput);
            operation.setDueDate(due);
            if (operation.getOwnerUserId() == null) {
                operation.setOwnerUserId(getCurrentUserId());
            }

            if (operation.getId() == null) {
                operationDAO.save(operation);
            } else {
                operationDAO.update(operation);
            }
            addActionMessage("Operazione amministrativa salvata");
            return SUCCESS;
        } catch (Exception ex) {
            addActionError("Errore salvataggio operazione");
            return ERROR;
        }
    }

    public String completeOperation() {
        try {
            AdminOperation row = operationDAO.findById(id);
            if (row == null) {
                addActionError("Operazione non trovata");
                return INPUT;
            }
            row.setStatus(AdminOperation.Status.COMPLETED);
            row.setCompletedAt(new Date());
            operationDAO.update(row);
            addActionMessage("Operazione completata");
            return SUCCESS;
        } catch (Exception ex) {
            addActionError("Errore completamento operazione");
            return ERROR;
        }
    }

    private void seedIfEmpty() {
        if (!operationDAO.findAllOrdered().isEmpty()) {
            return;
        }
        createSeed(AdminOperation.Area.PAYROLL_NORMATIVO, "Allineamento contributi INPS payroll", "PAYROLL_STATUTORY", 15);
        createSeed(AdminOperation.Area.ADEMPIMENTI_FISCALI, "Predisposizione LIPE trimestre", "LIPE", 20);
        createSeed(AdminOperation.Area.TESORERIA_RICONCILIAZIONE, "Riconciliazione movimenti banca", "BANK_RECON", 10);
        createSeed(AdminOperation.Area.CICLO_PASSIVO, "Matching fatture passive e ordini", "AP_3WAY_MATCH", 7);
        createSeed(AdminOperation.Area.CHIUSURE_CONTABILI, "Checklist chiusura mensile", "MONTH_CLOSE", 5);
        createSeed(AdminOperation.Area.COMPLIANCE_AUDIT, "Verifica conservazione sostitutiva", "DOC_COMPLIANCE", 30);
        createSeed(AdminOperation.Area.MONITORING_KPI, "Aggiornamento KPI amministrativi", "KPI_REFRESH", 3);
    }

    private void createSeed(AdminOperation.Area area, String title, String type, int daysAhead) {
        AdminOperation op = new AdminOperation();
        op.setArea(area);
        op.setTitle(title);
        op.setOperationType(type);
        op.setStatus(AdminOperation.Status.READY);
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_MONTH, daysAhead);
        op.setDueDate(c.getTime());
        op.setAmountDue(BigDecimal.ZERO);
        op.setOwnerUserId(getCurrentUserId());
        operationDAO.save(op);
    }

    protected Long getCurrentUserId() {
        User user = getCurrentUser();
        if (user == null || user.getId() == null) {
            return 1L;
        }
        return user.getId();
    }

    protected User getCurrentUser() {
        Map<String, Object> session = com.opensymphony.xwork2.ActionContext.getContext().getSession();
        return (User) session.get("currentUser");
    }

    private Date parseDate(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return new SimpleDateFormat("yyyy-MM-dd").parse(value.trim());
        } catch (ParseException ex) {
            return null;
        }
    }

    private String formatDate(Date d) {
        if (d == null) {
            return null;
        }
        return new SimpleDateFormat("yyyy-MM-dd").format(d);
    }

    public AdminOperation getOperation() { return operation; }
    public void setOperation(AdminOperation operation) { this.operation = operation; }
    public List<AdminOperation> getOperations() { return operations; }
    public Map<AdminOperation.Area, List<AdminOperation>> getOperationsByArea() { return operationsByArea; }
    public String getDueDateInput() { return dueDateInput; }
    public void setDueDateInput(String dueDateInput) { this.dueDateInput = dueDateInput; }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTotalOpen() { return totalOpen; }
    public Long getTotalInProgress() { return totalInProgress; }
    public Long getTotalOverdue() { return totalOverdue; }
    public Map<AdminOperation.Area, BigDecimal> getOpenAmountByArea() { return openAmountByArea; }
    public AdminOperation.Area[] getAreas() { return AdminOperation.Area.values(); }
    public AdminOperation.Status[] getStatuses() { return AdminOperation.Status.values(); }
    public void setOperationDAO(AdminOperationDAO operationDAO) { this.operationDAO = operationDAO; }
}
