package it.zensoftware.luna2.action;

import com.opensymphony.xwork2.ActionSupport;
import it.zensoftware.luna2.dao.GenericDAOImpl;
import it.zensoftware.luna2.model.Cliente;
import it.zensoftware.luna2.model.Fattura;
import it.zensoftware.luna2.model.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

public class FattureAction extends ActionSupport {
    private static final Logger logger = LogManager.getLogger(FattureAction.class);
    
    private GenericDAOImpl<Fattura, Long> fatturaDAO = new GenericDAOImpl<>(Fattura.class);
    private GenericDAOImpl<Cliente, Long> clienteDAO = new GenericDAOImpl<>(Cliente.class);
    
    private Fattura fattura;
    private List<Fattura> fatture;
    private Long id;
    private List<Cliente> clienti;
    
    // Getters and Setters
    public Fattura getFattura() { return fattura; }
    public void setFattura(Fattura fattura) { this.fattura = fattura; }
    public List<Fattura> getFatture() { return fatture; }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public List<Cliente> getClienti() { return clienti; }
    
    public String list() {
        try {
            logger.info("Listing all fatture");
            fatture = fatturaDAO.findAll();
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error listing fatture", e);
            addActionError("Errore nel caricamento delle fatture: " + e.getMessage());
            return ERROR;
        }
    }
    
    public String create() {
        try {
            fattura = new Fattura();
            fattura.setDataFattura(new Date());
            fattura.setNumero(generateNextNumero());
            clienti = clienteDAO.findAll();
            return INPUT;
        } catch (Exception e) {
            logger.error("Error creating new fattura", e);
            addActionError("Errore nella creazione della fattura: " + e.getMessage());
            return ERROR;
        }
    }
    
    public String edit() {
        try {
            if (id == null) {
                addActionError("ID fattura non specificato");
                return ERROR;
            }
            fattura = fatturaDAO.findById(id);
            if (fattura == null) {
                addActionError("Fattura non trovata");
                return ERROR;
            }
            clienti = clienteDAO.findAll();
            return INPUT;
        } catch (Exception e) {
            logger.error("Error editing fattura", e);
            addActionError("Errore nel caricamento della fattura: " + e.getMessage());
            return ERROR;
        }
    }
    
    public String save() {
        try {
            if (fattura == null) {
                addActionError("Dati fattura non validi");
                return INPUT;
            }
            
            // Set audit fields
            User currentUser = getCurrentUser();
            if (fattura.getId() == null) {
                fattura.setCreatedBy(currentUser);
            }
            
            // Calculate totals
            // calculateTotals();
            
            fatturaDAO.save(fattura);
            addActionMessage("Fattura salvata con successo");
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error saving fattura", e);
            addActionError("Errore nel salvataggio della fattura: " + e.getMessage());
            return INPUT;
        }
    }
    
    public String delete() {
        try {
            if (id == null) {
                addActionError("ID fattura non specificato");
                return ERROR;
            }
            fattura = fatturaDAO.findById(id);
            if (fattura == null) {
                addActionError("Fattura non trovata");
                return ERROR;
            }
            fatturaDAO.delete(fattura);
            addActionMessage("Fattura eliminata con successo");
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error deleting fattura", e);
            addActionError("Errore nell'eliminazione della fattura: " + e.getMessage());
            return ERROR;
        }
    }
    
    // private void calculateTotals() {
    //     if (fattura.getRighe() == null || fattura.getRighe().isEmpty()) {
    //         fattura.setTotale(BigDecimal.ZERO);
    //         return;
    //     }
    //     
    //     BigDecimal imponibile = BigDecimal.ZERO;
    //     BigDecimal iva = BigDecimal.ZERO;
    //     
    //     fattura.setImponibile(imponibile);
    //     fattura.setIva(iva);
    //     fattura.setTotale(imponibile.add(iva));
    // }
    
    private String generateNextNumero() {
        try {
            String anno = String.valueOf(new Date().getYear() + 1900);
            return String.format("FT-%s-001", anno);
        } catch (Exception e) {
            logger.error("Error generating numero fattura", e);
            return "FT-2026-001";
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
