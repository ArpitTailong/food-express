package com.foodexpress.order.messaging;

import com.foodexpress.order.domain.Order;
import com.foodexpress.order.domain.OrderItem;
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
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Publishes order events to Kafka for saga coordination.
 * 
 * Events are consumed by:
 * - Payment Service (for payment processing and refunds)
 * - Restaurant Service (for order preparation)
 * - Delivery Service (for driver assignment)
 * - Notification Service (for customer updates)
 * - Analytics Service (for metrics)
 * 
 * Note: This bean is only active when NOT running with 'local' profile.
 * For local testing, use NoOpOrderEventPublisher instead.
 */
@Component
@Profile("!local")
public class OrderEventPublisher implements OrderEventPublisherInterface {
    
    private static final Logger log = LoggerFactory.getLogger(OrderEventPublisher.class);
    
    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;
    
    @Value("${app.kafka.topics.order-events:order-events}")
    private String orderEventsTopic;
    
    public OrderEventPublisher(KafkaTemplate<String, OrderEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }
    
    // ========================================
    // EVENT TYPES (Sealed Hierarchy)
    // ========================================
    
    public sealed interface OrderEvent permits
            OrderCreatedEvent,
            OrderPaymentInitiatedEvent,
            OrderConfirmedEvent,
            OrderPreparingEvent,
            OrderReadyEvent,
            OrderOutForDeliveryEvent,
            OrderDeliveredEvent,
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
            String restaurantId,
            List<OrderItemInfo> items,
            BigDecimal totalAmount,
            String currency,
            DeliveryInfo deliveryInfo,
            Instant timestamp,
            String correlationId
    ) implements OrderEvent {
        @Override
        public String eventType() { return "ORDER_CREATED"; }
    }
    
    public record OrderPaymentInitiatedEvent(
            String eventId,
            String orderId,
            String customerId,
            BigDecimal amount,
            String currency,
            String paymentMethod,
            Instant timestamp,
            String correlationId
    ) implements OrderEvent {
        @Override
        public String eventType() { return "ORDER_PAYMENT_INITIATED"; }
    }
    
    public record OrderConfirmedEvent(
            String eventId,
            String orderId,
            String customerId,
            String restaurantId,
            String paymentId,
            List<OrderItemInfo> items,
            BigDecimal totalAmount,
            String currency,
            DeliveryInfo deliveryInfo,
            String deliveryInstructions,
            Instant timestamp,
            String correlationId
    ) implements OrderEvent {
        @Override
        public String eventType() { return "ORDER_CONFIRMED"; }
    }
    
    public record OrderPreparingEvent(
            String eventId,
            String orderId,
            String customerId,
            String restaurantId,
            Instant preparingStartedAt,
            Instant timestamp,
            String correlationId
    ) implements OrderEvent {
        @Override
        public String eventType() { return "ORDER_PREPARING"; }
    }
    
    public record OrderReadyEvent(
            String eventId,
            String orderId,
            String customerId,
            String restaurantId,
            DeliveryInfo deliveryInfo,
            Instant timestamp,
            String correlationId
    ) implements OrderEvent {
        @Override
        public String eventType() { return "ORDER_READY"; }
    }
    
    public record OrderOutForDeliveryEvent(
            String eventId,
            String orderId,
            String customerId,
            String driverId,
            DeliveryInfo deliveryInfo,
            Instant timestamp,
            String correlationId
    ) implements OrderEvent {
        @Override
        public String eventType() { return "ORDER_OUT_FOR_DELIVERY"; }
    }
    
    public record OrderDeliveredEvent(
            String eventId,
            String orderId,
            String customerId,
            String restaurantId,
            String driverId,
            BigDecimal totalAmount,
            Instant deliveredAt,
            Instant timestamp,
            String correlationId
    ) implements OrderEvent {
        @Override
        public String eventType() { return "ORDER_DELIVERED"; }
    }
    
    public record OrderCancelledEvent(
            String eventId,
            String orderId,
            String customerId,
            String restaurantId,
            String cancellationReason,
            String cancelledBy,
            boolean requiresRefund,
            String paymentId,
            Instant timestamp,
            String correlationId
    ) implements OrderEvent {
        @Override
        public String eventType() { return "ORDER_CANCELLED"; }
    }
    
    public record OrderFailedEvent(
            String eventId,
            String orderId,
            String customerId,
            String restaurantId,
            String failureReason,
            String failedStep,
            boolean requiresRefund,
            String paymentId,
            Instant timestamp,
            String correlationId
    ) implements OrderEvent {
        @Override
        public String eventType() { return "ORDER_FAILED"; }
    }
    
    // Supporting DTOs
    public record OrderItemInfo(
            String menuItemId,
            String menuItemName,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal totalPrice
    ) {}
    
    public record DeliveryInfo(
            String addressLine1,
            String city,
            String postalCode,
            Double latitude,
            Double longitude,
            String contactName,
            String contactPhone
    ) {}
    
    // ========================================
    // PUBLISH METHODS
    // ========================================
    
    public void publishOrderCreated(Order order) {
        OrderCreatedEvent event = new OrderCreatedEvent(
                generateEventId(),
                order.getId(),
                order.getCustomerId(),
                order.getRestaurantId(),
                toItemInfoList(order.getItems()),
                order.getTotalAmount(),
                order.getCurrency(),
                toDeliveryInfo(order),
                Instant.now(),
                order.getCorrelationId()
        );
        publish(event, order.getId());
    }
    
    public void publishPaymentInitiated(Order order) {
        OrderPaymentInitiatedEvent event = new OrderPaymentInitiatedEvent(
                generateEventId(),
                order.getId(),
                order.getCustomerId(),
                order.getTotalAmount(),
                order.getCurrency(),
                order.getPaymentMethod(),
                Instant.now(),
                order.getCorrelationId()
        );
        publish(event, order.getId());
    }
    
    public void publishOrderConfirmed(Order order) {
        OrderConfirmedEvent event = new OrderConfirmedEvent(
                generateEventId(),
                order.getId(),
                order.getCustomerId(),
                order.getRestaurantId(),
                order.getPaymentId(),
                toItemInfoList(order.getItems()),
                order.getTotalAmount(),
                order.getCurrency(),
                toDeliveryInfo(order),
                order.getDeliveryInstructions(),
                Instant.now(),
                order.getCorrelationId()
        );
        publish(event, order.getId());
    }
    
    public void publishOrderPreparing(Order order) {
        OrderPreparingEvent event = new OrderPreparingEvent(
                generateEventId(),
                order.getId(),
                order.getCustomerId(),
                order.getRestaurantId(),
                order.getPreparingAt(),
                Instant.now(),
                order.getCorrelationId()
        );
        publish(event, order.getId());
    }
    
    public void publishOrderReady(Order order) {
        OrderReadyEvent event = new OrderReadyEvent(
                generateEventId(),
                order.getId(),
                order.getCustomerId(),
                order.getRestaurantId(),
                toDeliveryInfo(order),
                Instant.now(),
                order.getCorrelationId()
        );
        publish(event, order.getId());
    }
    
    public void publishOutForDelivery(Order order) {
        OrderOutForDeliveryEvent event = new OrderOutForDeliveryEvent(
                generateEventId(),
                order.getId(),
                order.getCustomerId(),
                order.getDriverId(),
                toDeliveryInfo(order),
                Instant.now(),
                order.getCorrelationId()
        );
        publish(event, order.getId());
    }
    
    public void publishOrderDelivered(Order order) {
        OrderDeliveredEvent event = new OrderDeliveredEvent(
                generateEventId(),
                order.getId(),
                order.getCustomerId(),
                order.getRestaurantId(),
                order.getDriverId(),
                order.getTotalAmount(),
                order.getDeliveredAt(),
                Instant.now(),
                order.getCorrelationId()
        );
        publish(event, order.getId());
    }
    
    public void publishOrderCancelled(Order order) {
        boolean requiresRefund = order.getPaymentId() != null 
                && "SUCCESS".equals(order.getPaymentStatus());
        
        OrderCancelledEvent event = new OrderCancelledEvent(
                generateEventId(),
                order.getId(),
                order.getCustomerId(),
                order.getRestaurantId(),
                order.getCancellationReason(),
                order.getCancelledBy(),
                requiresRefund,
                order.getPaymentId(),
                Instant.now(),
                order.getCorrelationId()
        );
        publish(event, order.getId());
    }
    
    public void publishOrderFailed(Order order, String failedStep) {
        boolean requiresRefund = order.getPaymentId() != null 
                && "SUCCESS".equals(order.getPaymentStatus());
        
        OrderFailedEvent event = new OrderFailedEvent(
                generateEventId(),
                order.getId(),
                order.getCustomerId(),
                order.getRestaurantId(),
                order.getFailureReason(),
                failedStep,
                requiresRefund,
                order.getPaymentId(),
                Instant.now(),
                order.getCorrelationId()
        );
        publish(event, order.getId());
    }
    
    // ========================================
    // INTERNAL METHODS
    // ========================================
    
    private void publish(OrderEvent event, String key) {
        log.info("Publishing {} event for order {}", event.eventType(), event.orderId());
        
        ProducerRecord<String, OrderEvent> record = new ProducerRecord<>(
                orderEventsTopic,
                key,
                event
        );
        
        record.headers()
                .add("eventType", event.eventType().getBytes())
                .add("correlationId", event.correlationId() != null 
                        ? event.correlationId().getBytes() 
                        : "unknown".getBytes())
                .add("timestamp", event.timestamp().toString().getBytes());
        
        CompletableFuture<SendResult<String, OrderEvent>> future = kafkaTemplate.send(record);
        
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.debug("Event {} published to partition {} offset {}",
                        event.eventId(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                log.error("Failed to publish event {}: {}", event.eventId(), ex.getMessage(), ex);
            }
        });
    }
    
    private String generateEventId() {
        return UUID.randomUUID().toString();
    }
    
    private List<OrderItemInfo> toItemInfoList(List<OrderItem> items) {
        return items.stream()
                .map(item -> new OrderItemInfo(
                        item.getMenuItemId(),
                        item.getMenuItemName(),
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getTotalPrice()
                ))
                .toList();
    }
    
    private DeliveryInfo toDeliveryInfo(Order order) {
        var addr = order.getDeliveryAddress();
        if (addr == null) return null;
        return new DeliveryInfo(
                addr.getAddressLine1(),
                addr.getCity(),
                addr.getPostalCode(),
                addr.getLatitude(),
                addr.getLongitude(),
                addr.getContactName(),
                addr.getContactPhone()
        );
    }
}
