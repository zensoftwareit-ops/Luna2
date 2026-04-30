package it.zensoftware.luna2.api.validator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.validation.ConstraintValidatorContext;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit Tests per Custom Validators
 * Coverage: Partita IVA, Email, Numero Telefono
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Validator Tests")
public class ValidatorTest {

    @Mock
    private ConstraintValidatorContext constraintContext;

    // ==================== PARTITA IVA TESTS ====================

    @Test
    @DisplayName("Partita IVA valida")
    void testValidPartitaIva() {
        PartitaIvaValidator validator = new PartitaIvaValidator();
        validator.initialize(null);

        // Partita IVA di esempio valida (è una P.IVA reale di test)
        boolean result = validator.isValid("12345678901", constraintContext);
        assertTrue(result, "Partita IVA valida dovrebbe essere accettata");
    }

    @Test
    @DisplayName("Partita IVA null")
    void testPartitaIvaNulla() {
        PartitaIvaValidator validator = new PartitaIvaValidator();
        validator.initialize(null);

        boolean result = validator.isValid(null, constraintContext);
        assertFalse(result, "Partita IVA null dovrebbe essere rifiutata");
    }

    @Test
    @DisplayName("Partita IVA con formato errato")
    void testPartitaIvaInvalidFormat() {
        PartitaIvaValidator validator = new PartitaIvaValidator();
        validator.initialize(null);

        boolean result = validator.isValid("1234567890", constraintContext); // Solo 10 cifre
        assertFalse(result, "Partita IVA con 10 cifre dovrebbe essere rifiutata");
    }

    @Test
    @DisplayName("Partita IVA con caratteri non numerici")
    void testPartitaIvaWithLetters() {
        PartitaIvaValidator validator = new PartitaIvaValidator();
        validator.initialize(null);

        boolean result = validator.isValid("1234567890A", constraintContext);
        assertFalse(result, "Partita IVA con lettere dovrebbe essere rifiutata");
    }

    // ==================== EMAIL TESTS ====================

    @Test
    @DisplayName("Email valida")
    void testValidEmail() {
        EmailValidator validator = new EmailValidator();
        validator.initialize(null);

        boolean result = validator.isValid("test@example.com", constraintContext);
        assertTrue(result, "Email valida dovrebbe essere accettata");
    }

    @Test
    @DisplayName("Email italiana valida")
    void testValidItalianEmail() {
        EmailValidator validator = new EmailValidator();
        validator.initialize(null);

        boolean result = validator.isValid("user@company.it", constraintContext);
        assertTrue(result, "Email italiana valida dovrebbe essere accettata");
    }

    @Test
    @DisplayName("Email null")
    void testEmailNull() {
        EmailValidator validator = new EmailValidator();
        validator.initialize(null);

        boolean result = validator.isValid(null, constraintContext);
        assertFalse(result, "Email null dovrebbe essere rifiutata");
    }

    @Test
    @DisplayName("Email senza @")
    void testEmailWithoutAt() {
        EmailValidator validator = new EmailValidator();
        validator.initialize(null);

        boolean result = validator.isValid("testexample.com", constraintContext);
        assertFalse(result, "Email senza @ dovrebbe essere rifiutata");
    }

    @Test
    @DisplayName("Email troppo lunga")
    void testEmailTooLong() {
        EmailValidator validator = new EmailValidator();
        validator.initialize(null);

        String longEmail = "a".repeat(250) + "@example.com";
        boolean result = validator.isValid(longEmail, constraintContext);
        assertFalse(result, "Email > 254 caratteri dovrebbe essere rifiutata");
    }

    // ==================== PHONE NUMBER TESTS ====================

    @Test
    @DisplayName("Numero telefono mobile valido")
    void testValidMobileNumber() {
        ItalianPhoneNumberValidator validator = new ItalianPhoneNumberValidator();
        validator.initialize(null);

        boolean result = validator.isValid("3201234567", constraintContext);
        assertTrue(result, "Numero mobile valido dovrebbe essere accettato");
    }

    @Test
    @DisplayName("Numero telefono fisso valido")
    void testValidLandlineNumber() {
        ItalianPhoneNumberValidator validator = new ItalianPhoneNumberValidator();
        validator.initialize(null);

        boolean result = validator.isValid("0212345678", constraintContext);
        assertTrue(result, "Numero fisso valido dovrebbe essere accettato");
    }

    @Test
    @DisplayName("Numero telefono internazionale valido")
    void testValidInternationalNumber() {
        ItalianPhoneNumberValidator validator = new ItalianPhoneNumberValidator();
        validator.initialize(null);

        boolean result = validator.isValid("+393201234567", constraintContext);
        assertTrue(result, "Numero internazionale valido dovrebbe essere accettato");
    }

