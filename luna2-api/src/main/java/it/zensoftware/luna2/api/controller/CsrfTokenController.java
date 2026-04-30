package it.zensoftware.luna2.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller per gestire CSRF token
 *
 * Client flow:
 * 1. GET /api/csrf → riceve token
 * 2. Includi X-CSRF-Token header in POST/PUT/DELETE requests
 */
@RestController
@RequestMapping("/api")
public class CsrfTokenController {

    /**
     * Genereaza CSRF token per client
     * Da chiamare PRIMA di effettuare POST/PUT/DELETE
     *
     * Response:
     * {
     *   "token": "...",
     *   "headerName": "X-CSRF-TOKEN",
     *   "parameterName": "_csrf"
     * }
     */
    @GetMapping("/csrf")
    public ResponseEntity<CsrfTokenDto> getCsrfToken(CsrfToken token) {
        return ResponseEntity.ok(new CsrfTokenDto(
            token.getToken(),
            token.getHeaderName(),
            token.getParameterName()
        ));
    }

    public static class CsrfTokenDto {
        public String token;
        public String headerName;
        public String parameterName;

        public CsrfTokenDto(String token, String headerName, String parameterName) {
            this.token = token;
            this.headerName = headerName;
            this.parameterName = parameterName;
        }

        public String getToken() { return token; }
        public String getHeaderName() { return headerName; }
        public String getParameterName() { return parameterName; }
    }
}
