package com.foodexpress.order.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Order aggregate root entity.
 * 
 * Contains all order information including items, delivery details,
 * and status tracking for saga coordination.
 */
@Entity
@Table(name = "orders", indexes = {
        @Index(name = "idx_order_customer_id", columnList = "customer_id"),
        @Index(name = "idx_order_restaurant_id", columnList = "restaurant_id"),
        @Index(name = "idx_order_status", columnList = "status"),
        @Index(name = "idx_order_created_at", columnList = "created_at"),
        @Index(name = "idx_order_driver_id", columnList = "driver_id")
})
public class Order {
    
    @Id
    @Column(length = 36)
    private String id;
    
    // ========================================
    // PARTICIPANTS
    // ========================================
    
    @Column(name = "customer_id", nullable = false, length = 36)
    private String customerId;
    
    @Column(name = "restaurant_id", nullable = false, length = 36)
    private String restaurantId;
    
    @Column(name = "driver_id", length = 36)
    private String driverId;
    
    // ========================================
    // ORDER DETAILS
    // ========================================
    
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<OrderItem> items = new ArrayList<>();
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private OrderStatus status;
    
    // ========================================
    // PRICING
    // ========================================
    
    @Column(name = "subtotal", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;
    
    @Column(name = "delivery_fee", precision = 12, scale = 2)
    private BigDecimal deliveryFee = BigDecimal.ZERO;
    
    @Column(name = "tax_amount", precision = 12, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;
    
    @Column(name = "discount_amount", precision = 12, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;
    
    @Column(name = "tip_amount", precision = 12, scale = 2)
    private BigDecimal tipAmount = BigDecimal.ZERO;
    
    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;
    
    @Column(length = 3)
    private String currency = "INR";
    
    @Column(name = "coupon_code", length = 50)
    private String couponCode;
    
    // ========================================
    // DELIVERY INFORMATION
    // ========================================
    
    @Embedded
    private DeliveryAddress deliveryAddress;
    
    @Column(name = "delivery_instructions", length = 500)
    private String deliveryInstructions;
    
    @Column(name = "estimated_delivery_time")
    private LocalDateTime estimatedDeliveryTime;
    
    @Column(name = "actual_delivery_time")
    private LocalDateTime actualDeliveryTime;
    
    // ========================================
    // PAYMENT INFORMATION
    // ========================================
    
    @Column(name = "payment_id", length = 36)
    private String paymentId;
    
    @Column(name = "payment_method", length = 20)
    private String paymentMethod;
    
    @Column(name = "payment_status", length = 20)
    private String paymentStatus;
    
    // ========================================
    // STATUS TRACKING
    // ========================================
    
    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;
    
    @Column(name = "cancelled_by", length = 20)
    private String cancelledBy; // CUSTOMER, RESTAURANT, DRIVER, SYSTEM
    
    @Column(name = "failure_reason", length = 500)
    private String failureReason;
    
    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;
    
    // ========================================
    // RATINGS
    // ========================================
    
    @Column(name = "restaurant_rating")
    private Integer restaurantRating;
    
    @Column(name = "driver_rating")
    private Integer driverRating;
    
    @Column(name = "customer_feedback", length = 1000)
    private String customerFeedback;
    
    // ========================================
    // TIMESTAMPS
    // ========================================
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    @Column(name = "confirmed_at")
    private Instant confirmedAt;
    
    @Column(name = "preparing_at")
    private Instant preparingAt;
    
    @Column(name = "ready_at")
    private Instant readyAt;
    
    @Column(name = "picked_up_at")
    private Instant pickedUpAt;
    
    @Column(name = "delivered_at")
    private Instant deliveredAt;
    
    @Column(name = "cancelled_at")
    private Instant cancelledAt;
    
    // ========================================
    // OPTIMISTIC LOCKING & TRACING
    // ========================================
    
    @Version
    private Long version;
    
    @Column(name = "correlation_id", length = 64)
    private String correlationId;
    
    // ========================================
    // CONSTRUCTORS
    // ========================================
    
    protected Order() {} // JPA
    
    public Order(String customerId, String restaurantId, DeliveryAddress deliveryAddress) {
        this.id = UUID.randomUUID().toString();
        this.customerId = customerId;
        this.restaurantId = restaurantId;
        this.deliveryAddress = deliveryAddress;
        this.status = OrderStatus.PENDING;
        this.subtotal = BigDecimal.ZERO;
        this.totalAmount = BigDecimal.ZERO;
    }
    
    // ========================================
    // BUSINESS METHODS
    // ========================================
    
    /**
     * Add item to order (only in PENDING status)
     */
    public void addItem(OrderItem item) {
        if (status != OrderStatus.PENDING) {
            throw new IllegalStateException("Cannot add items to order in status: " + status);
        }
        item.setOrder(this);
        items.add(item);
        recalculateTotals();
    }
    
    /**
     * Remove item from order (only in PENDING status)
     */
    public void removeItem(OrderItem item) {
        if (status != OrderStatus.PENDING) {
            throw new IllegalStateException("Cannot remove items from order in status: " + status);
        }
        items.remove(item);
        recalculateTotals();
    }
    
    /**
     * Recalculate order totals
     */
    public void recalculateTotals() {
        this.subtotal = items.stream()
                .map(OrderItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        this.totalAmount = subtotal
                .add(deliveryFee != null ? deliveryFee : BigDecimal.ZERO)
                .add(taxAmount != null ? taxAmount : BigDecimal.ZERO)
                .add(tipAmount != null ? tipAmount : BigDecimal.ZERO)
                .subtract(discountAmount != null ? discountAmount : BigDecimal.ZERO);
    }
    
    // ========================================
    // STATE MACHINE TRANSITIONS
    // ========================================
    
    public boolean canTransitionTo(OrderStatus newStatus) {
        return status.canTransitionTo(newStatus);
    }
    
    private void validateTransition(OrderStatus newStatus) {
        if (!canTransitionTo(newStatus)) {
            throw new IllegalStateException(
                    "Invalid transition from %s to %s".formatted(status, newStatus));
        }
    }
    
    /**
     * Move to payment pending (checkout initiated)
     */
    public void initiatePayment(String paymentMethod) {
        validateTransition(OrderStatus.PAYMENT_PENDING);
        this.status = OrderStatus.PAYMENT_PENDING;
        this.paymentMethod = paymentMethod;
        this.paymentStatus = "PENDING";
    }
    
    /**
     * Payment successful - order confirmed
     */
    public void confirmOrder(String paymentId) {
        validateTransition(OrderStatus.CONFIRMED);
        this.status = OrderStatus.CONFIRMED;
        this.paymentId = paymentId;
        this.paymentStatus = "SUCCESS";
        this.confirmedAt = Instant.now();
    }
    
    /**
     * Restaurant started preparing
     */
    public void startPreparing() {
        validateTransition(OrderStatus.PREPARING);
        this.status = OrderStatus.PREPARING;
        this.preparingAt = Instant.now();
    }
    
    /**
     * Order ready for pickup
     */
    public void markReady() {
        validateTransition(OrderStatus.READY_FOR_PICKUP);
        this.status = OrderStatus.READY_FOR_PICKUP;
        this.readyAt = Instant.now();
    }
    
    /**
     * Driver picked up, out for delivery
     */
    public void startDelivery(String driverId) {
        validateTransition(OrderStatus.OUT_FOR_DELIVERY);
        this.status = OrderStatus.OUT_FOR_DELIVERY;
        this.driverId = driverId;
        this.pickedUpAt = Instant.now();
    }
    
    /**
     * Order delivered successfully
     */
    public void markDelivered() {
        validateTransition(OrderStatus.DELIVERED);
        this.status = OrderStatus.DELIVERED;
        this.deliveredAt = Instant.now();
        this.actualDeliveryTime = LocalDateTime.now();
    }
    
    /**
     * Cancel order
     */
    public void cancel(String reason, String cancelledBy) {
        validateTransition(OrderStatus.CANCELLED);
        this.status = OrderStatus.CANCELLED;
        this.cancellationReason = reason;
        this.cancelledBy = cancelledBy;
        this.cancelledAt = Instant.now();
    }
    
    /**
     * Mark order as failed
     */
    public void markFailed(String reason) {
        validateTransition(OrderStatus.FAILED);
        this.status = OrderStatus.FAILED;
        this.failureReason = reason;
    }
    
    /**
     * Payment failed
     */
    public void paymentFailed(String reason) {
        this.paymentStatus = "FAILED";
        markFailed("Payment failed: " + reason);
    }
    
    /**
     * Restaurant rejected order
     */
    public void restaurantRejected(String reason) {
        this.rejectionReason = reason;
        markFailed("Restaurant rejected: " + reason);
    }
    
    // ========================================
    // GETTERS
    // ========================================
    
    public String getId() { return id; }
    public String getCustomerId() { return customerId; }
    public String getRestaurantId() { return restaurantId; }
    public String getDriverId() { return driverId; }
    public List<OrderItem> getItems() { return items; }
    public OrderStatus getStatus() { return status; }
    public BigDecimal getSubtotal() { return subtotal; }
    public BigDecimal getDeliveryFee() { return deliveryFee; }
    public BigDecimal getTaxAmount() { return taxAmount; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public BigDecimal getTipAmount() { return tipAmount; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public String getCurrency() { return currency; }
    public String getCouponCode() { return couponCode; }
    public DeliveryAddress getDeliveryAddress() { return deliveryAddress; }
    public String getDeliveryInstructions() { return deliveryInstructions; }
    public LocalDateTime getEstimatedDeliveryTime() { return estimatedDeliveryTime; }
    public LocalDateTime getActualDeliveryTime() { return actualDeliveryTime; }
    public String getPaymentId() { return paymentId; }
    public String getPaymentMethod() { return paymentMethod; }
    public String getPaymentStatus() { return paymentStatus; }
    public String getCancellationReason() { return cancellationReason; }
    public String getCancelledBy() { return cancelledBy; }
    public String getFailureReason() { return failureReason; }
    public String getRejectionReason() { return rejectionReason; }
    public Integer getRestaurantRating() { return restaurantRating; }
    public Integer getDriverRating() { return driverRating; }
    public String getCustomerFeedback() { return customerFeedback; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getConfirmedAt() { return confirmedAt; }
    public Instant getPreparingAt() { return preparingAt; }
    public Instant getReadyAt() { return readyAt; }
    public Instant getPickedUpAt() { return pickedUpAt; }
    public Instant getDeliveredAt() { return deliveredAt; }
    public Instant getCancelledAt() { return cancelledAt; }
    public Long getVersion() { return version; }
    public String getCorrelationId() { return correlationId; }
    
    // ========================================
    // SETTERS (limited)
    // ========================================
    
    public void setDeliveryFee(BigDecimal deliveryFee) { 
        this.deliveryFee = deliveryFee;
        recalculateTotals();
    }
    
    public void setTaxAmount(BigDecimal taxAmount) { 
        this.taxAmount = taxAmount;
        recalculateTotals();
    }
    
    public void setDiscountAmount(BigDecimal discountAmount) { 
        this.discountAmount = discountAmount;
        recalculateTotals();
    }
    
    public void setTipAmount(BigDecimal tipAmount) { 
        this.tipAmount = tipAmount;
        recalculateTotals();
    }
    
    public void setCouponCode(String couponCode) { this.couponCode = couponCode; }
    public void setDeliveryInstructions(String deliveryInstructions) { this.deliveryInstructions = deliveryInstructions; }
    public void setEstimatedDeliveryTime(LocalDateTime estimatedDeliveryTime) { this.estimatedDeliveryTime = estimatedDeliveryTime; }
    public void setRestaurantRating(Integer restaurantRating) { this.restaurantRating = restaurantRating; }
    public void setDriverRating(Integer driverRating) { this.driverRating = driverRating; }
    public void setCustomerFeedback(String customerFeedback) { this.customerFeedback = customerFeedback; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
}