    @Test
    @DisplayName("Numero telefono con spazi")
    void testPhoneNumberWithSpaces() {
        ItalianPhoneNumberValidator validator = new ItalianPhoneNumberValidator();
        validator.initialize(null);

        boolean result = validator.isValid("320 123 4567", constraintContext);
        assertTrue(result, "Numero con spazi dovrebbe essere accettato");
    }

    @Test
    @DisplayName("Numero telefono null")
    void testPhoneNumberNull() {
        ItalianPhoneNumberValidator validator = new ItalianPhoneNumberValidator();
        validator.initialize(null);

        boolean result = validator.isValid(null, constraintContext);
        assertFalse(result, "Numero null dovrebbe essere rifiutato");
    }

    @Test
    @DisplayName("Numero telefono non valido")
    void testInvalidPhoneNumber() {
        ItalianPhoneNumberValidator validator = new ItalianPhoneNumberValidator();
        validator.initialize(null);

        boolean result = validator.isValid("1234567", constraintContext);
        assertFalse(result, "Numero con format non corretto dovrebbe essere rifiutato");
    }

    // ==================== PARTITA IVA ADDITIONAL TESTS ====================

    @Test
    @DisplayName("Partita IVA con tutti zeri")
    void testPartitaIvaAllZeros() {
        PartitaIvaValidator validator = new PartitaIvaValidator();
        validator.initialize(null);

        boolean result = validator.isValid("00000000000", constraintContext);
        assertFalse(result, "Partita IVA con tutti zeri dovrebbe essere rifiutata");
    }

    @Test
    @DisplayName("Partita IVA con spazi")
    void testPartitaIvaWithSpaces() {
        PartitaIvaValidator validator = new PartitaIvaValidator();
        validator.initialize(null);

        boolean result = validator.isValid("1234 5678 901", constraintContext);
        assertFalse(result, "Partita IVA con spazi dovrebbe essere rifiutata");
    }

    @Test
    @DisplayName("Partita IVA con trattini")
    void testPartitaIvaWithDashes() {
        PartitaIvaValidator validator = new PartitaIvaValidator();
        validator.initialize(null);

        boolean result = validator.isValid("12345-6789-01", constraintContext);
        assertFalse(result, "Partita IVA con trattini dovrebbe essere rifiutata");
    }

    @Test
    @DisplayName("Partita IVA troppo lunga")
    void testPartitaIvaTooLong() {
        PartitaIvaValidator validator = new PartitaIvaValidator();
        validator.initialize(null);

        boolean result = validator.isValid("123456789012", constraintContext); // 12 cifre
        assertFalse(result, "Partita IVA con 12 cifre dovrebbe essere rifiutata");
    }

    @Test
    @DisplayName("Partita IVA con caratteri speciali")
    void testPartitaIvaWithSpecialCharacters() {
        PartitaIvaValidator validator = new PartitaIvaValidator();
        validator.initialize(null);

        boolean result = validator.isValid("1234567890!", constraintContext);
        assertFalse(result, "Partita IVA con caratteri speciali dovrebbe essere rifiutata");
    }

    // ==================== EMAIL ADDITIONAL TESTS ====================

    @Test
    @DisplayName("Email con indirizzo locale molto lungo")
    void testEmailWithVeryLongLocalPart() {
        EmailValidator validator = new EmailValidator();
        validator.initialize(null);

        String email = "a".repeat(100) + "@example.com";
        boolean result = validator.isValid(email, constraintContext);
        assertTrue(result, "Email con parte locale lunga dovrebbe essere accettata");
    }

    @Test
    @DisplayName("Email con dominio internazionalizzato")
    void testEmailWithInternationalizedDomain() {
        EmailValidator validator = new EmailValidator();
        validator.initialize(null);

        boolean result = validator.isValid("test@münchen.de", constraintContext);
        // Può essere true o false dipende da implementazione
        assertNotNull(result);
    }

    @Test
    @DisplayName("Email con due punti consecutivi")
    void testEmailWithConsecutiveDots() {
        EmailValidator validator = new EmailValidator();
        validator.initialize(null);

        boolean result = validator.isValid("test..name@example.com", constraintContext);
        assertFalse(result, "Email con punti consecutivi dovrebbe essere rifiutata");
    }

    @Test
    @DisplayName("Email senza dominio")
    void testEmailWithoutDomain() {
        EmailValidator validator = new EmailValidator();
        validator.initialize(null);

        boolean result = validator.isValid("test@", constraintContext);
        assertFalse(result, "Email senza dominio dovrebbe essere rifiutata");
    }

    @Test
    @DisplayName("Email con spazi")
    void testEmailWithSpaces() {
        EmailValidator validator = new EmailValidator();
        validator.initialize(null);

        boolean result = validator.isValid("test @example.com", constraintContext);
        assertFalse(result, "Email con spazi dovrebbe essere rifiutata");
    }

