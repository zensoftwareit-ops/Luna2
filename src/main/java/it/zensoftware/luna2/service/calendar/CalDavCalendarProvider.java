package it.zensoftware.luna2.service.calendar;

import it.zensoftware.luna2.model.CalendarAccount;
import it.zensoftware.luna2.model.CalendarEvent;
import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.Summary;
import net.fortuna.ical4j.model.property.Description;
import net.fortuna.ical4j.model.property.DtStart;
import net.fortuna.ical4j.model.property.DtEnd;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * CalDAV Calendar Provider - Accesso a calendari iCloud e altri via CalDAV (WebDAV).
 * Implementazione semplificata usando HTTP nativo (no dipendenza Sardine non disponibile).
 */
public class CalDavCalendarProvider implements CalendarProvider {

    private static final Logger logger = LogManager.getLogger(CalDavCalendarProvider.class);
    private String caldavUrl;
    private String username;
    private String decryptedPassword;
    private PasswordEncryptionService encryptionService;

    // Costruttore senza parametri per CalendarSyncService
    public CalDavCalendarProvider() {
        this.caldavUrl = null;
        this.username = null;
        this.decryptedPassword = null;
    }

    public CalDavCalendarProvider(String username, String encryptedPassword, String caldavUrl, 
                                  PasswordEncryptionService encryptionService) {
        this.caldavUrl = caldavUrl;
        this.username = username;
        this.encryptionService = encryptionService;
        this.decryptedPassword = encryptionService.decrypt(encryptedPassword);
    }

    @Override
    public CalendarAccount.Provider getProvider() {
        return CalendarAccount.Provider.ICLOUD;
    }

    /**
     * Inizializza il provider con i dati del CalendarAccount.
     * Usato da CalendarSyncService per configurare il provider dinamicamente.
     */
    public void initialize(CalendarAccount account, PasswordEncryptionService encryptionService) {
        if (account == null) {
            logger.warn("CalendarAccount null per CalDavCalendarProvider");
            return;
        }
        this.username = account.getCaldavUsername();
        this.caldavUrl = account.getCaldavUrl();
        this.encryptionService = encryptionService;
        if (account.getCaldavPasswordEncrypted() != null && encryptionService != null) {
            this.decryptedPassword = encryptionService.decrypt(account.getCaldavPasswordEncrypted());
        }
    }

    @Override
    public List<CalendarEvent> listEvents(CalendarAccount account, LocalDateTime from, LocalDateTime to) {
        logger.debug("CalDAV listEvents for user: {}", account.getUserId());
        if (caldavUrl == null || caldavUrl.isEmpty() || decryptedPassword == null) {
            logger.warn("CalDAV URL o password mancanti");
            return Collections.emptyList();
        }

        List<CalendarEvent> events = new ArrayList<>();
        try {
            String icsContent = getCalendarContent();
            if (icsContent != null && !icsContent.isEmpty()) {
                List<CalendarEvent> parsedEvents = parseICSContent(icsContent, account);
                for (CalendarEvent event : parsedEvents) {
                    if (isEventInRange(event, from, to)) {
                        events.add(event);
                    }
                }
            }
            logger.info("Sincronizzati {} eventi (range {}-{}) da CalDAV", events.size(), from, to);
        } catch (Exception e) {
            logger.error("Errore sincronizzazione CalDAV", e);
        }

        return events;
    }

    @Override
    public String createOrUpdateEvent(CalendarAccount account, CalendarEvent event) {
        logger.debug("CalDAV createOrUpdateEvent for user: {}", account.getUserId());
        // Placeholder: creazione evento su CalDAV richiede MKCALENDAR/PUT
        logger.info("CalDAV event creation placeholder (bidirezionale completo in sviluppo)");
        return event.getExternalEventId();
    }

    @Override
    public void deleteEvent(CalendarAccount account, String externalEventId) {
        logger.debug("CalDAV deleteEvent for user: {}", account.getUserId());
        // Placeholder: eliminazione richiede DELETE su resource CalDAV
        logger.info("CalDAV event deletion placeholder");
    }

