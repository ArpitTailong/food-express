package com.foodexpress.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Payment Service for FoodExpress.
 * 
 * CRITICAL FEATURES:
 * - Idempotent payment APIs
 * - Payment state machine (CREATED → PROCESSING → SUCCESS/FAILED → REFUNDED)
 * - Distributed locking for concurrent payment prevention
 * - Saga pattern for distributed transactions
 * - Exactly-once semantics with Kafka
 * - PCI-DSS compliant (tokenization - no card data stored)
 * - Rate limiting on payment endpoints
 * - Retry with exponential backoff
 * 
 * PAYMENT FLOW:
 * 1. Client initiates payment with idempotency key
 * 2. Service acquires distributed lock
 * 3. Payment state machine validates transition
 * 4. Gateway called (with circuit breaker)
 * 5. Event published to Kafka
 * 6. Order service updated via saga
 */
@SpringBootApplication(scanBasePackages = {
        "com.foodexpress.payment",
        "com.foodexpress.common"
})
@EnableDiscoveryClient
@EnableFeignClients
@EnableScheduling
@EnableAsync
public class PaymentServiceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }
}
