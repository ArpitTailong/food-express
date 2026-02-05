# Payment Service

Payment processing microservice for Food Express application.

## Features

### Core Capabilities
- ✅ **Idempotent Payment Processing** - Duplicate request protection via Redis-backed idempotency keys
- ✅ **State Machine** - Robust payment state transitions (CREATED → PROCESSING → SUCCESS/FAILED → REFUNDED)
- ✅ **Distributed Locking** - Redisson-based locks prevent concurrent payment attempts
- ✅ **Saga Orchestration** - Event-driven saga for order-payment coordination
- ✅ **PCI-DSS Compliance** - Tokenization pattern - NO raw card data stored
- ✅ **Circuit Breaker** - Resilience4j protection for payment gateway calls
- ✅ **Rate Limiting** - Per-customer rate limits to prevent abuse
- ✅ **Retry Logic** - Exponential backoff for failed payments

### Technologies
- **Java 21** - Records, Sealed classes, Pattern matching, Virtual threads
- **Spring Boot 3.2.2** - Core framework
- **PostgreSQL** - Primary data store with Flyway migrations
- **Redis/Redisson** - Idempotency, distributed locking, caching
- **Apache Kafka** - Event streaming for saga pattern
- **Resilience4j** - Circuit breaker, retry, rate limiter, bulkhead
- **Micrometer + Prometheus** - Metrics and monitoring

## Architecture

### Payment State Machine

```
┌─────────┐    ┌────────────┐    ┌─────────┐
│ CREATED │───▶│ PROCESSING │───▶│ SUCCESS │───▶ REFUNDED
└─────────┘    └────────────┘    └─────────┘
                     │
                     ▼
                ┌────────┐
                │ FAILED │ (retryable)
                └────────┘
```

### Payment Flow

```
Client Request (POST /api/v1/payments)
  │
  ├─ Validate request (Spring Validation)
  ├─ Check idempotency key (Redis)
  │   └─ If duplicate → return cached response
  │
  ├─ Acquire distributed lock (Redisson)
  ├─ Create payment entity (CREATED state)
  ├─ Save to PostgreSQL
  ├─ Publish PaymentCreated event (Kafka)
  │
  ├─ Transition to PROCESSING state
  ├─ Call Payment Gateway (with circuit breaker)
  │   ├─ Success → mark SUCCESS, publish PaymentCompleted
  │   ├─ 3DS Required → return REQUIRES_ACTION
  │   └─ Failure → mark FAILED, publish PaymentFailed
  │
  ├─ Store response in Redis (idempotency cache)
  └─ Return response to client
```

### Saga Pattern (Choreography)

**Order Creation Saga:**
```
Order Service                Payment Service
     │                            │
     ├─ OrderCreated ────────────▶│
     │                            ├─ Process Payment
     │◀────────── PaymentCompleted│
     ├─ Mark Order CONFIRMED      │
     
     │◀──────────── PaymentFailed │
     └─ Cancel Order (compensate) │
```

**Order Cancellation Saga:**
```
Order Service                Payment Service
     │                            │
     ├─ OrderCancelled ──────────▶│
     │                            ├─ Refund Payment
     │◀────────── PaymentRefunded │
     └─ Mark Order CANCELLED      │
```

## API Endpoints

### Payment Operations

#### Create Payment
```http
POST /api/v1/payments
Headers:
  Authorization: Bearer <JWT>
  X-Idempotency-Key: <unique-key>
  Content-Type: application/json

Body:
{
  "orderId": "order-123",
  "customerId": "customer-456",
  "amount": 1299.99,
  "currency": "INR",
  "paymentMethod": "CARD",
  "gatewayToken": "tok_visa_4242"
}

Response (201):
{
  "paymentId": "pay-789",
  "status": "SUCCESS",
  "nextAction": null,
  "payment": {
    "id": "pay-789",
    "orderId": "order-123",
    "amount": 1299.99,
    "currency": "INR",
    "status": "SUCCESS",
    "gatewayTransactionId": "txn_abc123"
  }
}
```

