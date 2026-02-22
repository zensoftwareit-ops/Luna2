package it.zensoftware.luna2.service;

import it.zensoftware.luna2.dao.PickingListDAO;
import it.zensoftware.luna2.dao.PickingItemDAO;
import it.zensoftware.luna2.dao.GiacenzaDAO;
import it.zensoftware.luna2.dao.PosizioneDAO;
import it.zensoftware.luna2.model.PickingList;
import it.zensoftware.luna2.model.PickingList.StatoPicking;
import it.zensoftware.luna2.model.PickingItem;
import it.zensoftware.luna2.model.PickingItem.StatoItem;
import it.zensoftware.luna2.model.Ordine;
import it.zensoftware.luna2.model.OrdineRiga;
import it.zensoftware.luna2.model.Giacenza;
import it.zensoftware.luna2.model.Posizione;
import it.zensoftware.luna2.model.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;

/**
 * Service per la gestione delle picking lists e del workflow di prelievo
 */
public class PickingService {
    
    private static final Logger logger = LogManager.getLogger(PickingService.class);
    
    private final PickingListDAO pickingListDAO;
    private final PickingItemDAO pickingItemDAO;
    private final GiacenzaDAO giacenzaDAO;
    private final PosizioneDAO posizioneDAO;
    private final GiacenzeService giacenzeService;
    
    public PickingService() {
        this.pickingListDAO = new PickingListDAO();
        this.pickingItemDAO = new PickingItemDAO();
        this.giacenzaDAO = new GiacenzaDAO();
        this.posizioneDAO = new PosizioneDAO();
        this.giacenzeService = new GiacenzeService();
    }

    /**
     * Crea picking list da ordine
     */
    public PickingList creaPickingListDaOrdine(Ordine ordine, Long warehouseId, User utente) {
        logger.info("Creating picking list for ordine: {}", ordine.getId());
        
        if (ordine.getRighe() == null || ordine.getRighe().isEmpty()) {
            throw new IllegalArgumentException("Ordine senza righe");
        }
        
        // Crea picking list
        PickingList pickingList = new PickingList();
        pickingList.setNumero(generaNumeroPicking());
        pickingList.setOrdine(ordine);
        pickingList.setWarehouse(ordine.getWarehouse());
        if (warehouseId != null) {
            it.zensoftware.luna2.model.Warehouse warehouse = new it.zensoftware.luna2.model.Warehouse();
            warehouse.setId(warehouseId);
            pickingList.setWarehouse(warehouse);
        }
        pickingList.setStato(StatoPicking.DRAFT);
        pickingList.setDataCreazione(new Date());
        pickingList.setCreatoDa(utente);
        pickingList.setPriorita(ordine.getPriorita() != null ? ordine.getPriorita() : 1);
        
        // Salva picking list
        pickingList = pickingListDAO.save(pickingList);
        
        // Crea items
        int numeroLinea = 1;
        for (OrdineRiga riga : ordine.getRighe()) {
            if (riga.getProdotto() == null || riga.getQuantita() == null) {
                continue;
            }
            
            // Trova giacenza con disponibilità
            Giacenza giacenza = giacenzaDAO.findByWarehouseAndProdotto(
                pickingList.getWarehouse().getId(), 
                riga.getProdotto().getId()
            );
            
            if (giacenza == null || giacenza.getQuantitaDisponibile().compareTo(riga.getQuantita()) < 0) {
                logger.warn("Insufficient stock for product: {} - required: {}, available: {}", 
                           riga.getProdotto().getId(), riga.getQuantita(), 
                           giacenza != null ? giacenza.getQuantitaDisponibile() : BigDecimal.ZERO);
            }
            
            // Crea picking item
            PickingItem item = new PickingItem();
            item.setPickingList(pickingList);
            item.setProdotto(riga.getProdotto());
            item.setOrdineRiga(riga);
            item.setNumeroLinea(numeroLinea++);
            item.setQuantitaRichiesta(riga.getQuantita());
            item.setQuantitaPrelevata(BigDecimal.ZERO);
            item.setStato(StatoItem.PENDING);
            item.setGiacenza(giacenza);
            
            if (giacenza != null && giacenza.getPosizionePrincipale() != null) {
                item.setPosizione(giacenza.getPosizionePrincipale());
            }
            
            pickingItemDAO.save(item);
        }
        
        logger.info("Picking list created: {}", pickingList.getNumero());
        return pickingList;
    }

