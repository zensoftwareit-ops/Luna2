package it.zensoftware.luna2.action;

import com.opensymphony.xwork2.ActionSupport;
import it.zensoftware.luna2.dao.FornitoreDAO;
import it.zensoftware.luna2.model.Fornitore;
import it.zensoftware.luna2.model.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.struts2.ServletActionContext;

import java.io.*;
import java.util.List;
import java.util.Map;

public class FornitoriAction extends ActionSupport {
    private static final Logger logger = LogManager.getLogger(FornitoriAction.class);
    private FornitoreDAO fornitoreDAO = new FornitoreDAO();
    private Fornitore fornitore;
    private List<Fornitore> fornitori;
    private Long id;
    private String searchTerm;
    private String cittaFiltro;
    private String provinciaFiltro;
    private InputStream inputStream;
    private File fileImport;
    private String fileImportContentType;
    private String fileImportFileName;

    public String list() {
        try {
            fornitori = fornitoreDAO.findAll();
            
            // Applicare filtri
            if (searchTerm != null && !searchTerm.isEmpty()) {
                fornitori = filterByName(fornitori);
            }
            if (cittaFiltro != null && !cittaFiltro.isEmpty()) {
                fornitori = filterByCitta(fornitori);
            }
            if (provinciaFiltro != null && !provinciaFiltro.isEmpty()) {
                fornitori = filterByProvincia(fornitori);
            }
            
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error listing fornitori", e);
            addActionError("Errore nel caricamento dei fornitori");
            return ERROR;
        }
    }

    public String create() {
        fornitore = new Fornitore();
        return SUCCESS;
    }

    public String edit() {
        if (id != null) {
            fornitore = fornitoreDAO.findById(id);
        }
        return SUCCESS;
    }

    public String save() {
        User currentUser = getCurrentUser();
        if (fornitore.getId() == null) {
            // Generate automatic code if not provided
            if (fornitore.getCodiceFornitore() == null || fornitore.getCodiceFornitore().trim().isEmpty()) {
                fornitore.setCodiceFornitore(fornitoreDAO.generateNextCodiceFornitore());
            }
            fornitore.setCreatedBy(currentUser);
            fornitoreDAO.save(fornitore);
            addActionMessage("Fornitore creato con successo (Codice: " + fornitore.getCodiceFornitore() + ")");
        } else {
            fornitore.setModifiedBy(currentUser);
            fornitoreDAO.update(fornitore);
            addActionMessage("Fornitore aggiornato con successo");
        }
        return SUCCESS;
    }

    public String delete() {
        if (id != null) {
            Fornitore f = fornitoreDAO.findById(id);
            if (f != null) {
                f.setAttivo(false);
                fornitoreDAO.update(f);
                addActionMessage("Fornitore disattivato");
            }
        }
        return SUCCESS;
    }

    public String exportExcel() {
        try {
            List<Fornitore> allFornitori = fornitoreDAO.findAll();
            
            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("Fornitori");
            
            // Intestazioni
            Row headerRow = sheet.createRow(0);
            String[] headers = {"Codice", "Ragione Sociale", "P.IVA", "C.F.", "Email", "Telefono", "Sito Web", "Indirizzo", "Città", "Provincia", "CAP"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(createHeaderStyle(workbook));
            }
            
            // Dati
            int rowNum = 1;
            for (Fornitore f : allFornitori) {
                if (f.getAttivo()) {
                    Row row = sheet.createRow(rowNum++);
                    row.createCell(0).setCellValue(f.getCodiceFornitore() != null ? f.getCodiceFornitore() : "");
                    row.createCell(1).setCellValue(f.getRagioneSociale() != null ? f.getRagioneSociale() : "");
                    row.createCell(2).setCellValue(f.getPartitaIva() != null ? f.getPartitaIva() : "");
                    row.createCell(3).setCellValue(f.getCodiceFiscale() != null ? f.getCodiceFiscale() : "");
                    row.createCell(4).setCellValue(f.getEmail() != null ? f.getEmail() : "");
                    row.createCell(5).setCellValue(f.getTelefono() != null ? f.getTelefono() : "");
                    row.createCell(6).setCellValue(f.getSitoWeb() != null ? f.getSitoWeb() : "");
                    row.createCell(7).setCellValue(f.getIndirizzo() != null ? f.getIndirizzo() : "");
                    row.createCell(8).setCellValue(f.getCitta() != null ? f.getCitta() : "");
                    row.createCell(9).setCellValue(f.getProvincia() != null ? f.getProvincia() : "");
                    row.createCell(10).setCellValue(f.getCap() != null ? f.getCap() : "");
                }
            }
            
            // Autosize colonne
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            workbook.close();
            
            inputStream = new ByteArrayInputStream(baos.toByteArray());
            return "excel";
        } catch (Exception e) {
            logger.error("Error exporting fornitori", e);
            addActionError("Errore nell'esportazione dei fornitori");
            return ERROR;
        }
    }

