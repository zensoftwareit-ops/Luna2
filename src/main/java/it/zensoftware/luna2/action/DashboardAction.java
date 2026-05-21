package it.zensoftware.luna2.action;

import com.opensymphony.xwork2.ActionSupport;
import it.zensoftware.luna2.dao.*;
import it.zensoftware.luna2.model.*;
import it.zensoftware.luna2.model.Lead.Stato;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Dashboard Action - Enhanced with comprehensive metrics
 */
public class DashboardAction extends ActionSupport {
    
    private static final Logger logger = LogManager.getLogger(DashboardAction.class);
    private static final long serialVersionUID = 1L;

    private PreventivoDAO preventivoDAO = new PreventivoDAO();
    private ClienteDAO clienteDAO = new ClienteDAO();
    private FatturaDAO fatturaDAO = new FatturaDAO();
    private OrdineDAO ordineDAO = new OrdineDAO();
    private LeadDAO leadDAO = new LeadDAO();
    private GiacenzaDAO giacenzaDAO = new GiacenzaDAO();
    private CommessaDAO commessaDAO = new CommessaDAO();
    private FatturaPassivaDAO fatturaPassivaDAO = new FatturaPassivaDAO();

    // Core metrics
    private BigDecimal fatturatoMese;
    private BigDecimal fatturatoAnno;
    private int preventiviAperti;
    private int clientiAttivi;
    private int ordiniInLavorazione;
    private int fattureEmesseAnno;
    private int leadAttivi;
    private int leadConversioneRate;
    private int giacenzeSottoScorta;
    private BigDecimal valoreMagazzinoTotale;
    private int commesseAttive;
    private int fatturePassiveScadute;
    
    // Lists
    private List<Preventivo> ultimiPreventivi;
    private List<Fattura> ultimeFatture;
    private List<Ordine> ultimiOrdini;
    private List<Lead> ultimiLead;
    private List<Map<String, Object>> attivitaRecenti;
    private List<Map<String, Object>> alertMagazzino;
    private List<Map<String, Object>> taskScadenza;
    private Map<String, Integer> statisticheGiornaliere;
    
    // Chart data
    private List<BigDecimal> fatturatoMensile; // Ultimi 12 mesi
    private List<String> mesiChart;
    private Map<String, Integer> leadPerStato;

