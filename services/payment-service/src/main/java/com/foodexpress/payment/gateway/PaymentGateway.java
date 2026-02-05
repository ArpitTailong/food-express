package com.foodexpress.payment.gateway;

import com.foodexpress.payment.dto.PaymentDTOs.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Abstract Payment Gateway interface.
 * Implement this for each payment provider (Stripe, Razorpay, PayU, etc.)
 */
public interface PaymentGateway {
    
    /**
     * Charge a payment
     * @param request Gateway request with amount, currency, token
     * @return Gateway response with transaction ID or error
     */
    GatewayResponse charge(GatewayRequest request);
    
    /**
     * Refund a payment
     * @param transactionId Original transaction ID
     * @param amount Amount to refund (null for full refund)
     * @param reason Refund reason
     * @return Gateway response
     */
    GatewayResponse refund(String transactionId, BigDecimal amount, String reason);
    
    /**
     * Get payment status from gateway
     * @param transactionId Transaction ID
     * @return Gateway response with current status
     */
    GatewayResponse getStatus(String transactionId);
    
    /**
     * @return Gateway provider name (STRIPE, RAZORPAY, etc.)
     */
    String getProviderName();
    
    /**
     * Check if gateway supports the given payment method
     */
    boolean supportsPaymentMethod(String paymentMethod);
}
