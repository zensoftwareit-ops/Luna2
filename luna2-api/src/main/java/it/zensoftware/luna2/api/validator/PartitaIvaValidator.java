package it.zensoftware.luna2.api.validator;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

/**
 * Implementazione validazione Partita IVA italiana
 * - 11 cifre numeriche
 * - Check digit con algoritmo modulo 97
 */
public class PartitaIvaValidator implements ConstraintValidator<ValidPartitaIva, String> {

    private static final Pattern PATTERN = Pattern.compile("^[0-9]{11}$");

    @Override
    public void initialize(ValidPartitaIva annotation) {
        // No initialization needed
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isEmpty()) {
            return false;
        }

        // Check format: 11 digits
        if (!PATTERN.matcher(value).matches()) {
            return false;
        }

        // Check digit validation (Luhn-like algorithm for Italian P.IVA)
        return validatePartitaIvaCheckDigit(value);
    }

    /**
     * Validazione check digit per Partita IVA italiana
     * Algoritmo: modulo 97
     */
    private boolean validatePartitaIvaCheckDigit(String piva) {
        int[] moltiplicatori = {1, 2, 1, 2, 1, 2, 1, 2, 1, 2};
        int somma = 0;

        for (int i = 0; i < 10; i++) {
            int cifra = Character.getNumericValue(piva.charAt(i));
            int prodotto = cifra * moltiplicatori[i];

            if (prodotto > 9) {
                prodotto = (prodotto / 10) + (prodotto % 10);
            }

            somma += prodotto;
        }

        int checkDigit = (10 - (somma % 10)) % 10;
        return checkDigit == Character.getNumericValue(piva.charAt(10));
    }
}
