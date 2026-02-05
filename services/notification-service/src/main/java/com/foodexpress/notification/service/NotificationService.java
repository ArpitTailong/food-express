package com.foodexpress.notification.service;

import com.foodexpress.notification.domain.*;
import com.foodexpress.notification.dto.NotificationDTOs;
import com.foodexpress.notification.dto.NotificationDTOs.*;
import com.foodexpress.notification.repository.NotificationRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Notification Service.
 * Handles sending notifications across all channels.
 */
@Service
public class NotificationService {
    
    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    
    private final NotificationRepository notificationRepository;
    private final EmailService emailService;
    private final SmsService smsService;
    private final PushService pushService;
    private final UserPreferenceService preferenceService;
    
    // Metrics
    private final Counter emailsSent;
    private final Counter smsSent;
    private final Counter pushSent;
    private final Counter notificationsFailed;
    
    public NotificationService(
            NotificationRepository notificationRepository,
            EmailService emailService,
            SmsService smsService,
            PushService pushService,
            UserPreferenceService preferenceService,
            MeterRegistry meterRegistry) {
        
        this.notificationRepository = notificationRepository;
        this.emailService = emailService;
        this.smsService = smsService;
        this.pushService = pushService;
        this.preferenceService = preferenceService;
        
        this.emailsSent = Counter.builder("notifications.sent").tag("channel", "email").register(meterRegistry);
        this.smsSent = Counter.builder("notifications.sent").tag("channel", "sms").register(meterRegistry);
        this.pushSent = Counter.builder("notifications.sent").tag("channel", "push").register(meterRegistry);
        this.notificationsFailed = Counter.builder("notifications.failed").register(meterRegistry);
    }
    
    // ========================================
    // SEND NOTIFICATIONS
    // ========================================
    
