package com.foodexpress.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Authentication Service for FoodExpress.
 * 
 * Features:
 * - JWT token generation and validation
 * - OKTA OAuth2 integration
 * - Role-based access control (RBAC)
 * - Token refresh mechanism
 * - Token blacklisting (Redis)
 * - Multi-factor authentication support
 */
@SpringBootApplication(scanBasePackages = {
        "com.foodexpress.auth",
        "com.foodexpress.common"
})
@EnableDiscoveryClient
@EnableFeignClients
public class AuthServiceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
