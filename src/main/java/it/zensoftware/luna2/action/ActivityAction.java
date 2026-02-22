package it.zensoftware.luna2.action;

import com.opensymphony.xwork2.ActionSupport;
import it.zensoftware.luna2.dao.*;
import it.zensoftware.luna2.model.*;
import it.zensoftware.luna2.service.ActivityService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.struts2.ServletActionContext;
import org.hibernate.SessionFactory;

import java.util.*;

/**
 * ActivityAction - Struts2 Action per gestire attività CRM
 */
public class ActivityAction extends ActionSupport {
    
    private static final Logger logger = LogManager.getLogger(ActivityAction.class);
    private static final long serialVersionUID = 1L;

    private SessionFactory sessionFactory;
    private ActivityDAO activityDAO;
    private ActivityService activityService;
    private LeadDAO leadDAO;

    private Activity activity;
    private List<Activity> activities;
    private Long id;
    private Long leadId;
    private String titulo;
    private String descrizione;
    private Activity.ActivityType tipo;
    private Date dataProssima;
    private Map<String, Object> jsonResponse = new HashMap<>();
    private int success = 0;

    public ActivityAction() {
        activityDAO = new ActivityDAO();
    }

    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
        this.activityDAO = new ActivityDAO();
        this.activityService = new ActivityService();
        this.leadDAO = new LeadDAO();
    }

    /**
     * 1. LIST_BY_LEAD - Restituisce attività per un specific lead
     */
    public String listByLead() {
        try {
            if (leadId != null) {
                activities = activityDAO.findByLead(leadId);
                jsonResponse.put("activities", activities);
                jsonResponse.put("count", activities.size());
                success = 1;
            } else {
                jsonResponse.put("message", "Lead ID non specificato");
                success = 0;
            }
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error listing activities", e);
            jsonResponse.put("message", "Errore nel caricamento attività: " + e.getMessage());
            success = 0;
            return SUCCESS;
        }
    }

    /**
     * 2. CREATE_ACTIVITY - Crea una nuova attività
     */
    public String createActivity() {
        try {
            Lead lead = leadDAO.findById(leadId);
            if (lead == null) {
                jsonResponse.put("message", "Lead non trovato");
                success = 0;
                return SUCCESS;
            }

            User currentUser = getCurrentUser();
            activity = activityService.createActivity(
                lead, tipo, titulo, descrizione, currentUser, dataProssima
            );

            jsonResponse.put("activityId", activity.getId());
            jsonResponse.put("message", "Attività creata con successo");
            success = 1;
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error creating activity", e);
            jsonResponse.put("message", "Errore nella creazione attività: " + e.getMessage());
            success = 0;
            return SUCCESS;
        }
    }

    /**
     * 3. COMPLETE_ACTIVITY - Segna un'attività come completata
     */
    public String completeActivity() {
        try {
            if (id != null) {
                activity = activityService.completeActivity(id);
                jsonResponse.put("message", "Attività completata");
                success = 1;
            } else {
                jsonResponse.put("message", "Activity ID non specificato");
                success = 0;
            }
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error completing activity", e);
            jsonResponse.put("message", "Errore nel completamento attività: " + e.getMessage());
            success = 0;
            return SUCCESS;
        }
    }

    /**
     * 4. UPDATE_DUE_DATE - Aggiorna la data della prossima attività
     */
    public String updateDueDate() {
        try {
            if (id != null && dataProssima != null) {
                activity = activityService.updateNextActivity(id, dataProssima);
                jsonResponse.put("message", "Data prossima attività aggiornata");
                success = 1;
            } else {
                jsonResponse.put("message", "Parametri non validi");
                success = 0;
            }
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error updating due date", e);
            jsonResponse.put("message", "Errore nell'aggiornamento data: " + e.getMessage());
            success = 0;
            return SUCCESS;
        }
    }

    /**
     * 5. DELETE_ACTIVITY - Elimina un'attività
     */
    public String deleteActivity() {
        try {
            if (id != null) {
                Activity activity = activityDAO.findById(id);
                if (activity != null) {
                    activityDAO.delete(activity);
                    jsonResponse.put("message", "Attività eliminata");
                    success = 1;
                } else {
                    jsonResponse.put("message", "Attività non trovata");
                    success = 0;
                }
            } else {
                jsonResponse.put("message", "Activity ID non specificato");
                success = 0;
            }
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error deleting activity", e);
            jsonResponse.put("message", "Errore nell'eliminazione attività: " + e.getMessage());
            success = 0;
            return SUCCESS;
        }
    }

    /**
     * Get current user from session
     */
    private User getCurrentUser() {
        Map<String, Object> session = com.opensymphony.xwork2.ActionContext.getContext().getSession();
        Object user = session.get("currentUser");
        if (user instanceof User) {
            return (User) user;
        }
        return null;
    }

    // ============== GETTERS & SETTERS ==============

    public Activity getActivity() { return activity; }
    public void setActivity(Activity activity) { this.activity = activity; }

    public List<Activity> getActivities() { return activities; }
    public void setActivities(List<Activity> activities) { this.activities = activities; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getLeadId() { return leadId; }
    public void setLeadId(Long leadId) { this.leadId = leadId; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getDescrizione() { return descrizione; }
    public void setDescrizione(String descrizione) { this.descrizione = descrizione; }

    public Activity.ActivityType getTipo() { return tipo; }
    public void setTipo(Activity.ActivityType tipo) { this.tipo = tipo; }

    public Date getDataProssima() { return dataProssima; }
    public void setDataProssima(Date dataProssima) { this.dataProssima = dataProssima; }

    public Map<String, Object> getJsonResponse() { return jsonResponse; }
    public void setJsonResponse(Map<String, Object> jsonResponse) { this.jsonResponse = jsonResponse; }

    public int getSuccess() { return success; }
    public void setSuccess(int success) { this.success = success; }
}
