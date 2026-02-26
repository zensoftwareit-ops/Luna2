package it.zensoftware.luna2.action;

import com.opensymphony.xwork2.ActionSupport;
import it.zensoftware.luna2.dao.NoleggioOrdineDAO;
import it.zensoftware.luna2.model.NoleggioOrdine;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NoleggioOrdineAction extends ActionSupport {

    private static final Logger logger = LogManager.getLogger(NoleggioOrdineAction.class);
    private static final long serialVersionUID = 1L;

    private final NoleggioOrdineDAO ordineDAO = new NoleggioOrdineDAO();

    private List<Map<String, Object>> ordini = new ArrayList<>();
    private Map<String, Object> jsonResponse = new HashMap<>();

    private Long id;
    private String statusFiltro;
    private String searchTerm;
    private String nota;

    // Form fields
    private String targa;
    private String marcaModello;
    private String vin;
    private String etaConsegna;
    private String noteConsegna;

    public String index() {
        return SUCCESS;
    }

    public String list() {
        try {
            List<NoleggioOrdine> items;
            if (statusFiltro != null && !statusFiltro.isEmpty()) {
                items = ordineDAO.findByStatus(NoleggioOrdine.Status.valueOf(statusFiltro));
            } else {
                items = ordineDAO.findAll();
            }

            for (NoleggioOrdine ordine : items) {
                if (!matchesSearch(ordine)) {
                    continue;
                }
                Map<String, Object> row = new HashMap<>();
                row.put("id", ordine.getId());
                row.put("numeroOrdine", ordine.getNumeroOrdine());
                row.put("targa", ordine.getVin() != null ? ordine.getVin() : "-");
                row.put("marcaModello", buildMarcaModello(ordine));
                row.put("etaConsegna", formatDate(ordine.getDataConsegnaStimata()));
                row.put("status", ordine.getStatus() != null ? ordine.getStatus().name() : "-");
                row.put("ultimaCareCall", formatDateTime(ordine.getDataUltimoCareCall()));
                ordini.add(row);
            }

            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error listing ordini", e);
            jsonResponse.put("message", "Errore nel caricamento ordini");
            return ERROR;
        }
    }

    public String requireingCareCall() {
        try {
            int count = ordineDAO.findRequiringCareCall().size();
            jsonResponse.put("count", count);
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error loading care call count", e);
            jsonResponse.put("count", 0);
            return ERROR;
        }
    }

    public String registerCareCall() {
        try {
            if (id == null) {
                jsonResponse.put("message", "ID mancante");
                return ERROR;
            }
            NoleggioOrdine ordine = ordineDAO.findById(id);
            if (ordine == null) {
                jsonResponse.put("message", "Ordine non trovato");
                return ERROR;
            }
            ordine.setDataUltimoCareCall(new Date());
            int giorni = ordine.getGiorniTraCareCall() != null ? ordine.getGiorniTraCareCall() : 25;
            ordine.setProssimoCareCallPrevisto(new Date(System.currentTimeMillis() + (giorni * 24L * 60 * 60 * 1000)));
            if (nota != null && !nota.isEmpty()) {
                String current = ordine.getNoteInterne() != null ? ordine.getNoteInterne() + "\n" : "";
                ordine.setNoteInterne(current + "Care call: " + nota);
            }
            ordineDAO.update(ordine);
            jsonResponse.put("message", "Care call registrata");
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error registering care call", e);
            jsonResponse.put("message", "Errore registrazione care call");
            return ERROR;
        }
    }

    public String view() {
        try {
            if (id == null) {
                jsonResponse.put("message", "ID mancante");
                return ERROR;
            }
            NoleggioOrdine ordine = ordineDAO.findById(id);
            if (ordine == null) {
                jsonResponse.put("message", "Ordine non trovato");
                return ERROR;
            }
            jsonResponse.put("id", ordine.getId());
            jsonResponse.put("targa", ordine.getVin());
            jsonResponse.put("marcaModello", buildMarcaModello(ordine));
            jsonResponse.put("vin", ordine.getVin());
            jsonResponse.put("etaConsegna", formatDate(ordine.getDataConsegnaStimata()));
            jsonResponse.put("noteConsegna", ordine.getNoteConsegna());
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error viewing ordine", e);
            jsonResponse.put("message", "Errore nel caricamento ordine");
            return ERROR;
        }
    }

    public String save() {
        try {
            NoleggioOrdine ordine = id != null ? ordineDAO.findById(id) : new NoleggioOrdine();
            if (ordine == null) {
                ordine = new NoleggioOrdine();
            }

            if (marcaModello != null && !marcaModello.isEmpty()) {
                String[] parts = marcaModello.trim().split(" ", 2);
                ordine.setMarca(parts[0]);
                ordine.setModello(parts.length > 1 ? parts[1] : parts[0]);
            }
            if (vin != null && !vin.isEmpty()) {
                ordine.setVin(vin);
            }
            if (targa != null && !targa.isEmpty() && (ordine.getVin() == null || ordine.getVin().isEmpty())) {
                ordine.setVin(targa);
            }
            if (etaConsegna != null && !etaConsegna.isEmpty()) {
                ordine.setDataConsegnaStimata(parseDate(etaConsegna));
            }
            if (noteConsegna != null) {
                ordine.setNoteConsegna(noteConsegna);
            }

            if (ordine.getNumeroOrdine() == null || ordine.getNumeroOrdine().isEmpty()) {
                ordine.setNumeroOrdine("ORD-" + System.currentTimeMillis());
            }

            if (ordine.getId() == null) {
                ordineDAO.save(ordine);
            } else {
                ordineDAO.update(ordine);
            }

            jsonResponse.put("message", "Ordine salvato");
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error saving ordine", e);
            jsonResponse.put("message", "Errore nel salvataggio ordine");
            return ERROR;
        }
    }

    private boolean matchesSearch(NoleggioOrdine ordine) {
        if (searchTerm == null || searchTerm.isEmpty()) {
            return true;
        }
        String value = searchTerm.toLowerCase();
        return (ordine.getNumeroOrdine() != null && ordine.getNumeroOrdine().toLowerCase().contains(value))
            || (ordine.getMarca() != null && ordine.getMarca().toLowerCase().contains(value))
            || (ordine.getModello() != null && ordine.getModello().toLowerCase().contains(value))
            || (ordine.getVin() != null && ordine.getVin().toLowerCase().contains(value));
    }

    private String buildMarcaModello(NoleggioOrdine ordine) {
        String marca = ordine.getMarca() != null ? ordine.getMarca() : "";
        String modello = ordine.getModello() != null ? ordine.getModello() : "";
        return (marca + " " + modello).trim();
    }

    private String formatDate(Date date) {
        if (date == null) {
            return "";
        }
        return new SimpleDateFormat("yyyy-MM-dd").format(date);
    }

    private String formatDateTime(Date date) {
        if (date == null) {
            return "";
        }
        return new SimpleDateFormat("dd/MM/yyyy HH:mm").format(date);
    }

    private Date parseDate(String value) throws ParseException {
        return new SimpleDateFormat("yyyy-MM-dd").parse(value);
    }

    public List<Map<String, Object>> getOrdini() {
        return ordini;
    }

    public Map<String, Object> getJsonResponse() {
        return jsonResponse;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStatusFiltro() {
        return statusFiltro;
    }

    public void setStatusFiltro(String statusFiltro) {
        this.statusFiltro = statusFiltro;
    }

    public String getSearchTerm() {
        return searchTerm;
    }

    public void setSearchTerm(String searchTerm) {
        this.searchTerm = searchTerm;
    }

    public String getNota() {
        return nota;
    }

    public void setNota(String nota) {
        this.nota = nota;
    }

    public String getTarga() {
        return targa;
    }

    public void setTarga(String targa) {
        this.targa = targa;
    }

    public String getMarcaModello() {
        return marcaModello;
    }

    public void setMarcaModello(String marcaModello) {
        this.marcaModello = marcaModello;
    }

    public String getVin() {
        return vin;
    }

    public void setVin(String vin) {
        this.vin = vin;
    }

    public String getEtaConsegna() {
        return etaConsegna;
    }

    public void setEtaConsegna(String etaConsegna) {
        this.etaConsegna = etaConsegna;
    }

    public String getNoteConsegna() {
        return noteConsegna;
    }

    public void setNoteConsegna(String noteConsegna) {
        this.noteConsegna = noteConsegna;
    }
}
