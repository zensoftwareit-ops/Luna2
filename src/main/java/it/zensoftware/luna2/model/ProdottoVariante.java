package it.zensoftware.luna2.model;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Entity per le varianti di un prodotto variabile
 * Esempio: Taglia S, M, L, XL oppure Colore Rosso, Blu, Verde
 */
@Entity
@Table(name = "prodotto_varianti")
public class ProdottoVariante implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prodotto_id", nullable = false)
    private Prodotto prodotto;

    @Column(nullable = false, length = 50)
    private String sku;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(name = "attributo1_nome", length = 50)
    private String attributo1Nome; // es. "Taglia"

    @Column(name = "attributo1_valore", length = 100)
    private String attributo1Valore; // es. "L"

    @Column(name = "attributo2_nome", length = 50)
    private String attributo2Nome; // es. "Colore"

    @Column(name = "attributo2_valore", length = 100)
    private String attributo2Valore; // es. "Rosso"

    @Column(name = "attributo3_nome", length = 50)
    private String attributo3Nome;

    @Column(name = "attributo3_valore", length = 100)
    private String attributo3Valore;

    @Column(name = "prezzo_differenza", precision = 15, scale = 2, nullable = false)
    private BigDecimal prezzoDifferenza = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2)
    private BigDecimal giacenza;

    @Column(name = "codice_ean", length = 50)
    private String codiceEan;

    @Column(nullable = false)
    private Boolean disponibile = true;

    @Column(columnDefinition = "TEXT")
    private String note;

    // Constructors
    public ProdottoVariante() {}

    public ProdottoVariante(String sku, String nome) {
        this.sku = sku;
        this.nome = nome;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Prodotto getProdotto() {
        return prodotto;
    }

    public void setProdotto(Prodotto prodotto) {
        this.prodotto = prodotto;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getAttributo1Nome() {
        return attributo1Nome;
    }

    public void setAttributo1Nome(String attributo1Nome) {
        this.attributo1Nome = attributo1Nome;
    }

    public String getAttributo1Valore() {
        return attributo1Valore;
    }

    public void setAttributo1Valore(String attributo1Valore) {
        this.attributo1Valore = attributo1Valore;
    }

    public String getAttributo2Nome() {
        return attributo2Nome;
    }

    public void setAttributo2Nome(String attributo2Nome) {
        this.attributo2Nome = attributo2Nome;
    }

    public String getAttributo2Valore() {
        return attributo2Valore;
    }

    public void setAttributo2Valore(String attributo2Valore) {
        this.attributo2Valore = attributo2Valore;
    }

    public String getAttributo3Nome() {
        return attributo3Nome;
    }

    public void setAttributo3Nome(String attributo3Nome) {
        this.attributo3Nome = attributo3Nome;
    }

    public String getAttributo3Valore() {
        return attributo3Valore;
    }

    public void setAttributo3Valore(String attributo3Valore) {
        this.attributo3Valore = attributo3Valore;
    }

    public BigDecimal getPrezzoDifferenza() {
        return prezzoDifferenza;
    }

    public void setPrezzoDifferenza(BigDecimal prezzoDifferenza) {
        this.prezzoDifferenza = prezzoDifferenza;
    }

    public BigDecimal getGiacenza() {
        return giacenza;
    }

    public void setGiacenza(BigDecimal giacenza) {
        this.giacenza = giacenza;
    }

    public String getCodiceEan() {
        return codiceEan;
    }

    public void setCodiceEan(String codiceEan) {
        this.codiceEan = codiceEan;
    }

    public Boolean getDisponibile() {
        return disponibile;
    }

    public void setDisponibile(Boolean disponibile) {
        this.disponibile = disponibile;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    /**
     * Calcola il prezzo finale della variante
     */
    public BigDecimal getPrezzoFinale() {
        if (prodotto != null && prodotto.getPrezzoBase() != null) {
            return prodotto.getPrezzoBase().add(prezzoDifferenza);
        }
        return prezzoDifferenza;
    }

    /**
     * Restituisce una descrizione completa della variante
     */
    public String getDescrizioneCompleta() {
        StringBuilder sb = new StringBuilder(nome);
        if (attributo1Nome != null && attributo1Valore != null) {
            sb.append(" - ").append(attributo1Nome).append(": ").append(attributo1Valore);
        }
        if (attributo2Nome != null && attributo2Valore != null) {
            sb.append(", ").append(attributo2Nome).append(": ").append(attributo2Valore);
        }
        if (attributo3Nome != null && attributo3Valore != null) {
            sb.append(", ").append(attributo3Nome).append(": ").append(attributo3Valore);
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProdottoVariante)) return false;
        ProdottoVariante that = (ProdottoVariante) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
