package it.zensoftware.luna2.action;

import com.opensymphony.xwork2.ActionSupport;
import it.zensoftware.luna2.dao.ProdottoDAO;
import it.zensoftware.luna2.model.Prodotto;
import it.zensoftware.luna2.model.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.math.BigDecimal;
import java.util.*;

public class ProdottiImportExportAction extends ActionSupport {
    private static final Logger logger = LogManager.getLogger(ProdottiImportExportAction.class);
    
    private ProdottoDAO prodottoDAO = new ProdottoDAO();
    private File uploadFile;
    private String uploadFileContentType;
    private String uploadFileFileName;
    private InputStream excelStream;
    private String excelFileName;
    private int importedCount = 0;
    private int updatedCount = 0;
    private int errorCount = 0;
    private List<String> errorMessages = new ArrayList<>();

    /**
     * Mostra la pagina di import
     */
    public String importPage() {
        return SUCCESS;
    }

    /**
     * Download template Excel per import
     */
    public String downloadTemplate() {
        try {
            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("Prodotti");

            // Stile header
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Header
            Row headerRow = sheet.createRow(0);
            String[] columns = {
                "Codice*", "Nome*", "Descrizione", "Categoria", "TipoProdotto*",
                "UnitaMisura*", "PrezzoBase*", "CostoAcquisto", "IvaPercentuale",
                "ScontoMassimo", "Peso", "Volume", "CodiceEAN", 
                "GiacenzaMinima", "GestioneMagazzino", "Attivo", "Note"
            };

            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 4000);
            }

            // Riga di esempio
            Row exampleRow = sheet.createRow(1);
            exampleRow.createCell(0).setCellValue("PROD001");
            exampleRow.createCell(1).setCellValue("Prodotto Esempio");
            exampleRow.createCell(2).setCellValue("Descrizione del prodotto");
            exampleRow.createCell(3).setCellValue("Elettronica");
            exampleRow.createCell(4).setCellValue("STANDARD");
            exampleRow.createCell(5).setCellValue("PEZZO");
            exampleRow.createCell(6).setCellValue(99.90);
            exampleRow.createCell(7).setCellValue(50.00);
            exampleRow.createCell(8).setCellValue(22.00);
            exampleRow.createCell(9).setCellValue(10.00);
            exampleRow.createCell(10).setCellValue(0.500);
            exampleRow.createCell(11).setCellValue(0.001);
            exampleRow.createCell(12).setCellValue("1234567890123");
            exampleRow.createCell(13).setCellValue(5.00);
            exampleRow.createCell(14).setCellValue("SI");
            exampleRow.createCell(15).setCellValue("SI");
            exampleRow.createCell(16).setCellValue("Note facoltative");

            // Istruzioni
            Sheet instructionsSheet = workbook.createSheet("Istruzioni");
            Row r0 = instructionsSheet.createRow(0);
            r0.createCell(0).setCellValue("ISTRUZIONI PER L'IMPORT DEI PRODOTTI");
            
            Row r2 = instructionsSheet.createRow(2);
            r2.createCell(0).setCellValue("Campi obbligatori (*):");
            Row r3 = instructionsSheet.createRow(3);
            r3.createCell(0).setCellValue("- Codice: univoco, max 50 caratteri");
            Row r4 = instructionsSheet.createRow(4);
            r4.createCell(0).setCellValue("- Nome: nome prodotto, obbligatorio");
            Row r5 = instructionsSheet.createRow(5);
            r5.createCell(0).setCellValue("- TipoProdotto: STANDARD, A_MISURA, COMPOSTO, VARIABILE, SERVIZIO");
            Row r6 = instructionsSheet.createRow(6);
            r6.createCell(0).setCellValue("- UnitaMisura: PEZZO, KG, LITRO, METRO, MQ, MC, ORA");
            Row r7 = instructionsSheet.createRow(7);
            r7.createCell(0).setCellValue("- PrezzoBase: prezzo in euro (es. 99.90)");
            
