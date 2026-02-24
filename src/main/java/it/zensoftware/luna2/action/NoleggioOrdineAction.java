package it.zensoftware.luna2.action;

import com.opensymphony.xwork2.ActionSupport;
import it.zensoftware.luna2.dao.*;
import it.zensoftware.luna2.model.*;
import it.zensoftware.luna2.service.NoleggioLeadService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

/**
 * NoleggioOrdineAction - Vehicle order and care call management (Phase 3 and beyond)
 */
public class NoleggioOrdineAction extends ActionSupport {
    
    private static final Logger logger = LogManager.getLogger(NoleggioOrdineAction.class);
    private static final long serialVersionUID = 1L;

    private NoleggioOrdineDAO ordineDAO;
    private NoleggioLeadDAO leadDAO;
    private NoleggioLeadService leadService;
    
    private NoleggioOrdine ordine;
    private List<NoleggioOrdine> ordini;
    private NoleggioLead lead;
    
    private Long id;
    private Long leadId;
    private String numeroOrdine;
    private String statusFiltro;
    private String note;
    private Map<String, Object> jsonResponse = new HashMap<>();
    
    public NoleggioOrdineAction() {
        this.ordineDAO = new NoleggioOrdineDAO();
        this.leadDAO = new NoleggioLeadDAO();
        this.leadService = new NoleggioLeadService();
    }
    
    public String list() {
        try {
            if (leadId != null) {
                ordine = ordineDAO.findByLead(leadId);
                lead = leadDAO.findById(leadId);
            } else {
                ordini = ordineDAO.findActive();
            }
            
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error listing ordini", e);
            addActionError("Errore nel caricamento degli ordini");
            return ERROR;
        }
    }
    
    public String create() {
        try {
            if (leadId != null) {
                lead = leadDAO.findById(leadId);
            }
            ordine = new NoleggioOrdine();
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error creating ordine", e);
            return ERROR;
        }
    }
    
    public String save() {
        try {
            if (leadId == null) {
                addActionError("Lead non trovato");
                return ERROR;
            }
            
            User currentUser = getCurrentUser();
            
            if (ordine.getId() == null) {
                ordine = leadService.createOrdine(leadId, ordine, 
                                                  currentUser != null ? currentUser.getId() : 1L);
                addActionMessage("Ordine creato: " + ordine.getNumeroOrdine());
            } else {
                ordineDAO.update(ordine);
                addActionMessage("Ordine aggiornato");
            }
            
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error saving ordine", e);
            addActionError("Errore nel salvataggio: " + e.getMessage());
            return INPUT;
        }
    }
    
    public String updateStatus() {
        try {
            if (id == null) {
                jsonResponse.put("success", false);
                return SUCCESS;
            }
            
            NoleggioOrdine.Status newStatus = NoleggioOrdine.Status.valueOf(statusFiltro);
            ordine = leadService.updateOrdineStatus(id, newStatus, 
                                                   getCurrentUser() != null ? getCurrentUser().getId() : 1L);
            
            jsonResponse.put("success", true);
            jsonResponse.put("status", ordine.getStatus().toString());
            
            if (newStatus == NoleggioOrdine.Status.CONSEGNATO) {
                lead = ordine.getLead();
                jsonResponse.put("fase", lead.getFase().toString());
            }
            
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error updating status", e);
            jsonResponse.put("success", false);
            jsonResponse.put("error", e.getMessage());
            return SUCCESS;
        }
    }
    
    public String registerCareCall() {
        try {
            if (id == null) {
                jsonResponse.put("success", false);
                return SUCCESS;
            }
            
            ordine = leadService.registerCareCall(id, note, 
                                                 getCurrentUser() != null ? getCurrentUser().getId() : 1L);
            
            jsonResponse.put("success", true);
            jsonResponse.put("prossimoCareCall", ordine.getProssimoCareCallPrevisto());
            
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error registering care call", e);
            jsonResponse.put("success", false);
            jsonResponse.put("error", e.getMessage());
            return SUCCESS;
        }
    }
    
    public String requireingCareCall() {
        try {
            List<NoleggioOrdine> requiring = ordineDAO.findRequiringCareCall();
            jsonResponse.put("count", requiring.size());
            jsonResponse.put("ordini", requiring);
            
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error getting requiring care call", e);
            return ERROR;
        }
    }
    
    public String criticallyDelayed() {
        try {
            List<NoleggioOrdine> delayed = ordineDAO.findCriticallyDelayed(90);
            jsonResponse.put("count", delayed.size());
            jsonResponse.put("ordini", delayed);
            
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error getting critically delayed", e);
            return ERROR;
        }
    }
    
    public String delete() {
        try {
            if (id == null) {
                addActionError("ID non valido");
                return ERROR;
            }
            
            NoleggioOrdine o = ordineDAO.findById(id);
            if (o != null) {
                ordineDAO.delete(o);
                addActionMessage("Ordine eliminato");
            }
            
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error deleting ordine", e);
            addActionError("Errore nell'eliminazione");
            return ERROR;
        }
    }
    
    // ============== GETTERS/SETTERS ==============
    
    public NoleggioOrdine getOrdine() { return ordine; }
    public void setOrdine(NoleggioOrdine ordine) { this.ordine = ordine; }
    
    public List<NoleggioOrdine> getOrdini() { return ordini; }
    public void setOrdini(List<NoleggioOrdine> ordini) { this.ordini = ordini; }
    
    public NoleggioLead getLead() { return lead; }
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getLeadId() { return leadId; }
    public void setLeadId(Long leadId) { this.leadId = leadId; }
    
    public String getNumeroOrdine() { return numeroOrdine; }
    public void setNumeroOrdine(String numeroOrdine) { this.numeroOrdine = numeroOrdine; }
    
    public String getStatusFiltro() { return statusFiltro; }
    public void setStatusFiltro(String statusFiltro) { this.statusFiltro = statusFiltro; }
    
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    
    public Map<String, Object> getJsonResponse() { return jsonResponse; }
    
    private User getCurrentUser() {
        Map<String, Object> session = com.opensymphony.xwork2.ActionContext.getContext().getSession();
        return (User) session.get("currentUser");
    }
}
