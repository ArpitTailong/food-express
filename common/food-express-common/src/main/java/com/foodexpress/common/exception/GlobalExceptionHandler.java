package com.foodexpress.common.exception;

import com.foodexpress.common.dto.ApiResponse;
import com.foodexpress.common.dto.ApiResponse.ApiError;
import com.foodexpress.common.dto.ApiResponse.ApiMetadata;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.util.List;
import java.util.Optional;

/**
 * Global exception handler for consistent error responses across all services.
 * Uses pattern matching for type-safe exception handling (Java 21).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    private final Tracer tracer;
    
    @Autowired
    public GlobalExceptionHandler(Optional<Tracer> tracer) {
        this.tracer = tracer.orElse(null);
    }
    
    // ========================================
    // FOOD EXPRESS EXCEPTIONS
    // ========================================
    
    @ExceptionHandler(FoodExpressException.class)
    public ResponseEntity<ApiResponse<Void>> handleFoodExpressException(
            FoodExpressException ex, HttpServletRequest request) {
        
        // Pattern matching with sealed types - exhaustive handling
        var response = switch (ex) {
            case FoodExpressException.ValidationException ve -> {
                log.warn("Validation error: {}", ve.getMessage());
                List<ApiError> errors = ve.getFieldErrors().stream()
                        .map(fe -> ApiError.ofField(fe.field(), fe.message()))
                        .toList();
                yield ApiResponse.<Void>error(ve.getMessage(), errors);
            }
            
            case FoodExpressException.ResourceNotFoundException rnf -> {
                log.warn("Resource not found: {} with ID {}", rnf.getResourceType(), rnf.getResourceId());
                yield ApiResponse.<Void>error(rnf.getMessage(), 
                        ApiError.of(rnf.getErrorCode(), rnf.getMessage()));
            }
            
            case FoodExpressException.ConflictException ce -> {
                log.warn("Conflict: {} - {}", ce.getConflictType(), ce.getMessage());
                yield ApiResponse.<Void>error(ce.getMessage(), 
                        ApiError.of(ce.getConflictType(), ce.getMessage()));
            }
            
            case FoodExpressException.UnauthorizedException ue -> {
                log.warn("Unauthorized access attempt: {}", ue.getMessage());
                yield ApiResponse.<Void>error(ue.getMessage());
            }
            
            case FoodExpressException.ForbiddenException fe -> {
                log.warn("Forbidden access: {} (required: {})", fe.getMessage(), fe.getRequiredPermission());
                yield ApiResponse.<Void>error(fe.getMessage());
            }
            
            case FoodExpressException.PaymentException pe -> {
                log.error("Payment error for {}: {} (retryable: {})", 
                        pe.getPaymentId(), pe.getMessage(), pe.isRetryable());
                yield ApiResponse.<Void>error(pe.getMessage(),
                        ApiError.of(pe.getGatewayErrorCode(), pe.getMessage()));
            }
            
            case FoodExpressException.ExternalServiceException ese -> {
                log.error("External service error from {}: {}", ese.getServiceName(), ese.getMessage(), ese);
                yield ApiResponse.<Void>error("Service temporarily unavailable");
            }
            
            case FoodExpressException.RateLimitException rle -> {
                log.warn("Rate limit exceeded. Retry after {} seconds", rle.getRetryAfterSeconds());
                yield ApiResponse.<Void>error(rle.getMessage());
            }
            
            // Default case for any future exception types
            default -> {
                log.error("Unhandled FoodExpressException: {}", ex.getMessage(), ex);
                yield ApiResponse.<Void>error(ex.getMessage());
            }
        };
        
        return ResponseEntity
                .status(ex.getHttpStatus())
                .body(response.withMetadata(createMetadata(request)));
    }
    
    // ========================================
    // VALIDATION EXCEPTIONS
    // ========================================
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        
        List<ApiError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> ApiError.ofField(fe.getField(), fe.getDefaultMessage()))
                .toList();
        
        log.warn("Validation failed: {}", errors);
        
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.<Void>error("Validation failed", errors)
                        .withMetadata(createMetadata(request)));
    }
    
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request) {
        
        List<ApiError> errors = ex.getConstraintViolations().stream()
                .map(cv -> ApiError.ofField(cv.getPropertyPath().toString(), cv.getMessage()))
                .toList();
        
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.<Void>error("Validation failed", errors)
                        .withMetadata(createMetadata(request)));
    }
    
    // ========================================
    // SECURITY EXCEPTIONS
    // ========================================
    
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(
            AuthenticationException ex, HttpServletRequest request) {
        
        log.warn("Authentication failed: {}", ex.getMessage());
        
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.<Void>error("Authentication failed")
                        .withMetadata(createMetadata(request)));
    }
    
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(
            AccessDeniedException ex, HttpServletRequest request) {
        
        log.warn("Access denied: {}", ex.getMessage());
        
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.<Void>error("Access denied")
                        .withMetadata(createMetadata(request)));
    }
    
    // ========================================
    // GENERIC EXCEPTIONS
    // ========================================
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(
            Exception ex, HttpServletRequest request) {
        
        log.error("Unexpected error occurred", ex);
        
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.<Void>error("An unexpected error occurred")
                        .withMetadata(createMetadata(request)));
    }
    
    // ========================================
    // HELPER METHODS
    // ========================================
    
    private ApiMetadata createMetadata(HttpServletRequest request) {
        String traceId = tracer != null && tracer.currentSpan() != null 
                ? tracer.currentSpan().context().traceId() 
                : "N/A";
        return ApiMetadata.now(traceId, request.getRequestURI());
    }
}
