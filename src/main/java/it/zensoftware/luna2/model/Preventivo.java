package it.zensoftware.luna2.model;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Entity class for Preventivo
 */
@Entity
@Table(name = "preventivi")
public class Preventivo implements Serializable {
    
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String numero;

    @Column(nullable = false)
    private Integer anno;

    @Temporal(TemporalType.DATE)
    @Column(name = "data_preventivo", nullable = false)
    private Date dataPreventivo;

    @Temporal(TemporalType.DATE)
    @Column(name = "data_validita")
    private Date dataValidita;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lead_id")
    private Lead lead;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Stato stato = Stato.BOZZA;

    private String oggetto;

    @Column(name = "note_intestazione", columnDefinition = "TEXT")
    private String noteIntestazione;

    @Column(name = "note_piede", columnDefinition = "TEXT")
    private String notePiede;

    @Column(precision = 15, scale = 2, nullable = false)
    private BigDecimal imponibile = BigDecimal.ZERO;

    @Column(precision = 15, scale = 2, nullable = false)
    private BigDecimal iva = BigDecimal.ZERO;

    @Column(precision = 15, scale = 2, nullable = false)
    private BigDecimal totale = BigDecimal.ZERO;

    @Column(name = "sconto_percentuale", precision = 5, scale = 2)
    private BigDecimal scontoPercentuale = BigDecimal.ZERO;

    @Column(name = "sconto_importo", precision = 15, scale = 2)
    private BigDecimal scontoImporto = BigDecimal.ZERO;

    @Column(name = "spese_trasporto", precision = 15, scale = 2)
    private BigDecimal speseTrasporto = BigDecimal.ZERO;

    @Column(name = "condizioni_pagamento")
    private String condizioniPagamento;

    @Column(name = "tempi_consegna")
    private String tempiConsegna;

    @Column(name = "validita_giorni")
    private Integer validitaGiorni = 30;

    @Column(name = "ordine_id")
    private Long ordineId;

    // @OneToMany(mappedBy = "preventivo", cascade = CascadeType.ALL, orphanRemoval = true)
    // @OrderBy("rigaNumero ASC")
    private transient List<PreventivoRiga> righe = new ArrayList<>();

