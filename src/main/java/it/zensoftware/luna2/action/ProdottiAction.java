package it.zensoftware.luna2.action;

import com.opensymphony.xwork2.ActionSupport;
import it.zensoftware.luna2.dao.ProdottoDAO;
import it.zensoftware.luna2.dao.FornitoreDAO;
import it.zensoftware.luna2.dao.ProdottoFornitoreListinoDAO;
import it.zensoftware.luna2.model.Prodotto;
import it.zensoftware.luna2.model.Fornitore;
import it.zensoftware.luna2.model.ProdottoFornitoreListino;
import it.zensoftware.luna2.model.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Map;
import java.math.BigDecimal;

public class ProdottiAction extends ActionSupport {
    private static final Logger logger = LogManager.getLogger(ProdottiAction.class);
    private ProdottoDAO prodottoDAO = new ProdottoDAO();
    private FornitoreDAO fornitoreDAO = new FornitoreDAO();
    private ProdottoFornitoreListinoDAO listinoFornitoreDAO = new ProdottoFornitoreListinoDAO();
    private Prodotto prodotto;
    private List<Prodotto> prodotti;
    private List<Fornitore> fornitori;
    private List<ProdottoFornitoreListino> listiniFornitore;
    private Long id;
    private String searchTerm;
    private List<Long> listinoFornitoreIds;
    private List<BigDecimal> listinoPrezziAcquisto;

    public String list() {
        if (searchTerm != null && !searchTerm.isEmpty()) {
            prodotti = prodottoDAO.searchByNameOrCode(searchTerm);
        } else {
            prodotti = prodottoDAO.findAllActive();
        }
        return SUCCESS;
    }

    public String create() {
        prodotto = new Prodotto();
        fornitori = fornitoreDAO.findAll();
        listiniFornitore = java.util.Collections.emptyList();
        return SUCCESS;
    }

    public String edit() {
        if (id != null) {
            prodotto = prodottoDAO.findById(id);
            if (prodotto != null && prodotto.getId() != null) {
                listiniFornitore = listinoFornitoreDAO.findByProdottoId(prodotto.getId());
            }
        }
        fornitori = fornitoreDAO.findAll();
        if (listiniFornitore == null) {
            listiniFornitore = java.util.Collections.emptyList();
        }
        return SUCCESS;
    }

    public String save() {
        User currentUser = getCurrentUser();
        if (prodotto.getId() == null) {
            prodotto.setCreatedBy(currentUser);
            prodottoDAO.save(prodotto);
            listinoFornitoreDAO.replaceListinoProdotto(prodotto, listinoFornitoreIds, listinoPrezziAcquisto);
            addActionMessage("Prodotto creato con successo");
        } else {
            prodotto.setModifiedBy(currentUser);
            prodottoDAO.update(prodotto);
            listinoFornitoreDAO.replaceListinoProdotto(prodotto, listinoFornitoreIds, listinoPrezziAcquisto);
            addActionMessage("Prodotto aggiornato con successo");
        }
        return SUCCESS;
    }

    public String delete() {
        if (id != null) {
            Prodotto p = prodottoDAO.findById(id);
            if (p != null) {
                p.setAttivo(false);
                prodottoDAO.update(p);
                addActionMessage("Prodotto disattivato");
            }
        }
        return SUCCESS;
    }

    private User getCurrentUser() {
        Map<String, Object> session = com.opensymphony.xwork2.ActionContext.getContext().getSession();
        return (User) session.get("currentUser");
    }

    // Getter per gli enum da usare nei JSP
    public Prodotto.TipoProdotto[] getTipiProdotto() {
        return Prodotto.TipoProdotto.values();
    }

    public Prodotto.UnitaMisura[] getUnitaMisura() {
        return Prodotto.UnitaMisura.values();
    }

    public List<Fornitore> getFornitori() {
        return fornitori;
    }

    public List<ProdottoFornitoreListino> getListiniFornitore() {
        return listiniFornitore;
    }

    public Prodotto getProdotto() { return prodotto; }
    public void setProdotto(Prodotto prodotto) { this.prodotto = prodotto; }
    public List<Prodotto> getProdotti() { return prodotti; }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSearchTerm() { return searchTerm; }
    public void setSearchTerm(String searchTerm) { this.searchTerm = searchTerm; }
    public List<Long> getListinoFornitoreIds() { return listinoFornitoreIds; }
    public void setListinoFornitoreIds(List<Long> listinoFornitoreIds) { this.listinoFornitoreIds = listinoFornitoreIds; }
    public List<BigDecimal> getListinoPrezziAcquisto() { return listinoPrezziAcquisto; }
    public void setListinoPrezziAcquisto(List<BigDecimal> listinoPrezziAcquisto) { this.listinoPrezziAcquisto = listinoPrezziAcquisto; }
}
