package com.foodexpress.order.service;

import com.foodexpress.order.domain.*;
import com.foodexpress.order.dto.OrderDTOs;
import com.foodexpress.order.dto.OrderDTOs.*;
import com.foodexpress.order.messaging.OrderEventPublisherInterface;
import com.foodexpress.order.repository.OrderRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Order Service - Core business logic and saga orchestration.
 * 
 * Responsibilities:
 * - Order lifecycle management
 * - State machine enforcement
 * - Saga coordination via events
 * - Pricing and fee calculation
 * - Customer/Restaurant/Driver queries
 */
@Service
public class OrderService {
    
    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    
    private static final BigDecimal TAX_RATE = new BigDecimal("0.18"); // 18% GST
    private static final BigDecimal DEFAULT_DELIVERY_FEE = new BigDecimal("40.00");
    private static final int ESTIMATED_PREP_MINUTES = 25;
    private static final int ESTIMATED_DELIVERY_MINUTES = 35;
    
    private final OrderRepository orderRepository;
    private final OrderEventPublisherInterface eventPublisher;
    
    // Metrics
    private final Counter ordersCreated;
    private final Counter ordersConfirmed;
    private final Counter ordersDelivered;
    private final Counter ordersCancelled;
    private final Counter ordersFailed;
    
    public OrderService(
            OrderRepository orderRepository,
            OrderEventPublisherInterface eventPublisher,
            MeterRegistry meterRegistry) {
        
        this.orderRepository = orderRepository;
        this.eventPublisher = eventPublisher;
        
        // Initialize metrics
        this.ordersCreated = Counter.builder("orders.created").register(meterRegistry);
        this.ordersConfirmed = Counter.builder("orders.confirmed").register(meterRegistry);
        this.ordersDelivered = Counter.builder("orders.delivered").register(meterRegistry);
        this.ordersCancelled = Counter.builder("orders.cancelled").register(meterRegistry);
        this.ordersFailed = Counter.builder("orders.failed").register(meterRegistry);
    }
    
    // ========================================
    // ORDER CREATION
    // ========================================
    
    @Transactional
    public OrderResponse createOrder(String customerId, CreateOrderRequest request, String correlationId) {
        log.info("Creating order for customer {} at restaurant {}", customerId, request.restaurantId());
        
        // Create order
        DeliveryAddress deliveryAddress = OrderDTOs.toDeliveryAddress(request.deliveryAddress());
        Order order = new Order(customerId, request.restaurantId(), deliveryAddress);
        order.setCorrelationId(correlationId);
        order.setDeliveryInstructions(request.deliveryInstructions());
        order.setCouponCode(request.couponCode());
        
        // Add items
        for (OrderItemRequest itemRequest : request.items()) {
            OrderItem item = new OrderItem(
                    itemRequest.menuItemId(),
                    itemRequest.menuItemName(),
                    itemRequest.unitPrice(),
                    itemRequest.quantity()
            );
            item.setSpecialInstructions(itemRequest.specialInstructions());
            order.addItem(item);
        }
        
        // Calculate fees
        order.setDeliveryFee(DEFAULT_DELIVERY_FEE);
        order.setTaxAmount(order.getSubtotal().multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP));
        if (request.tipAmount() != null) {
            order.setTipAmount(request.tipAmount());
        }
        
        // Apply coupon discount (TODO: integrate with promotion service)
        if (request.couponCode() != null) {
            // Placeholder: 10% discount for any coupon
            BigDecimal discount = order.getSubtotal().multiply(new BigDecimal("0.10"))
                    .setScale(2, RoundingMode.HALF_UP);
            order.setDiscountAmount(discount);
        }
        
        // Calculate estimated delivery time
        order.setEstimatedDeliveryTime(
                LocalDateTime.now().plusMinutes(ESTIMATED_PREP_MINUTES + ESTIMATED_DELIVERY_MINUTES));
        
        // Save order
        order = orderRepository.save(order);
        ordersCreated.increment();
        
        log.info("Order {} created. Total: {} {}", order.getId(), order.getTotalAmount(), order.getCurrency());
        
        // Publish event
        eventPublisher.publishOrderCreated(order);
        
