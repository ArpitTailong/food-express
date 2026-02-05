package com.foodexpress.payment.service;

import com.foodexpress.payment.domain.Payment;
import com.foodexpress.payment.domain.PaymentStatus;
import com.foodexpress.payment.dto.PaymentDTOs;
import com.foodexpress.payment.dto.PaymentDTOs.*;
import com.foodexpress.payment.gateway.PaymentGateway;
import com.foodexpress.payment.messaging.PaymentEventPublisherInterface;
import com.foodexpress.payment.repository.PaymentRepository;
import com.foodexpress.payment.service.IdempotencyOperations.IdempotencyResult;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Core Payment Service handling payment lifecycle.
 * 
 * Features:
 * - Idempotent payment creation (via IdempotencyService)
 * - State machine transitions
 * - Circuit breaker for gateway calls
 * - Retry with exponential backoff
 * - Event publishing for saga coordination
 * - Comprehensive metrics
 */
@Service
public class PaymentService {
    
    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    
    private static final Duration IDEMPOTENCY_TTL = Duration.ofHours(24);
    private static final int MAX_RETRY_ATTEMPTS = 3;
    
    private final PaymentRepository paymentRepository;
    private final IdempotencyOperations idempotencyService;
    private final PaymentGateway paymentGateway;
    private final PaymentEventPublisherInterface eventPublisher;
    
    // Metrics
    private final Counter paymentsCreated;
    private final Counter paymentsSuccessful;
    private final Counter paymentsFailed;
    private final Counter paymentsRefunded;
    private final Timer paymentProcessingTimer;
    
    public PaymentService(
            PaymentRepository paymentRepository,
            IdempotencyOperations idempotencyService,
            PaymentGateway paymentGateway,
            PaymentEventPublisherInterface eventPublisher,
            MeterRegistry meterRegistry) {
        
        this.paymentRepository = paymentRepository;
        this.idempotencyService = idempotencyService;
        this.paymentGateway = paymentGateway;
        this.eventPublisher = eventPublisher;
        
        // Initialize metrics
        this.paymentsCreated = Counter.builder("payments.created")
                .description("Total payments created")
                .register(meterRegistry);
        this.paymentsSuccessful = Counter.builder("payments.successful")
                .description("Successful payments")
                .register(meterRegistry);
        this.paymentsFailed = Counter.builder("payments.failed")
                .description("Failed payments")
                .register(meterRegistry);
        this.paymentsRefunded = Counter.builder("payments.refunded")
                .description("Refunded payments")
                .register(meterRegistry);
        this.paymentProcessingTimer = Timer.builder("payments.processing.time")
                .description("Payment processing time")
                .register(meterRegistry);
    }
    
    // ========================================
    // PAYMENT CREATION (IDEMPOTENT)
    // ========================================
    
    /**
     * Create and process a payment with idempotency guarantee.
     * 
     * @param request Payment request
     * @param idempotencyKey Unique key for duplicate detection
     * @param correlationId Trace correlation ID
     * @return CreatePaymentResponse with payment details
     */
    @Transactional(transactionManager = "transactionManager")
    public CreatePaymentResponse createPayment(
            CreatePaymentRequest request, 
            String idempotencyKey,
            String correlationId) {
        
        log.info("Creating payment for order {} with idempotency key {}", 
                request.orderId(), idempotencyKey);
        
        // Execute with idempotency guarantee
        IdempotencyResult<CreatePaymentResponse> result = idempotencyService.executeIdempotent(
                idempotencyKey,
                CreatePaymentResponse.class,
                () -> processNewPayment(request, idempotencyKey, correlationId),
                IDEMPOTENCY_TTL
        );
        
        if (result.isDuplicate()) {
            log.info("Returning cached response for duplicate request: {}", idempotencyKey);
        }
        
        return result.response();
    }
    
    /**
     * Internal method to process a new payment (called within idempotency wrapper)
     */
    private CreatePaymentResponse processNewPayment(
            CreatePaymentRequest request,
            String idempotencyKey,
            String correlationId) {
        
        paymentsCreated.increment();
        
        // Create payment entity
        Payment payment = new Payment(
                request.orderId(),
                request.customerId(),
                idempotencyKey,
                request.amount(),
                request.currency(),
                request.paymentMethod()
        );
        payment.setCorrelationId(correlationId);
        
        // Save initial state
        final Payment savedPayment = paymentRepository.save(payment);
        log.info("Created payment {} for order {}", savedPayment.getId(), request.orderId());
        
        // Publish payment created event
        eventPublisher.publishPaymentCreated(savedPayment);
        
        // Process payment with gateway
        return paymentProcessingTimer.record(() -> 
                processWithGateway(savedPayment, request.gatewayToken()));
    }
    
