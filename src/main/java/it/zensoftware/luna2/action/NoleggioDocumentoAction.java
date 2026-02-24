package it.zensoftware.luna2.action;

import com.opensymphony.xwork2.ActionSupport;
import it.zensoftware.luna2.dao.*;
import it.zensoftware.luna2.model.*;
import it.zensoftware.luna2.service.NoleggioLeadService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

/**
 * NoleggioDocumentoAction - Document management in istruttoria phase
 */
public class NoleggioDocumentoAction extends ActionSupport {
    
    private static final Logger logger = LogManager.getLogger(NoleggioDocumentoAction.class);
    private static final long serialVersionUID = 1L;

    private NoleggioDocumentoDAO documentoDAO;
    private NoleggioLeadDAO leadDAO;
    private NoleggioLeadService leadService;
    
    private NoleggioDocumento documento;
    private List<NoleggioDocumento> documenti;
    private NoleggioLead lead;
    
    private Long id;
    private Long leadId;
    private String fileName;
    private String filePath;
    private Long fileSize;
    private String mimeType;
    private String tipoDocumentoFiltro;
    private Map<String, Object> jsonResponse = new HashMap<>();
    
    public NoleggioDocumentoAction() {
        this.documentoDAO = new NoleggioDocumentoDAO();
        this.leadDAO = new NoleggioLeadDAO();
        this.leadService = new NoleggioLeadService();
    }
    
    public String list() {
        try {
            if (leadId != null) {
                documenti = documentoDAO.findByLead(leadId);
                lead = leadDAO.findById(leadId);
            } else {
                documenti = documentoDAO.findAll();
            }
            
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error listing documenti", e);
            addActionError("Errore nel caricamento dei documenti");
            return ERROR;
        }
    }
    
    public String create() {
        try {
            if (leadId != null) {
                lead = leadDAO.findById(leadId);
            }
            documento = new NoleggioDocumento();
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error creating documento", e);
            return ERROR;
        }
    }
    
    public String save() {
        try {
            if (leadId == null) {
                addActionError("Lead non trovato");
                return ERROR;
            }
            
            User currentUser = getCurrentUser();
            
            if (documento.getId() == null) {
                documento = leadService.createDocumento(leadId, documento, 
                                                       currentUser != null ? currentUser.getId() : 1L);
                addActionMessage("Documento creato");
            } else {
                documentoDAO.update(documento);
                addActionMessage("Documento aggiornato");
            }
            
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error saving documento", e);
            addActionError("Errore nel salvataggio");
            return INPUT;
        }
    }
    
    public String requestDocument() {
        try {
            if (id == null) {
                jsonResponse.put("success", false);
                return SUCCESS;
            }
            
            documento = leadService.requestDocumento(id, 
                                                     getCurrentUser() != null ? getCurrentUser().getId() : 1L);
            
            jsonResponse.put("success", true);
            jsonResponse.put("status", documento.getStatus().toString());
            
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error requesting documento", e);
            jsonResponse.put("success", false);
            jsonResponse.put("error", e.getMessage());
            return SUCCESS;
        }
    }
    
    public String uploadDocument() {
        try {
            if (id == null) {
                jsonResponse.put("success", false);
                return SUCCESS;
            }
            
            documento = leadService.uploadDocumento(id, fileName, filePath, fileSize, mimeType);
            
            jsonResponse.put("success", true);
            jsonResponse.put("fileName", documento.getNomeFile());
            
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error uploading documento", e);
            jsonResponse.put("success", false);
            jsonResponse.put("error", e.getMessage());
            return SUCCESS;
        }
    }
    
    public String validateDocument() {
        try {
            if (id == null) {
                jsonResponse.put("success", false);
                return SUCCESS;
            }
            
            documento = leadService.validateDocumento(id, 
                                                      getCurrentUser() != null ? getCurrentUser().getId() : 1L);
            
            // Check if all documents validated
            long missing = documentoDAO.countMissingByLead(documento.getLead().getId());
            boolean allValidated = (missing == 0);
            
            jsonResponse.put("success", true);
            jsonResponse.put("status", documento.getStatus().toString());
            jsonResponse.put("allValidated", allValidated);
            
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error validating documento", e);
            jsonResponse.put("success", false);
            jsonResponse.put("error", e.getMessage());
            return SUCCESS;
        }
    }
    
    public String delete() {
        try {
            if (id == null) {
                addActionError("ID non valido");
                return ERROR;
            }
            
            NoleggioDocumento d = documentoDAO.findById(id);
            if (d != null) {
                documentoDAO.delete(d);
                addActionMessage("Documento eliminato");
            }
            
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error deleting documento", e);
            addActionError("Errore nell'eliminazione");
            return ERROR;
        }
    }
    
    public String documentiMancanti() {
        try {
            if (leadId == null) {
                jsonResponse.put("count", 0);
                return SUCCESS;
            }
            
            long missing = documentoDAO.countMissingByLead(leadId);
            jsonResponse.put("count", missing);
            jsonResponse.put("leadId", leadId);
            
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error getting missing documenti", e);
            return ERROR;
        }
    }
    
    // ============== GETTERS/SETTERS ==============
    
    public NoleggioDocumento getDocumento() { return documento; }
    public void setDocumento(NoleggioDocumento documento) { this.documento = documento; }
    
    public List<NoleggioDocumento> getDocumenti() { return documenti; }
    public void setDocumenti(List<NoleggioDocumento> documenti) { this.documenti = documenti; }
    
    public NoleggioLead getLead() { return lead; }
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getLeadId() { return leadId; }
    public void setLeadId(Long leadId) { this.leadId = leadId; }
    
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    
    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }
    
    public Map<String, Object> getJsonResponse() { return jsonResponse; }
    
    private User getCurrentUser() {
        Map<String, Object> session = com.opensymphony.xwork2.ActionContext.getContext().getSession();
        return (User) session.get("currentUser");
    }
}
