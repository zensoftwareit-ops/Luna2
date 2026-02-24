package it.zensoftware.luna2.action;

import com.opensymphony.xwork2.ActionSupport;
import it.zensoftware.luna2.dao.*;
import it.zensoftware.luna2.model.*;
import it.zensoftware.luna2.service.NoleggioLeadService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

/**
 * NoleggioValutazioneAction - Financial evaluation management in istruttoria phase
 */
public class NoleggioValutazioneAction extends ActionSupport {
    
    private static final Logger logger = LogManager.getLogger(NoleggioValutazioneAction.class);
    private static final long serialVersionUID = 1L;

    private NoleggioValutazioneDAO valutazioneDAO;
    private NoleggioLeadDAO leadDAO;
    private NoleggioLeadService leadService;
    
    private NoleggioValutazione valutazione;
    private List<NoleggioValutazione> valuazioni;
    private NoleggioLead lead;
    
    private Long id;
    private Long leadId;
    private String statusFiltro;
    private Map<String, Object> jsonResponse = new HashMap<>();
    
    public NoleggioValutazioneAction() {
        this.valutazioneDAO = new NoleggioValutazioneDAO();
        this.leadDAO = new NoleggioLeadDAO();
        this.leadService = new NoleggioLeadService();
    }
    
    public String list() {
        try {
            if (leadId != null) {
                valutazione = valutazioneDAO.findByLead(leadId);
                lead = leadDAO.findById(leadId);
            } else {
                valuazioni = valutazioneDAO.findAll();
            }
            
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error listing valuazioni", e);
            addActionError("Errore nel caricamento delle valutazioni");
            return ERROR;
        }
    }
    
    public String create() {
        try {
            if (leadId != null) {
                lead = leadDAO.findById(leadId);
            }
            valutazione = new NoleggioValutazione();
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error creating valutazione", e);
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
            
            if (valutazione.getId() == null) {
                valutazione = leadService.createValutazione(leadId, valutazione, 
                                                            currentUser != null ? currentUser.getId() : 1L);
                addActionMessage("Valutazione creata");
            } else {
                valutazioneDAO.update(valutazione);
                addActionMessage("Valutazione aggiornata");
            }
            
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error saving valutazione", e);
            addActionError("Errore nel salvataggio: " + e.getMessage());
            return INPUT;
        }
    }
    
    public String approve() {
        try {
            if (id == null) {
                jsonResponse.put("success", false);
                return SUCCESS;
            }
            
            lead = leadService.finalizeValutazione(id, true, 
                                                  getCurrentUser() != null ? getCurrentUser().getId() : 1L);
            
            jsonResponse.put("success", true);
            jsonResponse.put("fase", lead.getFase().toString());
            jsonResponse.put("message", "Valutazione approvata - Lead in ORDINE");
            
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error approving valutazione", e);
            jsonResponse.put("success", false);
            jsonResponse.put("error", e.getMessage());
            return SUCCESS;
        }
    }
    
    public String reject() {
        try {
            if (id == null) {
                jsonResponse.put("success", false);
                return SUCCESS;
            }
            
            leadService.finalizeValutazione(id, false, 
                                           getCurrentUser() != null ? getCurrentUser().getId() : 1L);
            
            jsonResponse.put("success", true);
            jsonResponse.put("message", "Valutazione rifiutata");
            
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error rejecting valutazione", e);
            jsonResponse.put("success", false);
            jsonResponse.put("error", e.getMessage());
            return SUCCESS;
        }
    }
    
    public String addSollecito() {
        try {
            if (id == null) {
                jsonResponse.put("success", false);
                return SUCCESS;
            }
            
            valutazione = valutazioneDAO.findById(id);
            valutazione.richiedeSollecitoDocumenti();
            valutazioneDAO.update(valutazione);
            
            jsonResponse.put("success", true);
            jsonResponse.put("soliciti", valutazione.getSollecitiInviatiCount());
            
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error adding sollecito", e);
            jsonResponse.put("success", false);
            jsonResponse.put("error", e.getMessage());
            return SUCCESS;
        }
    }
    
    public String requireingSolicito() {
        try {
            List<NoleggioValutazione> requiring = valutazioneDAO.findRequiringSollecito(4);
            jsonResponse.put("count", requiring.size());
            jsonResponse.put("valuazioni", requiring);
            
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error getting requiring sollecito", e);
            return ERROR;
        }
    }
    
    public String delete() {
        try {
            if (id == null) {
                addActionError("ID non valido");
                return ERROR;
            }
            
            NoleggioValutazione v = valutazioneDAO.findById(id);
            if (v != null) {
                valutazioneDAO.delete(v);
                addActionMessage("Valutazione eliminata");
            }
            
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error deleting valutazione", e);
            addActionError("Errore nell'eliminazione");
            return ERROR;
        }
    }
    
    // ============== GETTERS/SETTERS ==============
    
    public NoleggioValutazione getValutazione() { return valutazione; }
    public void setValutazione(NoleggioValutazione valutazione) { this.valutazione = valutazione; }
    
    public List<NoleggioValutazione> getValuazioni() { return valuazioni; }
    public void setValuazioni(List<NoleggioValutazione> valuazioni) { this.valuazioni = valuazioni; }
    
    public NoleggioLead getLead() { return lead; }
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getLeadId() { return leadId; }
    public void setLeadId(Long leadId) { this.leadId = leadId; }
    
    public String getStatusFiltro() { return statusFiltro; }
    public void setStatusFiltro(String statusFiltro) { this.statusFiltro = statusFiltro; }
    
    public Map<String, Object> getJsonResponse() { return jsonResponse; }
    
    private User getCurrentUser() {
        Map<String, Object> session = com.opensymphony.xwork2.ActionContext.getContext().getSession();
        return (User) session.get("currentUser");
    }
}
