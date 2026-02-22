package it.zensoftware.luna2.service;

import it.zensoftware.luna2.model.Fattura;
import it.zensoftware.luna2.model.FatturaRiga;
import it.zensoftware.luna2.model.Cliente;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Service for generating FatturaPA XML format compliant with SDI (Sistema di Interscambio)
 * Generates XML according to FatturaPA 1.2.2 standard for Italian electronic invoicing
 */
public class FatturaXMLService {
    private static final Logger logger = LogManager.getLogger(FatturaXMLService.class);
    
    private static final String VERSIONE_FORMATO = "FPR12";
    private static final String CODICE_PAESE = "IT";
    private static final String PROGRESSIVO_PATTERN = "yyMMddHHmm";
    private static final String DEFAULT_CODICE_DESTINATARIO = "0000000";
    private static final String DEFAULT_SDI_ENDPOINT = "https://api.luna.itsolutions-cloud.com/invia-sdi/index.php";

    private final Properties companyProps = loadCompanyProps();

    /**
     * Genera XML FatturaPA per una Fattura Reale
     * @param fattura Fattura entity (solo REALE)
     * @return XML string conforme FatturaPA 1.2.2
     * @throws Exception Se la fattura non è di tipo REALE o altri errori
     */
    public String generateFatturaXML(Fattura fattura) throws Exception {
        if (fattura == null) {
            throw new IllegalArgumentException("Fattura non può essere nulla");
        }
        
        if (!Fattura.TipoFattura.REALE.equals(fattura.getTipoFattura())) {
            throw new IllegalArgumentException("L'XML SDI può essere generato solo per Fatture REALI, non per Proforma");
        }

        if (fattura.getCliente() == null) {
            throw new IllegalArgumentException("La fattura deve avere un cliente associato");
        }

        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.newDocument();

            // Root element: p:FatturaElettronica
            Element fatturaElettronica = doc.createElement("p:FatturaElettronica");
            fatturaElettronica.setAttribute("xmlns:ds", "http://www.w3.org/2000/09/xmldsig#");
            fatturaElettronica.setAttribute("xmlns:p", "http://ivaservizi.agenziaentrate.gov.it/docs/xsd/fatture/v1.2");
            fatturaElettronica.setAttribute("versione", VERSIONE_FORMATO);
            doc.appendChild(fatturaElettronica);

            // FatturaElettronicaHeader
            Element header = createHeader(doc, fattura);
            fatturaElettronica.appendChild(header);

            // FatturaElettronicaBody
            Element body = doc.createElement("FatturaElettronicaBody");
            fatturaElettronica.appendChild(body);

            // DatiGenerali
            body.appendChild(createDatiGenerali(doc, fattura));

            // DatiBeniServizi (Righe fattura)
            body.appendChild(createDatiBeniServizi(doc, fattura));

            // DatiPagamento
            body.appendChild(createDatiPagamento(doc, fattura));

            // Convert to String
            String xmlString = documentToString(doc);
            logger.info("XML FatturaPA generato per fattura " + fattura.getNumero());
            return xmlString;

        } catch (Exception e) {
            logger.error("Errore nella generazione XML FatturaPA", e);
            throw new RuntimeException("Errore nella generazione XML: " + e.getMessage(), e);
        }
    }

    private Element createHeader(Document doc, Fattura fattura) {
        Element header = doc.createElement("FatturaElettronicaHeader");

        Element datiTrasmissione = doc.createElement("DatiTrasmissione");
        Element idTrasmittente = doc.createElement("IdTrasmittente");
        addElement(doc, idTrasmittente, "IdPaese", CODICE_PAESE);
        addElement(doc, idTrasmittente, "IdCodice", getCompanyValue("company.vat", "00000000000"));
        datiTrasmissione.appendChild(idTrasmittente);
        addElement(doc, datiTrasmissione, "ProgressivoInvio", buildProgressivoInvio());
        addElement(doc, datiTrasmissione, "FormatoTrasmissione", VERSIONE_FORMATO);

        String codiceDestinatario = safeValue(fattura.getCliente().getCodiceSdi(), DEFAULT_CODICE_DESTINATARIO);
        addElement(doc, datiTrasmissione, "CodiceDestinatario", codiceDestinatario);

        if (fattura.getCliente().getPec() != null && !fattura.getCliente().getPec().isEmpty()
                && DEFAULT_CODICE_DESTINATARIO.equals(codiceDestinatario)) {
            addElement(doc, datiTrasmissione, "PECDestinatario", fattura.getCliente().getPec());
        }

        header.appendChild(datiTrasmissione);
        header.appendChild(createCedentePrestatore(doc));
        header.appendChild(createCessionarioCommittente(doc, fattura.getCliente()));
        return header;
    }

    private Element createDatiGenerali(Document doc, Fattura fattura) {
        Element datiGenerali = doc.createElement("DatiGenerali");

        Element datiGeneraliDocumento = doc.createElement("DatiGeneraliDocumento");
        
        // Tipo documento: TD01 = Fattura
        addElement(doc, datiGeneraliDocumento, "TipoDocumento", "TD01");
        addElement(doc, datiGeneraliDocumento, "Divisa", "EUR");
        addElement(doc, datiGeneraliDocumento, "Data", formatDate(fattura.getDataFattura()));
        addElement(doc, datiGeneraliDocumento, "Numero", fattura.getNumero());
        
        // Causale (opzionale)
        addElement(doc, datiGeneraliDocumento, "Causale", "Fattura di vendita");
        
        // Importo Totale Documento
        addElement(doc, datiGeneraliDocumento, "ImportoTotaleDocumento", 
            fattura.getTotale() != null ? formatBigDecimal(fattura.getTotale()) : "0.00");

        datiGenerali.appendChild(datiGeneraliDocumento);

        return datiGenerali;
    }

    private Element createDatiBeniServizi(Document doc, Fattura fattura) {
        Element datiBeniServizi = doc.createElement("DatiBeniServizi");

        List<FatturaRiga> righe = fattura.getRighe();
        if (righe != null && !righe.isEmpty()) {
            for (int i = 0; i < righe.size(); i++) {
                FatturaRiga riga = righe.get(i);
                Element dettaglioLinee = doc.createElement("DettaglioLinee");

                addElement(doc, dettaglioLinee, "NumeroLinea", String.valueOf(i + 1));
                
                String descrizione = riga.getDescrizione() != null ? riga.getDescrizione() : 
                    (riga.getProdotto() != null ? riga.getProdotto().getNome() : "Descrizione");
                addElement(doc, dettaglioLinee, "Descrizione", descrizione);

                addElement(doc, dettaglioLinee, "Quantita", 
                    riga.getQuantita() != null ? formatBigDecimal(riga.getQuantita()) : "1.00");

                addElement(doc, dettaglioLinee, "PrezzoUnitario", 
                    riga.getPrezzoUnitario() != null ? formatBigDecimal(riga.getPrezzoUnitario()) : "0.00");

                addElement(doc, dettaglioLinee, "PrezzoTotale", 
                    riga.getTotaleRiga() != null ? formatBigDecimal(riga.getTotaleRiga()) : "0.00");

                // Aliquota IVA
                addElement(doc, dettaglioLinee, "AliquotaIVA", 
                    riga.getIvaPercentuale() != null ? formatBigDecimal(riga.getIvaPercentuale()) : "0.00");

                datiBeniServizi.appendChild(dettaglioLinee);
            }
        }

        datiBeniServizi.appendChild(createDatiRiepilogo(doc, fattura));
        return datiBeniServizi;
    }

    private Element createDatiPagamento(Document doc, Fattura fattura) {
        Element datiPagamento = doc.createElement("DatiPagamento");

        addElement(doc, datiPagamento, "CondizioniPagamento", "TP02"); // Bonifico bancario

        Element dettaglioPagamento = doc.createElement("DettaglioPagamento");
        addElement(doc, dettaglioPagamento, "ModalitaPagamento", "TRB"); // Bonifico
        addElement(doc, dettaglioPagamento, "DataScadenzaPagamento", 
            formatDate(fattura.getDataFattura())); // Data fattura come default
        addElement(doc, dettaglioPagamento, "ImportoPagamento", 
            fattura.getTotale() != null ? formatBigDecimal(fattura.getTotale()) : "0.00");

        datiPagamento.appendChild(dettaglioPagamento);
        return datiPagamento;
    }

    private Element createDatiRiepilogo(Document doc, Fattura fattura) {
        Element datiRiepilogo = doc.createElement("DatiRiepilogo");

        // Importo imponibile
        addElement(doc, datiRiepilogo, "ImponibileImporto", 
            fattura.getImponibile() != null ? formatBigDecimal(fattura.getImponibile()) : "0.00");

        // IVA
        addElement(doc, datiRiepilogo, "Imposta", 
            fattura.getIva() != null ? formatBigDecimal(fattura.getIva()) : "0.00");

        BigDecimal aliquota = resolveAliquotaIva(fattura.getRighe());
        addElement(doc, datiRiepilogo, "AliquotaIVA", formatBigDecimal(aliquota));

        if (BigDecimal.ZERO.compareTo(aliquota) == 0) {
            addElement(doc, datiRiepilogo, "Natura", "N1");
        }

        return datiRiepilogo;
    }

    private Element createCedentePrestatore(Document doc) {
        Element cedente = doc.createElement("CedentePrestatore");
        Element datiAnagrafici = doc.createElement("DatiAnagrafici");
        Element idFiscaleIva = doc.createElement("IdFiscaleIVA");
        addElement(doc, idFiscaleIva, "IdPaese", CODICE_PAESE);
        addElement(doc, idFiscaleIva, "IdCodice", getCompanyValue("company.vat", "00000000000"));
        datiAnagrafici.appendChild(idFiscaleIva);
        addElement(doc, datiAnagrafici, "CodiceFiscale", getCompanyValue("company.taxCode", "00000000000"));

        Element anagrafica = doc.createElement("Anagrafica");
        addElement(doc, anagrafica, "Denominazione", getCompanyValue("company.name", "Luna2 SRL"));
        datiAnagrafici.appendChild(anagrafica);
        cedente.appendChild(datiAnagrafici);

        Element sede = doc.createElement("Sede");
        addElement(doc, sede, "Indirizzo", getCompanyValue("company.address", "Via Luna 2"));
        addElement(doc, sede, "CAP", getCompanyValue("company.cap", "20100"));
        addElement(doc, sede, "Comune", getCompanyValue("company.city", "Milano"));
        addElement(doc, sede, "Provincia", getCompanyValue("company.province", "MI"));
        addElement(doc, sede, "Nazione", getCompanyValue("company.country", CODICE_PAESE));
        cedente.appendChild(sede);

        return cedente;
    }

    private Element createCessionarioCommittente(Document doc, Cliente cliente) {
        Element cessionario = doc.createElement("CessionarioCommittente");
        Element datiAnagrafici = doc.createElement("DatiAnagrafici");

        if (cliente.getPartitaIva() != null && !cliente.getPartitaIva().isEmpty()) {
            Element idFiscaleIva = doc.createElement("IdFiscaleIVA");
            addElement(doc, idFiscaleIva, "IdPaese", CODICE_PAESE);
            addElement(doc, idFiscaleIva, "IdCodice", cliente.getPartitaIva());
            datiAnagrafici.appendChild(idFiscaleIva);
        }

        addElement(doc, datiAnagrafici, "CodiceFiscale", safeValue(cliente.getCodiceFiscale(), "00000000000"));

        Element anagrafica = doc.createElement("Anagrafica");
        addElement(doc, anagrafica, "Denominazione", safeValue(cliente.getRagioneSociale(), "Cliente"));
        datiAnagrafici.appendChild(anagrafica);
        cessionario.appendChild(datiAnagrafici);

        Element sede = doc.createElement("Sede");
        addElement(doc, sede, "Indirizzo", safeValue(cliente.getIndirizzo(), "Indirizzo"));
        addElement(doc, sede, "CAP", safeValue(cliente.getCap(), "00000"));
        addElement(doc, sede, "Comune", safeValue(cliente.getCitta(), "Citta"));
        addElement(doc, sede, "Provincia", safeValue(cliente.getProvincia(), "XX"));
        addElement(doc, sede, "Nazione", CODICE_PAESE);
        cessionario.appendChild(sede);

        return cessionario;
    }

    /**
     * Helper: Aggiunge un elemento con testo
     */
    private void addElement(Document doc, Element parent, String tagName, String value) {
        Element element = doc.createElement(tagName);
        element.appendChild(doc.createTextNode(value != null ? value : ""));
        parent.appendChild(element);
    }

    /**
     * Formatta una data nel formato richiesto (YYYYMMDD)
     */
    private String formatDate(java.util.Date date) {
        if (date == null) {
            return new SimpleDateFormat("yyyyMMdd").format(new java.util.Date());
        }
        return new SimpleDateFormat("yyyyMMdd").format(date);
    }

    /**
     * Formatta un BigDecimal con 2 decimali
     */
    private String formatBigDecimal(BigDecimal value) {
        if (value == null) {
            return "0.00";
        }
        return String.format("%.2f", value);
    }

    private String buildProgressivoInvio() {
        return new SimpleDateFormat(PROGRESSIVO_PATTERN).format(new java.util.Date());
    }

    private BigDecimal resolveAliquotaIva(List<FatturaRiga> righe) {
        if (righe == null || righe.isEmpty()) {
            return new BigDecimal("22.00");
        }
        for (FatturaRiga riga : righe) {
            if (riga.getIvaPercentuale() != null) {
                return riga.getIvaPercentuale();
            }
        }
        return new BigDecimal("22.00");
    }

    private Properties loadCompanyProps() {
        Properties props = new Properties();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            if (is != null) {
                props.load(is);
            }
        } catch (Exception e) {
            logger.warn("Impossibile leggere application.properties per dati aziendali");
        }
        return props;
    }

    private String getCompanyValue(String key, String fallback) {
        String value = companyProps.getProperty(key);
        return safeValue(value, fallback);
    }

    private String safeValue(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.trim();
    }

    /**
     * Converte il Document XML a String
     */
    private String documentToString(Document doc) throws Exception {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty("indent", "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        transformer.setOutputProperty("encoding", "UTF-8");

        StringWriter sw = new StringWriter();
        transformer.transform(new DOMSource(doc), new StreamResult(sw));
        
        String xmlString = sw.toString();
        // Rimuovi la dichiarazione XML standard e aggiungi quella corretta per FatturaPA
        xmlString = xmlString.replaceFirst("<\\?xml[^>]+\\?>", "<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        
        return xmlString;
    }

    /**
     * Valida un XML FatturaPA (sanity check di base)
     */
    public boolean validateXML(String xmlString) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.parse(new java.io.ByteArrayInputStream(xmlString.getBytes("UTF-8")));
            
            logger.info("XML FatturaPA validato con successo");
            return true;
        } catch (Exception e) {
            logger.error("Errore nella validazione XML: " + e.getMessage());
            return false;
        }
    }

    public SdiResponse sendToSdi(String xmlString) {
        HttpURLConnection connection = null;
        try {
            String endpoint = getCompanyValue("sdi.endpoint", DEFAULT_SDI_ENDPOINT);
            URL url = new URL(endpoint);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(30000);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/xml; charset=UTF-8");
            connection.setRequestProperty("Accept", "application/json, text/plain, */*");

            try (OutputStream os = connection.getOutputStream()) {
                os.write(xmlString.getBytes("UTF-8"));
            }

            int statusCode = connection.getResponseCode();
            String responseBody = readResponseBody(connection, statusCode);
            return new SdiResponse(statusCode, responseBody);
        } catch (Exception e) {
            logger.error("Errore invio XML a SDI endpoint", e);
            return new SdiResponse(500, "Errore invio SDI: " + e.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
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

    /**
     * Parsa un XML FatturaPA e crea un oggetto Fattura (per import)
     * @param xmlString XML FatturaPA da parsare
     * @return Fattura entity popolata con i dati dall'XML
     */
    public Fattura parseFatturaXML(String xmlString) throws Exception {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new java.io.ByteArrayInputStream(xmlString.getBytes("UTF-8")));

            Fattura fattura = new Fattura();
            fattura.setTipoFattura(Fattura.TipoFattura.REALE);

            // Estrai numero e data fattura da DatiGeneraliDocumento
            org.w3c.dom.NodeList numeroNodes = doc.getElementsByTagName("Numero");
            if (numeroNodes.getLength() > 0) {
                fattura.setNumero(numeroNodes.item(0).getTextContent());
            }

            org.w3c.dom.NodeList dataNodes = doc.getElementsByTagName("Data");
            if (dataNodes.getLength() > 0) {
                String dataStr = dataNodes.item(0).getTextContent();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
                fattura.setDataFattura(sdf.parse(dataStr));
                fattura.setAnno(Integer.parseInt(dataStr.substring(0, 4)));
            }

            // Estrai ImportoTotaleDocumento
            org.w3c.dom.NodeList totaleNodes = doc.getElementsByTagName("ImportoTotaleDocumento");
            if (totaleNodes.getLength() > 0) {
                fattura.setTotale(new BigDecimal(totaleNodes.item(0).getTextContent()));
            }

            // Estrai imponibile e IVA da DatiRiepilogo
            org.w3c.dom.NodeList imponibileNodes = doc.getElementsByTagName("ImponibileImporto");
            if (imponibileNodes.getLength() > 0) {
                fattura.setImponibile(new BigDecimal(imponibileNodes.item(0).getTextContent()));
            }

            org.w3c.dom.NodeList impostaNodes = doc.getElementsByTagName("Imposta");
            if (impostaNodes.getLength() > 0) {
                fattura.setIva(new BigDecimal(impostaNodes.item(0).getTextContent()));
            }

            // Estrai righe da DettaglioLinee
            List<FatturaRiga> righe = new ArrayList<>();
            org.w3c.dom.NodeList dettaglioLineeNodes = doc.getElementsByTagName("DettaglioLinee");
            for (int i = 0; i < dettaglioLineeNodes.getLength(); i++) {
                org.w3c.dom.Element dettaglio = (org.w3c.dom.Element) dettaglioLineeNodes.item(i);
                FatturaRiga riga = new FatturaRiga();

                org.w3c.dom.NodeList descNodes = dettaglio.getElementsByTagName("Descrizione");
                if (descNodes.getLength() > 0) {
                    riga.setDescrizione(descNodes.item(0).getTextContent());
                }

                org.w3c.dom.NodeList qtaNodes = dettaglio.getElementsByTagName("Quantita");
                if (qtaNodes.getLength() > 0) {
                    riga.setQuantita(new BigDecimal(qtaNodes.item(0).getTextContent()));
                }

                org.w3c.dom.NodeList prezzoNodes = dettaglio.getElementsByTagName("PrezzoUnitario");
                if (prezzoNodes.getLength() > 0) {
                    riga.setPrezzoUnitario(new BigDecimal(prezzoNodes.item(0).getTextContent()));
                }

                org.w3c.dom.NodeList totaleRigaNodes = dettaglio.getElementsByTagName("PrezzoTotale");
                if (totaleRigaNodes.getLength() > 0) {
                    riga.setTotaleRiga(new BigDecimal(totaleRigaNodes.item(0).getTextContent()));
                }

                org.w3c.dom.NodeList aliquotaNodes = dettaglio.getElementsByTagName("AliquotaIVA");
                if (aliquotaNodes.getLength() > 0) {
                    riga.setIvaPercentuale(new BigDecimal(aliquotaNodes.item(0).getTextContent()));
                }

                riga.setFattura(fattura);
                righe.add(riga);
            }
            fattura.setRighe(righe);

            logger.info("XML FatturaPA parsato con successo: " + fattura.getNumero());
            return fattura;
        } catch (Exception e) {
            logger.error("Errore nel parsing XML FatturaPA", e);
            throw new RuntimeException("Errore nel parsing XML: " + e.getMessage(), e);
        }
    }

    public static class SdiResponse {
        private final int statusCode;
        private final String body;
        private final String sdiCodice;

        public SdiResponse(int statusCode, String body) {
            this.statusCode = statusCode;
            this.body = body;
            this.sdiCodice = extractCodiceFromJson(body);
        }

        public int getStatusCode() {
            return statusCode;
        }

        public String getBody() {
            return body;
        }
        
        public String getSdiCodice() {
            return sdiCodice;
        }

        public boolean isSuccess() {
            return statusCode >= 200 && statusCode < 300;
        }
        
        /**
         * Estrae il codice univoco dalla risposta JSON dell'endpoint SDI
         * Formato atteso: {"codice": "xxxxx"} o {"id": "xxxxx"}
         */
        private static String extractCodiceFromJson(String jsonBody) {
            if (jsonBody == null || jsonBody.isEmpty()) {
                return null;
            }
            
            try {
                // Simple JSON parsing per estrarre il campo "codice"
                String pattern1 = "\"codice\"\\s*:\\s*\"([^\"]+)\"";
                String pattern2 = "\"id\"\\s*:\\s*\"([^\"]+)\"";
                
                java.util.regex.Pattern p1 = java.util.regex.Pattern.compile(pattern1);
                java.util.regex.Matcher m1 = p1.matcher(jsonBody);
                if (m1.find()) {
                    return m1.group(1);
                }
                
                java.util.regex.Pattern p2 = java.util.regex.Pattern.compile(pattern2);
                java.util.regex.Matcher m2 = p2.matcher(jsonBody);
                if (m2.find()) {
                    return m2.group(1);
                }
            } catch (Exception e) {
                LogManager.getLogger(SdiResponse.class).warn("Errore nell'estrazione del codice SDI: " + e.getMessage());
            }
            
            return null;
        }
    }
}

