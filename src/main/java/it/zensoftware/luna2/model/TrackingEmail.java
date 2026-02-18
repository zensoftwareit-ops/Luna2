package it.zensoftware.luna2.model;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "tracking_email")
public class TrackingEmail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tracking_id", unique = true, nullable = false)
    private String trackingId;  // UUID univoco per ogni invio

    @ManyToOne
    @JoinColumn(name = "preventivo_id")
    private Preventivo preventivo;

    @Column(name = "email_destinatario")
    private String emailDestinatario;

    @Column(name = "data_invio")
    private Date dataInvio;

    @Column(name = "aperto")
    private Boolean aperto = false;

    @Column(name = "data_apertura")
    private Date dataApertura;

    @Column(name = "click_download")
    private Integer clickDownload = 0;

    @Column(name = "data_primo_download")
    private Date dataPrimoDownload;

    @Column(name = "data_ultimo_download")
    private Date dataUltimoDownload;

    @Column(name = "user_agent_apertura")
    private String userAgentApertura;

    @Column(name = "user_agent_download")
    private String userAgentDownload;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTrackingId() { return trackingId; }
    public void setTrackingId(String trackingId) { this.trackingId = trackingId; }

    public Preventivo getPreventivo() { return preventivo; }
    public void setPreventivo(Preventivo preventivo) { this.preventivo = preventivo; }

    public String getEmailDestinatario() { return emailDestinatario; }
    public void setEmailDestinatario(String emailDestinatario) { this.emailDestinatario = emailDestinatario; }

    public Date getDataInvio() { return dataInvio; }
    public void setDataInvio(Date dataInvio) { this.dataInvio = dataInvio; }

    public Boolean getAperto() { return aperto; }
    public void setAperto(Boolean aperto) { this.aperto = aperto; }

    public Date getDataApertura() { return dataApertura; }
    public void setDataApertura(Date dataApertura) { this.dataApertura = dataApertura; }

    public Integer getClickDownload() { return clickDownload; }
    public void setClickDownload(Integer clickDownload) { this.clickDownload = clickDownload; }

    public Date getDataPrimoDownload() { return dataPrimoDownload; }
    public void setDataPrimoDownload(Date dataPrimoDownload) { this.dataPrimoDownload = dataPrimoDownload; }

    public Date getDataUltimoDownload() { return dataUltimoDownload; }
    public void setDataUltimoDownload(Date dataUltimoDownload) { this.dataUltimoDownload = dataUltimoDownload; }

    public String getUserAgentApertura() { return userAgentApertura; }
    public void setUserAgentApertura(String userAgentApertura) { this.userAgentApertura = userAgentApertura; }

    public String getUserAgentDownload() { return userAgentDownload; }
    public void setUserAgentDownload(String userAgentDownload) { this.userAgentDownload = userAgentDownload; }
}
