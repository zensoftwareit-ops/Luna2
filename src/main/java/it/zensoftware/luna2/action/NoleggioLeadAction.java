package it.zensoftware.luna2.action;

import com.opensymphony.xwork2.ActionSupport;
import it.zensoftware.luna2.dao.*;
import it.zensoftware.luna2.model.*;
import it.zensoftware.luna2.service.NoleggioLeadService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.struts2.ServletActionContext;

import java.io.PrintWriter;
import java.util.*;
import javax.servlet.http.HttpServletResponse;

/**
 * NoleggioLeadAction - Rental CRM leads management (Phases 1-3)
 */
public class NoleggioLeadAction extends ActionSupport {
    
    private static final Logger logger = LogManager.getLogger(NoleggioLeadAction.class);
    private static final long serialVersionUID = 1L;

    private NoleggioLeadDAO leadDAO;
    private NolleggioPreventivoDAO preventivoDAO;
    private NoleggioDocumentoDAO documentoDAO;
    private NoleggioValutazioneDAO valutazioneDAO;
    private NoleggioOrdineDAO ordineDAO;
    private NoleggioLeadService leadService;
    
    private NoleggioLead lead;
    private List<NoleggioLead> leads;
    private NoleggioPreventivo preventivo;
    private NoleggioDocumento documento;
    private NoleggioValutazione valutazione;
    private NoleggioOrdine ordine;
    
    private Long id;
    private Long preventivoId;
    private Long documentoId;
    private Long valutazioneId;
    private Long ordineId;
    private String searchTerm;
    private NoleggioLead.Fase fasceFiltro;
    private Map<String, Object> jsonResponse = new HashMap<>();
    
    public NoleggioLeadAction() {
        this.leadDAO = new NoleggioLeadDAO();
        this.preventivoDAO = new NolleggioPreventivoDAO();
        this.documentoDAO = new NoleggioDocumentoDAO();
        this.valutazioneDAO = new NoleggioValutazioneDAO();
        this.ordineDAO = new NoleggioOrdineDAO();
        this.leadService = new NoleggioLeadService();
    }
    
    // ============== LEAD LIST ==============
    
    public String list() {
        try {
            // Filter by fase if specified
            if (fasceFiltro != null) {
                leads = leadDAO.findByFase(fasceFiltro);
            } else {
                leads = leadDAO.findAll();
            }
            
            // Search by customer name
            if (searchTerm != null && !searchTerm.isEmpty()) {
                List<NoleggioLead> filtered = new ArrayList<>();
                for (NoleggioLead l : leads) {
                    if (l.getNomeCliente().toLowerCase().contains(searchTerm.toLowerCase()) ||
                        l.getEmail().toLowerCase().contains(searchTerm.toLowerCase())) {
                        filtered.add(l);
                    }
                }
                leads = filtered;
            }
            
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error listing leads", e);
            addActionError("Errore nel caricamento dei lead");
            return ERROR;
        }
    }
    
    // ============== LEAD CRUD ==============
    
    public String create() {
        try {
            lead = new NoleggioLead();
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error creating lead", e);
            return ERROR;
        }
    }
    
    public String view() {
        try {
            if (id == null) {
                addActionError("ID non valido");
                return ERROR;
            }
            
            lead = leadDAO.findById(id);
            if (lead == null) {
                addActionError("Lead non trovato");
                return ERROR;
            }
            
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error viewing lead", e);
            addActionError("Errore nel caricamento del lead");
            return ERROR;
        }
    }
    
    public String save() {
        try {
            if (lead == null || lead.getNomeCliente() == null) {
                addFieldError("lead.nomeCliente", "Nome cliente è obbligatorio");
                return INPUT;
            }
            
            User currentUser = getCurrentUser();
            
            if (lead.getId() == null) {
                lead = leadService.createLead(lead, currentUser != null ? currentUser.getId() : 1L);
                addActionMessage("Lead creato con successo: " + lead.getCodiceNoleggio());
            } else {
                leadDAO.update(lead);
                addActionMessage("Lead aggiornato con successo");
            }
            
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error saving lead", e);
            addActionError("Errore nel salvataggio: " + e.getMessage());
            return INPUT;
        }
    }
    
