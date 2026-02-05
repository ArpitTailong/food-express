package com.foodexpress.analytics.repository;

import com.foodexpress.analytics.domain.OrderEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderEventRepository extends JpaRepository<OrderEvent, String> {
    
    // Unprocessed events for batch processing
    @Query("SELECT e FROM OrderEvent e WHERE e.processed = false ORDER BY e.eventTime ASC")
    Page<OrderEvent> findUnprocessedEvents(Pageable pageable);
    
    // Events by order
    List<OrderEvent> findByOrderIdOrderByEventTimeAsc(String orderId);
    
    // Events by type in time range
    @Query("""
            SELECT e FROM OrderEvent e 
            WHERE e.eventType = :eventType 
            AND e.eventTime BETWEEN :start AND :end
            """)
    List<OrderEvent> findByEventTypeAndTimeRange(
            @Param("eventType") String eventType,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);
    
    // Count events in last hour
    @Query("SELECT COUNT(e) FROM OrderEvent e WHERE e.eventType = :eventType AND e.eventTime > :since")
    long countEventsSince(@Param("eventType") String eventType, @Param("since") LocalDateTime since);
    
    // Cleanup old processed events
    @Modifying
    @Query("DELETE FROM OrderEvent e WHERE e.processed = true AND e.eventTime < :before")
    int deleteOldProcessedEvents(@Param("before") LocalDateTime before);
}