    /**
     * Process payment through the payment gateway.
     * Uses circuit breaker and retry patterns.
     */
    @CircuitBreaker(name = "paymentGateway", fallbackMethod = "gatewayFallback")
    @Retry(name = "paymentGateway")
    private CreatePaymentResponse processWithGateway(Payment payment, String gatewayToken) {
        
        // Update status to PROCESSING
        payment.startProcessing(gatewayToken);
        payment = paymentRepository.save(payment);
        
        // Call payment gateway
        GatewayRequest gatewayRequest = new GatewayRequest(
                payment.getId(),
                payment.getOrderId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getPaymentMethod(),
                gatewayToken,
                payment.getCustomerId(),
                payment.getIdempotencyKey(),
                null
        );
        
        GatewayResponse gatewayResponse = paymentGateway.charge(gatewayRequest);
        
        // Handle response
        if (gatewayResponse.requiresAction()) {
            // 3D Secure or additional authentication required
            log.info("Payment {} requires additional action", payment.getId());
            return createPendingResponse(payment, gatewayResponse);
        }
        
        if (gatewayResponse.success()) {
            // Payment successful
            payment.markSuccess(
                    gatewayResponse.transactionId(),
                    gatewayResponse.responseCode()
            );
            payment.setCardLastFour(gatewayResponse.cardLastFour());
            payment.setCardBrand(gatewayResponse.cardBrand());
            
            payment = paymentRepository.save(payment);
            paymentsSuccessful.increment();
            
            log.info("Payment {} completed successfully. Transaction: {}", 
                    payment.getId(), gatewayResponse.transactionId());
            
            // Publish success event
            eventPublisher.publishPaymentCompleted(payment);
            
            return createSuccessResponse(payment);
        } else {
            // Payment failed
            payment.markFailed(
                    gatewayResponse.errorCode(),
                    gatewayResponse.errorMessage()
            );
            payment = paymentRepository.save(payment);
            paymentsFailed.increment();
            
            log.warn("Payment {} failed: {} - {}", 
                    payment.getId(), gatewayResponse.errorCode(), gatewayResponse.errorMessage());
            
            // Publish failure event
            eventPublisher.publishPaymentFailed(payment);
            
            return createFailureResponse(payment);
        }
    }
    
    /**
     * Fallback when circuit breaker opens
     */
    private CreatePaymentResponse gatewayFallback(Payment payment, String gatewayToken, Throwable ex) {
        log.error("Gateway circuit breaker opened. Payment {} will be retried later", 
                payment.getId(), ex);
        
        payment.markFailed("GATEWAY_UNAVAILABLE", "Payment gateway is temporarily unavailable");
        paymentRepository.save(payment);
        paymentsFailed.increment();
        
        eventPublisher.publishPaymentFailed(payment);
        
        return createFailureResponse(payment);
    }
    
    // ========================================
    // PAYMENT REFUND
    // ========================================
    
    @Transactional(transactionManager = "transactionManager")
    public PaymentResponse refundPayment(
            String paymentId, 
            RefundRequest request,
            String idempotencyKey) {
        
        String refundIdempotencyKey = "refund:" + idempotencyKey;
        
        IdempotencyResult<PaymentResponse> result = idempotencyService.executeIdempotent(
                refundIdempotencyKey,
                PaymentResponse.class,
                () -> processRefund(paymentId, request),
                IDEMPOTENCY_TTL
        );
        
        return result.response();
    }
    
    private PaymentResponse processRefund(String paymentId, RefundRequest request) {
        Payment payment = paymentRepository.findByIdWithLock(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));
        
        if (!payment.canTransitionTo(PaymentStatus.REFUNDED)) {
            throw new InvalidPaymentStateException(
                    "Cannot refund payment in state: " + payment.getStatus());
        }
        
        BigDecimal refundAmount = request.amount() != null 
                ? request.amount() 
                : payment.getAmount();
        
        // Call gateway for refund
        GatewayResponse gatewayResponse = paymentGateway.refund(
                payment.getGatewayTransactionId(),
                refundAmount,
                request.reason()
        );
        
        if (!gatewayResponse.success()) {
            throw new RefundFailedException(
                    gatewayResponse.errorCode(), 
                    gatewayResponse.errorMessage()
            );
        }
        
        // Update payment
        payment.markRefunded(
                gatewayResponse.transactionId(),
                refundAmount,
                request.reason()
        );
        payment = paymentRepository.save(payment);
        paymentsRefunded.increment();
        
