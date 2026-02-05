package com.foodexpress.common.domain;

/**
 * Java 21 Sealed Interface for Order States.
 * 
 * Order State Machine:
 * PENDING → CONFIRMED → PREPARING → READY → PICKED_UP → DELIVERED
 *        ↘ CANCELLED
 *                    ↘ CANCELLED (before READY)
 */
public sealed interface OrderState permits
        OrderState.Pending,
        OrderState.Confirmed,
        OrderState.Preparing,
        OrderState.Ready,
        OrderState.PickedUp,
        OrderState.Delivered,
        OrderState.Cancelled {
    
    String name();
    
    default boolean isTerminal() {
        return this instanceof Delivered || this instanceof Cancelled;
    }
    
    default boolean canCancel() {
        return this instanceof Pending || this instanceof Confirmed || this instanceof Preparing;
    }
    
    // ========================================
    // SEALED IMPLEMENTATIONS
    // ========================================
    
    record Pending(String orderId, java.time.Instant createdAt) implements OrderState {
        @Override public String name() { return "PENDING"; }
    }
    
    record Confirmed(
            String orderId,
            java.time.Instant confirmedAt,
            String paymentId,
            java.time.Duration estimatedPrepTime
    ) implements OrderState {
        @Override public String name() { return "CONFIRMED"; }
    }
    
    record Preparing(
            String orderId,
            java.time.Instant startedAt,
            String restaurantId
    ) implements OrderState {
        @Override public String name() { return "PREPARING"; }
    }
    
    record Ready(
            String orderId,
            java.time.Instant readyAt,
            String pickupCode
    ) implements OrderState {
        @Override public String name() { return "READY"; }
    }
    
    record PickedUp(
            String orderId,
            java.time.Instant pickedUpAt,
            String deliveryPartnerId,
            java.time.Duration estimatedDeliveryTime
    ) implements OrderState {
        @Override public String name() { return "PICKED_UP"; }
    }
    
    record Delivered(
            String orderId,
            java.time.Instant deliveredAt,
            String proofOfDelivery
    ) implements OrderState {
        @Override public String name() { return "DELIVERED"; }
    }
    
    record Cancelled(
            String orderId,
            java.time.Instant cancelledAt,
            String reason,
            String cancelledBy,
            boolean refundInitiated
    ) implements OrderState {
        @Override public String name() { return "CANCELLED"; }
    }
    
    // ========================================
    // PATTERN MATCHING
    // ========================================
    
    static String describeState(OrderState state) {
        return switch (state) {
            case Pending p -> "Order %s pending since %s".formatted(p.orderId(), p.createdAt());
            case Confirmed c -> "Order %s confirmed, payment: %s".formatted(c.orderId(), c.paymentId());
            case Preparing p -> "Order %s being prepared at restaurant %s".formatted(p.orderId(), p.restaurantId());
            case Ready r -> "Order %s ready for pickup. Code: %s".formatted(r.orderId(), r.pickupCode());
            case PickedUp p -> "Order %s picked up by %s".formatted(p.orderId(), p.deliveryPartnerId());
            case Delivered d -> "Order %s delivered at %s".formatted(d.orderId(), d.deliveredAt());
            case Cancelled c -> "Order %s cancelled: %s".formatted(c.orderId(), c.reason());
        };
    }
}
