package com.foodexpress.order.repository;

import com.foodexpress.order.domain.Order;
import com.foodexpress.order.domain.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Order entity with optimized queries.
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, String> {
    
    // ========================================
    // CUSTOMER QUERIES
    // ========================================
    
    Page<Order> findByCustomerIdOrderByCreatedAtDesc(String customerId, Pageable pageable);
    
    Page<Order> findByCustomerIdAndStatusOrderByCreatedAtDesc(
            String customerId, OrderStatus status, Pageable pageable);
    
    @Query("""
            SELECT o FROM Order o 
            WHERE o.customerId = :customerId 
              AND o.status NOT IN ('DELIVERED', 'CANCELLED', 'FAILED')
            ORDER BY o.createdAt DESC
            """)
    List<Order> findActiveOrdersByCustomer(@Param("customerId") String customerId);
    
    // ========================================
    // RESTAURANT QUERIES
    // ========================================
    
    Page<Order> findByRestaurantIdOrderByCreatedAtDesc(String restaurantId, Pageable pageable);
    
    @Query("""
            SELECT o FROM Order o 
            WHERE o.restaurantId = :restaurantId 
              AND o.status IN ('CONFIRMED', 'PREPARING', 'READY_FOR_PICKUP')
            ORDER BY o.createdAt ASC
            """)
    List<Order> findActiveOrdersByRestaurant(@Param("restaurantId") String restaurantId);
    
    @Query("""
            SELECT o FROM Order o 
            WHERE o.restaurantId = :restaurantId 
              AND o.status = 'CONFIRMED'
            ORDER BY o.confirmedAt ASC
            """)
    List<Order> findPendingOrdersForRestaurant(@Param("restaurantId") String restaurantId);
    
    // ========================================
    // DRIVER QUERIES
    // ========================================
    
    Page<Order> findByDriverIdOrderByCreatedAtDesc(String driverId, Pageable pageable);
    
    @Query("""
            SELECT o FROM Order o 
            WHERE o.driverId = :driverId 
              AND o.status = 'OUT_FOR_DELIVERY'
            """)
    List<Order> findActiveDeliveriesForDriver(@Param("driverId") String driverId);
    
    @Query("""
            SELECT o FROM Order o 
            WHERE o.status = 'READY_FOR_PICKUP' 
              AND o.driverId IS NULL
            ORDER BY o.readyAt ASC
            """)
    List<Order> findOrdersAwaitingDriverAssignment();
    
    // ========================================
    // STATUS QUERIES
    // ========================================
    
    List<Order> findByStatusOrderByCreatedAtAsc(OrderStatus status);
    
    @Query("""
            SELECT o FROM Order o 
            WHERE o.status IN :statuses
            ORDER BY o.createdAt ASC
            """)
    List<Order> findByStatusIn(@Param("statuses") List<OrderStatus> statuses);
    
    // ========================================
    // STUCK ORDER DETECTION
    // ========================================
    
    @Query("""
            SELECT o FROM Order o 
            WHERE o.status = 'PAYMENT_PENDING' 
              AND o.updatedAt < :cutoff
            """)
    List<Order> findStuckPaymentPendingOrders(@Param("cutoff") Instant cutoff);
    
    @Query("""
            SELECT o FROM Order o 
            WHERE o.status = 'CONFIRMED' 
              AND o.confirmedAt < :cutoff
            """)
    List<Order> findStuckConfirmedOrders(@Param("cutoff") Instant cutoff);
    
    @Query("""
            SELECT o FROM Order o 
            WHERE o.status = 'PREPARING' 
              AND o.preparingAt < :cutoff
            """)
    List<Order> findStuckPreparingOrders(@Param("cutoff") Instant cutoff);
    
    // ========================================
    // LOCKING
    // ========================================
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.id = :id")
    Optional<Order> findByIdWithLock(@Param("id") String id);
    
    // ========================================
    // ANALYTICS
    // ========================================
    
    long countByStatus(OrderStatus status);
    
    long countByCustomerId(String customerId);
    
    long countByRestaurantId(String restaurantId);
    
    @Query("""
            SELECT COUNT(o) FROM Order o 
            WHERE o.status = :status 
              AND o.createdAt BETWEEN :start AND :end
            """)
    long countByStatusBetween(
            @Param("status") OrderStatus status,
            @Param("start") Instant start,
            @Param("end") Instant end);
    
    @Query("""
            SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o 
            WHERE o.status = 'DELIVERED' 
              AND o.deliveredAt BETWEEN :start AND :end
            """)
    java.math.BigDecimal getTotalRevenue(
            @Param("start") Instant start,
            @Param("end") Instant end);
    
    @Query("""
            SELECT COALESCE(AVG(o.totalAmount), 0) FROM Order o 
            WHERE o.status = 'DELIVERED' 
              AND o.deliveredAt BETWEEN :start AND :end
            """)
    java.math.BigDecimal getAverageOrderValue(
            @Param("start") Instant start,
            @Param("end") Instant end);
}
