package it.zensoftware.luna2.service;

import it.zensoftware.luna2.dao.NoleggioTicketDAO;
import it.zensoftware.luna2.dao.NoleggioContrattoDAO;
import it.zensoftware.luna2.model.NoleggioTicket;
import it.zensoftware.luna2.model.NoleggioContratto;
import it.zensoftware.luna2.service.notification.PushNotificationService;
import it.zensoftware.luna2.service.notification.event.NotificationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

/**
 * NoleggioTicketService - After-sales ticketing with anti-bounce detection
 * Manages Phase 4: POST_VENDITA with SLA tracking
 */
public class NoleggioTicketService {
    
    private static final Logger logger = LogManager.getLogger(NoleggioTicketService.class);
    
    private final NoleggioTicketDAO ticketDAO;
    private final NoleggioContrattoDAO contrattoDAO;
    private final PushNotificationService notificationService;
    
    // SLA defaults (hours)
    private static final Map<NoleggioTicket.Priorita, Integer> SLA_RISPOSTA = new HashMap<NoleggioTicket.Priorita, Integer>() {{
        put(NoleggioTicket.Priorita.URGENTE, 2);
        put(NoleggioTicket.Priorita.ALTA, 4);
        put(NoleggioTicket.Priorita.MEDIA, 8);
        put(NoleggioTicket.Priorita.BASSA, 24);
    }};
    
    private static final Map<NoleggioTicket.Priorita, Integer> SLA_RISOLUZIONE = new HashMap<NoleggioTicket.Priorita, Integer>() {{
        put(NoleggioTicket.Priorita.URGENTE, 4);
        put(NoleggioTicket.Priorita.ALTA, 8);
        put(NoleggioTicket.Priorita.MEDIA, 24);
        put(NoleggioTicket.Priorita.BASSA, 72);
    }};
    
    public NoleggioTicketService() {
        this.ticketDAO = new NoleggioTicketDAO();
        this.contrattoDAO = new NoleggioContrattoDAO();
        this.notificationService = PushNotificationService.getInstance();
    }
    
    /**
     * Create new after-sales ticket
     */
    public NoleggioTicket createTicket(Long contrattoId, NoleggioTicket ticket, Long userId) {
        NoleggioContratto contratto = contrattoDAO.findById(contrattoId);
        if (contratto == null) {
            throw new IllegalArgumentException("Contratto not found");
        }
        
        ticket.setContratto(contratto);
        ticket.setDataApertura(new Date());
        ticket.setUtenteCreazioneId(userId);
        ticket.setNumeroTicket(generaNumeroTicket());
        ticket.setStatus(NoleggioTicket.Status.APERTO);
        ticket.setFlagAntiRimbalzo(false);
        ticket.setSlaRispostaScaduto(false);
        ticket.setSlaRisoluzioneScaduto(false);
        
        // Set SLA based on priority
        if (ticket.getPriorita() != null) {
            ticket.setSlaRispostaOre(SLA_RISPOSTA.get(ticket.getPriorita()));
            ticket.setSlaRisoluzioneOre(SLA_RISOLUZIONE.get(ticket.getPriorita()));
        }
        
        ticketDAO.save(ticket);
        logger.info("Created ticket: " + ticket.getNumeroTicket());
        
        // Notify operator if assigned
        if (ticket.getOperatoreAssegnatoId() != null) {
            notificationService.sendNotificationToUser(
                String.valueOf(ticket.getOperatoreAssegnatoId()),
                "Nuovo Ticket Assegnato",
                "Ticket " + ticket.getNumeroTicket() + " - " + ticket.getCategoria(),
                getPriorityLevel(ticket.getPriorita())
            );
        }
        
        return ticket;
    }
    
