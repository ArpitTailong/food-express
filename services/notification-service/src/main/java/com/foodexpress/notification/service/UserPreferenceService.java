package com.foodexpress.notification.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * User Preference Service.
 * Fetches user contact info and notification preferences.
 */
@Service
public class UserPreferenceService {
    
    private static final Logger log = LoggerFactory.getLogger(UserPreferenceService.class);
    
    // TODO: Use Feign client to call User Service
    // private final UserServiceClient userServiceClient;
    
    public UserContactInfo getUserContactInfo(String userId) {
        log.debug("Fetching contact info for user {}", userId);
        
        // TODO: Call User Service via Feign
        // UserResponse user = userServiceClient.getUser(userId);
        // return new UserContactInfo(
        //     user.id(),
        //     user.email(),
        //     user.phoneNumber(),
        //     user.deviceToken()
        // );
        
        // Placeholder - in production, fetch from User Service
        return new UserContactInfo(
                userId,
                userId + "@example.com",
                "+919876543210",
                "fcm-token-" + userId
        );
    }
    
    public NotificationPreferences getPreferences(String userId) {
        log.debug("Fetching notification preferences for user {}", userId);
        
        // TODO: Call User Service to get preferences
        // Placeholder - default preferences
        return new NotificationPreferences(
                true,  // emailNotifications
                true,  // smsNotifications
                true,  // pushNotifications
                false  // promotionalEmails
        );
    }
    
    // Records for contact info and preferences
    public record UserContactInfo(
            String userId,
            String email,
            String phoneNumber,
            String deviceToken
    ) {}
    
    public record NotificationPreferences(
            boolean emailNotifications,
            boolean smsNotifications,
            boolean pushNotifications,
            boolean promotionalEmails
    ) {}
}
