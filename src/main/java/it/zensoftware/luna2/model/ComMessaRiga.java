package it.zensoftware.luna2.model;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Dettaglio riga di una Commessa
 */
@Entity
@Table(name = "commesse_righe")
public class ComMessaRiga {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "commessa_id", nullable = false)
    private Commessa commessa;

    @Column(name = "numero_riga", nullable = false)
    private Integer numeroRiga;

    @Column(name = "descrizione", nullable = false)
    private String descrizione;

    @Column(name = "quantita", precision = 10, scale = 2)
    private BigDecimal quantita = BigDecimal.ONE;

    @Column(name = "importo_unitario", precision = 10, scale = 2)
    private BigDecimal importoUnitario = BigDecimal.ZERO;

    @Column(name = "importo_totale", precision = 10, scale = 2)
    private BigDecimal importoTotale = BigDecimal.ZERO;

    @Column(name = "data_creazione", nullable = false, updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date dataCreazione = new Date();

    @Column(name = "data_modifica")
    @Temporal(TemporalType.TIMESTAMP)
    private Date dataModifica = new Date();

    // Constructors
    public ComMessaRiga() {}

    public ComMessaRiga(Commessa commessa, Integer numeroRiga, String descrizione, 
                        BigDecimal quantita, BigDecimal importoUnitario) {
        this.commessa = commessa;
        this.numeroRiga = numeroRiga;
        this.descrizione = descrizione;
        this.quantita = quantita;
        this.importoUnitario = importoUnitario;
        this.importoTotale = quantita.multiply(importoUnitario);
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Commessa getCommessa() { return commessa; }
    public void setCommessa(Commessa commessa) { this.commessa = commessa; }

    public Integer getNumeroRiga() { return numeroRiga; }
    public void setNumeroRiga(Integer numeroRiga) { this.numeroRiga = numeroRiga; }

    public String getDescrizione() { return descrizione; }
    public void setDescrizione(String descrizione) { this.descrizione = descrizione; }

    public BigDecimal getQuantita() { return quantita; }
    public void setQuantita(BigDecimal quantita) { 
        this.quantita = quantita;
        calcolaImportoTotale();
    }

    public BigDecimal getImportoUnitario() { return importoUnitario; }
    public void setImportoUnitario(BigDecimal importoUnitario) { 
        this.importoUnitario = importoUnitario;
        calcolaImportoTotale();
    }

    public BigDecimal getImportoTotale() { return importoTotale; }
    public void setImportoTotale(BigDecimal importoTotale) { this.importoTotale = importoTotale; }

    public Date getDataCreazione() { return dataCreazione; }
    public void setDataCreazione(Date dataCreazione) { this.dataCreazione = dataCreazione; }

    public Date getDataModifica() { return dataModifica; }
    public void setDataModifica(Date dataModifica) { this.dataModifica = dataModifica; }

    // Helper
    private void calcolaImportoTotale() {
        if (quantita != null && importoUnitario != null) {
            this.importoTotale = quantita.multiply(importoUnitario);
        }
    }
}
