package it.zensoftware.luna2.action;

import com.opensymphony.xwork2.ActionSupport;
import it.zensoftware.luna2.dao.*;
import it.zensoftware.luna2.model.*;
import it.zensoftware.luna2.service.NoleggioTicketService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

/**
 * NoleggioTicketAction - After-sales ticketing with anti-bounce detection (Phase 4)
 */
public class NoleggioTicketAction extends ActionSupport {
    
    private static final Logger logger = LogManager.getLogger(NoleggioTicketAction.class);
    private static final long serialVersionUID = 1L;

    private NoleggioTicketDAO ticketDAO;
    private NoleggioContrattoDAO contrattoDAO;
    private NoleggioTicketService ticketService;
    
    private NoleggioTicket ticket;
    private List<NoleggioTicket> tickets;
    private NoleggioContratto contratto;
    
    private Long id;
    private Long contrattoId;
    private Long operatoreId;
    private String statusFiltro;
    private String prioritaFiltro;
    private String oggetto;
    private String descrizione;
    private String soluzioneAdottata;
    private String motivo;
    private Map<String, Object> jsonResponse = new HashMap<>();
    
    public NoleggioTicketAction() {
        this.ticketDAO = new NoleggioTicketDAO();
        this.contrattoDAO = new NoleggioContrattoDAO();
        this.ticketService = new NoleggioTicketService();
    }
    
    public String list() {
        try {
            if (contrattoId != null) {
                tickets = ticketDAO.findByContratto(contrattoId);
                contratto = contrattoDAO.findById(contrattoId);
            } else {
                tickets = ticketDAO.findByStatus(NoleggioTicket.Status.APERTO);
            }
            
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error listing tickets", e);
            addActionError("Errore nel caricamento dei ticket");
            return ERROR;
        }
    }
    
    public String view() {
        try {
            if (id == null) {
                addActionError("ID non valido");
                return ERROR;
            }
            
            ticket = ticketDAO.findById(id);
            if (ticket == null) {
                addActionError("Ticket non trovato");
                return ERROR;
            }
            
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error viewing ticket", e);
            addActionError("Errore nel caricamento");
            return ERROR;
        }
    }
    
    public String create() {
        try {
            if (contrattoId != null) {
                contratto = contrattoDAO.findById(contrattoId);
            }
            ticket = new NoleggioTicket();
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error creating ticket", e);
            return ERROR;
        }
    }
    
    public String save() {
        try {
            if (contrattoId == null) {
                addActionError("Contratto non trovato");
                return ERROR;
            }
            
            User currentUser = getCurrentUser();
            
            if (ticket.getId() == null) {
                ticket = ticketService.createTicket(contrattoId, ticket, 
                                                   currentUser != null ? currentUser.getId() : 1L);
                addActionMessage("Ticket creato: " + ticket.getNumeroTicket());
            } else {
                ticketDAO.update(ticket);
                addActionMessage("Ticket aggiornato");
            }
            
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error saving ticket", e);
            addActionError("Errore nel salvataggio: " + e.getMessage());
            return INPUT;
        }
    }
    
    public String assign() {
        try {
            if (id == null || operatoreId == null) {
                jsonResponse.put("success", false);
                return SUCCESS;
            }
            
            ticket = ticketService.assignTicket(id, operatoreId, 
                                               getCurrentUser() != null ? getCurrentUser().getId() : 1L);
            
            jsonResponse.put("success", true);
            jsonResponse.put("operatoreId", ticket.getOperatoreAssegnatoId());
            
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error assigning ticket", e);
            jsonResponse.put("success", false);
            jsonResponse.put("error", e.getMessage());
            return SUCCESS;
        }
    }
    
    public String updateStatus() {
        try {
            if (id == null || statusFiltro == null) {
                jsonResponse.put("success", false);
                return SUCCESS;
            }
            
            NoleggioTicket.Status newStatus = NoleggioTicket.Status.valueOf(statusFiltro);
            ticket = ticketService.updateStatus(id, newStatus, null, 
                                               getCurrentUser() != null ? getCurrentUser().getId() : 1L);
            
            jsonResponse.put("success", true);
            jsonResponse.put("status", ticket.getStatus().toString());
            
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error updating status", e);
            jsonResponse.put("success", false);
            jsonResponse.put("error", e.getMessage());
            return SUCCESS;
        }
    }
    
