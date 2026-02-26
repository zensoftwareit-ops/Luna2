package it.zensoftware.luna2.service;

import it.zensoftware.luna2.dao.*;
import it.zensoftware.luna2.model.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

/**
 * NoleggioLeadService - Business logic for rental brokerage CRM
 * Manages Phases 1-3: Preventivazione, Istruttoria, Ordine
 */
public class NoleggioLeadService {
    
    private static final Logger logger = LogManager.getLogger(NoleggioLeadService.class);
    
    private final NoleggioLeadDAO leadDAO;
    private final NolleggioPreventivoDAO preventivoDAO;
    private final NoleggioDocumentoDAO documentoDAO;
    private final NoleggioValutazioneDAO valutazioneDAO;
    private final NoleggioOrdineDAO ordineDAO;
    
    
    public NoleggioLeadService() {
        this.leadDAO = new NoleggioLeadDAO();
        this.preventivoDAO = new NolleggioPreventivoDAO();
        this.documentoDAO = new NoleggioDocumentoDAO();
        this.valutazioneDAO = new NoleggioValutazioneDAO();
        this.ordineDAO = new NoleggioOrdineDAO();
    }
    
    // ==================== FASE 1: PREVENTIVAZIONE ====================
    
    /**
     * Create new rental lead
     */
    public NoleggioLead createLead(NoleggioLead lead, Long userId) {
        lead.setFase(NoleggioLead.Fase.PREVENTIVAZIONE);
        lead.setDataCreazione(new Date());
        leadDAO.save(lead);
        logger.info("Created noleggio lead: " + getLeadLabel(lead));
        
        return lead;
    }
    
    /**
     * Create quote for lead
     */
    public NoleggioPreventivo createPreventivo(Long leadId, NoleggioPreventivo preventivo, Long userId) {
        NoleggioLead lead = leadDAO.findById(leadId);
        if (lead == null) {
            throw new IllegalArgumentException("Lead not found: " + leadId);
        }
        
        // Check latest version number
        NoleggioPreventivo latest = preventivoDAO.findLatestVersionByLead(leadId);
        int nextVersion = (latest != null && latest.getVersione() != null) ? latest.getVersione() + 1 : 1;
        
        preventivo.setNoleggioLeadId(lead.getId());
        preventivo.setVersione(nextVersion);
        preventivo.setDataElaborazione(new Date());
        preventivo.setUtenteCreazioneId(userId);
        preventivo.setNumeroPreventivo(generaNumeroPreventivo(getLeadLabel(lead), nextVersion));
        preventivo.setStatus(NoleggioPreventivo.Status.BOZZA);
        
        preventivoDAO.save(preventivo);
        logger.info("Created preventivo: " + preventivo.getNumeroPreventivo());
        
        return preventivo;
    }
    
    /**
     * Send quote to customer
     */
    public NoleggioPreventivo sendPreventivo(Long preventivoId, Long userId) {
        NoleggioPreventivo preventivo = preventivoDAO.findById(preventivoId);
        if (preventivo == null) {
            throw new IllegalArgumentException("Preventivo not found");
        }
        
        preventivo.setStatus(NoleggioPreventivo.Status.INVIATO);
        preventivo.setDataInvio(new Date());
        preventivoDAO.update(preventivo);
        
        // Mark previous versions as replaced
        List<NoleggioPreventivo> previous = preventivoDAO.findByLead(preventivo.getNoleggioLeadId());
        for (NoleggioPreventivo prev : previous) {
            if (!prev.getId().equals(preventivoId) && prev.getStatus() == NoleggioPreventivo.Status.INVIATO) {
                prev.setStatus(NoleggioPreventivo.Status.SOSTITUITO);
                preventivoDAO.update(prev);
            }
        }
        
        logger.info("Sent preventivo: " + preventivo.getNumeroPreventivo());
        return preventivo;
    }
    
