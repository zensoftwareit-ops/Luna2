package it.zensoftware.luna2.service;

import it.zensoftware.luna2.dao.*;
import it.zensoftware.luna2.model.*;
import it.zensoftware.luna2.service.notification.PushNotificationService;
import it.zensoftware.luna2.service.notification.event.NotificationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

/**
 * NoleggioAutomationService - Core automation engine for broker rental CRM
 * Runs via cron jobs to execute business rules from PDF requirements:
 * - 48h quote follow-up
 * - 4-5 day document solicitation
 * - 20-30 day care calls
 * - 48-72h anti-bounce ticket detection
 */
public class NoleggioAutomationService {
    
    private static final Logger logger = LogManager.getLogger(NoleggioAutomationService.class);
    
    private final NolleggioPreventivoDAO preventivoDAO;
    private final NoleggioDocumentoDAO documentoDAO;
    private final NoleggioValutazioneDAO valutazioneDAO;
    private final NoleggioOrdineDAO ordineDAO;
    private final NoleggioTicketDAO ticketDAO;
    private readonly NoleggioNBTDAO nbtDAO;
    
    private final PushNotificationService notificationService;
    private final NoleggioLeadService leadService;
    private final NoleggioTicketService ticketService;
    private final NoleggioNBTService nbtService;
    
    public NoleggioAutomationService() {
        this.preventivoDAO = new NolleggioPreventivoDAO();
        this.documentoDAO = new NoleggioDocumentoDAO();
        this.valutazioneDAO = new NoleggioValutazioneDAO();
        this.ordineDAO = new NoleggioOrdineDAO();
        this.ticketDAO = new NoleggioTicketDAO();
        this.nbtDAO = new NoleggioNBTDAO();
        this.notificationService = PushNotificationService.getInstance();
        this.leadService = new NoleggioLeadService();
        this.ticketService = new NoleggioTicketService();
        this.nbtService = new NoleggioNBTService();
    }
    
    /**
     * Main automation scheduler (called via cron every hour)
     */
    public void runAutomationEngine() {
        logger.info("=== Starting Noleggio Automation Engine ===");
        
        try {
            // Every hour checks
            verificaFollowupPreventivi();
            verificaDocumentiScaduti();
            verificaSollecitiDocumenti();
            verificaValutazioniBloccate();
            
            // Every 2 hours
            verificaCareCall();
            
            // Every 4 hours
            verificaAntiRimbalzo();
            verificaNBTFollowup24h();
            verificaNBTRestituzione();
            
            logger.info("=== Automation Engine completed ===");
        } catch (Exception e) {
            logger.error("Critical error in automation engine", e);
        }
    }
    
    /**
     * FASE 1: Check quotes requiring 48h follow-up
     * Business rule: If quote sent 48h ago without response, send follow-up notification
     */
    public void verificaFollowupPreventivi() {
        try {
            List<NoleggioPreventivo> requireFollowup = preventivoDAO.findRequiringFollowup();
            
            logger.info("Found " + requireFollowup.size() + " quotes requiring 48h follow-up");
            
            for (NoleggioPreventivo preventivo : requireFollowup) {
                try {
                    // Send notification to lead's assigned user
                    if (preventivo.getLead().getUtenteAssegnatoId() != null) {
                        notificationService.sendNotificationToUser(
                            String.valueOf(preventivo.getLead().getUtenteAssegnatoId()),
                            "Preventivo Senza Risposta",
                            "Preventivo " + preventivo.getNumeroPreventivo() + 
                            " per " + preventivo.getLead().getNomeCliente() + 
                            " non è stato confermato dopo 48h",
                            NotificationEvent.Priority.HIGH
                        );
                    }
                    
                    // Mark follow-up sent
                    preventivo.setUltimoFollowup(new Date());
                    preventivoDAO.update(preventivo);
                    
                    logger.info("Follow-up sent for preventivo: " + preventivo.getNumeroPreventivo());
                    
                } catch (Exception e) {
                    logger.error("Error processing follow-up for preventivo " + preventivo.getId(), e);
                }
            }
            
        } catch (Exception e) {
            logger.error("Error in verificaFollowupPreventivi", e);
        }
    }
    
