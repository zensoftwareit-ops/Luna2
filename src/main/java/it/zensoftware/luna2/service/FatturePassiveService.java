package it.zensoftware.luna2.service;

import it.zensoftware.luna2.model.FatturaPassiva;
import it.zensoftware.luna2.model.Fornitore;
import it.zensoftware.luna2.dao.FatturaPassivaDAO;
import it.zensoftware.luna2.dao.FornitoreDAO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

/**
 * Service per il recupero delle fatture passive (ricevute) da SDI
 * Sincronizza automaticamente le fatture ricevute tramite endpoint SDI
 */
public class FatturePassiveService {
    
    private static final Logger logger = LogManager.getLogger(FatturePassiveService.class);
    
    private static final String DEFAULT_RICEVI_FATTURE_ENDPOINT = "https://api.luna.itsolutions-cloud.com/ricevi-fatture/index.php";
    private static final int CONNECT_TIMEOUT = 15000;
    private static final int READ_TIMEOUT = 30000;
    
    private final FatturaPassivaDAO fatturaPassivaDAO;
    private final FornitoreDAO fornitoreDAO;
    private final Properties companyProps = loadCompanyProps();
    
    public FatturePassiveService(FatturaPassivaDAO fatturaPassivaDAO, FornitoreDAO fornitoreDAO) {
        this.fatturaPassivaDAO = fatturaPassivaDAO;
        this.fornitoreDAO = fornitoreDAO;
    }
    
    /**
     * Sincronizza le fatture passive ricevute da SDI per la propria azienda
     * @param partitaIva della propria azienda per cui recuperare le fatture
     */
    public void sincronizzaFatturePassive(String partitaIva) {
        logger.info("Inizio sincronizzazione fatture passive per PIVA: " + partitaIva);
        try {
            List<FatturaPassiva> fattureRicevute = recuperaFatturePassive(partitaIva);
            
            int processate = 0;
            int inserite = 0;
            int aggiornate = 0;
            int errori = 0;
            
            for (FatturaPassiva fattura : fattureRicevute) {
                try {
                    // Verifica se la fattura esiste già
                    FatturaPassiva esistente = null;
                    if (fattura.getSdiIdMessaggio() != null) {
                        esistente = fatturaPassivaDAO.findBySdiIdMessaggio(fattura.getSdiIdMessaggio());
                    }
                    
                    if (esistente != null) {
                        // Aggiorna la fattura esistente
                        fattura.setId(esistente.getId());
                        fatturaPassivaDAO.update(fattura);
                        aggiornate++;
                        logger.info("Fattura passiva aggiornata: " + fattura.getNumero() + " da " + fattura.getFornitoreNome());
                    } else {
                        // Inserisci nuova fattura
                        fatturaPassivaDAO.save(fattura);
                        inserite++;
                        logger.info("Fattura passiva ricevuta: " + fattura.getNumero() + " da " + fattura.getFornitoreNome());
                    }
                    processate++;
                } catch (Exception e) {
                    errori++;
                    logger.warn("Errore nell'elaborazione della fattura: " + fattura.getNumero() + " - " + e.getMessage());
                }
            }
            
            logger.info("Sincronizzazione fatture passive completata: " + processate + " processate, " + 
                       inserite + " inserite, " + aggiornate + " aggiornate, " + errori + " errori");
            
        } catch (Exception e) {
            logger.error("Errore nella sincronizzazione delle fatture passive", e);
        }
    }
    
    /**
     * Recupera le fatture passive dall'endpoint SDI
     * @param partitaIva della propria azienda
     * @return Lista di FatturaPassiva parsate dalla risposta
     */
    public List<FatturaPassiva> recuperaFatturePassive(String partitaIva) {
        HttpURLConnection connection = null;
        List<FatturaPassiva> fatture = new ArrayList<>();
        
        try {
            String endpoint = getPropertyValue("sdi.ricevi-fatture.endpoint", DEFAULT_RICEVI_FATTURE_ENDPOINT);
            String url = endpoint + "?piva=" + URLEncoder.encode(partitaIva, "UTF-8");
            
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(CONNECT_TIMEOUT);
            connection.setReadTimeout(READ_TIMEOUT);
            connection.setRequestProperty("Accept", "application/xml, text/plain, */*");
            
            int statusCode = connection.getResponseCode();
            String responseBody = readResponseBody(connection, statusCode);
            
            if (statusCode >= 200 && statusCode < 300) {
                fatture = parseXmlRisposta(responseBody, partitaIva);
                logger.info(fatture.size() + " fatture passive recuperate da SDI");
            } else {
                logger.warn("Endpoint SDI ha restituito status " + statusCode);
            }
            
            return fatture;
            
        } catch (Exception e) {
            logger.error("Errore nel recupero delle fatture passive da SDI", e);
            return fatture;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
    
    /**
     * Parsa la risposta XML da SDI per estrarre le fatture
     * Formato atteso: <Fatture><Fattura><Numero>...</Numero>...</Fattura>...</Fatture>
     */
    private List<FatturaPassiva> parseXmlRisposta(String xmlBody, String partitaIvaCliente) {
        List<FatturaPassiva> fatture = new ArrayList<>();
        
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new java.io.ByteArrayInputStream(xmlBody.getBytes("UTF-8")));
            
            // Cerca elementi Fattura
            NodeList fattureNodes = doc.getElementsByTagName("Fattura");
            
            for (int i = 0; i < fattureNodes.getLength(); i++) {
                try {
                    Element fatturaElement = (Element) fattureNodes.item(i);
                    FatturaPassiva fattura = parseFatturaElement(fatturaElement);
                    
                    if (fattura != null) {
                        fatture.add(fattura);
                    }
                } catch (Exception e) {
                    logger.warn("Errore nel parsing di una fattura individuale: " + e.getMessage());
                }
            }
            
            logger.info("Parsed " + fatture.size() + " fatture dal XML di risposta");
            
        } catch (Exception e) {
            logger.error("Errore nel parsing XML delle fatture passive: " + e.getMessage());
        }
        
        return fatture;
    }
    
