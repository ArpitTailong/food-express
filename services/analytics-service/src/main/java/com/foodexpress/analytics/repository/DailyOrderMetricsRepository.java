package com.foodexpress.analytics.repository;

import com.foodexpress.analytics.domain.DailyOrderMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyOrderMetricsRepository extends JpaRepository<DailyOrderMetrics, Long> {
    
    Optional<DailyOrderMetrics> findByMetricDateAndRestaurantIdIsNull(LocalDate date);
    
    Optional<DailyOrderMetrics> findByMetricDateAndRestaurantId(LocalDate date, String restaurantId);
    
    // Platform-wide metrics for date range
    @Query("""
            SELECT m FROM DailyOrderMetrics m 
            WHERE m.restaurantId IS NULL 
            AND m.metricDate BETWEEN :startDate AND :endDate 
            ORDER BY m.metricDate
            """)
    List<DailyOrderMetrics> findPlatformMetricsBetween(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
    
    // Restaurant-specific metrics
    @Query("""
            SELECT m FROM DailyOrderMetrics m 
            WHERE m.restaurantId = :restaurantId 
            AND m.metricDate BETWEEN :startDate AND :endDate 
            ORDER BY m.metricDate
            """)
    List<DailyOrderMetrics> findRestaurantMetricsBetween(
            @Param("restaurantId") String restaurantId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
    
    // Aggregations
    @Query("""
            SELECT SUM(m.totalOrders) FROM DailyOrderMetrics m 
            WHERE m.restaurantId IS NULL 
            AND m.metricDate BETWEEN :startDate AND :endDate
            """)
    Integer sumTotalOrders(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    @Query("""
            SELECT SUM(m.grossRevenue) FROM DailyOrderMetrics m 
            WHERE m.restaurantId IS NULL 
            AND m.metricDate BETWEEN :startDate AND :endDate
            """)
    BigDecimal sumGrossRevenue(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    // Top restaurants by revenue
    @Query("""
            SELECT m.restaurantId, SUM(m.grossRevenue) as revenue 
            FROM DailyOrderMetrics m 
            WHERE m.restaurantId IS NOT NULL 
            AND m.metricDate BETWEEN :startDate AND :endDate 
            GROUP BY m.restaurantId 
            ORDER BY revenue DESC 
            LIMIT :limit
            """)
    List<Object[]> findTopRestaurantsByRevenue(
            @Param("startDate") LocalDate startDate, 
            @Param("endDate") LocalDate endDate,
            @Param("limit") int limit);
    
    // Top restaurants by orders
    @Query("""
            SELECT m.restaurantId, SUM(m.totalOrders) as orders 
            FROM DailyOrderMetrics m 
            WHERE m.restaurantId IS NOT NULL 
            AND m.metricDate BETWEEN :startDate AND :endDate 
            GROUP BY m.restaurantId 
            ORDER BY orders DESC 
            LIMIT :limit
            """)
    List<Object[]> findTopRestaurantsByOrders(
            @Param("startDate") LocalDate startDate, 
            @Param("endDate") LocalDate endDate,
            @Param("limit") int limit);
}
