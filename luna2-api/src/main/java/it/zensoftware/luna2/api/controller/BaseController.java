package it.zensoftware.luna2.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.time.LocalDateTime;

/**
 * API Base - Health check e informazioni server.
 */
@RestController
@RequestMapping("/api/v1")
public class BaseController {

    @GetMapping("/health")
    public ResponseEntity<HealthStatus> health() {
        return ResponseEntity.ok(new HealthStatus(
            "UP",
            LocalDateTime.now(),
            "Luna2 REST API 1.0.0"
        ));
    }

    @GetMapping("/info")
    public ResponseEntity<ApiInfo> info() {
        return ResponseEntity.ok(new ApiInfo(
            "Luna2 REST API",
            "1.0.0",
            "REST API completa per Luna2 CRM",
            "2026-02-23",
            new ApiEndpoints()
        ));
    }

    public static class HealthStatus {
        public String status;
        public LocalDateTime timestamp;
        public String version;

        public HealthStatus(String status, LocalDateTime timestamp, String version) {
            this.status = status;
            this.timestamp = timestamp;
            this.version = version;
        }
    }

    public static class ApiInfo {
        public String name;
        public String version;
        public String description;
        public String releaseDate;
        public ApiEndpoints endpoints;

        public ApiInfo(String name, String version, String description, String releaseDate, ApiEndpoints endpoints) {
            this.name = name;
            this.version = version;
            this.description = description;
            this.releaseDate = releaseDate;
            this.endpoints = endpoints;
        }
    }

    public static class ApiEndpoints {
        public String clienti = "GET /api/v1/clienti - Lista clienti";
        public String ordini = "GET /api/v1/ordini - Lista ordini";
        public String fatture = "GET /api/v1/fatture - Lista fatture";
        public String dashboard = "GET /api/v1/dashboard - Dashboard stats";
        public String calendario = "GET /api/v1/calendario/events - Eventi calendario";
        public String notifiche = "GET /api/v1/notifiche - Notifiche utente";
    }
}
