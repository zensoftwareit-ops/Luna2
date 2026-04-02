package it.zensoftware.luna2.action;

import it.zensoftware.luna2.model.ApprovalRequest;
import it.zensoftware.luna2.dao.ApprovalRequestDAO;
import it.zensoftware.luna2.model.User;
import com.opensymphony.xwork2.ActionSupport;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

/**
 * ApprovalWorkflowAction - Gestione workflow di approvazione (ferie, permessi, spese)
 * Sprint 1: Q2 2026
 */
public class ApprovalWorkflowAction extends ActionSupport {

    private static final Logger logger = LogManager.getLogger(ApprovalWorkflowAction.class);

    private ApprovalRequestDAO approvalRequestDAO = new ApprovalRequestDAO();
    private ApprovalRequest approvalRequest;
    private List<ApprovalRequest> approvalRequests;
    private Long requestId;
    private String rejectionReason;
    private Integer pendingCount;

    public String ferieForm() {
        if (approvalRequest == null) {
            approvalRequest = new ApprovalRequest();
        }
        return SUCCESS;
    }

    public String ferieList() {
        try {
            Long companyId = getCurrentCompanyId();
            Long userId = getCurrentUserId();
            approvalRequests = approvalRequestDAO.findByRequester(companyId, userId);
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Errore ferie list", e);
            addActionError("Errore caricamento");
            return ERROR;
        }
    }

    public String ferieCreate() {
        try {
            if (approvalRequest == null) {
                addActionError("Richiesta non valida");
                return INPUT;
            }
            if (approvalRequest.getStartDate() == null || approvalRequest.getEndDate() == null) {
                addActionError("Date obbligatorie");
                return INPUT;
            }
            if (approvalRequest.getStartDate().isAfter(approvalRequest.getEndDate())) {
                addActionError("La data inizio non puo essere successiva alla data fine");
                return INPUT;
            }

            Long companyId = getCurrentCompanyId();
            Long userId = getCurrentUserId();
            approvalRequest.setCompanyId(companyId);
            approvalRequest.setRequesterId(userId);
            approvalRequest.setRequestType("FERIE");
            long days = ChronoUnit.DAYS.between(approvalRequest.getStartDate(), approvalRequest.getEndDate()) + 1;
            approvalRequest.setDaysRequested((int) days);
            approvalRequest.setStatus("SUBMITTED");
            approvalRequest.setSubmittedDate(LocalDateTime.now());
            approvalRequest.setCreatedDate(LocalDateTime.now());
            approvalRequest.setUpdatedDate(LocalDateTime.now());
            approvalRequestDAO.save(approvalRequest);
            addActionMessage("Richiesta creata");
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Errore create", e);
            addActionError("Errore");
            return ERROR;
        }
    }

    public String submit() {
        try {
            if (requestId == null) {
                addActionError("ID richiesta mancante");
                return ERROR;
            }
            approvalRequestDAO.submit(requestId, getCurrentUserId());
            addActionMessage("Richiesta sottoposta per approvazione");
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Errore submit", e);
            addActionError("Errore submit richiesta");
            return ERROR;
        }
    }

    public String pendingApprovals() {
        try {
            Long companyId = getCurrentCompanyId();
            Long managerId = getCurrentUserId();
            approvalRequests = approvalRequestDAO.findPendingApprovals(companyId, managerId);
            pendingCount = approvalRequests.size();
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Errore pending approvals", e);
            addActionError("Errore caricamento approvazioni pendenti");
            return ERROR;
        }
    }

    public String approve() {
        try {
            if (requestId == null) {
                addActionError("ID mancante");
                return ERROR;
            }
            Long managerId = getCurrentUserId();
            approvalRequestDAO.approve(requestId, managerId);
            addActionMessage("Richiesta approvata");
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Errore approve", e);
            addActionError("Errore");
            return ERROR;
        }
    }

    public String reject() {
        try {
            if (requestId == null) {
                addActionError("ID mancante");
                return ERROR;
            }
            if (rejectionReason == null || rejectionReason.trim().isEmpty()) {
                addActionError("Motivo rifiuto obbligatorio");
                return INPUT;
            }
            Long managerId = getCurrentUserId();
            approvalRequestDAO.reject(requestId, managerId, rejectionReason.trim());
            addActionMessage("Richiesta rifiutata");
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Errore reject", e);
            addActionError("Errore rifiuto richiesta");
            return ERROR;
        }
    }

    protected User getCurrentUser() {
        Map<String, Object> session = com.opensymphony.xwork2.ActionContext.getContext().getSession();
        return (User) session.get("currentUser");
    }

    protected Long getCurrentUserId() {
        User user = getCurrentUser();
        if (user == null || user.getId() == null) {
            throw new IllegalStateException("Utente non autenticato");
        }
        return user.getId();
    }

    protected Long getCurrentCompanyId() {
        // TODO: sostituire con companyId reale da sessione tenant.
        return 1L;
    }

    // Getters & Setters
    public ApprovalRequest getApprovalRequest() { return approvalRequest; }
    public void setApprovalRequest(ApprovalRequest approvalRequest) { this.approvalRequest = approvalRequest; }
    public List<ApprovalRequest> getApprovalRequests() { return approvalRequests; }
    public void setApprovalRequests(List<ApprovalRequest> approvalRequests) { this.approvalRequests = approvalRequests; }
    public Long getRequestId() { return requestId; }
    public void setRequestId(Long requestId) { this.requestId = requestId; }
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
    public Integer getPendingCount() { return pendingCount; }
    public void setApprovalRequestDAO(ApprovalRequestDAO approvalRequestDAO) { this.approvalRequestDAO = approvalRequestDAO; }
}
