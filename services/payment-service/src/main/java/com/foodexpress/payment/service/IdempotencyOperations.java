package com.foodexpress.payment.service;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Interface for idempotency operations.
 * Provides a common contract for both Redis-based and in-memory implementations.
 */
public interface IdempotencyOperations {
    
    /**
     * Result of idempotency check
     */
    record IdempotencyResult<T>(
            boolean isDuplicate,
            T response,
            String idempotencyKey
    ) {
        public static <T> IdempotencyResult<T> duplicate(String key, T cachedResponse) {
            return new IdempotencyResult<>(true, cachedResponse, key);
        }
        
        public static <T> IdempotencyResult<T> newRequest(String key, T newResponse) {
            return new IdempotencyResult<>(false, newResponse, key);
        }
    }
    
    /**
     * Execute an operation with idempotency guarantee using default TTL.
     */
    <T> IdempotencyResult<T> executeIdempotent(
            String idempotencyKey,
            Class<T> responseType,
            Supplier<T> operation);
    
    /**
     * Execute an operation with idempotency guarantee using specified TTL.
     */
    <T> IdempotencyResult<T> executeIdempotent(
            String idempotencyKey,
            Class<T> responseType,
            Supplier<T> operation,
            Duration ttl);
    
    /**
     * Check if an idempotency key has already been processed.
     */
    boolean isProcessed(String idempotencyKey);
    
    /**
     * Get cached response for an idempotency key.
     */
    <T> Optional<T> getCachedResponse(String idempotencyKey, Class<T> responseType);
    
    /**
     * Store response for an idempotency key.
     */
    <T> void storeResponse(String idempotencyKey, T response, Duration ttl);
    
    /**
     * Invalidate an idempotency key.
     */
    void invalidate(String idempotencyKey);
    
    /**
     * Exception for lock acquisition failures.
     */
    class IdempotencyLockException extends RuntimeException {
        public IdempotencyLockException(String message) {
            super(message);
        }
        
        public IdempotencyLockException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
