package it.zensoftware.luna2.service;

import it.zensoftware.luna2.dao.NoleggioNBTDAO;
import it.zensoftware.luna2.model.NoleggioNBT;
import it.zensoftware.luna2.service.notification.PushNotificationService;
import it.zensoftware.luna2.service.notification.event.NotificationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

/**
 * NoleggioNBTService - Short-term rental management (1-30 days)
 * Parallel workflow to long-term rental
 */
public class NoleggioNBTService {
    
    private static final Logger logger = LogManager.getLogger(NoleggioNBTService.class);
    
    private final NoleggioNBTDAO nbtDAO;
    private final PushNotificationService notificationService;
    
    // Quote validity period (48 hours)
    private static final long QUOTE_VALIDITY_MS = 48L * 60 * 60 * 1000;
    
    public NoleggioNBTService() {
        this.nbtDAO = new NoleggioNBTDAO();
        this.notificationService = PushNotificationService.getInstance();
    }
    
    /**
     * Create new short-term rental request
     */
    public NoleggioNBT createRequest(NoleggioNBT nbt, Long userId) {
        nbt.setDataRichiesta(new Date());
        nbt.setUtenteCreazioneId(userId);
        nbt.setNumeroPratica(generaNumeroPratica());
        nbt.setStatus(NoleggioNBT.Status.RICHIESTA_RICEVUTA);
        nbt.setAlertFollowup24hInviato(false);
        nbt.setAlertRestituzioneInviato(false);
        
        // Calculate rental duration
        if (nbt.getDataInizioNoleggio() != null && nbt.getDataFineNoleggio() != null) {
            long diffMs = nbt.getDataFineNoleggio().getTime() - nbt.getDataInizioNoleggio().getTime();
            int giorniNoleggio = (int) (diffMs / (1000 * 60 * 60 * 24)) + 1;
            nbt.setGiorniNoleggio(giorniNoleggio);
            
            // Calculate total cost
            if (nbt.getTariffaGiornaliera() != null) {
                double costoBase = nbt.getTariffaGiornaliera() * giorniNoleggio;
                nbt.setImportoTotale(costoBase);
            }
        }
        
        nbtDAO.save(nbt);
        logger.info("Created NBT request: " + nbt.getNumeroPratica());
        
        return nbt;
    }
    
    /**
     * Create and send quote
     */
    public NoleggioNBT sendQuote(Long nbtId, Double tariffaGiornaliera, Long userId) {
        NoleggioNBT nbt = nbtDAO.findById(nbtId);
        if (nbt == null) {
            throw new IllegalArgumentException("NBT not found");
        }
        
        nbt.setTariffaGiornaliera(tariffaGiornaliera);
        
        // Calculate total cost
        double costoBase = tariffaGiornaliera * nbt.getGiorniNoleggio();
        double costoAccessori = (nbt.getAccessoriRichiesti() != null && !nbt.getAccessoriRichiesti().isEmpty()) 
                               ? costoBase * 0.1 : 0.0; // 10% extra for accessories
        nbt.setImportoTotale(costoBase + costoAccessori);
        
        nbt.setStatus(NoleggioNBT.Status.PREVENTIVO_INVIATO);
        nbt.setDataInvioPreventivo(new Date());
        
        // Set quote expiry (48h from now)
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.HOUR, 48);
        nbt.setPreventivoValidoFino(cal.getTime());
        
        nbtDAO.update(nbt);
        logger.info("Sent NBT quote: " + nbt.getNumeroPratica());
        