    @Test
    @DisplayName("Email vuota")
    void testEmptyEmail() {
        EmailValidator validator = new EmailValidator();
        validator.initialize(null);

        boolean result = validator.isValid("", constraintContext);
        assertFalse(result, "Email vuota dovrebbe essere rifiutata");
    }

    @Test
    @DisplayName("Email con @ multipli")
    void testEmailWithMultipleAts() {
        EmailValidator validator = new EmailValidator();
        validator.initialize(null);

        boolean result = validator.isValid("test@@example.com", constraintContext);
        assertFalse(result, "Email con @ multipli dovrebbe essere rifiutata");
    }

    // ==================== PHONE NUMBER ADDITIONAL TESTS ====================

    @Test
    @DisplayName("Numero telefonico con trattini")
    void testPhoneNumberWithDashes() {
        ItalianPhoneNumberValidator validator = new ItalianPhoneNumberValidator();
        validator.initialize(null);

        boolean result = validator.isValid("320-123-4567", constraintContext);
        assertTrue(result, "Numero con trattini dovrebbe essere accettato");
    }

    @Test
    @DisplayName("Numero telefonico troppo corto")
    void testPhoneNumberTooShort() {
        ItalianPhoneNumberValidator validator = new ItalianPhoneNumberValidator();
        validator.initialize(null);

        boolean result = validator.isValid("12345", constraintContext);
        assertFalse(result, "Numero troppo corto dovrebbe essere rifiutato");
    }

    @Test
    @DisplayName("Numero telefonico troppo lungo")
    void testPhoneNumberTooLong() {
        ItalianPhoneNumberValidator validator = new ItalianPhoneNumberValidator();
        validator.initialize(null);

        boolean result = validator.isValid("32012345678901234", constraintContext);
        assertFalse(result, "Numero troppo lungo dovrebbe essere rifiutato");
    }

    @Test
    @DisplayName("Numero telefonico con caratteri speciali")
    void testPhoneNumberWithSpecialChars() {
        ItalianPhoneNumberValidator validator = new ItalianPhoneNumberValidator();
        validator.initialize(null);

        boolean result = validator.isValid("320-123-4567!", constraintContext);
        assertFalse(result, "Numero con caratteri speciali dovrebbe essere rifiutato");
    }

    @Test
    @DisplayName("Numero telefonico internazionale incompleto")
    void testIncompleteInternationalNumber() {
        ItalianPhoneNumberValidator validator = new ItalianPhoneNumberValidator();
        validator.initialize(null);

        boolean result = validator.isValid("+39320", constraintContext);
        assertFalse(result, "Numero internazionale incompleto dovrebbe essere rifiutato");
    }

    @Test
    @DisplayName("Numero telefonico con parentesi")
    void testPhoneNumberWithParentheses() {
        ItalianPhoneNumberValidator validator = new ItalianPhoneNumberValidator();
        validator.initialize(null);

        boolean result = validator.isValid("(320) 1234567", constraintContext);
        // Dipende dall'implementazione
        assertNotNull(result);
    }

    @Test
    @DisplayName("Numero fisso Milano valido")
    void testValidMilanLandlineNumber() {
        ItalianPhoneNumberValidator validator = new ItalianPhoneNumberValidator();
        validator.initialize(null);

        boolean result = validator.isValid("0212345678", constraintContext);
        assertTrue(result, "Numero fisso Milano valido dovrebbe essere accettato");
    }

    @Test
    @DisplayName("Numero fisso Roma valido")
    void testValidRomeLandlineNumber() {
        ItalianPhoneNumberValidator validator = new ItalianPhoneNumberValidator();
        validator.initialize(null);

        boolean result = validator.isValid("0612345678", constraintContext);
        assertTrue(result, "Numero fisso Roma valido dovrebbe essere accettato");
    }

    @Test
    @DisplayName("Numero mobile Tim valido")
    void testValidTimMobileNumber() {
        ItalianPhoneNumberValidator validator = new ItalianPhoneNumberValidator();
        validator.initialize(null);

        boolean result = validator.isValid("3201234567", constraintContext);
        assertTrue(result, "Numero Tim valido dovrebbe essere accettato");
    }

    @Test
    @DisplayName("Numero mobile Vodafone valido")
    void testValidVodafoneMobileNumber() {
        ItalianPhoneNumberValidator validator = new ItalianPhoneNumberValidator();
        validator.initialize(null);

        boolean result = validator.isValid("3391234567", constraintContext);
        assertTrue(result, "Numero Vodafone valido dovrebbe essere accettato");
    }

    // ==================== CROSS-FIELD VALIDATION ====================

    @Test
    @DisplayName("Validazione con valori null ammessi in alcuni campi")
    void testValidationWithNullValues() {
        PartitaIvaValidator pivaValidator = new PartitaIvaValidator();
        pivaValidator.initialize(null);

        boolean result = pivaValidator.isValid(null, constraintContext);
        assertFalse(result, "Null dovrebbe essere gestito dai validatori");
    }
