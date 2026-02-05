package com.foodexpress.payment.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * In-memory Idempotency Service for local development.
 * Replaces Redis-based IdempotencyService when running with local profile.
 * 
 * This class implements IdempotencyOperations so it can be
 * used as a drop-in replacement for IdempotencyService.
 */
@Service
@Profile("local")
@Primary
public class InMemoryIdempotencyService implements IdempotencyOperations {
    
    private static final Logger log = LoggerFactory.getLogger(InMemoryIdempotencyService.class);
    
    private static final Duration DEFAULT_TTL = Duration.ofHours(24);
    
    private final Map<String, CachedEntry<?>> cache = new ConcurrentHashMap<>();
    private final Map<String, Lock> locks = new ConcurrentHashMap<>();
    
    private record CachedEntry<T>(T response, Instant expiresAt) {
        boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }
    
    /**
     * Execute with idempotency guarantee (main method)
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> IdempotencyResult<T> executeIdempotent(
            String idempotencyKey,
            Class<T> responseType,
            Supplier<T> operation,
            Duration ttl) {
        
        // Check for cached response
        CachedEntry<?> cached = cache.get(idempotencyKey);
        if (cached != null && !cached.isExpired()) {
            log.info("Idempotency hit - returning cached response for key: {}", idempotencyKey);
            return IdempotencyResult.duplicate(idempotencyKey, (T) cached.response());
        }
        
        // Remove expired entry
        if (cached != null && cached.isExpired()) {
            cache.remove(idempotencyKey);
        }
        
        // Get or create lock for this key
        Lock lock = locks.computeIfAbsent(idempotencyKey, k -> new ReentrantLock());
        
        lock.lock();
        try {
            // Double-check after acquiring lock
            cached = cache.get(idempotencyKey);
            if (cached != null && !cached.isExpired()) {
                log.info("Idempotency hit (after lock) - returning cached response for key: {}", idempotencyKey);
                return IdempotencyResult.duplicate(idempotencyKey, (T) cached.response());
            }
            
            // Execute the operation
            log.info("Executing operation for idempotency key: {}", idempotencyKey);
            T response = operation.get();
            
            // Store the response
            cache.put(idempotencyKey, new CachedEntry<>(response, Instant.now().plus(ttl)));
            
            return IdempotencyResult.newRequest(idempotencyKey, response);
            
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Execute with default TTL (24 hours)
     */
    @Override
    public <T> IdempotencyResult<T> executeIdempotent(
            String idempotencyKey,
            Class<T> responseType,
            Supplier<T> operation) {
        return executeIdempotent(idempotencyKey, responseType, operation, DEFAULT_TTL);
    }
    
    /**
     * Check if an idempotency key has already been processed
     */
    @Override
    public boolean isProcessed(String idempotencyKey) {
        CachedEntry<?> cached = cache.get(idempotencyKey);
        return cached != null && !cached.isExpired();
    }
    
    /**
     * Get cached response for an idempotency key
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getCachedResponse(String idempotencyKey, Class<T> responseType) {
        CachedEntry<?> cached = cache.get(idempotencyKey);
        if (cached != null && !cached.isExpired()) {
            return Optional.of((T) cached.response());
        }
        return Optional.empty();
    }
    
    /**
     * Store response for an idempotency key (for async operations)
     */
    @Override
    public <T> void storeResponse(String idempotencyKey, T response, Duration ttl) {
        cache.put(idempotencyKey, new CachedEntry<>(response, Instant.now().plus(ttl)));
        log.debug("Stored idempotency response for key: {}", idempotencyKey);
    }
    
    /**
     * Invalidate an idempotency key (for error recovery)
     */
    @Override
    public void invalidate(String idempotencyKey) {
        cache.remove(idempotencyKey);
        locks.remove(idempotencyKey);
        log.info("Invalidated idempotency key: {}", idempotencyKey);
    }
}