    /**
     * Accept quote and advance to istruttoria phase
     */
    public NoleggioLead acceptPreventivo(Long preventivoId, Long userId) {
        NoleggioPreventivo preventivo = preventivoDAO.findById(preventivoId);
        if (preventivo == null) {
            throw new IllegalArgumentException("Preventivo not found");
        }
        
        preventivo.setStatus(NoleggioPreventivo.Status.ACCETTATO);
        preventivo.setDataRisposta(new Date());
        preventivoDAO.update(preventivo);
        
        // Advance lead to ISTRUTTORIA phase
        NoleggioLead lead = leadDAO.findById(preventivo.getNoleggioLeadId());
        lead.avanzaFase();
        leadDAO.update(lead);
        
        logger.info("Accepted preventivo, lead advanced to ISTRUTTORIA: " + getLeadLabel(lead));
        return lead;
    }
    
    // ==================== FASE 2: ISTRUTTORIA ====================
    
    /**
     * Create document checklist for lead
     */
    public NoleggioDocumento createDocumento(Long leadId, NoleggioDocumento documento, Long userId) {
        NoleggioLead lead = leadDAO.findById(leadId);
        if (lead == null) {
            throw new IllegalArgumentException("Lead not found");
        }
        
        documento.setNoleggioLeadId(lead.getId());
        documento.setStatus(NoleggioDocumento.Status.DA_RICHIEDERE);
        
        documentoDAO.save(documento);
        logger.info("Created documento: " + documento.getTipoDocumento());
        
        return documento;
    }
    
    /**
     * Mark document as requested (triggers reminder automation)
     */
    public NoleggioDocumento requestDocumento(Long documentoId, Long userId) {
        NoleggioDocumento documento = documentoDAO.findById(documentoId);
        if (documento == null) {
            throw new IllegalArgumentException("Documento not found");
        }
        
        documento.marcaRichiesto();
        documentoDAO.update(documento);
        
        logger.info("Requested documento: " + documento.getTipoDocumento());
        return documento;
    }
    
    /**
     * Upload and validate document
     */
    public NoleggioDocumento uploadDocumento(Long documentoId, String fileName, String filePath, Long fileSize, String mimeType) {
        NoleggioDocumento documento = documentoDAO.findById(documentoId);
        if (documento == null) {
            throw new IllegalArgumentException("Documento not found");
        }
        
        documento.setStatus(NoleggioDocumento.Status.RICEVUTO);
        documento.setDataRicezione(new Date());
        documento.setNomeFile(fileName);
        documento.setFilePath(filePath);
        documento.setFileSize(fileSize);
        documento.setMimeType(mimeType);
        
        documentoDAO.update(documento);
        logger.info("Uploaded documento: " + fileName);
        
        return documento;
    }
    
    /**
     * Validate document (manual approval)
     */
    public NoleggioDocumento validateDocumento(Long documentoId, Long userId) {
        NoleggioDocumento documento = documentoDAO.findById(documentoId);
        if (documento == null) {
            throw new IllegalArgumentException("Documento not found");
        }
        
        documento.setStatus(NoleggioDocumento.Status.VALIDATO);
        documento.setDataValidazione(new Date());
        documentoDAO.update(documento);
        
        // Check if all documents validated -> advance to valutazione
        Long leadId = documento.getNoleggioLeadId();
        if (documentoDAO.areAllDocumentsValidated(leadId)) {
            NoleggioLead lead = leadDAO.findById(leadId);
            if (lead.getFase() == NoleggioLead.Fase.ISTRUTTORIA) {
                logger.info("All documents validated, ready for valutazione");
            }
        }
        
        return documento;
    }
    
    /**
     * Create financial evaluation
     */
    public NoleggioValutazione createValutazione(Long leadId, NoleggioValutazione valutazione, Long userId) {
        NoleggioLead lead = leadDAO.findById(leadId);
        if (lead == null) {
            throw new IllegalArgumentException("Lead not found");
        }
        
        // Check all documents validated
        if (!documentoDAO.areAllDocumentsValidated(leadId)) {
            throw new IllegalStateException("Not all documents validated");
        }
        
        valutazione.setLead(lead);
        valutazione.setDataAvvioIstruttoria(new Date());
        valutazione.setStatus(NoleggioValutazione.Status.IN_VALUTAZIONE);
        valutazione.setDocumentiMancantiCount(0);
        
        valutazioneDAO.save(valutazione);
        logger.info("Created valutazione for lead: " + getLeadLabel(lead));
        
        return valutazione;
    }
    
