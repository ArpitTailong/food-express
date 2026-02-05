package com.foodexpress.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Base exception for all FoodExpress business exceptions.
 * Uses sealed classes for type-safe exception handling.
 */
public sealed class FoodExpressException extends RuntimeException permits
        FoodExpressException.ValidationException,
        FoodExpressException.ResourceNotFoundException,
        FoodExpressException.ConflictException,
        FoodExpressException.UnauthorizedException,
        FoodExpressException.ForbiddenException,
        FoodExpressException.PaymentException,
        FoodExpressException.ExternalServiceException,
        FoodExpressException.RateLimitException {
    
    private final String errorCode;
    private final HttpStatus httpStatus;
    
    protected FoodExpressException(String message, String errorCode, HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }
    
    protected FoodExpressException(String message, String errorCode, HttpStatus httpStatus, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
    
    // ========================================
    // VALIDATION EXCEPTIONS (400)
    // ========================================
    
    public static final class ValidationException extends FoodExpressException {
        private final java.util.List<FieldError> fieldErrors;
        
        public ValidationException(String message) {
            super(message, "VALIDATION_ERROR", HttpStatus.BAD_REQUEST);
            this.fieldErrors = java.util.List.of();
        }
        
        public ValidationException(String message, java.util.List<FieldError> fieldErrors) {
            super(message, "VALIDATION_ERROR", HttpStatus.BAD_REQUEST);
            this.fieldErrors = fieldErrors;
        }
        
        public java.util.List<FieldError> getFieldErrors() {
            return fieldErrors;
        }
        
        public record FieldError(String field, String message, Object rejectedValue) {
            public static FieldError of(String field, String message) {
                return new FieldError(field, message, null);
            }
        }
    }
    
    // ========================================
    // NOT FOUND EXCEPTIONS (404)
    // ========================================
    
    public static final class ResourceNotFoundException extends FoodExpressException {
        private final String resourceType;
        private final String resourceId;
        
        public ResourceNotFoundException(String resourceType, String resourceId) {
            super("%s with ID '%s' not found".formatted(resourceType, resourceId),
                  "RESOURCE_NOT_FOUND", HttpStatus.NOT_FOUND);
            this.resourceType = resourceType;
            this.resourceId = resourceId;
        }
        
        public String getResourceType() {
            return resourceType;
        }
        
        public String getResourceId() {
            return resourceId;
        }
    }
    
    // ========================================
    // CONFLICT EXCEPTIONS (409)
    // ========================================
    
    public static final class ConflictException extends FoodExpressException {
        private final String conflictType;
        
        public ConflictException(String message, String conflictType) {
            super(message, "CONFLICT", HttpStatus.CONFLICT);
            this.conflictType = conflictType;
        }
        
        public static ConflictException duplicateResource(String resourceType, String identifier) {
            return new ConflictException(
                    "%s with identifier '%s' already exists".formatted(resourceType, identifier),
                    "DUPLICATE_RESOURCE"
            );
        }
        
        public static ConflictException optimisticLock(String resourceType, String resourceId) {
            return new ConflictException(
                    "%s '%s' was modified by another transaction".formatted(resourceType, resourceId),
                    "OPTIMISTIC_LOCK"
            );
        }
        
        public String getConflictType() {
            return conflictType;
        }
    }
    
    // ========================================
    // AUTH EXCEPTIONS (401 & 403)
    // ========================================
    
    public static final class UnauthorizedException extends FoodExpressException {
        public UnauthorizedException(String message) {
            super(message, "UNAUTHORIZED", HttpStatus.UNAUTHORIZED);
        }
        
        public static UnauthorizedException invalidToken() {
            return new UnauthorizedException("Invalid or expired authentication token");
        }
        
        public static UnauthorizedException missingToken() {
            return new UnauthorizedException("Authentication token is required");
        }
    }
    
    public static final class ForbiddenException extends FoodExpressException {
        private final String requiredPermission;
        
        public ForbiddenException(String message) {
            super(message, "FORBIDDEN", HttpStatus.FORBIDDEN);
            this.requiredPermission = null;
        }
        
        public ForbiddenException(String message, String requiredPermission) {
            super(message, "FORBIDDEN", HttpStatus.FORBIDDEN);
            this.requiredPermission = requiredPermission;
        }
        
        public String getRequiredPermission() {
            return requiredPermission;
        }
    }
    
    // ========================================
    // PAYMENT EXCEPTIONS
    // ========================================
    
    public static final class PaymentException extends FoodExpressException {
        private final String paymentId;
        private final String gatewayErrorCode;
        private final boolean retryable;
        
        public PaymentException(String message, String paymentId, String gatewayErrorCode, boolean retryable) {
            super(message, "PAYMENT_ERROR", HttpStatus.PAYMENT_REQUIRED);
            this.paymentId = paymentId;
            this.gatewayErrorCode = gatewayErrorCode;
            this.retryable = retryable;
        }
        
        public static PaymentException declined(String paymentId, String reason) {
            return new PaymentException("Payment declined: " + reason, paymentId, "DECLINED", false);
        }
        
        public static PaymentException timeout(String paymentId) {
            return new PaymentException("Payment gateway timeout", paymentId, "TIMEOUT", true);
        }
        
        public static PaymentException insufficientFunds(String paymentId) {
            return new PaymentException("Insufficient funds", paymentId, "INSUFFICIENT_FUNDS", false);
        }
        
        public String getPaymentId() { return paymentId; }
        public String getGatewayErrorCode() { return gatewayErrorCode; }
        public boolean isRetryable() { return retryable; }
    }
    
    // ========================================
    // EXTERNAL SERVICE EXCEPTIONS
    // ========================================
    
    public static final class ExternalServiceException extends FoodExpressException {
        private final String serviceName;
        private final boolean retryable;
        
        public ExternalServiceException(String serviceName, String message, boolean retryable) {
            super(message, "EXTERNAL_SERVICE_ERROR", HttpStatus.SERVICE_UNAVAILABLE);
            this.serviceName = serviceName;
            this.retryable = retryable;
        }
        
        public ExternalServiceException(String serviceName, String message, boolean retryable, Throwable cause) {
            super(message, "EXTERNAL_SERVICE_ERROR", HttpStatus.SERVICE_UNAVAILABLE, cause);
            this.serviceName = serviceName;
            this.retryable = retryable;
        }
        
        public String getServiceName() { return serviceName; }
        public boolean isRetryable() { return retryable; }
    }
    
    // ========================================
    // RATE LIMIT EXCEPTIONS (429)
    // ========================================
    
    public static final class RateLimitException extends FoodExpressException {
        private final long retryAfterSeconds;
        
        public RateLimitException(long retryAfterSeconds) {
            super("Rate limit exceeded. Retry after %d seconds".formatted(retryAfterSeconds),
                  "RATE_LIMIT_EXCEEDED", HttpStatus.TOO_MANY_REQUESTS);
            this.retryAfterSeconds = retryAfterSeconds;
        }
        
        public long getRetryAfterSeconds() {
            return retryAfterSeconds;
        }
    }
}
