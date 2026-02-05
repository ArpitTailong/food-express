package com.foodexpress.payment.controller;

import com.foodexpress.payment.dto.PaymentDTOs.*;
import com.foodexpress.payment.service.PaymentService;
import com.foodexpress.payment.service.PaymentService.*;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Payment REST API Controller.
 * 
 * Security:
 * - JWT authentication required
 * - Role-based access control
 * - Rate limiting per customer
 * 
 * Idempotency:
 * - X-Idempotency-Key header required for POST requests
 * - Duplicate requests return cached response
 */
@RestController
@RequestMapping("/api/v1/payments")
@Tag(name = "Payments", description = "Payment processing APIs")
public class PaymentController {
    
    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);
    
    private static final String IDEMPOTENCY_HEADER = "X-Idempotency-Key";
    private static final String CORRELATION_HEADER = "X-Correlation-Id";
    
    private final PaymentService paymentService;
    
    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }
    
    // ========================================
    // PAYMENT CREATION
    // ========================================
    
    @PostMapping
    @RateLimiter(name = "createPayment")
    @Timed(value = "payment.create", description = "Time to create a payment")
    @Operation(summary = "Create a payment", description = "Process a new payment for an order")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Payment created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "409", description = "Payment already exists for this order"),
            @ApiResponse(responseCode = "422", description = "Payment processing failed"),
            @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<CreatePaymentResponse> createPayment(
            @Valid @RequestBody CreatePaymentRequest request,
            @RequestHeader(IDEMPOTENCY_HEADER) String idempotencyKey,
            @RequestHeader(value = CORRELATION_HEADER, required = false) String correlationId,
            @AuthenticationPrincipal Jwt jwt) {
        
        // Generate correlation ID if not provided
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        
        // Set MDC for logging
        MDC.put("correlationId", correlationId);
        MDC.put("orderId", request.orderId());
        MDC.put("customerId", request.customerId());
        
        try {
            log.info("Creating payment for order {} with idempotency key {}", 
                    request.orderId(), idempotencyKey);
            
            CreatePaymentResponse response = paymentService.createPayment(
                    request, 
                    idempotencyKey, 
                    correlationId
            );
            
            HttpStatus status = switch (response.status()) {
                case "SUCCESS" -> HttpStatus.CREATED;
                case "REQUIRES_ACTION" -> HttpStatus.ACCEPTED;
                case "FAILED" -> HttpStatus.UNPROCESSABLE_ENTITY;
                default -> HttpStatus.OK;
            };
            
            return ResponseEntity.status(status).body(response);
            
        } finally {
            MDC.clear();
        }
    }
    
    // ========================================
    // PAYMENT QUERIES
    // ========================================
    
    @GetMapping("/{paymentId}")
    @Timed(value = "payment.get", description = "Time to get a payment")
    @Operation(summary = "Get payment details", description = "Retrieve payment by ID")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<PaymentResponse> getPayment(
            @PathVariable String paymentId,
            @AuthenticationPrincipal Jwt jwt) {
        
        log.debug("Getting payment {}", paymentId);
        
        return paymentService.getPayment(paymentId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/{paymentId}/status")
    @Timed(value = "payment.status", description = "Time to get payment status")
    @Operation(summary = "Get payment status", description = "Check current payment status")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<PaymentStatusResponse> getPaymentStatus(
            @PathVariable String paymentId) {
        
        log.debug("Getting status for payment {}", paymentId);
        
        try {
            PaymentStatusResponse status = paymentService.getPaymentStatus(paymentId);
            return ResponseEntity.ok(status);
        } catch (PaymentNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/order/{orderId}")
    @Timed(value = "payment.getByOrder", description = "Time to get payment by order")
    @Operation(summary = "Get payment for order", description = "Retrieve successful payment for an order")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN') or hasRole('SERVICE')")
    public ResponseEntity<PaymentResponse> getPaymentForOrder(
            @PathVariable String orderId) {
        
        log.debug("Getting payment for order {}", orderId);
        
        return paymentService.getPaymentForOrder(orderId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/customer/{customerId}")
    @Timed(value = "payment.getByCustomer", description = "Time to get customer payments")
    @Operation(summary = "Get customer payments", description = "Retrieve all payments for a customer")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<Page<PaymentResponse>> getCustomerPayments(
            @PathVariable String customerId,
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal Jwt jwt) {
        
        log.debug("Getting payments for customer {}", customerId);
        
        // Security check: customers can only view their own payments
        String tokenCustomerId = jwt.getSubject();
        boolean isAdmin = jwt.getClaimAsStringList("roles") != null 
                && jwt.getClaimAsStringList("roles").contains("ADMIN");
        
        if (!isAdmin && !customerId.equals(tokenCustomerId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        Page<PaymentResponse> payments = paymentService.getCustomerPayments(customerId, pageable);
        return ResponseEntity.ok(payments);
    }
    
    // ========================================
    // PAYMENT REFUND
    // ========================================
    
    @PostMapping("/{paymentId}/refund")
    @RateLimiter(name = "refundPayment")
    @Timed(value = "payment.refund", description = "Time to refund a payment")
    @Operation(summary = "Refund a payment", description = "Process a full or partial refund")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Refund processed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "404", description = "Payment not found"),
            @ApiResponse(responseCode = "409", description = "Payment cannot be refunded"),
            @ApiResponse(responseCode = "422", description = "Refund processing failed")
    })
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPPORT')")
    public ResponseEntity<PaymentResponse> refundPayment(
            @PathVariable String paymentId,
            @Valid @RequestBody RefundRequest request,
            @RequestHeader(IDEMPOTENCY_HEADER) String idempotencyKey,
            @RequestHeader(value = CORRELATION_HEADER, required = false) String correlationId) {
        
        log.info("Processing refund for payment {} with idempotency key {}", 
                paymentId, idempotencyKey);
        
        try {
            PaymentResponse response = paymentService.refundPayment(
                    paymentId, 
                    request, 
                    idempotencyKey
            );
            return ResponseEntity.ok(response);
            
        } catch (PaymentNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (InvalidPaymentStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (RefundFailedException e) {
            return ResponseEntity.unprocessableEntity().build();
        }
    }
    
    // ========================================
    // PAYMENT RETRY
    // ========================================
    
    @PostMapping("/{paymentId}/retry")
    @RateLimiter(name = "retryPayment")
    @Timed(value = "payment.retry", description = "Time to retry a payment")
    @Operation(summary = "Retry a failed payment", description = "Retry processing a failed payment")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payment retried successfully"),
            @ApiResponse(responseCode = "404", description = "Payment not found"),
            @ApiResponse(responseCode = "409", description = "Payment cannot be retried"),
            @ApiResponse(responseCode = "422", description = "Max retries exceeded")
    })
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<PaymentResponse> retryPayment(
            @PathVariable String paymentId,
            @RequestBody(required = false) RetryPaymentRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        
        log.info("Retrying payment {}", paymentId);
        
        try {
            RetryPaymentRequest retryRequest = request != null 
                    ? request 
                    : new RetryPaymentRequest(null);
            
            PaymentResponse response = paymentService.retryPayment(paymentId, retryRequest);
            return ResponseEntity.ok(response);
            
        } catch (PaymentNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (InvalidPaymentStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (MaxRetriesExceededException e) {
            return ResponseEntity.unprocessableEntity().build();
        }
    }
    
    // ========================================
    // EXCEPTION HANDLERS
    // ========================================
    
    @ExceptionHandler(PaymentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlePaymentNotFound(PaymentNotFoundException e) {
        log.warn("Payment not found: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("PAYMENT_NOT_FOUND", e.getMessage()));
    }
    
    @ExceptionHandler(InvalidPaymentStateException.class)
    public ResponseEntity<ErrorResponse> handleInvalidState(InvalidPaymentStateException e) {
        log.warn("Invalid payment state: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("INVALID_STATE", e.getMessage()));
    }
    
    @ExceptionHandler(RefundFailedException.class)
    public ResponseEntity<ErrorResponse> handleRefundFailed(RefundFailedException e) {
        log.error("Refund failed: {} - {}", e.getErrorCode(), e.getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ErrorResponse(e.getErrorCode(), e.getMessage()));
    }
    
    @ExceptionHandler(MaxRetriesExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxRetries(MaxRetriesExceededException e) {
        log.warn("Max retries exceeded: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ErrorResponse("MAX_RETRIES_EXCEEDED", e.getMessage()));
    }
    
    /**
     * Standard error response
     */
    public record ErrorResponse(String code, String message) {}
}