#### Get Payment
```http
GET /api/v1/payments/{paymentId}
Headers:
  Authorization: Bearer <JWT>

Response (200):
{
  "id": "pay-789",
  "orderId": "order-123",
  "customerId": "customer-456",
  "amount": 1299.99,
  "currency": "INR",
  "status": "SUCCESS",
  "paymentMethod": "CARD",
  "cardLastFour": "4242",
  "cardBrand": "VISA",
  "createdAt": "2026-02-04T10:30:00Z",
  "completedAt": "2026-02-04T10:30:05Z"
}
```

#### Refund Payment
```http
POST /api/v1/payments/{paymentId}/refund
Headers:
  Authorization: Bearer <JWT>
  X-Idempotency-Key: <unique-key>

Body:
{
  "reason": "Customer requested refund",
  "amount": 1299.99
}

Response (200):
{
  "id": "pay-789",
  "status": "REFUNDED",
  "refund": {
    "refundId": "ref_xyz",
    "amount": 1299.99,
    "reason": "Customer requested refund",
    "refundedAt": "2026-02-04T11:00:00Z"
  }
}
```

### Query Operations

- `GET /api/v1/payments/{paymentId}` - Get payment by ID
- `GET /api/v1/payments/{paymentId}/status` - Get payment status
- `GET /api/v1/payments/order/{orderId}` - Get payment for order
- `GET /api/v1/payments/customer/{customerId}` - List customer payments

## Configuration

### Environment Variables

```yaml
# Database
DB_URL=jdbc:postgresql://localhost:5432/payment_db
DB_USERNAME=postgres
DB_PASSWORD=postgres

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=

# Kafka
KAFKA_BROKERS=localhost:9092

# Security
JWT_ISSUER_URI=http://localhost:8081/auth-service
JWK_SET_URI=http://localhost:8081/auth-service/api/v1/auth/.well-known/jwks.json

# Payment Gateway
PAYMENT_GATEWAY_PROVIDER=MOCK  # MOCK, STRIPE, RAZORPAY
```

### Application Profiles

- `dev` - Development (default)
- `prod` - Production

## Database Schema

### payments table
```sql
CREATE TABLE payments (
    id                      VARCHAR(36) PRIMARY KEY,
    order_id                VARCHAR(36) NOT NULL,
    customer_id             VARCHAR(36) NOT NULL,
    idempotency_key         VARCHAR(64) UNIQUE NOT NULL,
    amount                  DECIMAL(12,2) NOT NULL,
    currency                CHAR(3) NOT NULL DEFAULT 'INR',
    status                  VARCHAR(20) NOT NULL,
    payment_method          VARCHAR(20),
    gateway_token           VARCHAR(255),
    gateway_transaction_id  VARCHAR(100),
    card_last_four          CHAR(4),
    card_brand              VARCHAR(20),
    error_code              VARCHAR(50),
    error_message           VARCHAR(500),
    attempt_count           INT DEFAULT 0,
    created_at              TIMESTAMPTZ NOT NULL,
    updated_at              TIMESTAMPTZ NOT NULL,
    processed_at            TIMESTAMPTZ,
    completed_at            TIMESTAMPTZ,
    version                 BIGINT NOT NULL DEFAULT 0
);
```

## Kafka Topics

### Published Events
- `payment-events` - Payment lifecycle events
  - `PAYMENT_CREATED`
  - `PAYMENT_COMPLETED`
  - `PAYMENT_FAILED`
  - `PAYMENT_REFUNDED`

### Consumed Events
- `order-events` - Order lifecycle events
  - `ORDER_CANCELLED` → Trigger refund
  - `ORDER_FAILED` → Trigger refund

## Resilience Patterns

### Circuit Breaker (Payment Gateway)
```yaml
slidingWindowSize: 10
failureRateThreshold: 50%
waitDurationInOpenState: 30s
halfOpenCallsPermitted: 3
```

### Retry (Exponential Backoff)
```yaml
maxAttempts: 3
waitDuration: 500ms
multiplier: 2.0
# Retries: 500ms, 1s, 2s
```

### Rate Limiting
- **Create Payment**: 10 requests/minute per customer
- **Refund**: 5 requests/minute
- **Retry**: 3 requests/minute

## Monitoring & Observability

### Metrics (Prometheus)
- `payments_created_total` - Total payments created
- `payments_successful_total` - Successful payments
- `payments_failed_total` - Failed payments
- `payments_refunded_total` - Refunded payments
- `payments_processing_time_seconds` - Payment processing duration

