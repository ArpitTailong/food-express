# 🍕 FoodExpress - Production-Grade Microservices Architecture

## Executive Summary

FoodExpress is a production-grade, cloud-native food delivery platform built using microservices architecture. This document outlines the architectural decisions, patterns, and design principles that guide the system.

---

## 📐 High-Level Architecture

```
                                    ┌─────────────────────────────────────────────────────────────┐
                                    │                        CLIENTS                               │
                                    │  (Mobile Apps, Web Apps, Partner APIs, Admin Dashboards)    │
                                    └─────────────────────────────────────────────────────────────┘
                                                              │
                                                              ▼
┌─────────────────────────────────────────────────────────────────────────────────────────────────────┐
│                                         API GATEWAY                                                  │
│                                    (Spring Cloud Gateway)                                           │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────┐   │
│  │ Rate Limiting│  │ JWT Validation│  │ Request Route│  │ Load Balance │  │ Circuit Breaker      │   │
│  └──────────────┘  └──────────────┘  └──────────────┘  └──────────────┘  └──────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────────────────────────────────┘
                                                              │
                    ┌─────────────────────────────────────────┼─────────────────────────────────────────┐
                    │                                         │                                         │
                    ▼                                         ▼                                         ▼
┌─────────────────────────────┐     ┌─────────────────────────────┐     ┌─────────────────────────────┐
│      SERVICE DISCOVERY       │     │       CONFIG SERVER          │     │    DISTRIBUTED TRACING     │
│         (Eureka)             │     │   (Spring Cloud Config)      │     │   (Zipkin + Micrometer)    │
└─────────────────────────────┘     └─────────────────────────────┘     └─────────────────────────────┘
                                                              │
─────────────────────────────────────────────────────────────────────────────────────────────────────────
                                    MICROSERVICES LAYER
─────────────────────────────────────────────────────────────────────────────────────────────────────────
│                                                                                                       │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌───────────┐  │
│  │   AUTH      │  │    USER     │  │   ORDER     │  │  PAYMENT    │  │NOTIFICATION │  │ ANALYTICS │  │
│  │  SERVICE    │  │   SERVICE   │  │  SERVICE    │  │  SERVICE    │  │   SERVICE   │  │  SERVICE  │  │
│  │             │  │             │  │             │  │             │  │             │  │           │  │
│  │ • JWT Auth  │  │ • Profile   │  │ • Orders    │  │ • Payment   │  │ • Email     │  │ • Audit   │  │
│  │ • OKTA      │  │ • Address   │  │ • Cart      │  │ • Gateway   │  │ • SMS       │  │ • Events  │  │
│  │ • RBAC      │  │ • Prefs     │  │ • Saga      │  │ • Refund    │  │ • Push      │  │ • Metrics │  │
│  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘  └─────┬─────┘  │
│         │                │                │                │                │              │         │
│         │                │                │                │                │              │         │
│    ┌────┴────┐      ┌────┴────┐      ┌────┴────┐      ┌────┴────┐          │              │         │
│    │ Redis   │      │PostgreSQL│      │PostgreSQL│      │PostgreSQL│          │              │         │
│    │ (Tokens)│      │+ Redis L2│      │         │      │ + Redis │          │              │         │
│    └─────────┘      └─────────┘      └─────────┘      └─────────┘          │              │         │
│                                                                             │              │         │
─────────────────────────────────────────────────────────────────────────────────────────────────────────
                                    MESSAGE BROKER (KAFKA)
─────────────────────────────────────────────────────────────────────────────────────────────────────────
│                                                                                                       │
│    Topics:                                                                                            │
│    ┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐                   │
│    │ order.created   │ │ payment.events  │ │ notification    │ │ audit.events    │                   │
│    │ order.updated   │ │ payment.success │ │ .requests       │ │                 │                   │
│    │ order.cancelled │ │ payment.failed  │ │                 │ │                 │                   │
│    └─────────────────┘ └─────────────────┘ └─────────────────┘ └─────────────────┘                   │
│                                                                                                       │
─────────────────────────────────────────────────────────────────────────────────────────────────────────
                                    OBSERVABILITY STACK
─────────────────────────────────────────────────────────────────────────────────────────────────────────
│                                                                                                       │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐                  │
│  │   Prometheus    │  │    Grafana      │  │     Zipkin      │  │   ELK Stack     │                  │
│  │   (Metrics)     │  │  (Dashboards)   │  │   (Tracing)     │  │   (Logging)     │                  │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘  └─────────────────┘                  │
│                                                                                                       │
─────────────────────────────────────────────────────────────────────────────────────────────────────────
```

---

## 🔄 Key Architectural Patterns

### 1. Saga Pattern (Choreography-based)

Used for distributed transactions across Order → Payment → Notification services.

```
┌─────────────┐    ORDER_CREATED    ┌─────────────┐   PAYMENT_SUCCESS   ┌─────────────────┐
│   ORDER     │ ──────────────────► │  PAYMENT    │ ─────────────────► │  NOTIFICATION   │
│  SERVICE    │                     │  SERVICE    │                     │    SERVICE      │
└─────────────┘                     └─────────────┘                     └─────────────────┘
      │                                   │                                     │
      │                            PAYMENT_FAILED                               │
      │ ◄─────────────────────────────────┤                                     │
      │                                                                         │
      │   COMPENSATING TRANSACTION                                              │
      │   (Mark Order as Failed)                                                │
      └─────────────────────────────────────────────────────────────────────────┘
```

