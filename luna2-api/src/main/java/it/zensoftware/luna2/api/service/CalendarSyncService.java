package it.zensoftware.luna2.api.service;

import it.zensoftware.luna2.dao.CalendarAccountDAO;
import it.zensoftware.luna2.dao.CalendarEventDAO;
import it.zensoftware.luna2.model.CalendarAccount;
import it.zensoftware.luna2.model.CalendarEvent;
import it.zensoftware.luna2.service.calendar.CalDavCalendarProvider;
import it.zensoftware.luna2.service.calendar.CalendarProvider;
import it.zensoftware.luna2.service.calendar.GoogleCalendarProvider;
import it.zensoftware.luna2.service.calendar.PasswordEncryptionService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * CalendarSyncService - Sincronizzazione bidirezionale calendario.
 * 
 * 1. PULL: Scarica eventi da provider esterno (Google Calendar, iCloud)
 * 2. PUSH: Carica nuovi/modificati eventi locali al provider
 * 
 * Utilizzo:
 * ```
 * CalendarSyncService syncService = new CalendarSyncService();
 * SyncResult result = syncService.syncCalendars(providerId, accountId);
 * ```
 */
public class CalendarSyncService {

    private static final Logger logger = LogManager.getLogger(CalendarSyncService.class);

    private final CalendarAccountDAO accountDAO = new CalendarAccountDAO();
    private final CalendarEventDAO eventDAO = new CalendarEventDAO();

    /**
     * Sincronizzazione completa bidirezionale.
     * 
     * @param provider ID provider (google, icloud, etc.)
     * @param accountId ID account calendar nel DB
     * @return SyncResult con statistiche
     */
    public SyncResult syncCalendars(String provider, Long accountId) {
        logger.info("Inizio sincronizzazione calendario: provider={}, accountId={}", provider, accountId);

        SyncResult result = new SyncResult(provider, LocalDateTime.now());

        try {
            if (accountId != null) {
                CalendarAccount account = accountDAO.findById(accountId);
                if (account == null) {
                    throw new IllegalArgumentException("CalendarAccount non trovato");
                }
                syncAccount(account, result);
            } else {
                List<CalendarAccount> accounts = accountDAO.findEnabled();
                for (CalendarAccount account : accounts) {
                    if (provider != null && !provider.isEmpty()
                            && !account.getProvider().name().equalsIgnoreCase(provider)) {
                        continue;
                    }
                    syncAccount(account, result);
                }
            }

            result.setSuccess(true);
        } catch (Exception e) {
            logger.error("Errore sincronizzazione calendario", e);
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
        }

        logger.info("Sincronizzazione completata: {}", result);
        return result;
    }

    /**
     * PULL: Scarica eventi da provider esterno verso DB locale.
     * 
     * Logica:
     * 1. Connetti a provider (CalDAV per iCloud, Google Calendar API per Google)
     * 2. Fetch tutti gli eventi del calendario esterno
     * 3. Per ogni evento esterno:
     *    - Se externalEventId non esiste nel DB → salva nuovo
     *    - Se esiste e modificato → aggiorna
     *    - Se cancellerllo da esterno → cancella dal DB
     * 4. Aggiorna lastSyncedAt
     */
    private void syncAccount(CalendarAccount account, SyncResult result) {
        CalendarProvider provider = getProvider(account);
        if (provider == null) {
            return;
        }

        LocalDateTime from = LocalDateTime.now().minusMonths(3);
        LocalDateTime to = LocalDateTime.now().plusMonths(3);

        // PULL
        List<CalendarEvent> externalEvents = provider.listEvents(account, from, to);
        for (CalendarEvent event : externalEvents) {
            event.setUserId(account.getUserId());
            event.setProvider(account.getProvider());
            if (event.getSourceType() == null) {
                event.setSourceType(CalendarEvent.SourceType.MEETING);
            }
            if (event.getSourceId() == null) {
                event.setSourceId(0L);
            }

            CalendarEvent existing = null;
            if (event.getExternalEventId() != null && !event.getExternalEventId().isEmpty()) {
                existing = eventDAO.findByExternalId(account.getUserId(), account.getProvider(), event.getExternalEventId());
            }

            if (existing == null) {
                eventDAO.save(event);
                result.addDownloadedEvents(1);
            } else {
                existing.setTitle(event.getTitle());
                existing.setDescription(event.getDescription());
                existing.setLocation(event.getLocation());
                existing.setStartTime(event.getStartTime());
                existing.setEndTime(event.getEndTime());
                existing.setUpdatedAt(LocalDateTime.now());
                eventDAO.update(existing);
            }
        }

        // PUSH
        List<CalendarEvent> localEvents = eventDAO.findByUserId(account.getUserId());
        for (CalendarEvent event : localEvents) {
            if (event.getProvider() != account.getProvider()) {
                continue;
            }

            if (event.getStatus() == CalendarEvent.Status.DELETED && event.getExternalEventId() != null) {
                provider.deleteEvent(account, event.getExternalEventId());
                result.addUploadedEvents(1);
                continue;
            }

            if (event.getExternalEventId() == null || event.getExternalEventId().isEmpty()) {
                String externalId = provider.createOrUpdateEvent(account, event);
                if (externalId != null) {
                    event.setExternalEventId(externalId);
                    event.setUpdatedAt(LocalDateTime.now());
                    eventDAO.update(event);
                    result.addUploadedEvents(1);
                }
            }
        }

        account.setLastSyncAt(LocalDateTime.now());
        account.setUpdatedAt(LocalDateTime.now());
        accountDAO.update(account);
    }

