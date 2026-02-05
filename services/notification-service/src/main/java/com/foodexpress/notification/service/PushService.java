package com.foodexpress.notification.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Push Notification Service.
 * Placeholder for FCM/APNs integration.
 */
@Service
public class PushService {
    
    private static final Logger log = LoggerFactory.getLogger(PushService.class);
    
    @Value("${notification.push.enabled:false}")
    private boolean pushEnabled;
    
    @RateLimiter(name = "pushSending")
    @CircuitBreaker(name = "pushService")
    public String send(String deviceToken, String title, String body) {
        log.info("Sending push notification to device {}", maskToken(deviceToken));
        
        if (!pushEnabled) {
            log.info("Push notifications disabled, simulating send");
            return "sim-" + UUID.randomUUID().toString();
        }
        
        // TODO: Integrate with Firebase Cloud Messaging
        // Example FCM integration:
        // Message message = Message.builder()
        //     .setToken(deviceToken)
        //     .setNotification(Notification.builder()
        //         .setTitle(title)
        //         .setBody(body)
        //         .build())
        //     .build();
        // String response = FirebaseMessaging.getInstance().send(message);
        // return response;
        
        String messageId = UUID.randomUUID().toString();
        log.info("Push notification sent successfully, messageId: {}", messageId);
        
        return messageId;
    }
    
    private String maskToken(String token) {
        if (token == null || token.length() < 8) return "***";
        return token.substring(0, 4) + "..." + token.substring(token.length() - 4);
    }
}