    public String exportCsv() {
        try {
            List<Fornitore> allFornitori = fornitoreDAO.findAll();
            
            StringBuilder csv = new StringBuilder();
            csv.append("Codice,Ragione Sociale,P.IVA,C.F.,Email,Telefono,Sito Web,Indirizzo,Città,Provincia,CAP\n");
            
            for (Fornitore f : allFornitori) {
                if (f.getAttivo()) {
                    csv.append("\"").append(escape(f.getCodiceFornitore())).append("\",");
                    csv.append("\"").append(escape(f.getRagioneSociale())).append("\",");
                    csv.append("\"").append(escape(f.getPartitaIva())).append("\",");
                    csv.append("\"").append(escape(f.getCodiceFiscale())).append("\",");
                    csv.append("\"").append(escape(f.getEmail())).append("\",");
                    csv.append("\"").append(escape(f.getTelefono())).append("\",");
                    csv.append("\"").append(escape(f.getSitoWeb())).append("\",");
                    csv.append("\"").append(escape(f.getIndirizzo())).append("\",");
                    csv.append("\"").append(escape(f.getCitta())).append("\",");
                    csv.append("\"").append(escape(f.getProvincia())).append("\",");
                    csv.append("\"").append(escape(f.getCap())).append("\"\n");
                }
            }
            
            inputStream = new ByteArrayInputStream(csv.toString().getBytes("UTF-8"));
            return "csv";
        } catch (Exception e) {
            logger.error("Error exporting CSV", e);
            addActionError("Errore nell'esportazione CSV");
            return ERROR;
        }
    }

