package it.zensoftware.luna2.api.service;

import it.zensoftware.luna2.dao.UserDAO;
import it.zensoftware.luna2.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

/**
 * AuthenticationService - JWT Authentication per REST API.
 * 
 * TODO: Integrare con Luna2 User table via JPA.
 * 
 * Utilizzo:
 * ```
 * AuthenticationService authService = new AuthenticationService();
 * LoginResult result = authService.authenticate("user@example.com", "password");
 * if (result.isSuccess()) {
 *   String token = result.getToken();  // Usa questo nel header Authorization: Bearer token
 * }
 * ```
 */
@Service
public class AuthenticationService {

    private static final Logger logger = LogManager.getLogger(AuthenticationService.class);
    private final UserDAO userDAO = new UserDAO();

    @Value("${jwt.secret:ChangeThisSecretInProduction}")
    private String jwtSecret;

    @Value("${jwt.exp-minutes:60}")
    private long tokenExpiryMinutes;

    @Value("${jwt.issuer:luna2-api}")
    private String tokenIssuer;

    /**
     * Autentica utente con email/password.
     * TODO: Integrare con UserDAO.findByEmail() e password hash verification.
     */
    public LoginResult authenticate(String usernameOrEmail, String password) {
        try {
            logger.debug("Tentativo login per: {}", usernameOrEmail);

            User user = resolveUser(usernameOrEmail, password);
            if (user == null) {
                return new LoginResult(false, null, "Credenziali invalide", LocalDateTime.now());
            }

            String token = generateJwtToken(user);
            return new LoginResult(true, token, "Login riuscito", LocalDateTime.now());
        } catch (Exception e) {
            logger.error("Errore authentication", e);
            return new LoginResult(false, null, "Errore server", LocalDateTime.now());
        }
    }

    /**
     * Genera JWT token.
     * TODO: Usare libreria JWT (io.jsonwebtoken:jjwt)
     */
    public String generateJwtToken(User user) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + (tokenExpiryMinutes * 60 * 1000));

        return Jwts.builder()
                .setSubject(user.getUsername())
                .setIssuer(tokenIssuer)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .claim("userId", user.getId())
                .claim("role", user.getRuolo().name())
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Valida JWT token.
     * TODO: Implementare con io.jsonwebtoken
     */
    public boolean validateToken(String token) {
        try {
            Claims claims = parseClaims(token);
            return claims.getExpiration() != null && claims.getExpiration().after(new Date());
        } catch (Exception e) {
            logger.warn("JWT non valido: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Estrae email dal token.
     */
    public String extractUsername(String token) {
        try {
            return parseClaims(token).getSubject();
        } catch (Exception e) {
            return null;
        }
    }

    public Long extractUserId(String token) {
        try {
            Object userId = parseClaims(token).get("userId");
            if (userId instanceof Number) {
                return ((Number) userId).longValue();
            }
            if (userId != null) {
                return Long.valueOf(userId.toString());
            }
        } catch (Exception e) {
            logger.warn("Impossibile estrarre userId dal token", e);
        }
        return null;
    }

    public String extractRole(String token) {
        try {
            Object role = parseClaims(token).get("role");
            return role != null ? role.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private User resolveUser(String usernameOrEmail, String password) {
        if (usernameOrEmail == null || password == null) {
            return null;
        }

        if (usernameOrEmail.contains("@")) {
            User user = userDAO.findByEmail(usernameOrEmail);
            if (user != null && user.getAttivo()) {
                boolean ok = org.mindrot.jbcrypt.BCrypt.checkpw(password, user.getPassword());
                return ok ? user : null;
            }
            return null;
        }

        return userDAO.authenticate(usernameOrEmail, password);
    }

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private SecretKey getSigningKey() {
        String secret = jwtSecret != null ? jwtSecret : "ChangeThisSecretInProduction";
        if (secret.length() < 32) {
            secret = (secret + "00000000000000000000000000000000").substring(0, 32);
        }
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    
    public static class LoginResult {
        public boolean success;
        public String token;
        public String message;
        public LocalDateTime timestamp;

        public LoginResult(boolean success, String token, String message, LocalDateTime timestamp) {
            this.success = success;
            this.token = token;
            this.message = message;
            this.timestamp = timestamp;
        }
    }
}
