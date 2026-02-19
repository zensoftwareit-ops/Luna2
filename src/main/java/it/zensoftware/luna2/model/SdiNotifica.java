package it.zensoftware.luna2.model;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

/**
 * Notifiche ricevute dal Sistema di Interscambio (SDI)
 * Contiene i dati delle notifiche di accettazione/rifiuto delle fatture
 */
@Entity
@Table(name = "sdi_notifiche")
public class SdiNotifica implements Serializable {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fattura_id")
    private Fattura fattura;
    
    @Column(name = "sdi_codice", nullable = false)
    private String sdiCodice;
    
    @Column(name = "stato", nullable = false)
    private String stato; // ACCETTATA, SCARTATA, ERRORE, CONSEGNATA
    
    @Column(name = "xml_risposta", columnDefinition = "TEXT")
    private String xmlRisposta;
    
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "data_ricezione")
    private Date dataRicezione;
    
    @Column(name = "letta")
    private Boolean letta = false;
    
    @Column(name = "descrizione_errore", columnDefinition = "TEXT")
    private String descrizioneErrore;
    
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "data_creazione", updatable = false)
    private Date dataCreazione;
    
    @PrePersist
    protected void onCreate() {
        dataCreazione = new Date();
        if (dataRicezione == null) {
            dataRicezione = new Date();
        }
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Fattura getFattura() { return fattura; }
    public void setFattura(Fattura fattura) { this.fattura = fattura; }
    
    public String getSdiCodice() { return sdiCodice; }
    public void setSdiCodice(String sdiCodice) { this.sdiCodice = sdiCodice; }
    
    public String getStato() { return stato; }
    public void setStato(String stato) { this.stato = stato; }
    
    public String getXmlRisposta() { return xmlRisposta; }
    public void setXmlRisposta(String xmlRisposta) { this.xmlRisposta = xmlRisposta; }
    
    public Date getDataRicezione() { return dataRicezione; }
    public void setDataRicezione(Date dataRicezione) { this.dataRicezione = dataRicezione; }
    
    public Boolean getLetta() { return letta; }
    public void setLetta(Boolean letta) { this.letta = letta; }
    
    public String getDescrizioneErrore() { return descrizioneErrore; }
    public void setDescrizioneErrore(String descrizioneErrore) { this.descrizioneErrore = descrizioneErrore; }
    
    public Date getDataCreazione() { return dataCreazione; }
}
