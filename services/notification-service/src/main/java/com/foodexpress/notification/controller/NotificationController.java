package com.foodexpress.notification.controller;

import com.foodexpress.notification.dto.NotificationDTOs.*;
import com.foodexpress.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/**
 * Notification REST API.
 */
@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "Notifications", description = "Notification management APIs")
public class NotificationController {
    
    private final NotificationService notificationService;
    
    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }
    
    // ========================================
    // USER ENDPOINTS
    // ========================================
    
    @GetMapping
    @Operation(summary = "Get in-app notifications")
    public ResponseEntity<Page<NotificationResponse>> getNotifications(
            @AuthenticationPrincipal Jwt jwt,
            @PageableDefault(size = 20) Pageable pageable) {
        
        String userId = jwt.getSubject();
        Page<NotificationResponse> notifications = notificationService.getInAppNotifications(userId, pageable);
        
        return ResponseEntity.ok(notifications);
    }
    
    @GetMapping("/summary")
    @Operation(summary = "Get notification summary with unread count")
    public ResponseEntity<NotificationSummary> getSummary(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        NotificationSummary summary = notificationService.getNotificationSummary(userId);
        
        return ResponseEntity.ok(summary);
    }
    
    @PostMapping("/{notificationId}/read")
    @Operation(summary = "Mark notification as read")
    public ResponseEntity<Void> markAsRead(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String notificationId) {
        
        notificationService.markAsRead(notificationId);
        
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/read-all")
    @Operation(summary = "Mark all notifications as read")
    public ResponseEntity<Void> markAllAsRead(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        notificationService.markAllAsRead(userId);
        
        return ResponseEntity.ok().build();
    }
    
    // ========================================
    // ADMIN/SERVICE ENDPOINTS
    // ========================================
    
    @PostMapping("/send")
    @Operation(summary = "Send notification (internal/admin)")
    public ResponseEntity<Void> sendNotification(@Valid @RequestBody SendNotificationRequest request) {
        notificationService.sendNotification(request);
        
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }
    
    @GetMapping("/stats")
    @Operation(summary = "Get delivery statistics")
    public ResponseEntity<DeliveryStats> getStats(
            @RequestParam(defaultValue = "24") int hoursBack) {
        
        DeliveryStats stats = notificationService.getDeliveryStats(hoursBack);
        
        return ResponseEntity.ok(stats);
    }
}