    /**
     * Assegna picking ad utente
     */
    public PickingList assegnaPicking(Long pickingListId, Long utenteId, User assegnatoDa) {
        logger.info("Assigning picking list {} to user {}", pickingListId, utenteId);
        
        PickingList pickingList = pickingListDAO.findById(pickingListId);
        if (pickingList == null) {
            throw new IllegalArgumentException("Picking list non trovata");
        }
        
        if (pickingList.getStato() != StatoPicking.DRAFT && 
            pickingList.getStato() != StatoPicking.ASSEGNATO) {
            throw new IllegalStateException("Picking list non assegnabile - stato: " + pickingList.getStato());
        }
        
        User utente = new User();
        utente.setId(utenteId);
        
        pickingList.setAssegnatoA(utente);
        pickingList.setStato(StatoPicking.ASSEGNATO);
        pickingList.setDataAssegnazione(new Date());
        
        pickingList = pickingListDAO.save(pickingList);
        
        logger.info("Picking list assigned successfully");
        return pickingList;
    }

    /**
     * Avvia picking
     */
    public PickingList avviaPicking(Long pickingListId, User utente) {
        logger.info("Starting picking list: {}", pickingListId);
        
        PickingList pickingList = pickingListDAO.findWithItems(pickingListId);
        if (pickingList == null) {
            throw new IllegalArgumentException("Picking list non trovata");
        }
        
        if (!pickingList.avviaPicking(utente)) {
            throw new IllegalStateException("Impossibile avviare picking - stato: " + pickingList.getStato());
        }
        
        // Impegna quantità per tutti gli items
        for (PickingItem item : pickingList.getItems()) {
            if (item.getGiacenza() != null) {
                try {
                    giacenzeService.impegnaQuantita(
                        item.getGiacenza().getWarehouse().getId(),
                        item.getProdotto().getId(),
                        item.getQuantitaRichiesta(),
                        utente
                    );
                    item.setStato(StatoItem.IN_PICKING);
                    pickingItemDAO.save(item);
                } catch (Exception e) {
                    logger.error("Error reserving stock for item: " + item.getId(), e);
                }
            }
        }
        
        pickingList = pickingListDAO.save(pickingList);
        
        logger.info("Picking started successfully");
        return pickingList;
    }

    /**
     * Registra prelievo di un item con barcode
     */
    public PickingItem registraPrelievo(Long pickingListId, String barcode, 
                                       BigDecimal quantita, User utente) {
        logger.info("Registering picking: list={}, barcode={}, quantita={}", 
                   pickingListId, barcode, quantita);
        
        // Trova item per barcode
        PickingItem item = pickingItemDAO.findByBarcode(pickingListId, barcode);
        if (item == null) {
            throw new IllegalArgumentException("Item non trovato per barcode: " + barcode);
        }
        
        if (item.getStato() != StatoItem.IN_PICKING && item.getStato() != StatoItem.PENDING) {
            throw new IllegalStateException("Item non prelevabile - stato: " + item.getStato());
        }
        
        // Valida quantità
        if (quantita.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantità deve essere maggiore di zero");
        }
        
        BigDecimal quantitaRimanente = item.getQuantitaRichiesta().subtract(item.getQuantitaPrelevata());
        if (quantita.compareTo(quantitaRimanente) > 0) {
            throw new IllegalArgumentException(
                "Quantità eccede il richiesto: prelevata=" + quantita + 
                ", rimanente=" + quantitaRimanente);
        }
        
        // Registra prelievo nell'item
        item.registraPrelievo(quantita, utente, barcode);
        item = pickingItemDAO.save(item);
        
        // Aggiorna picking list
        PickingList pickingList = pickingListDAO.findWithItems(pickingListId);
        pickingList.calcolaPercentualeCompletamento();
        pickingListDAO.save(pickingList);
        
        logger.info("Picking registered: item={}, prelevata={}/{}", 
                   item.getId(), item.getQuantitaPrelevata(), item.getQuantitaRichiesta());
        
        return item;
    }

    /**
     * Completa picking
     */
    public PickingList completaPicking(Long pickingListId, User utente) {
        logger.info("Completing picking list: {}", pickingListId);
        
        PickingList pickingList = pickingListDAO.findWithItems(pickingListId);
        if (pickingList == null) {
            throw new IllegalArgumentException("Picking list non trovata");
        }
        
        if (pickingList.getStato() != StatoPicking.IN_PROGRESS) {
            throw new IllegalStateException("Picking list non completabile - stato: " + pickingList.getStato());
        }
        
        // Verifica che tutti gli items siano completati o parziali
        boolean tuttiCompletati = true;
        boolean almenoUnoParziale = false;
        
        for (PickingItem item : pickingList.getItems()) {
            if (item.getStato() != StatoItem.PICKED && 
                item.getStato() != StatoItem.VERIFICATO) {
                tuttiCompletati = false;
            }
            
            if (item.getQuantitaPrelevata().compareTo(item.getQuantitaRichiesta()) < 0) {
                almenoUnoParziale = true;
            }
            
            // Scarica giacenza
            if (item.getQuantitaPrelevata().compareTo(BigDecimal.ZERO) > 0) {
                try {
                    giacenzeService.registraScarico(
                        item.getGiacenza().getWarehouse().getId(),
                        item.getProdotto().getId(),
                        item.getQuantitaPrelevata(),
                        "Picking " + pickingList.getNumero(),
                        utente
                    );
                } catch (Exception e) {
                    logger.error("Error scaricando giacenza for item: " + item.getId(), e);
                }
            }
            
            // Rilascia quantità non prelevata
            BigDecimal quantitaNonPrelevata = item.getQuantitaRichiesta().subtract(item.getQuantitaPrelevata());
            if (quantitaNonPrelevata.compareTo(BigDecimal.ZERO) > 0) {
                try {
                    giacenzeService.rilasciaQuantita(
                        item.getGiacenza().getWarehouse().getId(),
                        item.getProdotto().getId(),
                        quantitaNonPrelevata,
                        utente
                    );
                } catch (Exception e) {
                    logger.error("Error rilasciando giacenza for item: " + item.getId(), e);
                }
            }
        }
        
        // Completa picking list
        if (!pickingList.completaPicking(utente)) {
            throw new IllegalStateException("Impossibile completare picking");
        }
        
        if (almenoUnoParziale) {
            pickingList.setStato(StatoPicking.PARZIALE);
        }
        
        pickingList = pickingListDAO.save(pickingList);
        
        logger.info("Picking completed: {}", pickingList.getNumero());
        return pickingList;
    }

