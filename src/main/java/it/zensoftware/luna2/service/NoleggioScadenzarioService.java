package it.zensoftware.luna2.service;

import it.zensoftware.luna2.dao.*;
import it.zensoftware.luna2.model.*;
import it.zensoftware.luna2.service.calendar.CalendarSyncService;
import it.zensoftware.luna2.service.notification.PushNotificationService;
import it.zensoftware.luna2.service.notification.event.NotificationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

/**
 * NoleggioScadenzarioService - Phase 5: Contract renewal and maintenance scheduling
 * Integrates with CalendarSyncService for Google Calendar/iCloud synchronization
 */
public class NoleggioScadenzarioService {
    
    private static final Logger logger = LogManager.getLogger(NoleggioScadenzarioService.class);
    
    private final NoleggioContrattoDAO contrattoDAO;
    private final CalendarEventDAO calendarEventDAO;
    private final CalendarSyncService calendarSyncService;
    private final PushNotificationService notificationService;
    
    public NoleggioScadenzarioService() {
        this.contrattoDAO = new NoleggioContrattoDAO();
        this.calendarEventDAO = new CalendarEventDAO();
        this.calendarSyncService = new CalendarSyncService();
        this.notificationService = PushNotificationService.getInstance();
    }
    
    /**
     * Daily automation: Check all expiring contracts and create calendar events
     * AUTOMATION: Called via cron job nightly
     */
    public void verificaScadenzeECreaEventiCalendario() {
        logger.info("=== Starting Scadenzario verification ===");
        
        try {
            // Check contract renewals (4-6 months = 120 days in advance)
            verificaRinnovi(120);
            
            // Check km verification (every 6 months)
            verificaVerificaKm();
            
            // Check vehicle inspections (revisione)
            verificaRevisione(30);
            
            // Check maintenance (tagliando)
            verificaTagliando(15);
            
            // Check driver license expiry
            verificaScadenzaPatente(60);
            
            // Check insurance expiry
            verificaScadenzaAssicurazione(30);
            
            logger.info("=== Scadenzario verification completed ===");
        } catch (Exception e) {
            logger.error("Error in scadenzario verification", e);
        }
    }
    
    /**
     * Check contract renewals (4-6 months in advance)
     */
    private void verificaRinnovi(int giorniAnticipo) {
        List<NoleggioContratto> expiring = contrattoDAO.findExpiringWithin(giorniAnticipo);
        
        for (NoleggioContratto contratto : expiring) {
            try {
                logger.info("Processing renewal for contract: " + contratto.getNumeroContratto());
                
                // Calculate days to expiry
                long daysUntilExpiry = (contratto.getDataFine().getTime() - new Date().getTime()) 
                                     / (1000 * 60 * 60 * 24);
                
                // Create calendar event for renewal alert
                creaEventoScadenza(
                    contratto.getUtenteAssegnatoId(),
                    "Rinnovo Contratto " + contratto.getNumeroContratto(),
                    "Rinnovo contratto noleggio per " + contratto.getTarga() +
                    "\nData scadenza: " + contratto.getDataFine() +
                    "\nGiorni rimanenti: " + daysUntilExpiry,
                    contratto.getDataFine(),
                    CalendarEvent.SourceType.NOLEGGIO_RINNOVO,
                    contratto.getId()
                );
                
                // Mark alert activated
                contratto.setAllarmeRinnovoAttivato(true);
                contrattoDAO.update(contratto);
                
                // Send notification
                if (contratto.getUtenteAssegnatoId() != null) {
                    notificationService.sendNotificationToUser(
                        String.valueOf(contratto.getUtenteAssegnatoId()),
                        "Rinnovo Contratto Imminente",
                        "Contratto " + contratto.getNumeroContratto() + 
                        " scade tra " + daysUntilExpiry + " giorni",
                        NotificationEvent.Priority.HIGH
                    );
                }
                
            } catch (Exception e) {
                logger.error("Error processing renewal for contract " + contratto.getNumeroContratto(), e);
            }
        }
    }
    
