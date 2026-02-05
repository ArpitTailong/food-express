package com.foodexpress.payment.domain;

import java.util.Set;

/**
 * Payment status with built-in state machine transition rules.
 * 
 * State Flow:
 * ┌─────────┐    ┌────────────┐    ┌─────────┐
 * │ CREATED │───▶│ PROCESSING │───▶│ SUCCESS │───▶ REFUNDED
 * └─────────┘    └────────────┘    └─────────┘
 *                      │
 *                      ▼
 *                 ┌────────┐
 *                 │ FAILED │ (retryable)
 *                 └────────┘
 */
public enum PaymentStatus {
    
    CREATED {
        @Override
        public Set<PaymentStatus> allowedTransitions() {
            return Set.of(PROCESSING);
        }
    },
    
    PROCESSING {
        @Override
        public Set<PaymentStatus> allowedTransitions() {
            return Set.of(SUCCESS, FAILED);
        }
    },
    
    SUCCESS {
        @Override
        public Set<PaymentStatus> allowedTransitions() {
            return Set.of(REFUNDED);
        }
    },
    
    FAILED {
        @Override
        public Set<PaymentStatus> allowedTransitions() {
            // Failed payments can be retried (go back to PROCESSING)
            return Set.of(PROCESSING);
        }
    },
    
    REFUNDED {
        @Override
        public Set<PaymentStatus> allowedTransitions() {
            return Set.of(); // Terminal state
        }
    };
    
    /**
     * @return Set of allowed next states from current state
     */
    public abstract Set<PaymentStatus> allowedTransitions();
    
    /**
     * Check if transition to target status is allowed
     */
    public boolean canTransitionTo(PaymentStatus target) {
        return allowedTransitions().contains(target);
    }
    
    /**
     * @return true if this is a terminal state (no more transitions possible)
     */
    public boolean isTerminal() {
        return allowedTransitions().isEmpty();
    }
    
    /**
     * @return true if payment has completed successfully
     */
    public boolean isSuccessful() {
        return this == SUCCESS || this == REFUNDED;
    }
    
    /**
     * @return true if payment is still in progress
     */
    public boolean isPending() {
        return this == CREATED || this == PROCESSING;
    }
}
