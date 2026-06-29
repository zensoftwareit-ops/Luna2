package it.zensoftware.luna2.service.calendar;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.zensoftware.luna2.dao.CalendarAccountDAO;
import it.zensoftware.luna2.model.CalendarAccount;
import it.zensoftware.luna2.model.CalendarEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class GoogleCalendarProvider implements CalendarProvider {

    private static final Logger logger = LogManager.getLogger(GoogleCalendarProvider.class);
    private static final String GOOGLE_CALENDAR_BASE = "https://www.googleapis.com/calendar/v3/calendars";
    private static final String GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token";

    private final CalendarAccountDAO accountDAO = new CalendarAccountDAO();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public CalendarAccount.Provider getProvider() {
        return CalendarAccount.Provider.GOOGLE;
    }

    @Override
    public List<CalendarEvent> listEvents(CalendarAccount account, LocalDateTime from, LocalDateTime to) {
        if (account == null) {
            return Collections.emptyList();
        }

        String accessToken = ensureAccessToken(account);
        if (accessToken == null) {
            logger.warn("Access token non disponibile per user: {}", account.getUserId());
            return Collections.emptyList();
        }

        String calendarId = account.getCalendarId() != null && !account.getCalendarId().isEmpty()
                ? account.getCalendarId()
                : "primary";

        try {
            String timeMin = urlEncode(toRfc3339(from));
            String timeMax = urlEncode(toRfc3339(to));
            String url = GOOGLE_CALENDAR_BASE + "/" + urlEncode(calendarId)
                    + "/events?singleEvents=true&orderBy=startTime&timeMin=" + timeMin
                    + "&timeMax=" + timeMax;

            HttpURLConnection conn = openConnection(url, "GET", accessToken);
            int code = conn.getResponseCode();
            if (code == 401 && refreshAccessToken(account)) {
                accessToken = account.getAccessToken();
                conn = openConnection(url, "GET", accessToken);
                code = conn.getResponseCode();
            }

            if (code < 200 || code >= 300) {
                logger.warn("Errore Google Calendar listEvents: {}", code);
                return Collections.emptyList();
            }

            JsonNode root = objectMapper.readTree(conn.getInputStream());
            JsonNode items = root.get("items");
            if (items == null || !items.isArray()) {
                return Collections.emptyList();
            }

            List<CalendarEvent> results = new ArrayList<>();
            for (JsonNode item : items) {
                CalendarEvent event = mapGoogleEvent(item, account);
                if (event != null) {
                    results.add(event);
                }
            }

            return results;
        } catch (Exception e) {
            logger.error("Errore listEvents Google Calendar", e);
            return Collections.emptyList();
        }
    }

    @Override
    public String createOrUpdateEvent(CalendarAccount account, CalendarEvent event) {
        if (account == null || event == null) {
            return null;
        }

        String accessToken = ensureAccessToken(account);
        if (accessToken == null) {
            return null;
        }

        String calendarId = account.getCalendarId() != null && !account.getCalendarId().isEmpty()
                ? account.getCalendarId()
                : "primary";

        try {
            boolean isUpdate = event.getExternalEventId() != null && !event.getExternalEventId().isEmpty();
            String url = GOOGLE_CALENDAR_BASE + "/" + urlEncode(calendarId) + "/events";
            if (isUpdate) {
                url += "/" + urlEncode(event.getExternalEventId());
            }

            String payload = buildGoogleEventPayload(event, account.getTimeZone());
            HttpURLConnection conn = openConnection(url, isUpdate ? "PUT" : "POST", accessToken);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

            try (OutputStream os = conn.getOutputStream()) {
                os.write(payload.getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            if (code == 401 && refreshAccessToken(account)) {
                accessToken = account.getAccessToken();
                conn = openConnection(url, isUpdate ? "PUT" : "POST", accessToken);
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(payload.getBytes(StandardCharsets.UTF_8));
                }
                code = conn.getResponseCode();
            }

            if (code < 200 || code >= 300) {
                logger.warn("Errore Google Calendar create/update: {}", code);
                return null;
            }

            JsonNode response = objectMapper.readTree(conn.getInputStream());
            JsonNode idNode = response.get("id");
            return idNode != null ? idNode.asText() : event.getExternalEventId();
        } catch (Exception e) {
            logger.error("Errore create/update Google Calendar", e);
            return null;
        }
    }

    @Override
    public void deleteEvent(CalendarAccount account, String externalEventId) {
        if (account == null || externalEventId == null || externalEventId.isEmpty()) {
            return;
        }

        String accessToken = ensureAccessToken(account);
        if (accessToken == null) {
            return;
        }

        String calendarId = account.getCalendarId() != null && !account.getCalendarId().isEmpty()
                ? account.getCalendarId()
                : "primary";

        try {
            String url = GOOGLE_CALENDAR_BASE + "/" + urlEncode(calendarId) + "/events/" + urlEncode(externalEventId);
            HttpURLConnection conn = openConnection(url, "DELETE", accessToken);
            int code = conn.getResponseCode();
            if (code == 401 && refreshAccessToken(account)) {
                accessToken = account.getAccessToken();
                conn = openConnection(url, "DELETE", accessToken);
                code = conn.getResponseCode();
            }

            if (code < 200 || code >= 300) {
                logger.warn("Errore Google Calendar delete: {}", code);
            }
        } catch (Exception e) {
            logger.error("Errore delete Google Calendar", e);
        }
    }

    private CalendarEvent mapGoogleEvent(JsonNode item, CalendarAccount account) {
        if (item == null) {
            return null;
        }

        CalendarEvent event = new CalendarEvent();
        event.setExternalEventId(getText(item, "id"));
        event.setTitle(getText(item, "summary"));
        event.setDescription(getText(item, "description"));
        event.setLocation(getText(item, "location"));

        LocalDateTime start = parseGoogleDateTime(item.get("start"));
        LocalDateTime end = parseGoogleDateTime(item.get("end"));
        if (start == null || end == null) {
            return null;
        }

        event.setStartTime(start);
        event.setEndTime(end);
        event.setProvider(account.getProvider());
        return event;
    }

    private LocalDateTime parseGoogleDateTime(JsonNode node) {
        if (node == null) {
            return null;
        }

        JsonNode dateTimeNode = node.get("dateTime");
        if (dateTimeNode != null && !dateTimeNode.isNull()) {
            return ZonedDateTime.parse(dateTimeNode.asText()).toLocalDateTime();
        }

        JsonNode dateNode = node.get("date");
        if (dateNode != null && !dateNode.isNull()) {
            LocalDate date = LocalDate.parse(dateNode.asText());
            return date.atStartOfDay();
        }

        return null;
    }

    private String buildGoogleEventPayload(CalendarEvent event, String timeZone) throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("summary", event.getTitle());
        payload.put("description", event.getDescription());
        payload.put("location", event.getLocation());

        Map<String, Object> start = new HashMap<>();
        start.put("dateTime", toRfc3339(event.getStartTime()));
        start.put("timeZone", timeZone != null ? timeZone : "Europe/Rome");

        Map<String, Object> end = new HashMap<>();
        end.put("dateTime", toRfc3339(event.getEndTime()));
        end.put("timeZone", timeZone != null ? timeZone : "Europe/Rome");

        payload.put("start", start);
        payload.put("end", end);

        return objectMapper.writeValueAsString(payload);
    }

    private HttpURLConnection openConnection(String url, String method, String accessToken) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod(method);
        conn.setRequestProperty("Authorization", "Bearer " + accessToken);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);
        return conn;
    }

    private String ensureAccessToken(CalendarAccount account) {
        if (account.getAccessToken() != null && !account.getAccessToken().isEmpty()) {
            return account.getAccessToken();
        }

        if (refreshAccessToken(account)) {
            return account.getAccessToken();
        }

        return null;
    }

    private boolean refreshAccessToken(CalendarAccount account) {
        if (account.getRefreshToken() == null || account.getRefreshToken().isEmpty()) {
            return false;
        }

        Properties props = loadProperties();
        String clientId = props.getProperty("calendar.google.clientId");
        String clientSecret = props.getProperty("calendar.google.clientSecret");
        if (clientId == null || clientSecret == null) {
            return false;
        }

        try {
            String body = "client_id=" + urlEncode(clientId)
                    + "&client_secret=" + urlEncode(clientSecret)
                    + "&refresh_token=" + urlEncode(account.getRefreshToken())
                    + "&grant_type=refresh_token";

            HttpURLConnection conn = (HttpURLConnection) new URL(GOOGLE_TOKEN_URL).openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }

            if (conn.getResponseCode() < 200 || conn.getResponseCode() >= 300) {
                logger.warn("Refresh token fallito: {}", conn.getResponseCode());
                return false;
            }

            JsonNode response = objectMapper.readTree(conn.getInputStream());
            JsonNode accessToken = response.get("access_token");
            if (accessToken == null) {
                return false;
            }

            account.setAccessToken(accessToken.asText());
            accountDAO.update(account);
            return true;
        } catch (Exception e) {
            logger.error("Errore refresh token Google", e);
            return false;
        }
    }

    private Properties loadProperties() {
        Properties props = new Properties();
        try (InputStream is = this.getClass().getClassLoader().getResourceAsStream("application.properties")) {
            if (is != null) {
                props.load(is);
            }
        } catch (Exception e) {
            logger.warn("Impossibile leggere application.properties", e);
        }
        return props;
    }

    private String toRfc3339(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    private String getText(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value != null && !value.isNull() ? value.asText() : null;
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
