package com.foodexpress.analytics.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Daily Order Analytics - Pre-aggregated metrics.
 */
@Entity
@Table(name = "daily_order_metrics")
public class DailyOrderMetrics {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "metric_date", nullable = false)
    private LocalDate metricDate;
    
    @Column(name = "restaurant_id")
    private String restaurantId;
    
    // Order counts
    @Column(name = "total_orders")
    private int totalOrders;
    
    @Column(name = "completed_orders")
    private int completedOrders;
    
    @Column(name = "cancelled_orders")
    private int cancelledOrders;
    
    @Column(name = "failed_orders")
    private int failedOrders;
    
    // Revenue
    @Column(name = "gross_revenue", precision = 12, scale = 2)
    private BigDecimal grossRevenue;
    
    @Column(name = "net_revenue", precision = 12, scale = 2)
    private BigDecimal netRevenue;
    
    @Column(name = "delivery_fees", precision = 12, scale = 2)
    private BigDecimal deliveryFees;
    
    @Column(name = "tips_collected", precision = 12, scale = 2)
    private BigDecimal tipsCollected;
    
    @Column(name = "discounts_given", precision = 12, scale = 2)
    private BigDecimal discountsGiven;
    
    @Column(name = "refunds_issued", precision = 12, scale = 2)
    private BigDecimal refundsIssued;
    
    // Average metrics
    @Column(name = "avg_order_value", precision = 10, scale = 2)
    private BigDecimal avgOrderValue;
    
    @Column(name = "avg_delivery_time_mins")
    private Integer avgDeliveryTimeMinutes;
    
    @Column(name = "avg_preparation_time_mins")
    private Integer avgPreparationTimeMinutes;
    
    // Customer metrics
    @Column(name = "unique_customers")
    private int uniqueCustomers;
    
    @Column(name = "new_customers")
    private int newCustomers;
    
    @Column(name = "repeat_customers")
    private int repeatCustomers;
    
    // Ratings
    @Column(name = "avg_restaurant_rating", precision = 3, scale = 2)
    private BigDecimal avgRestaurantRating;
    
    @Column(name = "avg_driver_rating", precision = 3, scale = 2)
    private BigDecimal avgDriverRating;
    
    @Column(name = "ratings_count")
    private int ratingsCount;
    
    // Timestamps
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    protected DailyOrderMetrics() {}
    
    public DailyOrderMetrics(LocalDate metricDate) {
        this.metricDate = metricDate;
        this.grossRevenue = BigDecimal.ZERO;
        this.netRevenue = BigDecimal.ZERO;
        this.deliveryFees = BigDecimal.ZERO;
        this.tipsCollected = BigDecimal.ZERO;
        this.discountsGiven = BigDecimal.ZERO;
        this.refundsIssued = BigDecimal.ZERO;
        this.avgOrderValue = BigDecimal.ZERO;
        this.avgRestaurantRating = BigDecimal.ZERO;
        this.avgDriverRating = BigDecimal.ZERO;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    public DailyOrderMetrics(LocalDate metricDate, String restaurantId) {
        this(metricDate);
        this.restaurantId = restaurantId;
    }
    
    // Business methods
    public void incrementOrders(String status) {
        this.totalOrders++;
        switch (status) {
            case "DELIVERED" -> this.completedOrders++;
            case "CANCELLED" -> this.cancelledOrders++;
            case "FAILED" -> this.failedOrders++;
        }
        this.updatedAt = LocalDateTime.now();
    }
    
    public void addRevenue(BigDecimal orderTotal, BigDecimal deliveryFee, BigDecimal tip, BigDecimal discount) {
        this.grossRevenue = this.grossRevenue.add(orderTotal);
        this.deliveryFees = this.deliveryFees.add(deliveryFee != null ? deliveryFee : BigDecimal.ZERO);
        this.tipsCollected = this.tipsCollected.add(tip != null ? tip : BigDecimal.ZERO);
        this.discountsGiven = this.discountsGiven.add(discount != null ? discount : BigDecimal.ZERO);
        this.netRevenue = this.grossRevenue.subtract(this.discountsGiven);
        
        if (this.completedOrders > 0) {
            this.avgOrderValue = this.grossRevenue.divide(BigDecimal.valueOf(this.completedOrders), 2, java.math.RoundingMode.HALF_UP);
        }
        this.updatedAt = LocalDateTime.now();
    }
    
    public void addRefund(BigDecimal amount) {
        this.refundsIssued = this.refundsIssued.add(amount);
        this.netRevenue = this.netRevenue.subtract(amount);
        this.updatedAt = LocalDateTime.now();
    }
    
    // Getters
    public Long getId() { return id; }
    public LocalDate getMetricDate() { return metricDate; }
    public String getRestaurantId() { return restaurantId; }
    public int getTotalOrders() { return totalOrders; }
    public int getCompletedOrders() { return completedOrders; }
    public int getCancelledOrders() { return cancelledOrders; }
    public int getFailedOrders() { return failedOrders; }
    public BigDecimal getGrossRevenue() { return grossRevenue; }
    public BigDecimal getNetRevenue() { return netRevenue; }
    public BigDecimal getDeliveryFees() { return deliveryFees; }
    public BigDecimal getTipsCollected() { return tipsCollected; }
    public BigDecimal getDiscountsGiven() { return discountsGiven; }
    public BigDecimal getRefundsIssued() { return refundsIssued; }
    public BigDecimal getAvgOrderValue() { return avgOrderValue; }
    public Integer getAvgDeliveryTimeMinutes() { return avgDeliveryTimeMinutes; }
    public Integer getAvgPreparationTimeMinutes() { return avgPreparationTimeMinutes; }
    public int getUniqueCustomers() { return uniqueCustomers; }
    public int getNewCustomers() { return newCustomers; }
    public int getRepeatCustomers() { return repeatCustomers; }
    public BigDecimal getAvgRestaurantRating() { return avgRestaurantRating; }
    public BigDecimal getAvgDriverRating() { return avgDriverRating; }
    public int getRatingsCount() { return ratingsCount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    
    // Setters for aggregation
    public void setAvgDeliveryTimeMinutes(Integer mins) { 
        this.avgDeliveryTimeMinutes = mins;
        this.updatedAt = LocalDateTime.now();
    }
    
    public void setAvgPreparationTimeMinutes(Integer mins) { 
        this.avgPreparationTimeMinutes = mins;
        this.updatedAt = LocalDateTime.now();
    }
    
    public void setUniqueCustomers(int count) { 
        this.uniqueCustomers = count;
        this.updatedAt = LocalDateTime.now();
    }
    
    public void setNewCustomers(int count) { 
        this.newCustomers = count;
        this.updatedAt = LocalDateTime.now();
    }
    
    public void setRepeatCustomers(int count) { 
        this.repeatCustomers = count;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * Calculate average values based on completed orders.
     * Call this method to finalize daily metrics.
     */
    public void calculateAverages() {
        if (this.completedOrders > 0) {
            this.avgOrderValue = this.grossRevenue.divide(
                BigDecimal.valueOf(this.completedOrders), 
                2, 
                java.math.RoundingMode.HALF_UP
            );
        }
        this.updatedAt = LocalDateTime.now();
    }
}
