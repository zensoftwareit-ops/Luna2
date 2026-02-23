package it.zensoftware.luna2.model;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * CalendarAccount - Account per integrazione Google Calendar o CalDAV.
 */
@Entity
@Table(name = "calendar_accounts")
public class CalendarAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Provider provider;

    @Column(columnDefinition = "TEXT")
    private String accessToken;

    @Column(columnDefinition = "TEXT")
    private String refreshToken;

    @Column(length = 512)
    private String caldavUrl;

    @Column(length = 255)
    private String caldavUsername;

    @Column(columnDefinition = "TEXT")
    private String caldavPasswordEncrypted;

    @Column(length = 255)
    private String calendarId;

    @Column(length = 64)
    private String timeZone = "Europe/Rome";

    @Column(nullable = false)
    private Boolean syncEnabled = true;

    @Column
    private LocalDateTime lastSyncAt;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public enum Provider {
        GOOGLE, ICLOUD
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public Provider getProvider() { return provider; }
    public void setProvider(Provider provider) { this.provider = provider; }

    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }

    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }

    public String getCaldavUrl() { return caldavUrl; }
    public void setCaldavUrl(String caldavUrl) { this.caldavUrl = caldavUrl; }

    public String getCaldavUsername() { return caldavUsername; }
    public void setCaldavUsername(String caldavUsername) { this.caldavUsername = caldavUsername; }

    public String getCaldavPasswordEncrypted() { return caldavPasswordEncrypted; }
    public void setCaldavPasswordEncrypted(String caldavPasswordEncrypted) { 
        this.caldavPasswordEncrypted = caldavPasswordEncrypted; 
    }

    public String getCalendarId() { return calendarId; }
    public void setCalendarId(String calendarId) { this.calendarId = calendarId; }

    public String getTimeZone() { return timeZone; }
    public void setTimeZone(String timeZone) { this.timeZone = timeZone; }

    public Boolean getSyncEnabled() { return syncEnabled; }
    public void setSyncEnabled(Boolean syncEnabled) { this.syncEnabled = syncEnabled; }

    public LocalDateTime getLastSyncAt() { return lastSyncAt; }
    public void setLastSyncAt(LocalDateTime lastSyncAt) { this.lastSyncAt = lastSyncAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
