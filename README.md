# Food Express - Production-Grade Microservices Platform

A comprehensive food delivery application built with modern Java 21, Spring Boot 3.x, and Spring Cloud technologies. This project demonstrates production-ready patterns and best practices for building enterprise microservices.

## 🏗️ Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           API Gateway (8080)                             │
│                    Spring Cloud Gateway + Security                       │
└─────────────────────────────────────────────────────────────────────────┘
                                     │
        ┌────────────┬───────────────┼───────────────┬────────────┐
        ▼            ▼               ▼               ▼            ▼
┌──────────┐  ┌──────────┐   ┌──────────┐   ┌──────────┐  ┌──────────┐
│   Auth   │  │   User   │   │   Order  │   │  Payment │  │ Analytics│
│  Service │  │  Service │   │  Service │   │  Service │  │  Service │
│  (8081)  │  │  (8084)  │   │  (8083)  │   │  (8082)  │  │  (8086)  │
└──────────┘  └──────────┘   └──────────┘   └──────────┘  └──────────┘
        │            │               │               │            │
        └────────────┴───────────────┼───────────────┴────────────┘
                                     ▼
                         ┌──────────────────┐
                         │   Notification   │
                         │     Service      │
                         │     (8085)       │
                         └──────────────────┘
```

## 🚀 Technologies Used

### Core Stack
- **Java 21** - Latest LTS with Virtual Threads, Records, Sealed Classes, Pattern Matching
- **Spring Boot 3.2.2** - Modern application framework
- **Spring Cloud 2023.0.0** - Microservices infrastructure

### Infrastructure
| Component | Technology | Purpose |
|-----------|------------|---------|
| Service Discovery | Eureka Server | Service registration & discovery |
| Configuration | Config Server | Centralized configuration |
| API Gateway | Spring Cloud Gateway | Routing, rate limiting, security |
| Database | PostgreSQL 16 | Primary data store |
| Caching | Redis 7 | Distributed caching, sessions, locking |
| Messaging | Apache Kafka | Event-driven communication |
| Tracing | Zipkin | Distributed tracing |
| Metrics | Prometheus + Grafana | Monitoring & alerting |

### Key Patterns Implemented
- ✅ **Saga Pattern** - Distributed transactions (Choreography-based)
- ✅ **Idempotent APIs** - Safe retry mechanism with Redis locks
- ✅ **Circuit Breaker** - Resilience4j for fault tolerance
- ✅ **Event Sourcing** - Payment state machine
- ✅ **CQRS** - Analytics service read optimization
- ✅ **Domain-Driven Design** - Aggregate roots, bounded contexts

## 📁 Project Structure

```
food-delivery-application/
├── common/
│   └── food-express-common/        # Shared library (DTOs, Events, Exceptions)
├── infrastructure/
│   ├── discovery-server/           # Eureka Server (8761)
│   ├── config-server/              # Config Server (8888)
│   └── api-gateway/                # API Gateway (8080)
├── services/
│   ├── auth-service/               # Authentication & Authorization (8081)
│   ├── payment-service/            # Payment Processing (8082)
│   ├── order-service/              # Order Management (8083)
│   ├── user-service/               # User Profiles (8084)
│   ├── notification-service/       # Multi-channel Notifications (8085)
│   └── analytics-service/          # Business Intelligence (8086)
├── docker/
│   ├── postgres/
│   ├── prometheus/
│   └── grafana/
├── docker-compose.yml
├── pom.xml                         # Parent POM
└── ARCHITECTURE.md                 # Detailed architecture documentation
```

## 🛠️ Getting Started

### Prerequisites
- Java 21 (Temurin/OpenJDK recommended)
- Maven 3.9+
- Docker & Docker Compose
- PostgreSQL 16 (or use Docker)
- Redis 7 (or use Docker)
- Apache Kafka (or use Docker)

### Option 1: Run with Docker Compose (Recommended)

```bash
# Clone the repository
git clone https://github.com/your-org/food-express.git
cd food-express

# Start all infrastructure
docker-compose up -d

# Check status
docker-compose ps
```

### Option 2: Run Locally

```bash
# 1. Start infrastructure services
docker-compose up -d postgres redis kafka zookeeper

