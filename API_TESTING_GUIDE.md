# Food Express API Testing Guide

This document provides comprehensive testing scenarios for all microservices in the Food Express application.

## Prerequisites

- All services running on local profile
- Postman or any REST client
- JWT Token from Auth Service

## Service Ports

| Service | Port | Base URL |
|---------|------|----------|
| Auth Service | 8081 | http://localhost:8081 |
| Payment Service | 8082 | http://localhost:8082 |
| Order Service | 8083 | http://localhost:8083 |
| User Service | 8084 | http://localhost:8084 |
| Notification Service | 8085 | http://localhost:8085 |
| Analytics Service | 8086 | http://localhost:8086 |

---

## 1. Auth Service (Port 8081)

### 1.1 User Registration

**Endpoint:** `POST /api/v1/auth/register`

**Request:**
```json
{
  "email": "newuser@example.com",
  "password": "SecurePass123!",
  "firstName": "John",
  "lastName": "Doe",
  "phoneNumber": "+1234567890"
}
```

**Expected Response:** `201 Created`
```json
{
  "success": true,
  "message": "User registered successfully",
  "data": {
    "userId": "uuid-here",
    "email": "newuser@example.com"
  }
}
```

### 1.2 User Login

**Endpoint:** `POST /api/v1/auth/login`

**Request:**
```json
{
  "email": "admin@foodexpress.com",
  "password": "password123"
}
```

**Expected Response:** `200 OK`
```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 86400
  }
}
```

### 1.3 Refresh Token

**Endpoint:** `POST /api/v1/auth/refresh`

**Request:**
```json
{
  "refreshToken": "your-refresh-token-here"
}
```

**Expected Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "accessToken": "new-access-token",
    "refreshToken": "new-refresh-token"
  }
}
```

### 1.4 Logout

**Endpoint:** `POST /api/v1/auth/logout`

**Headers:**
```
Authorization: Bearer {accessToken}
```

**Expected Response:** `200 OK`

### 1.5 Get Current User

**Endpoint:** `GET /api/v1/auth/me`

**Headers:**
```
Authorization: Bearer {accessToken}
```

**Expected Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "userId": "uuid",
    "email": "admin@foodexpress.com",
    "roles": ["ADMIN"]
  }
}
```

---

## 2. User Service (Port 8084)

### 2.1 Get User Profile

**Endpoint:** `GET /api/v1/users/profile`

**Headers:**
```
Authorization: Bearer {accessToken}
```

**Expected Response:** `200 OK`

### 2.2 Update User Profile

**Endpoint:** `PUT /api/v1/users/profile`

**Headers:**
```
Authorization: Bearer {accessToken}
Content-Type: application/json
```

**Request:**
```json
{
  "firstName": "John",
  "lastName": "Smith",
  "phoneNumber": "+1987654321"
}
```

### 2.3 Add Delivery Address

**Endpoint:** `POST /api/v1/users/addresses`

**Headers:**
```
Authorization: Bearer {accessToken}
Content-Type: application/json
```

**Request:**
```json
{
  "label": "Home",
  "street": "123 Main Street",
  "city": "New York",
  "state": "NY",
  "zipCode": "10001",
  "country": "USA",
  "latitude": 40.7128,
  "longitude": -74.0060,
  "isDefault": true
}
```

### 2.4 Get All Addresses

**Endpoint:** `GET /api/v1/users/addresses`

**Headers:**
```
Authorization: Bearer {accessToken}
```

### 2.5 Delete Address

**Endpoint:** `DELETE /api/v1/users/addresses/{addressId}`

**Headers:**
```
Authorization: Bearer {accessToken}
```

---

## 3. Order Service (Port 8083)

### 3.1 Create Order

**Endpoint:** `POST /api/v1/orders`

**Headers:**
```
Authorization: Bearer {accessToken}
Content-Type: application/json
X-Idempotency-Key: unique-key-123
```

**Request:**
```json
{
  "restaurantId": "rest-001",
  "restaurantName": "Pizza Palace",
  "customerId": "cust-001",
  "customerName": "John Doe",
  "customerPhone": "+1234567890",
  "deliveryAddress": {
    "street": "123 Main St",
    "city": "New York",
    "state": "NY",
    "zipCode": "10001",
    "latitude": 40.7128,
    "longitude": -74.0060
  },
  "items": [
    {
      "menuItemId": "item-001",
      "name": "Margherita Pizza",
      "quantity": 2,
      "unitPrice": 12.99,
      "specialInstructions": "Extra cheese"
    },
    {
      "menuItemId": "item-002",
      "name": "Garlic Bread",
      "quantity": 1,
      "unitPrice": 4.99
    }
  ],
  "specialInstructions": "Ring doorbell twice",
  "paymentMethod": "CARD"
}
```

