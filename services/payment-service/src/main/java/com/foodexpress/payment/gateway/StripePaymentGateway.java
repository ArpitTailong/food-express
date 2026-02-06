package com.foodexpress.payment.gateway;

import com.foodexpress.payment.dto.PaymentDTOs.*;
import com.stripe.Stripe;
import com.stripe.exception.CardException;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stripe Payment Gateway implementation.
 * Handles real payment processing via Stripe API.
 * 
 * Configuration:
 * - stripe.api-key: Your Stripe secret key (sk_test_... or sk_live_...)
 * - stripe.webhook-secret: For webhook signature verification
 * 
 * Features:
 * - PaymentIntent API for SCA-compliant payments
 * - 3D Secure support
 * - Automatic card brand detection
 * - Refund processing
 * - Idempotency key support
 */
public class StripePaymentGateway implements PaymentGateway {
    
    private static final Logger log = LoggerFactory.getLogger(StripePaymentGateway.class);
    
    private String stripeApiKey;
    private String webhookSecret;
    
    // Cache for storing PaymentIntent IDs mapped to our payment IDs
    private final Map<String, String> paymentIntentCache = new ConcurrentHashMap<>();
    
    private static final Set<String> SUPPORTED_METHODS = Set.of("CARD", "WALLET");
    
    /**
     * Default constructor for programmatic configuration.
     */
    public StripePaymentGateway() {
    }
    
    /**
     * Set the Stripe API key.
     * @param apiKey Stripe secret key (sk_test_... or sk_live_...)
     */
    public void setApiKey(String apiKey) {
        this.stripeApiKey = apiKey;
    }
    
    /**
     * Set the webhook secret for signature verification.
     * @param webhookSecret Stripe webhook secret (whsec_...)
     */
    public void setWebhookSecret(String webhookSecret) {
        this.webhookSecret = webhookSecret;
    }
    
    /**
     * Get the webhook secret (for webhook controller).
     */
    public String getWebhookSecret() {
        return this.webhookSecret;
    }
    
    /**
     * Initialize the Stripe API with the configured key.
     * Must be called after setting the API key.
     */
    public void init() {
        if (stripeApiKey == null || stripeApiKey.isEmpty()) {
            throw new IllegalStateException("Stripe API key not configured!");
        }
        Stripe.apiKey = stripeApiKey;
        log.info("Stripe Payment Gateway initialized. Mode: {}", 
                stripeApiKey.startsWith("sk_test") ? "TEST" : 
                stripeApiKey.startsWith("sk_live") ? "LIVE" : "UNKNOWN");
    }
    
    @Override
    public String getProviderName() {
        return "STRIPE";
    }
    
    @Override
    public boolean supportsPaymentMethod(String paymentMethod) {
        return SUPPORTED_METHODS.contains(paymentMethod.toUpperCase());
    }
    