### 2. Payment State Machine

```
                    ┌──────────────┐
                    │   CREATED    │
                    └──────┬───────┘
                           │ initiate()
                           ▼
                    ┌──────────────┐
           ┌────────│  PROCESSING  │────────┐
           │        └──────────────┘        │
           │ success()              fail()  │
           ▼                                ▼
    ┌──────────────┐               ┌──────────────┐
    │   SUCCESS    │               │   FAILED     │
    └──────┬───────┘               └──────┬───────┘
           │ refund()                     │ retry()
           ▼                              │
    ┌──────────────┐                      │
    │   REFUNDED   │                      │
    └──────────────┘                      │
                                          │
           └──────────────────────────────┘
                    (back to PROCESSING)
```

### 3. Event-Driven Architecture

All services communicate asynchronously through Kafka for non-critical flows:

| Event | Producer | Consumers |
|-------|----------|-----------|
| `order.created` | Order Service | Payment Service, Analytics |
| `payment.success` | Payment Service | Order Service, Notification |
| `payment.failed` | Payment Service | Order Service, Notification |
| `notification.request` | Order/Payment | Notification Service |
| `audit.event` | All Services | Analytics Service |

### 4. Database-per-Service Pattern

Each microservice owns its data:

| Service | Database | Cache | Purpose |
|---------|----------|-------|---------|
| Auth Service | Redis | Redis | Token storage, session management |
| User Service | PostgreSQL | Redis (L2) | User profiles, addresses |
| Order Service | PostgreSQL | Redis | Orders, cart, order history |
| Payment Service | PostgreSQL | Redis | Transactions, payment state |
| Notification Service | PostgreSQL | - | Notification logs |
| Analytics Service | PostgreSQL | - | Audit logs, metrics |

---

## 🔐 Security Architecture

### Authentication Flow (JWT + OKTA OAuth2)

```
┌────────┐     1. Login Request      ┌────────────┐
│ Client │ ──────────────────────►   │   AUTH     │
│        │                           │  SERVICE   │
│        │     2. Redirect to OKTA   │            │
│        │ ◄───────────────────────  │            │
└────┬───┘                           └────────────┘
     │
     │  3. OKTA Authentication
     ▼
┌──────────┐
│   OKTA   │
│  Server  │
└────┬─────┘
     │  4. Authorization Code
     ▼
┌────────┐     5. Exchange Code      ┌────────────┐
│ Client │ ──────────────────────►   │   AUTH     │
│        │                           │  SERVICE   │
│        │     6. JWT Token          │            │
│        │ ◄───────────────────────  │            │
└────────┘                           └────────────┘
```

### Authorization Model (RBAC)

```java
public enum Role {
    CUSTOMER,           // Place orders, view history
    RESTAURANT_OWNER,   // Manage restaurant, menu
    DELIVERY_PARTNER,   // Accept deliveries
    ADMIN,              // Full system access
    SUPPORT             // Customer support
}
```

### Service-to-Service Authentication

- Internal JWT tokens for service-to-service calls
- API Gateway validates external tokens
- Services trust requests from API Gateway (with correlation ID)

---

## 💳 Payment Service - Deep Dive

### Idempotency Strategy

```
┌─────────────────────────────────────────────────────────────────┐
│                    IDEMPOTENCY IMPLEMENTATION                    │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  1. Client generates unique idempotency_key (UUID)              │
│                                                                  │
│  2. Request: POST /api/v1/payments                              │
│     Headers:                                                     │
│       X-Idempotency-Key: 550e8400-e29b-41d4-a716-446655440000   │
│                                                                  │
│  3. Server Flow:                                                 │
│     ┌─────────────────────────────────────────────────────────┐ │
│     │ Check Redis: GET idempotency:{key}                      │ │
│     │   ├── EXISTS: Return cached response                    │ │
│     │   └── NOT EXISTS:                                       │ │
│     │         ├── Acquire distributed lock                    │ │
│     │         ├── Process payment                             │ │
│     │         ├── Store result with TTL (24h)                 │ │
│     │         └── Release lock                                │ │
│     └─────────────────────────────────────────────────────────┘ │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### Exactly-Once Semantics (Kafka)

```java
// Producer Configuration
props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
props.put(ProducerConfig.ACKS_CONFIG, "all");
props.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, "payment-tx-");

// Consumer Configuration
props.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");
props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
```

### PCI-DSS Awareness

```
┌─────────────────────────────────────────────────────────────────┐
│                    TOKENIZATION FLOW                             │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ❌ We NEVER store:                                             │
│     • Card numbers (PAN)                                        │
│     • CVV/CVC                                                   │
│     • Full magnetic stripe data                                 │
│                                                                  │
│  ✅ We store:                                                   │
│     • Payment gateway tokens (Stripe payment_method_id)         │
│     • Last 4 digits (for display)                               │
│     • Card brand (Visa, MasterCard)                             │
│     • Expiry month/year                                         │
│                                                                  │
│  Flow:                                                           │
│  1. Client → Payment Gateway (Stripe.js) → Token               │
│  2. Token → Our Payment Service → Process with Token            │
│  3. No sensitive card data touches our servers                  │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 🛡️ Resilience Patterns

