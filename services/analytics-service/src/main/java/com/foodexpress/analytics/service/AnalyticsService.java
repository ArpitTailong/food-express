package com.foodexpress.analytics.service;

import com.foodexpress.analytics.domain.*;
import com.foodexpress.analytics.dto.AnalyticsDTOs.*;
import com.foodexpress.analytics.repository.*;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Analytics Service.
 * Provides aggregated metrics and insights.
 */
@Service
public class AnalyticsService {
    
    private static final Logger log = LoggerFactory.getLogger(AnalyticsService.class);
    
    private final DailyOrderMetricsRepository metricsRepository;
    private final OrderEventRepository eventRepository;
    private final MeterRegistry meterRegistry;
    
    public AnalyticsService(
            DailyOrderMetricsRepository metricsRepository,
            OrderEventRepository eventRepository,
            MeterRegistry meterRegistry) {
        this.metricsRepository = metricsRepository;
        this.eventRepository = eventRepository;
        this.meterRegistry = meterRegistry;
    }
    
    // ========================================
    // DASHBOARD METRICS
    // ========================================
    
    @Cacheable(value = "dashboardMetrics", key = "#date")
    @Transactional(readOnly = true)
    public DashboardMetrics getDashboardMetrics(LocalDate date) {
        log.info("Getting dashboard metrics for {}", date);
        
        DailyOrderMetrics metrics = metricsRepository
                .findByMetricDateAndRestaurantIdIsNull(date)
                .orElse(new DailyOrderMetrics(date));
        
        return buildDashboardMetrics(date, metrics);
    }
    
    @Transactional(readOnly = true)
    public DashboardMetrics getDashboardMetricsForRange(LocalDate startDate, LocalDate endDate) {
        List<DailyOrderMetrics> metricsList = metricsRepository.findPlatformMetricsBetween(startDate, endDate);
        
        if (metricsList.isEmpty()) {
            return buildDashboardMetrics(startDate, new DailyOrderMetrics(startDate));
        }
        
        // Aggregate metrics
        DailyOrderMetrics aggregated = aggregateMetrics(metricsList);
        return buildDashboardMetrics(startDate, aggregated);
    }
    
    private DashboardMetrics buildDashboardMetrics(LocalDate date, DailyOrderMetrics m) {
        double completionRate = m.getTotalOrders() > 0 
                ? (double) m.getCompletedOrders() / m.getTotalOrders() * 100 : 0;
        double cancellationRate = m.getTotalOrders() > 0 
                ? (double) m.getCancelledOrders() / m.getTotalOrders() * 100 : 0;
        double repeatRate = m.getUniqueCustomers() > 0 
                ? (double) m.getRepeatCustomers() / m.getUniqueCustomers() * 100 : 0;
        
        return new DashboardMetrics(
                date,
                new OrderMetrics(
                        m.getTotalOrders(),
                        m.getCompletedOrders(),
                        m.getCancelledOrders(),
                        m.getFailedOrders(),
                        completionRate,
                        cancellationRate
                ),
                new RevenueMetrics(
                        m.getGrossRevenue(),
                        m.getNetRevenue(),
                        m.getDeliveryFees(),
                        m.getTipsCollected(),
                        m.getDiscountsGiven(),
                        m.getRefundsIssued(),
                        m.getAvgOrderValue()
                ),
                new CustomerMetrics(
                        m.getUniqueCustomers(),
                        m.getNewCustomers(),
                        m.getRepeatCustomers(),
                        repeatRate
                ),
                new PerformanceMetrics(
                        m.getAvgDeliveryTimeMinutes() != null ? m.getAvgDeliveryTimeMinutes() : 0,
                        m.getAvgPreparationTimeMinutes() != null ? m.getAvgPreparationTimeMinutes() : 0,
                        m.getAvgRestaurantRating() != null ? m.getAvgRestaurantRating().doubleValue() : 0,
                        m.getAvgDriverRating() != null ? m.getAvgDriverRating().doubleValue() : 0,
                        m.getRatingsCount()
                )
        );
    }
    
    // ========================================
    // TIME SERIES
    // ========================================
    
    @Cacheable(value = "timeSeries", key = "#metric + '_' + #startDate + '_' + #endDate")
    @Transactional(readOnly = true)
    public TimeSeriesData getTimeSeries(String metric, LocalDate startDate, LocalDate endDate) {
        List<DailyOrderMetrics> metricsList = metricsRepository.findPlatformMetricsBetween(startDate, endDate);
        
        List<DataPoint> dataPoints = metricsList.stream()
                .map(m -> new DataPoint(
                        m.getMetricDate().toString(),
                        extractMetricValue(m, metric)
                ))
                .toList();
        
        return new TimeSeriesData(metric, "DAILY", dataPoints);
    }
    
