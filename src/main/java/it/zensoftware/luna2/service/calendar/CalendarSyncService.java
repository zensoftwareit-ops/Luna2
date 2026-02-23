package it.zensoftware.luna2.service.calendar;

import it.zensoftware.luna2.dao.CalendarAccountDAO;
import it.zensoftware.luna2.dao.CalendarEventDAO;
import it.zensoftware.luna2.dao.ReminderDAO;
import it.zensoftware.luna2.model.CalendarAccount;
import it.zensoftware.luna2.model.CalendarEvent;
import it.zensoftware.luna2.model.Reminder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Sincronizza Reminder con provider calendario esterni.
 */
public class CalendarSyncService {

    private static final Logger logger = LogManager.getLogger(CalendarSyncService.class);

    private final CalendarAccountDAO accountDAO = new CalendarAccountDAO();
    private final CalendarEventDAO eventDAO = new CalendarEventDAO();
    private final ReminderDAO reminderDAO = new ReminderDAO();

    private final Map<CalendarAccount.Provider, CalendarProvider> providers = new HashMap<>();

    public CalendarSyncService() {
        providers.put(CalendarAccount.Provider.GOOGLE, new GoogleCalendarProvider());
        providers.put(CalendarAccount.Provider.ICLOUD, new CalDavCalendarProvider());
    }

    public void syncAllAccounts() {
        List<CalendarAccount> accounts = accountDAO.findEnabled();
        for (CalendarAccount account : accounts) {
            try {
                syncAccount(account);
            } catch (Exception e) {
                logger.warn("Errore sync calendario per utente: " + account.getUserId(), e);
            }
        }
    }

    public void syncAccount(CalendarAccount account) {
        CalendarProvider provider = providers.get(account.getProvider());
        if (provider == null) {
            logger.warn("Provider calendario non supportato: " + account.getProvider());
            return;
        }

        // Inizializza provider CalDAV con credenziali dal CalendarAccount
        if (provider instanceof CalDavCalendarProvider) {
            CalDavCalendarProvider caldavProvider = (CalDavCalendarProvider) provider;
            String encryptionKey = getEncryptionKey();
            PasswordEncryptionService encryptionService = new PasswordEncryptionService(encryptionKey);
            caldavProvider.initialize(account, encryptionService);
        }

        // Sync reminders -> calendario esterno (placeholder)
        Date now = new Date();
        Date future = new Date(now.getTime() + (1000L * 60 * 60 * 24 * 30));
        List<Reminder> reminders = reminderDAO.findBetweenDates(now, future);

        for (Reminder reminder : reminders) {
            String userId = String.valueOf(reminder.getUtente().getId());
            CalendarEvent existing = eventDAO.findBySource(userId, account.getProvider(), CalendarEvent.SourceType.REMINDER, reminder.getId());
            CalendarEvent event = existing != null ? existing : buildEventFromReminder(reminder, account.getProvider());

            String externalId = provider.createOrUpdateEvent(account, event);

            if (externalId != null && (event.getExternalEventId() == null || event.getExternalEventId().isEmpty())) {
                event.setExternalEventId(externalId);
            }

            if (event.getId() == null) {
                eventDAO.save(event);
            } else {
                eventDAO.update(event);
            }
        }

        account.setLastSyncAt(LocalDateTime.now());
        account.setUpdatedAt(LocalDateTime.now());
        accountDAO.update(account);
    }

    private CalendarEvent buildEventFromReminder(Reminder reminder, CalendarAccount.Provider provider) {
        CalendarEvent event = new CalendarEvent();
        event.setUserId(String.valueOf(reminder.getUtente().getId()));
        event.setProvider(provider);
        event.setTitle(reminder.getTitolo() != null ? reminder.getTitolo() : "Reminder");
        event.setDescription(reminder.getMessaggio());

        LocalDateTime start = toLocalDateTime(reminder.getDataNotifica());
        LocalDateTime end = start.plusMinutes(30);
        event.setStartTime(start);
        event.setEndTime(end);

        event.setSourceType(CalendarEvent.SourceType.REMINDER);
        event.setSourceId(reminder.getId());
        return event;
    }

    private LocalDateTime toLocalDateTime(Date date) {
        return Instant.ofEpochMilli(date.getTime()).atZone(ZoneId.systemDefault()).toLocalDateTime();
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
        // Default key (should be env var in production)
        return "DefaultCalendarSecretKey2026";
    }
}