    public String execute() {
        try {
            User currentUser = getCurrentUser();
            logger.info("Dashboard accessed by: " + currentUser.getUsername());

            Calendar cal = Calendar.getInstance();
            int currentYear = cal.get(Calendar.YEAR);
            int currentMonth = cal.get(Calendar.MONTH);
            
            // Calculate month revenue from Fatture
            cal.set(Calendar.DAY_OF_MONTH, 1);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            Date inizioMese = cal.getTime();
            Date oggi = new Date();
            
            List<Fattura> fattureAnno = fatturaDAO.findByAnno(currentYear);
            fatturatoMese = BigDecimal.ZERO;
            fatturatoAnno = BigDecimal.ZERO;
            fattureEmesseAnno = fattureAnno.size();
            
            for (Fattura f : fattureAnno) {
                if (f.getTotale() != null) {
                    fatturatoAnno = fatturatoAnno.add(f.getTotale());
                    if (f.getDataFattura() != null && 
                        !f.getDataFattura().before(inizioMese) && 
                        !f.getDataFattura().after(oggi)) {
                        fatturatoMese = fatturatoMese.add(f.getTotale());
                    }
                }
            }

            // Count open preventivi
            List<Preventivo> preventiviInviati = preventivoDAO.findByStato(Preventivo.Stato.INVIATO);
            preventiviAperti = preventiviInviati != null ? preventiviInviati.size() : 0;

            // Count active clients
            clientiAttivi = (int) clienteDAO.count();
            
            // Ordini in lavorazione
            try {
                List<Ordine> ordini = ordineDAO.findAll();
                ordiniInLavorazione = (int) ordini.stream()
                    .filter(o -> o.getStato() == Ordine.Stato.IN_LAVORAZIONE ||
                                 o.getStato() == Ordine.Stato.CONFERMATO)
                    .count();
            } catch (Exception e) {
                ordiniInLavorazione = 0;
            }
            
            // Lead CRM metrics
            try {
                List<Lead> leads = leadDAO.findAll();
                leadAttivi = (int) leads.stream()
                    .filter(l -> l.getStato() != Stato.VINTO && l.getStato() != Stato.PERSO)
                    .count();
                long leadVinti = leads.stream().filter(l -> l.getStato() == Stato.VINTO).count();
                long leadQualificati = leads.stream().filter(l -> l.getStato() == Stato.QUALIFICATO).count();
                leadConversioneRate = leadQualificati > 0 ? (int) ((leadVinti * 100) / leadQualificati) : 0;
                
                // Lead per stato for chart
                leadPerStato = new HashMap<>();
                for (Stato stato : Stato.values()) {
                    long count = leads.stream().filter(l -> l.getStato() == stato).count();
                    leadPerStato.put(stato.name(), (int) count);
                }
                
                ultimiLead = leads.stream()
                    .sorted((a, b) -> b.getDataCreazione().compareTo(a.getDataCreazione()))
                    .limit(5)
                    .collect(Collectors.toList());
            } catch (Exception e) {
                logger.warn("Lead metrics not available: " + e.getMessage());
                leadAttivi = 0;
                leadConversioneRate = 0;
                leadPerStato = new HashMap<>();
                ultimiLead = new ArrayList<>();
            }
            
            // Magazzino metrics
            try {
                List<Giacenza> giacenze = giacenzaDAO.findAll();
                giacenzeSottoScorta = giacenzaDAO.findSottoScorta().size();
                valoreMagazzinoTotale = giacenze.stream()
                    .map(g -> g.getValoreGiacenza() != null ? g.getValoreGiacenza() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            } catch (Exception e) {
                giacenzeSottoScorta = 0;
                valoreMagazzinoTotale = BigDecimal.ZERO;
            }
            
            // Commesse attive
            try {
                List<Commessa> commesse = commessaDAO.findAll();
                commesseAttive = (int) commesse.stream()
                    .filter(c -> c.getStato() == Commessa.StatoCommessa.IN_LAVORAZIONE)
                    .count();
            } catch (Exception e) {
                commesseAttive = 0;
            }

            // Fatture passive in scadenza
            try {
                List<FatturaPassiva> fatturePassiveScadute = fatturaPassivaDAO.findScadute();
                this.fatturePassiveScadute = fatturePassiveScadute != null ? fatturePassiveScadute.size() : 0;
            } catch (Exception e) {
                logger.warn("Error loading passive invoices: " + e.getMessage());
                this.fatturePassiveScadute = 0;
            }

            // Get latest data
            ultimiPreventivi = preventivoDAO.findByAnno(currentYear);
            if (ultimiPreventivi != null && ultimiPreventivi.size() > 10) {
                ultimiPreventivi = ultimiPreventivi.subList(0, 10);
            }
            
            ultimeFatture = fattureAnno.stream()
                .sorted((a, b) -> b.getDataFattura().compareTo(a.getDataFattura()))
                .limit(5)
                .collect(Collectors.toList());
            
            // Chart data - Fatturato ultimi 12 mesi
            fatturatoMensile = new ArrayList<>();
            mesiChart = new ArrayList<>();
            for (int i = 11; i >= 0; i--) {
                Calendar chartCal = Calendar.getInstance();
                chartCal.add(Calendar.MONTH, -i);
                int anno = chartCal.get(Calendar.YEAR);
                int mese = chartCal.get(Calendar.MONTH);
                
                String[] mesiNomi = {"Gen", "Feb", "Mar", "Apr", "Mag", "Giu", 
                                     "Lug", "Ago", "Set", "Ott", "Nov", "Dic"};
                mesiChart.add(mesiNomi[mese]);
                
                List<Fattura> fattureMese = fatturaDAO.findByAnno(anno);
                BigDecimal totaleMese = fattureMese.stream()
                    .filter(f -> f.getDataFattura() != null)
                    .filter(f -> {
                        Calendar fCal = Calendar.getInstance();
                        fCal.setTime(f.getDataFattura());
                        return fCal.get(Calendar.MONTH) == mese && fCal.get(Calendar.YEAR) == anno;
                    })
                    .map(f -> f.getTotale() != null ? f.getTotale() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                fatturatoMensile.add(totaleMese);
            }
            
            // Recent activities aggregate
            generaAttivitaRecenti();
            
            // Warehouse alerts dettagliati
            generaAlertMagazzino();
            
            // Task e scadenze
            generaTaskScadenza();
            
            // Statistiche giornaliere
            calcolaStatisticheGiornaliere();

            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error loading dashboard", e);
            addActionError("Errore nel caricamento della dashboard: " + e.getMessage());
            return ERROR;
        }
    }

    private User getCurrentUser() {
        Map<String, Object> session = com.opensymphony.xwork2.ActionContext.getContext().getSession();
        return (User) session.get("currentUser");
    }

    // Getters
    public BigDecimal getFatturatoMese() { return fatturatoMese; }
    public BigDecimal getFatturatoAnno() { return fatturatoAnno; }
    public int getPreventiviAperti() { return preventiviAperti; }
    public int getClientiAttivi() { return clientiAttivi; }
    public int getOrdiniInLavorazione() { return ordiniInLavorazione; }
    public int getFattureEmesseAnno() { return fattureEmesseAnno; }
    public int getLeadAttivi() { return leadAttivi; }
    public int getLeadConversioneRate() { return leadConversioneRate; }
    public int getGiacenzeSottoScorta() { return giacenzeSottoScorta; }
    public BigDecimal getValoreMagazzinoTotale() { return valoreMagazzinoTotale; }
    public int getCommesseAttive() { return commesseAttive; }
    public int getFatturePassiveScadute() { return fatturePassiveScadute; }
    public List<Preventivo> getUltimiPreventivi() { return ultimiPreventivi; }
    public List<Fattura> getUltimeFatture() { return ultimeFatture; }
    public List<Ordine> getUltimiOrdini() { return ultimiOrdini; }
    public List<Lead> getUltimiLead() { return ultimiLead; }
    public List<Map<String, Object>> getAttivitaRecenti() { return attivitaRecenti; }
    public List<BigDecimal> getFatturatoMensile() { return fatturatoMensile; }
    public List<String> getMesiChart() { return mesiChart; }
    public Map<String, Integer> getLeadPerStato() { return leadPerStato; }
    public List<Map<String, Object>> getAlertMagazzino() { return alertMagazzino; }
    public List<Map<String, Object>> getTaskScadenza() { return taskScadenza; }
    public Map<String, Integer> getStatisticheGiornaliere() { return statisticheGiornaliere; }
    
    /**
     * Genera feed attività recenti aggregando dati da tutti i moduli
     */
    private void generaAttivitaRecenti() {
        attivitaRecenti = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        
        try {
            // Ultime fatture
            for (Fattura f : ultimeFatture) {
                if (attivitaRecenti.size() >= 20) break;
                Map<String, Object> activity = new HashMap<>();
                activity.put("tipo", "FATTURA");
                activity.put("icona", "receipt");
                activity.put("colore", "success");
                activity.put("titolo", "Fattura emessa: " + f.getNumero());
                activity.put("descrizione", f.getCliente().getRagioneSociale());
                activity.put("data", f.getDataFattura());
                activity.put("ora", sdf.format(f.getDataFattura()));
                attivitaRecenti.add(activity);
            }
            
            // Ultimi lead
            for (Lead l : ultimiLead) {
                if (attivitaRecenti.size() >= 20) break;
                Map<String, Object> activity = new HashMap<>();
                activity.put("tipo", "LEAD");
                activity.put("icona", "person-plus");
                activity.put("colore", "info");
                activity.put("titolo", "Nuovo lead: " + l.getNomeContattoCompleto());
                activity.put("descrizione", l.getAzienda() + " - " + l.getStato());
                activity.put("data", l.getDataCreazione());
                activity.put("ora", sdf.format(l.getDataCreazione()));
                attivitaRecenti.add(activity);
            }
            
            // Ultimi preventivi
            for (Preventivo p : ultimiPreventivi) {
                if (attivitaRecenti.size() >= 20) break;
                Map<String, Object> activity = new HashMap<>();
                activity.put("tipo", "PREVENTIVO");
                activity.put("icona", "file-earmark-text");
                activity.put("colore", "primary");
                activity.put("titolo", "Preventivo: " + p.getNumero());
                activity.put("descrizione", p.getCliente().getRagioneSociale());
                activity.put("data", p.getDataPreventivo());
                activity.put("ora", sdf.format(p.getDataPreventivo()));
                attivitaRecenti.add(activity);
            }
            
            // Sort by date descending
            attivitaRecenti.sort((a, b) -> {
                Date dateA = (Date) a.get("data");
                Date dateB = (Date) b.get("data");
                return dateB.compareTo(dateA);
            });
            
            // Keep only top 10
            if (attivitaRecenti.size() > 10) {
                attivitaRecenti = attivitaRecenti.subList(0, 10);
            }
        } catch (Exception e) {
            logger.warn("Error generating activity feed: " + e.getMessage());
            attivitaRecenti = new ArrayList<>();
        }
    }
    
    /**
     * Alert magazzino dettagliati con priorità
     */
    private void generaAlertMagazzino() {
        alertMagazzino = new ArrayList<>();
        
        try {
            List<Giacenza> giacenzeSottoScorta = giacenzaDAO.findSottoScorta();
            
            for (Giacenza g : giacenzeSottoScorta) {
                if (alertMagazzino.size() >= 5) break;
                
                Map<String, Object> alert = new HashMap<>();
                alert.put("prodotto", g.getProdotto().getNome());
                alert.put("codice", g.getProdotto().getCodice());
                alert.put("disponibile", g.getQuantitaAttuale());
                alert.put("minimo", g.getQuantitaMinima());
                
                BigDecimal percentuale = g.getQuantitaAttuale()
                    .divide(g.getQuantitaMinima(), 2, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal(100));
                alert.put("percentuale", percentuale.intValue());
                
                if (g.getQuantitaAttuale().compareTo(BigDecimal.ZERO) == 0) {
                    alert.put("priorita", "CRITICA");
                    alert.put("classe", "danger");
                } else if (percentuale.compareTo(new BigDecimal(30)) < 0) {
                    alert.put("priorita", "ALTA");
                    alert.put("classe", "warning");
                } else {
                    alert.put("priorita", "MEDIA");
                    alert.put("classe", "info");
                }
                
                alertMagazzino.add(alert);
            }
        } catch (Exception e) {
            logger.warn("Error generating warehouse alerts: " + e.getMessage());
            alertMagazzino = new ArrayList<>();
        }
    }
    
    /**
     * Task e scadenze prossimi 7 giorni
     */
    private void generaTaskScadenza() {
        taskScadenza = new ArrayList<>();
        
        try {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_MONTH, 7);
            Date settimanaSuccessiva = cal.getTime();
            Date oggi = new Date();
            
            // Lead da followup
            List<Lead> leads = leadDAO.findAll();
            for (Lead l : leads) {
                if (l.getDataProssimoFollowup() != null &&
                    !l.getDataProssimoFollowup().before(oggi) &&
                    !l.getDataProssimoFollowup().after(settimanaSuccessiva)) {
                    
                    Map<String, Object> task = new HashMap<>();
                    task.put("tipo", "FOLLOWUP");
                    task.put("titolo", "Follow-up lead: " + l.getNomeContattoCompleto());
                    task.put("scadenza", l.getDataProssimoFollowup());
                    task.put("priorita", calcolaPrioritaTask(l.getDataProssimoFollowup(), oggi));
                    taskScadenza.add(task);
                }
            }
            
            // Preventivi in scadenza (validità 30 giorni default)
            for (Preventivo p : ultimiPreventivi) {
                if (p.getStato() == Preventivo.Stato.INVIATO) {
                    Calendar prevCal = Calendar.getInstance();
                    prevCal.setTime(p.getDataPreventivo());
                    prevCal.add(Calendar.DAY_OF_MONTH, 30);
                    Date scadenzaPrev = prevCal.getTime();
                    
                    if (!scadenzaPrev.before(oggi) && !scadenzaPrev.after(settimanaSuccessiva)) {
                        Map<String, Object> task = new HashMap<>();
                        task.put("tipo", "PREVENTIVO");
                        task.put("titolo", "Scadenza preventivo: " + p.getNumero());
                        task.put("scadenza", scadenzaPrev);
                        task.put("priorita", calcolaPrioritaTask(scadenzaPrev, oggi));
                        taskScadenza.add(task);
                    }
                }
            }
            
            // Sort by date
            taskScadenza.sort((a, b) -> {
                Date dateA = (Date) a.get("scadenza");
                Date dateB = (Date) b.get("scadenza");
                return dateA.compareTo(dateB);
            });
            
        } catch (Exception e) {
            logger.warn("Error generating tasks: " + e.getMessage());
            taskScadenza = new ArrayList<>();
        }
    }
    
    private String calcolaPrioritaTask(Date scadenza, Date oggi) {
        long diff = scadenza.getTime() - oggi.getTime();
        long giorni = diff / (1000 * 60 * 60 * 24);
        
        if (giorni <= 1) return "ALTA";
        if (giorni <= 3) return "MEDIA";
        return "BASSA";
    }
    
    /**
     * Statistiche giornaliere comparative
     */
    private void calcolaStatisticheGiornaliere() {
        statisticheGiornaliere = new HashMap<>();
        
        try {
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            Date startOfDay = cal.getTime();
            Date now = new Date();
            
            // Fatture oggi
            long fattureOggi = ultimeFatture.stream()
                .filter(f -> f.getDataFattura() != null)
                .filter(f -> !f.getDataFattura().before(startOfDay))
                .count();
            statisticheGiornaliere.put("fattureOggi", (int) fattureOggi);
            
            // Lead creati oggi
            long leadOggi = ultimiLead.stream()
                .filter(l -> l.getDataCreazione() != null)
                .filter(l -> !l.getDataCreazione().before(startOfDay))
                .count();
            statisticheGiornaliere.put("leadOggi", (int) leadOggi);
            
            // Preventivi oggi
            long preventiviOggi = ultimiPreventivi.stream()
                .filter(p -> p.getDataPreventivo() != null)
                .filter(p -> !p.getDataPreventivo().before(startOfDay))
                .count();
            statisticheGiornaliere.put("preventiviOggi", (int) preventiviOggi);
            
        } catch (Exception e) {
            logger.warn("Error calculating daily stats: " + e.getMessage());
            statisticheGiornaliere = new HashMap<>();
        }
    }
}
