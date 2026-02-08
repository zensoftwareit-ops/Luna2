package it.zensoftware.luna2.action;

import com.opensymphony.xwork2.ActionSupport;
import it.zensoftware.luna2.dao.ProdottoDAO;
import it.zensoftware.luna2.dao.FornitoreDAO;
import it.zensoftware.luna2.model.Prodotto;
import it.zensoftware.luna2.model.Fornitore;
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
    private FornitoreDAO fornitoreDAO = new FornitoreDAO();
    private File uploadFile;
    private String uploadFileContentType;
    private String uploadFileFileName;
    private File fileImport;
    private String fileImportContentType;
    private String fileImportFileName;
    private InputStream excelStream;
    private String excelFileName;
    private int importedCount = 0;
    private int updatedCount = 0;
    private int errorCount = 0;
    private List<String> errorMessages = new ArrayList<>();
    private List<Map<String, String>> previewData;
    private List<String> fileHeaders;
    private Map<String, String> fieldMapping;

    /**
     * Mostra la pagina di import
     */
    public String importPage() {
        return SUCCESS;
    }

    /**
     * Parse uploaded file and show preview with mapping
     */
    public String parseFile() {
        try {
            if (fileImport == null) {
                addActionError("Nessun file selezionato");
                return INPUT;
            }

            previewData = new ArrayList<>();
            fileHeaders = new ArrayList<>();

            String fileName = fileImportFileName != null ? fileImportFileName.toLowerCase() : "";

            if (fileName.endsWith(".csv")) {
                parseCSVFile();
            } else if (fileName.endsWith(".xlsx") || fileName.endsWith(".xls")) {
                parseExcelFile();
            } else {
                addActionError("Formato file non supportato. Usare CSV o Excel.");
                return INPUT;
            }

            if (previewData.isEmpty()) {
                addActionError("Il file e' vuoto o non contiene dati validi");
                return INPUT;
            }

            Map<String, Object> session = com.opensymphony.xwork2.ActionContext.getContext().getSession();
            session.put("import_previewData_prodotti", previewData);
            session.put("import_fileHeaders_prodotti", fileHeaders);

            logger.info("File parsed successfully: {} rows", previewData.size());
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error parsing file", e);
            addActionError("Errore nella lettura del file: " + e.getMessage());
            return INPUT;
        }
    }

    /**
     * Process import with field mapping
     */
    public String processImport() {
        try {
            if (fieldMapping == null || fieldMapping.isEmpty()) {
                addActionError("Mappatura campi non specificata");
                return ERROR;
            }

            importedCount = 0;
            updatedCount = 0;
            errorCount = 0;
            errorMessages = new ArrayList<>();
            User currentUser = getCurrentUser();

            Map<String, Object> session = com.opensymphony.xwork2.ActionContext.getContext().getSession();
            previewData = (List<Map<String, String>>) session.get("import_previewData_prodotti");
            fileHeaders = (List<String>) session.get("import_fileHeaders_prodotti");

            if (previewData == null || previewData.isEmpty()) {
                addActionError("Dati di anteprima non trovati. Ricaricare il file.");
                return INPUT;
            }

            for (int i = 0; i < previewData.size(); i++) {
                try {
                    Map<String, String> row = previewData.get(i);
                    String codice = getMappedValue(row, "codice");
                    if (codice == null || codice.trim().isEmpty()) {
                        errorMessages.add("Riga " + (i + 2) + ": Codice obbligatorio mancante");
                        errorCount++;
                        continue;
                    }

                    Prodotto prodotto = prodottoDAO.findByCodice(codice.trim());
                    boolean isNew = (prodotto == null);

                    if (isNew) {
                        prodotto = new Prodotto();
                        prodotto.setCreatedBy(currentUser);
                    } else {
                        prodotto.setModifiedBy(currentUser);
                    }

                    prodotto.setCodice(codice.trim());

                    for (Map.Entry<String, String> mapping : fieldMapping.entrySet()) {
                        String csvField = mapping.getKey();
                        String entityField = mapping.getValue();
                        String value = row.get(csvField);

                        if (value != null && !value.trim().isEmpty()) {
                            mapFieldValue(prodotto, entityField, value.trim(), i + 2);
                        }
                    }

                    if (prodotto.getNome() == null || prodotto.getNome().trim().isEmpty()) {
                        errorMessages.add("Riga " + (i + 2) + ": Nome obbligatorio mancante");
                        errorCount++;
                        continue;
                    }

                    if (prodotto.getTipoProdotto() == null) {
                        errorMessages.add("Riga " + (i + 2) + ": TipoProdotto obbligatorio mancante");
                        errorCount++;
                        continue;
                    }

                    if (prodotto.getUnitaMisura() == null) {
                        errorMessages.add("Riga " + (i + 2) + ": UnitaMisura obbligatoria mancante");
                        errorCount++;
                        continue;
                    }

                    if (prodotto.getPrezzoBase() == null) {
                        errorMessages.add("Riga " + (i + 2) + ": Prezzo base obbligatorio mancante");
                        errorCount++;
                        continue;
                    }

                    if (isNew) {
                        prodottoDAO.save(prodotto);
                        importedCount++;
                    } else {
                        prodottoDAO.update(prodotto);
                        updatedCount++;
                    }

                } catch (Exception e) {
                    errorCount++;
                    errorMessages.add("Riga " + (i + 2) + ": " + e.getMessage());
                    logger.error("Error importing row " + i, e);
                }
            }

            addActionMessage("Import completato: " + importedCount + " nuovi, " + updatedCount +
                " aggiornati, " + errorCount + " errori");
            return SUCCESS;

        } catch (Exception e) {
            logger.error("Error processing import", e);
            addActionError("Errore nell'elaborazione dell'import: " + e.getMessage());
            return ERROR;
        }
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
                "Codice*", "Nome*", "Descrizione", "Categoria", "CodiceFornitore", "TipoProdotto*",
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
            exampleRow.createCell(4).setCellValue("FOR001");
            exampleRow.createCell(5).setCellValue("STANDARD");
            exampleRow.createCell(6).setCellValue("PEZZO");
            exampleRow.createCell(7).setCellValue(99.90);
            exampleRow.createCell(8).setCellValue(50.00);
            exampleRow.createCell(9).setCellValue(22.00);
            exampleRow.createCell(10).setCellValue(10.00);
            exampleRow.createCell(11).setCellValue(0.500);
            exampleRow.createCell(12).setCellValue(0.001);
            exampleRow.createCell(13).setCellValue("1234567890123");
            exampleRow.createCell(14).setCellValue(5.00);
            exampleRow.createCell(15).setCellValue("SI");
            exampleRow.createCell(16).setCellValue("SI");
            exampleRow.createCell(17).setCellValue("Note facoltative");

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
                "Codice", "Nome", "Descrizione", "Categoria", "CodiceFornitore", "TipoProdotto",
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
                row.createCell(4).setCellValue(p.getFornitore() != null ? p.getFornitore().getCodiceFornitore() : "");
                row.createCell(5).setCellValue(p.getTipoProdotto().name());
                row.createCell(6).setCellValue(p.getUnitaMisura().name());
                row.createCell(7).setCellValue(p.getPrezzoBase() != null ? p.getPrezzoBase().doubleValue() : 0);
                row.createCell(8).setCellValue(p.getCostoAcquisto() != null ? p.getCostoAcquisto().doubleValue() : 0);
                row.createCell(9).setCellValue(p.getIvaPercentuale() != null ? p.getIvaPercentuale().doubleValue() : 22);
                row.createCell(10).setCellValue(p.getScontoMassimo() != null ? p.getScontoMassimo().doubleValue() : 0);
                row.createCell(11).setCellValue(p.getPeso() != null ? p.getPeso().doubleValue() : 0);
                row.createCell(12).setCellValue(p.getVolume() != null ? p.getVolume().doubleValue() : 0);
                row.createCell(13).setCellValue(p.getCodiceEan() != null ? p.getCodiceEan() : "");
                row.createCell(14).setCellValue(p.getGiacenzaMinima() != null ? p.getGiacenzaMinima().doubleValue() : 0);
                row.createCell(15).setCellValue(p.getGestioneMagazzino() ? "SI" : "NO");
                row.createCell(16).setCellValue(p.getAttivo() ? "SI" : "NO");
                row.createCell(17).setCellValue(p.getNote() != null ? p.getNote() : "");
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

                    // Fornitore (opzionale)
                    String codiceFornitore = getCellValueAsString(row.getCell(4));
                    if (codiceFornitore != null && !codiceFornitore.trim().isEmpty()) {
                        Fornitore fornitore = fornitoreDAO.findByCodiceFornitore(codiceFornitore);
                        if (fornitore != null) {
                            prodotto.setFornitore(fornitore);
                        } else {
                            errorMessages.add("Riga " + (i+1) + ": Fornitore " + codiceFornitore + " non trovato (ignorato)");
                        }
                    } else {
                        prodotto.setFornitore(null);
                    }

                    // TipoProdotto
                    String tipoProdottoStr = getCellValueAsString(row.getCell(5));
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
                    String unitaMisuraStr = getCellValueAsString(row.getCell(6));
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
                    BigDecimal prezzoBase = getCellValueAsBigDecimal(row.getCell(7));
                    if (prezzoBase == null) {
                        errorMessages.add("Riga " + (i+1) + ": Prezzo base obbligatorio mancante");
                        errorCount++;
                        continue;
                    }
                    prodotto.setPrezzoBase(prezzoBase);
                    
                    prodotto.setCostoAcquisto(getCellValueAsBigDecimal(row.getCell(8)));
                    
                    BigDecimal iva = getCellValueAsBigDecimal(row.getCell(9));
                    prodotto.setIvaPercentuale(iva != null ? iva : new BigDecimal("22.00"));
                    
                    prodotto.setScontoMassimo(getCellValueAsBigDecimal(row.getCell(10)));
                    prodotto.setPeso(getCellValueAsBigDecimal(row.getCell(11)));
                    prodotto.setVolume(getCellValueAsBigDecimal(row.getCell(12)));
                    prodotto.setCodiceEan(getCellValueAsString(row.getCell(13)));
                    prodotto.setGiacenzaMinima(getCellValueAsBigDecimal(row.getCell(14)));

                    // Boolean
                    String gestioneMagazzino = getCellValueAsString(row.getCell(15));
                    prodotto.setGestioneMagazzino(!"NO".equalsIgnoreCase(gestioneMagazzino));
                    
                    String attivo = getCellValueAsString(row.getCell(16));
                    prodotto.setAttivo(!"NO".equalsIgnoreCase(attivo));
                    
                    prodotto.setNote(getCellValueAsString(row.getCell(17)));

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

    private void parseCSVFile() throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(fileImport), "UTF-8"));
        String line;
        boolean isHeader = true;

        while ((line = reader.readLine()) != null) {
            String[] values = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");

            if (isHeader) {
                for (String header : values) {
                    fileHeaders.add(header.trim().replace("\"", ""));
                }
                isHeader = false;
            } else {
                Map<String, String> row = new HashMap<>();
                for (int i = 0; i < values.length && i < fileHeaders.size(); i++) {
                    row.put(fileHeaders.get(i), values[i].trim().replace("\"", ""));
                }
                previewData.add(row);
                if (previewData.size() >= 100) break;
            }
        }
        reader.close();
    }

    private void parseExcelFile() throws IOException {
        Workbook workbook = new XSSFWorkbook(new FileInputStream(fileImport));
        Sheet sheet = workbook.getSheetAt(0);
        boolean isHeader = true;

        for (Row row : sheet) {
            if (isHeader) {
                for (Cell cell : row) {
                    fileHeaders.add(getCellValueAsString(cell));
                }
                isHeader = false;
            } else {
                Map<String, String> rowData = new HashMap<>();
                for (int i = 0; i < row.getLastCellNum() && i < fileHeaders.size(); i++) {
                    Cell cell = row.getCell(i);
                    rowData.put(fileHeaders.get(i), getCellValueAsString(cell));
                }
                previewData.add(rowData);
                if (previewData.size() >= 100) break;
            }
        }
        workbook.close();
    }

    private String getMappedValue(Map<String, String> row, String targetField) {
        if (fieldMapping == null) return null;
        for (Map.Entry<String, String> mapping : fieldMapping.entrySet()) {
            if (targetField.equals(mapping.getValue())) {
                return row.get(mapping.getKey());
            }
        }
        return null;
    }

    private void mapFieldValue(Prodotto prodotto, String field, String value, int rowNumber) {
        switch (field) {
            case "codice":
                prodotto.setCodice(value);
                break;
            case "nome":
                prodotto.setNome(value);
                break;
            case "descrizione":
                prodotto.setDescrizione(value);
                break;
            case "categoria":
                prodotto.setCategoria(value);
                break;
            case "codiceFornitore":
                Fornitore fornitore = fornitoreDAO.findByCodiceFornitore(value);
                if (fornitore != null) {
                    prodotto.setFornitore(fornitore);
                } else {
                    errorMessages.add("Riga " + rowNumber + ": Fornitore " + value + " non trovato (ignorato)");
                }
                break;
            case "tipoProdotto":
                prodotto.setTipoProdotto(parseTipoProdotto(value));
                break;
            case "unitaMisura":
                prodotto.setUnitaMisura(parseUnitaMisura(value));
                break;
            case "prezzoBase":
                prodotto.setPrezzoBase(parseBigDecimal(value));
                break;
            case "costoAcquisto":
                prodotto.setCostoAcquisto(parseBigDecimal(value));
                break;
            case "ivaPercentuale":
                prodotto.setIvaPercentuale(parseBigDecimal(value));
                break;
            case "scontoMassimo":
                prodotto.setScontoMassimo(parseBigDecimal(value));
                break;
            case "peso":
                prodotto.setPeso(parseBigDecimal(value));
                break;
            case "volume":
                prodotto.setVolume(parseBigDecimal(value));
                break;
            case "codiceEan":
                prodotto.setCodiceEan(value);
                break;
            case "giacenzaMinima":
                prodotto.setGiacenzaMinima(parseBigDecimal(value));
                break;
            case "gestioneMagazzino":
                prodotto.setGestioneMagazzino(parseBooleanSiNo(value));
                break;
            case "attivo":
                prodotto.setAttivo(parseBooleanSiNo(value));
                break;
            case "note":
                prodotto.setNote(value);
                break;
            default:
                break;
        }
    }

    private Prodotto.TipoProdotto parseTipoProdotto(String value) {
        if (value == null) return null;
        String normalized = value.trim().toUpperCase().replace(" ", "_");
        try {
            return Prodotto.TipoProdotto.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private Prodotto.UnitaMisura parseUnitaMisura(String value) {
        if (value == null) return null;
        String normalized = value.trim().toUpperCase();
        try {
            return Prodotto.UnitaMisura.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private BigDecimal parseBigDecimal(String value) {
        if (value == null) return null;
        String normalized = value.trim().replace(",", ".");
        if (normalized.isEmpty()) return null;
        try {
            return new BigDecimal(normalized);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private boolean parseBooleanSiNo(String value) {
        if (value == null) return true;
        String normalized = value.trim().toUpperCase();
        return !("NO".equals(normalized) || "FALSE".equals(normalized) || "0".equals(normalized));
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
    public File getFileImport() { return fileImport; }
    public void setFileImport(File fileImport) { this.fileImport = fileImport; }
    public String getFileImportContentType() { return fileImportContentType; }
    public void setFileImportContentType(String fileImportContentType) { 
        this.fileImportContentType = fileImportContentType; 
    }
    public String getFileImportFileName() { return fileImportFileName; }
    public void setFileImportFileName(String fileImportFileName) { 
        this.fileImportFileName = fileImportFileName; 
    }
    public InputStream getExcelStream() { return excelStream; }
    public String getExcelFileName() { return excelFileName; }
    public int getImportedCount() { return importedCount; }
    public int getUpdatedCount() { return updatedCount; }
    public int getErrorCount() { return errorCount; }
    public List<String> getErrorMessages() { return errorMessages; }
    public List<Map<String, String>> getPreviewData() { return previewData; }
    public void setPreviewData(List<Map<String, String>> previewData) { this.previewData = previewData; }
    public List<String> getFileHeaders() { return fileHeaders; }
    public void setFileHeaders(List<String> fileHeaders) { this.fileHeaders = fileHeaders; }
    public Map<String, String> getFieldMapping() { return fieldMapping; }
    public void setFieldMapping(Map<String, String> fieldMapping) { this.fieldMapping = fieldMapping; }
    public List<String> getImportErrors() { return errorMessages; }
}
