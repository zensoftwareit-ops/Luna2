package it.zensoftware.luna2.model;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

/**
 * Entity per le posizioni fisiche nel magazzino (scaffale/ripiano)
 */
@Entity
@Table(name = "posizioni_magazzino",
    uniqueConstraints = @UniqueConstraint(columnNames = {"warehouse_id", "codice"}),
    indexes = {
        @Index(name = "idx_posizioni_warehouse", columnList = "warehouse_id"),
        @Index(name = "idx_posizioni_codice", columnList = "codice")
    }
)
public class Posizione implements Serializable {
    
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @Column(nullable = false, length = 50)
    private String codice;

    @Column(length = 100)
    private String descrizione;

    @Column(length = 50)
    private String zona;

    @Column(length = 50)
    private String corsia;

    @Column(length = 50)
    private String scaffale;

    @Column(length = 50)
    private String ripiano;

    @Column(length = 50)
    private String colonna;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_posizione", length = 30)
    private TipoPosizione tipoPosizione;

    @Column(name = "capacita_max")
    private Integer capacitaMax;

    @Column(nullable = false)
    private Boolean attiva = true;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "data_creazione")
    private Date dataCreazione;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "data_modifica")
    private Date dataModifica;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    /**
     * Tipi di posizione nel magazzino
     */
    public enum TipoPosizione {
        SCAFFALE("Scaffale Standard"),
        PALLET("Posizione Pallet"),
        BANCALE("Bancale"),
        PICKING("Area Picking"),
        RICEZIONE("Area Ricezione"),
        SPEDIZIONE("Area Spedizione"),
        TRANSITO("Area Transito"),
        FRIGO("Cella Frigorifera"),
        ALTRO("Altro");

        private final String descrizione;

        TipoPosizione(String descrizione) {
            this.descrizione = descrizione;
        }

        public String getDescrizione() {
            return descrizione;
        }
    }

    // Costruttori
    public Posizione() {
        this.dataCreazione = new Date();
    }

    public Posizione(Warehouse warehouse, String codice) {
        this();
        this.warehouse = warehouse;
        this.codice = codice;
    }

    // Metodi di utilità
    @PreUpdate
    protected void onUpdate() {
        this.dataModifica = new Date();
    }

    /**
     * Restituisce una descrizione leggibile della posizione
     * Es: "A1-S3-R2" (Zona A1, Scaffale 3, Ripiano 2)
     */
    public String getPosizioneCompleta() {
        StringBuilder sb = new StringBuilder();
        if (zona != null && !zona.isEmpty()) {
            sb.append(zona);
        }
        if (corsia != null && !corsia.isEmpty()) {
            if (sb.length() > 0) sb.append("-");
            sb.append("C").append(corsia);
        }
        if (scaffale != null && !scaffale.isEmpty()) {
            if (sb.length() > 0) sb.append("-");
            sb.append("S").append(scaffale);
        }
        if (ripiano != null && !ripiano.isEmpty()) {
            if (sb.length() > 0) sb.append("-");
            sb.append("R").append(ripiano);
        }
        if (colonna != null && !colonna.isEmpty()) {
            if (sb.length() > 0) sb.append("-");
            sb.append("Col").append(colonna);
        }
        return sb.length() > 0 ? sb.toString() : codice;
    }

    /**
     * Genera un codice automatico dalla struttura
     */
    public String generaCodiceAuto() {
        return getPosizioneCompleta();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Warehouse getWarehouse() {
        return warehouse;
    }

    public void setWarehouse(Warehouse warehouse) {
        this.warehouse = warehouse;
    }

    public String getCodice() {
        return codice;
    }

    public void setCodice(String codice) {
        this.codice = codice;
    }

    public String getDescrizione() {
        return descrizione;
    }

    public void setDescrizione(String descrizione) {
        this.descrizione = descrizione;
    }

    public String getZona() {
        return zona;
    }

    public void setZona(String zona) {
        this.zona = zona;
    }

    public String getCorsia() {
        return corsia;
    }

    public void setCorsia(String corsia) {
        this.corsia = corsia;
    }

    public String getScaffale() {
        return scaffale;
    }

    public void setScaffale(String scaffale) {
        this.scaffale = scaffale;
    }

    public String getRipiano() {
        return ripiano;
    }

    public void setRipiano(String ripiano) {
        this.ripiano = ripiano;
    }

    public String getColonna() {
        return colonna;
    }

    public void setColonna(String colonna) {
        this.colonna = colonna;
    }

    public TipoPosizione getTipoPosizione() {
        return tipoPosizione;
    }

    public void setTipoPosizione(TipoPosizione tipoPosizione) {
        this.tipoPosizione = tipoPosizione;
    }

    public Integer getCapacitaMax() {
        return capacitaMax;
    }

    public void setCapacitaMax(Integer capacitaMax) {
        this.capacitaMax = capacitaMax;
    }

    public Boolean getAttiva() {
        return attiva;
    }

    public void setAttiva(Boolean attiva) {
        this.attiva = attiva;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
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

    @Override
    public String toString() {
        return "Posizione{" +
                "id=" + id +
                ", warehouse=" + (warehouse != null ? warehouse.getNome() : "null") +
                ", codice='" + codice + '\'' +
                ", posizione='" + getPosizioneCompleta() + '\'' +
                ", attiva=" + attiva +
                '}';
    }
}
