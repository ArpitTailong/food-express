package com.foodexpress.order.dto;

import com.foodexpress.order.domain.DeliveryAddress;
import com.foodexpress.order.domain.Order;
import com.foodexpress.order.domain.OrderItem;
import com.foodexpress.order.domain.OrderStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Order DTOs using Java Records.
 */
public final class OrderDTOs {
    
    private OrderDTOs() {}
    
    // ========================================
    // REQUEST DTOs
    // ========================================
    
    /**
     * Create order request
     */
    public record CreateOrderRequest(
            @NotBlank(message = "Restaurant ID is required")
            String restaurantId,
            
            @NotEmpty(message = "At least one item is required")
            @Valid
            List<OrderItemRequest> items,
            
            @NotNull(message = "Delivery address is required")
            @Valid
            DeliveryAddressRequest deliveryAddress,
            
            String deliveryInstructions,
            
            String couponCode,
            
            @DecimalMin(value = "0", message = "Tip must be non-negative")
            BigDecimal tipAmount
    ) {}
    
    /**
     * Order item request
     */
    public record OrderItemRequest(
            @NotBlank(message = "Menu item ID is required")
            String menuItemId,
            
            @NotBlank(message = "Menu item name is required")
            String menuItemName,
            
            @NotNull(message = "Unit price is required")
            @DecimalMin(value = "0.01", message = "Price must be positive")
            BigDecimal unitPrice,
            
            @NotNull(message = "Quantity is required")
            @Min(value = 1, message = "Quantity must be at least 1")
            @Max(value = 99, message = "Quantity cannot exceed 99")
            Integer quantity,
            
            String specialInstructions
    ) {}
    
    /**
     * Delivery address request
     */
    public record DeliveryAddressRequest(
            @NotBlank(message = "Address line 1 is required")
            String addressLine1,
            
            String addressLine2,
            
            @NotBlank(message = "City is required")
            String city,
            
            @NotBlank(message = "State is required")
            String state,
            
            @NotBlank(message = "Postal code is required")
            String postalCode,
            
            @NotBlank(message = "Country is required")
            String country,
            
            Double latitude,
            Double longitude,
            
            @NotBlank(message = "Contact name is required")
            String contactName,
            
            @NotBlank(message = "Contact phone is required")
            String contactPhone,
            
            String addressType
    ) {}
    
    /**
     * Checkout/initiate payment request
     */
    public record CheckoutRequest(
            @NotBlank(message = "Payment method is required")
            String paymentMethod, // CARD, WALLET, UPI, COD
            
            String gatewayToken, // For CARD payments
            
            String savedPaymentMethodId
    ) {}
    
    /**
     * Cancel order request
     */
    public record CancelOrderRequest(
            @NotBlank(message = "Cancellation reason is required")
            @Size(max = 500, message = "Reason cannot exceed 500 characters")
            String reason
    ) {}
    
    /**
     * Update order status request (for restaurant/driver)
     */
    public record UpdateStatusRequest(
            @NotNull(message = "New status is required")
            OrderStatus newStatus,
            
            String driverId, // For driver assignment
            
            LocalDateTime estimatedTime // For estimated delivery
    ) {}
    
    /**
     * Rate order request
     */
    public record RateOrderRequest(
            @Min(1) @Max(5)
            Integer restaurantRating,
            
            @Min(1) @Max(5)
            Integer driverRating,
            
            @Size(max = 1000)
            String feedback
    ) {}
    
    // ========================================
    // RESPONSE DTOs
    // ========================================
    
    /**
     * Full order response
     */
    public record OrderResponse(
            String id,
            String customerId,
            String restaurantId,
            String driverId,
            List<OrderItemResponse> items,
            String status,
            BigDecimal subtotal,
            BigDecimal deliveryFee,
            BigDecimal taxAmount,
            BigDecimal discountAmount,
            BigDecimal tipAmount,
            BigDecimal totalAmount,
            String currency,
            String couponCode,
            DeliveryAddressResponse deliveryAddress,
            String deliveryInstructions,
            LocalDateTime estimatedDeliveryTime,
            LocalDateTime actualDeliveryTime,
            String paymentId,
            String paymentMethod,
            String paymentStatus,
            String cancellationReason,
            String cancelledBy,
            String failureReason,
            Integer restaurantRating,
            Integer driverRating,
            Instant createdAt,
            Instant confirmedAt,
            Instant preparingAt,
            Instant readyAt,
            Instant pickedUpAt,
            Instant deliveredAt,
            Instant cancelledAt
    ) {}
    
    /**
     * Order item response
     */
    public record OrderItemResponse(
            String id,
            String menuItemId,
            String menuItemName,
            String menuItemDescription,
            String menuItemImageUrl,
            BigDecimal unitPrice,
            Integer quantity,
            BigDecimal totalPrice,
            String specialInstructions
    ) {}
    
