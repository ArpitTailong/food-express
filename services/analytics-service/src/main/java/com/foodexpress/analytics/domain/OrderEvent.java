package com.foodexpress.analytics.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Raw Order Event for analytics processing.
 */
@Entity
@Table(name = "order_events")
public class OrderEvent {
    
    @Id
    private String id;
    
    @Column(name = "order_id", nullable = false)
    private String orderId;
    
    @Column(name = "event_type", nullable = false)
    private String eventType;
    
    @Column(name = "customer_id")
    private String customerId;
    
    @Column(name = "restaurant_id")
    private String restaurantId;
    
    @Column(name = "driver_id")
    private String driverId;
    
    @Column(name = "order_total")
    private java.math.BigDecimal orderTotal;
    
    @Column(name = "delivery_fee")
    private java.math.BigDecimal deliveryFee;
    
    @Column(name = "tip_amount")
    private java.math.BigDecimal tipAmount;
    
    @Column(name = "discount_amount")
    private java.math.BigDecimal discountAmount;
    
    @Column(name = "restaurant_rating")
    private Integer restaurantRating;
    
    @Column(name = "driver_rating")
    private Integer driverRating;
    
    @Column(name = "preparation_time_mins")
    private Integer preparationTimeMinutes;
    
    @Column(name = "delivery_time_mins")
    private Integer deliveryTimeMinutes;
    
    @Column(columnDefinition = "jsonb")
    private String metadata;
    
    @Column(name = "event_time", nullable = false)
    private LocalDateTime eventTime;
    
    @Column(name = "processed")
    private boolean processed;
    
    @Column(name = "processed_at")
    private LocalDateTime processedAt;
    
    protected OrderEvent() {}
    
    public OrderEvent(String id, String orderId, String eventType, LocalDateTime eventTime) {
        this.id = id;
        this.orderId = orderId;
        this.eventType = eventType;
        this.eventTime = eventTime;
        this.processed = false;
    }
    
    public void markProcessed() {
        this.processed = true;
        this.processedAt = LocalDateTime.now();
    }
    
    // Getters
    public String getId() { return id; }
    public String getOrderId() { return orderId; }
    public String getEventType() { return eventType; }
    public String getCustomerId() { return customerId; }
    public String getRestaurantId() { return restaurantId; }
    public String getDriverId() { return driverId; }
    public java.math.BigDecimal getOrderTotal() { return orderTotal; }
    public java.math.BigDecimal getDeliveryFee() { return deliveryFee; }
    public java.math.BigDecimal getTipAmount() { return tipAmount; }
    public java.math.BigDecimal getDiscountAmount() { return discountAmount; }
    public Integer getRestaurantRating() { return restaurantRating; }
    public Integer getDriverRating() { return driverRating; }
    public Integer getPreparationTimeMinutes() { return preparationTimeMinutes; }
    public Integer getDeliveryTimeMinutes() { return deliveryTimeMinutes; }
    public String getMetadata() { return metadata; }
    public LocalDateTime getEventTime() { return eventTime; }
    public boolean isProcessed() { return processed; }
    public LocalDateTime getProcessedAt() { return processedAt; }
    
    // Setters
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public void setRestaurantId(String restaurantId) { this.restaurantId = restaurantId; }
    public void setDriverId(String driverId) { this.driverId = driverId; }
    public void setOrderTotal(java.math.BigDecimal orderTotal) { this.orderTotal = orderTotal; }
    public void setDeliveryFee(java.math.BigDecimal deliveryFee) { this.deliveryFee = deliveryFee; }
    public void setTipAmount(java.math.BigDecimal tipAmount) { this.tipAmount = tipAmount; }
    public void setDiscountAmount(java.math.BigDecimal discountAmount) { this.discountAmount = discountAmount; }
    public void setRestaurantRating(Integer rating) { this.restaurantRating = rating; }
    public void setDriverRating(Integer rating) { this.driverRating = rating; }
    public void setPreparationTimeMinutes(Integer mins) { this.preparationTimeMinutes = mins; }
    public void setDeliveryTimeMinutes(Integer mins) { this.deliveryTimeMinutes = mins; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
}
