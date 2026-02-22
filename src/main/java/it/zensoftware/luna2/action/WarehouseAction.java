package it.zensoftware.luna2.action;

import com.opensymphony.xwork2.ActionSupport;
import com.opensymphony.xwork2.ActionContext;
import it.zensoftware.luna2.dao.WarehouseDAO;
import it.zensoftware.luna2.dao.GiacenzaDAO;
import it.zensoftware.luna2.dao.PosizioneDAO;
import it.zensoftware.luna2.dao.ProdottoDAO;
import it.zensoftware.luna2.model.Warehouse;
import it.zensoftware.luna2.model.Giacenza;
import it.zensoftware.luna2.model.Posizione;
import it.zensoftware.luna2.model.Prodotto;
import it.zensoftware.luna2.model.User;
import it.zensoftware.luna2.service.GiacenzeService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.util.*;

/**
 * Action per la gestione warehouse e giacenze multi-magazzino
 */
public class WarehouseAction extends ActionSupport {
    
    private static final Logger logger = LogManager.getLogger(WarehouseAction.class);
    
    private final WarehouseDAO warehouseDAO = new WarehouseDAO();
    private final GiacenzaDAO giacenzaDAO = new GiacenzaDAO();
    private final PosizioneDAO posizioneDAO = new PosizioneDAO();
    private final ProdottoDAO prodottoDAO = new ProdottoDAO();
    private final GiacenzeService giacenzeService = new GiacenzeService();
    
    // Warehouse
    private Warehouse warehouse;
    private Long warehouseId;
    private List<Warehouse> warehouses;
    
    // Giacenze
    private List<Giacenza> giacenze;
    private Giacenza giacenza;
    private Long giacenzaId;
    private Long prodottoId;
    
    // Posizioni
    private List<Posizione> posizioni;
    private Posizione posizione;
    private Long posizioneId;
    
    // Movimenti
    private BigDecimal quantita;
    private BigDecimal costoUnitario;
    private String causale;
    private String note;
    
    // Trasferimenti
    private Long warehouseDestinazioneId;
    
    // Report
    private Map<String, Object> reportGiacenze;
    private List<Giacenza> sottoScorta;
    private List<Giacenza> daRiordinare;
    private BigDecimal valoreTotale;
    
    // Filtri
    private String searchTerm;
    private String filtroStato;
    
    // Getters/Setters
    public Warehouse getWarehouse() { return warehouse; }
    public void setWarehouse(Warehouse warehouse) { this.warehouse = warehouse; }
    public Long getWarehouseId() { return warehouseId; }
    public void setWarehouseId(Long warehouseId) { this.warehouseId = warehouseId; }
    public List<Warehouse> getWarehouses() { return warehouses; }
    public void setWarehouses(List<Warehouse> warehouses) { this.warehouses = warehouses; }
    
    public List<Giacenza> getGiacenze() { return giacenze; }
    public void setGiacenze(List<Giacenza> giacenze) { this.giacenze = giacenze; }
    public Giacenza getGiacenza() { return giacenza; }
    public void setGiacenza(Giacenza giacenza) { this.giacenza = giacenza; }
    public Long getGiacenzaId() { return giacenzaId; }
    public void setGiacenzaId(Long giacenzaId) { this.giacenzaId = giacenzaId; }
    public Long getProdottoId() { return prodottoId; }
    public void setProdottoId(Long prodottoId) { this.prodottoId = prodottoId; }
    
    public List<Posizione> getPosizioni() { return posizioni; }
    public void setPosizioni(List<Posizione> posizioni) { this.posizioni = posizioni; }
    public Posizione getPosizione() { return posizione; }
    public void setPosizione(Posizione posizione) { this.posizione = posizione; }
    public Long getPosizioneId() { return posizioneId; }
    public void setPosizioneId(Long posizioneId) { this.posizioneId = posizioneId; }
    