    /**
     * Assign ticket to operator
     */
    public NoleggioTicket assignTicket(Long ticketId, Long operatoreId, Long userId) {
        NoleggioTicket ticket = ticketDAO.findById(ticketId);
        if (ticket == null) {
            throw new IllegalArgumentException("Ticket not found");
        }
        
        ticket.setOperatoreAssegnatoId(operatoreId);
        ticket.setStatus(NoleggioTicket.Status.ASSEGNATO);
        ticket.setDataModifica(new Date());
        ticketDAO.update(ticket);
        
        // Notify operator
        notificationService.sendNotificationToUser(
            String.valueOf(operatoreId),
            "Ticket Assegnato",
            "Ticket " + ticket.getNumeroTicket() + " - " + ticket.getOggetto(),
            getPriorityLevel(ticket.getPriorita())
        );
        
        logger.info("Assigned ticket " + ticket.getNumeroTicket() + " to operator " + operatoreId);
        return ticket;
    }
    
    /**
     * Update ticket status with workflow transitions
     */
    public NoleggioTicket updateStatus(Long ticketId, NoleggioTicket.Status newStatus, String note, Long userId) {
        NoleggioTicket ticket = ticketDAO.findById(ticketId);
        if (ticket == null) {
            throw new IllegalArgumentException("Ticket not found");
        }
        
        NoleggioTicket.Status oldStatus = ticket.getStatus();
        ticket.setStatus(newStatus);
        ticket.setDataModifica(new Date());
        
        // Track status transitions
        if (newStatus == NoleggioTicket.Status.IN_LAVORAZIONE && ticket.getDataPrimaRisposta() == null) {
            ticket.setDataPrimaRisposta(new Date());
            logger.info("First response for ticket: " + ticket.getNumeroTicket());
        }
        
        if (newStatus == NoleggioTicket.Status.RISOLTO) {
            ticket.setDataRisoluzione(new Date());
            logger.info("Resolved ticket: " + ticket.getNumeroTicket());
        }
        
        if (note != null && !note.isEmpty()) {
            String currentNote = ticket.getNote() != null ? ticket.getNote() : "";
            ticket.setNote(currentNote + "\n[" + new Date() + "] " + note);
        }
        
        ticketDAO.update(ticket);
        return ticket;
    }
    
    /**
     * Close ticket
     */
    public NoleggioTicket closeTicket(Long ticketId, String soluzioneAdottata, Long userId) {
        NoleggioTicket ticket = ticketDAO.findById(ticketId);
        if (ticket == null) {
            throw new IllegalArgumentException("Ticket not found");
        }
        
        if (ticket.getStatus() != NoleggioTicket.Status.RISOLTO) {
            throw new IllegalStateException("Ticket must be RISOLTO before closing");
        }
        
        ticket.setStatus(NoleggioTicket.Status.CHIUSO);
        ticket.setDataChiusura(new Date());
        ticket.setSoluzioneAdottata(soluzioneAdottata);
        ticketDAO.update(ticket);
        
        logger.info("Closed ticket: " + ticket.getNumeroTicket());
        return ticket;
    }
    
    /**
     * Reopen ticket with anti-bounce detection (48-72h window)
     */
    public NoleggioTicket reopenTicket(Long ticketId, String motivo, Long userId) {
        NoleggioTicket ticket = ticketDAO.findById(ticketId);
        if (ticket == null) {
            throw new IllegalArgumentException("Ticket not found");
        }
        
        if (ticket.getStatus() != NoleggioTicket.Status.CHIUSO) {
            throw new IllegalStateException("Can only reopen CHIUSO tickets");
        }
        
        // Check anti-bounce window (48-72h)
        Date now = new Date();
        long hoursSinceClosure = (now.getTime() - ticket.getDataChiusura().getTime()) / (1000 * 60 * 60);
        
        boolean isAntiBounce = (hoursSinceClosure >= 48 && hoursSinceClosure <= 72);
        
        ticket.riapri();
        ticket.setDataModifica(now);
        
        if (isAntiBounce) {
            ticket.setFlagAntiRimbalzo(true);
            ticket.setStatus(NoleggioTicket.Status.ESCALATO);
            
            // Escalate to manager
            logger.warn("ANTI-RIMBALZO detected for ticket " + ticket.getNumeroTicket() + 
                       " - Reopened after " + hoursSinceClosure + " hours");
            
            // Notify manager (assuming userId is manager for now)
            notificationService.sendNotificationToUser(
                String.valueOf(userId),
                "⚠️ Anti-Rimbalzo Alert",
                "Ticket " + ticket.getNumeroTicket() + " riaperto dopo " + hoursSinceClosure + "h",
                NotificationEvent.Priority.URGENT
            );
        } else {
            ticket.setStatus(NoleggioTicket.Status.RIAPERTO);
        }
        
        String currentNote = ticket.getNote() != null ? ticket.getNote() : "";
        ticket.setNote(currentNote + "\n[RIAPERTO " + now + "] " + motivo);
        
        ticketDAO.update(ticket);
        logger.info("Reopened ticket: " + ticket.getNumeroTicket() + " (anti-rimbalzo: " + isAntiBounce + ")");
        
        return ticket;
    }
    
