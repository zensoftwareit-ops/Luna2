package it.zensoftware.luna2.action;

import com.opensymphony.xwork2.ActionSupport;
import it.zensoftware.luna2.dao.ClienteDAO;
import it.zensoftware.luna2.model.Cliente;
import it.zensoftware.luna2.model.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.struts2.ServletActionContext;

import java.io.*;
import java.util.List;
import java.util.Map;

/**
 * Clienti Action for managing clients
 */
public class ClientiAction extends ActionSupport {
    
    private static final Logger logger = LogManager.getLogger(ClientiAction.class);
    private static final long serialVersionUID = 1L;

    private ClienteDAO clienteDAO = new ClienteDAO();
    
    private Cliente cliente;
    private List<Cliente> clienti;
    private Long id;
    private String searchTerm;
    private String tipoFiltro;
    private String cittaFiltro;
    private String provinciaFiltro;
    private File fileImport;
    private String fileImportContentType;
    private String fileImportFileName;
    private InputStream inputStream;

    public String list() {
        try {
            if (searchTerm != null && !searchTerm.isEmpty()) {
                clienti = clienteDAO.searchByName(searchTerm);
            } else {
                clienti = clienteDAO.findAllActive();
            }
            
            // Applicare filtri aggiuntivi
            if (cittaFiltro != null && !cittaFiltro.isEmpty()) {
                clienti = filterByCitta(clienti);
            }
            if (provinciaFiltro != null && !provinciaFiltro.isEmpty()) {
                clienti = filterByProvincia(clienti);
            }
            
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error listing clienti", e);
            addActionError("Errore nel caricamento dei clienti");
            return ERROR;
        }
    }

    public String create() {
        cliente = new Cliente();
        return SUCCESS;
    }

    public String edit() {
        try {
            if (id == null) {
                addActionError("ID cliente non specificato");
                return ERROR;
            }
            cliente = clienteDAO.findById(id);
            if (cliente == null) {
                addActionError("Cliente non trovato");
                return ERROR;
            }
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error editing cliente", e);
            addActionError("Errore nel caricamento del cliente");
            return ERROR;
        }
    }

    public String save() {
        try {
            if (validateClient()) {
                User currentUser = getCurrentUser();
                
                if (cliente.getId() == null) {
                    // Generate automatic code if not provided
                    if (cliente.getCodiceCliente() == null || cliente.getCodiceCliente().trim().isEmpty()) {
                        cliente.setCodiceCliente(clienteDAO.generateNextCodiceCliente());
                    }
                    // New cliente
                    cliente.setCreatedBy(currentUser);
                    clienteDAO.save(cliente);
                    addActionMessage("Cliente creato con successo (Codice: " + cliente.getCodiceCliente() + ")");
                    logger.info("Cliente created: " + cliente.getRagioneSociale());
                } else {
                    // Update cliente
                    cliente.setModifiedBy(currentUser);
                    clienteDAO.update(cliente);
                    addActionMessage("Cliente aggiornato con successo");
                    logger.info("Cliente updated: " + cliente.getRagioneSociale());
                }
                return SUCCESS;
            }
            return INPUT;
        } catch (Exception e) {
            logger.error("Error saving cliente", e);
            addActionError("Errore nel salvataggio del cliente");
            return INPUT;
        }
    }

    public String delete() {
        try {
            if (id == null) {
                addActionError("ID cliente non specificato");
                return ERROR;
            }
            Cliente clienteToDelete = clienteDAO.findById(id);
            if (clienteToDelete != null) {
                // Soft delete
                clienteToDelete.setAttivo(false);
                clienteDAO.update(clienteToDelete);
                addActionMessage("Cliente disattivato con successo");
                logger.info("Cliente deactivated: " + clienteToDelete.getRagioneSociale());
            }
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error deleting cliente", e);
            addActionError("Errore nella cancellazione del cliente");
            return ERROR;
        }
    }