    /**
     * FASE 2: Check documents with expired validity
     * Business rule: Documents expiring soon (licenses, IDs)
     */
    public void verificaDocumentiScaduti() {
        try {
            // Check documents expiring within 30 days
            List<NoleggioDocumento> expiring = documentoDAO.findExpiringWithin(30);
            
            logger.info("Found " + expiring.size() + " documents expiring within 30 days");
            
            for (NoleggioDocumento documento : expiring) {
                try {
                    // Notify lead's assigned user
                    if (documento.getLead().getUtenteAssegnatoId() != null) {
                        notificationService.sendNotificationToUser(
                            String.valueOf(documento.getLead().getUtenteAssegnatoId()),
                            "Documento in Scadenza",
                            documento.getTipoDocumento() + " per " + documento.getLead().getNomeCliente() + 
                            " scade il " + documento.getDataScadenza(),
                            NotificationEvent.Priority.NORMAL
                        );
                    }
                    
                    logger.info("Expiry notification sent for documento: " + documento.getTipoDocumento());
                    
                } catch (Exception e) {
                    logger.error("Error processing documento expiry notification " + documento.getId(), e);
                }
            }
            
        } catch (Exception e) {
            logger.error("Error in verificaDocumentiScaduti", e);
        }
    }
    
    /**
     * FASE 2: Check for document solicitation reminders (4-5 days)
     * Business rule: Documents requested but not received for 4+ days
     */
    public void verificaSollecitiDocumenti() {
        try {
            List<NoleggioDocumento> requiring = documentoDAO.findRequiringReminder(4);
            
            logger.info("Found " + requiring.size() + " documents requiring solicitation reminder");
            
            for (NoleggioDocumento documento : requiring) {
                try {
                    // Send reminder to customer
                    if (documento.getLead().getClienteId() != null) {
                        notificationService.sendNotificationToUser(
                            String.valueOf(documento.getLead().getClienteId()),
                            "Documento in Sospeso",
                            "Abbiamo richiesto il " + documento.getTipoDocumento() + 
                            " il " + documento.getDataRichiesta() + 
                            ". Ricorda: Puoi caricarlo online.",
                            NotificationEvent.Priority.NORMAL
                        );
                    }
                    
                    logger.info("Solicitation reminder sent for documento: " + documento.getTipoDocumento());
                    
                } catch (Exception e) {
                    logger.error("Error processing documento solicitation " + documento.getId(), e);
                }
            }
            
        } catch (Exception e) {
            logger.error("Error in verificaSollecitiDocumenti", e);
        }
    }
    
    /**
     * FASE 2: Check for stalled evaluations (>7 days without progress)
     * Business rule: Alert manager if evaluation stuck
     */
    public void verificaValutazioniBloccate() {
        try {
            List<NoleggioValutazione> stalled = valutazioneDAO.findStalled(7);
            
            logger.info("Found " + stalled.size() + " evaluations stalled for >7 days");
            
            for (NoleggioValutazione valutazione : stalled) {
                try {
                    // Alert assigned manager/user
                    if (valutazione.getLead().getUtenteAssegnatoId() != null) {
                        notificationService.sendNotificationToUser(
                            String.valueOf(valutazione.getLead().getUtenteAssegnatoId()),
                            "⚠️ Valutazione Bloccata",
                            "La valutazione per " + valutazione.getLead().getNomeCliente() + 
                            " è ferma da più di 7 giorni",
                            NotificationEvent.Priority.HIGH
                        );
                    }
                    
                    logger.warn("Stalled valuation detected: " + valutazione.getId());
                    
                } catch (Exception e) {
                    logger.error("Error processing stalled valutazione " + valutazione.getId(), e);
                }
            }
            
        } catch (Exception e) {
            logger.error("Error in verificaValutazioniBloccate", e);
        }
    }
    
    /**
     * FASE 3: Check for care call reminders (20-30 days)
     * Business rule: Orders need regular customer contact
     */
    public void verificaCareCall() {
        try {
            List<NoleggioOrdine> requiring = ordineDAO.findRequiringCareCall();
            
            logger.info("Found " + requiring.size() + " orders requiring care call");
            
            for (NoleggioOrdine ordine : requiring) {
                try {
                    // Create task for operator
                    if (ordine.getLead().getUtenteAssegnatoId() != null) {
                        notificationService.sendNotificationToUser(
                            String.valueOf(ordine.getLead().getUtenteAssegnatoId()),
                            "Care Call Necessario",
                            "Contatto previsto per ordine " + ordine.getNumeroOrdine() +
                            " del " + ordine.getDataOrdine(),
                            NotificationEvent.Priority.NORMAL
                        );
                    }
                    
                    // Update next care call (reschedule 25 days from now)
                    Calendar cal = Calendar.getInstance();
                    cal.add(Calendar.DAY_OF_MONTH, 25);
                    ordine.setProssimoCareCallPrevisto(cal.getTime());
                    ordineDAO.update(ordine);
                    
                    logger.info("Care call reminder created for ordine: " + ordine.getNumeroOrdine());
                    
                } catch (Exception e) {
                    logger.error("Error processing care call for ordine " + ordine.getId(), e);
                }
            }
            
        } catch (Exception e) {
            logger.error("Error in verificaCareCall", e);
        }
    }
    
