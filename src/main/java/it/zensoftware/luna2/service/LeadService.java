package it.zensoftware.luna2.service;

import it.zensoftware.luna2.model.*;
import it.zensoftware.luna2.dao.*;
import org.hibernate.SessionFactory;
import java.util.*;

/**
 * LeadService - Business logic per la gestione dei lead CRM
 * Gestisce: stage transitions, weighted value, scoring, pipeline analytics
 */
public class LeadService {
    private SessionFactory sessionFactory;
    private LeadDAO leadDAO;
    private ActivityDAO activityDAO;
    private TaskDAO taskDAO;
    private StoriaLeadDAO storiaLeadDAO;
    private ReminderDAO reminderDAO;

    // Pipeline stages in sequenza
    private static final String[] PIPELINE_STAGES = {
        "BOZZA", "QUALIFICATO", "PROPOSTA", "NEGOZIAZIONE", "VINTO", "PERSO"
    };

    // Percentuali di chiusura approssimative per stage (usato per scoring)
    private static final Map<String, Integer> STAGE_CLOSURE_PROBABILITY = new HashMap<String, Integer>() {{
        put("BOZZA", 10);
        put("QUALIFICATO", 25);
        put("PROPOSTA", 50);
        put("NEGOZIAZIONE", 75);
        put("VINTO", 100);
        put("PERSO", 0);
    }};

    public LeadService(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
        this.leadDAO = new LeadDAO();
        this.activityDAO = new ActivityDAO();
        this.taskDAO = new TaskDAO();
        this.storiaLeadDAO = new StoriaLeadDAO();
        this.reminderDAO = new ReminderDAO();
    }

    /**
     * Crea un nuovo lead (inizia in stage BOZZA)
     */
    public Lead createLead(Lead lead, User createdBy) {
        lead.setStato(Lead.Stato.NUOVO);
        lead.setDataContatto(new Date());
        lead.setCreatedBy(createdBy);
        lead.setModifiedBy(createdBy);
        
        // Salva il lead
        leadDAO.save(lead);
        
        // Crea storia iniziale
        StoriaLead storia = new StoriaLead(lead, null, "BOZZA", "Lead creato");
        storia.setUtente(createdBy);
        storiaLeadDAO.save(storia);
        
        return lead;
    }

    /**
     * Qualifica un lead (transizione BOZZA -> QUALIFICATO)
     */
    public Lead qualifyLead(Lead lead, String motivo, User utente) {
        return changeStage(lead, "BOZZA", "QUALIFICATO", motivo, utente);
    }

    /**
     * Cambia lo stage del lead con audit trail
     */
    public Lead changeStage(Lead lead, String stageDa, String stageA, String motivo, User utente) {
        // Validazione stages
        if (!isValidStage(stageA)) {
            throw new IllegalArgumentException("Stage non valido: " + stageA);
        }

        // Validazione transizione (se lo stage esiste, non permette tornare indietro)
        int indexDa = getStageIndex(stageDa);
        int indexA = getStageIndex(stageA);

        // Permetti tutte le transizioni, ma log tutto
        lead.setStato(stageToStato(stageA));
        lead.setDataModifica(new Date());
        lead.setModifiedBy(utente);
        
        // Aggiorna probabilità chiusura se impostata
        if (lead.getProbabilitaChiusura() == null) {
            lead.setProbabilitaChiusura(STAGE_CLOSURE_PROBABILITY.get(stageA));
        }

        leadDAO.update(lead);

        // Crea storia del cambio
        StoriaLead storia = new StoriaLead(lead, stageDa, stageA, motivo);
        storia.setUtente(utente);
        storia.setValoreLeadAlCambio(calculateWeightedValue(lead));
        storia.setProbabilitaChiusuraAlCambio(lead.getProbabilitaChiusura());
        storiaLeadDAO.save(storia);

        return lead;
    }

    /**
     * Calcola il valore ponderato del lead:
     * (BudgetStimato * ProbabilitaChiusura / 100)
     */
    public Double calculateWeightedValue(Lead lead) {
        if (lead.getBudgetStimato() == null || lead.getProbabilitaChiusura() == null) {
            return 0.0;
        }
        return (lead.getBudgetStimato().multiply(new java.math.BigDecimal(lead.getProbabilitaChiusura()))
                .divide(new java.math.BigDecimal(100))).doubleValue();
    }

    /**
     * Salva attività (call, email, note, meeting) e crea reminder se possibile
     */
    public Activity logActivity(Lead lead, Activity.ActivityType tipo, String titolo, 
                                String descrizione, User utente, Date prossima) {
        Activity activity = new Activity(lead, tipo, titolo);
        activity.setDescrizione(descrizione);
        activity.setUtente(utente);
        activity.setDataAttivita(new Date());
        activity.setDataProssimaAttivita(prossima);
        
        activityDAO.save(activity);

        // Aggiorna lead: dataProssimoFollowup se impostata
        if (prossima != null) {
            lead.setDataProssimoFollowup(prossima);
            leadDAO.update(lead);
        }

        return activity;
    }

