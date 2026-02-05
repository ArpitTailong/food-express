package com.foodexpress.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Global filter to add and propagate correlation ID across services.
 * Ensures distributed tracing capability.
 */
@Component
public class CorrelationIdFilter implements GlobalFilter, Ordered {
    
    private static final Logger log = LoggerFactory.getLogger(CorrelationIdFilter.class);
    
    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    public static final String REQUEST_ID_HEADER = "X-Request-ID";
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        
        // Get or generate correlation ID
        String correlationId = request.getHeaders().getFirst(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = generateId();
        }
        
        // Generate request ID (unique per request)
        String requestId = generateId();
        
        // Add headers to downstream request
        String finalCorrelationId = correlationId;
        ServerHttpRequest modifiedRequest = request.mutate()
                .header(CORRELATION_ID_HEADER, correlationId)
                .header(REQUEST_ID_HEADER, requestId)
                .build();
        
        // Add to response headers
        exchange.getResponse().getHeaders().add(CORRELATION_ID_HEADER, correlationId);
        exchange.getResponse().getHeaders().add(REQUEST_ID_HEADER, requestId);
        
        // Log the request
        log.info("Gateway Request: {} {} | CorrelationId: {} | RequestId: {}",
                request.getMethod(), request.getPath(), correlationId, requestId);
        
        long startTime = System.currentTimeMillis();
        
        return chain.filter(exchange.mutate().request(modifiedRequest).build())
                .doFinally(signalType -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.info("Gateway Response: {} {} | Status: {} | Duration: {}ms | CorrelationId: {}",
                            request.getMethod(),
                            request.getPath(),
                            exchange.getResponse().getStatusCode(),
                            duration,
                            finalCorrelationId);
                });
    }
    
    @Override
    public int getOrder() {
        // Execute early in the filter chain
        return Ordered.HIGHEST_PRECEDENCE;
    }
    
    private String generateId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
