package it.zensoftware.luna2.util;

import java.util.*;
import java.util.regex.Pattern;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * ValidationUtil provides input validation methods for security hardening.
 * Implements whitelist-based validation for properties, emails, phones, and text fields.
 * 
 * OWASP Validation Principles Applied:
 * 1. Whitelist: Only allow known-good values (not blacklist)
 * 2. Default Deny: Reject if not explicitly allowed
 * 3. Fail Securely: Reject invalid input, don't try to fix it
 * 
 * @author AI Security Hardening
 * @version 1.0
 */
public class ValidationUtil {
    
    private static final Logger logger = LogManager.getLogger(ValidationUtil.class);
    
    // Property name whitelist: Maps entity class names to their allowed searchable properties
    private static final Map<String, Set<String>> PROPERTY_WHITELIST = new HashMap<>();
    
    // Email regex: RFC 5322 simplified pattern
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$"
    );
    
    // Phone regex: International format +39 XXX XXXXXXX (Italy) or +1 (XXX) XXX-XXXX (US)
    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "^\\+?[1-9]\\d{1,14}$|^(\\+39|0)[0-9]{6,13}$"
    );
    
    // General alphanumeric + spaces + basic punctuation
    private static final Pattern SAFE_TEXT_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9àáäâèéëêìíïîòóöôùúüûçñ\\s\\-.,()&']*$"
    );
    
    static {
        // Initialize property whitelists for common entities
        // This prevents HQL injection through property name parameter
        
        // Cliente properties
        Set<String> clienteProps = new HashSet<>(Arrays.asList(
            "id", "ragioneSociale", "partitaIva", "codiceFiscale", 
            "email", "telefono", "indirizzo", "cap", "citta", "provincia",
            "nazione", "stato", "dataCreazione", "dataModifica"
        ));
        PROPERTY_WHITELIST.put("it.zensoftware.luna2.model.Cliente", clienteProps);
        
        // Fornitore properties
        Set<String> fornitoreProps = new HashSet<>(Arrays.asList(
            "id", "ragioneSociale", "partitaIva", "codiceFiscale",
            "email", "telefono", "indirizzo", "cap", "citta", "provincia",
            "nazione", "referente", "dataCreazione", "dataModifica"
        ));
        PROPERTY_WHITELIST.put("it.zensoftware.luna2.model.Fornitore", fornitoreProps);
        
        // Prodotto properties
        Set<String> prodottoProps = new HashSet<>(Arrays.asList(
            "id", "codice", "descrizione", "categoria", "prezzo",
            "quantita", "um", "fornitore", "stato", "dataCreazione"
        ));
        PROPERTY_WHITELIST.put("it.zensoftware.luna2.model.Prodotto", prodottoProps);
        
        // Lead properties
        Set<String> leadProps = new HashSet<>(Arrays.asList(
            "id", "nome", "email", "telefono", "azienda", "titolo",
            "fonte", "status", "probabilita", "importo", "dataCreazione",
            "dataModifica", "owner"
        ));
        PROPERTY_WHITELIST.put("it.zensoftware.luna2.model.Lead", leadProps);
        
        // Activity properties
        Set<String> activityProps = new HashSet<>(Arrays.asList(
            "id", "tipo", "descrizione", "lead", "data", "owner", "stato"
        ));
        PROPERTY_WHITELIST.put("it.zensoftware.luna2.model.Activity", activityProps);
        
        // Fattura properties
        Set<String> fatturaProps = new HashSet<>(Arrays.asList(
            "id", "numero", "anno", "dataFattura", "dataScadenza",
            "cliente", "stato", "totale", "iva", "importoNetto"
        ));
        PROPERTY_WHITELIST.put("it.zensoftware.luna2.model.Fattura", fatturaProps);
    }
    
    /**
     * Private constructor to prevent instantiation
     */
    private ValidationUtil() {
    }
    
    /**
     * Validates property name against whitelist to prevent HQL injection.
     * 
     * SECURITY CRITICAL: Always use this before passing propertyName to HQL queries.
     * 
     * @param entityClass The entity class being queried
     * @param propertyName The property name to validate
     * @return true if property is whitelisted, false otherwise
     * @throws SecurityException if validation fails
     */
    public static boolean validatePropertyName(Class<?> entityClass, String propertyName) 
            throws SecurityException {
        
        if (propertyName == null || propertyName.trim().isEmpty()) {
            logger.warn("Empty property name attempted for " + entityClass.getName());
            throw new SecurityException("Property name cannot be empty");
        }
        
        // Check for suspicious patterns that indicate injection attempt
        if (propertyName.contains(";") || propertyName.contains("--") || 
            propertyName.contains("/*") || propertyName.contains("*/") ||
            propertyName.contains("'") || propertyName.contains("\"")) {
            logger.warn("Suspicious property name pattern: " + propertyName);
            throw new SecurityException("Invalid characters in property name");
        }
        
        Set<String> allowedProps = PROPERTY_WHITELIST.get(entityClass.getName());
        
        if (allowedProps == null) {
            logger.warn("No whitelist defined for entity: " + entityClass.getName());
            throw new SecurityException("Entity class not configured for property validation");
        }
        
        if (!allowedProps.contains(propertyName)) {
            logger.warn("Property not whitelisted for " + entityClass.getName() + 
                       ": " + propertyName);
            throw new SecurityException("Property '" + propertyName + "' is not allowed for " + 
                                       entityClass.getSimpleName());
        }
        
        return true;
    }
    
    /**
     * Validates email address format.
     * 
     * @param email The email to validate
     * @return true if email format is valid, false otherwise
     */
    public static boolean validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        
        if (email.length() < 5 || email.length() > 254) {
            logger.warn("Email length out of bounds: " + email.length());
            return false;
        }
        
        return EMAIL_PATTERN.matcher(email.toLowerCase()).matches();
    }
    
    /**
     * Validates phone number format (international format).
     * Supports:
     * - Italian: +39 or 0 prefix with 6-13 digits
     * - International: +1 to +9 with 1-14 digits
     * 
     * @param phone The phone number to validate
     * @return true if phone format is valid, false otherwise
     */
    public static boolean validatePhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return false;
        }
        
        // Remove common separators for validation
        String cleaned = phone.replaceAll("[\\s\\-().]", "");
        
        if (cleaned.length() < 7 || cleaned.length() > 16) {
            logger.warn("Phone number length out of bounds: " + cleaned.length());
            return false;
        }
        
        return PHONE_PATTERN.matcher(cleaned).matches();
    }
    
    /**
     * Validates text field length and content.
     * Prevents excessively long strings and dangerous characters.
     * 
     * @param text The text to validate
     * @param maxLength Maximum allowed length
     * @return true if text is valid, false otherwise
     */
    public static boolean validateTextLength(String text, int maxLength) {
        if (text == null) {
            return true; // null is allowed (nullable field)
        }
        
        if (text.length() > maxLength) {
            logger.warn("Text exceeds max length: " + text.length() + " > " + maxLength);
            return false;
        }
        
        // Check for excessive whitespace (potential DoS)
        if (text.length() > 0 && text.length() < maxLength) {
            long whitespaceCount = text.chars().filter(Character::isWhitespace).count();
            if (whitespaceCount > text.length() * 0.8) {
                logger.warn("Text contains excessive whitespace");
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Validates text field for safe characters (alphanumeric + spaces + basic punctuation).
     * Prevents XSS through database fields.
     * 
     * @param text The text to validate
     * @return true if text contains only safe characters, false otherwise
     */
    public static boolean validateSafeText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return true;
        }
        
        return SAFE_TEXT_PATTERN.matcher(text).matches();
    }
    
    /**
     * Comprehensive validation for alphanumeric identifier fields (codes, SKUs, etc.).
     * 
     * @param identifier The identifier to validate
     * @param maxLength Maximum allowed length
     * @return true if identifier is valid, false otherwise
     */
    public static boolean validateAlphanumericIdentifier(String identifier, int maxLength) {
        if (identifier == null || identifier.trim().isEmpty()) {
            return false;
        }
        
        if (identifier.length() > maxLength) {
            return false;
        }
        
        // Allow alphanumeric, hyphens, underscores only
        return identifier.matches("^[a-zA-Z0-9\\-_]+$");
    }
    
    /**
     * Validates numeric field ranges to prevent injection or overflow.
     * 
     * @param value The value to validate
     * @param min Minimum allowed value
     * @param max Maximum allowed value
     * @return true if value is within range, false otherwise
     */
    public static boolean validateNumericRange(Number value, Number min, Number max) {
        if (value == null) {
            return false;
        }
        
        double doubleValue = value.doubleValue();
        double minValue = min.doubleValue();
        double maxValue = max.doubleValue();
        
        return doubleValue >= minValue && doubleValue <= maxValue;
    }
    
    /**
     * Sanitizes user input by removing potentially dangerous characters.
     * Note: This is a secondary defense; whitelist validation is primary.
     * 
     * @param input The input to sanitize
     * @return Sanitized string
     */
    public static String sanitizeInput(String input) {
        if (input == null) {
            return null;
        }
        
        // Remove SQL injection characters
        return input.replaceAll("[;'\"\\\\]", "");
    }
    
    /**
     * Logs security validation event for audit trail.
     * 
     * @param eventType Type of validation (e.g., "PROPERTY_VALIDATION", "EMAIL_VALIDATION")
     * @param entityType The entity being validated
     * @param result Pass/Fail result
     * @param details Additional details
     */
    public static void logValidationEvent(String eventType, String entityType, 
                                         boolean result, String details) {
        String status = result ? "PASS" : "FAIL";
        logger.info("SECURITY_VALIDATION [" + status + "] " + eventType + 
                   " | Entity: " + entityType + " | Details: " + details);
    }
}
