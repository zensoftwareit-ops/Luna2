package it.zensoftware.luna2.action;

import com.opensymphony.xwork2.ActionSupport;
import it.zensoftware.luna2.dao.ClienteDAO;
import it.zensoftware.luna2.dao.DdtDAO;
import it.zensoftware.luna2.dao.ProdottoDAO;
import it.zensoftware.luna2.model.Cliente;
import it.zensoftware.luna2.model.Prodotto;
import it.zensoftware.luna2.model.User;

import com.itextpdf.text.Document;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

/**
 * DDT Action
 */
public class DdtAction extends ActionSupport {

    private static final long serialVersionUID = 1L;

    private final DdtDAO ddtDAO = new DdtDAO();
    private final ClienteDAO clienteDAO = new ClienteDAO();
    private final ProdottoDAO prodottoDAO = new ProdottoDAO();

    private List<DdtDAO.DdtListItem> ddtList;
    private List<Cliente> clienti;
    private DdtDAO.DdtHeader ddt;
    private List<DdtDAO.DdtRigaItem> righe;
    private List<Prodotto> prodotti;

    private Long id;
    private Long rigaId;
    private Integer anno;
    private String searchTerm;

    private Long clienteId;
    private String dataDdt;
    private String causaleTrasporto;
    private String aspettoBeni;
    private Integer numeroColli;
    private String trasportatore;
    private String indirizzoDestinazione;
    private String note;

    private Long prodottoId;
    private String descrizioneRiga;
    private BigDecimal quantita;
    private BigDecimal prezzoUnitario;
    private BigDecimal ivaPercentuale;

    private InputStream inputStream;
    private String contentDisposition;

    public String list() {
        if (anno == null) {
            anno = Calendar.getInstance().get(Calendar.YEAR);
        }

        ddtList = ddtDAO.findAll(anno, searchTerm);
        clienti = clienteDAO.findAll();
        return SUCCESS;
    }

    public String save() {
        if (clienteId == null) {
            addActionError("Seleziona un cliente");
            return list();
        }

        if (dataDdt == null || dataDdt.trim().isEmpty()) {
            addActionError("Inserisci la data DDT");
            return list();
        }

        LocalDate parsedDate;
        try {
            parsedDate = LocalDate.parse(dataDdt);
        } catch (DateTimeParseException e) {
            addActionError("Formato data non valido");
            return list();
        }

        int annoDdt = parsedDate.getYear();

        try {
            String numero = ddtDAO.generateNumero(annoDdt);
            ddtDAO.insert(
                numero,
                annoDdt,
                Date.valueOf(parsedDate),
                clienteId,
                emptyToNull(causaleTrasporto),
                emptyToNull(aspettoBeni),
                numeroColli,
                emptyToNull(trasportatore),
                emptyToNull(indirizzoDestinazione),
                emptyToNull(note),
                getCurrentUserId()
            );

            addActionMessage("DDT " + numero + " creato con successo");
            anno = annoDdt;
            clearForm();
            return list();
        } catch (Exception e) {
            addActionError("Errore durante il salvataggio DDT: " + e.getMessage());
            return list();
        }
    }

    public String delete() {
        if (id == null) {
            addActionError("ID DDT mancante");
            return list();
        }

        try {
            ddtDAO.delete(id);
            addActionMessage("DDT eliminato con successo");
        } catch (Exception e) {
            addActionError("Errore durante l'eliminazione DDT: " + e.getMessage());
        }

        return list();
    }

    public String view() {
        if (id == null) {
            addActionError("ID DDT mancante");
            return list();
        }

        ddt = ddtDAO.findHeaderById(id);
        if (ddt == null) {
            addActionError("DDT non trovato");
            return list();
        }

        righe = ddtDAO.findRigheByDdtId(id);
        prodotti = prodottoDAO.findAllActive();
        return "view";
    }

    public String addRiga() {
        if (id == null) {
            addActionError("ID DDT mancante");
            return list();
        }
        if (prodottoId == null) {
            addActionError("Seleziona un prodotto");
            return view();
        }
        if (quantita == null || quantita.compareTo(BigDecimal.ZERO) <= 0) {
            addActionError("Quantita non valida");
            return view();
        }
        if (prezzoUnitario == null || prezzoUnitario.compareTo(BigDecimal.ZERO) < 0) {
            addActionError("Prezzo unitario non valido");
            return view();
        }
        if (ivaPercentuale == null || ivaPercentuale.compareTo(BigDecimal.ZERO) < 0) {
            ivaPercentuale = new BigDecimal("22.00");
        }

        try {
            if (descrizioneRiga == null || descrizioneRiga.trim().isEmpty()) {
                Prodotto p = prodottoDAO.findById(prodottoId);
                descrizioneRiga = p != null ? p.getNome() : "Riga prodotto";
            }

            ddtDAO.addRiga(id, prodottoId, descrizioneRiga.trim(), quantita, prezzoUnitario, ivaPercentuale);
            addActionMessage("Riga aggiunta al DDT");
            clearRigaForm();
            return view();
        } catch (Exception e) {
            addActionError("Errore aggiunta riga: " + e.getMessage());
            return view();
        }
    }

    public String deleteRiga() {
        if (id == null || rigaId == null) {
            addActionError("ID riga o DDT mancante");
            return view();
        }
        try {
            ddtDAO.deleteRiga(rigaId, id);
            addActionMessage("Riga eliminata");
        } catch (Exception e) {
            addActionError("Errore eliminazione riga: " + e.getMessage());
        }
        return view();
    }

