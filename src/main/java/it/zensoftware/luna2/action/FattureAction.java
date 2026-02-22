package it.zensoftware.luna2.action;

import com.opensymphony.xwork2.ActionSupport;
import it.zensoftware.luna2.dao.FatturaDAO;
import it.zensoftware.luna2.dao.FatturaRigaDAO;
import it.zensoftware.luna2.dao.ClienteDAO;
import it.zensoftware.luna2.dao.TrackingEmailDAO;
import it.zensoftware.luna2.dao.PreventivoDAO;
import it.zensoftware.luna2.dto.FatturaTrackingDTO;
import it.zensoftware.luna2.model.Fattura;
import it.zensoftware.luna2.model.FatturaRiga;
import it.zensoftware.luna2.model.Cliente;
import it.zensoftware.luna2.model.TrackingEmail;
import it.zensoftware.luna2.model.Preventivo;
import it.zensoftware.luna2.model.PreventivoRiga;
import it.zensoftware.luna2.model.Preventivo.Stato;
import it.zensoftware.luna2.service.EmailService;
import it.zensoftware.luna2.service.FatturaXMLService;
import it.zensoftware.luna2.service.FattureExportService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.struts2.ServletActionContext;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.Properties;

public class FattureAction extends ActionSupport {
    private static final Logger logger = LogManager.getLogger(FattureAction.class);
    private FatturaDAO fatturaDAO = new FatturaDAO();
    private FatturaRigaDAO fatturaRigaDAO = new FatturaRigaDAO();
    private ClienteDAO clienteDAO = new ClienteDAO();
    private TrackingEmailDAO trackingEmailDAO = new TrackingEmailDAO();
    private PreventivoDAO preventivoDAO = new PreventivoDAO();
    private EmailService emailService = new EmailService();
    private FatturaXMLService xmlService = new FatturaXMLService();
    private FattureExportService exportService = new FattureExportService();
    
    private Fattura fattura;
    private List<Fattura> fatture;
    private List<FatturaTrackingDTO> fattureConTracking;
    private List<Cliente> clienti;
    private List<FatturaRiga> righe;
    private FatturaRiga riga;
    
    private Long id;
    private Long rigaId;
    private Integer anno;
    private Fattura.TipoFattura tipo;
    private Long clienteId;
    private InputStream inputStream;
    private String contentDisposition;
    private String emailDestinatario;
    private String trackingId;
    private String messageEmail;
    private java.io.File uploadFile;
    private String uploadFileContentType;
    private String uploadFileFileName;

    public String list() {
        if (anno == null) {
            anno = Calendar.getInstance().get(Calendar.YEAR);
        }
        
        if (tipo != null) {
            fatture = fatturaDAO.findByTipo(tipo);
        } else {
            fatture = fatturaDAO.findByAnno(anno);
        }
        
        // Populate tracking data for each fattura
        fattureConTracking = new ArrayList<>();
        for (Fattura f : fatture) {
            Long totalEmails = trackingEmailDAO.countEmailsForFattura(f.getId());
            Long openedEmails = trackingEmailDAO.countOpensForFattura(f.getId());
            Long downloadedEmails = trackingEmailDAO.countDownloadsForFattura(f.getId());
            Long totalDownloads = trackingEmailDAO.getTotalDownloadCountFattura(f.getId());
            
            fattureConTracking.add(new FatturaTrackingDTO(f, totalEmails, openedEmails, downloadedEmails, totalDownloads));
        }
        
        return SUCCESS;
    }

    public String create() {
        fattura = new Fattura();
        fattura.setDataFattura(new Date());
        fattura.setAnno(Calendar.getInstance().get(Calendar.YEAR));
        fattura.setTipoFattura(Fattura.TipoFattura.PROFORMA);
        
        clienti = clienteDAO.findAll();
        
        return SUCCESS;
    }

    public String edit() {
        if (id != null) {
            fattura = fatturaDAO.findWithRighe(id);
            if (fattura == null) {
                addActionError("Fattura non trovata");
                return ERROR;
            }
            fattura.getRighe().size(); // Force load
            clienti = clienteDAO.findAll();
        }
        return SUCCESS;
    }

