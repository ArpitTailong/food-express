package com.foodexpress.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Notification Service Application.
 * 
 * Multi-channel notification delivery:
 * - Email (SMTP with templates)
 * - SMS (Twilio integration ready)
 * - Push Notifications (FCM ready)
 * - In-App (WebSocket)
 * 
 * Consumes events from Kafka topics:
 * - order-events
 * - payment-events
 * - user-events
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class NotificationServiceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
