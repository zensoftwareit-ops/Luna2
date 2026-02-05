package it.zensoftware.luna2.action;

import com.opensymphony.xwork2.ActionSupport;
import it.zensoftware.luna2.dao.PreventivoDAO;
import it.zensoftware.luna2.dao.ClienteDAO;
import it.zensoftware.luna2.model.Preventivo;
import it.zensoftware.luna2.model.Cliente;
import it.zensoftware.luna2.model.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class PreventiviAction extends ActionSupport {
    private static final Logger logger = LogManager.getLogger(PreventiviAction.class);
    private PreventivoDAO preventivoDAO = new PreventivoDAO();
    private ClienteDAO clienteDAO = new ClienteDAO();
    private Preventivo preventivo;
    private List<Preventivo> preventivi;
    private List<Cliente> clienti;
    private Long id;

    public String list() {
        int anno = Calendar.getInstance().get(Calendar.YEAR);
        preventivi = preventivoDAO.findByAnno(anno);
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
            preventivo = preventivoDAO.findById(id);
            clienti = clienteDAO.findAllActive();
        }
        return SUCCESS;
    }

    public String save() {
        User currentUser = getCurrentUser();
        
        if (preventivo.getId() == null) {
            preventivo.setCreatedBy(currentUser);
            preventivoDAO.save(preventivo);
            addActionMessage("Preventivo creato con successo");
        } else {
            preventivo.setModifiedBy(currentUser);
            preventivoDAO.update(preventivo);
            addActionMessage("Preventivo aggiornato con successo");
        }
        return SUCCESS;
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
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
}