    public String delete() {
        try {
            if (id == null) {
                addActionError("ID non valido");
                return ERROR;
            }
            
            NoleggioLead l = leadDAO.findById(id);
            if (l != null) {
                leadDAO.delete(l);
                addActionMessage("Lead eliminato con successo");
            }
            
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error deleting lead", e);
            addActionError("Errore nell'eliminazione");
            return ERROR;
        }
    }
    
    // ============== PARTE QUOTE MANAGEMENT ==============
    
    public String createQuote() {
        try {
            if (id == null) {
                addActionError("Lead non trovato");
                return ERROR;
            }
            
            lead = leadDAO.findById(id);
            preventivo = new NoleggioPreventivo();
            
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error creating quote", e);
            return ERROR;
        }
    }
    
    public String saveQuote() {
        try {
            if (id == null) {
                addActionError("Lead non valido");
                return ERROR;
            }
            
            User currentUser = getCurrentUser();
            preventivo = leadService.createPreventivo(id, preventivo, 
                                                      currentUser != null ? currentUser.getId() : 1L);
            
            addActionMessage("Preventivo creato: " + preventivo.getNumeroPreventivo());
            jsonResponse.put("success", true);
            jsonResponse.put("preventivoId", preventivo.getId());
            
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error saving quote", e);
            jsonResponse.put("success", false);
            jsonResponse.put("error", e.getMessage());
            return SUCCESS;
        }
    }
    
    public String sendQuote() {
        try {
            if (preventivoId == null) {
                jsonResponse.put("success", false);
                jsonResponse.put("error", "Preventivo non trovato");
                return SUCCESS;
            }
            
            preventivo = preventivoDAO.findById(preventivoId);
            leadService.sendPreventivo(preventivoId, 
                                     getCurrentUser() != null ? getCurrentUser().getId() : 1L);
            
            jsonResponse.put("success", true);
            jsonResponse.put("message", "Preventivo inviato");
            
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error sending quote", e);
            jsonResponse.put("success", false);
            jsonResponse.put("error", e.getMessage());
            return SUCCESS;
        }
    }
    
    public String acceptQuote() {
        try {
            if (preventivoId == null) {
                jsonResponse.put("success", false);
                return SUCCESS;
            }
            
            lead = leadService.acceptPreventivo(preventivoId, 
                                                getCurrentUser() != null ? getCurrentUser().getId() : 1L);
            
            jsonResponse.put("success", true);
            jsonResponse.put("fase", lead.getFase().toString());
            jsonResponse.put("message", "Preventivo accettato - Lead in ISTRUTTORIA");
            
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error accepting quote", e);
            jsonResponse.put("success", false);
            jsonResponse.put("error", e.getMessage());
            return SUCCESS;
        }
    }
    
    // ============== DASHBOARD JSON ==============
    
    public String dashboardData() {
        try {
            Map<String, Long> faseCount = new HashMap<>();
            for (NoleggioLead.Fase fase : NoleggioLead.Fase.values()) {
                faseCount.put(fase.toString(), leadDAO.countByFase(fase));
            }
            
            jsonResponse.put("faseData", faseCount);
            jsonResponse.put("totalLeads", leadDAO.findAll().size());
            
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error getting dashboard data", e);
            return ERROR;
        }
    }
    
    // ============== GETTERS/SETTERS ==============
    
    public NoleggioLead getLead() { return lead; }
    public void setLead(NoleggioLead lead) { this.lead = lead; }
    
    public List<NoleggioLead> getLeads() { return leads; }
    public void setLeads(List<NoleggioLead> leads) { this.leads = leads; }
    
    public NoleggioPreventivo getPreventivo() { return preventivo; }
    public void setPreventivo(NoleggioPreventivo preventivo) { this.preventivo = preventivo; }
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getPreventivoId() { return preventivoId; }
    public void setPreventivoId(Long preventivoId) { this.preventivoId = preventivoId; }
    
    public String getSearchTerm() { return searchTerm; }
    public void setSearchTerm(String searchTerm) { this.searchTerm = searchTerm; }
    
    public NoleggioLead.Fase getFasceFiltro() { return fasceFiltro; }
    public void setFasceFiltro(NoleggioLead.Fase fasceFiltro) { this.fasceFiltro = fasceFiltro; }
    
    public Map<String, Object> getJsonResponse() { return jsonResponse; }
    
    private User getCurrentUser() {
        Map<String, Object> session = com.opensymphony.xwork2.ActionContext.getContext().getSession();
        return (User) session.get("currentUser");
    }
}
