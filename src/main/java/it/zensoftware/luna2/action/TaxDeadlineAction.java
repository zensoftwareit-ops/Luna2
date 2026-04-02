package it.zensoftware.luna2.action;

import com.opensymphony.xwork2.ActionSupport;
import it.zensoftware.luna2.dao.TaxDeadlineDAO;
import it.zensoftware.luna2.model.TaxDeadline;
import it.zensoftware.luna2.model.TaxDeadline.DeadlineStatus;
import it.zensoftware.luna2.model.TaxDeadline.DeadlineType;
import it.zensoftware.luna2.model.TaxDeadline.Frequency;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Action per la gestione dello scadenzario fiscale (Tax Deadlines)
 * Gestisce tutte le scadenze obbligatorie per una SRL
 */
public class TaxDeadlineAction extends ActionSupport {

    private static final Logger logger = LogManager.getLogger(TaxDeadlineAction.class);

    private final TaxDeadlineDAO taxDeadlineDAO = new TaxDeadlineDAO();

    private List<TaxDeadline> deadlines = new ArrayList<>();
    private List<TaxDeadline> overdueDeadlines = new ArrayList<>();
    private List<TaxDeadline> upcomingDeadlines = new ArrayList<>();
    private TaxDeadline taxDeadline;
    private Long id;

    private String filterType; // Filter by DeadlineType
    private String filterStatus; // Filter by DeadlineStatus
    private Map<String, Object> jsonResponse = new HashMap<>();

    /**
     * Lista tutte le scadenze fiscali
     */
    public String list() {
        try {
            if ("OVERDUE".equals(filterStatus)) {
                overdueDeadlines = taxDeadlineDAO.findOverdueDeadlines(new Date());
                deadlines = overdueDeadlines;
            } else if ("UPCOMING".equals(filterStatus)) {
                upcomingDeadlines = taxDeadlineDAO.findUpcomingDeadlines(30);
                deadlines = upcomingDeadlines;
            } else {
                deadlines = taxDeadlineDAO.findOpenDeadlines();
            }

            // Filtra per tipo se specificato
            if (filterType != null && !filterType.isEmpty()) {
                try {
                    DeadlineType typeFilter = DeadlineType.valueOf(filterType);
                    deadlines.removeIf(d -> d.getType() != typeFilter);
                } catch (IllegalArgumentException e) {
                    logger.warn("Tipo filtro non valido: " + filterType);
                }
            }

            return SUCCESS;
        } catch (Exception e) {
            logger.error("Errore nel caricamento scadenze", e);
            addActionError("Errore caricamento scadenze: " + e.getMessage());
            return ERROR;
        }
    }

    /**
     * Form per creare/modificare una scadenza
     */
    public String create() {
        taxDeadline = new TaxDeadline();
        taxDeadline.setDeadlineDate(new Date());
        return SUCCESS;
    }

    /**
     * Modifica una scadenza esistente
     */
    public String edit() {
        if (id != null) {
            taxDeadline = taxDeadlineDAO.findById(id);
            if (taxDeadline == null) {
                addActionError("Scadenza non trovata");
                return ERROR;
            }
        }
        return SUCCESS;
    }

    /**
     * Salva una scadenza (nuova o modifica)
     */
    public String save() {
        try {
            if (taxDeadline == null) {
                addActionError("Scadenza non valida");
                return ERROR;
            }

            // Validazioni base
            if (taxDeadline.getTitle() == null || taxDeadline.getTitle().trim().isEmpty()) {
                addActionError("Titolo obbligatorio");
                return ERROR;
            }

            if (taxDeadline.getDeadlineDate() == null) {
                addActionError("Data scadenza obbligatoria");
                return ERROR;
            }

            if (taxDeadline.getType() == null) {
                taxDeadline.setType(DeadlineType.ALTRO);
            }

            if (taxDeadline.getStatus() == null) {
                taxDeadline.setStatus(DeadlineStatus.OPEN);
            }

            if (taxDeadline.getFrequency() == null) {
                taxDeadline.setFrequency(Frequency.ONCE);
            }

            taxDeadlineDAO.save(taxDeadline);
            addActionMessage("Scadenza salvata con successo");
            return "redirect";
        } catch (Exception e) {
            logger.error("Errore nel salvataggio della scadenza", e);
            addActionError("Errore salvataggio: " + e.getMessage());
            return ERROR;
        }
    }

