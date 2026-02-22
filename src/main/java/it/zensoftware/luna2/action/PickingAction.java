package it.zensoftware.luna2.action;

import com.opensymphony.xwork2.ActionSupport;
import com.opensymphony.xwork2.ActionContext;
import it.zensoftware.luna2.dao.WarehouseDAO;
import it.zensoftware.luna2.dao.*;
import it.zensoftware.luna2.model.PickingList;
import it.zensoftware.luna2.model.PickingList.StatoPicking;
import it.zensoftware.luna2.model.PickingItem;
import it.zensoftware.luna2.model.PickingItem.StatoItem;
import it.zensoftware.luna2.model.Ordine;
import it.zensoftware.luna2.model.Warehouse;
import it.zensoftware.luna2.model.User;
import it.zensoftware.luna2.service.PickingService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.util.*;

/**
 * Action per la gestione delle picking lists
 */
public class PickingAction extends ActionSupport {
    
    private static final Logger logger = LogManager.getLogger(PickingAction.class);
    
    private final PickingService pickingService = new PickingService();
    private final WarehouseDAO warehouseDAO = new WarehouseDAO();
    private final OrdineDAO ordineDAO = new OrdineDAO();
    
    // Picking list
    private PickingList pickingList;
    private Long pickingListId;
    private List<PickingList> pickingLists;
    private String numeroPicking;
    
    // Picking items
    private List<PickingItem> items;
    private PickingItem item;
    private Long itemId;
    
    // Ordine
    private Long ordineId;
    private Ordine ordine;
    
    // Warehouse
    private Long warehouseId;
    private List<Warehouse> warehouses;
    
    // Operazioni
    private Long utenteAssegnatoId;
    private String barcode;
    private BigDecimal quantita;
    private String motivazione;
    
    // Filtri
    private String filtroStato;
    private String searchTerm;
    
    // Dashboard
    private long countInAttesa;
    private long countInCorso;
    private long countCompletati;
    
    // Getters/Setters
    public PickingList getPickingList() { return pickingList; }
    public void setPickingList(PickingList pickingList) { this.pickingList = pickingList; }
    public Long getPickingListId() { return pickingListId; }
    public void setPickingListId(Long pickingListId) { this.pickingListId = pickingListId; }
    public List<PickingList> getPickingLists() { return pickingLists; }
    public void setPickingLists(List<PickingList> pickingLists) { this.pickingLists = pickingLists; }
    public String getNumeroPicking() { return numeroPicking; }
    public void setNumeroPicking(String numeroPicking) { this.numeroPicking = numeroPicking; }
    