    public String save() {
        try {
            if (fattura == null) {
                addActionError("Fattura non valida");
                return ERROR;
            }

            if (fattura.getId() == null) {
                fatturaDAO.save(fattura);
            } else {
                fatturaDAO.update(fattura);
            }

            addActionMessage("Fattura salvata con successo");
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Errore nel salvataggio fattura", e);
            addActionError("Errore: " + e.getMessage());
            return ERROR;
        }
    }

    public String delete() {
        try {
            if (id != null) {
                fattura = fatturaDAO.findById(id);
                if (fattura != null) {
                    fatturaDAO.delete(fattura);
                    addActionMessage("Fattura eliminata");
                }
            }
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Errore nell'eliminazione", e);
            addActionError("Errore: " + e.getMessage());
            return ERROR;
        }
    }

    /**
     * Crea una fattura direttamente da un preventivo ACCETTATO
     * (usato quando il modulo Produzione è disabilitato)
     */
    public String createFromPreventivo() {
        try {
            if (id == null) {
                addActionError("ID preventivo non specificato");
                return ERROR;
            }
            
            // Carica preventivo con righe
            Preventivo preventivo = preventivoDAO.findWithRighe(id);
            if (preventivo == null) {
                addActionError("Preventivo non trovato");
                return ERROR;
            }
            
            // Verifica che il preventivo sia ACCETTATO
            if (preventivo.getStato() != Stato.ACCETTATO) {
                addActionError("Il preventivo deve essere ACCETTATO per creare una fattura");
                return ERROR;
            }
            
            // Crea nuova fattura
            Calendar cal = Calendar.getInstance();
            int anno = cal.get(Calendar.YEAR);
            
            fattura = new Fattura();
            fattura.setNumero(fatturaDAO.generaNuovoNumero(anno));
            fattura.setAnno(anno);
            fattura.setDataFattura(new Date());
            
            // Data scadenza 30 giorni
            cal.add(Calendar.DAY_OF_MONTH, 30);
            fattura.setDataScadenza(cal.getTime());
            
            // Copia dati dal preventivo
            fattura.setCliente(preventivo.getCliente());
            fattura.setOggetto(preventivo.getOggetto());
            fattura.setImponibile(preventivo.getImponibile());
            fattura.setIva(preventivo.getIva());
            fattura.setTotale(preventivo.getTotale());
            fattura.setTipoFattura(Fattura.TipoFattura.ORDINARIA);
            fattura.setStato(Fattura.StatoFattura.EMESSA);
            
            // Salva fattura
            fattura = fatturaDAO.save(fattura);
            
            // Copia righe dal preventivo
            if (preventivo.getRighe() != null && !preventivo.getRighe().isEmpty()) {
                for (PreventivoRiga rigaPrev : preventivo.getRighe()) {
                    FatturaRiga riga = new FatturaRiga();
                    riga.setFattura(fattura);
                    riga.setRigaNumero(rigaPrev.getRigaNumero());
                    riga.setDescrizione(rigaPrev.getDescrizione());
                    riga.setQuantita(rigaPrev.getQuantita());
                    riga.setPrezzoUnitario(rigaPrev.getPrezzoUnitario());
                    riga.setImportoTotale(rigaPrev.getImportoTotale());
                    fatturaRigaDAO.save(riga);
                }
            }
            
            // Aggiorna stato preventivo
            preventivo.setStato(Stato.CONVERTITO);
            preventivoDAO.update(preventivo);
            
            logger.info("Fattura {} creata da preventivo {}", fattura.getNumero(), preventivo.getNumero());
            addActionMessage("Fattura " + fattura.getNumero() + " creata con successo dal preventivo");
            
            // Imposta l'id della fattura per il redirect
            id = fattura.getId();
            return "redirect-fattura";
            
        } catch (IllegalStateException e) {
            logger.error("Business rule violation: {}", e.getMessage());
            addActionError(e.getMessage());
            return ERROR;
        } catch (Exception e) {
            logger.error("Errore nella creazione fattura da preventivo", e);
            addActionError("Errore nella creazione della fattura: " + e.getMessage());
            return ERROR;
        }
    }

