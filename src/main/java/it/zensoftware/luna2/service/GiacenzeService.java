package it.zensoftware.luna2.service;

import it.zensoftware.luna2.dao.GiacenzaDAO;
import it.zensoftware.luna2.dao.WarehouseDAO;
import it.zensoftware.luna2.dao.PosizioneDAO;
import it.zensoftware.luna2.dao.MovimentoMagazzinoDAO;
import it.zensoftware.luna2.model.Giacenza;
import it.zensoftware.luna2.model.Warehouse;
import it.zensoftware.luna2.model.Posizione;
import it.zensoftware.luna2.model.Prodotto;
import it.zensoftware.luna2.model.MovimentoMagazzino;
import it.zensoftware.luna2.model.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Service per la gestione delle giacenze e dei movimenti di magazzino
 */
public class GiacenzeService {
    
    private static final Logger logger = LogManager.getLogger(GiacenzeService.class);
    
    private final GiacenzaDAO giacenzaDAO;
    private final WarehouseDAO warehouseDAO;
    private final PosizioneDAO posizioneDAO;
    private final MovimentoMagazzinoDAO movimentoDAO;
    
    public GiacenzeService() {
        this.giacenzaDAO = new GiacenzaDAO();
        this.warehouseDAO = new WarehouseDAO();
        this.posizioneDAO = new PosizioneDAO();
        this.movimentoDAO = new MovimentoMagazzinoDAO();
    }

    /**
     * Carica giacenza per warehouse e prodotto
     */
    public Giacenza getGiacenza(Long warehouseId, Long prodottoId) {
        return giacenzaDAO.findByWarehouseAndProdotto(warehouseId, prodottoId);
    }

    /**
     * Carica tutte le giacenze di un warehouse
     */
    public List<Giacenza> getGiacenzeByWarehouse(Long warehouseId) {
        return giacenzaDAO.findByWarehouse(warehouseId);
    }

    /**
     * Carica tutte le giacenze di un prodotto in tutti i warehouse
     */
    public List<Giacenza> getGiacenzeByProdotto(Long prodottoId) {
        return giacenzaDAO.findByProdotto(prodottoId);
    }