### Health Checks
- `GET /actuator/health` - Overall health
- `GET /actuator/health/db` - Database connectivity
- `GET /actuator/health/redis` - Redis connectivity
- `GET /actuator/metrics` - Application metrics
- `GET /actuator/prometheus` - Prometheus metrics

### Tracing
- Zipkin integration for distributed tracing
- Correlation ID propagation via MDC

## Security

### Authentication & Authorization
- JWT-based authentication
- Role-based access control (RBAC)
  - `CUSTOMER` - Create payment, view own payments, retry
  - `ADMIN` - All operations including refunds
  - `SUPPORT` - Refund operations
  - `SERVICE` - Service-to-service calls

### Idempotency
- Required header: `X-Idempotency-Key`
- 24-hour TTL for idempotency cache
- Prevents duplicate charges from network retries

## Testing

### Mock Payment Gateway

Use special test tokens for predictable behavior:

```
tok_success           → Always succeeds
tok_fail              → Always fails
tok_insufficient      → Insufficient funds error
tok_declined          → Card declined error
tok_3ds_required      → Requires 3D Secure
tok_network_error     → Network error (for circuit breaker testing)
```

### Example Test Flow
```bash
# Create payment with test token
curl -X POST http://localhost:8083/api/v1/payments \
  -H "Authorization: Bearer $JWT" \
  -H "X-Idempotency-Key: test-$(date +%s)" \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "order-123",
    "customerId": "customer-456",
    "amount": 100.00,
    "currency": "INR",
    "paymentMethod": "CARD",
    "gatewayToken": "tok_success"
  }'
```

## Scheduled Jobs

### Stuck Payment Handler
- **Schedule**: Every 10 minutes
- **Purpose**: Mark payments stuck in PROCESSING as FAILED
- **Timeout**: 30 minutes (configurable)

### Failed Payment Retry
- **Schedule**: Every 5 minutes
- **Purpose**: Auto-retry failed payments with exponential backoff
- **Max Attempts**: 3

### Payment Statistics
- **Schedule**: Every hour
- **Purpose**: Log payment counts by status

## API Documentation

- Swagger UI: http://localhost:8083/swagger-ui.html
- OpenAPI Spec: http://localhost:8083/v3/api-docs

## Development

### Prerequisites
- Java 21
- PostgreSQL 15+
- Redis 7+
- Apache Kafka 3.x

### Run Locally
```bash
# Start dependencies
docker-compose up -d postgres redis kafka

# Run service
./mvnw spring-boot:run
```

### Build
```bash
./mvnw clean package
```

## Production Considerations

1. **Payment Gateway Integration**
   - Replace `MockPaymentGateway` with real implementation (Stripe, Razorpay)
   - Implement webhook handlers for async payment confirmations
   - Add 3D Secure flow handling

2. **Security Hardening**
   - Enable TLS for Kafka, Redis, PostgreSQL
   - Rotate JWT signing keys regularly
   - Implement rate limiting at API Gateway level
   - Add request signing for service-to-service calls

3. **High Availability**
   - Use PostgreSQL replication
   - Deploy Redis Cluster
   - Configure Kafka with replication factor ≥ 3
   - Run multiple service instances behind load balancer

4. **Disaster Recovery**
   - Regular database backups
   - Kafka topic retention policies
   - Idempotency key cleanup (after 30 days)

5. **Monitoring & Alerts**
   - Alert on high failure rates (> 5%)
   - Alert on stuck payments
   - Monitor circuit breaker state changes
   - Track P95/P99 latency

## Troubleshooting

### Payment Stuck in PROCESSING
- Check payment gateway logs
- Verify network connectivity
- Scheduled job will auto-mark as FAILED after 30 minutes

### Duplicate Payment Detected
- Check idempotency key in Redis
- Verify client is generating unique keys
- Check 24-hour cache TTL

### Circuit Breaker Open
- Check payment gateway availability
- Review recent error rates
- Wait for automatic half-open transition (30s)

### Rate Limit Exceeded
- Check customer request frequency
- Adjust rate limits in Resilience4j config
- Implement exponential backoff in client

## License

MIT