    @OneToMany(mappedBy = "preventivo", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Commessa> commesse = new ArrayList<>();

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
        BOZZA, INVIATO, ACCETTATO, RIFIUTATO, SCADUTO
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

    // Constructors
    public Preventivo() {}

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public Integer getAnno() {
        return anno;
    }

    public void setAnno(Integer anno) {
        this.anno = anno;
    }

    public Date getDataPreventivo() {
        return dataPreventivo;
    }

    public void setDataPreventivo(Date dataPreventivo) {
        this.dataPreventivo = dataPreventivo;
    }

    public Date getDataValidita() {
        return dataValidita;
    }

    public void setDataValidita(Date dataValidita) {
        this.dataValidita = dataValidita;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }

    public Lead getLead() {
        return lead;
    }

    public void setLead(Lead lead) {
        this.lead = lead;
    }

    public Stato getStato() {
        return stato;
    }

    public void setStato(Stato stato) {
        this.stato = stato;
    }

    public String getOggetto() {
        return oggetto;
    }

    public void setOggetto(String oggetto) {
        this.oggetto = oggetto;
    }

    public String getNoteIntestazione() {
        return noteIntestazione;
    }

    public void setNoteIntestazione(String noteIntestazione) {
        this.noteIntestazione = noteIntestazione;
    }

    public String getNotePiede() {
        return notePiede;
    }

    public void setNotePiede(String notePiede) {
        this.notePiede = notePiede;
    }

    public BigDecimal getImponibile() {
        return imponibile;
    }

    public void setImponibile(BigDecimal imponibile) {
        this.imponibile = imponibile;
    }

    public BigDecimal getIva() {
        return iva;
    }

    public void setIva(BigDecimal iva) {
        this.iva = iva;
    }

    public BigDecimal getTotale() {
        return totale;
    }

    public void setTotale(BigDecimal totale) {
        this.totale = totale;
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

    public BigDecimal getSpeseTrasporto() {
        return speseTrasporto;
    }

    public void setSpeseTrasporto(BigDecimal speseTrasporto) {
        this.speseTrasporto = speseTrasporto;
    }

    public String getCondizioniPagamento() {
        return condizioniPagamento;
    }

    public void setCondizioniPagamento(String condizioniPagamento) {
        this.condizioniPagamento = condizioniPagamento;
    }

    public String getTempiConsegna() {
        return tempiConsegna;
    }

    public void setTempiConsegna(String tempiConsegna) {
        this.tempiConsegna = tempiConsegna;
    }

    public Integer getValiditaGiorni() {
        return validitaGiorni;
    }

    public void setValiditaGiorni(Integer validitaGiorni) {
        this.validitaGiorni = validitaGiorni;
    }

    public Long getOrdineId() {
        return ordineId;
    }

    public void setOrdineId(Long ordineId) {
        this.ordineId = ordineId;
    }

    public List<PreventivoRiga> getRighe() {
        return righe;
    }

    public void setRighe(List<PreventivoRiga> righe) {
        this.righe = righe;
    }

    public void addRiga(PreventivoRiga riga) {
        righe.add(riga);
        riga.setPreventivo(this);
    }

    public void removeRiga(PreventivoRiga riga) {
        righe.remove(riga);
        riga.setPreventivo(null);
    }

    public Date getDataCreazione() {
        return dataCreazione;
    }

    public void setDataCreazione(Date dataCreazione) {
        this.dataCreazione = dataCreazione;
    }

    public Date getDataModifica() {
        return dataModifica;
    }

    public void setDataModifica(Date dataModifica) {
        this.dataModifica = dataModifica;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public User getModifiedBy() {
        return modifiedBy;
    }

    public void setModifiedBy(User modifiedBy) {
        this.modifiedBy = modifiedBy;
    }

    public void calcolaTotali() {
        BigDecimal totaleRighe = BigDecimal.ZERO;
        BigDecimal totaleIva = BigDecimal.ZERO;

        for (PreventivoRiga riga : righe) {
            if (riga.getTipoRiga() == PreventivoRiga.TipoRiga.PRODOTTO) {
                totaleRighe = totaleRighe.add(riga.getImponibileRiga());
                totaleIva = totaleIva.add(riga.getIvaImporto());
            }
        }

        // Applica sconto generale
        if (scontoPercentuale != null && scontoPercentuale.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal sconto = totaleRighe.multiply(scontoPercentuale).divide(new BigDecimal("100"), 2, BigDecimal.ROUND_HALF_UP);
            totaleRighe = totaleRighe.subtract(sconto);
            scontoImporto = sconto;
        } else if (scontoImporto != null && scontoImporto.compareTo(BigDecimal.ZERO) > 0) {
            totaleRighe = totaleRighe.subtract(scontoImporto);
        }

        // Aggiungi spese trasporto
        if (speseTrasporto != null) {
            totaleRighe = totaleRighe.add(speseTrasporto);
        }

        this.imponibile = totaleRighe;
        this.iva = totaleIva;
        this.totale = totaleRighe.add(totaleIva);
    }

    @Override
    public String toString() {
        return "Preventivo{" +
                "id=" + id +
                ", numero='" + numero + '\'' +
                ", cliente=" + (cliente != null ? cliente.getRagioneSociale() : null) +
                ", stato=" + stato +
                ", totale=" + totale +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Preventivo)) return false;
        Preventivo that = (Preventivo) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public List<Commessa> getCommesse() {
        return commesse;
    }

    public void setCommesse(List<Commessa> commesse) {
        this.commesse = commesse;
    }
}
