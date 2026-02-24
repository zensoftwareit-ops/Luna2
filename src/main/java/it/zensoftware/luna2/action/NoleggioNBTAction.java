package it.zensoftware.luna2.action;

import com.opensymphony.xwork2.ActionSupport;
import it.zensoftware.luna2.dao.*;
import it.zensoftware.luna2.model.*;
import it.zensoftware.luna2.service.NoleggioNBTService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

/**
 * NoleggioNBTAction - Short-term rental management (1-30 days)
 */
public class NoleggioNBTAction extends ActionSupport {
    
    private static final Logger logger = LogManager.getLogger(NoleggioNBTAction.class);
    private static final long serialVersionUID = 1L;

    private NoleggioNBTDAO nbtDAO;
    private NoleggioNBTService nbtService;
    
    private NoleggioNBT nbt;
    private List<NoleggioNBT> nbts;
    
    private Long id;
    private Long clienteId;
    private String numeroPratica;
    private String statusFiltro;
    private Double tariffaGiornaliera;
    private String marca;
    private String modello;
    private String targa;
    private Integer kmIniziali;
    private Integer kmFinali;
    private String noteConsegna;
    private String noteRestituzione;
    private Double importoPagato;
    private String metodoPagamento;
    private String motivoAnnullamento;
    private Map<String, Object> jsonResponse = new HashMap<>();
    
    public NoleggioNBTAction() {
        this.nbtDAO = new NoleggioNBTDAO();
        this.nbtService = new NoleggioNBTService();
    }
    
    public String list() {
        try {
            nbts = nbtDAO.findActive();
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error listing NBT", e);
            addActionError("Errore nel caricamento dei noleggi");
            return ERROR;
        }
    }
    
    public String view() {
        try {
            if (id == null) {
                addActionError("ID non valido");
                return ERROR;
            }
            
            nbt = nbtDAO.findById(id);
            if (nbt == null) {
                addActionError("Noleggio non trovato");
                return ERROR;
            }
            
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error viewing NBT", e);
            addActionError("Errore nel caricamento");
            return ERROR;
        }
    }
    
    public String create() {
        try {
            nbt = new NoleggioNBT();
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error creating NBT", e);
            return ERROR;
        }
    }
    
    public String save() {
        try {
            User currentUser = getCurrentUser();
            
            if (nbt.getId() == null) {
                nbt = nbtService.createRequest(nbt, 
                                              currentUser != null ? currentUser.getId() : 1L);
                addActionMessage("Richiesta creata: " + nbt.getNumeroPratica());
            } else {
                nbtDAO.update(nbt);
                addActionMessage("Noleggio aggiornato");
            }
            
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error saving NBT", e);
            addActionError("Errore nel salvataggio: " + e.getMessage());
            return INPUT;
        }
    }
    
    public String sendQuote() {
        try {
            if (id == null || tariffaGiornaliera == null) {
                jsonResponse.put("success", false);
                return SUCCESS;
            }
            
            nbt = nbtService.sendQuote(id, tariffaGiornaliera, 
                                      getCurrentUser() != null ? getCurrentUser().getId() : 1L);
            
            jsonResponse.put("success", true);
            jsonResponse.put("numeroPratica", nbt.getNumeroPratica());
            jsonResponse.put("importoTotale", nbt.getImportoTotale());
            jsonResponse.put("preventivoValidoFino", nbt.getPreventivoValidoFino());
            
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error sending quote", e);
            jsonResponse.put("success", false);
            jsonResponse.put("error", e.getMessage());
            return SUCCESS;
        }
    }
    
    public String confirmBooking() {
        try {
            if (id == null) {
                jsonResponse.put("success", false);
                return SUCCESS;
            }
            
            nbt = nbtService.confirmBooking(id, 
                                           getCurrentUser() != null ? getCurrentUser().getId() : 1L);
            
            jsonResponse.put("success", true);
            jsonResponse.put("status", nbt.getStatus().toString());
            
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error confirming booking", e);
            jsonResponse.put("success", false);
            jsonResponse.put("error", e.getMessage());
            return SUCCESS;
        }
    }
    
