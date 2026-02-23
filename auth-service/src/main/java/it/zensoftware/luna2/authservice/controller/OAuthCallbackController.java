package it.zensoftware.luna2.authservice.controller;

import it.zensoftware.luna2.authservice.oauth.OAuthStateValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Controller
public class OAuthCallbackController {

    private static final Logger logger = LoggerFactory.getLogger(OAuthCallbackController.class);

    @Value("${oauth.google.state.secret}")
    private String stateSecret;

    @Value("${oauth.google.state.ttl.seconds:900}")
    private long stateTtlSeconds;

    @GetMapping("/oauth/google/callback")
    public RedirectView googleCallback(
            @RequestParam(name = "code", required = false) String code,
            @RequestParam(name = "state", required = false) String state,
            @RequestParam(name = "error", required = false) String error) {

        if (error != null) {
            logger.warn("OAuth error from Google: {}", error);
            return redirectToError("OAuth_error_from_provider", error);
        }

        if (code == null || code.isEmpty()) {
            logger.warn("Missing authorization code");
            return redirectToError("missing_code", "No authorization code received");
        }

        if (state == null || state.isEmpty()) {
            logger.warn("Missing state parameter");
            return redirectToError("missing_state", "No state parameter received");
        }

        Map<String, Object> statePayload = OAuthStateValidator.verify(state, stateSecret, stateTtlSeconds * 1000L);
        if (statePayload == null) {
            logger.warn("State validation failed");
            return redirectToError("invalid_state", "State validation failed");
        }

        String tenant = String.valueOf(statePayload.get("tenant"));
        String returnPath = String.valueOf(statePayload.get("returnUrl"));

        if (tenant == null || tenant.isEmpty() || returnPath == null || returnPath.isEmpty()) {
            logger.warn("State payload missing tenant or returnUrl");
            return redirectToError("invalid_state_payload", "Missing required state fields");
        }

        String targetUrl = buildTargetUrl(tenant, returnPath, code, state);
        logger.info("Redirecting to tenant: {} with path: {}", tenant, returnPath);
        return new RedirectView(targetUrl, false, true, false);
    }

    private String buildTargetUrl(String tenant, String returnPath, String code, String state) {
        try {
            String baseUrl = "https://" + tenant;
            String encodedCode = URLEncoder.encode(code, StandardCharsets.UTF_8);
            String encodedState = URLEncoder.encode(state, StandardCharsets.UTF_8);
            return baseUrl + returnPath.replace("/app/calendar/calendar", "/app/calendar/calendar-google-callback")
                    + "?code=" + encodedCode + "&state=" + encodedState;
        } catch (Exception e) {
            logger.error("Error building target URL", e);
            return null;
        }
    }

    private RedirectView redirectToError(String errorCode, String errorMsg) {
        try {
            String errorUrl = "https://auth.gestionaleluna.it/error" +
                    "?code=" + URLEncoder.encode(errorCode, StandardCharsets.UTF_8) +
                    "&message=" + URLEncoder.encode(errorMsg, StandardCharsets.UTF_8);
            return new RedirectView(errorUrl, false, true, false);
        } catch (Exception e) {
            logger.error("Error building error URL", e);
            return new RedirectView("https://auth.gestionaleluna.it/error", false, true, false);
        }
    }

}
