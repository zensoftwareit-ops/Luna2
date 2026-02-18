package it.zensoftware.luna2.action;

import com.opensymphony.xwork2.ActionSupport;
import it.zensoftware.luna2.dao.OrdineDAO;
import it.zensoftware.luna2.dao.OrdineRigaDAO;
import it.zensoftware.luna2.dao.ClienteDAO;
import it.zensoftware.luna2.dao.TrackingEmailDAO;
import it.zensoftware.luna2.dto.OrdineTrackingDTO;
import it.zensoftware.luna2.model.Ordine;
import it.zensoftware.luna2.model.OrdineRiga;
import it.zensoftware.luna2.model.Cliente;
import it.zensoftware.luna2.model.TrackingEmail;
import it.zensoftware.luna2.service.EmailService;
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

public class OrdiniAction extends ActionSupport {
    private static final Logger logger = LogManager.getLogger(OrdiniAction.class);
    private OrdineDAO ordineDAO = new OrdineDAO();
    private OrdineRigaDAO ordineRigaDAO = new OrdineRigaDAO();
    private ClienteDAO clienteDAO = new ClienteDAO();
    private TrackingEmailDAO trackingEmailDAO = new TrackingEmailDAO();
    private EmailService emailService = new EmailService();
    
    private Ordine ordine;
    private List<Ordine> ordini;
    private List<OrdineTrackingDTO> ordiniConTracking;
    private List<Cliente> clienti;
    private List<OrdineRiga> righe;
    private OrdineRiga riga;
    
    private Long id;
    private Long rigaId;
    private Integer anno;
    private Ordine.Stato stato;
    private Long clienteId;
    private InputStream inputStream;
    private String contentDisposition;
    private String emailDestinatario;
    private String trackingId;
    private String messageEmail;

    public String list() {
        if (anno == null) {
            anno = Calendar.getInstance().get(Calendar.YEAR);
        }
        
        if (stato != null) {
            ordini = ordineDAO.findByStato(stato);
        } else if (clienteId != null) {
            // TODO: implement findByClienteId for Ordine if needed
            ordini = ordineDAO.findByAnno(anno);
        } else {
            ordini = ordineDAO.findByAnno(anno);
        }
        
        // Populate tracking data for each ordine
        ordiniConTracking = new ArrayList<>();
        for (Ordine o : ordini) {
            Long totalEmails = trackingEmailDAO.countEmailsForOrdine(o.getId());
            Long openedEmails = trackingEmailDAO.countOpensForOrdine(o.getId());
            Long downloadedEmails = trackingEmailDAO.countDownloadsForOrdine(o.getId());
            Long totalDownloads = trackingEmailDAO.getTotalDownloadCountOrdine(o.getId());
            
            ordiniConTracking.add(new OrdineTrackingDTO(o, totalEmails, openedEmails, downloadedEmails, totalDownloads));
        }
        
        return SUCCESS;
    }

    public String create() {
        ordine = new Ordine();
        ordine.setDataOrdine(new Date());
        ordine.setAnno(Calendar.getInstance().get(Calendar.YEAR));
        
        // Generate next numero
        Integer nextNumero = ordineDAO.getNextNumero(ordine.getAnno());
        ordine.setNumero(String.format("ORD-%04d", nextNumero));
        
        ordine.setStato(Ordine.Stato.CONFERMATO);
        clienti = clienteDAO.findAll();
        
        return SUCCESS;
    }

    public String edit() {
        if (id != null) {
            ordine = ordineDAO.findWithRighe(id);
            if (ordine == null) {
                addActionError("Ordine non trovato");
                return ERROR;
            }
            ordine.getRighe().size(); // Force load
            clienti = clienteDAO.findAll();
        }
        return SUCCESS;
    }

    public String save() {
        try {
            if (ordine == null) {
                addActionError("Ordine non valido");
                return ERROR;
            }

            if (ordine.getId() == null) {
                ordineDAO.save(ordine);
            } else {
                ordineDAO.update(ordine);
            }

            addActionMessage("Ordine salvato con successo");
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Errore nel salvataggio ordine", e);
            addActionError("Errore: " + e.getMessage());
            return ERROR;
        }
    }

    public String delete() {
        try {
            if (id != null) {
                ordine = ordineDAO.findById(id);
                if (ordine != null) {
                    ordineDAO.delete(ordine);
                    addActionMessage("Ordine eliminato");
                }
            }
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Errore nell'eliminazione", e);
            addActionError("Errore: " + e.getMessage());
            return ERROR;
        }
    }

    public String generatePdf() {
        try {
            if (id == null) {
                addActionError("Ordine non trovato");
                return ERROR;
            }

            ordine = ordineDAO.findWithRighe(id);
            if (ordine == null) {
                addActionError("Ordine non trovato");
                return ERROR;
            }

            righe = ordineDAO.findWithRighe(id).getRighe();

            Document document = new Document(PageSize.A4);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, baos);
            document.open();

            generaPdfTecnico(document);

            document.close();

            inputStream = new ByteArrayInputStream(baos.toByteArray());
            contentDisposition = "attachment; filename=\"Ordine_" + ordine.getNumero() + ".pdf\"";
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
                addActionError("Ordine e email ricevente sono obbligatori");
                return INPUT;
            }