**Expected Response:** `201 Created`
```json
{
  "success": true,
  "data": {
    "orderId": "uuid-here",
    "orderNumber": "ORD-20260205-XXXX",
    "status": "PENDING",
    "subtotal": 30.97,
    "deliveryFee": 2.99,
    "tax": 2.78,
    "total": 36.74,
    "estimatedDeliveryTime": "2026-02-05T15:30:00"
  }
}
```

### 3.2 Get Order by ID

**Endpoint:** `GET /api/v1/orders/{orderId}`

**Headers:**
```
Authorization: Bearer {accessToken}
```

**Expected Response:** `200 OK`

### 3.3 Get Order Status

**Endpoint:** `GET /api/v1/orders/{orderId}/status`

**Headers:**
```
Authorization: Bearer {accessToken}
```

**Expected Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "orderId": "uuid",
    "status": "CONFIRMED",
    "statusHistory": [
      {"status": "PENDING", "timestamp": "2026-02-05T14:00:00"},
      {"status": "CONFIRMED", "timestamp": "2026-02-05T14:02:00"}
    ]
  }
}
```

### 3.4 Get Customer Orders

**Endpoint:** `GET /api/v1/orders/customer/{customerId}`

**Headers:**
```
Authorization: Bearer {accessToken}
```

**Query Parameters:**
- `page` (optional): Page number (default: 0)
- `size` (optional): Page size (default: 20)

### 3.5 Update Order Status (Restaurant/Driver)

**Endpoint:** `PUT /api/v1/orders/{orderId}/status`

**Headers:**
```
Authorization: Bearer {accessToken}
Content-Type: application/json
```

**Request - Accept Order:**
```json
{
  "status": "CONFIRMED",
  "estimatedPreparationTime": 25
}
```

**Request - Mark as Preparing:**
```json
{
  "status": "PREPARING"
}
```

**Request - Mark as Ready:**
```json
{
  "status": "READY_FOR_PICKUP"
}
```

**Request - Driver Pickup:**
```json
{
  "status": "PICKED_UP",
  "driverId": "driver-001",
  "driverName": "Mike Driver",
  "driverPhone": "+1555123456"
}
```

**Request - Out for Delivery:**
```json
{
  "status": "OUT_FOR_DELIVERY",
  "estimatedDeliveryTime": "2026-02-05T15:45:00"
}
```

**Request - Delivered:**
```json
{
  "status": "DELIVERED"
}
```

### 3.6 Cancel Order

**Endpoint:** `POST /api/v1/orders/{orderId}/cancel`

**Headers:**
```
Authorization: Bearer {accessToken}
Content-Type: application/json
```

**Request:**
```json
{
  "reason": "Changed my mind",
  "cancelledBy": "CUSTOMER"
}
```

### 3.7 Rate Order

**Endpoint:** `POST /api/v1/orders/{orderId}/rate`

**Headers:**
```
Authorization: Bearer {accessToken}
Content-Type: application/json
```

**Request:**
```json
{
  "foodRating": 5,
  "deliveryRating": 4,
  "overallRating": 5,
  "comment": "Great food, slightly late delivery"
}
```

---

## 4. Payment Service (Port 8082)

### 4.1 Create Payment

**Endpoint:** `POST /api/v1/payments`

**Headers:**
```
Authorization: Bearer {accessToken}
Content-Type: application/json
X-Idempotency-Key: pay-unique-key-123
```

**Request:**
```json
{
  "orderId": "order-001",
  "customerId": "cust-001",
  "amount": 36.74,
  "currency": "USD",
  "paymentMethod": "CARD",
  "gatewayToken": "tok_success"
}
```

**Gateway Tokens for Testing:**
- `tok_success` - Successful payment
- `tok_decline` - Declined payment
- `tok_insufficient` - Insufficient funds
- `tok_expired` - Expired card
- `tok_error` - Gateway error

**Expected Response:** `200 OK`
```json
{
  "paymentId": "uuid-here",
  "status": "SUCCESS",
  "payment": {
    "orderId": "order-001",
    "amount": 36.74,
    "currency": "USD",
    "status": "SUCCESS",
    "gatewayTransactionId": "txn_success_xxxxx",
    "cardLastFour": "4242",
    "cardBrand": "VISA"
  }
}
```

### 4.2 Get Payment by ID

**Endpoint:** `GET /api/v1/payments/{paymentId}`

**Headers:**
```
Authorization: Bearer {accessToken}
```

### 4.3 Get Payment Status

**Endpoint:** `GET /api/v1/payments/{paymentId}/status`

**Headers:**
```
Authorization: Bearer {accessToken}
```

**Expected Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "paymentId": "uuid",
    "status": "SUCCESS",
    "gatewayTransactionId": "txn_xxxxx"
  }
}
```

