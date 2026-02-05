package com.foodexpress.payment.messaging;

import com.foodexpress.payment.domain.Payment;

/**
 * Interface for publishing payment events.
 * Allows different implementations for production (Kafka) and local testing (no-op).
 */
public interface PaymentEventPublisherInterface {
    
    void publishPaymentCreated(Payment payment);
    
    void publishPaymentCompleted(Payment payment);
    
    void publishPaymentFailed(Payment payment);
    
    void publishPaymentRefunded(Payment payment);
}
