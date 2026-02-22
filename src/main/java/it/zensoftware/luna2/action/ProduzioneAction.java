package it.zensoftware.luna2.action;

import com.opensymphony.xwork2.ActionSupport;
import it.zensoftware.luna2.model.Commessa;
import it.zensoftware.luna2.model.Commessa.StatoCommessa;
import it.zensoftware.luna2.model.Fattura;
import it.zensoftware.luna2.service.CommesseService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.struts2.interceptor.SessionAware;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Action per il modulo Produzione/Commesse
 * Gestisce il workflow: Preventivo → Commessa → Fattura
 */
public class ProduzioneAction extends ActionSupport implements SessionAware {

    private static final Logger logger = LogManager.getLogger(ProduzioneAction.class);
    private static final long serialVersionUID = 1L;

    private Map<String, Object> session;
    private CommesseService commesseService;

    // Parametri
    private Long id;
    private Long preventivoId;
    private String searchTerm;
    private String stato;
    private String motivazione;
    private Integer percentualeCompletamento;

    // Risultati
    private List<Commessa> commesse;
    private Commessa commessa;
    private Fattura fattura;
    private String message;
    private String errorMessage;

    // Dashboard stats
    private long countAperte;
    private long countInLavorazione;
    private long countSospese;
    private long countCompletate;
    private BigDecimal valoreAperte;
    private BigDecimal valoreInLavorazione;
    private List<Commessa> commesseScadute;

    public ProduzioneAction() {
        this.commesseService = new CommesseService();
    }

    /**
     * Dashboard commesse
     */
    public String commesse() {
        try {
            logger.info("Loading commesse dashboard");

            // Carica tutte le commesse
            if (searchTerm != null && !searchTerm.trim().isEmpty()) {
                commesse = commesseService.search(searchTerm);
            } else if (stato != null && !stato.isEmpty()) {
                StatoCommessa statoEnum = StatoCommessa.valueOf(stato);
                commesse = commesseService.findByStato(statoEnum);
            } else {
                commesse = commesseService.findAll();
            }

            // Statistiche
            countAperte = commesseService.findByStato(StatoCommessa.APERTA).size();
            countInLavorazione = commesseService.findByStato(StatoCommessa.IN_LAVORAZIONE).size();
            countSospese = commesseService.findByStato(StatoCommessa.SOSPESA).size();
            countCompletate = commesseService.findByStato(StatoCommessa.COMPLETATA).size();

            valoreAperte = commesseService.calcolaValoreTotale(StatoCommessa.APERTA);
            valoreInLavorazione = commesseService.calcolaValoreTotale(StatoCommessa.IN_LAVORAZIONE);

            commesseScadute = commesseService.findScadute();

            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error loading commesse dashboard", e);
            errorMessage = "Errore caricamento dashboard: " + e.getMessage();
            return ERROR;
        }
    }

    /**
     * Visualizza dettaglio commessa
     */
    public String view() {
        try {
            if (id == null) {
                errorMessage = "ID commessa non specificato";
                return ERROR;
            }

            logger.info("Loading commessa details: {}", id);
            commessa = commesseService.findWithRighe(id);

            if (commessa == null) {
                errorMessage = "Commessa non trovata";
                return ERROR;
            }

            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error loading commessa details", e);
            errorMessage = "Errore caricamento commessa: " + e.getMessage();
            return ERROR;
        }
    }

    /**
     * Crea commessa da preventivo
     */
    public String creaCommessaDaPreventivo() {
        try {
            if (preventivoId == null) {
                errorMessage = "ID preventivo non specificato";
                return ERROR;
            }

            logger.info("Creating commessa from preventivo: {}", preventivoId);
            commessa = commesseService.creaCommessaDaPreventivo(preventivoId);

            message = "Commessa " + commessa.getNumero() + " creata con successo";
            return SUCCESS;
        } catch (IllegalStateException e) {
            logger.warn("Cannot create commessa: {}", e.getMessage());
            errorMessage = e.getMessage();
            return ERROR;
        } catch (Exception e) {
            logger.error("Error creating commessa from preventivo", e);
            errorMessage = "Errore creazione commessa: " + e.getMessage();
            return ERROR;
        }
    }

    /**
     * Avvia lavorazione commessa
     */
    public String avviaLavorazione() {
        try {
            if (id == null) {
                errorMessage = "ID commessa non specificato";
                return ERROR;
            }

            logger.info("Starting commessa: {}", id);
            commessa = commesseService.avviaLavorazione(id);

            message = "Commessa avviata con successo";
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error starting commessa", e);
            errorMessage = "Errore avvio commessa: " + e.getMessage();
            return ERROR;
        }
    }

    /**
     * Sospendi commessa
     */
    public String sospendi() {
        try {
            if (id == null) {
                errorMessage = "ID commessa non specificato";
                return ERROR;
            }

            logger.info("Suspending commessa: {}", id);
            commessa = commesseService.sospendiCommessa(id, motivazione);

            message = "Commessa sospesa";
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error suspending commessa", e);
            errorMessage = "Errore sospensione commessa: " + e.getMessage();
            return ERROR;
        }
    }

