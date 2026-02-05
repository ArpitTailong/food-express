package com.foodexpress.discovery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * Eureka Discovery Server for FoodExpress Microservices.
 * 
 * All microservices register themselves with this server,
 * enabling service discovery and load balancing.
 * 
 * Access the Eureka Dashboard at: http://localhost:8761
 * 
 * Features:
 * - Service Registration & Discovery
 * - Health Monitoring
 * - Self-Preservation Mode
 * - Secured Dashboard Access
 */
@SpringBootApplication
@EnableEurekaServer
public class DiscoveryServerApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(DiscoveryServerApplication.class, args);
    }
}
