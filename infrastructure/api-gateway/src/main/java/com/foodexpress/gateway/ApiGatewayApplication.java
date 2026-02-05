package com.foodexpress.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * API Gateway for FoodExpress Microservices.
 * 
 * Features:
 * - Route management to all microservices
 * - JWT validation and authentication
 * - Rate limiting (Redis-based)
 * - Circuit breaker for fault tolerance
 * - Request/Response logging
 * - Correlation ID propagation
 * - Load balancing (via Eureka)
 * 
 * All external traffic enters through this gateway.
 */
@SpringBootApplication
@EnableDiscoveryClient
public class ApiGatewayApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
