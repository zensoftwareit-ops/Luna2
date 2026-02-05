package it.zensoftware.luna2.model;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "ordini")
public class Ordine implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private String numero;
    private Integer anno;
    @Temporal(TemporalType.DATE)
    @Column(name = "data_ordine")
    private Date dataOrdine;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "preventivo_id")
    private Preventivo preventivo;
    @Enumerated(EnumType.STRING)
    private Stato stato = Stato.CONFERMATO;
    private String oggetto;
    @Column(name = "riferimento_cliente")
    private String riferimentoCliente;
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
    @Column(name = "sconto_percentuale", precision = 5, scale = 2)
    private BigDecimal scontoPercentuale = BigDecimal.ZERO;
    @Column(name = "sconto_importo", precision = 15, scale = 2)
    private BigDecimal scontoImporto = BigDecimal.ZERO;
    @Column(name = "spese_trasporto", precision = 15, scale = 2)
    private BigDecimal speseTrasporto = BigDecimal.ZERO;
    @Column(name = "condizioni_pagamento")
    private String condizioniPagamento;
    @Temporal(TemporalType.DATE)
    @Column(name = "data_consegna_prevista")
    private Date dataConsegnaPrevista;
    // // @OneToMany(mappedBy = "ordine", cascade = CascadeType.ALL, orphanRemoval = true)
    // // @OrderBy("rigaNumero ASC")
    // Temporarily commented - OrdineRiga class is not yet implemented
    // // // private List<OrdineRiga> righe = new ArrayList<>();
    private transient List<OrdineRiga> righe = new ArrayList<>();
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "data_creazione", updatable = false)
    private Date dataCreazione;
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "data_modifica")
    private Date dataModifica;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "modified_by")
    private User modifiedBy;

    public enum Stato {
        CONFERMATO, IN_LAVORAZIONE, PARZIALMENTE_EVASO, EVASO, ANNULLATO
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
    public Date getDataOrdine() { return dataOrdine; }
    public void setDataOrdine(Date dataOrdine) { this.dataOrdine = dataOrdine; }
    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }
    public Preventivo getPreventivo() { return preventivo; }
    public void setPreventivo(Preventivo preventivo) { this.preventivo = preventivo; }
    public Stato getStato() { return stato; }
    public void setStato(Stato stato) { this.stato = stato; }
    public String getOggetto() { return oggetto; }
    public void setOggetto(String oggetto) { this.oggetto = oggetto; }
    public String getRiferimentoCliente() { return riferimentoCliente; }
    public void setRiferimentoCliente(String riferimentoCliente) { this.riferimentoCliente = riferimentoCliente; }
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
    public BigDecimal getScontoPercentuale() { return scontoPercentuale; }
    public void setScontoPercentuale(BigDecimal scontoPercentuale) { this.scontoPercentuale = scontoPercentuale; }
    public BigDecimal getScontoImporto() { return scontoImporto; }
    public void setScontoImporto(BigDecimal scontoImporto) { this.scontoImporto = scontoImporto; }
    public BigDecimal getSpeseTrasporto() { return speseTrasporto; }
    public void setSpeseTrasporto(BigDecimal speseTrasporto) { this.speseTrasporto = speseTrasporto; }
    public String getCondizioniPagamento() { return condizioniPagamento; }
    public void setCondizioniPagamento(String condizioniPagamento) { this.condizioniPagamento = condizioniPagamento; }
    public Date getDataConsegnaPrevista() { return dataConsegnaPrevista; }
    public void setDataConsegnaPrevista(Date dataConsegnaPrevista) { this.dataConsegnaPrevista = dataConsegnaPrevista; }
    public List<OrdineRiga> getRighe() { return righe; }
    public void setRighe(List<OrdineRiga> righe) { this.righe = righe; }
    public void addRiga(OrdineRiga riga) { righe.add(riga); riga.setOrdine(this); }
    public void removeRiga(OrdineRiga riga) { righe.remove(riga); riga.setOrdine(null); }
    public Date getDataCreazione() { return dataCreazione; }
    public void setDataCreazione(Date dataCreazione) { this.dataCreazione = dataCreazione; }
    public Date getDataModifica() { return dataModifica; }
    public void setDataModifica(Date dataModifica) { this.dataModifica = dataModifica; }
    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }
    public User getModifiedBy() { return modifiedBy; }
    public void setModifiedBy(User modifiedBy) { this.modifiedBy = modifiedBy; }
}