    public String assignVehicle() {
        try {
            if (id == null || marca == null || modello == null || targa == null) {
                jsonResponse.put("success", false);
                return SUCCESS;
            }
            
            nbt = nbtService.assignVehicle(id, marca, modello, targa, 
                                          getCurrentUser() != null ? getCurrentUser().getId() : 1L);
            
            jsonResponse.put("success", true);
            jsonResponse.put("vehicleAssigned", nbt.getMarcaModelloVeicoloAssegnato());
            jsonResponse.put("targa", nbt.getTargaVeicoloAssegnato());
            
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error assigning vehicle", e);
            jsonResponse.put("success", false);
            jsonResponse.put("error", e.getMessage());
            return SUCCESS;
        }
    }
    
    public String markReady() {
        try {
            if (id == null) {
                jsonResponse.put("success", false);
                return SUCCESS;
            }
            
            nbt = nbtService.markReadyForDelivery(id, 
                                                 getCurrentUser() != null ? getCurrentUser().getId() : 1L);
            
            jsonResponse.put("success", true);
            jsonResponse.put("status", nbt.getStatus().toString());
            
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error marking ready", e);
            jsonResponse.put("success", false);
            jsonResponse.put("error", e.getMessage());
            return SUCCESS;
        }
    }
    
    public String startRental() {
        try {
            if (id == null) {
                jsonResponse.put("success", false);
                return SUCCESS;
            }
            
            nbt = nbtService.startRental(id, kmIniziali, noteConsegna, 
                                        getCurrentUser() != null ? getCurrentUser().getId() : 1L);
            
            jsonResponse.put("success", true);
            jsonResponse.put("status", nbt.getStatus().toString());
            
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error starting rental", e);
            jsonResponse.put("success", false);
            jsonResponse.put("error", e.getMessage());
            return SUCCESS;
        }
    }
    
    public String endRental() {
        try {
            if (id == null) {
                jsonResponse.put("success", false);
                return SUCCESS;
            }
            
            nbt = nbtService.endRental(id, kmFinali, noteRestituzione, 
                                      getCurrentUser() != null ? getCurrentUser().getId() : 1L);
            
            jsonResponse.put("success", true);
            jsonResponse.put("status", nbt.getStatus().toString());
            jsonResponse.put("costoExtraKm", nbt.getCostoExtraKm());
            jsonResponse.put("importoTotale", nbt.getImportoTotale());
            
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error ending rental", e);
            jsonResponse.put("success", false);
            jsonResponse.put("error", e.getMessage());
            return SUCCESS;
        }
    }
    
    public String completeRental() {
        try {
            if (id == null || importoPagato == null) {
                jsonResponse.put("success", false);
                return SUCCESS;
            }
            
            nbt = nbtService.completeRental(id, importoPagato, metodoPagamento, 
                                           getCurrentUser() != null ? getCurrentUser().getId() : 1L);
            
            jsonResponse.put("success", true);
            jsonResponse.put("status", nbt.getStatus().toString());
            
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error completing rental", e);
            jsonResponse.put("success", false);
            jsonResponse.put("error", e.getMessage());
            return SUCCESS;
        }
    }
    
    public String cancelRental() {
        try {
            if (id == null) {
                jsonResponse.put("success", false);
                return SUCCESS;
            }
            
            nbt = nbtService.cancelRental(id, motivoAnnullamento, 
                                         getCurrentUser() != null ? getCurrentUser().getId() : 1L);
            
            jsonResponse.put("success", true);
            jsonResponse.put("status", nbt.getStatus().toString());
            
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error cancelling rental", e);
            jsonResponse.put("success", false);
            jsonResponse.put("error", e.getMessage());
            return SUCCESS;
        }
    }
    