    public String generatePdf() {
        if (id == null) {
            addActionError("ID DDT mancante");
            return ERROR;
        }

        try {
            DdtDAO.DdtHeader header = ddtDAO.findHeaderById(id);
            List<DdtDAO.DdtRigaItem> rows = ddtDAO.findRigheByDdtId(id);
            if (header == null) {
                addActionError("DDT non trovato");
                return ERROR;
            }

            Document document = new Document(PageSize.A4);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, baos);
            document.open();

            document.add(new Paragraph("Documento di Trasporto"));
            document.add(new Paragraph("Numero: " + header.getNumero()));
            document.add(new Paragraph("Data: " + header.getDataDdt()));
            document.add(new Paragraph("Cliente: " + header.getClienteRagioneSociale()));
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.addCell("#");
            table.addCell("Descrizione");
            table.addCell("Quantita");
            table.addCell("Prezzo");

            int idx = 1;
            for (DdtDAO.DdtRigaItem r : rows) {
                table.addCell(String.valueOf(idx++));
                table.addCell(r.getDescrizione() != null ? r.getDescrizione() : "");
                table.addCell(r.getQuantita() != null ? r.getQuantita().toPlainString() : "0");
                table.addCell(r.getPrezzoUnitario() != null ? r.getPrezzoUnitario().toPlainString() : "0.00");
            }

            document.add(table);
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Imponibile: " + header.getImponibile()));
            document.add(new Paragraph("IVA: " + header.getIva()));
            document.add(new Paragraph("Totale: " + header.getTotale()));
            document.close();

            inputStream = new ByteArrayInputStream(baos.toByteArray());
            contentDisposition = "attachment; filename=\"DDT_" + header.getNumero() + ".pdf\"";
            return SUCCESS;
        } catch (Exception e) {
            addActionError("Errore generazione PDF: " + e.getMessage());
            return ERROR;
        }
    }

    public String convertToFattura() {
        if (id == null) {
            addActionError("ID DDT mancante");
            return list();
        }

        try {
            Long fatturaId = ddtDAO.convertToFattura(id, getCurrentUserId());
            addActionMessage("DDT convertito in fattura con successo");
            id = fatturaId;
            return "redirect-fattura";
        } catch (Exception e) {
            addActionError("Errore conversione in fattura: " + e.getMessage());
            return view();
        }
    }

    private Long getCurrentUserId() {
        Map<String, Object> session = com.opensymphony.xwork2.ActionContext.getContext().getSession();
        User user = (User) session.get("currentUser");
        if (user == null) {
            user = (User) session.get("user");
        }
        return user != null ? user.getId() : null;
    }

    private String emptyToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void clearForm() {
        id = null;
        clienteId = null;
        dataDdt = null;
        causaleTrasporto = null;
        aspettoBeni = null;
        numeroColli = null;
        trasportatore = null;
        indirizzoDestinazione = null;
        note = null;
    }

    private void clearRigaForm() {
        prodottoId = null;
        descrizioneRiga = null;
        quantita = null;
        prezzoUnitario = null;
        ivaPercentuale = null;
    }

    public List<DdtDAO.DdtListItem> getDdtList() { return ddtList; }
    public List<Cliente> getClienti() { return clienti; }
    public DdtDAO.DdtHeader getDdt() { return ddt; }
    public List<DdtDAO.DdtRigaItem> getRighe() { return righe; }
    public List<Prodotto> getProdotti() { return prodotti; }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getRigaId() { return rigaId; }
    public void setRigaId(Long rigaId) { this.rigaId = rigaId; }
    public Integer getAnno() { return anno; }
    public void setAnno(Integer anno) { this.anno = anno; }
    public String getSearchTerm() { return searchTerm; }
    public void setSearchTerm(String searchTerm) { this.searchTerm = searchTerm; }
    public Long getClienteId() { return clienteId; }
    public void setClienteId(Long clienteId) { this.clienteId = clienteId; }
    public String getDataDdt() { return dataDdt; }
    public void setDataDdt(String dataDdt) { this.dataDdt = dataDdt; }
    public String getCausaleTrasporto() { return causaleTrasporto; }
    public void setCausaleTrasporto(String causaleTrasporto) { this.causaleTrasporto = causaleTrasporto; }
    public String getAspettoBeni() { return aspettoBeni; }
    public void setAspettoBeni(String aspettoBeni) { this.aspettoBeni = aspettoBeni; }
    public Integer getNumeroColli() { return numeroColli; }
    public void setNumeroColli(Integer numeroColli) { this.numeroColli = numeroColli; }
    public String getTrasportatore() { return trasportatore; }
    public void setTrasportatore(String trasportatore) { this.trasportatore = trasportatore; }
    public String getIndirizzoDestinazione() { return indirizzoDestinazione; }
    public void setIndirizzoDestinazione(String indirizzoDestinazione) { this.indirizzoDestinazione = indirizzoDestinazione; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public Long getProdottoId() { return prodottoId; }
    public void setProdottoId(Long prodottoId) { this.prodottoId = prodottoId; }
    public String getDescrizioneRiga() { return descrizioneRiga; }
    public void setDescrizioneRiga(String descrizioneRiga) { this.descrizioneRiga = descrizioneRiga; }
    public BigDecimal getQuantita() { return quantita; }
    public void setQuantita(BigDecimal quantita) { this.quantita = quantita; }
    public BigDecimal getPrezzoUnitario() { return prezzoUnitario; }
    public void setPrezzoUnitario(BigDecimal prezzoUnitario) { this.prezzoUnitario = prezzoUnitario; }
    public BigDecimal getIvaPercentuale() { return ivaPercentuale; }
    public void setIvaPercentuale(BigDecimal ivaPercentuale) { this.ivaPercentuale = ivaPercentuale; }
    public InputStream getInputStream() { return inputStream; }
    public String getContentDisposition() { return contentDisposition; }
}