    /**
     * Parsa un singolo elemento Fattura dal DOM
     */
    private FatturaPassiva parseFatturaElement(Element fatturaElement) {
        try {
            FatturaPassiva fattura = new FatturaPassiva();
            
            // Estrai dati dalla fattura
            fattura.setNumero(getElementValue(fatturaElement, "Numero"));
            fattura.setOggetto(getElementValue(fatturaElement, "Oggetto"));
            fattura.setNoteGenerali(getElementValue(fatturaElement, "Note"));
            
            // Dati fornitore
            fattura.setFornitoreNome(getElementValue(fatturaElement, "FornitoreNome"));
            fattura.setFornitorePiva(getElementValue(fatturaElement, "FornitorePiva"));
            
            // Importi
            String imponibileStr = getElementValue(fatturaElement, "Imponibile");
            if (imponibileStr != null && !imponibileStr.isEmpty()) {
                fattura.setImponibile(new BigDecimal(imponibileStr.replace(",", ".")));
            }
            
            String ivaStr = getElementValue(fatturaElement, "IVA");
            if (ivaStr != null && !ivaStr.isEmpty()) {
                fattura.setIva(new BigDecimal(ivaStr.replace(",", ".")));
            }
            
            String totaleStr = getElementValue(fatturaElement, "Totale");
            if (totaleStr != null && !totaleStr.isEmpty()) {
                fattura.setTotale(new BigDecimal(totaleStr.replace(",", ".")));
            }
            
            // Date
            String dataFatturaStr = getElementValue(fatturaElement, "DataFattura");
            if (dataFatturaStr != null && !dataFatturaStr.isEmpty()) {
                try {
                    fattura.setDataFattura(new SimpleDateFormat("yyyy-MM-dd").parse(dataFatturaStr));
                    fattura.setAnno(Integer.parseInt(dataFatturaStr.substring(0, 4)));
                } catch (Exception e) {
                    logger.warn("Errore nel parsing della data: " + dataFatturaStr);
                }
            }
            
            String dataScadenzaStr = getElementValue(fatturaElement, "DataScadenza");
            if (dataScadenzaStr != null && !dataScadenzaStr.isEmpty()) {
                try {
                    fattura.setDataScadenza(new SimpleDateFormat("yyyy-MM-dd").parse(dataScadenzaStr));
                } catch (Exception e) {
                    logger.warn("Errore nel parsing della data scadenza: " + dataScadenzaStr);
                }
            }
            
            // Dati SDI
            fattura.setSdiIdMessaggio(getElementValue(fatturaElement, "IdMessaggio"));
            fattura.setStatoRicezione(getElementValue(fatturaElement, "Stato"));
            
            // Ricerca fornitore nel DB se esiste
            if (fattura.getFornitorePiva() != null && !fattura.getFornitorePiva().isEmpty()) {
                try {
                    Fornitore fornitore = fornitoreDAO.findByPartitaIva(fattura.getFornitorePiva());
                    if (fornitore != null) {
                        fattura.setFornitore(fornitore);
                    }
                } catch (Exception e) {
                    logger.debug("Fornitore con PIVA " + fattura.getFornitorePiva() + " non trovato nel DB");
                }
            }
            
            fattura.setDataRicezione(new Date()); // Timestamp di ricezione sincronizzazione
            fattura.setStatoPagamento(FatturaPassiva.StatoPagamento.DA_PAGARE);
            
            return fattura;
            
        } catch (Exception e) {
            logger.error("Errore nel parsing dell'elemento Fattura: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Estrae il valore di un elemento XML
     */
    private String getElementValue(Element element, String tagName) {
        try {
            NodeList nodeList = element.getElementsByTagName(tagName);
            if (nodeList.getLength() > 0) {
                return nodeList.item(0).getTextContent();
            }
        } catch (Exception e) {
            logger.debug("Elemento non trovato: " + tagName);
        }
        return null;
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
            logger.warn("Errore nella lettura della risposta: " + e.getMessage());
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
}