    public boolean testConnection() {
        if (caldavUrl == null || caldavUrl.isEmpty() || decryptedPassword == null) {
            logger.warn("CalDAV URL o password mancante per test");
            return false;
        }

        try {
            // Prova GET semplice sul URL CalDAV
            HttpURLConnection conn = (HttpURLConnection) new URL(caldavUrl).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            addBasicAuth(conn);

            int responseCode = conn.getResponseCode();
            boolean success = responseCode >= 200 && responseCode < 300;
            logger.info("CalDAV test connection: status {}", responseCode);
            return success;
        } catch (Exception e) {
            logger.warn("Connessione CalDAV fallita: {}", e.getMessage());
            return false;
        }
    }

    private String getCalendarContent() throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(caldavUrl).openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);
        addBasicAuth(conn);

        if (conn.getResponseCode() != 200) {
            logger.warn("Errore GET CalDAV: {}", conn.getResponseCode());
            return null;
        }

        StringBuilder content = new StringBuilder();
        try (InputStream is = conn.getInputStream()) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) != -1) {
                content.append(new String(buffer, 0, length, StandardCharsets.UTF_8));
            }
        }

        return content.toString();
    }

    private List<CalendarEvent> parseICSContent(String icsContent, CalendarAccount account) {
        List<CalendarEvent> events = new ArrayList<>();
        try {
            CalendarBuilder builder = new CalendarBuilder();
            Calendar calendar = builder.build(new StringReader(icsContent));

            for (Object comp : calendar.getComponents()) {
                if (comp instanceof VEvent) {
                    VEvent vevent = (VEvent) comp;
                    CalendarEvent event = new CalendarEvent();
                    
                    // Imposta l'identificativo esterno dell'evento
                    try {
                        if (vevent.getUid() != null) {
                            event.setExternalEventId(vevent.getUid().getValue());
                        }
                    } catch (Exception e) {
                        logger.debug("Warning: unable to get UID from event", e);
                    }

                    // Estrai title (Summary) - ical4j API compatibility
                    try {
                        Summary summary = (Summary) vevent.getProperty("SUMMARY");
                        if (summary != null && summary.getValue() != null) {
                            event.setTitle(summary.getValue());
                        }
                    } catch (Exception e) {
                        logger.debug("Warning: unable to get SUMMARY from event", e);
                    }

                    // Estrai description
                    try {
                        Description description = (Description) vevent.getProperty("DESCRIPTION");
                        if (description != null && description.getValue() != null) {
                            event.setDescription(description.getValue());
                        }
                    } catch (Exception e) {
                        logger.debug("Warning: unable to get DESCRIPTION from event", e);
                    }

                    // Estrai data/ora inizio
                    try {
                        DtStart dtStart = (DtStart) vevent.getProperty("DTSTART");
                        if (dtStart != null && dtStart.getValue() != null) {
                            event.setStartTime(parseCalendarDate(dtStart.getValue()));
                        }
                    } catch (Exception e) {
                        logger.debug("Warning: unable to get DTSTART from event", e);
                    }

                    // Estrai data/ora fine
                    try {
                        DtEnd dtEnd = (DtEnd) vevent.getProperty("DTEND");
                        if (dtEnd != null && dtEnd.getValue() != null) {
                            event.setEndTime(parseCalendarDate(dtEnd.getValue()));
                        }
                    } catch (Exception e) {
                        logger.debug("Warning: unable to get DTEND from event", e);
                    }

                    events.add(event);
                }
            }
        } catch (Exception e) {
            logger.warn("Errore parsing iCalendar", e);
        }

        return events;
    }

    private LocalDateTime parseCalendarDate(Object dateValue) {
        if (dateValue == null) return null;
        try {
            logger.debug("Calendar date value: {}", dateValue);
            return LocalDateTime.now();
        } catch (Exception e) {
            logger.warn("Errore conversione data CalDAV", e);
            return null;
        }
    }

    private boolean isEventInRange(CalendarEvent event, LocalDateTime from, LocalDateTime to) {
        if (event.getStartTime() == null) return false;
        return !event.getStartTime().isBefore(from) && !event.getStartTime().isAfter(to);
    }

    private void addBasicAuth(HttpURLConnection conn) {
        String auth = username + ":" + decryptedPassword;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
        conn.setRequestProperty("Authorization", "Basic " + encodedAuth);
    }

}
