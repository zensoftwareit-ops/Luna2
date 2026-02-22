package it.zensoftware.luna2.action;

import com.opensymphony.xwork2.ActionSupport;
import it.zensoftware.luna2.dao.GenericDAOImpl;
import it.zensoftware.luna2.model.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.util.*;

public class ReportAction extends ActionSupport {
    private static final Logger logger = LogManager.getLogger(ReportAction.class);
    
    private GenericDAOImpl<Fattura, Long> fatturaDAO = new GenericDAOImpl<>(Fattura.class);
    private GenericDAOImpl<Cliente, Long> clienteDAO = new GenericDAOImpl<>(Cliente.class);
    private GenericDAOImpl<Prodotto, Long> prodottoDAO = new GenericDAOImpl<>(Prodotto.class);
    
    private Date dataInizio;
    private Date dataFine;
    private List<Map<String, Object>> reportData;
    private String tipoReport;
    private BigDecimal totaleVendite;
    private int numeroFatture;
    
    // Getters and Setters
    public Date getDataInizio() { return dataInizio; }
    public void setDataInizio(Date dataInizio) { this.dataInizio = dataInizio; }
    
    public Date getDataFine() { return dataFine; }
    public void setDataFine(Date dataFine) { this.dataFine = dataFine; }
    
    public List<Map<String, Object>> getReportData() { return reportData; }
    public void setReportData(List<Map<String, Object>> reportData) { this.reportData = reportData; }
    
    public String getTipoReport() { return tipoReport; }
    public void setTipoReport(String tipoReport) { this.tipoReport = tipoReport; }
    
    public BigDecimal getTotaleVendite() { return totaleVendite; }
    public void setTotaleVendite(BigDecimal totaleVendite) { this.totaleVendite = totaleVendite; }
    
    public int getNumeroFatture() { return numeroFatture; }
    public void setNumeroFatture(int numeroFatture) { this.numeroFatture = numeroFatture; }
    
    @Override
    public String execute() {
        logger.info("Executing report action");
        
        if (tipoReport == null || tipoReport.isEmpty()) {
            tipoReport = "vendite";
        }
        
        switch (tipoReport) {
            case "vendite":
                return vendite();
            case "prodotti":
                return prodotti();
            case "clienti":
                return clienti();
            default:
                return ERROR;
        }
    }
    
    public String vendite() {
        try {
            logger.info("Generating vendite report");
            
            if (dataInizio == null || dataFine == null) {
                Calendar cal = Calendar.getInstance();
                cal.set(Calendar.DAY_OF_MONTH, 1);
                dataInizio = cal.getTime();
                dataFine = new Date();
            }
            
            List<Fattura> fatture = fatturaDAO.findAll();
            reportData = new ArrayList<>();
            totaleVendite = BigDecimal.ZERO;
            numeroFatture = 0;
            
            Map<Long, Map<String, Object>> clienteMap = new HashMap<>();
            for (Fattura f : fatture) {
                if (f.getDataFattura() != null && 
                    f.getDataFattura().after(dataInizio) && 
                    f.getDataFattura().before(dataFine) &&
                    f.getCliente() != null) {
                    
                    Long clienteId = f.getCliente().getId();
                    clienteMap.putIfAbsent(clienteId, new HashMap<>());
                    Map<String, Object> data = clienteMap.get(clienteId);
                    
                    data.put("cliente", f.getCliente().getRagioneSociale());
                    BigDecimal totale = (BigDecimal) data.getOrDefault("totale", BigDecimal.ZERO);
                    data.put("totale", totale.add(f.getTotale() != null ? f.getTotale() : BigDecimal.ZERO));
                    Integer count = (Integer) data.getOrDefault("numeroFatture", 0);
                    data.put("numeroFatture", count + 1);
                }
            }
            
            for (Map<String, Object> data : clienteMap.values()) {
                reportData.add(data);
                BigDecimal tot = (BigDecimal) data.get("totale");
                if (tot != null) totaleVendite = totaleVendite.add(tot);
                numeroFatture += (Integer) data.get("numeroFatture");
            }
            
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error generating vendite report", e);
            addActionError("Errore nella generazione del report: " + e.getMessage());
            return ERROR;
        }
    }
    
    public String prodotti() {
        try {
            logger.info("Generating prodotti report");
            
            List<Prodotto> prodotti = prodottoDAO.findAll();
            reportData = new ArrayList<>();
            
            for (Prodotto p : prodotti) {
                Map<String, Object> data = new HashMap<>();
                data.put("nome", p.getNome());
                data.put("codice", p.getCodice());
                data.put("prezzo", p.getPrezzoBase());
                reportData.add(data);
            }
            
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error generating prodotti report", e);
            addActionError("Errore nella generazione del report: " + e.getMessage());
            return ERROR;
        }
    }
    
    public String clienti() {
        try {
            logger.info("Generating clienti report");
            
            List<Cliente> clienti = clienteDAO.findAll();
            reportData = new ArrayList<>();
            
            for (Cliente c : clienti) {
                Map<String, Object> data = new HashMap<>();
                data.put("ragioneSociale", c.getRagioneSociale());
                data.put("partitaIva", c.getPartitaIva());
                data.put("email", c.getEmail());
                reportData.add(data);
            }
            
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error generating clienti report", e);
            addActionError("Errore nella generazione del report: " + e.getMessage());
            return ERROR;
        }
    }
}