    /**
     * Delivery address response
     */
    public record DeliveryAddressResponse(
            String addressLine1,
            String addressLine2,
            String city,
            String state,
            String postalCode,
            String country,
            Double latitude,
            Double longitude,
            String contactName,
            String contactPhone,
            String addressType,
            String fullAddress
    ) {}
    
    /**
     * Order summary (for lists)
     */
    public record OrderSummaryResponse(
            String id,
            String restaurantId,
            String restaurantName, // Populated from restaurant service
            String status,
            int itemCount,
            BigDecimal totalAmount,
            String currency,
            Instant createdAt,
            LocalDateTime estimatedDeliveryTime
    ) {}
    
    /**
     * Checkout response
     */
    public record CheckoutResponse(
            String orderId,
            String status,
            PaymentInfo payment
    ) {
        public record PaymentInfo(
                String paymentId,
                String status,
                NextAction nextAction
        ) {}
        
        public record NextAction(
                String type,
                String redirectUrl,
                String clientSecret
        ) {}
    }
    
    /**
     * Order status response
     */
    public record OrderStatusResponse(
            String orderId,
            OrderStatus status,
            String statusDescription,
            boolean isActive,
            boolean isCancellable,
            LocalDateTime estimatedDeliveryTime,
            List<StatusHistoryItem> statusHistory
    ) {
        public record StatusHistoryItem(
                String status,
                Instant timestamp,
                String description
        ) {}
    }
    
    // ========================================
    // MAPPER METHODS
    // ========================================
    
    public static OrderResponse toResponse(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getCustomerId(),
                order.getRestaurantId(),
                order.getDriverId(),
                order.getItems().stream().map(OrderDTOs::toItemResponse).toList(),
                order.getStatus().name(),
                order.getSubtotal(),
                order.getDeliveryFee(),
                order.getTaxAmount(),
                order.getDiscountAmount(),
                order.getTipAmount(),
                order.getTotalAmount(),
                order.getCurrency(),
                order.getCouponCode(),
                toAddressResponse(order.getDeliveryAddress()),
                order.getDeliveryInstructions(),
                order.getEstimatedDeliveryTime(),
                order.getActualDeliveryTime(),
                order.getPaymentId(),
                order.getPaymentMethod(),
                order.getPaymentStatus(),
                order.getCancellationReason(),
                order.getCancelledBy(),
                order.getFailureReason(),
                order.getRestaurantRating(),
                order.getDriverRating(),
                order.getCreatedAt(),
                order.getConfirmedAt(),
                order.getPreparingAt(),
                order.getReadyAt(),
                order.getPickedUpAt(),
                order.getDeliveredAt(),
                order.getCancelledAt()
        );
    }
    
    public static OrderItemResponse toItemResponse(OrderItem item) {
        return new OrderItemResponse(
                item.getId(),
                item.getMenuItemId(),
                item.getMenuItemName(),
                item.getMenuItemDescription(),
                item.getMenuItemImageUrl(),
                item.getUnitPrice(),
                item.getQuantity(),
                item.getTotalPrice(),
                item.getSpecialInstructions()
        );
    }
    
    public static DeliveryAddressResponse toAddressResponse(DeliveryAddress addr) {
        if (addr == null) return null;
        return new DeliveryAddressResponse(
                addr.getAddressLine1(),
                addr.getAddressLine2(),
                addr.getCity(),
                addr.getState(),
                addr.getPostalCode(),
                addr.getCountry(),
                addr.getLatitude(),
                addr.getLongitude(),
                addr.getContactName(),
                addr.getContactPhone(),
                addr.getAddressType(),
                addr.getFullAddress()
        );
    }
    
    public static DeliveryAddress toDeliveryAddress(DeliveryAddressRequest req) {
        DeliveryAddress addr = new DeliveryAddress(
                req.addressLine1(),
                req.city(),
                req.state(),
                req.postalCode(),
                req.country(),
                req.contactName(),
                req.contactPhone()
        );
        addr.setAddressLine2(req.addressLine2());
        addr.setLatitude(req.latitude());
        addr.setLongitude(req.longitude());
        addr.setAddressType(req.addressType());
        return addr;
    }
    
    public static OrderSummaryResponse toSummaryResponse(Order order, String restaurantName) {
        return new OrderSummaryResponse(
                order.getId(),
                order.getRestaurantId(),
                restaurantName,
                order.getStatus().name(),
                order.getItems().size(),
                order.getTotalAmount(),
                order.getCurrency(),
                order.getCreatedAt(),
                order.getEstimatedDeliveryTime()
        );
    }
}