    /**
     * Annulla picking
     */
    public PickingList annullaPicking(Long pickingListId, String motivazione, User utente) {
        logger.info("Cancelling picking list: {}", pickingListId);
        
        PickingList pickingList = pickingListDAO.findWithItems(pickingListId);
        if (pickingList == null) {
            throw new IllegalArgumentException("Picking list non trovata");
        }
        
        if (pickingList.getStato() == StatoPicking.COMPLETATO || 
            pickingList.getStato() == StatoPicking.ANNULLATO) {
            throw new IllegalStateException("Picking list non annullabile - stato: " + pickingList.getStato());
        }
        
        // Rilascia tutte le quantità impegnate
        for (PickingItem item : pickingList.getItems()) {
            if (item.getGiacenza() != null && item.getStato() == StatoItem.IN_PICKING) {
                BigDecimal daRilasciare = item.getQuantitaRichiesta().subtract(item.getQuantitaPrelevata());
                if (daRilasciare.compareTo(BigDecimal.ZERO) > 0) {
                    try {
                        giacenzeService.rilasciaQuantita(
                            item.getGiacenza().getWarehouse().getId(),
                            item.getProdotto().getId(),
                            daRilasciare,
                            utente
                        );
                    } catch (Exception e) {
                        logger.error("Error rilasciando giacenza for item: " + item.getId(), e);
                    }
                }
            }
            
            item.setStato(StatoItem.PENDING);
            pickingItemDAO.save(item);
        }
        
        pickingList.setStato(StatoPicking.ANNULLATO);
        pickingList.setNote(pickingList.getNote() != null ? 
                           pickingList.getNote() + "\nAnnullato: " + motivazione : 
                           "Annullato: " + motivazione);
        pickingList = pickingListDAO.save(pickingList);
        
        logger.info("Picking cancelled");
        return pickingList;
    }

    /**
     * Trova picking lists per warehouse
     */
    public List<PickingList> getPickingByWarehouse(Long warehouseId, StatoPicking stato) {
        return pickingListDAO.findByWarehouse(warehouseId, stato);
    }

    /**
     * Trova picking lists per utente
     */
    public List<PickingList> getPickingByUtente(Long utenteId, StatoPicking stato) {
        return pickingListDAO.findByUtente(utenteId, stato);
    }

    /**
     * Trova picking in attesa
     */
    public List<PickingList> getPickingInAttesa(Long warehouseId) {
        return pickingListDAO.findInAttesa(warehouseId);
    }

    /**
     * Trova picking in corso
     */
    public List<PickingList> getPickingInCorso(Long warehouseId) {
        return pickingListDAO.findInCorso(warehouseId);
    }

    /**
     * Carica picking con items
     */
    public PickingList getPickingWithItems(Long pickingListId) {
        return pickingListDAO.findWithItems(pickingListId);
    }

    /**
     * Carica items di un picking
     */
    public List<PickingItem> getItemsByPicking(Long pickingListId) {
        return pickingItemDAO.findByPickingList(pickingListId);
    }

    /**
     * Cerca picking per numero
     */
    public PickingList findByNumero(String numero) {
        return pickingListDAO.findByNumero(numero);
    }

    /**
     * Genera numero picking automatico
     */
    private String generaNumeroPicking() {
        return "PICK-" + System.currentTimeMillis();
    }

    /**
     * Salva picking list
     */
    public PickingList save(PickingList pickingList) {
        return pickingListDAO.save(pickingList);
    }

    /**
     * Carica picking per ID
     */
    public PickingList findById(Long id) {
        return pickingListDAO.findById(id);
    }
}
