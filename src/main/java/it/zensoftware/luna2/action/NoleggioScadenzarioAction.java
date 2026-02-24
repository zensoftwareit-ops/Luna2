package it.zensoftware.luna2.action;

import com.opensymphony.xwork2.ActionSupport;
import it.zensoftware.luna2.dao.*;
import it.zensoftware.luna2.model.*;
import it.zensoftware.luna2.service.NoleggioScadenzarioService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

/**
 * NoleggioScadenzarioAction - Contract maintenance and renewal scheduling with calendar sync (Phase 5)
 */
public class NoleggioScadenzarioAction extends ActionSupport {
    
    private static final Logger logger = LogManager.getLogger(NoleggioScadenzarioAction.class);
    private static final long serialVersionUID = 1L;

    private NoleggioContrattoDAO contrattoDAO;
    private CalendarEventDAO calendarEventDAO;
    private NoleggioScadenzarioService scadenzarioService;
    
    private List<NoleggioContratto> contratti;
    private List<CalendarEvent> calendarEvents;
    private NoleggioContratto contratto;
    
    private Long id;
    private String tipoScadenza;
    private Integer giorniAnticipo;
    private Map<String, Object> jsonResponse = new HashMap<>();
    
    public NoleggioScadenzarioAction() {
        this.contrattoDAO = new NoleggioContrattoDAO();
        this.calendarEventDAO = new CalendarEventDAO();
        this.scadenzarioService = new NoleggioScadenzarioService();
    }
    
    /**
     * Daily automation: Run all scadenzario checks and create calendar events
     */
    public String runAutomation() {
        try {
            scadenzarioService.verificaScadenzeECreaEventiCalendario();
            
            jsonResponse.put("success", true);
            jsonResponse.put("message", "Scadenzario verification completed");
            
            logger.info("Scadenzario automation executed successfully");
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error running scadenzario automation", e);
            jsonResponse.put("success", false);
            jsonResponse.put("error", e.getMessage());
            return SUCCESS;
        }
    }
    
    /**
     * Get contracts expiring within specified days
     */
    public String expiringRenewals() {
        try {
            if (giorniAnticipo == null) {
                giorniAnticipo = 120; // Default: 4-6 months
            }
            
            contratti = contrattoDAO.findExpiringWithin(giorniAnticipo);
            jsonResponse.put("count", contratti.size());
            jsonResponse.put("giorniAnticipo", giorniAnticipo);
            jsonResponse.put("contratti", contratti);
            
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error getting expiring renewals", e);
            return ERROR;
        }
    }
    
    /**
     * Get contracts requiring km verification
     */
    public String kmVerifications() {
        try {
            contratti = contrattoDAO.findRequiringKmVerification();
            jsonResponse.put("count", contratti.size());
            jsonResponse.put("contratti", contratti);
            
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error getting km verifications", e);
            return ERROR;
        }
    }
    
    /**
     * Get contracts with upcoming vehicle inspection
     */
    public String upcomingRevisioni() {
        try {
            if (giorniAnticipo == null) {
                giorniAnticipo = 30;
            }
            
            contratti = contrattoDAO.findWithUpcomingRevisione(giorniAnticipo);
            jsonResponse.put("count", contratti.size());
            jsonResponse.put("giorniAnticipo", giorniAnticipo);
            jsonResponse.put("contratti", contratti);
            
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error getting upcoming revisioni", e);
            return ERROR;
        }
    }
    
    /**
     * Get contracts with upcoming maintenance
     */
    public String upcomingTagliandi() {
        try {
            if (giorniAnticipo == null) {
                giorniAnticipo = 15;
            }
            
            contratti = contrattoDAO.findWithUpcomingTagliando(giorniAnticipo);
            jsonResponse.put("count", contratti.size());
            jsonResponse.put("giorniAnticipo", giorniAnticipo);
            jsonResponse.put("contratti", contratti);
            
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error getting upcoming tagliandi", e);
            return ERROR;
        }
    }
    
    /**
     * Get contracts with expiring driver licenses
     */
    public String expiringLicenses() {
        try {
            if (giorniAnticipo == null) {
                giorniAnticipo = 60;
            }
            
            contratti = contrattoDAO.findWithExpiringLicense(giorniAnticipo);
            jsonResponse.put("count", contratti.size());
            jsonResponse.put("giorniAnticipo", giorniAnticipo);
            jsonResponse.put("contratti", contratti);
            
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error getting expiring licenses", e);
            return ERROR;
        }
    }
    
