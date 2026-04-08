package it.zensoftware.luna2.model;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Entity
@Table(
    name = "prodotti_fornitori_listini",
    uniqueConstraints = @UniqueConstraint(name = "uk_prodotto_fornitore", columnNames = {"prodotto_id", "fornitore_id"})
)
public class ProdottoFornitoreListino implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prodotto_id", nullable = false)
    private Prodotto prodotto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fornitore_id", nullable = false)
    private Fornitore fornitore;

    @Column(name = "prezzo_acquisto", precision = 15, scale = 2, nullable = false)
    private BigDecimal prezzoAcquisto;

    @Column(name = "codice_articolo_fornitore", length = 100)
    private String codiceArticoloFornitore;

    @Column(nullable = false)
    private Boolean attivo = true;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "data_creazione", updatable = false)
    private Date dataCreazione;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "data_modifica")
    private Date dataModifica;

    @PrePersist
    protected void onCreate() {
        dataCreazione = new Date();
        dataModifica = new Date();
    }

    @PreUpdate
    protected void onUpdate() {
        dataModifica = new Date();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Prodotto getProdotto() { return prodotto; }
    public void setProdotto(Prodotto prodotto) { this.prodotto = prodotto; }
    public Fornitore getFornitore() { return fornitore; }
    public void setFornitore(Fornitore fornitore) { this.fornitore = fornitore; }
    public BigDecimal getPrezzoAcquisto() { return prezzoAcquisto; }
    public void setPrezzoAcquisto(BigDecimal prezzoAcquisto) { this.prezzoAcquisto = prezzoAcquisto; }
    public String getCodiceArticoloFornitore() { return codiceArticoloFornitore; }
    public void setCodiceArticoloFornitore(String codiceArticoloFornitore) { this.codiceArticoloFornitore = codiceArticoloFornitore; }
    public Boolean getAttivo() { return attivo; }
    public void setAttivo(Boolean attivo) { this.attivo = attivo; }
    public Date getDataCreazione() { return dataCreazione; }
    public void setDataCreazione(Date dataCreazione) { this.dataCreazione = dataCreazione; }
    public Date getDataModifica() { return dataModifica; }
    public void setDataModifica(Date dataModifica) { this.dataModifica = dataModifica; }
}
