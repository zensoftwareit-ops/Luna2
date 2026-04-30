package it.zensoftware.luna2.api.security;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * AOP Aspect per Rate Limiting
 * Intercetta metodiannotati con @RateLimited e verifica limite
 *
 * Ritorna HTTP 429 (Too Many Requests) se rate limit superato
 */
@Aspect
@Component
public class RateLimitedAspect {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitedAspect.class);

    @Autowired
    private RateLimitingService rateLimitingService;

    @Around("@annotation(rateLimited)")
    public Object rateLimit(ProceedingJoinPoint pjp, RateLimited rateLimited) throws Throwable {
        String key = generateKey(pjp, rateLimited.key());

        boolean allowed = rateLimitingService.tryConsumeWindow(
            key,
            rateLimited.maxRequests(),
            rateLimited.windowMinutes()
        );

        if (!allowed) {
            logger.warn("Rate limit exceeded for {} ({}req/{}min)",
                key, rateLimited.maxRequests(), rateLimited.windowMinutes());

            return ResponseEntity
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .body(new RateLimitError(
                    429,
                    "Rate limit exceeded",
                    "Max " + rateLimited.maxRequests() + " requests per " +
                    rateLimited.windowMinutes() + " minute(s)",
                    System.currentTimeMillis()
                ));
        }

        // Rate limit ok - procedi con la richiesta
        return pjp.proceed();
    }

    /**
     * Genera chiave univoca per il rate limiter
     * Combina: IP Address + Endpoint + Custom Key
     */
    private String generateKey(ProceedingJoinPoint pjp, String customKey) {
        String ip = getClientIpAddress();
        String endpoint = pjp.getTarget().getClass().getSimpleName() + "." + pjp.getSignature().getName();

        if (customKey != null && !customKey.isEmpty()) {
            return customKey + "-" + ip;
        }

        return ip + "-" + endpoint;
    }

    /**
     * Estrae IP client dalla request (supporta proxy/load balancer)
     */
    private String getClientIpAddress() {
        try {
            HttpServletRequest request =
                ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes())
                    .getRequest();

            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                return xForwardedFor.split(",")[0].trim();
            }

            String xRealIp = request.getHeader("X-Real-IP");
            if (xRealIp != null && !xRealIp.isEmpty()) {
                return xRealIp;
            }

            return request.getRemoteAddr();

        } catch (Exception e) {
            logger.warn("Error getting client IP, using default", e);
            return "unknown";
        }
    }

    // ==================== ERROR RESPONSE DTO ====================

    public static class RateLimitError {
        public int status;
        public String message;
        public String detail;
        public long timestamp;

        public RateLimitError(int status, String message, String detail, long timestamp) {
            this.status = status;
            this.message = message;
            this.detail = detail;
            this.timestamp = timestamp;
        }

        public int getStatus() { return status; }
        public String getMessage() { return message; }
        public String getDetail() { return detail; }
        public long getTimestamp() { return timestamp; }
    }
}