    public List<PickingItem> getItems() { return items; }
    public void setItems(List<PickingItem> items) { this.items = items; }
    public PickingItem getItem() { return item; }
    public void setItem(PickingItem item) { this.item = item; }
    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }
    
    public Long getOrdineId() { return ordineId; }
    public void setOrdineId(Long ordineId) { this.ordineId = ordineId; }
    public Ordine getOrdine() { return ordine; }
    public void setOrdine(Ordine ordine) { this.ordine = ordine; }
    
    public Long getWarehouseId() { return warehouseId; }
    public void setWarehouseId(Long warehouseId) { this.warehouseId = warehouseId; }
    public List<Warehouse> getWarehouses() { return warehouses; }
    public void setWarehouses(List<Warehouse> warehouses) { this.warehouses = warehouses; }
    
    public Long getUtenteAssegnatoId() { return utenteAssegnatoId; }
    public void setUtenteAssegnatoId(Long utenteAssegnatoId) { this.utenteAssegnatoId = utenteAssegnatoId; }
    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }
    public BigDecimal getQuantita() { return quantita; }
    public void setQuantita(BigDecimal quantita) { this.quantita = quantita; }
    public String getMotivazione() { return motivazione; }
    public void setMotivazione(String motivazione) { this.motivazione = motivazione; }
    
    public String getFiltroStato() { return filtroStato; }
    public void setFiltroStato(String filtroStato) { this.filtroStato = filtroStato; }
    public String getSearchTerm() { return searchTerm; }
    public void setSearchTerm(String searchTerm) { this.searchTerm = searchTerm; }
    
    public long getCountInAttesa() { return countInAttesa; }
    public void setCountInAttesa(long count) { this.countInAttesa = count; }
    public long getCountInCorso() { return countInCorso; }
    public void setCountInCorso(long count) { this.countInCorso = count; }
    public long getCountCompletati() { return countCompletati; }
    public void setCountCompletati(long count) { this.countCompletati = count; }
    
    /**
     * Dashboard picking lists
     */
    public String dashboard() {
        try {
            warehouses = warehouseDAO.findAllActive();
            
            if (warehouseId == null && !warehouses.isEmpty()) {
                warehouseId = warehouses.get(0).getId();
            }
            
            if (warehouseId != null) {
                pickingLists = pickingService.getPickingInCorso(warehouseId);
                
                PickingListDAO dao = new PickingListDAO();
                countInAttesa = dao.countByStato(StatoPicking.ASSEGNATO, warehouseId);
                countInCorso = dao.countByStato(StatoPicking.IN_PROGRESS, warehouseId);
                countCompletati = dao.countByStato(StatoPicking.COMPLETATO, warehouseId);
            }
            
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error loading picking dashboard", e);
            addActionError("Errore caricamento dashboard: " + e.getMessage());
            return ERROR;
        }
    }
    
    /**
     * Lista picking lists
     */
    public String list() {
        try {
            warehouses = warehouseDAO.findAllActive();
            
            if (filtroStato != null && !filtroStato.isEmpty()) {
                StatoPicking stato = StatoPicking.valueOf(filtroStato);
                pickingLists = pickingService.getPickingByWarehouse(warehouseId, stato);
            } else {
                pickingLists = pickingService.getPickingByWarehouse(warehouseId, null);
            }
            
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error loading picking lists", e);
            addActionError("Errore caricamento picking lists: " + e.getMessage());
            return ERROR;
        }
    }
    
    /**
     * Dettaglio picking list
     */
    public String view() {
        try {
            if (pickingListId == null && numeroPicking != null) {
                pickingList = pickingService.findByNumero(numeroPicking);
            } else if (pickingListId != null) {
                pickingList = pickingService.getPickingWithItems(pickingListId);
            }
            
            if (pickingList == null) {
                addActionError("Picking list non trovata");
                return ERROR;
            }
            
            items = pickingList.getItems();
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error loading picking list", e);
            addActionError("Errore caricamento picking list: " + e.getMessage());
            return ERROR;
        }
    }
    
    /**
     * Form creazione picking da ordine
     */
    public String formNew() {
        try {
            warehouses = warehouseDAO.findAllActive();
            if (ordineId != null) {
                ordine = ordineDAO.findById(ordineId);
            }
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error loading form", e);
            addActionError("Errore: " + e.getMessage());
            return ERROR;
        }
    }
    
    /**
     * Crea picking list da ordine
     */
    public String create() {
        try {
            if (ordineId == null) {
                addActionError("Ordine mancante");
                return INPUT;
            }
            
            Ordine ord = ordineDAO.findById(ordineId);
            if (ord == null) {
                addActionError("Ordine non trovato");
                return INPUT;
            }
            
            User user = getCurrentUser();
            pickingList = pickingService.creaPickingListDaOrdine(ord, warehouseId, user);
            
            addActionMessage("Picking list creata: " + pickingList.getNumero());
            pickingListId = pickingList.getId();
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error creating picking list", e);
            addActionError("Errore creazione picking list: " + e.getMessage());
            return INPUT;
        }
    }
    
    /**
     * Assegna picking ad utente
     */
    public String assegna() {
        try {
            if (pickingListId == null || utenteAssegnatoId == null) {
                addActionError("Parametri mancanti");
                return INPUT;
            }
            
            User user = getCurrentUser();
            pickingList = pickingService.assegnaPicking(pickingListId, utenteAssegnatoId, user);
            
            addActionMessage("Picking list assegnata");
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error assigning picking", e);
            addActionError("Errore assegnazione: " + e.getMessage());
            return INPUT;
        }
    }
    
    /**
     * Avvia picking
     */
    public String avvia() {
        try {
            if (pickingListId == null) {
                addActionError("Picking list mancante");
                return INPUT;
            }
            
            User user = getCurrentUser();
            pickingList = pickingService.avviaPicking(pickingListId, user);
            
            addActionMessage("Picking avviato");
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error starting picking", e);
            addActionError("Errore avvio picking: " + e.getMessage());
            return INPUT;
        }
    }
    
    /**
     * Registra prelievo (mobile-friendly con barcode)
     */
    public String scan() {
        try {
            if (pickingListId == null || barcode == null || barcode.trim().isEmpty()) {
                addActionError("Parametri mancanti");
                return INPUT;
            }
            
            if (quantita == null || quantita.compareTo(BigDecimal.ZERO) <= 0) {
                quantita = BigDecimal.ONE;
            }
            
            User user = getCurrentUser();
            item = pickingService.registraPrelievo(pickingListId, barcode, quantita, user);
            
            addActionMessage("Prelievo registrato: " + item.getProdotto().getDescrizione());
            
            // Ricarica picking list aggiornata
            pickingList = pickingService.getPickingWithItems(pickingListId);
            items = pickingList.getItems();
            
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error scanning item", e);
            addActionError("Errore scansione: " + e.getMessage());
            
            // Ricarica picking list per mostrare errore
            try {
                pickingList = pickingService.getPickingWithItems(pickingListId);
                items = pickingList.getItems();
            } catch (Exception ex) {
                logger.error("Error reloading picking list", ex);
            }
            
            return INPUT;
        }
    }
    
    /**
     * Completa picking
     */
    public String completa() {
        try {
            if (pickingListId == null) {
                addActionError("Picking list mancante");
                return INPUT;
            }
            
            User user = getCurrentUser();
            pickingList = pickingService.completaPicking(pickingListId, user);
            
            addActionMessage("Picking completato");
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error completing picking", e);
            addActionError("Errore completamento: " + e.getMessage());
            return INPUT;
        }
    }
    
    /**
     * Annulla picking
     */
    public String annulla() {
        try {
            if (pickingListId == null) {
                addActionError("Picking list mancante");
                return INPUT;
            }
            
            User user = getCurrentUser();
            pickingList = pickingService.annullaPicking(pickingListId, motivazione, user);
            
            addActionMessage("Picking annullato");
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error cancelling picking", e);
            addActionError("Errore annullamento: " + e.getMessage());
            return INPUT;
        }
    }
    
    /**
     * Le mie picking lists (per operatore)
     */
    public String myPicking() {
        try {
            User user = getCurrentUser();
            if (user != null) {
                pickingLists = pickingService.getPickingByUtente(user.getId(), null);
            }
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error loading my picking", e);
            addActionError("Errore: " + e.getMessage());
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
