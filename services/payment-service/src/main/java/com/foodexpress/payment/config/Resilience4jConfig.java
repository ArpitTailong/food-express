package com.foodexpress.payment.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Resilience4j configuration for Payment Service.
 * 
 * Patterns configured:
 * - Circuit Breaker: Prevent cascading failures
 * - Retry: Automatic retry with exponential backoff
 * - Rate Limiter: Protect against abuse
 * - Time Limiter: Timeout protection
 */
@Configuration
public class Resilience4jConfig {
    
    // ========================================
    // CIRCUIT BREAKER
    // ========================================
    
    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        // Payment Gateway circuit breaker
        CircuitBreakerConfig gatewayConfig = CircuitBreakerConfig.custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .failureRateThreshold(50.0f)
                .slowCallRateThreshold(50.0f)
                .slowCallDurationThreshold(Duration.ofSeconds(5))
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .permittedNumberOfCallsInHalfOpenState(3)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .recordExceptions(
                        RuntimeException.class,
                        Exception.class
                )
                .ignoreExceptions(
                        IllegalArgumentException.class,
                        IllegalStateException.class
                )
                .build();
        
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();
        registry.addConfiguration("paymentGateway", gatewayConfig);
        
        // Standard circuit breaker (less aggressive)
        CircuitBreakerConfig standardConfig = CircuitBreakerConfig.custom()
                .slidingWindowSize(20)
                .minimumNumberOfCalls(10)
                .failureRateThreshold(60.0f)
                .waitDurationInOpenState(Duration.ofSeconds(60))
                .build();
        
        registry.addConfiguration("standard", standardConfig);
        
        return registry;
    }
    
    // ========================================
    // RETRY
    // ========================================
    
    @Bean
    public RetryRegistry retryRegistry() {
        // Payment Gateway retry with exponential backoff
        RetryConfig gatewayRetryConfig = RetryConfig.custom()
                .maxAttempts(3)
                .intervalFunction(io.github.resilience4j.core.IntervalFunction
                        .ofExponentialBackoff(500, 2.0)) // 500ms, 1s, 2s
                .retryExceptions(
                        java.net.SocketTimeoutException.class,
                        java.io.IOException.class,
                        java.util.concurrent.TimeoutException.class
                )
                .ignoreExceptions(
                        IllegalArgumentException.class,
                        IllegalStateException.class
                )
                .build();
        
        RetryRegistry registry = RetryRegistry.ofDefaults();
        registry.addConfiguration("paymentGateway", gatewayRetryConfig);
        
        // Database retry (faster, fewer attempts)
        RetryConfig dbRetryConfig = RetryConfig.custom()
                .maxAttempts(2)
                .intervalFunction(io.github.resilience4j.core.IntervalFunction.ofDefaults())
                .retryExceptions(
                        org.springframework.dao.TransientDataAccessException.class,
                        org.springframework.transaction.CannotCreateTransactionException.class
                )
                .build();
        
        registry.addConfiguration("database", dbRetryConfig);
        
        return registry;
    }
    
    // ========================================
    // RATE LIMITER
    // ========================================
    
    @Bean
    public RateLimiterRegistry rateLimiterRegistry() {
        // Payment creation: 10 requests per minute per user
        RateLimiterConfig createPaymentConfig = RateLimiterConfig.custom()
                .limitRefreshPeriod(Duration.ofMinutes(1))
                .limitForPeriod(10)
                .timeoutDuration(Duration.ofSeconds(5))
                .build();
        
        RateLimiterRegistry registry = RateLimiterRegistry.ofDefaults();
        registry.addConfiguration("createPayment", createPaymentConfig);
        
        // Refund: 5 requests per minute (admin operation)
        RateLimiterConfig refundConfig = RateLimiterConfig.custom()
                .limitRefreshPeriod(Duration.ofMinutes(1))
                .limitForPeriod(5)
                .timeoutDuration(Duration.ofSeconds(5))
                .build();
        
        registry.addConfiguration("refundPayment", refundConfig);
        
        // Retry: 3 requests per minute
        RateLimiterConfig retryConfig = RateLimiterConfig.custom()
                .limitRefreshPeriod(Duration.ofMinutes(1))
                .limitForPeriod(3)
                .timeoutDuration(Duration.ofSeconds(5))
                .build();
        
        registry.addConfiguration("retryPayment", retryConfig);
        
        // Standard: 100 requests per minute
        RateLimiterConfig standardConfig = RateLimiterConfig.custom()
                .limitRefreshPeriod(Duration.ofMinutes(1))
                .limitForPeriod(100)
                .timeoutDuration(Duration.ofSeconds(5))
                .build();
        
        registry.addConfiguration("standard", standardConfig);
        
        return registry;
    }
    
    // ========================================
    // TIME LIMITER
    // ========================================
    
    @Bean
    public TimeLimiterRegistry timeLimiterRegistry() {
        // Payment Gateway timeout: 10 seconds
        TimeLimiterConfig gatewayConfig = TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(10))
                .cancelRunningFuture(true)
                .build();
        
        TimeLimiterRegistry registry = TimeLimiterRegistry.ofDefaults();
        registry.addConfiguration("paymentGateway", gatewayConfig);
        
        // Database timeout: 5 seconds
        TimeLimiterConfig dbConfig = TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(5))
                .cancelRunningFuture(true)
                .build();
        
        registry.addConfiguration("database", dbConfig);
        
        return registry;
    }
}