    public String close() {
        try {
            if (id == null) {
                jsonResponse.put("success", false);
                return SUCCESS;
            }
            
            ticket = ticketService.closeTicket(id, soluzioneAdottata, 
                                              getCurrentUser() != null ? getCurrentUser().getId() : 1L);
            
            jsonResponse.put("success", true);
            jsonResponse.put("message", "Ticket chiuso");
            
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error closing ticket", e);
            jsonResponse.put("success", false);
            jsonResponse.put("error", e.getMessage());
            return SUCCESS;
        }
    }
    
    public String reopen() {
        try {
            if (id == null) {
                jsonResponse.put("success", false);
                return SUCCESS;
            }
            
            ticket = ticketService.reopenTicket(id, motivo, 
                                               getCurrentUser() != null ? getCurrentUser().getId() : 1L);
            
            jsonResponse.put("success", true);
            jsonResponse.put("status", ticket.getStatus().toString());
            jsonResponse.put("antiBounce", ticket.isFlagAntiRimbalzo());
            
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error reopening ticket", e);
            jsonResponse.put("success", false);
            jsonResponse.put("error", e.getMessage());
            return SUCCESS;
        }
    }
    
    public String escalate() {
        try {
            if (id == null) {
                jsonResponse.put("success", false);
                return SUCCESS;
            }
            
            ticket = ticketService.escalateTicket(id, motivo, 
                                                 getCurrentUser() != null ? getCurrentUser().getId() : 1L);
            
            jsonResponse.put("success", true);
            jsonResponse.put("status", ticket.getStatus().toString());
            
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error escalating ticket", e);
            jsonResponse.put("success", false);
            jsonResponse.put("error", e.getMessage());
            return SUCCESS;
        }
    }
    
    public String antiBounceAlerts() {
        try {
            List<NoleggioTicket> withBounce = ticketDAO.findWithAntiBounce();
            jsonResponse.put("count", withBounce.size());
            jsonResponse.put("tickets", withBounce);
            
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error getting anti-bounce alerts", e);
            return ERROR;
        }
    }
    
    public String slaExceeded() {
        try {
            List<NoleggioTicket> exceededRisposta = ticketDAO.findWithSlaRispostaExceeded();
            List<NoleggioTicket> exceededRisoluzione = ticketDAO.findWithSlaRisoluzioneExceeded();
            
            jsonResponse.put("slaRispostaCount", exceededRisposta.size());
            jsonResponse.put("slaRisoluzioneCount", exceededRisoluzione.size());
            
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error getting SLA exceeded", e);
            return ERROR;
        }
    }
    
    // ============== GETTERS/SETTERS ==============
    
    public NoleggioTicket getTicket() { return ticket; }
    public void setTicket(NoleggioTicket ticket) { this.ticket = ticket; }
    
    public List<NoleggioTicket> getTickets() { return tickets; }
    public void setTickets(List<NoleggioTicket> tickets) { this.tickets = tickets; }
    
    public NoleggioContratto getContratto() { return contratto; }
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getContrattoId() { return contrattoId; }
    public void setContrattoId(Long contrattoId) { this.contrattoId = contrattoId; }
    
    public Long getOperatoreId() { return operatoreId; }
    public void setOperatoreId(Long operatoreId) { this.operatoreId = operatoreId; }
    
    public String getStatusFiltro() { return statusFiltro; }
    public void setStatusFiltro(String statusFiltro) { this.statusFiltro = statusFiltro; }
    
    public String getPriotaFiltro() { return prioritaFiltro; }
    public void setPrioritaFiltro(String prioritaFiltro) { this.prioritaFiltro = prioritaFiltro; }
    
    public String getOggetto() { return oggetto; }
    public void setOggetto(String oggetto) { this.oggetto = oggetto; }
    
    public String getDescrizione() { return descrizione; }
    public void setDescrizione(String descrizione) { this.descrizione = descrizione; }
    
    public String getSoluzioneAdottata() { return soluzioneAdottata; }
    public void setSoluzioneAdottata(String soluzioneAdottata) { this.soluzioneAdottata = soluzioneAdottata; }
    
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
    
    public Map<String, Object> getJsonResponse() { return jsonResponse; }
    
    private User getCurrentUser() {
        Map<String, Object> session = com.opensymphony.xwork2.ActionContext.getContext().getSession();
        return (User) session.get("currentUser");
    }
}
