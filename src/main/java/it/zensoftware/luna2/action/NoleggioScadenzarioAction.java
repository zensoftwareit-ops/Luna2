package it.zensoftware.luna2.action;

import com.opensymphony.xwork2.ActionSupport;
import it.zensoftware.luna2.dao.CalendarEventDAO;
import it.zensoftware.luna2.dao.NoleggioContrattoDAO;
import it.zensoftware.luna2.model.CalendarAccount;
import it.zensoftware.luna2.model.CalendarEvent;
import it.zensoftware.luna2.model.NoleggioContratto;
import it.zensoftware.luna2.service.NoleggioAutomationService;
import it.zensoftware.luna2.service.NoleggioScadenzarioService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NoleggioScadenzarioAction extends ActionSupport {

    private static final Logger logger = LogManager.getLogger(NoleggioScadenzarioAction.class);
    private static final long serialVersionUID = 1L;

    private final NoleggioContrattoDAO contrattoDAO = new NoleggioContrattoDAO();
    private final CalendarEventDAO calendarEventDAO = new CalendarEventDAO();
    private final NoleggioScadenzarioService scadenzarioService = new NoleggioScadenzarioService();
    private final NoleggioAutomationService automationService = new NoleggioAutomationService();

    private Map<String, Object> jsonResponse = new HashMap<>();
    private List<Map<String, Object>> events = new ArrayList<>();
    private List<Map<String, Object>> contratti = new ArrayList<>();

    private Long contrattoId;
    private String titolo;
    private String descrizione;
    private String dataEvento;

    public String index() {
        return SUCCESS;
    }

    public String summary() {
        try {
            Map<String, Object> summary = new HashMap<>();
            summary.put("expiringRenewals", contrattoDAO.findExpiringWithin(120).size());
            summary.put("kmVerifications", contrattoDAO.findRequiringKmVerification().size());
            summary.put("upcomingRevisioni", contrattoDAO.findWithUpcomingRevisione(30).size());
            summary.put("upcomingTagliandi", contrattoDAO.findWithUpcomingTagliando(15).size());
            summary.put("expiringLicenses", contrattoDAO.findWithExpiringLicense(60).size());
            summary.put("expiringInsurances", contrattoDAO.findWithExpiringInsurance(30).size());
            jsonResponse.put("summary", summary);
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error loading scadenzario summary", e);
            jsonResponse.put("summary", new HashMap<>());
            return ERROR;
        }
    }

    public String expiringRenewals() {
        try {
            for (NoleggioContratto contratto : contrattoDAO.findExpiringWithin(120)) {
                contratti.add(mapContratto(contratto));
            }
            jsonResponse.put("contratti", contratti);
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error loading expiring renewals", e);
            return ERROR;
        }
    }

    public String kmVerifications() {
        try {
            for (NoleggioContratto contratto : contrattoDAO.findRequiringKmVerification()) {
                Map<String, Object> row = mapContratto(contratto);
                row.put("kmAttuali", contratto.getKmAttuali());
                row.put("kmLimite", contratto.getKmTotaliPrevisti());
                Integer kmEcceduti = null;
                if (contratto.getKmAttuali() != null && contratto.getKmTotaliPrevisti() != null) {
                    kmEcceduti = contratto.getKmAttuali() - contratto.getKmTotaliPrevisti();
                }
                row.put("kmEcceduti", kmEcceduti);
                contratti.add(row);
            }
            jsonResponse.put("contratti", contratti);
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error loading km verifications", e);
            return ERROR;
        }
    }

    public String upcomingRevisioni() {
        try {
            for (NoleggioContratto contratto : contrattoDAO.findWithUpcomingRevisione(30)) {
                Map<String, Object> row = mapContratto(contratto);
                row.put("dataProssimaRevisione", contratto.getDataProssimaRevisione());
                row.put("kmProssimaRevisione", contratto.getKmProssimoTagliando());
                contratti.add(row);
            }
            jsonResponse.put("contratti", contratti);
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error loading revisioni", e);
            return ERROR;
        }
    }

    public String upcomingTagliandi() {
        try {
            for (NoleggioContratto contratto : contrattoDAO.findWithUpcomingTagliando(15)) {
                Map<String, Object> row = mapContratto(contratto);
                row.put("dataProssimoTagliando", contratto.getDataProssimoTagliando());
                row.put("kmProssimoTagliando", contratto.getKmProssimoTagliando());
                contratti.add(row);
            }
            jsonResponse.put("contratti", contratti);
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error loading tagliandi", e);
            return ERROR;
        }
    }

    public String calendarEvents() {
        try {
            List<CalendarEvent> list = calendarEventDAO.findAll();
            for (CalendarEvent event : list) {
                Map<String, Object> row = new HashMap<>();
                row.put("titolo", event.getTitle());
                row.put("data", event.getStartTime());
                row.put("sourceType", event.getSourceType().name());
                row.put("utenteAssegnatoId", event.getUserId());
                row.put("sincronizzatoGoogle", event.getProvider() == CalendarAccount.Provider.GOOGLE);
                events.add(row);
            }
            jsonResponse.put("events", events);
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error loading calendar events", e);
            return ERROR;
        }
    }

    public String runAutomation() {
        try {
            scadenzarioService.verificaScadenzeECreaEventiCalendario();
            automationService.runAutomationEngine();
            jsonResponse.put("message", "Automazione completata");
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error running automation", e);
            jsonResponse.put("message", "Errore automazione");
            return ERROR;
        }
    }

    public String createEvent() {
        try {
            if (contrattoId == null) {
                jsonResponse.put("message", "Contratto mancante");
                return ERROR;
            }
            CalendarEvent event = new CalendarEvent();
            event.setUserId("1");
            event.setProvider(CalendarAccount.Provider.GOOGLE);
            event.setTitle(titolo != null ? titolo : "Evento Scadenzario");
            event.setDescription(descrizione);
            LocalDateTime start = LocalDateTime.now();
            if (dataEvento != null && !dataEvento.isEmpty()) {
                Date parsed = java.sql.Date.valueOf(dataEvento);
                start = parsed.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
            }
            event.setStartTime(start);
            event.setEndTime(start.plusHours(1));
            event.setSourceType(CalendarEvent.SourceType.REMINDER);
            event.setSourceId(contrattoId);
            calendarEventDAO.save(event);
            jsonResponse.put("message", "Evento creato");
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error creating calendar event", e);
            jsonResponse.put("message", "Errore creazione evento");
            return ERROR;
        }
    }

    private Map<String, Object> mapContratto(NoleggioContratto contratto) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", contratto.getId());
        row.put("numeroContratto", contratto.getNumeroContratto());
        row.put("targa", contratto.getTarga());
        row.put("dataFine", contratto.getDataFine());
        return row;
    }

    public Map<String, Object> getJsonResponse() {
        return jsonResponse;
    }

    public List<Map<String, Object>> getEvents() {
        return events;
    }

    public List<Map<String, Object>> getContratti() {
        return contratti;
    }

    public Long getContrattoId() {
        return contrattoId;
    }

    public void setContrattoId(Long contrattoId) {
        this.contrattoId = contrattoId;
    }

    public String getTitolo() {
        return titolo;
    }

    public void setTitolo(String titolo) {
        this.titolo = titolo;
    }

    public String getDescrizione() {
        return descrizione;
    }

    public void setDescrizione(String descrizione) {
        this.descrizione = descrizione;
    }

    public String getDataEvento() {
        return dataEvento;
    }

    public void setDataEvento(String dataEvento) {
        this.dataEvento = dataEvento;
    }
}