    /**
     * PUSH: Carica eventi modificati dal DB verso provider esterno.
     * 
     * Logica:
     * 1. Recupera eventi con status PENDING_UPLOAD o lastModified > lastSyncedAt
     * 2. Per ogni evento:
     *    - Se nuovo → crea su provider (POST)
     *    - Se modificato → aggiorna su provider (PUT)
     *    - Se cancelled → cancella da provider (DELETE)
     * 3. Risincronizza per confirmare
     * 4. Aggiorna status a SYNCED
     */
    private CalendarProvider getProvider(CalendarAccount account) {
        if (account.getProvider() == CalendarAccount.Provider.GOOGLE) {
            return new GoogleCalendarProvider();
        }

        if (account.getProvider() == CalendarAccount.Provider.ICLOUD) {
            CalDavCalendarProvider caldav = new CalDavCalendarProvider();
            caldav.initialize(account, new PasswordEncryptionService(getEncryptionKey()));
            return caldav;
        }

        return null;
    }

    /**
     * Cancella eventi da provider esterno (soft delete nel DB).
     */
    public void deleteEventFromProvider(String provider, Long accountId, String externalEventId) {
        if (accountId == null) {
            return;
        }
        CalendarAccount account = accountDAO.findById(accountId);
        if (account == null) {
            return;
        }
        CalendarProvider calendarProvider = getProvider(account);
        if (calendarProvider != null) {
            calendarProvider.deleteEvent(account, externalEventId);
        }
    }

    /**
     * Risultato della sincronizzazione.
     */
    
    public static class SyncResult {
        public String provider;
        public LocalDateTime timestamp;
        public boolean success;
        public Integer downloadedEvents = 0;
        public Integer uploadedEvents = 0;
        public String errorMessage;
        public List<String> warnings = new ArrayList<>();

        public SyncResult(String provider, LocalDateTime timestamp) {
            this.provider = provider;
            this.timestamp = timestamp;
        }

        public void addDownloadedEvents(int count) {
            this.downloadedEvents += count;
        }

        public void addUploadedEvents(int count) {
            this.uploadedEvents += count;
        }

        public void addWarning(String warning) {
            this.warnings.add(warning);
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }

        @Override
        public String toString() {
            return String.format(
                "SyncResult{provider=%s, downloaded=%d, uploaded=%d, success=%s, errors=%s}",
                provider, downloadedEvents, uploadedEvents, success, errorMessage
            );
        }
    }

    private String getEncryptionKey() {
        Properties props = new Properties();
        try (InputStream is = this.getClass().getClassLoader().getResourceAsStream("application.properties")) {
            if (is != null) {
                props.load(is);
                String key = props.getProperty("calendar.encryption.secret");
                if (key != null && !key.isEmpty()) {
                    return key;
                }
            }
        } catch (Exception e) {
            logger.warn("Impossibile leggere encryption key da application.properties", e);
        }
        return "DefaultCalendarSecretKey2026";
    }
}