    /**
     * Finalize evaluation and advance to ORDINE phase
     */
    public NoleggioLead finalizeValutazione(Long valutazioneId, boolean approved, Long userId) {
        NoleggioValutazione valutazione = valutazioneDAO.findById(valutazioneId);
        if (valutazione == null) {
            throw new IllegalArgumentException("Valutazione not found");
        }
        
        if (approved) {
            valutazione.approvaValutazione(userId, valutazione.getScoreCreditizio(), valutazione.getEsitoRischioCredito());
            valutazioneDAO.update(valutazione);
            
            // Advance lead to ORDINE phase
            NoleggioLead lead = valutazione.getLead();
            lead.avanzaFase();
            leadDAO.update(lead);
            
            logger.info("Valutazione approved, lead advanced to ORDINE: " + getLeadLabel(lead));
            return lead;
        } else {
            valutazione.rifiutaValutazione(userId, valutazione.getMotivazioneRifiuto());
            valutazioneDAO.update(valutazione);
            
            logger.info("Valutazione rejected for lead: " + getLeadLabel(valutazione.getLead()));
            return valutazione.getLead();
        }
    }
    
    // ==================== FASE 3: ORDINE ====================
    
    /**
     * Create vehicle order
     */
    public NoleggioOrdine createOrdine(Long leadId, NoleggioOrdine ordine, Long userId) {
        NoleggioLead lead = leadDAO.findById(leadId);
        if (lead == null) {
            throw new IllegalArgumentException("Lead not found");
        }
        
        if (lead.getFase() != NoleggioLead.Fase.ORDINE) {
            throw new IllegalStateException("Lead not in ORDINE phase");
        }
        
        ordine.setLead(lead);
        ordine.setDataOrdine(new Date());
        ordine.setNumeroOrdine(generaNumeroOrdine(getLeadLabel(lead)));
        ordine.setStatus(NoleggioOrdine.Status.ORDINE_CREATO);
        ordine.setNotificaEtaInviata(false);
        
        // Schedule first care call in 20 days
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, 20);
        ordine.setProssimoCareCallPrevisto(cal.getTime());
        
        ordineDAO.save(ordine);
        logger.info("Created ordine: " + ordine.getNumeroOrdine());
        
        return ordine;
    }
    
    /**
     * Update order status with care call tracking
     */
    public NoleggioOrdine updateOrdineStatus(Long ordineId, NoleggioOrdine.Status newStatus, Long userId) {
        NoleggioOrdine ordine = ordineDAO.findById(ordineId);
        if (ordine == null) {
            throw new IllegalArgumentException("Ordine not found");
        }
        
        ordine.setStatus(newStatus);
        ordineDAO.update(ordine);
        
        // If delivered, advance lead to POST_VENDITA
        if (newStatus == NoleggioOrdine.Status.CONSEGNATO) {
            ordine.setDataConsegnaEffettiva(new Date());
            NoleggioLead lead = ordine.getLead();
            lead.avanzaFase();
            leadDAO.update(lead);
            
            logger.info("Order delivered, lead advanced to POST_VENDITA: " + getLeadLabel(lead));
        }
        
        return ordine;
    }
    
    /**
     * Register care call execution
     */
    public NoleggioOrdine registerCareCall(Long ordineId, String note, Long userId) {
        NoleggioOrdine ordine = ordineDAO.findById(ordineId);
        if (ordine == null) {
            throw new IllegalArgumentException("Ordine not found");
        }
        
        ordine.registraCareCallEffettuato();
        String currentNote = ordine.getNoteInterne() != null ? ordine.getNoteInterne() + "\n" : "";
        ordine.setNoteInterne(currentNote + "[Care Call " + new Date() + "] " + note);
        ordineDAO.update(ordine);
        
        logger.info("Registered care call for ordine: " + ordine.getNumeroOrdine());
        return ordine;
    }
    
    // ==================== UTILITIES ====================
    
    private String getLeadLabel(NoleggioLead lead) {
        if (lead == null) {
            return "NL-UNKNOWN";
        }
        Long id = lead.getId() != null ? lead.getId() : lead.getLeadId();
        return "NL-" + (id != null ? id : "UNKNOWN");
    }
    
    private String generaNumeroPreventivo(String codiceNoleggio, int versione) {
        return codiceNoleggio + "-PV" + String.format("%02d", versione);
    }
    
    private String generaNumeroOrdine(String codiceNoleggio) {
        return codiceNoleggio + "-ORD";
    }
}
