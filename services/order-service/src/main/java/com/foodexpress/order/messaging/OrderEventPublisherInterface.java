package com.foodexpress.order.messaging;

import com.foodexpress.order.domain.Order;

/**
 * Interface for publishing order events.
 * Allows different implementations for production (Kafka) and local testing (no-op).
 */
public interface OrderEventPublisherInterface {
    
    void publishOrderCreated(Order order);
    
    void publishPaymentInitiated(Order order);
    
    void publishOrderConfirmed(Order order);
    
    void publishOrderPreparing(Order order);
    
    void publishOrderReady(Order order);
    
    void publishOutForDelivery(Order order);
    
    void publishOrderDelivered(Order order);
    
    void publishOrderCancelled(Order order);
    
    void publishOrderFailed(Order order, String failedStep);
}
