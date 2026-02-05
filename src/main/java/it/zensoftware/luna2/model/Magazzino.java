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

@Entity
@Table(name = "movimenti_magazzino")
class MovimentoMagazzino implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prodotto_id")
    private Prodotto prodotto;
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_movimento")
    private TipoMovimento tipoMovimento;
    private String causale;
    @Column(precision = 15, scale = 3)
    private BigDecimal quantita;
    @Column(name = "costo_unitario", precision = 15, scale = 2)
    private BigDecimal costoUnitario;
    @Column(name = "giacenza_dopo", precision = 15, scale = 3)
    private BigDecimal giacenzaDopo;
    @Column(name = "documento_tipo")
    private String documentoTipo;
    @Column(name = "documento_id")
    private Long documentoId;
    @Temporal(TemporalType.DATE)
    @Column(name = "data_movimento")
    private Date dataMovimento;
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "data_creazione")
    private Date dataCreazione;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    public enum TipoMovimento {
        CARICO, SCARICO, RETTIFICA, INVENTARIO
    }

    public Long getId() { return id; }
    public Prodotto getProdotto() { return prodotto; }
    public void setProdotto(Prodotto prodotto) { this.prodotto = prodotto; }
    public TipoMovimento getTipoMovimento() { return tipoMovimento; }
    public void setTipoMovimento(TipoMovimento tipoMovimento) { this.tipoMovimento = tipoMovimento; }
    public BigDecimal getQuantita() { return quantita; }
    public void setQuantita(BigDecimal quantita) { this.quantita = quantita; }
    public Date getDataMovimento() { return dataMovimento; }
    public void setDataMovimento(Date dataMovimento) { this.dataMovimento = dataMovimento; }
}

@Entity
@Table(name = "note")
class Nota implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_entita")
    private TipoEntita tipoEntita;
    @Column(name = "entita_id")
    private Long entitaId;
    private String titolo;
    @Column(columnDefinition = "TEXT")
    private String contenuto;
    private Boolean importante = false;
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "data_creazione")
    private Date dataCreazione;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    public enum TipoEntita {
        CLIENTE, FORNITORE, LEAD, PREVENTIVO, ORDINE, DDT, FATTURA
    }

    public Long getId() { return id; }
    public TipoEntita getTipoEntita() { return tipoEntita; }
    public void setTipoEntita(TipoEntita tipoEntita) { this.tipoEntita = tipoEntita; }
    public Long getEntitaId() { return entitaId; }
    public void setEntitaId(Long entitaId) { this.entitaId = entitaId; }
    public String getTitolo() { return titolo; }
    public void setTitolo(String titolo) { this.titolo = titolo; }
    public String getContenuto() { return contenuto; }
    public void setContenuto(String contenuto) { this.contenuto = contenuto; }
    public Boolean getImportante() { return importante; }
    public void setImportante(Boolean importante) { this.importante = importante; }
}

@Entity
@Table(name = "scadenze")
class Scadenza implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING)
    private Tipo tipo;
    @Column(name = "tipo_entita")
    private String tipoEntita;
    @Column(name = "entita_id")
    private Long entitaId;
    private String titolo;
    @Column(columnDefinition = "TEXT")
    private String descrizione;
    @Temporal(TemporalType.DATE)
    @Column(name = "data_scadenza")
    private Date dataScadenza;
    private Boolean completata = false;
    @Enumerated(EnumType.STRING)
    private Priorita priorita = Priorita.MEDIA;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assegnata_a")
    private User assegnataA;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;
    @Column(precision = 15, scale = 2)
    private BigDecimal importo;

    public enum Tipo {
        FATTURA, PAGAMENTO, ATTIVITA, PROMEMORIA, FOLLOWUP
    }

    public enum Priorita {
        BASSA, MEDIA, ALTA, URGENTE
    }

    public Long getId() { return id; }
    public Tipo getTipo() { return tipo; }
    public void setTipo(Tipo tipo) { this.tipo = tipo; }
    public String getTitolo() { return titolo; }
    public void setTitolo(String titolo) { this.titolo = titolo; }
    public Date getDataScadenza() { return dataScadenza; }
    public void setDataScadenza(Date dataScadenza) { this.dataScadenza = dataScadenza; }
    public Boolean getCompletata() { return completata; }
    public void setCompletata(Boolean completata) { this.completata = completata; }
    public Priorita getPriorita() { return priorita; }
    public void setPriorita(Priorita priorita) { this.priorita = priorita; }
}

@Entity
@Table(name = "listini")
class Listino implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nome;
    @Column(columnDefinition = "TEXT")
    private String descrizione;
    private Boolean attivo = true;

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public Boolean getAttivo() { return attivo; }
    public void setAttivo(Boolean attivo) { this.attivo = attivo; }
}
