package com.foodexpress.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;

/**
 * Standard API response wrapper using Java 21 Record.
 * Immutable by design, perfect for API responses.
 *
 * @param <T> The type of data contained in the response
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        String message,
        T data,
        List<ApiError> errors,
        ApiMetadata metadata
) {
    
    /**
     * Nested record for error details
     */
    public record ApiError(
            String code,
            String field,
            String message
    ) {
        public static ApiError of(String code, String message) {
            return new ApiError(code, null, message);
        }
        
        public static ApiError ofField(String field, String message) {
            return new ApiError("VALIDATION_ERROR", field, message);
        }
    }
    
    /**
     * Nested record for response metadata
     */
    public record ApiMetadata(
            String traceId,
            Instant timestamp,
            String path,
            Long processingTimeMs
    ) {
        public static ApiMetadata now(String traceId, String path) {
            return new ApiMetadata(traceId, Instant.now(), path, null);
        }
        
        public ApiMetadata withProcessingTime(long processingTimeMs) {
            return new ApiMetadata(traceId, timestamp, path, processingTimeMs);
        }
    }
    
    // ========================================
    // FACTORY METHODS
    // ========================================
    
    /**
     * Create a successful response with data
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "Success", data, null, null);
    }
    
    /**
     * Create a successful response with message and data
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data, null, null);
    }
    
    /**
     * Create a successful response with metadata
     */
    public static <T> ApiResponse<T> success(T data, ApiMetadata metadata) {
        return new ApiResponse<>(true, "Success", data, null, metadata);
    }
    
    /**
     * Create an error response
     */
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, null, null, null);
    }
    
    /**
     * Create an error response with error details
     */
    public static <T> ApiResponse<T> error(String message, List<ApiError> errors) {
        return new ApiResponse<>(false, message, null, errors, null);
    }
    
    /**
     * Create an error response with single error
     */
    public static <T> ApiResponse<T> error(String message, ApiError error) {
        return new ApiResponse<>(false, message, null, List.of(error), null);
    }
    
    /**
     * Create an error response with metadata
     */
    public static <T> ApiResponse<T> error(String message, List<ApiError> errors, ApiMetadata metadata) {
        return new ApiResponse<>(false, message, null, errors, metadata);
    }
    
    /**
     * Add metadata to existing response
     */
    public ApiResponse<T> withMetadata(ApiMetadata metadata) {
        return new ApiResponse<>(success, message, data, errors, metadata);
    }
}
