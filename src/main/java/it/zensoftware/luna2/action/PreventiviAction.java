package it.zensoftware.luna2.action;

import com.opensymphony.xwork2.ActionSupport;
import it.zensoftware.luna2.dao.PreventivoDAO;
import it.zensoftware.luna2.dao.PreventivoRigaDAO;
import it.zensoftware.luna2.dao.ClienteDAO;
import it.zensoftware.luna2.dao.ProdottoDAO;
import it.zensoftware.luna2.dao.TrackingEmailDAO;
import it.zensoftware.luna2.dao.ModuleSettingDAO;
import it.zensoftware.luna2.dao.AccountingProfileDAO;
import it.zensoftware.luna2.dto.PreventivoTrackingDTO;
import it.zensoftware.luna2.model.Preventivo;
import it.zensoftware.luna2.model.PreventivoRiga;
import it.zensoftware.luna2.model.Cliente;
import it.zensoftware.luna2.model.Prodotto;
import it.zensoftware.luna2.model.User;
import it.zensoftware.luna2.model.TrackingEmail;
import it.zensoftware.luna2.model.Commessa;
import it.zensoftware.luna2.model.ModuleSetting;
import it.zensoftware.luna2.model.AccountingProfile;
import it.zensoftware.luna2.service.EmailService;
import it.zensoftware.luna2.service.CommesseService;
import it.zensoftware.luna2.service.notification.EventPublisher;
import it.zensoftware.luna2.service.notification.event.NotificationEventFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.struts2.ServletActionContext;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Properties;

public class PreventiviAction extends ActionSupport {
    private static final Logger logger = LogManager.getLogger(PreventiviAction.class);
    private PreventivoDAO preventivoDAO = new PreventivoDAO();
    private PreventivoRigaDAO preventivoRigaDAO = new PreventivoRigaDAO();
    private ClienteDAO clienteDAO = new ClienteDAO();
    private ProdottoDAO prodottoDAO = new ProdottoDAO();
    private AccountingProfileDAO profileDAO = new AccountingProfileDAO();
    private TrackingEmailDAO trackingEmailDAO = new TrackingEmailDAO();
    private ModuleSettingDAO moduleSettingDAO = new ModuleSettingDAO();
    private EmailService emailService = new EmailService();
    private CommesseService commesseService = new CommesseService();
    
    private Preventivo preventivo;
    private List<Preventivo> preventivi;
    private List<PreventivoTrackingDTO> preventiviConTracking;
    private List<Cliente> clienti;
    private List<Prodotto> prodotti;
    private List<PreventivoRiga> righe;
    private PreventivoRiga riga;
    
    private Long id;
    private Long rigaId;
    private Long prodottoId;
    private Integer anno;
    private Preventivo.Stato stato;
    private Long clienteId;
    private InputStream inputStream;
    private String contentDisposition;
    private String tipo;  // "tecnico" o "descrittivo"
    private String emailDestinatario;
    private String trackingId;
    private String messageEmail;
    private String dataPreventivoStr; // usato per il binding della data dal form HTML
    private Integer righeCount;

    public String list() {
        if (anno == null) {
            anno = Calendar.getInstance().get(Calendar.YEAR);
        }
        
        if (stato != null) {
            preventivi = preventivoDAO.findByStato(stato);
        } else if (clienteId != null) {
            preventivi = preventivoDAO.findByClienteId(clienteId);
        } else {
            preventivi = preventivoDAO.findByAnno(anno);
        }
        
        // Populate tracking data for each preventivo
        preventiviConTracking = new ArrayList<>();
        for (Preventivo p : preventivi) {
            Long totalEmails = trackingEmailDAO.countEmailsForPreventivo(p.getId());
            Long openedEmails = trackingEmailDAO.countOpensForPreventivo(p.getId());
            Long downloadedEmails = trackingEmailDAO.countDownloadsForPreventivo(p.getId());
            Long totalDownloads = trackingEmailDAO.getTotalDownloadCount(p.getId());
            
            preventiviConTracking.add(new PreventivoTrackingDTO(p, totalEmails, openedEmails, downloadedEmails, totalDownloads));
        }
        
        return SUCCESS;
    }

    public String create() {
        preventivo = new Preventivo();
        preventivo.setDataPreventivo(new Date());
        preventivo.setAnno(Calendar.getInstance().get(Calendar.YEAR));
        dataPreventivoStr = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        
        // Generate next numero
        String nextNumero = preventivoDAO.getNextNumero(preventivo.getAnno());
        preventivo.setNumero(nextNumero);
        
        // Load clienti for selection
        clienti = clienteDAO.findAllActive();
        prodotti = prodottoDAO.findAllActive();
        return SUCCESS;
    }

    public String edit() {
        if (id != null) {
            preventivo = preventivoDAO.findWithRighe(id);
            if (preventivo != null) {
                righe = preventivo.getRighe();
                if (preventivo.getDataPreventivo() != null) {
                    dataPreventivoStr = new SimpleDateFormat("yyyy-MM-dd").format(preventivo.getDataPreventivo());
                }
            }
            clienti = clienteDAO.findAllActive();
            prodotti = prodottoDAO.findAllActive();
        }
        return SUCCESS;
    }

    public String view() {
        if (id != null) {
            preventivo = preventivoDAO.findWithRighe(id);
            if (preventivo != null) {
                righe = preventivo.getRighe();
            }
        }
        return SUCCESS;
    }

