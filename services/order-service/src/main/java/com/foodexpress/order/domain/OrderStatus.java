package com.foodexpress.order.domain;

import java.util.Set;

/**
 * Order status with built-in state machine transition rules.
 * 
 * Complete Order Lifecycle:
 * 
 * ┌────────────┐    ┌───────────────┐    ┌───────────┐
 * │  PENDING   │───▶│ PAYMENT_PENDING│───▶│ CONFIRMED │
 * └────────────┘    └───────────────┘    └───────────┘
 *       │                  │                   │
 *       ▼                  ▼                   ▼
 * ┌───────────┐      ┌───────────┐      ┌───────────┐
 * │ CANCELLED │      │  FAILED   │      │ PREPARING │
 * └───────────┘      └───────────┘      └───────────┘
 *                                             │
 *                                             ▼
 *                                   ┌─────────────────┐
 *                                   │ READY_FOR_PICKUP│
 *                                   └─────────────────┘
 *                                             │
 *                                             ▼
 *                                   ┌─────────────────┐
 *                                   │OUT_FOR_DELIVERY │
 *                                   └─────────────────┘
 *                                             │
 *                                             ▼
 *                                   ┌─────────────────┐
 *                                   │   DELIVERED     │
 *                                   └─────────────────┘
 */
public enum OrderStatus {
    
    /**
     * Order just created, items added to cart
     */
    PENDING {
        @Override
        public Set<OrderStatus> allowedTransitions() {
            return Set.of(PAYMENT_PENDING, CANCELLED);
        }
    },
    
    /**
     * Payment initiated, waiting for payment confirmation
     */
    PAYMENT_PENDING {
        @Override
        public Set<OrderStatus> allowedTransitions() {
            return Set.of(CONFIRMED, FAILED, CANCELLED);
        }
    },
    
    /**
     * Payment successful, order confirmed and sent to restaurant
     */
    CONFIRMED {
        @Override
        public Set<OrderStatus> allowedTransitions() {
            return Set.of(PREPARING, CANCELLED, FAILED);
        }
    },
    
    /**
     * Restaurant accepted and preparing the order
     */
    PREPARING {
        @Override
        public Set<OrderStatus> allowedTransitions() {
            return Set.of(READY_FOR_PICKUP, CANCELLED, FAILED);
        }
    },
    
    /**
     * Order ready for driver to pick up
     */
    READY_FOR_PICKUP {
        @Override
        public Set<OrderStatus> allowedTransitions() {
            return Set.of(OUT_FOR_DELIVERY, CANCELLED, FAILED);
        }
    },
    
    /**
     * Driver picked up, en route to customer
     */
    OUT_FOR_DELIVERY {
        @Override
        public Set<OrderStatus> allowedTransitions() {
            return Set.of(DELIVERED, FAILED);
        }
    },
    
    /**
     * Successfully delivered to customer
     */
    DELIVERED {
        @Override
        public Set<OrderStatus> allowedTransitions() {
            return Set.of(); // Terminal state
        }
    },
    
    /**
     * Order cancelled (by customer, restaurant, or system)
     */
    CANCELLED {
        @Override
        public Set<OrderStatus> allowedTransitions() {
            return Set.of(); // Terminal state
        }
    },
    
    /**
     * Order failed (payment failed, restaurant rejected, delivery failed)
     */
    FAILED {
        @Override
        public Set<OrderStatus> allowedTransitions() {
            return Set.of(); // Terminal state
        }
    };
    
    /**
     * @return Set of allowed next states from current state
     */
    public abstract Set<OrderStatus> allowedTransitions();
    
    /**
     * Check if transition to target status is allowed
     */
    public boolean canTransitionTo(OrderStatus target) {
        return allowedTransitions().contains(target);
    }
    
    /**
     * @return true if this is a terminal state
     */
    public boolean isTerminal() {
        return allowedTransitions().isEmpty();
    }
    
    /**
     * @return true if order is in an active state (not terminal)
     */
    public boolean isActive() {
        return !isTerminal();
    }
    
    /**
     * @return true if order can be cancelled
     */
    public boolean isCancellable() {
        return canTransitionTo(CANCELLED);
    }
    
    /**
     * @return true if order has been successfully placed (past pending)
     */
    public boolean isPlaced() {
        return this != PENDING && this != CANCELLED && this != FAILED;
    }
    
    /**
     * @return true if order is being prepared or later
     */
    public boolean isInProgress() {
        return this == PREPARING || this == READY_FOR_PICKUP || this == OUT_FOR_DELIVERY;
    }
}
