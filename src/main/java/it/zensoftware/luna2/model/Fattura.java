package it.zensoftware.luna2.model;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "fatture")
public class Fattura implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private String numero;
    private Integer anno;
    @Temporal(TemporalType.DATE)
    @Column(name = "data_fattura")
    private Date dataFattura;
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_fattura")
    private TipoFattura tipoFattura = TipoFattura.PROFORMA;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;
    @Column(name = "ordine_id")
    private Long ordineId;
    private String oggetto;
    @Column(name = "note_intestazione", columnDefinition = "TEXT")
    private String noteIntestazione;
    @Column(name = "note_piede", columnDefinition = "TEXT")
    private String notePiede;
    @Column(precision = 15, scale = 2)
    private BigDecimal imponibile = BigDecimal.ZERO;
    @Column(precision = 15, scale = 2)
    private BigDecimal iva = BigDecimal.ZERO;
    @Column(precision = 15, scale = 2)
    private BigDecimal totale = BigDecimal.ZERO;
    @Column(name = "ritenuta_acconto", precision = 15, scale = 2)
    private BigDecimal ritenutaAcconto = BigDecimal.ZERO;
    @Column(name = "totale_netto", precision = 15, scale = 2)
    private BigDecimal totaleNetto = BigDecimal.ZERO;
    @Enumerated(EnumType.STRING)
    @Column(name = "stato_pagamento")
    private StatoPagamento statoPagamento = StatoPagamento.DA_PAGARE;
    @Temporal(TemporalType.DATE)
    @Column(name = "data_scadenza")
    private Date dataScadenza;
    @Temporal(TemporalType.DATE)
    @Column(name = "data_pagamento")
    private Date dataPagamento;
    @Column(name = "metodo_pagamento")
    private String metodoPagamento;
    @Column(name = "fattura_elettronica_inviata")
    private Boolean fatturaElettronicaInviata = false;
    
    @Column(name = "sdi_codice")
    private String sdiCodice;
    
    @Column(name = "sdi_stato")
    private String sdiStato; // "INVIATA", "ACCETTATA", "ERRORE", null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "commessa_id")
    private Commessa commessa;

    @OneToMany(mappedBy = "fattura", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("rigaNumero ASC")
    private List<FatturaRiga> righe = new ArrayList<>();

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "data_creazione", updatable = false)
    private Date dataCreazione;
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "data_modifica")
    private Date dataModifica;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    public enum TipoFattura {
        PROFORMA, REALE, ORDINARIA // ORDINARIA è alias di REALE
    }

    public enum StatoPagamento {
        DA_PAGARE, PARZIALMENTE_PAGATA, PAGATA, SCADUTA, EMESSA // EMESSA per fatture elettroniche inviate
    }

    // Alias per compatibilità
    public static class StatoFattura {
        public static final StatoPagamento DA_PAGARE = StatoPagamento.DA_PAGARE;
        public static final StatoPagamento PAGATA = StatoPagamento.PAGATA;
        public static final StatoPagamento PARZIALMENTE_PAGATA = StatoPagamento.PARZIALMENTE_PAGATA;
        public static final StatoPagamento SCADUTA = StatoPagamento.SCADUTA;
        public static final StatoPagamento EMESSA = StatoPagamento.EMESSA;
    }

    @PrePersist
    protected void onCreate() {
        dataCreazione = new Date();
        dataModifica = new Date();
    }

    @PreUpdate
    protected void onUpdate() {
        dataModifica = new Date();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }
    public Integer getAnno() { return anno; }
    public void setAnno(Integer anno) { this.anno = anno; }
    public Date getDataFattura() { return dataFattura; }
    public void setDataFattura(Date dataFattura) { this.dataFattura = dataFattura; }
    public TipoFattura getTipoFattura() { return tipoFattura; }
    public void setTipoFattura(TipoFattura tipoFattura) { this.tipoFattura = tipoFattura; }
    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }
    public Long getOrdineId() { return ordineId; }
    public void setOrdineId(Long ordineId) { this.ordineId = ordineId; }
    public String getOggetto() { return oggetto; }
    public void setOggetto(String oggetto) { this.oggetto = oggetto; }
    public String getNoteIntestazione() { return noteIntestazione; }
    public void setNoteIntestazione(String noteIntestazione) { this.noteIntestazione = noteIntestazione; }
    public String getNotePiede() { return notePiede; }
    public void setNotePiede(String notePiede) { this.notePiede = notePiede; }
    public BigDecimal getImponibile() { return imponibile; }
    public void setImponibile(BigDecimal imponibile) { this.imponibile = imponibile; }
    public BigDecimal getIva() { return iva; }
    public void setIva(BigDecimal iva) { this.iva = iva; }
    public BigDecimal getTotale() { return totale; }
    public void setTotale(BigDecimal totale) { this.totale = totale; }
    public BigDecimal getRitenutaAcconto() { return ritenutaAcconto; }
    public void setRitenutaAcconto(BigDecimal ritenutaAcconto) { this.ritenutaAcconto = ritenutaAcconto; }
    public BigDecimal getTotaleNetto() { return totaleNetto; }
    public void setTotaleNetto(BigDecimal totaleNetto) { this.totaleNetto = totaleNetto; }
    public StatoPagamento getStatoPagamento() { return statoPagamento; }
    public void setStatoPagamento(StatoPagamento statoPagamento) { this.statoPagamento = statoPagamento; }
    
    // Alias per compatibilità
    public void setStato(StatoPagamento stato) {
        this.statoPagamento = stato;
    }
    
    public StatoPagamento getStato() {
        return statoPagamento;
    }
    
    public Date getDataScadenza() { return dataScadenza; }
    public void setDataScadenza(Date dataScadenza) { this.dataScadenza = dataScadenza; }
    public Date getDataPagamento() { return dataPagamento; }
    public void setDataPagamento(Date dataPagamento) { this.dataPagamento = dataPagamento; }
    public String getMetodoPagamento() { return metodoPagamento; }
    public void setMetodoPagamento(String metodoPagamento) { this.metodoPagamento = metodoPagamento; }
    public Boolean getFatturaElettronicaInviata() { return fatturaElettronicaInviata; }
    public void setFatturaElettronicaInviata(Boolean fatturaElettronicaInviata) { this.fatturaElettronicaInviata = fatturaElettronicaInviata; }
    
    public void setPagato(BigDecimal importo) {
        // In realtà tracciamo importo pagato attraverso lo stato
        if (importo != null && importo.compareTo(BigDecimal.ZERO) > 0) {
            if (importo.compareTo(totale) >= 0) {
                this.statoPagamento = StatoPagamento.PAGATA;
                this.dataPagamento = new Date();
            } else {
                this.statoPagamento = StatoPagamento.PARZIALMENTE_PAGATA;
            }
        }
    }
    
    public void setNote(String note) {
        this.notePiede = note; // Note in piede fattura
    }

    public List<FatturaRiga> getRighe() { return righe; }
    public void setRighe(List<FatturaRiga> righe) { this.righe = righe; }
    public void addRiga(FatturaRiga riga) { righe.add(riga); riga.setFattura(this); }

    public Date getDataCreazione() { return dataCreazione; }
    public Date getDataModifica() { return dataModifica; }
    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }
    
    public String getSdiCodice() { return sdiCodice; }
    public void setSdiCodice(String sdiCodice) { this.sdiCodice = sdiCodice; }
    public String getSdiStato() { return sdiStato; }
    public void setSdiStato(String sdiStato) { this.sdiStato = sdiStato; }
    
    public Commessa getCommessa() { return commessa; }
    public void setCommessa(Commessa commessa) { this.commessa = commessa; }
}