    public String save() {
        User currentUser = getCurrentUser();
        
        // Parsa la data dal formato HTML yyyy-MM-dd
        Date dataParsed = null;
        if (dataPreventivoStr != null && !dataPreventivoStr.isEmpty()) {
            try {
                dataParsed = new SimpleDateFormat("yyyy-MM-dd").parse(dataPreventivoStr);
            } catch (java.text.ParseException e) {
                logger.warn("Formato data non valido: {}", dataPreventivoStr);
            }
        }
        
        try {
            if (preventivo.getId() == null) {
                // === NUOVO PREVENTIVO ===
                if (dataParsed != null) preventivo.setDataPreventivo(dataParsed);
                preventivo.setCreatedBy(currentUser);
                preventivo.setStato(Preventivo.Stato.BOZZA);
                if (preventivo.getAnno() == null) {
                    preventivo.setAnno(Calendar.getInstance().get(Calendar.YEAR));
                }
                preventivoDAO.save(preventivo);
                addActionMessage("Preventivo creato con successo");
            } else {
                // === AGGIORNAMENTO ESISTENTE ===
                // Carica entity dal DB (lazy collections non inizializzate)
                // per evitare che orphanRemoval su 'commesse' le cancelli
                Preventivo existing = preventivoDAO.findById(preventivo.getId());
                if (existing == null) {
                    addActionError("Preventivo non trovato");
                    return ERROR;
                }
                // Copia solo i campi editabili dal form
                if (dataParsed != null) existing.setDataPreventivo(dataParsed);
                existing.setCliente(preventivo.getCliente());
                existing.setOggetto(preventivo.getOggetto());
                existing.setStato(preventivo.getStato());
                existing.setValiditaGiorni(preventivo.getValiditaGiorni());
                existing.setNoteIntestazione(preventivo.getNoteIntestazione());
                existing.setNotePiede(preventivo.getNotePiede());
                existing.setCondizioniPagamento(preventivo.getCondizioniPagamento());
                existing.setTempiConsegna(preventivo.getTempiConsegna());
                existing.setScontoPercentuale(preventivo.getScontoPercentuale());
                existing.setSpeseTrasporto(preventivo.getSpeseTrasporto());
                existing.setModifiedBy(currentUser);
                preventivoDAO.update(existing);
                preventivo = existing;

                // Elimina righe vecchie solo se il form ha inviato esplicitamente il conteggio.
                // In questo modo evitiamo perdita dati se il binding delle righe non arriva al server.
                if (righeCount != null) {
                    preventivoRigaDAO.deleteByPreventivoId(preventivo.getId());
                } else {
                    logger.warn("Salvataggio preventivo {} senza righeCount: mantengo le righe esistenti", preventivo.getId());
                }
                addActionMessage("Preventivo aggiornato con successo");
            }
            
            // Salva le righe dal form
            if (righe != null && !righe.isEmpty()) {
                for (int i = 0; i < righe.size(); i++) {
                    PreventivoRiga riga = righe.get(i);
                    if (riga != null && riga.getDescrizione() != null && !riga.getDescrizione().isEmpty()) {
                        riga.setPreventivo(preventivo);
                        if (riga.getTipoRiga() == null) {
                            riga.setTipoRiga(PreventivoRiga.TipoRiga.PRODOTTO);
                        }
                        if (riga.getRigaNumero() == null || riga.getRigaNumero() == 0) {
                            riga.setRigaNumero(i + 1);
                        }

                        if (riga.getQuantita() == null) {
                            riga.setQuantita(BigDecimal.ONE);
                        }
                        if (riga.getPrezzoUnitario() == null) {
                            riga.setPrezzoUnitario(BigDecimal.ZERO);
                        }
                        if (riga.getScontoPercentuale() == null) {
                            riga.setScontoPercentuale(BigDecimal.ZERO);
                        }
                        if (riga.getIvaPercentuale() == null) {
                            riga.setIvaPercentuale(new BigDecimal("22.00"));
                        }

                        if (riga.getProdotto() != null && riga.getProdotto().getId() != null) {
                            Prodotto prodottoPersistente = prodottoDAO.findById(riga.getProdotto().getId());
                            riga.setProdotto(prodottoPersistente);
                        } else {
                            riga.setProdotto(null);
                        }

                        preventivoRigaDAO.save(riga);
                    }
                }
            }
            
            // Ricalcola totali dopo salvataggio
            preventivoDAO.ricalcolaTotali(preventivo);
            
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error saving preventivo: {}", e.getMessage(), e);
            clienti = clienteDAO.findAllActive();
            prodotti = prodottoDAO.findAllActive();
            addActionError("Errore durante il salvataggio: " + e.getMessage());
            return ERROR;
        }
    }

    public String delete() {
        if (id != null) {
            try {
                Preventivo preventivo = preventivoDAO.findById(id);
                if (preventivo != null) {
                    // Delete righe first
                    preventivoRigaDAO.deleteByPreventivoId(id);
                    // Then delete preventivo
                    preventivoDAO.delete(preventivo);
                    addActionMessage("Preventivo eliminato con successo");
                }
            } catch (Exception e) {
                logger.error("Error deleting preventivo", e);
                addActionError("Errore durante l'eliminazione del preventivo");
            }
        }
        return SUCCESS;
    }

    public String duplica() {
        if (id != null) {
            try {
                Preventivo original = preventivoDAO.findWithRighe(id);
                if (original != null) {
                    Preventivo duplicate = new Preventivo();
                    
                    // Copy fields
                    duplicate.setCliente(original.getCliente());
                    duplicate.setLead(original.getLead());
                    duplicate.setOggetto("Copia di: " + original.getOggetto());
                    duplicate.setNoteIntestazione(original.getNoteIntestazione());
                    duplicate.setNotePiede(original.getNotePiede());
                    duplicate.setCondizioniPagamento(original.getCondizioniPagamento());
                    duplicate.setTempiConsegna(original.getTempiConsegna());
                    duplicate.setValiditaGiorni(original.getValiditaGiorni());
                    duplicate.setScontoPercentuale(original.getScontoPercentuale());
                    duplicate.setScontoImporto(original.getScontoImporto());
                    duplicate.setSpeseTrasporto(original.getSpeseTrasporto());
                    
                    // Set new numero and data
                    duplicate.setAnno(Calendar.getInstance().get(Calendar.YEAR));
                    duplicate.setNumero(preventivoDAO.getNextNumero(duplicate.getAnno()));
                    duplicate.setDataPreventivo(new Date());
                    duplicate.setStato(Preventivo.Stato.BOZZA);
                    duplicate.setCreatedBy(getCurrentUser());
                    
                    preventivoDAO.save(duplicate);
                    
                    // Copy righe
                    if (original.getRighe() != null) {
                        for (PreventivoRiga originalRiga : original.getRighe()) {
                            PreventivoRiga duplicateRiga = new PreventivoRiga();
                            duplicateRiga.setPreventivo(duplicate);
                            duplicateRiga.setRigaNumero(originalRiga.getRigaNumero());
                            duplicateRiga.setTipoRiga(originalRiga.getTipoRiga());
                            duplicateRiga.setProdotto(originalRiga.getProdotto());
                            duplicateRiga.setDescrizione(originalRiga.getDescrizione());
                            duplicateRiga.setQuantita(originalRiga.getQuantita());
                            duplicateRiga.setUnitaMisura(originalRiga.getUnitaMisura());
                            duplicateRiga.setPrezzoUnitario(originalRiga.getPrezzoUnitario());
                            duplicateRiga.setScontoPercentuale(originalRiga.getScontoPercentuale());
                            duplicateRiga.setScontoImporto(originalRiga.getScontoImporto());
                            duplicateRiga.setIvaPercentuale(originalRiga.getIvaPercentuale());
                            duplicateRiga.setNote(originalRiga.getNote());
                            preventivoRigaDAO.save(duplicateRiga);
                        }
                    }
                    
                    // Ricalcola totali
                    preventivoDAO.ricalcolaTotali(duplicate);
                    
                    id = duplicate.getId();
                    addActionMessage("Preventivo duplicato con successo");
                    return SUCCESS;
                }
            } catch (Exception e) {
                logger.error("Error duplicating preventivo", e);
                addActionError("Errore durante la duplicazione del preventivo");
            }
        }
        return ERROR;
    }

