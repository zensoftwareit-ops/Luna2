package it.zensoftware.luna2.api.controller;

import it.zensoftware.luna2.api.service.AuthenticationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * Authentication endpoints (JWT).
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticationService authenticationService;

    public AuthController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        if (request == null || request.username == null || request.password == null) {
            return ResponseEntity.badRequest().body(LoginResponse.error("Credenziali mancanti"));
        }

        AuthenticationService.LoginResult result = authenticationService.authenticate(
            request.username,
            request.password
        );

        if (!result.success) {
            return ResponseEntity.status(401).body(LoginResponse.error(result.message));
        }

        return ResponseEntity.ok(LoginResponse.ok(result.token));
    }

    public static class LoginRequest {
        public String username;
        public String password;
    }

    public static class LoginResponse {
        public boolean success;
        public String token;
        public String message;
        public LocalDateTime timestamp;

        public static LoginResponse ok(String token) {
            LoginResponse res = new LoginResponse();
            res.success = true;
            res.token = token;
            res.message = "Login riuscito";
            res.timestamp = LocalDateTime.now();
            return res;
        }

        public static LoginResponse error(String message) {
            LoginResponse res = new LoginResponse();
            res.success = false;
            res.token = null;
            res.message = message;
            res.timestamp = LocalDateTime.now();
            return res;
        }
    }
}