    /**
     * Marca una scadenza come completata
     */
    public String complete() {
        try {
            if (id != null) {
                taxDeadlineDAO.completeDeadline(id);
                addActionMessage("Scadenza marcata come completata");
            }
            return "redirect";
        } catch (Exception e) {
            logger.error("Errore nel completamento della scadenza", e);
            addActionError("Errore: " + e.getMessage());
            return ERROR;
        }
    }

    /**
     * Elimina una scadenza
     */
    public String delete() {
        try {
            if (id != null) {
                TaxDeadline toDelete = taxDeadlineDAO.findById(id);
                if (toDelete != null) {
                    taxDeadlineDAO.delete(toDelete);
                    addActionMessage("Scadenza eliminata");
                }
            }
            return "redirect";
        } catch (Exception e) {
            logger.error("Errore nella cancellazione della scadenza", e);
            addActionError("Errore cancellazione: " + e.getMessage());
            return ERROR;
        }
    }

    /**
     * Dashboard con statistiche scadenze
     */
    public String dashboard() {
        try {
            Map<String, Long> stats = new HashMap<>();

            // Conta scadenze per stato
            long open = taxDeadlineDAO.countOpen();
            long overdue = taxDeadlineDAO.countOverdue(new Date());
            long upcoming7 = taxDeadlineDAO.countUpcoming(7);

            stats.put("openCount", open);
            stats.put("overdueCount", overdue);
            stats.put("upcoming7Count", upcoming7);

            // Conta per tipo
            for (DeadlineType type : DeadlineType.values()) {
                stats.put("count_" + type.name(), taxDeadlineDAO.countByType(type));
            }

            // Ultimi 5 open
            List<TaxDeadline> latest = new ArrayList<>();
            for (TaxDeadline d : taxDeadlineDAO.findOpenDeadlines()) {
                if (latest.size() < 5) {
                    latest.add(d);
                }
            }

            jsonResponse.put("stats", stats);
            jsonResponse.put("latestDeadlines", latest);

            return SUCCESS;
        } catch (Exception e) {
            logger.error("Errore nella dashboard", e);
            return ERROR;
        }
    }

    /**
     * Endpoint JSON per scadenze imminenti (7 giorni)
     */
    public String getUpcoming() {
        try {
            List<TaxDeadline> upcoming = taxDeadlineDAO.findUpcomingDeadlines(7);
            jsonResponse.put("success", true);
            jsonResponse.put("deadlines", upcoming);
            jsonResponse.put("count", upcoming.size());
            return SUCCESS;
        } catch (Exception e) {
            jsonResponse.put("success", false);
            jsonResponse.put("error", e.getMessage());
            return ERROR;
        }
    }

    /**
     * Endpoint JSON per scadenze scadute
     */
    public String getOverdue() {
        try {
            List<TaxDeadline> overdue = taxDeadlineDAO.findOverdueDeadlines(new Date());
            jsonResponse.put("success", true);
            jsonResponse.put("deadlines", overdue);
            jsonResponse.put("count", overdue.size());
            return SUCCESS;
        } catch (Exception e) {
            jsonResponse.put("success", false);
            jsonResponse.put("error", e.getMessage());
            return ERROR;
        }
    }

