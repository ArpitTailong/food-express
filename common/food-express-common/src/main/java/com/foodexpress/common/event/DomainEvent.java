package com.foodexpress.common.event;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Base Event interface using Java 21 Sealed Types.
 * All domain events must implement this interface.
 * 
 * Events are immutable records that represent facts about what happened.
 */
public sealed interface DomainEvent permits
        DomainEvent.OrderEvent,
        DomainEvent.PaymentEvent,
        DomainEvent.UserEvent,
        DomainEvent.NotificationEvent,
        DomainEvent.AuditEvent {
    
    /**
     * Unique identifier for this event
     */
    String eventId();
    
    /**
     * Type of the event (e.g., "ORDER_CREATED", "PAYMENT_SUCCESS")
     */
    String eventType();
    
    /**
     * When the event occurred
     */
    Instant occurredAt();
    
    /**
     * Aggregate ID (the main entity this event belongs to)
     */
    String aggregateId();
    
    /**
     * Version for optimistic locking / event ordering
     */
    long version();
    
    /**
     * Correlation ID for distributed tracing
     */
    String correlationId();
    
    /**
     * Source service that generated this event
     */
    String source();
    
    // ========================================
    // ORDER EVENTS
    // ========================================
    
    sealed interface OrderEvent extends DomainEvent permits
            OrderCreatedEvent,
            OrderConfirmedEvent,
            OrderCancelledEvent,
            OrderStatusChangedEvent {
    }
    
    record OrderCreatedEvent(
            String eventId,
            Instant occurredAt,
            String aggregateId,
            long version,
            String correlationId,
            String source,
            // Order-specific data
            String customerId,
            String restaurantId,
            java.math.BigDecimal totalAmount,
            String currency,
            java.util.List<OrderItem> items,
            DeliveryAddress deliveryAddress
    ) implements OrderEvent {
        
        public OrderCreatedEvent {
            if (eventId == null) eventId = UUID.randomUUID().toString();
            if (occurredAt == null) occurredAt = Instant.now();
            if (source == null) source = "order-service";
        }
        
        @Override
        public String eventType() { return "ORDER_CREATED"; }
        
        public record OrderItem(String menuItemId, String name, int quantity, java.math.BigDecimal price) {}
        public record DeliveryAddress(String street, String city, String zipCode, String coordinates) {}
    }
    
    record OrderConfirmedEvent(
            String eventId,
            Instant occurredAt,
            String aggregateId,
            long version,
            String correlationId,
            String source,
            // Confirmation data
            String paymentId,
            Instant estimatedDeliveryTime
    ) implements OrderEvent {
        
        public OrderConfirmedEvent {
            if (eventId == null) eventId = UUID.randomUUID().toString();
            if (occurredAt == null) occurredAt = Instant.now();
            if (source == null) source = "order-service";
        }
        
        @Override
        public String eventType() { return "ORDER_CONFIRMED"; }
    }
    
    record OrderCancelledEvent(
            String eventId,
            Instant occurredAt,
            String aggregateId,
            long version,
            String correlationId,
            String source,
            // Cancellation data
            String reason,
            String cancelledBy,
            boolean refundRequired
    ) implements OrderEvent {
        
        @Override
        public String eventType() { return "ORDER_CANCELLED"; }
    }
    
    record OrderStatusChangedEvent(
            String eventId,
            Instant occurredAt,
            String aggregateId,
            long version,
            String correlationId,
            String source,
            // Status change data
            String previousStatus,
            String newStatus,
            Map<String, Object> metadata
    ) implements OrderEvent {
        
        @Override
        public String eventType() { return "ORDER_STATUS_CHANGED"; }
    }
    
    // ========================================
    // PAYMENT EVENTS
    // ========================================
    
    sealed interface PaymentEvent extends DomainEvent permits
            PaymentInitiatedEvent,
            PaymentSuccessEvent,
            PaymentFailedEvent,
            PaymentRefundedEvent {
    }
    
    record PaymentInitiatedEvent(
            String eventId,
            Instant occurredAt,
            String aggregateId,
            long version,
            String correlationId,
            String source,
            // Payment data
            String orderId,
            String customerId,
            java.math.BigDecimal amount,
            String currency,
            String paymentMethod,
            String idempotencyKey
    ) implements PaymentEvent {
        
        public PaymentInitiatedEvent {
            if (eventId == null) eventId = UUID.randomUUID().toString();
            if (occurredAt == null) occurredAt = Instant.now();
            if (source == null) source = "payment-service";
        }
        
        @Override
        public String eventType() { return "PAYMENT_INITIATED"; }
    }
    
    record PaymentSuccessEvent(
            String eventId,
            Instant occurredAt,
            String aggregateId,
            long version,
            String correlationId,
            String source,
            // Success data
            String orderId,
            String gatewayTransactionId,
            java.math.BigDecimal amountCharged,
            String currency,
            String receiptUrl
    ) implements PaymentEvent {
        
        @Override
        public String eventType() { return "PAYMENT_SUCCESS"; }
    }
    
    record PaymentFailedEvent(
            String eventId,
            Instant occurredAt,
            String aggregateId,
            long version,
            String correlationId,
            String source,
            // Failure data
            String orderId,
            String errorCode,
            String errorMessage,
            int attemptNumber,
            boolean retryable
    ) implements PaymentEvent {
        
        @Override
        public String eventType() { return "PAYMENT_FAILED"; }
    }
    
    record PaymentRefundedEvent(
            String eventId,
            Instant occurredAt,
            String aggregateId,
            long version,
            String correlationId,
            String source,
            // Refund data
            String orderId,
            String refundId,
            java.math.BigDecimal refundAmount,
            String reason
    ) implements PaymentEvent {
        
        @Override
        public String eventType() { return "PAYMENT_REFUNDED"; }
    }
    
    // ========================================
    // USER EVENTS
    // ========================================
    
    sealed interface UserEvent extends DomainEvent permits
            UserRegisteredEvent,
            UserUpdatedEvent {
    }
    
    record UserRegisteredEvent(
            String eventId,
            Instant occurredAt,
            String aggregateId,
            long version,
            String correlationId,
            String source,
            // User data
            String email,
            String firstName,
            String lastName,
            String role
    ) implements UserEvent {
        
        @Override
        public String eventType() { return "USER_REGISTERED"; }
    }
    
    record UserUpdatedEvent(
            String eventId,
            Instant occurredAt,
            String aggregateId,
            long version,
            String correlationId,
            String source,
            // Update data
            Map<String, Object> changedFields
    ) implements UserEvent {
        
        @Override
        public String eventType() { return "USER_UPDATED"; }
    }
    
    // ========================================
    // NOTIFICATION EVENTS
    // ========================================
    
    sealed interface NotificationEvent extends DomainEvent permits
            NotificationRequestedEvent,
            NotificationSentEvent {
    }
    
    record NotificationRequestedEvent(
            String eventId,
            Instant occurredAt,
            String aggregateId,
            long version,
            String correlationId,
            String source,
            // Notification data
            String userId,
            String channel,  // EMAIL, SMS, PUSH
            String templateId,
            Map<String, String> templateData,
            int priority
    ) implements NotificationEvent {
        
        @Override
        public String eventType() { return "NOTIFICATION_REQUESTED"; }
    }
    
    record NotificationSentEvent(
            String eventId,
            Instant occurredAt,
            String aggregateId,
            long version,
            String correlationId,
            String source,
            // Sent confirmation
            String channel,
            boolean success,
            String externalId
    ) implements NotificationEvent {
        
        @Override
        public String eventType() { return "NOTIFICATION_SENT"; }
    }
    
    // ========================================
    // AUDIT EVENTS
    // ========================================
    
    sealed interface AuditEvent extends DomainEvent permits
            AuditLogEvent {
    }
    
    record AuditLogEvent(
            String eventId,
            Instant occurredAt,
            String aggregateId,
            long version,
            String correlationId,
            String source,
            // Audit data
            String action,
            String actorId,
            String actorType,
            String resourceType,
            String resourceId,
            Map<String, Object> oldValue,
            Map<String, Object> newValue,
            String ipAddress,
            String userAgent
    ) implements AuditEvent {
        
        @Override
        public String eventType() { return "AUDIT_LOG"; }
    }
}
