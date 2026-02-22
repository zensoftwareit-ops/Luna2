package it.zensoftware.luna2.action;

import com.opensymphony.xwork2.ActionSupport;
import it.zensoftware.luna2.dao.FatturaPassivaDAO;
import it.zensoftware.luna2.dao.FornitoreDAO;
import it.zensoftware.luna2.model.FatturaPassiva;
import it.zensoftware.luna2.service.FatturePassiveService;
import it.zensoftware.luna2.service.FattureExportService;
import it.zensoftware.luna2.util.HibernateUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;
import java.util.Calendar;

/**
 * Action per la gestione delle fatture passive (ricevute da SDI)
 * Fornisce endpoint per il polling automatico tramite cron job
 */
public class FatturePassiveAction extends ActionSupport {
    
    private static final Logger logger = LogManager.getLogger(FatturePassiveAction.class);
    
    private List<FatturaPassiva> fatturePassive;
    private FatturaPassiva fatturaPassiva;
    private Long id;
    private Integer anno;
    private String stato;
    private InputStream inputStream;
    private String contentDisposition;
    private java.io.File uploadFile;
    private String uploadFileContentType;
    private String uploadFileFileName;
    
    private final FatturaPassivaDAO fatturaPassivaDAO = new FatturaPassivaDAO();
    private final FornitoreDAO fornitoreDAO = new FornitoreDAO();
    private final FatturePassiveService fatturePassiveService = new FatturePassiveService(fatturaPassivaDAO, fornitoreDAO);
    private final FattureExportService exportService = new FattureExportService();
    
    /**
     * Lista le fatture passive ricevute
     */
    public String list() {
        try {
            if (anno != null) {
                fatturePassive = fatturaPassivaDAO.findByAnno(anno);
            } else {
                fatturePassive = fatturaPassivaDAO.findAll();
            }
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Errore nel caricamento lista fatture passive", e);
            addActionError("Errore: " + e.getMessage());
            return ERROR;
        }
    }
    
    /**
     * Visualizza il dettaglio di una fattura passiva
     */
    public String view() {
        try {
            if (id == null) {
                addActionError("ID fattura non fornito");
                return ERROR;
            }
            
            fatturaPassiva = fatturaPassivaDAO.findById(id);
            if (fatturaPassiva == null) {
                addActionError("Fattura non trovata");
                return ERROR;
            }
            
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Errore nel caricamento fattura passiva", e);
            addActionError("Errore: " + e.getMessage());
            return ERROR;
        }
    }
    
    /**
     * Sincronizza le fatture passive ricevute da SDI (eseguito come cron job)
     * Questo metodo richiama l'endpoint https://api.luna.itsolutions-cloud.com/ricevi-fatture/index.php?piva=xxxxx
     */
    public String sincronizzaFatturePassive() {
        try {
            logger.info("Inizio sincronizzazione fatture passive da SDI");
            
            // Leggi la partita IVA dalla configurazione
            String partitaIva = getCompanyPartitaIva();
            if (partitaIva == null || partitaIva.isEmpty()) {
                addActionError("Partita IVA aziendale non configurata in application.properties");
                logger.error("ERRORE: Company PIVA non configurata");
                return ERROR;
            }
            
            // Sincronizza le fatture
            fatturePassiveService.sincronizzaFatturePassive(partitaIva);
            
            addActionMessage("Sincronizzazione fatture passive completata");
            return SUCCESS;
            
        } catch (Exception e) {
            logger.error("Errore durante la sincronizzazione delle fatture passive", e);
            addActionError("Errore: " + e.getMessage());
            return ERROR;
        }
    }
    
    /**
     * Marca una fattura passiva come pagata
     */
    public String registraPagamento() {
        try {
            if (id == null) {
                addActionError("ID fattura non fornito");
                return ERROR;
            }
            
            fatturaPassiva = fatturaPassivaDAO.findById(id);
            if (fatturaPassiva == null) {
                addActionError("Fattura non trovata");
                return ERROR;
            }
            
            fatturaPassiva.setStatoPagamento(FatturaPassiva.StatoPagamento.PAGATA);
            fatturaPassivaDAO.update(fatturaPassiva);
            
            addActionMessage("Fattura marcata come pagata");
            logger.info("Fattura passiva " + fatturaPassiva.getNumero() + " marcata come pagata");
            
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Errore nel registrazione pagamento", e);
            addActionError("Errore: " + e.getMessage());
            return ERROR;
        }
    }
    
