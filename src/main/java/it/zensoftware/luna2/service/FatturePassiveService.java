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
    private static final String DEFAULT_FATTURA_RICEVUTA_ENDPOINT = "https://api.luna.itsolutions-cloud.com/fattura-ricevuta/index.php";
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

                        // Notifica endpoint esterno anche su update: il servizio remoto può essere idempotente
                        notificaFatturaRicevutaSafe(fattura);
                    } else {
                        // Inserisci nuova fattura
                        fatturaPassivaDAO.save(fattura);
                        inserite++;
                        logger.info("Fattura passiva ricevuta: " + fattura.getNumero() + " da " + fattura.getFornitoreNome());

                        notificaFatturaRicevutaSafe(fattura);
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

            // XML fattura e codice (se presenti nella risposta)
            String xmlSdi = firstNonEmpty(
                    getElementValue(fatturaElement, "xml"),
                    getElementValue(fatturaElement, "XML"),
                    getElementValue(fatturaElement, "Xml"),
                    getElementValue(fatturaElement, "XmlSdi"),
                    getElementValue(fatturaElement, "FileXML"),
                    getElementValue(fatturaElement, "FileXml")
            );
            if (xmlSdi != null && !xmlSdi.isEmpty()) {
                fattura.setXmlSdi(xmlSdi);
            }

            String codice = firstNonEmpty(
                    getElementValue(fatturaElement, "codice"),
                    getElementValue(fatturaElement, "Codice")
            );
            if ((codice == null || codice.isEmpty()) && xmlSdi != null && !xmlSdi.isEmpty()) {
                codice = extractCodiceFromXml(xmlSdi);
            }
            fattura.setCodice(codice);
            
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

    private void notificaFatturaRicevutaSafe(FatturaPassiva fattura) {
        try {
            String codice = fattura != null ? fattura.getCodice() : null;
            if (codice == null || codice.trim().isEmpty()) {
                logger.warn("Skip chiamata fattura-ricevuta: tag codice mancante per fattura " + (fattura != null ? fattura.getNumero() : "(null)"));
                return;
            }
            notificaFatturaRicevuta(codice.trim());
        } catch (Exception e) {
            logger.warn("Errore nella notifica fattura-ricevuta: " + e.getMessage());
        }
    }

    /**
     * Chiama endpoint esterno dopo import fattura passiva.
     * GET https://api.luna.itsolutions-cloud.com/fattura-ricevuta/index.php?codice=...
     */
    private void notificaFatturaRicevuta(String codice) {
        HttpURLConnection connection = null;
        try {
            String endpoint = getPropertyValue("sdi.fattura-ricevuta.endpoint", DEFAULT_FATTURA_RICEVUTA_ENDPOINT);
            String separator = endpoint.contains("?") ? "&" : "?";
            String url = endpoint + separator + "codice=" + URLEncoder.encode(codice, "UTF-8");

            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(CONNECT_TIMEOUT);
            connection.setReadTimeout(READ_TIMEOUT);
            connection.setRequestProperty("Accept", "text/plain, application/xml, */*");

            int statusCode = connection.getResponseCode();
            String responseBody = readResponseBody(connection, statusCode);

            if (statusCode >= 200 && statusCode < 300) {
                logger.info("Notifica fattura-ricevuta OK per codice=" + codice + " (HTTP " + statusCode + ")");
            } else {
                logger.warn("Notifica fattura-ricevuta KO per codice=" + codice + " (HTTP " + statusCode + ") body=" + truncate(responseBody, 300));
            }
        } catch (Exception e) {
            logger.warn("Errore chiamando endpoint fattura-ricevuta per codice=" + codice + ": " + e.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String extractCodiceFromXml(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new java.io.ByteArrayInputStream(xml.getBytes("UTF-8")));

            NodeList n1 = doc.getElementsByTagName("codice");
            if (n1 != null && n1.getLength() > 0) {
                String v = n1.item(0).getTextContent();
                return v != null ? v.trim() : null;
            }
            NodeList n2 = doc.getElementsByTagName("Codice");
            if (n2 != null && n2.getLength() > 0) {
                String v = n2.item(0).getTextContent();
                return v != null ? v.trim() : null;
            }
        } catch (Exception e) {
            logger.debug("Impossibile estrarre <codice> dall'XML: " + e.getMessage());
        }
        return null;
    }

    private String firstNonEmpty(String... values) {
        if (values == null) return null;
        for (String v : values) {
            if (v != null && !v.trim().isEmpty()) {
                return v.trim();
            }
        }
        return null;
    }

    private String truncate(String s, int maxLen) {
        if (s == null) return null;
        if (s.length() <= maxLen) return s;
        return s.substring(0, maxLen) + "...";
    }

    /**
     * Parsa un singolo XML FatturaPA e crea un oggetto FatturaPassiva
     * @param xmlString XML da parsare
     * @return FatturaPassiva entity popolata
     */
    public FatturaPassiva parseSingleFatturaXML(String xmlString) throws Exception {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new java.io.ByteArrayInputStream(xmlString.getBytes("UTF-8")));

            FatturaPassiva fattura = new FatturaPassiva();

            // Estrai IdentificativoSdI
            NodeList idSdiNodes = doc.getElementsByTagName("IdentificativoSdI");
            if (idSdiNodes.getLength() > 0) {
                fattura.setSdiIdMessaggio(idSdiNodes.item(0).getTextContent());
            }

            // Estrai dati del cedente prestatore (fornitore)
            NodeList cedenteNodes = doc.getElementsByTagName("CedentePrestatore");
            if (cedenteNodes.getLength() > 0) {
                Element cedente = (Element) cedenteNodes.item(0);
                
                // Denominazione o Nome + Cognome
                NodeList denomNodes = cedente.getElementsByTagName("Denominazione");
                if (denomNodes.getLength() > 0) {
                    fattura.setFornitoreNome(denomNodes.item(0).getTextContent());
                } else {
                    String nome = "";
                    String cognome = "";
                    NodeList nomeNodes = cedente.getElementsByTagName("Nome");
                    NodeList cognomeNodes = cedente.getElementsByTagName("Cognome");
                    if (nomeNodes.getLength() > 0) nome = nomeNodes.item(0).getTextContent();
                    if (cognomeNodes.getLength() > 0) cognome = cognomeNodes.item(0).getTextContent();
                    fattura.setFornitoreNome((nome + " " + cognome).trim());
                }

                // Partita IVA
                NodeList pivaNodes = cedente.getElementsByTagName("IdCodice");
                if (pivaNodes.getLength() > 0) {
                    fattura.setFornitorePiva(pivaNodes.item(0).getTextContent());
                }
            }

            // Estrai dati generali documento
            NodeList datiDocNodes = doc.getElementsByTagName("DatiGeneraliDocumento");
            if (datiDocNodes.getLength() > 0) {
                Element datiDoc = (Element) datiDocNodes.item(0);

                // Numero fattura
                NodeList numeroNodes = datiDoc.getElementsByTagName("Numero");
                if (numeroNodes.getLength() > 0) {
                    fattura.setNumero(numeroNodes.item(0).getTextContent());
                }

                // Data fattura
                NodeList dataNodes = datiDoc.getElementsByTagName("Data");
                if (dataNodes.getLength() > 0) {
                    String dataStr = dataNodes.item(0).getTextContent();
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                    fattura.setDataFattura(sdf.parse(dataStr));
                    fattura.setAnno(Integer.parseInt(dataStr.substring(0, 4)));
                }

                // Totale documento
                NodeList totaleNodes = datiDoc.getElementsByTagName("ImportoTotaleDocumento");
                if (totaleNodes.getLength() > 0) {
                    fattura.setTotale(new BigDecimal(totaleNodes.item(0).getTextContent()));
                }
            }

            // Estrai imponibile e IVA da DatiRiepilogo
            NodeList riepilogoNodes = doc.getElementsByTagName("DatiRiepilogo");
            if (riepilogoNodes.getLength() > 0) {
                Element riepilogo = (Element) riepilogoNodes.item(0);

                NodeList imponibileNodes = riepilogo.getElementsByTagName("ImponibileImporto");
                if (imponibileNodes.getLength() > 0) {
                    fattura.setImponibile(new BigDecimal(imponibileNodes.item(0).getTextContent()));
                }

                NodeList impostaNodes = riepilogo.getElementsByTagName("Imposta");
                if (impostaNodes.getLength() > 0) {
                    fattura.setIva(new BigDecimal(impostaNodes.item(0).getTextContent()));
                }
            }

            logger.info("XML FatturaPA parsato per fattura passiva: " + fattura.getNumero());
            return fattura;

        } catch (Exception e) {
            logger.error("Errore nel parsing XML fattura passiva", e);
            throw new RuntimeException("Errore nel parsing XML: " + e.getMessage(), e);
        }
    }
}
