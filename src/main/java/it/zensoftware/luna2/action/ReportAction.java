package it.zensoftware.luna2.action;

import com.opensymphony.xwork2.ActionSupport;
import it.zensoftware.luna2.dao.*;
import it.zensoftware.luna2.model.*;
import it.zensoftware.luna2.util.PdfReportGenerator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Enhanced Report Action with Excel export and advanced metrics
 */
public class ReportAction extends ActionSupport {
    
    private static final Logger logger = LogManager.getLogger(ReportAction.class);
    private static final long serialVersionUID = 1L;

    private GenericDAOImpl<Fattura, Long> fatturaDAO = new GenericDAOImpl<>(Fattura.class);
    private GenericDAOImpl<Cliente, Long> clienteDAO = new GenericDAOImpl<>(Cliente.class);
    private GenericDAOImpl<Prodotto, Long> prodottoDAO = new GenericDAOImpl<>(Prodotto.class);
    private LeadDAO leadDAO;
    private GiacenzaDAO giacenzaDAO;
    private OrdineDAO ordineDAO;

    private String tipoReport;
    private Date dataInizio;
    private Date dataFine;
    private List<Map<String, Object>> reportData;
    private BigDecimal totaleVendite;
    private int numeroFatture;
    private String formato = "html"; // html, excel, pdf
    private int page = 1;
    private int pageSize = 50;
    private int totalRecords;
    private int totalPages;
    
    // Chart data
    private List<String> chartLabels;
    private List<BigDecimal> chartValues;
    
    // Excel export
    private InputStream inputStream;
    private String contentType;
    private String contentDisposition;

    @Override
    public String execute() {
        try {
            if (dataInizio == null || dataFine == null) {
                Calendar cal = Calendar.getInstance();
                dataFine = cal.getTime();
                cal.add(Calendar.MONTH, -12);
                dataInizio = cal.getTime();
            }

            // Initialize DAOs only when needed
            if ("crm".equals(tipoReport) && leadDAO == null) {
                leadDAO = new LeadDAO();
            }
            if ("magazzino".equals(tipoReport) && giacenzaDAO == null) {
                giacenzaDAO = new GiacenzaDAO();
            }
            if ("ordini".equals(tipoReport) && ordineDAO == null) {
                ordineDAO = new OrdineDAO();
            }

            if ("excel".equals(formato)) {
                return exportExcel();
            }
            if ("pdf".equals(formato)) {
                return exportPdf();
            }

            if ("vendite".equals(tipoReport)) {
                return vendite();
            } else if ("prodotti".equals(tipoReport)) {
                return prodotti();
            } else if ("clienti".equals(tipoReport)) {
                return clienti();
            } else if ("crm".equals(tipoReport)) {
                return crmLeadReport();
            } else if ("magazzino".equals(tipoReport)) {
                return magazzinoReport();
            } else if ("ordini".equals(tipoReport)) {
                return ordiniReport();
            }

            return vendite(); // Default
        } catch (Exception e) {
            logger.error("Error generating report", e);
            addActionError("Errore nella generazione del report: " + e.getMessage());
            return ERROR;
        }
    }

