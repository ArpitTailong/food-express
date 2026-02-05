package com.foodexpress.payment.messaging;

import com.foodexpress.payment.domain.Payment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * No-op implementation of PaymentEventPublisherInterface for local testing.
 * Simply logs the events without actually publishing to Kafka.
 */
@Component
@Profile("local")
public class NoOpPaymentEventPublisher implements PaymentEventPublisherInterface {
    
    private static final Logger log = LoggerFactory.getLogger(NoOpPaymentEventPublisher.class);
    
    @Override
    public void publishPaymentCreated(Payment payment) {
        log.info("[LOCAL] Payment created event - paymentId: {}, orderId: {}, amount: {} {}", 
                payment.getId(), payment.getOrderId(), payment.getAmount(), payment.getCurrency());
    }
    
    @Override
    public void publishPaymentCompleted(Payment payment) {
        log.info("[LOCAL] Payment completed event - paymentId: {}, orderId: {}, txnId: {}", 
                payment.getId(), payment.getOrderId(), payment.getGatewayTransactionId());
    }
    
    @Override
    public void publishPaymentFailed(Payment payment) {
        log.info("[LOCAL] Payment failed event - paymentId: {}, orderId: {}, error: {}", 
                payment.getId(), payment.getOrderId(), payment.getErrorMessage());
    }
    
    @Override
    public void publishPaymentRefunded(Payment payment) {
        log.info("[LOCAL] Payment refunded event - paymentId: {}, orderId: {}, refundAmount: {}", 
                payment.getId(), payment.getOrderId(), payment.getRefundAmount());
    }
}
