package it.zensoftware.luna2.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Luna2 REST API - Spring Boot Application
 * Porta: 8081
 * Endpoint base: /api/v1/
 * WebSocket: /ws/
 * Swagger: /api-docs
 */
@SpringBootApplication
@EnableScheduling
@ComponentScan(basePackages = {
    "it.zensoftware.luna2.api"
})
public class Luna2ApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(Luna2ApiApplication.class, args);
    }
}
