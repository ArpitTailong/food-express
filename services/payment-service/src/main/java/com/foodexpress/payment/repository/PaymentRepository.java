package com.foodexpress.payment.repository;

import com.foodexpress.payment.domain.Payment;
import com.foodexpress.payment.domain.PaymentStatus;
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
 * Repository for Payment entity with optimized queries for payment processing.
 * Includes pessimistic locking support for concurrent payment operations.
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, String> {
    
    // ========================================
    // IDEMPOTENCY SUPPORT
    // ========================================
    
    /**
     * Find existing payment by idempotency key (for duplicate detection)
     */
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);
    
    /**
     * Check if payment with idempotency key exists (faster than findBy)
     */
    boolean existsByIdempotencyKey(String idempotencyKey);
    
    // ========================================
    // ORDER QUERIES
    // ========================================
    
    /**
     * Find all payments for an order (including retries)
     */
    List<Payment> findByOrderIdOrderByCreatedAtDesc(String orderId);
    
    /**
     * Find the latest payment for an order
     */
    Optional<Payment> findFirstByOrderIdOrderByCreatedAtDesc(String orderId);
    
    /**
     * Find successful payment for an order
     */
    @Query("SELECT p FROM Payment p WHERE p.orderId = :orderId AND p.status = 'SUCCESS'")
    Optional<Payment> findSuccessfulPaymentForOrder(@Param("orderId") String orderId);
    
    // ========================================
    // CUSTOMER QUERIES
    // ========================================
    
    /**
     * Find all payments by customer with pagination
     */
    Page<Payment> findByCustomerIdOrderByCreatedAtDesc(String customerId, Pageable pageable);
    
    /**
     * Find customer payments by status
     */
    Page<Payment> findByCustomerIdAndStatusOrderByCreatedAtDesc(
            String customerId, PaymentStatus status, Pageable pageable);
    
    // ========================================
    // STATUS QUERIES
    // ========================================
    
    /**
     * Find payments by status (for background jobs)
     */
    List<Payment> findByStatusOrderByCreatedAtAsc(PaymentStatus status);
    
    /**
     * Find stuck payments (in PROCESSING for too long)
     */
    @Query("SELECT p FROM Payment p WHERE p.status = 'PROCESSING' AND p.processedAt < :cutoff")
    List<Payment> findStuckPayments(@Param("cutoff") Instant cutoff);
    
    /**
     * Find failed payments eligible for retry
     */
    @Query("""
            SELECT p FROM Payment p 
            WHERE p.status = 'FAILED' 
              AND p.attemptCount < :maxAttempts 
              AND p.updatedAt < :retryAfter
            ORDER BY p.attemptCount ASC, p.updatedAt ASC
            """)
    List<Payment> findRetryablePayments(
            @Param("maxAttempts") int maxAttempts,
            @Param("retryAfter") Instant retryAfter);
    
    // ========================================
    // PESSIMISTIC LOCKING (for concurrent updates)
    // ========================================
    
    /**
     * Find and lock payment by ID (PESSIMISTIC_WRITE)
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p WHERE p.id = :id")
    Optional<Payment> findByIdWithLock(@Param("id") String id);
    
    /**
     * Find and lock payment by idempotency key
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p WHERE p.idempotencyKey = :key")
    Optional<Payment> findByIdempotencyKeyWithLock(@Param("key") String key);
    
    // ========================================
    // BULK STATUS UPDATES
    // ========================================
    
    /**
     * Mark stuck payments as failed (batch operation)
     */
    @Modifying
    @Query("""
            UPDATE Payment p 
            SET p.status = 'FAILED', 
                p.errorCode = 'TIMEOUT', 
                p.errorMessage = 'Payment processing timed out'
            WHERE p.status = 'PROCESSING' 
              AND p.processedAt < :cutoff
            """)
    int markStuckPaymentsAsFailed(@Param("cutoff") Instant cutoff);
    
    // ========================================
    // ANALYTICS QUERIES
    // ========================================
    
    /**
     * Count payments by status
     */
    long countByStatus(PaymentStatus status);
    
    /**
     * Count payments by status in date range
     */
    @Query("""
            SELECT COUNT(p) FROM Payment p 
            WHERE p.status = :status 
              AND p.createdAt BETWEEN :start AND :end
            """)
    long countByStatusBetween(
            @Param("status") PaymentStatus status,
            @Param("start") Instant start,
            @Param("end") Instant end);
    
    /**
     * Get total successful payment amount in date range
     */
    @Query("""
            SELECT COALESCE(SUM(p.amount), 0) FROM Payment p 
            WHERE p.status = 'SUCCESS' 
              AND p.completedAt BETWEEN :start AND :end
            """)
    java.math.BigDecimal getTotalSuccessfulAmount(
            @Param("start") Instant start,
            @Param("end") Instant end);
}
