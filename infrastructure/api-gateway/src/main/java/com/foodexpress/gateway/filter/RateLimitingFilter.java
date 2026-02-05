package com.foodexpress.gateway.filter;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Custom rate limiting filter for specific endpoints.
 * Especially important for payment endpoints.
 */
@Component
public class RateLimitingFilter extends AbstractGatewayFilterFactory<RateLimitingFilter.Config> {
    
    private static final Logger log = LoggerFactory.getLogger(RateLimitingFilter.class);
    
    private final ConcurrentHashMap<String, RateLimiter> rateLimiters = new ConcurrentHashMap<>();
    
    public RateLimitingFilter() {
        super(Config.class);
    }
    
    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            // Get client identifier (IP or user ID)
            String clientId = extractClientId(exchange);
            String rateLimiterKey = config.getName() + ":" + clientId;
            
            // Get or create rate limiter for this client
            RateLimiter rateLimiter = rateLimiters.computeIfAbsent(rateLimiterKey, key -> {
                RateLimiterConfig rateLimiterConfig = RateLimiterConfig.custom()
                        .limitForPeriod(config.getLimitForPeriod())
                        .limitRefreshPeriod(Duration.ofSeconds(config.getLimitRefreshPeriodSeconds()))
                        .timeoutDuration(Duration.ofMillis(config.getTimeoutDurationMs()))
                        .build();
                return RateLimiterRegistry.of(rateLimiterConfig).rateLimiter(key);
            });
            
            // Try to acquire permission
            boolean permitted = rateLimiter.acquirePermission();
            
            if (!permitted) {
                log.warn("Rate limit exceeded for client: {} on endpoint: {}", 
                        clientId, config.getName());
                
                exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                exchange.getResponse().getHeaders().add("Retry-After", 
                        String.valueOf(config.getLimitRefreshPeriodSeconds()));
                exchange.getResponse().getHeaders().add("X-RateLimit-Limit", 
                        String.valueOf(config.getLimitForPeriod()));
                
                return exchange.getResponse().setComplete();
            }
            
            return chain.filter(exchange);
        };
    }
    
    private String extractClientId(org.springframework.web.server.ServerWebExchange exchange) {
        // Try to get user ID from JWT first
        var principal = exchange.getPrincipal().block();
        if (principal != null) {
            return principal.getName();
        }
        
        // Fall back to IP address
        var remoteAddress = exchange.getRequest().getRemoteAddress();
        if (remoteAddress != null) {
            return remoteAddress.getAddress().getHostAddress();
        }
        
        return "anonymous";
    }
    
    /**
     * Configuration for rate limiting
     */
    public static class Config {
        private String name = "default";
        private int limitForPeriod = 100;
        private int limitRefreshPeriodSeconds = 1;
        private int timeoutDurationMs = 0;
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public int getLimitForPeriod() {
            return limitForPeriod;
        }
        
        public void setLimitForPeriod(int limitForPeriod) {
            this.limitForPeriod = limitForPeriod;
        }
        
        public int getLimitRefreshPeriodSeconds() {
            return limitRefreshPeriodSeconds;
        }
        
        public void setLimitRefreshPeriodSeconds(int limitRefreshPeriodSeconds) {
            this.limitRefreshPeriodSeconds = limitRefreshPeriodSeconds;
        }
        
        public int getTimeoutDurationMs() {
            return timeoutDurationMs;
        }
        
        public void setTimeoutDurationMs(int timeoutDurationMs) {
            this.timeoutDurationMs = timeoutDurationMs;
        }
    }
}
