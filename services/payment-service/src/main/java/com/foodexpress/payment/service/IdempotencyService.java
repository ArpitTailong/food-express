package com.foodexpress.payment.service;

import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Idempotency Service for ensuring payment operations are idempotent.
 * 
 * Uses Redis for:
 * 1. Storing idempotency key -> response mapping
 * 2. Distributed locking during first execution
 * 
 * Flow:
 * 1. Check if idempotency key exists in Redis
 *    - If exists: Return cached response (duplicate request)
 *    - If not: Acquire lock and proceed
 * 2. Execute the operation
 * 3. Store response in Redis with TTL
 * 4. Release lock
 */
@Service
@Profile("!local")
public class IdempotencyService implements IdempotencyOperations {
    
    private static final Logger log = LoggerFactory.getLogger(IdempotencyService.class);
    
    private static final String IDEMPOTENCY_KEY_PREFIX = "payment:idempotency:";
    private static final String LOCK_PREFIX = "payment:lock:";
    
    private static final Duration DEFAULT_TTL = Duration.ofHours(24);
    private static final long LOCK_WAIT_TIME_SECONDS = 10;
    private static final long LOCK_LEASE_TIME_SECONDS = 30;
    
    private final RedissonClient redissonClient;
    
    public IdempotencyService(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }
    
    /**
     * Result of idempotency check - use interface record
     */
    
    /**
     * Execute an operation with idempotency guarantee.
     * 
     * @param idempotencyKey Unique key for this operation
     * @param responseType Class of the response (for deserialization)
     * @param operation The operation to execute if not duplicate
     * @param ttl Time-to-live for the idempotency record
     * @return IdempotencyResult containing the response and duplicate status
     */
    public <T> IdempotencyResult<T> executeIdempotent(
            String idempotencyKey,
            Class<T> responseType,
            Supplier<T> operation,
            Duration ttl) {
        
        String redisKey = IDEMPOTENCY_KEY_PREFIX + idempotencyKey;
        String lockKey = LOCK_PREFIX + idempotencyKey;
        
        // Step 1: Check cache first (fast path for duplicates)
        RBucket<T> bucket = redissonClient.getBucket(redisKey);
        T cachedResponse = bucket.get();
        
        if (cachedResponse != null) {
            log.info("Duplicate request detected for idempotency key: {}", idempotencyKey);
            return IdempotencyResult.duplicate(idempotencyKey, cachedResponse);
        }
        
        // Step 2: Acquire distributed lock
        RLock lock = redissonClient.getLock(lockKey);
        
        try {
            boolean acquired = lock.tryLock(LOCK_WAIT_TIME_SECONDS, LOCK_LEASE_TIME_SECONDS, TimeUnit.SECONDS);
            
            if (!acquired) {
                throw new IdempotencyLockException(
                        "Failed to acquire lock for idempotency key: " + idempotencyKey);
            }
            
            try {
                // Step 3: Double-check after acquiring lock (another thread might have completed)
                cachedResponse = bucket.get();
                if (cachedResponse != null) {
                    log.info("Duplicate detected after lock acquisition: {}", idempotencyKey);
                    return IdempotencyResult.duplicate(idempotencyKey, cachedResponse);
                }
                
                // Step 4: Execute the operation
                log.debug("Executing operation for idempotency key: {}", idempotencyKey);
                T response = operation.get();
                
                // Step 5: Store response in Redis
                bucket.set(response, ttl);
                log.debug("Stored idempotency response for key: {}, TTL: {}", idempotencyKey, ttl);
                
                return IdempotencyResult.newRequest(idempotencyKey, response);
                
            } finally {
                // Step 6: Release lock
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IdempotencyLockException("Interrupted while waiting for lock", e);
        }
    }
    
    /**
     * Execute with default TTL (24 hours)
     */
    public <T> IdempotencyResult<T> executeIdempotent(
            String idempotencyKey,
            Class<T> responseType,
            Supplier<T> operation) {
        return executeIdempotent(idempotencyKey, responseType, operation, DEFAULT_TTL);
    }
    
    /**
     * Check if an idempotency key has already been processed
     */
    public boolean isProcessed(String idempotencyKey) {
        String redisKey = IDEMPOTENCY_KEY_PREFIX + idempotencyKey;
        return redissonClient.getBucket(redisKey).isExists();
    }
    
    /**
     * Get cached response for an idempotency key
     */
    public <T> Optional<T> getCachedResponse(String idempotencyKey, Class<T> responseType) {
        String redisKey = IDEMPOTENCY_KEY_PREFIX + idempotencyKey;
        RBucket<T> bucket = redissonClient.getBucket(redisKey);
        return Optional.ofNullable(bucket.get());
    }
    
    /**
     * Store response for an idempotency key (for async operations)
     */
    public <T> void storeResponse(String idempotencyKey, T response, Duration ttl) {
        String redisKey = IDEMPOTENCY_KEY_PREFIX + idempotencyKey;
        RBucket<T> bucket = redissonClient.getBucket(redisKey);
        bucket.set(response, ttl);
        log.debug("Stored idempotency response for key: {}", idempotencyKey);
    }
    
    /**
     * Invalidate an idempotency key (for error recovery)
     */
    public void invalidate(String idempotencyKey) {
        String redisKey = IDEMPOTENCY_KEY_PREFIX + idempotencyKey;
        redissonClient.getBucket(redisKey).delete();
        log.info("Invalidated idempotency key: {}", idempotencyKey);
    }
    
    /**
     * Exception for lock acquisition failures - use interface exception
     */
}
