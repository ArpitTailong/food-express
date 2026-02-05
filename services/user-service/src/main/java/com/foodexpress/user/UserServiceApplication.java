package com.foodexpress.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * User Service Application.
 * 
 * Manages user profiles, addresses, and preferences for:
 * - Customers
 * - Drivers
 * - Restaurant Owners
 * 
 * Features:
 * - Profile CRUD with soft delete
 * - Multiple delivery addresses per user
 * - User preferences (dietary restrictions, notification settings)
 * - Favorites (restaurants, menu items)
 * - GDPR compliance (data export, deletion)
 */
@SpringBootApplication
@EnableCaching
@EnableAsync
public class UserServiceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
