package it.zensoftware.luna2.dto;

import it.zensoftware.luna2.model.Preventivo;

/**
 * Data Transfer Object per Preventivo con dati di tracking allegati
 */
public class PreventivoTrackingDTO {
    private Preventivo preventivo;
    private Long totalEmails;       // Total emails sent
    private Long openedEmails;      // Emails aperte
    private Long downloadedEmails;  // Emails with downloads
    private Long totalDownloads;    // Total number of downloads
    
    public PreventivoTrackingDTO(Preventivo preventivo, Long totalEmails, Long openedEmails, Long downloadedEmails, Long totalDownloads) {
        this.preventivo = preventivo;
        this.totalEmails = totalEmails != null ? totalEmails : 0L;
        this.openedEmails = openedEmails != null ? openedEmails : 0L;
        this.downloadedEmails = downloadedEmails != null ? downloadedEmails : 0L;
        this.totalDownloads = totalDownloads != null ? totalDownloads : 0L;
    }

    // Getters
    public Preventivo getPreventivo() {
        return preventivo;
    }

    public Long getTotalEmails() {
        return totalEmails;
    }

    public Long getOpenedEmails() {
        return openedEmails;
    }

    public Long getDownloadedEmails() {
        return downloadedEmails;
    }

    public Long getTotalDownloads() {
        return totalDownloads;
    }

    // Calculated fields
    public Double getOpenPercentage() {
        if (totalEmails == 0) return 0.0;
        return (openedEmails.doubleValue() / totalEmails.doubleValue()) * 100;
    }

    public Double getDownloadPercentage() {
        if (totalEmails == 0) return 0.0;
        return (downloadedEmails.doubleValue() / totalEmails.doubleValue()) * 100;
    }

    public String getOpenPercentageFormatted() {
        return String.format("%.1f%%", getOpenPercentage());
    }

    public String getDownloadPercentageFormatted() {
        return String.format("%.1f%%", getDownloadPercentage());
    }

    public String getTrackingStatus() {
        if (totalEmails == 0) return "Non inviato";
        if (openedEmails > 0) return "Aperto";
        if (downloadedEmails > 0) return "Scaricato";
        return "Inviato";
    }

    public String getTrackingBadgeClass() {
        if (totalEmails == 0) return "badge-secondary";
        if (downloadedEmails > 0) return "badge-success";
        if (openedEmails > 0) return "badge-info";
        return "badge-warning";
    }
}
