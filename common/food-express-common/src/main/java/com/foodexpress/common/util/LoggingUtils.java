package com.foodexpress.common.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Structured logging utilities for JSON-formatted logs.
 * Provides consistent logging format across all services.
 */
public final class LoggingUtils {
    
    private static final Logger log = LoggerFactory.getLogger(LoggingUtils.class);
    
    // Patterns for sensitive data masking
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "([a-zA-Z0-9._%+-]+)@([a-zA-Z0-9.-]+\\.[a-zA-Z]{2,})");
    private static final Pattern CARD_PATTERN = Pattern.compile(
            "\\b(\\d{4})[- ]?(\\d{4})[- ]?(\\d{4})[- ]?(\\d{4})\\b");
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "\\b(\\d{3})[- ]?(\\d{3})[- ]?(\\d{4})\\b");
    private static final Pattern SSN_PATTERN = Pattern.compile(
            "\\b(\\d{3})[- ]?(\\d{2})[- ]?(\\d{4})\\b");
    
    private LoggingUtils() {} // Utility class
    
    // ========================================
    // STRUCTURED LOG EVENTS
    // ========================================
    
    /**
     * Structured log event for JSON serialization
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record LogEvent(
            @JsonProperty("timestamp") Instant timestamp,
            @JsonProperty("level") String level,
            @JsonProperty("service") String service,
            @JsonProperty("traceId") String traceId,
            @JsonProperty("spanId") String spanId,
            @JsonProperty("correlationId") String correlationId,
            @JsonProperty("message") String message,
            @JsonProperty("context") Map<String, Object> context,
            @JsonProperty("exception") ExceptionInfo exception
    ) {
        public static Builder builder() {
            return new Builder();
        }
        
        public static class Builder {
            private String level = "INFO";
            private String service;
            private String message;
            private Map<String, Object> context = new HashMap<>();
            private ExceptionInfo exception;
            
            public Builder level(String level) {
                this.level = level;
                return this;
            }
            
            public Builder service(String service) {
                this.service = service;
                return this;
            }
            
            public Builder message(String message) {
                this.message = message;
                return this;
            }
            
            public Builder addContext(String key, Object value) {
                this.context.put(key, value);
                return this;
            }
            
            public Builder context(Map<String, Object> context) {
                this.context = context;
                return this;
            }
            
            public Builder exception(Throwable t) {
                this.exception = ExceptionInfo.from(t);
                return this;
            }
            
            public LogEvent build() {
                return new LogEvent(
                        Instant.now(),
                        level,
                        service,
                        MDC.get("traceId"),
                        MDC.get("spanId"),
                        MDC.get("correlationId"),
                        message,
                        context.isEmpty() ? null : context,
                        exception
                );
            }
        }
    }
    
    /**
     * Exception information for structured logging
     */
    public record ExceptionInfo(
            String type,
            String message,
            String stackTrace
    ) {
        public static ExceptionInfo from(Throwable t) {
            if (t == null) return null;
            
            StringBuilder sb = new StringBuilder();
            for (StackTraceElement element : t.getStackTrace()) {
                sb.append("\tat ").append(element.toString()).append("\n");
                if (sb.length() > 2000) { // Limit stack trace length
                    sb.append("\t... truncated ...\n");
                    break;
                }
            }
            
            return new ExceptionInfo(
                    t.getClass().getName(),
                    t.getMessage(),
                    sb.toString()
            );
        }
    }
    
    // ========================================
    // SENSITIVE DATA MASKING
    // ========================================
    
    /**
     * Mask sensitive data in a string
     */
    public static String maskSensitiveData(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        
        String result = input;
        
        // Mask email addresses (show first 2 chars and domain)
        result = EMAIL_PATTERN.matcher(result).replaceAll(m -> {
            String localPart = m.group(1);
            String domain = m.group(2);
            String masked = localPart.length() > 2 
                    ? localPart.substring(0, 2) + "***" 
                    : "***";
            return masked + "@" + domain;
        });
        
        // Mask card numbers (show last 4 digits)
        result = CARD_PATTERN.matcher(result).replaceAll("****-****-****-$4");
        
        // Mask phone numbers
        result = PHONE_PATTERN.matcher(result).replaceAll("***-***-$3");
        
        // Mask SSN
        result = SSN_PATTERN.matcher(result).replaceAll("***-**-$3");
        
        return result;
    }
    
    /**
     * Mask a credit card number, showing only last 4 digits
     */
    public static String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return "****";
        }
        String digitsOnly = cardNumber.replaceAll("[^0-9]", "");
        if (digitsOnly.length() < 4) {
            return "****";
        }
        return "****" + digitsOnly.substring(digitsOnly.length() - 4);
    }
    
    /**
     * Mask an email address
     */
    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***@***";
        }
        String[] parts = email.split("@");
        String local = parts[0];
        String masked = local.length() > 2 
                ? local.substring(0, 2) + "***" 
                : "***";
        return masked + "@" + parts[1];
    }
    
    /**
     * Mask a map of values (for logging payment data, etc.)
     */
    public static Map<String, Object> maskPaymentData(Map<String, Object> data) {
        Map<String, Object> masked = new HashMap<>(data);
        
        // Fields to completely mask
        String[] sensitiveFields = {"cardNumber", "cvv", "cvc", "pin", "password", "secret"};
        for (String field : sensitiveFields) {
            if (masked.containsKey(field)) {
                masked.put(field, "***REDACTED***");
            }
        }
        
        // Fields to partially mask
        if (masked.containsKey("email") && masked.get("email") instanceof String email) {
            masked.put("email", maskEmail(email));
        }
        
        return masked;
    }
    
    // ========================================
    // MDC HELPERS
    // ========================================
    
    /**
     * Set common MDC fields for request processing
     */
    public static void setRequestContext(String requestId, String userId, String path) {
        MDC.put("requestId", requestId);
        MDC.put("userId", userId);
        MDC.put("path", path);
    }
    
    /**
     * Set payment context for logging
     */
    public static void setPaymentContext(String paymentId, String orderId, String amount) {
        MDC.put("paymentId", paymentId);
        MDC.put("orderId", orderId);
        MDC.put("amount", amount);
    }
    
    /**
     * Clear all custom MDC fields
     */
    public static void clearContext() {
        MDC.clear();
    }
    
    // ========================================
    // CONVENIENCE LOGGING METHODS
    // ========================================
    
    /**
     * Log a payment event with proper masking
     */
    public static void logPaymentEvent(String event, String paymentId, String orderId, 
                                        java.math.BigDecimal amount, String maskedCard) {
        log.info("Payment Event: {} | PaymentID: {} | OrderID: {} | Amount: {} | Card: {}",
                event, paymentId, orderId, amount, maskedCard);
    }
    
    /**
     * Log an API request
     */
    public static void logApiRequest(String method, String path, String userId, 
                                      long durationMs, int statusCode) {
        log.info("API Request: {} {} | User: {} | Duration: {}ms | Status: {}",
                method, path, userId, durationMs, statusCode);
    }
}
