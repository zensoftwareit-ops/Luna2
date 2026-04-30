package it.zensoftware.luna2.api.security;

import java.lang.annotation.*;

/**
 * Annotation per rate limiting di endpoint
 * Previene brute force attacks e abuso API
 *
 * Uso:
 * @RateLimited(maxRequests = 100, windowMinutes = 5)
 * public ResponseEntity<> endpoint(...) { ... }
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimited {
    
    /**
     * Numero massimo di richieste per la finestra temporale
     */
    int maxRequests() default 100;
    
    /**
     * Finestra temporale in minuti
     */
    int windowMinutes() default 5;
    
    /**
     * Identificatore univoco per il rate limiter (es. endpoint name)
     * Se non specificato, usa il nome della classe + metodo
     */
    String key() default "";
}
