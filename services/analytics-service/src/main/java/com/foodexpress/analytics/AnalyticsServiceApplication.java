package com.foodexpress.analytics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Analytics Service Application.
 * 
 * Real-time analytics and business intelligence:
 * - Event stream processing from all services
 * - Aggregated metrics and KPIs
 * - Historical trend analysis
 * - Performance dashboards
 * 
 * Consumes events from:
 * - order-events, order-analytics
 * - payment-events
 * - user-events
 */
@SpringBootApplication
@EnableCaching
@EnableScheduling
public class AnalyticsServiceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(AnalyticsServiceApplication.class, args);
    }
}
