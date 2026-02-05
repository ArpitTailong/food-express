package com.foodexpress.payment.scheduler;

import com.foodexpress.payment.domain.Payment;
import com.foodexpress.payment.repository.PaymentRepository;
import com.foodexpress.payment.messaging.PaymentEventPublisherInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Scheduled jobs for payment processing.
 * 
 * Jobs:
 * 1. Mark stuck payments as failed (payments in PROCESSING for too long)
 * 2. Retry failed payments (with exponential backoff)
 * 3. Cleanup old idempotency keys (optional)
 */
@Component
public class PaymentScheduledJobs {
    
    private static final Logger log = LoggerFactory.getLogger(PaymentScheduledJobs.class);
    
    @Value("${app.payment.stuck-payment-timeout-minutes:30}")
    private int stuckPaymentTimeoutMinutes;
    
    private final PaymentRepository paymentRepository;
    private final PaymentEventPublisherInterface eventPublisher;
    
    public PaymentScheduledJobs(
            PaymentRepository paymentRepository,
            PaymentEventPublisherInterface eventPublisher) {
        this.paymentRepository = paymentRepository;
        this.eventPublisher = eventPublisher;
    }
    
    /**
     * Mark stuck payments as failed.
     * Runs every 10 minutes.
     * 
     * A payment is "stuck" if it's been in PROCESSING state for longer than
     * the configured timeout (default: 30 minutes).
     */
    @Scheduled(fixedDelayString = "${app.payment.stuck-payment-check-interval:600000}") // 10 minutes
    @Transactional(transactionManager = "transactionManager")
    public void markStuckPaymentsAsFailed() {
        log.info("Starting stuck payments check...");
        
        try {
            Instant cutoff = Instant.now().minus(stuckPaymentTimeoutMinutes, ChronoUnit.MINUTES);
            
            List<Payment> stuckPayments = paymentRepository.findStuckPayments(cutoff);
            
            if (stuckPayments.isEmpty()) {
                log.debug("No stuck payments found");
                return;
            }
            
            log.warn("Found {} stuck payments to mark as failed", stuckPayments.size());
            
            int failedCount = 0;
            for (Payment payment : stuckPayments) {
                try {
                    payment.markFailed(
                            "TIMEOUT",
                            "Payment processing timed out after %d minutes".formatted(stuckPaymentTimeoutMinutes)
                    );
                    paymentRepository.save(payment);
                    
                    // Publish failure event
                    eventPublisher.publishPaymentFailed(payment);
                    
                    failedCount++;
                    
                    log.warn("Marked stuck payment {} as failed (order: {}, processing started at: {})",
                            payment.getId(),
                            payment.getOrderId(),
                            payment.getProcessedAt());
                    
                } catch (Exception e) {
                    log.error("Failed to mark payment {} as failed: {}", 
                            payment.getId(), e.getMessage(), e);
                }
            }
            
            log.info("Marked {} out of {} stuck payments as failed", failedCount, stuckPayments.size());
            
        } catch (Exception e) {
            log.error("Error in stuck payments check: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Retry failed payments with exponential backoff.
     * Runs every 5 minutes.
     * 
     * Only retries payments that:
     * - Are in FAILED state
     * - Have not exceeded max retry attempts
     * - Last attempt was more than (2^attemptCount) minutes ago
     */
    @Scheduled(fixedDelayString = "${app.payment.retry-check-interval:300000}") // 5 minutes
    public void retryFailedPayments() {
        log.debug("Starting failed payments retry check...");
        
        try {
            // Find payments eligible for retry
            // Exponential backoff: retry after 2^attemptCount minutes
            // Attempt 1: retry after 2 minutes
            // Attempt 2: retry after 4 minutes
            // Attempt 3: retry after 8 minutes
            
            Instant now = Instant.now();
            
            List<Payment> failedPayments = paymentRepository.findRetryablePayments(
                    3, // Max attempts
                    now.minus(2, ChronoUnit.MINUTES) // At least 2 minutes since last attempt
            );
            
            if (failedPayments.isEmpty()) {
                log.debug("No failed payments eligible for retry");
                return;
            }
            
            log.info("Found {} failed payments eligible for retry", failedPayments.size());
            
            for (Payment payment : failedPayments) {
                // Check exponential backoff
                int attemptCount = payment.getAttemptCount();
                long minutesSinceLastAttempt = ChronoUnit.MINUTES.between(
                        payment.getUpdatedAt(), 
                        now
                );
                long requiredWaitMinutes = (long) Math.pow(2, attemptCount);
                
                if (minutesSinceLastAttempt < requiredWaitMinutes) {
                    log.debug("Payment {} not ready for retry yet. Wait {} more minutes",
                            payment.getId(),
                            requiredWaitMinutes - minutesSinceLastAttempt);
                    continue;
                }
                
                log.info("Retrying failed payment {} (order: {}, attempt: {}/3)",
                        payment.getId(),
                        payment.getOrderId(),
                        attemptCount + 1);
                
                // Note: In a real implementation, this would trigger the payment
                // processing flow again. For now, just log.
                // In production: Use a message queue or service call to trigger retry
            }
            
        } catch (Exception e) {
            log.error("Error in failed payments retry: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Log payment statistics.
     * Runs every hour.
     */
    @Scheduled(cron = "${app.payment.stats-cron:0 0 * * * *}") // Every hour
    public void logPaymentStatistics() {
        try {
            long createdCount = paymentRepository.countByStatus(
                    com.foodexpress.payment.domain.PaymentStatus.CREATED);
            long processingCount = paymentRepository.countByStatus(
                    com.foodexpress.payment.domain.PaymentStatus.PROCESSING);
            long successCount = paymentRepository.countByStatus(
                    com.foodexpress.payment.domain.PaymentStatus.SUCCESS);
            long failedCount = paymentRepository.countByStatus(
                    com.foodexpress.payment.domain.PaymentStatus.FAILED);
            long refundedCount = paymentRepository.countByStatus(
                    com.foodexpress.payment.domain.PaymentStatus.REFUNDED);
            
            log.info("Payment Statistics - CREATED: {}, PROCESSING: {}, SUCCESS: {}, FAILED: {}, REFUNDED: {}",
                    createdCount, processingCount, successCount, failedCount, refundedCount);
            
        } catch (Exception e) {
            log.error("Error logging payment statistics: {}", e.getMessage(), e);
        }
    }
}
