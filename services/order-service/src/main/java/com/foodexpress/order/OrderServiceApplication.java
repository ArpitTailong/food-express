package com.foodexpress.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Order Service - Saga Orchestrator for FoodExpress.
 * 
 * This is the central service that coordinates the order lifecycle:
 * 
 * ORDER FLOW (Saga Pattern):
 * 1. Customer creates order → Order PENDING
 * 2. Payment initiated → Waiting for Payment Service
 * 3. Payment success → Order CONFIRMED, notify Restaurant
 * 4. Restaurant accepts → Order PREPARING
 * 5. Restaurant ready → Order READY_FOR_PICKUP
 * 6. Driver assigned → Order OUT_FOR_DELIVERY
 * 7. Delivered → Order DELIVERED
 * 
 * COMPENSATING TRANSACTIONS:
 * - Payment fails → Cancel Order
 * - Restaurant rejects → Refund Payment, Cancel Order
 * - Customer cancels → Refund Payment (if paid), Cancel Order
 * - Delivery fails → Refund Payment, Cancel Order
 * 
 * EVENTS PUBLISHED:
 * - OrderCreated, OrderConfirmed, OrderPreparing
 * - OrderReady, OrderOutForDelivery, OrderDelivered
 * - OrderCancelled, OrderFailed
 * 
 * EVENTS CONSUMED:
 * - PaymentCompleted, PaymentFailed, PaymentRefunded
 * - RestaurantAccepted, RestaurantRejected
 * - DriverAssigned, DeliveryCompleted, DeliveryFailed
 */
@SpringBootApplication(scanBasePackages = {
        "com.foodexpress.order",
        "com.foodexpress.common"
})
@EnableDiscoveryClient
@EnableFeignClients
@EnableAsync
@EnableScheduling
public class OrderServiceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
