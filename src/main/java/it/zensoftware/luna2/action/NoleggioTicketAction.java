package it.zensoftware.luna2.action;

import com.opensymphony.xwork2.ActionSupport;
import it.zensoftware.luna2.dao.NoleggioTicketDAO;
import it.zensoftware.luna2.model.NoleggioTicket;
import it.zensoftware.luna2.service.NoleggioTicketService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NoleggioTicketAction extends ActionSupport {

    private static final Logger logger = LogManager.getLogger(NoleggioTicketAction.class);
    private static final long serialVersionUID = 1L;

    private final NoleggioTicketDAO ticketDAO = new NoleggioTicketDAO();
    private final NoleggioTicketService ticketService = new NoleggioTicketService();

    private List<Map<String, Object>> tickets = new ArrayList<>();
    private Map<String, Object> jsonResponse = new HashMap<>();

    private Long id;
    private String statusFiltro;
    private String prioritaFiltro;
    private String searchTerm;
    private String motivo;
    private String soluzioneAdottata;

    public String index() {
        return SUCCESS;
    }

    public String list() {
        try {
            List<NoleggioTicket> items = ticketDAO.findAll();
            for (NoleggioTicket ticket : items) {
                if (!matchesFilter(ticket)) {
                    continue;
                }
                Map<String, Object> row = new HashMap<>();
                row.put("id", ticket.getId());
                row.put("numeroTicket", ticket.getNumeroTicket());
                row.put("oggetto", ticket.getOggetto());
                row.put("priorita", ticket.getPriorita() != null ? ticket.getPriorita().name() : "-");
                row.put("status", ticket.getStatus() != null ? ticket.getStatus().name() : "-");
                row.put("operatoreAssegnatoId", ticket.getOperatoreAssegnatoId());
                row.put("dataCreazione", formatDateTime(ticket.getDataApertura()));
                tickets.add(row);
            }
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error listing tickets", e);
            jsonResponse.put("message", "Errore nel caricamento ticket");
            return ERROR;
        }
    }

    public String antiBounceAlerts() {
        try {
            jsonResponse.put("count", ticketDAO.findWithAntiBounce().size());
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error loading anti-bounce alerts", e);
            jsonResponse.put("count", 0);
            return ERROR;
        }
    }

    public String slaExceeded() {
        try {
            jsonResponse.put("slaRispostaCount", ticketDAO.findWithSlaRispostaExceeded().size());
            jsonResponse.put("slaRisoluzioneCount", ticketDAO.findWithSlaRisoluzioneExceeded().size());
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error loading SLA alerts", e);
            jsonResponse.put("slaRispostaCount", 0);
            jsonResponse.put("slaRisoluzioneCount", 0);
            return ERROR;
        }
    }

    public String view() {
        try {
            if (id == null) {
                jsonResponse.put("message", "ID mancante");
                return ERROR;
            }
            NoleggioTicket ticket = ticketDAO.findById(id);
            if (ticket == null) {
                jsonResponse.put("message", "Ticket non trovato");
                return ERROR;
            }
            jsonResponse.put("id", ticket.getId());
            jsonResponse.put("numeroTicket", ticket.getNumeroTicket());
            jsonResponse.put("priorita", ticket.getPriorita() != null ? ticket.getPriorita().name() : "-");
            jsonResponse.put("status", ticket.getStatus() != null ? ticket.getStatus().name() : "-");
            jsonResponse.put("oggetto", ticket.getOggetto());
            jsonResponse.put("descrizione", ticket.getDescrizione());
            jsonResponse.put("operatoreAssegnatoId", ticket.getOperatoreAssegnatoId());
            jsonResponse.put("soluzioneAdottata", ticket.getSoluzioneApplicata());
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error viewing ticket", e);
            jsonResponse.put("message", "Errore nel caricamento ticket");
            return ERROR;
        }
    }

    public String reopen() {
        try {
            if (id == null) {
                jsonResponse.put("message", "ID mancante");
                return ERROR;
            }
            ticketService.reopenTicket(id, motivo, 1L);
            jsonResponse.put("message", "Ticket riaperto");
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error reopening ticket", e);
            jsonResponse.put("message", "Errore riapertura ticket");
            return ERROR;
        }
    }

    public String close() {
        try {
            if (id == null) {
                jsonResponse.put("message", "ID mancante");
                return ERROR;
            }
            ticketService.closeTicket(id, soluzioneAdottata, 1L);
            jsonResponse.put("message", "Ticket chiuso");
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error closing ticket", e);
            jsonResponse.put("message", "Errore chiusura ticket");
            return ERROR;
        }
    }

    public String escalate() {
        try {
            if (id == null) {
                jsonResponse.put("message", "ID mancante");
                return ERROR;
            }
            NoleggioTicket ticket = ticketDAO.findById(id);
            if (ticket == null) {
                jsonResponse.put("message", "Ticket non trovato");
                return ERROR;
            }
            ticket.setStatus(NoleggioTicket.Status.ESCALATO);
            ticket.setMotivazioneRiapertura(motivo);
            ticketDAO.update(ticket);
            jsonResponse.put("message", "Ticket escalato");
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error escalating ticket", e);
            jsonResponse.put("message", "Errore escalation ticket");
            return ERROR;
        }
    }

    private boolean matchesFilter(NoleggioTicket ticket) {
        if (statusFiltro != null && !statusFiltro.isEmpty()) {
            if (ticket.getStatus() == null || !ticket.getStatus().name().equals(statusFiltro)) {
                return false;
            }
        }
        if (prioritaFiltro != null && !prioritaFiltro.isEmpty()) {
            if (ticket.getPriorita() == null || !ticket.getPriorita().name().equals(prioritaFiltro)) {
                return false;
            }
        }
        if (searchTerm != null && !searchTerm.isEmpty()) {
            String value = searchTerm.toLowerCase();
            return (ticket.getNumeroTicket() != null && ticket.getNumeroTicket().toLowerCase().contains(value))
                || (ticket.getOggetto() != null && ticket.getOggetto().toLowerCase().contains(value));
        }
        return true;
    }

    private String formatDateTime(Date date) {
        if (date == null) {
            return "";
        }
        return new SimpleDateFormat("dd/MM/yyyy HH:mm").format(date);
    }

    public List<Map<String, Object>> getTickets() {
        return tickets;
    }

    public Map<String, Object> getJsonResponse() {
        return jsonResponse;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStatusFiltro() {
        return statusFiltro;
    }

    public void setStatusFiltro(String statusFiltro) {
        this.statusFiltro = statusFiltro;
    }

    public String getPrioritaFiltro() {
        return prioritaFiltro;
    }

    public void setPrioritaFiltro(String prioritaFiltro) {
        this.prioritaFiltro = prioritaFiltro;
    }

    public String getSearchTerm() {
        return searchTerm;
    }

    public void setSearchTerm(String searchTerm) {
        this.searchTerm = searchTerm;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public String getSoluzioneAdottata() {
        return soluzioneAdottata;
    }

    public void setSoluzioneAdottata(String soluzioneAdottata) {
        this.soluzioneAdottata = soluzioneAdottata;
    }
}
