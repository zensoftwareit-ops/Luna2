package it.zensoftware.luna2.model;

import javax.persistence.*;
import java.util.Date;

/**
 * JPA Entity per tracciare trasmissioni E-invoicing a SDI
 *
 * Traccia:
 * - ID trasmissione assegnato da SDI
 * - Stato (SUBMITTED, ACCEPTED, REJECTED, DELIVERED)
 * - Timestamp di creazione/ultimo polling
 * - Errori riscontrati
 */
@Entity
@Table(name = "sdi_transmissions")
public class SdiTransmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_trasmissione", unique = true, nullable = false)
    private String idTrasmissione;

    @Column(name = "fattura_id", nullable = false)
    private Long fatturaId;

    @Column(name = "numero_fattura")
    private String numeroFattura;

    @Enumerated(EnumType.STRING)
    @Column(name = "stato", nullable = false)
    private StatoTrasmissione stato;

    @Column(name = "data_creazione", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date dataCreazione;

    @Column(name = "data_ultimo_polling")
    @Temporal(TemporalType.TIMESTAMP)
    private Date dataUltimoPolling;

    @Column(name = "data_notifica")
    @Temporal(TemporalType.TIMESTAMP)
    private Date dataNotifica;

    @Column(name = "errore", length = 1000)
    private String errore;

    @Column(name = "descrizione_errore", length = 2000)
    private String descrizioneErrore;

    @Column(name = "numero_tentativi", columnDefinition = "INT DEFAULT 0")
    private Integer numeroTentativi;

    @Column(name = "xml_contenuto", columnDefinition = "LONGTEXT")
    private String xmlContenuto;

    // ==================== CONSTRUCTOR ====================

    public SdiTransmission() {
        this.stato = StatoTrasmissione.SUBMITTED;
        this.dataCreazione = new Date();
        this.numeroTentativi = 0;
    }

    // ==================== ENUM ====================

    public enum StatoTrasmissione {
        SUBMITTED,      // Appena inviata a SDI
        ACCEPTED,       // Accettata da SDI
        REJECTED,       // Rifiutata da SDI
        DELIVERED,      // Consegnata al SdI
        ERROR,          // Errore nella trasmissione
        PENDING         // In attesa di risposta
    }

    // ==================== BUSINESS METHODS ====================

    public boolean isPendingResponse() {
        return stato == StatoTrasmissione.SUBMITTED || stato == StatoTrasmissione.PENDING;
    }

    public boolean hasError() {
        return stato == StatoTrasmissione.REJECTED || stato == StatoTrasmissione.ERROR;
    }

    public void recordAttempt() {
        this.numeroTentativi = (numeroTentativi != null ? numeroTentativi : 0) + 1;
        this.dataUltimoPolling = new Date();
    }

    // ==================== GETTERS & SETTERS ====================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getIdTrasmissione() { return idTrasmissione; }
    public void setIdTrasmissione(String idTrasmissione) { this.idTrasmissione = idTrasmissione; }

    public Long getFatturaId() { return fatturaId; }
    public void setFatturaId(Long fatturaId) { this.fatturaId = fatturaId; }

    public String getNumeroFattura() { return numeroFattura; }
    public void setNumeroFattura(String numeroFattura) { this.numeroFattura = numeroFattura; }

    public StatoTrasmissione getStato() { return stato; }
    public void setStato(StatoTrasmissione stato) { this.stato = stato; }

    public Date getDataCreazione() { return dataCreazione; }
    public void setDataCreazione(Date dataCreazione) { this.dataCreazione = dataCreazione; }

    public Date getDataUltimoPolling() { return dataUltimoPolling; }
    public void setDataUltimoPolling(Date dataUltimoPolling) { this.dataUltimoPolling = dataUltimoPolling; }

    public Date getDataNotifica() { return dataNotifica; }
    public void setDataNotifica(Date dataNotifica) { this.dataNotifica = dataNotifica; }

    public String getErrore() { return errore; }
    public void setErrore(String errore) { this.errore = errore; }

    public String getDescrizioneErrore() { return descrizioneErrore; }
    public void setDescrizioneErrore(String descrizioneErrore) { this.descrizioneErrore = descrizioneErrore; }

    public Integer getNumeroTentativi() { return numeroTentativi; }
    public void setNumeroTentativi(Integer numeroTentativi) { this.numeroTentativi = numeroTentativi; }

    public String getXmlContenuto() { return xmlContenuto; }
    public void setXmlContenuto(String xmlContenuto) { this.xmlContenuto = xmlContenuto; }
}