    private String escape(String str) {
        if (str == null) return "";
        return str.replace("\"", "\"\"");
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_40_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private List<Fornitore> filterByName(List<Fornitore> list) {
        list.removeIf(f -> f.getRagioneSociale() == null || 
                          (!f.getRagioneSociale().toLowerCase().contains(searchTerm.toLowerCase()) && 
                           (f.getCodiceFornitore() == null || !f.getCodiceFornitore().toLowerCase().contains(searchTerm.toLowerCase()))));
        return list;
    }

    private List<Fornitore> filterByCitta(List<Fornitore> list) {
        list.removeIf(f -> f.getCitta() == null || !f.getCitta().equalsIgnoreCase(cittaFiltro));
        return list;
    }

    private List<Fornitore> filterByProvincia(List<Fornitore> list) {
        list.removeIf(f -> f.getProvincia() == null || !f.getProvincia().equalsIgnoreCase(provinciaFiltro));
        return list;
    }

    private User getCurrentUser() {
        Map<String, Object> session = com.opensymphony.xwork2.ActionContext.getContext().getSession();
        return (User) session.get("currentUser");
    }

    public Fornitore getFornitore() { return fornitore; }
    public void setFornitore(Fornitore fornitore) { this.fornitore = fornitore; }
    public List<Fornitore> getFornitori() { return fornitori; }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getSearchTerm() {
        return searchTerm;
    }

    public void setSearchTerm(String searchTerm) {
        this.searchTerm = searchTerm;
    }

    public String getCittaFiltro() {
        return cittaFiltro;
    }

    public void setCittaFiltro(String cittaFiltro) {
        this.cittaFiltro = cittaFiltro;
    }

    public String getProvinciaFiltro() {
        return provinciaFiltro;
    }

    public void setProvinciaFiltro(String provinciaFiltro) {
        this.provinciaFiltro = provinciaFiltro;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public void setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
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

    // ============= IMPORT MASSIVO =============
    
    private List<Map<String, String>> previewData;
    private List<String> fileHeaders;
    private Map<String, String> fieldMapping;
    private int importedCount;
    private int errorCount;
    private List<String> importErrors;

    public String importForm() {
        return SUCCESS;
    }

    public String parseFile() {
        try {
            if (fileImport == null) {
                addActionError("Nessun file selezionato");
                return INPUT;
            }

            previewData = new java.util.ArrayList<>();
            fileHeaders = new java.util.ArrayList<>();

            String fileName = fileImportFileName.toLowerCase();
            
            if (fileName.endsWith(".csv")) {
                parseCSVFile();
            } else if (fileName.endsWith(".xlsx") || fileName.endsWith(".xls")) {
                parseExcelFile();
            } else {
                addActionError("Formato file non supportato. Usare CSV o Excel.");
                return INPUT;
            }

            if (previewData.isEmpty()) {
                addActionError("Il file è vuoto o non contiene dati validi");
                return INPUT;
            }

            // Save to session for later use
            ServletActionContext.getRequest().getSession().setAttribute("import_previewData_fornitori", previewData);
            ServletActionContext.getRequest().getSession().setAttribute("import_fileHeaders_fornitori", fileHeaders);

            return SUCCESS;
            
        } catch (Exception e) {
            logger.error("Error parsing file", e);
            addActionError("Errore nella lettura del file: " + e.getMessage());
            return INPUT;
        }
    }

    public String processImport() {
        try {
            if (fieldMapping == null || fieldMapping.isEmpty()) {
                addActionError("Mappatura campi non specificata");
                return ERROR;
            }

            importedCount = 0;
            errorCount = 0;
            importErrors = new java.util.ArrayList<>();
            User currentUser = getCurrentUser();

            // Retrieve data from session
            previewData = (List<Map<String, String>>) ServletActionContext.getRequest().getSession().getAttribute("import_previewData_fornitori");
            fileHeaders = (List<String>) ServletActionContext.getRequest().getSession().getAttribute("import_fileHeaders_fornitori");
            
            if (previewData == null || previewData.isEmpty()) {
                addActionError("Dati di anteprima non trovati. Ricaricare il file.");
                return INPUT;
            }

            for (int i = 0; i < previewData.size(); i++) {
                try {
                    Map<String, String> row = previewData.get(i);
                    Fornitore newFornitore = new Fornitore();

                    for (Map.Entry<String, String> mapping : fieldMapping.entrySet()) {
                        String csvField = mapping.getKey();
                        String entityField = mapping.getValue();
                        String value = row.get(csvField);

                        if (value != null && !value.trim().isEmpty()) {
                            mapFieldValue(newFornitore, entityField, value.trim());
                        }
                    }

                    if (newFornitore.getCodiceFornitore() == null || newFornitore.getCodiceFornitore().trim().isEmpty()) {
                        newFornitore.setCodiceFornitore(fornitoreDAO.generateNextCodiceFornitore());
                    }

                    if (newFornitore.getRagioneSociale() == null || newFornitore.getRagioneSociale().trim().isEmpty()) {
                        importErrors.add("Riga " + (i + 2) + ": Ragione sociale mancante");
                        errorCount++;
                        continue;
                    }

                    newFornitore.setCreatedBy(currentUser);
                    newFornitore.setAttivo(true);
                    fornitoreDAO.save(newFornitore);
                    importedCount++;

                } catch (Exception e) {
                    errorCount++;
                    importErrors.add("Riga " + (i + 2) + ": " + e.getMessage());
                }
            }

            addActionMessage("Import completato: " + importedCount + " fornitori importati, " + errorCount + " errori");
            return SUCCESS;

        } catch (Exception e) {
            logger.error("Error processing import", e);
            addActionError("Errore nell'elaborazione dell'import: " + e.getMessage());
            return ERROR;
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
                Map<String, String> row = new java.util.HashMap<>();
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
                Map<String, String> rowData = new java.util.HashMap<>();
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

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        
        switch (cell.getCellType()) {
            case STRING: return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    double numValue = cell.getNumericCellValue();
                    return numValue == (long) numValue ? String.valueOf((long) numValue) : String.valueOf(numValue);
                }
            case BOOLEAN: return String.valueOf(cell.getBooleanCellValue());
            case FORMULA: return cell.getCellFormula();
            default: return "";
        }
    }

    private void mapFieldValue(Fornitore fornitore, String field, String value) {
        switch (field) {
            case "codiceFornitore": fornitore.setCodiceFornitore(value); break;
            case "ragioneSociale": fornitore.setRagioneSociale(value); break;
            case "partitaIva": fornitore.setPartitaIva(value); break;
            case "codiceFiscale": fornitore.setCodiceFiscale(value); break;
            case "indirizzo": fornitore.setIndirizzo(value); break;
            case "citta": fornitore.setCitta(value); break;
            case "provincia": fornitore.setProvincia(value); break;
            case "cap": fornitore.setCap(value); break;
            case "paese": fornitore.setPaese(value); break;
            case "telefono": fornitore.setTelefono(value); break;
            case "email": fornitore.setEmail(value); break;
            case "sitoWeb": fornitore.setSitoWeb(value); break;
            case "condizioniPagamento": fornitore.setCondizioniPagamento(value); break;
            case "note": fornitore.setNote(value); break;
        }
    }

    public List<Map<String, String>> getPreviewData() { return previewData; }
    public void setPreviewData(List<Map<String, String>> previewData) { this.previewData = previewData; }
    public List<String> getFileHeaders() { return fileHeaders; }
    public void setFileHeaders(List<String> fileHeaders) { this.fileHeaders = fileHeaders; }
    public Map<String, String> getFieldMapping() { return fieldMapping; }
    public void setFieldMapping(Map<String, String> fieldMapping) { this.fieldMapping = fieldMapping; }
    public int getImportedCount() { return importedCount; }
    public int getErrorCount() { return errorCount; }
    public List<String> getImportErrors() { return importErrors; }
}
