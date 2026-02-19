package it.zensoftware.luna2.action;

import com.opensymphony.xwork2.ActionSupport;
import it.zensoftware.luna2.dao.ComMessaDAO;
import it.zensoftware.luna2.dao.ComMessaRigaDAO;
import it.zensoftware.luna2.dao.PreventivoDAO;
import it.zensoftware.luna2.dao.ModuleSettingDAO;
import it.zensoftware.luna2.model.Commessa;
import it.zensoftware.luna2.model.ComMessaRiga;
import it.zensoftware.luna2.model.Preventivo;
import it.zensoftware.luna2.model.ModuleSetting;
import it.zensoftware.luna2.service.ComMesseService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Calendar;
import java.util.List;

/**
 * Action per la gestione delle Commesse
 */
public class ComMesseAction extends ActionSupport {

    private static final Logger logger = LogManager.getLogger(ComMesseAction.class);

    private List<Commessa> commesse;
    private Commessa commessa;
    private List<ComMessaRiga> righe;
    private ComMessaRiga riga;

    private Long id;
    private Long preventivoId;
    private Long rigaId;
    private Integer anno;
    private Integer percentualeCompletamento;

    private final ComMessaDAO comMessaDAO = new ComMessaDAO();
    private final ComMessaRigaDAO comMessaRigaDAO = new ComMessaRigaDAO();
    private final PreventivoDAO preventivoDAO = new PreventivoDAO();
    private final ModuleSettingDAO moduleSettingDAO = new ModuleSettingDAO();
    private final ComMesseService comMesseService = new ComMesseService(comMessaDAO);

    /**
     * Lista tutte le commesse
     */
    public String list() {
        try {
            // Verifica che il modulo Commesse sia abilitato
            if (!isModuloComMesseAbilitato()) {
                addActionError("Modulo Commesse non abilitato");
                return ERROR;
            }

            if (anno != null) {
                commesse = comMessaDAO.findByAnno(anno);
            } else {
                commesse = comMessaDAO.findAll();
            }

            logger.info("Lista commesse caricata: " + (commesse != null ? commesse.size() : 0) + " commesse");
            return SUCCESS;

        } catch (Exception e) {
            logger.error("Errore nel caricamento lista commesse", e);
            addActionError("Errore: " + e.getMessage());
            return ERROR;
        }
    }

    /**
     * Visualizza i dettagli di una commessa
     */
    public String view() {
        try {
            if (id == null) {
                addActionError("ID commessa non specificato");
                return ERROR;
            }

            commessa = comMessaDAO.findById(id);
            if (commessa == null) {
                addActionError("Commessa non trovata");
                return ERROR;
            }

            righe = comMessaRigaDAO.findByCommessa(id);
            return SUCCESS;

        } catch (Exception e) {
            logger.error("Errore nel caricamento commessa", e);
            addActionError("Errore: " + e.getMessage());
            return ERROR;
        }
    }

    /**
     * Crea una nuova commessa dal preventivo
     */
    public String creaFromPreventivo() {
        try {
            if (preventivoId == null) {
                addActionError("ID preventivo non specificato");
                return ERROR;
            }

            Preventivo preventivo = preventivoDAO.findById(preventivoId);
            if (preventivo == null) {
                addActionError("Preventivo non trovato");
                return ERROR;
            }

            commessa = comMesseService.creaCommessaDaPreventivo(preventivo);
            addActionMessage("Commessa creata con successo: " + commessa.getNumero());
            logger.info("Commessa creata da preventivo " + preventivo.getNumero() + " - Commessa: " + commessa.getNumero());

            return SUCCESS;

        } catch (Exception e) {
            logger.error("Errore nella creazione commessa", e);
            addActionError("Errore: " + e.getMessage());
            return ERROR;
        }
    }

    /**
     * Aggiorna lo stato della commessa
     */
    public String aggiornaStato() {
        try {
            if (id == null) {
                addActionError("ID commessa non specificato");
                return ERROR;
            }

            commessa = comMessaDAO.findById(id);
            if (commessa == null) {
                addActionError("Commessa non trovata");
                return ERROR;
            }

            // Aggiorna percentuale se fornita
            if (percentualeCompletamento != null) {
                comMesseService.aggiornaPercentualeCompletamento(commessa, percentualeCompletamento);
                addActionMessage("Percentuale completamento aggiornata: " + percentualeCompletamento + "%");
            }

            logger.info("Commessa " + commessa.getNumero() + " aggiornata - Stato: " + commessa.getStato());
            return SUCCESS;

        } catch (Exception e) {
            logger.error("Errore nell'aggiornamento commessa", e);
            addActionError("Errore: " + e.getMessage());
            return ERROR;
        }
    }

