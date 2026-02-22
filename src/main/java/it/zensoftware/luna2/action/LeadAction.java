package it.zensoftware.luna2.action;

import com.opensymphony.xwork2.ActionSupport;
import it.zensoftware.luna2.dao.*;
import it.zensoftware.luna2.model.*;
import it.zensoftware.luna2.service.LeadService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.struts2.ServletActionContext;
import org.hibernate.SessionFactory;

import java.io.PrintWriter;
import java.util.*;
import javax.servlet.http.HttpServletResponse;

/**
 * Lead Action for CRM lead management with advanced pipeline features
 */
public class LeadAction extends ActionSupport {
    
    private static final Logger logger = LogManager.getLogger(LeadAction.class);
    private static final long serialVersionUID = 1L;

    private SessionFactory sessionFactory;
    private LeadDAO leadDAO;
    private LeadService leadService;
    private ActivityDAO activityDAO;
    private TaskDAO taskDAO;
    private StoriaLeadDAO storiaLeadDAO;
    private ReminderDAO reminderDAO;
    
    private Lead lead;
    private List<Lead> leads;
    private Long id;
    private String searchTerm;
    private String statoFiltro;
    
    // CRM-specific fields
    private String newStage;
    private String stageChangeReason;
    private Activity.ActivityType activityType;
    private String activityTitle;
    private String activityDescription;
    private Date nextActivityDate;
    private String taskDescription;
    private Date taskDueDate;
    private Task.TaskPriority taskPriority;
    private List<Activity> activities;
    private List<Task> tasks;
    private List<StoriaLead> stageHistory;
    private Map<String, Object> jsonResponse = new HashMap<>();
    private int success = 0;
    
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

    /**
     * Change stage of lead in CRM pipeline (supports drag-drop)
     */
    public String changeStage() {
        try {
            initializeDaos();
            if (id != null) {
                lead = leadDAO.findById(id);
                if (lead == null) {
                    jsonResponse.put("success", 0);
                    jsonResponse.put("message", "Lead non trovato");
                    return SUCCESS;
                }
                
                String oldStage = lead.getStato().toString();
                User currentUser = getCurrentUser();
                leadService.changeStage(lead, oldStage, newStage, stageChangeReason, currentUser);
                
                jsonResponse.put("success", 1);
                jsonResponse.put("leadId", lead.getId());
                jsonResponse.put("oldStage", oldStage);
                jsonResponse.put("newStage", newStage);
                jsonResponse.put("valoreStimato", leadService.calculateWeightedValue(lead));
                jsonResponse.put("message", "Stage cambiato da " + oldStage + " a " + newStage);
            }
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error changing stage", e);
            jsonResponse.put("success", 0);
            jsonResponse.put("message", "Errore nel cambio stage: " + e.getMessage());
            return SUCCESS;
        }
    }

    /**
     * Add activity (call, email, note, meeting) to lead
     */
    public String addActivity() {
        try {
            initializeDaos();
            if (id != null) {
                lead = leadDAO.findById(id);
                if (lead == null) {
                    jsonResponse.put("success", 0);
                    jsonResponse.put("message", "Lead non trovato");
                    return SUCCESS;
                }
                
                User currentUser = getCurrentUser();
                Activity activity = leadService.logActivity(
                    lead, activityType, activityTitle, activityDescription, 
                    currentUser, nextActivityDate
                );
                
                jsonResponse.put("success", 1);
                jsonResponse.put("activityId", activity.getId());
                jsonResponse.put("message", "Attività registrata: " + activityTitle);
            }
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error adding activity", e);
            jsonResponse.put("success", 0);
            jsonResponse.put("message", "Errore nel salvataggio attività: " + e.getMessage());
            return SUCCESS;
        }
    }

    /**
     * Add task/follow-up to lead
     */
    public String addTask() {
        try {
            initializeDaos();
            if (id != null) {
                lead = leadDAO.findById(id);
                if (lead == null) {
                    jsonResponse.put("success", 0);
                    jsonResponse.put("message", "Lead non trovato");
                    return SUCCESS;
                }
                
                User currentUser = getCurrentUser();
                Task task = leadService.createTask(
                    lead, taskDescription, taskDueDate, 
                    taskPriority != null ? taskPriority : Task.TaskPriority.MEDIA,
                    currentUser, currentUser
                );
                
                jsonResponse.put("success", 1);
                jsonResponse.put("taskId", task.getId());
                jsonResponse.put("message", "Task creato: " + taskDescription);
            }
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error adding task", e);
            jsonResponse.put("success", 0);
            jsonResponse.put("message", "Errore nel salvataggio task: " + e.getMessage());
            return SUCCESS;
        }
    }