    private BigDecimal extractMetricValue(DailyOrderMetrics m, String metric) {
        return switch (metric.toLowerCase()) {
            case "orders" -> BigDecimal.valueOf(m.getTotalOrders());
            case "revenue" -> m.getGrossRevenue();
            case "aov" -> m.getAvgOrderValue();
            case "customers" -> BigDecimal.valueOf(m.getUniqueCustomers());
            default -> BigDecimal.ZERO;
        };
    }
    
    // ========================================
    // RANKINGS
    // ========================================
    
    @Cacheable(value = "restaurantRankings", key = "#startDate + '_' + #endDate + '_' + #limit")
    @Transactional(readOnly = true)
    public List<RestaurantRanking> getTopRestaurants(LocalDate startDate, LocalDate endDate, int limit) {
        List<Object[]> results = metricsRepository.findTopRestaurantsByRevenue(startDate, endDate, limit);
        
        List<RestaurantRanking> rankings = new ArrayList<>();
        int rank = 1;
        for (Object[] row : results) {
            rankings.add(new RestaurantRanking(
                    (String) row[0],
                    "Restaurant " + row[0], // TODO: Fetch from Restaurant Service
                    0, // TODO: Fetch order count
                    (BigDecimal) row[1],
                    0.0, // TODO: Fetch rating
                    rank++
            ));
        }
        return rankings;
    }
    
    // ========================================
    // REAL-TIME METRICS
    // ========================================
    
    @Transactional(readOnly = true)
    public RealTimeMetrics getRealTimeMetrics() {
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        
        long ordersLastHour = eventRepository.countEventsSince("ORDER_CREATED", oneHourAgo);
        
        // These would typically come from other services or real-time cache
        return new RealTimeMetrics(
                System.currentTimeMillis(),
                (int) ordersLastHour,
                0, // TODO: Active orders from Order Service
                0, // TODO: Available drivers from User Service
                0, // TODO: Pending pickups
                BigDecimal.ZERO, // TODO: Calculate from events
                0.0 // TODO: Calculate avg delivery time
        );
    }
    
    // ========================================
    // PERIOD REPORT
    // ========================================
    
    @Transactional(readOnly = true)
    public PeriodReport generatePeriodReport(LocalDate startDate, LocalDate endDate) {
        List<DailyOrderMetrics> metricsList = metricsRepository.findPlatformMetricsBetween(startDate, endDate);
        
        if (metricsList.isEmpty()) {
            DailyOrderMetrics empty = new DailyOrderMetrics(startDate);
            return new PeriodReport(
                    startDate, endDate,
                    new OrderMetrics(0, 0, 0, 0, 0, 0),
                    new RevenueMetrics(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 
                                       BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO),
                    new CustomerMetrics(0, 0, 0, 0),
                    new PerformanceMetrics(0, 0, 0, 0, 0),
                    List.of(),
                    Map.of(),
                    List.of("No data available for this period")
            );
        }
        
        DailyOrderMetrics aggregated = aggregateMetrics(metricsList);
        DashboardMetrics dashboard = buildDashboardMetrics(startDate, aggregated);
        
        List<DailyTrend> trends = metricsList.stream()
                .map(m -> new DailyTrend(
                        m.getMetricDate(),
                        m.getTotalOrders(),
                        m.getGrossRevenue(),
                        m.getNewCustomers()
                ))
                .toList();
        
        List<String> insights = generateInsights(metricsList, aggregated);
        
        return new PeriodReport(
                startDate, endDate,
                dashboard.orders(),
                dashboard.revenue(),
                dashboard.customers(),
                dashboard.performance(),
                trends,
                Map.of(), // TODO: Revenue by restaurant
                insights
        );
    }
    
    // ========================================
    // RESTAURANT ANALYTICS
    // ========================================
    
    @Transactional(readOnly = true)
    public RestaurantAnalytics getRestaurantAnalytics(String restaurantId, LocalDate startDate, LocalDate endDate) {
        List<DailyOrderMetrics> metricsList = metricsRepository.findRestaurantMetricsBetween(restaurantId, startDate, endDate);
        
        DailyOrderMetrics aggregated = aggregateMetrics(metricsList);
        
        return new RestaurantAnalytics(
                restaurantId,
                startDate,
                endDate,
                aggregated.getTotalOrders(),
                aggregated.getGrossRevenue(),
                aggregated.getAvgOrderValue(),
                aggregated.getAvgRestaurantRating() != null ? aggregated.getAvgRestaurantRating().doubleValue() : 0,
                aggregated.getAvgPreparationTimeMinutes() != null ? aggregated.getAvgPreparationTimeMinutes() : 0,
                List.of(), // TODO: Peak hours analysis
                List.of(), // TODO: Top items
                aggregated.getUniqueCustomers() > 0 
                        ? (double) aggregated.getRepeatCustomers() / aggregated.getUniqueCustomers() * 100 : 0
        );
    }
    
