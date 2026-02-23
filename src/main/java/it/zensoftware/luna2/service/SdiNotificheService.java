package it.zensoftware.luna2.service;

import it.zensoftware.luna2.dao.FatturaDAO;
import it.zensoftware.luna2.dao.SdiNotificaDAO;
import it.zensoftware.luna2.dao.UserDAO;
import it.zensoftware.luna2.model.Fattura;
import it.zensoftware.luna2.model.SdiNotifica;
import it.zensoftware.luna2.model.User;
import it.zensoftware.luna2.service.notification.EventPublisher;
import it.zensoftware.luna2.service.notification.event.NotificationEventFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service per il recupero delle notifiche dal Sistema di Interscambio (SDI)
 * Effettua polling dell'endpoint SDI per verificare lo stato delle fatture inviate
 */
public class SdiNotificheService {
    
    private static final Logger logger = LogManager.getLogger(SdiNotificheService.class);
    
    private static final String DEFAULT_NOTIFICHE_ENDPOINT = "https://api.luna.itsolutions-cloud.com/ricevi-notifiche/index.php";
    private static final int CONNECT_TIMEOUT = 15000;
    private static final int READ_TIMEOUT = 30000;
    
    private final FatturaDAO fatturaDAO;
    private final SdiNotificaDAO sdiNotificaDAO;
    private final Properties companyProps = loadCompanyProps();
    
    public SdiNotificheService(FatturaDAO fatturaDAO, SdiNotificaDAO sdiNotificaDAO) {
        this.fatturaDAO = fatturaDAO;
        this.sdiNotificaDAO = sdiNotificaDAO;
    }
    
    /**
     * Recupera le notifiche per tutte le fatture inviate a SDI che non hanno ancora una notifica
     */
    public void sincronizzaTutteNotifiche() {
        logger.info("Inizio sincronizzazione delle notifiche SDI");
        try {
            // Cerca tutte le fatture REALE che hanno sdiCodice ma non hanno ancora notifica
            java.util.List<Fattura> fattureInAttesa = fatturaDAO.findFattureWithoutNotifica();
            
            int processate = 0;
            int successo = 0;
            int errori = 0;
            
            for (Fattura fattura : fattureInAttesa) {
                try {
                    if (fattura.getSdiCodice() != null && !fattura.getSdiCodice().isEmpty()) {
                        NotificaResponse response = recuperaNotifica(fattura.getSdiCodice());
                        
                        if (response.trovata) {
                            SdiNotifica notifica = new SdiNotifica();
                            notifica.setFattura(fattura);
                            notifica.setSdiCodice(fattura.getSdiCodice());
                            notifica.setStato(response.stato);
                            notifica.setXmlRisposta(response.xmlRisposta);
                            notifica.setDescrizioneErrore(response.descrizioneErrore);
                            
                            sdiNotificaDAO.save(notifica);
                            
                            // Aggiorna lo stato della fattura
                            fattura.setSdiStato(response.stato);
                            fatturaDAO.update(fattura);

                                String userId = resolveUserId(fattura);
                                if (userId != null) {
                                EventPublisher.getInstance().publishEvent(
                                    NotificationEventFactory.notificaSdi(
                                        userId,
                                        response.stato,
                                        response.descrizioneErrore,
                                        fattura.getNumero()
                                    )
                                );
                                }
                            
                            successo++;
                            logger.info("Notifica recuperata per fattura " + fattura.getNumero() + ": " + response.stato);
                        }
                    }
                    processate++;
                } catch (Exception e) {
                    errori++;
                    logger.warn("Errore nel recupero notifica per fattura " + fattura.getNumero() + ": " + e.getMessage());
                }
            }
            
            logger.info("Sincronizzazione notifiche completata: " + processate + " processate, " + successo + " con notifica, " + errori + " errori");
        } catch (Exception e) {
            logger.error("Errore nella sincronizzazione delle notifiche SDI", e);
        }
    }
    
