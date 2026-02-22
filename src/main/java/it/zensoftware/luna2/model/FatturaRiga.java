package it.zensoftware.luna2.model;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;

@Entity
@Table(name = "fatture_righe")
public class FatturaRiga implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fattura_id")
    private Fattura fattura;

    @Column(name = "riga_numero")
    private Integer rigaNumero;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prodotto_id")
    private Prodotto prodotto;

    @Column(columnDefinition = "TEXT")
    private String descrizione;

    @Column(precision = 15, scale = 3)
    private BigDecimal quantita = BigDecimal.ONE;

    @Column(name = "prezzo_unitario", precision = 15, scale = 2)
    private BigDecimal prezzoUnitario = BigDecimal.ZERO;

    @Column(name = "imponibile_riga", precision = 15, scale = 2)
    private BigDecimal imponibileRiga = BigDecimal.ZERO;

    @Column(name = "iva_percentuale", precision = 5, scale = 2)
    private BigDecimal ivaPercentuale = new BigDecimal("22.00");

    @Column(name = "totale_riga", precision = 15, scale = 2)
    private BigDecimal totaleRiga = BigDecimal.ZERO;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Fattura getFattura() { return fattura; }
    public void setFattura(Fattura fattura) { this.fattura = fattura; }

    public Integer getRigaNumero() { return rigaNumero; }
    public void setRigaNumero(Integer rigaNumero) { this.rigaNumero = rigaNumero; }

    public Prodotto getProdotto() { return prodotto; }
    public void setProdotto(Prodotto prodotto) { this.prodotto = prodotto; }

    public String getDescrizione() { return descrizione; }
    public void setDescrizione(String descrizione) { this.descrizione = descrizione; }

    public BigDecimal getQuantita() { return quantita; }
    public void setQuantita(BigDecimal quantita) { this.quantita = quantita; }

    public BigDecimal getPrezzoUnitario() { return prezzoUnitario; }
    public void setPrezzoUnitario(BigDecimal prezzoUnitario) { this.prezzoUnitario = prezzoUnitario; }

    public BigDecimal getImponibileRiga() { return imponibileRiga; }
    public void setImponibileRiga(BigDecimal imponibileRiga) { this.imponibileRiga = imponibileRiga; }

    public BigDecimal getIvaPercentuale() { return ivaPercentuale; }
    public void setIvaPercentuale(BigDecimal ivaPercentuale) { this.ivaPercentuale = ivaPercentuale; }

    public BigDecimal getTotaleRiga() { return totaleRiga; }
    public void setTotaleRiga(BigDecimal totaleRiga) { this.totaleRiga = totaleRiga; }
    
    // Alias per compatibilità
    public void setImportoTotale(BigDecimal importoTotale) {
        this.totaleRiga = importoTotale;
    }
    
    public BigDecimal getImportoTotale() {
        return totaleRiga;
    }
}