    /**
     * Riprendi commessa
     */
    public String riprendi() {
        try {
            if (id == null) {
                errorMessage = "ID commessa non specificato";
                return ERROR;
            }

            logger.info("Resuming commessa: {}", id);
            commessa = commesseService.riprendiCommessa(id);

            message = "Commessa ripresa";
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error resuming commessa", e);
            errorMessage = "Errore ripresa commessa: " + e.getMessage();
            return ERROR;
        }
    }

    /**
     * Completa commessa
     */
    public String completa() {
        try {
            if (id == null) {
                errorMessage = "ID commessa non specificato";
                return ERROR;
            }

            logger.info("Completing commessa: {}", id);
            commessa = commesseService.completaCommessa(id);

            message = "Commessa completata";
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error completing commessa", e);
            errorMessage = "Errore completamento commessa: " + e.getMessage();
            return ERROR;
        }
    }

    /**
     * Annulla commessa
     */
    public String annulla() {
        try {
            if (id == null) {
                errorMessage = "ID commessa non specificato";
                return ERROR;
            }

            logger.info("Cancelling commessa: {}", id);
            commessa = commesseService.annullaCommessa(id, motivazione);

            message = "Commessa annullata";
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error cancelling commessa", e);
            errorMessage = "Errore annullamento commessa: " + e.getMessage();
            return ERROR;
        }
    }

    /**
     * Aggiorna percentuale completamento
     */
    public String aggiornaPercentuale() {
        try {
            if (id == null || percentualeCompletamento == null) {
                errorMessage = "Parametri non validi";
                return ERROR;
            }

            logger.info("Updating commessa {} percentuale to {}", id, percentualeCompletamento);
            commessa = commesseService.aggiornaPercentuale(id, percentualeCompletamento);

            message = "Percentuale aggiornata";
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error updating percentuale", e);
            errorMessage = "Errore aggiornamento percentuale: " + e.getMessage();
            return ERROR;
        }
    }

    /**
     * Crea fattura da commessa
     */
    public String creaFatturaDaCommessa() {
        try {
            if (id == null) {
                errorMessage = "ID commessa non specificato";
                return ERROR;
            }

            logger.info("Creating fattura from commessa: {}", id);
            fattura = commesseService.creaFatturaDaCommessa(id);

            message = "Fattura " + fattura.getNumero() + " creata con successo";
            return SUCCESS;
        } catch (IllegalStateException e) {
            logger.warn("Cannot create fattura: {}", e.getMessage());
            errorMessage = e.getMessage();
            return ERROR;
        } catch (Exception e) {
            logger.error("Error creating fattura from commessa", e);
            errorMessage = "Errore creazione fattura: " + e.getMessage();
            return ERROR;
        }
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPreventivoId() { return preventivoId; }
    public void setPreventivoId(Long preventivoId) { this.preventivoId = preventivoId; }

    public String getSearchTerm() { return searchTerm; }
    public void setSearchTerm(String searchTerm) { this.searchTerm = searchTerm; }

    public String getStato() { return stato; }
    public void setStato(String stato) { this.stato = stato; }

    public String getMotivazione() { return motivazione; }
    public void setMotivazione(String motivazione) { this.motivazione = motivazione; }

    public Integer getPercentualeCompletamento() { return percentualeCompletamento; }
    public void setPercentualeCompletamento(Integer percentualeCompletamento) { 
        this.percentualeCompletamento = percentualeCompletamento; 
    }

    public List<Commessa> getCommesse() { return commesse; }
    public void setCommesse(List<Commessa> commesse) { this.commesse = commesse; }

    public Commessa getCommessa() { return commessa; }
    public void setCommessa(Commessa commessa) { this.commessa = commessa; }

    public Fattura getFattura() { return fattura; }
    public void setFattura(Fattura fattura) { this.fattura = fattura; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public long getCountAperte() { return countAperte; }
    public void setCountAperte(long countAperte) { this.countAperte = countAperte; }

    public long getCountInLavorazione() { return countInLavorazione; }
    public void setCountInLavorazione(long countInLavorazione) { 
        this.countInLavorazione = countInLavorazione; 
    }

    public long getCountSospese() { return countSospese; }
    public void setCountSospese(long countSospese) { this.countSospese = countSospese; }

    public long getCountCompletate() { return countCompletate; }
    public void setCountCompletate(long countCompletate) { this.countCompletate = countCompletate; }

    public BigDecimal getValoreAperte() { return valoreAperte; }
    public void setValoreAperte(BigDecimal valoreAperte) { this.valoreAperte = valoreAperte; }

    public BigDecimal getValoreInLavorazione() { return valoreInLavorazione; }
    public void setValoreInLavorazione(BigDecimal valoreInLavorazione) { 
        this.valoreInLavorazione = valoreInLavorazione; 
    }

    public List<Commessa> getCommesseScadute() { return commesseScadute; }
    public void setCommesseScadute(List<Commessa> commesseScadute) { 
        this.commesseScadute = commesseScadute; 
    }

    @Override
    public void setSession(Map<String, Object> session) {
        this.session = session;
    }
}