    public BigDecimal getQuantita() { return quantita; }
    public void setQuantita(BigDecimal quantita) { this.quantita = quantita; }
    public BigDecimal getCostoUnitario() { return costoUnitario; }
    public void setCostoUnitario(BigDecimal costoUnitario) { this.costoUnitario = costoUnitario; }
    public String getCausale() { return causale; }
    public void setCausale(String causale) { this.causale = causale; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    
    public Long getWarehouseDestinazioneId() { return warehouseDestinazioneId; }
    public void setWarehouseDestinazioneId(Long destinazione) { this.warehouseDestinazioneId = destinazione; }
    
    public Map<String, Object> getReportGiacenze() { return reportGiacenze; }
    public void setReportGiacenze(Map<String, Object> report) { this.reportGiacenze = report; }
    public List<Giacenza> getSottoScorta() { return sottoScorta; }
    public void setSottoScorta(List<Giacenza> sottoScorta) { this.sottoScorta = sottoScorta; }
    public List<Giacenza> getDaRiordinare() { return daRiordinare; }
    public void setDaRiordinare(List<Giacenza> daRiordinare) { this.daRiordinare = daRiordinare; }
    public BigDecimal getValoreTotale() { return valoreTotale; }
    public void setValoreTotale(BigDecimal valoreTotale) { this.valoreTotale = valoreTotale; }
    
    public String getSearchTerm() { return searchTerm; }
    public void setSearchTerm(String searchTerm) { this.searchTerm = searchTerm; }
    public String getFiltroStato() { return filtroStato; }
    public void setFiltroStato(String filtroStato) { this.filtroStato = filtroStato; }
    
    /**
     * Lista warehouses
     */
    public String listWarehouses() {
        try {
            warehouses = warehouseDAO.findAllActive();
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error loading warehouses", e);
            addActionError("Errore caricamento magazzini: " + e.getMessage());
            return ERROR;
        }
    }
    
    /**
     * Form nuovo warehouse
     */
    public String newWarehouse() {
        warehouse = new Warehouse();
        return SUCCESS;
    }
    
    /**
     * Form edit warehouse
     */
    public String editWarehouse() {
        try {
            if (warehouseId == null) {
                addActionError("ID warehouse mancante");
                return ERROR;
            }
            warehouse = warehouseDAO.findById(warehouseId);
            if (warehouse == null) {
                addActionError("Warehouse non trovato");
                return ERROR;
            }
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error loading warehouse", e);
            addActionError("Errore caricamento warehouse: " + e.getMessage());
            return ERROR;
        }
    }
    
    /**
     * Salva warehouse
     */
    public String saveWarehouse() {
        try {
            if (warehouse == null) {
                addActionError("Dati warehouse mancanti");
                return INPUT;
            }
            
            // Se è il primo warehouse o è marcato come principale
            if (warehouse.isPrincipale()) {
                warehouseDAO.setPrincipale(warehouse.getId());
            }
            
            warehouse = warehouseDAO.save(warehouse);
            addActionMessage("Warehouse salvato con successo");
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error saving warehouse", e);
            addActionError("Errore salvataggio warehouse: " + e.getMessage());
            return INPUT;
        }
    }
    
    /**
     * Dashboard giacenze per warehouse
     */
    public String dashboardGiacenze() {
        try {
            warehouses = warehouseDAO.findAllActive();
            
            if (warehouseId == null && !warehouses.isEmpty()) {
                warehouseId = warehouses.get(0).getId();
            }
            
            if (warehouseId != null) {
                warehouse = warehouseDAO.findById(warehouseId);
                giacenze = giacenzaDAO.findByWarehouse(warehouseId);
                sottoScorta = giacenzaDAO.findSottoScorta(warehouseId);
                daRiordinare = giacenzaDAO.findDaRiordinare(warehouseId);
                valoreTotale = giacenzaDAO.calcolaValoreTotale(warehouseId);
            }
            
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error loading giacenze dashboard", e);
            addActionError("Errore caricamento giacenze: " + e.getMessage());
            return ERROR;
        }
    }
    
    /**
     * Lista giacenze
     */
    public String listGiacenze() {
        try {
            if (warehouseId != null) {
                giacenze = giacenzaDAO.findByWarehouse(warehouseId);
                warehouse = warehouseDAO.findById(warehouseId);
            } else if (prodottoId != null) {
                giacenze = giacenzaDAO.findByProdotto(prodottoId);
            } else {
                warehouses = warehouseDAO.findAllActive();
                if (!warehouses.isEmpty()) {
                    giacenze = giacenzaDAO.findByWarehouse(warehouses.get(0).getId());
                }
            }
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error loading giacenze", e);
            addActionError("Errore caricamento giacenze: " + e.getMessage());
            return ERROR;
        }
    }
    
    /**
     * Dettaglio giacenza
     */
    public String viewGiacenza() {
        try {
            if (giacenzaId == null) {
                addActionError("ID giacenza mancante");
                return ERROR;
            }
            giacenza = giacenzaDAO.findById(giacenzaId);
            if (giacenza == null) {
                addActionError("Giacenza non trovata");
                return ERROR;
            }
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error loading giacenza", e);
            addActionError("Errore caricamento giacenza: " + e.getMessage());
            return ERROR;
        }
    }
    
    /**
     * Form carico merce
     */
    public String formCarico() {
        try {
            warehouses = warehouseDAO.findAllActive();
            if (prodottoId != null) {
                Prodotto prodotto = prodottoDAO.findById(prodottoId);
                giacenza = new Giacenza();
                giacenza.setProdotto(prodotto);
            }
            if (warehouseId != null) {
                warehouse = warehouseDAO.findById(warehouseId);
                posizioni = posizioneDAO.findByWarehouse(warehouseId);
            }
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error loading form carico", e);
            addActionError("Errore: " + e.getMessage());
            return ERROR;
        }
    }
    
    /**
     * Registra carico
     */
    public String carico() {
        try {
            if (warehouseId == null || prodottoId == null || quantita == null) {
                addActionError("Warehouse, prodotto e quantità sono obbligatori");
                return INPUT;
            }
            
            User user = getCurrentUser();
            giacenza = giacenzeService.registraCarico(
                warehouseId, prodottoId, quantita, costoUnitario,
                posizioneId, causale, user
            );
            
            addActionMessage("Carico registrato con successo");
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error registering carico", e);
            addActionError("Errore registrazione carico: " + e.getMessage());
            return INPUT;
        }
    }
    
    /**
     * Form scarico merce
     */
    public String formScarico() {
        try {
            warehouses = warehouseDAO.findAllActive();
            if (prodottoId != null && warehouseId != null) {
                giacenza = giacenzaDAO.findByWarehouseAndProdotto(warehouseId, prodottoId);
            }
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error loading form scarico", e);
            addActionError("Errore: " + e.getMessage());
            return ERROR;
        }
    }
    
    /**
     * Registra scarico
     */
    public String scarico() {
        try {
            if (warehouseId == null || prodottoId == null || quantita == null) {
                addActionError("Warehouse, prodotto e quantità sono obbligatori");
                return INPUT;
            }
            
            User user = getCurrentUser();
            giacenza = giacenzeService.registraScarico(
                warehouseId, prodottoId, quantita, causale, user
            );
            
            addActionMessage("Scarico registrato con successo");
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error registering scarico", e);
            addActionError("Errore registrazione scarico: " + e.getMessage());
            return INPUT;
        }
    }
    
    /**
     * Form trasferimento
     */
    public String formTrasferimento() {
        try {
            warehouses = warehouseDAO.findAllActive();
            if (warehouseId != null && prodottoId != null) {
                giacenza = giacenzaDAO.findByWarehouseAndProdotto(warehouseId, prodottoId);
            }
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error loading form trasferimento", e);
            addActionError("Errore: " + e.getMessage());
            return ERROR;
        }
    }
    
    /**
     * Trasferimento tra warehouse
     */
    public String trasferimento() {
        try {
            if (warehouseId == null || warehouseDestinazioneId == null || 
                prodottoId == null || quantita == null) {
                addActionError("Tutti i campi sono obbligatori");
                return INPUT;
            }
            
            User user = getCurrentUser();
            giacenzeService.trasferisci(
                warehouseId, warehouseDestinazioneId, prodottoId, quantita, note, user
            );
            
            addActionMessage("Trasferimento completato con successo");
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error during trasferimento", e);
            addActionError("Errore trasferimento: " + e.getMessage());
            return INPUT;
        }
    }
    
    /**
     * Report giacenze
     */
    public String report() {
        try {
            if (warehouseId == null) {
                Warehouse principale = warehouseDAO.findPrincipale();
                if (principale != null) {
                    warehouseId = principale.getId();
                }
            }
            
            if (warehouseId != null) {
                reportGiacenze = giacenzeService.generaReportGiacenze(warehouseId);
                warehouse = warehouseDAO.findById(warehouseId);
            }
            
            warehouses = warehouseDAO.findAllActive();
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error generating report", e);
            addActionError("Errore generazione report: " + e.getMessage());
            return ERROR;
        }
    }
    
    /**
     * Ottiene utente corrente dalla sessione
     */
    private User getCurrentUser() {
        Map<String, Object> session = ActionContext.getContext().getSession();
        return (User) session.get("user");
    }
}