    /**
     * Escalate ticket to manager
     */
    public NoleggioTicket escalateTicket(Long ticketId, String motivo, Long managerId) {
        NoleggioTicket ticket = ticketDAO.findById(ticketId);
        if (ticket == null) {
            throw new IllegalArgumentException("Ticket not found");
        }
        
        ticket.setStatus(NoleggioTicket.Status.ESCALATO);
        ticket.setPriorita(NoleggioTicket.Priorita.URGENTE);
        ticket.setDataModifica(new Date());
        
        String currentNote = ticket.getNote() != null ? ticket.getNote() : "";
        ticket.setNote(currentNote + "\n[ESCALATO " + new Date() + "] " + motivo);
        
        ticketDAO.update(ticket);
        
        // Notify manager
        notificationService.sendNotificationToUser(
            String.valueOf(managerId),
            "Ticket Escalato",
            "Ticket " + ticket.getNumeroTicket() + " - " + ticket.getOggetto(),
            NotificationEvent.Priority.URGENT
        );
        
        logger.info("Escalated ticket: " + ticket.getNumeroTicket());
        return ticket;
    }
    
    /**
     * Check and update SLA violations
     */
    public void checkSlaViolations(NoleggioTicket ticket) {
        Date now = new Date();
        
        // Check response SLA
        if (ticket.getDataPrimaRisposta() == null && !ticket.isSlaRispostaScaduto()) {
            long hoursSinceOpening = (now.getTime() - ticket.getDataApertura().getTime()) / (1000 * 60 * 60);
            if (hoursSinceOpening > ticket.getSlaRispostaOre()) {
                ticket.setSlaRispostaScaduto(true);
                ticketDAO.update(ticket);
                
                logger.warn("SLA risposta exceeded for ticket: " + ticket.getNumeroTicket());
                
                // Notify operator and manager
                if (ticket.getOperatoreAssegnatoId() != null) {
                    notificationService.sendNotificationToUser(
                        String.valueOf(ticket.getOperatoreAssegnatoId()),
                        "⚠️ SLA Risposta Scaduto",
                        "Ticket " + ticket.getNumeroTicket(),
                        NotificationEvent.Priority.HIGH
                    );
                }
            }
        }
        
        // Check resolution SLA
        if (ticket.getDataRisoluzione() == null && !ticket.isSlaRisoluzioneScaduto()) {
            long hoursSinceOpening = (now.getTime() - ticket.getDataApertura().getTime()) / (1000 * 60 * 60);
            if (hoursSinceOpening > ticket.getSlaRisoluzioneOre()) {
                ticket.setSlaRisoluzioneScaduto(true);
                ticketDAO.update(ticket);
                
                logger.warn("SLA risoluzione exceeded for ticket: " + ticket.getNumeroTicket());
            }
        }
    }
    
    // ==================== UTILITIES ====================
    
    private String generaNumeroTicket() {
        return "TKT" + System.currentTimeMillis();
    }
    
    private NotificationEvent.Priority getPriorityLevel(NoleggioTicket.Priorita priorita) {
        if (priorita == null) return NotificationEvent.Priority.NORMAL;
        
        switch (priorita) {
            case URGENTE: return NotificationEvent.Priority.URGENT;
            case ALTA: return NotificationEvent.Priority.HIGH;
            case MEDIA: return NotificationEvent.Priority.NORMAL;
            case BASSA: return NotificationEvent.Priority.LOW;
            default: return NotificationEvent.Priority.NORMAL;
        }
    }
}
