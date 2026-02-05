package com.foodexpress.common.domain;

/**
 * Java 21 Sealed Interface for Payment States.
 * 
 * Sealed classes/interfaces restrict which classes can extend/implement them,
 * providing exhaustive pattern matching and compile-time safety.
 * 
 * Payment State Machine:
 * CREATED → PROCESSING → SUCCESS → REFUNDED
 *                     ↘ FAILED → (retry) → PROCESSING
 *                              ↘ CANCELLED
 */
public sealed interface PaymentState permits
        PaymentState.Created,
        PaymentState.Processing,
        PaymentState.Success,
        PaymentState.Failed,
        PaymentState.Refunded,
        PaymentState.Cancelled {
    
    /**
     * Get the string representation of the state
     */
    String name();
    
    /**
     * Check if this is a terminal state (no further transitions allowed)
     */
    default boolean isTerminal() {
        return this instanceof Refunded || this instanceof Cancelled;
    }
    
    /**
     * Check if payment can be refunded from current state
     */
    default boolean canRefund() {
        return this instanceof Success;
    }
    
    /**
     * Check if payment can be retried from current state
     */
    default boolean canRetry() {
        return this instanceof Failed;
    }
    
    // ========================================
    // SEALED IMPLEMENTATIONS (Records)
    // ========================================
    
    /**
     * Initial state - Payment request created but not yet processed
     */
    record Created(String paymentId, java.time.Instant createdAt) implements PaymentState {
        @Override
        public String name() { return "CREATED"; }
    }
    
    /**
     * Payment is being processed by the payment gateway
     */
    record Processing(
            String paymentId,
            String gatewayTransactionId,
            java.time.Instant startedAt,
            int attemptNumber
    ) implements PaymentState {
        @Override
        public String name() { return "PROCESSING"; }
    }
    
    /**
     * Payment completed successfully
     */
    record Success(
            String paymentId,
            String gatewayTransactionId,
            java.time.Instant completedAt,
            java.math.BigDecimal amountCharged,
            String receiptUrl
    ) implements PaymentState {
        @Override
        public String name() { return "SUCCESS"; }
    }
    
    /**
     * Payment failed
     */
    record Failed(
            String paymentId,
            String gatewayTransactionId,
            java.time.Instant failedAt,
            String errorCode,
            String errorMessage,
            int attemptNumber,
            boolean retryable
    ) implements PaymentState {
        @Override
        public String name() { return "FAILED"; }
    }
    
    /**
     * Payment was refunded (partial or full)
     */
    record Refunded(
            String paymentId,
            String refundId,
            java.time.Instant refundedAt,
            java.math.BigDecimal refundAmount,
            String reason
    ) implements PaymentState {
        @Override
        public String name() { return "REFUNDED"; }
    }
    
    /**
     * Payment was cancelled before processing
     */
    record Cancelled(
            String paymentId,
            java.time.Instant cancelledAt,
            String reason,
            String cancelledBy
    ) implements PaymentState {
        @Override
        public String name() { return "CANCELLED"; }
    }
    
    // ========================================
    // PATTERN MATCHING UTILITIES (Java 21)
    // ========================================
    
    /**
     * Example of exhaustive pattern matching with sealed types
     */
    static String describeState(PaymentState state) {
        return switch (state) {
            case Created c -> 
                "Payment %s created at %s, awaiting processing".formatted(c.paymentId(), c.createdAt());
            
            case Processing p -> 
                "Payment %s processing (attempt #%d) via gateway %s".formatted(
                    p.paymentId(), p.attemptNumber(), p.gatewayTransactionId());
            
            case Success s -> 
                "Payment %s completed. Amount: %s. Receipt: %s".formatted(
                    s.paymentId(), s.amountCharged(), s.receiptUrl());
            
            case Failed f when f.retryable() -> 
                "Payment %s failed (retryable): %s. Attempt #%d".formatted(
                    f.paymentId(), f.errorMessage(), f.attemptNumber());
            
            case Failed f -> 
                "Payment %s failed permanently: %s [%s]".formatted(
                    f.paymentId(), f.errorMessage(), f.errorCode());
            
            case Refunded r -> 
                "Payment %s refunded. Amount: %s. Reason: %s".formatted(
                    r.paymentId(), r.refundAmount(), r.reason());
            
            case Cancelled c -> 
                "Payment %s cancelled by %s. Reason: %s".formatted(
                    c.paymentId(), c.cancelledBy(), c.reason());
        };
    }
    
    /**
     * Validate state transition
     */
    static boolean isValidTransition(PaymentState from, PaymentState to) {
        return switch (from) {
            case Created c -> to instanceof Processing;
            case Processing p -> to instanceof Success || to instanceof Failed;
            case Failed f when f.retryable() -> to instanceof Processing || to instanceof Cancelled;
            case Failed f -> to instanceof Cancelled;
            case Success s -> to instanceof Refunded;
            case Refunded r -> false; // Terminal state
            case Cancelled c -> false; // Terminal state
        };
    }
}