    public String view() {
        try {
            if (id == null) {
                addActionError("ID cliente non specificato");
                return ERROR;
            }
            cliente = clienteDAO.findById(id);
            if (cliente == null) {
                addActionError("Cliente non trovato");
                return ERROR;
            }
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error viewing cliente", e);
            addActionError("Errore nel caricamento del cliente");
            return ERROR;
        }
    }

    public String exportExcel() {
        try {
            List<Cliente> allClienti = clienteDAO.findAllActive();
            
            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("Clienti");
            
            // Intestazioni
            Row headerRow = sheet.createRow(0);
            String[] headers = {"Codice", "Ragione Sociale", "P.IVA", "C.F.", "Email", "Telefono", "Indirizzo", "Città", "Provincia", "CAP", "Tipo"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(createHeaderStyle(workbook));
            }
            
            // Dati
            int rowNum = 1;
            for (Cliente c : allClienti) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(c.getCodiceCliente() != null ? c.getCodiceCliente() : "");
                row.createCell(1).setCellValue(c.getRagioneSociale() != null ? c.getRagioneSociale() : "");
                row.createCell(2).setCellValue(c.getPartitaIva() != null ? c.getPartitaIva() : "");
                row.createCell(3).setCellValue(c.getCodiceFiscale() != null ? c.getCodiceFiscale() : "");
                row.createCell(4).setCellValue(c.getEmail() != null ? c.getEmail() : "");
                row.createCell(5).setCellValue(c.getTelefono() != null ? c.getTelefono() : "");
                row.createCell(6).setCellValue(c.getIndirizzo() != null ? c.getIndirizzo() : "");
                row.createCell(7).setCellValue(c.getCitta() != null ? c.getCitta() : "");
                row.createCell(8).setCellValue(c.getProvincia() != null ? c.getProvincia() : "");
                row.createCell(9).setCellValue(c.getCap() != null ? c.getCap() : "");
                row.createCell(10).setCellValue(c.getTipoAnagrafica() != null ? c.getTipoAnagrafica().toString() : "");
            }
            
            // Autosize colonne
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            
            // Scrivi il file
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            workbook.close();
            
            inputStream = new ByteArrayInputStream(baos.toByteArray());
            return "excel";
        } catch (Exception e) {
            logger.error("Error exporting clienti", e);
            addActionError("Errore nell'esportazione dei clienti");
            return ERROR;
        }
    }

    public String exportCsv() {
        try {
            List<Cliente> allClienti = clienteDAO.findAllActive();
            
            StringBuilder csv = new StringBuilder();
            csv.append("Codice,Ragione Sociale,P.IVA,C.F.,Email,Telefono,Indirizzo,Città,Provincia,CAP,Tipo\n");
            
            for (Cliente c : allClienti) {
                csv.append("\"").append(escape(c.getCodiceCliente())).append("\",");
                csv.append("\"").append(escape(c.getRagioneSociale())).append("\",");
                csv.append("\"").append(escape(c.getPartitaIva())).append("\",");
                csv.append("\"").append(escape(c.getCodiceFiscale())).append("\",");
                csv.append("\"").append(escape(c.getEmail())).append("\",");
                csv.append("\"").append(escape(c.getTelefono())).append("\",");
                csv.append("\"").append(escape(c.getIndirizzo())).append("\",");
                csv.append("\"").append(escape(c.getCitta())).append("\",");
                csv.append("\"").append(escape(c.getProvincia())).append("\",");
                csv.append("\"").append(escape(c.getCap())).append("\",");
                csv.append("\"").append(escape(c.getTipoAnagrafica() != null ? c.getTipoAnagrafica().toString() : "")).append("\"\n");
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

    private List<Cliente> filterByTipo(List<Cliente> list) {
        list.removeIf(c -> !c.getTipoAnagrafica().toString().equals(tipoFiltro));
        return list;
    }

    private List<Cliente> filterByCitta(List<Cliente> list) {
        list.removeIf(c -> c.getCitta() == null || !c.getCitta().equalsIgnoreCase(cittaFiltro));
        return list;
    }

    private List<Cliente> filterByProvincia(List<Cliente> list) {
        list.removeIf(c -> c.getProvincia() == null || !c.getProvincia().equalsIgnoreCase(provinciaFiltro));
        return list;
    }

    private boolean validateClient() {
        if (cliente.getRagioneSociale() == null || cliente.getRagioneSociale().isEmpty()) {
            addFieldError("cliente.ragioneSociale", "Ragione sociale è obbligatoria");
            return false;
        }
        return true;
    }

    private User getCurrentUser() {
        Map<String, Object> session = com.opensymphony.xwork2.ActionContext.getContext().getSession();
        return (User) session.get("currentUser");
    }

    // Getters and Setters
    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }

    public List<Cliente> getClienti() {
        return clienti;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSearchTerm() {
        return searchTerm;
    }

    public void setSearchTerm(String searchTerm) {
        this.searchTerm = searchTerm;
    }

    public String getTipoFiltro() {
        return tipoFiltro;
    }

    public void setTipoFiltro(String tipoFiltro) {
        this.tipoFiltro = tipoFiltro;
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

    public File getFileImport() {
        return fileImport;
    }

    public void setFileImport(File fileImport) {
        this.fileImport = fileImport;
    }

    public String getFileImportContentType() {
        return fileImportContentType;
    }

    public void setFileImportContentType(String fileImportContentType) {
        this.fileImportContentType = fileImportContentType;
    }

    public String getFileImportFileName() {
        return fileImportFileName;
    }

    public void setFileImportFileName(String fileImportFileName) {
        this.fileImportFileName = fileImportFileName;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public void setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    public Cliente.TipoAnagrafica[] getTipiCliente() {
        return Cliente.TipoAnagrafica.values();
    }

    // ============= IMPORT MASSIVO =============
    
    private List<Map<String, String>> previewData;
    private List<String> fileHeaders;
    private Map<String, String> fieldMapping;
    private int importedCount;
    private int errorCount;
    private List<String> importErrors;

    /**
     * Show import form
     */
    public String importForm() {
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
            ServletActionContext.getRequest().getSession().setAttribute("import_previewData", previewData);
            ServletActionContext.getRequest().getSession().setAttribute("import_fileHeaders", fileHeaders);

            logger.info("File parsed successfully: " + previewData.size() + " rows");
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
            errorCount = 0;
            importErrors = new java.util.ArrayList<>();
            User currentUser = getCurrentUser();

            // Retrieve data from session
            previewData = (List<Map<String, String>>) ServletActionContext.getRequest().getSession().getAttribute("import_previewData");
            fileHeaders = (List<String>) ServletActionContext.getRequest().getSession().getAttribute("import_fileHeaders");
            
            if (previewData == null || previewData.isEmpty()) {
                addActionError("Dati di anteprima non trovati. Ricaricare il file.");
                return INPUT;
            }

            for (int i = 0; i < previewData.size(); i++) {
                try {
                    Map<String, String> row = previewData.get(i);
                    Cliente newCliente = new Cliente();

                    // Map fields from CSV to entity
                    for (Map.Entry<String, String> mapping : fieldMapping.entrySet()) {
                        String csvField = mapping.getKey();
                        String entityField = mapping.getValue();
                        String value = row.get(csvField);

                        if (value != null && !value.trim().isEmpty()) {
                            mapFieldValue(newCliente, entityField, value.trim());
                        }
                    }

                    // Generate code if not provided
                    if (newCliente.getCodiceCliente() == null || newCliente.getCodiceCliente().trim().isEmpty()) {
                        newCliente.setCodiceCliente(clienteDAO.generateNextCodiceCliente());
                    }

                    // Validate required fields
                    if (newCliente.getRagioneSociale() == null || newCliente.getRagioneSociale().trim().isEmpty()) {
                        importErrors.add("Riga " + (i + 2) + ": Ragione sociale mancante");
                        errorCount++;
                        continue;
                    }

                    // Save cliente
                    newCliente.setCreatedBy(currentUser);
                    newCliente.setAttivo(true);
                    clienteDAO.save(newCliente);
                    importedCount++;

                } catch (Exception e) {
                    errorCount++;
                    importErrors.add("Riga " + (i + 2) + ": " + e.getMessage());
                    logger.error("Error importing row " + i, e);
                }
            }

            addActionMessage("Import completato: " + importedCount + " clienti importati, " + errorCount + " errori");
            logger.info("Import completed: " + importedCount + " imported, " + errorCount + " errors");
            
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
            String[] values = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)"); // Handle quoted commas

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
                
                if (previewData.size() >= 100) break; // Preview only first 100 rows
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
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    double numValue = cell.getNumericCellValue();
                    if (numValue == (long) numValue) {
                        return String.valueOf((long) numValue);
                    } else {
                        return String.valueOf(numValue);
                    }
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }

    private void mapFieldValue(Cliente cliente, String field, String value) {
        switch (field) {
            case "codiceCliente":
                cliente.setCodiceCliente(value);
                break;
            case "ragioneSociale":
                cliente.setRagioneSociale(value);
                break;
            case "tipoAnagrafica":
                try {
                    cliente.setTipoAnagrafica(Cliente.TipoAnagrafica.valueOf(value.toUpperCase()));
                } catch (Exception e) {
                    cliente.setTipoAnagrafica(Cliente.TipoAnagrafica.CLIENTE);
                }
                break;
            case "partitaIva":
                cliente.setPartitaIva(value);
                break;
            case "codiceFiscale":
                cliente.setCodiceFiscale(value);
                break;
            case "indirizzo":
                cliente.setIndirizzo(value);
                break;
            case "citta":
                cliente.setCitta(value);
                break;
            case "provincia":
                cliente.setProvincia(value);
                break;
            case "cap":
                cliente.setCap(value);
                break;
            case "paese":
                cliente.setPaese(value);
                break;
            case "telefono":
                cliente.setTelefono(value);
                break;
            case "email":
                cliente.setEmail(value);
                break;
            case "pec":
                cliente.setPec(value);
                break;
            case "codiceSdi":
                cliente.setCodiceSdi(value);
                break;
            case "sitoWeb":
                cliente.setSitoWeb(value);
                break;
            case "condizioniPagamento":
                cliente.setCondizioniPagamento(value);
                break;
            case "listinoDefault":
                try {
                    cliente.setListinoDefault(Long.parseLong(value));
                } catch (Exception e) {
                    logger.warn("Invalid listinoDefault value: " + value);
                }
                break;
            case "scontoPercentuale":
                try {
                    cliente.setScontoPercentuale(new java.math.BigDecimal(value));
                } catch (Exception e) {
                    logger.warn("Invalid scontoPercentuale value: " + value);
                }
                break;
            case "fidoMassimo":
                try {
                    cliente.setFidoMassimo(new java.math.BigDecimal(value));
                } catch (Exception e) {
                    logger.warn("Invalid fidoMassimo value: " + value);
                }
                break;
            case "note":
                cliente.setNote(value);
                break;
        }
    }

    // Getters and setters for import fields
    public List<Map<String, String>> getPreviewData() {
        return previewData;
    }

    public void setPreviewData(List<Map<String, String>> previewData) {
        this.previewData = previewData;
    }

    public List<String> getFileHeaders() {
        return fileHeaders;
    }

    public void setFileHeaders(List<String> fileHeaders) {
        this.fileHeaders = fileHeaders;
    }

    public Map<String, String> getFieldMapping() {
        return fieldMapping;
    }

    public void setFieldMapping(Map<String, String> fieldMapping) {
        this.fieldMapping = fieldMapping;
    }

    public int getImportedCount() {
        return importedCount;
    }

    public int getErrorCount() {
        return errorCount;
    }

    public List<String> getImportErrors() {
        return importErrors;
    }
}
