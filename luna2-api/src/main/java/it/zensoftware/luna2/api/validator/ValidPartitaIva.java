package it.zensoftware.luna2.api.validator;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

/**
 * Validazione Partita IVA italiana
 * Formato: 11 cifre numeriche
 * Algoritmo di check digit con modulo 97
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PartitaIvaValidator.class)
@Documented
public @interface ValidPartitaIva {
    String message() default "Partita IVA non valida. Formato: 11 cifre numeriche";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