        return nbt;
    }
    
    /**
     * Confirm reservation
     */
    public NoleggioNBT confirmBooking(Long nbtId, Long userId) {
        NoleggioNBT nbt = nbtDAO.findById(nbtId);
        if (nbt == null) {
            throw new IllegalArgumentException("NBT not found");
        }
        
        if (nbt.getStatus() != NoleggioNBT.Status.PREVENTIVO_INVIATO) {
            throw new IllegalStateException("Quote must be sent first");
        }
        
        // Check quote validity
        Date now = new Date();
        if (nbt.getPreventivoValidoFino() != null && now.after(nbt.getPreventivoValidoFino())) {
            throw new IllegalStateException("Quote expired");
        }
        
        nbt.setStatus(NoleggioNBT.Status.PRENOTAZIONE_CONFERMATA);
        nbt.setDataConfermaPrenotazione(now);
        nbtDAO.update(nbt);
        
        logger.info("Confirmed NBT booking: " + nbt.getNumeroPratica());
        
        // Notify user
        if (nbt.getUtenteCreazioneId() != null) {
            notificationService.sendNotificationToUser(
                String.valueOf(nbt.getUtenteCreazioneId()),
                "Prenotazione Confermata",
                "NBT " + nbt.getNumeroPratica() + " confermato",
                NotificationEvent.Priority.NORMAL
            );
        }
        
        return nbt;
    }
    
    /**
     * Assign vehicle to booking
     */
    public NoleggioNBT assignVehicle(Long nbtId, String marca, String modello, String targa, Long userId) {
        NoleggioNBT nbt = nbtDAO.findById(nbtId);
        if (nbt == null) {
            throw new IllegalArgumentException("NBT not found");
        }
        
        nbt.setMarcaModelloVeicoloAssegnato(marca + " " + modello);
        nbt.setTargaVeicoloAssegnato(targa);
        nbt.setStatus(NoleggioNBT.Status.VEICOLO_ASSEGNATO);
        nbtDAO.update(nbt);
        
        logger.info("Assigned vehicle to NBT " + nbt.getNumeroPratica() + ": " + targa);
        return nbt;
    }
    
    /**
     * Mark vehicle ready for pickup/delivery
     */
    public NoleggioNBT markReadyForDelivery(Long nbtId, Long userId) {
        NoleggioNBT nbt = nbtDAO.findById(nbtId);
        if (nbt == null) {
            throw new IllegalArgumentException("NBT not found");
        }
        
        nbt.setStatus(NoleggioNBT.Status.PRONTA_CONSEGNA);
        nbtDAO.update(nbt);
        
        logger.info("NBT ready for delivery: " + nbt.getNumeroPratica());
        
        // Notify customer
        if (nbt.getClienteId() != null) {
            notificationService.sendNotificationToUser(
                String.valueOf(nbt.getClienteId()),
                "Veicolo Pronto",
                "Il veicolo per NBT " + nbt.getNumeroPratica() + " è pronto per il ritiro",
                NotificationEvent.Priority.HIGH
            );
        }
        
        return nbt;
    }
    
    /**
     * Start rental (vehicle picked up)
     */
    public NoleggioNBT startRental(Long nbtId, Integer kmIniziali, String noteConsegna, Long userId) {
        NoleggioNBT nbt = nbtDAO.findById(nbtId);
        if (nbt == null) {
            throw new IllegalArgumentException("NBT not found");
        }
        
        nbt.setStatus(NoleggioNBT.Status.IN_CORSO);
        nbt.setDataConsegnaEffettiva(new Date());
        nbt.setKmIniziali(kmIniziali);
        nbt.setNoteConsegna(noteConsegna);
        nbtDAO.update(nbt);
        
        logger.info("Started NBT rental: " + nbt.getNumeroPratica());
        return nbt;
    }
    
    /**
     * End rental (vehicle returned)
     */
    public NoleggioNBT endRental(Long nbtId, Integer kmFinali, String noteRestituzione, Long userId) {
        NoleggioNBT nbt = nbtDAO.findById(nbtId);
        if (nbt == null) {
            throw new IllegalArgumentException("NBT not found");
        }
        
        nbt.setStatus(NoleggioNBT.Status.RESTITUITO);
        nbt.setDataRestituzioneEffettiva(new Date());
        nbt.setKmFinali(kmFinali);
        nbt.setNoteRestituzione(noteRestituzione);
        
        // Calculate extra km cost
        if (nbt.getKmIniziali() != null && kmFinali != null) {
            int kmPercorsi = kmFinali - nbt.getKmIniziali();
            int kmFranchigia = (nbt.getFranchigiaKmGiornaliera() != null) 
                             ? nbt.getFranchigiaKmGiornaliera() * nbt.getGiorniNoleggio() 
                             : 0;
            
            if (kmPercorsi > kmFranchigia) {
                int kmExtra = kmPercorsi - kmFranchigia;
                double costoKmExtra = nbt.calcolaCostiKmExtra();
                nbt.setCostoExtraKm(costoKmExtra);
                
                // Update total cost
                nbt.setImportoTotale(nbt.getImportoTotale() + costoKmExtra);
                
                logger.info("NBT " + nbt.getNumeroPratica() + ": km extra = " + kmExtra + 
                          ", costo extra = €" + costoKmExtra);
            }
        }
        
        nbtDAO.update(nbt);
        logger.info("Ended NBT rental: " + nbt.getNumeroPratica());
        
        return nbt;
    }
    
    /**
     * Complete rental with payment
     */
    public NoleggioNBT completeRental(Long nbtId, Double importoPagato, String metodoPagamento, Long userId) {
        NoleggioNBT nbt = nbtDAO.findById(nbtId);
        if (nbt == null) {
            throw new IllegalArgumentException("NBT not found");
        }
        
        if (nbt.getStatus() != NoleggioNBT.Status.RESTITUITO) {
            throw new IllegalStateException("Vehicle must be returned first");
        }
        
        nbt.setStatus(NoleggioNBT.Status.COMPLETATO);
        nbt.setImportoPagato(importoPagato);
        nbt.setMetodoPagamento(metodoPagamento);
        nbt.setDataCompletamento(new Date());
        nbtDAO.update(nbt);
        
        logger.info("Completed NBT rental: " + nbt.getNumeroPratica() + 
                  " - Payment: €" + importoPagato);
        
        return nbt;
    }
    
    /**
     * Cancel rental
     */
    public NoleggioNBT cancelRental(Long nbtId, String motivoAnnullamento, Long userId) {
        NoleggioNBT nbt = nbtDAO.findById(nbtId);
        if (nbt == null) {
            throw new IllegalArgumentException("NBT not found");
        }
        
        nbt.setStatus(NoleggioNBT.Status.ANNULLATO);
        nbt.setMotivoAnnullamento(motivoAnnullamento);
        nbtDAO.update(nbt);
        
        logger.info("Cancelled NBT rental: " + nbt.getNumeroPratica());
        return nbt;
    }
    
    // ==================== UTILITIES ====================
    
    private String generaNumeroPratica() {
        return "NBT" + System.currentTimeMillis();
    }
}