    public String requireingFollowup() {
        try {
            List<NoleggioNBT> requiring = nbtDAO.findRequiringFollowup24h();
            jsonResponse.put("count", requiring.size());
            jsonResponse.put("nbts", requiring);
            
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error getting requiring followup", e);
            return ERROR;
        }
    }
    
    public String requireingReturn() {
        try {
            List<NoleggioNBT> requiring = nbtDAO.findRequiringReturnAlert();
            jsonResponse.put("count", requiring.size());
            jsonResponse.put("nbts", requiring);
            
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error getting requiring return", e);
            return ERROR;
        }
    }
    
    public String dashboard() {
        try {
            Map<String, Long> statusCount = new HashMap<>();
            for (NoleggioNBT.Status status : NoleggioNBT.Status.values()) {
                statusCount.put(status.toString(), nbtDAO.countByStatus(status));
            }
            
            Double totalRevenue = nbtDAO.calculateTotalRevenue();
            
            jsonResponse.put("statusData", statusCount);
            jsonResponse.put("totalRevenue", totalRevenue);
            jsonResponse.put("completedCount", nbtDAO.countByStatus(NoleggioNBT.Status.COMPLETATO));
            
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error getting dashboard", e);
            return ERROR;
        }
    }
    
    // ============== GETTERS/SETTERS ==============
    
    public NoleggioNBT getNbt() { return nbt; }
    public void setNbt(NoleggioNBT nbt) { this.nbt = nbt; }
    
    public List<NoleggioNBT> getNbts() { return nbts; }
    public void setNbts(List<NoleggioNBT> nbts) { this.nbts = nbts; }
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getClienteId() { return clienteId; }
    public void setClienteId(Long clienteId) { this.clienteId = clienteId; }
    
    public String getNumeroPratica() { return numeroPratica; }
    public void setNumeroPratica(String numeroPratica) { this.numeroPratica = numeroPratica; }
    
    public String getStatusFiltro() { return statusFiltro; }
    public void setStatusFiltro(String statusFiltro) { this.statusFiltro = statusFiltro; }
    
    public Double getTariffaGiornaliera() { return tariffaGiornaliera; }
    public void setTariffaGiornaliera(Double tariffaGiornaliera) { this.tariffaGiornaliera = tariffaGiornaliera; }
    
    public String getMarca() { return marca; }
    public void setMarca(String marca) { this.marca = marca; }
    
    public String getModello() { return modello; }
    public void setModello(String modello) { this.modello = modello; }
    
    public String getTarga() { return targa; }
    public void setTarga(String targa) { this.targa = targa; }
    
    public Integer getKmIniziali() { return kmIniziali; }
    public void setKmIniziali(Integer kmIniziali) { this.kmIniziali = kmIniziali; }
    
    public Integer getKmFinali() { return kmFinali; }
    public void setKmFinali(Integer kmFinali) { this.kmFinali = kmFinali; }
    
    public String getNoteConsegna() { return noteConsegna; }
    public void setNoteConsegna(String noteConsegna) { this.noteConsegna = noteConsegna; }
    
    public String getNoteRestituzione() { return noteRestituzione; }
    public void setNoteRestituzione(String noteRestituzione) { this.noteRestituzione = noteRestituzione; }
    
    public Double getImportoPagato() { return importoPagato; }
    public void setImportoPagato(Double importoPagato) { this.importoPagato = importoPagato; }
    
    public String getMetodoPagamento() { return metodoPagamento; }
    public void setMetodoPagamento(String metodoPagamento) { this.metodoPagamento = metodoPagamento; }
    
    public String getMotivoAnnullamento() { return motivoAnnullamento; }
    public void setMotivoAnnullamento(String motivoAnnullamento) { this.motivoAnnullamento = motivoAnnullamento; }
    
    public Map<String, Object> getJsonResponse() { return jsonResponse; }
    
    private User getCurrentUser() {
        Map<String, Object> session = com.opensymphony.xwork2.ActionContext.getContext().getSession();
        return (User) session.get("currentUser");
    }
}