    /**
     * Elimina una fattura passiva
     */
    public String delete() {
        try {
            if (id == null) {
                addActionError("ID fattura non fornito");
                return ERROR;
            }
            
            fatturaPassiva = fatturaPassivaDAO.findById(id);
            if (fatturaPassiva == null) {
                addActionError("Fattura non trovata");
                return ERROR;
            }
            
            fatturaPassivaDAO.delete(fatturaPassiva);
            addActionMessage("Fattura eliminata");
            logger.info("Fattura passiva " + fatturaPassiva.getNumero() + " eliminata");
            
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Errore nella eliminazione fattura", e);
            addActionError("Errore: " + e.getMessage());
            return ERROR;
        }
    }

    public String esportaAssosoftware() {
        try {
            if (anno == null) {
                anno = Calendar.getInstance().get(Calendar.YEAR);
            }

            // Recupera tutte le fatture passive dell'anno
            List<FatturaPassiva> fattureEsportazione = fatturaPassivaDAO.findByAnno(anno);

            if (fattureEsportazione.isEmpty()) {
                addActionError("Nessuna fattura passiva trovata per l'anno " + anno);
                return ERROR;
            }

            // Genera il file di esportazione
            byte[] fileContent = exportService.esportaFatturePassiveAssosoftware(fattureEsportazione);
            
            // Imposta il download
            inputStream = new ByteArrayInputStream(fileContent);
            contentDisposition = "attachment;filename=" + exportService.generateFileName("passive", anno);

            logger.info("Export Assosoftware fatture passive: " + fattureEsportazione.size() + " fatture esportate per anno " + anno);
            return SUCCESS;

        } catch (Exception e) {
            logger.error("Errore nell'esportazione Assosoftware", e);
            addActionError("Errore durante l'esportazione: " + e.getMessage());
            return ERROR;
        }
    }

    public String esportaSingolaAssosoftware() {
        try {
            if (id == null) {
                addActionError("ID fattura non specificato");
                return ERROR;
            }

            fatturaPassiva = fatturaPassivaDAO.findById(id);
            if (fatturaPassiva == null) {
                addActionError("Fattura passiva non trovata");
                return ERROR;
            }

            // Genera il file di esportazione
            byte[] fileContent = exportService.esportaSingolaFatturaPassivaAssosoftware(fatturaPassiva);
            
            // Imposta il download
            inputStream = new ByteArrayInputStream(fileContent);
            contentDisposition = "attachment;filename=fattura_passiva_" + fatturaPassiva.getNumero() + "_assosoftware.txt";

            logger.info("Export Assosoftware singola fattura passiva: " + fatturaPassiva.getNumero());
            return SUCCESS;

        } catch (Exception e) {
            logger.error("Errore nell'esportazione Assosoftware singola fattura passiva", e);
            addActionError("Errore durante l'esportazione: " + e.getMessage());
            return ERROR;
        }
    }

    /**
     * Export XML di una fattura passiva (se disponibile)
     */
    public String exportXml() {
        try {
            if (id == null) {
                addActionError("ID fattura non specificato");
                return ERROR;
            }

            fatturaPassiva = fatturaPassivaDAO.findById(id);
            if (fatturaPassiva == null) {
                addActionError("Fattura passiva non trovata");
                return ERROR;
            }

            String xmlContent = fatturaPassiva.getXmlSdi();
            if (xmlContent == null || xmlContent.isEmpty()) {
                addActionError("XML non disponibile per questa fattura passiva");
                return ERROR;
            }

            inputStream = new ByteArrayInputStream(xmlContent.getBytes("UTF-8"));
            contentDisposition = "attachment;filename=FatturaPassiva_" + 
                (fatturaPassiva.getNumero() != null ? fatturaPassiva.getNumero().replaceAll("[^a-zA-Z0-9]", "_") : "unknown") + ".xml";

            logger.info("Export XML fattura passiva: " + fatturaPassiva.getNumero());
            return SUCCESS;

        } catch (Exception e) {
            logger.error("Errore nell'export XML fattura passiva", e);
            addActionError("Errore durante l'export: " + e.getMessage());
            return ERROR;
        }
    }

