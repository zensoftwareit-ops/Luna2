package it.zensoftware.luna2.api.validator;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

/**
 * Validazione Email italiana/internazionale
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = EmailValidator.class)
@Documented
public @interface ValidEmail {
    String message() default "Email non valida";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
