package it.zensoftware.luna2.authservice.oauth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class OAuthStateValidator {

    private static final Logger logger = LoggerFactory.getLogger(OAuthStateValidator.class);

    public static Map<String, Object> verify(String state, String secret, long ttlMillis) {
        try {
            String[] parts = state.split("\\.");
            if (parts.length != 2) {
                logger.warn("State malformed: expected 2 parts, got {}", parts.length);
                return null;
            }

            String payloadB64 = parts[0];
            String signature = parts[1];
            String expected = hmacSha256(secret, payloadB64);
            if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), signature.getBytes(StandardCharsets.UTF_8))) {
                logger.warn("State signature verification failed");
                return null;
            }

            byte[] payloadBytes = Base64.getUrlDecoder().decode(payloadB64);
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> payload = mapper.readValue(payloadBytes, HashMap.class);
            
            Object ts = payload.get("ts");
            if (ts instanceof Number) {
                long age = System.currentTimeMillis() - ((Number) ts).longValue();
                if (age < 0 || age > ttlMillis) {
                    logger.warn("State expired: age {} ms, ttl {} ms", age, ttlMillis);
                    return null;
                }
            }
            
            return payload;
        } catch (Exception e) {
            logger.warn("State verification error", e);
            return null;
        }
    }

    private static String hmacSha256(String secret, String payload) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] sig = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        return base64Url(sig);
    }

    private static String base64Url(byte[] data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }

}
