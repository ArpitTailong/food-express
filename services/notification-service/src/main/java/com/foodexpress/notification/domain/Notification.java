package com.foodexpress.notification.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Notification Entity.
 * Tracks notification delivery across all channels.
 */
@Entity
@Table(name = "notifications")
public class Notification {
    
    @Id
    private String id;
    
    @Column(name = "user_id", nullable = false)
    private String userId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationChannel channel;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationStatus status;
    
    @Column(nullable = false)
    private String title;
    
    @Column(columnDefinition = "TEXT", nullable = false)
    private String message;
    
    // Recipient info based on channel
    @Column
    private String recipient; // email, phone, or device token
    
    // Reference to the source event
    @Column(name = "reference_type")
    private String referenceType; // ORDER, PAYMENT, etc.
    
    @Column(name = "reference_id")
    private String referenceId;
    
    // Tracking
    @Column(name = "external_id")
    private String externalId; // ID from email/SMS provider
    
    @Column(name = "retry_count")
    private int retryCount;
    
    @Column(name = "max_retries")
    private int maxRetries;
    
    @Column(name = "failure_reason")
    private String failureReason;
    
    // User interaction
    @Column(name = "is_read")
    private boolean isRead;
    
    @Column(name = "read_at")
    private LocalDateTime readAt;
    
    // Metadata
    @Column(columnDefinition = "jsonb")
    private String metadata;
    
    // Timestamps
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "sent_at")
    private LocalDateTime sentAt;
    
    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;
    
    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;
    
    protected Notification() {}
    
    public Notification(String userId, NotificationType type, NotificationChannel channel,
                       String title, String message, String recipient) {
        this.id = UUID.randomUUID().toString();
        this.userId = userId;
        this.type = type;
        this.channel = channel;
        this.title = title;
        this.message = message;
        this.recipient = recipient;
        this.status = NotificationStatus.PENDING;
        this.retryCount = 0;
        this.maxRetries = 3;
        this.isRead = false;
        this.createdAt = LocalDateTime.now();
    }
    
    // Business methods
    public void markSent(String externalId) {
        this.status = NotificationStatus.SENT;
        this.externalId = externalId;
        this.sentAt = LocalDateTime.now();
    }
    
    public void markDelivered() {
        this.status = NotificationStatus.DELIVERED;
        this.deliveredAt = LocalDateTime.now();
    }
    
    public void markFailed(String reason) {
        if (retryCount < maxRetries) {
            this.retryCount++;
            this.failureReason = reason;
            // Exponential backoff: 1, 2, 4 minutes
            int delayMinutes = (int) Math.pow(2, retryCount - 1);
            this.nextRetryAt = LocalDateTime.now().plusMinutes(delayMinutes);
        } else {
            this.status = NotificationStatus.FAILED;
            this.failureReason = reason;
            this.nextRetryAt = null;
        }
    }
    
    public void cancel() {
        this.status = NotificationStatus.CANCELLED;
    }
    
    public void markRead() {
        this.isRead = true;
        this.readAt = LocalDateTime.now();
    }
    
    public boolean shouldRetry() {
        return status != NotificationStatus.DELIVERED 
                && status != NotificationStatus.CANCELLED
                && retryCount < maxRetries
                && nextRetryAt != null
                && LocalDateTime.now().isAfter(nextRetryAt);
    }
    
    // Getters
    public String getId() { return id; }
    public String getUserId() { return userId; }
    public NotificationType getType() { return type; }
    public NotificationChannel getChannel() { return channel; }
    public NotificationStatus getStatus() { return status; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public String getRecipient() { return recipient; }
    public String getReferenceType() { return referenceType; }
    public String getReferenceId() { return referenceId; }
    public String getExternalId() { return externalId; }
    public int getRetryCount() { return retryCount; }
    public int getMaxRetries() { return maxRetries; }
    public String getFailureReason() { return failureReason; }
    public boolean isRead() { return isRead; }
    public LocalDateTime getReadAt() { return readAt; }
    public String getMetadata() { return metadata; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getSentAt() { return sentAt; }
    public LocalDateTime getDeliveredAt() { return deliveredAt; }
    public LocalDateTime getNextRetryAt() { return nextRetryAt; }
    
    // Setters for references and metadata
    public void setReference(String type, String id) {
        this.referenceType = type;
        this.referenceId = id;
    }
    
    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }
}