    /**
     * Carica le giacenze sotto scorta
     */
    public List<Giacenza> getGiacenzeSottoScorta(Long warehouseId) {
        List<Giacenza> giacenze = warehouseId != null ? 
            giacenzaDAO.findByWarehouse(warehouseId) : 
            giacenzaDAO.findAll();
        
        return giacenze.stream()
            .filter(Giacenza::isSottoScorta)
            .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Carica le giacenze da riordinare
     */
    public List<Giacenza> getGiacenzeDaRiordinare(Long warehouseId) {
        List<Giacenza> giacenze = warehouseId != null ? 
            giacenzaDAO.findByWarehouse(warehouseId) : 
            giacenzaDAO.findAll();
        
        return giacenze.stream()
            .filter(Giacenza::isDaRiordinare)
            .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Calcola il valore totale delle giacenze
     */
    public BigDecimal calcolaValoreTotale(Long warehouseId) {
        List<Giacenza> giacenze = warehouseId != null ? 
            giacenzaDAO.findByWarehouse(warehouseId) : 
            giacenzaDAO.findAll();
        
        return giacenze.stream()
            .map(Giacenza::getValoreGiacenza)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Registra un movimento di carico
     */
    public Giacenza registraCarico(Long warehouseId, Long prodottoId, 
                                   BigDecimal quantita, BigDecimal costoUnitario,
                                   Long posizioneId, String causale, User utente) {
        
        logger.info("Registering carico: warehouse={}, prodotto={}, quantita={}", 
                   warehouseId, prodottoId, quantita);
        
        if (quantita == null || quantita.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantità deve essere maggiore di zero");
        }
        
        // Carica o crea giacenza
        Giacenza giacenza = giacenzaDAO.findByWarehouseAndProdotto(warehouseId, prodottoId);
        if (giacenza == null) {
            giacenza = new Giacenza();
            Warehouse warehouse = warehouseDAO.findById(warehouseId);
            Prodotto prodotto = new Prodotto();
            prodotto.setId(prodottoId);
            giacenza.setWarehouse(warehouse);
            giacenza.setProdotto(prodotto);
            giacenza.setQuantitaAttuale(BigDecimal.ZERO);
            giacenza.setQuantitaDisponibile(BigDecimal.ZERO);
            giacenza.setQuantitaImpegnata(BigDecimal.ZERO);
            giacenza.setQuantitaInOrdine(BigDecimal.ZERO);
            giacenza.setCostoMedioPonderato(BigDecimal.ZERO);
            giacenza.setValoreGiacenza(BigDecimal.ZERO);
        }
        
        // Imposta posizione se specificata
        if (posizioneId != null) {
            Posizione posizione = posizioneDAO.findById(posizioneId);
            giacenza.setPosizione(posizione);
        }
        
        BigDecimal giacenzaPrima = giacenza.getQuantitaAttuale();
        
        // Aggiorna giacenza
        giacenza.aggiornaDopoMovimento(quantita, costoUnitario);
        giacenza = giacenzaDAO.save(giacenza);
        
        // Registra movimento
        MovimentoMagazzino movimento = new MovimentoMagazzino();
        movimento.setProdotto(giacenza.getProdotto());
        movimento.setTipoMovimento(MovimentoMagazzino.TipoMovimento.CARICO);
        movimento.setQuantita(quantita);
        movimento.setCostoUnitario(costoUnitario);
        movimento.setCausale(causale != null ? causale : "Carico merce");
        movimento.setGiacenzaPrima(giacenzaPrima);
        movimento.setGiacenzaDopo(giacenza.getQuantitaAttuale());
        movimento.setDataMovimento(new Date());
        movimento.setDataCreazione(new Date());
        movimento.setCreatedBy(utente);
        movimento.setNote("");
        movimentoDAO.save(movimento);
        
        logger.info("Carico registered: new stock={}, avg cost={}", 
                   giacenza.getQuantitaAttuale(), giacenza.getCostoMedioPonderato());
        
        return giacenza;
    }

    /**
     * Registra un movimento di scarico
     */
    public Giacenza registraScarico(Long warehouseId, Long prodottoId, 
                                    BigDecimal quantita, String causale, User utente) {
        
        logger.info("Registering scarico: warehouse={}, prodotto={}, quantita={}", 
                   warehouseId, prodottoId, quantita);
        
        if (quantita == null || quantita.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantità deve essere maggiore di zero");
        }
        
        // Carica giacenza
        Giacenza giacenza = giacenzaDAO.findByWarehouseAndProdotto(warehouseId, prodottoId);
        if (giacenza == null) {
            throw new IllegalStateException("Giacenza non trovata");
        }
        
        // Verifica disponibilità
        if (giacenza.getQuantitaDisponibile().compareTo(quantita) < 0) {
            throw new IllegalStateException(
                "Quantità disponibile insufficiente: richiesta=" + quantita + 
                ", disponibile=" + giacenza.getQuantitaDisponibile());
        }
        
        BigDecimal giacenzaPrima = giacenza.getQuantitaAttuale();
        
        // Aggiorna quantità
        giacenza.aggiornaDopoMovimento(quantita.negate(), null);
        giacenza = giacenzaDAO.save(giacenza);
        
        // Registra movimento
        MovimentoMagazzino movimento = new MovimentoMagazzino();
        movimento.setProdotto(giacenza.getProdotto());
        movimento.setTipoMovimento(MovimentoMagazzino.TipoMovimento.SCARICO);
        movimento.setQuantita(quantita);
        movimento.setCostoUnitario(giacenza.getCostoMedioPonderato());
        movimento.setCausale(causale != null ? causale : "Scarico merce");
        movimento.setGiacenzaPrima(giacenzaPrima);
        movimento.setGiacenzaDopo(giacenza.getQuantitaAttuale());
        movimento.setDataMovimento(new Date());
        movimento.setDataCreazione(new Date());
        movimento.setCreatedBy(utente);
        movimentoDAO.save(movimento);
        
        logger.info("Scarico registered: new stock={}", giacenza.getQuantitaAttuale());
        
        return giacenza;
    }

    /**
     * Trasferisce giacenza tra warehouse
     */
    public void trasferisci(Long warehouseOrigineId, Long warehouseDestinazioneId,
                           Long prodottoId, BigDecimal quantita, String note, User utente) {
        
        logger.info("Transferring stock: from={}, to={}, prodotto={}, quantita={}", 
                   warehouseOrigineId, warehouseDestinazioneId, prodottoId, quantita);
        
        if (quantita == null || quantita.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantità deve essere maggiore di zero");
        }
        
        if (warehouseOrigineId.equals(warehouseDestinazioneId)) {
            throw new IllegalArgumentException("Warehouse origine e destinazione devono essere diversi");
        }
        
        // Scarica da origine
        Giacenza giacenzaOrigine = giacenzaDAO.findByWarehouseAndProdotto(warehouseOrigineId, prodottoId);
        if (giacenzaOrigine == null) {
            throw new IllegalStateException("Giacenza origine non trovata");
        }
        
        if (giacenzaOrigine.getQuantitaDisponibile().compareTo(quantita) < 0) {
            throw new IllegalStateException(
                "Quantità disponibile insufficiente nel warehouse origine");
        }
        
        BigDecimal costoMedio = giacenzaOrigine.getCostoMedioPonderato();
        BigDecimal giacenzaOrigPrima = giacenzaOrigine.getQuantitaAttuale();
        
        // Scarico da origine
        giacenzaOrigine.aggiornaDopoMovimento(quantita.negate(), null);
        giacenzaDAO.save(giacenzaOrigine);
        
        // Movimento uscita
        MovimentoMagazzino movimentoUscita = new MovimentoMagazzino();
        movimentoUscita.setProdotto(giacenzaOrigine.getProdotto());
        movimentoUscita.setTipoMovimento(MovimentoMagazzino.TipoMovimento.TRASFERIMENTO_USCITA);
        movimentoUscita.setQuantita(quantita);
        movimentoUscita.setCostoUnitario(costoMedio);
        movimentoUscita.setCausale("Trasferimento a warehouse " + warehouseDestinazioneId);
        movimentoUscita.setGiacenzaPrima(giacenzaOrigPrima);
        movimentoUscita.setGiacenzaDopo(giacenzaOrigine.getQuantitaAttuale());
        movimentoUscita.setDataMovimento(new Date());
        movimentoUscita.setDataCreazione(new Date());
        movimentoUscita.setCreatedBy(utente);
        movimentoDAO.save(movimentoUscita);
        
        // Carico in destinazione
        Giacenza giacenzaDestinazione = giacenzaDAO.findByWarehouseAndProdotto(warehouseDestinazioneId, prodottoId);
        if (giacenzaDestinazione == null) {
            giacenzaDestinazione = new Giacenza();
            Warehouse warehouse = warehouseDAO.findById(warehouseDestinazioneId);
            giacenzaDestinazione.setWarehouse(warehouse);
            giacenzaDestinazione.setProdotto(giacenzaOrigine.getProdotto());
            giacenzaDestinazione.setQuantitaAttuale(BigDecimal.ZERO);
            giacenzaDestinazione.setQuantitaDisponibile(BigDecimal.ZERO);
            giacenzaDestinazione.setQuantitaImpegnata(BigDecimal.ZERO);
            giacenzaDestinazione.setQuantitaInOrdine(BigDecimal.ZERO);
            giacenzaDestinazione.setCostoMedioPonderato(costoMedio);
            giacenzaDestinazione.setValoreGiacenza(BigDecimal.ZERO);
        }
        
        BigDecimal giacenzaDestPrima = giacenzaDestinazione.getQuantitaAttuale();
        
        giacenzaDestinazione.aggiornaDopoMovimento(quantita, costoMedio);
        giacenzaDAO.save(giacenzaDestinazione);
        
        // Movimento entrata
        MovimentoMagazzino movimentoEntrata = new MovimentoMagazzino();
        movimentoEntrata.setProdotto(giacenzaDestinazione.getProdotto());
        movimentoEntrata.setTipoMovimento(MovimentoMagazzino.TipoMovimento.TRASFERIMENTO_ENTRATA);
        movimentoEntrata.setQuantita(quantita);
        movimentoEntrata.setCostoUnitario(costoMedio);
        movimentoEntrata.setCausale("Trasferimento da warehouse " + warehouseOrigineId);
        movimentoEntrata.setGiacenzaPrima(giacenzaDestPrima);
        movimentoEntrata.setGiacenzaDopo(giacenzaDestinazione.getQuantitaAttuale());
        movimentoEntrata.setDataMovimento(new Date());
        movimentoEntrata.setDataCreazione(new Date());
        movimentoEntrata.setCreatedBy(utente);
        movimentoDAO.save(movimentoEntrata);
        
        logger.info("Transfer completed successfully");
    }

    /**
     * Impegna quantità per ordine
     */
    public void impegnaQuantita(Long warehouseId, Long prodottoId, 
                               BigDecimal quantita, User utente) {
        
        logger.info("Reserving stock: warehouse={}, prodotto={}, quantita={}", 
                   warehouseId, prodottoId, quantita);
        
        Giacenza giacenza = giacenzaDAO.findByWarehouseAndProdotto(warehouseId, prodottoId);
        if (giacenza == null) {
            throw new IllegalStateException("Giacenza non trovata");
        }
        
        if (giacenza.getQuantitaDisponibile().compareTo(quantita) < 0) {
            throw new IllegalStateException(
                "Quantità disponibile insufficiente");
        }
        
        BigDecimal impegnataAttuale = giacenza.getQuantitaImpegnata();
        giacenza.setQuantitaImpegnata(impegnataAttuale.add(quantita));
        giacenza.ricalcolaDisponibile();
        giacenzaDAO.save(giacenza);
        
        logger.info("Stock reserved successfully");
    }

    /**
     * Rilascia quantità impegnata
     */
    public void rilasciaQuantita(Long warehouseId, Long prodottoId, 
                                BigDecimal quantita, User utente) {
        
        logger.info("Releasing stock: warehouse={}, prodotto={}, quantita={}", 
                   warehouseId, prodottoId, quantita);
        
        Giacenza giacenza = giacenzaDAO.findByWarehouseAndProdotto(warehouseId, prodottoId);
        if (giacenza == null) {
            throw new IllegalStateException("Giacenza non trovata");
        }
        
        BigDecimal impegnataAttuale = giacenza.getQuantitaImpegnata();
        giacenza.setQuantitaImpegnata(impegnataAttuale.subtract(quantita));
        giacenza.ricalcolaDisponibile();
        giacenzaDAO.save(giacenza);
        
        logger.info("Stock released successfully");
    }

    /**
     * Ottiene disponibilità totale di un prodotto in tutti i warehouse
     */
    public BigDecimal getDisponibilitaTotale(Long prodottoId) {
        List<Giacenza> giacenze = giacenzaDAO.findByProdotto(prodottoId);
        return giacenze.stream()
            .map(Giacenza::getQuantitaDisponibile)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Genera report giacenze per warehouse
     */
    public Map<String, Object> generaReportGiacenze(Long warehouseId) {
        Map<String, Object> report = new HashMap<>();
        
        List<Giacenza> giacenze = giacenzaDAO.findByWarehouse(warehouseId);
        List<Giacenza> sottoScorta = getGiacenzeSottoScorta(warehouseId);
        List<Giacenza> daRiordinare = getGiacenzeDaRiordinare(warehouseId);
        BigDecimal valoreTotale = calcolaValoreTotale(warehouseId);
        
        report.put("totaleArticoli", giacenze.size());
        report.put("articoliSottoScorta", sottoScorta.size());
        report.put("articoliDaRiordinare", daRiordinare.size());
        report.put("valoreTotaleGiacenze", valoreTotale);
        report.put("giacenze", giacenze);
        report.put("sottoScorta", sottoScorta);
        report.put("daRiordinare", daRiordinare);
        
        return report;
    }

    /**
     * Salva giacenza
     */
    public Giacenza save(Giacenza giacenza) {
        return giacenzaDAO.save(giacenza);
    }

    /**
     * Carica giacenza per ID
     */
    public Giacenza findById(Long id) {
        return giacenzaDAO.findById(id);
    }
}

