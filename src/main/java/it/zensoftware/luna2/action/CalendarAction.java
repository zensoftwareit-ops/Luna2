package it.zensoftware.luna2.action;

import com.opensymphony.xwork2.ActionSupport;
import it.zensoftware.luna2.dao.CalendarAccountDAO;
import it.zensoftware.luna2.model.CalendarAccount;
import it.zensoftware.luna2.model.User;
import it.zensoftware.luna2.service.calendar.CalendarSyncService;
import it.zensoftware.luna2.service.calendar.PasswordEncryptionService;
import it.zensoftware.luna2.service.calendar.CalDavCalendarProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.struts2.ServletActionContext;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.security.SecureRandom;
import java.security.MessageDigest;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class CalendarAction extends ActionSupport {

    private static final Logger logger = LogManager.getLogger(CalendarAction.class);

    private final CalendarAccountDAO accountDAO = new CalendarAccountDAO();
    private final CalendarSyncService syncService = new CalendarSyncService();

    private List<CalendarAccount> accounts;
    private String googleAuthUrl;
    
    // iCloud form fields
    private String caldavUsername;
    private String caldavPassword;
    private String caldavUrl;
    private boolean testConnectionResult;
    private String testConnectionMessage;

    public String list() {
        User currentUser = getCurrentUser();
        if (currentUser != null) {
            accounts = accountDAO.findByUserId(String.valueOf(currentUser.getId()));
        }
        return SUCCESS;
    }

    public String sync() {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            addActionError("Utente non autenticato");
            return ERROR;
        }

        List<CalendarAccount> userAccounts = accountDAO.findByUserId(String.valueOf(currentUser.getId()));
        for (CalendarAccount account : userAccounts) {
            try {
                syncService.syncAccount(account);
            } catch (Exception e) {
                logger.warn("Errore sync calendario per utente: " + currentUser.getId(), e);
            }
        }

        addActionMessage("Sincronizzazione calendario avviata");
        return SUCCESS;
    }

    public String connectGoogle() {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return LOGIN;
        }

        Properties props = loadProperties();
        String clientId = props.getProperty("calendar.google.clientId", "");
        String redirectUri = props.getProperty("calendar.google.redirectUri", "");
        String stateSecret = props.getProperty("calendar.google.stateSecret", "");
        String tenantKey = props.getProperty("calendar.tenantKey", "");

        if (clientId.isEmpty() || redirectUri.isEmpty()) {
            addActionError("Configura calendar.google.clientId e calendar.google.redirectUri in application.properties");
            return ERROR;
        }

        if (stateSecret.isEmpty()) {
            addActionError("Configura calendar.google.stateSecret per OAuth centralizzato");
            return ERROR;
        }

        if (tenantKey.isEmpty()) {
            tenantKey = ServletActionContext.getRequest().getServerName();
        }

        String returnUrl = buildReturnUrl();
        String state = buildSignedState(stateSecret, tenantKey, returnUrl, currentUser.getId());

        String scope = "https://www.googleapis.com/auth/calendar";
        googleAuthUrl = "https://accounts.google.com/o/oauth2/v2/auth" +
                "?client_id=" + urlEncode(clientId) +
                "&redirect_uri=" + urlEncode(redirectUri) +
                "&response_type=code" +
                "&scope=" + urlEncode(scope) +
                "&access_type=offline" +
                "&prompt=consent" +
                "&state=" + urlEncode(state);

        addActionMessage("URL OAuth generato. Aprilo per autorizzare l'account Google.");
        return SUCCESS;
    }

    public String googleCallback() {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return LOGIN;
        }

        String code = ServletActionContext.getRequest().getParameter("code");
        String state = ServletActionContext.getRequest().getParameter("state");
        if (code == null || code.isEmpty()) {
            addActionError("Codice OAuth mancante");
            return ERROR;
        }

        Properties props = loadProperties();
        String clientId = props.getProperty("calendar.google.clientId", "");
        String clientSecret = props.getProperty("calendar.google.clientSecret", "");
        String redirectUri = props.getProperty("calendar.google.redirectUri", "");
        String stateSecret = props.getProperty("calendar.google.stateSecret", "");
        long stateTtlSeconds = parseLong(props.getProperty("calendar.google.stateTtlSeconds", "900"), 900L);

        Map<String, Object> statePayload = null;
        if (state != null && !state.isEmpty() && !stateSecret.isEmpty()) {
            statePayload = verifySignedState(stateSecret, state, stateTtlSeconds * 1000L);
            if (statePayload == null) {
                addActionError("OAuth state non valido o scaduto");
                return ERROR;
            }

            Object stateUserId = statePayload.get("uid");
            if (stateUserId != null && !String.valueOf(currentUser.getId()).equals(String.valueOf(stateUserId))) {
                logger.warn("OAuth state user mismatch: expected {}, got {}", currentUser.getId(), stateUserId);
            }
        }

        if (clientId.isEmpty() || clientSecret.isEmpty() || redirectUri.isEmpty()) {
            addActionError("Configura calendar.google.clientId, calendar.google.clientSecret e calendar.google.redirectUri");
            return ERROR;
        }

        Map<String, Object> tokenResponse = exchangeCodeForTokens(code, clientId, clientSecret, redirectUri);
        if (tokenResponse == null || !tokenResponse.containsKey("access_token")) {
            addActionError("Token exchange fallito. Verifica clientSecret e redirectUri");
            return ERROR;
        }

        String userId = String.valueOf(currentUser.getId());
        CalendarAccount account = accountDAO.findByUserAndProvider(userId, CalendarAccount.Provider.GOOGLE);
        if (account == null) {
            account = new CalendarAccount();
            account.setUserId(userId);
            account.setProvider(CalendarAccount.Provider.GOOGLE);
        }

        account.setAccessToken(String.valueOf(tokenResponse.get("access_token")));
        if (tokenResponse.get("refresh_token") != null) {
            account.setRefreshToken(String.valueOf(tokenResponse.get("refresh_token")));
        }
        account.setCalendarId("primary");
        account.setSyncEnabled(true);
        account.setUpdatedAt(LocalDateTime.now());

        if (account.getId() == null) {
            accountDAO.save(account);
        } else {
            accountDAO.update(account);
        }

        addActionMessage("Account Google collegato. Sync pronto (placeholder)." );

        if (statePayload != null && statePayload.get("returnUrl") != null) {
            try {
                String returnUrl = String.valueOf(statePayload.get("returnUrl"));
                ServletActionContext.getResponse().sendRedirect(returnUrl);
                return NONE;
            } catch (Exception e) {
                logger.warn("Redirect post OAuth fallito", e);
            }
        }
        return SUCCESS;
    }

    public String icloudForm() {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return LOGIN;
        }
        // Mostra form di input iCloud
        return SUCCESS;
    }

    public String testIcloudConnection() {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return LOGIN;
        }

        if (caldavUrl == null || caldavUrl.isEmpty()) {
            testConnectionResult = false;
            testConnectionMessage = "URL CalDAV mancante";
            return ERROR;
        }

        if (caldavUsername == null || caldavUsername.isEmpty() || caldavPassword == null || caldavPassword.isEmpty()) {
            testConnectionResult = false;
            testConnectionMessage = "Credenziali iCloud mancanti";
            return ERROR;
        }

        try {
            // Validazione semplice formato URL CalDAV (iCloud, Nextcloud, etc.)
            if (!caldavUrl.startsWith("http://") && !caldavUrl.startsWith("https://")) {
                testConnectionResult = false;
                testConnectionMessage = "URL CalDAV non valido. Deve iniziare con http:// o https://";
                addActionError(testConnectionMessage);
                return ERROR;
            }

            // Placeholder: full connection test richiederebbe HTTP Basic Auth test
            // Per ora, consideriamo il test OK se formato esatto
            testConnectionResult = true;
            testConnectionMessage = "Formato credenziali CalDAV validato. (Test connessione remota non eseguito in dev mode)";
            addActionMessage("Credenziali iCloud pronte. Procedi con il salvataggio.");
            
            logger.info("CalDAV test validation OK for user: {}", currentUser.getId());
        } catch (Exception e) {
            logger.warn("Errore validazione CalDAV", e);
            testConnectionResult = false;
            testConnectionMessage = "Errore: " + e.getMessage();
            addActionError(testConnectionMessage);
            return ERROR;
        }

        return SUCCESS;
    }

    public String connectIcloud() {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return LOGIN;
        }

        if (caldavUrl == null || caldavUrl.isEmpty() || caldavUsername == null || 
            caldavUsername.isEmpty() || caldavPassword == null || caldavPassword.isEmpty()) {
            addActionError("Tutti i campi iCloud sono obbligatori");
            return ERROR;
        }

        try {
            String userId = String.valueOf(currentUser.getId());
            CalendarAccount account = accountDAO.findByUserAndProvider(userId, CalendarAccount.Provider.ICLOUD);
            
            if (account == null) {
                account = new CalendarAccount();
                account.setUserId(userId);
                account.setProvider(CalendarAccount.Provider.ICLOUD);
            }

            PasswordEncryptionService encryptionService = getEncryptionService();
            account.setCaldavUrl(caldavUrl);
            account.setCaldavUsername(caldavUsername);
            account.setCaldavPasswordEncrypted(encryptionService.encrypt(caldavPassword));
            account.setSyncEnabled(true);
            account.setUpdatedAt(LocalDateTime.now());

            if (account.getId() == null) {
                accountDAO.save(account);
            } else {
                accountDAO.update(account);
            }

            addActionMessage("Account iCloud collegato. Sincronizzazione abilitata.");
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Errore connessione iCloud", e);
            addActionError("Errore salvataggio account iCloud: " + e.getMessage());
            return ERROR;
        }
    }

    private User getCurrentUser() {
        Map<String, Object> session = com.opensymphony.xwork2.ActionContext.getContext().getSession();
        return (User) session.get("currentUser");
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

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String buildReturnUrl() {
        javax.servlet.http.HttpServletRequest request = ServletActionContext.getRequest();
        String scheme = request.getScheme();
        String host = request.getServerName();
        int port = request.getServerPort();
        boolean defaultPort = ("http".equalsIgnoreCase(scheme) && port == 80)
                || ("https".equalsIgnoreCase(scheme) && port == 443);
        String baseUrl = scheme + "://" + host + (defaultPort ? "" : ":" + port) + request.getContextPath();
        return baseUrl + "/app/calendar/calendar";
    }

    private String buildSignedState(String secret, String tenantKey, String returnUrl, Long userId) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("tenant", tenantKey);
            payload.put("returnUrl", returnUrl);
            payload.put("uid", userId);
            payload.put("ts", System.currentTimeMillis());
            payload.put("nonce", generateNonce());

            ObjectMapper mapper = new ObjectMapper();
            String payloadJson = mapper.writeValueAsString(payload);
            String payloadB64 = base64Url(payloadJson.getBytes(StandardCharsets.UTF_8));
            String signature = hmacSha256(secret, payloadB64);
            return payloadB64 + "." + signature;
        } catch (Exception e) {
            logger.warn("Errore costruzione state OAuth", e);
            return "";
        }
    }

    private Map<String, Object> verifySignedState(String secret, String state, long ttlMillis) {
        try {
            String[] parts = state.split("\\.");
            if (parts.length != 2) {
                return null;
            }

            String payloadB64 = parts[0];
            String signature = parts[1];
            String expected = hmacSha256(secret, payloadB64);
            if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), signature.getBytes(StandardCharsets.UTF_8))) {
                return null;
            }

            byte[] payloadBytes = Base64.getUrlDecoder().decode(payloadB64);
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> payload = mapper.readValue(payloadBytes, HashMap.class);
            Object ts = payload.get("ts");
            if (ts instanceof Number) {
                long age = System.currentTimeMillis() - ((Number) ts).longValue();
                if (age < 0 || age > ttlMillis) {
                    return null;
                }
            }
            return payload;
        } catch (Exception e) {
            logger.warn("Errore verifica state OAuth", e);
            return null;
        }
    }

    private String hmacSha256(String secret, String payload) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] sig = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        return base64Url(sig);
    }

    private String base64Url(byte[] data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }

    private String generateNonce() {
        byte[] bytes = new byte[16];
        new SecureRandom().nextBytes(bytes);
        return base64Url(bytes);
    }

    private long parseLong(String value, long fallback) {
        try {
            return Long.parseLong(value);
        } catch (Exception e) {
            return fallback;
        }
    }

    public String doTestConnection() {
        return testIcloudConnection();
    }

    public String doConnectIcloud() {
        return connectIcloud();
    }

    private Map<String, Object> exchangeCodeForTokens(String code, String clientId, String clientSecret, String redirectUri) {
        try {
            URL url = new URL("https://oauth2.googleapis.com/token");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            String body = "code=" + urlEncode(code) +
                    "&client_id=" + urlEncode(clientId) +
                    "&client_secret=" + urlEncode(clientSecret) +
                    "&redirect_uri=" + urlEncode(redirectUri) +
                    "&grant_type=authorization_code";

            try (OutputStream os = connection.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }

            InputStream responseStream = connection.getResponseCode() >= 200 && connection.getResponseCode() < 300
                    ? connection.getInputStream()
                    : connection.getErrorStream();

            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(responseStream, HashMap.class);
        } catch (Exception e) {
            logger.warn("Errore token exchange Google", e);
            return null;
        }
    }

    public List<CalendarAccount> getAccounts() { return accounts; }
    public String getGoogleAuthUrl() { return googleAuthUrl; }

    public String getCaldavUsername() { return caldavUsername; }
    public void setCaldavUsername(String caldavUsername) { this.caldavUsername = caldavUsername; }

    public String getCaldavPassword() { return caldavPassword; }
    public void setCaldavPassword(String caldavPassword) { this.caldavPassword = caldavPassword; }

    public String getCaldavUrl() { return caldavUrl; }
    public void setCaldavUrl(String caldavUrl) { this.caldavUrl = caldavUrl; }

    public boolean isTestConnectionResult() { return testConnectionResult; }
    public String getTestConnectionMessage() { return testConnectionMessage; }

    private PasswordEncryptionService getEncryptionService() {
        Properties props = loadProperties();
        String encryptionSecret = props.getProperty("calendar.encryption.secret", "DefaultCalendarSecret");
        return new PasswordEncryptionService(encryptionSecret);
    }
}
