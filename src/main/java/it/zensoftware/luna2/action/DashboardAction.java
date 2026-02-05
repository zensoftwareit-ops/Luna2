package it.zensoftware.luna2.action;

import com.opensymphony.xwork2.ActionSupport;
import it.zensoftware.luna2.dao.PreventivoDAO;
import it.zensoftware.luna2.dao.ClienteDAO;
import it.zensoftware.luna2.model.Preventivo;
import it.zensoftware.luna2.model.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

/**
 * Dashboard Action
 */
public class DashboardAction extends ActionSupport {
    
    private static final Logger logger = LogManager.getLogger(DashboardAction.class);
    private static final long serialVersionUID = 1L;

    private PreventivoDAO preventivoDAO = new PreventivoDAO();
    private ClienteDAO clienteDAO = new ClienteDAO();

    private BigDecimal fatturatoMese;
    private int preventiviAperti;
    private int clientiAttivi;
    private List<Preventivo> ultimiPreventivi;

    public String execute() {
        try {
            User currentUser = getCurrentUser();
            logger.info("Dashboard accessed by: " + currentUser.getUsername());

            // Calculate current month revenue (simplified - should come from Fatture)
            fatturatoMese = BigDecimal.ZERO; // TODO: Calculate from Fatture

            // Count open preventivi
            List<Preventivo> preventiviInviati = preventivoDAO.findByStato(Preventivo.Stato.INVIATO);
            preventiviAperti = preventiviInviati.size();

            // Count active clients
            clientiAttivi = (int) clienteDAO.count();

            // Get latest preventivi
            int currentYear = Calendar.getInstance().get(Calendar.YEAR);
            ultimiPreventivi = preventivoDAO.findByAnno(currentYear);
            if (ultimiPreventivi.size() > 10) {
                ultimiPreventivi = ultimiPreventivi.subList(0, 10);
            }

            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error loading dashboard", e);
            addActionError("Errore nel caricamento della dashboard");
            return ERROR;
        }
    }

    private User getCurrentUser() {
        Map<String, Object> session = com.opensymphony.xwork2.ActionContext.getContext().getSession();
        return (User) session.get("currentUser");
    }

    // Getters
    public BigDecimal getFatturatoMese() {
        return fatturatoMese;
    }

    public int getPreventiviAperti() {
        return preventiviAperti;
    }

    public int getClientiAttivi() {
        return clientiAttivi;
    }

    public List<Preventivo> getUltimiPreventivi() {
        return ultimiPreventivi;
    }
}