    /**
     * Recupera la notifica per un codice SDI specifico
     * @param sdiCodice Codice univoco ricevuto da SDI
     * @return NotificaResponse con i dati della notifica
     */
    public NotificaResponse recuperaNotifica(String sdiCodice) {
        HttpURLConnection connection = null;
        try {
            String endpoint = getPropertyValue("sdi.notifiche.endpoint", DEFAULT_NOTIFICHE_ENDPOINT);
            String url = endpoint + "?codice=" + URLEncoder.encode(sdiCodice, "UTF-8");
            
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(CONNECT_TIMEOUT);
            connection.setReadTimeout(READ_TIMEOUT);
            connection.setRequestProperty("Accept", "application/xml, text/plain, */*");
            
            int statusCode = connection.getResponseCode();
            String responseBody = readResponseBody(connection, statusCode);
            
            if (statusCode >= 200 && statusCode < 300) {
                // Parsa la risposta XML per estrarre stato e dati
                NotificaResponse response = parseXmlResponse(responseBody);
                logger.info("Notifica recuperata per codice " + sdiCodice + ": " + response.stato);
                return response;
            } else {
                logger.warn("Endpoint SDI ha restituito status " + statusCode + " per codice " + sdiCodice);
                return new NotificaResponse(false, null, "HTTP " + statusCode, responseBody);
            }
        } catch (Exception e) {
            logger.error("Errore nel recupero della notifica per codice " + sdiCodice, e);
            return new NotificaResponse(false, null, "Errore: " + e.getMessage(), null);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
    
    /**
     * Parsa una risposta XML SDI per estrarre lo stato della notifica
     * Formato atteso: <Notifica><Stato>ACCETTATA|SCARTATA|ERRORE</Stato><Descrizione>...</Descrizione></Notifica>
     */
    private NotificaResponse parseXmlResponse(String xmlBody) {
        try {
            // Simple XML parsing con regex
            Pattern statoPattern = Pattern.compile("<Stato>([^<]+)</Stato>");
            Pattern descrizionePattern = Pattern.compile("<Descrizione>([^<]*)</Descrizione>");
            Pattern errorePattern = Pattern.compile("<Errore>([^<]*)</Errore>");
            
            Matcher statoMatcher = statoPattern.matcher(xmlBody);
            String stato = null;
            if (statoMatcher.find()) {
                stato = statoMatcher.group(1).toUpperCase();
            } else {
                // Fallback: controlla se la risposta contiene parole chiave
                if (xmlBody.toUpperCase().contains("ACCETTATA") || xmlBody.toUpperCase().contains("ACCEPTED")) {
                    stato = "ACCETTATA";
                } else if (xmlBody.toUpperCase().contains("SCARTATA") || xmlBody.toUpperCase().contains("REJECTED")) {
                    stato = "SCARTATA";
                } else if (xmlBody.toUpperCase().contains("ERRORE") || xmlBody.toUpperCase().contains("ERROR")) {
                    stato = "ERRORE";
                }
            }
            
            String descrizione = null;
            Matcher descrizioneMatcher = descrizionePattern.matcher(xmlBody);
            if (descrizioneMatcher.find()) {
                descrizione = descrizioneMatcher.group(1);
            }
            
            // Se non trovata descrizione, cerca Errore
            if (descrizione == null || descrizione.isEmpty()) {
                Matcher erroreMatcher = errorePattern.matcher(xmlBody);
                if (erroreMatcher.find()) {
                    descrizione = erroreMatcher.group(1);
                }
            }
            
            boolean trovata = stato != null;
            return new NotificaResponse(trovata, stato, descrizione, xmlBody);
            
        } catch (Exception e) {
            logger.warn("Errore nel parsing della risposta XML SDI: " + e.getMessage());
            return new NotificaResponse(false, null, "Errore parsing: " + e.getMessage(), xmlBody);
        }
    }
    
    private String readResponseBody(HttpURLConnection connection, int statusCode) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                statusCode >= 200 && statusCode < 300 ? connection.getInputStream() : connection.getErrorStream(),
                "UTF-8"))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString().trim();
        } catch (Exception e) {
            return "";
        }
    }
    
    private Properties loadCompanyProps() {
        try {
            Properties props = new Properties();
            props.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("application.properties"));
            return props;
        } catch (Exception e) {
            logger.warn("Errore nel caricamento application.properties: " + e.getMessage());
            return new Properties();
        }
    }
    
    private String getPropertyValue(String key, String defaultValue) {
        String value = companyProps.getProperty(key);
        return value != null ? value : defaultValue;
    }

    private String resolveUserId(Fattura fattura) {
        if (fattura.getCreatedBy() != null) {
            return String.valueOf(fattura.getCreatedBy().getId());
        }

        User admin = new UserDAO().findByUsername("admin");
        return admin != null ? String.valueOf(admin.getId()) : null;
    }
    
    /**
     * Inner class per rappresentare la risposta di una notifica SDI
     */
    public static class NotificaResponse {
        public final boolean trovata;
        public final String stato; // ACCETTATA, SCARTATA, ERRORE, CONSEGNATA
        public final String descrizioneErrore;
        public final String xmlRisposta;
        
        public NotificaResponse(boolean trovata, String stato, String descrizioneErrore, String xmlRisposta) {
            this.trovata = trovata;
            this.stato = stato;
            this.descrizioneErrore = descrizioneErrore;
            this.xmlRisposta = xmlRisposta;
        }
    }
}