    public String vendite() {
        List<Fattura> fatture = fatturaDAO.findAll();
        
        // Filter by date
        fatture = fatture.stream()
            .filter(f -> f.getDataFattura() != null)
            .filter(f -> !f.getDataFattura().before(dataInizio) && !f.getDataFattura().after(dataFine))
            .collect(Collectors.toList());

        // Group by cliente
        Map<Long, List<Fattura>> grouped = fatture.stream()
            .filter(f -> f.getCliente() != null)
            .collect(Collectors.groupingBy(f -> f.getCliente().getId()));

        reportData = new ArrayList<>();
        totaleVendite = BigDecimal.ZERO;
        numeroFatture = fatture.size();

        for (Map.Entry<Long, List<Fattura>> entry : grouped.entrySet()) {
            Map<String, Object> row = new HashMap<>();
            List<Fattura> clienteFatture = entry.getValue();
            Cliente cliente = clienteFatture.get(0).getCliente();
            
            BigDecimal totale = clienteFatture.stream()
                .map(f -> f.getTotale() != null ? f.getTotale() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            row.put("cliente", cliente.getRagioneSociale());
            row.put("numeroFatture", clienteFatture.size());
            row.put("totale", totale);
            reportData.add(row);
            
            totaleVendite = totaleVendite.add(totale);
        }

        // Sort by totale descending
        reportData.sort((a, b) -> ((BigDecimal) b.get("totale")).compareTo((BigDecimal) a.get("totale")));
        
        // Pagination
        totalRecords = reportData.size();
        totalPages = (int) Math.ceil((double) totalRecords / pageSize);
        int start = (page - 1) * pageSize;
        int end = Math.min(start + pageSize, totalRecords);
        if (start < totalRecords) {
            reportData = reportData.subList(start, end);
        }
        
        // Prepare chart data - Top 10
        chartLabels = new ArrayList<>();
        chartValues = new ArrayList<>();
        int count = 0;
        for (Map<String, Object> row : reportData) {
            if (count++ >= 10) break;
            chartLabels.add((String) row.get("cliente"));
            chartValues.add((BigDecimal) row.get("totale"));
        }

        return SUCCESS;
    }

    public String prodotti() {
        List<Prodotto> prodotti = prodottoDAO.findAll();
        reportData = new ArrayList<>();
        
        for (Prodotto p : prodotti) {
            Map<String, Object> data = new HashMap<>();
            data.put("nome", p.getNome());
            data.put("codice", p.getCodice());
            data.put("prezzo", p.getPrezzoBase());
            data.put("categoria", p.getCategoria() != null ? p.getCategoria() : "N/A");
            reportData.add(data);
        }
        
        return SUCCESS;
    }

    public String clienti() {
        List<Cliente> clienti = clienteDAO.findAll();
        reportData = new ArrayList<>();
        
        for (Cliente c : clienti) {
            Map<String, Object> data = new HashMap<>();
            data.put("ragioneSociale", c.getRagioneSociale());
            data.put("partitaIva", c.getPartitaIva());
            data.put("email", c.getEmail());
            data.put("telefono", c.getTelefono() != null ? c.getTelefono() : "-");
            reportData.add(data);
        }
        
        return SUCCESS;
    }

    public String crmLeadReport() {
        try {
            List<Lead> leads = leadDAO.findAll();
            
            // Filter by date
            leads = leads.stream()
                .filter(l -> l.getDataCreazione() != null)
                .filter(l -> !l.getDataCreazione().before(dataInizio) && !l.getDataCreazione().after(dataFine))
                .collect(Collectors.toList());

            reportData = new ArrayList<>();
            Map<Lead.Stato, Long> leadPerStato = leads.stream()
                .collect(Collectors.groupingBy(Lead::getStato, Collectors.counting()));

            for (Map.Entry<Lead.Stato, Long> entry : leadPerStato.entrySet()) {
                Map<String, Object> row = new HashMap<>();
                row.put("stato", entry.getKey().name());
                row.put("numero", entry.getValue());
                row.put("percentuale", leads.size() > 0 ? (entry.getValue() * 100 / leads.size()) : 0);
                reportData.add(row);
            }
            
            // Chart data
            chartLabels = new ArrayList<>();
            chartValues = new ArrayList<>();
            for (Map<String, Object> row : reportData) {
                chartLabels.add((String) row.get("stato"));
                chartValues.add(BigDecimal.valueOf((Long) row.get("numero")));
            }

            return SUCCESS;
        } catch (Exception e) {
            logger.warn("CRM report error (module may be disabled): " + e.getMessage());
            reportData = new ArrayList<>();
            return SUCCESS;
        }
    }

    public String magazzinoReport() {
        try {
            List<Giacenza> giacenze = giacenzaDAO.findAll();
            
            reportData = new ArrayList<>();
            BigDecimal valoreTotale = BigDecimal.ZERO;

            for (Giacenza g : giacenze) {
                Map<String, Object> row = new HashMap<>();
                row.put("prodotto", g.getProdotto().getNome());
                row.put("quantita", g.getQuantitaAttuale());
                row.put("scorta", g.getQuantitaMinima());
                row.put("valore", g.getValoreGiacenza() != null ? g.getValoreGiacenza() : BigDecimal.ZERO);
                row.put("stato", g.getQuantitaAttuale().compareTo(g.getQuantitaMinima()) < 0 ? "SOTTO SCORTA" : "OK");
                reportData.add(row);
                
                if (g.getValoreGiacenza() != null) {
                    valoreTotale = valoreTotale.add(g.getValoreGiacenza());
                }
            }
            
            totaleVendite = valoreTotale; // Riuso campo
            
            // Chart data - Top 10 by value
            reportData.sort((a, b) -> ((BigDecimal) b.get("valore")).compareTo((BigDecimal) a.get("valore")));
            chartLabels = new ArrayList<>();
            chartValues = new ArrayList<>();
            int count = 0;
            for (Map<String, Object> row : reportData) {
                if (count++ >= 10) break;
                chartLabels.add((String) row.get("prodotto"));
                chartValues.add((BigDecimal) row.get("valore"));
            }

            return SUCCESS;
        } catch (Exception e) {
            logger.warn("Magazzino report error (module may be disabled): " + e.getMessage());
            reportData = new ArrayList<>();
            return SUCCESS;
        }
    }

    public String ordiniReport() {
        try {
            List<Ordine> ordini = ordineDAO.findAll();
            
            // Filter by date
            ordini = ordini.stream()
                .filter(o -> o.getDataOrdine() != null)
                .filter(o -> !o.getDataOrdine().before(dataInizio) && !o.getDataOrdine().after(dataFine))
                .collect(Collectors.toList());

            reportData = new ArrayList<>();
            Map<Ordine.Stato, Long> ordiniPerStato = ordini.stream()
                .collect(Collectors.groupingBy(Ordine::getStato, Collectors.counting()));

            for (Map.Entry<Ordine.Stato, Long> entry : ordiniPerStato.entrySet()) {
                Map<String, Object> row = new HashMap<>();
                row.put("stato", entry.getKey().name());
                row.put("numero", entry.getValue());
                reportData.add(row);
            }
            
            // Chart data
            chartLabels = new ArrayList<>();
            chartValues = new ArrayList<>();
            for (Map<String, Object> row : reportData) {
                chartLabels.add((String) row.get("stato"));
                chartValues.add(BigDecimal.valueOf((Long) row.get("numero")));
            }

            return SUCCESS;
        } catch (Exception e) {
            logger.warn("Ordini report error (module may be disabled): " + e.getMessage());
            reportData = new ArrayList<>();
            return SUCCESS;
        }
    }

    public String exportExcel() {
        try {
            // Execute report logic first based on tipo
            switch (tipoReport != null ? tipoReport : "vendite") {
                case "vendite": vendite(); break;
                case "prodotti": prodotti(); break;
                case "clienti": clienti(); break;
                case "crm": crmLeadReport(); break;
                case "magazzino": magazzinoReport(); break;
                case "ordini": ordiniReport(); break;
                default: vendite();
            }

            // Create Excel workbook
            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("Report");

            // Header style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 12);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            if (reportData != null && !reportData.isEmpty()) {
                // Header row
                Row headerRow = sheet.createRow(0);
                Map<String, Object> firstRow = reportData.get(0);
                int colIdx = 0;
                for (String key : firstRow.keySet()) {
                    Cell cell = headerRow.createCell(colIdx++);
                    cell.setCellValue(key.toUpperCase());
                    cell.setCellStyle(headerStyle);
                }

                // Data rows
                int rowIdx = 1;
                for (Map<String, Object> data : reportData) {
                    Row row = sheet.createRow(rowIdx++);
                    colIdx = 0;
                    for (Object value : data.values()) {
                        Cell cell = row.createCell(colIdx++);
                        if (value instanceof BigDecimal) {
                            cell.setCellValue(((BigDecimal) value).doubleValue());
                        } else if (value instanceof Integer) {
                            cell.setCellValue((Integer) value);
                        } else if (value instanceof Long) {
                            cell.setCellValue((Long) value);
                        } else {
                            cell.setCellValue(value != null ? value.toString() : "");
                        }
                    }
                }

                // Auto-size columns
                for (int i = 0; i < firstRow.size(); i++) {
                    sheet.autoSizeColumn(i);
                }
            }

            // Write to byte array
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            workbook.close();

            inputStream = new ByteArrayInputStream(baos.toByteArray());
            contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
            String filename = "report_" + (tipoReport != null ? tipoReport : "vendite") + "_" + sdf.format(new Date()) + ".xlsx";
            contentDisposition = "attachment; filename=\"" + filename + "\"";

            return "excel";
        } catch (Exception e) {
            logger.error("Error exporting Excel", e);
            addActionError("Errore nell'esportazione Excel: " + e.getMessage());
            return ERROR;
        }
    }
    