    /**
     * Completa una commessa
     */
    public String completa() {
        try {
            if (id == null) {
                addActionError("ID commessa non specificato");
                return ERROR;
            }

            commessa = comMessaDAO.findById(id);
            if (commessa == null) {
                addActionError("Commessa non trovata");
                return ERROR;
            }

            comMesseService.completaCommessa(commessa);
            addActionMessage("Commessa completata");
            logger.info("Commessa " + commessa.getNumero() + " completata");

            return SUCCESS;

        } catch (Exception e) {
            logger.error("Errore nel completamento commessa", e);
            addActionError("Errore: " + e.getMessage());
            return ERROR;
        }
    }

    /**
     * Sospende una commessa
     */
    public String sospendi() {
        try {
            if (id == null) {
                addActionError("ID commessa non specificato");
                return ERROR;
            }

            commessa = comMessaDAO.findById(id);
            if (commessa == null) {
                addActionError("Commessa non trovata");
                return ERROR;
            }

            comMesseService.sospendiCommessa(commessa);
            addActionMessage("Commessa sospesa");
            logger.info("Commessa " + commessa.getNumero() + " sospesa");

            return SUCCESS;

        } catch (Exception e) {
            logger.error("Errore nella sospensione commessa", e);
            addActionError("Errore: " + e.getMessage());
            return ERROR;
        }
    }

    /**
     * Riprende una commessa sospesa
     */
    public String riprendi() {
        try {
            if (id == null) {
                addActionError("ID commessa non specificato");
                return ERROR;
            }

            commessa = comMessaDAO.findById(id);
            if (commessa == null) {
                addActionError("Commessa non trovata");
                return ERROR;
            }

            comMesseService.riprediCommessa(commessa);
            addActionMessage("Commessa ripresa");
            logger.info("Commessa " + commessa.getNumero() + " ripresa");

            return SUCCESS;

        } catch (Exception e) {
            logger.error("Errore nella ripresa commessa", e);
            addActionError("Errore: " + e.getMessage());
            return ERROR;
        }
    }

    /**
     * Annulla una commessa
     */
    public String annulla() {
        try {
            if (id == null) {
                addActionError("ID commessa non specificato");
                return ERROR;
            }

            commessa = comMessaDAO.findById(id);
            if (commessa == null) {
                addActionError("Commessa non trovata");
                return ERROR;
            }

            comMesseService.annullaCommessa(commessa);
            addActionMessage("Commessa annullata");
            logger.info("Commessa " + commessa.getNumero() + " annullata");

            return SUCCESS;

        } catch (Exception e) {
            logger.error("Errore nell'annullamento commessa", e);
            addActionError("Errore: " + e.getMessage());
            return ERROR;
        }
    }

    /**
     * Verifica se il modulo Commesse è abilitato
     */
    private boolean isModuloComMesseAbilitato() {
        try {
            ModuleSetting setting = moduleSettingDAO.findByCode("PRODUZIONE");
            return setting != null && setting.getEnabled();
        } catch (Exception e) {
            logger.warn("Errore nella verifica del modulo Commesse", e);
            return false;
        }
    }

    // Getters and Setters
    public List<Commessa> getCommesse() { return commesse; }
    public void setCommesse(List<Commessa> commesse) { this.commesse = commesse; }

    public Commessa getCommessa() { return commessa; }
    public void setCommessa(Commessa commessa) { this.commessa = commessa; }

    public List<ComMessaRiga> getRighe() { return righe; }
    public void setRighe(List<ComMessaRiga> righe) { this.righe = righe; }

    public ComMessaRiga getRiga() { return riga; }
    public void setRiga(ComMessaRiga riga) { this.riga = riga; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPreventivoId() { return preventivoId; }
    public void setPreventivoId(Long preventivoId) { this.preventivoId = preventivoId; }

    public Long getRigaId() { return rigaId; }
    public void setRigaId(Long rigaId) { this.rigaId = rigaId; }

    public Integer getAnno() { return anno; }
    public void setAnno(Integer anno) { this.anno = anno; }

    public Integer getPercentualeCompletamento() { return percentualeCompletamento; }
    public void setPercentualeCompletamento(Integer percentualeCompletamento) { 
        this.percentualeCompletamento = percentualeCompletamento; 
    }
}
