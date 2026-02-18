package it.zensoftware.luna2.action;

import com.opensymphony.xwork2.ActionSupport;
import it.zensoftware.luna2.dao.PreventivoDAO;
import it.zensoftware.luna2.dao.PreventivoRigaDAO;
import it.zensoftware.luna2.dao.ClienteDAO;
import it.zensoftware.luna2.dao.ProdottoDAO;
import it.zensoftware.luna2.model.Preventivo;
import it.zensoftware.luna2.model.PreventivoRiga;
import it.zensoftware.luna2.model.Cliente;
import it.zensoftware.luna2.model.Prodotto;
import it.zensoftware.luna2.model.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class PreventiviAction extends ActionSupport {
    private static final Logger logger = LogManager.getLogger(PreventiviAction.class);
    private PreventivoDAO preventivoDAO = new PreventivoDAO();
    private PreventivoRigaDAO preventivoRigaDAO = new PreventivoRigaDAO();
    private ClienteDAO clienteDAO = new ClienteDAO();
    private ProdottoDAO prodottoDAO = new ProdottoDAO();
    
    private Preventivo preventivo;
    private List<Preventivo> preventivi;
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
        
        return SUCCESS;
    }

    public String create() {
        preventivo = new Preventivo();
        preventivo.setDataPreventivo(new Date());
        preventivo.setAnno(Calendar.getInstance().get(Calendar.YEAR));
        
        // Generate next numero
        String nextNumero = preventivoDAO.getNextNumero(preventivo.getAnno());
        preventivo.setNumero(nextNumero);
        
        // Load clienti for selection
        clienti = clienteDAO.findAllActive();
        return SUCCESS;
    }

    public String edit() {
        if (id != null) {
            preventivo = preventivoDAO.findWithRighe(id);
            if (preventivo != null) {
                righe = preventivo.getRighe();
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
        
        try {
            if (preventivo.getId() == null) {
                preventivo.setCreatedBy(currentUser);
                preventivo.setStato(Preventivo.Stato.BOZZA);
                preventivoDAO.save(preventivo);
                addActionMessage("Preventivo creato con successo");
            } else {
                preventivo.setModifiedBy(currentUser);
                preventivoDAO.update(preventivo);
                
                // Ricalcola totali dopo salvataggio
                preventivoDAO.ricalcolaTotali(preventivo);
                
                addActionMessage("Preventivo aggiornato con successo");
            }
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error saving preventivo", e);
            addActionError("Errore durante il salvataggio del preventivo");
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
                    Document document = new Document(PageSize.A4, 50, 50, 50, 50);
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    PdfWriter.getInstance(document, baos);
                    document.open();
                    
                    // Header con dati azienda
                    Paragraph header = new Paragraph();
                    header.add(new Chunk("PREVENTIVO", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24)));
                    header.setAlignment(Element.ALIGN_CENTER);
                    document.add(header);
                    
                    document.add(new Paragraph(" "));
                    
                    // Numero e data preventivo
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
                    
                    // Tabella righe
                    PdfPTable table = new PdfPTable(5);
                    table.setWidthPercentage(100);
                    table.setWidths(new float[]{30, 15, 15, 20, 20});
                    
                    // Header tabella
                    String[] headers = {"Prodotto", "Quantità", "Prezzo", "Importo", ""};
                    for (String headerText : headers) {
                        PdfPCell cell = new PdfPCell(new Phrase(headerText, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));
                        cell.setBackgroundColor(new BaseColor(220, 220, 220));
                        cell.setPadding(5);
                        table.addCell(cell);
                    }
                    
                    // Righe preventivo
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
                            table.addCell(new PdfPCell(new Phrase("", FontFactory.getFont(FontFactory.HELVETICA, 9))));
                        }
                    }
                    document.add(table);
                    document.add(new Paragraph(" "));
                    
                    // Totali
                    PdfPTable totalsTable = new PdfPTable(2);
                    totalsTable.setWidthPercentage(50);
                    totalsTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    totalsTable.setWidths(new float[]{60, 40});
                    
                    // Imponibile
                    PdfPCell labelCell = new PdfPCell(new Phrase("Imponibile:", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11)));
                    labelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    totalsTable.addCell(labelCell);
                    PdfPCell valueCell = new PdfPCell(new Phrase("€ " + String.format("%.2f", preventivo.getImponibile()), FontFactory.getFont(FontFactory.HELVETICA, 11)));
                    valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    totalsTable.addCell(valueCell);
                    
                    // IVA
                    labelCell = new PdfPCell(new Phrase("IVA (22%):", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11)));
                    labelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    totalsTable.addCell(labelCell);
                    valueCell = new PdfPCell(new Phrase("€ " + String.format("%.2f", preventivo.getIva()), FontFactory.getFont(FontFactory.HELVETICA, 11)));
                    valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    totalsTable.addCell(valueCell);
                    
                    // Totale
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
                    
                    // Stato
                    Paragraph statoParagraph = new Paragraph();
                    statoParagraph.add(new Chunk("Stato: ", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11)));
                    statoParagraph.add(new Chunk(preventivo.getStato().toString(), FontFactory.getFont(FontFactory.HELVETICA, 11)));
                    document.add(statoParagraph);
                    
                    document.close();
                    
                    byte[] pdfBytes = baos.toByteArray();
                    inputStream = new ByteArrayInputStream(pdfBytes);
                    contentDisposition = "attachment;filename=" + preventivo.getNumero() + ".pdf";
                    
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

    private User getCurrentUser() {
        Map<String, Object> session = com.opensymphony.xwork2.ActionContext.getContext().getSession();
        return (User) session.get("currentUser");
    }

    // Getters and Setters
    public Preventivo getPreventivo() { return preventivo; }
    public void setPreventivo(Preventivo preventivo) { this.preventivo = preventivo; }
    public List<Preventivo> getPreventivi() { return preventivi; }
    public List<Cliente> getClienti() { return clienti; }
    public List<Prodotto> getProdotti() { return prodotti; }
    public List<PreventivoRiga> getRighe() { return righe; }
    public void setRighe(List<PreventivoRiga> righe) { this.righe = righe; }
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
}
