package it.zensoftware.luna2.model;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Fatture Passive - Fatture ricevute da fornitori tramite Sistema di Interscambio (SDI)
 * Sincronizzate automaticamente tramite endpoint: https://api.luna.itsolutions-cloud.com/ricevi-fatture/
 */
@Entity
@Table(name = "fatture_passive")
public class FatturaPassiva implements Serializable {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String numero;
    
    private Integer anno;
    
    @Temporal(TemporalType.DATE)
    @Column(name = "data_fattura")
    private Date dataFattura;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fornitore_id")
    private Fornitore fornitore;
    
    @Column(name = "fornitore_nome")
    private String fornitoreNome; // fallback se fornitore non trovato
    
    @Column(name = "fornitore_piva")
    private String fornitorePiva;
    
    private String oggetto;
    
    @Column(name = "note_generali", columnDefinition = "TEXT")
    private String noteGenerali;
    
    @Column(precision = 15, scale = 2)
    private BigDecimal imponibile = BigDecimal.ZERO;
    
    @Column(precision = 15, scale = 2)
    private BigDecimal iva = BigDecimal.ZERO;
    
    @Column(precision = 15, scale = 2)
    private BigDecimal totale = BigDecimal.ZERO;
    
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
    
    @Column(name = "xml_sdi", columnDefinition = "TEXT")
    private String xmlSdi; // XML della fattura ricevuta da SDI
    
    @Column(name = "sdi_id_messaggio")
    private String sdiIdMessaggio; // ID del messaggio SDI
    
    @Column(name = "stato_ricezione")
    private String statoRicezione; // RICEVUTA, REGISTRATA, ARCHIVIATA, ecc.
    
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "data_ricezione")
    private Date dataRicezione;
    
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "data_creazione", updatable = false)
    private Date dataCreazione;
    
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "data_modifica")
    private Date dataModifica;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;
    
    public enum StatoPagamento {
        DA_PAGARE, PARZIALMENTE_PAGATA, PAGATA, SCADUTA
    }
    
    @PrePersist
    protected void onCreate() {
        dataCreazione = new Date();
        dataModifica = new Date();
        if (dataRicezione == null) {
            dataRicezione = new Date();
        }
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
    
    public Fornitore getFornitore() { return fornitore; }
    public void setFornitore(Fornitore fornitore) { this.fornitore = fornitore; }
    
    public String getFornitoreNome() { return fornitoreNome; }
    public void setFornitoreNome(String fornitoreNome) { this.fornitoreNome = fornitoreNome; }
    
    public String getFornitorePiva() { return fornitorePiva; }
    public void setFornitorePiva(String fornitorePiva) { this.fornitorePiva = fornitorePiva; }
    
    public String getOggetto() { return oggetto; }
    public void setOggetto(String oggetto) { this.oggetto = oggetto; }
    
    public String getNoteGenerali() { return noteGenerali; }
    public void setNoteGenerali(String noteGenerali) { this.noteGenerali = noteGenerali; }
    
    public BigDecimal getImponibile() { return imponibile; }
    public void setImponibile(BigDecimal imponibile) { this.imponibile = imponibile; }
    
    public BigDecimal getIva() { return iva; }
    public void setIva(BigDecimal iva) { this.iva = iva; }
    
    public BigDecimal getTotale() { return totale; }
    public void setTotale(BigDecimal totale) { this.totale = totale; }
    
    public StatoPagamento getStatoPagamento() { return statoPagamento; }
    public void setStatoPagamento(StatoPagamento statoPagamento) { this.statoPagamento = statoPagamento; }
    
    public Date getDataScadenza() { return dataScadenza; }
    public void setDataScadenza(Date dataScadenza) { this.dataScadenza = dataScadenza; }
    
    public Date getDataPagamento() { return dataPagamento; }
    public void setDataPagamento(Date dataPagamento) { this.dataPagamento = dataPagamento; }
    
    public String getMetodoPagamento() { return metodoPagamento; }
    public void setMetodoPagamento(String metodoPagamento) { this.metodoPagamento = metodoPagamento; }
    
    public String getXmlSdi() { return xmlSdi; }
    public void setXmlSdi(String xmlSdi) { this.xmlSdi = xmlSdi; }
    
    public String getSdiIdMessaggio() { return sdiIdMessaggio; }
    public void setSdiIdMessaggio(String sdiIdMessaggio) { this.sdiIdMessaggio = sdiIdMessaggio; }
    
    public String getStatoRicezione() { return statoRicezione; }
    public void setStatoRicezione(String statoRicezione) { this.statoRicezione = statoRicezione; }
    
    public Date getDataRicezione() { return dataRicezione; }
    public void setDataRicezione(Date dataRicezione) { this.dataRicezione = dataRicezione; }
    
    public Date getDataCreazione() { return dataCreazione; }
    public Date getDataModifica() { return dataModifica; }
    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }
}
