package it.zensoftware.luna2.model;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Entity per i componenti di un prodotto composto
 */
@Entity
@Table(name = "prodotto_componenti")
public class ProdottoComponente implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prodotto_id", nullable = false)
    private Prodotto prodotto;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "componente_id", nullable = false)
    private Prodotto componente;

    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal quantita = BigDecimal.ONE;

    @Column(name = "prezzo_override", precision = 15, scale = 2)
    private BigDecimal prezzoOverride;

    @Column(length = 200)
    private String note;

    // Constructors
    public ProdottoComponente() {}

    public ProdottoComponente(Prodotto prodotto, Prodotto componente, BigDecimal quantita) {
        this.prodotto = prodotto;
        this.componente = componente;
        this.quantita = quantita;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Prodotto getProdotto() {
        return prodotto;
    }

    public void setProdotto(Prodotto prodotto) {
        this.prodotto = prodotto;
    }

    public Prodotto getComponente() {
        return componente;
    }

    public void setComponente(Prodotto componente) {
        this.componente = componente;
    }

    public BigDecimal getQuantita() {
        return quantita;
    }

    public void setQuantita(BigDecimal quantita) {
        this.quantita = quantita;
    }

    public BigDecimal getPrezzoOverride() {
        return prezzoOverride;
    }

    public void setPrezzoOverride(BigDecimal prezzoOverride) {
        this.prezzoOverride = prezzoOverride;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    /**
     * Calcola il prezzo totale del componente
     */
    public BigDecimal getPrezzoTotale() {
        BigDecimal prezzo = prezzoOverride != null ? prezzoOverride : 
                           (componente != null ? componente.getPrezzoBase() : BigDecimal.ZERO);
        return prezzo.multiply(quantita);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProdottoComponente)) return false;
        ProdottoComponente that = (ProdottoComponente) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
