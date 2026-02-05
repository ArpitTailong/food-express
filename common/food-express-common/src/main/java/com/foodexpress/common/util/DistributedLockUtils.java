package com.foodexpress.common.util;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Distributed locking utilities using Redisson.
 * Provides safe patterns for distributed lock acquisition.
 */
public final class DistributedLockUtils {
    
    private static final Logger log = LoggerFactory.getLogger(DistributedLockUtils.class);
    
    private static final long DEFAULT_WAIT_TIME = 10; // seconds
    private static final long DEFAULT_LEASE_TIME = 30; // seconds
    
    private DistributedLockUtils() {} // Utility class
    
    // ========================================
    // LOCK EXECUTION PATTERNS
    // ========================================
    
    /**
     * Execute a task with a distributed lock.
     * The lock is automatically released after execution.
     *
     * @param redisson Redisson client
     * @param lockKey Lock key (should be unique per resource)
     * @param task Task to execute while holding the lock
     * @return Result of the task
     * @throws LockAcquisitionException if lock cannot be acquired
     */
    public static <T> T executeWithLock(RedissonClient redisson, String lockKey, 
                                         Supplier<T> task) {
        return executeWithLock(redisson, lockKey, DEFAULT_WAIT_TIME, 
                              DEFAULT_LEASE_TIME, TimeUnit.SECONDS, task);
    }
    
    /**
     * Execute a task with a distributed lock with custom timeouts.
     */
    public static <T> T executeWithLock(RedissonClient redisson, String lockKey,
                                         long waitTime, long leaseTime, TimeUnit unit,
                                         Supplier<T> task) {
        RLock lock = redisson.getLock(lockKey);
        boolean acquired = false;
        
        try {
            acquired = lock.tryLock(waitTime, leaseTime, unit);
            
            if (!acquired) {
                log.warn("Failed to acquire lock: {} after waiting {} {}", 
                        lockKey, waitTime, unit);
                throw new LockAcquisitionException(lockKey, waitTime, unit);
            }
            
            log.debug("Lock acquired: {}", lockKey);
            return task.get();
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LockAcquisitionException(lockKey, e);
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("Lock released: {}", lockKey);
            }
        }
    }
    
    /**
     * Execute a task with lock, returning empty Optional if lock not acquired.
     */
    public static <T> java.util.Optional<T> tryExecuteWithLock(
            RedissonClient redisson, String lockKey, Supplier<T> task) {
        return tryExecuteWithLock(redisson, lockKey, DEFAULT_WAIT_TIME, 
                                  DEFAULT_LEASE_TIME, TimeUnit.SECONDS, task);
    }
    
    /**
     * Try to execute with lock, returning Optional.
     */
    public static <T> java.util.Optional<T> tryExecuteWithLock(
            RedissonClient redisson, String lockKey,
            long waitTime, long leaseTime, TimeUnit unit,
            Supplier<T> task) {
        try {
            return java.util.Optional.ofNullable(
                    executeWithLock(redisson, lockKey, waitTime, leaseTime, unit, task));
        } catch (LockAcquisitionException e) {
            log.debug("Could not acquire lock {}, returning empty", lockKey);
            return java.util.Optional.empty();
        }
    }
    
    // ========================================
    // IDEMPOTENCY LOCK PATTERN
    // ========================================
    
    /**
     * Execute with idempotency guarantee.
     * Uses Redis to store the result and return cached result for duplicate requests.
     */
    public static <T> IdempotentResult<T> executeIdempotent(
            RedissonClient redisson, 
            String idempotencyKey,
            long cacheTtl, TimeUnit ttlUnit,
            Supplier<T> task) {
        
        String lockKey = "idempotency:lock:" + idempotencyKey;
        String cacheKey = "idempotency:result:" + idempotencyKey;
        
        // Check if result already exists
        var bucket = redisson.<T>getBucket(cacheKey);
        T cachedResult = bucket.get();
        
        if (cachedResult != null) {
            log.info("Returning cached result for idempotency key: {}", idempotencyKey);
            return IdempotentResult.cached(cachedResult);
        }
        
        // Acquire lock and execute
        return executeWithLock(redisson, lockKey, () -> {
            // Double-check after acquiring lock
            T doubleCheckResult = bucket.get();
            if (doubleCheckResult != null) {
                return IdempotentResult.cached(doubleCheckResult);
            }
            
            // Execute the task
            T result = task.get();
            
            // Cache the result
            bucket.set(result, cacheTtl, ttlUnit);
            
            return IdempotentResult.fresh(result);
        });
    }
    
    /**
     * Result wrapper for idempotent operations
     */
    public record IdempotentResult<T>(
            T result,
            boolean fromCache
    ) {
        public static <T> IdempotentResult<T> cached(T result) {
            return new IdempotentResult<>(result, true);
        }
        
        public static <T> IdempotentResult<T> fresh(T result) {
            return new IdempotentResult<>(result, false);
        }
    }
    
    // ========================================
    // FAIR LOCK (FIFO ORDER)
    // ========================================
    
    /**
     * Execute with a fair lock (FIFO ordering).
     * Useful for payment processing where order matters.
     */
    public static <T> T executeWithFairLock(RedissonClient redisson, String lockKey,
                                             Supplier<T> task) {
        RLock lock = redisson.getFairLock(lockKey);
        boolean acquired = false;
        
        try {
            acquired = lock.tryLock(DEFAULT_WAIT_TIME, DEFAULT_LEASE_TIME, TimeUnit.SECONDS);
            
            if (!acquired) {
                throw new LockAcquisitionException(lockKey, DEFAULT_WAIT_TIME, TimeUnit.SECONDS);
            }
            
            return task.get();
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LockAcquisitionException(lockKey, e);
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
    
    // ========================================
    // MULTI-LOCK (Multiple resources)
    // ========================================
    
    /**
     * Execute with multiple locks atomically.
     */
    public static <T> T executeWithMultiLock(RedissonClient redisson, 
                                              java.util.List<String> lockKeys,
                                              Supplier<T> task) {
        RLock[] locks = lockKeys.stream()
                .map(redisson::getLock)
                .toArray(RLock[]::new);
        
        RLock multiLock = redisson.getMultiLock(locks);
        boolean acquired = false;
        
        try {
            acquired = multiLock.tryLock(DEFAULT_WAIT_TIME, DEFAULT_LEASE_TIME, TimeUnit.SECONDS);
            
            if (!acquired) {
                throw new LockAcquisitionException(
                        String.join(", ", lockKeys), DEFAULT_WAIT_TIME, TimeUnit.SECONDS);
            }
            
            return task.get();
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LockAcquisitionException(String.join(", ", lockKeys), e);
        } finally {
            if (acquired) {
                multiLock.unlock();
            }
        }
    }
    
    // ========================================
    // EXCEPTIONS
    // ========================================
    
    public static class LockAcquisitionException extends RuntimeException {
        private final String lockKey;
        
        public LockAcquisitionException(String lockKey, long waitTime, TimeUnit unit) {
            super("Failed to acquire lock '%s' after %d %s".formatted(lockKey, waitTime, unit));
            this.lockKey = lockKey;
        }
        
        public LockAcquisitionException(String lockKey, Throwable cause) {
            super("Interrupted while acquiring lock '%s'".formatted(lockKey), cause);
            this.lockKey = lockKey;
        }
        
        public String getLockKey() {
            return lockKey;
        }
    }
}
