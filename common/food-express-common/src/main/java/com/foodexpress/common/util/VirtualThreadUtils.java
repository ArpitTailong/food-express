package com.foodexpress.common.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.function.Supplier;

/**
 * Utility class for Virtual Threads and Structured Concurrency (Java 21).
 * Provides convenient methods for async operations with proper context propagation.
 */
public final class VirtualThreadUtils {
    
    private static final Logger log = LoggerFactory.getLogger(VirtualThreadUtils.class);
    
    // Virtual thread executor - unlimited virtual threads
    private static final ExecutorService VIRTUAL_EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();
    
    // Thread factory for named virtual threads
    private static final ThreadFactory NAMED_VIRTUAL_FACTORY = Thread.ofVirtual()
            .name("food-express-vt-", 0)
            .factory();
    
    private VirtualThreadUtils() {} // Utility class
    
    // ========================================
    // BASIC VIRTUAL THREAD OPERATIONS
    // ========================================
    
    /**
     * Run a task on a virtual thread with MDC context propagation
     */
    public static CompletableFuture<Void> runAsync(Runnable task) {
        Map<String, String> contextMap = MDC.getCopyOfContextMap();
        return CompletableFuture.runAsync(() -> {
            try {
                if (contextMap != null) MDC.setContextMap(contextMap);
                task.run();
            } finally {
                MDC.clear();
            }
        }, VIRTUAL_EXECUTOR);
    }
    
    /**
     * Supply a value asynchronously on a virtual thread
     */
    public static <T> CompletableFuture<T> supplyAsync(Supplier<T> supplier) {
        Map<String, String> contextMap = MDC.getCopyOfContextMap();
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (contextMap != null) MDC.setContextMap(contextMap);
                return supplier.get();
            } finally {
                MDC.clear();
            }
        }, VIRTUAL_EXECUTOR);
    }
    
    /**
     * Execute multiple tasks concurrently and wait for all to complete
     */
    @SafeVarargs
    public static <T> java.util.List<T> executeAll(Supplier<T>... suppliers) {
        var futures = java.util.Arrays.stream(suppliers)
                .map(VirtualThreadUtils::supplyAsync)
                .toList();
        
        return futures.stream()
                .map(CompletableFuture::join)
                .toList();
    }
    
    // ========================================
    // CONCURRENT EXECUTION (Non-Preview Alternative)
    // ========================================
    
    /**
     * Execute tasks concurrently - all succeed or fail fast.
     * Uses standard CompletableFuture for Java 21 compatibility without preview.
     * 
     * @param tasks List of tasks to execute
     * @return List of results if all succeed
     * @throws Exception if any task fails
     */
    public static <T> java.util.List<T> executeWithStructuredConcurrency(
            java.util.List<Callable<T>> tasks) throws Exception {
        
        java.util.List<CompletableFuture<T>> futures = tasks.stream()
                .map(task -> CompletableFuture.supplyAsync(() -> {
                    try {
                        return task.call();
                    } catch (Exception e) {
                        throw new CompletionException(e);
                    }
                }, VIRTUAL_EXECUTOR))
                .toList();
        
        // Wait for all and collect results
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        
        return futures.stream()
                .map(CompletableFuture::join)
                .toList();
    }
    
    /**
     * Execute tasks and return first successful result.
     * Uses CompletableFuture.anyOf for first-success semantics.
     */
    @SuppressWarnings("unchecked")
    public static <T> T executeFirstSuccess(java.util.List<Callable<T>> tasks) throws Exception {
        java.util.List<CompletableFuture<T>> futures = tasks.stream()
                .map(task -> CompletableFuture.supplyAsync(() -> {
                    try {
                        return task.call();
                    } catch (Exception e) {
                        throw new CompletionException(e);
                    }
                }, VIRTUAL_EXECUTOR))
                .toList();
        
        return (T) CompletableFuture.anyOf(futures.toArray(new CompletableFuture[0])).join();
    }
    
    // ========================================
    // TIMEOUT UTILITIES
    // ========================================
    
    /**
     * Execute with timeout
     */
    public static <T> T executeWithTimeout(Supplier<T> supplier, long timeout, TimeUnit unit) {
        try {
            return supplyAsync(supplier).get(timeout, unit);
        } catch (TimeoutException e) {
            log.warn("Operation timed out after {} {}", timeout, unit);
            throw new RuntimeException("Operation timed out", e);
        } catch (Exception e) {
            throw new RuntimeException("Operation failed", e);
        }
    }
    
    /**
     * Execute with timeout and fallback
     */
    public static <T> T executeWithTimeoutOrElse(Supplier<T> supplier, long timeout, 
                                                  TimeUnit unit, T fallback) {
        try {
            return supplyAsync(supplier).get(timeout, unit);
        } catch (Exception e) {
            log.warn("Operation failed or timed out, returning fallback: {}", e.getMessage());
            return fallback;
        }
    }
    
    // ========================================
    // RETRY UTILITIES
    // ========================================
    
    /**
     * Execute with exponential backoff retry
     */
    public static <T> T executeWithRetry(Supplier<T> supplier, int maxAttempts, 
                                          long initialDelayMs, double multiplier) {
        int attempt = 0;
        long delay = initialDelayMs;
        
        while (true) {
            try {
                attempt++;
                return supplier.get();
            } catch (Exception e) {
                if (attempt >= maxAttempts) {
                    log.error("All {} attempts failed", maxAttempts);
                    throw new RuntimeException("Max retry attempts exceeded", e);
                }
                
                log.warn("Attempt {} failed, retrying in {} ms: {}", attempt, delay, e.getMessage());
                
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Retry interrupted", ie);
                }
                
                delay = (long) (delay * multiplier);
            }
        }
    }
    
    // ========================================
    // CORRELATION ID UTILITIES
    // ========================================
    
    public static final String CORRELATION_ID_KEY = "correlationId";
    public static final String TRACE_ID_KEY = "traceId";
    public static final String SPAN_ID_KEY = "spanId";
    
    /**
     * Generate a new correlation ID
     */
    public static String generateCorrelationId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
    
    /**
     * Set correlation ID in MDC
     */
    public static void setCorrelationId(String correlationId) {
        MDC.put(CORRELATION_ID_KEY, correlationId);
    }
    
    /**
     * Get current correlation ID from MDC
     */
    public static String getCorrelationId() {
        String correlationId = MDC.get(CORRELATION_ID_KEY);
        return correlationId != null ? correlationId : generateCorrelationId();
    }
    
    /**
     * Execute with new correlation ID
     */
    public static <T> T withNewCorrelationId(Supplier<T> supplier) {
        try {
            MDC.put(CORRELATION_ID_KEY, generateCorrelationId());
            return supplier.get();
        } finally {
            MDC.remove(CORRELATION_ID_KEY);
        }
    }
    
    // ========================================
    // SHUTDOWN HOOK
    // ========================================
    
    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down virtual thread executor");
            VIRTUAL_EXECUTOR.shutdown();
            try {
                if (!VIRTUAL_EXECUTOR.awaitTermination(30, TimeUnit.SECONDS)) {
                    VIRTUAL_EXECUTOR.shutdownNow();
                }
            } catch (InterruptedException e) {
                VIRTUAL_EXECUTOR.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }));
    }
}
