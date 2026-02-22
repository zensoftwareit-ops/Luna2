package it.zensoftware.luna2.model;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Entity per le righe (items) di una picking list
 * Rappresenta un singolo prodotto da prelevare
 */
@Entity
@Table(name = "picking_items",
    indexes = {
        @Index(name = "idx_picking_item_list", columnList = "picking_list_id"),
        @Index(name = "idx_picking_item_prodotto", columnList = "prodotto_id"),
        @Index(name = "idx_picking_item_stato", columnList = "stato")
    }
)
public class PickingItem implements Serializable {
    
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "picking_list_id", nullable = false)
    private PickingList pickingList;

    @Column(name = "numero_linea", nullable = false)
    private Integer numeroLinea;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "prodotto_id", nullable = false)
    private Prodotto prodotto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "giacenza_id")
    private Giacenza giacenza;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "posizione_id")
    private Posizione posizione;

    @Column(name = "quantita_richiesta", precision = 15, scale = 3, nullable = false)
    private BigDecimal quantitaRichiesta;

    @Column(name = "quantita_prelevata", precision = 15, scale = 3)
    private BigDecimal quantitaPrelevata = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatoItem stato = StatoItem.PENDING;

    @Column(name = "priorita_prelievo")
    private Integer prioritaPrelievo = 5;

    @Column(name = "barcode_scansionato", length = 100)
    private String barcodeScansionato;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "data_prelievo")
    private Date dataPrelievo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prelevato_da")
    private User prelevatoDa;

    @Column(columnDefinition = "TEXT")
    private String note;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ordine_riga_id")
    private OrdineRiga ordineRiga;

    /**
     * Stati di un picking item
     */
    public enum StatoItem {
        PENDING("In Attesa"),
        IN_PICKING("In Prelievo"),
        PICKED("Prelevato"),
        VERIFICATO("Verificato"),
        PARZIALE("Parzialmente Prelevato"),
        NON_DISPONIBILE("Non Disponibile"),
        ANNULLATO("Annullato");

        private final String descrizione;

        StatoItem(String descrizione) {
            this.descrizione = descrizione;
        }

        public String getDescrizione() {
            return descrizione;
        }
    }

    // Costruttori
    public PickingItem() {
    }

    public PickingItem(Prodotto prodotto, BigDecimal quantitaRichiesta) {
        this.prodotto = prodotto;
        this.quantitaRichiesta = quantitaRichiesta;
    }

    public PickingItem(Prodotto prodotto, BigDecimal quantitaRichiesta, Giacenza giacenza) {
        this(prodotto, quantitaRichiesta);
        this.giacenza = giacenza;
        if (giacenza != null && giacenza.getPosizione() != null) {
            this.posizione = giacenza.getPosizione();
        }
    }

    // Metodi di business logic
    /**
     * Verifica se l'item è completamente prelevato
     */
    public boolean isCompletato() {
        if (quantitaPrelevata == null || quantitaRichiesta == null) return false;
        return quantitaPrelevata.compareTo(quantitaRichiesta) >= 0;
    }

    /**
     * Verifica se l'item è parzialmente prelevato
     */
    public boolean isParziale() {
        if (quantitaPrelevata == null || quantitaRichiesta == null) return false;
        return quantitaPrelevata.compareTo(BigDecimal.ZERO) > 0 && 
               quantitaPrelevata.compareTo(quantitaRichiesta) < 0;
    }

    /**
     * Calcola la quantità rimanente da prelevare
     */
    public BigDecimal getQuantitaRimanente() {
        if (quantitaPrelevata == null) return quantitaRichiesta;
        BigDecimal rimanente = quantitaRichiesta.subtract(quantitaPrelevata);
        return rimanente.compareTo(BigDecimal.ZERO) > 0 ? rimanente : BigDecimal.ZERO;
    }

    /**
     * Calcola la percentuale di completamento
     */
    public int getPercentualeCompletamento() {
        if (quantitaRichiesta == null || quantitaRichiesta.compareTo(BigDecimal.ZERO) == 0) {
            return 0;
        }
        if (quantitaPrelevata == null) return 0;
        
        return quantitaPrelevata.multiply(new BigDecimal("100"))
            .divide(quantitaRichiesta, 0, BigDecimal.ROUND_HALF_UP)
            .intValue();
    }

    /**
     * Registra il prelievo di una quantità
     */
    public void registraPrelievo(BigDecimal quantita, User utente) {
        if (quantitaPrelevata == null) {
            quantitaPrelevata = BigDecimal.ZERO;
        }
        quantitaPrelevata = quantitaPrelevata.add(quantita);
        
        // Aggiorna stato
        if (isCompletato()) {
            this.stato = StatoItem.PICKED;
        } else if (isParziale()) {
            this.stato = StatoItem.PARZIALE;
        } else {
            this.stato = StatoItem.IN_PICKING;
        }
        
        this.dataPrelievo = new Date();
        this.prelevatoDa = utente;
    }

    /**
     * Registra il prelievo di una quantità con note/barcode
     */
    public void registraPrelievo(BigDecimal quantita, User utente, String barcode) {
        registraPrelievo(quantita, utente);
        if (barcode != null && !barcode.isEmpty()) {
            this.barcodeScansionato = barcode;
        }
    }

    /**
     * Verifica l'item dopo il prelievo
     */
    public void verifica(User utente) {
        if (this.stato == StatoItem.PICKED) {
            this.stato = StatoItem.VERIFICATO;
        }
    }

    /**
     * Marca l'item come non disponibile
     */
    public void marcaNonDisponibile(String motivo) {
        this.stato = StatoItem.NON_DISPONIBILE;
        if (motivo != null && !motivo.isEmpty()) {
            this.note = (this.note != null ? this.note + "\n" : "") + "Non disponibile: " + motivo;
        }
    }

    /**
     * Restituisce la posizione come stringa
     */
    public String getPosizioneString() {
        if (posizione != null) {
            return posizione.getPosizioneCompleta();
        }
        if (giacenza != null && giacenza.getPosizione() != null) {
            return giacenza.getPosizione().getPosizioneCompleta();
        }
        return "N/D";
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PickingList getPickingList() {
        return pickingList;
    }

    public void setPickingList(PickingList pickingList) {
        this.pickingList = pickingList;
    }

    public Integer getNumeroLinea() {
        return numeroLinea;
    }

    public void setNumeroLinea(Integer numeroLinea) {
        this.numeroLinea = numeroLinea;
    }

    public Prodotto getProdotto() {
        return prodotto;
    }

    public void setProdotto(Prodotto prodotto) {
        this.prodotto = prodotto;
    }

    public Giacenza getGiacenza() {
        return giacenza;
    }

    public void setGiacenza(Giacenza giacenza) {
        this.giacenza = giacenza;
    }

    public Posizione getPosizione() {
        return posizione;
    }

    public void setPosizione(Posizione posizione) {
        this.posizione = posizione;
    }

    public BigDecimal getQuantitaRichiesta() {
        return quantitaRichiesta;
    }

    public void setQuantitaRichiesta(BigDecimal quantitaRichiesta) {
        this.quantitaRichiesta = quantitaRichiesta;
    }

    public BigDecimal getQuantitaPrelevata() {
        return quantitaPrelevata;
    }

    public void setQuantitaPrelevata(BigDecimal quantitaPrelevata) {
        this.quantitaPrelevata = quantitaPrelevata;
    }

    public StatoItem getStato() {
        return stato;
    }

    public void setStato(StatoItem stato) {
        this.stato = stato;
    }

    public Integer getPrioritaPrelievo() {
        return prioritaPrelievo;
    }

    public void setPrioritaPrelievo(Integer prioritaPrelievo) {
        this.prioritaPrelievo = prioritaPrelievo;
    }

    public String getBarcodeScansionato() {
        return barcodeScansionato;
    }

    public void setBarcodeScansionato(String barcodeScansionato) {
        this.barcodeScansionato = barcodeScansionato;
    }

    public Date getDataPrelievo() {
        return dataPrelievo;
    }

    public void setDataPrelievo(Date dataPrelievo) {
        this.dataPrelievo = dataPrelievo;
    }

    public User getPrelevatoDa() {
        return prelevatoDa;
    }

    public void setPrelevatoDa(User prelevatoDa) {
        this.prelevatoDa = prelevatoDa;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public OrdineRiga getOrdineRiga() {
        return ordineRiga;
    }

    public void setOrdineRiga(OrdineRiga ordineRiga) {
        this.ordineRiga = ordineRiga;
    }

    @Override
    public String toString() {
        return "PickingItem{" +
                "id=" + id +
                ", numeroLinea=" + numeroLinea +
                ", prodotto=" + (prodotto != null ? prodotto.getCodice() : "null") +
                ", quantitaRichiesta=" + quantitaRichiesta +
                ", quantitaPrelevata=" + quantitaPrelevata +
                ", stato=" + stato +
                ", posizione=" + getPosizioneString() +
                '}';
    }
}