    /**
     * Popola seed data con scadenze fiscali critiche per una SRL
     */
    public String seedItalianDeadlines() {
        try {
            logger.info("Popolo scadenzario con deadlines fiscali italiane");

            // Cancella quelle esistenti se necessario
            List<TaxDeadline> existing = taxDeadlineDAO.findAllOrdered();
            if (existing != null && !existing.isEmpty()) {
                logger.info("Scadenze già presenti: " + existing.size());
                return SUCCESS;
            }

            int year = Calendar.getInstance().get(Calendar.YEAR);

            // F24 Mensile per IVA
            for (int month = 1; month <= 12; month++) {
                Calendar cal = Calendar.getInstance();
                cal.set(Calendar.YEAR, year);
                cal.set(Calendar.MONTH, month - 1);
                cal.set(Calendar.DAY_OF_MONTH, 20);

                TaxDeadline f24Monthly = new TaxDeadline();
                f24Monthly.setTitle("F24 IVA Mensile - " + getMonthName(month));
                f24Monthly.setType(DeadlineType.F24);
                f24Monthly.setStatus(DeadlineStatus.OPEN);
                f24Monthly.setFrequency(Frequency.MONTHLY);
                f24Monthly.setDeadlineDate(cal.getTime());
                f24Monthly.setNotes("Versamento mensile IVA entro il 20 del mese successivo");
                taxDeadlineDAO.save(f24Monthly);
            }

            // IVA Trimestrale (se regime trimestrale)
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.YEAR, year);
            cal.set(Calendar.MONTH, Calendar.MARCH);
            cal.set(Calendar.DAY_OF_MONTH, 16);
            TaxDeadline ivaQ1 = new TaxDeadline();
            ivaQ1.setTitle("IVA Trimestrale Q1");
            ivaQ1.setType(DeadlineType.IVA);
            ivaQ1.setFrequency(Frequency.QUARTERLY);
            ivaQ1.setDeadlineDate(cal.getTime());
            ivaQ1.setStatus(DeadlineStatus.OPEN);
            ivaQ1.setNotes("Dichiarazione IVA Q1 entro il 16 aprile");
            taxDeadlineDAO.save(ivaQ1);

            // E3
            cal = Calendar.getInstance();
            cal.set(Calendar.YEAR, year);
            cal.set(Calendar.MONTH, Calendar.APRIL);
            cal.set(Calendar.DAY_OF_MONTH, 30);
            TaxDeadline e3 = new TaxDeadline();
            e3.setTitle("Dichiarazione E3 (Ritenute su Lavoro Dipendente)");
            e3.setType(DeadlineType.IRPEF);
            e3.setFrequency(Frequency.ANNUAL);
            e3.setDeadlineDate(cal.getTime());
            e3.setStatus(DeadlineStatus.OPEN);
            e3.setNotes("Comunicazione ritenute fiscali entro 30 aprile");
            taxDeadlineDAO.save(e3);

            // CUD (Certificazione Unica Dipendenti)
            cal = Calendar.getInstance();
            cal.set(Calendar.YEAR, year);
            cal.set(Calendar.MONTH, Calendar.JANUARY);
            cal.set(Calendar.DAY_OF_MONTH, 31);
            TaxDeadline cud = new TaxDeadline();
            cud.setTitle("CUD - Certificazione Unica Dipendenti");
            cud.setType(DeadlineType.IRPEF);
            cud.setFrequency(Frequency.ANNUAL);
            cud.setDeadlineDate(cal.getTime());
            cud.setStatus(DeadlineStatus.OPEN);
            cud.setNotes("CUD da consegnare a dipendenti entro 31 gennaio");
            taxDeadlineDAO.save(cud);

            // Versamento INPS
            cal = Calendar.getInstance();
            cal.set(Calendar.YEAR, year);
            cal.set(Calendar.MONTH, Calendar.FEBRUARY);
            cal.set(Calendar.DAY_OF_MONTH, 16);
            TaxDeadline inps = new TaxDeadline();
            inps.setTitle("Versamento INPS Mensile");
            inps.setType(DeadlineType.INPS);
            inps.setFrequency(Frequency.MONTHLY);
            inps.setDeadlineDate(cal.getTime());
            inps.setStatus(DeadlineStatus.OPEN);
            inps.setNotes("Versamento contributi INPS entro il 16 del mese");
            taxDeadlineDAO.save(inps);

            // Dichiarazione 730/UNICO
            cal = Calendar.getInstance();
            cal.set(Calendar.YEAR, year);
            cal.set(Calendar.MONTH, Calendar.JUNE);
            cal.set(Calendar.DAY_OF_MONTH, 30);
            TaxDeadline unico = new TaxDeadline();
            unico.setTitle("Dichiarazione dei Redditi (UNICO)");
            unico.setType(DeadlineType.IRES);
            unico.setFrequency(Frequency.ANNUAL);
            unico.setDeadlineDate(cal.getTime());
            unico.setStatus(DeadlineStatus.OPEN);
            unico.setNotes("Dichiarazione UNICO entro 30 giugno");
            taxDeadlineDAO.save(unico);

            // Comunicazione IRAP
            cal = Calendar.getInstance();
            cal.set(Calendar.YEAR, year);
            cal.set(Calendar.MONTH, Calendar.MAY);
            cal.set(Calendar.DAY_OF_MONTH, 20);
            TaxDeadline irap = new TaxDeadline();
            irap.setTitle("IRAP - Imposta Regionale Attività Produttive");
            irap.setType(DeadlineType.IRAP);
            irap.setFrequency(Frequency.ANNUAL);
            irap.setDeadlineDate(cal.getTime());
            irap.setStatus(DeadlineStatus.OPEN);
            irap.setNotes("Versamento IRAP entro 20 maggio");
            taxDeadlineDAO.save(irap);

            // Bilancio e Comunicazioni Camera di Commercio
            cal = Calendar.getInstance();
            cal.set(Calendar.YEAR, year);
            cal.set(Calendar.MONTH, Calendar.DECEMBER);
            cal.set(Calendar.DAY_OF_MONTH, 15);
            TaxDeadline bilancio = new TaxDeadline();
            bilancio.setTitle("Chiusura Bilancio d'Esercizio");
            bilancio.setType(DeadlineType.ALTRO);
            bilancio.setFrequency(Frequency.ANNUAL);
            bilancio.setDeadlineDate(cal.getTime());
            bilancio.setStatus(DeadlineStatus.OPEN);
            bilancio.setNotes("Chiusura bilancio, deposito in camera di commercio entro 15 giugno anno successivo");
            taxDeadlineDAO.save(bilancio);

            logger.info("Scadenzario fiscale italiano completo!");
            addActionMessage("Scadenzario fiscale popolato con successo");
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Errore nel seed del scadenzario", e);
            addActionError("Errore seed: " + e.getMessage());
            return ERROR;
        }
    }

    private String getMonthName(int month) {
        String[] months = {"Gennaio", "Febbraio", "Marzo", "Aprile", "Maggio", "Giugno",
                "Luglio", "Agosto", "Settembre", "Ottobre", "Novembre", "Dicembre"};
        return months[month - 1];
    }

    // Getters and Setters
    public List<TaxDeadline> getDeadlines() {
        return deadlines;
    }

    public void setDeadlines(List<TaxDeadline> deadlines) {
        this.deadlines = deadlines;
    }

    public List<TaxDeadline> getOverdueDeadlines() {
        return overdueDeadlines;
    }

    public void setOverdueDeadlines(List<TaxDeadline> overdueDeadlines) {
        this.overdueDeadlines = overdueDeadlines;
    }

    public List<TaxDeadline> getUpcomingDeadlines() {
        return upcomingDeadlines;
    }

    public void setUpcomingDeadlines(List<TaxDeadline> upcomingDeadlines) {
        this.upcomingDeadlines = upcomingDeadlines;
    }

    public TaxDeadline getTaxDeadline() {
        return taxDeadline;
    }

    public void setTaxDeadline(TaxDeadline taxDeadline) {
        this.taxDeadline = taxDeadline;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFilterType() {
        return filterType;
    }

    public void setFilterType(String filterType) {
        this.filterType = filterType;
    }

    public String getFilterStatus() {
        return filterStatus;
    }

    public void setFilterStatus(String filterStatus) {
        this.filterStatus = filterStatus;
    }

    public Map<String, Object> getJsonResponse() {
        return jsonResponse;
    }

    public void setJsonResponse(Map<String, Object> jsonResponse) {
        this.jsonResponse = jsonResponse;
    }

    public DeadlineType[] getDeadlineTypes() {
        return DeadlineType.values();
    }

    public DeadlineStatus[] getDeadlineStatuses() {
        return DeadlineStatus.values();
    }

    public Frequency[] getFrequencies() {
        return Frequency.values();
    }
}