    /**
     * Check km verification (every 6 months)
     */
    private void verificaVerificaKm() {
        List<NoleggioContratto> requiring = contrattoDAO.findRequiringKmVerification();
        
        for (NoleggioContratto contratto : requiring) {
            try {
                logger.info("Creating km verification event for contract: " + contratto.getNumeroContratto());
                
                // Schedule next verification for 6 months from now
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.MONTH, 6);
                Date nextVerification = cal.getTime();
                
                // Create calendar event
                creaEventoScadenza(
                    contratto.getUtenteAssegnatoId(),
                    "Verifica KM " + contratto.getTarga(),
                    "Verifica chilometraggio per " + contratto.getTarga() +
                    "\nKm attuali: " + contratto.getKmAttuali() +
                    "\nKm previsti: " + contratto.getKmTotaliPrevisti(),
                    contratto.getProssimaVerificaKm(),
                    CalendarEvent.SourceType.NOLEGGIO_VERIFICA_KM,
                    contratto.getId()
                );
                
                // Update next verification date
                contratto.setProssimaVerificaKm(nextVerification);
                contrattoDAO.update(contratto);
                
            } catch (Exception e) {
                logger.error("Error processing km verification for contract " + contratto.getNumeroContratto(), e);
            }
        }
    }
    
    /**
     * Check vehicle inspection (revisione)
     */
    private void verificaRevisione(int giorniAnticipo) {
        List<NoleggioContratto> requiring = contrattoDAO.findWithUpcomingRevisione(giorniAnticipo);
        
        for (NoleggioContratto contratto : requiring) {
            try {
                logger.info("Creating revisione event for contract: " + contratto.getNumeroContratto());
                
                // Create calendar event
                creaEventoScadenza(
                    contratto.getUtenteAssegnatoId(),
                    "Revisione Veicolo " + contratto.getTarga(),
                    "Revisione obbligatoria per " + contratto.getTarga() +
                    "\nMeritato presso: " + (contratto.getOfficina() != null ? contratto.getOfficina() : "Da definire"),
                    contratto.getDataProssimaRevisione(),
                    CalendarEvent.SourceType.NOLEGGIO_REVISIONE,
                    contratto.getId()
                );
                
            } catch (Exception e) {
                logger.error("Error processing revisione for contract " + contratto.getNumeroContratto(), e);
            }
        }
    }
    
    /**
     * Check maintenance (tagliando)
     */
    private void verificaTagliando(int giorniAnticipo) {
        List<NoleggioContratto> requiring = contrattoDAO.findWithUpcomingTagliando(giorniAnticipo);
        
        for (NoleggioContratto contratto : requiring) {
            try {
                logger.info("Creating tagliando event for contract: " + contratto.getNumeroContratto());
                
                // Create calendar event
                creaEventoScadenza(
                    contratto.getUtenteAssegnatoId(),
                    "Tagliando Manutenzione " + contratto.getTarga(),
                    "Tagliando ordinario per " + contratto.getTarga() +
                    "\nOfficina: " + (contratto.getOfficina() != null ? contratto.getOfficina() : "Autocertificabile"),
                    contratto.getDataProssimoTagliando(),
                    CalendarEvent.SourceType.NOLEGGIO_TAGLIANDO,
                    contratto.getId()
                );
                
            } catch (Exception e) {
                logger.error("Error processing tagliando for contract " + contratto.getNumeroContratto(), e);
            }
        }
    }
    
    /**
     * Check driver license expiry
     */
    private void verificaScadenzaPatente(int giorniAnticipo) {
        List<NoleggioContratto> requiring = contrattoDAO.findWithExpiringLicense(giorniAnticipo);
        
        for (NoleggioContratto contratto : requiring) {
            try {
                logger.info("Creating patente expiry event for contract: " + contratto.getNumeroContratto());
                
                // Create calendar event
                creaEventoScadenza(
                    contratto.getUtenteAssegnatoId(),
                    "Scadenza Patente " + contratto.getNomeConducente(),
                    "La patente di " + contratto.getNomeConducente() + 
                    " per il contratto " + contratto.getNumeroContratto() + " scade",
                    contratto.getDataScadenzaPatenteConducente(),
                    CalendarEvent.SourceType.NOLEGGIO_PATENTE,
                    contratto.getId()
                );
                
                // Alert notification
                if (contratto.getUtenteAssegnatoId() != null) {
                    notificationService.sendNotificationToUser(
                        String.valueOf(contratto.getUtenteAssegnatoId()),
                        "⚠️ Patente in Scadenza",
                        "La patente di " + contratto.getNomeConducente() + " scade a breve",
                        NotificationEvent.Priority.HIGH
                    );
                }
                
            } catch (Exception e) {
                logger.error("Error processing patente expiry for contract " + contratto.getNumeroContratto(), e);
            }
        }
    }
    
    /**
     * Check insurance expiry
     */
    private void verificaScadenzaAssicurazione(int giorniAnticipo) {
        List<NoleggioContratto> requiring = contrattoDAO.findWithExpiringInsurance(giorniAnticipo);
        
        for (NoleggioContratto contratto : requiring) {
            try {
                logger.info("Creating insurance expiry event for contract: " + contratto.getNumeroContratto());
                
                // Create calendar event
                creaEventoScadenza(
                    contratto.getUtenteAssegnatoId(),
                    "Rinnovo Assicurazione " + contratto.getTarga(),
                    "Rinnovo polizza assicurativa per " + contratto.getTarga() +
                    "\nCompagnia: " + (contratto.getCompagniaAssicurativa() != null ? contratto.getCompagniaAssicurativa() : "Non specificata"),
                    contratto.getDataScadenzaAssicurazione(),
                    CalendarEvent.SourceType.NOLEGGIO_ASSICURAZIONE,
                    contratto.getId()
                );
                
            } catch (Exception e) {
                logger.error("Error processing insurance expiry for contract " + contratto.getNumeroContratto(), e);
            }
        }
    }
    
    /**
     * Create calendar event and auto-sync with Google Calendar/iCloud
     */
    private void creaEventoScadenza(Long userId, String titolo, String descrizione, 
                                    Date dataScadenza, CalendarEvent.SourceType sourceType, Long sourceId) {
        try {
            // Create calendar event
            CalendarEvent event = new CalendarEvent();
            event.setUserId(String.valueOf(userId));
            event.setSource(CalendarAccount.Provider.GOOGLE); // Default, will also sync to iCloud
            event.setTitle(titolo);
            event.setDescription(descrizione);
            
            // Set event time: all-day event on expiry date
            LocalDateTime startTime = dataScadenza.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
            event.setStartTime(startTime);
            event.setEndTime(startTime.plusHours(1)); // 1-hour default duration
            
            event.setSourceType(sourceType);
            event.setSourceId(sourceId);
            event.setCreatedAt(LocalDateTime.now());
            event.setUpdatedAt(LocalDateTime.now());
            
            // Save to database
            calendarEventDAO.save(event);
            logger.info("Created calendar event: " + titolo);
            
            // Auto-sync with providers (Google Calendar, iCloud)
            calendarSyncService.syncAllAccounts();
            
        } catch (Exception e) {
            logger.error("Error creating calendar event", e);
        }
    }
    
    /**
     * Manually create scadenzario event (for testing/immediate scheduling)
     */
    public CalendarEvent creaEventoManuale(Long userId, String titolo, String descrizione, Date data) {
        CalendarEvent event = new CalendarEvent();
        event.setUserId(String.valueOf(userId));
        event.setSource(CalendarAccount.Provider.GOOGLE);
        event.setTitle(titolo);
        event.setDescription(descrizione);
        
        LocalDateTime startTime = data.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        event.setStartTime(startTime);
        event.setEndTime(startTime.plusHours(1));
        
        event.setSourceType(CalendarEvent.SourceType.NOLEGGIO_RINNOVO);
        event.setCreatedAt(LocalDateTime.now());
        event.setUpdatedAt(LocalDateTime.now());
        
        calendarEventDAO.save(event);
        
        // Sync immediately
        calendarSyncService.syncAllAccounts();
        
        logger.info("Created manual calendar event: " + titolo);
        return event;
    }
}