        log.info("Payment {} refunded. Refund ID: {}", 
                paymentId, gatewayResponse.transactionId());
        
        // Publish refund event
        eventPublisher.publishPaymentRefunded(payment);
        
        return PaymentDTOs.toResponse(payment);
    }
    
    // ========================================
    // PAYMENT QUERIES
    // ========================================
    
    @Transactional(transactionManager = "transactionManager", readOnly = true)
    public Optional<PaymentResponse> getPayment(String paymentId) {
        return paymentRepository.findById(paymentId)
                .map(PaymentDTOs::toResponse);
    }
    
    @Transactional(transactionManager = "transactionManager", readOnly = true)
    public PaymentStatusResponse getPaymentStatus(String paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));
        return PaymentDTOs.toStatusResponse(payment);
    }
    
    @Transactional(transactionManager = "transactionManager", readOnly = true)
    public Optional<PaymentResponse> getPaymentForOrder(String orderId) {
        return paymentRepository.findSuccessfulPaymentForOrder(orderId)
                .map(PaymentDTOs::toResponse);
    }
    
    @Transactional(transactionManager = "transactionManager", readOnly = true)
    public Page<PaymentResponse> getCustomerPayments(String customerId, Pageable pageable) {
        return paymentRepository.findByCustomerIdOrderByCreatedAtDesc(customerId, pageable)
                .map(PaymentDTOs::toResponse);
    }
    
    // ========================================
    // RETRY LOGIC
    // ========================================
    
    @Transactional(transactionManager = "transactionManager")
    public PaymentResponse retryPayment(String paymentId, RetryPaymentRequest request) {
        Payment payment = paymentRepository.findByIdWithLock(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));
        
        if (payment.getStatus() != PaymentStatus.FAILED) {
            throw new InvalidPaymentStateException(
                    "Can only retry failed payments. Current status: " + payment.getStatus());
        }
        
        if (payment.getAttemptCount() >= MAX_RETRY_ATTEMPTS) {
            throw new MaxRetriesExceededException(paymentId, MAX_RETRY_ATTEMPTS);
        }
        
        String gatewayToken = request.gatewayToken() != null 
                ? request.gatewayToken() 
                : payment.getGatewayToken();
        
        // Retry payment
        payment.retryPayment();
        payment = paymentRepository.save(payment);
        
        CreatePaymentResponse response = processWithGateway(payment, gatewayToken);
        
        return response.payment();
    }
    
    // ========================================
    // RESPONSE BUILDERS
    // ========================================
    
    private CreatePaymentResponse createSuccessResponse(Payment payment) {
        return new CreatePaymentResponse(
                payment.getId(),
                payment.getStatus().name(),
                null,
                PaymentDTOs.toResponse(payment)
        );
    }
    
    private CreatePaymentResponse createFailureResponse(Payment payment) {
        return new CreatePaymentResponse(
                payment.getId(),
                payment.getStatus().name(),
                null,
                PaymentDTOs.toResponse(payment)
        );
    }
    
    private CreatePaymentResponse createPendingResponse(Payment payment, GatewayResponse gateway) {
        CreatePaymentResponse.NextAction nextAction = null;
        if (gateway.nextAction() != null) {
            nextAction = new CreatePaymentResponse.NextAction(
                    gateway.nextAction().type(),
                    gateway.nextAction().url(),
                    gateway.nextAction().clientSecret(),
                    null
            );
        }
        
        return new CreatePaymentResponse(
                payment.getId(),
                "REQUIRES_ACTION",
                nextAction,
                PaymentDTOs.toResponse(payment)
        );
    }
    
    // ========================================
    // EXCEPTION CLASSES
    // ========================================
    
    public static class PaymentNotFoundException extends RuntimeException {
        public PaymentNotFoundException(String paymentId) {
            super("Payment not found: " + paymentId);
        }
    }
    
    public static class InvalidPaymentStateException extends RuntimeException {
        public InvalidPaymentStateException(String message) {
            super(message);
        }
    }
    
    public static class RefundFailedException extends RuntimeException {
        private final String errorCode;
        
        public RefundFailedException(String errorCode, String message) {
            super(message);
            this.errorCode = errorCode;
        }
        
        public String getErrorCode() {
            return errorCode;
        }
    }
    
    public static class MaxRetriesExceededException extends RuntimeException {
        public MaxRetriesExceededException(String paymentId, int maxRetries) {
            super("Payment %s has exceeded max retry attempts (%d)".formatted(paymentId, maxRetries));
        }
    }
}