### 4.4 Get Payment by Order ID

**Endpoint:** `GET /api/v1/payments/order/{orderId}`

**Headers:**
```
Authorization: Bearer {accessToken}
```

### 4.5 Refund Payment

**Endpoint:** `POST /api/v1/payments/{paymentId}/refund`

**Headers:**
```
Authorization: Bearer {accessToken}
Content-Type: application/json
X-Idempotency-Key: refund-unique-key-123
```

**Request - Full Refund:**
```json
{
  "reason": "Order cancelled by customer"
}
```

**Request - Partial Refund:**
```json
{
  "amount": 15.00,
  "reason": "Missing item in order"
}
```

**Expected Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "paymentId": "uuid",
    "refundId": "refund-uuid",
    "refundAmount": 36.74,
    "status": "REFUNDED"
  }
}
```

### 4.6 Retry Failed Payment

**Endpoint:** `POST /api/v1/payments/{paymentId}/retry`

**Headers:**
```
Authorization: Bearer {accessToken}
Content-Type: application/json
X-Idempotency-Key: retry-unique-key-123
```

**Request:**
```json
{
  "gatewayToken": "tok_success"
}
```

---

## 5. Notification Service (Port 8085)

### 5.1 Send Notification

**Endpoint:** `POST /api/v1/notifications`

**Headers:**
```
Authorization: Bearer {accessToken}
Content-Type: application/json
```

**Request:**
```json
{
  "userId": "user-001",
  "type": "ORDER_UPDATE",
  "title": "Order Confirmed",
  "message": "Your order #ORD-123 has been confirmed",
  "channels": ["PUSH", "EMAIL"],
  "data": {
    "orderId": "order-001",
    "status": "CONFIRMED"
  }
}
```

### 5.2 Get User Notifications

**Endpoint:** `GET /api/v1/notifications/user/{userId}`

**Headers:**
```
Authorization: Bearer {accessToken}
```

**Query Parameters:**
- `page` (optional): Page number
- `size` (optional): Page size
- `unreadOnly` (optional): true/false

### 5.3 Mark Notification as Read

**Endpoint:** `PUT /api/v1/notifications/{notificationId}/read`

**Headers:**
```
Authorization: Bearer {accessToken}
```

### 5.4 Mark All as Read

**Endpoint:** `PUT /api/v1/notifications/user/{userId}/read-all`

**Headers:**
```
Authorization: Bearer {accessToken}
```

### 5.5 Get Unread Count

**Endpoint:** `GET /api/v1/notifications/user/{userId}/unread-count`

**Headers:**
```
Authorization: Bearer {accessToken}
```

---

## 6. Analytics Service (Port 8086)

### 6.1 Track Event

**Endpoint:** `POST /api/v1/analytics/events`

**Headers:**
```
Authorization: Bearer {accessToken}
Content-Type: application/json
```

**Request:**
```json
{
  "eventType": "ORDER_PLACED",
  "userId": "user-001",
  "properties": {
    "orderId": "order-001",
    "orderValue": 36.74,
    "itemCount": 3
  },
  "timestamp": "2026-02-05T14:30:00Z"
}
```

### 6.2 Get Order Analytics

**Endpoint:** `GET /api/v1/analytics/orders`

**Headers:**
```
Authorization: Bearer {accessToken}
```

**Query Parameters:**
- `startDate`: 2026-02-01
- `endDate`: 2026-02-05
- `restaurantId` (optional): Filter by restaurant

### 6.3 Get Revenue Analytics

**Endpoint:** `GET /api/v1/analytics/revenue`

**Headers:**
```
Authorization: Bearer {accessToken}
```

**Query Parameters:**
- `startDate`: 2026-02-01
- `endDate`: 2026-02-05
- `groupBy`: DAY | WEEK | MONTH

### 6.4 Get User Analytics

**Endpoint:** `GET /api/v1/analytics/users/{userId}`

**Headers:**
```
Authorization: Bearer {accessToken}
```

---

## Complete Testing Flow

### Scenario 1: Complete Order Flow

1. **Login** → Get access token
2. **Create Order** → Get orderId
3. **Create Payment** → Pay for order
4. **Update Order Status** → CONFIRMED
5. **Update Order Status** → PREPARING
6. **Update Order Status** → READY_FOR_PICKUP
7. **Update Order Status** → PICKED_UP (assign driver)
8. **Update Order Status** → OUT_FOR_DELIVERY
9. **Update Order Status** → DELIVERED
10. **Rate Order** → Submit rating

### Scenario 2: Order Cancellation with Refund

1. **Login** → Get access token
2. **Create Order** → Get orderId
3. **Create Payment** → Get paymentId
4. **Cancel Order** → Order cancelled
5. **Refund Payment** → Full refund

### Scenario 3: Failed Payment Retry

1. **Login** → Get access token
2. **Create Order** → Get orderId
3. **Create Payment** with `tok_decline` → Payment failed
4. **Retry Payment** with `tok_success` → Payment success

---

## Postman Collection Variables

Set these variables in your Postman environment:

```json
{
  "baseUrl": "http://localhost",
  "authPort": "8081",
  "paymentPort": "8082",
  "orderPort": "8083",
  "userPort": "8084",
  "notificationPort": "8085",
  "analyticsPort": "8086",
  "accessToken": "",
  "refreshToken": "",
  "currentOrderId": "",
  "currentPaymentId": ""
}
```

## Postman Pre-request Script (Auto Token)

Add this to collection-level pre-request script:

```javascript
// Auto-login and set token
if (!pm.environment.get("accessToken")) {
    pm.sendRequest({
        url: pm.environment.get("baseUrl") + ":" + pm.environment.get("authPort") + "/api/v1/auth/login",
        method: 'POST',
        header: {
            'Content-Type': 'application/json'
        },
        body: {
            mode: 'raw',
            raw: JSON.stringify({
                email: "admin@foodexpress.com",
                password: "password123"
            })
        }
    }, function (err, res) {
        if (!err) {
            var jsonData = res.json();
            pm.environment.set("accessToken", jsonData.data.accessToken);
            pm.environment.set("refreshToken", jsonData.data.refreshToken);
        }
    });
}
```

---

## Error Responses

### 400 Bad Request
```json
{
  "success": false,
  "message": "Validation failed",
  "errors": [
    {"field": "email", "message": "Invalid email format"},
    {"field": "amount", "message": "Amount must be positive"}
  ]
}
```

### 401 Unauthorized
```json
{
  "success": false,
  "message": "Invalid or expired token"
}
```

### 403 Forbidden
```json
{
  "success": false,
  "message": "Access denied"
}
```

### 404 Not Found
```json
{
  "success": false,
  "message": "Resource not found"
}
```

### 409 Conflict (Idempotency)
```json
{
  "success": false,
  "message": "Request already processed",
  "data": {
    "originalResponse": {...}
  }
}
```

### 500 Internal Server Error
```json
{
  "success": false,
  "message": "Internal server error",
  "errorCode": "INTERNAL_ERROR"
}
```

---

## Tips for Testing

1. **Always include Authorization header** for protected endpoints
2. **Use unique X-Idempotency-Key** for POST requests to prevent duplicates
3. **Check service logs** for debugging (in the terminal running each service)
4. **H2 Console** available at `http://localhost:{port}/h2-console` for each service
5. **Test token expiry** by waiting or using short-lived tokens

---

## H2 Database Console Access

For debugging, access the H2 console:

- **URL:** `http://localhost:{port}/h2-console`
- **JDBC URL:** `jdbc:h2:mem:{service}db` (e.g., `jdbc:h2:mem:authdb`)
- **Username:** `sa`
- **Password:** (empty)

| Service | H2 Console | JDBC URL |
|---------|------------|----------|
| Auth | http://localhost:8081/h2-console | jdbc:h2:mem:authdb |
| Payment | http://localhost:8082/h2-console | jdbc:h2:mem:paymentdb |
| Order | http://localhost:8083/h2-console | jdbc:h2:mem:orderdb |
| User | http://localhost:8084/h2-console | jdbc:h2:mem:userdb |
| Notification | http://localhost:8085/h2-console | jdbc:h2:mem:notificationdb |
| Analytics | http://localhost:8086/h2-console | jdbc:h2:mem:analyticsdb |
