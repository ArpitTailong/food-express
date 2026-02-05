package com.foodexpress.notification.domain;

/**
 * Notification delivery status.
 */
public enum NotificationStatus {
    PENDING,
    SENT,
    DELIVERED,
    FAILED,
    CANCELLED;
    
    public boolean isTerminal() {
        return this == DELIVERED || this == FAILED || this == CANCELLED;
    }
    
    public boolean canRetry() {
        return this == FAILED;
    }
}