            ordine = ordineDAO.findWithRighe(id);
            if (ordine == null) {
                addActionError("Ordine non trovato");
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
                "Allega l'ordine numero " + ordine.getNumero() + " per la review.";

            // Invia email con tracciamento
            TrackingEmail tracking = emailService.sendOrdineEmail(ordine, emailDestinatario, 
                    emailBodyMessage, smtpUsername, smtpPassword, smtpFromEmail);

            // Keep the Ordine in its current state (no state change for order emails)
            // ordineDAO.update(ordine);

            addActionMessage("Email inviata con successo a " + emailDestinatario);
            logger.info("Email inviata per ordine " + ordine.getNumero() + " a " + emailDestinatario);
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

            Long ordineId = tracking.getOrdine().getId();
            ordine = ordineDAO.findWithRighe(ordineId);

            if (ordine == null) {
                addActionError("Ordine non trovato");
                return ERROR;
            }

            righe = ordine.getRighe();

            // Traccia il download
            String userAgent = ServletActionContext.getRequest().getHeader("User-Agent");
            emailService.trackDownload(trackingId, userAgent);

            // Genera PDF tecnico
            Document document = new Document(PageSize.A4);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, baos);
            document.open();

            generaPdfTecnico(document);

            document.close();

            inputStream = new ByteArrayInputStream(baos.toByteArray());
            contentDisposition = "attachment; filename=\"Ordine_" + ordine.getNumero() + ".pdf\"";
            return SUCCESS;

        } catch (Exception e) {
            logger.error("Errore nel download con tracciamento", e);
            addActionError("Errore durante il download: " + e.getMessage());
            return ERROR;
        }
    }

    private void generaPdfTecnico(Document document) throws DocumentException {
        document.add(new Paragraph(new Chunk("ORDINE", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20))));
        document.add(new Paragraph(" "));

        PdfPTable infoTable = new PdfPTable(2);
        infoTable.setWidthPercentage(100);
        infoTable.setWidths(new float[]{50, 50});

        addTableCell(infoTable, "Numero:", ordine.getNumero() != null ? ordine.getNumero().toString() : "");
        addTableCell(infoTable, "Data:", ordine.getDataOrdine() != null ? 
            new SimpleDateFormat("dd/MM/yyyy").format(ordine.getDataOrdine()) : "");
        addTableCell(infoTable, "Cliente:", ordine.getCliente() != null ? ordine.getCliente().getRagioneSociale() : "");
        addTableCell(infoTable, "Stato:", ordine.getStato() != null ? ordine.getStato().toString() : "");
        document.add(infoTable);
        document.add(new Paragraph(" "));

        if (righe != null && !righe.isEmpty()) {
            PdfPTable articoliTable = new PdfPTable(5);
            articoliTable.setWidthPercentage(100);
            articoliTable.setWidths(new float[]{35, 15, 15, 15, 20});

            addTableHeaderCell(articoliTable, "Descrizione");
            addTableHeaderCell(articoliTable, "Quantità");
            addTableHeaderCell(articoliTable, "Prezzo");
            addTableHeaderCell(articoliTable, "Sconto %");
            addTableHeaderCell(articoliTable, "Importo");

            for (OrdineRiga riga : righe) {
                if (OrdineRiga.TipoRiga.PRODOTTO.equals(riga.getTipoRiga())) {
                    String descrizione = riga.getDescrizione() != null ? riga.getDescrizione() : 
                        (riga.getProdotto() != null ? riga.getProdotto().getNome() : "");
                    addTableCell(articoliTable, descrizione);
                    addTableCell(articoliTable, riga.getQuantita() != null ? riga.getQuantita().toString() : "");
                    addTableCell(articoliTable, "€ " + (riga.getPrezzoUnitario() != null ? 
                        String.format("%.2f", riga.getPrezzoUnitario()) : "0.00"));
                    addTableCell(articoliTable, riga.getScontoPercentuale() != null ? 
                        String.format("%.2f", riga.getScontoPercentuale()) + "%" : "0%");
                    addTableCell(articoliTable, "€ " + (riga.getTotaleRiga() != null ? 
                        String.format("%.2f", riga.getTotaleRiga()) : "0.00"));
                }
            }

            document.add(articoliTable);
            document.add(new Paragraph(" "));
        }

        addTotalsSectionToPdf(document);
    }

    private void addTotalsSectionToPdf(Document document) throws DocumentException {
        BigDecimal totaleImponibile = BigDecimal.ZERO;
        BigDecimal totaleIva = BigDecimal.ZERO;

        if (righe != null) {
            for (OrdineRiga riga : righe) {
                if (riga.getImponibileRiga() != null) {
                    totaleImponibile = totaleImponibile.add(riga.getImponibileRiga());
                }
            }
        }

        // Calcola IVA al 22% se applicabile
        totaleIva = totaleImponibile.multiply(new BigDecimal("0.22"));
        BigDecimal totaleGenerale = totaleImponibile.add(totaleIva);

        PdfPTable totalsTable = new PdfPTable(2);
        totalsTable.setWidthPercentage(50);
        totalsTable.setHorizontalAlignment(Element.ALIGN_RIGHT);

        addTableCell(totalsTable, "Imponibile:", "€ " + String.format("%.2f", totaleImponibile));
        addTableCell(totalsTable, "IVA (22%):", "€ " + String.format("%.2f", totaleIva));

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

    // Getters and Setters
    public Ordine getOrdine() { return ordine; }
    public void setOrdine(Ordine ordine) { this.ordine = ordine; }
    public List<Ordine> getOrdini() { return ordini; }
    public void setOrdini(List<Ordine> ordini) { this.ordini = ordini; }
    public List<OrdineTrackingDTO> getOrdiniConTracking() { return ordiniConTracking; }
    public void setOrdiniConTracking(List<OrdineTrackingDTO> ordiniConTracking) { this.ordiniConTracking = ordiniConTracking; }
    public List<Cliente> getClienti() { return clienti; }
    public void setClienti(List<Cliente> clienti) { this.clienti = clienti; }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Integer getAnno() { return anno; }
    public void setAnno(Integer anno) { this.anno = anno; }
    public Ordine.Stato getStato() { return stato; }
    public void setStato(Ordine.Stato stato) { this.stato = stato; }
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
}