@Entity
@Table(name = "ordini_righe")
class OrdineRiga implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ordine_id")
    private Ordine ordine;
    @Column(name = "riga_numero")
    private Integer rigaNumero;
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_riga")
    private TipoRiga tipoRiga = TipoRiga.PRODOTTO;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prodotto_id")
    private Prodotto prodotto;
    @Column(columnDefinition = "TEXT")
    private String descrizione;
    @Column(precision = 15, scale = 3)
    private BigDecimal quantita = BigDecimal.ONE;
    @Column(name = "quantita_evasa", precision = 15, scale = 3)
    private BigDecimal quantitaEvasa = BigDecimal.ZERO;
    @Column(name = "unita_misura")
    private String unitaMisura;
    @Column(name = "prezzo_unitario", precision = 15, scale = 2)
    private BigDecimal prezzoUnitario = BigDecimal.ZERO;
    @Column(name = "sconto_percentuale", precision = 5, scale = 2)
    private BigDecimal scontoPercentuale = BigDecimal.ZERO;
    @Column(name = "imponibile_riga", precision = 15, scale = 2)
    private BigDecimal imponibileRiga = BigDecimal.ZERO;
    @Column(name = "iva_percentuale", precision = 5, scale = 2)
    private BigDecimal ivaPercentuale = new BigDecimal("22.00");
    @Column(name = "totale_riga", precision = 15, scale = 2)
    private BigDecimal totaleRiga = BigDecimal.ZERO;

    public enum TipoRiga {
        PRODOTTO, DESCRIZIONE, SUBTOTALE
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Ordine getOrdine() { return ordine; }
    public void setOrdine(Ordine ordine) { this.ordine = ordine; }
    public Integer getRigaNumero() { return rigaNumero; }
    public void setRigaNumero(Integer rigaNumero) { this.rigaNumero = rigaNumero; }
    public TipoRiga getTipoRiga() { return tipoRiga; }
    public void setTipoRiga(TipoRiga tipoRiga) { this.tipoRiga = tipoRiga; }
    public Prodotto getProdotto() { return prodotto; }
    public void setProdotto(Prodotto prodotto) { this.prodotto = prodotto; }
    public String getDescrizione() { return descrizione; }
    public void setDescrizione(String descrizione) { this.descrizione = descrizione; }
    public BigDecimal getQuantita() { return quantita; }
    public void setQuantita(BigDecimal quantita) { this.quantita = quantita; }
    public BigDecimal getQuantitaEvasa() { return quantitaEvasa; }
    public void setQuantitaEvasa(BigDecimal quantitaEvasa) { this.quantitaEvasa = quantitaEvasa; }
    public String getUnitaMisura() { return unitaMisura; }
    public void setUnitaMisura(String unitaMisura) { this.unitaMisura = unitaMisura; }
    public BigDecimal getPrezzoUnitario() { return prezzoUnitario; }
    public void setPrezzoUnitario(BigDecimal prezzoUnitario) { this.prezzoUnitario = prezzoUnitario; }
    public BigDecimal getScontoPercentuale() { return scontoPercentuale; }
    public void setScontoPercentuale(BigDecimal scontoPercentuale) { this.scontoPercentuale = scontoPercentuale; }
    public BigDecimal getImponibileRiga() { return imponibileRiga; }
    public void setImponibileRiga(BigDecimal imponibileRiga) { this.imponibileRiga = imponibileRiga; }
    public BigDecimal getIvaPercentuale() { return ivaPercentuale; }
    public void setIvaPercentuale(BigDecimal ivaPercentuale) { this.ivaPercentuale = ivaPercentuale; }
    public BigDecimal getTotaleRiga() { return totaleRiga; }
    public void setTotaleRiga(BigDecimal totaleRiga) { this.totaleRiga = totaleRiga; }
}
