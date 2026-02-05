package com.foodexpress.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

/**
 * Spring Cloud Config Server for FoodExpress.
 * 
 * Provides centralized configuration management for all microservices.
 * Supports configuration from:
 * - Git repository
 * - Local file system (for development)
 * - Vault (for secrets)
 * 
 * Access configuration at: http://localhost:8888/{application}/{profile}
 */
@SpringBootApplication
@EnableConfigServer
public class ConfigServerApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(ConfigServerApplication.class, args);
    }
}