    @Async
    @Transactional
    public void sendNotification(SendNotificationRequest request) {
        log.info("Sending notification to user {} via {}", request.userId(), request.channels());
        
        // Get user preferences and contact info
        UserPreferenceService.UserContactInfo contactInfo = preferenceService.getUserContactInfo(request.userId());
        UserPreferenceService.NotificationPreferences prefs = preferenceService.getPreferences(request.userId());
        
        // Determine actual message
        String title = request.title() != null ? request.title() : request.type().getTitle();
        String message = request.message() != null ? request.message() : request.type().getDefaultMessage();
        
        // Apply template data if provided
        if (request.templateData() != null) {
            for (Map.Entry<String, String> entry : request.templateData().entrySet()) {
                message = message.replace("{" + entry.getKey() + "}", entry.getValue());
                title = title.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }
        
        // Send to each requested channel
        for (NotificationChannel channel : request.channels()) {
            if (!shouldSendToChannel(channel, prefs, request.type())) {
                log.debug("Skipping {} channel for user {} due to preferences", channel, request.userId());
                continue;
            }
            
            String recipient = getRecipientForChannel(channel, contactInfo);
            if (recipient == null) {
                log.warn("No recipient for {} channel for user {}", channel, request.userId());
                continue;
            }
            
            Notification notification = new Notification(
                    request.userId(),
                    request.type(),
                    channel,
                    title,
                    message,
                    recipient
            );
            
            if (request.referenceType() != null) {
                notification.setReference(request.referenceType(), request.referenceId());
            }
            
            notificationRepository.save(notification);
            
            // Send asynchronously
            sendViaChannel(notification);
        }
    }
    
    @RateLimiter(name = "emailSending")
    @CircuitBreaker(name = "emailService", fallbackMethod = "emailFallback")
    private void sendViaChannel(Notification notification) {
        try {
            String externalId = switch (notification.getChannel()) {
                case EMAIL -> emailService.send(
                        notification.getRecipient(),
                        notification.getTitle(),
                        notification.getMessage(),
                        notification.getType()
                );
                case SMS -> smsService.send(
                        notification.getRecipient(),
                        notification.getMessage()
                );
                case PUSH -> pushService.send(
                        notification.getRecipient(),
                        notification.getTitle(),
                        notification.getMessage()
                );
                case IN_APP -> {
                    // In-app notifications don't need external sending
                    notification.markSent(null);
                    notification.markDelivered();
                    yield null;
                }
            };
            
            if (notification.getChannel() != NotificationChannel.IN_APP) {
                notification.markSent(externalId);
                incrementChannelCounter(notification.getChannel());
            }
            
            notificationRepository.save(notification);
            log.info("Notification {} sent successfully via {}", notification.getId(), notification.getChannel());
            
        } catch (Exception e) {
            log.error("Failed to send notification {} via {}: {}",
                    notification.getId(), notification.getChannel(), e.getMessage());
            notification.markFailed(e.getMessage());
            notificationRepository.save(notification);
            notificationsFailed.increment();
        }
    }
    
    private void emailFallback(Notification notification, Throwable t) {
        log.warn("Email service unavailable, queueing for retry: {}", t.getMessage());
        notification.markFailed("Service unavailable: " + t.getMessage());
        notificationRepository.save(notification);
    }
    
    // ========================================
    // RETRY MECHANISM
    // ========================================
    
    @Scheduled(fixedDelayString = "${notification.retry.interval:60000}")
    @Transactional
    public void processRetryQueue() {
        List<Notification> toRetry = notificationRepository.findNotificationsToRetry(LocalDateTime.now());
        
        if (!toRetry.isEmpty()) {
            log.info("Processing {} notifications for retry", toRetry.size());
            toRetry.forEach(this::sendViaChannel);
        }
    }
    
    // ========================================
    // IN-APP NOTIFICATIONS
    // ========================================
    
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getInAppNotifications(String userId, Pageable pageable) {
        return notificationRepository.findInAppByUserId(userId, pageable)
                .map(NotificationDTOs::toResponse);
    }
    
    @Transactional(readOnly = true)
    public NotificationSummary getNotificationSummary(String userId) {
        List<Notification> unread = notificationRepository.findUnreadByUserId(userId);
        Page<Notification> recent = notificationRepository.findInAppByUserId(userId, PageRequest.of(0, 10));
        
        return new NotificationSummary(
                recent.getTotalElements(),
                unread.size(),
                recent.getContent().stream().map(NotificationDTOs::toResponse).toList()
        );
    }
    
    @Transactional
    public void markAsRead(String notificationId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            n.markRead();
            notificationRepository.save(n);
        });
    }
    
    @Transactional
    public int markAllAsRead(String userId) {
        return notificationRepository.markAllAsRead(userId, LocalDateTime.now());
    }
    
    // ========================================
    // STATS
    // ========================================
    
    @Transactional(readOnly = true)
    public DeliveryStats getDeliveryStats(int hoursBack) {
        LocalDateTime since = LocalDateTime.now().minusHours(hoursBack);
        
        long delivered = notificationRepository.countByStatusSince(NotificationStatus.DELIVERED, since);
        long failed = notificationRepository.countByStatusSince(NotificationStatus.FAILED, since);
        long pending = notificationRepository.countByStatusSince(NotificationStatus.PENDING, since);
        long sent = notificationRepository.countByStatusSince(NotificationStatus.SENT, since);
        
        long total = delivered + failed + pending + sent;
        double rate = total > 0 ? (double) delivered / total * 100 : 0;
        
        return new DeliveryStats(total, delivered, failed, pending, rate);
    }
    
    // ========================================
    // CLEANUP
    // ========================================
    
    @Scheduled(cron = "${notification.cleanup.cron:0 0 3 * * ?}") // 3 AM daily
    @Transactional
    public void cleanupOldNotifications() {
        LocalDateTime before = LocalDateTime.now().minusDays(90);
        int deleted = notificationRepository.deleteOldNotifications(before);
        log.info("Cleaned up {} old notifications", deleted);
    }
    
    // ========================================
    // HELPER METHODS
    // ========================================
    
    private boolean shouldSendToChannel(NotificationChannel channel,
                                       UserPreferenceService.NotificationPreferences prefs,
                                       NotificationType type) {
        // Promotional notifications respect user preferences strictly
        if (type == NotificationType.PROMOTIONAL) {
            return switch (channel) {
                case EMAIL -> prefs.promotionalEmails();
                case SMS -> prefs.smsNotifications() && prefs.promotionalEmails();
                case PUSH -> prefs.pushNotifications() && prefs.promotionalEmails();
                case IN_APP -> true;
            };
        }
        
        // Transactional notifications use basic preferences
        return switch (channel) {
            case EMAIL -> prefs.emailNotifications();
            case SMS -> prefs.smsNotifications();
            case PUSH -> prefs.pushNotifications();
            case IN_APP -> true;
        };
    }
    
    private String getRecipientForChannel(NotificationChannel channel,
                                         UserPreferenceService.UserContactInfo contactInfo) {
        return switch (channel) {
            case EMAIL -> contactInfo.email();
            case SMS -> contactInfo.phoneNumber();
            case PUSH -> contactInfo.deviceToken();
            case IN_APP -> contactInfo.userId();
        };
    }
    
    private void incrementChannelCounter(NotificationChannel channel) {
        switch (channel) {
            case EMAIL -> emailsSent.increment();
            case SMS -> smsSent.increment();
            case PUSH -> pushSent.increment();
            default -> {}
        }
    }
}