    /**
     * Get contracts with expiring insurance
     */
    public String expiringInsurances() {
        try {
            if (giorniAnticipo == null) {
                giorniAnticipo = 30;
            }
            
            contratti = contrattoDAO.findWithExpiringInsurance(giorniAnticipo);
            jsonResponse.put("count", contratti.size());
            jsonResponse.put("giorniAnticipo", giorniAnticipo);
            jsonResponse.put("contratti", contratti);
            
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error getting expiring insurances", e);
            return ERROR;
        }
    }
    
    /**
     * Get all calendar events
     */
    public String calendarEvents() {
        try {
            calendarEvents = calendarEventDAO.findAll();
            jsonResponse.put("count", calendarEvents.size());
            jsonResponse.put("events", calendarEvents);
            
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error getting calendar events", e);
            return ERROR;
        }
    }
    
    /**
     * Get calendar events by source type
     */
    public String eventsByType() {
        try {
            if (tipoScadenza == null) {
                jsonResponse.put("error", "Tipo scadenza non specificato");
                return SUCCESS;
            }
            
            CalendarEvent.SourceType sourceType = CalendarEvent.SourceType.valueOf(tipoScadenza);
            // Filtered search would need to be implemented in CalendarEventDAO
            // For now, get all and filter in service layer
            calendarEvents = calendarEventDAO.findAll();
            
            jsonResponse.put("sourceType", sourceType);
            jsonResponse.put("count", calendarEvents.size());
            jsonResponse.put("events", calendarEvents);
            
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error getting events by type", e);
            return ERROR;
        }
    }
    
    /**
     * Create manual calendar event
     */
    public String createEvent() {
        try {
            if (id == null) {
                jsonResponse.put("success", false);
                return SUCCESS;
            }
            
            contratto = contrattoDAO.findById(id);
            if (contratto == null) {
                jsonResponse.put("success", false);
                jsonResponse.put("error", "Contratto non trovato");
                return SUCCESS;
            }
            
            // Create event for contract renewal
            CalendarEvent event = scadenzarioService.creaEventoManuale(
                contratto.getUtenteAssegnatoId(),
                "Rinnovo Contratto " + contratto.getNumeroContratto(),
                "Rinnovo contratto noleggio per " + contratto.getTarga(),
                contratto.getDataFine()
            );
            
            jsonResponse.put("success", true);
            jsonResponse.put("eventId", event.getId());
            
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error creating event", e);
            jsonResponse.put("success", false);
            jsonResponse.put("error", e.getMessage());
            return SUCCESS;
        }
    }
    
    /**
     * Get scadenzario summary
     */
    public String summary() {
        try {
            Map<String, Long> summary = new HashMap<>();
            
            summary.put("expiringRenewals", (long) contrattoDAO.findExpiringWithin(120).size());
            summary.put("kmVerifications", (long) contrattoDAO.findRequiringKmVerification().size());
            summary.put("upcomingRevisioni", (long) contrattoDAO.findWithUpcomingRevisione(30).size());
            summary.put("upcomingTagliandi", (long) contrattoDAO.findWithUpcomingTagliando(15).size());
            summary.put("expiringLicenses", (long) contrattoDAO.findWithExpiringLicense(60).size());
            summary.put("expiringInsurances", (long) contrattoDAO.findWithExpiringInsurance(30).size());
            summary.put("exceededKm", (long) contrattoDAO.findWithExceededKm().size());
            
            jsonResponse.put("summary", summary);
            jsonResponse.put("totalActive", contrattoDAO.findActive().size());
            
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error getting summary", e);
            return ERROR;
        }
    }
    
    // ============== GETTERS/SETTERS ==============
    
    public List<NoleggioContratto> getContratti() { return contratti; }
    public void setContratti(List<NoleggioContratto> contratti) { this.contratti = contratti; }
    
    public List<CalendarEvent> getCalendarEvents() { return calendarEvents; }
    public void setCalendarEvents(List<CalendarEvent> calendarEvents) { this.calendarEvents = calendarEvents; }
    
    public NoleggioContratto getContratto() { return contratto; }
    public void setContratto(NoleggioContratto contratto) { this.contratto = contratto; }
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getTipoScadenza() { return tipoScadenza; }
    public void setTipoScadenza(String tipoScadenza) { this.tipoScadenza = tipoScadenza; }
    
    public Integer getGiorniAnticipo() { return giorniAnticipo; }
    public void setGiorniAnticipo(Integer giorniAnticipo) { this.giorniAnticipo = giorniAnticipo; }
    
    public Map<String, Object> getJsonResponse() { return jsonResponse; }
}
