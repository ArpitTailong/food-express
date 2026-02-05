package com.foodexpress.order.messaging;

import com.foodexpress.order.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Consumes payment events for saga coordination.
 * 
 * Handles:
 * - PAYMENT_COMPLETED → Confirm order
 * - PAYMENT_FAILED → Fail order
 * - PAYMENT_REFUNDED → Update order status
 */
@Component
public class PaymentEventConsumer {
    
    private static final Logger log = LoggerFactory.getLogger(PaymentEventConsumer.class);
    
    private final OrderService orderService;
    
    public PaymentEventConsumer(OrderService orderService) {
        this.orderService = orderService;
    }
    
    // ========================================
    // PAYMENT EVENT RECORDS
    // ========================================
    
    public sealed interface PaymentEvent permits 
            PaymentCompletedEvent, 
            PaymentFailedEvent, 
            PaymentRefundedEvent {
        
        String eventId();
        String eventType();
        String paymentId();
        String orderId();
        String customerId();
        Instant timestamp();
        String correlationId();
    }
    
    public record PaymentCompletedEvent(
            String eventId,
            String paymentId,
            String orderId,
            String customerId,
            java.math.BigDecimal amount,
            String currency,
            String gatewayTransactionId,
            Instant timestamp,
            String correlationId
    ) implements PaymentEvent {
        @Override
        public String eventType() { return "PAYMENT_COMPLETED"; }
    }
    
    public record PaymentFailedEvent(
            String eventId,
            String paymentId,
            String orderId,
            String customerId,
            String errorCode,
            String errorMessage,
            int attemptCount,
            boolean canRetry,
            Instant timestamp,
            String correlationId
    ) implements PaymentEvent {
        @Override
        public String eventType() { return "PAYMENT_FAILED"; }
    }
    
    public record PaymentRefundedEvent(
            String eventId,
            String paymentId,
            String orderId,
            String customerId,
            java.math.BigDecimal refundAmount,
            String refundReason,
            Instant timestamp,
            String correlationId
    ) implements PaymentEvent {
        @Override
        public String eventType() { return "PAYMENT_REFUNDED"; }
    }
    
    // ========================================
    // EVENT HANDLER
    // ========================================
    
    @KafkaListener(
            topics = "${app.kafka.topics.payment-events:payment-events}",
            groupId = "${spring.kafka.consumer.group-id:order-service}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handlePaymentEvent(
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
        
        try (var ignored = MDC.putCloseable("correlationId", correlationId);
             var ignored2 = MDC.putCloseable("orderId", key)) {
            
            log.info("Received {} event for order {} (partition: {}, offset: {})",
                    eventType, key, partition, offset);
            
            switch (eventType) {
                case "PAYMENT_COMPLETED" -> handlePaymentCompleted(key, eventJson, correlationId);
                case "PAYMENT_FAILED" -> handlePaymentFailed(key, eventJson, correlationId);
                case "PAYMENT_REFUNDED" -> handlePaymentRefunded(key, eventJson, correlationId);
                default -> log.debug("Ignoring event type: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Error processing payment event: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    // ========================================
    // HANDLERS
    // ========================================
    
    private void handlePaymentCompleted(String orderId, String eventJson, String correlationId) {
        log.info("Processing payment completed for order {}", orderId);
        
        try {
            // Extract paymentId from JSON (simplified - in production use proper deserialization)
            String paymentId = extractField(eventJson, "paymentId");
            
            orderService.confirmOrderAfterPayment(orderId, paymentId);
            log.info("Order {} confirmed after payment {}", orderId, paymentId);
            
        } catch (Exception e) {
            log.error("Failed to confirm order {} after payment: {}", orderId, e.getMessage(), e);
            // The order will remain in PAYMENT_PENDING - scheduled job can handle cleanup
        }
    }
    
    private void handlePaymentFailed(String orderId, String eventJson, String correlationId) {
        log.info("Processing payment failed for order {}", orderId);
        
        try {
            String errorMessage = extractField(eventJson, "errorMessage");
            String canRetryStr = extractField(eventJson, "canRetry");
            boolean canRetry = "true".equalsIgnoreCase(canRetryStr);
            
            if (canRetry) {
                log.info("Payment for order {} failed but can retry. Order remains in PAYMENT_PENDING", orderId);
                // Don't fail the order yet - customer can retry payment
            } else {
                orderService.handlePaymentFailure(orderId, errorMessage);
                log.warn("Order {} marked as FAILED due to payment failure", orderId);
            }
            
        } catch (Exception e) {
            log.error("Failed to handle payment failure for order {}: {}", orderId, e.getMessage(), e);
        }
    }
    
    private void handlePaymentRefunded(String orderId, String eventJson, String correlationId) {
        log.info("Processing payment refunded for order {}", orderId);
        
        try {
            // Update order to reflect refund
            orderService.handlePaymentRefunded(orderId);
            log.info("Order {} updated after refund", orderId);
            
        } catch (Exception e) {
            log.error("Failed to handle refund for order {}: {}", orderId, e.getMessage(), e);
        }
    }
    
    // Simple JSON field extraction (in production, use Jackson ObjectMapper)
    private String extractField(String json, String fieldName) {
        String searchPattern = "\"" + fieldName + "\":\"";
        int start = json.indexOf(searchPattern);
        if (start < 0) {
            // Try without quotes for boolean/number
            searchPattern = "\"" + fieldName + "\":";
            start = json.indexOf(searchPattern);
            if (start < 0) return null;
            start += searchPattern.length();
            int end = json.indexOf(",", start);
            if (end < 0) end = json.indexOf("}", start);
            return json.substring(start, end).trim();
        }
        start += searchPattern.length();
        int end = json.indexOf("\"", start);
        return json.substring(start, end);
    }
}
