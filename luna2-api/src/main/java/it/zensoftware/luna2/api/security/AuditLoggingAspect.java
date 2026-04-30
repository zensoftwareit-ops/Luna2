package it.zensoftware.luna2.api.security;

import it.zensoftware.luna2.dao.AuditLogDAO;
import it.zensoftware.luna2.model.AuditLog;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.UUID;

/**
 * AOP Aspect per audit logging
 * Intercetta metodi annotati con @Auditable e registra operazioni sensibili
 * nel database per tracciamento e compliance normativa
 */
@Aspect
@Component
public class AuditLoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditLoggingAspect.class);
    private final AuditLogDAO auditLogDAO = new AuditLogDAO();

    /**
     * Intercetta metodi annotati con @Auditable
     * Registra informazioni sulla richiesta, esecuzione e risultato
     */
    @Around("@annotation(auditable)")
    public Object auditMethod(ProceedingJoinPoint joinPoint, Auditable auditable) throws Throwable {
        String action = auditable.action();
        String entityType = auditable.entityType();

        // Estrae informazioni dalla richiesta HTTP
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes != null ? attributes.getRequest() : null;
        String ipAddress = extractClientIp(request);
        String httpMethod = request != null ? request.getMethod() : "UNKNOWN";
        String endpoint = request != null ? request.getRequestURI() : "UNKNOWN";

        // Recupera autenticazione
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userName = auth != null ? auth.getName() : "ANONYMOUS";

        // Crea entry di audit
        AuditLog auditLog = new AuditLog();
        auditLog.setUserName(userName);
        auditLog.setAction(action);
        auditLog.setEntityType(entityType);
        auditLog.setIpAddress(ipAddress);
        auditLog.setHttpMethod(httpMethod);
        auditLog.setEndpoint(endpoint);

        long startTime = System.currentTimeMillis();
        Object result = null;
        boolean success = false;
        Exception exception = null;

        try {
            // Esegue il metodo originale
            result = joinPoint.proceed();
            success = true;
            auditLog.setResult("SUCCESS");
            auditLog.setStatusCode(extractStatusCode(result));

            if (auditable.logSuccess()) {
                log.info("[AUDIT] {} - {} by {} from {} - SUCCESS", action, entityType, userName, ipAddress);
            }

        } catch (Throwable e) {
            exception = new Exception(e);
            auditLog.setResult("FAILURE");
            auditLog.setStatusCode(extractErrorStatusCode(e));
            auditLog.setDetails(e.getMessage() != null ? e.getMessage().substring(0, Math.min(e.getMessage().length(), 500)) : "Unknown error");

            if (auditable.logFailure()) {
                log.warn("[AUDIT] {} - {} by {} from {} - FAILURE: {}", action, entityType, userName, ipAddress, e.getMessage());
            }

            throw e;

        } finally {
            // Calcola durata
            long duration = System.currentTimeMillis() - startTime;

            // Tenta di estrarre ID entità da parametri del metodo
            Long entityId = extractEntityId(joinPoint);
            if (entityId != null) {
                auditLog.setEntityId(entityId);
            }

            // Aggiungi informazioni aggiuntive nei dettagli
            if (auditLog.getDetails() == null) {
                auditLog.setDetails("Duration: " + duration + "ms");
            }

            // Salva il log di audit in modo asincrono
            saveAuditLogAsync(auditLog);
        }

        return result;
    }

    /**
     * Estrae l'IP del client dalla richiesta HTTP
     * Supporta proxy headers (X-Forwarded-For, X-Real-IP)
     */
    private String extractClientIp(HttpServletRequest request) {
        if (request == null) {
            return "UNKNOWN";
        }

        // Controlla X-Forwarded-For (per proxy)
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        // Controlla X-Real-IP (per nginx proxy)
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        // Usa l'IP remoto della connessione
        return request.getRemoteAddr();
    }

    /**
     * Estrae il codice di status dalla response
     */
    private Integer extractStatusCode(Object result) {
        if (result instanceof org.springframework.http.ResponseEntity) {
            org.springframework.http.ResponseEntity<?> response = (org.springframework.http.ResponseEntity<?>) result;
            return response.getStatusCode().value();
        }
        return 200; // Default per successful execution
    }

    /**
     * Estrae il codice di status nel caso di errore
     */
    private Integer extractErrorStatusCode(Throwable e) {
        // Può essere esteso per mappare eccezioni specifiche a codici HTTP
        return 500;
    }

    /**
     * Tenta di estrarre l'ID dell'entità dai parametri del metodo
     * Cerca parametri di tipo Long con nome "id" o che terminano con "Id"
     */
    private Long extractEntityId(ProceedingJoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        String[] paramNames = getParameterNames(joinPoint);

        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof Long) {
                String paramName = paramNames != null && i < paramNames.length ? paramNames[i] : "";
                if ("id".equalsIgnoreCase(paramName) || paramName.toLowerCase().endsWith("id")) {
                    return (Long) args[i];
                }
            }
        }

        return null;
    }

    /**
     * Estrae nomi dei parametri - implementazione semplice
     * In un'applicazione reale, usare javassist o Spring's LocalVariableTableParameterNameDiscoverer
     */
    private String[] getParameterNames(ProceedingJoinPoint joinPoint) {
        // Implementazione semplificata - ritorna null
        // In produzione, usare Spring ParameterNameDiscoverer
        return null;
    }

    /**
     * Salva il log di audit in modo asincrono per non bloccare la risposta
     */
    private void saveAuditLogAsync(AuditLog auditLog) {
        Thread auditThread = new Thread(() -> {
            try {
                auditLogDAO.save(auditLog);
                log.debug("Audit log salvato: {}", auditLog);
            } catch (Exception e) {
                log.error("Errore nel salvataggio audit log", e);
                // Non rethrow per non interrompere il flusso originale
            }
        });
        auditThread.setName("AuditLogger-" + UUID.randomUUID());
        auditThread.setDaemon(true);
        auditThread.start();
    }
}
