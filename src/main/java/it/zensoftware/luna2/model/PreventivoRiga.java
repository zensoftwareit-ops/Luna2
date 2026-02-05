package it.zensoftware.luna2.model;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Entity class for PreventivoRiga
 */
@Entity
@Table(name = "preventivi_righe")
public class PreventivoRiga implements Serializable {
    
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "preventivo_id", nullable = false)
    private Preventivo preventivo;

    @Column(name = "riga_numero", nullable = false)
    private Integer rigaNumero;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_riga", nullable = false)
    private TipoRiga tipoRiga = TipoRiga.PRODOTTO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prodotto_id")
    private Prodotto prodotto;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String descrizione;

    @Column(precision = 15, scale = 3)
    private BigDecimal quantita = BigDecimal.ONE;

    @Column(name = "unita_misura", length = 20)
    private String unitaMisura;

    @Column(name = "prezzo_unitario", precision = 15, scale = 2)
    private BigDecimal prezzoUnitario = BigDecimal.ZERO;

    @Column(name = "sconto_percentuale", precision = 5, scale = 2)
    private BigDecimal scontoPercentuale = BigDecimal.ZERO;

    @Column(name = "sconto_importo", precision = 15, scale = 2)
    private BigDecimal scontoImporto = BigDecimal.ZERO;

    @Column(name = "imponibile_riga", precision = 15, scale = 2)
    private BigDecimal imponibileRiga = BigDecimal.ZERO;

    @Column(name = "iva_percentuale", precision = 5, scale = 2)
    private BigDecimal ivaPercentuale = new BigDecimal("22.00");

    @Column(name = "iva_importo", precision = 15, scale = 2)
    private BigDecimal ivaImporto = BigDecimal.ZERO;

    @Column(name = "totale_riga", precision = 15, scale = 2)
    private BigDecimal totaleRiga = BigDecimal.ZERO;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "data_creazione", updatable = false)
    private Date dataCreazione;

    public enum TipoRiga {
        PRODOTTO, DESCRIZIONE, SUBTOTALE
    }

    @PrePersist
    protected void onCreate() {
        dataCreazione = new Date();
        calcolaTotale();
    }

    @PreUpdate
    protected void onUpdate() {
        calcolaTotale();
    }

    // Constructors
    public PreventivoRiga() {}

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Preventivo getPreventivo() {
        return preventivo;
    }

    public void setPreventivo(Preventivo preventivo) {
        this.preventivo = preventivo;
    }

    public Integer getRigaNumero() {
        return rigaNumero;
    }

    public void setRigaNumero(Integer rigaNumero) {
        this.rigaNumero = rigaNumero;
    }

    public TipoRiga getTipoRiga() {
        return tipoRiga;
    }

    public void setTipoRiga(TipoRiga tipoRiga) {
        this.tipoRiga = tipoRiga;
    }

    public Prodotto getProdotto() {
        return prodotto;
    }

    public void setProdotto(Prodotto prodotto) {
        this.prodotto = prodotto;
    }

    public String getDescrizione() {
        return descrizione;
    }

    public void setDescrizione(String descrizione) {
        this.descrizione = descrizione;
    }

    public BigDecimal getQuantita() {
        return quantita;
    }

    public void setQuantita(BigDecimal quantita) {
        this.quantita = quantita;
    }

    public String getUnitaMisura() {
        return unitaMisura;
    }

    public void setUnitaMisura(String unitaMisura) {
        this.unitaMisura = unitaMisura;
    }

    public BigDecimal getPrezzoUnitario() {
        return prezzoUnitario;
    }

    public void setPrezzoUnitario(BigDecimal prezzoUnitario) {
        this.prezzoUnitario = prezzoUnitario;
    }

    public BigDecimal getScontoPercentuale() {
        return scontoPercentuale;
    }

    public void setScontoPercentuale(BigDecimal scontoPercentuale) {
        this.scontoPercentuale = scontoPercentuale;
    }

    public BigDecimal getScontoImporto() {
        return scontoImporto;
    }

    public void setScontoImporto(BigDecimal scontoImporto) {
        this.scontoImporto = scontoImporto;
    }

    public BigDecimal getImponibileRiga() {
        return imponibileRiga;
    }

    public void setImponibileRiga(BigDecimal imponibileRiga) {
        this.imponibileRiga = imponibileRiga;
    }

    public BigDecimal getIvaPercentuale() {
        return ivaPercentuale;
    }

    public void setIvaPercentuale(BigDecimal ivaPercentuale) {
        this.ivaPercentuale = ivaPercentuale;
    }

    public BigDecimal getIvaImporto() {
        return ivaImporto;
    }

    public void setIvaImporto(BigDecimal ivaImporto) {
        this.ivaImporto = ivaImporto;
    }

    public BigDecimal getTotaleRiga() {
        return totaleRiga;
    }

    public void setTotaleRiga(BigDecimal totaleRiga) {
        this.totaleRiga = totaleRiga;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Date getDataCreazione() {
        return dataCreazione;
    }

    public void setDataCreazione(Date dataCreazione) {
        this.dataCreazione = dataCreazione;
    }

    public void calcolaTotale() {
        if (tipoRiga == TipoRiga.PRODOTTO && quantita != null && prezzoUnitario != null) {
            // Calcola imponibile
            BigDecimal importo = quantita.multiply(prezzoUnitario);
            
            // Applica sconto
            if (scontoPercentuale != null && scontoPercentuale.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal sconto = importo.multiply(scontoPercentuale).divide(new BigDecimal("100"), 2, BigDecimal.ROUND_HALF_UP);
                importo = importo.subtract(sconto);
                scontoImporto = sconto;
            } else if (scontoImporto != null && scontoImporto.compareTo(BigDecimal.ZERO) > 0) {
                importo = importo.subtract(scontoImporto);
            }
            
            this.imponibileRiga = importo;
            
            // Calcola IVA
            if (ivaPercentuale != null && ivaPercentuale.compareTo(BigDecimal.ZERO) > 0) {
                this.ivaImporto = importo.multiply(ivaPercentuale).divide(new BigDecimal("100"), 2, BigDecimal.ROUND_HALF_UP);
            } else {
                this.ivaImporto = BigDecimal.ZERO;
            }
            
            this.totaleRiga = imponibileRiga.add(ivaImporto);
        }
    }

    @Override
    public String toString() {
        return "PreventivoRiga{" +
                "id=" + id +
                ", descrizione='" + descrizione + '\'' +
                ", quantita=" + quantita +
                ", prezzoUnitario=" + prezzoUnitario +
                ", totaleRiga=" + totaleRiga +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PreventivoRiga)) return false;
        PreventivoRiga that = (PreventivoRiga) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
