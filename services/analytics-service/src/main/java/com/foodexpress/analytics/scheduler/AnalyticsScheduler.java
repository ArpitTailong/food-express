package com.foodexpress.analytics.scheduler;

import com.foodexpress.analytics.domain.OrderEvent;
import com.foodexpress.analytics.repository.DailyOrderMetricsRepository;
import com.foodexpress.analytics.repository.OrderEventRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled Tasks for Analytics Service.
 * Handles batch processing, cleanup, and daily aggregation.
 */
@Component
public class AnalyticsScheduler {
    
    private static final Logger log = LoggerFactory.getLogger(AnalyticsScheduler.class);
    
    private final OrderEventRepository eventRepository;
    private final DailyOrderMetricsRepository metricsRepository;
    private final Counter eventsProcessedCounter;
    private final Counter eventsDeletedCounter;
    
    public AnalyticsScheduler(
            OrderEventRepository eventRepository,
            DailyOrderMetricsRepository metricsRepository,
            MeterRegistry meterRegistry) {
        this.eventRepository = eventRepository;
        this.metricsRepository = metricsRepository;
        
        this.eventsProcessedCounter = Counter.builder("analytics.events.processed")
                .description("Number of events processed by scheduler")
                .register(meterRegistry);
        
        this.eventsDeletedCounter = Counter.builder("analytics.events.deleted")
                .description("Number of old events deleted")
                .register(meterRegistry);
    }
    
    /**
     * Process any unprocessed events every minute.
     * This serves as a fallback for events that weren't processed in real-time.
     */
    @Scheduled(fixedRate = 60000) // Every minute
    @Transactional
    public void processUnprocessedEvents() {
        log.debug("Starting unprocessed events batch job");
        
        Page<OrderEvent> unprocessedPage = eventRepository.findUnprocessedEvents(PageRequest.of(0, 100));
        List<OrderEvent> unprocessedEvents = unprocessedPage.getContent();
        
        if (unprocessedEvents.isEmpty()) {
            return;
        }
        
        log.info("Processing {} unprocessed events", unprocessedEvents.size());
        
        for (OrderEvent event : unprocessedEvents) {
            try {
                event.markProcessed();
                eventRepository.save(event);
                eventsProcessedCounter.increment();
            } catch (Exception e) {
                log.error("Error processing event {}: {}", event.getId(), e.getMessage());
            }
        }
    }
    
    /**
     * Daily cleanup of old processed events.
     * Runs at 3 AM every day.
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupOldEvents() {
        log.info("Starting daily event cleanup");
        
        // Delete events older than 30 days
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);
        
        int deletedCount = eventRepository.deleteOldProcessedEvents(cutoffDate);
        eventsDeletedCounter.increment(deletedCount);
        
        log.info("Deleted {} old processed events", deletedCount);
    }
    
    /**
     * Daily metrics finalization.
     * Runs at 1 AM to finalize previous day's metrics.
     */
    @Scheduled(cron = "0 0 1 * * *")
    @Transactional
    public void finalizeDailyMetrics() {
        log.info("Starting daily metrics finalization");
        
        LocalDate yesterday = LocalDate.now().minusDays(1);
        
        // Get all metrics for yesterday
        var metricsList = metricsRepository.findPlatformMetricsBetween(yesterday, yesterday);
        
        if (metricsList.isEmpty()) {
            log.info("No metrics to finalize for {}", yesterday);
            return;
        }
        
        for (var metrics : metricsList) {
            // Calculate final averages
            if (metrics.getTotalOrders() > 0) {
                metrics.calculateAverages();
            }
            metricsRepository.save(metrics);
        }
        
        log.info("Finalized {} metric records for {}", metricsList.size(), yesterday);
    }
    
    /**
     * Weekly report generation.
     * Runs every Monday at 6 AM.
     */
    @Scheduled(cron = "0 0 6 * * MON")
    public void generateWeeklyReport() {
        log.info("Generating weekly report");
        
        LocalDate endDate = LocalDate.now().minusDays(1);
        LocalDate startDate = endDate.minusDays(6);
        
        // Log weekly summary
        var weeklyMetrics = metricsRepository.findPlatformMetricsBetween(startDate, endDate);
        
        int totalOrders = weeklyMetrics.stream()
                .mapToInt(m -> m.getTotalOrders())
                .sum();
        
        var totalRevenue = weeklyMetrics.stream()
                .map(m -> m.getGrossRevenue())
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        
        log.info("Weekly Summary ({} to {}): {} orders, ₹{} revenue",
                startDate, endDate, totalOrders, totalRevenue);
    }
}