    // ========================================
    // HELPER METHODS
    // ========================================
    
    private DailyOrderMetrics aggregateMetrics(List<DailyOrderMetrics> metricsList) {
        DailyOrderMetrics aggregated = new DailyOrderMetrics(LocalDate.now());
        
        int totalOrders = 0, completedOrders = 0, cancelledOrders = 0, failedOrders = 0;
        BigDecimal grossRevenue = BigDecimal.ZERO;
        BigDecimal netRevenue = BigDecimal.ZERO;
        BigDecimal deliveryFees = BigDecimal.ZERO;
        BigDecimal tips = BigDecimal.ZERO;
        BigDecimal discounts = BigDecimal.ZERO;
        BigDecimal refunds = BigDecimal.ZERO;
        int uniqueCustomers = 0, newCustomers = 0, repeatCustomers = 0;
        int deliveryTimeSum = 0, prepTimeSum = 0, timeCount = 0;
        
        for (DailyOrderMetrics m : metricsList) {
            totalOrders += m.getTotalOrders();
            completedOrders += m.getCompletedOrders();
            cancelledOrders += m.getCancelledOrders();
            failedOrders += m.getFailedOrders();
            grossRevenue = grossRevenue.add(m.getGrossRevenue());
            netRevenue = netRevenue.add(m.getNetRevenue());
            deliveryFees = deliveryFees.add(m.getDeliveryFees());
            tips = tips.add(m.getTipsCollected());
            discounts = discounts.add(m.getDiscountsGiven());
            refunds = refunds.add(m.getRefundsIssued());
            uniqueCustomers += m.getUniqueCustomers();
            newCustomers += m.getNewCustomers();
            repeatCustomers += m.getRepeatCustomers();
            
            if (m.getAvgDeliveryTimeMinutes() != null) {
                deliveryTimeSum += m.getAvgDeliveryTimeMinutes() * m.getCompletedOrders();
                prepTimeSum += (m.getAvgPreparationTimeMinutes() != null ? m.getAvgPreparationTimeMinutes() : 0) * m.getCompletedOrders();
                timeCount += m.getCompletedOrders();
            }
        }
        
        // Set aggregated values via increment methods
        for (int i = 0; i < completedOrders; i++) aggregated.incrementOrders("DELIVERED");
        for (int i = 0; i < cancelledOrders; i++) aggregated.incrementOrders("CANCELLED");
        for (int i = 0; i < failedOrders; i++) aggregated.incrementOrders("FAILED");
        
        aggregated.addRevenue(grossRevenue, deliveryFees, tips, discounts);
        aggregated.setUniqueCustomers(uniqueCustomers);
        aggregated.setNewCustomers(newCustomers);
        aggregated.setRepeatCustomers(repeatCustomers);
        
        if (timeCount > 0) {
            aggregated.setAvgDeliveryTimeMinutes(deliveryTimeSum / timeCount);
            aggregated.setAvgPreparationTimeMinutes(prepTimeSum / timeCount);
        }
        
        return aggregated;
    }
    
    private List<String> generateInsights(List<DailyOrderMetrics> metricsList, DailyOrderMetrics aggregated) {
        List<String> insights = new ArrayList<>();
        
        if (aggregated.getTotalOrders() > 0) {
            double completionRate = (double) aggregated.getCompletedOrders() / aggregated.getTotalOrders() * 100;
            if (completionRate > 95) {
                insights.add("Excellent order completion rate of " + String.format("%.1f", completionRate) + "%");
            } else if (completionRate < 80) {
                insights.add("Order completion rate is below target at " + String.format("%.1f", completionRate) + "%");
            }
        }
        
        if (aggregated.getUniqueCustomers() > 0 && aggregated.getRepeatCustomers() > 0) {
            double repeatRate = (double) aggregated.getRepeatCustomers() / aggregated.getUniqueCustomers() * 100;
            if (repeatRate > 30) {
                insights.add("Strong customer retention with " + String.format("%.1f", repeatRate) + "% repeat customers");
            }
        }
        
        if (aggregated.getAvgOrderValue().compareTo(new BigDecimal("500")) > 0) {
            insights.add("Average order value is healthy at ₹" + aggregated.getAvgOrderValue());
        }
        
        return insights;
    }
}
