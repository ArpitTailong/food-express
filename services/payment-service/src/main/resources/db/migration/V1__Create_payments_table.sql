-- V1__Create_payments_table.sql
-- Payment Service Database Schema
-- Version: 1.0.0
-- Author: Food Express Team

-- ========================================
-- PAYMENTS TABLE
-- ========================================

CREATE TABLE IF NOT EXISTS payments (
    id                      VARCHAR(36)     PRIMARY KEY,
    order_id                VARCHAR(36)     NOT NULL,
    customer_id             VARCHAR(36)     NOT NULL,
    idempotency_key         VARCHAR(64)     NOT NULL UNIQUE,
    
    -- Amount details
    amount                  DECIMAL(12, 2)  NOT NULL,
    currency                CHAR(3)         NOT NULL DEFAULT 'INR',
    
    -- Status
    status                  VARCHAR(20)     NOT NULL DEFAULT 'CREATED',
    payment_method          VARCHAR(20),
    
    -- Gateway information (NO RAW CARD DATA - PCI-DSS)
    gateway_token           VARCHAR(255),
    gateway_transaction_id  VARCHAR(100),
    gateway_response_code   VARCHAR(50),
    
    -- Masked card info for display
    card_last_four          CHAR(4),
    card_brand              VARCHAR(20),
    
    -- Error handling
    error_code              VARCHAR(50),
    error_message           VARCHAR(500),
    attempt_count           INTEGER         NOT NULL DEFAULT 0,
    
    -- Refund information
    refund_id               VARCHAR(100),
    refund_amount           DECIMAL(12, 2),
    refund_reason           VARCHAR(255),
    refunded_at             TIMESTAMPTZ,
    
    -- Timestamps
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at            TIMESTAMPTZ,
    completed_at            TIMESTAMPTZ,
    
    -- Optimistic locking
    version                 BIGINT          NOT NULL DEFAULT 0,
    
    -- Tracing
    correlation_id          VARCHAR(64),
    
    -- Constraints
    CONSTRAINT chk_status CHECK (status IN ('CREATED', 'PROCESSING', 'SUCCESS', 'FAILED', 'REFUNDED')),
    CONSTRAINT chk_payment_method CHECK (payment_method IN ('CARD', 'WALLET', 'UPI', 'COD', 'NET_BANKING')),
    CONSTRAINT chk_amount_positive CHECK (amount > 0),
    CONSTRAINT chk_refund_amount CHECK (refund_amount IS NULL OR (refund_amount > 0 AND refund_amount <= amount))
);

-- ========================================
-- INDEXES
-- ========================================

-- Primary lookup by order
CREATE INDEX IF NOT EXISTS idx_payment_order_id ON payments(order_id);

-- Idempotency check (already unique constraint)
CREATE UNIQUE INDEX IF NOT EXISTS idx_payment_idempotency_key ON payments(idempotency_key);

-- Status-based queries for batch jobs
CREATE INDEX IF NOT EXISTS idx_payment_status ON payments(status);

-- Customer payment history
CREATE INDEX IF NOT EXISTS idx_payment_customer_id ON payments(customer_id);

-- Find stuck payments
CREATE INDEX IF NOT EXISTS idx_payment_processing_time ON payments(status, processed_at) 
    WHERE status = 'PROCESSING';

-- Find retryable failed payments
CREATE INDEX IF NOT EXISTS idx_payment_failed_retry ON payments(status, attempt_count, updated_at) 
    WHERE status = 'FAILED';

-- Time-based analytics
CREATE INDEX IF NOT EXISTS idx_payment_created_at ON payments(created_at);
CREATE INDEX IF NOT EXISTS idx_payment_completed_at ON payments(completed_at) 
    WHERE status = 'SUCCESS';

-- ========================================
-- COMMENTS
-- ========================================

COMMENT ON TABLE payments IS 'Payment transactions for orders';
COMMENT ON COLUMN payments.idempotency_key IS 'Client-provided key for duplicate detection';
COMMENT ON COLUMN payments.gateway_token IS 'Payment gateway token (NOT raw card data)';
COMMENT ON COLUMN payments.attempt_count IS 'Number of payment attempts for this transaction';
COMMENT ON COLUMN payments.version IS 'Optimistic locking version';

-- ========================================
-- TRIGGER: Auto-update updated_at
-- ========================================

CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_payments_updated_at
    BEFORE UPDATE ON payments
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
