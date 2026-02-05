package com.foodexpress.order.messaging;

import com.foodexpress.order.domain.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * No-op implementation of OrderEventPublisherInterface for local testing.
 * Simply logs the events without actually publishing to Kafka.
 */
@Component
@Profile("local")
public class NoOpOrderEventPublisher implements OrderEventPublisherInterface {
    
    private static final Logger log = LoggerFactory.getLogger(NoOpOrderEventPublisher.class);
    
    @Override
    public void publishOrderCreated(Order order) {
        log.info("[LOCAL] Order created event - orderId: {}, customerId: {}, total: {} {}", 
                order.getId(), order.getCustomerId(), order.getTotalAmount(), order.getCurrency());
    }
    
    @Override
    public void publishPaymentInitiated(Order order) {
        log.info("[LOCAL] Payment initiated event - orderId: {}, amount: {} {}", 
                order.getId(), order.getTotalAmount(), order.getCurrency());
    }
    
    @Override
    public void publishOrderConfirmed(Order order) {
        log.info("[LOCAL] Order confirmed event - orderId: {}", order.getId());
    }
    
    @Override
    public void publishOrderPreparing(Order order) {
        log.info("[LOCAL] Order preparing event - orderId: {}", order.getId());
    }
    
    @Override
    public void publishOrderReady(Order order) {
        log.info("[LOCAL] Order ready event - orderId: {}", order.getId());
    }
    
    @Override
    public void publishOutForDelivery(Order order) {
        log.info("[LOCAL] Order out for delivery event - orderId: {}, driverId: {}", 
                order.getId(), order.getDriverId());
    }
    
    @Override
    public void publishOrderDelivered(Order order) {
        log.info("[LOCAL] Order delivered event - orderId: {}", order.getId());
    }
    
    @Override
    public void publishOrderCancelled(Order order) {
        log.info("[LOCAL] Order cancelled event - orderId: {}, reason: {}", 
                order.getId(), order.getCancellationReason());
    }
    
    @Override
    public void publishOrderFailed(Order order, String failedStep) {
        log.info("[LOCAL] Order failed event - orderId: {}, failedStep: {}, reason: {}", 
                order.getId(), failedStep, order.getFailureReason());
    }
}
