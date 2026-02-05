package com.foodexpress.notification.dto;

import com.foodexpress.notification.domain.*;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Notification Service DTOs.
 */
public final class NotificationDTOs {
    
    private NotificationDTOs() {}
    
    // ========================================
    // REQUEST DTOs
    // ========================================
    
    public record SendNotificationRequest(
            @NotBlank(message = "User ID is required")
            String userId,
            
            @NotNull(message = "Notification type is required")
            NotificationType type,
            
            @NotEmpty(message = "At least one channel is required")
            List<NotificationChannel> channels,
            
            String title,
            String message,
            
            String referenceType,
            String referenceId,
            
            Map<String, String> templateData
    ) {}
    
    public record SendBulkNotificationRequest(
            @NotEmpty(message = "User IDs are required")
            List<String> userIds,
            
            @NotNull
            NotificationType type,
            
            @NotEmpty
            List<NotificationChannel> channels,
            
            String title,
            String message,
            
            Map<String, String> templateData
    ) {}
    
    // ========================================
    // RESPONSE DTOs
    // ========================================
    
    public record NotificationResponse(
            String id,
            String userId,
            String type,
            String channel,
            String status,
            String title,
            String message,
            boolean isRead,
            LocalDateTime createdAt,
            LocalDateTime sentAt,
            LocalDateTime deliveredAt,
            LocalDateTime readAt
    ) {}
    
    public record NotificationSummary(
            long total,
            long unread,
            List<NotificationResponse> recent
    ) {}
    
    public record DeliveryStats(
            long totalSent,
            long delivered,
            long failed,
            long pending,
            double deliveryRate
    ) {}
    
    // ========================================
    // MAPPERS
    // ========================================
    
    public static NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getUserId(),
                notification.getType().name(),
                notification.getChannel().name(),
                notification.getStatus().name(),
                notification.getTitle(),
                notification.getMessage(),
                notification.isRead(),
                notification.getCreatedAt(),
                notification.getSentAt(),
                notification.getDeliveredAt(),
                notification.getReadAt()
        );
    }
}
