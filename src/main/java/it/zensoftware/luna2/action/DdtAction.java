package it.zensoftware.luna2.action;

import com.opensymphony.xwork2.ActionSupport;
import it.zensoftware.luna2.dao.ClienteDAO;
import it.zensoftware.luna2.dao.DdtDAO;
import it.zensoftware.luna2.model.Cliente;
import it.zensoftware.luna2.model.User;

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

    private List<DdtDAO.DdtListItem> ddtList;
    private List<Cliente> clienti;

    private Long id;
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

    public List<DdtDAO.DdtListItem> getDdtList() { return ddtList; }
    public List<Cliente> getClienti() { return clienti; }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
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
}
