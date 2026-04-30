package it.zensoftware.luna2.api.security;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Service per gestire Rate Limiting degli endpoint
 * Utilizza Guava's RateLimiter per thread-safe rate limiting
 *
 * Supporta:
 * - Per-IP rate limiting
 * - Per-endpoint rate limiting
 * - Configurable request windows
 */
@Service
public class RateLimitingService {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitingService.class);

    private final Cache<String, RateLimiter> limiters;

    public RateLimitingService() {
        // Cache di RateLimiter, evict dopo 1 ora di inattività
        this.limiters = CacheBuilder.newBuilder()
            .expireAfterAccess(1, TimeUnit.HOURS)
            .build();
    }

    /**
     * Controlla se una richiesta può essere processata
     *
     * @param key identificatore univoco (es. ipAddress-endpoint)
     * @param requestsPerSecond numero di richieste consentite al secondo
     * @return true se richiesta può procedere, false se rate limit superato
     */
    public boolean tryConsume(String key, double requestsPerSecond) {
        try {
            RateLimiter rateLimiter = limiters.get(key, () -> RateLimiter.create(requestsPerSecond));
            boolean allowed = rateLimiter.tryAcquire();

            if (!allowed) {
                logger.warn("Rate limit exceeded per key: {}", key);
            }

            return allowed;

        } catch (ExecutionException e) {
            logger.error("Errore nel rate limiting per key: {}", key, e);
            // In caso di errore, consenti comunque (fail-open per disponibilità)
            return true;
        }
    }

    /**
     * Controlla blocking (attende se necessario)
     * Non usare per production HTTP requests - usa tryConsume invece
     */
    public void acquire(String key, double requestsPerSecond) {
        try {
            RateLimiter limiter = limiters.get(key, () -> RateLimiter.create(requestsPerSecond));
            limiter.acquire();
        } catch (ExecutionException e) {
            logger.error("Errore nel rate limiting blocking: {}", key, e);
        }
    }

    /**
     * Converte window (req per N minuti) a req per secondo
     */
    public static double convertToRequestsPerSecond(int requests, int minutes) {
        return (double) requests / (minutes * 60);
    }

    /**
     * Factory: crea limiter con configurazione max request / window
     */
    public boolean tryConsumeWindow(String key, int maxRequests, int windowMinutes) {
        double requestsPerSecond = convertToRequestsPerSecond(maxRequests, windowMinutes);
        return tryConsume(key, requestsPerSecond);
    }
}
