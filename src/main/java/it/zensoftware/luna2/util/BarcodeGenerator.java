package it.zensoftware.luna2.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import it.zensoftware.luna2.model.Prodotto;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility per la generazione di etichette con barcode in formato PDF
 */
public class BarcodeGenerator {
    private static final Logger logger = LogManager.getLogger(BarcodeGenerator.class);
    
    private static final int BARCODE_WIDTH = 250;
    private static final int BARCODE_HEIGHT = 100;
    
    /**
     * Genera un PDF con etichette barcode per una lista di prodotti
     */
    public static ByteArrayOutputStream generaEtichettePDF(List<Prodotto> prodotti, int copiePerProdotto) throws Exception {
        Document document = new Document(PageSize.A4);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        try {
            PdfWriter.getInstance(document, baos);
            document.open();
            
            // Font
            Font fontTitolo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font fontTesto = FontFactory.getFont(FontFactory.HELVETICA, 9);
            Font fontCodice = FontFactory.getFont(FontFactory.COURIER, 8);
            
            // Crea tabella per layout etichette (2 colonne)
            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10f);
            table.setSpacingAfter(10f);
            
            for (Prodotto prodotto : prodotti) {
                for (int copia = 0; copia < copiePerProdotto; copia++) {
                    PdfPCell cell = creaEtichettaProdotto(prodotto, fontTitolo, fontTesto, fontCodice);
                    table.addCell(cell);
                }
            }
            
            // Se numero dispari di etichette, aggiungi cella vuota
            if ((prodotti.size() * copiePerProdotto) % 2 != 0) {
                PdfPCell emptyCell = new PdfPCell();
                emptyCell.setBorder(Rectangle.NO_BORDER);
                table.addCell(emptyCell);
            }
            
            document.add(table);
            
        } catch (Exception e) {
            logger.error("Errore durante la generazione del PDF etichette", e);
            throw e;
        } finally {
            if (document.isOpen()) {
                document.close();
            }
        }
        
        return baos;
    }
    
    /**
     * Crea una singola etichetta per un prodotto
     */
    private static PdfPCell creaEtichettaProdotto(Prodotto prodotto, Font fontTitolo, Font fontTesto, Font fontCodice) throws Exception {
        PdfPCell cell = new PdfPCell();
        cell.setPadding(10);
        cell.setBorder(Rectangle.BOX);
        cell.setBorderWidth(1);
        
        // Tabella interna per organizzare il contenuto dell'etichetta
        PdfPTable innerTable = new PdfPTable(1);
        innerTable.setWidthPercentage(100);
        
        // Nome prodotto
        PdfPCell nomeCellHeader = new PdfPCell(new Phrase(prodotto.getNome(), fontTitolo));
        nomeCellHeader.setBorder(Rectangle.NO_BORDER);
        nomeCellHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
        nomeCellHeader.setPaddingBottom(5);
        innerTable.addCell(nomeCellHeader);
        
        // Codice prodotto
        String codice = prodotto.getCodice() != null ? prodotto.getCodice() : "";
        if (!codice.isEmpty()) {
            PdfPCell codiceCell = new PdfPCell(new Phrase("Codice: " + codice, fontTesto));
            codiceCell.setBorder(Rectangle.NO_BORDER);
            codiceCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            codiceCell.setPaddingBottom(5);
            innerTable.addCell(codiceCell);
        }
        
        // Genera barcode
        try {
            String barcodeText = codice.isEmpty() ? ("PROD" + prodotto.getId()) : codice;
            Image barcodeImage = generaBarcodeImage(barcodeText);
            
            if (barcodeImage != null) {
                PdfPCell barcodeCell = new PdfPCell(barcodeImage);
                barcodeCell.setBorder(Rectangle.NO_BORDER);
                barcodeCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                barcodeCell.setPaddingTop(5);
                barcodeCell.setPaddingBottom(5);
                innerTable.addCell(barcodeCell);
                
                // Testo sotto il barcode
                PdfPCell barcodeTextCell = new PdfPCell(new Phrase(barcodeText, fontCodice));
                barcodeTextCell.setBorder(Rectangle.NO_BORDER);
                barcodeTextCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                innerTable.addCell(barcodeTextCell);
            }
        } catch (Exception e) {
            logger.warn("Impossibile generare barcode per prodotto " + prodotto.getCodice(), e);
        }
        
        // Prezzo (se presente)
        if (prodotto.getPrezzoBase() != null && prodotto.getPrezzoBase().compareTo(java.math.BigDecimal.ZERO) > 0) {
            PdfPCell prezzoCell = new PdfPCell(new Phrase("€ " + prodotto.getPrezzoBase().setScale(2), fontTesto));
            prezzoCell.setBorder(Rectangle.NO_BORDER);
            prezzoCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            prezzoCell.setPaddingTop(5);
            innerTable.addCell(prezzoCell);
        }
        
        cell.addElement(innerTable);
        return cell;
    }
    
    /**
     * Genera un'immagine barcode usando ZXing
     */
    private static Image generaBarcodeImage(String text) throws Exception {
        try {
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.MARGIN, 1);
            
            BarcodeFormat format = BarcodeFormat.CODE_128;
            
            // Prova EAN-13 se il codice è numerico e di 12-13 cifre
            if (text.matches("\\d{12,13}")) {
                format = BarcodeFormat.EAN_13;
                // EAN-13 richiede esattamente 13 cifre
                if (text.length() == 12) {
                    text = text + calcolaCheckDigitEAN13(text);
                }
            }
            
            BitMatrix bitMatrix = new MultiFormatWriter().encode(
                text,
                format,
                BARCODE_WIDTH,
                BARCODE_HEIGHT,
                hints
            );
            
            ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
            byte[] pngData = pngOutputStream.toByteArray();
            
            return Image.getInstance(pngData);
            
        } catch (Exception e) {
            logger.error("Errore durante la generazione del barcode per: " + text, e);
            throw e;
        }
    }
    
    /**
     * Calcola il check digit per EAN-13
     */
    private static String calcolaCheckDigitEAN13(String code12) {
        int sum = 0;
        for (int i = 0; i < 12; i++) {
            int digit = Character.getNumericValue(code12.charAt(i));
            sum += (i % 2 == 0) ? digit : digit * 3;
        }
        int checkDigit = (10 - (sum % 10)) % 10;
        return String.valueOf(checkDigit);
    }
    
    /**
     * Genera etichetta singola per un prodotto
     */
    public static ByteArrayOutputStream generaEtichettaSingola(Prodotto prodotto) throws Exception {
        return generaEtichettePDF(List.of(prodotto), 1);
    }
}
