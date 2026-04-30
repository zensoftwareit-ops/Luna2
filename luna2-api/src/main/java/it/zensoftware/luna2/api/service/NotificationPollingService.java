package it.zensoftware.luna2.api.service;

import it.zensoftware.luna2.model.SdiTransmission;
import it.zensoftware.luna2.dao.SdiTransmissionDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.util.List;

/**
 * SDI Notification Polling Service
 * Background task che effettua polling periodico delle notifiche da SDI
 */
@Service
public class NotificationPollingService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationPollingService.class);
    private final SdiTransmissionDAO transmissionDAO = new SdiTransmissionDAO();
    private final SdiService sdiService = new SdiService();

    /**
     * Polling ogni 10 secondi (configurable via ${sdi.polling.interval})
     */
    @Scheduled(fixedDelayString = "${sdi.polling.interval:10000}")
    public void pollPendingTransmissions() {
        try {
            logger.debug("SDI Polling ciclo avviato");
            List<SdiTransmission> pending = transmissionDAO.findPendingTransmissions();

            if (pending.isEmpty()) {
                return;
            }

            logger.info("Polling {} trasmissioni in attesa", pending.size());

            for (SdiTransmission tx : pending) {
                processPendingTransmission(tx);
            }

        } catch (Exception e) {
            logger.error("Errore polling notifiche SDI", e);
        }
    }

    private void processPendingTransmission(SdiTransmission tx) {
        try {
            long ageMs = System.currentTimeMillis() - tx.getDataCreazione().getTime();

            // Timeout check: 6 minuti
            if (ageMs > 360000) {
                logger.warn("Timeout trasmissione {}: {} min", tx.getIdTrasmissione(), ageMs / 60000);
                tx.setStato(SdiTransmission.StatoTrasmissione.ERROR);
                tx.setDescrizioneErrore("Timeout: nessuna risposta da SDI dopo 6 minuti");
                transmissionDAO.update(tx);
                return;
            }

            // Max retries check: 5 tentativi
            if (tx.getNumeroTentativi() != null && tx.getNumeroTentativi() >= 5) {
                logger.warn("Max retries raggiunto per {}", tx.getIdTrasmissione());
                tx.setStato(SdiTransmission.StatoTrasmissione.ERROR);
                tx.setDescrizioneErrore("Max retries raggiunto (5 tentativi)");
                transmissionDAO.update(tx);
                return;
            }

            // Poll SDI per notifica
            SdiService.SdiNotificationResponse notification =
                sdiService.pollSdiNotification(tx.getIdTrasmissione());

            if (notification != null) {
                tx.setStato(SdiTransmission.StatoTrasmissione.valueOf(notification.stato));
                tx.setDataNotifica(new java.util.Date());
                tx.setDescrizioneErrore(notification.descrizioneErrore);
                logger.info("Notifica ricevuta per {}: {}", tx.getIdTrasmissione(), notification.stato);
            } else {
                tx.recordAttempt();
                logger.debug("No notification yet for {}, attempt {}", tx.getIdTrasmissione(), tx.getNumeroTentativi());
            }

            transmissionDAO.update(tx);

        } catch (Exception e) {
            logger.error("Errore processing trasmissione {}", tx.getIdTrasmissione(), e);
            tx.recordAttempt();
            transmissionDAO.update(tx);
        }
    }

    public PollingStatistics getStatistics() {
        SdiTransmissionDAO.SdiTransmissionStats stats = transmissionDAO.getStatistics();
        PollingStatistics p = new PollingStatistics();
        p.totale = stats.totale;
        p.accepted = stats.accepted;
        p.rejected = stats.rejected;
        p.successRate = stats.getSuccessRate();
        p.errorRate = stats.getErrorRate();
        return p;
    }

    public static class PollingStatistics {
        public Long totale;
        public Long accepted;
        public Long rejected;
        public float successRate;
        public float errorRate;

        @Override
        public String toString() {
            return String.format("PollingStats: %d total, success=%.1f%%, error=%.1f%%",
                totale, successRate, errorRate);
        }
    }
}
