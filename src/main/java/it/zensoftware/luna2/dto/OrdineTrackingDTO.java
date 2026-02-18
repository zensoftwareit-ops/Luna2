package it.zensoftware.luna2.dto;

import it.zensoftware.luna2.model.Ordine;
import java.math.BigDecimal;

/**
 * Data Transfer Object per aggregare statistiche di tracciamento email associate a un ordine.
 * Fornisce metriche di engagement per ordini nella vista lista.
 */
public class OrdineTrackingDTO {
    private Ordine ordine;
    private Long totalEmails;
    private Long openedEmails;
    private Long downloadedEmails;
    private Long totalDownloads;

    public OrdineTrackingDTO(Ordine ordine, Long totalEmails, Long openedEmails, Long downloadedEmails, Long totalDownloads) {
        this.ordine = ordine;
        this.totalEmails = totalEmails;
        this.openedEmails = openedEmails;
        this.downloadedEmails = downloadedEmails;
        this.totalDownloads = totalDownloads;
    }

    // Getters
    public Ordine getOrdine() { return ordine; }
    public Long getTotalEmails() { return totalEmails != null ? totalEmails : 0L; }
    public Long getOpenedEmails() { return openedEmails != null ? openedEmails : 0L; }
    public Long getDownloadedEmails() { return downloadedEmails != null ? downloadedEmails : 0L; }
    public Long getTotalDownloads() { return totalDownloads != null ? totalDownloads : 0L; }

    /**
     * Calcola la percentuale di aperture rispetto al totale email inviate
     */
    public Double getOpenPercentage() {
        if (getTotalEmails() == 0) return 0.0;
        return (getOpenedEmails() * 100.0) / getTotalEmails();
    }

    /**
     * Calcola la percentuale di download rispetto a email aperte
     */
    public Double getDownloadPercentage() {
        if (getOpenedEmails() == 0) return 0.0;
        return (getDownloadedEmails() * 100.0) / getOpenedEmails();
    }

    /**
     * Ritorna uno stato descrittivo del tracciamento
     */
    public String getTrackingStatus() {
        if (getTotalEmails() == 0) return "Nessun invio";
        if (getOpenedEmails() == 0) return "Non aperto";
        if (getDownloadedEmails() == 0) return "Aperto";
        return "Aperto e scaricato";
    }

    /**
     * Ritorna la classe CSS Bootstrap per lo badge
     */
    public String getTrackingBadgeClass() {
        if (getTotalEmails() == 0) return "bg-secondary";
        if (getOpenedEmails() == 0) return "bg-warning";
        if (getDownloadedEmails() == 0) return "bg-info";
        return "bg-success";
    }
}
