package it.zensoftware.luna2.api.service;

import it.zensoftware.luna2.model.Fattura;
import it.zensoftware.luna2.model.Cliente;
import it.zensoftware.luna2.model.SdiTransmission;
import it.zensoftware.luna2.dao.SdiTransmissionDAO;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * SDI Service - Gestione trasmissioni E-invoicing (Fatture Elettroniche)
 * Implementa: XML generation, HTTP client, polling, persistenza trasmissioni
 */
@Service
public class SdiService {

    private static final Logger logger = LoggerFactory.getLogger(SdiService.class);
    private static final String SDI_TEST_ENDPOINT = "https://sditest.fatturapa.it/ricevi/ricezione";
    private static final String SDI_PROD_ENDPOINT = "https://sdiservizi.fatturapa.it/ricevi/ricezione";
    private static final String ENVIRONMENT = System.getProperty("sdi.env", "test");
    private static final String SDI_ENDPOINT = "test".equalsIgnoreCase(ENVIRONMENT) ?
        SDI_TEST_ENDPOINT : SDI_PROD_ENDPOINT;
    private static final DateTimeFormatter ISO_8601 = DateTimeFormatter.ISO_DATE_TIME;

    private final SdiTransmissionDAO transmissionDAO = new SdiTransmissionDAO();

    /**
     * Genera XML di fattura conforme XSD Agenzia Entrate
     */
    public String generateFatturaXml(Fattura fattura) throws Exception {
        logger.info("Generando XML per fattura {}", fattura != null ? fattura.getNumero() : "null");

        FatturaElettronicaDTO dto = mapFatturaToXmlDto(fattura);

        JAXBContext context = JAXBContext.newInstance(FatturaElettronicaDTO.class);
        Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");

        StringWriter sw = new StringWriter();
        marshaller.marshal(dto, sw);

        String xml = sw.toString();
        logger.debug("XML generato: {} bytes", xml.length());
        return xml;
    }

    /**
     * Invia fattura all'SDI in modalità asincrona
     */
    @Async
    public void inviaFatturaAsync(Fattura fattura) {
        try {
            String idTrasmissione = inviaFattura(fattura);
            logger.info("Fattura {} inviata a SDI: {}", fattura.getNumero(), idTrasmissione);
            saveSdiTransmissionId(fattura.getId(), idTrasmissione);
        } catch (Exception e) {
            logger.error("Errore invio fattura {}", fattura.getNumero(), e);
            recordTransmissionError(fattura.getId(), e.getMessage());
        }
    }

    /**
     * Invia fattura all'SDI (sincrono)
     */
    public String inviaFattura(Fattura fattura) throws Exception {
        String xml = generateFatturaXml(fattura);
        return sendXmlToSdi(xml);
    }

