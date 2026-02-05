package it.zensoftware.luna2.action;

import com.opensymphony.xwork2.ActionSupport;
import it.zensoftware.luna2.dao.LeadDAO;
import it.zensoftware.luna2.model.Lead;
import it.zensoftware.luna2.model.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.struts2.ServletActionContext;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Lead Action for CRM lead management
 */
public class LeadAction extends ActionSupport {
    
    private static final Logger logger = LogManager.getLogger(LeadAction.class);
    private static final long serialVersionUID = 1L;

    private LeadDAO leadDAO = new LeadDAO();
    
    private Lead lead;
    private List<Lead> leads;
    private Long id;
    private String searchTerm;
    private String statoFiltro;
    
    public String list() {
        try {
            if (statoFiltro != null && !statoFiltro.isEmpty()) {
                leads = leadDAO.findByStato(Lead.Stato.valueOf(statoFiltro));
            } else {
                leads = leadDAO.findOpenLeads();
            }
            
            if (searchTerm != null && !searchTerm.isEmpty()) {
                leads.removeIf(l -> !l.getAzienda().toLowerCase().contains(searchTerm.toLowerCase()) &&
                        !l.getNomeContatto().toLowerCase().contains(searchTerm.toLowerCase()));
            }
            
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error listing leads", e);
            addActionError("Errore nel caricamento dei lead");
            return ERROR;
        }
    }

    public String view() {
        try {
            if (id != null) {
                lead = leadDAO.findById(id);
                if (lead == null) {
                    addActionError("Lead non trovato");
                    return ERROR;
                }
            }
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error viewing lead", e);
            addActionError("Errore nel caricamento del lead");
            return ERROR;
        }
    }

    public String create() {
        lead = new Lead();
        lead.setDataContatto(new Date());
        return SUCCESS;
    }

    public String edit() {
        try {
            if (id != null) {
                lead = leadDAO.findById(id);
                if (lead == null) {
                    addActionError("Lead non trovato");
                    return INPUT;
                }
            }
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error editing lead", e);
            addActionError("Errore nel caricamento del lead");
            return INPUT;
        }
    }

    public String save() {
        try {
            if (!validateLead()) {
                return INPUT;
            }
            
            User currentUser = getCurrentUser();
            
            if (lead.getId() == null) {
                // Generate lead code if not provided
                if (lead.getAzienda() == null || lead.getAzienda().trim().isEmpty()) {
                    addFieldError("lead.azienda", "Azienda è obbligatoria");
                    return INPUT;
                }
                lead.setCreatedBy(currentUser);
                leadDAO.save(lead);
                addActionMessage("Lead creato con successo");
            } else {
                lead.setModifiedBy(currentUser);
                leadDAO.update(lead);
                addActionMessage("Lead aggiornato con successo");
            }
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error saving lead", e);
            addActionError("Errore nel salvataggio del lead: " + e.getMessage());
            return INPUT;
        }
    }

    public String delete() {
        try {
            if (id != null) {
                Lead l = leadDAO.findById(id);
                if (l != null) {
                    leadDAO.delete(l);
                    addActionMessage("Lead eliminato con successo");
                } else {
                    addActionError("Lead non trovato");
                }
            }
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error deleting lead", e);
            addActionError("Errore nell'eliminazione del lead");
            return ERROR;
        }
    }

    private boolean validateLead() {
        if (lead.getAzienda() == null || lead.getAzienda().trim().isEmpty()) {
            addFieldError("lead.azienda", "Azienda è obbligatoria");
            return false;
        }
        if (lead.getNomeContatto() == null || lead.getNomeContatto().trim().isEmpty()) {
            addFieldError("lead.nomeContatto", "Nome contatto è obbligatorio");
            return false;
        }
        if (lead.getEmail() == null || lead.getEmail().trim().isEmpty()) {
            addFieldError("lead.email", "Email è obbligatoria");
            return false;
        }
        return true;
    }

    private User getCurrentUser() {
        Map<String, Object> session = com.opensymphony.xwork2.ActionContext.getContext().getSession();
        return (User) session.get("currentUser");
    }

    // Getters and Setters
    public Lead getLead() {
        return lead;
    }

    public void setLead(Lead lead) {
        this.lead = lead;
    }

    public List<Lead> getLeads() {
        return leads;
    }

    public void setLeads(List<Lead> leads) {
        this.leads = leads;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSearchTerm() {
        return searchTerm;
    }

    public void setSearchTerm(String searchTerm) {
        this.searchTerm = searchTerm;
    }

    public String getStatoFiltro() {
        return statoFiltro;
    }

    public void setStatoFiltro(String statoFiltro) {
        this.statoFiltro = statoFiltro;
    }

    public Lead.Stato[] getStatiLead() {
        return Lead.Stato.values();
    }

    public Lead.Origine[] getOriginiLead() {
        return Lead.Origine.values();
    }
}
