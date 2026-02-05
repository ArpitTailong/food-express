-- ============================================
-- V1: Create Notifications Table
-- Food Express Notification Service
-- ============================================

CREATE TABLE notifications (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    
    type VARCHAR(50) NOT NULL,
    channel VARCHAR(20) NOT NULL CHECK (channel IN ('EMAIL', 'SMS', 'PUSH', 'IN_APP')),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' 
        CHECK (status IN ('PENDING', 'SENT', 'DELIVERED', 'FAILED', 'CANCELLED')),
    
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    recipient VARCHAR(255),
    
    -- Reference to source event
    reference_type VARCHAR(50),
    reference_id VARCHAR(100),
    
    -- Delivery tracking
    external_id VARCHAR(255),
    retry_count INTEGER NOT NULL DEFAULT 0,
    max_retries INTEGER NOT NULL DEFAULT 3,
    failure_reason TEXT,
    
    -- User interaction
    is_read BOOLEAN NOT NULL DEFAULT false,
    read_at TIMESTAMP,
    
    -- Metadata (JSON)
    metadata JSONB,
    
    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sent_at TIMESTAMP,
    delivered_at TIMESTAMP,
    next_retry_at TIMESTAMP
);

-- Indexes
CREATE INDEX idx_notifications_user ON notifications(user_id);
CREATE INDEX idx_notifications_user_channel ON notifications(user_id, channel);
CREATE INDEX idx_notifications_user_unread ON notifications(user_id, is_read) WHERE is_read = false;
CREATE INDEX idx_notifications_status ON notifications(status);
CREATE INDEX idx_notifications_retry ON notifications(next_retry_at) WHERE status NOT IN ('DELIVERED', 'CANCELLED');
CREATE INDEX idx_notifications_created ON notifications(created_at DESC);
CREATE INDEX idx_notifications_reference ON notifications(reference_type, reference_id);

-- Comments
COMMENT ON TABLE notifications IS 'Multi-channel notification delivery tracking';
COMMENT ON COLUMN notifications.channel IS 'Delivery channel: EMAIL, SMS, PUSH, or IN_APP';
COMMENT ON COLUMN notifications.external_id IS 'Message ID from external provider (SendGrid, Twilio, FCM)';
COMMENT ON COLUMN notifications.next_retry_at IS 'When to retry failed notification (exponential backoff)';
