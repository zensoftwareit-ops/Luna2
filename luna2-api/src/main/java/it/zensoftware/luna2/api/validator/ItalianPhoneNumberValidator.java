package it.zensoftware.luna2.api.validator;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

/**
 * Implementazione validazione Numero Telefono italiano
 * Supporta:
 * - 0XX XXXXXXX (fisso, 9 cifre dopo 0)
 * - 3XX XXXXXXX (mobile, 9 cifre dopo 3)
 * - +39 3XX XXXXXXX (internazionale)
 */
public class ItalianPhoneNumberValidator implements ConstraintValidator<ValidItalianPhoneNumber, String> {

    // Regex per fisso italiano (0XX XXXXXXX)
    private static final Pattern LANDLINE_PATTERN = Pattern.compile("^0[0-9]{9,10}$");

    // Regex per mobile italiano (3XX XXXXXXX)
    private static final Pattern MOBILE_PATTERN = Pattern.compile("^3[0-9]{9}$");

    // Regex per internazionale (+39 3XX XXXXXXX)
    private static final Pattern INTERNATIONAL_PATTERN = Pattern.compile("^\\+39[0-9]{9,10}$");

    @Override
    public void initialize(ValidItalianPhoneNumber annotation) {
        // No initialization needed
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isEmpty()) {
            return false;
        }

        // Remove spaces and dashes
        String cleaned = value.replaceAll("[\\s\\-]", "");

        return LANDLINE_PATTERN.matcher(cleaned).matches() ||
               MOBILE_PATTERN.matcher(cleaned).matches() ||
               INTERNATIONAL_PATTERN.matcher(cleaned).matches();
    }
}
