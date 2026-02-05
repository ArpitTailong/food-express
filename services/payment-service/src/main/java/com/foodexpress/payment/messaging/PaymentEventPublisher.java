package com.foodexpress.payment.messaging;

import com.foodexpress.payment.domain.Payment;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Publishes payment events to Kafka for saga coordination.
 * 
 * Events published:
 * - payment.created - When payment is initiated
 * - payment.completed - When payment succeeds
 * - payment.failed - When payment fails
 * - payment.refunded - When payment is refunded
 * 
 * These events are consumed by:
 * - Order Service (to update order status)
 * - Notification Service (to send payment receipts)
 * - Analytics Service (for revenue tracking)
 * 
 * Note: This bean is only active when NOT running with 'local' profile.
 */
@Component
@Profile("!local")
public class PaymentEventPublisher implements PaymentEventPublisherInterface {
    
    private static final Logger log = LoggerFactory.getLogger(PaymentEventPublisher.class);
    
    private final KafkaTemplate<String, PaymentEvent> kafkaTemplate;
    
    @Value("${app.kafka.topics.payment-events:payment-events}")
    private String paymentEventsTopic;
    
    public PaymentEventPublisher(KafkaTemplate<String, PaymentEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }
    
    // ========================================
    // EVENT TYPES
    // ========================================
    
    /**
     * Base payment event (sealed hierarchy)
     */
    public sealed interface PaymentEvent permits 
            PaymentCreatedEvent, 
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
    
    public record PaymentCreatedEvent(
            String eventId,
            String paymentId,
            String orderId,
            String customerId,
            BigDecimal amount,
            String currency,
            String paymentMethod,
            Instant timestamp,
            String correlationId
    ) implements PaymentEvent {
        @Override
        public String eventType() {
            return "PAYMENT_CREATED";
        }
    }
    
    public record PaymentCompletedEvent(
            String eventId,
            String paymentId,
            String orderId,
            String customerId,
            BigDecimal amount,
            String currency,
            String gatewayTransactionId,
            String cardLastFour,
            String cardBrand,
            Instant timestamp,
            String correlationId
    ) implements PaymentEvent {
        @Override
        public String eventType() {
            return "PAYMENT_COMPLETED";
        }
    }
    
    public record PaymentFailedEvent(
            String eventId,
            String paymentId,
            String orderId,
            String customerId,
            BigDecimal amount,
            String currency,
            String errorCode,
            String errorMessage,
            int attemptCount,
            boolean canRetry,
            Instant timestamp,
            String correlationId
    ) implements PaymentEvent {
        @Override
        public String eventType() {
            return "PAYMENT_FAILED";
        }
    }
    
    public record PaymentRefundedEvent(
            String eventId,
            String paymentId,
            String orderId,
            String customerId,
            BigDecimal originalAmount,
            BigDecimal refundAmount,
            String refundId,
            String refundReason,
            Instant timestamp,
            String correlationId
    ) implements PaymentEvent {
        @Override
        public String eventType() {
            return "PAYMENT_REFUNDED";
        }
    }
    
    // ========================================
    // PUBLISH METHODS
    // ========================================
    
    public void publishPaymentCreated(Payment payment) {
        PaymentCreatedEvent event = new PaymentCreatedEvent(
                generateEventId(),
                payment.getId(),
                payment.getOrderId(),
                payment.getCustomerId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getPaymentMethod(),
                Instant.now(),
                payment.getCorrelationId()
        );
        
        publish(event, payment.getOrderId());
    }
    
    public void publishPaymentCompleted(Payment payment) {
        PaymentCompletedEvent event = new PaymentCompletedEvent(
                generateEventId(),
                payment.getId(),
                payment.getOrderId(),
                payment.getCustomerId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getGatewayTransactionId(),
                payment.getCardLastFour(),
                payment.getCardBrand(),
                Instant.now(),
                payment.getCorrelationId()
        );
        
        publish(event, payment.getOrderId());
    }
    
    public void publishPaymentFailed(Payment payment) {
        boolean canRetry = payment.getAttemptCount() < 3;
        
        PaymentFailedEvent event = new PaymentFailedEvent(
                generateEventId(),
                payment.getId(),
                payment.getOrderId(),
                payment.getCustomerId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getErrorCode(),
                payment.getErrorMessage(),
                payment.getAttemptCount(),
                canRetry,
                Instant.now(),
                payment.getCorrelationId()
        );
        
        publish(event, payment.getOrderId());
    }
    
    public void publishPaymentRefunded(Payment payment) {
        PaymentRefundedEvent event = new PaymentRefundedEvent(
                generateEventId(),
                payment.getId(),
                payment.getOrderId(),
                payment.getCustomerId(),
                payment.getAmount(),
                payment.getRefundAmount(),
                payment.getRefundId(),
                payment.getRefundReason(),
                Instant.now(),
                payment.getCorrelationId()
        );
        
        publish(event, payment.getOrderId());
    }
    
    // ========================================
    // INTERNAL METHODS
    // ========================================
    
    private void publish(PaymentEvent event, String key) {
        log.info("Publishing {} event for payment {} (order: {})", 
                event.eventType(), event.paymentId(), event.orderId());
        
        ProducerRecord<String, PaymentEvent> record = new ProducerRecord<>(
                paymentEventsTopic,
                key,  // Partition by order ID for ordering guarantee
                event
        );
        
        // Add headers for tracing
        record.headers()
                .add("eventType", event.eventType().getBytes())
                .add("correlationId", event.correlationId() != null 
                        ? event.correlationId().getBytes() 
                        : "unknown".getBytes())
                .add("timestamp", event.timestamp().toString().getBytes());
        
        CompletableFuture<SendResult<String, PaymentEvent>> future = kafkaTemplate.send(record);
        
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.debug("Event {} published successfully to partition {} offset {}", 
                        event.eventId(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                log.error("Failed to publish event {}: {}", event.eventId(), ex.getMessage(), ex);
                // In production: Consider dead letter queue or retry mechanism
            }
        });
    }
    
    private String generateEventId() {
        return UUID.randomUUID().toString();
    }
}
