package it.zensoftware.luna2.model;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Entity
@Table(name = "movimenti_magazzino")
public class MovimentoMagazzino implements Serializable {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prodotto_id")
    private Prodotto prodotto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fornitore_id")
    private Fornitore fornitore;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_movimento", nullable = false)
    private TipoMovimento tipoMovimento;
    
    @Column(length = 255)
    private String causale;
    
    @Column(precision = 15, scale = 3, nullable = false)
    private BigDecimal quantita;
    
    @Column(name = "costo_unitario", precision = 15, scale = 2)
    private BigDecimal costoUnitario;
    
    @Column(name = "giacenza_prima", precision = 15, scale = 3)
    private BigDecimal giacenzaPrima;
    
    @Column(name = "giacenza_dopo", precision = 15, scale = 3)
    private BigDecimal giacenzaDopo;
    
    @Column(name = "documento_tipo", length = 50)
    private String documentoTipo;
    
    @Column(name = "documento_id")
    private Long documentoId;
    
    @Column(name = "documento_numero", length = 50)
    private String documentoNumero;
    
    @Temporal(TemporalType.DATE)
    @Column(name = "data_movimento", nullable = false)
    private Date dataMovimento;
    
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "data_creazione")
    private Date dataCreazione;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;
    
    @Column(length = 1000)
    private String note;

    public enum TipoMovimento {
        CARICO("Carico", "+"),
        SCARICO("Scarico", "-"),
        RETTIFICA("Rettifica", "±"),
        INVENTARIO("Inventario", "="),
        TRASFERIMENTO_USCITA("Trasferimento Uscita", "-"),
        TRASFERIMENTO_ENTRATA("Trasferimento Entrata", "+");

        private final String descrizione;
        private final String simbolo;

        TipoMovimento(String descrizione, String simbolo) {
            this.descrizione = descrizione;
            this.simbolo = simbolo;
        }

        public String getDescrizione() {
            return descrizione;
        }

        public String getSimbolo() {
            return simbolo;
        }
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

    public Fornitore getFornitore() {
        return fornitore;
    }

    public void setFornitore(Fornitore fornitore) {
        this.fornitore = fornitore;
    }

    public TipoMovimento getTipoMovimento() {
        return tipoMovimento;
    }

    public void setTipoMovimento(TipoMovimento tipoMovimento) {
        this.tipoMovimento = tipoMovimento;
    }

    public String getCausale() {
        return causale;
    }

    public void setCausale(String causale) {
        this.causale = causale;
    }

    public BigDecimal getQuantita() {
        return quantita;
    }

    public void setQuantita(BigDecimal quantita) {
        this.quantita = quantita;
    }

    public BigDecimal getCostoUnitario() {
        return costoUnitario;
    }

    public void setCostoUnitario(BigDecimal costoUnitario) {
        this.costoUnitario = costoUnitario;
    }

    public BigDecimal getGiacenzaPrima() {
        return giacenzaPrima;
    }

    public void setGiacenzaPrima(BigDecimal giacenzaPrima) {
        this.giacenzaPrima = giacenzaPrima;
    }

    public BigDecimal getGiacenzaDopo() {
        return giacenzaDopo;
    }

    public void setGiacenzaDopo(BigDecimal giacenzaDopo) {
        this.giacenzaDopo = giacenzaDopo;
    }

    public String getDocumentoTipo() {
        return documentoTipo;
    }

    public void setDocumentoTipo(String documentoTipo) {
        this.documentoTipo = documentoTipo;
    }

    public Long getDocumentoId() {
        return documentoId;
    }

    public void setDocumentoId(Long documentoId) {
        this.documentoId = documentoId;
    }

    public String getDocumentoNumero() {
        return documentoNumero;
    }

    public void setDocumentoNumero(String documentoNumero) {
        this.documentoNumero = documentoNumero;
    }

    public Date getDataMovimento() {
        return dataMovimento;
    }

    public void setDataMovimento(Date dataMovimento) {
        this.dataMovimento = dataMovimento;
    }

    public Date getDataCreazione() {
        return dataCreazione;
    }

    public void setDataCreazione(Date dataCreazione) {
        this.dataCreazione = dataCreazione;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    @PrePersist
    protected void onCreate() {
        if (dataCreazione == null) {
            dataCreazione = new Date();
        }
        if (dataMovimento == null) {
            dataMovimento = new Date();
        }
    }
}
