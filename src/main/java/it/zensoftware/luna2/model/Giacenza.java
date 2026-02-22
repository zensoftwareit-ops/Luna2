package it.zensoftware.luna2.model;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Entity per le giacenze (stock) per warehouse e prodotto
 * Rappresenta la quantità disponibile di un prodotto in un magazzino specifico
 */
@Entity
@Table(name = "giacenze", 
    uniqueConstraints = @UniqueConstraint(columnNames = {"warehouse_id", "prodotto_id"}),
    indexes = {
        @Index(name = "idx_giacenze_warehouse", columnList = "warehouse_id"),
        @Index(name = "idx_giacenze_prodotto", columnList = "prodotto_id"),
        @Index(name = "idx_giacenze_quantita", columnList = "quantita_disponibile")
    }
)
public class Giacenza implements Serializable {
    
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prodotto_id", nullable = false)
    private Prodotto prodotto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "posizione_id")
    private Posizione posizione;

    @Column(name = "quantita_attuale", precision = 15, scale = 3, nullable = false)
    private BigDecimal quantitaAttuale = BigDecimal.ZERO;

    @Column(name = "quantita_disponibile", precision = 15, scale = 3, nullable = false)
    private BigDecimal quantitaDisponibile = BigDecimal.ZERO;

    @Column(name = "quantita_impegnata", precision = 15, scale = 3, nullable = false)
    private BigDecimal quantitaImpegnata = BigDecimal.ZERO;

    @Column(name = "quantita_in_ordine", precision = 15, scale = 3, nullable = false)
    private BigDecimal quantitaInOrdine = BigDecimal.ZERO;

    @Column(name = "quantita_minima", precision = 15, scale = 3)
    private BigDecimal quantitaMinima;

    @Column(name = "quantita_massima", precision = 15, scale = 3)
    private BigDecimal quantitaMassima;

    @Column(name = "punto_riordino", precision = 15, scale = 3)
    private BigDecimal puntoRiordino;

    @Column(name = "costo_medio_ponderato", precision = 15, scale = 4)
    private BigDecimal costoMedioPonderato;

    @Column(name = "valore_giacenza", precision = 15, scale = 2)
    private BigDecimal valoreGiacenza = BigDecimal.ZERO;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "data_ultimo_movimento")
    private Date dataUltimoMovimento;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "data_creazione")
    private Date dataCreazione;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "data_modifica")
    private Date dataModifica;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    // Costruttori
    public Giacenza() {
        this.dataCreazione = new Date();
    }

    public Giacenza(Warehouse warehouse, Prodotto prodotto) {
        this();
        this.warehouse = warehouse;
        this.prodotto = prodotto;
    }

    // Metodi di business logic
    @PreUpdate
    protected void onUpdate() {
        this.dataModifica = new Date();
        calcolaValoreGiacenza();
    }

    @PrePersist
    protected void onCreate() {
        calcolaValoreGiacenza();
    }

    /**
     * Calcola il valore della giacenza (quantità × costo medio ponderato)
     */
    public void calcolaValoreGiacenza() {
        if (quantitaAttuale != null && costoMedioPonderato != null) {
            this.valoreGiacenza = quantitaAttuale.multiply(costoMedioPonderato)
                .setScale(2, BigDecimal.ROUND_HALF_UP);
        } else {
            this.valoreGiacenza = BigDecimal.ZERO;
        }
    }

    /**
     * Ricalcola la quantità disponibile (attuale - impegnata)
     */
    public void ricalcolaDisponibile() {
        if (quantitaAttuale != null && quantitaImpegnata != null) {
            this.quantitaDisponibile = quantitaAttuale.subtract(quantitaImpegnata);
        } else {
            this.quantitaDisponibile = quantitaAttuale != null ? quantitaAttuale : BigDecimal.ZERO;
        }
    }

    /**
     * Verifica se la giacenza è sotto la quantità minima
     */
    public boolean isSottoScorta() {
        if (quantitaMinima == null) return false;
        return quantitaAttuale.compareTo(quantitaMinima) < 0;
    }

    /**
     * Verifica se la giacenza ha raggiunto il punto di riordino
     */
    public boolean isDaRiordinare() {
        if (puntoRiordino == null) return false;
        return quantitaAttuale.compareTo(puntoRiordino) <= 0;
    }

    /**
     * Verifica se la giacenza supera la quantità massima
     */
    public boolean isSoprascorta() {
        if (quantitaMassima == null) return false;
        return quantitaAttuale.compareTo(quantitaMassima) > 0;
    }

    /**
     * Aggiorna le quantità dopo un movimento
     */
    public void aggiornaDopoMovimento(BigDecimal deltaQuantita, BigDecimal costoUnitario) {
        this.quantitaAttuale = quantitaAttuale.add(deltaQuantita);
        ricalcolaDisponibile();
        
        // Aggiorna costo medio ponderato se fornito
        if (costoUnitario != null && deltaQuantita.compareTo(BigDecimal.ZERO) > 0) {
            aggiornaCostoMedioPonderato(deltaQuantita, costoUnitario);
        }
        
        this.dataUltimoMovimento = new Date();
        calcolaValoreGiacenza();
    }

    /**
     * Aggiorna il costo medio ponderato con un nuovo carico
     */
    public void aggiornaCostoMedioPonderato(BigDecimal quantitaNuova, BigDecimal costoNuovo) {
        if (costoMedioPonderato == null) {
            costoMedioPonderato = costoNuovo;
            return;
        }
        
        BigDecimal quantitaVecchia = quantitaAttuale.subtract(quantitaNuova);
        if (quantitaVecchia.compareTo(BigDecimal.ZERO) <= 0) {
            costoMedioPonderato = costoNuovo;
            return;
        }
        
        BigDecimal valoreVecchio = quantitaVecchia.multiply(costoMedioPonderato);
        BigDecimal valoreNuovo = quantitaNuova.multiply(costoNuovo);
        BigDecimal valoreTotale = valoreVecchio.add(valoreNuovo);
        
        this.costoMedioPonderato = valoreTotale.divide(quantitaAttuale, 4, BigDecimal.ROUND_HALF_UP);
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Warehouse getWarehouse() {
        return warehouse;
    }

    public void setWarehouse(Warehouse warehouse) {
        this.warehouse = warehouse;
    }

    public Prodotto getProdotto() {
        return prodotto;
    }

    public void setProdotto(Prodotto prodotto) {
        this.prodotto = prodotto;
    }

    public Posizione getPosizione() {
        return posizione;
    }

    public void setPosizione(Posizione posizione) {
        this.posizione = posizione;
    }

    public Posizione getPosizionePrincipale() {
        return posizione; // Alias per compatibilità
    }

    public BigDecimal getQuantitaAttuale() {
        return quantitaAttuale;
    }

    public void setQuantitaAttuale(BigDecimal quantitaAttuale) {
        this.quantitaAttuale = quantitaAttuale;
    }

    public BigDecimal getQuantitaDisponibile() {
        return quantitaDisponibile;
    }

    public void setQuantitaDisponibile(BigDecimal quantitaDisponibile) {
        this.quantitaDisponibile = quantitaDisponibile;
    }

    public BigDecimal getQuantitaImpegnata() {
        return quantitaImpegnata;
    }

    public void setQuantitaImpegnata(BigDecimal quantitaImpegnata) {
        this.quantitaImpegnata = quantitaImpegnata;
    }

    public BigDecimal getQuantitaInOrdine() {
        return quantitaInOrdine;
    }

    public void setQuantitaInOrdine(BigDecimal quantitaInOrdine) {
        this.quantitaInOrdine = quantitaInOrdine;
    }

    public BigDecimal getQuantitaMinima() {
        return quantitaMinima;
    }

    public void setQuantitaMinima(BigDecimal quantitaMinima) {
        this.quantitaMinima = quantitaMinima;
    }

    public BigDecimal getQuantitaMassima() {
        return quantitaMassima;
    }

    public void setQuantitaMassima(BigDecimal quantitaMassima) {
        this.quantitaMassima = quantitaMassima;
    }

    public BigDecimal getPuntoRiordino() {
        return puntoRiordino;
    }

    public void setPuntoRiordino(BigDecimal puntoRiordino) {
        this.puntoRiordino = puntoRiordino;
    }

    public BigDecimal getCostoMedioPonderato() {
        return costoMedioPonderato;
    }

    public void setCostoMedioPonderato(BigDecimal costoMedioPonderato) {
        this.costoMedioPonderato = costoMedioPonderato;
    }

    public BigDecimal getValoreGiacenza() {
        return valoreGiacenza;
    }

    public void setValoreGiacenza(BigDecimal valoreGiacenza) {
        this.valoreGiacenza = valoreGiacenza;
    }

    public Date getDataUltimoMovimento() {
        return dataUltimoMovimento;
    }

    public void setDataUltimoMovimento(Date dataUltimoMovimento) {
        this.dataUltimoMovimento = dataUltimoMovimento;
    }

    public Date getDataCreazione() {
        return dataCreazione;
    }

    public void setDataCreazione(Date dataCreazione) {
        this.dataCreazione = dataCreazione;
    }

    public Date getDataModifica() {
        return dataModifica;
    }

    public void setDataModifica(Date dataModifica) {
        this.dataModifica = dataModifica;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    @Override
    public String toString() {
        return "Giacenza{" +
                "id=" + id +
                ", warehouse=" + (warehouse != null ? warehouse.getNome() : "null") +
                ", prodotto=" + (prodotto != null ? prodotto.getCodice() : "null") +
                ", quantitaAttuale=" + quantitaAttuale +
                ", quantitaDisponibile=" + quantitaDisponibile +
                ", valoreGiacenza=" + valoreGiacenza +
                '}';
    }
}
