package com.foodexpress.analytics.controller;

import com.foodexpress.analytics.dto.AnalyticsDTOs.*;
import com.foodexpress.analytics.service.AnalyticsService;
import com.foodexpress.common.dto.ApiResponse;
import io.micrometer.core.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Analytics REST Controller.
 * Provides endpoints for dashboards, reports, and real-time metrics.
 */
@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {
    
    private static final Logger log = LoggerFactory.getLogger(AnalyticsController.class);
    
    private final AnalyticsService analyticsService;
    
    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }
    
    // ========================================
    // DASHBOARD
    // ========================================
    
    /**
     * Get today's dashboard metrics.
     */
    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESTAURANT_OWNER')")
    @Timed(value = "analytics.dashboard.today", description = "Time to get today's dashboard")
    public ResponseEntity<ApiResponse<DashboardMetrics>> getDashboard() {
        log.info("Getting today's dashboard metrics");
        
        DashboardMetrics metrics = analyticsService.getDashboardMetrics(LocalDate.now());
        
        return ResponseEntity.ok(ApiResponse.success(metrics));
    }
    
    /**
     * Get dashboard metrics for a specific date.
     */
    @GetMapping("/dashboard/{date}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESTAURANT_OWNER')")
    @Timed(value = "analytics.dashboard.date", description = "Time to get dashboard for date")
    public ResponseEntity<ApiResponse<DashboardMetrics>> getDashboardForDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        log.info("Getting dashboard metrics for {}", date);
        
        DashboardMetrics metrics = analyticsService.getDashboardMetrics(date);
        
        return ResponseEntity.ok(ApiResponse.success(metrics));
    }
    
    /**
     * Get dashboard metrics for a date range.
     */
    @GetMapping("/dashboard/range")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESTAURANT_OWNER')")
    @Timed(value = "analytics.dashboard.range", description = "Time to get dashboard for range")
    public ResponseEntity<ApiResponse<DashboardMetrics>> getDashboardForRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        log.info("Getting dashboard metrics for {} to {}", startDate, endDate);
        
        DashboardMetrics metrics = analyticsService.getDashboardMetricsForRange(startDate, endDate);
        
        return ResponseEntity.ok(ApiResponse.success(metrics));
    }
    
    // ========================================
    // TIME SERIES
    // ========================================
    
    /**
     * Get time series data for a specific metric.
     */
    @GetMapping("/time-series/{metric}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESTAURANT_OWNER')")
    @Timed(value = "analytics.timeseries", description = "Time to get time series data")
    public ResponseEntity<ApiResponse<TimeSeriesData>> getTimeSeries(
            @PathVariable String metric,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        log.info("Getting time series for {} from {} to {}", metric, startDate, endDate);
        
        TimeSeriesData data = analyticsService.getTimeSeries(metric, startDate, endDate);
        
        return ResponseEntity.ok(ApiResponse.success(data));
    }
    
    // ========================================
    // REAL-TIME
    // ========================================
    
    /**
     * Get real-time metrics.
     */
    @GetMapping("/realtime")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESTAURANT_OWNER')")
    @Timed(value = "analytics.realtime", description = "Time to get real-time metrics")
    public ResponseEntity<ApiResponse<RealTimeMetrics>> getRealTimeMetrics() {
        log.info("Getting real-time metrics");
        
        RealTimeMetrics metrics = analyticsService.getRealTimeMetrics();
        
        return ResponseEntity.ok(ApiResponse.success(metrics));
    }
    
    // ========================================
    // RANKINGS
    // ========================================
    
    /**
     * Get top restaurants by revenue.
     */
    @GetMapping("/rankings/restaurants")
    @PreAuthorize("hasRole('ADMIN')")
    @Timed(value = "analytics.rankings.restaurants", description = "Time to get restaurant rankings")
    public ResponseEntity<ApiResponse<List<RestaurantRanking>>> getTopRestaurants(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "10") int limit) {
        
        log.info("Getting top {} restaurants from {} to {}", limit, startDate, endDate);
        
        List<RestaurantRanking> rankings = analyticsService.getTopRestaurants(startDate, endDate, limit);
        
        return ResponseEntity.ok(ApiResponse.success(rankings));
    }
    
    // ========================================
    // RESTAURANT ANALYTICS
    // ========================================
    
    /**
     * Get analytics for a specific restaurant.
     */
    @GetMapping("/restaurants/{restaurantId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESTAURANT_OWNER')")
    @Timed(value = "analytics.restaurant", description = "Time to get restaurant analytics")
    public ResponseEntity<ApiResponse<RestaurantAnalytics>> getRestaurantAnalytics(
            @PathVariable String restaurantId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        log.info("Getting analytics for restaurant {} from {} to {}", restaurantId, startDate, endDate);
        
        RestaurantAnalytics analytics = analyticsService.getRestaurantAnalytics(restaurantId, startDate, endDate);
        
        return ResponseEntity.ok(ApiResponse.success(analytics));
    }
    
    // ========================================
    // REPORTS
    // ========================================
    
    /**
     * Generate period report.
     */
    @GetMapping("/reports/period")
    @PreAuthorize("hasRole('ADMIN')")
    @Timed(value = "analytics.reports.period", description = "Time to generate period report")
    public ResponseEntity<ApiResponse<PeriodReport>> getPeriodReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        log.info("Generating period report from {} to {}", startDate, endDate);
        
        PeriodReport report = analyticsService.generatePeriodReport(startDate, endDate);
        
        return ResponseEntity.ok(ApiResponse.success(report));
    }
    
    // ========================================
    // HEALTH CHECK
    // ========================================
    
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Analytics Service is running");
    }
}
