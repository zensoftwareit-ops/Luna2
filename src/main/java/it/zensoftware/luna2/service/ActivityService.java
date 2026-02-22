package it.zensoftware.luna2.service;

import it.zensoftware.luna2.model.*;
import it.zensoftware.luna2.dao.*;
import org.hibernate.SessionFactory;
import java.util.*;

/**
 * ActivityService - Business logic per attività CRM
 */
public class ActivityService {
    private ActivityDAO activityDAO;
    private TaskDAO taskDAO;
    private ReminderDAO reminderDAO;

    public ActivityService() {
        this.activityDAO = new ActivityDAO();
        this.taskDAO = new TaskDAO();
        this.reminderDAO = new ReminderDAO();
    }

    /**
     * Crea una nuova attività
     */
    public Activity createActivity(Lead lead, Activity.ActivityType tipo, String titolo,
                                   String descrizione, User utente, Date dataProssima) {
        Activity activity = new Activity(lead, tipo, titolo);
        activity.setDescrizione(descrizione);
        activity.setUtente(utente);
        activity.setDataProssimaAttivita(dataProssima);
        activity.setStato(Activity.ActivityStatus.PENDING);
        
        activityDAO.save(activity);
        return activity;
    }

    /**
     * Completa un'attività
     */
    public Activity completeActivity(Long activityId) {
        Activity activity = activityDAO.findById(activityId);
        if (activity != null) {
            activity.setStato(Activity.ActivityStatus.COMPLETED);
            activity.setDataModifica(new Date());
            activityDAO.update(activity);
        }
        return activity;
    }

    /**
     * Cancella un'attività
     */
    public void deleteActivity(Long activityId) {
        Activity activity = activityDAO.findById(activityId);
        if (activity != null) {
            activityDAO.delete(activity);
        }
    }

    /**
     * Restituisce attività pendenti per un utente
     */
    public List<Activity> getPendingActivities(User utente) {
        return activityDAO.findPendingByUtente(utente.getId());
    }

    /**
     * Restituisce attività per un lead
     */
    public List<Activity> getActivityHistory(Lead lead) {
        return activityDAO.findByLead(lead);
    }

    /**
     * Restituisce attività per tipo
     */
    public List<Activity> getActivitiesByType(Activity.ActivityType tipo) {
        return activityDAO.findByTipo(tipo);
    }

    /**
     * Conta attività per lead
     */
    public long countActivitiesByLead(Long leadId) {
        return activityDAO.countByLead(leadId);
    }

    /**
     * Restituisce statistiche attività per tipo
     */
    public Map<String, Long> getActivityStats() {
        Map<String, Long> stats = new HashMap<>();
        List<Activity> activities = activityDAO.findAll();
        
        for (Activity activity : activities) {
            String type = activity.getTipo().toString();
            stats.put(type, stats.getOrDefault(type, 0L) + 1);
        }
        
        return stats;
    }

    /**
     * Aggiorna una nota attività
     */
    public Activity updateActivityNote(Long activityId, String nuotaNota) {
        Activity activity = activityDAO.findById(activityId);
        if (activity != null) {
            activity.setDescrizione(nuotaNota);
            activity.setDataModifica(new Date());
            activityDAO.update(activity);
        }
        return activity;
    }

    /**
     * Reschedula la prossima attività
     */
    public Activity updateNextActivity(Long activityId, Date nuovaData) {
        Activity activity = activityDAO.findById(activityId);
        if (activity != null) {
            activity.setDataProssimaAttivita(nuovaData);
            activity.setDataModifica(new Date());
            activityDAO.update(activity);
        }
        return activity;
    }
}

