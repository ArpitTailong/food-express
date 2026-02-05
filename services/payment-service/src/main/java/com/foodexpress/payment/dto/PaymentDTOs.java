package com.foodexpress.payment.dto;

import com.foodexpress.payment.domain.PaymentStatus;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Payment DTOs using Java Records for immutability.
 * All requests/responses are defined as nested records.
 */
public final class PaymentDTOs {
    
    private PaymentDTOs() {} // Prevent instantiation
    
    // ========================================
    // REQUEST DTOs
    // ========================================
    
    /**
     * Request to create a new payment.
     * The idempotency key is passed via header (X-Idempotency-Key).
     */
    public record CreatePaymentRequest(
            @NotBlank(message = "Order ID is required")
            String orderId,
            
            @NotBlank(message = "Customer ID is required")
            String customerId,
            
            @NotNull(message = "Amount is required")
            @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
            @Digits(integer = 10, fraction = 2, message = "Amount format is invalid")
            BigDecimal amount,
            
            @NotBlank(message = "Currency is required")
            @Size(min = 3, max = 3, message = "Currency must be 3 characters (ISO 4217)")
            String currency,
            
            @NotBlank(message = "Payment method is required")
            String paymentMethod, // CARD, WALLET, UPI, COD
            
            // Gateway token (from Stripe.js, Razorpay, etc.)
            // Required for CARD payments
            String gatewayToken,
            
            // Optional metadata
            String description,
            
            // For saved cards - customer payment method ID
            String savedPaymentMethodId
    ) {
        // Compact constructor for validation
        public CreatePaymentRequest {
            currency = currency != null ? currency.toUpperCase() : null;
            paymentMethod = paymentMethod != null ? paymentMethod.toUpperCase() : null;
        }
    }
    
    /**
     * Request to refund a payment
     */
    public record RefundRequest(
            @NotBlank(message = "Reason is required")
            @Size(max = 255, message = "Reason must be less than 255 characters")
            String reason,
            
            // If null, full refund
            @DecimalMin(value = "0.01", message = "Refund amount must be greater than 0")
            @Digits(integer = 10, fraction = 2, message = "Refund amount format is invalid")
            BigDecimal amount
    ) {}
    
    /**
     * Request to retry a failed payment
     */
    public record RetryPaymentRequest(
            // Optional new gateway token (if card details changed)
            String gatewayToken
    ) {}
    
    // ========================================
    // RESPONSE DTOs
    // ========================================
    
    /**
     * Standard payment response
     */
    public record PaymentResponse(
            String id,
            String orderId,
            String customerId,
            BigDecimal amount,
            String currency,
            String status,
            String paymentMethod,
            String gatewayTransactionId,
            String cardLastFour,
            String cardBrand,
            int attemptCount,
            String errorCode,
            String errorMessage,
            Instant createdAt,
            Instant processedAt,
            Instant completedAt,
            RefundInfo refund
    ) {
        /**
         * Refund information (included only if refunded)
         */
        public record RefundInfo(
                String refundId,
                BigDecimal amount,
                String reason,
                Instant refundedAt
        ) {}
    }
    
    /**
     * Response for create payment (includes next action if 3DS required)
     */
    public record CreatePaymentResponse(
            String paymentId,
            String status,
            NextAction nextAction,
            PaymentResponse payment
    ) {
        /**
         * Next action for client (e.g., 3D Secure authentication)
         */
        public record NextAction(
                String type, // REDIRECT_TO_URL, USE_STRIPE_SDK, etc.
                String redirectUrl,
                String clientSecret, // For Stripe.js
                Object metadata
        ) {}
    }
    
    /**
     * Response for payment status check
     */
    public record PaymentStatusResponse(
            String paymentId,
            String orderId,
            PaymentStatus status,
            boolean isSuccessful,
            boolean isPending,
            boolean canRetry,
            String message
    ) {}
    
    // ========================================
    // INTERNAL DTOs (for service communication)
    // ========================================
    
    /**
     * Gateway request (sent to payment gateway)
     */
    public record GatewayRequest(
            String paymentId,
            String orderId,
            BigDecimal amount,
            String currency,
            String paymentMethod,
            String gatewayToken,
            String customerId,
            String idempotencyKey,
            String description
    ) {}
    
    /**
     * Gateway response (received from payment gateway)
     */
    public record GatewayResponse(
            boolean success,
            String transactionId,
            String responseCode,
            String errorCode,
            String errorMessage,
            String cardLastFour,
            String cardBrand,
            boolean requiresAction,
            NextActionDetails nextAction
    ) {
        public record NextActionDetails(
                String type,
                String url,
                String clientSecret
        ) {}
    }
    
    /**
     * Idempotency result wrapper
     */
    public record IdempotencyResult<T>(
            boolean isDuplicate,
            T cachedResponse,
            T newResponse
    ) {
        public T getResponse() {
            return isDuplicate ? cachedResponse : newResponse;
        }
    }
    
    // ========================================
    // BUILDER METHODS (for conversion from entity)
    // ========================================
    
    public static PaymentResponse toResponse(com.foodexpress.payment.domain.Payment payment) {
        PaymentResponse.RefundInfo refundInfo = null;
        if (payment.getRefundId() != null) {
            refundInfo = new PaymentResponse.RefundInfo(
                    payment.getRefundId(),
                    payment.getRefundAmount(),
                    payment.getRefundReason(),
                    payment.getRefundedAt()
            );
        }
        
        return new PaymentResponse(
                payment.getId(),
                payment.getOrderId(),
                payment.getCustomerId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getStatus().name(),
                payment.getPaymentMethod(),
                payment.getGatewayTransactionId(),
                payment.getCardLastFour(),
                payment.getCardBrand(),
                payment.getAttemptCount(),
                payment.getErrorCode(),
                payment.getErrorMessage(),
                payment.getCreatedAt(),
                payment.getProcessedAt(),
                payment.getCompletedAt(),
                refundInfo
        );
    }
    
    public static PaymentStatusResponse toStatusResponse(com.foodexpress.payment.domain.Payment payment) {
        String message = switch (payment.getStatus()) {
            case CREATED -> "Payment initiated, awaiting processing";
            case PROCESSING -> "Payment is being processed";
            case SUCCESS -> "Payment completed successfully";
            case FAILED -> "Payment failed: " + payment.getErrorMessage();
            case REFUNDED -> "Payment has been refunded";
        };
        
        boolean canRetry = payment.getStatus() == PaymentStatus.FAILED 
                && payment.getAttemptCount() < 3;
        
        return new PaymentStatusResponse(
                payment.getId(),
                payment.getOrderId(),
                payment.getStatus(),
                payment.getStatus().isSuccessful(),
                payment.getStatus().isPending(),
                canRetry,
                message
        );
    }
}
