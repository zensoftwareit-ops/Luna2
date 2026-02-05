package it.zensoftware.luna2.model;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Entity class for Cliente
 */
@Entity
@Table(name = "clienti")
public class Cliente implements Serializable {
    
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_anagrafica", nullable = false)
    private TipoAnagrafica tipoAnagrafica = TipoAnagrafica.CLIENTE;

    @Column(name = "ragione_sociale", nullable = false)
    private String ragioneSociale;

    @Column(name = "partita_iva", length = 20)
    private String partitaIva;

    @Column(name = "codice_fiscale", length = 20)
    private String codiceFiscale;

    @Column(name = "codice_sdi", length = 10)
    private String codiceSdi;

    @Column(length = 100)
    private String pec;

    @Column(length = 30)
    private String telefono;

    @Column(length = 100)
    private String email;

    @Column(name = "sito_web")
    private String sitoWeb;

    private String indirizzo;

    @Column(length = 100)
    private String citta;

    @Column(length = 2)
    private String provincia;

    @Column(length = 10)
    private String cap;

    @Column(length = 100)
    private String paese = "Italia";

    @Column(name = "codice_cliente", unique = true, length = 50)
    private String codiceCliente;

    @Column(name = "condizioni_pagamento", length = 100)
    private String condizioniPagamento;

    @Column(name = "listino_default")
    private Long listinoDefault;

    @Column(name = "sconto_percentuale", precision = 5, scale = 2)
    private BigDecimal scontoPercentuale = BigDecimal.ZERO;

    @Column(name = "fido_massimo", precision = 15, scale = 2)
    private BigDecimal fidoMassimo;

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
    // @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL, orphanRemoval = true)
    // @OrderBy("nome ASC")
    private transient List<Contatto> contatti = new ArrayList<>();

    public enum TipoAnagrafica {
        CLIENTE, PROSPECT
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
    public Cliente() {}

    public Cliente(String ragioneSociale) {
        this.ragioneSociale = ragioneSociale;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public TipoAnagrafica getTipoAnagrafica() {
        return tipoAnagrafica;
    }

    public void setTipoAnagrafica(TipoAnagrafica tipoAnagrafica) {
        this.tipoAnagrafica = tipoAnagrafica;
    }

    public String getRagioneSociale() {
        return ragioneSociale;
    }

    public void setRagioneSociale(String ragioneSociale) {
        this.ragioneSociale = ragioneSociale;
    }

    public String getPartitaIva() {
        return partitaIva;
    }

    public void setPartitaIva(String partitaIva) {
        this.partitaIva = partitaIva;
    }

    public String getCodiceFiscale() {
        return codiceFiscale;
    }

    public void setCodiceFiscale(String codiceFiscale) {
        this.codiceFiscale = codiceFiscale;
    }

    public String getCodiceSdi() {
        return codiceSdi;
    }

    public void setCodiceSdi(String codiceSdi) {
        this.codiceSdi = codiceSdi;
    }

    public String getPec() {
        return pec;
    }

    public void setPec(String pec) {
        this.pec = pec;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSitoWeb() {
        return sitoWeb;
    }

    public void setSitoWeb(String sitoWeb) {
        this.sitoWeb = sitoWeb;
    }

    public String getIndirizzo() {
        return indirizzo;
    }

    public void setIndirizzo(String indirizzo) {
        this.indirizzo = indirizzo;
    }

    public String getCitta() {
        return citta;
    }

    public void setCitta(String citta) {
        this.citta = citta;
    }

    public String getProvincia() {
        return provincia;
    }

    public void setProvincia(String provincia) {
        this.provincia = provincia;
    }

    public String getCap() {
        return cap;
    }

    public void setCap(String cap) {
        this.cap = cap;
    }

    public String getPaese() {
        return paese;
    }

    public void setPaese(String paese) {
        this.paese = paese;
    }

    public String getCodiceCliente() {
        return codiceCliente;
    }

    public void setCodiceCliente(String codiceCliente) {
        this.codiceCliente = codiceCliente;
    }

    public String getCondizioniPagamento() {
        return condizioniPagamento;
    }

    public void setCondizioniPagamento(String condizioniPagamento) {
        this.condizioniPagamento = condizioniPagamento;
    }

    public Long getListinoDefault() {
        return listinoDefault;
    }

    public void setListinoDefault(Long listinoDefault) {
        this.listinoDefault = listinoDefault;
    }

    public BigDecimal getScontoPercentuale() {
        return scontoPercentuale;
    }

    public void setScontoPercentuale(BigDecimal scontoPercentuale) {
        this.scontoPercentuale = scontoPercentuale;
    }

    public BigDecimal getFidoMassimo() {
        return fidoMassimo;
    }

    public void setFidoMassimo(BigDecimal fidoMassimo) {
        this.fidoMassimo = fidoMassimo;
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

    public List<Contatto> getContatti() {
        return contatti;
    }

    public void setContatti(List<Contatto> contatti) {
        this.contatti = contatti;
    }

    public void addContatto(Contatto contatto) {
        contatti.add(contatto);
        contatto.setCliente(this);
    }

    public void removeContatto(Contatto contatto) {
        contatti.remove(contatto);
        contatto.setCliente(null);
    }

    public String getIndirizzoCompleto() {
        StringBuilder sb = new StringBuilder();
        if (indirizzo != null && !indirizzo.isEmpty()) {
            sb.append(indirizzo);
        }
        if (cap != null && !cap.isEmpty()) {
            sb.append(" ").append(cap);
        }
        if (citta != null && !citta.isEmpty()) {
            sb.append(" ").append(citta);
        }
        if (provincia != null && !provincia.isEmpty()) {
            sb.append(" (").append(provincia).append(")");
        }
        if (paese != null && !paese.isEmpty() && !"Italia".equals(paese)) {
            sb.append(" - ").append(paese);
        }
        return sb.toString().trim();
    }

    @Override
    public String toString() {
        return "Cliente{" +
                "id=" + id +
                ", ragioneSociale='" + ragioneSociale + '\'' +
                ", partitaIva='" + partitaIva + '\'' +
                ", email='" + email + '\'' +
                ", attivo=" + attivo +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Cliente)) return false;
        Cliente cliente = (Cliente) o;
        return id != null && id.equals(cliente.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
