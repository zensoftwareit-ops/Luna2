package it.zensoftware.luna2.model;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Entity
@Table(name = "magazzino")
public class Magazzino implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne
    @JoinColumn(name = "prodotto_id", unique = true)
    private Prodotto prodotto;
    @Column(name = "giacenza_attuale", precision = 15, scale = 3)
    private BigDecimal giacenzaAttuale = BigDecimal.ZERO;
    @Column(name = "giacenza_disponibile", precision = 15, scale = 3)
    private BigDecimal giacenzaDisponibile = BigDecimal.ZERO;
    @Column(name = "giacenza_impegnata", precision = 15, scale = 3)
    private BigDecimal giacenzaImpegnata = BigDecimal.ZERO;
    @Column(name = "giacenza_in_ordine", precision = 15, scale = 3)
    private BigDecimal giacenzaInOrdine = BigDecimal.ZERO;
    @Column(name = "valore_magazzino", precision = 15, scale = 2)
    private BigDecimal valoreMagazzino = BigDecimal.ZERO;
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "data_aggiornamento")
    private Date dataAggiornamento;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Prodotto getProdotto() { return prodotto; }
    public void setProdotto(Prodotto prodotto) { this.prodotto = prodotto; }
    public BigDecimal getGiacenzaAttuale() { return giacenzaAttuale; }
    public void setGiacenzaAttuale(BigDecimal giacenzaAttuale) { this.giacenzaAttuale = giacenzaAttuale; }
    public BigDecimal getGiacenzaDisponibile() { return giacenzaDisponibile; }
    public void setGiacenzaDisponibile(BigDecimal giacenzaDisponibile) { this.giacenzaDisponibile = giacenzaDisponibile; }
    public BigDecimal getGiacenzaImpegnata() { return giacenzaImpegnata; }
    public void setGiacenzaImpegnata(BigDecimal giacenzaImpegnata) { this.giacenzaImpegnata = giacenzaImpegnata; }
    public BigDecimal getGiacenzaInOrdine() { return giacenzaInOrdine; }
    public void setGiacenzaInOrdine(BigDecimal giacenzaInOrdine) { this.giacenzaInOrdine = giacenzaInOrdine; }
    public BigDecimal getValoreMagazzino() { return valoreMagazzino; }
    public void setValoreMagazzino(BigDecimal valoreMagazzino) { this.valoreMagazzino = valoreMagazzino; }
    public Date getDataAggiornamento() { return dataAggiornamento; }
    public void setDataAggiornamento(Date dataAggiornamento) { this.dataAggiornamento = dataAggiornamento; }
}