    /**
     * Invia XML grezzo all'endpoint SDI
     */
    private String sendXmlToSdi(String xmlContent) throws Exception {
        String idTrasmissione = UUID.randomUUID().toString();

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(SDI_ENDPOINT);
            httpPost.setHeader("Content-Type", "application/xml; charset=UTF-8");
            httpPost.setHeader("X-Transmission-Id", idTrasmissione);
            httpPost.setEntity(new StringEntity(xmlContent, "UTF-8"));

            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                int statusCode = response.getStatusLine().getStatusCode();

                if (statusCode >= 200 && statusCode < 300) {
                    logger.info("Trasmissione {} accettata (HTTP {})", idTrasmissione, statusCode);
                    return idTrasmissione;
                } else {
                    String errorMsg = "SDI rifiutato: HTTP " + statusCode;
                    logger.error(errorMsg);
                    throw new SdiException(errorMsg, statusCode);
                }
            }
        }
    }

    /**
     * Polling notifiche con exponential backoff
     */
    public SdiNotificationResponse pollSdiNotification(String idTrasmissione) throws Exception {
        logger.info("Polling notifiche per {}", idTrasmissione);

        int maxRetries = 5;
        long baseDelay = 5000;

        for (int retry = 0; retry < maxRetries; retry++) {
            try {
                SdiNotificationResponse notification = fetchNotificationFromSdi(idTrasmissione);
                if (notification != null) {
                    logger.info("Notifica ricevuta: {}", notification.stato);
                    return notification;
                }

                long delayMs = baseDelay * (long) Math.pow(2, retry);
                logger.debug("Retry in {} ms", delayMs);
                Thread.sleep(delayMs);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new SdiException("Polling interrotto", e);
            }
        }

        throw new SdiException("Timeout polling: " + idTrasmissione);
    }

    /**
     * Recupera notifica da DB
     */
    private SdiNotificationResponse fetchNotificationFromSdi(String idTrasmissione) {
        try {
            SdiTransmission transmission = transmissionDAO.findByIdTrasmissione(idTrasmissione);

            if (transmission == null) {
                logger.debug("Trasmissione {} non trovata", idTrasmissione);
                return null;
            }

            if (!transmission.isPendingResponse()) {
                return new SdiNotificationResponse(
                    transmission.getIdTrasmissione(),
                    transmission.getStato().toString(),
                    transmission.getDataNotifica() != null ?
                        transmission.getDataNotifica().toString() : new java.util.Date().toString(),
                    transmission.getDescrizioneErrore()
                );
            }

            logger.debug("Trasmissione {} in attesa", idTrasmissione);
            return null;

        } catch (Exception e) {
            logger.error("Errore recupero notifica: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Mapper: Fattura JPA → FatturaElettronicaDTO
     */
    private FatturaElettronicaDTO mapFatturaToXmlDto(Fattura fattura) {
        FatturaElettronicaDTO dto = new FatturaElettronicaDTO();

        if (fattura == null) {
            return dto;
        }

        dto.setNumeroFattura(fattura.getNumero());
        dto.setDataFattura(ISO_8601.format(LocalDateTime.now()));
        dto.setImportoTotale(fattura.getTotale() != null ?
            fattura.getTotale().toPlainString() : "0.00");

        if (fattura.getCliente() != null) {
            Cliente cliente = fattura.getCliente();
            dto.setClienteRagioneSociale(cliente.getRagioneSociale());
            dto.setClientePartitaIva(cliente.getPartitaIva());
            dto.setClienteIndirizzo(cliente.getIndirizzo());
            dto.setClienteCitta(cliente.getCitta());
            dto.setClienteCap(cliente.getCap());
        }

        return dto;
    }

    /**
     * Salva trasmissione in DB
     */
    private void saveSdiTransmissionId(Long fatturaId, String idTrasmissione) {
        try {
            SdiTransmission transmission = new SdiTransmission();
            transmission.setIdTrasmissione(idTrasmissione);
            transmission.setFatturaId(fatturaId);
            transmission.setStato(SdiTransmission.StatoTrasmissione.SUBMITTED);
            transmissionDAO.save(transmission);
            logger.info("Trasmissione {} salvata", idTrasmissione);
        } catch (Exception e) {
            logger.error("Errore salvataggio trasmissione", e);
        }
    }

    /**
     * Registra errore trasmissione
     */
    private void recordTransmissionError(Long fatturaId, String errorMessage) {
        try {
            java.util.List<SdiTransmission> transmissions = transmissionDAO.findByFatturaId(fatturaId);
            if (!transmissions.isEmpty()) {
                SdiTransmission tx = transmissions.get(0);
                tx.setStato(SdiTransmission.StatoTrasmissione.ERROR);
                tx.setDescrizioneErrore(errorMessage);
                tx.recordAttempt();
                transmissionDAO.update(tx);
                logger.warn("Errore registrato per fattura {}: {}", fatturaId, errorMessage);
            }
        } catch (Exception e) {
            logger.error("Errore nella registrazione dell'errore", e);
        }
    }

    // ==================== DTOs ====================

    public static class FatturaElettronicaDTO {
        private String numeroFattura;
        private String dataFattura;
        private String importoTotale;
        private String clienteRagioneSociale;
        private String clientePartitaIva;
        private String clienteIndirizzo;
        private String clienteCitta;
        private String clienteCap;

        public String getNumeroFattura() { return numeroFattura; }
        public void setNumeroFattura(String numeroFattura) { this.numeroFattura = numeroFattura; }

        public String getDataFattura() { return dataFattura; }
        public void setDataFattura(String dataFattura) { this.dataFattura = dataFattura; }

        public String getImportoTotale() { return importoTotale; }
        public void setImportoTotale(String importoTotale) { this.importoTotale = importoTotale; }

        public String getClienteRagioneSociale() { return clienteRagioneSociale; }
        public void setClienteRagioneSociale(String clienteRagioneSociale) {
            this.clienteRagioneSociale = clienteRagioneSociale;
        }

        public String getClientePartitaIva() { return clientePartitaIva; }
        public void setClientePartitaIva(String clientePartitaIva) {
            this.clientePartitaIva = clientePartitaIva;
        }

        public String getClienteIndirizzo() { return clienteIndirizzo; }
        public void setClienteIndirizzo(String clienteIndirizzo) {
            this.clienteIndirizzo = clienteIndirizzo;
        }

        public String getClienteCitta() { return clienteCitta; }
        public void setClienteCitta(String clienteCitta) { this.clienteCitta = clienteCitta; }

        public String getClienteCap() { return clienteCap; }
        public void setClienteCap(String clienteCap) { this.clienteCap = clienteCap; }
    }

    public static class SdiNotificationResponse {
        public String idTrasmissione;
        public String stato;
        public String dataStato;
        public String descrizioneErrore;

        public SdiNotificationResponse() {}

        public SdiNotificationResponse(String idTrasmissione, String stato,
                                      String dataStato, String descrizioneErrore) {
            this.idTrasmissione = idTrasmissione;
            this.stato = stato;
            this.dataStato = dataStato;
            this.descrizioneErrore = descrizioneErrore;
        }
    }

    public static class SdiException extends Exception {
        private int httpStatusCode;

        public SdiException(String message) { super(message); }
        public SdiException(String message, int httpStatusCode) {
            super(message);
            this.httpStatusCode = httpStatusCode;
        }
        public SdiException(String message, Throwable cause) { super(message, cause); }

        public int getHttpStatusCode() { return httpStatusCode; }
    }
}