    public String generatePdf() {
        try {
            if (id == null) {
                addActionError("Fattura non trovata");
                return ERROR;
            }

            fattura = fatturaDAO.findWithRighe(id);
            if (fattura == null) {
                addActionError("Fattura non trovata");
                return ERROR;
            }

            righe = fatturaDAO.findWithRighe(id).getRighe();

            Document document = new Document(PageSize.A4);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, baos);
            document.open();

            generaPdf(document);

            document.close();

            inputStream = new ByteArrayInputStream(baos.toByteArray());
            contentDisposition = "attachment; filename=\"Fattura_" + fattura.getNumero() + ".pdf\"";
            return SUCCESS;

        } catch (Exception e) {
            logger.error("Errore nella generazione PDF", e);
            addActionError("Errore: " + e.getMessage());
            return ERROR;
        }
    }

    public String sendEmail() {
        try {
            if (id == null || emailDestinatario == null || emailDestinatario.isEmpty()) {
                addActionError("Fattura e email ricevente sono obbligatori");
                return INPUT;
            }

            fattura = fatturaDAO.findWithRighe(id);
            if (fattura == null) {
                addActionError("Fattura non trovata");
                return ERROR;
            }

            // Leggi configurazione SMTP dalle properties
            Properties props = new Properties();
            try (java.io.InputStream is = this.getClass().getClassLoader().getResourceAsStream("application.properties")) {
                props.load(is);
            }

            String smtpHost = props.getProperty("smtp.host", "smtp.gmail.com");
            String smtpUsername = props.getProperty("smtp.username");
            String smtpPassword = props.getProperty("smtp.password");
            String smtpFromEmail = props.getProperty("smtp.username");

            if (smtpUsername == null || smtpPassword == null) {
                addActionError("Configurazione SMTP incompleta. Verifica application.properties");
                logger.error("SMTP configuration missing in application.properties");
                return ERROR;
            }

            String emailBodyMessage = messageEmail != null ? messageEmail : 
                "Allega la fattura numero " + fattura.getNumero() + " per la review.";

            // Invia email con tracciamento
            TrackingEmail tracking = emailService.sendFatturaEmail(fattura, emailDestinatario, 
                    emailBodyMessage, smtpUsername, smtpPassword, smtpFromEmail);

            addActionMessage("Email inviata con successo a " + emailDestinatario);
            logger.info("Email inviata per fattura " + fattura.getNumero() + " a " + emailDestinatario);
            return SUCCESS;

        } catch (Exception e) {
            logger.error("Errore durante l'invio email", e);
            addActionError("Errore durante l'invio: " + e.getMessage());
            return ERROR;
        }
    }

    public String trackPixel() {
        try {
            if (trackingId == null || trackingId.isEmpty()) {
                logger.warn("Track pixel called without tracking ID");
                return ERROR;
            }

            String userAgent = ServletActionContext.getRequest().getHeader("User-Agent");
            emailService.trackPixelOpen(trackingId, userAgent);

            // Return 1x1 transparent GIF
            byte[] gifBytes = {
                0x47, 0x49, 0x46, 0x38, (byte) 0x39, 0x61, 0x01, 0x00, 0x01, 0x00, (byte) 0x80,
                0x00, 0x00, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, 0x00, 0x00, 0x00, 0x21, (byte) 0xF9,
                0x04, 0x01, 0x00, 0x00, 0x00, 0x00, 0x2C, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x01,
                0x00, 0x00, 0x02, 0x02, 0x44, 0x01, 0x00, 0x3B
            };

            inputStream = new ByteArrayInputStream(gifBytes);
            contentDisposition = "inline; filename=\"pixel.gif\"";
            return SUCCESS;

        } catch (Exception e) {
            logger.error("Errore nel tracking pixel", e);
            return ERROR;
        }
    }

    public String downloadWithTracking() {
        try {
            if (trackingId == null || trackingId.isEmpty()) {
                logger.warn("Download called without tracking ID");
                return ERROR;
            }

            TrackingEmail tracking = trackingEmailDAO.findByTrackingId(trackingId);
            if (tracking == null) {
                logger.warn("Tracking record not found for ID: " + trackingId);
                return ERROR;
            }

            Long fatturaId = tracking.getFattura().getId();
            fattura = fatturaDAO.findWithRighe(fatturaId);

            if (fattura == null) {
                addActionError("Fattura non trovata");
                return ERROR;
            }

            righe = fattura.getRighe();

            // Traccia il download
            String userAgent = ServletActionContext.getRequest().getHeader("User-Agent");
            emailService.trackDownload(trackingId, userAgent);

            // Genera PDF
            Document document = new Document(PageSize.A4);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, baos);
            document.open();

            generaPdf(document);

            document.close();

            inputStream = new ByteArrayInputStream(baos.toByteArray());
            contentDisposition = "attachment; filename=\"Fattura_" + fattura.getNumero() + ".pdf\"";
            return SUCCESS;

        } catch (Exception e) {
            logger.error("Errore nel download con tracciamento", e);
            addActionError("Errore durante il download: " + e.getMessage());
            return ERROR;
        }
    }

    private void generaPdf(Document document) throws DocumentException {
        String tipoLabel = Fattura.TipoFattura.PROFORMA.equals(fattura.getTipoFattura()) ? "PROFORMA" : "FATTURA";
        document.add(new Paragraph(new Chunk(tipoLabel, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20))));
        document.add(new Paragraph(" "));

        PdfPTable infoTable = new PdfPTable(2);
        infoTable.setWidthPercentage(100);
        infoTable.setWidths(new float[]{50, 50});

        addTableCell(infoTable, "Numero:", fattura.getNumero() != null ? fattura.getNumero() : "");
        addTableCell(infoTable, "Data:", fattura.getDataFattura() != null ? 
            new SimpleDateFormat("dd/MM/yyyy").format(fattura.getDataFattura()) : "");
        addTableCell(infoTable, "Cliente:", fattura.getCliente() != null ? fattura.getCliente().getRagioneSociale() : "");
        addTableCell(infoTable, "Tipo:", fattura.getTipoFattura() != null ? fattura.getTipoFattura().toString() : "");
        document.add(infoTable);
        document.add(new Paragraph(" "));

        if (righe != null && !righe.isEmpty()) {
            PdfPTable articoliTable = new PdfPTable(5);
            articoliTable.setWidthPercentage(100);
            articoliTable.setWidths(new float[]{35, 15, 15, 15, 20});

            addTableHeaderCell(articoliTable, "Descrizione");
            addTableHeaderCell(articoliTable, "Quantità");
            addTableHeaderCell(articoliTable, "Prezzo");
            addTableHeaderCell(articoliTable, "IVA %");
            addTableHeaderCell(articoliTable, "Importo");

            for (FatturaRiga riga : righe) {
                String descrizione = riga.getDescrizione() != null ? riga.getDescrizione() : 
                    (riga.getProdotto() != null ? riga.getProdotto().getNome() : "");
                addTableCell(articoliTable, descrizione);
                addTableCell(articoliTable, riga.getQuantita() != null ? riga.getQuantita().toString() : "");
                addTableCell(articoliTable, "€ " + (riga.getPrezzoUnitario() != null ? 
                    String.format("%.2f", riga.getPrezzoUnitario()) : "0.00"));
                addTableCell(articoliTable, riga.getIvaPercentuale() != null ? 
                    String.format("%.2f", riga.getIvaPercentuale()) + "%" : "0%");
                addTableCell(articoliTable, "€ " + (riga.getTotaleRiga() != null ? 
                    String.format("%.2f", riga.getTotaleRiga()) : "0.00"));
            }

            document.add(articoliTable);
            document.add(new Paragraph(" "));
        }

        addTotalsSectionToPdf(document);
    }

    private void addTotalsSectionToPdf(Document document) throws DocumentException {
        BigDecimal totaleImponibile = fattura.getImponibile() != null ? fattura.getImponibile() : BigDecimal.ZERO;
        BigDecimal totaleIva = fattura.getIva() != null ? fattura.getIva() : BigDecimal.ZERO;
        BigDecimal totaleGenerale = fattura.getTotale() != null ? fattura.getTotale() : BigDecimal.ZERO;

        PdfPTable totalsTable = new PdfPTable(2);
        totalsTable.setWidthPercentage(50);
        totalsTable.setHorizontalAlignment(Element.ALIGN_RIGHT);

        addTableCell(totalsTable, "Imponibile:", "€ " + String.format("%.2f", totaleImponibile));
        addTableCell(totalsTable, "IVA:", "€ " + String.format("%.2f", totaleIva));

        PdfPCell totaleLabelCell = new PdfPCell(new Phrase("TOTALE:", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12)));
        totaleLabelCell.setBackgroundColor(new BaseColor(150, 150, 150));
        totalsTable.addCell(totaleLabelCell);

        PdfPCell totaleValueCell = new PdfPCell(new Phrase("€ " + String.format("%.2f", totaleGenerale), 
            FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12)));
        totaleValueCell.setBackgroundColor(new BaseColor(150, 150, 150));
        totalsTable.addCell(totaleValueCell);

        document.add(totalsTable);
    }

    private void addTableCell(PdfPTable table, String label, String value) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));
        labelCell.setBackgroundColor(new BaseColor(200, 200, 200));
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, FontFactory.getFont(FontFactory.HELVETICA, 10)));
        table.addCell(valueCell);
    }

    private void addTableCell(PdfPTable table, String value) {
        PdfPCell cell = new PdfPCell(new Phrase(value, FontFactory.getFont(FontFactory.HELVETICA, 10)));
        table.addCell(cell);
    }

    private void addTableHeaderCell(PdfPTable table, String label) {
        PdfPCell cell = new PdfPCell(new Phrase(label, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11)));
        cell.setBackgroundColor(new BaseColor(100, 100, 100));
        cell.setFixedHeight(25);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        com.itextpdf.text.Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, BaseColor.WHITE);
        cell.setPhrase(new Phrase(label, font));
        table.addCell(cell);
    }

    public String generateXmlSdi() {
        try {
            if (id == null) {
                addActionError("Fattura non trovata");
                return ERROR;
            }

            fattura = fatturaDAO.findWithRighe(id);
            if (fattura == null) {
                addActionError("Fattura non trovata");
                return ERROR;
            }

            if (!Fattura.TipoFattura.REALE.equals(fattura.getTipoFattura())) {
                addActionError("L'XML SDI può essere generato solo per Fatture REALI, non per Proforma");
                return ERROR;
            }

            // Genera XML
            String xmlContent = xmlService.generateFatturaXML(fattura);

            // Valida XML
            if (!xmlService.validateXML(xmlContent)) {
                addActionError("XML generato non è valido");
                return ERROR;
            }

            inputStream = new ByteArrayInputStream(xmlContent.getBytes("UTF-8"));
            contentDisposition = "attachment; filename=\"Fattura_" + fattura.getNumero() + ".xml\"";

            addActionMessage("XML SDI generato per fattura " + fattura.getNumero());
            logger.info("XML SDI generato per fattura " + fattura.getNumero());
            return SUCCESS;

        } catch (Exception e) {
            logger.error("Errore nella generazione XML SDI", e);
            addActionError("Errore: " + e.getMessage());
            return ERROR;
        }
    }

    public String downloadXmlSdi() {
        try {
            if (id == null) {
                addActionError("Fattura non trovata");
                return ERROR;
            }

            fattura = fatturaDAO.findWithRighe(id);
            if (fattura == null) {
                addActionError("Fattura non trovata");
                return ERROR;
            }

            if (!Fattura.TipoFattura.REALE.equals(fattura.getTipoFattura())) {
                addActionError("L'XML SDI è disponibile solo per Fatture REALI");
                return ERROR;
            }

            // Genera XML
            String xmlContent = xmlService.generateFatturaXML(fattura);

            // Valida XML
            if (!xmlService.validateXML(xmlContent)) {
                addActionError("XML generato non è valido");
                return ERROR;
            }

            inputStream = new ByteArrayInputStream(xmlContent.getBytes("UTF-8"));
            contentDisposition = "attachment; filename=\"Fattura_" + fattura.getNumero() + ".xml\"";

            logger.info("XML SDI scaricato per fattura " + fattura.getNumero());
            return SUCCESS;

        } catch (Exception e) {
            logger.error("Errore durante lo scaricamento XML SDI", e);
            addActionError("Errore: " + e.getMessage());
            return ERROR;
        }
    }

    public String sendXmlSdi() {
        try {
            if (id == null) {
                addActionError("Fattura non trovata");
                return ERROR;
            }

            fattura = fatturaDAO.findWithRighe(id);
            if (fattura == null) {
                addActionError("Fattura non trovata");
                return ERROR;
            }

            if (!Fattura.TipoFattura.REALE.equals(fattura.getTipoFattura())) {
                addActionError("L'invio SDI e' disponibile solo per Fatture REALI");
                return ERROR;
            }

            String xmlContent = xmlService.generateFatturaXML(fattura);
            if (!xmlService.validateXML(xmlContent)) {
                addActionError("XML generato non e' valido");
                return ERROR;
            }

            FatturaXMLService.SdiResponse response = xmlService.sendToSdi(xmlContent);
            if (response.isSuccess()) {
                // Salva il codice SDI ricevuto nella fattura
                if (response.getSdiCodice() != null && !response.getSdiCodice().isEmpty()) {
                    fattura.setSdiCodice(response.getSdiCodice());
                    fattura.setSdiStato("INVIATA"); // Stato iniziale
                    fatturaDAO.update(fattura);
                    addActionMessage("XML inviato a SDI con successo. Codice: " + response.getSdiCodice());
                    logger.info("XML SDI inviato per fattura " + fattura.getNumero() + 
                               " - Codice SDI: " + response.getSdiCodice() + " - HTTP " + response.getStatusCode());
                } else {
                    // Invio riuscito ma codice non ricevuto - possibile errore nella risposta
                    addActionMessage("XML inviato a SDI (HTTP " + response.getStatusCode() + ") ma codice non ricevuto");
                    logger.warn("XML inviato per fattura " + fattura.getNumero() + 
                               " ma codice SDI non trovato nella risposta: " + response.getBody());
                }
                return SUCCESS;
            }

            addActionError("Errore invio SDI (HTTP " + response.getStatusCode() + ")");
            logger.warn("Invio SDI fallito per fattura " + fattura.getNumero() + " - HTTP " + response.getStatusCode() + " - " + response.getBody());
            return ERROR;

        } catch (Exception e) {
            logger.error("Errore durante l'invio XML SDI", e);
            addActionError("Errore: " + e.getMessage());
            return ERROR;
        }
    }

    /**
     * Import di una fattura da file XML FatturaPA
     */
    public String importXml() {
        try {
            if (uploadFile == null) {
                addActionError("Nessun file XML caricato");
                return ERROR;
            }

            // Leggi il file XML
            java.io.FileInputStream fis = new java.io.FileInputStream(uploadFile);
            byte[] data = new byte[(int) uploadFile.length()];
            fis.read(data);
            fis.close();
            String xmlContent = new String(data, "UTF-8");

            // Valida XML
            if (!xmlService.validateXML(xmlContent)) {
                addActionError("Il file XML non è valido");
                return ERROR;
            }

            // Parsa XML e crea fattura
            Fattura fatturaImportata = xmlService.parseFatturaXML(xmlContent);
            
            // Verifica se esiste già una fattura con lo stesso numero
            Fattura esistente = null;
            if (fatturaImportata.getNumero() != null && fatturaImportata.getAnno() != null) {
                List<Fattura> fattureAnno = fatturaDAO.findByAnno(fatturaImportata.getAnno());
                for (Fattura f : fattureAnno) {
                    if (fatturaImportata.getNumero().equals(f.getNumero())) {
                        esistente = f;
                        break;
                    }
                }
            }

            if (esistente != null) {
                addActionError("Fattura " + fatturaImportata.getNumero() + " già esistente nel sistema");
                return ERROR;
            }

            // Salva la fattura (senza cliente per ora - può essere assegnato manualmente)
            fatturaDAO.save(fatturaImportata);
            
            // Salva le righe
            if (fatturaImportata.getRighe() != null) {
                for (FatturaRiga r : fatturaImportata.getRighe()) {
                    r.setFattura(fatturaImportata);
                    fatturaRigaDAO.save(r);
                }
            }

            addActionMessage("Fattura " + fatturaImportata.getNumero() + " importata con successo da XML");
            logger.info("Fattura importata da XML: " + uploadFileFileName);
            id = fatturaImportata.getId();
            return SUCCESS;

        } catch (Exception e) {
            logger.error("Errore durante l'import XML fattura", e);
            addActionError("Errore nell'import: " + e.getMessage());
            return ERROR;
        }
    }


    public String esportaAssosoftware() {
        try {
            if (anno == null) {
                anno = Calendar.getInstance().get(Calendar.YEAR);
            }

            // Recupera tutte le fatture dell'anno
            List<Fattura> fattureEsportazione = fatturaDAO.findByAnno(anno);

            if (fattureEsportazione.isEmpty()) {
                addActionError("Nessuna fattura trovata per l'anno " + anno);
                return ERROR;
            }

            // Genera il file di esportazione
            byte[] fileContent = exportService.esportaFattureAssosoftware(fattureEsportazione);
            
            // Imposta il download
            inputStream = new ByteArrayInputStream(fileContent);
            contentDisposition = "attachment;filename=" + exportService.generateFileName("attive", anno);

            logger.info("Export Assosoftware fatture attive: " + fattureEsportazione.size() + " fatture esportate per anno " + anno);
            return SUCCESS;

        } catch (Exception e) {
            logger.error("Errore nell'esportazione Assosoftware", e);
            addActionError("Errore durante l'esportazione: " + e.getMessage());
            return ERROR;
        }
    }

    public String esportaSingolaAssosoftware() {
        try {
            if (id == null) {
                addActionError("ID fattura non specificato");
                return ERROR;
            }

            fattura = fatturaDAO.findById(id);
            if (fattura == null) {
                addActionError("Fattura non trovata");
                return ERROR;
            }

            // Genera il file di esportazione
            byte[] fileContent = exportService.esportaSingolaFatturaAssosoftware(fattura);
            
            // Imposta il download
            inputStream = new ByteArrayInputStream(fileContent);
            contentDisposition = "attachment;filename=fattura_" + fattura.getNumero() + "_assosoftware.txt";

            logger.info("Export Assosoftware singola fattura: " + fattura.getNumero());
            return SUCCESS;

        } catch (Exception e) {
            logger.error("Errore nell'esportazione Assosoftware singola fattura", e);
            addActionError("Errore durante l'esportazione: " + e.getMessage());
            return ERROR;
        }
    }


    // Getters and Setters
    public Fattura getFattura() { return fattura; }
    public void setFattura(Fattura fattura) { this.fattura = fattura; }
    public List<Fattura> getFatture() { return fatture; }
    public void setFatture(List<Fattura> fatture) { this.fatture = fatture; }
    public List<FatturaTrackingDTO> getFattureConTracking() { return fattureConTracking; }
    public void setFattureConTracking(List<FatturaTrackingDTO> fattureConTracking) { this.fattureConTracking = fattureConTracking; }
    public List<Cliente> getClienti() { return clienti; }
    public void setClienti(List<Cliente> clienti) { this.clienti = clienti; }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Integer getAnno() { return anno; }
    public void setAnno(Integer anno) { this.anno = anno; }
    public Fattura.TipoFattura getTipo() { return tipo; }
    public void setTipo(Fattura.TipoFattura tipo) { this.tipo = tipo; }
    public Long getClienteId() { return clienteId; }
    public void setClienteId(Long clienteId) { this.clienteId = clienteId; }
    public InputStream getInputStream() { return inputStream; }
    public void setInputStream(InputStream inputStream) { this.inputStream = inputStream; }
    public String getContentDisposition() { return contentDisposition; }
    public void setContentDisposition(String contentDisposition) { this.contentDisposition = contentDisposition; }
    public String getEmailDestinatario() { return emailDestinatario; }
    public void setEmailDestinatario(String emailDestinatario) { this.emailDestinatario = emailDestinatario; }
    public String getTrackingId() { return trackingId; }
    public void setTrackingId(String trackingId) { this.trackingId = trackingId; }
    public String getMessageEmail() { return messageEmail; }
    public void setMessageEmail(String messageEmail) { this.messageEmail = messageEmail; }
    public java.io.File getUploadFile() { return uploadFile; }
    public void setUploadFile(java.io.File uploadFile) { this.uploadFile = uploadFile; }
    public String getUploadFileContentType() { return uploadFileContentType; }
    public void setUploadFileContentType(String uploadFileContentType) { this.uploadFileContentType = uploadFileContentType; }
    public String getUploadFileFileName() { return uploadFileFileName; }
    public void setUploadFileFileName(String uploadFileFileName) { this.uploadFileFileName = uploadFileFileName; }
}
