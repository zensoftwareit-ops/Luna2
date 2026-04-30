package it.zensoftware.luna2.api.controller;

import it.zensoftware.luna2.dao.FatturaDAO;
import it.zensoftware.luna2.model.Fattura;
import it.zensoftware.luna2.api.service.SdiService;
import it.zensoftware.luna2.api.security.RateLimited;
import it.zensoftware.luna2.api.security.Auditable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * SDI Controller - Gestione trasmissioni E-invoicing
 *
 * Endpoint:
 * POST   /api/v1/sdi/trasmetti/{fatturaId}     - Invia fattura all'SDI
 * GET    /api/v1/sdi/status/{idTrasmissione}   - Verifica stato trasmissione
 * GET    /api/v1/sdi/notifiche/{idTrasmissione} - Recupera notifiche
 */
@RestController
@RequestMapping("/api/v1/sdi")
public class SdiController {

    private static final Logger logger = LoggerFactory.getLogger(SdiController.class);

    private final SdiService sdiService = new SdiService();
    private final FatturaDAO fatturaDAO = new FatturaDAO();

    /**
     * Invia una fattura all'SDI
     * POST /api/v1/sdi/trasmetti/{fatturaId}
     *
     * Rate limit: Max 50 requests per 5 minutes per IP
     * @param fatturaId ID della fattura da inviare
     * @return SdiTransmissionResponse con ID trasmissione
     */
    @Auditable(action = "TRASMETTI_SDI", entityType = "Fattura")
    @RateLimited(maxRequests = 50, windowMinutes = 5)
    @PostMapping("/trasmetti/{fatturaId}")
    public ResponseEntity<SdiTransmissionResponse> trasmettiAlturaaSdi(
            @PathVariable Long fatturaId) {

        logger.info("Richiesta trasmissione fattura {} a SDI", fatturaId);

        try {
            // Recupera fattura dal DB
            Fattura fattura = fatturaDAO.findById(fatturaId);
            if (fattura == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new SdiTransmissionResponse(
                        null,
                        "NOT_FOUND",
                        "Fattura non trovata",
                        null
                    ));
            }

            // Invia a SDI in modalità asincrona
            sdiService.inviaFatturaAsync(fattura);

            // Genera ID trasmissione
            String idTrasmissione = java.util.UUID.randomUUID().toString();

            return ResponseEntity.ok(new SdiTransmissionResponse(
                idTrasmissione,
                "SUBMITTED",
                "Fattura inviata a SDI",
                System.currentTimeMillis()
            ));

        } catch (SdiService.SdiException e) {
            logger.error("Errore SDI: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new SdiTransmissionResponse(
                    null,
                    "ERROR",
                    "Errore nella trasmissione: " + e.getMessage(),
                    null
                ));

        } catch (Exception e) {
            logger.error("Errore inatteso durante trasmissione", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new SdiTransmissionResponse(
                    null,
                    "ERROR",
                    "Errore inatteso",
                    null
                ));
        }
    }

    /**
     * Verifica lo stato di una trasmissione
     * GET /api/v1/sdi/status/{idTrasmissione}
     *
     * @param idTrasmissione ID della trasmissione
     * @return SdiStatusResponse con stato attuale
     */
    @GetMapping("/status/{idTrasmissione}")
    public ResponseEntity<SdiStatusResponse> getTransmissionStatus(
            @PathVariable String idTrasmissione) {

        logger.info("Controllo stato trasmissione {}", idTrasmissione);

        try {
            // TODO: Implementare logica di recupero stato da DB
            // Per ora, restituisce placeholder

            return ResponseEntity.ok(new SdiStatusResponse(
                idTrasmissione,
                "PENDING",
                "In attesa di risposta da SDI",
                System.currentTimeMillis()
            ));

        } catch (Exception e) {
            logger.error("Errore nel recupero dello stato", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Recupera notifiche per una trasmissione
     * GET /api/v1/sdi/notifiche/{idTrasmissione}
     *
     * @param idTrasmissione ID della trasmissione
     * @return SdiNotificationResponse con dettagli notifica
     */
    @GetMapping("/notifiche/{idTrasmissione}")
    public ResponseEntity<SdiService.SdiNotificationResponse> getNotification(
            @PathVariable String idTrasmissione) {

        logger.info("Recupero notifiche per trasmissione {}", idTrasmissione);

        try {
            SdiService.SdiNotificationResponse notification =
                sdiService.pollSdiNotification(idTrasmissione);

            if (notification == null) {
                return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
            }

            return ResponseEntity.ok(notification);

        } catch (SdiService.SdiException e) {
            logger.error("Errore nel recupero notifiche: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        } catch (Exception e) {
            logger.error("Errore inatteso", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Polling batch per notifiche
     * GET /api/v1/sdi/notifiche/lista?stato=PENDING
     */
    @GetMapping("/notifiche/lista")
    public ResponseEntity<SdiNotificheListaResponse> getNotificheList(
            @RequestParam(required = false) String stato) {

        logger.info("Recupero lista notifiche con stato: {}", stato);

        try {
            // TODO: Implementare query batch per recuperare notifiche pendenti
            return ResponseEntity.ok(new SdiNotificheListaResponse(
                0,
                "Nessuna notifica disponibile al momento"
            ));

        } catch (Exception e) {
            logger.error("Errore nel recupero lista notifiche", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ==================== RESPONSE CLASSES ====================

    public static class SdiTransmissionResponse {
        public String idTrasmissione;
        public String stato;
        public String messaggio;
        public Long timestamp;

        public SdiTransmissionResponse(String idTrasmissione, String stato,
                                      String messaggio, Long timestamp) {
            this.idTrasmissione = idTrasmissione;
            this.stato = stato;
            this.messaggio = messaggio;
            this.timestamp = timestamp;
        }
    }

    public static class SdiStatusResponse {
        public String idTrasmissione;
        public String stato;
        public String descrizione;
        public Long lastUpdate;

        public SdiStatusResponse(String idTrasmissione, String stato,
                               String descrizione, Long lastUpdate) {
            this.idTrasmissione = idTrasmissione;
            this.stato = stato;
            this.descrizione = descrizione;
            this.lastUpdate = lastUpdate;
        }
    }

    public static class SdiNotificheListaResponse {
        public int count;
        public String messaggio;

        public SdiNotificheListaResponse(int count, String messaggio) {
            this.count = count;
            this.messaggio = messaggio;
        }
    }
}
