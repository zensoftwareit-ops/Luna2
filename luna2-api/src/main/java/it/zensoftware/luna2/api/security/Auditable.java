package it.zensoftware.luna2.api.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation per marcare metodi che devono essere auditati
 * Utilizzata da AuditLoggingAspect per tracciare operazioni sensibili
 *
 * Esempio:
 * @Auditable(action = "CREATE_FATTURA", entityType = "Fattura")
 * public ResponseEntity<FatturaDTO> create(@Valid @RequestBody FatturaDTO dto)
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {
    /**
     * Azione che viene tracciata (es. CREATE, UPDATE, DELETE, SEND)
     */
    String action() default "";

    /**
     * Tipo di entità su cui l'azione agisce (es. Fattura, Cliente, Pagamento)
     */
    String entityType() default "";

    /**
     * Indica se l'esito positivo deve essere registrato
     * Default: true
     */
    boolean logSuccess() default true;

    /**
     * Indica se i dettagli del fallimento devono essere registrati
     * Default: true
     */
    boolean logFailure() default true;
}