    // ===== RIGHE MANAGEMENT =====

    public String addRiga() {
        if (id != null && riga != null) {
            try {
                preventivo = preventivoDAO.findById(id);
                riga.setPreventivo(preventivo);
                
                // Get next riga numero
                Integer nextNumero = preventivoRigaDAO.getNextRigaNumero(id);
                riga.setRigaNumero(nextNumero);
                
                // If prodotto selected, copy data
                if (prodottoId != null) {
                    Prodotto prodotto = prodottoDAO.findById(prodottoId);
                    if (prodotto != null) {
                        riga.setProdotto(prodotto);
                        if (riga.getDescrizione() == null || riga.getDescrizione().isEmpty()) {
                            riga.setDescrizione(prodotto.getNome());
                        }
                        if (riga.getPrezzoUnitario() == null || riga.getPrezzoUnitario().compareTo(BigDecimal.ZERO) == 0) {
                            riga.setPrezzoUnitario(prodotto.getPrezzoBase());
                        }
                        if (riga.getUnitaMisura() == null || riga.getUnitaMisura().isEmpty()) {
                            riga.setUnitaMisura(prodotto.getUnitaMisura().name());
                        }
                    }
                }
                
                preventivoRigaDAO.save(riga);
                
                // Ricalcola totali
                preventivoDAO.ricalcolaTotali(preventivo);
                
                addActionMessage("Riga aggiunta con successo");
                return SUCCESS;
            } catch (Exception e) {
                logger.error("Error adding riga", e);
                addActionError("Errore durante l'aggiunta della riga");
                return ERROR;
            }
        }
        return ERROR;
    }

    public String updateRiga() {
        if (rigaId != null && riga != null) {
            try {
                PreventivoRiga existing = preventivoRigaDAO.findById(rigaId);
                if (existing != null) {
                    existing.setDescrizione(riga.getDescrizione());
                    existing.setQuantita(riga.getQuantita());
                    existing.setPrezzoUnitario(riga.getPrezzoUnitario());
                    existing.setScontoPercentuale(riga.getScontoPercentuale());
                    existing.setScontoImporto(riga.getScontoImporto());
                    existing.setIvaPercentuale(riga.getIvaPercentuale());
                    existing.setNote(riga.getNote());
                    
                    preventivoRigaDAO.update(existing);
                    
                    // Ricalcola totali
                    preventivoDAO.ricalcolaTotali(existing.getPreventivo());
                    
                    addActionMessage("Riga aggiornata con successo");
                    return SUCCESS;
                }
            } catch (Exception e) {
                logger.error("Error updating riga", e);
                addActionError("Errore durante l'aggiornamento della riga");
                return ERROR;
            }
        }
        return ERROR;
    }

    public String deleteRiga() {
        if (rigaId != null) {
            try {
                PreventivoRiga riga = preventivoRigaDAO.findById(rigaId);
                if (riga != null) {
                    Preventivo preventivo = riga.getPreventivo();
                    preventivoRigaDAO.delete(riga);
                    
                    // Ricalcola totali
                    preventivoDAO.ricalcolaTotali(preventivo);
                    
                    addActionMessage("Riga eliminata con successo");
                    return SUCCESS;
                }
            } catch (Exception e) {
                logger.error("Error deleting riga", e);
                addActionError("Errore durante l'eliminazione della riga");
                return ERROR;
            }
        }
        return ERROR;
    }

    public String loadRighe() {
        if (id != null) {
            righe = preventivoRigaDAO.findByPreventivoId(id);
            return SUCCESS;
        }
        return ERROR;
    }

    // ===== WORKFLOW METHODS =====

    public String invia() {
        if (id != null) {
            try {
                preventivo = preventivoDAO.findById(id);
                if (preventivo != null && preventivo.getStato() == Preventivo.Stato.BOZZA) {
                    preventivo.setStato(Preventivo.Stato.INVIATO);
                    preventivo.setModifiedBy(getCurrentUser());
                    preventivoDAO.update(preventivo);
                    addActionMessage("Preventivo inviato con successo");
                    return SUCCESS;
                } else {
                    addActionError("Il preventivo non può essere inviato");
                }
            } catch (Exception e) {
                logger.error("Error sending preventivo", e);
                addActionError("Errore durante l'invio del preventivo");
            }
        }
        return ERROR;
    }

    public String accetta() {
        if (id != null) {
            try {
                preventivo = preventivoDAO.findById(id);
                if (preventivo != null && preventivo.getStato() == Preventivo.Stato.INVIATO) {
                    preventivo.setStato(Preventivo.Stato.ACCETTATO);
                    preventivo.setModifiedBy(getCurrentUser());
                    preventivoDAO.update(preventivo);
                    addActionMessage("Preventivo accettato con successo");
                    return SUCCESS;
                } else {
                    addActionError("Il preventivo non può essere accettato");
                }
            } catch (Exception e) {
                logger.error("Error accepting preventivo", e);
                addActionError("Errore durante l'accettazione del preventivo");
            }
        }
        return ERROR;
    }

