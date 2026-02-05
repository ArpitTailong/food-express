package com.foodexpress.analytics.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Analytics DTOs - Using Java 21 Records.
 */
public final class AnalyticsDTOs {
    
    private AnalyticsDTOs() {}
    
    // ========================================
    // DASHBOARD RESPONSES
    // ========================================
    
    public record DashboardMetrics(
            LocalDate date,
            OrderMetrics orders,
            RevenueMetrics revenue,
            CustomerMetrics customers,
            PerformanceMetrics performance
    ) {}
    
    public record OrderMetrics(
            int totalOrders,
            int completedOrders,
            int cancelledOrders,
            int failedOrders,
            double completionRate,
            double cancellationRate
    ) {}
    
    public record RevenueMetrics(
            BigDecimal grossRevenue,
            BigDecimal netRevenue,
            BigDecimal deliveryFees,
            BigDecimal tips,
            BigDecimal discounts,
            BigDecimal refunds,
            BigDecimal avgOrderValue
    ) {}
    
    public record CustomerMetrics(
            int uniqueCustomers,
            int newCustomers,
            int repeatCustomers,
            double repeatRate
    ) {}
    
    public record PerformanceMetrics(
            int avgDeliveryTimeMinutes,
            int avgPreparationTimeMinutes,
            double avgRestaurantRating,
            double avgDriverRating,
            int totalRatings
    ) {}
    
    // ========================================
    // TIME SERIES
    // ========================================
    
    public record TimeSeriesData(
            String metric,
            String granularity, // HOURLY, DAILY, WEEKLY, MONTHLY
            List<DataPoint> data
    ) {}
    
    public record DataPoint(
            String timestamp,
            BigDecimal value
    ) {}
    
    // ========================================
    // RANKINGS
    // ========================================
    
    public record RestaurantRanking(
            String restaurantId,
            String restaurantName,
            int totalOrders,
            BigDecimal revenue,
            double avgRating,
            int rank
    ) {}
    
    public record DriverRanking(
            String driverId,
            String driverName,
            int deliveriesCompleted,
            int avgDeliveryTimeMinutes,
            double avgRating,
            int rank
    ) {}
    
    public record TopItemsResponse(
            List<TopItem> items
    ) {}
    
    public record TopItem(
            String menuItemId,
            String itemName,
            String restaurantName,
            int orderCount,
            BigDecimal revenue
    ) {}
    
    // ========================================
    // REPORTS
    // ========================================
    
    public record PeriodReport(
            LocalDate startDate,
            LocalDate endDate,
            OrderMetrics orders,
            RevenueMetrics revenue,
            CustomerMetrics customers,
            PerformanceMetrics performance,
            List<DailyTrend> dailyTrends,
            Map<String, BigDecimal> revenueByRestaurant,
            List<String> insights
    ) {}
    
    public record DailyTrend(
            LocalDate date,
            int orders,
            BigDecimal revenue,
            int newCustomers
    ) {}
    
    // ========================================
    // REAL-TIME METRICS
    // ========================================
    
    public record RealTimeMetrics(
            long timestamp,
            int ordersLastHour,
            int activeOrders,
            int availableDrivers,
            int pendingPickups,
            BigDecimal revenueLastHour,
            double avgDeliveryTimeLastHour
    ) {}
    
    // ========================================
    // RESTAURANT ANALYTICS
    // ========================================
    
    public record RestaurantAnalytics(
            String restaurantId,
            LocalDate startDate,
            LocalDate endDate,
            int totalOrders,
            BigDecimal totalRevenue,
            BigDecimal avgOrderValue,
            double avgRating,
            int avgPreparationTimeMinutes,
            List<HourlyDistribution> peakHours,
            List<TopItem> topItems,
            double repeatCustomerRate
    ) {}
    
    public record HourlyDistribution(
            int hour,
            int orderCount,
            BigDecimal revenue
    ) {}
    
    // ========================================
    // DRIVER ANALYTICS
    // ========================================
    
    public record DriverAnalytics(
            String driverId,
            LocalDate startDate,
            LocalDate endDate,
            int totalDeliveries,
            BigDecimal totalEarnings,
            BigDecimal avgTipPerDelivery,
            int avgDeliveryTimeMinutes,
            double avgRating,
            double onTimeRate,
            List<HourlyDistribution> activeHours
    ) {}
}
