package it.zensoftware.luna2.model;

import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Entity per le liste di prelievo (picking lists)
 * Gestisce i prelievi dei prodotti dal magazzino per gli ordini
 */
@Entity
@Table(name = "picking_lists",
    indexes = {
        @Index(name = "idx_picking_ordine", columnList = "ordine_id"),
        @Index(name = "idx_picking_warehouse", columnList = "warehouse_id"),
        @Index(name = "idx_picking_stato", columnList = "stato"),
        @Index(name = "idx_picking_data", columnList = "data_creazione")
    }
)
public class PickingList implements Serializable {
    
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, length = 50)
    private String numero;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ordine_id", nullable = false)
    private Ordine ordine;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatoPicking stato = StatoPicking.DRAFT;

    @Column(nullable = false)
    private Integer priorita = 5;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "data_creazione", nullable = false)
    private Date dataCreazione;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "data_inizio")
    private Date dataInizio;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "data_completamento")
    private Date dataCompletamento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assegnato_a")
    private User assegnatoA;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "completato_da")
    private User completatoDa;

    @OneToMany(mappedBy = "pickingList", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("numeroLinea ASC")
    private List<PickingItem> items = new ArrayList<>();

    @Column(columnDefinition = "TEXT")
    private String note;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "data_assegnazione")
    private Date dataAssegnazione;

    /**
     * Stati del picking
     */
    public enum StatoPicking {
        DRAFT("Bozza"),
        ASSEGNATO("Assegnato"),
        IN_PROGRESS("In Corso"),
        PARZIALE("Parzialmente Completato"),
        COMPLETATO("Completato"),
        ANNULLATO("Annullato");

        private final String descrizione;

        StatoPicking(String descrizione) {
            this.descrizione = descrizione;
        }

        public String getDescrizione() {
            return descrizione;
        }
    }

    // Costruttori
    public PickingList() {
        this.dataCreazione = new Date();
    }

    public PickingList(Ordine ordine, Warehouse warehouse) {
        this();
        this.ordine = ordine;
        this.warehouse = warehouse;
        generaNumero();
    }

    // Metodi di business logic
    private void generaNumero() {
        if (numero == null && ordine != null) {
            this.numero = "PKL-" + ordine.getNumero() + "-" + System.currentTimeMillis() % 10000;
        }
    }

    /**
     * Aggiunge un item alla picking list
     */
    public void addItem(PickingItem item) {
        items.add(item);
        item.setPickingList(this);
        item.setNumeroLinea(items.size());
    }

    /**
     * Rimuove un item dalla picking list
     */
    public void removeItem(PickingItem item) {
        items.remove(item);
        item.setPickingList(null);
        // Rinumera le righe
        for (int i = 0; i < items.size(); i++) {
            items.get(i).setNumeroLinea(i + 1);
        }
    }

    /**
     * Verifica se tutti gli item sono stati prelevati completamente
     */
    public boolean isTuttoPrelevato() {
        if (items.isEmpty()) return false;
        return items.stream().allMatch(PickingItem::isCompletato);
    }

    /**
     * Verifica se almeno un item è stato prelevato
     */
    public boolean isParzialePrelevato() {
        return items.stream().anyMatch(item -> 
            item.getQuantitaPrelevata() != null && 
            item.getQuantitaPrelevata().compareTo(java.math.BigDecimal.ZERO) > 0
        );
    }

    /**
     * Calcola la percentuale di completamento
     */
    public int getPercentualeCompletamento() {
        if (items.isEmpty()) return 0;
        
        java.math.BigDecimal totaleRichiesto = items.stream()
            .map(PickingItem::getQuantitaRichiesta)
            .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        
        if (totaleRichiesto.compareTo(java.math.BigDecimal.ZERO) == 0) return 0;
        
        java.math.BigDecimal totalePrelevato = items.stream()
            .map(item -> item.getQuantitaPrelevata() != null ? item.getQuantitaPrelevata() : java.math.BigDecimal.ZERO)
            .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        
        return totalePrelevato.multiply(new java.math.BigDecimal("100"))
            .divide(totaleRichiesto, 0, java.math.BigDecimal.ROUND_HALF_UP)
            .intValue();
    }

    /**
     * Calcola e aggiorna la percentuale di completamento (returns void)
     */
    public void calcolaPercentualeCompletamento() {
        // Calcola percentuale, utility method per PickingService
        int percentuale = getPercentualeCompletamento();
        // La percentuale è calcolata ma non stored (calcolata on-demand)
    }

    /**
     * Avvia il picking (cambia stato in IN_PROGRESS)
     */
    public boolean avviaPicking(User utente) {
        if (this.stato == StatoPicking.DRAFT || this.stato == StatoPicking.ASSEGNATO) {
            this.stato = StatoPicking.IN_PROGRESS;
            this.dataInizio = new Date();
            if (utente != null && this.assegnatoA == null) {
                this.assegnatoA = utente;
            }
            return true;
        }
        return false;
    }

    /**
     * Completa il picking
     */
    public boolean completaPicking(User utente) {
        if (this.stato != StatoPicking.IN_PROGRESS) {
            return false;
        }
        if (isTuttoPrelevato()) {
            this.stato = StatoPicking.COMPLETATO;
        } else if (isParzialePrelevato()) {
            this.stato = StatoPicking.PARZIALE;
        } else {
            return false;
        }
        this.dataCompletamento = new Date();
        this.completatoDa = utente;
        return true;
    }

    /**
     * Annulla il picking
     */
    public void annullaPicking() {
        this.stato = StatoPicking.ANNULLATO;
        this.dataCompletamento = new Date();
    }

    /**
     * Calcola il tempo di picking in minuti
     */
    public Long getTempoPickingMinuti() {
        if (dataInizio == null || dataCompletamento == null) return null;
        long diffMs = dataCompletamento.getTime() - dataInizio.getTime();
        return diffMs / (60 * 1000);
    }

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

    public Ordine getOrdine() {
        return ordine;
    }

    public void setOrdine(Ordine ordine) {
        this.ordine = ordine;
    }

    public Warehouse getWarehouse() {
        return warehouse;
    }

    public void setWarehouse(Warehouse warehouse) {
        this.warehouse = warehouse;
    }

    public StatoPicking getStato() {
        return stato;
    }

    public void setStato(StatoPicking stato) {
        this.stato = stato;
    }

    public Integer getPriorita() {
        return priorita;
    }

    public void setPriorita(Integer priorita) {
        this.priorita = priorita;
    }

    public Date getDataCreazione() {
        return dataCreazione;
    }

    public void setDataCreazione(Date dataCreazione) {
        this.dataCreazione = dataCreazione;
    }

    public Date getDataInizio() {
        return dataInizio;
    }

    public void setDataInizio(Date dataInizio) {
        this.dataInizio = dataInizio;
    }

    public Date getDataCompletamento() {
        return dataCompletamento;
    }

    public void setDataCompletamento(Date dataCompletamento) {
        this.dataCompletamento = dataCompletamento;
    }

    public User getAssegnatoA() {
        return assegnatoA;
    }

    public void setAssegnatoA(User assegnatoA) {
        this.assegnatoA = assegnatoA;
    }

    public User getCompletatoDa() {
        return completatoDa;
    }

    public void setCompletatoDa(User completatoDa) {
        this.completatoDa = completatoDa;
    }

    public List<PickingItem> getItems() {
        return items;
    }

    public void setItems(List<PickingItem> items) {
        this.items = items;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public void setCreatoDa(User createdBy) {
        this.createdBy = createdBy;
    }

    public Date getDataAssegnazione() {
        return dataAssegnazione;
    }

    public void setDataAssegnazione(Date dataAssegnazione) {
        this.dataAssegnazione = dataAssegnazione;
    }

    @Override
    public String toString() {
        return "PickingList{" +
                "id=" + id +
                ", numero='" + numero + '\'' +
                ", ordine=" + (ordine != null ? ordine.getNumero() : "null") +
                ", warehouse=" + (warehouse != null ? warehouse.getNome() : "null") +
                ", stato=" + stato +
                ", items=" + (items != null ? items.size() : 0) +
                '}';
    }
}
