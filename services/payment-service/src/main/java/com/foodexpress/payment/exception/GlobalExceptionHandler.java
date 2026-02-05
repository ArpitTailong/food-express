package com.foodexpress.payment.exception;

import com.foodexpress.payment.service.IdempotencyOperations.IdempotencyLockException;
import com.foodexpress.payment.service.PaymentService.*;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for Payment Service.
 * Provides consistent error responses across all endpoints.
 */
@RestControllerAdvice
@org.springframework.stereotype.Component("paymentGlobalExceptionHandler")
public class GlobalExceptionHandler {
    
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    /**
     * Standard error response
     */
    public record ErrorResponse(
            String timestamp,
            int status,
            String error,
            String code,
            String message,
            String path,
            Map<String, String> details
    ) {
        public ErrorResponse(int status, String error, String code, String message, String path) {
            this(Instant.now().toString(), status, error, code, message, path, null);
        }
        
        public ErrorResponse(int status, String error, String code, String message, String path, Map<String, String> details) {
            this(Instant.now().toString(), status, error, code, message, path, details);
        }
    }
    
    // ========================================
    // BUSINESS EXCEPTIONS
    // ========================================
    
    @ExceptionHandler(PaymentNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handlePaymentNotFound(PaymentNotFoundException ex, WebRequest request) {
        log.warn("Payment not found: {}", ex.getMessage());
        return new ErrorResponse(
                404,
                "Not Found",
                "PAYMENT_NOT_FOUND",
                ex.getMessage(),
                request.getDescription(false).replace("uri=", "")
        );
    }
    
    @ExceptionHandler(InvalidPaymentStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleInvalidState(InvalidPaymentStateException ex, WebRequest request) {
        log.warn("Invalid payment state: {}", ex.getMessage());
        return new ErrorResponse(
                409,
                "Conflict",
                "INVALID_PAYMENT_STATE",
                ex.getMessage(),
                request.getDescription(false).replace("uri=", "")
        );
    }
    
    @ExceptionHandler(RefundFailedException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ErrorResponse handleRefundFailed(RefundFailedException ex, WebRequest request) {
        log.error("Refund failed: {} - {}", ex.getErrorCode(), ex.getMessage());
        return new ErrorResponse(
                422,
                "Unprocessable Entity",
                ex.getErrorCode(),
                ex.getMessage(),
                request.getDescription(false).replace("uri=", "")
        );
    }
    
    @ExceptionHandler(MaxRetriesExceededException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ErrorResponse handleMaxRetries(MaxRetriesExceededException ex, WebRequest request) {
        log.warn("Max retries exceeded: {}", ex.getMessage());
        return new ErrorResponse(
                422,
                "Unprocessable Entity",
                "MAX_RETRIES_EXCEEDED",
                ex.getMessage(),
                request.getDescription(false).replace("uri=", "")
        );
    }
    
    // ========================================
    // IDEMPOTENCY EXCEPTIONS
    // ========================================
    
    @ExceptionHandler(IdempotencyLockException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleIdempotencyLock(IdempotencyLockException ex, WebRequest request) {
        log.warn("Idempotency lock exception: {}", ex.getMessage());
        return new ErrorResponse(
                409,
                "Conflict",
                "IDEMPOTENCY_LOCK_FAILED",
                "Unable to process request. Please try again.",
                request.getDescription(false).replace("uri=", "")
        );
    }
    
    @ExceptionHandler(MissingRequestHeaderException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleMissingHeader(MissingRequestHeaderException ex, WebRequest request) {
        log.warn("Missing required header: {}", ex.getHeaderName());
        
        if ("X-Idempotency-Key".equals(ex.getHeaderName())) {
            return new ErrorResponse(
                    400,
                    "Bad Request",
                    "MISSING_IDEMPOTENCY_KEY",
                    "X-Idempotency-Key header is required for this operation",
                    request.getDescription(false).replace("uri=", "")
            );
        }
        
        return new ErrorResponse(
                400,
                "Bad Request",
                "MISSING_HEADER",
                ex.getMessage(),
                request.getDescription(false).replace("uri=", "")
        );
    }
    
    // ========================================
    // VALIDATION EXCEPTIONS
    // ========================================
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidationErrors(MethodArgumentNotValidException ex, WebRequest request) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        log.warn("Validation failed: {}", errors);
        
        return new ErrorResponse(
                400,
                "Bad Request",
                "VALIDATION_FAILED",
                "Request validation failed. Check 'details' for field errors.",
                request.getDescription(false).replace("uri=", ""),
                errors
        );
    }
    
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleIllegalArgument(IllegalArgumentException ex, WebRequest request) {
        log.warn("Illegal argument: {}", ex.getMessage());
        return new ErrorResponse(
                400,
                "Bad Request",
                "INVALID_ARGUMENT",
                ex.getMessage(),
                request.getDescription(false).replace("uri=", "")
        );
    }
    
    // ========================================
    // RESILIENCE4J EXCEPTIONS
    // ========================================
    
    @ExceptionHandler(RequestNotPermitted.class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    public ErrorResponse handleRateLimitExceeded(RequestNotPermitted ex, WebRequest request) {
        log.warn("Rate limit exceeded: {}", ex.getMessage());
        return new ErrorResponse(
                429,
                "Too Many Requests",
                "RATE_LIMIT_EXCEEDED",
                "Rate limit exceeded. Please try again later.",
                request.getDescription(false).replace("uri=", "")
        );
    }
    
    @ExceptionHandler(io.github.resilience4j.circuitbreaker.CallNotPermittedException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ErrorResponse handleCircuitBreakerOpen(
            io.github.resilience4j.circuitbreaker.CallNotPermittedException ex, 
            WebRequest request) {
        log.error("Circuit breaker open: {}", ex.getMessage());
        return new ErrorResponse(
                503,
                "Service Unavailable",
                "SERVICE_UNAVAILABLE",
                "Service is temporarily unavailable. Please try again later.",
                request.getDescription(false).replace("uri=", "")
        );
    }
    
    // ========================================
    // SECURITY EXCEPTIONS
    // ========================================
    
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponse handleAccessDenied(AccessDeniedException ex, WebRequest request) {
        log.warn("Access denied: {}", ex.getMessage());
        return new ErrorResponse(
                403,
                "Forbidden",
                "ACCESS_DENIED",
                "You don't have permission to access this resource",
                request.getDescription(false).replace("uri=", "")
        );
    }
    
    // ========================================
    // DATABASE EXCEPTIONS
    // ========================================
    
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleDataIntegrityViolation(
            org.springframework.dao.DataIntegrityViolationException ex, 
            WebRequest request) {
        log.error("Data integrity violation: {}", ex.getMessage());
        
        String message = "Data integrity violation";
        String code = "DATA_INTEGRITY_VIOLATION";
        
        if (ex.getMessage() != null) {
            if (ex.getMessage().contains("unique constraint")) {
                message = "A record with this data already exists";
                code = "DUPLICATE_RECORD";
            } else if (ex.getMessage().contains("foreign key constraint")) {
                message = "Referenced entity does not exist";
                code = "INVALID_REFERENCE";
            }
        }
        
        return new ErrorResponse(
                409,
                "Conflict",
                code,
                message,
                request.getDescription(false).replace("uri=", "")
        );
    }
    
    @ExceptionHandler(org.springframework.dao.OptimisticLockingFailureException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleOptimisticLockingFailure(
            org.springframework.dao.OptimisticLockingFailureException ex, 
            WebRequest request) {
        log.warn("Optimistic locking failure: {}", ex.getMessage());
        return new ErrorResponse(
                409,
                "Conflict",
                "CONCURRENT_MODIFICATION",
                "Resource was modified by another user. Please refresh and try again.",
                request.getDescription(false).replace("uri=", "")
        );
    }
    
    // ========================================
    // GENERIC EXCEPTIONS
    // ========================================
    
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleGenericException(Exception ex, WebRequest request) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        return new ErrorResponse(
                500,
                "Internal Server Error",
                "INTERNAL_ERROR",
                "An unexpected error occurred. Please contact support if the problem persists.",
                request.getDescription(false).replace("uri=", "")
        );
    }
}
