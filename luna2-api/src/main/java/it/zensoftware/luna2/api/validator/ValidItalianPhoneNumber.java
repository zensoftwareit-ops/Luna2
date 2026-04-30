package it.zensoftware.luna2.api.validator;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

/**
 * Validazione Numero Telefono italiano
 * Formati supportati:
 * - 0XX XXXXXXX (fisso)
 * - 3XX XXXXXXX (mobile)
 * - +39 3XX XXXXXXX (internazionale)
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ItalianPhoneNumberValidator.class)
@Documented
public @interface ValidItalianPhoneNumber {
    String message() default "Numero di telefono italiano non valido";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
