package it.zensoftware.luna2.model;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * Entità Commessa - Rappresenta un ordine di lavoro/produzione
 * Collega un Preventivo alla Fattura finale
 */
@Entity
@Table(name = "commesse")
public class Commessa {

    public enum StatoCommessa {
        APERTA, IN_LAVORAZIONE, SOSPESA, COMPLETATA, CHIUSA, ANNULLATA
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero", nullable = false, unique = true)
    private String numero;

    @Column(name = "anno", nullable = false)
    private Integer anno;

    @Column(name = "descrizione")
    private String descrizione;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "preventivo_id", nullable = false)
    private Preventivo preventivo;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "fattura_id")
    private Fattura fattura;

    @Column(name = "data_apertura", nullable = false)
    private Date dataApertura;

    @Column(name = "data_prevista_fine")
    private Date dataPrevistFine;

    @Column(name = "data_fine_effettiva")
    private Date dataFineEffettiva;

    @Column(name = "data_chiusura")
    private Date dataChiusura;

    @Column(name = "stato", nullable = false)
    @Enumerated(EnumType.STRING)
    private StatoCommessa stato = StatoCommessa.APERTA;

    @Column(name = "imponibile", precision = 10, scale = 2)
    private BigDecimal imponibile = BigDecimal.ZERO;

    @Column(name = "iva", precision = 10, scale = 2)
    private BigDecimal iva = BigDecimal.ZERO;

    @Column(name = "totale", precision = 10, scale = 2)
    private BigDecimal totale = BigDecimal.ZERO;

    @Column(name = "percentuale_completamento")
    private Integer percentualeCompletamento = 0;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "data_creazione", nullable = false, updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date dataCreazione = new Date();

    @Column(name = "data_modifica")
    @Temporal(TemporalType.TIMESTAMP)
    private Date dataModifica = new Date();

    @OneToMany(mappedBy = "commessa", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ComMessaRiga> righe;

    // Constructors
    public Commessa() {
        this.dataApertura = new Date();
        this.stato = StatoCommessa.APERTA;
    }

    public Commessa(String numero, Integer anno, Preventivo preventivo) {
        this.numero = numero;
        this.anno = anno;
        this.preventivo = preventivo;
        this.dataApertura = new Date();
        this.stato = StatoCommessa.APERTA;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }

    public Integer getAnno() { return anno; }
    public void setAnno(Integer anno) { this.anno = anno; }

    public String getDescrizione() { return descrizione; }
    public void setDescrizione(String descrizione) { this.descrizione = descrizione; }

    public Preventivo getPreventivo() { return preventivo; }
    public void setPreventivo(Preventivo preventivo) { this.preventivo = preventivo; }

    public Fattura getFattura() { return fattura; }
    public void setFattura(Fattura fattura) { this.fattura = fattura; }

    public Date getDataApertura() { return dataApertura; }
    public void setDataApertura(Date dataApertura) { this.dataApertura = dataApertura; }

    public Date getDataPrevistFine() { return dataPrevistFine; }
    public void setDataPrevistFine(Date dataPrevistFine) { this.dataPrevistFine = dataPrevistFine; }

    public Date getDataFineEffettiva() { return dataFineEffettiva; }
    public void setDataFineEffettiva(Date dataFineEffettiva) { this.dataFineEffettiva = dataFineEffettiva; }

    public Date getDataChiusura() { return dataChiusura; }
    public void setDataChiusura(Date dataChiusura) { this.dataChiusura = dataChiusura; }

    public StatoCommessa getStato() { return stato; }
    public void setStato(StatoCommessa stato) { this.stato = stato; }

    public BigDecimal getImponibile() { return imponibile; }
    public void setImponibile(BigDecimal imponibile) { this.imponibile = imponibile; }

    public BigDecimal getIva() { return iva; }
    public void setIva(BigDecimal iva) { this.iva = iva; }

    public BigDecimal getTotale() { return totale; }
    public void setTotale(BigDecimal totale) { this.totale = totale; }

    public Integer getPercentualeCompletamento() { return percentualeCompletamento; }
    public void setPercentualeCompletamento(Integer percentualeCompletamento) { 
        this.percentualeCompletamento = percentualeCompletamento; 
    }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public Date getDataCreazione() { return dataCreazione; }
    public void setDataCreazione(Date dataCreazione) { this.dataCreazione = dataCreazione; }

    public Date getDataModifica() { return dataModifica; }
    public void setDataModifica(Date dataModifica) { this.dataModifica = dataModifica; }

    public List<ComMessaRiga> getRighe() { return righe; }
    public void setRighe(List<ComMessaRiga> righe) { this.righe = righe; }
}