        return OrderDTOs.toResponse(order);
    }
    
    // ========================================
    // CHECKOUT / PAYMENT
    // ========================================
    
    @Transactional
    public CheckoutResponse checkout(String orderId, CheckoutRequest request, String correlationId) {
        log.info("Processing checkout for order {}", orderId);
        
        Order order = orderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        
        // Validate state
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new InvalidOrderStateException(
                    "Cannot checkout order in status: " + order.getStatus());
        }
        
        // Initiate payment
        order.initiatePayment(request.paymentMethod());
        order.setCorrelationId(correlationId);
        order = orderRepository.save(order);
        
        // Publish event for Payment Service to process
        eventPublisher.publishPaymentInitiated(order);
        
        log.info("Checkout initiated for order {}. Waiting for payment.", orderId);
        
        // Return checkout response (payment will be processed asynchronously)
        return new CheckoutResponse(
                order.getId(),
                order.getStatus().name(),
                new CheckoutResponse.PaymentInfo(
                        null, // Payment ID will be set after payment processing
                        "PENDING",
                        null // Next action will be provided by payment service
                )
        );
    }
    
    // ========================================
    // SAGA EVENT HANDLERS
    // ========================================
    
    @Transactional
    public void confirmOrderAfterPayment(String orderId, String paymentId) {
        log.info("Confirming order {} after payment {}", orderId, paymentId);
        
        Order order = orderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        
        if (order.getStatus() != OrderStatus.PAYMENT_PENDING) {
            log.warn("Order {} not in PAYMENT_PENDING state (current: {}). Skipping confirmation.",
                    orderId, order.getStatus());
            return;
        }
        
        order.confirmOrder(paymentId);
        order = orderRepository.save(order);
        ordersConfirmed.increment();
        
        log.info("Order {} confirmed. Notifying restaurant.", orderId);
        
        // Publish event for Restaurant Service
        eventPublisher.publishOrderConfirmed(order);
    }
    
    @Transactional
    public void handlePaymentFailure(String orderId, String reason) {
        log.warn("Handling payment failure for order {}: {}", orderId, reason);
        
        Order order = orderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        
        if (order.getStatus() != OrderStatus.PAYMENT_PENDING) {
            log.warn("Order {} not in PAYMENT_PENDING state. Skipping failure handling.", orderId);
            return;
        }
        
        order.paymentFailed(reason);
        order = orderRepository.save(order);
        ordersFailed.increment();
        
        log.warn("Order {} marked as FAILED due to payment failure", orderId);
        
        // Publish failure event
        eventPublisher.publishOrderFailed(order, "PAYMENT");
    }
    
    @Transactional
    public void handlePaymentRefunded(String orderId) {
        log.info("Handling payment refund for order {}", orderId);
        
        // Just log for now - order should already be CANCELLED or FAILED
        orderRepository.findById(orderId).ifPresent(order -> {
            log.info("Order {} (status: {}) payment has been refunded", orderId, order.getStatus());
        });
    }
    
    // ========================================
    // STATUS UPDATES
    // ========================================
    
    @Transactional
    public OrderResponse startPreparing(String orderId) {
        log.info("Starting preparation for order {}", orderId);
        
        Order order = orderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        
        order.startPreparing();
        order = orderRepository.save(order);
        
        eventPublisher.publishOrderPreparing(order);
        
        return OrderDTOs.toResponse(order);
    }
    
    @Transactional
    public OrderResponse markReady(String orderId) {
        log.info("Marking order {} as ready for pickup", orderId);
        
        Order order = orderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        
        order.markReady();
        order = orderRepository.save(order);
        
        eventPublisher.publishOrderReady(order);
        
        return OrderDTOs.toResponse(order);
    }
    
    @Transactional
    public OrderResponse startDelivery(String orderId, String driverId) {
        log.info("Starting delivery for order {} with driver {}", orderId, driverId);
        
        Order order = orderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        
        order.startDelivery(driverId);
        order = orderRepository.save(order);
        
        eventPublisher.publishOutForDelivery(order);
        
        return OrderDTOs.toResponse(order);
    }
    
    @Transactional
    public OrderResponse markDelivered(String orderId) {
        log.info("Marking order {} as delivered", orderId);
        
        Order order = orderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        
        order.markDelivered();
        order = orderRepository.save(order);
        ordersDelivered.increment();
        
        eventPublisher.publishOrderDelivered(order);
        
        return OrderDTOs.toResponse(order);
    }
    
    // ========================================
    // CANCELLATION
    // ========================================
    
    @Transactional
    public OrderResponse cancelOrder(String orderId, String reason, String cancelledBy) {
        log.info("Cancelling order {} by {} - reason: {}", orderId, cancelledBy, reason);
        
        Order order = orderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        
        if (!order.getStatus().isCancellable()) {
            throw new InvalidOrderStateException(
                    "Cannot cancel order in status: " + order.getStatus());
        }
        
        order.cancel(reason, cancelledBy);
        order = orderRepository.save(order);
        ordersCancelled.increment();
        
        // Publish cancellation event (triggers refund if payment was made)
        eventPublisher.publishOrderCancelled(order);
        
        return OrderDTOs.toResponse(order);
    }
    
    // ========================================
    // RATINGS
    // ========================================
    
    @Transactional
    public OrderResponse rateOrder(String orderId, String customerId, RateOrderRequest request) {
        Order order = orderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        
        // Verify ownership
        if (!order.getCustomerId().equals(customerId)) {
            throw new UnauthorizedAccessException("Cannot rate order belonging to another customer");
        }
        
        // Only delivered orders can be rated
        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new InvalidOrderStateException("Can only rate delivered orders");
        }
        
        if (request.restaurantRating() != null) {
            order.setRestaurantRating(request.restaurantRating());
        }
        if (request.driverRating() != null) {
            order.setDriverRating(request.driverRating());
        }
        if (request.feedback() != null) {
            order.setCustomerFeedback(request.feedback());
        }
        
        order = orderRepository.save(order);
        
        return OrderDTOs.toResponse(order);
    }
    
    // ========================================
    // QUERIES
    // ========================================
    
    @Transactional(readOnly = true)
    public Optional<OrderResponse> getOrder(String orderId) {
        return orderRepository.findById(orderId)
                .map(OrderDTOs::toResponse);
    }
    
    @Transactional(readOnly = true)
    public OrderResponse getOrderForCustomer(String orderId, String customerId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        
        if (!order.getCustomerId().equals(customerId)) {
            throw new UnauthorizedAccessException("Cannot access order belonging to another customer");
        }
        
        return OrderDTOs.toResponse(order);
    }
    
    @Transactional(readOnly = true)
    public Page<OrderResponse> getCustomerOrders(String customerId, Pageable pageable) {
        return orderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId, pageable)
                .map(OrderDTOs::toResponse);
    }
    
    @Transactional(readOnly = true)
    public List<OrderResponse> getActiveOrdersForCustomer(String customerId) {
        return orderRepository.findActiveOrdersByCustomer(customerId).stream()
                .map(OrderDTOs::toResponse)
                .toList();
    }
    
    @Transactional(readOnly = true)
    public List<OrderResponse> getActiveOrdersForRestaurant(String restaurantId) {
        return orderRepository.findActiveOrdersByRestaurant(restaurantId).stream()
                .map(OrderDTOs::toResponse)
                .toList();
    }
    
    @Transactional(readOnly = true)
    public List<OrderResponse> getPendingOrdersForRestaurant(String restaurantId) {
        return orderRepository.findPendingOrdersForRestaurant(restaurantId).stream()
                .map(OrderDTOs::toResponse)
                .toList();
    }
    
    @Transactional(readOnly = true)
    public List<OrderResponse> getActiveDeliveriesForDriver(String driverId) {
        return orderRepository.findActiveDeliveriesForDriver(driverId).stream()
                .map(OrderDTOs::toResponse)
                .toList();
    }
    
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersAwaitingDriver() {
        return orderRepository.findOrdersAwaitingDriverAssignment().stream()
                .map(OrderDTOs::toResponse)
                .toList();
    }
    
    // ========================================
    // EXCEPTION CLASSES
    // ========================================
    
    public static class OrderNotFoundException extends RuntimeException {
        public OrderNotFoundException(String orderId) {
            super("Order not found: " + orderId);
        }
    }
    
    public static class InvalidOrderStateException extends RuntimeException {
        public InvalidOrderStateException(String message) {
            super(message);
        }
    }
    
    public static class UnauthorizedAccessException extends RuntimeException {
        public UnauthorizedAccessException(String message) {
            super(message);
        }
    }
}