    public String exportPdf() {
        try {
            // Execute report logic based on type
            byte[] pdfBytes = null;
            
            switch (tipoReport != null ? tipoReport : "vendite") {
                case "vendite":
                    vendite();
                    pdfBytes = PdfReportGenerator.generateVenditeReport(reportData, totaleVendite, numeroFatture);
                    break;
                case "magazzino":
                    magazzinoReport();
                    pdfBytes = PdfReportGenerator.generateMagazzinoReport(reportData, totaleVendite);
                    break;
                case "crm":
                    crmLeadReport();
                    pdfBytes = PdfReportGenerator.generateCrmReport(reportData);
                    break;
                default:
                    vendite();
                    pdfBytes = PdfReportGenerator.generateVenditeReport(reportData, totaleVendite, numeroFatture);
            }
            
            inputStream = new ByteArrayInputStream(pdfBytes);
            contentType = "application/pdf";
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
            String filename = "report_" + (tipoReport != null ? tipoReport : "vendite") + "_" + sdf.format(new Date()) + ".pdf";
            contentDisposition = "attachment; filename=\"" + filename + "\"";
            
            return "pdf";
        } catch (Exception e) {
            logger.error("Error exporting PDF", e);
            addActionError("Errore nell'esportazione PDF: " + e.getMessage());
            return ERROR;
        }
    }

    // Getters and Setters
    public Date getDataInizio() { return dataInizio; }
    public void setDataInizio(Date dataInizio) { this.dataInizio = dataInizio; }
    public Date getDataFine() { return dataFine; }
    public void setDataFine(Date dataFine) { this.dataFine = dataFine; }
    public List<Map<String, Object>> getReportData() { return reportData; }
    public String getTipoReport() { return tipoReport; }
    public void setTipoReport(String tipoReport) { this.tipoReport = tipoReport; }
    public BigDecimal getTotaleVendite() { return totaleVendite; }
    public int getNumeroFatture() { return numeroFatture; }
    public String getFormato() { return formato; }
    public void setFormato(String formato) { this.formato = formato; }
    public List<String> getChartLabels() { return chartLabels; }
    public List<BigDecimal> getChartValues() { return chartValues; }
    public InputStream getInputStream() { return inputStream; }
    public String getContentType() { return contentType; }
    public String getContentDisposition() { return contentDisposition; }
    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    public int getPageSize() { return pageSize; }
    public void setPageSize(int pageSize) { this.pageSize = pageSize; }
    public int getTotalRecords() { return totalRecords; }
    public int getTotalPages() { return totalPages; }
}
