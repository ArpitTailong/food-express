package com.foodexpress.payment.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Payment entity with optimistic locking and state machine support.
 * 
 * IMPORTANT: No sensitive card data is stored (PCI-DSS compliance).
 * Only gateway tokens and masked card info are persisted.
 */
@Entity
@Table(name = "payments", indexes = {
        @Index(name = "idx_payment_order_id", columnList = "order_id"),
        @Index(name = "idx_payment_idempotency_key", columnList = "idempotency_key", unique = true),
        @Index(name = "idx_payment_status", columnList = "status"),
        @Index(name = "idx_payment_customer_id", columnList = "customer_id")
})
public class Payment {
    
    @Id
    @Column(length = 36)
    private String id;
    
    @Column(name = "order_id", nullable = false, length = 36)
    private String orderId;
    
    @Column(name = "customer_id", nullable = false, length = 36)
    private String customerId;
    
    @Column(name = "idempotency_key", nullable = false, unique = true, length = 64)
    private String idempotencyKey;
    
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;
    
    @Column(nullable = false, length = 3)
    private String currency;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;
    
    @Column(name = "payment_method", length = 20)
    private String paymentMethod; // CARD, WALLET, UPI, etc.
    
    // Gateway information (NO RAW CARD DATA)
    @Column(name = "gateway_token", length = 255)
    private String gatewayToken; // Stripe payment_method_id, etc.
    
    @Column(name = "gateway_transaction_id", length = 100)
    private String gatewayTransactionId;
    
    @Column(name = "gateway_response_code", length = 50)
    private String gatewayResponseCode;
    
    // Masked card info for display only
    @Column(name = "card_last_four", length = 4)
    private String cardLastFour;
    
    @Column(name = "card_brand", length = 20)
    private String cardBrand;
    
    // Failure information
    @Column(name = "error_code", length = 50)
    private String errorCode;
    
    @Column(name = "error_message", length = 500)
    private String errorMessage;
    
    @Column(name = "attempt_count")
    private int attemptCount = 0;
    
    // Refund information
    @Column(name = "refund_id", length = 100)
    private String refundId;
    
    @Column(name = "refund_amount", precision = 12, scale = 2)
    private BigDecimal refundAmount;
    
    @Column(name = "refund_reason", length = 255)
    private String refundReason;
    
    @Column(name = "refunded_at")
    private Instant refundedAt;
    
    // Timestamps
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    @Column(name = "processed_at")
    private Instant processedAt;
    
    @Column(name = "completed_at")
    private Instant completedAt;
    
    // Optimistic locking
    @Version
    private Long version;
    
    // Correlation ID for tracing
    @Column(name = "correlation_id", length = 64)
    private String correlationId;
    
    // ========================================
    // CONSTRUCTORS
    // ========================================
    
    protected Payment() {} // JPA
    
    public Payment(String orderId, String customerId, String idempotencyKey,
                   BigDecimal amount, String currency, String paymentMethod) {
        this.id = UUID.randomUUID().toString();
        this.orderId = orderId;
        this.customerId = customerId;
        this.idempotencyKey = idempotencyKey;
        this.amount = amount;
        this.currency = currency;
        this.paymentMethod = paymentMethod;
        this.status = PaymentStatus.CREATED;
        this.attemptCount = 0;
    }
    
    // ========================================
    // STATE MACHINE TRANSITIONS
    // ========================================
    
    public boolean canTransitionTo(PaymentStatus newStatus) {
        return status.canTransitionTo(newStatus);
    }
    
    public void startProcessing(String gatewayToken) {
        validateTransition(PaymentStatus.PROCESSING);
        this.status = PaymentStatus.PROCESSING;
        this.gatewayToken = gatewayToken;
        this.processedAt = Instant.now();
        this.attemptCount++;
    }
    
    public void markSuccess(String gatewayTransactionId, String responseCode) {
        validateTransition(PaymentStatus.SUCCESS);
        this.status = PaymentStatus.SUCCESS;
        this.gatewayTransactionId = gatewayTransactionId;
        this.gatewayResponseCode = responseCode;
        this.completedAt = Instant.now();
    }
    
    public void markFailed(String errorCode, String errorMessage) {
        validateTransition(PaymentStatus.FAILED);
        this.status = PaymentStatus.FAILED;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }
    
    public void markRefunded(String refundId, BigDecimal refundAmount, String reason) {
        validateTransition(PaymentStatus.REFUNDED);
        this.status = PaymentStatus.REFUNDED;
        this.refundId = refundId;
        this.refundAmount = refundAmount;
        this.refundReason = reason;
        this.refundedAt = Instant.now();
    }
    
    public void retryPayment() {
        if (this.status != PaymentStatus.FAILED) {
            throw new IllegalStateException("Can only retry failed payments");
        }
        this.status = PaymentStatus.PROCESSING;
        this.errorCode = null;
        this.errorMessage = null;
        this.processedAt = Instant.now();
        this.attemptCount++;
    }
    
    private void validateTransition(PaymentStatus newStatus) {
        if (!canTransitionTo(newStatus)) {
            throw new IllegalStateException(
                    "Invalid transition from %s to %s".formatted(status, newStatus));
        }
    }
    
    // ========================================
    // GETTERS & SETTERS
    // ========================================
    
    public String getId() { return id; }
    public String getOrderId() { return orderId; }
    public String getCustomerId() { return customerId; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public PaymentStatus getStatus() { return status; }
    public String getPaymentMethod() { return paymentMethod; }
    public String getGatewayToken() { return gatewayToken; }
    public String getGatewayTransactionId() { return gatewayTransactionId; }
    public String getGatewayResponseCode() { return gatewayResponseCode; }
    public String getCardLastFour() { return cardLastFour; }
    public String getCardBrand() { return cardBrand; }
    public String getErrorCode() { return errorCode; }
    public String getErrorMessage() { return errorMessage; }
    public int getAttemptCount() { return attemptCount; }
    public String getRefundId() { return refundId; }
    public BigDecimal getRefundAmount() { return refundAmount; }
    public String getRefundReason() { return refundReason; }
    public Instant getRefundedAt() { return refundedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getProcessedAt() { return processedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public Long getVersion() { return version; }
    public String getCorrelationId() { return correlationId; }
    
    public void setCardLastFour(String cardLastFour) { this.cardLastFour = cardLastFour; }
    public void setCardBrand(String cardBrand) { this.cardBrand = cardBrand; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
}
