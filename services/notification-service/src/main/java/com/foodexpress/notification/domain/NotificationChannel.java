package com.foodexpress.notification.domain;

/**
 * Notification channel types.
 */
public enum NotificationChannel {
    EMAIL,
    SMS,
    PUSH,
    IN_APP;
    
    public boolean requiresExternalService() {
        return this == EMAIL || this == SMS || this == PUSH;
    }
}