    /**
     * Get Kanban pipeline view with leads grouped by stage
     */
    public String pipeline() {
        try {
            initializeDaos();
            Map<String, List<Lead>> pipelineData = new HashMap<>();
            List<Lead> allLeads = leadDAO.findAll();
            
            for (Lead l : allLeads) {
                String stage = l.getStato().toString();
                pipelineData.computeIfAbsent(stage, k -> new ArrayList<>()).add(l);
            }
            
            leads = allLeads;
            jsonResponse.put("pipeline", pipelineData);
            jsonResponse.put("stats", leadService.getCountByStage());
            jsonResponse.put("conversionRate", leadService.getConversionRate());
            jsonResponse.put("topOpportunities", leadService.getTopOpportunitiesByValue(5));
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error loading pipeline", e);
            addActionError("Errore nel caricamento pipeline: " + e.getMessage());
            return ERROR;
        }
    }

    /**
     * Export lead list to CSV
     */
    public String export() {
        try {
            initializeDaos();
            List<Lead> allLeads = leadDAO.findAll();
            
            HttpServletResponse response = ServletActionContext.getResponse();
            response.setContentType("text/csv");
            response.setHeader("Content-Disposition", "attachment; filename=\"lead_export.csv\"");
            
            PrintWriter writer = response.getWriter();
            
            // CSV Header
            writer.println("ID,Nome,Cognome,Azienda,Email,Telefono,Stato,Budget,Probabilita,Valore");
            
            // CSV Data
            for (Lead l : allLeads) {
                writer.printf("%d,\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",%s,%f,%d,%f%n",
                    l.getId(),
                    l.getNomeContatto() != null ? l.getNomeContatto() : "",
                    l.getCognomeContatto() != null ? l.getCognomeContatto() : "",
                    l.getAzienda() != null ? l.getAzienda() : "",
                    l.getEmail() != null ? l.getEmail() : "",
                    l.getTelefono() != null ? l.getTelefono() : "",
                    l.getStato(),
                    l.getBudgetStimato() != null ? l.getBudgetStimato() : 0,
                    l.getProbabilitaChiusura() != null ? l.getProbabilitaChiusura() : 0,
                    leadService.calculateWeightedValue(l)
                );
            }
            writer.flush();
            writer.close();
            
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error exporting leads", e);
            addActionError("Errore nell'esportazione: " + e.getMessage());
            return ERROR;
        }
    }

    /**
     * Initialize DAOs and service
     */
    private void initializeDaos() {
        if (leadDAO == null) {
            leadDAO = new LeadDAO();
        }
        if (leadService == null && sessionFactory != null) {
            leadService = new LeadService(sessionFactory);
        }
    }

    /**
     * Get current user from session
     */
    private User getCurrentUser() {
        Map<String, Object> session = com.opensymphony.xwork2.ActionContext.getContext().getSession();
        // Get User object from session
        Object user = session.get("currentUser");
        if (user instanceof User) {
            return (User) user;
        }
        // Fallback: return null or create dummy user
        return null;
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

    // CRM-specific getters and setters
    public String getNewStage() { return newStage; }
    public void setNewStage(String newStage) { this.newStage = newStage; }

    public String getStageChangeReason() { return stageChangeReason; }
    public void setStageChangeReason(String stageChangeReason) { this.stageChangeReason = stageChangeReason; }

    public Activity.ActivityType getActivityType() { return activityType; }
    public void setActivityType(Activity.ActivityType activityType) { this.activityType = activityType; }

    public String getActivityTitle() { return activityTitle; }
    public void setActivityTitle(String activityTitle) { this.activityTitle = activityTitle; }

    public String getActivityDescription() { return activityDescription; }
    public void setActivityDescription(String activityDescription) { this.activityDescription = activityDescription; }

    public Date getNextActivityDate() { return nextActivityDate; }
    public void setNextActivityDate(Date nextActivityDate) { this.nextActivityDate = nextActivityDate; }

    public String getTaskDescription() { return taskDescription; }
    public void setTaskDescription(String taskDescription) { this.taskDescription = taskDescription; }

    public Date getTaskDueDate() { return taskDueDate; }
    public void setTaskDueDate(Date taskDueDate) { this.taskDueDate = taskDueDate; }

    public Task.TaskPriority getTaskPriority() { return taskPriority; }
    public void setTaskPriority(Task.TaskPriority taskPriority) { this.taskPriority = taskPriority; }

    public List<Activity> getActivities() { return activities; }
    public void setActivities(List<Activity> activities) { this.activities = activities; }

    public List<Task> getTasks() { return tasks; }
    public void setTasks(List<Task> tasks) { this.tasks = tasks; }

    public List<StoriaLead> getStageHistory() { return stageHistory; }
    public void setStageHistory(List<StoriaLead> stageHistory) { this.stageHistory = stageHistory; }

    public Map<String, Object> getJsonResponse() { return jsonResponse; }
    public void setJsonResponse(Map<String, Object> jsonResponse) { this.jsonResponse = jsonResponse; }

    public int getSuccess() { return success; }
    public void setSuccess(int success) { this.success = success; }

    public SessionFactory getSessionFactory() { return sessionFactory; }
    public void setSessionFactory(SessionFactory sessionFactory) { this.sessionFactory = sessionFactory; }
}