    public String rifiuta() {
        if (id != null) {
            try {
                preventivo = preventivoDAO.findById(id);
                if (preventivo != null && preventivo.getStato() == Preventivo.Stato.INVIATO) {
                    preventivo.setStato(Preventivo.Stato.RIFIUTATO);
                    preventivo.setModifiedBy(getCurrentUser());
                    preventivoDAO.update(preventivo);
                    addActionMessage("Preventivo rifiutato");
                    return SUCCESS;
                } else {
                    addActionError("Il preventivo non può essere rifiutato");
                }
            } catch (Exception e) {
                logger.error("Error rejecting preventivo", e);
                addActionError("Errore durante il rifiuto del preventivo");
            }
        }
        return ERROR;
    }

    /**
     * Trasforma il preventivo accettato in una commessa
     * (solo se modulo produzione è abilitato)
     */
    public String trasformaInCommessa() {
        if (id == null) {
            addActionError("ID preventivo non specificato");
            return ERROR;
        }

        // Verifica che il modulo produzione sia abilitato
        if (!isProduzioneEnabled()) {
            addActionError("Il modulo Produzione/Commesse non è abilitato");
            return ERROR;
        }

        try {
            preventivo = preventivoDAO.findById(id);
            
            if (preventivo == null) {
                addActionError("Preventivo non trovato");
                return ERROR;
            }

            // Verifica che il preventivo sia ACCETTATO
            if (preventivo.getStato() != Preventivo.Stato.ACCETTATO) {
                addActionError("Solo i preventivi ACCETTATI possono essere trasformati in commesse");
                return ERROR;
            }

            // Crea la commessa tramite il service
            Commessa commessa = commesseService.creaCommessaDaPreventivo(id);
            
            if (commessa != null) {
                addActionMessage("Commessa " + commessa.getNumero() + " creata con successo dal preventivo " + preventivo.getNumero());
                // Redirect alla vista della commessa
                id = commessa.getId();
                return "redirect-commessa";
            } else {
                addActionError("Errore durante la creazione della commessa");
                return ERROR;
            }
        } catch (IllegalStateException e) {
            logger.warn("Cannot create commessa from preventivo {}: {}", id, e.getMessage());
            addActionError(e.getMessage());
            return ERROR;
        } catch (Exception e) {
            logger.error("Error creating commessa from preventivo", e);
            addActionError("Errore durante la trasformazione in commessa: " + e.getMessage());
            return ERROR;
        }
    }

    /**
     * Verifica se il modulo Produzione è abilitato
     */
    public boolean isProduzioneEnabled() {
        try {
            ModuleSetting setting = moduleSettingDAO.findByCode("PRODUZIONE");
            return setting != null && Boolean.TRUE.equals(setting.getEnabled());
        } catch (Exception e) {
            logger.error("Error checking produzione module status", e);
            return false;
        }
    }

    public String ricalcolaTotali() {
        if (id != null) {
            try {
                preventivo = preventivoDAO.findById(id);
                if (preventivo != null) {
                    preventivoDAO.ricalcolaTotali(preventivo);
                    addActionMessage("Totali ricalcolati con successo");
                    return SUCCESS;
                }
            } catch (Exception e) {
                logger.error("Error recalculating totals", e);
                addActionError("Errore durante il ricalcolo dei totali");
            }
        }
        return ERROR;
    }

    public String generatePdf() {
        try {
            if (id != null) {
                preventivo = preventivoDAO.findWithRighe(id);
                if (preventivo != null) {
                    if (tipo == null || tipo.isEmpty()) {
                        tipo = "tecnico";
                    }
                    
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    
                    if ("descrittivo".equalsIgnoreCase(tipo)) {
                        generaPdfDescrittivo(baos);
                    } else {
                        generaPdfTecnico(baos);
                    }
                    
                    byte[] pdfBytes = baos.toByteArray();
                    inputStream = new ByteArrayInputStream(pdfBytes);
                    contentDisposition = "attachment;filename=" + preventivo.getNumero() + "_" + tipo + ".pdf";
                    
                    return SUCCESS;
                }
            }
            addActionError("Preventivo non trovato");
            return ERROR;
        } catch (Exception e) {
            logger.error("Error generating PDF", e);
            addActionError("Errore nella generazione del PDF: " + e.getMessage());
            return ERROR;
        }
    }

    private void generaPdfTecnico(ByteArrayOutputStream baos) throws Exception {
        Document document = new Document(PageSize.A4, 50, 50, 50, 50);
        PdfWriter.getInstance(document, baos);
        document.open();
        
        // Add logo from company profile
        AccountingProfile profilo = profileDAO.getDefaultProfile();
        if (profilo != null && profilo.getLogoPath() != null && !profilo.getLogoPath().isEmpty()) {
            try {
                String logoPath = ServletActionContext.getServletContext().getRealPath(profilo.getLogoPath());
                Image logo = Image.getInstance(logoPath);
                logo.scaleToFit(100, 100);
                logo.setAlignment(Image.ALIGN_LEFT);
                document.add(logo);
                document.add(new Paragraph(" "));
            } catch (Exception e) {
                logger.warn("Errore durante l'inserimento del logo nel PDF", e);
            }
        }
        
        // Header
        Paragraph header = new Paragraph();
        header.add(new Chunk("PREVENTIVO", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24)));
        header.setAlignment(Element.ALIGN_CENTER);
        document.add(header);
        document.add(new Paragraph(" "));
        
