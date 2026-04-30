package it.zensoftware.luna2.model;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Pagamento - Tracciamento dei pagamenti per Fatture
 *
 * Rappresenta un pagamento ricevuto per una fattura.
 * Una fattura può avere uno o più pagamenti (pagamenti rateali, acconti, ecc.)
 */
@Entity
@Table(name = "pagamenti")
public class Pagamento implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fattura_id", nullable = false)
    private Fattura fattura;

    @Column(name = "importo", precision = 15, scale = 2, nullable = false)
    private BigDecimal importo = BigDecimal.ZERO;

    @Temporal(TemporalType.DATE)
    @Column(name = "data_pagamento", nullable = false)
    private Date dataPagamento;

    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_pagamento")
    private MetodoPagamento metodoPagamento = MetodoPagamento.BONIFICO;

    @Column(name = "riferimento", length = 255)
    private String riferimento; // Es: numero versamento, ID transazione bancaria, codice carta, ecc.

    @Column(name = "banca_mittente", length = 255)
    private String bancaMittente; // Banca del cliente che effettua il pagamento

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "causale", columnDefinition = "TEXT")
    private String causale; // Causale del versamento

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "data_creazione", updatable = false)
    private Date dataCreazione;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "data_modifica")
    private Date dataModifica;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "riconciliato", nullable = false)
    private Boolean riconciliato = false; // Se il pagamento è stato riconciliato con l'estratto conto

    @Temporal(TemporalType.DATE)
    @Column(name = "data_riconciliazione")
    private Date dataRiconciliazione;

    /**
     * Enum per i metodi di pagamento supportati
     */
    public enum MetodoPagamento {
        BONIFICO("Bonifico Bancario"),
        CARTA("Pagamento con Carta"),
        CONTANTI("Contanti"),
        ASSEGNO("Assegno"),
        VAGLIA("Vaglia Postale"),
        RID("Ricevuta di Incasso"),
        F24("Modello F24"),
        CREDITO("Credito Client"),
        ALTRO("Altro");

        private final String descrizione;

        MetodoPagamento(String descrizione) {
            this.descrizione = descrizione;
        }

        public String getDescrizione() {
            return descrizione;
        }
    }

    @PrePersist
    protected void onCreate() {
        dataCreazione = new Date();
        dataModifica = new Date();
        if (dataPagamento == null) {
            dataPagamento = new Date();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        dataModifica = new Date();
    }

    // ==================== Getters and Setters ====================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Fattura getFattura() {
        return fattura;
    }

    public void setFattura(Fattura fattura) {
        this.fattura = fattura;
    }

    public BigDecimal getImporto() {
        return importo;
    }

    public void setImporto(BigDecimal importo) {
        this.importo = importo != null ? importo : BigDecimal.ZERO;
    }

    public Date getDataPagamento() {
        return dataPagamento;
    }

    public void setDataPagamento(Date dataPagamento) {
        this.dataPagamento = dataPagamento;
    }

    public MetodoPagamento getMetodoPagamento() {
        return metodoPagamento;
    }

    public void setMetodoPagamento(MetodoPagamento metodoPagamento) {
        this.metodoPagamento = metodoPagamento;
    }

    public String getRiferimento() {
        return riferimento;
    }

    public void setRiferimento(String riferimento) {
        this.riferimento = riferimento;
    }

    public String getBancaMittente() {
        return bancaMittente;
    }

    public void setBancaMittente(String bancaMittente) {
        this.bancaMittente = bancaMittente;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getCausale() {
        return causale;
    }

    public void setCausale(String causale) {
        this.causale = causale;
    }

    public Date getDataCreazione() {
        return dataCreazione;
    }

    public Date getDataModifica() {
        return dataModifica;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public Boolean getRiconciliato() {
        return riconciliato;
    }

    public void setRiconciliato(Boolean riconciliato) {
        this.riconciliato = riconciliato != null ? riconciliato : false;
    }

    public Date getDataRiconciliazione() {
        return dataRiconciliazione;
    }

    public void setDataRiconciliazione(Date dataRiconciliazione) {
        this.dataRiconciliazione = dataRiconciliazione;
    }

    // ==================== Business Methods ====================

    /**
     * Verifica se il pagamento è completo per la fattura
     */
    public boolean isPagamentoCompleto() {
        if (fattura == null || fattura.getTotale() == null) {
            return false;
        }
        return importo.compareTo(fattura.getTotale()) >= 0;
    }

    /**
     * Calcola l'importo residuo da pagare
     */
    public BigDecimal getImportoResiduo() {
        if (fattura == null || fattura.getTotale() == null) {
            return BigDecimal.ZERO;
        }
        return fattura.getTotale().subtract(importo).max(BigDecimal.ZERO);
    }

    @Override
    public String toString() {
        return "Pagamento{" +
                "id=" + id +
                ", fattura=" + (fattura != null ? fattura.getNumero() : "null") +
                ", importo=" + importo +
                ", dataPagamento=" + dataPagamento +
                ", metodoPagamento=" + metodoPagamento +
                ", riconciliato=" + riconciliato +
                '}';
    }
}
