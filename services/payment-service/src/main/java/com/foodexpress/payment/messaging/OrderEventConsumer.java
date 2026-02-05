package com.foodexpress.payment.messaging;

import com.foodexpress.payment.service.PaymentService;
import com.foodexpress.payment.dto.PaymentDTOs.RefundRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Consumes order events for saga coordination.
 * 
 * Listens to:
 * - ORDER_CANCELLED - Triggers payment refund
 * - ORDER_FAILED - Triggers payment refund
 * - ORDER_CREATED - Validates order has pending payment
 * 
 * This is the compensating transaction handler for the Order Saga.
 */
@Component
public class OrderEventConsumer {
    
    private static final Logger log = LoggerFactory.getLogger(OrderEventConsumer.class);
    
    private final PaymentService paymentService;
    
    public OrderEventConsumer(PaymentService paymentService) {
        this.paymentService = paymentService;
    }
    
    // ========================================
    // ORDER EVENT RECORDS
    // ========================================
    
    /**
     * Order event from Order Service
     */
    public sealed interface OrderEvent permits 
            OrderCreatedEvent, 
            OrderCancelledEvent, 
            OrderFailedEvent {
        
        String eventId();
        String eventType();
        String orderId();
        String customerId();
        Instant timestamp();
        String correlationId();
    }
    
    public record OrderCreatedEvent(
            String eventId,
            String orderId,
            String customerId,
            BigDecimal totalAmount,
            String currency,
            Instant timestamp,
            String correlationId
    ) implements OrderEvent {
        @Override
        public String eventType() {
            return "ORDER_CREATED";
        }
    }
    
    public record OrderCancelledEvent(
            String eventId,
            String orderId,
            String customerId,
            String cancellationReason,
            String cancelledBy, // CUSTOMER, SYSTEM, RESTAURANT
            Instant timestamp,
            String correlationId
    ) implements OrderEvent {
        @Override
        public String eventType() {
            return "ORDER_CANCELLED";
        }
    }
    
    public record OrderFailedEvent(
            String eventId,
            String orderId,
            String customerId,
            String failureReason,
            String failedStep, // Which saga step failed
            Instant timestamp,
            String correlationId
    ) implements OrderEvent {
        @Override
        public String eventType() {
            return "ORDER_FAILED";
        }
    }
    
    // ========================================
    // EVENT HANDLERS
    // ========================================
    
    @KafkaListener(
            topics = "${app.kafka.topics.order-events:order-events}",
            groupId = "${spring.kafka.consumer.group-id:payment-service}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleOrderEvent(
            @Payload String eventJson,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(value = "eventType", required = false) byte[] eventTypeHeader,
            @Header(value = "correlationId", required = false) byte[] correlationIdHeader,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {
        
        String eventType = eventTypeHeader != null ? new String(eventTypeHeader) : "UNKNOWN";
        String correlationId = correlationIdHeader != null 
                ? new String(correlationIdHeader) 
                : "unknown";
        
        // Set MDC for logging context
        try (var ignored = MDC.putCloseable("correlationId", correlationId);
             var ignored2 = MDC.putCloseable("orderId", key)) {
            
            log.info("Received {} event for order {} (partition: {}, offset: {})", 
                    eventType, key, partition, offset);
            
            // Route to appropriate handler based on event type
            // Note: In production, use proper JSON deserialization
            switch (eventType) {
                case "ORDER_CANCELLED" -> handleOrderCancelled(key, eventJson, correlationId);
                case "ORDER_FAILED" -> handleOrderFailed(key, eventJson, correlationId);
                case "ORDER_CREATED" -> handleOrderCreated(key, eventJson, correlationId);
                default -> log.debug("Ignoring event type: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Error processing order event: {}", e.getMessage(), e);
            // In production: Consider dead letter queue
            throw e; // Let Kafka handle retry
        }
    }
    
    /**
     * Handle order cancellation - trigger refund (compensating transaction)
     */
    private void handleOrderCancelled(String orderId, String eventJson, String correlationId) {
        log.info("Processing order cancellation for order {}", orderId);
        
        // Find successful payment for this order
        var paymentOpt = paymentService.getPaymentForOrder(orderId);
        
        if (paymentOpt.isEmpty()) {
            log.warn("No successful payment found for cancelled order {}", orderId);
            return;
        }
        
        var payment = paymentOpt.get();
        
        // Check if already refunded
        if ("REFUNDED".equals(payment.status())) {
            log.info("Payment {} already refunded for order {}", payment.id(), orderId);
            return;
        }
        
        try {
            // Initiate refund
            String idempotencyKey = "order-cancel-refund:" + orderId;
            RefundRequest refundRequest = new RefundRequest(
                    "Order cancelled by customer",
                    null // Full refund
            );
            
            paymentService.refundPayment(payment.id(), refundRequest, idempotencyKey);
            log.info("Refund initiated for payment {} (order {})", payment.id(), orderId);
            
        } catch (Exception e) {
            log.error("Failed to refund payment {} for cancelled order {}: {}", 
                    payment.id(), orderId, e.getMessage());
            // In production: Consider retry or manual intervention
            throw e;
        }
    }
    
    /**
     * Handle order failure - trigger refund (compensating transaction)
     */
    private void handleOrderFailed(String orderId, String eventJson, String correlationId) {
        log.info("Processing order failure for order {}", orderId);
        
        // Similar to cancellation but with different reason
        var paymentOpt = paymentService.getPaymentForOrder(orderId);
        
        if (paymentOpt.isEmpty()) {
            log.debug("No successful payment found for failed order {} - may not have been charged", 
                    orderId);
            return;
        }
        
        var payment = paymentOpt.get();
        
        if ("REFUNDED".equals(payment.status())) {
            log.info("Payment {} already refunded for order {}", payment.id(), orderId);
            return;
        }
        
        try {
            String idempotencyKey = "order-failure-refund:" + orderId;
            RefundRequest refundRequest = new RefundRequest(
                    "Order processing failed",
                    null
            );
            
            paymentService.refundPayment(payment.id(), refundRequest, idempotencyKey);
            log.info("Refund initiated for payment {} (failed order {})", payment.id(), orderId);
            
        } catch (Exception e) {
            log.error("Failed to refund payment {} for failed order {}: {}", 
                    payment.id(), orderId, e.getMessage());
            throw e;
        }
    }
    
    /**
     * Handle order creation - log for audit/debugging
     */
    private void handleOrderCreated(String orderId, String eventJson, String correlationId) {
        log.debug("Order {} created - awaiting payment", orderId);
        // Payment will be initiated by the client, not automatically
    }
}
