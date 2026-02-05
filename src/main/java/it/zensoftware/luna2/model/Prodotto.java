package it.zensoftware.luna2.model;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Entity class for Prodotto
 */
@Entity
@Table(name = "prodotti")
public class Prodotto implements Serializable {
    
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String codice;

    @Column(nullable = false)
    private String nome;

    @Column(columnDefinition = "TEXT")
    private String descrizione;

    @Column(length = 100)
    private String categoria;

    @Enumerated(EnumType.STRING)
    @Column(name = "unita_misura", nullable = false)
    private UnitaMisura unitaMisura = UnitaMisura.PEZZO;

    @Column(name = "prezzo_base", precision = 15, scale = 2, nullable = false)
    private BigDecimal prezzoBase = BigDecimal.ZERO;

    @Column(name = "costo_acquisto", precision = 15, scale = 2)
    private BigDecimal costoAcquisto;

    @Column(name = "iva_percentuale", precision = 5, scale = 2, nullable = false)
    private BigDecimal ivaPercentuale = new BigDecimal("22.00");

    @Column(name = "sconto_massimo", precision = 5, scale = 2)
    private BigDecimal scontoMassimo = BigDecimal.ZERO;

    @Column(precision = 10, scale = 3)
    private BigDecimal peso;

    @Column(precision = 10, scale = 3)
    private BigDecimal volume;

    @Column(name = "codice_ean", length = 50)
    private String codiceEan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fornitore_id")
    private Fornitore fornitore;

    @Column(name = "gestione_magazzino", nullable = false)
    private Boolean gestioneMagazzino = true;

    @Column(name = "giacenza_minima", precision = 10, scale = 2)
    private BigDecimal giacenzaMinima;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(nullable = false)
    private Boolean attivo = true;

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

    public enum UnitaMisura {
        PEZZO, KG, LITRO, METRO, MQ, MC, ORA
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
    public Prodotto() {}

    public Prodotto(String codice, String nome, BigDecimal prezzoBase) {
        this.codice = codice;
        this.nome = nome;
        this.prezzoBase = prezzoBase;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCodice() {
        return codice;
    }

    public void setCodice(String codice) {
        this.codice = codice;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getDescrizione() {
        return descrizione;
    }

    public void setDescrizione(String descrizione) {
        this.descrizione = descrizione;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public UnitaMisura getUnitaMisura() {
        return unitaMisura;
    }

    public void setUnitaMisura(UnitaMisura unitaMisura) {
        this.unitaMisura = unitaMisura;
    }

    public BigDecimal getPrezzoBase() {
        return prezzoBase;
    }

    public void setPrezzoBase(BigDecimal prezzoBase) {
        this.prezzoBase = prezzoBase;
    }

    public BigDecimal getCostoAcquisto() {
        return costoAcquisto;
    }

    public void setCostoAcquisto(BigDecimal costoAcquisto) {
        this.costoAcquisto = costoAcquisto;
    }

    public BigDecimal getIvaPercentuale() {
        return ivaPercentuale;
    }

    public void setIvaPercentuale(BigDecimal ivaPercentuale) {
        this.ivaPercentuale = ivaPercentuale;
    }

    public BigDecimal getScontoMassimo() {
        return scontoMassimo;
    }

    public void setScontoMassimo(BigDecimal scontoMassimo) {
        this.scontoMassimo = scontoMassimo;
    }

    public BigDecimal getPeso() {
        return peso;
    }

    public void setPeso(BigDecimal peso) {
        this.peso = peso;
    }

    public BigDecimal getVolume() {
        return volume;
    }

    public void setVolume(BigDecimal volume) {
        this.volume = volume;
    }

    public String getCodiceEan() {
        return codiceEan;
    }

    public void setCodiceEan(String codiceEan) {
        this.codiceEan = codiceEan;
    }

    public Fornitore getFornitore() {
        return fornitore;
    }

    public void setFornitore(Fornitore fornitore) {
        this.fornitore = fornitore;
    }

    public Boolean getGestioneMagazzino() {
        return gestioneMagazzino;
    }

    public void setGestioneMagazzino(Boolean gestioneMagazzino) {
        this.gestioneMagazzino = gestioneMagazzino;
    }

    public BigDecimal getGiacenzaMinima() {
        return giacenzaMinima;
    }

    public void setGiacenzaMinima(BigDecimal giacenzaMinima) {
        this.giacenzaMinima = giacenzaMinima;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Boolean getAttivo() {
        return attivo;
    }

    public void setAttivo(Boolean attivo) {
        this.attivo = attivo;
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

    public BigDecimal getMargine() {
        if (costoAcquisto != null && prezzoBase != null) {
            return prezzoBase.subtract(costoAcquisto);
        }
        return BigDecimal.ZERO;
    }

    public BigDecimal getMarginePercentuale() {
        if (costoAcquisto != null && prezzoBase != null && costoAcquisto.compareTo(BigDecimal.ZERO) > 0) {
            return getMargine().divide(costoAcquisto, 4, BigDecimal.ROUND_HALF_UP)
                    .multiply(new BigDecimal("100"));
        }
        return BigDecimal.ZERO;
    }

    @Override
    public String toString() {
        return "Prodotto{" +
                "id=" + id +
                ", codice='" + codice + '\'' +
                ", nome='" + nome + '\'' +
                ", prezzoBase=" + prezzoBase +
                ", attivo=" + attivo +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Prodotto)) return false;
        Prodotto prodotto = (Prodotto) o;
        return id != null && id.equals(prodotto.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
