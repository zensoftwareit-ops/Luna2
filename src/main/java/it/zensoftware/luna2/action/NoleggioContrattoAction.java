package it.zensoftware.luna2.action;

import com.opensymphony.xwork2.ActionSupport;
import it.zensoftware.luna2.dao.*;
import it.zensoftware.luna2.model.*;
import it.zensoftware.luna2.service.NoleggioScadenzarioService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

/**
 * NoleggioContrattoAction - Active contract management with calendar integration (Phase 5)
 */
public class NoleggioContrattoAction extends ActionSupport {
    
    private static final Logger logger = LogManager.getLogger(NoleggioContrattoAction.class);
    private static final long serialVersionUID = 1L;

    private NoleggioContrattoDAO contrattoDAO;
    private NoleggioLeadDAO leadDAO;
    private NoleggioScadenzarioService scadenzarioService;
    
    private NoleggioContratto contratto;
    private List<NoleggioContratto> contratti;
    private NoleggioLead lead;
    
    private Long id;
    private Long leadId;
    private String statusFiltro;
    private String targa;
    private Map<String, Object> jsonResponse = new HashMap<>();
    
    public NoleggioContrattoAction() {
        this.contrattoDAO = new NoleggioContrattoDAO();
        this.leadDAO = new NoleggioLeadDAO();
        this.scadenzarioService = new NoleggioScadenzarioService();
    }
    
    public String list() {
        try {
            contratti = contrattoDAO.findActive();
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error listing contratti", e);
            addActionError("Errore nel caricamento dei contratti");
            return ERROR;
        }
    }
    
    public String view() {
        try {
            if (id == null) {
                addActionError("ID non valido");
                return ERROR;
            }
            
            contratto = contrattoDAO.findById(id);
            if (contratto == null) {
                addActionError("Contratto non trovato");
                return ERROR;
            }
            
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error viewing contratto", e);
            addActionError("Errore nel caricamento");
            return ERROR;
        }
    }
    
    public String create() {
        try {
            if (leadId != null) {
                lead = leadDAO.findById(leadId);
            }
            contratto = new NoleggioContratto();
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error creating contratto", e);
            return ERROR;
        }
    }
    
    public String save() {
        try {
            if (contratto == null || contratto.getTarga() == null) {
                addFieldError("contratto.targa", "Targa è obbligatoria");
                return INPUT;
            }
            
            User currentUser = getCurrentUser();
            
            if (contratto.getId() == null) {
                contratto.setDataCreazione(new Date());
                contratto.setUtenteCreazioneId(currentUser != null ? currentUser.getId() : 1L);
                contratto.setStatus(NoleggioContratto.Status.ATTIVO);
                contrattoDAO.save(contratto);
                addActionMessage("Contratto creato: " + contratto.getNumeroContratto());
            } else {
                contratto.setDataModifica(new Date());
                contrattoDAO.update(contratto);
                addActionMessage("Contratto aggiornato");
            }
            
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error saving contratto", e);
            addActionError("Errore nel salvataggio: " + e.getMessage());
            return INPUT;
        }
    }
    
    public String expiringRenewals() {
        try {
            List<NoleggioContratto> expiring = contrattoDAO.findExpiringWithin(120);
            jsonResponse.put("count", expiring.size());
            jsonResponse.put("contratti", expiring);
            
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error getting expiring renewals", e);
            return ERROR;
        }
    }
    
    public String kmVerification() {
        try {
            List<NoleggioContratto> requiring = contrattoDAO.findRequiringKmVerification();
            jsonResponse.put("count", requiring.size());
            jsonResponse.put("contratti", requiring);
            
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error getting km verification", e);
            return ERROR;
        }
    }
    
    public String upcomingRevisione() {
        try {
            List<NoleggioContratto> upcoming = contrattoDAO.findWithUpcomingRevisione(30);
            jsonResponse.put("count", upcoming.size());
            jsonResponse.put("contratti", upcoming);
            
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error getting upcoming revisione", e);
            return ERROR;
        }
    }
    
    public String upcomingTagliando() {
        try {
            List<NoleggioContratto> upcoming = contrattoDAO.findWithUpcomingTagliando(15);
            jsonResponse.put("count", upcoming.size());
            jsonResponse.put("contratti", upcoming);
            
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error getting upcoming tagliando", e);
            return ERROR;
        }
    }
    
    public String exceededKm() {
        try {
            List<NoleggioContratto> exceeded = contrattoDAO.findWithExceededKm();
            jsonResponse.put("count", exceeded.size());
            jsonResponse.put("contratti", exceeded);
            
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error getting exceeded km", e);
            return ERROR;
        }
    }
    
    public String delete() {
        try {
            if (id == null) {
                addActionError("ID non valido");
                return ERROR;
            }
            
            NoleggioContratto c = contrattoDAO.findById(id);
            if (c != null) {
                c.setStatus(NoleggioContratto.Status.SCADUTO);
                contrattoDAO.update(c);
                addActionMessage("Contratto scaduto");
            }
            
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error deleting contratto", e);
            addActionError("Errore nella modifica");
            return ERROR;
        }
    }
    
    // ============== GETTERS/SETTERS ==============
    
    public NoleggioContratto getContratto() { return contratto; }
    public void setContratto(NoleggioContratto contratto) { this.contratto = contratto; }
    
    public List<NoleggioContratto> getContratti() { return contratti; }
    public void setContratti(List<NoleggioContratto> contratti) { this.contratti = contratti; }
    
    public NoleggioLead getLead() { return lead; }
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getLeadId() { return leadId; }
    public void setLeadId(Long leadId) { this.leadId = leadId; }
    
    public String getStatusFiltro() { return statusFiltro; }
    public void setStatusFiltro(String statusFiltro) { this.statusFiltro = statusFiltro; }
    
    public String getTarga() { return targa; }
    public void setTarga(String targa) { this.targa = targa; }
    
    public Map<String, Object> getJsonResponse() { return jsonResponse; }
    
    private User getCurrentUser() {
        Map<String, Object> session = com.opensymphony.xwork2.ActionContext.getContext().getSession();
        return (User) session.get("currentUser");
    }
}