        // Info preventivo
        Paragraph infoPreventivo = new Paragraph();
        infoPreventivo.add(new Chunk("Numero: ", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11)));
        infoPreventivo.add(new Chunk(preventivo.getNumero() + "\n", FontFactory.getFont(FontFactory.HELVETICA, 11)));
        infoPreventivo.add(new Chunk("Data: ", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11)));
        infoPreventivo.add(new Chunk(new SimpleDateFormat("dd/MM/yyyy").format(preventivo.getDataPreventivo()) + "\n", FontFactory.getFont(FontFactory.HELVETICA, 11)));
        if (preventivo.getDataValidita() != null) {
            infoPreventivo.add(new Chunk("Validità: ", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11)));
            infoPreventivo.add(new Chunk(new SimpleDateFormat("dd/MM/yyyy").format(preventivo.getDataValidita()), FontFactory.getFont(FontFactory.HELVETICA, 11)));
        }
        document.add(infoPreventivo);
        document.add(new Paragraph(" "));
        
        // Cliente
        if (preventivo.getCliente() != null) {
            Paragraph clienteInfo = new Paragraph();
            clienteInfo.add(new Chunk("Destinatario:\n", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11)));
            clienteInfo.add(new Chunk(preventivo.getCliente().getRagioneSociale() + "\n", FontFactory.getFont(FontFactory.HELVETICA, 11)));
            if (preventivo.getCliente().getIndirizzo() != null) {
                clienteInfo.add(new Chunk(preventivo.getCliente().getIndirizzo() + "\n", FontFactory.getFont(FontFactory.HELVETICA, 10)));
            }
            if (preventivo.getCliente().getCitta() != null) {
                clienteInfo.add(new Chunk(preventivo.getCliente().getCitta(), FontFactory.getFont(FontFactory.HELVETICA, 10)));
            }
            document.add(clienteInfo);
        }
        document.add(new Paragraph(" "));
        
        // Tabella righe compatta
        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{30, 15, 15, 20, 5});
        
        String[] headers = {"Prodotto", "Quantità", "Prezzo", "Importo", ""};
        for (String headerText : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(headerText, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));
            cell.setBackgroundColor(new BaseColor(220, 220, 220));
            cell.setPadding(5);
            table.addCell(cell);
        }
        
        if (preventivo.getRighe() != null) {
            for (PreventivoRiga riga : preventivo.getRighe()) {
                if (riga.getProdotto() != null) {
                    table.addCell(new PdfPCell(new Phrase(riga.getProdotto().getNome(), FontFactory.getFont(FontFactory.HELVETICA, 9))));
                } else {
                    table.addCell(new PdfPCell(new Phrase("-", FontFactory.getFont(FontFactory.HELVETICA, 9))));
                }
                table.addCell(new PdfPCell(new Phrase(riga.getQuantita().toString(), FontFactory.getFont(FontFactory.HELVETICA, 9))));
                table.addCell(new PdfPCell(new Phrase("€ " + String.format("%.2f", riga.getPrezzoUnitario()), FontFactory.getFont(FontFactory.HELVETICA, 9))));
                BigDecimal importo = riga.getQuantita().multiply(riga.getPrezzoUnitario());
                table.addCell(new PdfPCell(new Phrase("€ " + String.format("%.2f", importo), FontFactory.getFont(FontFactory.HELVETICA, 9))));
                table.addCell(new PdfPCell(new Phrase(" ", FontFactory.getFont(FontFactory.HELVETICA, 9))));
            }
        }
        document.add(table);
        document.add(new Paragraph(" "));
        
        addTotalsSectionToPdf(document);
        document.close();
    }

    private void generaPdfDescrittivo(ByteArrayOutputStream baos) throws Exception {
        Document document = new Document(PageSize.A4, 50, 50, 50, 50);
        PdfWriter.getInstance(document, baos);
        document.open();
        
        // Add logo from company profile
        AccountingProfile profilo = profileDAO.getDefaultProfile();
        if (profilo != null && profilo.getLogoPath() != null && !profilo.getLogoPath().isEmpty()) {
            try {
                String logoPath = ServletActionContext.getServletContext().getRealPath(profilo.getLogoPath());
                Image logo = Image.getInstance(logoPath);
                logo.scaleToFit(100, 100);
                logo.setAlignment(Image.ALIGN_LEFT);
                document.add(logo);
                document.add(new Paragraph(" "));
            } catch (Exception e) {
                logger.warn("Errore durante l'inserimento del logo nel PDF", e);
            }
        }
        
        // Header
        Paragraph header = new Paragraph();
        header.add(new Chunk("PREVENTIVO", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24)));
        header.setAlignment(Element.ALIGN_CENTER);
        document.add(header);
        document.add(new Paragraph(" "));
        
        // Info preventivo
        Paragraph infoPreventivo = new Paragraph();
        infoPreventivo.add(new Chunk("Numero: ", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11)));
        infoPreventivo.add(new Chunk(preventivo.getNumero() + "\n", FontFactory.getFont(FontFactory.HELVETICA, 11)));
        infoPreventivo.add(new Chunk("Data: ", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11)));
        infoPreventivo.add(new Chunk(new SimpleDateFormat("dd/MM/yyyy").format(preventivo.getDataPreventivo()) + "\n", FontFactory.getFont(FontFactory.HELVETICA, 11)));
        if (preventivo.getDataValidita() != null) {
            infoPreventivo.add(new Chunk("Validità: ", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11)));
            infoPreventivo.add(new Chunk(new SimpleDateFormat("dd/MM/yyyy").format(preventivo.getDataValidita()), FontFactory.getFont(FontFactory.HELVETICA, 11)));
        }
        document.add(infoPreventivo);
        document.add(new Paragraph(" "));
        
        // Cliente
        if (preventivo.getCliente() != null) {
            Paragraph clienteInfo = new Paragraph();
            clienteInfo.add(new Chunk("Destinatario:\n", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11)));
            clienteInfo.add(new Chunk(preventivo.getCliente().getRagioneSociale() + "\n", FontFactory.getFont(FontFactory.HELVETICA, 11)));
            if (preventivo.getCliente().getIndirizzo() != null) {
                clienteInfo.add(new Chunk(preventivo.getCliente().getIndirizzo() + "\n", FontFactory.getFont(FontFactory.HELVETICA, 10)));
            }
            if (preventivo.getCliente().getCitta() != null) {
                clienteInfo.add(new Chunk(preventivo.getCliente().getCitta(), FontFactory.getFont(FontFactory.HELVETICA, 10)));
            }
            document.add(clienteInfo);
        }
        document.add(new Paragraph(" "));
        
        // Righe descrittive - una per riga con spazio per descrizioni
        document.add(new Paragraph("ARTICOLI", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12)));
        document.add(new Paragraph(" "));
        
        if (preventivo.getRighe() != null && !preventivo.getRighe().isEmpty()) {
            for (int i = 0; i < preventivo.getRighe().size(); i++) {
                PreventivoRiga riga = preventivo.getRighe().get(i);
                
                // Info riga
                Paragraph rigaInfo = new Paragraph();
                rigaInfo.add(new Chunk((i + 1) + ". ", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11)));
                if (riga.getProdotto() != null) {
                    rigaInfo.add(new Chunk(riga.getProdotto().getNome(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11)));
                    rigaInfo.add(new Chunk("\n", FontFactory.getFont(FontFactory.HELVETICA, 10)));
                    if (riga.getProdotto().getDescrizione() != null && !riga.getProdotto().getDescrizione().isEmpty()) {
                        rigaInfo.add(new Chunk(riga.getProdotto().getDescrizione() + "\n", FontFactory.getFont(FontFactory.HELVETICA, 9)));
                    }
                }
                document.add(rigaInfo);
                
                // Dettagli prezzo
                PdfPTable detailsTable = new PdfPTable(4);
                detailsTable.setWidthPercentage(100);
                detailsTable.setWidths(new float[]{30, 20, 25, 25});
                
                PdfPCell cell = new PdfPCell(new Phrase("Quantità: " + riga.getQuantita(), FontFactory.getFont(FontFactory.HELVETICA, 10)));
                cell.setBorder(Rectangle.NO_BORDER);
                detailsTable.addCell(cell);
                
                cell = new PdfPCell(new Phrase("Prezzo: € " + String.format("%.2f", riga.getPrezzoUnitario()), FontFactory.getFont(FontFactory.HELVETICA, 10)));
                cell.setBorder(Rectangle.NO_BORDER);
                detailsTable.addCell(cell);
                
                BigDecimal importo = riga.getQuantita().multiply(riga.getPrezzoUnitario());
                cell = new PdfPCell(new Phrase("", FontFactory.getFont(FontFactory.HELVETICA, 10)));
                cell.setBorder(Rectangle.NO_BORDER);
                detailsTable.addCell(cell);
                
                cell = new PdfPCell(new Phrase("Importo: € " + String.format("%.2f", importo), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11)));
                cell.setBorder(Rectangle.NO_BORDER);
                cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                detailsTable.addCell(cell);
                
                document.add(detailsTable);
                document.add(new Paragraph(" "));
            }
        }
        
        addTotalsSectionToPdf(document);
        document.close();
    }

    private void addTotalsSectionToPdf(Document document) throws DocumentException {
        document.add(new Paragraph(" "));
        
        PdfPTable totalsTable = new PdfPTable(2);
        totalsTable.setWidthPercentage(50);
        totalsTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalsTable.setWidths(new float[]{60, 40});
        
        PdfPCell labelCell = new PdfPCell(new Phrase("Imponibile:", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11)));
        labelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalsTable.addCell(labelCell);
        PdfPCell valueCell = new PdfPCell(new Phrase("€ " + String.format("%.2f", preventivo.getImponibile()), FontFactory.getFont(FontFactory.HELVETICA, 11)));
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalsTable.addCell(valueCell);
        
        labelCell = new PdfPCell(new Phrase("IVA (22%):", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11)));
        labelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalsTable.addCell(labelCell);
        valueCell = new PdfPCell(new Phrase("€ " + String.format("%.2f", preventivo.getIva()), FontFactory.getFont(FontFactory.HELVETICA, 11)));
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalsTable.addCell(valueCell);
        
        labelCell = new PdfPCell(new Phrase("TOTALE:", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14)));
        labelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        labelCell.setBackgroundColor(new BaseColor(240, 240, 240));
        totalsTable.addCell(labelCell);
        valueCell = new PdfPCell(new Phrase("€ " + String.format("%.2f", preventivo.getTotale()), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14)));
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        valueCell.setBackgroundColor(new BaseColor(240, 240, 240));
        totalsTable.addCell(valueCell);
        
        document.add(totalsTable);
        document.add(new Paragraph(" "));
        
        Paragraph statoParagraph = new Paragraph();
        statoParagraph.add(new Chunk("Stato: ", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11)));
        statoParagraph.add(new Chunk(preventivo.getStato().toString(), FontFactory.getFont(FontFactory.HELVETICA, 11)));
        document.add(statoParagraph);
    }

    public String sendEmail() {
        try {
            if (id == null || emailDestinatario == null || emailDestinatario.isEmpty()) {
                addActionError("Preventivo e email ricevente sono obbligatori");
                return INPUT;
            }

            preventivo = preventivoDAO.findWithRighe(id);
            if (preventivo == null) {
                addActionError("Preventivo non trovato");
                return ERROR;
            }

            // Leggi configurazione SMTP dalle properties
            Properties props = new Properties();
            try (java.io.InputStream is = this.getClass().getClassLoader().getResourceAsStream("application.properties")) {
                props.load(is);
            }

            String smtpHost = props.getProperty("smtp.host", "smtp.gmail.com");
            String smtpUsername = props.getProperty("smtp.username");
            String smtpPassword = props.getProperty("smtp.password");
            String smtpFromEmail = props.getProperty("smtp.username");

            if (smtpUsername == null || smtpPassword == null) {
                addActionError("Configurazione SMTP incompleta. Verifica application.properties");
                logger.error("SMTP configuration missing in application.properties");
                return ERROR;
            }

            String emailBodyMessage = messageEmail != null ? messageEmail : 
                "Allega il preventivo numero " + preventivo.getNumero() + " per la review.";

            // Invia email con tracciamento
            TrackingEmail tracking = emailService.sendPreventiveEmail(preventivo, emailDestinatario, 
                    emailBodyMessage, smtpUsername, smtpPassword, smtpFromEmail);

            preventivo.setStato(Preventivo.Stato.INVIATO);
            preventivoDAO.update(preventivo);

            addActionMessage("Email inviata con successo a " + emailDestinatario);
            logger.info("Email inviata per preventivo " + preventivo.getNumero() + " a " + emailDestinatario);
            return SUCCESS;

        } catch (Exception e) {
            logger.error("Errore durante l'invio email", e);
            addActionError("Errore durante l'invio: " + e.getMessage());
            return ERROR;
        }
    }

    public String trackPixel() {
        try {
            if (trackingId == null || trackingId.isEmpty()) {
                logger.warn("Track pixel called without tracking ID");
                return ERROR;
            }

            String userAgent = ServletActionContext.getRequest().getHeader("User-Agent");
            emailService.trackPixelOpen(trackingId, userAgent);

            TrackingEmail tracking = trackingEmailDAO.findByTrackingId(trackingId);
            if (tracking != null && tracking.getPreventivo() != null) {
                Preventivo trackedPreventivo = tracking.getPreventivo();
                String ownerUserId = resolveOwnerUserId(trackedPreventivo);
                if (ownerUserId != null) {
                    EventPublisher.getInstance().publishEvent(
                            NotificationEventFactory.preventivoAperto(
                                    ownerUserId,
                                    trackedPreventivo.getNumero(),
                                    trackedPreventivo.getCliente() != null
                                            ? trackedPreventivo.getCliente().getRagioneSociale()
                                            : "Cliente",
                                    LocalDateTime.now()
                            )
                    );
                }
            }

            // Return 1x1 transparent GIF
            byte[] gifBytes = {
                0x47, 0x49, 0x46, 0x38, (byte) 0x39, 0x61, 0x01, 0x00, 0x01, 0x00, (byte) 0x80,
                0x00, 0x00, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, 0x00, 0x00, 0x00, 0x21, (byte) 0xF9,
                0x04, 0x01, 0x00, 0x00, 0x00, 0x00, 0x2C, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x01,
                0x00, 0x00, 0x02, 0x02, 0x44, 0x01, 0x00, 0x3B
            };

            inputStream = new ByteArrayInputStream(gifBytes);
            contentDisposition = "inline; filename=\"pixel.gif\"";
            return SUCCESS;

        } catch (Exception e) {
            logger.error("Errore nel tracking pixel", e);
            return ERROR;
        }
    }

    public String downloadWithTracking() {
        try {
            if (trackingId == null || trackingId.isEmpty()) {
                logger.warn("Download called without tracking ID");
                return ERROR;
            }

            TrackingEmail tracking = trackingEmailDAO.findByTrackingId(trackingId);
            if (tracking == null) {
                logger.warn("Tracking record not found for ID: " + trackingId);
                return ERROR;
            }

            Long preventivoId = tracking.getPreventivo().getId();
            preventivo = preventivoDAO.findWithRighe(preventivoId);

            if (preventivo == null) {
                addActionError("Preventivo non trovato");
                return ERROR;
            }

            // Traccia il download
            String userAgent = ServletActionContext.getRequest().getHeader("User-Agent");
            emailService.trackDownload(trackingId, userAgent);

                String ownerUserId = resolveOwnerUserId(preventivo);
                if (ownerUserId != null) {
                EventPublisher.getInstance().publishEvent(
                    NotificationEventFactory.preventivoLetto(
                        ownerUserId,
                        preventivo.getNumero(),
                        preventivo.getCliente() != null
                            ? preventivo.getCliente().getRagioneSociale()
                            : "Cliente",
                        30
                    )
                );
                }

            // Determina il tipo di layout
            String layoutType = tipo != null ? tipo : "tecnico";

            // Genera PDF
            Document document = new Document(PageSize.A4);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, baos);
            document.open();

            if ("descrittivo".equals(layoutType)) {
                generaPdfDescrittivo(document);
            } else {
                generaPdfTecnico(document);
            }

            document.close();

            inputStream = new ByteArrayInputStream(baos.toByteArray());
            contentDisposition = "attachment; filename=\"Preventivo_" + preventivo.getNumero() + ".pdf\"";
            return SUCCESS;

        } catch (Exception e) {
            logger.error("Errore nel download con tracciamento", e);
            addActionError("Errore durante il download: " + e.getMessage());
            return ERROR;
        }
    }

    private void generaPdfDescrittivo(Document document) throws DocumentException {
        // Implementation from existing method
        document.add(new Paragraph(new Chunk("PREVENTIVO", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20))));
        document.add(new Paragraph(" "));

        PdfPTable infoTable = new PdfPTable(2);
        infoTable.setWidthPercentage(100);
        infoTable.setWidths(new float[]{50, 50});

        addTableCell(infoTable, "Numero:", preventivo.getNumero());
        addTableCell(infoTable, "Data:", new SimpleDateFormat("dd/MM/yyyy").format(preventivo.getDataPreventivo()));
        addTableCell(infoTable, "Cliente:", preventivo.getCliente() != null ? preventivo.getCliente().getRagioneSociale() : "");
        addTableCell(infoTable, "Validità:", preventivo.getValiditaGiorni() != null ? preventivo.getValiditaGiorni().toString() + " giorni" : "");
        document.add(infoTable);
        document.add(new Paragraph(" "));

        if (righe != null && !righe.isEmpty()) {
            for (PreventivoRiga riga : righe) {
                String nomeProdotto = riga.getProdotto() != null ? riga.getProdotto().getNome() : riga.getDescrizione();
                Paragraph p = new Paragraph();
                p.add(new Chunk(nomeProdotto, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12)));
                document.add(p);

                if (riga.getProdotto() != null && riga.getProdotto().getDescrizione() != null && !riga.getProdotto().getDescrizione().isEmpty()) {
                    Paragraph desc = new Paragraph(riga.getProdotto().getDescrizione(), FontFactory.getFont(FontFactory.HELVETICA, 10));
                    document.add(desc);
                }

                PdfPTable rigaTable = new PdfPTable(4);
                rigaTable.setWidthPercentage(80);
                rigaTable.setWidths(new float[]{30, 20, 25, 25});

                addTableHeaderCell(rigaTable, "Quantità");
                addTableHeaderCell(rigaTable, "Unitá Misura");
                addTableHeaderCell(rigaTable, "Prezzo Unitario");
                addTableHeaderCell(rigaTable, "Importo");

                addTableCell(rigaTable, riga.getQuantita().toString());
                addTableCell(rigaTable, riga.getUnitaMisura() != null ? riga.getUnitaMisura() : "");
                addTableCell(rigaTable, "€ " + String.format("%.2f", riga.getPrezzoUnitario()));
                addTableCell(rigaTable, "€ " + String.format("%.2f", riga.getTotaleRiga()));

                document.add(rigaTable);
                document.add(new Paragraph(" "));
            }
        }

        addTotalsSectionToPdf(document);
    }

    private void generaPdfTecnico(Document document) throws DocumentException {
        // Implementation from existing method
        document.add(new Paragraph(new Chunk("PREVENTIVO", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20))));
        document.add(new Paragraph(" "));

        PdfPTable infoTable = new PdfPTable(2);
        infoTable.setWidthPercentage(100);
        infoTable.setWidths(new float[]{50, 50});

        addTableCell(infoTable, "Numero:", preventivo.getNumero());
        addTableCell(infoTable, "Data:", new SimpleDateFormat("dd/MM/yyyy").format(preventivo.getDataPreventivo()));
        addTableCell(infoTable, "Cliente:", preventivo.getCliente() != null ? preventivo.getCliente().getRagioneSociale() : "");
        addTableCell(infoTable, "Validità:", preventivo.getValiditaGiorni() != null ? preventivo.getValiditaGiorni().toString() + " giorni" : "");
        document.add(infoTable);
        document.add(new Paragraph(" "));

        if (righe != null && !righe.isEmpty()) {
            PdfPTable articoliTable = new PdfPTable(4);
            articoliTable.setWidthPercentage(100);
            articoliTable.setWidths(new float[]{40, 20, 20, 20});

            addTableHeaderCell(articoliTable, "Prodotto");
            addTableHeaderCell(articoliTable, "Quantità");
            addTableHeaderCell(articoliTable, "Prezzo");
            addTableHeaderCell(articoliTable, "Importo");

            for (PreventivoRiga riga : righe) {
                String nomeProdotto = riga.getProdotto() != null ? riga.getProdotto().getNome() : riga.getDescrizione();
                addTableCell(articoliTable, nomeProdotto);
                addTableCell(articoliTable, riga.getQuantita().toString());
                addTableCell(articoliTable, "€ " + String.format("%.2f", riga.getPrezzoUnitario()));
                addTableCell(articoliTable, "€ " + String.format("%.2f", riga.getTotaleRiga()));
            }

            document.add(articoliTable);
            document.add(new Paragraph(" "));
        }

        addTotalsSectionToPdf(document);
    }

    private void addTableCell(PdfPTable table, String label, String value) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));
        labelCell.setBackgroundColor(new BaseColor(200, 200, 200));
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, FontFactory.getFont(FontFactory.HELVETICA, 10)));
        table.addCell(valueCell);
    }

    private void addTableCell(PdfPTable table, String value) {
        PdfPCell cell = new PdfPCell(new Phrase(value, FontFactory.getFont(FontFactory.HELVETICA, 10)));
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(cell);
    }

    private void addTableHeaderCell(PdfPTable table, String header) {
        PdfPCell cell = new PdfPCell(new Phrase(header, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11)));
        cell.setBackgroundColor(new BaseColor(100, 100, 100));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }

    private User getCurrentUser() {
        Map<String, Object> session = com.opensymphony.xwork2.ActionContext.getContext().getSession();
        return (User) session.get("currentUser");
    }

    private String resolveOwnerUserId(Preventivo trackedPreventivo) {
        if (trackedPreventivo.getCreatedBy() != null) {
            return String.valueOf(trackedPreventivo.getCreatedBy().getId());
        }
        if (trackedPreventivo.getModifiedBy() != null) {
            return String.valueOf(trackedPreventivo.getModifiedBy().getId());
        }
        User currentUser = getCurrentUser();
        return currentUser != null ? String.valueOf(currentUser.getId()) : null;
    }

    // Getters and Setters
    public Preventivo getPreventivo() { return preventivo; }
    public void setPreventivo(Preventivo preventivo) { this.preventivo = preventivo; }
    public List<Preventivo> getPreventivi() { return preventivi; }
    public List<PreventivoTrackingDTO> getPreventiviConTracking() { return preventiviConTracking; }
    public List<Cliente> getClienti() { return clienti; }
    public List<Prodotto> getProdotti() { return prodotti; }
    public List<PreventivoRiga> getRighe() { return righe; }
    public void setRighe(List<PreventivoRiga> righe) { this.righe = righe; }
    public Preventivo.Stato[] getStatiPreventivo() { return Preventivo.Stato.values(); }
    public PreventivoRiga getRiga() { return riga; }
    public void setRiga(PreventivoRiga riga) { this.riga = riga; }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getRigaId() { return rigaId; }
    public void setRigaId(Long rigaId) { this.rigaId = rigaId; }
    public Long getProdottoId() { return prodottoId; }
    public void setProdottoId(Long prodottoId) { this.prodottoId = prodottoId; }
    public Integer getAnno() { return anno; }
    public void setAnno(Integer anno) { this.anno = anno; }
    public Preventivo.Stato getStato() { return stato; }
    public void setStato(Preventivo.Stato stato) { this.stato = stato; }
    public Long getClienteId() { return clienteId; }
    public void setClienteId(Long clienteId) { this.clienteId = clienteId; }
    public InputStream getInputStream() { return inputStream; }
    public String getContentDisposition() { return contentDisposition; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getEmailDestinatario() { return emailDestinatario; }
    public void setEmailDestinatario(String emailDestinatario) { this.emailDestinatario = emailDestinatario; }
    public String getTrackingId() { return trackingId; }
    public void setTrackingId(String trackingId) { this.trackingId = trackingId; }
    public String getMessageEmail() { return messageEmail; }
    public void setMessageEmail(String messageEmail) { this.messageEmail = messageEmail; }
    public String getDataPreventivoStr() { return dataPreventivoStr; }
    public void setDataPreventivoStr(String dataPreventivoStr) { this.dataPreventivoStr = dataPreventivoStr; }
    public Integer getRigheCount() { return righeCount; }
    public void setRigheCount(Integer righeCount) { this.righeCount = righeCount; }}