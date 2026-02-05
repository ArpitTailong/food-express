package com.foodexpress.analytics.messaging;

import com.foodexpress.analytics.domain.DailyOrderMetrics;
import com.foodexpress.analytics.domain.OrderEvent;
import com.foodexpress.analytics.repository.DailyOrderMetricsRepository;
import com.foodexpress.analytics.repository.OrderEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Kafka Event Consumer for Analytics.
 * Processes events from Order, Payment, and User services.
 */
@Component
public class AnalyticsEventConsumer {
    
    private static final Logger log = LoggerFactory.getLogger(AnalyticsEventConsumer.class);
    
    private final OrderEventRepository eventRepository;
    private final DailyOrderMetricsRepository metricsRepository;
    
    public AnalyticsEventConsumer(
            OrderEventRepository eventRepository,
            DailyOrderMetricsRepository metricsRepository) {
        this.eventRepository = eventRepository;
        this.metricsRepository = metricsRepository;
    }
    
    @KafkaListener(
            topics = {"order-events", "order-analytics"},
            groupId = "analytics-service",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void handleOrderEvent(
            @Payload Map<String, Object> event,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(value = "eventType", required = false) String eventType,
            Acknowledgment ack) {
        
        try {
            log.debug("Received order event: {}", eventType);
            
            String orderId = (String) event.get("orderId");
            
            // Store raw event
            OrderEvent orderEvent = new OrderEvent(
                    UUID.randomUUID().toString(),
                    orderId,
                    eventType != null ? eventType : "UNKNOWN",
                    LocalDateTime.now()
            );
            
            // Extract and set additional data
            if (event.containsKey("customerId")) {
                orderEvent.setCustomerId((String) event.get("customerId"));
            }
            if (event.containsKey("restaurantId")) {
                orderEvent.setRestaurantId((String) event.get("restaurantId"));
            }
            if (event.containsKey("driverId")) {
                orderEvent.setDriverId((String) event.get("driverId"));
            }
            if (event.containsKey("totalAmount")) {
                orderEvent.setOrderTotal(extractBigDecimal(event.get("totalAmount")));
            }
            if (event.containsKey("deliveryFee")) {
                orderEvent.setDeliveryFee(extractBigDecimal(event.get("deliveryFee")));
            }
            if (event.containsKey("tipAmount")) {
                orderEvent.setTipAmount(extractBigDecimal(event.get("tipAmount")));
            }
            if (event.containsKey("discountAmount")) {
                orderEvent.setDiscountAmount(extractBigDecimal(event.get("discountAmount")));
            }
            
            eventRepository.save(orderEvent);
            
            // Update daily metrics for relevant events
            if (eventType != null && shouldUpdateMetrics(eventType)) {
                updateDailyMetrics(orderEvent, eventType);
            }
            
            ack.acknowledge();
            
        } catch (Exception e) {
            log.error("Error processing order event: {}", e.getMessage(), e);
            // Still acknowledge to prevent infinite loop
            // In production, send to DLQ
            ack.acknowledge();
        }
    }
    
    @KafkaListener(
            topics = "payment-events",
            groupId = "analytics-service",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void handlePaymentEvent(
            @Payload Map<String, Object> event,
            @Header(value = "eventType", required = false) String eventType,
            Acknowledgment ack) {
        
        try {
            log.debug("Received payment event: {}", eventType);
            
            // Track refunds
            if ("REFUND_COMPLETED".equals(eventType)) {
                BigDecimal amount = extractBigDecimal(event.get("amount"));
                if (amount != null) {
                    LocalDate today = LocalDate.now();
                    DailyOrderMetrics metrics = metricsRepository
                            .findByMetricDateAndRestaurantIdIsNull(today)
                            .orElse(new DailyOrderMetrics(today));
                    
                    metrics.addRefund(amount);
                    metricsRepository.save(metrics);
                }
            }
            
            ack.acknowledge();
            
        } catch (Exception e) {
            log.error("Error processing payment event: {}", e.getMessage(), e);
            ack.acknowledge();
        }
    }
    
    private boolean shouldUpdateMetrics(String eventType) {
        return switch (eventType) {
            case "ORDER_CREATED", "ORDER_DELIVERED", "ORDER_CANCELLED", "ORDER_FAILED" -> true;
            default -> false;
        };
    }
    
    private void updateDailyMetrics(OrderEvent event, String eventType) {
        LocalDate today = LocalDate.now();
        
        // Platform-wide metrics
        DailyOrderMetrics platformMetrics = metricsRepository
                .findByMetricDateAndRestaurantIdIsNull(today)
                .orElse(new DailyOrderMetrics(today));
        
        // Restaurant-specific metrics
        DailyOrderMetrics restaurantMetrics = null;
        if (event.getRestaurantId() != null) {
            restaurantMetrics = metricsRepository
                    .findByMetricDateAndRestaurantId(today, event.getRestaurantId())
                    .orElse(new DailyOrderMetrics(today, event.getRestaurantId()));
        }
        
        switch (eventType) {
            case "ORDER_CREATED" -> {
                platformMetrics.incrementOrders("PENDING");
                if (restaurantMetrics != null) restaurantMetrics.incrementOrders("PENDING");
            }
            case "ORDER_DELIVERED" -> {
                platformMetrics.incrementOrders("DELIVERED");
                if (event.getOrderTotal() != null) {
                    platformMetrics.addRevenue(
                            event.getOrderTotal(),
                            event.getDeliveryFee(),
                            event.getTipAmount(),
                            event.getDiscountAmount()
                    );
                }
                if (restaurantMetrics != null) {
                    restaurantMetrics.incrementOrders("DELIVERED");
                    if (event.getOrderTotal() != null) {
                        restaurantMetrics.addRevenue(
                                event.getOrderTotal(),
                                event.getDeliveryFee(),
                                event.getTipAmount(),
                                event.getDiscountAmount()
                        );
                    }
                }
            }
            case "ORDER_CANCELLED" -> {
                platformMetrics.incrementOrders("CANCELLED");
                if (restaurantMetrics != null) restaurantMetrics.incrementOrders("CANCELLED");
            }
            case "ORDER_FAILED" -> {
                platformMetrics.incrementOrders("FAILED");
                if (restaurantMetrics != null) restaurantMetrics.incrementOrders("FAILED");
            }
        }
        
        metricsRepository.save(platformMetrics);
        if (restaurantMetrics != null) {
            metricsRepository.save(restaurantMetrics);
        }
        
        event.markProcessed();
        eventRepository.save(event);
    }
    
    private BigDecimal extractBigDecimal(Object value) {
        if (value == null) return null;
        if (value instanceof BigDecimal) return (BigDecimal) value;
        if (value instanceof Number) return BigDecimal.valueOf(((Number) value).doubleValue());
        if (value instanceof String) {
            try {
                return new BigDecimal((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}