    @Override
    public GatewayResponse charge(GatewayRequest request) {
        log.info("Stripe Gateway: Processing charge for payment {} amount {} {}",
                request.paymentId(), request.amount(), request.currency());
        
        try {
            // Convert amount to cents/smallest currency unit
            long amountInCents = request.amount()
                    .multiply(BigDecimal.valueOf(100))
                    .longValue();
            
            // Build PaymentIntent parameters
            PaymentIntentCreateParams.Builder paramsBuilder = PaymentIntentCreateParams.builder()
                    .setAmount(amountInCents)
                    .setCurrency(request.currency().toLowerCase())
                    .setConfirm(true)
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                    .setEnabled(true)
                                    .setAllowRedirects(PaymentIntentCreateParams.AutomaticPaymentMethods.AllowRedirects.NEVER)
                                    .build()
                    )
                    .putMetadata("payment_id", request.paymentId())
                    .putMetadata("order_id", request.orderId())
                    .putMetadata("customer_id", request.customerId());
            
            // Add payment method (token from Stripe.js)
            if (request.gatewayToken() != null && !request.gatewayToken().isEmpty()) {
                paramsBuilder.setPaymentMethod(request.gatewayToken());
            }
            
            // Add description if provided
            if (request.description() != null) {
                paramsBuilder.setDescription(request.description());
            }
            
            // Add idempotency key for safe retries
            Map<String, Object> requestOptions = new HashMap<>();
            if (request.idempotencyKey() != null) {
                requestOptions.put("idempotencyKey", request.idempotencyKey());
            }
            
            // Create and confirm PaymentIntent
            PaymentIntent paymentIntent = PaymentIntent.create(
                    paramsBuilder.build(),
                    com.stripe.net.RequestOptions.builder()
                            .setIdempotencyKey(request.idempotencyKey())
                            .build()
            );
            
            // Cache the PaymentIntent ID
            paymentIntentCache.put(request.paymentId(), paymentIntent.getId());
            
            log.info("Stripe Gateway: PaymentIntent created. ID: {}, Status: {}", 
                    paymentIntent.getId(), paymentIntent.getStatus());
            
            // Handle different PaymentIntent statuses
            return handlePaymentIntentStatus(paymentIntent);
            
        } catch (CardException e) {
            // Card was declined
            log.warn("Stripe Gateway: Card declined for payment {}. Code: {}, Message: {}", 
                    request.paymentId(), e.getCode(), e.getMessage());
            return createFailureResponse(
                    mapStripeErrorCode(e.getCode()),
                    e.getMessage()
            );
        } catch (StripeException e) {
            // Other Stripe errors
            log.error("Stripe Gateway: Error processing payment {}. Type: {}, Message: {}", 
                    request.paymentId(), e.getClass().getSimpleName(), e.getMessage());
            return createFailureResponse(
                    "STRIPE_ERROR",
                    "Payment processing failed: " + e.getMessage()
            );
        } catch (Exception e) {
            log.error("Stripe Gateway: Unexpected error for payment {}", request.paymentId(), e);
            return createFailureResponse(
                    "GATEWAY_ERROR",
                    "Unexpected error: " + e.getMessage()
            );
        }
    }
    
    @Override
    public GatewayResponse refund(String transactionId, BigDecimal amount, String reason) {
        log.info("Stripe Gateway: Processing refund for transaction {} amount {}", 
                transactionId, amount);
        
        try {
            RefundCreateParams.Builder paramsBuilder = RefundCreateParams.builder()
                    .setPaymentIntent(transactionId);
            
            // If amount specified, partial refund
            if (amount != null) {
                long amountInCents = amount.multiply(BigDecimal.valueOf(100)).longValue();
                paramsBuilder.setAmount(amountInCents);
            }
            
            // Add reason if provided
            if (reason != null) {
                paramsBuilder.putMetadata("reason", reason);
                // Map to Stripe's refund reasons
                if (reason.toLowerCase().contains("duplicate")) {
                    paramsBuilder.setReason(RefundCreateParams.Reason.DUPLICATE);
                } else if (reason.toLowerCase().contains("fraud")) {
                    paramsBuilder.setReason(RefundCreateParams.Reason.FRAUDULENT);
                } else {
                    paramsBuilder.setReason(RefundCreateParams.Reason.REQUESTED_BY_CUSTOMER);
                }
            }
            
            Refund refund = Refund.create(paramsBuilder.build());
            
            log.info("Stripe Gateway: Refund successful. Refund ID: {}, Status: {}", 
                    refund.getId(), refund.getStatus());
            
            boolean success = "succeeded".equals(refund.getStatus()) || 
                              "pending".equals(refund.getStatus());
            
            return new GatewayResponse(
                    success,
                    refund.getId(),
                    success ? "REFUND_PROCESSED" : "REFUND_PENDING",
                    success ? null : "REFUND_FAILED",
                    success ? null : "Refund status: " + refund.getStatus(),
                    null,
                    null,
                    false,
                    null
            );
            
        } catch (StripeException e) {
            log.error("Stripe Gateway: Refund failed for transaction {}. Error: {}", 
                    transactionId, e.getMessage());
            return createFailureResponse(
                    "REFUND_FAILED",
                    "Refund failed: " + e.getMessage()
            );
        }
    }
    
    @Override
    public GatewayResponse getStatus(String transactionId) {
        log.info("Stripe Gateway: Checking status for transaction {}", transactionId);
        
        try {
            PaymentIntent paymentIntent = PaymentIntent.retrieve(transactionId);
            return handlePaymentIntentStatus(paymentIntent);
            
        } catch (StripeException e) {
            log.error("Stripe Gateway: Failed to get status for transaction {}. Error: {}", 
                    transactionId, e.getMessage());
            return createFailureResponse(
                    "STATUS_CHECK_FAILED",
                    "Could not retrieve payment status: " + e.getMessage()
            );
        }
    }
    
    /**
     * Handle PaymentIntent status and create appropriate response
     */
    private GatewayResponse handlePaymentIntentStatus(PaymentIntent paymentIntent) {
        String status = paymentIntent.getStatus();
        
        // Extract card details if available
        String cardLast4 = null;
        String cardBrand = null;
        if (paymentIntent.getPaymentMethod() != null) {
            try {
                var paymentMethod = com.stripe.model.PaymentMethod.retrieve(
                        paymentIntent.getPaymentMethod()
                );
                if (paymentMethod.getCard() != null) {
                    cardLast4 = paymentMethod.getCard().getLast4();
                    cardBrand = paymentMethod.getCard().getBrand().toUpperCase();
                }
            } catch (StripeException e) {
                log.warn("Could not retrieve payment method details: {}", e.getMessage());
            }
        }
        
        switch (status) {
            case "succeeded":
                return new GatewayResponse(
                        true,
                        paymentIntent.getId(),
                        "APPROVED",
                        null,
                        null,
                        cardLast4,
                        cardBrand,
                        false,
                        null
                );
                
            case "requires_action":
            case "requires_source_action":
                // 3D Secure or other authentication required
                String clientSecret = paymentIntent.getClientSecret();
                String redirectUrl = null;
                
                if (paymentIntent.getNextAction() != null) {
                    var nextAction = paymentIntent.getNextAction();
                    if (nextAction.getRedirectToUrl() != null) {
                        redirectUrl = nextAction.getRedirectToUrl().getUrl();
                    }
                }
                
                return new GatewayResponse(
                        false,
                        paymentIntent.getId(),
                        "REQUIRES_ACTION",
                        null,
                        null,
                        cardLast4,
                        cardBrand,
                        true,
                        new GatewayResponse.NextActionDetails(
                                "USE_STRIPE_SDK",
                                redirectUrl,
                                clientSecret
                        )
                );
                
            case "requires_payment_method":
                return createFailureResponse(
                        "PAYMENT_METHOD_REQUIRED",
                        "Payment method is required or was declined"
                );
                
            case "requires_confirmation":
                return new GatewayResponse(
                        false,
                        paymentIntent.getId(),
                        "REQUIRES_CONFIRMATION",
                        null,
                        null,
                        cardLast4,
                        cardBrand,
                        true,
                        new GatewayResponse.NextActionDetails(
                                "CONFIRM_PAYMENT",
                                null,
                                paymentIntent.getClientSecret()
                        )
                );
                
            case "processing":
                return new GatewayResponse(
                        false,
                        paymentIntent.getId(),
                        "PROCESSING",
                        null,
                        "Payment is being processed",
                        cardLast4,
                        cardBrand,
                        false,
                        null
                );
                
            case "canceled":
                return createFailureResponse(
                        "PAYMENT_CANCELED",
                        "Payment was canceled"
                );
                
            default:
                return createFailureResponse(
                        "UNKNOWN_STATUS",
                        "Unknown payment status: " + status
                );
        }
    }
    
    /**
     * Map Stripe error codes to our internal codes
     */
    private String mapStripeErrorCode(String stripeCode) {
        if (stripeCode == null) return "CARD_DECLINED";
        
        return switch (stripeCode) {
            case "card_declined" -> "CARD_DECLINED";
            case "insufficient_funds" -> "INSUFFICIENT_FUNDS";
            case "expired_card" -> "EXPIRED_CARD";
            case "incorrect_cvc" -> "INCORRECT_CVC";
            case "incorrect_number" -> "INVALID_CARD_NUMBER";
            case "processing_error" -> "PROCESSING_ERROR";
            case "rate_limit" -> "RATE_LIMIT_EXCEEDED";
            case "authentication_required" -> "AUTHENTICATION_REQUIRED";
            default -> "CARD_ERROR_" + stripeCode.toUpperCase();
        };
    }
    
    /**
     * Create a failure response
     */
    private GatewayResponse createFailureResponse(String errorCode, String errorMessage) {
        return new GatewayResponse(
                false,
                null,
                "DECLINED",
                errorCode,
                errorMessage,
                null,
                null,
                false,
                null
        );
    }
}