    /**
     * Import di una fattura passiva da file XML
     */
    public String importXml() {
        try {
            if (uploadFile == null) {
                addActionError("Nessun file XML caricato");
                return ERROR;
            }

            // Leggi il file XML
            java.io.FileInputStream fis = new java.io.FileInputStream(uploadFile);
            byte[] data = new byte[(int) uploadFile.length()];
            fis.read(data);
            fis.close();
            String xmlContent = new String(data, "UTF-8");

            // Parsa XML per estrarre dati fattura passiva
            FatturaPassiva fatturaImportata = fatturePassiveService.parseSingleFatturaXML(xmlContent);

            // Verifica se la fattura esiste già
            FatturaPassiva esistente = null;
            if (fatturaImportata.getSdiIdMessaggio() != null) {
                esistente = fatturaPassivaDAO.findBySdiIdMessaggio(fatturaImportata.getSdiIdMessaggio());
            }

            if (esistente != null) {
                addActionError("Fattura passiva con ID messaggio " + fatturaImportata.getSdiIdMessaggio() + " già esistente");
                return ERROR;
            }

            // Salva XML completo
            fatturaImportata.setXmlSdi(xmlContent);

            // Cerca fornitore se PIVA presente
            if (fatturaImportata.getFornitorePiva() != null && !fatturaImportata.getFornitorePiva().isEmpty()) {
                it.zensoftware.luna2.model.Fornitore fornitore = fornitoreDAO.findByPartitaIva(fatturaImportata.getFornitorePiva());
                if (fornitore != null) {
                    fatturaImportata.setFornitore(fornitore);
                }
            }

            fatturaPassivaDAO.save(fatturaImportata);

            addActionMessage("Fattura passiva " + fatturaImportata.getNumero() + " importata con successo");
            logger.info("Fattura passiva importata da XML: " + uploadFileFileName);
            id = fatturaImportata.getId();
            return SUCCESS;

        } catch (Exception e) {
            logger.error("Errore durante l'import XML fattura passiva", e);
            addActionError("Errore nell'import: " + e.getMessage());
            return ERROR;
        }
    }
    
    // Getters and Setters per upload file
    public java.io.File getUploadFile() {
        return uploadFile;
    }
    
    public void setUploadFile(java.io.File uploadFile) {
        this.uploadFile = uploadFile;
    }
    
    public String getUploadFileContentType() {
        return uploadFileContentType;
    }
    
    public void setUploadFileContentType(String uploadFileContentType) {
        this.uploadFileContentType = uploadFileContentType;
    }
    
    public String getUploadFileFileName() {
        return uploadFileFileName;
    }
    
    public void setUploadFileFileName(String uploadFileFileName) {
        this.uploadFileFileName = uploadFileFileName;
    }

    /**
     * Estrae la partita IVA aziendale da application.properties
     */
    private String getCompanyPartitaIva() {
        try {
            Properties props = new Properties();
            props.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("application.properties"));
            return props.getProperty("company.vat", "");
        } catch (Exception e) {
            logger.error("Errore nella lettura della PIVA aziendale", e);
            return null;
        }
    }
    
    // Getters and Setters
    public List<FatturaPassiva> getFatturePassive() {
        return fatturePassive;
    }
    
    public void setFatturePassive(List<FatturaPassiva> fatturePassive) {
        this.fatturePassive = fatturePassive;
    }
    
    public FatturaPassiva getFatturaPassiva() {
        return fatturaPassiva;
    }
    
    public void setFatturaPassiva(FatturaPassiva fatturaPassiva) {
        this.fatturaPassiva = fatturaPassiva;
    }
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Integer getAnno() {
        return anno;
    }
    
    public void setAnno(Integer anno) {
        this.anno = anno;
    }
    
    public String getStato() {
        return stato;
    }
    
    public void setStato(String stato) {
        this.stato = stato;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public void setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    public String getContentDisposition() {
        return contentDisposition;
    }

    public void setContentDisposition(String contentDisposition) {
        this.contentDisposition = contentDisposition;
    }
}
