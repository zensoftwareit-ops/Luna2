package it.zensoftware.luna2.util;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import it.zensoftware.luna2.model.*;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;

/**
 * Utility per generazione PDF reports con iText
 */
public class PdfReportGenerator {
    
    private static final Font TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, BaseColor.BLACK);
    private static final Font HEADER_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, BaseColor.WHITE);
    private static final Font NORMAL_FONT = FontFactory.getFont(FontFactory.HELVETICA, 10, BaseColor.BLACK);
    private static final Font SMALL_FONT = FontFactory.getFont(FontFactory.HELVETICA, 8, BaseColor.GRAY);
    
    public static byte[] generateVenditeReport(List<Map<String, Object>> data, BigDecimal totale, int numeroFatture) throws Exception {
        Document document = new Document(PageSize.A4);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, baos);
        
        document.open();
        
        // Title
        Paragraph title = new Paragraph("Report Vendite", TITLE_FONT);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(20);
        document.add(title);
        
        // Summary info
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        Paragraph info = new Paragraph("Generato il: " + sdf.format(new java.util.Date()), SMALL_FONT);
        info.setAlignment(Element.ALIGN_RIGHT);
        info.setSpacingAfter(10);
        document.add(info);
        
        // Summary table
        PdfPTable summaryTable = new PdfPTable(2);
        summaryTable.setWidthPercentage(50);
        summaryTable.setHorizontalAlignment(Element.ALIGN_LEFT);
        summaryTable.setSpacingAfter(20);
        
        addCellToTable(summaryTable, "Numero Fatture:", NORMAL_FONT, BaseColor.LIGHT_GRAY);
        addCellToTable(summaryTable, String.valueOf(numeroFatture), NORMAL_FONT, BaseColor.WHITE);
        addCellToTable(summaryTable, "Totale Vendite", NORMAL_FONT, BaseColor.WHITE);
        addCellToTable(summaryTable, "€ " + String.format("%,.2f", totale), NORMAL_FONT, BaseColor.WHITE);
        
        document.add(summaryTable);
        
        // Data table
        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{3, 1, 2});
        table.setSpacingBefore(10);
        
        // Headers
        addHeaderCell(table, "Cliente");
        addHeaderCell(table, "N. Fatture");
        addHeaderCell(table, "Totale");
        
        // Data rows
        for (Map<String, Object> row : data) {
            addCellToTable(table, (String) row.get("cliente"), NORMAL_FONT, BaseColor.WHITE);
            addCellToTable(table, String.valueOf(row.get("numeroFatture")), NORMAL_FONT, BaseColor.WHITE);
            addCellToTable(table, "€ " + String.format("%,.2f", row.get("totale")), NORMAL_FONT, BaseColor.WHITE);
        }
        
        document.add(table);
        
        // Footer
        Paragraph footer = new Paragraph("Luna2 ERP System - © 2026", SMALL_FONT);
        footer.setAlignment(Element.ALIGN_CENTER);
        footer.setSpacingBefore(30);
        document.add(footer);
        
        document.close();
        return baos.toByteArray();
    }
    
    public static byte[] generateMagazzinoReport(List<Map<String, Object>> data, BigDecimal valoreTotale) throws Exception {
        Document document = new Document(PageSize.A4, 36, 36, 54, 36);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, baos);
        
        document.open();
        
        // Title
        Paragraph title = new Paragraph("Report Magazzino", TITLE_FONT);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(20);
        document.add(title);
        
        // Summary
        Paragraph summary = new Paragraph("Valore Totale Giacenze: € " + String.format("%,.2f", valoreTotale), NORMAL_FONT);
        summary.setAlignment(Element.ALIGN_RIGHT);
        summary.setSpacingAfter(15);
        document.add(summary);
        
        // Table
        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{3, 1.5f, 1.5f, 1.5f, 1.5f});
        
        addHeaderCell(table, "Prodotto");
        addHeaderCell(table, "Quantità");
        addHeaderCell(table, "Scorta Min");
        addHeaderCell(table, "Valore");
        addHeaderCell(table, "Stato");
        
        for (Map<String, Object> row : data) {
            addCellToTable(table, (String) row.get("prodotto"), NORMAL_FONT, BaseColor.WHITE);
            addCellToTable(table, String.valueOf(row.get("quantita")), NORMAL_FONT, BaseColor.WHITE);
            addCellToTable(table, String.valueOf(row.get("scorta")), NORMAL_FONT, BaseColor.WHITE);
            addCellToTable(table, "€ " + String.format("%,.2f", row.get("valore")), NORMAL_FONT, BaseColor.WHITE);
            
            String stato = (String) row.get("stato");
            BaseColor bgColor = stato.equals("SOTTO SCORTA") ? new BaseColor(255, 220, 220) : BaseColor.WHITE;
            addCellToTable(table, stato, NORMAL_FONT, bgColor);
        }
        
        document.add(table);
        document.close();
        
        return baos.toByteArray();
    }
    
    public static byte[] generateCrmReport(List<Map<String, Object>> data) throws Exception {
        Document document = new Document(PageSize.A4);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, baos);
        
        document.open();
        
        Paragraph title = new Paragraph("Report CRM - Lead Pipeline", TITLE_FONT);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(20);
        document.add(title);
        
        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(80);
        table.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.setWidths(new float[]{2, 1, 1});
        
        addHeaderCell(table, "Stato");
        addHeaderCell(table, "Numero");
        addHeaderCell(table, "Percentuale");
        
        for (Map<String, Object> row : data) {
            addCellToTable(table, (String) row.get("stato"), NORMAL_FONT, BaseColor.WHITE);
            addCellToTable(table, String.valueOf(row.get("numero")), NORMAL_FONT, BaseColor.WHITE);
            addCellToTable(table, row.get("percentuale") + "%", NORMAL_FONT, BaseColor.WHITE);
        }
        
        document.add(table);
        document.close();
        
        return baos.toByteArray();
    }
    
    private static void addHeaderCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, HEADER_FONT));
        cell.setBackgroundColor(new BaseColor(41, 128, 185));
        cell.setPadding(8);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }
    
    private static void addCellToTable(PdfPTable table, String text, Font font, BaseColor bgColor) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bgColor);
        cell.setPadding(6);
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        table.addCell(cell);
    }
}
