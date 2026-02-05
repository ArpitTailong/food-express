package com.foodexpress.gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;

/**
 * Fallback controller for circuit breaker scenarios.
 * Returns graceful degradation responses when services are unavailable.
 */
@RestController
@RequestMapping("/fallback")
public class FallbackController {
    
    @GetMapping(value = "/user", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Map<String, Object>> userServiceFallback() {
        return createFallbackResponse("user-service", "User service is temporarily unavailable");
    }
    
    @GetMapping(value = "/order", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Map<String, Object>> orderServiceFallback() {
        return createFallbackResponse("order-service", "Order service is temporarily unavailable");
    }
    
    @GetMapping(value = "/payment", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Map<String, Object>> paymentServiceFallback() {
        return createFallbackResponse("payment-service", 
                "Payment service is temporarily unavailable. Please try again later.");
    }
    
    @GetMapping(value = "/notification", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Map<String, Object>> notificationServiceFallback() {
        return createFallbackResponse("notification-service", 
                "Notification service is temporarily unavailable");
    }
    
    @GetMapping(value = "/analytics", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Map<String, Object>> analyticsServiceFallback() {
        return createFallbackResponse("analytics-service", 
                "Analytics service is temporarily unavailable");
    }
    
    private Mono<Map<String, Object>> createFallbackResponse(String service, String message) {
        return Mono.just(Map.of(
                "success", false,
                "message", message,
                "service", service,
                "timestamp", Instant.now().toString(),
                "status", HttpStatus.SERVICE_UNAVAILABLE.value(),
                "fallback", true
        ));
    }
}