### Circuit Breaker Configuration

```yaml
resilience4j:
  circuitbreaker:
    instances:
      paymentGateway:
        slidingWindowSize: 10
        failureRateThreshold: 50
        waitDurationInOpenState: 30s
        permittedNumberOfCallsInHalfOpenState: 3
        slowCallDurationThreshold: 2s
        slowCallRateThreshold: 80
```

### Retry Strategy

```yaml
resilience4j:
  retry:
    instances:
      paymentGateway:
        maxAttempts: 3
        waitDuration: 1s
        exponentialBackoffMultiplier: 2
        retryExceptions:
          - java.net.ConnectException
          - java.util.concurrent.TimeoutException
        ignoreExceptions:
          - com.foodexpress.payment.exception.PaymentDeclinedException
```

### Rate Limiting

```yaml
resilience4j:
  ratelimiter:
    instances:
      paymentApi:
        limitForPeriod: 100        # requests
        limitRefreshPeriod: 1s     # per second
        timeoutDuration: 500ms     # wait time for permit
```

---

## 📊 Observability Stack

### Metrics (Prometheus)

Key metrics exposed:
- `payment_requests_total{status, method}`
- `payment_processing_duration_seconds`
- `order_created_total{restaurant_id}`
- `circuit_breaker_state{name, state}`
- `kafka_consumer_lag`

### Distributed Tracing

```
[Gateway] ─── traceId: abc123 ───►[Order Service]───►[Payment Service]───►[Notification]
    │                                    │                  │                    │
    └──── spanId: 001 ───────────────────┴── spanId: 002 ──┴── spanId: 003 ────┘
```

### Structured Logging

```json
{
  "timestamp": "2026-02-03T10:15:30.123Z",
  "level": "INFO",
  "service": "payment-service",
  "traceId": "abc123def456",
  "spanId": "789xyz",
  "message": "Payment processed successfully",
  "context": {
    "orderId": "ORD-12345",
    "paymentId": "PAY-67890",
    "amount": 150.00,
    "currency": "USD",
    "maskedCard": "****4242"
  }
}
```

---

## 📁 Project Module Structure

```
food-express/
├── pom.xml                           # Parent POM
├── docs/                             # Documentation
│   ├── ARCHITECTURE.md
│   ├── API_CONTRACTS.md
│   └── DEPLOYMENT.md
│
├── common/                           # Shared Library
│   └── food-express-common/
│       ├── pom.xml
│       └── src/main/java/
│           └── com/foodexpress/common/
│               ├── dto/              # Shared DTOs (Records)
│               ├── event/            # Kafka Event schemas
│               ├── exception/        # Common exceptions
│               ├── security/         # Security utilities
│               └── util/             # Utilities
│
├── infrastructure/
│   ├── discovery-server/             # Eureka Server
│   ├── config-server/                # Spring Cloud Config
│   └── api-gateway/                  # Spring Cloud Gateway
│
├── services/
│   ├── auth-service/
│   ├── user-service/
│   ├── order-service/
│   ├── payment-service/
│   ├── notification-service/
│   └── analytics-service/
│
├── docker/
│   ├── docker-compose.yml
│   ├── docker-compose.infra.yml
│   └── Dockerfile.template
│
└── jenkins/
    └── Jenkinsfile
```

---

## 🎯 Design Decisions & Trade-offs

| Decision | Rationale | Trade-off |
|----------|-----------|-----------|
| Choreography Saga | Loose coupling, services independent | Complex debugging, eventual consistency |
| PostgreSQL per service | Data isolation, independent scaling | Data duplication, cross-service queries complex |
| Redis for distributed locking | Fast, proven solution | Additional infrastructure |
| Kafka for events | Durability, exactly-once support | Operational complexity |
| API Gateway security | Centralized auth, simplified services | Single point of failure (mitigated by replicas) |

---

## 📝 Interview Talking Points

1. **Why Saga over 2PC?**
   - 2PC has blocking, single point of failure
   - Saga supports long-running transactions
   - Better for microservices (eventual consistency accepted)

2. **How do you handle duplicate payments?**
   - Idempotency keys in Redis
   - Distributed locking before processing
   - Database unique constraints on transaction IDs

3. **How is PCI compliance achieved?**
   - Tokenization - no raw card data stored
   - Payment gateway handles sensitive data
   - We only store tokens, masked data

4. **Why Virtual Threads?**
   - Millions of concurrent connections with minimal threads
   - Simplified async code (no callback hell)
   - Perfect for I/O-bound operations (DB, HTTP calls)

5. **Circuit Breaker strategy?**
   - Prevent cascade failures
   - Fast failure for degraded services
   - Automatic recovery with half-open state

---

*Next: Setting up the Parent POM and Common Library module*
