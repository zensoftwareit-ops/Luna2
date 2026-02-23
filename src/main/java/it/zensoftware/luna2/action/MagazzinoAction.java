package it.zensoftware.luna2.action;

import com.opensymphony.xwork2.ActionSupport;
import com.opensymphony.xwork2.ActionContext;
import it.zensoftware.luna2.dao.MagazzinoDAO;
import it.zensoftware.luna2.dao.MovimentoMagazzinoDAO;
import it.zensoftware.luna2.dao.ProdottoDAO;
import it.zensoftware.luna2.model.Magazzino;
import it.zensoftware.luna2.model.MovimentoMagazzino;
import it.zensoftware.luna2.model.Prodotto;
import it.zensoftware.luna2.model.User;
import it.zensoftware.luna2.service.notification.EventPublisher;
import it.zensoftware.luna2.service.notification.event.NotificationEventFactory;
import it.zensoftware.luna2.util.BarcodeGenerator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class MagazzinoAction extends ActionSupport {
    private static final Logger logger = LogManager.getLogger(MagazzinoAction.class);
    
    private MagazzinoDAO magazzinoDAO = new MagazzinoDAO();
    private MovimentoMagazzinoDAO movimentoDAO = new MovimentoMagazzinoDAO();
    private ProdottoDAO prodottoDAO = new ProdottoDAO();
    
    // Lista giacenze
    private List<Magazzino> giacenze;
    private List<Magazzino> sottoScorta;
    private BigDecimal valoreTotale;
    
    // Lista prodotti (per stampa etichette)
    private List<Prodotto> prodotti;
    
    // Movimento
    private MovimentoMagazzino movimento;
    private Long prodottoId;
    private String tipoMovimento;
    private BigDecimal quantita;
    private String causale;
    private String note;
    private Date dataMovimento;
    private BigDecimal costoUnitario;
    
    // Storico
    private List<MovimentoMagazzino> movimenti;
    private Long movimentoId;
    
    // Scanner barcode
    private String barcodeInput;
    private Prodotto prodottoScansionato;
    private String scanResult;
    
    // PDF etichette
    private InputStream pdfStream;
    private String pdfFileName;
    private List<Long> prodottiSelezionati;
    private int copiePerEtichetta = 1;
    
    /**
     * Lista giacenze magazzino
     */
    public String list() {
        logger.info("MagazzinoAction.list() - START");
        try {
            logger.debug("Calling magazzinoDAO.findAll()");
            giacenze = magazzinoDAO.findAll();
            if (giacenze == null) {
                giacenze = new ArrayList<>();
            }
            logger.debug("Found {} giacenze", giacenze.size());
            
            logger.debug("Calling magazzinoDAO.findSottoScorta()");
            sottoScorta = magazzinoDAO.findSottoScorta();
            if (sottoScorta == null) {
                sottoScorta = new ArrayList<>();
            }
            logger.debug("Found {} prodotti sotto scorta", sottoScorta.size());
            
            logger.debug("Calling magazzinoDAO.getValoreTotaleMagazzino()");
            valoreTotale = magazzinoDAO.getValoreTotaleMagazzino();
            if (valoreTotale == null) {
                valoreTotale = BigDecimal.ZERO;
            }
            logger.debug("Valore totale: {}", valoreTotale);
            
            logger.info("MagazzinoAction.list() - SUCCESS");
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Errore durante il caricamento delle giacenze", e);
            addActionError("Errore durante il caricamento delle giacenze: " + e.getMessage());
            return ERROR;
        }
    }
    
    /**
     * Lista prodotti per stampa etichette
     */
    public String listProdotti() {
        logger.info("MagazzinoAction.listProdotti() - START");
        try {
            logger.debug("Calling prodottoDAO.findAllActive()");
            prodotti = prodottoDAO.findAllActive();
            if (prodotti == null) {
                prodotti = new ArrayList<>();
            }
            logger.debug("Found {} prodotti", prodotti.size());
            
            logger.info("MagazzinoAction.listProdotti() - SUCCESS");
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Errore durante il caricamento dei prodotti", e);
            addActionError("Errore durante il caricamento dei prodotti: " + e.getMessage());
            return ERROR;
        }
    }
    
    /**
     * Form nuovo movimento
     */
    public String nuovoMovimento() {
        try {
            if (prodottoId != null) {
                Prodotto prodotto = prodottoDAO.findById(prodottoId);
                if (prodotto != null) {
                    movimento = new MovimentoMagazzino();
                    movimento.setProdotto(prodotto);
                    movimento.setDataMovimento(new Date());
                }
            }
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Errore durante l'apertura del form movimento", e);
            addActionError("Errore durante l'apertura del form movimento");
            return ERROR;
        }
    }
    
    /**
     * Registra carico merce
     */
    public String carico() {
        try {
            if (prodottoId == null || quantita == null) {
                addActionError("Prodotto e quantità sono obbligatori");
                return INPUT;
            }
            
            Prodotto prodotto = prodottoDAO.findById(prodottoId);
            if (prodotto == null) {
                addActionError("Prodotto non trovato");
                return INPUT;
            }
            
            // Crea o recupera magazzino
            Magazzino magazzino = magazzinoDAO.getOrCreateByProdotto(prodotto);
            
            // Crea movimento
            MovimentoMagazzino mov = new MovimentoMagazzino();
            mov.setProdotto(prodotto);
            mov.setTipoMovimento(MovimentoMagazzino.TipoMovimento.CARICO);
            mov.setQuantita(quantita);
            mov.setCausale(causale != null ? causale : "Carico merce");
            mov.setNote(note);
            mov.setDataMovimento(dataMovimento != null ? dataMovimento : new Date());
            mov.setCostoUnitario(costoUnitario);
            mov.setGiacenzaPrima(magazzino.getGiacenzaAttuale());
            
            // Aggiorna giacenza
            BigDecimal nuovaGiacenza = magazzino.getGiacenzaAttuale().add(quantita);
            magazzino.setGiacenzaAttuale(nuovaGiacenza);
            magazzino.setGiacenzaDisponibile(nuovaGiacenza.subtract(magazzino.getGiacenzaImpegnata()));
            magazzino.setDataAggiornamento(new Date());
            
            mov.setGiacenzaDopo(nuovaGiacenza);
            
            // Aggiorna valore magazzino se c'è un costo unitario
            if (costoUnitario != null && costoUnitario.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal valore = nuovaGiacenza.multiply(costoUnitario);
                magazzino.setValoreMagazzino(valore);
            }
            
            // Imposta utente
            Map<String, Object> session = ActionContext.getContext().getSession();
            User user = (User) session.get("user");
            if (user != null) {
                mov.setCreatedBy(user);
            }
            
            // Salva
            movimentoDAO.saveMovimento(mov);
            magazzinoDAO.saveOrUpdate(magazzino);
            
            addActionMessage("Carico registrato con successo");
            logger.info("Carico registrato: Prodotto {} - Quantità: {}", prodotto.getCodice(), quantita);

                if (user != null) {
                EventPublisher.getInstance().publishEvent(
                    NotificationEventFactory.merceInMagazzino(
                        String.valueOf(user.getId()),
                        prodotto.getCodice(),
                        prodotto.getNome(),
                        quantita != null ? quantita.intValue() : 0,
                        prodotto.getFornitore() != null ? prodotto.getFornitore().getRagioneSociale() : "N/D"
                    )
                );
                }
            
            return SUCCESS;
            
        } catch (Exception e) {
            logger.error("Errore durante il carico merce", e);
            addActionError("Errore durante il carico merce: " + e.getMessage());
            return ERROR;
        }
    }
    
    /**
     * Registra scarico merce
     */
    public String scarico() {
        try {
            if (prodottoId == null || quantita == null) {
                addActionError("Prodotto e quantità sono obbligatori");
                return INPUT;
            }
            
            Prodotto prodotto = prodottoDAO.findById(prodottoId);
            if (prodotto == null) {
                addActionError("Prodotto non trovato");
                return INPUT;
            }
            
            // Recupera magazzino
            Magazzino magazzino = magazzinoDAO.findByProdotto(prodottoId);
            if (magazzino == null) {
                addActionError("Giacenza non trovata per questo prodotto");
                return INPUT;
            }
            
            // Verifica giacenza disponibile
            if (magazzino.getGiacenzaDisponibile().compareTo(quantita) < 0) {
                addActionError("Giacenza disponibile insufficiente. Disponibile: " + magazzino.getGiacenzaDisponibile());
                return INPUT;
            }
            
            // Crea movimento
            MovimentoMagazzino mov = new MovimentoMagazzino();
            mov.setProdotto(prodotto);
            mov.setTipoMovimento(MovimentoMagazzino.TipoMovimento.SCARICO);
            mov.setQuantita(quantita.negate()); // Negativo per scarico
            mov.setCausale(causale != null ? causale : "Scarico merce");
            mov.setNote(note);
            mov.setDataMovimento(dataMovimento != null ? dataMovimento : new Date());
            mov.setGiacenzaPrima(magazzino.getGiacenzaAttuale());
            
            // Aggiorna giacenza
            BigDecimal nuovaGiacenza = magazzino.getGiacenzaAttuale().subtract(quantita);
            magazzino.setGiacenzaAttuale(nuovaGiacenza);
            magazzino.setGiacenzaDisponibile(nuovaGiacenza.subtract(magazzino.getGiacenzaImpegnata()));
            magazzino.setDataAggiornamento(new Date());
            
            mov.setGiacenzaDopo(nuovaGiacenza);
            
            // Ricalcola valore magazzino
            if (magazzino.getValoreMagazzino() != null && magazzino.getGiacenzaAttuale().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal costoMedio = magazzino.getValoreMagazzino().divide(
                    magazzino.getGiacenzaAttuale().add(quantita), 
                    2, 
                    BigDecimal.ROUND_HALF_UP
                );
                BigDecimal nuovoValore = nuovaGiacenza.multiply(costoMedio);
                magazzino.setValoreMagazzino(nuovoValore);
            }
            
            // Imposta utente
            Map<String, Object> session = ActionContext.getContext().getSession();
            User user = (User) session.get("user");
            if (user != null) {
                mov.setCreatedBy(user);
            }
            
            // Salva
            movimentoDAO.saveMovimento(mov);
            magazzinoDAO.saveOrUpdate(magazzino);
            
            addActionMessage("Scarico registrato con successo");
            logger.info("Scarico registrato: Prodotto {} - Quantità: {}", prodotto.getCodice(), quantita);
            
            return SUCCESS;
            
        } catch (Exception e) {
            logger.error("Errore durante lo scarico merce", e);
            addActionError("Errore durante lo scarico merce: " + e.getMessage());
            return ERROR;
        }
    }
    
    /**
     * Storico movimenti per prodotto
     */
    public String storicoMovimenti() {
        try {
            if (prodottoId != null) {
                movimenti = movimentoDAO.findByProdotto(prodottoId, 100);
            } else {
                movimenti = movimentoDAO.findAll();
            }
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Errore durante il caricamento dello storico movimenti", e);
            addActionError("Errore durante il caricamento dello storico");
            return ERROR;
        }
    }
    
    /**
     * Scanner barcode - Form
     */
    public String scanner() {
        return SUCCESS;
    }
    
    /**
     * Cerca prodotto da barcode
     */
    public String cercaBarcode() {
        try {
            if (barcodeInput == null || barcodeInput.trim().isEmpty()) {
                scanResult = "ERROR: Codice barcode vuoto";
                return SUCCESS;
            }
            
            // Cerca per codice EAN
            prodottoScansionato = prodottoDAO.findByCodiceEan(barcodeInput.trim());
            
            // Se non trovato, cerca per codice prodotto
            if (prodottoScansionato == null) {
                prodottoScansionato = prodottoDAO.findByCodice(barcodeInput.trim());
            }
            
            if (prodottoScansionato != null) {
                scanResult = "SUCCESS";
                prodottoId = prodottoScansionato.getId();
            } else {
                scanResult = "NOT_FOUND: Prodotto non trovato";
            }
            
            return SUCCESS;
            
        } catch (Exception e) {
            logger.error("Errore durante la ricerca barcode", e);
            scanResult = "ERROR: " + e.getMessage();
            return ERROR;
        }
    }
    
    /**
     * Genera PDF etichette barcode
     */
    public String stampaEtichette() {
        try {
            List<Prodotto> prodottiDaStampare = new ArrayList<>();
            
            if (prodottiSelezionati != null && !prodottiSelezionati.isEmpty()) {
                // Stampa prodotti selezionati
                for (Long id : prodottiSelezionati) {
                    Prodotto p = prodottoDAO.findById(id);
                    if (p != null) {
                        prodottiDaStampare.add(p);
                    }
                }
            } else if (prodottoId != null) {
                // Stampa singolo prodotto
                Prodotto p = prodottoDAO.findById(prodottoId);
                if (p != null) {
                    prodottiDaStampare.add(p);
                }
            }
            
            if (prodottiDaStampare.isEmpty()) {
                addActionError("Nessun prodotto selezionato per la stampa");
                return ERROR;
            }
            
            // Genera PDF
            byte[] pdfBytes = BarcodeGenerator.generaEtichettePDF(prodottiDaStampare, copiePerEtichetta).toByteArray();
            pdfStream = new ByteArrayInputStream(pdfBytes);
            
            // Nome file
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
            pdfFileName = "etichette_" + sdf.format(new Date()) + ".pdf";
            
            return SUCCESS;
            
        } catch (Exception e) {
            logger.error("Errore durante la generazione delle etichette PDF", e);
            addActionError("Errore durante la generazione delle etichette: " + e.getMessage());
            return ERROR;
        }
    }
    
    /**
     * Stampa etichetta singola
     */
    public String stampaEtichettaSingola() {
        try {
            if (prodottoId == null) {
                addActionError("Prodotto non specificato");
                return ERROR;
            }
            
            Prodotto prodotto = prodottoDAO.findById(prodottoId);
            if (prodotto == null) {
                addActionError("Prodotto non trovato");
                return ERROR;
            }
            
            byte[] pdfBytes = BarcodeGenerator.generaEtichettaSingola(prodotto).toByteArray();
            pdfStream = new ByteArrayInputStream(pdfBytes);
            pdfFileName = "etichetta_" + (prodotto.getCodice() != null ? prodotto.getCodice() : prodotto.getId()) + ".pdf";
            
            return SUCCESS;
            
        } catch (Exception e) {
            logger.error("Errore durante la generazione dell'etichetta singola", e);
            addActionError("Errore durante la generazione dell'etichetta: " + e.getMessage());
            return ERROR;
        }
    }
    
    // Getters per le enumerazioni
    public MovimentoMagazzino.TipoMovimento[] getTipiMovimento() {
        return MovimentoMagazzino.TipoMovimento.values();
    }
    
    // Getters and Setters
    public List<Magazzino> getGiacenze() { return giacenze; }
    public List<Magazzino> getSottoScorta() { return sottoScorta; }
    public BigDecimal getValoreTotale() { return valoreTotale; }
    public List<Prodotto> getProdotti() { return prodotti; }
    public MovimentoMagazzino getMovimento() { return movimento; }
    public void setMovimento(MovimentoMagazzino movimento) { this.movimento = movimento; }
    public Long getProdottoId() { return prodottoId; }
    public void setProdottoId(Long prodottoId) { this.prodottoId = prodottoId; }
    public String getTipoMovimento() { return tipoMovimento; }
    public void setTipoMovimento(String tipoMovimento) { this.tipoMovimento = tipoMovimento; }
    public BigDecimal getQuantita() { return quantita; }
    public void setQuantita(BigDecimal quantita) { this.quantita = quantita; }
    public String getCausale() { return causale; }
    public void setCausale(String causale) { this.causale = causale; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public Date getDataMovimento() { return dataMovimento; }
    public void setDataMovimento(Date dataMovimento) { this.dataMovimento = dataMovimento; }
    public BigDecimal getCostoUnitario() { return costoUnitario; }
    public void setCostoUnitario(BigDecimal costoUnitario) { this.costoUnitario = costoUnitario; }
    public List<MovimentoMagazzino> getMovimenti() { return movimenti; }
    public Long getMovimentoId() { return movimentoId; }
    public void setMovimentoId(Long movimentoId) { this.movimentoId = movimentoId; }
    public String getBarcodeInput() { return barcodeInput; }
    public void setBarcodeInput(String barcodeInput) { this.barcodeInput = barcodeInput; }
    public Prodotto getProdottoScansionato() { return prodottoScansionato; }
    public String getScanResult() { return scanResult; }
    public InputStream getPdfStream() { return pdfStream; }
    public String getPdfFileName() { return pdfFileName; }
    public List<Long> getProdottiSelezionati() { return prodottiSelezionati; }
    public void setProdottiSelezionati(List<Long> prodottiSelezionati) { this.prodottiSelezionati = prodottiSelezionati; }
    public int getCopiePerEtichetta() { return copiePerEtichetta; }
    public void setCopiePerEtichetta(int copiePerEtichetta) { this.copiePerEtichetta = copiePerEtichetta; }
}
