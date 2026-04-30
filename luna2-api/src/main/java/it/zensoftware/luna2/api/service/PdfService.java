package it.zensoftware.luna2.api.service;

import it.zensoftware.luna2.model.Fattura;
import it.zensoftware.luna2.model.Preventivo;
import it.zensoftware.luna2.model.Cliente;
import org.springframework.stereotype.Service;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.table.Table;
import com.itextpdf.layout.element.cell.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.VerticalAlignment;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * PdfService - Generazione documenti PDF
 *
 * Supporta la generazione di:
 * - Fatture (REALE, PROFORMA)
 * - Preventivi
 * - DDT (Documenti di Trasporto)
 */
@Service
public class PdfService {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy");

    /**
     * Genera PDF per una fattura
     */
    public byte[] generateFatturaPdf(Fattura fattura) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc);

        // Intestazione
        addDocumentHeader(document, "FATTURA", fattura.getNumero(), fattura.getDataFattura());

        // Dati Cliente
        if (fattura.getCliente() != null) {
            addClientSection(document, fattura.getCliente());
        }

        // Tabella righe
        if (fattura.getRighe() != null && !fattura.getRighe().isEmpty()) {
            addRigheTable(document, fattura);
        }

        // Totali
        addTotaliSection(document, fattura);

        // Note
        if (fattura.getNotePiede() != null && !fattura.getNotePiede().isEmpty()) {
            document.add(new Paragraph("\n"));
            document.add(new Paragraph("NOTE:")
                .setBold());
            document.add(new Paragraph(fattura.getNotePiede())
                .setFontSize(10));
        }

        // Footer
        addFooter(document, "Fattura generata il " + DATE_FORMAT.format(new Date()));

        document.close();
        return baos.toByteArray();
    }

    /**
     * Genera PDF per un preventivo
     */
    public byte[] generatePreventivoPdf(Preventivo preventivo) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc);

        // Intestazione
        addDocumentHeader(document, "PREVENTIVO", preventivo.getNumero(), preventivo.getDataCreazione());

        // Dati Cliente
        if (preventivo.getCliente() != null) {
            addClientSection(document, preventivo.getCliente());
        }

        // Righe Preventivo
        if (preventivo.getRighe() != null && !preventivo.getRighe().isEmpty()) {
            addRighePreventivo(document, preventivo);
        }

        // Totali
        addTotaliPreventivo(document, preventivo);

        // Footer
        addFooter(document, "Preventivo generato il " + DATE_FORMAT.format(new Date()));

        document.close();
        return baos.toByteArray();
    }

    /**
     * Genera PDF per un DDT
     */
    public byte[] generateDdtPdf(Long ddtId, String numero, Date dataDdt, Cliente cliente) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc);

        // Intestazione
        addDocumentHeader(document, "DDT", numero, dataDdt);

        // Dati Cliente
        if (cliente != null) {
            addClientSection(document, cliente);
        }

        // Nota
        document.add(new Paragraph("\nDocumento di Trasporto")
            .setFontSize(14).setBold().setTextAlignment(TextAlignment.CENTER));

        // Footer
        addFooter(document, "DDT generato il " + DATE_FORMAT.format(new Date()));

        document.close();
        return baos.toByteArray();
    }

    // ==================== HELPER METHODS ====================

    private void addDocumentHeader(Document document, String tipo, String numero, Date data) {
        Paragraph title = new Paragraph(tipo)
            .setFontSize(24)
            .setBold()
            .setTextAlignment(TextAlignment.CENTER);
        document.add(title);

        Paragraph numData = new Paragraph(
            "Nr. " + numero + " del " + (data != null ? DATE_FORMAT.format(data) : "")
        )
            .setFontSize(12)
            .setTextAlignment(TextAlignment.CENTER);
        document.add(numData);

        document.add(new Paragraph("\n"));
    }

    private void addClientSection(Document document, Cliente cliente) {
        document.add(new Paragraph("CLIENTE")
            .setBold()
            .setFontSize(12));

        Paragraph clientData = new Paragraph();
        if (cliente.getRagioneSociale() != null) {
            clientData.add(cliente.getRagioneSociale()).add("\n");
        }
        if (cliente.getIndirizzo() != null) {
            clientData.add(cliente.getIndirizzo()).add("\n");
        }
        if (cliente.getCitta() != null) {
            clientData.add(cliente.getCitta());
            if (cliente.getCap() != null) {
                clientData.add(" (").add(cliente.getCap()).add(")");
            }
            clientData.add("\n");
        }
        if (cliente.getPartitaIva() != null) {
            clientData.add("P.IVA: ").add(cliente.getPartitaIva()).add("\n");
        }

        document.add(clientData.setFontSize(10));
        document.add(new Paragraph("\n"));
    }

    private void addRigheTable(Document document, Fattura fattura) throws Exception {
        float[] columnWidths = {0.4f, 0.15f, 0.15f, 0.15f, 0.15f};
        Table table = new Table(columnWidths);

        // Header
        table.addHeaderCell(new Cell().add(new Paragraph("Prodotto")).setBold());
        table.addHeaderCell(new Cell().add(new Paragraph("Qta")).setBold());
        table.addHeaderCell(new Cell().add(new Paragraph("Prezzo U.")).setBold());
        table.addHeaderCell(new Cell().add(new Paragraph("Totale")).setBold());
        table.addHeaderCell(new Cell().add(new Paragraph("IVA %")).setBold());

        // Rows
        if (fattura.getRighe() != null) {
            for (var riga : fattura.getRighe()) {
                if (riga.getProdotto() != null) {
                    table.addCell(new Cell().add(new Paragraph(riga.getProdotto().getNome())));
                    table.addCell(new Cell().add(new Paragraph(String.valueOf(riga.getQuantita()))));
                    table.addCell(new Cell().add(new Paragraph(
                        (riga.getPrezzoUnitario() != null ? riga.getPrezzoUnitario().toPlainString() : "0.00")
                    )));
                    table.addCell(new Cell().add(new Paragraph(
                        (riga.getImportoTotale() != null ? riga.getImportoTotale().toPlainString() : "0.00")
                    )));
                    table.addCell(new Cell().add(new Paragraph(
                        (riga.getIvaPercentuale() != null ? riga.getIvaPercentuale().toPlainString() : "0.00")
                    )));
                }
            }
        }

        document.add(table);
        document.add(new Paragraph("\n"));
    }

    private void addRighePreventivo(Document document, Preventivo preventivo) {
        float[] columnWidths = {0.5f, 0.2f, 0.15f, 0.15f};
        Table table = new Table(columnWidths);

        // Header
        table.addHeaderCell(new Cell().add(new Paragraph("Descrizione")).setBold());
        table.addHeaderCell(new Cell().add(new Paragraph("Qta")).setBold());
        table.addHeaderCell(new Cell().add(new Paragraph("Prezzo")).setBold());
        table.addHeaderCell(new Cell().add(new Paragraph("Totale")).setBold());

        // Rows
        if (preventivo.getRighe() != null) {
            for (var riga : preventivo.getRighe()) {
                table.addCell(new Cell().add(new Paragraph(
                    riga.getDescrizione() != null ? riga.getDescrizione() : ""
                )));
                table.addCell(new Cell().add(new Paragraph(
                    String.valueOf(riga.getQuantita() != null ? riga.getQuantita() : 0)
                )));
                table.addCell(new Cell().add(new Paragraph(
                    riga.getPrezzoUnitario() != null ? riga.getPrezzoUnitario().toPlainString() : "0.00"
                )));
                table.addCell(new Cell().add(new Paragraph(
                    riga.getImportoTotale() != null ? riga.getImportoTotale().toPlainString() : "0.00"
                )));
            }
        }

        document.add(table);
        document.add(new Paragraph("\n"));
    }

    private void addTotaliSection(Document document, Fattura fattura) {
        float[] columnWidths = {0.5f, 0.2f, 0.3f};
        Table table = new Table(columnWidths);

        table.addCell(new Cell().add(new Paragraph("Imponibile:")));
        table.addCell(new Cell().add(new Paragraph("")));
        table.addCell(new Cell().add(new Paragraph(
            fattura.getImponibile() != null ? fattura.getImponibile().toPlainString() : "0.00"
        )));

        table.addCell(new Cell().add(new Paragraph("IVA:")));
        table.addCell(new Cell().add(new Paragraph("")));
        table.addCell(new Cell().add(new Paragraph(
            fattura.getIva() != null ? fattura.getIva().toPlainString() : "0.00"
        )));

        table.addCell(new Cell().add(new Paragraph("TOTALE:").setBold()));
        table.addCell(new Cell().add(new Paragraph("")));
        table.addCell(new Cell().add(new Paragraph(
            fattura.getTotale() != null ? fattura.getTotale().toPlainString() : "0.00"
        ).setBold()));

        document.add(table);
    }

    private void addTotaliPreventivo(Document document, Preventivo preventivo) {
        Paragraph totale = new Paragraph("\nTOTALE PREVENTIVO: ")
            .setBold()
            .setFontSize(12);
        totale.add(preventivo.getImporteTotale() != null ?
            preventivo.getImporteTotale().toPlainString() : "0.00");
        document.add(totale);
    }

    private void addFooter(Document document, String footerText) {
        document.add(new Paragraph("\n\n"));
        document.add(new Paragraph(footerText)
            .setFontSize(9)
            .setTextAlignment(TextAlignment.CENTER)
            .setItalic());
    }
}
