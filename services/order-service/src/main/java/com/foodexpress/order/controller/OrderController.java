package com.foodexpress.order.controller;

import com.foodexpress.order.dto.OrderDTOs.*;
import com.foodexpress.order.service.OrderService;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Order Management REST API.
 * 
 * Provides endpoints for:
 * - Creating and managing orders
 * - Checkout and payment initiation
 * - Order status tracking
 * - Ratings and feedback
 */
@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "Orders", description = "Order management APIs")
public class OrderController {
    
    private static final Logger log = LoggerFactory.getLogger(OrderController.class);
    
    private final OrderService orderService;
    
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }
    
    // ========================================
    // CUSTOMER ENDPOINTS
    // ========================================
    
    @PostMapping
    @RateLimiter(name = "createOrder")
    @Timed(value = "order.create", description = "Time taken to create order")
    @Operation(summary = "Create a new order", description = "Creates an order in PENDING status")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Order created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid order data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<OrderResponse> createOrder(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateOrderRequest request,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        
        String customerId = jwt.getSubject();
        if (correlationId == null) {
            correlationId = UUID.randomUUID().toString();
        }
        
        log.info("Creating order for customer {} with correlation ID {}", customerId, correlationId);
        
        OrderResponse response = orderService.createOrder(customerId, request, correlationId);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @PostMapping("/{orderId}/checkout")
    @RateLimiter(name = "checkout")
    @Timed(value = "order.checkout", description = "Time taken to checkout order")
    @Operation(summary = "Checkout order", description = "Initiates payment for the order")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Checkout initiated"),
        @ApiResponse(responseCode = "400", description = "Invalid checkout request or order state"),
        @ApiResponse(responseCode = "404", description = "Order not found")
    })
    public ResponseEntity<CheckoutResponse> checkout(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String orderId,
            @Valid @RequestBody CheckoutRequest request,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey) {
        
        String customerId = jwt.getSubject();
        if (correlationId == null) {
            correlationId = UUID.randomUUID().toString();
        }
        
        log.info("Checkout for order {} by customer {}", orderId, customerId);
        
        // Verify ownership first
        orderService.getOrderForCustomer(orderId, customerId);
        
        CheckoutResponse response = orderService.checkout(orderId, request, correlationId);
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{orderId}")
    @Timed(value = "order.get", description = "Time taken to get order")
    @Operation(summary = "Get order by ID", description = "Retrieves order details for the authenticated customer")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Order found"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "Order not found")
    })
    public ResponseEntity<OrderResponse> getOrder(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String orderId) {
        
        String customerId = jwt.getSubject();
        
        OrderResponse response = orderService.getOrderForCustomer(orderId, customerId);
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping
    @Timed(value = "order.list", description = "Time taken to list orders")
    @Operation(summary = "List customer orders", description = "Lists all orders for the authenticated customer")
    public ResponseEntity<Page<OrderResponse>> getMyOrders(
            @AuthenticationPrincipal Jwt jwt,
            @PageableDefault(size = 20) Pageable pageable) {
        
        String customerId = jwt.getSubject();
        
        Page<OrderResponse> orders = orderService.getCustomerOrders(customerId, pageable);
        
        return ResponseEntity.ok(orders);
    }
    
    @GetMapping("/active")
    @Operation(summary = "Get active orders", description = "Gets currently active orders for the customer")
    public ResponseEntity<List<OrderResponse>> getActiveOrders(
            @AuthenticationPrincipal Jwt jwt) {
        
        String customerId = jwt.getSubject();
        
        List<OrderResponse> orders = orderService.getActiveOrdersForCustomer(customerId);
        
        return ResponseEntity.ok(orders);
    }
    
    @PostMapping("/{orderId}/cancel")
    @Timed(value = "order.cancel", description = "Time taken to cancel order")
    @Operation(summary = "Cancel order", description = "Cancels an order if still cancellable")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Order cancelled"),
        @ApiResponse(responseCode = "400", description = "Order cannot be cancelled"),
        @ApiResponse(responseCode = "404", description = "Order not found")
    })
    public ResponseEntity<OrderResponse> cancelOrder(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String orderId,
            @Valid @RequestBody CancelOrderRequest request) {
        
        String customerId = jwt.getSubject();
        
        // Verify ownership
        orderService.getOrderForCustomer(orderId, customerId);
        
        OrderResponse response = orderService.cancelOrder(orderId, request.reason(), customerId);
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/{orderId}/rate")
    @Timed(value = "order.rate", description = "Time taken to rate order")
    @Operation(summary = "Rate order", description = "Submit rating and feedback for a delivered order")
    public ResponseEntity<OrderResponse> rateOrder(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String orderId,
            @Valid @RequestBody RateOrderRequest request) {
        
        String customerId = jwt.getSubject();
        
        OrderResponse response = orderService.rateOrder(orderId, customerId, request);
        
        return ResponseEntity.ok(response);
    }
    
    // ========================================
    // RESTAURANT ENDPOINTS
    // ========================================
    
    @GetMapping("/restaurant/{restaurantId}/active")
    @Operation(summary = "Get active orders for restaurant")
    public ResponseEntity<List<OrderResponse>> getRestaurantActiveOrders(
            @PathVariable String restaurantId) {
        
        // TODO: Verify caller is authorized for this restaurant
        List<OrderResponse> orders = orderService.getActiveOrdersForRestaurant(restaurantId);
        
        return ResponseEntity.ok(orders);
    }
    
    @GetMapping("/restaurant/{restaurantId}/pending")
    @Operation(summary = "Get pending orders for restaurant")
    public ResponseEntity<List<OrderResponse>> getRestaurantPendingOrders(
            @PathVariable String restaurantId) {
        
        List<OrderResponse> orders = orderService.getPendingOrdersForRestaurant(restaurantId);
        
        return ResponseEntity.ok(orders);
    }
    
    @PostMapping("/{orderId}/prepare")
    @Operation(summary = "Start preparing order", description = "Restaurant marks order as being prepared")
    public ResponseEntity<OrderResponse> startPreparing(
            @PathVariable String orderId) {
        
        // TODO: Verify caller is the restaurant for this order
        OrderResponse response = orderService.startPreparing(orderId);
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/{orderId}/ready")
    @Operation(summary = "Mark order ready", description = "Restaurant marks order as ready for pickup")
    public ResponseEntity<OrderResponse> markReady(
            @PathVariable String orderId) {
        
        OrderResponse response = orderService.markReady(orderId);
        
        return ResponseEntity.ok(response);
    }
    
    // ========================================
    // DRIVER ENDPOINTS
    // ========================================
    
    @GetMapping("/driver/available")
    @Operation(summary = "Get orders awaiting driver", description = "Lists orders ready for driver pickup")
    public ResponseEntity<List<OrderResponse>> getOrdersAwaitingDriver() {
        
        List<OrderResponse> orders = orderService.getOrdersAwaitingDriver();
        
        return ResponseEntity.ok(orders);
    }
    
    @GetMapping("/driver/{driverId}/active")
    @Operation(summary = "Get driver's active deliveries")
    public ResponseEntity<List<OrderResponse>> getDriverActiveDeliveries(
            @PathVariable String driverId) {
        
        List<OrderResponse> orders = orderService.getActiveDeliveriesForDriver(driverId);
        
        return ResponseEntity.ok(orders);
    }
    
    @PostMapping("/{orderId}/pickup")
    @Operation(summary = "Pick up order", description = "Driver picks up order for delivery")
    public ResponseEntity<OrderResponse> pickupOrder(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String orderId) {
        
        String driverId = jwt.getSubject();
        
        OrderResponse response = orderService.startDelivery(orderId, driverId);
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/{orderId}/deliver")
    @Operation(summary = "Mark as delivered", description = "Driver marks order as delivered")
    public ResponseEntity<OrderResponse> markDelivered(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String orderId) {
        
        // TODO: Verify caller is the assigned driver
        OrderResponse response = orderService.markDelivered(orderId);
        
        return ResponseEntity.ok(response);
    }
    
    // ========================================
    // ADMIN ENDPOINTS
    // ========================================
    
    @GetMapping("/admin/{orderId}")
    @Operation(summary = "Get any order (admin)", description = "Admin access to any order")
    public ResponseEntity<OrderResponse> getOrderAdmin(
            @PathVariable String orderId) {
        
        return orderService.getOrder(orderId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