            Row r9 = instructionsSheet.createRow(9);
            r9.createCell(0).setCellValue("Campi SI/NO:");
            Row r10 = instructionsSheet.createRow(10);
            r10.createCell(0).setCellValue("- GestioneMagazzino: SI o NO (default SI)");
            Row r11 = instructionsSheet.createRow(11);
            r11.createCell(0).setCellValue("- Attivo: SI o NO (default SI)");

            Row r13 = instructionsSheet.createRow(13);
            r13.createCell(0).setCellValue("Note:");
            Row r14 = instructionsSheet.createRow(14);
            r14.createCell(0).setCellValue("- Se il Codice esiste già, il prodotto verrà aggiornato");
            Row r15 = instructionsSheet.createRow(15);
            r15.createCell(0).setCellValue("- I campi numerici usano il punto come separatore decimale");

            instructionsSheet.setColumnWidth(0, 15000);

            // Scrivi in memoria
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            workbook.close();

            excelStream = new ByteArrayInputStream(baos.toByteArray());
            excelFileName = "template_import_prodotti.xlsx";

            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error creating template", e);
            addActionError("Errore nella generazione del template: " + e.getMessage());
            return ERROR;
        }
    }

    /**
     * Export catalogo prodotti in Excel
     */
    public String exportCatalog() {
        try {
            List<Prodotto> prodotti = prodottoDAO.findAll();
            
            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("Prodotti");

            // Stile header
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Header
            Row headerRow = sheet.createRow(0);
            String[] columns = {
                "Codice", "Nome", "Descrizione", "Categoria", "TipoProdotto",
                "UnitaMisura", "PrezzoBase", "CostoAcquisto", "IvaPercentuale",
                "ScontoMassimo", "Peso", "Volume", "CodiceEAN", 
                "GiacenzaMinima", "GestioneMagazzino", "Attivo", "Note"
            };

            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 4000);
            }

            // Dati
            int rowNum = 1;
            for (Prodotto p : prodotti) {
                Row row = sheet.createRow(rowNum++);
                
                row.createCell(0).setCellValue(p.getCodice());
                row.createCell(1).setCellValue(p.getNome());
                row.createCell(2).setCellValue(p.getDescrizione() != null ? p.getDescrizione() : "");
                row.createCell(3).setCellValue(p.getCategoria() != null ? p.getCategoria() : "");
                row.createCell(4).setCellValue(p.getTipoProdotto().name());
                row.createCell(5).setCellValue(p.getUnitaMisura().name());
                row.createCell(6).setCellValue(p.getPrezzoBase() != null ? p.getPrezzoBase().doubleValue() : 0);
                row.createCell(7).setCellValue(p.getCostoAcquisto() != null ? p.getCostoAcquisto().doubleValue() : 0);
                row.createCell(8).setCellValue(p.getIvaPercentuale() != null ? p.getIvaPercentuale().doubleValue() : 22);
                row.createCell(9).setCellValue(p.getScontoMassimo() != null ? p.getScontoMassimo().doubleValue() : 0);
                row.createCell(10).setCellValue(p.getPeso() != null ? p.getPeso().doubleValue() : 0);
                row.createCell(11).setCellValue(p.getVolume() != null ? p.getVolume().doubleValue() : 0);
                row.createCell(12).setCellValue(p.getCodiceEan() != null ? p.getCodiceEan() : "");
                row.createCell(13).setCellValue(p.getGiacenzaMinima() != null ? p.getGiacenzaMinima().doubleValue() : 0);
                row.createCell(14).setCellValue(p.getGestioneMagazzino() ? "SI" : "NO");
                row.createCell(15).setCellValue(p.getAttivo() ? "SI" : "NO");
                row.createCell(16).setCellValue(p.getNote() != null ? p.getNote() : "");
            }

            // Scrivi in memoria
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            workbook.close();

            excelStream = new ByteArrayInputStream(baos.toByteArray());
            excelFileName = "catalogo_prodotti_" + System.currentTimeMillis() + ".xlsx";

            addActionMessage("Export completato: " + prodotti.size() + " prodotti esportati");
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error exporting catalog", e);
            addActionError("Errore nell'export: " + e.getMessage());
            return ERROR;
        }
    }

    /**
     * Import prodotti da Excel
     */
    public String importCatalog() {
        if (uploadFile == null) {
            addActionError("Nessun file selezionato");
            return INPUT;
        }

        try {
            FileInputStream fis = new FileInputStream(uploadFile);
            Workbook workbook = new XSSFWorkbook(fis);
            Sheet sheet = workbook.getSheetAt(0);

            User currentUser = getCurrentUser();
            
            // Salta header (riga 0)
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                try {
                    String codice = getCellValueAsString(row.getCell(0));
                    if (codice == null || codice.trim().isEmpty()) {
                        errorMessages.add("Riga " + (i+1) + ": Codice obbligatorio mancante");
                        errorCount++;
                        continue;
                    }

                    // Cerca prodotto esistente
                    Prodotto prodotto = prodottoDAO.findByCodice(codice);
                    boolean isNew = (prodotto == null);
                    
                    if (isNew) {
                        prodotto = new Prodotto();
                        prodotto.setCreatedBy(currentUser);
                    } else {
                        prodotto.setModifiedBy(currentUser);
                    }

                    // Campi obbligatori
                    prodotto.setCodice(codice);
                    
                    String nome = getCellValueAsString(row.getCell(1));
                    if (nome == null || nome.trim().isEmpty()) {
                        errorMessages.add("Riga " + (i+1) + ": Nome obbligatorio mancante");
                        errorCount++;
                        continue;
                    }
                    prodotto.setNome(nome);

                    // Campi opzionali
                    prodotto.setDescrizione(getCellValueAsString(row.getCell(2)));
                    prodotto.setCategoria(getCellValueAsString(row.getCell(3)));

                    // TipoProdotto
                    String tipoProdottoStr = getCellValueAsString(row.getCell(4));
                    if (tipoProdottoStr != null && !tipoProdottoStr.isEmpty()) {
                        try {
                            prodotto.setTipoProdotto(Prodotto.TipoProdotto.valueOf(tipoProdottoStr.toUpperCase()));
                        } catch (IllegalArgumentException e) {
                            prodotto.setTipoProdotto(Prodotto.TipoProdotto.STANDARD);
                        }
                    } else {
                        prodotto.setTipoProdotto(Prodotto.TipoProdotto.STANDARD);
                    }

                    // UnitaMisura
                    String unitaMisuraStr = getCellValueAsString(row.getCell(5));
                    if (unitaMisuraStr != null && !unitaMisuraStr.isEmpty()) {
                        try {
                            prodotto.setUnitaMisura(Prodotto.UnitaMisura.valueOf(unitaMisuraStr.toUpperCase()));
                        } catch (IllegalArgumentException e) {
                            prodotto.setUnitaMisura(Prodotto.UnitaMisura.PEZZO);
                        }
                    } else {
                        prodotto.setUnitaMisura(Prodotto.UnitaMisura.PEZZO);
                    }

                    // Prezzi e valori numerici
                    BigDecimal prezzoBase = getCellValueAsBigDecimal(row.getCell(6));
                    if (prezzoBase == null) {
                        errorMessages.add("Riga " + (i+1) + ": Prezzo base obbligatorio mancante");
                        errorCount++;
                        continue;
                    }
                    prodotto.setPrezzoBase(prezzoBase);
                    
                    prodotto.setCostoAcquisto(getCellValueAsBigDecimal(row.getCell(7)));
                    
                    BigDecimal iva = getCellValueAsBigDecimal(row.getCell(8));
                    prodotto.setIvaPercentuale(iva != null ? iva : new BigDecimal("22.00"));
                    
                    prodotto.setScontoMassimo(getCellValueAsBigDecimal(row.getCell(9)));
                    prodotto.setPeso(getCellValueAsBigDecimal(row.getCell(10)));
                    prodotto.setVolume(getCellValueAsBigDecimal(row.getCell(11)));
                    prodotto.setCodiceEan(getCellValueAsString(row.getCell(12)));
                    prodotto.setGiacenzaMinima(getCellValueAsBigDecimal(row.getCell(13)));

                    // Boolean
                    String gestioneMagazzino = getCellValueAsString(row.getCell(14));
                    prodotto.setGestioneMagazzino(!"NO".equalsIgnoreCase(gestioneMagazzino));
                    
                    String attivo = getCellValueAsString(row.getCell(15));
                    prodotto.setAttivo(!"NO".equalsIgnoreCase(attivo));
                    
                    prodotto.setNote(getCellValueAsString(row.getCell(16)));

                    // Salva
                    if (isNew) {
                        prodottoDAO.save(prodotto);
                        importedCount++;
                    } else {
                        prodottoDAO.update(prodotto);
                        updatedCount++;
                    }

                } catch (Exception e) {
                    errorMessages.add("Riga " + (i+1) + ": " + e.getMessage());
                    errorCount++;
                    logger.error("Error importing row " + i, e);
                }
            }

            workbook.close();
            fis.close();

            // Messaggio finale
            if (importedCount > 0 || updatedCount > 0) {
                addActionMessage("Import completato: " + importedCount + " nuovi, " + 
                               updatedCount + " aggiornati, " + errorCount + " errori");
            }
            if (errorCount > 0 && !errorMessages.isEmpty()) {
                for (String msg : errorMessages) {
                    addActionError(msg);
                }
            }

            return SUCCESS;

        } catch (Exception e) {
            logger.error("Error importing catalog", e);
            addActionError("Errore nell'import: " + e.getMessage());
            return ERROR;
        }
    }

    // Helper methods
    private String getCellValueAsString(Cell cell) {
        if (cell == null) return null;
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                return String.valueOf((long)cell.getNumericCellValue());
            case BOOLEAN:
                return cell.getBooleanCellValue() ? "SI" : "NO";
            default:
                return null;
        }
    }

    private BigDecimal getCellValueAsBigDecimal(Cell cell) {
        if (cell == null) return null;
        
        try {
            switch (cell.getCellType()) {
                case NUMERIC:
                    return BigDecimal.valueOf(cell.getNumericCellValue());
                case STRING:
                    String value = cell.getStringCellValue().trim();
                    if (value.isEmpty()) return null;
                    return new BigDecimal(value.replace(",", "."));
                default:
                    return null;
            }
        } catch (Exception e) {
            return null;
        }
    }

    private User getCurrentUser() {
        Map<String, Object> session = com.opensymphony.xwork2.ActionContext.getContext().getSession();
        return (User) session.get("currentUser");
    }

    // Getters and Setters
    public File getUploadFile() { return uploadFile; }
    public void setUploadFile(File uploadFile) { this.uploadFile = uploadFile; }
    public String getUploadFileContentType() { return uploadFileContentType; }
    public void setUploadFileContentType(String uploadFileContentType) { 
        this.uploadFileContentType = uploadFileContentType; 
    }
    public String getUploadFileFileName() { return uploadFileFileName; }
    public void setUploadFileFileName(String uploadFileFileName) { 
        this.uploadFileFileName = uploadFileFileName; 
    }
    public InputStream getExcelStream() { return excelStream; }
    public String getExcelFileName() { return excelFileName; }
    public int getImportedCount() { return importedCount; }
    public int getUpdatedCount() { return updatedCount; }
    public int getErrorCount() { return errorCount; }
    public List<String> getErrorMessages() { return errorMessages; }
}
