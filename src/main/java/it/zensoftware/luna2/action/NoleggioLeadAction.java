package it.zensoftware.luna2.action;

import com.opensymphony.xwork2.ActionSupport;
import it.zensoftware.luna2.dao.LeadDAO;
import it.zensoftware.luna2.dao.NoleggioLeadDAO;
import it.zensoftware.luna2.model.Lead;
import it.zensoftware.luna2.model.NoleggioLead;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NoleggioLeadAction extends ActionSupport {

    private static final Logger logger = LogManager.getLogger(NoleggioLeadAction.class);
    private static final long serialVersionUID = 1L;

    private final NoleggioLeadDAO noleggioLeadDAO = new NoleggioLeadDAO();
    private final LeadDAO leadDAO = new LeadDAO();

    private List<Map<String, Object>> leads = new ArrayList<>();
    private Map<String, Object> jsonResponse = new HashMap<>();
    private Map<String, Object> dashboardData = new HashMap<>();

    private Long id;
    private String fasceFiltro;
    private String searchTerm;

    // Form fields
    private String ragioneSociale;
    private String nomeContatto;
    private String email;
    private String telefono;
    private String utilizzo;
    private Integer kmAnnuali;
    private Integer durataMesi;
    private BigDecimal budgetMax;
    private String note;
    private String modelloRichiesto;
    private String tipoNoleggio;
    private String origineTipo;

    public String execute() {
        try {
            logger.info("=== NoleggioLeadAction.execute() STARTED ===");
            
            // Load all leads
            List<NoleggioLead> items = noleggioLeadDAO.findAll();
            logger.info("Found " + items.size() + " NoleggioLead items in DB");

            for (NoleggioLead nl : items) {
                logger.info("Processing lead ID: " + nl.getId() + ", Client ID: " + nl.getLeadId());
                Lead lead = leadDAO.findById(nl.getLeadId());
                if (!matchesSearch(lead)) {
                    logger.debug("Lead filtered out by search");
                    continue;
                }

                Map<String, Object> row = new HashMap<>();
                row.put("id", nl.getId());
                row.put("numeroPratica", buildNumeroPratica(nl));
                row.put("ragioneSociale", lead != null ? lead.getAzienda() : "-");
                row.put("email", lead != null ? lead.getEmail() : "-");
                row.put("telefono", lead != null ? lead.getTelefono() : "-");
                row.put("fase", nl.getFase() != null ? nl.getFase().name() : "-");
                row.put("utenteAssegnatoId", null);
                row.put("dataCreazione", formatDateTime(nl.getDataCreazione()));
                leads.add(row);
                logger.info("Added lead to list: " + row.get("numeroPratica"));
            }

            logger.info("Total leads to display: " + leads.size());

            // Load dashboard counts
            Map<NoleggioLead.Fase, Integer> counts = new HashMap<>();
            for (NoleggioLead.Fase fase : NoleggioLead.Fase.values()) {
                counts.put(fase, 0);
            }

            for (NoleggioLead nl : items) {
                if (nl.getFase() != null) {
                    counts.put(nl.getFase(), counts.get(nl.getFase()) + 1);
                }
            }

            dashboardData.put("countPreventivazione", counts.get(NoleggioLead.Fase.PREVENTIVAZIONE));
            dashboardData.put("countIstruttoria", counts.get(NoleggioLead.Fase.ISTRUTTORIA));
            dashboardData.put("countOrdine", counts.get(NoleggioLead.Fase.ORDINE));
            dashboardData.put("countPostVendita", counts.get(NoleggioLead.Fase.POST_VENDITA));

            logger.info("=== NoleggioLeadAction.execute() COMPLETED - Returning SUCCESS ===");
            return SUCCESS;
        } catch (Exception e) {
            logger.error("=== NoleggioLeadAction.execute() ERROR ===", e);
            return ERROR;
        }
    }

    public String list() {
        try {
            List<NoleggioLead> items;
            if (fasceFiltro != null && !fasceFiltro.isEmpty()) {
                items = noleggioLeadDAO.findByProperty("fase", NoleggioLead.Fase.valueOf(fasceFiltro));
            } else {
                items = noleggioLeadDAO.findAll();
            }

            for (NoleggioLead nl : items) {
                Lead lead = leadDAO.findById(nl.getLeadId());
                if (!matchesSearch(lead)) {
                    continue;
                }

                Map<String, Object> row = new HashMap<>();
                row.put("id", nl.getId());
                row.put("numeroPratica", buildNumeroPratica(nl));
                row.put("ragioneSociale", lead != null ? lead.getAzienda() : "-");
                row.put("email", lead != null ? lead.getEmail() : "-");
                row.put("telefono", lead != null ? lead.getTelefono() : "-");
                row.put("fase", nl.getFase() != null ? nl.getFase().name() : "-");
                row.put("utenteAssegnatoId", null);
                row.put("dataCreazione", formatDateTime(nl.getDataCreazione()));
                leads.add(row);
            }

            // Return JSON for AJAX calls
            jsonResponse.put("leads", leads);
            jsonResponse.put("success", true);
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error listing noleggio leads", e);
            jsonResponse.put("message", "Errore nel caricamento lead");
            jsonResponse.put("success", false);
            return ERROR;
        }
    }

    public String dashboardData() {
        try {
            Map<NoleggioLead.Fase, Integer> counts = new HashMap<>();
            for (NoleggioLead.Fase fase : NoleggioLead.Fase.values()) {
                counts.put(fase, 0);
            }

            List<NoleggioLead> items = noleggioLeadDAO.findAll();
            for (NoleggioLead nl : items) {
                if (nl.getFase() != null) {
                    counts.put(nl.getFase(), counts.get(nl.getFase()) + 1);
                }
            }

            dashboardData.put("countPreventivazione", counts.get(NoleggioLead.Fase.PREVENTIVAZIONE));
            dashboardData.put("countIstruttoria", counts.get(NoleggioLead.Fase.ISTRUTTORIA));
            dashboardData.put("countOrdine", counts.get(NoleggioLead.Fase.ORDINE));
            dashboardData.put("countPostVendita", counts.get(NoleggioLead.Fase.POST_VENDITA));
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error loading noleggio lead dashboard", e);
            jsonResponse.put("message", "Errore nel caricamento dashboard");
            return ERROR;
        }
    }

    public String view() {
        try {
            if (id == null) {
                jsonResponse.put("message", "ID mancante");
                return ERROR;
            }

            NoleggioLead nl = noleggioLeadDAO.findById(id);
            if (nl == null) {
                jsonResponse.put("message", "Lead non trovato");
                return ERROR;
            }

            Lead lead = leadDAO.findById(nl.getLeadId());
            jsonResponse.put("id", nl.getId());
            jsonResponse.put("ragioneSociale", lead != null ? lead.getAzienda() : "");
            jsonResponse.put("nomeContatto", lead != null ? lead.getNomeContatto() : "");
            jsonResponse.put("email", lead != null ? lead.getEmail() : "");
            jsonResponse.put("telefono", lead != null ? lead.getTelefono() : "");
            jsonResponse.put("kmAnnuali", nl.getKmAnnui());
            jsonResponse.put("durataMesi", nl.getDurataMesi());
            jsonResponse.put("budgetMax", nl.getBudgetMensile());
            jsonResponse.put("modelloRichiesto", nl.getModelloRichiesto());
            jsonResponse.put("note", lead != null ? lead.getNote() : "");
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error viewing noleggio lead", e);
            jsonResponse.put("message", "Errore nel caricamento lead");
            return ERROR;
        }
    }

    public String save() {
        try {
            Lead lead;
            NoleggioLead nl;

            if (id != null) {
                nl = noleggioLeadDAO.findById(id);
                if (nl == null) {
                    jsonResponse.put("message", "Lead non trovato");
                    return ERROR;
                }
                lead = leadDAO.findById(nl.getLeadId());
            } else {
                lead = new Lead();
                lead.setOrigine(Lead.Origine.WEBSITE);
                lead.setStato(Lead.Stato.NUOVO);
                lead.setDataContatto(new Date());
                nl = new NoleggioLead();
                nl.setFase(NoleggioLead.Fase.PREVENTIVAZIONE);
                nl.setOrigineTipo(parseOrigine());
                nl.setTipoNoleggio(parseTipoNoleggio());
            }

            if (lead != null) {
                lead.setAzienda(ragioneSociale != null ? ragioneSociale : "");
                lead.setNomeContatto(nomeContatto);
                lead.setEmail(email);
                lead.setTelefono(telefono);
                lead.setEsigenza(utilizzo);
                lead.setNote(note);
                if (budgetMax != null) {
                    lead.setBudgetStimato(budgetMax);
                }
            }

            if (lead.getId() == null) {
                leadDAO.save(lead);
            } else {
                leadDAO.update(lead);
            }

            nl.setLeadId(lead.getId());
            nl.setKmAnnui(kmAnnuali);
            nl.setDurataMesi(durataMesi);
            nl.setBudgetMensile(budgetMax);
            nl.setModelloRichiesto(modelloRichiesto);

            if (nl.getId() == null) {
                noleggioLeadDAO.save(nl);
            } else {
                noleggioLeadDAO.update(nl);
            }

            jsonResponse.put("message", "Lead salvato");
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error saving noleggio lead", e);
            jsonResponse.put("message", "Errore nel salvataggio lead");
            return ERROR;
        }
    }

    public String delete() {
        try {
            if (id == null) {
                jsonResponse.put("message", "ID mancante");
                return ERROR;
            }
            NoleggioLead nl = noleggioLeadDAO.findById(id);
            if (nl != null) {
                noleggioLeadDAO.delete(nl);
            }
            jsonResponse.put("message", "Lead eliminato");
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error deleting noleggio lead", e);
            jsonResponse.put("message", "Errore eliminazione lead");
            return ERROR;
        }
    }

    private boolean matchesSearch(Lead lead) {
        if (searchTerm == null || searchTerm.isEmpty()) {
            return true;
        }
        if (lead == null) {
            return false;
        }
        String value = searchTerm.toLowerCase();
        return (lead.getAzienda() != null && lead.getAzienda().toLowerCase().contains(value))
            || (lead.getEmail() != null && lead.getEmail().toLowerCase().contains(value))
            || (lead.getTelefono() != null && lead.getTelefono().toLowerCase().contains(value));
    }

    private String buildNumeroPratica(NoleggioLead nl) {
        return "NL-" + nl.getId();
    }

    private String formatDateTime(Date date) {
        if (date == null) {
            return "";
        }
        return new SimpleDateFormat("dd/MM/yyyy HH:mm").format(date);
    }

    private NoleggioLead.OrigineTipo parseOrigine() {
        if (origineTipo == null || origineTipo.isEmpty()) {
            return NoleggioLead.OrigineTipo.DIRETTO;
        }
        return NoleggioLead.OrigineTipo.valueOf(origineTipo);
    }

    private NoleggioLead.TipoNoleggio parseTipoNoleggio() {
        if (tipoNoleggio == null || tipoNoleggio.isEmpty()) {
            return NoleggioLead.TipoNoleggio.LUNGO_TERMINE;
        }
        return NoleggioLead.TipoNoleggio.valueOf(tipoNoleggio);
    }

    public List<Map<String, Object>> getLeads() {
        return leads;
    }

    public List<Map<String, Object>> getEntities() {
        return leads;
    }

    public Map<String, Object> getJsonResponse() {
        return jsonResponse;
    }

    public Map<String, Object> getDashboardData() {
        return dashboardData;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFasceFiltro() {
        return fasceFiltro;
    }

    public void setFasceFiltro(String fasceFiltro) {
        this.fasceFiltro = fasceFiltro;
    }

    public String getSearchTerm() {
        return searchTerm;
    }

    public void setSearchTerm(String searchTerm) {
        this.searchTerm = searchTerm;
    }

    public String getRagioneSociale() {
        return ragioneSociale;
    }

    public void setRagioneSociale(String ragioneSociale) {
        this.ragioneSociale = ragioneSociale;
    }

    public String getNomeContatto() {
        return nomeContatto;
    }

    public void setNomeContatto(String nomeContatto) {
        this.nomeContatto = nomeContatto;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getUtilizzo() {
        return utilizzo;
    }

    public void setUtilizzo(String utilizzo) {
        this.utilizzo = utilizzo;
    }

    public Integer getKmAnnuali() {
        return kmAnnuali;
    }

    public void setKmAnnuali(Integer kmAnnuali) {
        this.kmAnnuali = kmAnnuali;
    }

    public Integer getDurataMesi() {
        return durataMesi;
    }

    public void setDurataMesi(Integer durataMesi) {
        this.durataMesi = durataMesi;
    }

    public BigDecimal getBudgetMax() {
        return budgetMax;
    }

    public void setBudgetMax(BigDecimal budgetMax) {
        this.budgetMax = budgetMax;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getModelloRichiesto() {
        return modelloRichiesto;
    }

    public void setModelloRichiesto(String modelloRichiesto) {
        this.modelloRichiesto = modelloRichiesto;
    }

    public String getTipoNoleggio() {
        return tipoNoleggio;
    }

    public void setTipoNoleggio(String tipoNoleggio) {
        this.tipoNoleggio = tipoNoleggio;
    }

    public String getOrigineTipo() {
        return origineTipo;
    }

    public void setOrigineTipo(String origineTipo) {
        this.origineTipo = origineTipo;
    }
}