# 2. Build the project
mvn clean install -DskipTests

# 3. Start Discovery Server first
cd infrastructure/discovery-server
mvn spring-boot:run

# 4. Start Config Server
cd infrastructure/config-server
mvn spring-boot:run

# 5. Start other services
cd services/auth-service && mvn spring-boot:run
cd services/user-service && mvn spring-boot:run
cd services/order-service && mvn spring-boot:run
cd services/payment-service && mvn spring-boot:run
cd services/notification-service && mvn spring-boot:run
cd services/analytics-service && mvn spring-boot:run

# 6. Start API Gateway last
cd infrastructure/api-gateway && mvn spring-boot:run
```

## 📡 API Endpoints

### Authentication
```bash
# Register a new user
POST http://localhost:8080/api/v1/auth/register

# Login
POST http://localhost:8080/api/v1/auth/login

# Refresh token
POST http://localhost:8080/api/v1/auth/refresh
```

### Orders
```bash
# Create order (authenticated)
POST http://localhost:8080/api/v1/orders

# Get my orders
GET http://localhost:8080/api/v1/orders/me

# Checkout (initiate payment)
POST http://localhost:8080/api/v1/orders/{orderId}/checkout
```

### Payments
```bash
# Process payment (idempotent)
POST http://localhost:8080/api/v1/payments
Headers: Idempotency-Key: {unique-key}

# Get payment status
GET http://localhost:8080/api/v1/payments/{paymentId}
```

### Analytics (Admin only)
```bash
# Get dashboard metrics
GET http://localhost:8080/api/v1/analytics/dashboard

# Get time series data
GET http://localhost:8080/api/v1/analytics/time-series/orders?startDate=2024-01-01&endDate=2024-01-31
```

## 🔐 Security

### Authentication Flow
1. User registers/logs in via Auth Service
2. JWT token issued with roles (CUSTOMER, DRIVER, RESTAURANT_OWNER, ADMIN)
3. API Gateway validates JWT on each request
4. Services use `@PreAuthorize` for role-based access

### Idempotency
Payment and order APIs support idempotent operations:
```bash
curl -X POST http://localhost:8080/api/v1/payments \
  -H "Authorization: Bearer {token}" \
  -H "Idempotency-Key: unique-request-id-12345" \
  -H "Content-Type: application/json" \
  -d '{"orderId": "order-123", "amount": 599.00}'
```

## 📊 Monitoring

### Endpoints
- **Eureka Dashboard**: http://localhost:8761
- **Prometheus**: http://localhost:9090
- **Grafana**: http://localhost:3000 (admin/admin)
- **Zipkin**: http://localhost:9411
- **Kafka UI**: http://localhost:8089

### Health Checks
Each service exposes Spring Actuator endpoints:
```bash
# Health check
GET http://localhost:{port}/actuator/health

# Prometheus metrics
GET http://localhost:{port}/actuator/prometheus

# Service info
GET http://localhost:{port}/actuator/info
```

## 🧪 Testing

```bash
# Run all tests
mvn test

# Run integration tests
mvn verify -P integration-tests

# Run specific service tests
cd services/payment-service && mvn test
```

## 📚 Key Java 21 Features Used

### Records (Immutable DTOs)
```java
public record OrderResponse(
    String id,
    String customerId,
    OrderStatus status,
    BigDecimal totalAmount,
    LocalDateTime createdAt
) {}
```

### Sealed Classes (Domain Events)
```java
public sealed interface DomainEvent permits 
    OrderCreatedEvent, 
    OrderDeliveredEvent, 
    PaymentCompletedEvent {
    String eventId();
    LocalDateTime occurredAt();
}
```

### Pattern Matching
```java
String description = switch (status) {
    case PENDING -> "Order is being processed";
    case CONFIRMED -> "Order confirmed by restaurant";
    case DELIVERED -> "Order delivered successfully";
    case CANCELLED -> "Order was cancelled";
};
```

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'Add amazing feature'`)
4. Push to branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- Spring Boot team for the excellent framework
- Netflix OSS for Eureka and inspiration
- Apache Kafka for reliable messaging
- The open-source community

---

**Built with ❤️ using Java 21 and Spring Boot 3.x**
