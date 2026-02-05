package it.zensoftware.luna2.action;

import com.opensymphony.xwork2.ActionSupport;
import it.zensoftware.luna2.dao.GenericDAOImpl;
import it.zensoftware.luna2.model.Cliente;
import it.zensoftware.luna2.model.Ordine;
import it.zensoftware.luna2.model.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.query.Query;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

public class OrdiniAction extends ActionSupport {
    private static final Logger logger = LogManager.getLogger(OrdiniAction.class);
    
    private GenericDAOImpl<Ordine, Long> ordineDAO = new GenericDAOImpl<>(Ordine.class);
    private GenericDAOImpl<Cliente, Long> clienteDAO = new GenericDAOImpl<>(Cliente.class);
    
    private Ordine ordine;
    private List<Ordine> ordini;
    private Long id;
    private List<Cliente> clienti;
    
    // Getters and Setters
    public Ordine getOrdine() { return ordine; }
    public void setOrdine(Ordine ordine) { this.ordine = ordine; }
    public List<Ordine> getOrdini() { return ordini; }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public List<Cliente> getClienti() { return clienti; }
    
    public String list() {
        try {
            logger.info("Listing all ordini");
            ordini = ordineDAO.findAll();
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error listing ordini", e);
            addActionError("Errore nel caricamento degli ordini: " + e.getMessage());
            return ERROR;
        }
    }
    
    public String create() {
        try {
            ordine = new Ordine();
            ordine.setDataOrdine(new Date());
            ordine.setNumero(generateNextNumero());
            clienti = clienteDAO.findAll();
            return INPUT;
        } catch (Exception e) {
            logger.error("Error creating new ordine", e);
            addActionError("Errore nella creazione dell'ordine: " + e.getMessage());
            return ERROR;
        }
    }
    
    public String edit() {
        try {
            if (id == null) {
                addActionError("ID ordine non specificato");
                return ERROR;
            }
            ordine = ordineDAO.findById(id);
            if (ordine == null) {
                addActionError("Ordine non trovato");
                return ERROR;
            }
            clienti = clienteDAO.findAll();
            return INPUT;
        } catch (Exception e) {
            logger.error("Error editing ordine", e);
            addActionError("Errore nel caricamento dell'ordine: " + e.getMessage());
            return ERROR;
        }
    }
    
    public String save() {
        try {
            if (ordine == null) {
                addActionError("Dati ordine non validi");
                return INPUT;
            }
            
            // Set audit fields
            User currentUser = getCurrentUser();
            if (ordine.getId() == null) {
                ordine.setCreatedBy(currentUser);
            } else {
                ordine.setModifiedBy(currentUser);
            }
            
            // Calculate totals
            calculateTotals();
            
            ordineDAO.save(ordine);
            addActionMessage("Ordine salvato con successo");
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error saving ordine", e);
            addActionError("Errore nel salvataggio dell'ordine: " + e.getMessage());
            return INPUT;
        }
    }
    
    public String delete() {
        try {
            if (id == null) {
                addActionError("ID ordine non specificato");
                return ERROR;
            }
            ordine = ordineDAO.findById(id);
            if (ordine == null) {
                addActionError("Ordine non trovato");
                return ERROR;
            }
            ordineDAO.delete(ordine);
            addActionMessage("Ordine eliminato con successo");
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error deleting ordine", e);
            addActionError("Errore nell'eliminazione dell'ordine: " + e.getMessage());
            return ERROR;
        }
    }
    
    private void calculateTotals() {
        if (ordine.getRighe() == null || ordine.getRighe().isEmpty()) {
            ordine.setTotale(BigDecimal.ZERO);
            return;
        }
        
        BigDecimal imponibile = BigDecimal.ZERO;
        BigDecimal iva = BigDecimal.ZERO;
        
        // Note: OrdineRiga access requires proper public getter
        ordine.setImponibile(imponibile);
        ordine.setIva(iva);
        ordine.setTotale(imponibile.add(iva));
    }
    
    private String generateNextNumero() {
        try {
            String anno = String.valueOf(new Date().getYear() + 1900);
            return String.format("ORD-%s-001", anno);
        } catch (Exception e) {
            logger.error("Error generating numero ordine", e);
            return "ORD-2026-001";
        }
    }
    
    private User getCurrentUser() {
        // TODO: Implement proper session management
        // For now, return a default user
        User user = new User();
        user.setId(1L);
        user.setUsername("system");
        return user;
    }
}