@Entity
@Table(name = "ddt")
class Ddt implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private String numero;
    private Integer anno;
    @Temporal(TemporalType.DATE)
    @Column(name = "data_ddt")
    private Date dataDdt;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;
    @Column(name = "ordine_id")
    private Long ordineId;
    @Column(name = "causale_trasporto")
    private String causaleTrasporto;
    @Column(name = "numero_colli")
    private Integer numeroColli;
    private String trasportatore;
    @Column(precision = 15, scale = 2)
    private BigDecimal imponibile = BigDecimal.ZERO;
    @Column(name = "fattura_id")
    private Long fatturaId;
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "data_creazione", updatable = false)
    private Date dataCreazione;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }
    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }
    public Date getDataDdt() { return dataDdt; }
    public void setDataDdt(Date dataDdt) { this.dataDdt = dataDdt; }
}

@Entity
@Table(name = "ddt_righe")
class DdtRiga implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ddt_id")
    private Ddt ddt;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prodotto_id")
    private Prodotto prodotto;
    @Column(columnDefinition = "TEXT")
    private String descrizione;
    @Column(precision = 15, scale = 3)
    private BigDecimal quantita = BigDecimal.ONE;

    public Long getId() { return id; }
    public Ddt getDdt() { return ddt; }
    public void setDdt(Ddt ddt) { this.ddt = ddt; }
    public Prodotto getProdotto() { return prodotto; }
    public void setProdotto(Prodotto prodotto) { this.prodotto = prodotto; }
    public String getDescrizione() { return descrizione; }
    public void setDescrizione(String descrizione) { this.descrizione = descrizione; }
    public BigDecimal getQuantita() { return quantita; }
    public void setQuantita(BigDecimal quantita) { this.quantita = quantita; }
}
