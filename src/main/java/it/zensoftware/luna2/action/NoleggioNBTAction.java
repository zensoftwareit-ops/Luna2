package it.zensoftware.luna2.action;

import com.opensymphony.xwork2.ActionSupport;
import it.zensoftware.luna2.dao.NoleggioNBTDAO;
import it.zensoftware.luna2.model.NoleggioNBT;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NoleggioNBTAction extends ActionSupport {

    private static final Logger logger = LogManager.getLogger(NoleggioNBTAction.class);
    private static final long serialVersionUID = 1L;

    private final NoleggioNBTDAO nbtDAO = new NoleggioNBTDAO();

    private List<Map<String, Object>> nbts = new ArrayList<>();
    private Map<String, Object> jsonResponse = new HashMap<>();

    private Long id;

    // Form fields
    private String ragioneSociale;
    private String email;
    private String dataInizio;
    private String dataFine;
    private String marcaPreferita;
    private String modelloPreferito;

    public String index() {
        return SUCCESS;
    }

    public String list() {
        try {
            List<NoleggioNBT> items = nbtDAO.findAll();
            for (NoleggioNBT nbt : items) {
                Map<String, Object> row = new HashMap<>();
                row.put("id", nbt.getId());
                row.put("numeroPratica", nbt.getNumeroPratica());
                row.put("ragioneSociale", nbt.getNomeCliente());
                row.put("dataInizio", formatDate(nbt.getDataInizioNoleggio()));
                row.put("dataFine", formatDate(nbt.getDataFineNoleggio()));
                row.put("numeroGiorni", nbt.getNumeroGiorni());
                row.put("preventivoValidoFino", formatDate(nbt.getPreventivoValidoFino()));
                row.put("status", nbt.getStatus() != null ? nbt.getStatus().name() : "-");
                row.put("targaVeicoloAssegnato", nbt.getTargaAssegnata());
                nbts.add(row);
            }
            jsonResponse.put("nbts", nbts);
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error listing NBT", e);
            jsonResponse.put("nbts", new ArrayList<>());
            return ERROR;
        }
    }

    public String dashboard() {
        try {
            Map<String, Long> statusData = new HashMap<>();
            statusData.put("RICHIESTA", nbtDAO.countByStatus(NoleggioNBT.Status.RICHIESTA_RICEVUTA));
            statusData.put("CONFERMATA", nbtDAO.countByStatus(NoleggioNBT.Status.PRENOTAZIONE_CONFERMATA));
            long completed = nbtDAO.countByStatus(NoleggioNBT.Status.COMPLETATO);
            jsonResponse.put("statusData", statusData);
            jsonResponse.put("completedCount", completed);
            jsonResponse.put("totalRevenue", nbtDAO.calculateTotalRevenue());
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error loading NBT dashboard", e);
            return ERROR;
        }
    }

    public String requireingFollowup() {
        try {
            List<NoleggioNBT> items = nbtDAO.findRequiringFollowup24h();
            for (NoleggioNBT nbt : items) {
                Map<String, Object> row = new HashMap<>();
                row.put("numeroPratica", nbt.getNumeroPratica());
                row.put("ragioneSociale", nbt.getNomeCliente());
                nbts.add(row);
            }
            jsonResponse.put("nbts", nbts);
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error loading followup NBT", e);
            jsonResponse.put("nbts", new ArrayList<>());
            return ERROR;
        }
    }

    public String requireingReturn() {
        try {
            List<NoleggioNBT> items = nbtDAO.findRequiringReturnAlert();
            for (NoleggioNBT nbt : items) {
                Map<String, Object> row = new HashMap<>();
                row.put("numeroPratica", nbt.getNumeroPratica());
                row.put("targetaVeicoloAssegnato", nbt.getTargaAssegnata());
                nbts.add(row);
            }
            jsonResponse.put("nbts", nbts);
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error loading return NBT", e);
            jsonResponse.put("nbts", new ArrayList<>());
            return ERROR;
        }
    }

    public String save() {
        try {
            NoleggioNBT nbt = id != null ? nbtDAO.findById(id) : new NoleggioNBT();
            if (nbt == null) {
                nbt = new NoleggioNBT();
            }
            nbt.setNomeCliente(ragioneSociale);
            nbt.setEmail(email);
            if (dataInizio != null && !dataInizio.isEmpty()) {
                nbt.setDataInizioNoleggio(java.sql.Date.valueOf(dataInizio));
            }
            if (dataFine != null && !dataFine.isEmpty()) {
                nbt.setDataFineNoleggio(java.sql.Date.valueOf(dataFine));
            }
            nbt.setMarcaPreferita(marcaPreferita);
            nbt.setModelloPreferito(modelloPreferito);
            if (nbt.getClienteId() == null) {
                nbt.setClienteId(1L);
            }
            if (nbt.getTipologiaNoleggio() == null) {
                nbt.setTipologiaNoleggio(NoleggioNBT.TipologiaNoleggio.GIORNALIERO);
            }
            if (nbt.getCategoriaRichiesta() == null) {
                nbt.setCategoriaRichiesta(NoleggioNBT.CategoriaVeicolo.CITY_CAR);
            }
            if (nbt.getLuogoRitiro() == null) {
                nbt.setLuogoRitiro("Da definire");
            }
            if (nbt.getLuogoRiconsegna() == null) {
                nbt.setLuogoRiconsegna("Da definire");
            }
            if (nbt.getTelefono() == null) {
                nbt.setTelefono("-");
            }
            if (nbt.getNumeroGiorni() == null && nbt.getDataInizioNoleggio() != null && nbt.getDataFineNoleggio() != null) {
                long diff = nbt.getDataFineNoleggio().getTime() - nbt.getDataInizioNoleggio().getTime();
                nbt.setNumeroGiorni((int) Math.max(1, diff / (1000 * 60 * 60 * 24)));
            }
            if (nbt.getStatus() == null) {
                nbt.setStatus(NoleggioNBT.Status.RICHIESTA_RICEVUTA);
            }

            if (nbt.getId() == null) {
                nbtDAO.save(nbt);
            } else {
                nbtDAO.update(nbt);
            }
            jsonResponse.put("message", "NBT salvato");
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error saving NBT", e);
            jsonResponse.put("message", "Errore nel salvataggio NBT");
            return ERROR;
        }
    }

    public String view() {
        try {
            if (id == null) {
                jsonResponse.put("message", "ID mancante");
                return ERROR;
            }
            NoleggioNBT nbt = nbtDAO.findById(id);
            if (nbt == null) {
                jsonResponse.put("message", "NBT non trovato");
                return ERROR;
            }
            jsonResponse.put("id", nbt.getId());
            jsonResponse.put("numeroPratica", nbt.getNumeroPratica());
            jsonResponse.put("ragioneSociale", nbt.getNomeCliente());
            jsonResponse.put("email", nbt.getEmail());
            jsonResponse.put("dataInizio", formatDate(nbt.getDataInizioNoleggio()));
            jsonResponse.put("dataFine", formatDate(nbt.getDataFineNoleggio()));
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error viewing NBT", e);
            jsonResponse.put("message", "Errore nel caricamento NBT");
            return ERROR;
        }
    }

    private String formatDate(java.util.Date date) {
        if (date == null) {
            return "";
        }
        return new SimpleDateFormat("yyyy-MM-dd").format(date);
    }

    public List<Map<String, Object>> getNbts() {
        return nbts;
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

    public String getRagioneSociale() {
        return ragioneSociale;
    }

    public void setRagioneSociale(String ragioneSociale) {
        this.ragioneSociale = ragioneSociale;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDataInizio() {
        return dataInizio;
    }

    public void setDataInizio(String dataInizio) {
        this.dataInizio = dataInizio;
    }

    public String getDataFine() {
        return dataFine;
    }

    public void setDataFine(String dataFine) {
        this.dataFine = dataFine;
    }

    public String getMarcaPreferita() {
        return marcaPreferita;
    }

    public void setMarcaPreferita(String marcaPreferita) {
        this.marcaPreferita = marcaPreferita;
    }

    public String getModelloPreferito() {
        return modelloPreferito;
    }

    public void setModelloPreferito(String modelloPreferito) {
        this.modelloPreferito = modelloPreferito;
    }
}