    /**
     * Crea un task/follow-up
     */
    public Task createTask(Lead lead, String descrizione, Date dataScadenza, 
                          Task.TaskPriority priorita, User assegnatoA, User creatoDa) {
        Task task = new Task(lead, descrizione, dataScadenza);
        task.setPriorita(priorita);
        task.setAssegnatoA(assegnatoA);
        task.setCreatoDa(creatoDa);
        task.setStato(Task.TaskStatus.OPEN);
        
        taskDAO.save(task);

        // Crea reminder per il task
        Reminder reminder = new Reminder(lead, assegnatoA, Reminder.ReminderType.TASK, 
                                         "Task: " + descrizione, dataScadenza);
        reminder.setTask(task);
        reminder.setCanale("sistema");
        reminderDAO.save(reminder);

        return task;
    }

    /**
     * Completa un task
     */
    public Task completeTask(Long taskId, User utente) {
        Task task = taskDAO.findById(taskId);
        if (task != null) {
            task.setStato(Task.TaskStatus.COMPLETED);
            task.setDataCompletamento(new Date());
            taskDAO.update(task);
        }
        return task;
    }

    /**
     * Restituisce i lead scaduti (senza attività per X giorni)
     */
    public List<Lead> findExpiredLeads(int days) {
        // Logica: lead con dataProssimoFollowup passata e nessuna attività recente
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -days);
        Date dataLimite = cal.getTime();
        
        List<Lead> allLeads = leadDAO.findAll();
        List<Lead> expired = new ArrayList<>();
        
        for (Lead lead : allLeads) {
            if (lead.getDataProssimoFollowup() != null && 
                lead.getDataProssimoFollowup().before(dataLimite)) {
                
                // Controlla se ha attività recente
                List<Activity> activities = activityDAO.findByLead(lead.getId());
                if (activities.isEmpty() || 
                    (activities.get(0).getDataAttivita().before(dataLimite))) {
                    expired.add(lead);
                }
            }
        }
        
        return expired;
    }

    /**
     * Ranking dei lead per pipeline (top opportunities)
     */
    public List<Lead> getTopOpportunitiesByValue(int limit) {
        List<Lead> leads = leadDAO.findAll();
        leads.sort((a, b) -> {
            Double valueA = calculateWeightedValue(a);
            Double valueB = calculateWeightedValue(b);
            return valueB.compareTo(valueA);  // Ordinamento descrescente
        });
        return leads.stream().limit(limit).collect(java.util.stream.Collectors.toList());
    }

    /**
     * Analytics: conti per stage
     */
    public Map<String, Long> getCountByStage() {
        Map<String, Long> result = new HashMap<>();
        for (String stage : PIPELINE_STAGES) {
            result.put(stage, storiaLeadDAO.countByStageA(stage));
        }
        return result;
    }

    /**
     * Analytics: tasso di conversione QUALIFICATO -> VINTO
     */
    public Double getConversionRate() {
        Long qualified = storiaLeadDAO.countByStageA("QUALIFICATO");
        Long won = storiaLeadDAO.countByStageA("VINTO");
        
        if (qualified == 0) return 0.0;
        return (won.doubleValue() / qualified.doubleValue()) * 100.0;
    }

    /**
     * Cancella un lead (soft delete o hard delete)
     */
    public void deleteLead(Long leadId) {
        // Opzionale: implementare soft delete (aggiungere colonna "deleted")
        // Per ora facciamo hard delete
        Lead lead = leadDAO.findById(leadId);
        if (lead != null) {
            leadDAO.delete(lead);
        }
    }

    /* ============== UTILITY METHODS ============== */

    private boolean isValidStage(String stage) {
        for (String s : PIPELINE_STAGES) {
            if (s.equals(stage)) return true;
        }
        return false;
    }

    private int getStageIndex(String stage) {
        for (int i = 0; i < PIPELINE_STAGES.length; i++) {
            if (PIPELINE_STAGES[i].equals(stage)) return i;
        }
        return -1;
    }

    private Lead.Stato stageToStato(String stage) {
        // Mapping tra stage CRM (BOZZA, QUALIFICATO, etc) e Stato enums esistenti
        switch (stage) {
            case "BOZZA":
                return Lead.Stato.NUOVO;
            case "QUALIFICATO":
                return Lead.Stato.QUALIFICATO;
            case "PROPOSTA":
                return Lead.Stato.CONTATTATO;
            case "NEGOZIAZIONE":
                return Lead.Stato.CONTATTATO;
            case "VINTO":
                return Lead.Stato.QUALIFICATO;
            case "PERSO":
                return Lead.Stato.QUALIFICATO;
            default:
                return Lead.Stato.NUOVO;
        }
    }

    // Getters per i DAO
    public LeadDAO getLeadDAO() { return leadDAO; }
    public ActivityDAO getActivityDAO() { return activityDAO; }
    public TaskDAO getTaskDAO() { return taskDAO; }
    public StoriaLeadDAO getStoriaLeadDAO() { return storiaLeadDAO; }
    public ReminderDAO getReminderDAO() { return reminderDAO; }
}
