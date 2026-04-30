package it.zensoftware.luna2.api.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Input Sanitizer - Previene XSS, SQL Injection, e altri attacchi
 *
 * Strategie:
 * - Rimuove HTML tags
 * - Rimuove SQL injection patterns
 * - Limita lunghezza input
 * - Escapa caratteri pericolosi
 *
 * Uso:
 * String clean = InputSanitizer.sanitize(userInput);
 */
@Component
public class InputSanitizer {

    private static final Logger logger = LoggerFactory.getLogger(InputSanitizer.class);

    // Pattern pericolosi per SQL injection
    private static final String SQL_INJECTION_PATTERN =
        "(?i)(DROP|DELETE|INSERT|UPDATE|EXEC|EXECUTE|UNION|SELECT|ALTER|CREATE|TRUNCATE|" +
        "PRAGMA|SET|DECLARE|CAST|CHAR|CONVERT|ASCII|SUBSTR)" +
        "|(-{2})|(/\\*|\\*/)|(;)|(\\||&&)|(')|(\\\\)";

    // Pattern per XSS
    private static final String XSS_PATTERN = "<[^>]*>";

    // Pattern per script tags
    private static final String SCRIPT_PATTERN = "(?i)<script[^>]*>.*?</script>";

    /**
     * Sanitizza string input rimuovendo possibili attacchi
     * IMPORTANTE: Non è una protezione definitiva - usare sempre parametrized queries in DB
     *
     * @param input String da pulire
     * @return String sanitizzato
     */
    public static String sanitize(String input) {
        if (input == null) {
            return null;
        }

        if (input.isEmpty()) {
            return "";
        }

        String sanitized = input;

        // 1. Rimuovi script tags
        sanitized = sanitized.replaceAll(SCRIPT_PATTERN, "");

        // 2. Rimuovi HTML tags
        sanitized = sanitized.replaceAll(XSS_PATTERN, "");

        // 3. Rimuovi SQL injection patterns
        sanitized = sanitized.replaceAll(SQL_INJECTION_PATTERN, "");

        // 4. Rimuovi null bytes
        sanitized = sanitized.replaceAll("\0", "");

        // 5. Trim whitespace
        sanitized = sanitized.trim();

        // 6. Limita lunghezza (max 1000 caratteri per campo generico)
        if (sanitized.length() > 1000) {
            sanitized = sanitized.substring(0, 1000);
            logger.warn("Input truncated to 1000 chars due to length limit");
        }

        // Log suspicious patterns
        if (!sanitized.equals(input)) {
            logger.warn("Suspicious input detected and sanitized. Original length: {}, Sanitized length: {}",
                input.length(), sanitized.length());
        }

        return sanitized;
    }

    /**
     * Sanitizza per i nomi di file (più restrittivo)
     */
    public static String sanitizeFilename(String filename) {
        if (filename == null) return null;

        // Rimuovi percorsi e path traversal
        String safe = filename.replaceAll("[./\\\\]", "");

        // Rimuovi caratteri speciali
        safe = safe.replaceAll("[^a-zA-Z0-9._-]", "");

        // Max 255 chars
        if (safe.length() > 255) {
            safe = safe.substring(0, 255);
        }

        return safe.isEmpty() ? "file" : safe;
    }

    /**
     * Sanitizza per numeri (assicura formato numerico)
     */
    public static String sanitizeNumeric(String input) {
        if (input == null) return null;
        return input.replaceAll("[^0-9.,]", "");
    }

    /**
     * Sanitizza per email (basic check)
     */
    public static String sanitizeEmail(String email) {
        if (email == null) return null;

        String sanitized = email.toLowerCase().trim();

        // Rimuovi spazi
        sanitized = sanitized.replaceAll("\\s", "");

        // Max 254 chars (RFC 5321)
        if (sanitized.length() > 254) {
            sanitized = sanitized.substring(0, 254);
        }

        return sanitized;
    }

    /**
     * Escapa HTML entities per safe output
     * Usa quando devi mostrare user input in HTML
     */
    public static String escapeHtml(String input) {
        if (input == null) return null;

        return input
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");
    }

    /**
     * Check se input contiene pattern pericolosi (senza rimuoverli)
     * Utile per logging/alerting
     */
    public static boolean containsSuspiciousPatterns(String input) {
        if (input == null) return false;

        return input.matches("(?i).*(" + SQL_INJECTION_PATTERN + "|" + XSS_PATTERN + ").*");
    }
}