    /**
     * FASE 4: Check for anti-bounce ticket reopenings (48-72h window)
     * Business rule: If ticket reopened 48-72h after closure = escalate
     */
    public void verificaAntiRimbalzo() {
        try {
            List<NoleggioTicket> withBounce = ticketDAO.findWithAntiBounce();
            
            logger.info("Found " + withBounce.size() + " tickets with anti-bounce flag");
            
            for (NoleggioTicket ticket : withBounce) {
                try {
                    // Already escalated, but send additional alert
                    notificationService.sendNotificationToUser(
                        "1", // Send to admin/manager
                        "🚨 Anti-Rimbalzo Alert",
                        "Ticket " + ticket.getNumeroTicket() + 
                        " riaperto entro 48-72h - Richiede attenzione del manager",
                        NotificationEvent.Priority.URGENT
                    );
                    
                    logger.warn("Anti-bounce escalation for ticket: " + ticket.getNumeroTicket());
                    
                } catch (Exception e) {
                    logger.error("Error processing anti-bounce ticket " + ticket.getId(), e);
                }
            }
            
        } catch (Exception e) {
            logger.error("Error in verificaAntiRimbalzo", e);
        }
    }
    
    /**
     * NBT: Check for 24h follow-up on quotes
     */
    public void verificaNBTFollowup24h() {
        try {
            List<NoleggioNBT> requiring = nbtDAO.findRequiringFollowup24h();
            
            logger.info("Found " + requiring.size() + " NBT quotes requiring 24h follow-up");
            
            for (NoleggioNBT nbt : requiring) {
                try {
                    // Send reminder
                    if (nbt.getClienteId() != null) {
                        notificationService.sendNotificationToUser(
                            String.valueOf(nbt.getClienteId()),
                            "Preventivo NBT - Conferma Richiesta",
                            "Il tuo preventivo NBT " + nbt.getNumeroPratica() + 
                            " è in scadenza. Conferma entro 48 ore per mantenere il prezzo.",
                            NotificationEvent.Priority.HIGH
                        );
                    }
                    
                    // Mark alert sent
                    nbt.setAlertFollowup24hInviato(true);
                    nbtDAO.update(nbt);
                    
                    logger.info("24h follow-up sent for NBT: " + nbt.getNumeroPratica());
                    
                } catch (Exception e) {
                    logger.error("Error processing NBT 24h follow-up " + nbt.getId(), e);
                }
            }
            
        } catch (Exception e) {
            logger.error("Error in verificaNBTFollowup24h", e);
        }
    }
    
    /**
     * NBT: Check for return reminders (24h before end)
     */
    public void verificaNBTRestituzione() {
        try {
            List<NoleggioNBT> requiring = nbtDAO.findRequiringReturnAlert();
            
            logger.info("Found " + requiring.size() + " NBT rentals requiring return alert");
            
            for (NoleggioNBT nbt : requiring) {
                try {
                    // Send return alert to customer
                    if (nbt.getClienteId() != null) {
                        notificationService.sendNotificationToUser(
                            String.valueOf(nbt.getClienteId()),
                            "Restituzione Veicolo NBT Domani",
                            "La restituzione del veicolo per NBT " + nbt.getNumeroPratica() +
                            " è prevista per domani.",
                            NotificationEvent.Priority.HIGH
                        );
                    }
                    
                    // Mark alert sent
                    nbt.setAlertRestituzioneInviato(true);
                    nbtDAO.update(nbt);
                    
                    logger.info("Return alert sent for NBT: " + nbt.getNumeroPratica());
                    
                } catch (Exception e) {
                    logger.error("Error processing NBT return alert " + nbt.getId(), e);
                }
            }
            
        } catch (Exception e) {
            logger.error("Error in verificaNBTRestituzione", e);
        }
    }
}
