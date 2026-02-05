-- Analytics Service Database Schema
-- V1__Create_analytics_tables.sql

-- Daily Order Metrics Table (Pre-aggregated daily data)
CREATE TABLE daily_order_metrics (
    id BIGSERIAL PRIMARY KEY,
    metric_date DATE NOT NULL,
    restaurant_id VARCHAR(36),
    
    -- Order Counts
    total_orders INTEGER NOT NULL DEFAULT 0,
    completed_orders INTEGER NOT NULL DEFAULT 0,
    cancelled_orders INTEGER NOT NULL DEFAULT 0,
    failed_orders INTEGER NOT NULL DEFAULT 0,
    
    -- Revenue Metrics
    gross_revenue DECIMAL(15, 2) NOT NULL DEFAULT 0,
    net_revenue DECIMAL(15, 2) NOT NULL DEFAULT 0,
    delivery_fees DECIMAL(15, 2) NOT NULL DEFAULT 0,
    tips_collected DECIMAL(15, 2) NOT NULL DEFAULT 0,
    discounts_given DECIMAL(15, 2) NOT NULL DEFAULT 0,
    refunds_issued DECIMAL(15, 2) NOT NULL DEFAULT 0,
    avg_order_value DECIMAL(10, 2) NOT NULL DEFAULT 0,
    
    -- Customer Metrics
    unique_customers INTEGER NOT NULL DEFAULT 0,
    new_customers INTEGER NOT NULL DEFAULT 0,
    repeat_customers INTEGER NOT NULL DEFAULT 0,
    
    -- Performance Metrics
    avg_delivery_time_minutes INTEGER,
    avg_preparation_time_minutes INTEGER,
    avg_restaurant_rating DECIMAL(3, 2),
    avg_driver_rating DECIMAL(3, 2),
    ratings_count INTEGER NOT NULL DEFAULT 0,
    
    -- Audit
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    CONSTRAINT uq_daily_metrics_date_restaurant UNIQUE (metric_date, restaurant_id)
);

-- Indexes for efficient querying
CREATE INDEX idx_daily_metrics_date ON daily_order_metrics(metric_date);
CREATE INDEX idx_daily_metrics_restaurant ON daily_order_metrics(restaurant_id) WHERE restaurant_id IS NOT NULL;
CREATE INDEX idx_daily_metrics_date_range ON daily_order_metrics(metric_date, restaurant_id);
CREATE INDEX idx_daily_metrics_platform ON daily_order_metrics(metric_date) WHERE restaurant_id IS NULL;

-- Order Events Table (Raw events for detailed analysis)
CREATE TABLE order_events (
    id VARCHAR(36) PRIMARY KEY,
    order_id VARCHAR(36) NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    event_timestamp TIMESTAMP NOT NULL,
    
    -- Related Entities
    customer_id VARCHAR(36),
    restaurant_id VARCHAR(36),
    driver_id VARCHAR(36),
    
    -- Financial Data
    order_total DECIMAL(10, 2),
    delivery_fee DECIMAL(10, 2),
    tip_amount DECIMAL(10, 2),
    discount_amount DECIMAL(10, 2),
    
    -- Processing Status
    processed BOOLEAN NOT NULL DEFAULT FALSE,
    processed_at TIMESTAMP,
    
    -- Audit
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for event processing
CREATE INDEX idx_order_events_order_id ON order_events(order_id);
CREATE INDEX idx_order_events_type ON order_events(event_type);
CREATE INDEX idx_order_events_timestamp ON order_events(event_timestamp);
CREATE INDEX idx_order_events_unprocessed ON order_events(processed) WHERE processed = FALSE;
CREATE INDEX idx_order_events_restaurant ON order_events(restaurant_id) WHERE restaurant_id IS NOT NULL;
CREATE INDEX idx_order_events_customer ON order_events(customer_id) WHERE customer_id IS NOT NULL;

-- Hourly Aggregates for Real-time Dashboard (Optional - for high-traffic scenarios)
CREATE TABLE hourly_metrics (
    id BIGSERIAL PRIMARY KEY,
    metric_hour TIMESTAMP NOT NULL,
    restaurant_id VARCHAR(36),
    
    orders_count INTEGER NOT NULL DEFAULT 0,
    gross_revenue DECIMAL(15, 2) NOT NULL DEFAULT 0,
    unique_customers INTEGER NOT NULL DEFAULT 0,
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT uq_hourly_metrics UNIQUE (metric_hour, restaurant_id)
);

CREATE INDEX idx_hourly_metrics_hour ON hourly_metrics(metric_hour);
CREATE INDEX idx_hourly_metrics_restaurant ON hourly_metrics(restaurant_id) WHERE restaurant_id IS NOT NULL;

-- Function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Trigger for daily_order_metrics
CREATE TRIGGER update_daily_metrics_updated_at
    BEFORE UPDATE ON daily_order_metrics
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Comment on tables
COMMENT ON TABLE daily_order_metrics IS 'Pre-aggregated daily metrics for dashboard and reporting';
COMMENT ON TABLE order_events IS 'Raw order events for detailed analysis and debugging';
COMMENT ON TABLE hourly_metrics IS 'Hourly aggregates for real-time dashboard (high-traffic scenarios)';
