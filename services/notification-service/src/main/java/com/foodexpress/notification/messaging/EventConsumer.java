package com.foodexpress.notification.messaging;

import com.foodexpress.notification.domain.*;
import com.foodexpress.notification.dto.NotificationDTOs.SendNotificationRequest;
import com.foodexpress.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Kafka Event Consumer.
 * Listens to various service events and triggers notifications.
 */
@Component
public class EventConsumer {
    
    private static final Logger log = LoggerFactory.getLogger(EventConsumer.class);
    
    private final NotificationService notificationService;
    
    public EventConsumer(NotificationService notificationService) {
        this.notificationService = notificationService;
    }
    
    // ========================================
    // ORDER EVENTS
    // ========================================
    
    @KafkaListener(
            topics = "order-events",
            groupId = "notification-service",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleOrderEvent(
            @Payload Map<String, Object> event,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(value = "eventType", required = false) String eventType,
            Acknowledgment ack) {
        
        try {
            log.info("Received order event: {}", eventType);
            
            String orderId = (String) event.get("orderId");
            String customerId = (String) event.get("customerId");
            String restaurantId = (String) event.get("restaurantId");
            
            NotificationType type = mapOrderEventType(eventType);
            if (type == null) {
                log.debug("Ignoring order event type: {}", eventType);
                ack.acknowledge();
                return;
            }
            
            // Build notification request
            SendNotificationRequest request = new SendNotificationRequest(
                    customerId,
                    type,
                    List.of(NotificationChannel.EMAIL, NotificationChannel.PUSH, NotificationChannel.IN_APP),
                    null, // Use default title
                    null, // Use default message
                    "ORDER",
                    orderId,
                    Map.of(
                            "orderId", orderId,
                            "orderNumber", (String) event.getOrDefault("orderNumber", orderId)
                    )
            );
            
            notificationService.sendNotification(request);
            
            ack.acknowledge();
            
        } catch (Exception e) {
            log.error("Error processing order event: {}", e.getMessage(), e);
            // Don't acknowledge - will be redelivered
        }
    }
    
    // ========================================
    // PAYMENT EVENTS
    // ========================================
    
    @KafkaListener(
            topics = "payment-events",
            groupId = "notification-service",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handlePaymentEvent(
            @Payload Map<String, Object> event,
            @Header(value = "eventType", required = false) String eventType,
            Acknowledgment ack) {
        
        try {
            log.info("Received payment event: {}", eventType);
            
            String paymentId = (String) event.get("paymentId");
            String orderId = (String) event.get("orderId");
            String customerId = (String) event.get("customerId");
            
            NotificationType type = mapPaymentEventType(eventType);
            if (type == null) {
                ack.acknowledge();
                return;
            }
            
            SendNotificationRequest request = new SendNotificationRequest(
                    customerId,
                    type,
                    List.of(NotificationChannel.EMAIL, NotificationChannel.PUSH, NotificationChannel.IN_APP),
                    null,
                    null,
                    "PAYMENT",
                    paymentId,
                    Map.of(
                            "paymentId", paymentId,
                            "orderId", orderId != null ? orderId : "",
                            "amount", String.valueOf(event.getOrDefault("amount", ""))
                    )
            );
            
            notificationService.sendNotification(request);
            
            ack.acknowledge();
            
        } catch (Exception e) {
            log.error("Error processing payment event: {}", e.getMessage(), e);
        }
    }
    
    // ========================================
    // USER EVENTS
    // ========================================
    
    @KafkaListener(
            topics = "user-events",
            groupId = "notification-service",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleUserEvent(
            @Payload Map<String, Object> event,
            @Header(value = "eventType", required = false) String eventType,
            Acknowledgment ack) {
        
        try {
            log.info("Received user event: {}", eventType);
            
            String userId = (String) event.get("userId");
            
            NotificationType type = mapUserEventType(eventType);
            if (type == null) {
                ack.acknowledge();
                return;
            }
            
            List<NotificationChannel> channels = type == NotificationType.EMAIL_VERIFICATION
                    ? List.of(NotificationChannel.EMAIL)
                    : List.of(NotificationChannel.EMAIL, NotificationChannel.IN_APP);
            
            SendNotificationRequest request = new SendNotificationRequest(
                    userId,
                    type,
                    channels,
                    null,
                    null,
                    "USER",
                    userId,
                    Map.of("userId", userId)
            );
            
            notificationService.sendNotification(request);
            
            ack.acknowledge();
            
        } catch (Exception e) {
            log.error("Error processing user event: {}", e.getMessage(), e);
        }
    }
    
    // ========================================
    // MAPPERS
    // ========================================
    
    private NotificationType mapOrderEventType(String eventType) {
        if (eventType == null) return null;
        
        return switch (eventType.toUpperCase()) {
            case "ORDER_CREATED" -> NotificationType.ORDER_PLACED;
            case "ORDER_CONFIRMED" -> NotificationType.ORDER_CONFIRMED;
            case "ORDER_PREPARING" -> NotificationType.ORDER_PREPARING;
            case "ORDER_READY" -> NotificationType.ORDER_READY;
            case "ORDER_OUT_FOR_DELIVERY" -> NotificationType.ORDER_OUT_FOR_DELIVERY;
            case "ORDER_DELIVERED" -> NotificationType.ORDER_DELIVERED;
            case "ORDER_CANCELLED" -> NotificationType.ORDER_CANCELLED;
            default -> null;
        };
    }
    
    private NotificationType mapPaymentEventType(String eventType) {
        if (eventType == null) return null;
        
        return switch (eventType.toUpperCase()) {
            case "PAYMENT_COMPLETED" -> NotificationType.PAYMENT_SUCCESS;
            case "PAYMENT_FAILED" -> NotificationType.PAYMENT_FAILED;
            case "REFUND_INITIATED" -> NotificationType.REFUND_INITIATED;
            case "REFUND_COMPLETED" -> NotificationType.REFUND_COMPLETED;
            default -> null;
        };
    }
    
    private NotificationType mapUserEventType(String eventType) {
        if (eventType == null) return null;
        
        return switch (eventType.toUpperCase()) {
            case "USER_REGISTERED" -> NotificationType.WELCOME;
            case "EMAIL_VERIFICATION_REQUESTED" -> NotificationType.EMAIL_VERIFICATION;
            case "PASSWORD_RESET_REQUESTED" -> NotificationType.PASSWORD_RESET;
            case "ACCOUNT_SUSPENDED" -> NotificationType.ACCOUNT_SUSPENDED;
            default -> null;
        };
    }
}
